package com.udb.bancoagricultura.bancodeagriculturasalvadoreno.model.pojo;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "acciones_personal", schema = "banco_agricultura")
public class AccionPersonal implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_accion")
    private int idAccion;

    // Relación: La acción es SOBRE un empleado
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_empleado", nullable = false)
    private Empleado empleado;

    // Guardamos solo el ID del gerente, como está en tu BD
    @Column(name = "id_gerente_sucursal", nullable = false)
    private int idGerenteSucursal;

    @Column(name = "tipo_accion", nullable = false)
    private String tipoAccion; // CONTRATACION, BAJA

    @Column(name = "motivo")
    private String motivo;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "fecha_solicitud")
    private Date fechaSolicitud; // Nombre de columna en BD

    @Column(name = "estado")
    private String estado; // PENDIENTE, APROBADA, RECHAZADA

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "fecha_resolucion")
    private Date fechaResolucion; // Nombre de columna en BD

    // --- Getters y Setters ---

    public int getIdAccion() {
        return idAccion;
    }

    public void setIdAccion(int idAccion) {
        this.idAccion = idAccion;
    }

    public Empleado getEmpleado() {
        return empleado;
    }

    public void setEmpleado(Empleado empleado) {
        this.empleado = empleado;
    }

    public int getIdGerenteSucursal() {
        return idGerenteSucursal;
    }

    public void setIdGerenteSucursal(int idGerenteSucursal) {
        this.idGerenteSucursal = idGerenteSucursal;
    }

    public String getTipoAccion() {
        return tipoAccion;
    }

    public void setTipoAccion(String tipoAccion) {
        this.tipoAccion = tipoAccion;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public Date getFechaSolicitud() {
        return fechaSolicitud;
    }

    public void setFechaSolicitud(Date fechaSolicitud) {
        this.fechaSolicitud = fechaSolicitud;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public Date getFechaResolucion() {
        return fechaResolucion;
    }

    public void setFechaResolucion(Date fechaResolucion) {
        this.fechaResolucion = fechaResolucion;
    }
}