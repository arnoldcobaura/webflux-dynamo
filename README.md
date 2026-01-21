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

## Próximas mejoras

- Agregar endpoints GET por ID, DELETE, UPDATE
- Implementar paginación en `findAll()`
- Agregar validaciones de entrada
- Implementar manejo de errores más robusto
- Agregar tests unitarios e integración
