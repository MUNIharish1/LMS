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

@WebServlet("/books")
public class BookServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    // =========================================================
    // GET - DISPLAY BOOKS
    // =========================================================

    @Override
    protected void doGet(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        // Session Guard
        HttpSession session = request.getSession(false);

        if (session == null ||
            session.getAttribute("user") == null) {

            response.sendRedirect(
                    request.getContextPath() + "/login.jsp"
            );
            return;
        }

        String search = request.getParameter("search");

        List<Book> books = new ArrayList<>();

        StringBuilder sql = new StringBuilder();

        sql.append(
            "SELECT id, title, author, category, " +
            "total_copies, available_copies " +
            "FROM books"
        );

        // Search
        if (search != null && !search.trim().isEmpty()) {

            search = search.trim();

            sql.append(
                " WHERE LOWER(REPLACE(title, ' ', '')) LIKE ? " +
                " OR LOWER(REPLACE(author, ' ', '')) LIKE ? " +
                " OR LOWER(REPLACE(category, ' ', '')) LIKE ?"
            );
        }

        sql.append(" ORDER BY id DESC");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps =
                     conn.prepareStatement(sql.toString())) {

            if (search != null && !search.trim().isEmpty()) {

                String searchPattern =
                        "%" + search.toLowerCase()
                        .replace(" ", "") + "%";

                ps.setString(1, searchPattern);
                ps.setString(2, searchPattern);
                ps.setString(3, searchPattern);
            }

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    Book book = new Book(
                            rs.getInt("id"),
                            rs.getString("title"),
                            rs.getString("author"),
                            rs.getString("category"),
                            rs.getInt("total_copies"),
                            rs.getInt("available_copies")
                    );

                    books.add(book);
                }
            }

        } catch (SQLException e) {

            e.printStackTrace();

            request.setAttribute(
                    "error",
                    "Unable to load books: " + e.getMessage()
            );
        }

        request.setAttribute("books", books);

        request.getRequestDispatcher("/books.jsp")
               .forward(request, response);
    }

    // =========================================================
    // POST - ADD / EDIT / DELETE BOOK
    // =========================================================

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);

        if (session == null ||
            session.getAttribute("user") == null) {

            response.sendRedirect(
                    request.getContextPath() + "/login.jsp"
            );
            return;
        }

        User currentUser =
                (User) session.getAttribute("user");

        // Only ADMIN can modify books
        if (currentUser == null ||
            !"ADMIN".equalsIgnoreCase(currentUser.getRole())) {

            response.sendRedirect(
                    request.getContextPath() + "/login.jsp"
            );
            return;
        }

        String action = request.getParameter("action");

        if (action == null) {
            response.sendRedirect(
                    request.getContextPath() + "/books"
            );
            return;
        }

        // =====================================================
        // ADD BOOK
        // =====================================================

        if ("add".equalsIgnoreCase(action)) {

            String title = request.getParameter("title");
            String author = request.getParameter("author");
            String category = request.getParameter("category");

            String copiesParameter =
                    request.getParameter("copies");

            try {

                int copies =
                        Integer.parseInt(copiesParameter);

                if (title == null ||
                    author == null ||
                    category == null ||
                    title.trim().isEmpty() ||
                    author.trim().isEmpty() ||
                    category.trim().isEmpty() ||
                    copies < 0) {

                    response.sendRedirect(
                            request.getContextPath() +
                            "/books?error=invalid"
                    );
                    return;
                }

                String sql =
                    "INSERT INTO books " +
                    "(title, author, category, total_copies, " +
                    "available_copies) " +
                    "VALUES (?, ?, ?, ?, ?)";

                try (Connection conn =
                             DBConnection.getConnection();
                     PreparedStatement ps =
                             conn.prepareStatement(sql)) {

                    ps.setString(1, title.trim());
                    ps.setString(2, author.trim());
                    ps.setString(3, category.trim());
                    ps.setInt(4, copies);
                    ps.setInt(5, copies);

                    ps.executeUpdate();
                }

            } catch (NumberFormatException e) {

                response.sendRedirect(
                        request.getContextPath() +
                        "/books?error=invalid"
                );
                return;

            } catch (SQLException e) {

                e.printStackTrace();

                response.sendRedirect(
                        request.getContextPath() +
                        "/books?error=database"
                );
                return;
            }
        }

        // =====================================================
        // EDIT BOOK
        // =====================================================

        else if ("edit".equalsIgnoreCase(action)) {

            String bookIdParameter =
                    request.getParameter("bookId");

            String title =
                    request.getParameter("title");

            String author =
                    request.getParameter("author");

            String category =
                    request.getParameter("category");

            String totalCopiesParameter =
                    request.getParameter("totalCopies");

            try {

                int bookId =
                        Integer.parseInt(bookIdParameter);

                int newTotalCopies =
                        Integer.parseInt(totalCopiesParameter);

                if (newTotalCopies < 0) {
                    response.sendRedirect(
                            request.getContextPath() +
                            "/books?error=invalid"
                    );
                    return;
                }

                try (Connection conn =
                             DBConnection.getConnection()) {

                    conn.setAutoCommit(false);

                    try {

                        // -----------------------------------------
                        // 1. Get old stock values
                        // -----------------------------------------

                        int oldTotal = 0;
                        int oldAvailable = 0;

                        String fetchSql =
                            "SELECT total_copies, available_copies " +
                            "FROM books WHERE id = ?";

                        try (PreparedStatement psFetch =
                                     conn.prepareStatement(fetchSql)) {

                            psFetch.setInt(1, bookId);

                            try (ResultSet rs =
                                         psFetch.executeQuery()) {

                                if (rs.next()) {

                                    oldTotal =
                                            rs.getInt(
                                                    "total_copies");

                                    oldAvailable =
                                            rs.getInt(
                                                    "available_copies");
                                }
                            }
                        }

                        // -----------------------------------------
                        // 2. Calculate stock difference
                        // -----------------------------------------

                        int difference =
                                newTotalCopies - oldTotal;

                        int newAvailable =
                                oldAvailable + difference;

                        // Prevent negative inventory
                        if (newAvailable < 0) {
                            newAvailable = 0;
                        }

                        // available cannot exceed total
                        if (newAvailable > newTotalCopies) {
                            newAvailable = newTotalCopies;
                        }

                        // -----------------------------------------
                        // 3. Update book
                        // -----------------------------------------

                        String updateSql =
                            "UPDATE books SET " +
                            "title = ?, " +
                            "author = ?, " +
                            "category = ?, " +
                            "total_copies = ?, " +
                            "available_copies = ? " +
                            "WHERE id = ?";

                        try (PreparedStatement psUp =
                                     conn.prepareStatement(updateSql)) {

                            psUp.setString(1, title);
                            psUp.setString(2, author);
                            psUp.setString(3, category);
                            psUp.setInt(4, newTotalCopies);
                            psUp.setInt(5, newAvailable);
                            psUp.setInt(6, bookId);

                            psUp.executeUpdate();
                        }

                        conn.commit();

                    } catch (SQLException e) {

                        conn.rollback();

                        throw e;

                    } finally {

                        conn.setAutoCommit(true);
                    }
                }

            } catch (NumberFormatException e) {

                response.sendRedirect(
                        request.getContextPath() +
                        "/books?error=invalid"
                );
                return;

            } catch (SQLException e) {

                e.printStackTrace();

                response.sendRedirect(
                        request.getContextPath() +
                        "/books?error=database"
                );
                return;
            }
        }

        // =====================================================
        // DELETE BOOK
        // =====================================================

        else if ("delete".equalsIgnoreCase(action)) {

            String bookIdParameter =
                    request.getParameter("bookId");

            try {

                int bookId =
                        Integer.parseInt(bookIdParameter);

                String sql =
                        "DELETE FROM books WHERE id = ?";

                try (Connection conn =
                             DBConnection.getConnection();
                     PreparedStatement ps =
                             conn.prepareStatement(sql)) {

                    ps.setInt(1, bookId);
                    ps.executeUpdate();
                }

            } catch (NumberFormatException e) {

                response.sendRedirect(
                        request.getContextPath() +
                        "/books?error=invalid"
                );
                return;

            } catch (SQLException e) {

                e.printStackTrace();

                response.sendRedirect(
                        request.getContextPath() +
                        "/books?error=database"
                );
                return;
            }
        }

        // =====================================================
        // INVALID ACTION
        // =====================================================

        else {

            response.sendRedirect(
                    request.getContextPath() +
                    "/books?error=invalid"
            );
            return;
        }

        response.sendRedirect(
                request.getContextPath() + "/books"
        );
    }
}