package com.example.demo.dto;

import com.example.demo.model.Comment;

public record ComentarioResponse(
        String id,
        String username,
        String comentario_corto,
        Short estrellas,
        String comentario_largo,
        String imagen1,
        String imagen2) {

    public static ComentarioResponse from(Comment comment) {
        return new ComentarioResponse(
                comment.getId(),
                comment.getUsername(),
                comment.getComentario_corto(),
                comment.getEstrellas(),
                comment.getComentario_largo(),
                comment.getImagen1(),
                comment.getImagen2());
    }
} 