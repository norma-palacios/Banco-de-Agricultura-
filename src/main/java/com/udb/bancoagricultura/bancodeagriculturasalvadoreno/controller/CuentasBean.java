package com.udb.bancoagricultura.bancodeagriculturasalvadoreno.controller;

// ... (Importaciones existentes, incluyendo java.util.ArrayList)
import com.udb.bancoagricultura.bancodeagriculturasalvadoreno.model.pojo.CuentaBancaria;
import com.udb.bancoagricultura.bancodeagriculturasalvadoreno.model.pojo.Movimiento;
import com.udb.bancoagricultura.bancodeagriculturasalvadoreno.model.pojo.TipoMovimiento;
import com.udb.bancoagricultura.bancodeagriculturasalvadoreno.model.pojo.Usuario;
import com.udb.bancoagricultura.bancodeagriculturasalvadoreno.model.util.JPAUtil;
import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

@Named
@SessionScoped
public class CuentasBean implements Serializable {

    @Inject
    private LoginBean loginBean;

    // Las inicializaciones son correctas
    private List<CuentaBancaria> listaCuentas = new ArrayList<>();
    private String tipoCuentaNueva;
    private int idCuentaOrigen;
    private String numeroCuentaDestino;
    private BigDecimal montoTransferencia;
    private String descripcionTransferencia;
    private List<Movimiento> listaMovimientosRecientes = new ArrayList<>();
    private String vistaActual = "inicio";

    @PostConstruct
    public void init() {
        cargarCuentas();
        cargarMovimientosRecientes();
    }

    public void cargarCuentas() {
        if (loginBean.getUsuarioLogeado() == null) {
            listaCuentas = new ArrayList<>();
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
            this.listaCuentas = new ArrayList<>();
        } finally {
            em.close();
        }
    }

    // --- MÉTODO CORREGIDO ---
    public void cargarMovimientosRecientes() {
        if (loginBean.getUsuarioLogeado() == null) {
            listaMovimientosRecientes = new ArrayList<>();
            return;
        }
        EntityManager em = JPAUtil.getEntityManager();
        try {
            // Paso 1: Obtener los IDs de las cuentas del usuario
            TypedQuery<Integer> idQuery = em.createQuery(
                    "SELECT c.idCuenta FROM CuentaBancaria c WHERE c.cliente.idUsuario = :idUsuario", Integer.class);
            idQuery.setParameter("idUsuario", loginBean.getUsuarioLogeado().getIdUsuario());
            List<Integer> idsDeMisCuentas = idQuery.getResultList();

            if (idsDeMisCuentas.isEmpty()) {
                this.listaMovimientosRecientes = new ArrayList<>();
                return;
            }

            // --- 2. CONSULTA CORREGIDA (AHORA ES SEGURA) ---
            TypedQuery<Movimiento> query = em.createQuery(
                    "SELECT m FROM Movimiento m " +
                            "WHERE (m.cuentaOrigen IS NOT NULL AND m.cuentaOrigen.idCuenta IN :listaIds) " + // Comprueba IS NOT NULL
                            "OR (m.cuentaDestino IS NOT NULL AND m.cuentaDestino.idCuenta IN :listaIds) " + // Comprueba IS NOT NULL
                            "ORDER BY m.fechaMovimiento DESC",
                    Movimiento.class
            );
            query.setParameter("listaIds", idsDeMisCuentas);
            query.setMaxResults(5);
            this.listaMovimientosRecientes = query.getResultList();

        } catch (Exception e) {
            e.printStackTrace();
            this.listaMovimientosRecientes = new ArrayList<>();
        } finally {
            em.close();
        }
    }


    public void cambiarVista(String nuevaVista) {
        this.vistaActual = nuevaVista;
        if (nuevaVista.equals("inicio")) {
            cargarCuentas();
            cargarMovimientosRecientes();
        }
        if (nuevaVista.equals("cuentas")) {
            cargarCuentas();
        }
        if (nuevaVista.equals("movimientos")) {
            cargarTodosMovimientos();
        }
    }

    // --- MÉTODO CORREGIDO ---
    public void cargarTodosMovimientos() {
        if (loginBean.getUsuarioLogeado() == null) {
            listaMovimientosRecientes = new ArrayList<>();
            return;
        }
        EntityManager em = JPAUtil.getEntityManager();
        try {
            // Paso 1: Obtener los IDs
            TypedQuery<Integer> idQuery = em.createQuery(
                    "SELECT c.idCuenta FROM CuentaBancaria c WHERE c.cliente.idUsuario = :idUsuario", Integer.class);
            idQuery.setParameter("idUsuario", loginBean.getUsuarioLogeado().getIdUsuario());
            List<Integer> idsDeMisCuentas = idQuery.getResultList();

            if (idsDeMisCuentas.isEmpty()) {
                this.listaMovimientosRecientes = new ArrayList<>();
                return;
            }

            // --- 2. CONSULTA CORREGIDA (AHORA ES SEGURA) ---
            TypedQuery<Movimiento> query = em.createQuery(
                    "SELECT m FROM Movimiento m " +
                            "WHERE (m.cuentaOrigen IS NOT NULL AND m.cuentaOrigen.idCuenta IN :listaIds) " + // Comprueba IS NOT NULL
                            "OR (m.cuentaDestino IS NOT NULL AND m.cuentaDestino.idCuenta IN :listaIds) " + // Comprueba IS NOT NULL
                            "ORDER BY m.fechaMovimiento DESC",
                    Movimiento.class
            );
            query.setParameter("listaIds", idsDeMisCuentas);
            // Sin MaxResults
            this.listaMovimientosRecientes = query.getResultList();

        } catch (Exception e) {
            e.printStackTrace();
            this.listaMovimientosRecientes = new ArrayList<>();
        } finally {
            em.close();
        }
    }

    // ... (El resto de tus métodos: crearCuenta, realizarTransferencia, y Getters/Setters no cambian) ...

    // ... (crearCuenta) ...
    public String crearCuenta() {
        FacesContext context = FacesContext.getCurrentInstance();
        if (listaCuentas.size() >= 3) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, "Límite alcanzado", "Ya tienes el máximo de 3 cuentas permitidas."));
            return null;
        }
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            CuentaBancaria nuevaCuenta = new CuentaBancaria();
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

    // ... (realizarTransferencia) ...
    public String realizarTransferencia() {
        FacesContext context = FacesContext.getCurrentInstance();
        EntityManager em = JPAUtil.getEntityManager();
        try {
            if (montoTransferencia == null || montoTransferencia.compareTo(BigDecimal.ZERO) <= 0) {
                context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "El monto debe ser mayor a cero."));
                return null;
            }
            em.getTransaction().begin();
            CuentaBancaria cuentaOrigen = em.find(CuentaBancaria.class, idCuentaOrigen);
            if (cuentaOrigen == null) {
                context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Cuenta de origen no encontrada."));
                em.getTransaction().rollback();
                return null;
            }
            TypedQuery<CuentaBancaria> queryDestino = em.createQuery("SELECT c FROM CuentaBancaria c WHERE c.numeroCuenta = :numCuenta AND c.estado = 'ACTIVA'", CuentaBancaria.class);
            queryDestino.setParameter("numCuenta", numeroCuentaDestino);
            CuentaBancaria cuentaDestino = queryDestino.getSingleResult();
            if (cuentaOrigen.getIdCuenta() == cuentaDestino.getIdCuenta()) {
                context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "No puedes transferir a la misma cuenta."));
                em.getTransaction().rollback();
                return null;
            }
            if (cuentaOrigen.getSaldo().compareTo(montoTransferencia) < 0) {
                context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Fondos insuficientes en la cuenta de origen."));
                em.getTransaction().rollback();
                return null;
            }
            cuentaOrigen.setSaldo(cuentaOrigen.getSaldo().subtract(montoTransferencia));
            cuentaDestino.setSaldo(cuentaDestino.getSaldo().add(montoTransferencia));
            TipoMovimiento tipoTransferencia;
            try {
                TypedQuery<TipoMovimiento> queryTipo = em.createQuery("SELECT t FROM TipoMovimiento t WHERE t.nombre = 'TRANSFERENCIA'", TipoMovimiento.class);
                tipoTransferencia = queryTipo.getSingleResult();
            } catch (NoResultException e) {
                context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error de Configuración", "No se encontró el tipo de movimiento 'TRANSFERENCIA' en la BD."));
                em.getTransaction().rollback();
                return null;
            }
            Movimiento movSalida = new Movimiento();
            movSalida.setCuentaOrigen(cuentaOrigen);
            movSalida.setTipoMovimiento(tipoTransferencia);
            movSalida.setMonto(montoTransferencia);
            movSalida.setDescripcion("Transferencia a " + cuentaDestino.getNumeroCuenta() + ": " + descripcionTransferencia);
            movSalida.setFechaMovimiento(new Date());
            Movimiento movEntrada = new Movimiento();
            movEntrada.setCuentaDestino(cuentaDestino);
            movEntrada.setTipoMovimiento(tipoTransferencia);
            movEntrada.setMonto(montoTransferencia);
            movEntrada.setDescripcion("Transferencia de " + cuentaOrigen.getNumeroCuenta() + ": " + descripcionTransferencia);
            movEntrada.setFechaMovimiento(new Date());
            em.persist(movSalida);
            em.persist(movEntrada);
            em.merge(cuentaOrigen);
            em.merge(cuentaDestino);
            em.getTransaction().commit();
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "¡Éxito!", "Transferencia realizada correctamente."));
            cargarCuentas();
            cargarMovimientosRecientes();
            this.numeroCuentaDestino = null;
            this.montoTransferencia = null;
            this.descripcionTransferencia = null;
        } catch (NoResultException e) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "La cuenta de destino no existe o está inactiva."));
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
        } catch (Exception e) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Ocurrió un error inesperado. " + e.getMessage()));
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            e.printStackTrace();
        } finally {
            em.close();
        }
        return null;
    }

    // ... (Getters y Setters) ...
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
    public int getIdCuentaOrigen() {
        return idCuentaOrigen;
    }
    public void setIdCuentaOrigen(int idCuentaOrigen) {
        this.idCuentaOrigen = idCuentaOrigen;
    }
    public String getNumeroCuentaDestino() {
        return numeroCuentaDestino;
    }
    public void setNumeroCuentaDestino(String numeroCuentaDestino) {
        this.numeroCuentaDestino = numeroCuentaDestino;
    }
    public BigDecimal getMontoTransferencia() {
        return montoTransferencia;
    }
    public void setMontoTransferencia(BigDecimal montoTransferencia) {
        this.montoTransferencia = montoTransferencia;
    }
    public String getDescripcionTransferencia() {
        return descripcionTransferencia;
    }
    public void setDescripcionTransferencia(String descripcionTransferencia) {
        this.descripcionTransferencia = descripcionTransferencia;
    }
    public String getVistaActual() {
        return vistaActual;
    }
    public void setVistaActual(String vistaActual) {
        this.vistaActual = vistaActual;
    }
    public List<Movimiento> getListaMovimientosRecientes() {
        return listaMovimientosRecientes;
    }
    public void setListaMovimientosRecientes(List<Movimiento> listaMovimientosRecientes) {
        this.listaMovimientosRecientes = listaMovimientosRecientes;
    }
}