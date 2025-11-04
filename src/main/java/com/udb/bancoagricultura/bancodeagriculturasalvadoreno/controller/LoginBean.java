package com.udb.bancoagricultura.bancodeagriculturasalvadoreno.controller;

import com.udb.bancoagricultura.bancodeagriculturasalvadoreno.model.pojo.Usuario;
import com.udb.bancoagricultura.bancodeagriculturasalvadoreno.service.LoginServices;

import javax.enterprise.context.SessionScoped;
import javax.inject.Named;
import java.io.Serializable;

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
            return "inicio.xhtml?faces-redirect=true";
        } else {
            mensajeError = "Usuario o contraseña incorrectos o cuenta inactiva.";
            return null;
        }
    }

    public String logout() {
        usuarioLogeado = null;
        username = null;
        password = null;
        return "login.xhtml?faces-redirect=true";
    }

    // Getters y Setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Usuario getUsuarioLogeado() { return usuarioLogeado; }

    public String getMensajeError() { return mensajeError; }
}