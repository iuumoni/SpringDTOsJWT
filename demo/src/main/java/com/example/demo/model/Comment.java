package com.example.demo.model;

import com.example.demo.dto.ComentarioRequest;
import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "comentarios")
@Entity
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Setter
    private String id;
    private String username;
    private String comentario_corto;
    private Short estrellas;
    private String comentario_largo;
    private String imagen1;
    private String imagen2;

    @JsonBackReference
    @ManyToOne(fetch = FetchType.LAZY)
    @Setter
    private Producto producto;

    public static Comment fromRequest(ComentarioRequest req) {
        return Comment.builder()
                .username(req.username())
                .comentario_corto(req.comentario_corto())
                .estrellas(req.estrellas() != null ? req.estrellas().shortValue() : null)
                .comentario_largo(req.comentario_largo())
                .imagen1(req.imagen1())
                .imagen2(req.imagen2())
                .build();
    }

    public void updateFrom(ComentarioRequest req) {
        this.username = req.username();
        this.comentario_corto = req.comentario_corto();
        this.estrellas = req.estrellas() != null ? req.estrellas().shortValue() : null;
        this.comentario_largo = req.comentario_largo();
        this.imagen1 = req.imagen1();
        this.imagen2 = req.imagen2();
    }
} 