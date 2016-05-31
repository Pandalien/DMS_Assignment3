/*
 * Carpool Server, DMS Assignment 3
 */
package models;

import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import models.Transaction;

/**
 *
 * @author andyc
 */
@Stateless
public class TransactionFacade extends AbstractFacade<Transaction> {

    @PersistenceContext(unitName = "CarpoolServerPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public TransactionFacade() {
        super(Transaction.class);
    }
    
    public List<Transaction> findByPassengerId(Object id){
        Query q = em.createNamedQuery("Transaction.findByPassengerId");
        q.setParameter("passengerId", id);
        return q.getResultList();
    }
    
    public List<Transaction> findByDriverId(Object id){
        Query q = em.createNamedQuery("Transaction.findByDriverId");
        q.setParameter("driverId", id);
        return q.getResultList();
    }
    
    public List<Transaction> findByUserId(Object id){
        Query q = em.createNamedQuery("Transaction.findByUserId");
        q.setParameter("userId", id);
        return q.getResultList();
    }
}
