/*
 * Carpool Server, DMS Assignment 3
 */
package carpoolserver;

import java.io.*;
import javax.ejb.*;
import javax.naming.*;
import javax.servlet.*;
import javax.servlet.http.*;
import org.json.*;

/**
 *
 * @author macbookair
 */
public class Carpooler extends HttpServlet {
  
  @EJB private CarpoolerEJBInterface carpoolerEJB;  
  
  
  /**
   * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
   * methods.
   *
   * @param request servlet request
   * @param response servlet response
   * @throws ServletException if a servlet-specific error occurs
   * @throws IOException if an I/O error occurs
   */
  protected void processRequest(HttpServletRequest request, HttpServletResponse response)
          throws ServletException, IOException {
    
    // Check for a valid session and call the CarpoolerSessionBean EJB for the biz logic,
    // and return response.
    // If no valid session, one can be created after a call to verifyUser or createNewUser.
    // Basic protocol:
    // jsonRequest must have at least one field in the root: "function": <string>
    // jsonResponse must have at least one field in the root: "result": <string>
    // callers/receivers may of course add any number of extra fields or structures as required.
    // http://www.docjar.com/docs/api/org/json/JSONObject.html

    String result = "OK";
    
    // parameters are extracted from the jsonRequest object
    JSONObject jsonRequest = null; 
    
    // create json object from post request body:
    try {
      BufferedReader reader = request.getReader();
      StringBuilder responsebody = new StringBuilder();
      String line;
      while ((line = reader.readLine()) != null)
          responsebody.append(line);
      jsonRequest = new JSONObject(responsebody.toString());
    }
    catch (Exception e) {
      // report exception back to the client (for debugging only)
      result = "Server Exception: " + e.getMessage();
    }
    
    // response JSON object
    JSONObject jsonResponse = new JSONObject();        
    
/*    
    // get the carpooler stateful ejb
    // if no valid http session, check for login or create new user
    HttpSession session = request.getSession(true);
    carpoolerEJB = (CarpoolerEJBInterface)session.getAttribute("CarpoolerSessionBean");      
    if (carpoolerEJB == null) {
      try {
        InitialContext ic = new InitialContext();
        carpoolerEJB = (CarpoolerEJBInterface)ic.lookup(CarpoolerEJBInterface.class.getName());
        session.setAttribute("CarpoolerSessionBean", carpoolerEJB);         
      }
      catch (Exception e) {
        result = "Server Exception: " + e.getMessage();
      }
    }
*/

    if (carpoolerEJB == null) {
      result = "Server Error: Couldn't get EJB!";
    }
    // If valid request, call the named function
    else if (jsonRequest != null) {
    
      HttpSession session = request.getSession(true);      

      int user_id = 0;
      User user = null;      
//        user = (User)session.getAttribute("user");

      // only the user_id is stored as the session attribute 
      // (this value is fixed to the user and does not change during the session)
      Integer user_id_object = (Integer)session.getAttribute("user_id");
      if (user_id_object != null) {
        user_id = user_id_object.intValue();
        user = carpoolerEJB.getUser(user_id);
      }
      
      String function = jsonRequest.optString("function", "");
      if (function.equals("createaccount") || function.equals("login")) {
        String username = jsonRequest.optString("username", "");
        String password = jsonRequest.optString("password", "");
        int usertype = jsonRequest.optInt("usertype", User.OFFLINE);
        double lat = jsonRequest.optDouble("lat", 0);
        double lng = jsonRequest.optDouble("lng", 0);
        double dest_lat = jsonRequest.optDouble("dest_lat", 0);
        double dest_lng = jsonRequest.optDouble("dest_lng", 0);
        double proximity = jsonRequest.optDouble("proximity", 1);
        // verify inputs:
        if (username == null || password == null || username.length() == 0 || password.length() == 0) 
          result = "Username or password cannot be empty.";
        else if (!username.matches("^[a-zA-Z0-9_]+$")) 
          result = "Illegal characters in username (a-z, 0_9 and _ only).";
        else {
          // verification OK
          if (function.equals("createaccount"))
            result = carpoolerEJB.createNewUser(username, password);
          else
            result = carpoolerEJB.authenticateUser(username, password);            
          if (result == null) { // null result = no error

            user = carpoolerEJB.getUser(username);
            user_id = user.getUserID();
            // session user_id is set and created once here and does not change 
            // for the remainder of the session
            session.setAttribute("user_id", new Integer(user_id));             
            
            user.setStatus(usertype);
            user.setLatLng(lat, lng);
            user.setDestLatLng(dest_lat, dest_lng);
            user.setProximity(proximity);
            carpoolerEJB.updateFromUser(user);            
        
            try {
              // send back all this user's details at login
              jsonResponse.put("user", user.toJSONObject());
              // send back user list (this is refreshed by locationupdate below)
              jsonResponse.put("userlist", carpoolerEJB.getUserList(user));
            }
            catch (Exception e) {
              System.err.println(e.getMessage());            
            }
            
          }
        }
      }
      else if (user != null) { // a valid user session bean must have been acquired for these next functions:
        
        if (function.equals("logout")) {

//          carpoolerEJB.updateStatus(user.getUserID(), User.OFFLINE);
          carpoolerEJB.updateStatus(user_id, User.OFFLINE);
          session.invalidate();
          
        }        
        else if (function.equals("locationupdate")) {
          
          double lat = jsonRequest.optDouble("lat", 0);
          double lng = jsonRequest.optDouble("lng", 0);
//          carpoolerEJB.updateLocation(user.getUserID(), lat, lng);
          carpoolerEJB.updateLocation(user_id, lat, lng);
          
          // update response tailered for passenger or driver
          // IN ADDITION:
          // PASSENGER must receive the DRIVER_USERNAME for a pending lift -
          // This is the information that must match the NFC or QRcode on the
          // Android device before it transmits a COLLECTED message.
          try {
            // this is a continous update, because the other party may cancel the transaction
            
            // if the session is for a passenger with a pending, collected or completed status:
            if (user.getStatus() > User.PASSENGER) {
              // find the driver from the transaction, and return with response:
              int transaction_id = carpoolerEJB.findTransactionId(user.getStatus(), user.getUserID());
              int driver_id = carpoolerEJB.getDriverId(transaction_id);
              User driver = carpoolerEJB.getUser(driver_id);
              if (driver != null)
                jsonResponse.put("driver", driver.toJSONObject()); 
              jsonResponse.put("status", user.getStatus());
            }
            jsonResponse.put("userlist", carpoolerEJB.getUserList(user));
          }
          catch (Exception e) {
            System.err.println(e.getMessage());            
          }
          
        }
        // next are Transaction updates:
        else if (function.equals("pending")) {
          
          // sent by driver's phone
          // this indicates to the system that no other driver should collect this passenger         
          int passenger_id = jsonRequest.optInt("passenger_id", 0);
          if (passenger_id > 0) {
            carpoolerEJB.newPendingTransaction(user_id, passenger_id);            
            carpoolerEJB.updateStatus(passenger_id, User.PASSENGER_PENDING);                                   
          }
         
          try {
            jsonResponse.put("userlist", carpoolerEJB.getUserList(user));
          }
          catch (Exception e) {
            System.err.println(e.getMessage());            
          }
          
        }
        else if (function.equals("cancelled")) {
          
          // may be sent by either driver or passenger
          // cancels the pending transaction

          int other_user_id = jsonRequest.optInt("other_user_id", 0);
          int passenger_id = user.isDriver() ? other_user_id : user.getUserID();
          if (passenger_id > 0) {
            int transaction_id = carpoolerEJB.findTransactionId(
                      User.PASSENGER_PENDING, 
                      passenger_id
              );
            if (transaction_id > 0) 
              carpoolerEJB.cancelPendingTransaction(transaction_id);  
          }
          
          try {
            jsonResponse.put("userlist", carpoolerEJB.getUserList(user));
          }
          catch (Exception e) {
            System.err.println(e.getMessage());            
          }         
          
        }
        else if (function.equals("collected")) {
          
          // sent by passenger's phone
          // driver_id is read from NFC tag (or QR code)
          
          // lookup transaction_id
          int transaction_id = carpoolerEJB.findTransactionId(
                    User.PASSENGER_PENDING, 
                    user_id
            );          

 //         int driver_id = jsonRequest.optInt("driver_id", 0);
          double lat = jsonRequest.optDouble("lat", 0);
          double lng = jsonRequest.optDouble("lng", 0);   
//          if (driver_id > 0) {          
            carpoolerEJB.updateStatus(user_id, User.PASSENGER_COLLECTED);                  
            if (transaction_id > 0) {
              long dt = new java.util.Date().getTime(); // unix timestamp
              carpoolerEJB.setTransactionInProgress(transaction_id, dt, lat, lng);
            }
//          }
          
        }
        else if (function.equals("completed")) {
          
          // sent by passenger's phone        
          // driver_id is read from NFC tag (or QR code)        
          // (optionally this could also be sent by the driver's phone if clicking the End button?).
          
          // lookup transaction_id
          int transaction_id = carpoolerEJB.findTransactionId(
                    User.PASSENGER_COLLECTED, 
                    user_id
            );                              
          
 //         int driver_id = jsonRequest.optInt("driver_id", 0);          
          double lat = jsonRequest.optDouble("lat", 0);
          double lng = jsonRequest.optDouble("lng", 0);                    
//          if (driver_id > 0) {         
            carpoolerEJB.updateStatus(user_id, User.PASSENGER_COMPLETED);   
            if (transaction_id > 0) {
              long dt = new java.util.Date().getTime(); // unix timestamp
              carpoolerEJB.setTransactionCompleted(transaction_id, dt, lat, lng);  
            }
//          }
          
        }

      }
      else
        result = "No function.";
      
    }

    // place result message
    try {
      if (result == null)
        result = "OK";
      jsonResponse.put("result", result);        
    }
    catch (JSONException e) {
      System.err.println(e.getMessage());
    }
    
    // write JSON object out as the http response     
    response.setContentType("application/json;charset=UTF-8");   
    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(response.getOutputStream(), "UTF-8"));
    writer.write(jsonResponse.toString());
    writer.close();
  } // Carpooler
  
  
  // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
  /**
   * Handles the HTTP <code>GET</code> method.
   *
   * @param request servlet request
   * @param response servlet response
   * @throws ServletException if a servlet-specific error occurs
   * @throws IOException if an I/O error occurs
   */
  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
          throws ServletException, IOException {
    processRequest(request, response);
  }

  /**
   * Handles the HTTP <code>POST</code> method.
   *
   * @param request servlet request
   * @param response servlet response
   * @throws ServletException if a servlet-specific error occurs
   * @throws IOException if an I/O error occurs
   */
  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
          throws ServletException, IOException {
    processRequest(request, response);
  }

  /**
   * Returns a short description of the servlet.
   *
   * @return a String containing servlet description
   */
  @Override
  public String getServletInfo() {
    return "Short description";
  }// </editor-fold>

}