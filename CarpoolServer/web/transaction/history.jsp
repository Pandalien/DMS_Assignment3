<%-- 
    Document   : browse all listings or seller's listings
    Created on : Apr 17, 2016, 1:52:18 PM
    Author     : Andy Chen
--%>

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
    <body>
        <h1>My carpooling history</h1>
        <table>
            <tr>
                <th>Transaction Id</th><th>Driver</th><th>Passenger</th><th>Tag on time</th><th>Tag off time</th>
            </tr>
            <%
                List<models.Transaction> list = (List<models.Transaction>) request.getAttribute("history-list");
                
                models.User currentUser = (models.User) session.getAttribute("current_user");
                for (models.Transaction item : list) {%>
                
                <tr>
                    <td><%= item.getTransactionId()%></td>
                    <td><%= item.getDriverId().getUsername()%></td>
                    <td><%= item.getPassengerId().getUsername()%></td>
                    <td><%= item.getCollectedDt()%></td>
                    <td><%= item.getCompletedDt()%></td>
                </tr>
            <%}%>
        </table>
    </body>
</html>