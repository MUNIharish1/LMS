package library.management.system;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/transactions")
public class TransactionServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    // =========================================================
    // GET - DISPLAY TRANSACTIONS
    // =========================================================

    @Override
    protected void doGet(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        // -----------------------------------------------------
        // SESSION CHECK
        // -----------------------------------------------------

        HttpSession session = request.getSession(false);

        if (session == null ||
            session.getAttribute("user") == null) {

            response.sendRedirect(
                    request.getContextPath() + "/login.jsp"
            );
            return;
        }

        User user =
                (User) session.getAttribute("user");

        List<Transaction> transactions =
                new ArrayList<>();

        // -----------------------------------------------------
        // DATABASE CONNECTION
        // -----------------------------------------------------

        try (Connection conn =
                     DBConnection.getConnection()) {

            if (conn == null) {
                request.setAttribute(
                        "transactions",
                        transactions
                );

                request.getRequestDispatcher(
                        "/transactions.jsp"
                ).forward(request, response);

                return;
            }

            String sql;

            // =================================================
            // ADMIN VIEW
            // =================================================

            if ("ADMIN".equalsIgnoreCase(user.getRole())) {

                sql =
                    "SELECT t.id, " +
                    "t.student_id, " +
                    "u.username AS student_name, " +
                    "t.book_id, " +
                    "b.title AS book_title, " +
                    "t.issue_date, " +
                    "t.due_date, " +
                    "t.return_date, " +
                    "t.fine_amount, " +
                    "t.status " +
                    "FROM transactions t " +
                    "JOIN users u ON t.student_id = u.id " +
                    "JOIN books b ON t.book_id = b.id " +
                    "ORDER BY t.id DESC";

            }

            // =================================================
            // STUDENT VIEW
            // =================================================

            else {

                /*
                 * Student history contains:
                 *
                 * 1. Normal borrowing transactions
                 * 2. Rejected book requests
                 */

                sql =
                    "SELECT t.id, " +
                    "t.student_id, " +
                    "u.username AS student_name, " +
                    "t.book_id, " +
                    "b.title AS book_title, " +
                    "t.issue_date, " +
                    "t.due_date, " +
                    "t.return_date, " +
                    "t.fine_amount, " +
                    "t.status " +
                    "FROM transactions t " +
                    "JOIN users u ON t.student_id = u.id " +
                    "JOIN books b ON t.book_id = b.id " +
                    "WHERE t.student_id = ? " +

                    "UNION ALL " +

                    "SELECT r.id, " +
                    "r.user_id AS student_id, " +
                    "u.username AS student_name, " +
                    "r.book_id, " +
                    "b.title AS book_title, " +
                    "r.request_date AS issue_date, " +
                    "NULL AS due_date, " +
                    "NULL AS return_date, " +
                    "0.0 AS fine_amount, " +
                    "r.status " +
                    "FROM book_requests r " +
                    "JOIN users u ON r.user_id = u.id " +
                    "JOIN books b ON r.book_id = b.id " +
                    "WHERE r.user_id = ? " +
                    "AND UPPER(r.status) = 'REJECTED' " +

                    "ORDER BY id DESC";
            }

            // -------------------------------------------------
            // PREPARED STATEMENT
            // -------------------------------------------------

            try (PreparedStatement stmt =
                         conn.prepareStatement(sql)) {

                // Student query has TWO ? parameters.
                if (!"ADMIN".equalsIgnoreCase(
                        user.getRole())) {

                    stmt.setInt(
                            1,
                            user.getId()
                    );

                    stmt.setInt(
                            2,
                            user.getId()
                    );
                }

                // -------------------------------------------------
                // EXECUTE QUERY
                // -------------------------------------------------

                try (ResultSet rs =
                             stmt.executeQuery()) {

                    while (rs.next()) {

                        Transaction t =
                                new Transaction();

                        t.setId(
                                rs.getInt("id")
                        );

                        t.setStudentId(
                                rs.getInt("student_id")
                        );

                        t.setStudentName(
                                rs.getString("student_name")
                        );

                        t.setBookId(
                                rs.getInt("book_id")
                        );

                        t.setBookTitle(
                                rs.getString("book_title")
                        );

                        t.setIssueDate(
                                rs.getDate("issue_date")
                        );

                        t.setDueDate(
                                rs.getDate("due_date")
                        );

                        t.setReturnDate(
                                rs.getDate("return_date")
                        );

                        t.setFineAmount(
                                rs.getDouble("fine_amount")
                        );

                        t.setStatus(
                                rs.getString("status")
                        );

                        transactions.add(t);
                    }
                }
            }

        } catch (Exception e) {

            e.printStackTrace();
        }

        // -----------------------------------------------------
        // SEND DATA TO JSP
        // -----------------------------------------------------

        request.setAttribute(
                "transactions",
                transactions
        );

        request.getRequestDispatcher(
                "/transactions.jsp"
        ).forward(request, response);
    }


    // =========================================================
    // POST - RETURN BOOK
    // =========================================================

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        // -----------------------------------------------------
        // SESSION CHECK
        // -----------------------------------------------------

        HttpSession session =
                request.getSession(false);

        if (session == null ||
            session.getAttribute("user") == null) {

            response.sendRedirect(
                    request.getContextPath() + "/login.jsp"
            );

            return;
        }

        User user =
                (User) session.getAttribute("user");

        String action =
                request.getParameter("action");


        // =====================================================
        // ADMIN - RETURN BOOK
        // =====================================================

        if ("ADMIN".equalsIgnoreCase(user.getRole()) &&
            "returnBook".equalsIgnoreCase(action)) {

            String txIdParam =
                    request.getParameter("transactionId");

            // -------------------------------------------------
            // CHECK TRANSACTION ID
            // -------------------------------------------------

            if (txIdParam == null ||
                txIdParam.trim().isEmpty()) {

                response.sendRedirect(
                        request.getContextPath()
                        + "/transactions"
                );

                return;
            }

            int transactionId;

            try {

                transactionId =
                        Integer.parseInt(
                                txIdParam.trim()
                        );

            } catch (NumberFormatException e) {

                response.sendRedirect(
                        request.getContextPath()
                        + "/transactions"
                );

                return;
            }


            // -------------------------------------------------
            // DATABASE TRANSACTION
            // -------------------------------------------------

            try (Connection conn =
                         DBConnection.getConnection()) {

                if (conn == null) {

                    response.sendRedirect(
                            request.getContextPath()
                            + "/transactions"
                    );

                    return;
                }

                conn.setAutoCommit(false);

                try {

                    int bookId = 0;

                    java.time.LocalDate dueDate = null;


                    // =========================================
                    // GET BOOK ID AND DUE DATE
                    // =========================================

                    String fetchSql =
                            "SELECT book_id, due_date " +
                            "FROM transactions " +
                            "WHERE id = ?";

                    try (PreparedStatement stmt =
                                 conn.prepareStatement(
                                         fetchSql)) {

                        stmt.setInt(
                                1,
                                transactionId
                        );

                        try (ResultSet rs =
                                     stmt.executeQuery()) {

                            if (rs.next()) {

                                bookId =
                                        rs.getInt("book_id");

                                java.sql.Date sqlDueDate =
                                        rs.getDate("due_date");

                                if (sqlDueDate != null) {

                                    dueDate =
                                            sqlDueDate.toLocalDate();
                                }
                            }
                        }
                    }


                    // =========================================
                    // CALCULATE FINE
                    // =========================================

                    if (bookId > 0 &&
                        dueDate != null) {

                        java.time.LocalDate today =
                                java.time.LocalDate.now();

                        double fine = 0.0;

                        if (today.isAfter(dueDate)) {

                            long daysOverdue =
                                    java.time.temporal.ChronoUnit
                                            .DAYS
                                            .between(
                                                    dueDate,
                                                    today
                                            );

                            // ₹1 per day
                            fine =
                                    daysOverdue * 1.0;
                        }


                        // =====================================
                        // UPDATE TRANSACTION
                        // =====================================

                        String updateTxSql =
                                "UPDATE transactions " +
                                "SET return_date = ?, " +
                                "fine_amount = ?, " +
                                "status = 'RETURNED' " +
                                "WHERE id = ?";

                        try (PreparedStatement stmt =
                                     conn.prepareStatement(
                                             updateTxSql)) {

                            stmt.setDate(
                                    1,
                                    java.sql.Date.valueOf(
                                            today
                                    )
                            );

                            stmt.setDouble(
                                    2,
                                    fine
                            );

                            stmt.setInt(
                                    3,
                                    transactionId
                            );

                            stmt.executeUpdate();
                        }


                        // =====================================
                        // INCREASE AVAILABLE COPIES
                        // =====================================

                        String updateBookSql =
                                "UPDATE books " +
                                "SET available_copies = " +
                                "available_copies + 1 " +
                                "WHERE id = ?";

                        try (PreparedStatement stmt =
                                     conn.prepareStatement(
                                             updateBookSql)) {

                            stmt.setInt(
                                    1,
                                    bookId
                            );

                            stmt.executeUpdate();
                        }
                    }


                    // =========================================
                    // COMMIT
                    // =========================================

                    conn.commit();

                } catch (SQLException ex) {

                    conn.rollback();

                    ex.printStackTrace();

                } finally {

                    conn.setAutoCommit(true);
                }

            } catch (Exception e) {

                e.printStackTrace();
            }
        }


        // -----------------------------------------------------
        // RETURN TO TRANSACTIONS PAGE
        // -----------------------------------------------------

        response.sendRedirect(
                request.getContextPath()
                + "/transactions"
        );
    }
}