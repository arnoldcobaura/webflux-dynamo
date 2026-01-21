package com.empresa.demo.webflux_dynamo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Producto {
	
	private String id;
	private String nombre;
	private Double precio;
	private LocalDateTime createAt;
}