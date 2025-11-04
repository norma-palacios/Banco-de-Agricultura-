package com.udb.bancoagricultura.bancodeagriculturasalvadoreno.model.util;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;


public class JPAUtil {

    // El nombre "bancoAgriculturaPU" DEBE COINCIDIR con el 'persistence-unit name'
    // que definimos en persistence.xml
    private static final String PERSISTENCE_UNIT_NAME = "bancoAgriculturaPU";

    // El EntityManagerFactory es "costoso" de crear.
    // Lo creamos UNA SOLA VEZ para toda la aplicación.
    private static EntityManagerFactory factory;


    private static void initFactory() {
        try {
            if (factory == null) {
                factory = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME);
            }
        } catch (Exception e) {
            // Error grave: la aplicación no puede conectarse a la BD
            e.printStackTrace();
            throw new RuntimeException("Error al inicializar el EntityManagerFactory", e);
        }
    }


    public static EntityManager getEntityManager() {
        // Aseguramos que la fábrica esté inicializada
        initFactory();

        // Creamos y retornamos un nuevo EntityManager
        return factory.createEntityManager();
    }


    public static void shutdown() {
        if (factory != null) {
            factory.close();
        }
    }
}