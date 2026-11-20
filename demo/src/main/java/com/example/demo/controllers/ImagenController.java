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

import com.example.demo.dto.ImagenRequest;
import com.example.demo.dto.ImagenResponse;
import com.example.demo.model.Image;
import com.example.demo.model.Producto;
import com.example.demo.repo.imagenRepo;
import com.example.demo.repo.productoRepo;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Imágenes", description = "Operaciones para gestionar imágenes de productos")
public class ImagenController {

    @Autowired
    imagenRepo repo;

    @Autowired
    productoRepo repoProducto;

    @GetMapping("imagenes")
    @PreAuthorize("permitAll()")
    @SecurityRequirements
    @Operation(summary = "Listar todas las imágenes", description = "Obtiene el listado completo de imágenes registradas")
    public ResponseEntity<List<ImagenResponse>> findAllimagenes() {
        List<ImagenResponse> response = this.repo.findAll().stream()
                .map(ImagenResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }

    @PostMapping("imagen")
    @Operation(summary = "Crear imagen", description = "Registra una nueva imagen (requiere rol USER o ADMIN)")
    public ResponseEntity<ImagenResponse> addimagen(@Valid @RequestBody ImagenRequest request) {
        Image imagen = Image.fromRequest(request);
        Image saved = repo.save(imagen);
        URI location = URI.create("/imagen/" + saved.getId());
        return ResponseEntity.created(location).body(ImagenResponse.from(saved));
    }

    @PostMapping("imagen/{productId}")
    @Operation(summary = "Crear imagen en producto", description = "Asocia una nueva imagen a un producto por su ID (requiere rol USER o ADMIN)")
    public ResponseEntity<ImagenResponse> addimagen(@Valid @RequestBody ImagenRequest request, @PathVariable String productId) {
        Optional<Producto> p = repoProducto.findById(productId);
        if (p.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Image imagen = Image.fromRequest(request);
        imagen.setProducto(p.get());
        Image saved = repo.save(imagen);
        URI location = URI.create("/imagen/" + saved.getId());
        return ResponseEntity.created(location).body(ImagenResponse.from(saved));
    }

    @GetMapping("imagenes/producto/{productId}")
    @PreAuthorize("permitAll()")
    @SecurityRequirements
    @Operation(summary = "Listar imágenes por producto", description = "Obtiene las imágenes asociadas a un producto por su ID")
    public ResponseEntity<Set<ImagenResponse>> getImagensByProduct(@PathVariable String productId) {
        return repoProducto.findById(productId)
                .map(p -> ResponseEntity.ok(
                        p.getImagenes().stream()
                                .map(ImagenResponse::from)
                                .collect(Collectors.toSet())))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("imagen/{id}")
    @PreAuthorize("permitAll()")
    @SecurityRequirements
    @Operation(summary = "Obtener imagen por ID", description = "Busca una imagen específica mediante su identificador")
    public ResponseEntity<ImagenResponse> getById(@PathVariable String id) {
        return repo.findById(id)
                .map(value -> ResponseEntity.ok(ImagenResponse.from(value)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("imagen/{id}")
    @Operation(summary = "Actualizar imagen", description = "Actualiza los datos de una imagen existente (requiere rol USER o ADMIN)")
    public ResponseEntity<ImagenResponse> putusuari(@PathVariable String id, @Valid @RequestBody ImagenRequest request) {
        Optional<Image> existingOpt = repo.findById(id);
        if (existingOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Image existing = existingOpt.get();
        existing.updateFrom(request);
        Image saved = repo.save(existing);
        return ResponseEntity.ok(ImagenResponse.from(saved));
    }

    @DeleteMapping("imagen/{id}")
    @Operation(summary = "Eliminar imagen", description = "Elimina una imagen por su ID (requiere rol USER o ADMIN)")
    public ResponseEntity<Void> deleteMailbox(@PathVariable String id) {
        if (!repo.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }
} 