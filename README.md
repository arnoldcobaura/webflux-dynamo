# Spring Boot WebFlux con DynamoDB

Aplicación demo de Spring Boot WebFlux integrada con AWS DynamoDB para operaciones reactivas.

## Descripción

Esta es una aplicación de demostración que muestra cómo integrar Spring Boot WebFlux con DynamoDB usando el AWS SDK v2. La aplicación implementa un servicio reactivo para gestionar productos con operaciones asincrónicas.

## Requisitos previos

- Java 17+
- Maven 3.6+
- Cuenta AWS con credenciales configuradas localmente
- Tabla DynamoDB creada en AWS

## Configuración

### 1. Crear tabla en DynamoDB

Ejecuta el siguiente comando con AWS CLI:

```bash
aws dynamodb create-table \
  --table-name productos \
  --attribute-definitions AttributeName=id,AttributeType=S \
  --key-schema AttributeName=id,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST \
  --region us-east-2
```

### 2. Configurar credenciales AWS

Las credenciales se cargan automáticamente desde `~/.aws/credentials`. Verifica que estén configuradas:

```bash
aws configure
```

La aplicación usa `Region.US_EAST_2` por defecto. Si necesitas cambiar la región, edita `DynamoDbConfig.java`.

## Estructura del proyecto

```
src/main/java/com/empresa/demo/webflux_dynamo/
├── WebfluxDynamoApplication.java    # Clase principal
├── DynamoDbConfig.java              # Configuración del cliente DynamoDB
├── Producto.java                    # Entidad
├── ProductoService.java             # Servicio reactivo
├── ProductoController.java          # Controlador REST
└── DataLoader.java                  # Cargador de datos iniciales
```

## Dependencias principales

- **Spring Boot 3.5.9** - Framework web reactivo
- **Spring WebFlux** - Stack reactivo
- **AWS SDK v2** - Cliente DynamoDB asincrónico
- **Lombok** - Reducción de boilerplate
- **Project Reactor** - Implementación de Mono/Flux

## Ejecución

### Compilar y ejecutar

```bash
mvn clean install
mvn spring-boot:run
```

La aplicación se inicia en `http://localhost:8080` y carga automáticamente 9 productos de ejemplo en DynamoDB.

## API Endpoints

### GET /api/productos

Retorna todos los productos como un stream reactivo (Flux).

```bash
curl http://localhost:8080/api/productos
```

**Respuesta:**
```json
[
  {
    "id": "uuid-1",
    "nombre": "TV Panasonic Pantalla LCD",
    "precio": 456.89,
    "createAt": "2024-01-21T13:05:00"
  },
  ...
]
```

### POST /api/productos

Crea un nuevo producto.

```bash
curl -X POST http://localhost:8080/api/productos \
  -H "Content-Type: application/json" \
  -d '{
    "nombre": "Laptop Dell XPS",
    "precio": 1299.99
  }'
```

**Respuesta:**
```json
{
  "id": "uuid-generado",
  "nombre": "Laptop Dell XPS",
  "precio": 1299.99,
  "createAt": "2024-01-21T13:06:00"
}
```

## Conceptos clave

### Reactividad

- **Mono**: Representa 0 o 1 elemento (usado en `save()`)
- **Flux**: Representa 0 a N elementos (usado en `findAll()`)
- Ambos son no-bloqueantes y permiten procesamiento asincrónico

### DynamoDB sin repositorio reactivo

A diferencia de MongoDB, DynamoDB no tiene un repositorio reactivo oficial en Spring Data. Por eso:
- Usamos `DynamoDbAsyncClient` directamente
- Convertimos `CompletableFuture` a `Mono`/`Flux` con `Mono.fromFuture()`
- Implementamos el servicio manualmente

### Mapeo de datos

Los productos se mapean manualmente entre objetos Java y `AttributeValue` de DynamoDB:
- Strings: `AttributeValue.builder().s(valor).build()`
- Números: `AttributeValue.builder().n(valor).build()`

## Logs

La aplicación registra:
- Productos insertados durante la carga inicial
- Errores en operaciones DynamoDB
- Acceso a endpoints

Ejemplo:
```
Producto insertado: uuid-1 - TV Panasonic Pantalla LCD
Carga de datos completada
Producto: TV PANASONIC PANTALLA LCD
```

## Notas de desarrollo

- El `DataLoader` se ejecuta automáticamente al arrancar (implementa `CommandLineRunner`)
- Los IDs se generan automáticamente con UUID
- Los timestamps se generan automáticamente con `LocalDateTime.now()`
- La región AWS es `us-east-2` (configurable en `DynamoDbConfig`)

## Despliegue en ECS

### 1. Construir imagen Docker

Para Mac (Apple Silicon) - construir para arquitectura x86_64:

```bash
docker buildx build --platform linux/amd64 -t webflux_demo:1.0 --load .
```

### 2. Subir imagen a ECR

```bash
# Obtener Account ID
aws sts get-caller-identity --query Account --output text

# Autenticar en ECR
aws ecr get-login-password --region us-east-2 | docker login --username AWS --password-stdin TU_ACCOUNT_ID.dkr.ecr.us-east-2.amazonaws.com

# Crear repositorio (si no existe)
aws ecr create-repository --repository-name webflux_demo --region us-east-2

# Taggear y subir imagen
docker tag webflux_demo:1.0 TU_ACCOUNT_ID.dkr.ecr.us-east-2.amazonaws.com/webflux_demo:1.0
docker push TU_ACCOUNT_ID.dkr.ecr.us-east-2.amazonaws.com/webflux_demo:1.0
```

### 3. Configurar IAM Roles

#### Rol de Task (para acceso a DynamoDB)

**Desde consola AWS:**
1. Ve a **IAM > Roles > Create role**
2. **Trusted entity**: "AWS service"
3. **Service**: "Elastic Container Service"
4. **Use case**: "Elastic Container Service Task"
5. **Next**
6. **Permissions**: Busca y selecciona "AmazonDynamoDBFullAccess"
7. **Next**
8. **Role name**: `ECS-DynamoDB-Role`
9. **Create role**

**Desde línea de comandos:**
- **Nombre**: `ECS-DynamoDB-Role`
- **Trusted entity**: Elastic Container Service Task
- **Permissions**: `AmazonDynamoDBFullAccess`

#### Rol de Execution (para descargar imágenes ECR)

**Desde consola AWS:**
1. Ve a **IAM > Roles > Create role**
2. **Trusted entity**: "AWS service"
3. **Service**: "Elastic Container Service"
4. **Use case**: "Task Execution Role for Elastic Container Service"
5. **Next**
6. **Permissions**: "AmazonEC2ContainerRegistryReadOnly" y "CloudWatchLogsFullAccess"
7. **Next**
8. **Role name**: `ecsTaskExecutionRole` (si no existe)
9. **Create role**

**Nota**: `ecsTaskExecutionRole` suele existir por defecto en la mayoría de cuentas AWS.

**Desde línea de comandos:**
- **Nombre**: `ecsTaskExecutionRole` (ya existe por defecto)
- **Permissions**: Acceso a ECR y CloudWatch Logs

### 4. Crear Task Definition en ECS

```json
{
  "family": "webflux-dynamo-task",
  "networkMode": "awsvpc",
  "requiresCompatibilities": ["FARGATE"],
  "cpu": "256",
  "memory": "512",
  "executionRoleArn": "arn:aws:iam::TU_ACCOUNT:role/ecsTaskExecutionRole",
  "taskRoleArn": "arn:aws:iam::TU_ACCOUNT:role/ECS-DynamoDB-Role",
  "containerDefinitions": [
    {
      "name": "webflux-dynamo",
      "image": "TU_ACCOUNT.dkr.ecr.us-east-2.amazonaws.com/webflux_demo:1.0",
      "portMappings": [{"containerPort": 8080, "protocol": "tcp"}],
      "environment": [
        {"name": "AWS_REGION", "value": "us-east-2"}
      ]
    }
  ]
}
```

### 5. Crear Cluster y Servicio

#### Opción A: Desde consola AWS

**Crear Cluster:**
1. Ve a **ECS > Clusters > Create cluster**
2. **Cluster name**: `webflux-cluster`
3. **Infrastructure**: FARGATE (sin servidor)
4. **Create cluster**

**Crear Task Definition:**
1. Ve a **ECS > Task Definitions > Create new Task Definition**
2. **Launch type**: FARGATE
3. **Task name**: `webflux-dynamo-task`
4. **Task memory**: 512 GB
5. **Task CPU**: 256 vCPU
6. **Task role**: `ECS-DynamoDB-Role`
7. **Execution role**: `ecsTaskExecutionRole`
8. **Container name**: `webflux-dynamo`
9. **Image URI**: `TU_ACCOUNT.dkr.ecr.us-east-2.amazonaws.com/webflux_demo:1.0`
10. **Port mappings**: 8080
11. **Environment variables**: `AWS_REGION`: `us-east-2`
12. **Create**

**Crear Servicio:**
1. Dentro del cluster: **Create service**
2. **Task definition**: `webflux-dynamo-task`
3. **Service name**: `webflux-service`
4. **Number of tasks**: 1
5. **Networking**: Configura VPC y subnets
6. **Load balancer**: Opcional, para acceso público
7. **Create service**

#### Opción B: Desde línea de comandos

- **Cluster**: `webflux-cluster` (FARGATE)
- **Service**: `webflux-service`
- **Task count**: 1
- **Networking**: Configurar VPC y subnets
- **Load Balancer**: Opcional, para acceso público

### 6. Troubleshooting

#### Error: `exec format error`
**Causa**: Imagen construida para ARM64 (Mac) pero ECS usa x86_64
**Solución**: Usar `--platform linux/amd64` al construir

#### Error: Acceso denegado a DynamoDB
**Causa**: Task role sin permisos DynamoDB
**Solución**: Asignar rol `ECS-DynamoDB-Role` con permisos `AmazonDynamoDBFullAccess`

#### Error: No se puede encontrar la imagen
**Causa**: Imagen no subida a ECR o tag incorrecto
**Solución**: Verificar URI en ECR y usar tag específico (ej: `:2.0`)

#### Verificar funcionamiento
```bash
curl http://IP_PUBLICO:8080/api/productos
```

## Próximas mejoras

- Agregar endpoints GET por ID, DELETE, UPDATE
- Implementar paginación en `findAll()`
- Agregar validaciones de entrada
- Implementar manejo de errores más robusto
- Agregar tests unitarios e integración
- Configurar CI/CD para despliegue automático
- Agregar métricas y monitoring
