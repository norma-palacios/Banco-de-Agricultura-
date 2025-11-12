package com.udb.bancoagricultura.bancodeagriculturasalvadoreno.model.pojo;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "acciones_personal", schema = "banco_agricultura")
public class AccionPersonal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_accion")
    private Integer idAccion;

    @ManyToOne
    @JoinColumn(name = "id_empleado")
    private Empleado empleado;

    @Column(name = "id_gerente_sucursal")
    private Integer idGerenteSucursal;
    private String motivo;

    @Column(name = "fecha_solicitud")
    private Date fechaCreacion;

    private String estado;

    @Column(name = "fecha_resolucion")
    private String fechaResolucion;

    @Column(name = "tipo_accion")
    private String tipoAccion;

    public Integer getIdAccion() {
        return idAccion;
    }

    public void setIdAccion(Integer idAccion) {
        this.idAccion = idAccion;
    }

    public Empleado getEmpleado() {
        return empleado;
    }

    public void setEmpleado(Empleado empleado) {
        this.empleado = empleado;
    }

    public Integer getIdGerenteSucursal() {
        return idGerenteSucursal;
    }

    public void setIdGerenteSucursal(Integer idGerenteSucursal) {
        this.idGerenteSucursal = idGerenteSucursal;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public Date getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(Date fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getFechaResolucion() {
        return fechaResolucion;
    }

    public void setFechaResolucion(String fechaResolucion) {
        this.fechaResolucion = fechaResolucion;
    }

    public String getTipoAccion() {
        return tipoAccion;
    }

    public void setTipoAccion(String tipoAccion) {
        this.tipoAccion = tipoAccion;
    }
}