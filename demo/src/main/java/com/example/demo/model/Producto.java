package com.example.demo.model;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import com.example.demo.dto.ProductoPatchRequest;
import com.example.demo.dto.ProductoRequest;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
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
@Table(name = "producto")
@Entity
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Setter
    private String id;

    private String nombre;

    private String descripcion;
    private String lote;
    private float precioUnitario;
    private LocalDate fechaCaducidad;

    @Builder.Default
    @JsonManagedReference
    @OneToMany(mappedBy = "producto", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private Set<Comment> comentarios = new HashSet<>();

    @Builder.Default
    @JsonManagedReference
    @OneToMany(mappedBy = "producto", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @Setter
    private Set<Image> imagenes = new HashSet<>();

    @Builder.Default
    @JsonBackReference
    @ManyToMany(mappedBy = "productos", fetch = FetchType.EAGER)
    @Setter
    private Set<Department> deparments = new HashSet<>();

    public static Producto fromRequest(ProductoRequest req) {
        return Producto.builder()
                .nombre(req.nombre())
                .descripcion(req.descripcion())
                .lote(req.lote())
                .precioUnitario(req.precioUnitario())
                .fechaCaducidad(req.fechaCaducidad())
                .build();
    }

    public void updateFrom(ProductoRequest req) {
        this.nombre = req.nombre();
        this.descripcion = req.descripcion();
        this.lote = req.lote();
        this.precioUnitario = req.precioUnitario();
        this.fechaCaducidad = req.fechaCaducidad();
    }

    public void patchFrom(ProductoPatchRequest patch) {
        if (patch.nombre() != null) {
            this.nombre = patch.nombre();
        }
        if (patch.descripcion() != null) {
            this.descripcion = patch.descripcion();
        }
        if (patch.lote() != null) {
            this.lote = patch.lote();
        }
        if (patch.precioUnitario() != null) {
            this.precioUnitario = patch.precioUnitario();
        }
        if (patch.fechaCaducidad() != null) {
            this.fechaCaducidad = patch.fechaCaducidad();
        }
    }
} 