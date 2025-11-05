package com.udb.bancoagricultura.bancodeagriculturasalvadoreno.model.pojo;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "tipos_movimiento", schema = "banco_agricultura")
public class TipoMovimiento implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tipo_movimiento")
    private int idTipoMovimiento;

    @Column(name = "nombre", nullable = false, length = 50)
    private String nombre;

    @Column(name = "descripcion", length = 200)
    private String descripcion;

    // --- Getters y Setters ---
    public int getIdTipoMovimiento() { return idTipoMovimiento; }
    public void setIdTipoMovimiento(int idTipoMovimiento) { this.idTipoMovimiento = idTipoMovimiento; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
}