package com.udb.bancoagricultura.bancodeagriculturasalvadoreno.controller;


import com.udb.bancoagricultura.bancodeagriculturasalvadoreno.model.pojo.Empleado;
import com.udb.bancoagricultura.bancodeagriculturasalvadoreno.model.pojo.Persona;
import com.udb.bancoagricultura.bancodeagriculturasalvadoreno.model.pojo.Sucursal;
import com.udb.bancoagricultura.bancodeagriculturasalvadoreno.model.pojo.Usuario;
import com.udb.bancoagricultura.bancodeagriculturasalvadoreno.model.util.JPAUtil;
import at.favre.lib.crypto.bcrypt.BCrypt; // Para hashear la clave


import com.udb.bancoagricultura.bancodeagriculturasalvadoreno.model.pojo.AccionPersonal;
import com.udb.bancoagricultura.bancodeagriculturasalvadoreno.model.pojo.Movimiento;
import javax.persistence.NoResultException;

import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.io.Serializable;
import java.math.BigDecimal; // Importante para Salario
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Named
@SessionScoped
public class GerenteGeneralBean implements Serializable {

    @Inject
    private LoginBean loginBean;

    private String vistaActual = "sucursales";

    // Listas para la vista "Sucursales"
    private List<Sucursal> listaSucursales = new ArrayList<>();
    private List<Empleado> listaGerentesSucursal = new ArrayList<>();

    // --- LISTAS NUEVAS AÑADIDAS ---
    private List<AccionPersonal> listaAccionesPendientes = new ArrayList<>();
    private List<Movimiento> listaTodosMovimientos = new ArrayList<>();

    // Campos para el formulario de "Nueva Sucursal"
    private String nuevoNombre;
    private String nuevaDireccion;
    private String nuevoTelefono;

    // Campos para el formulario de Gerente de Sucursal
    private int sucursalAsignadaId;
    private String gerenteDui;
    private String gerenteNombres;
    private String gerenteApellidos;
    private Date gerenteFechaNac;
    private String gerenteEmail;
    private String gerenteTelefono;
    private String gerenteUsername;
    private String gerentePassword;
    private BigDecimal gerenteSalario;

    @PostConstruct
    public void init() {
        // Cargar ambas listas al iniciar
        cargarSucursales();
        cargarGerentesDeSucursal();
    }

    // --- MÉTODOS DE DATOS ---

    public void cargarSucursales() {
        if (loginBean.getUsuarioLogeado() == null) return;
        EntityManager em = JPAUtil.getEntityManager();
        try {
            TypedQuery<Sucursal> query = em.createQuery("SELECT s FROM Sucursal s ORDER BY s.nombre", Sucursal.class);
            this.listaSucursales = query.getResultList();
        } catch (Exception e) {
            e.printStackTrace();
            this.listaSucursales = new ArrayList<>();
        } finally {
            em.close();
        }
    }

    public void cargarGerentesDeSucursal() {
        if (loginBean.getUsuarioLogeado() == null) return;
        EntityManager em = JPAUtil.getEntityManager();
        try {
            TypedQuery<Empleado> query = em.createQuery(
                    "SELECT e FROM Empleado e " +
                            "JOIN FETCH e.persona " +
                            "JOIN FETCH e.sucursal " +
                            "WHERE e.cargo = 'GERENTE_SUCURSAL' " +
                            "ORDER BY e.sucursal.nombre",
                    Empleado.class
            );
            this.listaGerentesSucursal = query.getResultList();
        } catch (Exception e) {
            e.printStackTrace();
            this.listaGerentesSucursal = new ArrayList<>();
        } finally {
            em.close();
        }
    }

    // --- NUEVOS MÉTODOS DE DATOS AÑADIDOS ---

    public void cargarAccionesPendientes() {
        if (loginBean.getUsuarioLogeado() == null) return;
        EntityManager em = JPAUtil.getEntityManager();
        try {
            // Consulta que trae acciones PENDIENTES con sus Empleados y Personas
            TypedQuery<AccionPersonal> query = em.createQuery(
                    "SELECT ap FROM AccionPersonal ap " +
                            "JOIN FETCH ap.empleado e " +
                            "JOIN FETCH e.persona " +
                            "WHERE ap.estado = 'PENDIENTE'",
                    AccionPersonal.class
            );
            this.listaAccionesPendientes = query.getResultList();
        } catch (Exception e) {
            e.printStackTrace();
            this.listaAccionesPendientes = new ArrayList<>();
        } finally {
            em.close();
        }
    }

    public void cargarTodosMovimientos() {
        if (loginBean.getUsuarioLogeado() == null) return;
        EntityManager em = JPAUtil.getEntityManager();
        try {
            // Carga todos los movimientos, con el tipo de movimiento
            TypedQuery<Movimiento> query = em.createQuery(
                    "SELECT m FROM Movimiento m " +
                            "JOIN FETCH m.tipoMovimiento " +
                            "ORDER BY m.fechaMovimiento DESC",
                    Movimiento.class
            );
            query.setMaxResults(100); // Limitar a los últimos 100 para no sobrecargar
            this.listaTodosMovimientos = query.getResultList();
        } catch (Exception e) {
            e.printStackTrace();
            this.listaTodosMovimientos = new ArrayList<>();
        } finally {
            em.close();
        }
    }

    // --- MÉTODOS DE ACCIÓN (Botones) ---

    public String crearSucursal() {
        // ... (Tu código existente)
        FacesContext context = FacesContext.getCurrentInstance();
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            Sucursal nuevaSucursal = new Sucursal();
            nuevaSucursal.setNombre(nuevoNombre);
            nuevaSucursal.setDireccion(nuevaDireccion);
            nuevaSucursal.setTelefono(nuevoTelefono);
            nuevaSucursal.setEstado("ACTIVA");
            nuevaSucursal.setFechaCreacion(new Date());
            em.persist(nuevaSucursal);
            em.getTransaction().commit();
            this.nuevoNombre = null;
            this.nuevaDireccion = null;
            this.nuevoTelefono = null;
            cargarSucursales();
            cargarGerentesDeSucursal();
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Éxito", "Nueva sucursal creada."));
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            e.printStackTrace();
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "No se pudo crear la sucursal."));
        } finally {
            em.close();
        }
        return null;
    }

    public String crearYAsignarGerente() {
        // ... (Tu código existente)
        FacesContext context = FacesContext.getCurrentInstance();
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            Persona nuevaPersona = new Persona();
            nuevaPersona.setDui(gerenteDui);
            nuevaPersona.setNombres(gerenteNombres);
            nuevaPersona.setApellidos(gerenteApellidos);
            nuevaPersona.setFechaNacimiento(gerenteFechaNac);
            nuevaPersona.setEmail(gerenteEmail);
            nuevaPersona.setTelefono(gerenteTelefono);
            em.persist(nuevaPersona);
            em.flush();
            Usuario nuevoUsuario = new Usuario();
            nuevoUsuario.setIdPersona(nuevaPersona.getIdPersona());
            nuevoUsuario.setUsername(gerenteUsername);
            String hash = BCrypt.withDefaults().hashToString(12, gerentePassword.toCharArray());
            nuevoUsuario.setPasswordHash(hash);
            nuevoUsuario.setTipoUsuario("GERENTE_SUCURSAL");
            nuevoUsuario.setEstado("ACTIVO");
            em.persist(nuevoUsuario);
            Sucursal sucursal = em.find(Sucursal.class, sucursalAsignadaId);
            if (sucursal == null) { throw new Exception("La sucursal seleccionada no existe."); }
            Empleado nuevoEmpleado = new Empleado();
            nuevoEmpleado.setPersona(nuevaPersona);
            nuevoEmpleado.setSucursal(sucursal);
            nuevoEmpleado.setCargo("GERENTE_SUCURSAL");
            nuevoEmpleado.setSalario(gerenteSalario);
            nuevoEmpleado.setFechaContratacion(new Date());
            nuevoEmpleado.setEstado("ACTIVO");
            em.persist(nuevoEmpleado);
            em.getTransaction().commit();
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Éxito", "Gerente de Sucursal creado y asignado."));
            cargarSucursales();
            cargarGerentesDeSucursal();
            limpiarFormularioGerente();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            e.printStackTrace();
            if (e.getCause() != null && e.getCause().getMessage().contains("Duplicate entry")) {
                context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "No se pudo crear el gerente. El DUI, Email o Username ya existe."));
            } else {
                context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "No se pudo crear el gerente. " + e.getMessage()));
            }
        } finally {
            em.close();
        }
        return null;
    }

    private void limpiarFormularioGerente() {
        this.sucursalAsignadaId = 0;
        this.gerenteDui = null;
        this.gerenteNombres = null;
        this.gerenteApellidos = null;
        this.gerenteFechaNac = null;
        this.gerenteEmail = null;
        this.gerenteTelefono = null;
        this.gerenteUsername = null;
        this.gerentePassword = null;
        this.gerenteSalario = null;
    }

    // --- NUEVOS MÉTODOS DE ACCIÓN AÑADIDOS ---

    public void aprobarAccion(AccionPersonal accion) {
        if (!"PENDIENTE".equals(accion.getEstado())) return;

        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();

            AccionPersonal accionMerge = em.merge(accion);
            accionMerge.setEstado("APROBADA");
            accionMerge.setFechaResolucion(new Date());

            Empleado empleado = em.merge(accionMerge.getEmpleado());
            if ("CONTRATACION".equals(accionMerge.getTipoAccion())) {
                empleado.setEstado("ACTIVO");
            } else if ("BAJA".equals(accionMerge.getTipoAccion())) {
                empleado.setEstado("INACTIVO");
            }

            em.getTransaction().commit();
            cargarAccionesPendientes();
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Éxito", "Acción aprobada."));

        } catch (Exception e) {
            if(em.getTransaction().isActive()) em.getTransaction().rollback();
            e.printStackTrace();
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "No se pudo aprobar la acción."));
        } finally {
            em.close();
        }
    }

    public void rechazarAccion(AccionPersonal accion) {
        if (!"PENDIENTE".equals(accion.getEstado())) return;

        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();

            AccionPersonal accionMerge = em.merge(accion);
            accionMerge.setEstado("RECHAZADA");
            accionMerge.setFechaResolucion(new Date());

            if ("CONTRATACION".equals(accionMerge.getTipoAccion())) {
                Empleado empleado = em.merge(accionMerge.getEmpleado());
                empleado.setEstado("INACTIVO"); // La contratación fue rechazada
            }

            em.getTransaction().commit();
            cargarAccionesPendientes();
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, "Rechazado", "Acción rechazada."));

        } catch (Exception e) {
            if(em.getTransaction().isActive()) em.getTransaction().rollback();
            e.printStackTrace();
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "No se pudo rechazar la acción."));
        } finally {
            em.close();
        }
    }

    // --- MÉTODO CAMBIAR VISTA (MODIFICADO) ---
    public void cambiarVista(String nuevaVista) {
        this.vistaActual = nuevaVista;
        if (nuevaVista.equals("sucursales")) {
            cargarSucursales();
            cargarGerentesDeSucursal();
        }
        // --- AÑADIDOS ---
        if (nuevaVista.equals("personal")) {
            cargarAccionesPendientes();
        }
        if (nuevaVista.equals("movimientos")) {
            cargarTodosMovimientos();
        }
    }

    // --- Getters y Setters ---

    public String getVistaActual() { return vistaActual; }
    public void setVistaActual(String vistaActual) { this.vistaActual = vistaActual; }

    public List<Sucursal> getListaSucursales() { return listaSucursales; }
    public void setListaSucursales(List<Sucursal> listaSucursales) { this.listaSucursales = listaSucursales; }

    public List<Empleado> getListaGerentesSucursal() { return listaGerentesSucursal; }
    public void setListaGerentesSucursal(List<Empleado> listaGerentesSucursal) { this.listaGerentesSucursal = listaGerentesSucursal; }

    public String getNuevoNombre() { return nuevoNombre; }
    public void setNuevoNombre(String nuevoNombre) { this.nuevoNombre = nuevoNombre; }

    public String getNuevaDireccion() { return nuevaDireccion; }
    public void setNuevaDireccion(String nuevaDireccion) { this.nuevaDireccion = nuevaDireccion; }

    public String getNuevoTelefono() { return nuevoTelefono; }
    public void setNuevoTelefono(String nuevoTelefono) { this.nuevoTelefono = nuevoTelefono; }

    public int getSucursalAsignadaId() { return sucursalAsignadaId; }
    public void setSucursalAsignadaId(int sucursalAsignadaId) { this.sucursalAsignadaId = sucursalAsignadaId; }
    public String getGerenteDui() { return gerenteDui; }
    public void setGerenteDui(String gerenteDui) { this.gerenteDui = gerenteDui; }
    public String getGerenteNombres() { return gerenteNombres; }
    public void setGerenteNombres(String gerenteNombres) { this.gerenteNombres = gerenteNombres; }
    public String getGerenteApellidos() { return gerenteApellidos; }
    public void setGerenteApellidos(String gerenteApellidos) { this.gerenteApellidos = gerenteApellidos; }
    public Date getGerenteFechaNac() { return gerenteFechaNac; }
    public void setGerenteFechaNac(Date gerenteFechaNac) { this.gerenteFechaNac = gerenteFechaNac; }
    public String getGerenteEmail() { return gerenteEmail; }
    public void setGerenteEmail(String gerenteEmail) { this.gerenteEmail = gerenteEmail; }
    public String getGerenteTelefono() { return gerenteTelefono; }
    public void setGerenteTelefono(String gerenteTelefono) { this.gerenteTelefono = gerenteTelefono; }
    public String getGerenteUsername() { return gerenteUsername; }
    public void setGerenteUsername(String gerenteUsername) { this.gerenteUsername = gerenteUsername; }
    public String getGerentePassword() { return gerentePassword; }
    public void setGerentePassword(String gerentePassword) { this.gerentePassword = gerentePassword; }
    public BigDecimal getGerenteSalario() { return gerenteSalario; }
    public void setGerenteSalario(BigDecimal gerenteSalario) { this.gerenteSalario = gerenteSalario; }

    // --- NUEVOS GETTERS/SETTERS AÑADIDOS ---
    public List<AccionPersonal> getListaAccionesPendientes() { return listaAccionesPendientes; }
    public void setListaAccionesPendientes(List<AccionPersonal> lista) { this.listaAccionesPendientes = lista; }

    public List<Movimiento> getListaTodosMovimientos() { return listaTodosMovimientos; }
    public void setListaTodosMovimientos(List<Movimiento> lista) { this.listaTodosMovimientos = lista; }
}