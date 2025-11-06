package com.udb.bancoagricultura.bancodeagriculturasalvadoreno.controller;

import com.udb.bancoagricultura.bancodeagriculturasalvadoreno.model.pojo.Usuario;
import com.udb.bancoagricultura.bancodeagriculturasalvadoreno.service.LoginServices;

import javax.enterprise.context.SessionScoped;
import javax.inject.Named;
import java.io.Serializable;
import java.io.IOException;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpSession;

@Named
@SessionScoped
public class LoginBean implements Serializable {
    private String username;
    private String password;
    private Usuario usuarioLogeado;
    private String mensajeError;

    private final LoginServices loginService = new LoginServices();


    public String login() {
        usuarioLogeado = loginService.validarUsuario(username, password);

        if (usuarioLogeado != null) {
            mensajeError = null;
            switch (usuarioLogeado.getTipoUsuario()) {
                case "CAJERO":
                    return "iniciocajero.xhtml?faces-redirect=true";
                case "CLIENTE":
                    return "inicio.xhtml?faces-redirect=true";
                case "DEPENDIENTE":
                    return "iniciodependiente.xhtml?faces-redirect=true";
                case "GERENTE_SUCURSAL":
                    return "iniciogerentesucursal.xhtml?faces-redirect=true";
                case "GERENTE_GENERAL":
                    return "iniciogerentegeneral.xhtml?faces-redirect=true";
                default:

                    return "index.xhtml?error=rol_invalido";
            }

        } else {
            mensajeError = "Usuario o contraseña incorrectos o cuenta inactiva.";
            return null;
        }
    }


    public String logout() throws IOException {
        FacesContext context = FacesContext.getCurrentInstance();
        ExternalContext externalContext = context.getExternalContext();
        HttpSession session = (HttpSession) externalContext.getSession(false);

        if (session != null) {
            session.invalidate();
        }

        this.usuarioLogeado = null;
        this.username = null;
        this.password = null;

        externalContext.redirect(externalContext.getRequestContextPath() + "/index.xhtml");
        context.responseComplete();
        return null;
    }


    public void verificarSesion() {
        FacesContext context = FacesContext.getCurrentInstance();
        ExternalContext externalContext = context.getExternalContext();

        if (this.usuarioLogeado == null) {
            try {

                externalContext.redirect(externalContext.getRequestContextPath() + "/index.xhtml");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    // Getters y Setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Usuario getUsuarioLogeado() { return usuarioLogeado; }

    public String getMensajeError() { return mensajeError; }
}