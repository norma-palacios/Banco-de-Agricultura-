package com.udb.bancoagricultura.bancodeagriculturasalvadoreno.controller;

import com.udb.bancoagricultura.bancodeagriculturasalvadoreno.model.pojo.*;
import com.udb.bancoagricultura.bancodeagriculturasalvadoreno.model.util.JPAUtil;

import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import javax.transaction.Transactional;
import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Named
@SessionScoped
public class GerenteSucursalBean implements Serializable {

    @Inject
    private LoginBean loginBean; // Para seguridad y contexto
    private String vistaActual = "ingresoEmpleados";
    // variables
    private String dui;
    private String nombres;
    private String apellidos;
    private java.util.Date fechaNacimiento;
    private String telefono;
    private String email;
    private String direccion;

    // --- Datos del Empleado ---
    private int idSucursal;
    private String cargo;
    private BigDecimal salario;
    private String motivo;

    private String mensaje;
    private List<Empleado> empleadosActivos;


    public void cambiarVista(String nuevaVista) {
        this.vistaActual = nuevaVista;
    }

    @Transactional
    public void registrarPersonaYEmpleado() {
        EntityManager em = null;
        EntityTransaction tx = null;

        try {
            em = JPAUtil.getEntityManager();
            tx = em.getTransaction();
            tx.begin(); // 👈 INICIA la transacción

            Integer idGerente = obtenerIdGerente(em);

            if (idGerente == null) {
                mensaje = "⚠️ No se encontró el gerente logueado.";
                return;
            }

            TypedQuery<Sucursal> query = em.createQuery(
                    "SELECT e.sucursal FROM Empleado e WHERE e.idEmpleado = :idGerente",
                    Sucursal.class
            );
            query.setParameter("idGerente", idGerente);
            Sucursal sucursalGerente = query.getSingleResult();

            if (sucursalGerente == null) {
                mensaje = "⚠️ No se encontró la sucursal del gerente.";
                return;
            }

            // 2️⃣ Crear y persistir persona
            Persona p = new Persona();
            p.setDui(dui);
            p.setNombres(nombres);
            p.setApellidos(apellidos);
            p.setFechaNacimiento(fechaNacimiento);
            p.setTelefono(telefono);
            p.setEmail(email);
            p.setDireccion(direccion);
            p.setFechaRegistro(new java.util.Date());
            em.persist(p);

            // 3️⃣ Crear y persistir empleado
            Empleado emp = new Empleado();
            emp.setPersona(p);
            emp.setSucursal(sucursalGerente);
            emp.setCargo(cargo);
            emp.setSalario(salario);
            emp.setFechaContratacion(new Date(System.currentTimeMillis()));
            emp.setEstado("PENDIENTE_APROBACION");
            em.persist(emp);

            // 4️⃣ Crear y persistir acción de personal
            AccionPersonal acc = new AccionPersonal();
            acc.setEmpleado(emp);
            acc.setIdGerenteSucursal(idGerente);
            acc.setTipoAccion("CONTRATACION");
            acc.setMotivo(motivo);
            acc.setFechaCreacion(new java.util.Date());
            acc.setEstado("PENDIENTE");
            em.persist(acc);

            tx.commit();

            mensaje = "✅ Persona y empleado registrados correctamente en sucursal #" + sucursalGerente.getNombre();

            limpiarCampos();

        } catch (Exception e) {
            e.printStackTrace();
            mensaje = "❌ Error al registrar: " + e.getMessage();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    private Integer obtenerIdGerente(EntityManager em) {
        try {
            TypedQuery<Integer> query = em.createQuery(
                    "SELECT e.idEmpleado " +
                            "FROM Empleado e " +
                            "JOIN Persona p ON e.persona.idPersona = p.idPersona " +
                            "JOIN Usuario u ON u.idPersona = p.idPersona " +
                            "WHERE u.username = :username",
                    Integer.class
            );

            query.setParameter("username", loginBean.getUsername());
            return query.getSingleResult();

        } catch (NoResultException e) {
            System.out.println("⚠️ No se encontró empleado asociado al usuario logueado.");
            return null;
        }
    }

    public List<Empleado> getEmpleadosActivos() {
        if (empleadosActivos == null) {
            cargarEmpleadosActivos();
        }
        return empleadosActivos;
    }

    public void cargarEmpleadosActivos() {
        EntityManager em = null;
        try {
            em = JPAUtil.getEntityManager();
            empleadosActivos = em.createQuery(
                            "SELECT e FROM Empleado e WHERE e.estado = 'ACTIVO'", Empleado.class)
                    .getResultList();
        } catch (Exception e) {
            e.printStackTrace();
            empleadosActivos = new ArrayList<>();
        } finally {
            if (em != null) em.close();
        }
    }


    public void desactivarEmpleado(Empleado empleadoSeleccionado) {
        EntityManager em = null;
        EntityTransaction tx = null;

        try {
            em = JPAUtil.getEntityManager();
            tx = em.getTransaction();
            tx.begin();

            Integer idGerente = obtenerIdGerente(em);

            if (idGerente == null) {
                mensaje = "⚠️ No se encontró el gerente logueado.";
                return;
            }

            // Actualizar estado
            Empleado empleado = em.find(Empleado.class, empleadoSeleccionado.getIdEmpleado());
            empleado.setEstado("INACTIVO");
            em.merge(empleado);

            // Registrar acción
            AccionPersonal accion = new AccionPersonal();
            accion.setEmpleado(empleado);
            accion.setIdGerenteSucursal(idGerente);
            accion.setTipoAccion("DESACTIVACIÓN");
            accion.setMotivo("Desactivado por el Gerente de Sucursal");
            accion.setFechaCreacion(new java.util.Date());
            accion.setEstado("PENDIENTE");
            em.persist(accion);

            tx.commit();

            mensaje = "✅ Empleado " + empleado.getPersona().getNombres() + " desactivado correctamente.";

            // Recargar lista actualizada
            cargarEmpleadosActivos();

        } catch (Exception e) {
            if (tx != null && tx.isActive()) tx.rollback();
            e.printStackTrace();
            mensaje = "❌ Error al desactivar empleado: " + e.getMessage();
        } finally {
            if (em != null) em.close();
        }
    }


    public String getVistaActual() { return vistaActual; }
    public void setVistaActual(String vistaActual) { this.vistaActual = vistaActual; }

    // setter y getters

    private void limpiarCampos() {
        dui = "";
        nombres = "";
        apellidos = "";
        fechaNacimiento = null; // ← ahora limpiamos como Date
        telefono = "";
        email = "";
        direccion = "";
        cargo = "";
        salario = BigDecimal.valueOf(0.0);
        motivo = "";
    }

    // --- Getters y Setters ---
    public String getDui() { return dui; }
    public void setDui(String dui) { this.dui = dui; }

    public String getNombres() { return nombres; }
    public void setNombres(String nombres) { this.nombres = nombres; }

    public String getApellidos() { return apellidos; }
    public void setApellidos(String apellidos) { this.apellidos = apellidos; }

    public java.util.Date getFechaNacimiento() { return fechaNacimiento; }
    public void setFechaNacimiento(java.util.Date fechaNacimiento) { this.fechaNacimiento = fechaNacimiento; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public int getIdSucursal() { return idSucursal; }
    public void setIdSucursal(int idSucursal) { this.idSucursal = idSucursal; }

    public String getCargo() { return cargo; }
    public void setCargo(String cargo) { this.cargo = cargo; }

    public BigDecimal getSalario() { return salario; }
    public void setSalario(BigDecimal salario) { this.salario = salario; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }

    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }
}
