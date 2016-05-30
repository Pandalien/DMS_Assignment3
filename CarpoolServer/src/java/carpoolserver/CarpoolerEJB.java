/*
 * Carpool Server, DMS Assignment 3
 */
package carpoolserver;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.ArrayList.*;
import javax.ejb.*;
import javax.annotation.*;
import org.json.*;

/**
 *
 * @author macbookair
 */
@Stateless
public class CarpoolerEJB implements CarpoolerEJBInterface {
  
  private final String config_file = "config-local.xml"; // "config-online.xml"

  protected Connection connection;  
  protected String dbName;
  protected String userTableName;
  protected String transactionTableName;
  
  
  public CarpoolerEJB() {

  } // CarpoolerEJB
  
  // @PostActivate & @PrePassivate - for Stateful EJBs
  // @PostConstruct & @PreDestroy - for Stateless EJBs
  // https://docs.oracle.com/javaee/6/tutorial/doc/giplj.html
  // http://stackoverflow.com/questions/14949316/stateful-bean-using-predestroy-to-close-database-connection
  
//  @PostActivate
//  public void postActivate() {
  @PostConstruct
  public void postConstruct() {
    // load bean properties file
    Properties config = new Properties();
    try {
      config.loadFromXML(getClass().getResourceAsStream(config_file));
 
      String dbDriver = config.getProperty("dbDriver");
      String dbURL = config.getProperty("dbURL");
      dbName = config.getProperty("dbName");     
      String dbUser = config.getProperty("dbUser");
      String dbPassword = config.getProperty("dbPassword");    
      userTableName = config.getProperty("userTableName");
      transactionTableName = config.getProperty("transactionTableName");      

      // connect to database        
      Class.forName(dbDriver);
      connection = DriverManager.getConnection(dbURL, dbUser, dbPassword);    
    }
    catch (Exception e) {
      System.err.println(e.getMessage());
    }
  } // postConstruct
  
  
//  @PrePassivate
//  public void prePassivate() {
  @PreDestroy
  public void preDestroy() {
    try {
      connection.close();
    }
    catch (SQLException e) {
      System.err.println(e);
    }
  } // preDestroy
  
  
  public void createDatabaseTablesIfNotExist() {
    
    // creates the database and tables - if they do not already exist.
    // in this system we call it once when creating a new user account.
    
    String sql = new String();
      
    try {
      Statement statement = connection.createStatement();
        
      // create database if not exists, and use it:
      sql = "create database if not exists " + dbName + ";";
      statement.executeUpdate(sql);
        
      // create user table if not exists:
      sql = "create table if not exists " + dbName + "." + userTableName + " (" 
          + " user_id    int not null auto_increment"
          + ",username   varchar(100) not null" 
          + ",password   varchar(100) not null" 
          + ",points     int not null default 0" 
          + ",status     int not null default 0"              
          + ",lat        double"
          + ",lng        double"
          + ",dest_lat   double" 
          + ",dest_lng   double"  
          + ",proximity  double"
          + ",transaction_id int default 0" // only used for a passengers
          + ",primary key (user_id)"
          + ");";
      statement.executeUpdate(sql);
      
      // create transaction table if not exists:
      sql = "create table if not exists " + dbName + "." + transactionTableName + " ("
          + " transaction_id int not null auto_increment"
          + ",driver_id      int not null"
          + ",passenger_id   int not null"
          + ",status         int not null default 0"    // status: pending, in progress, completed
          + ",collected_dt   datetime"        
          + ",collected_lat  double"
          + ",collected_lng  double"              
          + ",completed_dt   datetime"
          + ",completed_lat  double"
          + ",completed_lng  double"
          + ",primary key (transaction_id)"
          + ",constraint fk_driver_id foreign key (driver_id) references " + dbName + "." + userTableName + "(user_id)"
          + ",constraint fk_passenger_id foreign key (passenger_id) references " + dbName + "." + userTableName + "(user_id)"              
          + ");";
      statement.executeUpdate(sql);      
      
    }
    catch (SQLException e) {
      System.err.println(e.getMessage() + ". Last statement: " + sql);
    }    
    
  } // createDatabaseTablesIfNotExist
  
  
  public String authenticateUser(String username, String password) {
    // null result means no error
    String result = null; 
    String query = new String();
    try {
      PreparedStatement preparedStatement;
      // check username is in the table and the password matches:      
      query = "select * from " + dbName + "." + userTableName 
            + " where username = ? ";
      preparedStatement = connection.prepareStatement(query);
      preparedStatement.setString(1, username);
      ResultSet resultSet = preparedStatement.executeQuery();
      if (!resultSet.next())
        result = "User does not exist.";
      else if (!resultSet.getString("password").equals(password))
        result = "Incorrect password.";
    }
    catch (Exception e) {
      result = "An error occurred while trying to verify the user.";
      System.err.println(e.getMessage());            
    }
    return result;
  } // authenticateUser
  
  
  public String createNewUser(String username, String password) {
    createDatabaseTablesIfNotExist();
    
    // null result means no error
    String result = null;
    String query = new String();
    try {
      PreparedStatement preparedStatement;
      // check if username is in the table:
      query = "select * from " + dbName + "." + userTableName 
            + " where username = ?;";
      preparedStatement = connection.prepareStatement(query);
      preparedStatement.setString(1, username);
      ResultSet resultSet = preparedStatement.executeQuery();
      if (resultSet.next()) 
        result = "User already exists.";
      else {
        // username not in table, so create a new entry:
        query = "insert into " + dbName + "." + userTableName 
              + " (username, password) values (?, ?);";
        preparedStatement = connection.prepareStatement(query);        
        preparedStatement.setString(1, username);
        preparedStatement.setString(2, password);        
        if (preparedStatement.executeUpdate() != 1)
          result = "Could not add user to table.";
      }
    }
    catch (Exception e) {
      result = "An error occurred while trying to create a new user.";
      System.err.println(e.getMessage());      
    }
    return result;
  } // createNewUser
  
  
  public User getUser(String username) {
    User user = null;
    try {
      PreparedStatement preparedStatement;
      String query = "select * from " + dbName + "." + userTableName 
                   + " where username = ? ";
      preparedStatement = connection.prepareStatement(query);
      preparedStatement.setString(1, username);
      ResultSet resultSet = preparedStatement.executeQuery();
      if (resultSet.next()) 
        user = getUserFromResultSet(resultSet);
    }
    catch (Exception e) {
      System.err.println(e.getMessage());            
    }
    return user;
  } // getUser
  
  
  public User getUser(int user_id) {
    User user = null;
    try {
      PreparedStatement preparedStatement;
      String query = "select * from " + dbName + "." + userTableName 
                   + " where user_id = ?";
      preparedStatement = connection.prepareStatement(query);
      preparedStatement.setInt(1, user_id);
      ResultSet resultSet = preparedStatement.executeQuery();
      if (resultSet.next()) 
        user = getUserFromResultSet(resultSet);
    }
    catch (Exception e) {
      System.err.println(e.getMessage());            
    }
    return user;
  } // getUser
  
  
  public int getUserId(String username) {  
    int result = 0;
    try {
      PreparedStatement preparedStatement;
      String query = "select user_id from " + dbName + "." + userTableName 
                   + " where username = ?";
      preparedStatement = connection.prepareStatement(query);
      preparedStatement.setString(1, username);
      ResultSet resultSet = preparedStatement.executeQuery();
      if (resultSet.next()) 
        result = resultSet.getInt("username");
    }
    catch (Exception e) {
      System.err.println(e.getMessage());            
    }
    return result;
  } // getUserId
  
  
  private User getUserFromResultSet(ResultSet resultSet) {
    User user = new User();    
    try {
      user.setUserID(resultSet.getInt("user_id"));
      user.setUsername(resultSet.getString("username"));
      user.setPoints(resultSet.getInt("points"));                
      user.setStatus(resultSet.getInt("status"));
      user.setLatLng(resultSet.getDouble("lat"), resultSet.getDouble("lng"));
      user.setDestLatLng(resultSet.getDouble("dest_lat"), resultSet.getDouble("dest_lng"));
      user.setProximity(resultSet.getDouble("proximity"));
      user.setTransactionId(resultSet.getInt("transaction_id"));
    }
    catch (Exception e) {
      System.err.println(e.getMessage());      
    }
    return user;
  } // getUserFromResultSet
  
  
  public JSONObject getUserList(User forUser) {
    // for this user, return list of available passengers or drivers in range.
    // sort list by points
    
    // list needs to be tailored for each person
    // * driver sees all passengers, and only passengers where they are part
    // of a pending or inprogress transaction
    // (so they must be the driver_id for the passenger's transaction_id)
    // * passenger receives a list of all drivers in the area for info purposes only
    
    // first, get the user list as an array of User objects
    ArrayList<User> userlist = new ArrayList();    
    try {
      PreparedStatement preparedStatement = null;
      String query = "";
      if (forUser.getStatus() == User.DRIVER) {
        query = "select *"
              + " from " + dbName + "." + userTableName + " u"
              + "     ," + dbName + "." + transactionTableName + " t"
              + " where u.user_id != ?"
              + " and ((u.status = ?) or"              
              + "      (u.status in (?, ?, ?) and"
              + "       t.transaction_id = u.transaction_id"
              + "       and t.driver_id = ?)"
              + "     );";
        preparedStatement = connection.prepareStatement(query);
        preparedStatement.setInt(1, forUser.getUserID());
        preparedStatement.setInt(2, User.PASSENGER);
        preparedStatement.setInt(3, User.PASSENGER_PENDING);        
        preparedStatement.setInt(4, User.PASSENGER_COLLECTED);        
        preparedStatement.setInt(5, User.PASSENGER_COMPLETED);                
        preparedStatement.setInt(6, forUser.getUserID());                        
      }
      else if (forUser.getStatus() == User.PASSENGER) {
        query = "select * from " + dbName + "." + userTableName
              + " where user_id != ?"
              + " and status = ?;";
        preparedStatement = connection.prepareStatement(query);
        preparedStatement.setInt(1, forUser.getUserID());
        preparedStatement.setInt(2, User.DRIVER);        
      }

      ResultSet resultSet = preparedStatement.executeQuery();
      while (resultSet.next()) 
        userlist.add(getUserFromResultSet(resultSet));
    }
    catch (Exception e) {
      System.err.println(e.getMessage());
    }
    
    // second, send back the json object
    JSONObject jsonUserList = new JSONObject();
    for (User user: userlist) {
      try {
        jsonUserList.put(user.getUsername(), user.toJSONObject());
      }
      catch (Exception e) {
        System.err.println(e.getMessage());
      }
    }
    
    return jsonUserList;
  } // getUserList
  
  
  public void updateFromUser(User user) {
    try {
      PreparedStatement preparedStatement;
      String query = "update " + dbName + "." + userTableName 
                   + " set points = ?"
                   + "    ,status = ?"
                   + "    ,lat = ?"
                   + "    ,lng = ?"
                   + "    ,dest_lat = ?"
                   + "    ,dest_lng = ?"
                   + "    ,proximity = ?"
                   + "    ,transaction_id = ?"
                   + " where user_id = ?;";      
      preparedStatement = connection.prepareStatement(query);
      preparedStatement.setInt(1, user.getPoints());      
      preparedStatement.setInt(2, user.getStatus());
      preparedStatement.setDouble(3, user.getLat());
      preparedStatement.setDouble(4, user.getLng());      
      preparedStatement.setDouble(5, user.getDestLat());
      preparedStatement.setDouble(6, user.getDestLng());      
      preparedStatement.setDouble(7, user.getProximity());      
      preparedStatement.setInt(8, user.getTransactionId());            
      preparedStatement.setInt(9, user.getUserID());      
      preparedStatement.executeUpdate();
    }
    catch (Exception e) {
      System.err.println(e.getMessage());                  
    }
  } // updateFromUser

  
  public void updatePoints(int user_id, int points) {  
    try {
      PreparedStatement preparedStatement;
      String query = "update " + dbName + "." + userTableName 
                   + " set points = ?"
                   + " where user_id = ?;";      
      preparedStatement = connection.prepareStatement(query);
      preparedStatement.setInt(1, points);      
      preparedStatement.setInt(2, user_id);
      preparedStatement.executeUpdate();
    }
    catch (Exception e) {
      System.err.println(e.getMessage());                  
    }
  } // updatePoints
  
  public void updateLocation(int user_id, double lat, double lng) {
    try {
      PreparedStatement preparedStatement;
      String query = "update " + dbName + "." + userTableName 
                   + " set lat = ?,lng = ?"
                   + " where user_id = ?;";      
      preparedStatement = connection.prepareStatement(query);
      preparedStatement.setDouble(1, lat);      
      preparedStatement.setDouble(2, lng);            
      preparedStatement.setInt(3, user_id);
      preparedStatement.executeUpdate();
    }
    catch (Exception e) {
      System.err.println(e.getMessage());                  
    }
  } // updateLocation
  
  public void updateDestination(int user_id, double dest_lat, double dest_lng) {
    try {
      PreparedStatement preparedStatement;
      String query = "update " + dbName + "." + userTableName 
                   + " set dest_lat = ?, dest_lng = ?"
                   + " where user_id = ?;";      
      preparedStatement = connection.prepareStatement(query);
      preparedStatement.setDouble(1, dest_lat);      
      preparedStatement.setDouble(2, dest_lng);            
      preparedStatement.setInt(3, user_id);
      preparedStatement.executeUpdate();
    }
    catch (Exception e) {
      System.err.println(e.getMessage());                  
    }
  } // updateDestination
  
  public void updateProximity(int user_id, double proximity) {  
    try {
      PreparedStatement preparedStatement;
      String query = "update " + dbName + "." + userTableName 
                   + " set proximity = ?"
                   + " where user_id = ?;";      
      preparedStatement = connection.prepareStatement(query);
      preparedStatement.setDouble(1, proximity);      
      preparedStatement.setInt(2, user_id);
      preparedStatement.executeUpdate();
    }
    catch (Exception e) {
      System.err.println(e.getMessage());                  
    }
  } // updateProximity
  
  
  public void updateStatus(int user_id, int status) {  
    try {
      PreparedStatement preparedStatement;
      String query = "update " + dbName + "." + userTableName 
                   + " set status = ?"
                   + " where user_id = ?;";      
      preparedStatement = connection.prepareStatement(query);
      preparedStatement.setInt(1, status);      
      preparedStatement.setInt(2, user_id);
      preparedStatement.executeUpdate();
    }
    catch (Exception e) {
      System.err.println(e.getMessage());                  
    }
  } // updateStatus
  
  
  public void updateTransactionId(int user_id, int transaction_id) {  
    try {
      PreparedStatement preparedStatement;
      String query = "update " + dbName + "." + userTableName 
                   + " set transaction_id = ?"
                   + " where user_id = ?;";       
      preparedStatement = connection.prepareStatement(query);
      preparedStatement.setInt(1, transaction_id);      
      preparedStatement.setInt(2, user_id);
      preparedStatement.executeUpdate();
    }
    catch (Exception e) {
      System.err.println(e.getMessage());                  
    }
  } // updateTransactionId
  


  public int getDriverId(int transaction_id) {
    int result = 0;
    try {
      PreparedStatement preparedStatement;
      String query = "select * from " + dbName + "." + transactionTableName 
                   + " where transaction_id = ?;";      
      preparedStatement = connection.prepareStatement(query);
      preparedStatement.setInt(1, transaction_id);      
      ResultSet resultSet = preparedStatement.executeQuery();
      if (resultSet.next()) 
        result = resultSet.getInt("driver_id");

    }
    catch (Exception e) {
      System.err.println(e.getMessage());                  
    }
    return result;
  } // getDriverId
  
  
  
  public int newPendingTransaction(int driver_id, int passenger_id) {  
    // returns new transaction_id
    // 0 if an error
    int result = 0;     
    String query = new String();
    try {
      PreparedStatement preparedStatement;
      query = "insert into " + dbName + "." + transactionTableName 
            + " (driver_id, passenger_id, status)"
            + " values (?, ?, ?);";
      preparedStatement = connection.prepareStatement(query);        
      preparedStatement.setInt(1, driver_id);
      preparedStatement.setInt(2, passenger_id);        
      preparedStatement.setInt(3, User.PASSENGER_PENDING);
      if (preparedStatement.executeUpdate() == 1) {
        query = "select last_insert_id();";
        ResultSet resultSet = connection.createStatement().executeQuery(query);
        if (resultSet.next())
          result = resultSet.getInt(1);
      }
    }
    catch (Exception e) {
      System.err.println(e.getMessage());      
    }
    return result;
  } // newTransaction
  
  
  public void cancelPendingTransaction(int transaction_id) {
    try {
      
      PreparedStatement preparedStatement;      
      String query;

      // clear the transaction value for the passenger
      // and set status back "passenger"
      int passenger_id = 0;
      query = "select passenger_id from " + dbName + "." + transactionTableName 
            + " where transaction_id = ?;";      
      preparedStatement = connection.prepareStatement(query);
      preparedStatement.setInt(1, transaction_id);      
      ResultSet resultSet = preparedStatement.executeQuery();
      if (resultSet.next()) 
        passenger_id = resultSet.getInt("passenger_id");      
      if (passenger_id > 0) {
        updateTransactionId(passenger_id, 0);
        updateStatus(passenger_id, User.PASSENGER);
      }
      
      // remove transaction from the table
      // (but could be left there marked as cancelled also)
      query 
        = "delete from " + dbName + "." + transactionTableName 
        + " where transaction_id = ?;";      
      preparedStatement = connection.prepareStatement(query);
      preparedStatement.setInt(1, transaction_id);
      preparedStatement.executeUpdate();      

    }
    catch (Exception e) {
      System.err.println(e.getMessage());                  
    }    
  } // cancelPendingTransaction

  
  public void setTransactionInProgress(int transaction_id, long dt, double lat, double lng) {
    try {
      PreparedStatement preparedStatement;
      String query 
        = "update " + dbName + "." + transactionTableName 
        + " set status = ?, collected_dt = ?, collected_lat = ?, collected_lng = ?"
        + " where transaction_id = ?;";      
      preparedStatement = connection.prepareStatement(query);
      preparedStatement.setInt(1, User.PASSENGER_COLLECTED);
      preparedStatement.setLong(2, dt);
      preparedStatement.setDouble(3, lat);      
      preparedStatement.setDouble(4, lng);            
      preparedStatement.setInt(5, transaction_id);
      preparedStatement.executeUpdate();
    }
    catch (Exception e) {
      System.err.println(e.getMessage());                  
    }    
  } // setTransactionInProgress
 
  
  public void setTransactionCompleted(int transaction_id, long dt, double lat, double lng) {
    try {
      PreparedStatement preparedStatement;
      String query 
        = "update " + dbName + "." + transactionTableName
        + " set status = ?, completed_dt = ?, completed_lat = ?, completed_lng = ?"
        + " where transaction_id = ?;";      
      preparedStatement = connection.prepareStatement(query);
      preparedStatement.setInt(1, User.PASSENGER_COMPLETED);
      preparedStatement.setLong(2, dt);
      preparedStatement.setDouble(3, lat);      
      preparedStatement.setDouble(4, lng);            
      preparedStatement.setInt(5, transaction_id);
      preparedStatement.executeUpdate();
    }
    catch (Exception e) {
      System.err.println(e.getMessage());                  
    }    
  } // setTransactionCompleted
  

} // CarpoolerEJB