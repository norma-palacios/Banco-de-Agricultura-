package com.udb.bancoagricultura.bancodeagriculturasalvadoreno.controller;


import javax.enterprise.context.RequestScoped;
import javax.inject.Named;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.persistence.EntityManager;


import com.udb.bancoagricultura.bancodeagriculturasalvadoreno.model.util.JPAUtil;

import com.udb.bancoagricultura.bancodeagriculturasalvadoreno.model.pojo.Persona;
import com.udb.bancoagricultura.bancodeagriculturasalvadoreno.model.pojo.Usuario;


import at.favre.lib.crypto.bcrypt.BCrypt;

import java.io.Serializable;
import java.util.Date;

@Named
@RequestScoped
public class RegistroBean implements Serializable {

    // --- Campos para la Persona ---
    private String dui;
    private String nombres;
    private String apellidos;
    private Date fechaNacimiento;
    private String telefono;
    private String email;
    private String direccion;

    // --- Campos para el Usuario ---
    private String username;
    private String password;



    public String getDui() { return dui; }
    public void setDui(String dui) { this.dui = dui; }
    public String getNombres() { return nombres; }
    public void setNombres(String nombres) { this.nombres = nombres; }
    public String getApellidos() { return apellidos; }
    public void setApellidos(String apellidos) { this.apellidos = apellidos; }
    public Date getFechaNacimiento() { return fechaNacimiento; }
    public void setFechaNacimiento(Date fechaNacimiento) { this.fechaNacimiento = fechaNacimiento; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }


    public String registrar() {
        EntityManager em = JPAUtil.getEntityManager();
        FacesContext context = FacesContext.getCurrentInstance();

        try {

            em.getTransaction().begin();

            // 1. Crear y guardar la Persona
            Persona nuevaPersona = new Persona();
            nuevaPersona.setDui(dui);
            nuevaPersona.setNombres(nombres);
            nuevaPersona.setApellidos(apellidos);
            nuevaPersona.setFechaNacimiento(fechaNacimiento);
            nuevaPersona.setEmail(email);
            nuevaPersona.setTelefono(telefono);
            nuevaPersona.setDireccion(direccion);

            em.persist(nuevaPersona);


            em.flush();

            // 2. Crear y guardar el Usuario
            Usuario nuevoUsuario = new Usuario();
            nuevoUsuario.setIdPersona(nuevaPersona.getIdPersona()); // Enlazamos con la persona creada
            nuevoUsuario.setUsername(username);


            String hashedPassword = BCrypt.withDefaults().hashToString(12, password.toCharArray());
            nuevoUsuario.setPasswordHash(hashedPassword);


            nuevoUsuario.setTipoUsuario("CLIENTE");
            nuevoUsuario.setEstado("ACTIVO");

            em.persist(nuevoUsuario);


            em.getTransaction().commit();


            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "¡Registro Exitoso!", "Tu cuenta ha sido creada. Ahora puedes iniciar sesión."));

            // Redirigimos al Login (index.xhtml)
            return "index.xhtml?faces-redirect=true";

        } catch (Exception e) {
            // Revertir todo si algo falla (ej. DUI o email duplicado)
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            // Mostrar mensaje de error
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error en el registro", "No se pudo completar el registro. Verifica los datos (ej. DUI o Email duplicado)."));
            return null; // Nos quedamos en la misma página
        } finally {
            em.close();
        }
    }
}