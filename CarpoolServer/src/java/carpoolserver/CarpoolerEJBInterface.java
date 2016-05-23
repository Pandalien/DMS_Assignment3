/*
 * Carpool Server, DMS Assignment 3
 */
package carpoolserver;

import javax.ejb.*;
import org.json.*;

/**
 *
 * @author macbookair
 */
@Remote
public interface CarpoolerEJBInterface {
  
  public void createDatabaseTablesIfNotExist();  

  public String authenticateUser(String username, String password);

  public String createNewUser(String username, String password); 
  
  public User getUser(String username);
  
  public User getUser(int user_id);
  
  public void updateStatus(int user_id, int status);  
  
  public void updatePoints(int user_id, int points);    
  
  public void updateLocation(int user_id, double lat, double lng);
  
  public void updateDestination(int user_id, double lat, double lng);
  
  public void updateProximity(int user_id, double proximity);
  
//  public void updateUser(User user);
  
  public JSONObject getUserList(User forUser);
  
}
