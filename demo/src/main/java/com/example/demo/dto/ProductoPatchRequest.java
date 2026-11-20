package com.example.demo.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ProductoPatchRequest(
        @Size(min = 1, max = 255) String nombre,
        @Size(max = 1000) String descripcion,
        @Size(max = 100) String lote,
        @PositiveOrZero Float precioUnitario,
        LocalDate fechaCaducidad) {

    public boolean isEmpty() {
        return nombre == null
                && descripcion == null
                && lote == null
                && precioUnitario == null
                && fechaCaducidad == null;
    }
} 