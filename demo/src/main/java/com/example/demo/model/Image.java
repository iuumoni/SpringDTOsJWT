package com.example.demo.model;

import com.example.demo.dto.ImagenRequest;
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
@Table(name = "imagenes")
@Entity
public class Image {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Setter
    private String id;
    private String url;
    private Short prioridad;
    private String tamanio;
    private boolean responsiva;

    @JsonBackReference
    @ManyToOne(fetch = FetchType.LAZY)
    @Setter
    private Producto producto;

    public static Image fromRequest(ImagenRequest req) {
        return Image.builder()
                .url(req.url())
                .prioridad(req.prioridad() != null ? req.prioridad().shortValue() : null)
                .tamanio(req.tamanio())
                .responsiva(Boolean.TRUE.equals(req.responsiva()))
                .build();
    }

    public void updateFrom(ImagenRequest req) {
        this.url = req.url();
        this.prioridad = req.prioridad() != null ? req.prioridad().shortValue() : null;
        this.tamanio = req.tamanio();
        this.responsiva = Boolean.TRUE.equals(req.responsiva());
    }
} 