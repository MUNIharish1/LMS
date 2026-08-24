<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.List"%>
<%@ page import="library.management.system.User"%>
<%@ page import="library.management.system.BookRequest"%>

<%
User user = (User) session.getAttribute("user");
if (user == null) {
    response.sendRedirect(request.getContextPath() + "/login.jsp");
    return;
}

@SuppressWarnings("unchecked")
List<BookRequest> requestList = (List<BookRequest>) request.getAttribute("requestList");
String contextPath = request.getContextPath();
%>

<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>Book Requests - Library System</title>
<style>
body { font-family: Arial, sans-serif; margin: 0; background: #f4f6f9; }
.navbar { background: #343a40; color: white; padding: 15px 20px; display: flex; justify-content: space-between; align-items: center; }
.brand { font-size: 18px; font-weight: bold; }
.nav-links a { color: #ffc107; text-decoration: none; margin-left: 15px; }
.nav-links a:hover { color: white; }
.active-link { color: white !important; border-bottom: 2px solid #ffc107; padding-bottom: 3px; }
.container { padding: 30px; }
table { width: 100%; border-collapse: collapse; background: white; margin-top: 20px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
th, td { border: 1px solid #ddd; padding: 12px; text-align: left; }
th { background: #f2f2f2; font-weight: bold; color: #333; }
tr:hover { background-color: #f8f9fa; }
.badge { padding: 4px 8px; border-radius: 4px; font-size: 12px; font-weight: bold; text-transform: uppercase; }
.badge-pending { background-color: #ffc107; color: #333; }
.badge-approved { background-color: #28a745; color: white; }
.badge-rejected { background-color: #dc3545; color: white; }
.approve-btn { background: #28a745; color: white; border: none; padding: 6px 12px; cursor: pointer; border-radius: 3px; font-weight: bold; margin-right: 5px; }
.reject-btn { background: #dc3545; color: white; border: none; padding: 6px 12px; cursor: pointer; border-radius: 3px; font-weight: bold; }
</style>
</head>
<body>

<!-- UNIFIED NAVBAR -->
<div class="navbar">
    <div class="brand">Library Management System</div>
    <div class="nav-links">
        <a href="<%= contextPath %>/dashboard">Dashboard</a>
        <a href="<%= contextPath %>/books">Books</a>
        <% if("ADMIN".equalsIgnoreCase(user.getRole())) { %>
            <a href="<%= contextPath %>/transactions">Transactions</a>
            <a href="<%= contextPath %>/requests" class="active-link">Book Requests</a>
        <% } else { %>
            <a href="<%= contextPath %>/transactions">My History</a>
            <a href="<%= contextPath %>/requests" class="active-link">My Requests</a>
        <% } %>
        <a href="<%= contextPath %>/auth">Logout</a>
    </div>
</div>

<div class="container">
    <h2><%= "ADMIN".equalsIgnoreCase(user.getRole()) ? "Pending Book Requests" : "My Book Requests" %></h2>

    <table>
    <thead>
        <tr>
            <th>Request ID</th>
            <% if("ADMIN".equalsIgnoreCase(user.getRole())) { %>
                <th>Student ID</th>
                <th>Student Name</th>
            <% } %>
            <th>Book ID</th>
            <th>Book Title</th>
            <th>Request Date</th>
            <th>Status</th>
            <% if("ADMIN".equalsIgnoreCase(user.getRole())) { %>
                <th>Action</th>
            <% } %>
        </tr>
    </thead>
    <tbody>
    <%
    if (requestList != null && !requestList.isEmpty()) {
        for (BookRequest req : requestList) {
            String statusClass = "badge-pending";
            if ("APPROVED".equalsIgnoreCase(req.getStatus())) statusClass = "badge-approved";
            else if ("REJECTED".equalsIgnoreCase(req.getStatus())) statusClass = "badge-rejected";
    %>
        <tr>
            <td><%= req.getId() %></td>
            <% if("ADMIN".equalsIgnoreCase(user.getRole())) { %>
                <td><%= req.getUserId() %></td>
                <td><%= req.getUsername() != null ? req.getUsername() : "N/A" %></td>
            <% } %>
            <td><%= req.getBookId() %></td>
            <td><%= req.getBookTitle() != null ? req.getBookTitle() : "N/A" %></td>
            <td><%= req.getRequestDate() %></td>
            <td><span class="badge <%= statusClass %>"><%= req.getStatus() %></span></td>
            <% if("ADMIN".equalsIgnoreCase(user.getRole())) { %>
                <td>
                <% if("PENDING".equalsIgnoreCase(req.getStatus())) { %>
                    <form action="<%= contextPath %>/requests" method="post" style="display:inline;">
                        <input type="hidden" name="action" value="approve">
                        <input type="hidden" name="requestId" value="<%= req.getId() %>">
                        <button type="submit" class="approve-btn">Approve</button>
                    </form>
                    <form action="<%= contextPath %>/requests" method="post" style="display:inline;">
                        <input type="hidden" name="action" value="reject">
                        <input type="hidden" name="requestId" value="<%= req.getId() %>">
                        <button type="submit" class="reject-btn">Reject</button>
                    </form>
                <% } else { %>
                    -
                <% } %>
                </td>
            <% } %>
        </tr>
    <%
        }
    } else {
    %>
        <tr>
            <td colspan="<%= "ADMIN".equalsIgnoreCase(user.getRole()) ? "8" : "5" %>" style="text-align: center;">No book requests found.</td>
        </tr>
    <%
    }
    %>
    </tbody>
    </table>
</div>

</body>
</html>