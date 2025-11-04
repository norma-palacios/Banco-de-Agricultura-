package com.udb.bancoagricultura.bancodeagriculturasalvadoreno.service;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.udb.bancoagricultura.bancodeagriculturasalvadoreno.model.pojo.Usuario;
import com.udb.bancoagricultura.bancodeagriculturasalvadoreno.model.util.JPAUtil;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

public class LoginServices {
    public Usuario validarUsuario(String username, String password) {
        EntityManager em = JPAUtil.getEntityManager();

        try {
            // Consulta JPQL (no SQL)
            TypedQuery<Usuario> query = em.createQuery(
                    "SELECT u FROM Usuario u WHERE u.username = :username AND u.estado = 'ACTIVO'",
                    Usuario.class
            );
            query.setParameter("username", username);

            Usuario usuario = query.getSingleResult();

            // Compara la contraseña
            if (usuario != null && BCrypt.verifyer().verify(password.toCharArray(), usuario.getPasswordHash()).verified){
                return usuario;
            }
        } catch (NoResultException e) {
            // No existe el usuario
            return null;
        } finally {
            em.close();
        }

        return null;
    }
}
