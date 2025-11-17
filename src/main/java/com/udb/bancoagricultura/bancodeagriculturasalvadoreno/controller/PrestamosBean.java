/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.udb.bancoagricultura.bancodeagriculturasalvadoreno.controller;

import com.udb.bancoagricultura.bancodeagriculturasalvadoreno.model.pojo.CuentaBancaria;
import com.udb.bancoagricultura.bancodeagriculturasalvadoreno.model.pojo.Empleado;
import com.udb.bancoagricultura.bancodeagriculturasalvadoreno.model.pojo.Prestamo;
import com.udb.bancoagricultura.bancodeagriculturasalvadoreno.model.util.JPAUtil;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 *
 * @author felix
 */
@ManagedBean
@ViewScoped
public class PrestamosBean implements Serializable {

    private int idCuenta;
    private BigDecimal montoSolicitado;
    private Prestamo prestamo = new Prestamo();
    private List<Prestamo> listaPrestamos;

    @PostConstruct
    public void init() {
        cargarPrestamos();
    }

    public void registrarPrestamo() {
        EntityManager em = JPAUtil.getEntityManager();

        try {
            em.getTransaction().begin();

            CuentaBancaria cuenta = em.find(CuentaBancaria.class, idCuenta);
            if (cuenta == null) {
                addMessage("Cuenta no encontrada.", FacesMessage.SEVERITY_ERROR);
                return;
            }

            // Obtener cajero (empleado activo)
            TypedQuery<Empleado> q = em.createQuery(
                "SELECT e FROM Empleado e WHERE e.cargo = 'CAJERO' AND e.estado = 'ACTIVO'", Empleado.class);
            Empleado cajero = q.setMaxResults(1).getSingleResult();

            // Determinar interés y máximo permitido según salario
            BigDecimal salario = cajero.getSalario();
            BigDecimal interes = BigDecimal.ZERO;
            BigDecimal maximo = BigDecimal.ZERO;

            if (salario.compareTo(new BigDecimal(365)) <= 0) {
                interes = new BigDecimal("0.03");
                maximo = new BigDecimal("10000");
            } else if (salario.compareTo(new BigDecimal(600)) <= 0) {
                interes = new BigDecimal("0.03");
                maximo = new BigDecimal("25000");
            } else if (salario.compareTo(new BigDecimal(900)) <= 0) {
                interes = new BigDecimal("0.04");
                maximo = new BigDecimal("35000");
            } else {
                interes = new BigDecimal("0.05");
                maximo = new BigDecimal("50000");
            }

            if (montoSolicitado.compareTo(maximo) > 0) {
                addMessage("El monto excede el límite permitido según el salario.", FacesMessage.SEVERITY_WARN);
                em.getTransaction().rollback();
                return;
            }

            // Calcular plazo y cuota mensual (simple aproximación)
            int plazo = 5; // años
            BigDecimal cuota = montoSolicitado
                    .multiply(interes.add(BigDecimal.ONE))
                    .divide(new BigDecimal(plazo * 12), BigDecimal.ROUND_HALF_UP);

            prestamo.setCuenta(cuenta);
            prestamo.setEmpleado(cajero);
            prestamo.setMonto(montoSolicitado);
            prestamo.setInteres(interes);
            prestamo.setPlazoAnios(plazo);
            prestamo.setCuotaMensual(cuota);
            prestamo.setEstado("EN ESPERA");
            prestamo.setFechaApertura(new Date());

            em.persist(prestamo);
            em.getTransaction().commit();

            addMessage("Préstamo registrado exitosamente.", FacesMessage.SEVERITY_INFO);
            cargarPrestamos();

        } catch (Exception e) {
            em.getTransaction().rollback();
            addMessage("Error al registrar el préstamo: " + e.getMessage(), FacesMessage.SEVERITY_ERROR);
        } finally {
            em.close();
        }
    }

    public void cargarPrestamos() {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            listaPrestamos = em.createQuery("SELECT p FROM Prestamo p", Prestamo.class).getResultList();
        } finally {
            em.close();
        }
    }

    private void addMessage(String msg, FacesMessage.Severity severity) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(severity, msg, null));
    }

    // Getters y Setters
    public int getIdCuenta() { return idCuenta; }
    public void setIdCuenta(int idCuenta) { this.idCuenta = idCuenta; }

    public BigDecimal getMontoSolicitado() { return montoSolicitado; }
    public void setMontoSolicitado(BigDecimal montoSolicitado) { this.montoSolicitado = montoSolicitado; }

    public List<Prestamo> getListaPrestamos() { return listaPrestamos; }
}
