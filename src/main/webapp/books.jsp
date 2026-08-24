<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.List"%>
<%@ page import="library.management.system.User"%>
<%@ page import="library.management.system.Book"%>

<%
// Fixed: Retrieve user using "user" key to match AuthServlet
User user = (User) session.getAttribute("user");
if (user == null) {
    response.sendRedirect(request.getContextPath() + "/login.jsp");
    return;
}

@SuppressWarnings("unchecked")
List<Book> books = (List<Book>) request.getAttribute("books");
String search = request.getParameter("search");
if (search == null) search = "";
String contextPath = request.getContextPath();
%>

<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>Books Catalog - Library System</title>
<style>
body { font-family: Arial, sans-serif; margin: 0; background: #f4f6f9; }
.navbar { background: #343a40; color: white; padding: 15px 20px; display: flex; justify-content: space-between; align-items: center; }
.brand { font-size: 18px; font-weight: bold; }
.nav-links a { color: #ffc107; text-decoration: none; margin-left: 15px; }
.nav-links a:hover { color: white; }
.active-link { color: white !important; border-bottom: 2px solid #ffc107; padding-bottom: 3px; }
.container { padding: 30px; }
.search-box { margin-bottom: 20px; display: flex; gap: 10px; align-items: center; }
.search-box input[type=search] { width: 320px; padding: 8px; box-sizing: border-box; }
.search-box select { padding: 8px; border: 1px solid #ccc; border-radius: 3px; background: white; }
.search-box button { padding: 8px 15px; background: #007bff; color: white; border: none; cursor: pointer; border-radius: 3px; }
.add-book-card { background: white; padding: 20px; width: 420px; border-radius: 5px; margin-bottom: 25px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
.add-book-card h3 { margin-top: 0; color: #333; }
.add-book-card input { width: 95%; padding: 8px; margin-bottom: 12px; border: 1px solid #ccc; border-radius: 3px; }
.add-book-card button { padding: 9px 18px; background: #28a745; color: white; border: none; border-radius: 3px; cursor: pointer; font-weight: bold; }
table { width: 100%; border-collapse: collapse; background: white; }
th, td { border: 1px solid #ddd; padding: 10px; text-align: left; }
th { background: #f2f2f2; }
.request-btn { background: #007bff; color: white; border: none; padding: 6px 12px; cursor: pointer; border-radius: 3px; }
.edit-btn { background: #ffc107; color: #333; border: none; padding: 6px 12px; cursor: pointer; border-radius: 3px; font-weight: bold; }
.out-btn { background: #6c757d; color: white; border: none; padding: 6px 12px; cursor: not-allowed; border-radius: 3px; opacity: 0.6; }

/* MODAL STYLES FOR EDIT BOOK */
.modal { display: none; position: fixed; z-index: 1000; left: 0; top: 0; width: 100%; height: 100%; background-color: rgba(0,0,0,0.5); }
.modal-content { background-color: #fff; margin: 10% auto; padding: 25px; border-radius: 5px; width: 400px; box-shadow: 0 4px 8px rgba(0,0,0,0.2); }
.modal-content h3 { margin-top: 0; }
.modal-content label { font-size: 13px; font-weight: bold; color: #555; }
.modal-content input { width: 95%; padding: 8px; margin: 5px 0 15px 0; border: 1px solid #ccc; border-radius: 3px; }
.modal-actions { display: flex; justify-content: flex-end; gap: 10px; }
.save-btn { background: #28a745; color: white; border: none; padding: 8px 16px; border-radius: 3px; cursor: pointer; }
.close-btn { background: #6c757d; color: white; border: none; padding: 8px 16px; border-radius: 3px; cursor: pointer; }
</style>
</head>
<body>

<div class="navbar">
    <div class="brand">Library Management System</div>
    <div class="nav-links">
        <a href="<%= contextPath %>/dashboard">Dashboard</a>
        <a href="<%= contextPath %>/books" class="active-link">Books</a>
        <% if(user != null && "ADMIN".equalsIgnoreCase(user.getRole())) { %>
            <a href="<%= contextPath %>/transactions">Transactions</a>
            <a href="<%= contextPath %>/requests">Book Requests</a>
        <% } else { %>
            <a href="<%= contextPath %>/transactions">My History</a>
            <a href="<%= contextPath %>/requests">My Requests</a>
        <% } %>
        <a href="<%= contextPath %>/auth">Logout</a>
    </div>
</div>

<div class="container">
<h2>Book Inventory</h2>

<!-- ADMIN ONLY: ADD NEW BOOK FORM -->
<% if(user != null && "ADMIN".equalsIgnoreCase(user.getRole())) { %>
<div class="add-book-card">
    <h3>+ Add New Book</h3>
    <form action="<%= contextPath %>/books" method="post">
        <input type="hidden" name="action" value="add">
        <input type="text" name="title" placeholder="Book Title" required>
        <input type="text" name="author" placeholder="Author" required>
        <input type="text" name="category" placeholder="Category (e.g. Data Science)" required>
        <input type="number" name="copies" placeholder="Total Copies" min="1" required>
        <button type="submit">Add Book to Catalog</button>
    </form>
</div>
<% } %>

<!-- REAL-TIME SEARCH & CATEGORY FILTER -->
<div class="search-box">
    <form action="<%= contextPath %>/books" method="get" id="searchForm" style="display: flex; gap: 10px;">
        <input 
            type="search" 
            id="searchInput"
            name="search" 
            placeholder="Search Title, Author, or Category..." 
            value="<%= search %>"
            oninput="filterBooksRealtime()"
            onsearch="filterBooksRealtime()"
        >
        <select id="categoryFilter" onchange="filterBooksRealtime()">
            <option value="">All Categories</option>
            <% 
            if (books != null && !books.isEmpty()) {
                java.util.Set<String> categories = new java.util.HashSet<>();
                for (Book b : books) {
                    if (b.getCategory() != null && !b.getCategory().trim().isEmpty()) {
                        categories.add(b.getCategory().trim());
                    }
                }
                for (String cat : categories) {
            %>
                <option value="<%= cat.toLowerCase() %>"><%= cat %></option>
            <% 
                }
            } 
            %>
        </select>
        <button type="submit">Search</button>
    </form>
</div>

<!-- BOOKS TABLE -->
<table id="booksTable">
<thead>
    <tr>
        <th>ID</th>
        <th>Title</th>
        <th>Author</th>
        <th>Category</th>
        <th>Total Copies</th>
        <th>Available Copies</th>
        <th>Action</th>
    </tr>
</thead>
<tbody>
<%
if (books != null && !books.isEmpty()) {
    for (Book b : books) {
%>
    <tr class="book-row">
        <td><%= b.getId() %></td>
        <td class="book-title"><%= b.getTitle() %></td>
        <td class="book-author"><%= b.getAuthor() %></td>
        <td class="book-category"><%= b.getCategory() %></td>
        <td><%= b.getTotalCopies() %></td>
        <td><%= b.getAvailableCopies() %></td>
        <td>
        <% if(user != null && "ADMIN".equalsIgnoreCase(user.getRole())) { %>
            <button 
                type="button" 
                class="edit-btn" 
                onclick="openEditModal(<%= b.getId() %>, '<%= b.getTitle().replace("'", "\\'") %>', '<%= b.getAuthor().replace("'", "\\'") %>', '<%= b.getCategory().replace("'", "\\'") %>', <%= b.getTotalCopies() %>)">
                Edit
            </button>
        <% } else { %>
            <% if(b.getAvailableCopies() > 0) { %>
                <form action="<%= contextPath %>/requests" method="post" style="display:inline;">
                    <!-- Fixed: Value matches action checks in RequestServlet -->
                    <input type="hidden" name="action" value="create">
                    <input type="hidden" name="bookId" value="<%= b.getId() %>">
                    <button type="submit" class="request-btn">Request Book</button>
                </form>
            <% } else { %>
                <button type="button" class="out-btn" disabled>Unavailable</button>
            <% } %>
        <% } %>
        </td>
    </tr>
<%
    }
} else {
%>
    <tr id="noBooksRow">
        <td colspan="7" style="text-align: center;">No books found matching your query.</td>
    </tr>
<%
}
%>
    <tr id="noRealtimeResultsRow" style="display: none;">
        <td colspan="7" style="text-align: center;">No matching books found.</td>
    </tr>
</tbody>
</table>
</div>

<!-- EDIT BOOK MODAL (ADMIN ONLY) -->
<% if(user != null && "ADMIN".equalsIgnoreCase(user.getRole())) { %>
<div id="editModal" class="modal">
    <div class="modal-content">
        <h3>Edit Book Details</h3>
        <form action="<%= contextPath %>/books" method="post">
            <input type="hidden" name="action" value="edit">
            <input type="hidden" id="editBookId" name="bookId">

            <label>Title:</label>
            <input type="text" id="editTitle" name="title" required>

            <label>Author:</label>
            <input type="text" id="editAuthor" name="author" required>

            <label>Category:</label>
            <input type="text" id="editCategory" name="category" required>

            <label>Total Copies:</label>
            <input type="number" id="editTotalCopies" name="totalCopies" min="1" required>

            <div class="modal-actions">
                <button type="button" class="close-btn" onclick="closeEditModal()">Cancel</button>
                <button type="submit" class="save-btn">Save Changes</button>
            </div>
        </form>
    </div>
</div>

<script>
function openEditModal(id, title, author, category, copies) {
    document.getElementById('editBookId').value = id;
    document.getElementById('editTitle').value = title;
    document.getElementById('editAuthor').value = author;
    document.getElementById('editCategory').value = category;
    document.getElementById('editTotalCopies').value = copies;
    document.getElementById('editModal').style.display = 'block';
}

function closeEditModal() {
    document.getElementById('editModal').style.display = 'none';
}

window.onclick = function(event) {
    var modal = document.getElementById('editModal');
    if (event.target === modal) {
        modal.style.display = 'none';
    }
}
</script>
<% } %>

<script>
function filterBooksRealtime() {
    var searchInput = document.getElementById('searchInput').value.toLowerCase().replace(/\s+/g, '');
    var selectedCategory = document.getElementById('categoryFilter').value.toLowerCase();
    var rows = document.querySelectorAll('.book-row');
    var visibleCount = 0;

    rows.forEach(function(row) {
        var title = row.querySelector('.book-title').textContent.toLowerCase().replace(/\s+/g, '');
        var author = row.querySelector('.book-author').textContent.toLowerCase().replace(/\s+/g, '');
        var category = row.querySelector('.book-category').textContent.toLowerCase().replace(/\s+/g, '');
        var fullCategoryText = row.querySelector('.book-category').textContent.toLowerCase().trim();

        var matchesSearch = (title.indexOf(searchInput) > -1) || 
                            (author.indexOf(searchInput) > -1) || 
                            (category.indexOf(searchInput) > -1);

        var matchesCategory = (selectedCategory === '') || (fullCategoryText === selectedCategory);

        if (matchesSearch && matchesCategory) {
            row.style.display = '';
            visibleCount++;
        } else {
            row.style.display = 'none';
        }
    });

    var noResultsRow = document.getElementById('noRealtimeResultsRow');
    if (noResultsRow) {
        if (visibleCount === 0 && rows.length > 0) {
            noResultsRow.style.display = '';
        } else {
            noResultsRow.style.display = 'none';
        }
    }
}

// Apply real-time filter automatically on page load if search term exists
document.addEventListener("DOMContentLoaded", function() {
    filterBooksRealtime();
});
</script>

</body>
</html>