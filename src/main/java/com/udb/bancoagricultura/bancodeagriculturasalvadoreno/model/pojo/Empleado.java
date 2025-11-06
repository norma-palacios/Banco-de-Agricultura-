package com.udb.bancoagricultura.bancodeagriculturasalvadoreno.model.pojo;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "empleados", schema = "banco_agricultura")
public class Empleado implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_empleado")
    private int idEmpleado;

    // Relación: Un empleado es una Persona
    @OneToOne
    @JoinColumn(name = "id_persona", nullable = false)
    private Persona persona;

    // Relación: Un empleado pertenece a una Sucursal
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_sucursal", nullable = false)
    private Sucursal sucursal;

    @Column(name = "cargo", nullable = false)
    private String cargo; // e.g., 'GERENTE_SUCURSAL'

    @Column(name = "salario", nullable = false)
    private BigDecimal salario;

    @Temporal(TemporalType.DATE)
    @Column(name = "fecha_contratacion", nullable = false)
    private Date fechaContratacion;

    @Column(name = "estado")
    private String estado;

    // --- Getters y Setters ---

    public int getIdEmpleado() { return idEmpleado; }
    public void setIdEmpleado(int idEmpleado) { this.idEmpleado = idEmpleado; }

    public Persona getPersona() { return persona; }
    public void setPersona(Persona persona) { this.persona = persona; }

    public Sucursal getSucursal() { return sucursal; }
    public void setSucursal(Sucursal sucursal) { this.sucursal = sucursal; }

    public String getCargo() { return cargo; }
    public void setCargo(String cargo) { this.cargo = cargo; }

    public BigDecimal getSalario() { return salario; }
    public void setSalario(BigDecimal salario) { this.salario = salario; }

    public Date getFechaContratacion() { return fechaContratacion; }
    public void setFechaContratacion(Date fechaContratacion) { this.fechaContratacion = fechaContratacion; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}