/*
 * Carpool Server, DMS Assignment 3
 */
package dmsassignment3.carpool;

import java.io.*;
import org.json.*;

/**
 *
 * @author macbookair
 */
public class User implements Serializable {
  
  // status constants
  public static int OFFLINE = 0;
  public static int DRIVER = 1;
  public static int PASSENGER = 2;
  
  // User session fields
  private int user_id;
  private String username;
  private int points;
  private int status;
  private double lat;
  private double lng;
  private double dest_lat;
  private double dest_lng;
  private double proximity; // km
  
  public User() {
    user_id = 0;
    username = "blah";
    points = 0;
    status = OFFLINE;
    lat = 0;
    lng = 0;
    dest_lat = 0;
    dest_lng = 0;
    proximity = 1;
  }
  
  public User(JSONObject jsonObject) {
    user_id = jsonObject.optInt("user_id", 0);
    username = jsonObject.optString("username", "");
    points = jsonObject.optInt("points", 0);
    status = jsonObject.optInt("status", OFFLINE);
    lat = jsonObject.optDouble("lat", 0);
    lng = jsonObject.optDouble("lng", 0);
    dest_lat = jsonObject.optDouble("dest_lat", 0);
    dest_lng = jsonObject.optDouble("dest_lng", 0);
    proximity = jsonObject.optDouble("proximity", proximity);    
  }

  public int getUserID() {
    return user_id;
  }
  
  public void setUserID(int user_id) {
    this.user_id = user_id;
  }
  
  public String getUsername() {
    return username;
  }
  
  public void setUsername(String username) {
    this.username = username;
  }

  public int getPoints() {
    return points;
  }  
  
  public void setPoints(int points) {
    this.points = points;
  }
  
  public boolean isDriver() {
    return status == DRIVER;
  }
  
  public boolean isPassenger() {
    return status == PASSENGER;
  }
  
  public void setStatus(int status) {
    this.status = status;
  }
  
  public double getLat() {
    return lat;
  }
  
  public double getLng() {
    return lng;
  }
  
  public void setLatLng(double lat, double lng) {
    this.lat = lat;
    this.lng = lng;
  }
  
  public double getDestLat() {
    return dest_lat;
  }
  
  public double getDestLng() {
    return dest_lng;
  }
  
  public void setDestLatLng(double dest_lat, double dest_lng) {
    this.dest_lat = dest_lat;
    this.dest_lng = dest_lng;
  }
  
  public double getProximity() {
    return this.proximity;
  }
  
  public void setProximity(double proximity) {
    this.proximity = proximity;
  }
  
  public String toString() {
    return username;
  }

  public JSONObject toJSONObject()  {
    JSONObject jsonObject = new JSONObject();
    try {
      jsonObject.put("user_id", user_id);
      jsonObject.put("username", username);
      jsonObject.put("points", points);
      jsonObject.put("status", status);
      jsonObject.put("lat", lat);
      jsonObject.put("lng", lng);
      jsonObject.put("dest_lat", dest_lat);
      jsonObject.put("dest_lng", dest_lng);
      jsonObject.put("proximity", proximity);
    }
    catch (Exception e) {
      System.err.println(e.getMessage());
    }
    return jsonObject;
  }
  
}
