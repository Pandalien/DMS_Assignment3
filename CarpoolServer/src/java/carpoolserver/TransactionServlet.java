/*
 * Carpool Server, DMS Assignment 3
 */
package carpoolserver;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author AndyChen
 */
public class TransactionServlet extends HttpServlet {

    @EJB
    private models.UserFacade ejbFacade;
    @EJB
    private models.TransactionFacade ejbFacadeTrans;
    
    final String CurrentUserAttributeName = "current_user";
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
        models.User user = ejbFacade.findByUsernameAndPassword(request.getParameter("username"), request.getParameter("password"));
        request.getSession().setAttribute(CurrentUserAttributeName, user);
        
        if (user == null) {
            sendMessage(request, response, "Please use the login first in the login screen.");
            return;
        }
        
        List<models.Transaction> ts = ejbFacadeTrans.findByDriverId(user);
        
        if (ts.size() < 1) {
            sendMessage(request, response, "No records are available.");
            return;
        }
        request.setAttribute("history-list", ts);
        
        review(request, response, "/transaction/history.jsp");
        
//        String msg = user.getUsername();
//        msg += "size: " + user.getTransactionCollection().size();
//        
//        for (int i = 0; i < user.getTransactionCollection().size(); i++) {
//            msg += " " + user.getTransactionCollection();
//        }
//        
//        sendMessage(request, response, "Hello World");
    }
    
    protected void sendMessage(HttpServletRequest req, HttpServletResponse resp, String msg) {
        req.setAttribute("server_message", msg);
        
        review(req, resp, "/message.jsp");
    }
    
    protected void review(HttpServletRequest req, HttpServletResponse resp, String jsp){
        RequestDispatcher dispatcher = getServletContext().getRequestDispatcher(jsp);
        try {
            dispatcher.forward(req, resp);
        } catch (ServletException | IOException ex) {
            Logger.getLogger(TransactionServlet.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

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
