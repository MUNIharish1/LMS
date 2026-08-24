package library.management.system;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/auth")
public class AuthServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Logout action: Invalidate session and redirect to login page
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        response.sendRedirect(request.getContextPath() + "/login.jsp");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String usernameParam = request.getParameter("username");
        String passwordParam = request.getParameter("password");

        if (usernameParam == null || passwordParam == null || 
            usernameParam.trim().isEmpty() || passwordParam.trim().isEmpty()) {
            request.setAttribute("error", "Username and Password are required.");
            request.getRequestDispatcher("/login.jsp").forward(request, response);
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            if (conn != null) {
                String sql = "SELECT id, username, password, role FROM users WHERE username = ? AND password = ?";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, usernameParam.trim());
                    stmt.setString(2, passwordParam.trim());

                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            User user = new User();
                            user.setId(rs.getInt("id"));
                            user.setUsername(rs.getString("username"));
                            user.setPassword(rs.getString("password"));
                            user.setRole(rs.getString("role"));

                            HttpSession session = request.getSession(true);
                            session.setAttribute("user", user);

                            // REDIRECT TO DASHBOARD BY DEFAULT UPON LOGIN
                            response.sendRedirect(request.getContextPath() + "/dashboard");
                            return;
                        } else {
                            request.setAttribute("error", "Invalid username or password.");
                            request.getRequestDispatcher("/login.jsp").forward(request, response);
                            return;
                        }
                    }
                }
            } else {
                request.setAttribute("error", "Database connection error. Please try again later.");
                request.getRequestDispatcher("/login.jsp").forward(request, response);
            }
        } catch (Exception e) {
            e.printStackTrace();
            request.setAttribute("error", "An internal error occurred: " + e.getMessage());
            request.getRequestDispatcher("/login.jsp").forward(request, response);
        }
    }
}