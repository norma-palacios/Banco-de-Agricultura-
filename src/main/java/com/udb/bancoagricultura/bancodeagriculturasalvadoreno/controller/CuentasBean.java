package com.udb.bancoagricultura.bancodeagriculturasalvadoreno.controller;

// Importaciones (las que ya tenías)
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

    // --- CAMPOS PARA TRANSFERENCIA ---
    private int idCuentaOrigen;
    private String numeroCuentaDestino;
    private BigDecimal montoTransferencia;
    private String descripcionTransferencia;


    private List<Movimiento> listaMovimientosRecientes;
    private String vistaActual = "inicio"; // 'inicio', 'cuentas', 'movimientos', 'transferencias'



    @PostConstruct
    public void init() {
        // Carga todo lo necesario al iniciar sesión
        cargarCuentas();
        cargarMovimientosRecientes(); // Carga los movimientos para el dashboard
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


    public void cargarMovimientosRecientes() {
        if (loginBean.getUsuarioLogeado() == null) return;
        EntityManager em = JPAUtil.getEntityManager();
        try {
            // Busca los 5 movimientos más recientes donde el cliente sea origen O destino
            TypedQuery<Movimiento> query = em.createQuery(
                    "SELECT m FROM Movimiento m " +
                            "WHERE m.cuentaOrigen.cliente.idUsuario = :idUsuario OR m.cuentaDestino.cliente.idUsuario = :idUsuario " +
                            "ORDER BY m.fechaMovimiento DESC",
                    Movimiento.class
            );
            query.setParameter("idUsuario", loginBean.getUsuarioLogeado().getIdUsuario());
            query.setMaxResults(5); // Limitar a los 5 más recientes
            this.listaMovimientosRecientes = query.getResultList();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            em.close();
        }
    }


    public void cambiarVista(String nuevaVista) {
        this.vistaActual = nuevaVista;

        // Recargar datos si es necesario
        // Si volvemos a inicio, recargamos todo
        if (nuevaVista.equals("inicio")) {
            cargarCuentas();
            cargarMovimientosRecientes();
        }
        // Si vamos a cuentas, solo recargamos las cuentas
        if (nuevaVista.equals("cuentas")) {
            cargarCuentas();
        }
        // Si vamos a movimientos, recargamos la lista completa (no solo 5)
        if (nuevaVista.equals("movimientos")) {
            // Este método cargará TODOS los movimientos (modificamos la query)
            cargarTodosMovimientos();
        }
    }


    public void cargarTodosMovimientos() {
        if (loginBean.getUsuarioLogeado() == null) return;
        EntityManager em = JPAUtil.getEntityManager();
        try {
            // Misma query que recientes, pero SIN setMaxResults()
            TypedQuery<Movimiento> query = em.createQuery(
                    "SELECT m FROM Movimiento m " +
                            "WHERE m.cuentaOrigen.cliente.idUsuario = :idUsuario OR m.cuentaDestino.cliente.idUsuario = :idUsuario " +
                            "ORDER BY m.fechaMovimiento DESC",
                    Movimiento.class
            );
            query.setParameter("idUsuario", loginBean.getUsuarioLogeado().getIdUsuario());
            // ¡Usamos la misma lista! La vista 'movimientos' la mostrará completa
            this.listaMovimientosRecientes = query.getResultList();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            em.close();
        }
    }


    public String crearCuenta() {
        FacesContext context = FacesContext.getCurrentInstance();
        if (listaCuentas.size() >= 3) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, "Límite alcanzado", "Ya tienes el máximo de 3 cuentas permitidas."));
            return null;
        }
        EntityManager em = JPAUtil.getEntityManager();
        try {
            // ... (lógica de creación de cuenta) ...
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

            // --- CAMBIO ---
            // Recargar cuentas (para que aparezca en la lista)
            cargarCuentas();

            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "¡Éxito!", "Nueva cuenta de " + tipoCuentaNueva + " creada."));
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "No se pudo crear la cuenta."));
            e.printStackTrace();
        } finally {
            em.close();
        }
        return null; // AJAX se encarga de redibujar
    }


    public String realizarTransferencia() {
        FacesContext context = FacesContext.getCurrentInstance();
        EntityManager em = JPAUtil.getEntityManager();

        try {


            em.getTransaction().begin();

            // 2. Obtener la cuenta de origen
            CuentaBancaria cuentaOrigen = em.find(CuentaBancaria.class, idCuentaOrigen);
            if (cuentaOrigen == null) { /* ... error ... */ em.getTransaction().rollback(); return null; }
            // 3. Obtener la cuenta de destino
            TypedQuery<CuentaBancaria> queryDestino = em.createQuery("SELECT c FROM CuentaBancaria c WHERE c.numeroCuenta = :numCuenta AND c.estado = 'ACTIVA'", CuentaBancaria.class);
            queryDestino.setParameter("numCuenta", numeroCuentaDestino);
            CuentaBancaria cuentaDestino = queryDestino.getSingleResult();
            if (cuentaOrigen.getIdCuenta() == cuentaDestino.getIdCuenta()) { /* ... error ... */ em.getTransaction().rollback(); return null; }
            // 4. Verificar fondos
            if (cuentaOrigen.getSaldo().compareTo(montoTransferencia) < 0) { /* ... error ... */ em.getTransaction().rollback(); return null; }
            // 5. Realizar la transacción
            cuentaOrigen.setSaldo(cuentaOrigen.getSaldo().subtract(montoTransferencia));
            cuentaDestino.setSaldo(cuentaDestino.getSaldo().add(montoTransferencia));
            // 6. Registrar los movimientos
            TipoMovimiento tipoTransferencia;
            try {
                TypedQuery<TipoMovimiento> queryTipo = em.createQuery("SELECT t FROM TipoMovimiento t WHERE t.nombre = 'TRANSFERENCIA'", TipoMovimiento.class);
                tipoTransferencia = queryTipo.getSingleResult();
            } catch (NoResultException e) { /* ... error ... */ em.getTransaction().rollback(); return null; }

            Movimiento movSalida = new Movimiento(); /* ... (llenar datos movSalida) ... */
            movSalida.setCuentaOrigen(cuentaOrigen);
            movSalida.setTipoMovimiento(tipoTransferencia);
            movSalida.setMonto(montoTransferencia);
            movSalida.setDescripcion("Transferencia a " + cuentaDestino.getNumeroCuenta() + ": " + descripcionTransferencia);
            movSalida.setFechaMovimiento(new Date());

            Movimiento movEntrada = new Movimiento(); /* ... (llenar datos movEntrada) ... */
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

            // --- CAMBIO ---
            cargarCuentas(); // Recarga la lista de cuentas con el nuevo saldo
            cargarMovimientosRecientes(); // Recarga el dashboard

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