package com.example.demo.controllers;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.CategoriaRequest;
import com.example.demo.dto.CategoriaResponse;
import com.example.demo.model.Department;
import com.example.demo.model.Producto;
import com.example.demo.repo.categoriaRepo;
import com.example.demo.repo.productoRepo;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Categorías", description = "Operaciones para gestionar categorías")
public class CategoriaController {

    @Autowired
    categoriaRepo repo;

    @Autowired
    productoRepo repoProducto;

    @GetMapping("categorias")
    @PreAuthorize("permitAll()")
    @SecurityRequirements
    @Operation(summary = "Listar todas las categorías", description = "Obtiene el catálogo completo de categorías")
    public ResponseEntity<List<CategoriaResponse>> findAllcategoria() {
        List<CategoriaResponse> response = this.repo.findAll().stream()
                .map(CategoriaResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }

    @PostMapping("categoria")
    @Operation(summary = "Crear categoría", description = "Registra una nueva categoría (requiere rol ADMIN)")
    public ResponseEntity<CategoriaResponse> addcategoria(@Valid @RequestBody CategoriaRequest request) {
        Department categoria = Department.fromRequest(request);
        Department saved = repo.save(categoria);
        URI location = URI.create("/categoria/" + saved.getId());
        return ResponseEntity.created(location).body(CategoriaResponse.from(saved));
    }

    @PostMapping("addProductoCategoria/{productId}/{categoriaId}")
    @Operation(summary = "Asociar producto a categoría", description = "Agrega un producto existente a una categoría (requiere rol ADMIN)")
    public ResponseEntity<CategoriaResponse> addcategoria(@PathVariable String productId, @PathVariable String categoriaId) {
        Optional<Department> dOpt = repo.findById(categoriaId);
        if (dOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Optional<Producto> pOpt = repoProducto.findById(productId);
        if (pOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Department d = dOpt.get();
        d.getProductos().add(pOpt.get());
        Department saved = repo.save(d);
        return ResponseEntity.ok(CategoriaResponse.from(saved));
    }

    @GetMapping("categoria/producto/{productId}")
    @PreAuthorize("permitAll()")
    @SecurityRequirements
    @Operation(summary = "Obtener categorías por producto", description = "Lista las categorías asociadas a un producto por su ID")
    public ResponseEntity<Set<CategoriaResponse>> getDepartmentsByProduct(@PathVariable String productId) {
        return repoProducto.findById(productId)
                .map(p -> ResponseEntity.ok(
                        p.getDeparments().stream()
                                .map(CategoriaResponse::from)
                                .collect(Collectors.toSet())))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("categoria/{id}")
    @PreAuthorize("permitAll()")
    @SecurityRequirements
    @Operation(summary = "Obtener categoría por ID", description = "Busca una categoría específica mediante su identificador")
    public ResponseEntity<CategoriaResponse> getById(@PathVariable String id) {
        return repo.findById(id)
                .map(value -> ResponseEntity.ok(CategoriaResponse.from(value)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("categoria/{id}")
    @Operation(summary = "Actualizar categoría", description = "Actualiza los datos de una categoría existente (requiere rol ADMIN)")
    public ResponseEntity<CategoriaResponse> putDepartment(@PathVariable String id, @Valid @RequestBody CategoriaRequest request) {
        Optional<Department> existingOpt = repo.findById(id);
        if (existingOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Department existing = existingOpt.get();
        existing.updateFrom(request);
        Department saved = repo.save(existing);
        return ResponseEntity.ok(CategoriaResponse.from(saved));
    }

    @DeleteMapping("categoria/{id}")
    @Operation(summary = "Eliminar categoría", description = "Elimina una categoría por su ID (requiere rol ADMIN)")
    public ResponseEntity<Void> deleteDeparment(@PathVariable String id) {
        if (!repo.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }
} 