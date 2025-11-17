/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.udb.bancoagricultura.bancodeagriculturasalvadoreno.model.util;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.udb.bancoagricultura.bancodeagriculturasalvadoreno.model.pojo.Usuario;
import javax.persistence.*;
import java.util.List;

/**
 *
 * @author felix
 */
public class PasswordFixer {

    public static void main(String[] args) {
        // Crea la conexión usando el nombre exacto del persistence.xml
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("bancoAgriculturaPU");
        EntityManager em = emf.createEntityManager();

        try {
            em.getTransaction().begin();

            // Busca usuarios cuyas contraseñas aún no están cifradas (menos de 20 caracteres)
            TypedQuery<Usuario> query = em.createQuery(
                "SELECT u FROM Usuario u WHERE LENGTH(u.passwordHash) < 20",
                Usuario.class
            );

            List<Usuario> usuarios = query.getResultList();

            if (usuarios.isEmpty()) {
                System.out.println("No se encontraron usuarios con contraseñas sin cifrar.");
            } else {
                for (Usuario u : usuarios) {
                    System.out.println("Actualizando usuario: " + u.getUsername());
                    String nuevaClave = BCrypt.withDefaults()
                            .hashToString(12, u.getPasswordHash().toCharArray());
                    u.setPasswordHash(nuevaClave);
                    em.merge(u);
                }
                System.out.println("Contraseñas actualizadas correctamente.");
            }

            em.getTransaction().commit();

        } catch (Exception e) {
            e.printStackTrace();
            em.getTransaction().rollback();
        } finally {
            em.close();
            emf.close();
        }
    }
}
