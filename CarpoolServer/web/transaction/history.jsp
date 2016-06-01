<%-- 
    Document   : browse all listings or seller's listings
    Created on : Apr 17, 2016, 1:52:18 PM
    Author     : Andy Chen
--%>

<%@page import="java.text.SimpleDateFormat"%>
<%@page import="java.util.Objects"%>
<%@page import="models.Transaction"%>
<%@page import="models.User"%>
<%@page import="java.util.List"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <link rel="stylesheet" href="resources/css/site.css"/>
        <title>My carpooling history</title>
    </head>
    <%
        models.User currentUser = (models.User) session.getAttribute("current_user");
        SimpleDateFormat dt = new SimpleDateFormat("yyyy-MM-dd hh:mm");
    %>
    <body>
        <h3 style="text-align: center">Carpooling history of <%=currentUser.getUsername()%></h3>
        <table>
            <tr>
                <th>ID</th><th>Driver</th><th>Passenger</th><th>Tag on</th><th>Tag off</th>
            </tr>
            <%
                List<models.Transaction> list = (List<models.Transaction>) request.getAttribute("history-list");
                
                
                for (models.Transaction item : list) {%>
                
                <tr>
                    <td><%= item.getTransactionId()%></td>
                    <td><%= item.getDriverId().getUsername()%></td>
                    <td><%= item.getPassengerId().getUsername()%></td>
                    <td><%= item.getCollectedDt()!=null? dt.format(item.getCollectedDt()) : "-"%></td>
                    <td><%= item.getCompletedDt()!=null? dt.format(item.getCompletedDt()) : "-"%></td>
                </tr>
            <%}%>
        </table>
    </body>
</html>