package com.udb.bancoagricultura.bancodeagriculturasalvadoreno.model.pojo;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "movimientos", schema = "banco_agricultura")
public class Movimiento implements Serializable {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_movimiento")
    private int idMovimiento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cuenta_origen")
    private CuentaBancaria cuentaOrigen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cuenta_destino")
    private CuentaBancaria cuentaDestino;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_tipo_movimiento", nullable = false)
    private TipoMovimiento tipoMovimiento;

    @Column(name = "monto", nullable = false)
    private BigDecimal monto;

    @Column(name = "descripcion", length = 200)
    private String descripcion;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "fecha_movimiento")
    private Date fechaMovimiento;


    // ...
    public int getIdMovimiento() { return idMovimiento; }
    public void setIdMovimiento(int idMovimiento) { this.idMovimiento = idMovimiento; }
    public CuentaBancaria getCuentaOrigen() { return cuentaOrigen; }
    public void setCuentaOrigen(CuentaBancaria cuentaOrigen) { this.cuentaOrigen = cuentaOrigen; }
    public CuentaBancaria getCuentaDestino() { return cuentaDestino; }
    public void setCuentaDestino(CuentaBancaria cuentaDestino) { this.cuentaDestino = cuentaDestino; }
    public TipoMovimiento getTipoMovimiento() { return tipoMovimiento; }
    public void setTipoMovimiento(TipoMovimiento tipoMovimiento) { this.tipoMovimiento = tipoMovimiento; }
    public BigDecimal getMonto() { return monto; }
    public void setMonto(BigDecimal monto) { this.monto = monto; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public Date getFechaMovimiento() { return fechaMovimiento; }
    public void setFechaMovimiento(Date fechaMovimiento) { this.fechaMovimiento = fechaMovimiento; }
}