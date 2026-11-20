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

import com.example.demo.dto.ComentarioRequest;
import com.example.demo.dto.ComentarioResponse;
import com.example.demo.model.Comment;
import com.example.demo.model.Producto;
import com.example.demo.repo.comentarioRepo;
import com.example.demo.repo.productoRepo;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Comentarios", description = "Operaciones para gestionar comentarios de productos")
public class ComentarioController {

    @Autowired
    comentarioRepo repo;

    @Autowired
    productoRepo repoProducto;

    @GetMapping("comentarios")
    @PreAuthorize("permitAll()")
    @SecurityRequirements
    @Operation(summary = "Listar todos los comentarios", description = "Obtiene el listado completo de comentarios registrados")
    public ResponseEntity<List<ComentarioResponse>> findAllcomentarios() {
        List<ComentarioResponse> response = this.repo.findAll().stream()
                .map(ComentarioResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }

    @PostMapping("comentario")
    @Operation(summary = "Crear comentario", description = "Registra un nuevo comentario (requiere rol USER o ADMIN)")
    public ResponseEntity<ComentarioResponse> addcomentario(@Valid @RequestBody ComentarioRequest request) {
        Comment comentario = Comment.fromRequest(request);
        Comment saved = repo.save(comentario);
        URI location = URI.create("/comentario/" + saved.getId());
        return ResponseEntity.created(location).body(ComentarioResponse.from(saved));
    }

    @PostMapping("comentario/{productId}")
    @Operation(summary = "Crear comentario en producto", description = "Asocia un nuevo comentario a un producto por su ID (requiere rol USER o ADMIN)")
    public ResponseEntity<ComentarioResponse> addcomentario(@Valid @RequestBody ComentarioRequest request, @PathVariable String productId) {
        Optional<Producto> p = repoProducto.findById(productId);
        if (p.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Comment comentario = Comment.fromRequest(request);
        comentario.setProducto(p.get());
        Comment saved = repo.save(comentario);
        URI location = URI.create("/comentario/" + saved.getId());
        return ResponseEntity.created(location).body(ComentarioResponse.from(saved));
    }

    @GetMapping("comentario/{productId}")
    @PreAuthorize("permitAll()")
    @SecurityRequirements
    @Operation(summary = "Listar comentarios por producto", description = "Obtiene los comentarios asociados a un producto por su ID")
    public ResponseEntity<Set<ComentarioResponse>> getComentariosByProduct(@PathVariable String productId) {
        return repoProducto.findById(productId)
                .map(p -> ResponseEntity.ok(
                        p.getComentarios().stream()
                                .map(ComentarioResponse::from)
                                .collect(Collectors.toSet())))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("comentario/{id}")
    @PreAuthorize("permitAll()")
    @SecurityRequirements
    @Operation(summary = "Obtener comentario por ID", description = "Busca un comentario específico mediante su identificador")
    public ResponseEntity<ComentarioResponse> getById(@PathVariable String id) {
        return repo.findById(id)
                .map(value -> ResponseEntity.ok(ComentarioResponse.from(value)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("comentario/{id}")
    @Operation(summary = "Actualizar comentario", description = "Actualiza los datos de un comentario existente (requiere rol USER o ADMIN)")
    public ResponseEntity<ComentarioResponse> putusuari(@PathVariable String id, @Valid @RequestBody ComentarioRequest request) {
        Optional<Comment> existingOpt = repo.findById(id);
        if (existingOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Comment existing = existingOpt.get();
        existing.updateFrom(request);
        Comment saved = repo.save(existing);
        return ResponseEntity.ok(ComentarioResponse.from(saved));
    }

    @DeleteMapping("comentario/{id}")
    @Operation(summary = "Eliminar comentario", description = "Elimina un comentario por su ID (requiere rol USER o ADMIN)")
    public ResponseEntity<Void> deleteMailbox(@PathVariable String id) {
        if (!repo.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }
} 