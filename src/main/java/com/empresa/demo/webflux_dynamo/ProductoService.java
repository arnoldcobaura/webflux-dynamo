package com.empresa.demo.webflux_dynamo;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class ProductoService {

	private final DynamoDbAsyncClient dynamoDb;
	private static final String TABLE_NAME = "productos";

	public ProductoService(DynamoDbAsyncClient dynamoDb) {
		this.dynamoDb = dynamoDb;
	}

	public Mono<Producto> save(Producto producto) {
		if (producto.getId() == null) {
			producto.setId(UUID.randomUUID().toString());
		}
		if (producto.getCreateAt() == null) {
			producto.setCreateAt(LocalDateTime.now());
		}

		Map<String, AttributeValue> item = new HashMap<>();
		item.put("id", AttributeValue.builder().s(producto.getId()).build());
		item.put("nombre", AttributeValue.builder().s(producto.getNombre()).build());
		item.put("precio", AttributeValue.builder().n(producto.getPrecio().toString()).build());
		item.put("createAt", AttributeValue.builder().s(producto.getCreateAt().toString()).build());

		PutItemRequest request = PutItemRequest.builder()
				.tableName(TABLE_NAME)
				.item(item)
				.build();

		return Mono.fromFuture(dynamoDb.putItem(request))
				.map(response -> producto);
	}

	public Flux<Producto> findAll() {
		ScanRequest request = ScanRequest.builder()
				.tableName(TABLE_NAME)
				.build();

		return Mono.fromFuture(dynamoDb.scan(request))
				.flatMapMany(response -> Flux.fromIterable(response.items()))
				.map(this::mapToProducto);
	}

	private Producto mapToProducto(Map<String, AttributeValue> item) {
		return new Producto(
				item.get("id").s(),
				item.get("nombre").s(),
				Double.parseDouble(item.get("precio").n()),
				LocalDateTime.parse(item.get("createAt").s()));
	}
}