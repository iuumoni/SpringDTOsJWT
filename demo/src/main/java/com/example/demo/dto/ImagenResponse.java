package com.example.demo.dto;

import com.example.demo.model.Image;

public record ImagenResponse(
        String id,
        String url,
        Short prioridad,
        String tamanio,
        boolean responsiva) {

    public static ImagenResponse from(Image image) {
        return new ImagenResponse(
                image.getId(),
                image.getUrl(),
                image.getPrioridad(),
                image.getTamanio(),
                image.isResponsiva());
    }
} 