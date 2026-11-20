package com.example.demo.dto;

import com.example.demo.model.Department;

public record CategoriaResponse(
        String id,
        String nombre,
        String descripcion,
        String tipo,
        String icono,
        String imagenNavbar) {

    public static CategoriaResponse from(Department department) {
        return new CategoriaResponse(
                department.getId(),
                department.getNombre(),
                department.getDescripcion(),
                department.getTipo(),
                department.getIcono(),
                department.getImagenNavbar());
    }
} 