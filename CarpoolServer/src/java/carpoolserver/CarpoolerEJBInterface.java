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
  
  public int getUserId(String username);
  
  public JSONObject getUserList(User forUser);
  
  public void updateFromUser(User user);  
  
  public void updatePoints(int user_id, int points);    
  
  public void updateLocation(int user_id, double lat, double lng);
  
  public void updateDestination(int user_id, double lat, double lng);
  
  public void updateProximity(int user_id, double proximity);
  
//  public void updateUser(User user);
  public void updateStatus(int user_id, int status);  
  
  public void updateTransactionId(int user_id, int transaction_id);  


  public int getDriverId(int transaction_id);
  
  public int newPendingTransaction(int driver_id, int passenger_id);
  
  public void cancelPendingTransaction(int transaction_id);
  
  public void setTransactionInProgress(int transaction_id, long dt, double lat, double lng);
  
  public void setTransactionCompleted(int transaction_id, long dt, double lat, double lng);
  
}
