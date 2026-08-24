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

@WebServlet("/dashboard")
public class DashboardServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        // =========================
        // SESSION GUARD
        // =========================

        HttpSession session = request.getSession(false);

        if (session == null ||
            session.getAttribute("user") == null) {

            response.sendRedirect(
                    request.getContextPath() + "/login.jsp"
            );
            return;
        }

        User user = (User) session.getAttribute("user");

        // =========================
        // DASHBOARD VARIABLES
        // =========================

        int totalBooks = 0;
        int totalBookCopies = 0;
        int totalUsers = 0;
        int activeLoans = 0;
        int pendingRequests = 0;

        int myActiveLoans = 0;
        int myRequests = 0;
        double myFine = 0.0;

        // =========================
        // DATABASE CONNECTION
        // =========================

        try (Connection conn = DBConnection.getConnection()) {

            if (conn != null) {

                // =========================
                // ADMIN
                // =========================

                if ("ADMIN".equalsIgnoreCase(user.getRole())) {

                    // -------------------------
                    // 1. TOTAL BOOKS & COPIES
                    // -------------------------

                    String sqlBooks =
                            "SELECT COUNT(*) AS total_titles, " +
                            "COALESCE(SUM(total_copies), 0) AS total_copies " +
                            "FROM books";

                    try (PreparedStatement stmt =
                                 conn.prepareStatement(sqlBooks);
                         ResultSet rs = stmt.executeQuery()) {

                        if (rs.next()) {
                            totalBooks =
                                    rs.getInt("total_titles");

                            totalBookCopies =
                                    rs.getInt("total_copies");
                        }
                    }

                    // -------------------------
                    // 2. REGISTERED STUDENTS
                    // -------------------------

                    String sqlUsers =
                            "SELECT COUNT(*) " +
                            "FROM users " +
                            "WHERE UPPER(role) = 'STUDENT'";

                    try (PreparedStatement stmt =
                                 conn.prepareStatement(sqlUsers);
                         ResultSet rs = stmt.executeQuery()) {

                        if (rs.next()) {
                            totalUsers = rs.getInt(1);
                        }
                    }

                    // -------------------------
                    // 3. ACTIVE LOANS
                    // -------------------------

                    String sqlLoans =
                            "SELECT COUNT(*) " +
                            "FROM transactions " +
                            "WHERE UPPER(status) IN " +
                            "('ISSUED', 'OVERDUE')";

                    try (PreparedStatement stmt =
                                 conn.prepareStatement(sqlLoans);
                         ResultSet rs = stmt.executeQuery()) {

                        if (rs.next()) {
                            activeLoans = rs.getInt(1);
                        }
                    }

                    // -------------------------
                    // 4. PENDING REQUESTS
                    // -------------------------

                    String sqlRequests =
                            "SELECT COUNT(*) " +
                            "FROM book_requests " +
                            "WHERE UPPER(status) = 'PENDING'";

                    try (PreparedStatement stmt =
                                 conn.prepareStatement(sqlRequests);
                         ResultSet rs = stmt.executeQuery()) {

                        if (rs.next()) {
                            pendingRequests = rs.getInt(1);
                        }
                    }
                }

                // =========================
                // STUDENT
                // =========================

                else {

                    // -------------------------
                    // 1. MY ACTIVE LOANS + FINE
                    // -------------------------

                    String sqlMyLoans =
                            "SELECT COUNT(*), " +
                            "COALESCE(SUM(fine_amount), 0) " +
                            "FROM transactions " +
                            "WHERE student_id = ? " +
                            "AND UPPER(status) IN " +
                            "('ISSUED', 'OVERDUE')";

                    try (PreparedStatement stmt =
                                 conn.prepareStatement(sqlMyLoans)) {

                        /*
                         * IMPORTANT:
                         * Your User class must provide getId().
                         */
                        stmt.setInt(1, user.getId());

                        try (ResultSet rs =
                                     stmt.executeQuery()) {

                            if (rs.next()) {
                                myActiveLoans = rs.getInt(1);
                                myFine = rs.getDouble(2);
                            }
                        }
                    }

                    // -------------------------
                    // 2. MY BOOK REQUESTS
                    // -------------------------

                    String sqlMyRequests =
                            "SELECT COUNT(*) " +
                            "FROM book_requests " +
                            "WHERE user_id = ?";

                    try (PreparedStatement stmt =
                                 conn.prepareStatement(sqlMyRequests)) {

                        stmt.setInt(1, user.getId());

                        try (ResultSet rs =
                                     stmt.executeQuery()) {

                            if (rs.next()) {
                                myRequests = rs.getInt(1);
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        // =========================
        // SEND DATA TO JSP
        // =========================

        request.setAttribute(
                "totalBooks",
                totalBooks
        );

        request.setAttribute(
                "totalBookCopies",
                totalBookCopies
        );

        request.setAttribute(
                "totalUsers",
                totalUsers
        );

        request.setAttribute(
                "activeLoans",
                activeLoans
        );

        request.setAttribute(
                "pendingRequests",
                pendingRequests
        );

        request.setAttribute(
                "myActiveLoans",
                myActiveLoans
        );

        request.setAttribute(
                "myRequests",
                myRequests
        );

        request.setAttribute(
                "myFine",
                myFine
        );

        // =========================
        // OPEN DASHBOARD
        // =========================

        request.getRequestDispatcher(
                "/dashboard.jsp"
        ).forward(request, response);
    }
}