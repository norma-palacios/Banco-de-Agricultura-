package com.udb.bancoagricultura.bancodeagriculturasalvadoreno.controller;

import com.udb.bancoagricultura.bancodeagriculturasalvadoreno.model.pojo.CuentaBancaria;
import com.udb.bancoagricultura.bancodeagriculturasalvadoreno.model.pojo.Movimiento;
import com.udb.bancoagricultura.bancodeagriculturasalvadoreno.model.pojo.TipoMovimiento;
import com.udb.bancoagricultura.bancodeagriculturasalvadoreno.model.util.JPAUtil;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@ManagedBean(name = "movimientosBean") // nombre explícito para EL
@ViewScoped
public class MovimientosBean implements Serializable {

    private static final long serialVersionUID = 1L;

    // --- Campos expuestos a la vista ---
    private String idCuentaParam;            // mantener como String (Opción B)
    private Double monto;                    // usar Double (no double) para evitar NPE en conversión
    private int tipoMovimientoId;            // sigue existiendo pero NO se usa en esta pantalla
    private List<Movimiento> listaMovimientos;
    private List<TipoMovimiento> listaTipos; // puede existir aunque no se muestre
    private String mensaje;

    @PostConstruct
    public void init() {
        // Cargar catálogo si existe, no es obligatorio para esta pantalla
        EntityManager em = JPAUtil.getEntityManager();
        try {
            listaTipos = em.createQuery("FROM TipoMovimiento", TipoMovimiento.class).getResultList();
        } catch (Exception ignored) {
        } finally {
            em.close();
        }
    }

    // ------------------ Acciones expuestas ------------------

    public void cargarMovimientos() {
        if (!isNumeric(idCuentaParam)) {
            addMessage("Ingrese un ID de cuenta válido (solo números).", FacesMessage.SEVERITY_ERROR);
            return;
        }
        Integer idCuenta = Integer.valueOf(idCuentaParam.trim());

        EntityManager em = JPAUtil.getEntityManager();
        try {
            TypedQuery<Movimiento> query = em.createQuery(
                "SELECT m FROM Movimiento m " +
                "WHERE (m.cuentaOrigen.idCuenta = :id OR m.cuentaDestino.idCuenta = :id) " +
                "ORDER BY m.fechaMovimiento DESC",
                Movimiento.class
            );
            query.setParameter("id", idCuenta);
            listaMovimientos = query.getResultList();
            addMessage("Movimientos cargados.", FacesMessage.SEVERITY_INFO);
        } catch (Exception e) {
            addMessage("Error al cargar movimientos: " + e.getMessage(), FacesMessage.SEVERITY_ERROR);
        } finally {
            em.close();
        }
    }

    public void registrarDeposito() {
        registrarMovimiento("Depósito");
    }

    public void registrarRetiro() {
        registrarMovimiento("Retiro");
    }

    // ------------------ Lógica interna ------------------

    private void registrarMovimiento(String tipo) {
        // Validaciones de entrada (Opción B)
        if (!isNumeric(idCuentaParam)) {
            addMessage("Ingrese un ID de cuenta válido (solo números).", FacesMessage.SEVERITY_ERROR);
            return;
        }
        if (monto == null || monto <= 0) {
            addMessage("Ingrese un monto válido mayor a 0.", FacesMessage.SEVERITY_ERROR);
            return;
        }

        Integer idCuenta = Integer.valueOf(idCuentaParam.trim());

        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();

            CuentaBancaria cuenta = em.find(CuentaBancaria.class, idCuenta);
            if (cuenta == null) {
                addMessage("Cuenta no encontrada.", FacesMessage.SEVERITY_ERROR);
                em.getTransaction().rollback();
                return;
            }

            // Buscar TipoMovimiento por nombre (si no existe, avisar claramente)
            TipoMovimiento tipoMov;
            try {
                tipoMov = em.createQuery(
                        "FROM TipoMovimiento t WHERE t.nombre = :nombre", TipoMovimiento.class)
                        .setParameter("nombre", tipo)
                        .getSingleResult();
            } catch (NoResultException nre) {
                addMessage("Tipo de movimiento \"" + tipo + "\" no existe en la base. " +
                           "Inserte 'Depósito' y 'Retiro' en tipos_movimiento.", FacesMessage.SEVERITY_ERROR);
                em.getTransaction().rollback();
                return;
            }

            BigDecimal montoBD = BigDecimal.valueOf(monto);
            BigDecimal saldoActual = cuenta.getSaldo() == null ? BigDecimal.ZERO : cuenta.getSaldo();

            // Crear movimiento
            Movimiento mov = new Movimiento();
            mov.setCuentaOrigen(cuenta);
            mov.setTipoMovimiento(tipoMov);
            mov.setMonto(montoBD);
            mov.setFechaMovimiento(new Date());

            // Ajuste de saldo
            if ("Depósito".equalsIgnoreCase(tipoMov.getNombre())) {
                cuenta.setSaldo(saldoActual.add(montoBD));
            } else { // Retiro
                if (saldoActual.compareTo(montoBD) < 0) {
                    addMessage("Fondos insuficientes.", FacesMessage.SEVERITY_WARN);
                    em.getTransaction().rollback();
                    return;
                }
                cuenta.setSaldo(saldoActual.subtract(montoBD));
            }

            em.persist(mov);
            em.merge(cuenta);
            em.getTransaction().commit();

            addMessage("Movimiento registrado correctamente.", FacesMessage.SEVERITY_INFO);
            cargarMovimientos(); // refrescar tabla

            // limpiar monto para evitar reenvíos accidentales
            this.monto = null;

        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            addMessage("Error al registrar el movimiento: " + e.getMessage(), FacesMessage.SEVERITY_ERROR);
        } finally {
            em.close();
        }
    }

    private boolean isNumeric(String s) {
        return s != null && s.trim().matches("\\d+");
    }

    private void addMessage(String resumen, FacesMessage.Severity tipo) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(tipo, resumen, null));
        this.mensaje = resumen;
    }

    // ------------------ Getters/Setters ------------------

    public String getIdCuentaParam() { return idCuentaParam; }
    public void setIdCuentaParam(String idCuentaParam) { this.idCuentaParam = idCuentaParam; }

    public Double getMonto() { return monto; }            // usar wrapper
    public void setMonto(Double monto) { this.monto = monto; }

    public int getTipoMovimientoId() { return tipoMovimientoId; }
    public void setTipoMovimientoId(int tipoMovimientoId) { this.tipoMovimientoId = tipoMovimientoId; }

    public List<Movimiento> getListaMovimientos() { return listaMovimientos; }
    public void setListaMovimientos(List<Movimiento> listaMovimientos) { this.listaMovimientos = listaMovimientos; }

    public List<TipoMovimiento> getListaTipos() { return listaTipos; }
    public void setListaTipos(List<TipoMovimiento> listaTipos) { this.listaTipos = listaTipos; }

    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }
}