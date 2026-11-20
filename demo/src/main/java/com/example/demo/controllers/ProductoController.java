package com.example.demo.controllers;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.ProductoPatchRequest;
import com.example.demo.dto.ProductoRequest;
import com.example.demo.dto.ProductoResponse;
import com.example.demo.model.Producto;
import com.example.demo.repo.productoRepo;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Productos", description = "Operaciones para gestionar productos")
public class ProductoController {

    @Autowired
    productoRepo repo;

    @GetMapping("productos")
    @PreAuthorize("permitAll()")
    @SecurityRequirements
    @Operation(summary = "Listar todos los productos", description = "Obtiene el catálogo completo de productos")
    public ResponseEntity<List<ProductoResponse>> findAllProductos() {
        List<ProductoResponse> response = this.repo.findAll().stream()
                .map(ProductoResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }

    @PostMapping("producto")
    @Operation(summary = "Crear producto", description = "Registra un nuevo producto (requiere rol ADMIN)")
    public ResponseEntity<ProductoResponse> addProducto(@Valid @RequestBody ProductoRequest request) {
        Producto producto = Producto.fromRequest(request);
        Producto saved = repo.save(producto);
        URI location = URI.create("/producto/" + saved.getId());
        return ResponseEntity.created(location).body(ProductoResponse.from(saved));
    }

    @GetMapping("producto/{id}")
    @PreAuthorize("permitAll()")
    @SecurityRequirements
    @Operation(summary = "Obtener producto por ID", description = "Busca un producto específico mediante su identificador")
    public ResponseEntity<ProductoResponse> getById(@PathVariable String id) {
        return repo.findById(id)
                .map(value -> ResponseEntity.ok(ProductoResponse.from(value)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("producto/{id}")
    @Operation(summary = "Actualizar producto", description = "Reemplaza todos los datos editables de un producto existente (requiere rol ADMIN)")
    public ResponseEntity<ProductoResponse> putProduct(@PathVariable String id, @Valid @RequestBody ProductoRequest request) {
        Optional<Producto> existingOpt = repo.findById(id);
        if (existingOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Producto existing = existingOpt.get();
        existing.updateFrom(request);
        Producto saved = repo.save(existing);
        return ResponseEntity.ok(ProductoResponse.from(saved));
    }

    @PatchMapping("producto/{id}")
    @Operation(summary = "Actualizar parcialmente un producto", description = "Actualiza solo los campos enviados en el cuerpo (requiere rol ADMIN)")
    public ResponseEntity<ProductoResponse> patchProducto(
            @PathVariable String id,
            @Valid @RequestBody ProductoPatchRequest patch) {
        if (patch.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        Optional<Producto> existingOpt = repo.findById(id);
        if (existingOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Producto existing = existingOpt.get();
        existing.patchFrom(patch);
        Producto saved = repo.save(existing);
        return ResponseEntity.ok(ProductoResponse.from(saved));
    }

    @DeleteMapping("producto/{id}")
    @Operation(summary = "Eliminar producto", description = "Elimina un producto por su ID (requiere rol ADMIN)")
    public ResponseEntity<Void> deleteMailbox(@PathVariable("id") String id) {
        if (!repo.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }
} 