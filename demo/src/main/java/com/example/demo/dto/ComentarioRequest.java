package com.example.demo.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ComentarioRequest(
        @NotBlank String username,
        @NotBlank String comentario_corto,
        @NotNull @Min(1) @Max(5) Integer estrellas,
        String comentario_largo,
        String imagen1,
        String imagen2) {
} 