package com.udb.bancoagricultura.bancodeagriculturasalvadoreno.service;

import com.udb.bancoagricultura.bancodeagriculturasalvadoreno.model.pojo.*;
import com.udb.bancoagricultura.bancodeagriculturasalvadoreno.model.util.JPAUtil;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

public class DependienteService {

    // La comisión es del 5%
    private static final BigDecimal TASA_COMISION = new BigDecimal("0.05");

    // IDs de Tipos de Movimiento (DEBES VERIFICAR O CREAR ESTOS EN TU BD)
    private static final int ID_TIPO_DEPOSITO_DEPENDIENTE = 3; // Asumido
    private static final int ID_TIPO_RETIRO_DEPENDIENTE = 4;   // Asumido

    /**
     * Busca una Persona por su DUI.
     */
    public Persona buscarPersonaPorDui(String dui) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            TypedQuery<Persona> query = em.createQuery(
                    "SELECT p FROM Persona p WHERE p.dui = :dui", Persona.class);
            query.setParameter("dui", dui);
            return query.getSingleResult();
        } catch (NoResultException e) {
            return null; // No encontrado
        } finally {
            em.close();
        }
    }

    /**
     * Busca una Persona por su ID (para obtener los datos del cliente).
     */
    public Persona buscarPersonaPorId(int idPersona) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.find(Persona.class, idPersona);
        } finally {
            em.close();
        }
    }

    /**
     * Busca un Usuario de tipo CLIENTE a partir de un objeto Persona.
     */
    public Usuario buscarClientePorDui(String dui) {
        Persona persona = buscarPersonaPorDui(dui);
        if (persona == null) {
            return null; // Si no hay persona, no hay cliente
        }

        EntityManager em = JPAUtil.getEntityManager();
        try {
            TypedQuery<Usuario> query = em.createQuery(
                    "SELECT u FROM Usuario u WHERE u.idPersona = :idPersona AND u.tipoUsuario = 'CLIENTE'", Usuario.class);
            query.setParameter("idPersona", persona.getIdPersona());
            return query.getSingleResult();
        } catch (NoResultException e) {
            return null; // Persona existe pero no es un usuario CLIENTE
        } finally {
            em.close();
        }
    }

    /**
     * Obtiene todas las cuentas activas de un Usuario.
     */
    public List<CuentaBancaria> buscarCuentasPorUsuario(Usuario usuario) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            TypedQuery<CuentaBancaria> query = em.createQuery(
                    "SELECT c FROM CuentaBancaria c WHERE c.cliente = :usuario AND c.estado = 'ACTIVA'",
                    CuentaBancaria.class);
            query.setParameter("usuario", usuario);
            return query.getResultList();
        } finally {
            em.close();
        }
    }

    /**
     * Realiza un depósito (abono) a una cuenta. Cobra una comisión del 5% sobre
     * el monto.
     */
    public CuentaBancaria realizarDeposito(Usuario dependiente, int idCuentaDestino, BigDecimal monto) throws Exception {
        EntityManager em = JPAUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();

            // 1. Encontrar la cuenta de destino
            CuentaBancaria cuentaDestino = em.find(CuentaBancaria.class, idCuentaDestino);
            if (cuentaDestino == null) {
                throw new Exception("La cuenta de destino no existe.");
            }

            // 2. Encontrar el Tipo de Movimiento (Depósito)
            TipoMovimiento tipoMov = em.find(TipoMovimiento.class, ID_TIPO_DEPOSITO_DEPENDIENTE);
            if (tipoMov == null) {
                throw new Exception("Error de configuración: Tipo de movimiento 'DEPOSITO_DEPENDIENTE' no encontrado.");
            }

            // 3. Calcular comisión y montos
            BigDecimal comision = monto.multiply(TASA_COMISION);
            BigDecimal montoNeto = monto.subtract(comision); // Al cliente le llega el monto menos la comisión

            // 4. Actualizar saldo de la cuenta destino
            BigDecimal saldoAnterior = cuentaDestino.getSaldo();
            cuentaDestino.setSaldo(saldoAnterior.add(montoNeto));
            em.merge(cuentaDestino);

            // 5. Crear y guardar el movimiento
            Movimiento mov = new Movimiento();
            mov.setCuentaDestino(cuentaDestino);
            mov.setTipoMovimiento(tipoMov);
            mov.setMonto(montoNeto); // El monto del movimiento es el neto
            mov.setComision(comision); // Guardamos la comisión
            mov.setDependiente(dependiente); // Guardamos quién hizo la op            mov.setFechaMovimiento(new Date());
            mov.setDescripcion("Depósito en comercio");

            em.persist(mov);

            tx.commit();
            return cuentaDestino; // Devuelve la cuenta con el saldo actualizado

        } catch (Exception e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw e; // Lanza la excepción para que el Bean la atrape
        } finally {
            em.close();
        }
    }

    /**
     * Realiza un retiro de una cuenta. Cobra una comisión del 5% (se suma al
     * monto a retirar).
     */
    public CuentaBancaria realizarRetiro(Usuario dependiente, int idCuentaOrigen, BigDecimal monto) throws Exception {
        EntityManager em = JPAUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();

            // 1. Encontrar la cuenta de origen
            CuentaBancaria cuentaOrigen = em.find(CuentaBancaria.class, idCuentaOrigen);
            if (cuentaOrigen == null) {
                throw new Exception("La cuenta de origen no existe.");
            }

            // 2. Encontrar el Tipo de Movimiento (Retiro)
            TipoMovimiento tipoMov = em.find(TipoMovimiento.class, ID_TIPO_RETIRO_DEPENDIENTE);
            if (tipoMov == null) {
                throw new Exception("Error de configuración: Tipo de movimiento 'RETIRO_DEPENDIENTE' no encontrado.");
            }

            // 3. Calcular comisión y montos
            BigDecimal comision = monto.multiply(TASA_COMISION);
            BigDecimal montoTotalDebitar = monto.add(comision); // Se le cobra al cliente el monto + la comisión

            // 4. Validar fondos
            if (cuentaOrigen.getSaldo().compareTo(montoTotalDebitar) < 0) {
                throw new Exception("Fondos insuficientes. El cliente no tiene saldo para cubrir el retiro más la comisión.");
            }

            // 5. Actualizar saldo de la cuenta origen
            BigDecimal saldoAnterior = cuentaOrigen.getSaldo();
            cuentaOrigen.setSaldo(saldoAnterior.subtract(montoTotalDebitar));
            em.merge(cuentaOrigen);

            // 6. Crear y guardar el movimiento
            Movimiento mov = new Movimiento();
            mov.setCuentaOrigen(cuentaOrigen);
            mov.setTipoMovimiento(tipoMov);
            mov.setMonto(monto); // El monto del movimiento es lo que el cliente retira
            mov.setComision(comision); // Guardamos la comisión
            mov.setDependiente(dependiente); // Guardamos quién hizo la op            mov.setFechaMovimiento(new Date());
            mov.setDescripcion("Retiro en comercio");

            em.persist(mov);

            tx.commit();
            return cuentaOrigen; // Devuelve la cuenta con el saldo actualizado

        } catch (Exception e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw e; // Lanza la excepción para que el Bean la atrape
        } finally {
            em.close();
        }
    }
}
