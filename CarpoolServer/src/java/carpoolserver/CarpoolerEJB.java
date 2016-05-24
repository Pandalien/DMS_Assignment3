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
          + ",primary key (user_id)"
          + ");";
      statement.executeUpdate(sql);
      
      // create transaction table if not exists:
      sql = "create table if not exists " + dbName + "." + transactionTableName + " ("
          + " transaction_id int not null auto_increment"
          + ",driver_id      int not null"
          + ",passenger_id   int not null"
          + ",status         int not null"    // status: pending, in progress, completed
          + ",collected_dt   datetime"        
          + ",collected_lat  double"
          + ",collected_lng  double"              
          + ",dropped_dt     datetime"
          + ",dropped_lat    double"
          + ",dropped_lng    double"
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
      query = "select * from " + dbName + "." + userTableName + " where username = ? ";
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
      query = "select * from " + dbName + "." + userTableName + " where username = ? ;";
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
      String query = "select * from " + dbName + "." + userTableName + " where username = ? ";
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
      String query = "select * from " + dbName + "." + userTableName + " where user_id = ? ";
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

  
  public void updateStatus(int user_id, int status) {  
    try {
      PreparedStatement preparedStatement;
      String query = "update " + dbName + "." + userTableName + " set status=? where user_id=?;";      
      preparedStatement = connection.prepareStatement(query);
      preparedStatement.setInt(1, status);      
      preparedStatement.setInt(2, user_id);
      preparedStatement.executeUpdate();
    }
    catch (Exception e) {
      System.err.println(e.getMessage());                  
    }
  } // updateStatus
  
  public void updatePoints(int user_id, int points) {  
    try {
      PreparedStatement preparedStatement;
      String query = "update " + dbName + "." + userTableName + " set points=? where user_id=?;";      
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
      String query = "update " + dbName + "." + userTableName + " set lat=?,lng=? where user_id=?;";      
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
      String query = "update " + dbName + "." + userTableName + " set dest_lat=?,dest_lng=? where user_id=?;";      
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
      String query = "update " + dbName + "." + userTableName + " set proximity=? where user_id=?;";      
      preparedStatement = connection.prepareStatement(query);
      preparedStatement.setDouble(1, proximity);      
      preparedStatement.setInt(2, user_id);
      preparedStatement.executeUpdate();
    }
    catch (Exception e) {
      System.err.println(e.getMessage());                  
    }
  } // updateProximity
  
  
  public JSONObject getUserList(User forUser) {
    // for this user, return list of available passengers or drivers in range.
    // sort list by points/distance
    
    // also check transaction table to see if user is currently in a transaction
    // I think it can be done with one sql command
    
    ArrayList<User> userlist = new ArrayList();
    int status = forUser.isDriver() ? User.PASSENGER : User.DRIVER;
    try {
      PreparedStatement preparedStatement;
      String query = "select * from " + dbName + "." + userTableName;// + " where status = ? ";
      preparedStatement = connection.prepareStatement(query);
//      preparedStatement.setInt(1, status);
      ResultSet resultSet = preparedStatement.executeQuery();
      while (resultSet.next()) 
        userlist.add(getUserFromResultSet(resultSet));
    }
    catch (Exception e) {
      System.err.println(e.getMessage());
    }
    
    JSONObject jsonUserList = new JSONObject();
    for (User u: userlist) {
      try {
        jsonUserList.put(u.getUsername(), u.toJSONObject());
      }
      catch (Exception e) {
        System.err.println(e.getMessage());
      }
    }
    
    return jsonUserList;
  } // getUserList
  
  
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
    }
    catch (Exception e) {
      System.err.println(e.getMessage());      
    }
    return user;
  } // getUserFromResultSet
  

} // CarpoolerEJB