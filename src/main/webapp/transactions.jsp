<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.List"%>
<%@ page import="library.management.system.User"%>
<%@ page import="library.management.system.Transaction"%>

<%
User user = (User) session.getAttribute("user");
if (user == null) {
    response.sendRedirect(request.getContextPath() + "/login.jsp");
    return;
}

String contextPath = request.getContextPath();
List<Transaction> transactions = (List<Transaction>) request.getAttribute("transactions");
%>

<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title><%= "ADMIN".equalsIgnoreCase(user.getRole()) ? "Transactions" : "My History" %></title>
<style>
body { font-family: Arial, sans-serif; margin: 0; background: #f4f6f9; }
.navbar { background: #343a40; color: white; padding: 15px 20px; display: flex; justify-content: space-between; align-items: center; }
.brand { font-size: 18px; font-weight: bold; }
.nav-links a { color: #ffc107; text-decoration: none; margin-left: 15px; }
.nav-links a:hover { color: white; }
.active-link { color: white !important; border-bottom: 2px solid #ffc107; padding-bottom: 3px; }
.container { padding: 30px; }
table { width: 100%; border-collapse: collapse; background: white; border-radius: 5px; overflow: hidden; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
th, td { padding: 12px 15px; text-align: left; border-bottom: 1px solid #ddd; }
th { background: #007bff; color: white; font-weight: 600; }
tr:hover { background: #f1f1f1; }
.badge { padding: 4px 8px; border-radius: 4px; font-size: 12px; font-weight: bold; color: white; }
.badge-issued { background: #ffc107; color: #212529; }
.badge-returned { background: #28a745; }
.badge-rejected { background: #dc3545; }
.btn-return { background: #28a745; color: white; border: none; padding: 6px 12px; border-radius: 4px; cursor: pointer; }
.btn-return:hover { background: #218838; }
</style>
</head>
<body>

<div class="navbar">
    <div class="brand">Library Management System</div>
    <div class="nav-links">
        <a href="<%= contextPath %>/dashboard">Dashboard</a>
        <a href="<%= contextPath %>/books">Books</a>
        <% if("ADMIN".equalsIgnoreCase(user.getRole())) { %>
            <a href="<%= contextPath %>/transactions" class="active-link">Transactions</a>
            <a href="<%= contextPath %>/requests">Book Requests</a>
        <% } else { %>
            <a href="<%= contextPath %>/transactions" class="active-link">My History</a>
            <a href="<%= contextPath %>/requests">My Requests</a>
        <% } %>
        <a href="<%= contextPath %>/auth">Logout</a>
    </div>
</div>

<div class="container">
    <h2><%= "ADMIN".equalsIgnoreCase(user.getRole()) ? "All System Transactions" : "My Borrowing History" %></h2>

    <table>
        <thead>
            <tr>
                <th>ID</th>
                <% if("ADMIN".equalsIgnoreCase(user.getRole())) { %>
                    <th>Student Name</th>
                <% } %>
                <th>Book Title</th>
                <th>Issue / Request Date</th>
                <th>Due Date</th>
                <th>Return Date</th>
                <th>Fine Amount</th>
                <th>Status</th>
                <% if("ADMIN".equalsIgnoreCase(user.getRole())) { %>
                    <th>Action</th>
                <% } %>
            </tr>
        </thead>
        <tbody>
        <% if (transactions != null && !transactions.isEmpty()) { 
            for (Transaction t : transactions) { %>
            <tr>
                <td><%= t.getId() %></td>
                <% if("ADMIN".equalsIgnoreCase(user.getRole())) { %>
                    <td><%= t.getStudentName() %></td>
                <% } %>
                <td><%= t.getBookTitle() %></td>
                <td><%= t.getIssueDate() != null ? t.getIssueDate() : "-" %></td>
                <td><%= t.getDueDate() != null ? t.getDueDate() : "-" %></td>
                <td><%= t.getReturnDate() != null ? t.getReturnDate() : "-" %></td>
                <td>₹<%= String.format("%.2f", t.getFineAmount()) %></td>
                <td>
                    <% if ("ISSUED".equalsIgnoreCase(t.getStatus())) { %>
                        <span class="badge badge-issued">ISSUED</span>
                    <% } else if ("RETURNED".equalsIgnoreCase(t.getStatus())) { %>
                        <span class="badge badge-returned">RETURNED</span>
                    <% } else if ("REJECTED".equalsIgnoreCase(t.getStatus())) { %>
                        <span class="badge badge-rejected">REJECTED</span>
                    <% } else { %>
                        <span class="badge badge-issued"><%= t.getStatus() %></span>
                    <% } %>
                </td>
                <% if("ADMIN".equalsIgnoreCase(user.getRole())) { %>
                    <td>
                        <% if ("ISSUED".equalsIgnoreCase(t.getStatus())) { %>
                            <form action="<%= contextPath %>/transactions" method="post" style="display:inline;">
                                <input type="hidden" name="action" value="returnBook">
                                <input type="hidden" name="transactionId" value="<%= t.getId() %>">
                                <button type="submit" class="btn-return">Mark Returned</button>
                            </form>
                        <% } else { %>
                            -
                        <% } %>
                    </td>
                <% } %>
            </tr>
        <%  } 
           } else { %>
            <tr>
                <td colspan="<%= "ADMIN".equalsIgnoreCase(user.getRole()) ? 9 : 7 %>" style="text-align: center;">No history found.</td>
            </tr>
        <% } %>
        </tbody>
    </table>
</div>

</body>
</html>