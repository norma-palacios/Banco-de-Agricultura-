package com.udb.bancoagricultura.bancodeagriculturasalvadoreno.controller;

import com.udb.bancoagricultura.bancodeagriculturasalvadoreno.model.pojo.CuentaBancaria;
import com.udb.bancoagricultura.bancodeagriculturasalvadoreno.model.pojo.Movimiento;
import com.udb.bancoagricultura.bancodeagriculturasalvadoreno.model.util.JPAUtil;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean; // Usamos @ManagedBean esta vez
import javax.faces.bean.ViewScoped;  // @ViewScoped para que viva mientras estemos en la página
import javax.faces.context.FacesContext;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

@ManagedBean
@ViewScoped
public class MovimientosBean implements Serializable {

    private String idCuentaParam;
    private CuentaBancaria cuenta;
    private List<Movimiento> listaMovimientos;


    @PostConstruct
    public void init() {

        Map<String, String> params = FacesContext.getCurrentInstance()
                .getExternalContext().getRequestParameterMap();
        idCuentaParam = params.get("id");

        if (idCuentaParam != null && !idCuentaParam.isEmpty()) {
            cargarMovimientos();
        }
    }

    public void cargarMovimientos() {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            int id = Integer.parseInt(idCuentaParam);


            this.cuenta = em.find(CuentaBancaria.class, id);




            TypedQuery<Movimiento> query = em.createQuery(
                    "SELECT m FROM Movimiento m " +
                            "WHERE m.cuentaOrigen.idCuenta = :id OR m.cuentaDestino.idCuenta = :id " +
                            "ORDER BY m.fechaMovimiento DESC",
                    Movimiento.class
            );
            query.setParameter("id", id);
            this.listaMovimientos = query.getResultList();

        } catch (Exception e) {
            e.printStackTrace();

        } finally {
            em.close();
        }
    }



    public CuentaBancaria getCuenta() {
        return cuenta;
    }

    public void setCuenta(CuentaBancaria cuenta) {
        this.cuenta = cuenta;
    }

    public List<Movimiento> getListaMovimientos() {
        return listaMovimientos;
    }

    public void setListaMovimientos(List<Movimiento> listaMovimientos) {
        this.listaMovimientos = listaMovimientos;
    }

    public String getIdCuentaParam() {
        return idCuentaParam;
    }

    public void setIdCuentaParam(String idCuentaParam) {
        this.idCuentaParam = idCuentaParam;
    }
}