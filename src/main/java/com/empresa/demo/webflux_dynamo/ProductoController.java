package com.empresa.demo.webflux_dynamo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/productos")
public class ProductoController {

    private final ProductoService productoService;
    private static final Logger log = LoggerFactory.getLogger(ProductoController.class);

    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    @GetMapping
    public Flux<Producto> listar() {
        return productoService.findAll()
                .doOnNext(p -> log.info("Producto: %s", p.getNombre()));
    }

    @PostMapping
    public Mono<Producto> crear(@RequestBody Producto producto) {
        return productoService.save(producto)
                .doOnNext(p -> log.info("Producto creado: %s", p.getId()));
    }
}