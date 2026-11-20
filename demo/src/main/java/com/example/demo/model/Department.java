package com.example.demo.model;

import java.util.HashSet;
import java.util.Set;

import com.example.demo.dto.CategoriaRequest;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
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
@Table(name = "Categorias")
@Entity
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Setter
    private String id;
    private String nombre;
    private String descripcion;
    private String tipo;
    private String icono;
    private String imagenNavbar;

    @Builder.Default
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "producto_deparments",
            joinColumns = @JoinColumn(name = "product_id"),
            inverseJoinColumns = @JoinColumn(name = "deparments_id")
    )
    @Setter
    private Set<Producto> productos = new HashSet<>();

    public static Department fromRequest(CategoriaRequest req) {
        return Department.builder()
                .nombre(req.nombre())
                .descripcion(req.descripcion())
                .tipo(req.tipo())
                .icono(req.icono())
                .imagenNavbar(req.imagenNavbar())
                .build();
    }

    public void updateFrom(CategoriaRequest req) {
        this.nombre = req.nombre();
        this.descripcion = req.descripcion();
        this.tipo = req.tipo();
        this.icono = req.icono();
        this.imagenNavbar = req.imagenNavbar();
    }
} 