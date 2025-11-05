package com.udb.bancoagricultura.bancodeagriculturasalvadoreno.controller;

import com.udb.bancoagricultura.bancodeagriculturasalvadoreno.model.pojo.CuentaBancaria;
import com.udb.bancoagricultura.bancodeagriculturasalvadoreno.model.pojo.Usuario;
import com.udb.bancoagricultura.bancodeagriculturasalvadoreno.model.util.JPAUtil;

import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Random;

@Named
@SessionScoped
public class CuentasBean implements Serializable {

    @Inject
    private LoginBean loginBean;

    private List<CuentaBancaria> listaCuentas;
    private String tipoCuentaNueva;

    @PostConstruct
    public void init() {
        cargarCuentas();
    }

    public void cargarCuentas() {
        if (loginBean.getUsuarioLogeado() == null) {
            return;
        }

        EntityManager em = JPAUtil.getEntityManager();
        try {
            TypedQuery<CuentaBancaria> query = em.createQuery(
                    "SELECT c FROM CuentaBancaria c WHERE c.cliente.idUsuario = :idUsuario",
                    CuentaBancaria.class
            );
            query.setParameter("idUsuario", loginBean.getUsuarioLogeado().getIdUsuario());
            this.listaCuentas = query.getResultList();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            em.close();
        }
    }

    public String crearCuenta() {
        FacesContext context = FacesContext.getCurrentInstance();

        // REQUISITO: Máximo 3 cuentas
        if (listaCuentas.size() >= 3) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, "Límite alcanzado", "Ya tienes el máximo de 3 cuentas permitidas."));
            return null;
        }

        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();

            CuentaBancaria nuevaCuenta = new CuentaBancaria();

            // REQUISITO: ID aleatorio
            Random rand = new Random();
            long numero = 1000000000L + (long)(rand.nextDouble() * 9000000000L);
            nuevaCuenta.setNumeroCuenta(String.valueOf(numero));

            nuevaCuenta.setTipoCuenta(tipoCuentaNueva);
            nuevaCuenta.setSaldo(BigDecimal.ZERO);
            nuevaCuenta.setEstado("ACTIVA");


            nuevaCuenta.setFechaApertura(new Date());


            Usuario usuarioAdjunto = em.merge(loginBean.getUsuarioLogeado());
            nuevaCuenta.setCliente(usuarioAdjunto);

            em.persist(nuevaCuenta);
            em.getTransaction().commit();


            cargarCuentas();

            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "¡Éxito!", "Nueva cuenta de " + tipoCuentaNueva + " creada."));

        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "No se pudo crear la cuenta."));
            e.printStackTrace();
        } finally {
            em.close();
        }

        return null;
    }

    // --- Getters y Setters ---
    public List<CuentaBancaria> getListaCuentas() {
        return listaCuentas;
    }

    public void setListaCuentas(List<CuentaBancaria> listaCuentas) {
        this.listaCuentas = listaCuentas;
    }

    public String getTipoCuentaNueva() {
        return tipoCuentaNueva;
    }

    public void setTipoCuentaNueva(String tipoCuentaNueva) {
        this.tipoCuentaNueva = tipoCuentaNueva;
    }
}