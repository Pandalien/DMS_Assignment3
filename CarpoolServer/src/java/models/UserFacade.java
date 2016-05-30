/*
 * Carpool Server, DMS Assignment 3
 */
package models;

import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

/**
 *
 * @author Administrator
 */
@Stateless
public class UserFacade extends AbstractFacade<User> {

    @PersistenceContext(unitName = "CarpoolServerPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public UserFacade() {
        super(User.class);
    }
    
    public User findByUsernameAndPassword(Object username, Object password){
        Query q = em.createNamedQuery("User.findByUsernameAndPassword");
        q.setParameter("username", username);
        q.setParameter("password", password);
        
        List<User> users = q.getResultList();
        
        if (users != null && users.size() > 0) {
            return users.get(0);
        }else{
            return null;
        }
    }
}
