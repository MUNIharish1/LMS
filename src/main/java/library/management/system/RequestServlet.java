package library.management.system;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/requests")
public class RequestServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    // =========================================================
    // GET - DISPLAY BOOK REQUESTS
    // =========================================================

    @Override
    protected void doGet(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        // -----------------------------------------------------
        // SESSION GUARD
        // -----------------------------------------------------

        HttpSession session = request.getSession(false);

        if (session == null ||
            session.getAttribute("user") == null) {

            response.sendRedirect(
                    request.getContextPath() + "/login.jsp"
            );
            return;
        }

        User user = (User) session.getAttribute("user");

        List<BookRequest> requestList =
                new ArrayList<>();

        // -----------------------------------------------------
        // DATABASE
        // -----------------------------------------------------

        try (Connection conn =
                     DBConnection.getConnection()) {

            if (conn == null) {

                request.setAttribute(
                        "requestList",
                        requestList
                );

                request.getRequestDispatcher(
                        "/requests.jsp"
                ).forward(request, response);

                return;
            }

            String sql;

            // -------------------------------------------------
            // ADMIN - SHOW ALL REQUESTS
            // -------------------------------------------------

            if ("ADMIN".equalsIgnoreCase(user.getRole())) {

                sql =
                    "SELECT r.id, r.user_id, u.username, " +
                    "r.book_id, b.title AS book_title, " +
                    "r.request_date, r.status " +
                    "FROM book_requests r " +
                    "JOIN users u ON r.user_id = u.id " +
                    "JOIN books b ON r.book_id = b.id " +
                    "ORDER BY r.id DESC";

            }

            // -------------------------------------------------
            // STUDENT - SHOW OWN REQUESTS
            // -------------------------------------------------

            else {

                sql =
                    "SELECT r.id, r.user_id, u.username, " +
                    "r.book_id, b.title AS book_title, " +
                    "r.request_date, r.status " +
                    "FROM book_requests r " +
                    "JOIN users u ON r.user_id = u.id " +
                    "JOIN books b ON r.book_id = b.id " +
                    "WHERE r.user_id = ? " +
                    "ORDER BY r.id DESC";
            }

            // -------------------------------------------------
            // PREPARED STATEMENT
            // -------------------------------------------------

            try (PreparedStatement stmt =
                         conn.prepareStatement(sql)) {

                if (!"ADMIN".equalsIgnoreCase(
                        user.getRole())) {

                    stmt.setInt(
                            1,
                            user.getId()
                    );
                }

                // -------------------------------------------------
                // RESULT
                // -------------------------------------------------

                try (ResultSet rs =
                             stmt.executeQuery()) {

                    while (rs.next()) {

                        BookRequest req =
                                new BookRequest();

                        req.setId(
                                rs.getInt("id")
                        );

                        req.setUserId(
                                rs.getInt("user_id")
                        );

                        req.setUsername(
                                rs.getString("username")
                        );

                        req.setBookId(
                                rs.getInt("book_id")
                        );

                        req.setBookTitle(
                                rs.getString("book_title")
                        );

                        req.setRequestDate(
                                rs.getDate("request_date")
                        );

                        req.setStatus(
                                rs.getString("status")
                        );

                        requestList.add(req);
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
                "requestList",
                requestList
        );

        request.getRequestDispatcher(
                "/requests.jsp"
        ).forward(request, response);
    }


    // =========================================================
    // POST - CREATE / APPROVE / REJECT
    // =========================================================

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        // -----------------------------------------------------
        // SESSION GUARD
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
        // STUDENT - CREATE BOOK REQUEST
        // =====================================================

        if ("create".equalsIgnoreCase(action)) {

            String bookIdParam =
                    request.getParameter("bookId");

            if (bookIdParam == null ||
                bookIdParam.trim().isEmpty()) {

                response.sendRedirect(
                        request.getContextPath() + "/requests"
                );

                return;
            }

            try {

                int bookId =
                        Integer.parseInt(
                                bookIdParam.trim()
                        );

                // -------------------------------------------------
                // INSERT BOOK REQUEST
                // -------------------------------------------------

                try (Connection conn =
                             DBConnection.getConnection()) {

                    if (conn != null) {

                        String insertSql =
                                "INSERT INTO book_requests " +
                                "(user_id, book_id, request_date, status) " +
                                "VALUES (?, ?, ?, 'PENDING')";

                        try (PreparedStatement stmt =
                                     conn.prepareStatement(
                                             insertSql)) {

                            stmt.setInt(
                                    1,
                                    user.getId()
                            );

                            stmt.setInt(
                                    2,
                                    bookId
                            );

                            stmt.setDate(
                                    3,
                                    java.sql.Date.valueOf(
                                            LocalDate.now()
                                    )
                            );

                            stmt.executeUpdate();
                        }
                    }
                }

            } catch (NumberFormatException e) {

                e.printStackTrace();

            } catch (SQLException e) {

                e.printStackTrace();

            } catch (Exception e) {

                e.printStackTrace();
            }

            // -------------------------------------------------
            // RETURN TO REQUESTS
            // -------------------------------------------------

            response.sendRedirect(
                    request.getContextPath() + "/requests"
            );

            return;
        }


        // =====================================================
        // ADMIN - APPROVE / REJECT
        // =====================================================

        if ("ADMIN".equalsIgnoreCase(user.getRole())) {

            // -------------------------------------------------
            // GET REQUEST ID
            // -------------------------------------------------

            String requestIdParam =
                    request.getParameter("requestId");

            if (requestIdParam == null ||
                requestIdParam.trim().isEmpty()) {

                response.sendRedirect(
                        request.getContextPath() + "/requests"
                );

                return;
            }

            int requestId;

            try {

                requestId =
                        Integer.parseInt(
                                requestIdParam.trim()
                        );

            } catch (NumberFormatException e) {

                response.sendRedirect(
                        request.getContextPath() + "/requests"
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
                            request.getContextPath() + "/requests"
                    );

                    return;
                }

                conn.setAutoCommit(false);

                try {

                    // =========================================
                    // APPROVE
                    // =========================================

                    if ("approve".equalsIgnoreCase(action)) {

                        int userId = 0;
                        int bookId = 0;

                        // -----------------------------------------
                        // GET USER ID AND BOOK ID
                        // -----------------------------------------

                        String fetchSql =
                                "SELECT user_id, book_id " +
                                "FROM book_requests " +
                                "WHERE id = ?";

                        try (PreparedStatement stmt =
                                     conn.prepareStatement(
                                             fetchSql)) {

                            stmt.setInt(
                                    1,
                                    requestId
                            );

                            try (ResultSet rs =
                                         stmt.executeQuery()) {

                                if (rs.next()) {

                                    userId =
                                            rs.getInt("user_id");

                                    bookId =
                                            rs.getInt("book_id");
                                }
                            }
                        }


                        // -----------------------------------------
                        // CHECK REQUEST
                        // -----------------------------------------

                        if (userId <= 0 ||
                            bookId <= 0) {

                            conn.rollback();

                            response.sendRedirect(
                                    request.getContextPath()
                                    + "/requests"
                            );

                            return;
                        }


                        // -----------------------------------------
                        // CHECK AVAILABLE COPIES
                        // -----------------------------------------

                        int availableCopies = 0;

                        String bookSql =
                                "SELECT available_copies " +
                                "FROM books " +
                                "WHERE id = ?";

                        try (PreparedStatement stmt =
                                     conn.prepareStatement(
                                             bookSql)) {

                            stmt.setInt(
                                    1,
                                    bookId
                            );

                            try (ResultSet rs =
                                         stmt.executeQuery()) {

                                if (rs.next()) {

                                    availableCopies =
                                            rs.getInt(
                                                    "available_copies"
                                            );
                                }
                            }
                        }


                        // -----------------------------------------
                        // NO COPY AVAILABLE
                        // -----------------------------------------

                        if (availableCopies <= 0) {

                            conn.rollback();

                            response.sendRedirect(
                                    request.getContextPath()
                                    + "/requests"
                            );

                            return;
                        }


                        // -----------------------------------------
                        // UPDATE REQUEST STATUS
                        // -----------------------------------------

                        String updateRequestSql =
                                "UPDATE book_requests " +
                                "SET status = 'APPROVED' " +
                                "WHERE id = ?";

                        try (PreparedStatement stmt =
                                     conn.prepareStatement(
                                             updateRequestSql)) {

                            stmt.setInt(
                                    1,
                                    requestId
                            );

                            stmt.executeUpdate();
                        }


                        // -----------------------------------------
                        // CREATE TRANSACTION
                        // -----------------------------------------

                        LocalDate today =
                                LocalDate.now();

                        LocalDate dueDate =
                                today.plusDays(14);

                        String insertTransactionSql =
                                "INSERT INTO transactions " +
                                "(student_id, book_id, issue_date, " +
                                "due_date, status, fine_amount) " +
                                "VALUES (?, ?, ?, ?, 'ISSUED', 0)";

                        try (PreparedStatement stmt =
                                     conn.prepareStatement(
                                             insertTransactionSql)) {

                            stmt.setInt(
                                    1,
                                    userId
                            );

                            stmt.setInt(
                                    2,
                                    bookId
                            );

                            stmt.setDate(
                                    3,
                                    java.sql.Date.valueOf(
                                            today
                                    )
                            );

                            stmt.setDate(
                                    4,
                                    java.sql.Date.valueOf(
                                            dueDate
                                    )
                            );

                            stmt.executeUpdate();
                        }


                        // -----------------------------------------
                        // DECREASE AVAILABLE COPIES
                        // -----------------------------------------

                        String updateBooksSql =
                                "UPDATE books " +
                                "SET available_copies = " +
                                "available_copies - 1 " +
                                "WHERE id = ? " +
                                "AND available_copies > 0";

                        try (PreparedStatement stmt =
                                     conn.prepareStatement(
                                             updateBooksSql)) {

                            stmt.setInt(
                                    1,
                                    bookId
                            );

                            stmt.executeUpdate();
                        }
                    }


                    // =========================================
                    // REJECT
                    // =========================================

                    else if ("reject".equalsIgnoreCase(action)) {

                        String rejectSql =
                                "UPDATE book_requests " +
                                "SET status = 'REJECTED' " +
                                "WHERE id = ?";

                        try (PreparedStatement stmt =
                                     conn.prepareStatement(
                                             rejectSql)) {

                            stmt.setInt(
                                    1,
                                    requestId
                            );

                            stmt.executeUpdate();
                        }
                    }


                    // =========================================
                    // COMMIT
                    // =========================================

                    conn.commit();

                } catch (SQLException e) {

                    conn.rollback();

                    e.printStackTrace();

                } finally {

                    conn.setAutoCommit(true);
                }

            } catch (Exception e) {

                e.printStackTrace();
            }

            // -------------------------------------------------
            // RETURN TO REQUESTS PAGE
            // -------------------------------------------------

            response.sendRedirect(
                    request.getContextPath() + "/requests"
            );

            return;
        }


        // =====================================================
        // DEFAULT
        // =====================================================

        response.sendRedirect(
                request.getContextPath() + "/requests"
        );
    }
}