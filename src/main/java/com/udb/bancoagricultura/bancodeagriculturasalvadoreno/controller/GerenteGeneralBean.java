package com.udb.bancoagricultura.bancodeagriculturasalvadoreno.controller;

// Importaciones necesarias
import com.udb.bancoagricultura.bancodeagriculturasalvadoreno.model.pojo.Empleado;
import com.udb.bancoagricultura.bancodeagriculturasalvadoreno.model.pojo.Persona;
import com.udb.bancoagricultura.bancodeagriculturasalvadoreno.model.pojo.Sucursal;
import com.udb.bancoagricultura.bancodeagriculturasalvadoreno.model.pojo.Usuario;
import com.udb.bancoagricultura.bancodeagriculturasalvadoreno.model.util.JPAUtil;
import at.favre.lib.crypto.bcrypt.BCrypt; // Para hashear la clave

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
    private LoginBean loginBean; // Para seguridad y contexto


    private String vistaActual = "sucursales";



    private List<Sucursal> listaSucursales = new ArrayList<>();
    // Esta lista es para la <h:dataTable> (la tabla principal)
    private List<Empleado> listaGerentesSucursal = new ArrayList<>();

    // Campos para el formulario de "Nueva Sucursal"
    private String nuevoNombre;
    private String nuevaDireccion;
    private String nuevoTelefono;

    // --- CAMPOS: Para el formulario de Gerente de Sucursal (sin cambios) ---
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

    // --- MÉTODO INIT (MODIFICADO) ---
    @PostConstruct
    public void init() {
        // Cargar ambas listas al iniciar
        cargarSucursales();
        cargarGerentesDeSucursal();
    }


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
            // Consulta que trae Empleados CON sus Personas y Sucursales
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
            this.listaGerentesSucursal = new ArrayList<>(); // Asegurar que no sea null
        } finally {
            em.close();
        }
    }




    public String crearSucursal() {
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

            // Recargar ambas listas
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
        FacesContext context = FacesContext.getCurrentInstance();
        EntityManager em = JPAUtil.getEntityManager();

        try {
            em.getTransaction().begin();

            // 1. Crear la Persona
            Persona nuevaPersona = new Persona();
            nuevaPersona.setDui(gerenteDui);
            nuevaPersona.setNombres(gerenteNombres);
            nuevaPersona.setApellidos(gerenteApellidos);
            nuevaPersona.setFechaNacimiento(gerenteFechaNac);
            nuevaPersona.setEmail(gerenteEmail);
            nuevaPersona.setTelefono(gerenteTelefono);
            em.persist(nuevaPersona);
            em.flush();

            // 2. Crear el Usuario
            Usuario nuevoUsuario = new Usuario();
            nuevoUsuario.setIdPersona(nuevaPersona.getIdPersona()); // Correcto
            nuevoUsuario.setUsername(gerenteUsername);
            String hash = BCrypt.withDefaults().hashToString(12, gerentePassword.toCharArray());
            nuevoUsuario.setPasswordHash(hash);
            nuevoUsuario.setTipoUsuario("GERENTE_SUCURSAL");
            nuevoUsuario.setEstado("ACTIVO");
            em.persist(nuevoUsuario);

            // 3. Crear el Empleado
            Sucursal sucursal = em.find(Sucursal.class, sucursalAsignadaId);
            if (sucursal == null) {
                throw new Exception("La sucursal seleccionada no existe.");
            }
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

            // Recargar ambas listas
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
        return null; // AJAX
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

    // --- MÉTODO CAMBIAR VISTA (MODIFICADO) ---
    public void cambiarVista(String nuevaVista) {
        this.vistaActual = nuevaVista;
        // Si vamos a la vista de sucursales, recargamos todo
        if (nuevaVista.equals("sucursales")) {
            cargarSucursales();
            cargarGerentesDeSucursal();
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
}