package com.udb.bancoagricultura.bancodeagriculturasalvadoreno.controller;

import com.udb.bancoagricultura.bancodeagriculturasalvadoreno.model.pojo.CuentaBancaria;
import com.udb.bancoagricultura.bancodeagriculturasalvadoreno.model.pojo.Persona;
import com.udb.bancoagricultura.bancodeagriculturasalvadoreno.model.pojo.Usuario;
import com.udb.bancoagricultura.bancodeagriculturasalvadoreno.service.DependienteService; // Crearemos este servicio

import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Named
@SessionScoped
public class DependienteBean implements Serializable {

    @Inject
    private LoginBean loginBean; // Para saber qué dependiente está logeado

    // --- Variables para la Búsqueda ---
    private String duiBusqueda;
    private Persona clienteEncontrado;
    private Usuario usuarioCliente;
    private List<CuentaBancaria> cuentasCliente;
    private String mensajeBusqueda;

    // --- Variables para la Transacción ---
    private int idCuentaSeleccionada;
    private BigDecimal montoTransaccion;
    private String mensajeTransaccion;

    // Instancia de nuestro nuevo servicio
    private final DependienteService dependienteService = new DependienteService();

    @PostConstruct
    public void init() {
        // Inicializa las listas para evitar errores
        cuentasCliente = new ArrayList<>();
    }

    /**
     * Acción llamada desde el XHTML para buscar un cliente por su DUI.
     */
    public void buscarCliente() {
        limpiarCampos();
        try {
            // Usamos el servicio para buscar
            this.usuarioCliente = dependienteService.buscarClientePorDui(duiBusqueda);

            if (this.usuarioCliente != null) {
                // Si encontramos, cargamos sus datos y cuentas
                this.clienteEncontrado = dependienteService.buscarPersonaPorId(this.usuarioCliente.getIdPersona());
                this.cuentasCliente = dependienteService.buscarCuentasPorUsuario(this.usuarioCliente);

                if (this.cuentasCliente.isEmpty()) {
                    this.mensajeBusqueda = "Cliente encontrado, pero no tiene cuentas bancarias activas.";
                }
            } else {
                this.mensajeBusqueda = "No se encontró ningún cliente con el DUI: " + duiBusqueda;
            }

        } catch (Exception e) {
            this.mensajeBusqueda = "Error al realizar la búsqueda: " + e.getMessage();
            e.printStackTrace();
        }
    }

    /**
     * Acción para realizar un depósito (Abono).
     */
    public void realizarDeposito() {
        FacesContext context = FacesContext.getCurrentInstance();
        if (montoTransaccion == null || montoTransaccion.compareTo(BigDecimal.ZERO) <= 0) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "El monto debe ser mayor a cero."));
            return;
        }

        try {
            Usuario dependienteLogeado = loginBean.getUsuarioLogeado();

            // El servicio se encarga de toda la lógica (comisión, actualizar saldo, crear movimiento)
            CuentaBancaria cuentaActualizada = dependienteService.realizarDeposito(dependienteLogeado, idCuentaSeleccionada, montoTransaccion);

            // Actualizamos la vista
            actualizarListaCuentas(cuentaActualizada);
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Éxito", "Depósito realizado correctamente."));
            limpiarCamposTransaccion();

        } catch (Exception e) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "No se pudo realizar el depósito: " + e.getMessage()));
            e.printStackTrace();
        }
    }

    /**
     * Acción para realizar un retiro.
     */
    public void realizarRetiro() {
        FacesContext context = FacesContext.getCurrentInstance();
        if (montoTransaccion == null || montoTransaccion.compareTo(BigDecimal.ZERO) <= 0) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "El monto debe ser mayor a cero."));
            return;
        }

        try {
            Usuario dependienteLogeado = loginBean.getUsuarioLogeado();

            // El servicio se encarga de la lógica (validar fondos, comisión, actualizar saldo, crear movimiento)
            CuentaBancaria cuentaActualizada = dependienteService.realizarRetiro(dependienteLogeado, idCuentaSeleccionada, montoTransaccion);

            // Actualizamos la vista
            actualizarListaCuentas(cuentaActualizada);
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Éxito", "Retiro realizado correctamente."));
            limpiarCamposTransaccion();

        } catch (Exception e) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "No se pudo realizar el retiro: " + e.getMessage()));
            e.printStackTrace();
        }
    }

    // --- Métodos de ayuda ---

    private void limpiarCampos() {
        this.clienteEncontrado = null;
        this.usuarioCliente = null;
        this.cuentasCliente.clear();
        this.mensajeBusqueda = null;
        limpiarCamposTransaccion();
    }

    private void limpiarCamposTransaccion() {
        this.montoTransaccion = null;
        this.idCuentaSeleccionada = 0;
    }

    // Actualiza el saldo en la lista que se muestra en pantalla
    private void actualizarListaCuentas(CuentaBancaria cuentaActualizada) {
        for (int i = 0; i < cuentasCliente.size(); i++) {
            if (cuentasCliente.get(i).getIdCuenta() == cuentaActualizada.getIdCuenta()) {
                cuentasCliente.set(i, cuentaActualizada);
                break;
            }
        }
    }


    // --- Getters y Setters ---

    public String getDuiBusqueda() { return duiBusqueda; }
    public void setDuiBusqueda(String duiBusqueda) { this.duiBusqueda = duiBusqueda; }

    public Persona getClienteEncontrado() { return clienteEncontrado; }
    public void setClienteEncontrado(Persona clienteEncontrado) { this.clienteEncontrado = clienteEncontrado; }

    public Usuario getUsuarioCliente() { return usuarioCliente; }
    public void setUsuarioCliente(Usuario usuarioCliente) { this.usuarioCliente = usuarioCliente; }

    public List<CuentaBancaria> getCuentasCliente() { return cuentasCliente; }
    public void setCuentasCliente(List<CuentaBancaria> cuentasCliente) { this.cuentasCliente = cuentasCliente; }

    public String getMensajeBusqueda() { return mensajeBusqueda; }
    public void setMensajeBusqueda(String mensajeBusqueda) { this.mensajeBusqueda = mensajeBusqueda; }

    public int getIdCuentaSeleccionada() { return idCuentaSeleccionada; }
    public void setIdCuentaSeleccionada(int idCuentaSeleccionada) { this.idCuentaSeleccionada = idCuentaSeleccionada; }

    public BigDecimal getMontoTransaccion() { return montoTransaccion; }
    public void setMontoTransaccion(BigDecimal montoTransaccion) { this.montoTransaccion = montoTransaccion; }

    public String getMensajeTransaccion() { return mensajeTransaccion; }
    public void setMensajeTransaccion(String mensajeTransaccion) { this.mensajeTransaccion = mensajeTransaccion; }
}