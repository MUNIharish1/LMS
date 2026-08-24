<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="library.management.system.User"%>

<%
User user = (User) session.getAttribute("user");
if (user == null) {
    response.sendRedirect(request.getContextPath() + "/login.jsp");
    return;
}

String contextPath = request.getContextPath();

Integer totalBooks = (Integer) request.getAttribute("totalBooks");
Integer totalBookCopies = (Integer) request.getAttribute("totalBookCopies");
Integer totalUsers = (Integer) request.getAttribute("totalUsers");
Integer activeLoans = (Integer) request.getAttribute("activeLoans");
Integer pendingRequests = (Integer) request.getAttribute("pendingRequests");

Integer myActiveLoans = (Integer) request.getAttribute("myActiveLoans");
Integer myRequests = (Integer) request.getAttribute("myRequests");
Double myFine = (Double) request.getAttribute("myFine");
%>

<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>Dashboard - Library System</title>
<style>
body { font-family: Arial, sans-serif; margin: 0; background: #f4f6f9; }
.navbar { background: #343a40; color: white; padding: 15px 20px; display: flex; justify-content: space-between; align-items: center; }
.brand { font-size: 18px; font-weight: bold; }
.nav-links a { color: #ffc107; text-decoration: none; margin-left: 15px; }
.nav-links a:hover { color: white; }
.active-link { color: white !important; border-bottom: 2px solid #ffc107; padding-bottom: 3px; }
.container { padding: 30px; }
.welcome-card { background: white; padding: 20px; border-radius: 5px; margin-bottom: 25px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
.stats-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 20px; }
.stat-card { background: white; padding: 20px; border-radius: 5px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); border-left: 5px solid #007bff; }
.stat-card.warning { border-left-color: #ffc107; }
.stat-card.danger { border-left-color: #dc3545; }
.stat-card.success { border-left-color: #28a745; }
.stat-card h3 { margin: 0 0 10px 0; font-size: 14px; color: #6c757d; text-transform: uppercase; }
.stat-card .value { font-size: 28px; font-weight: bold; color: #333; }
.stat-card .sub-value { font-size: 13px; color: #6c757d; margin-top: 5px; font-weight: normal; }
</style>
</head>
<body>

<div class="navbar">
    <div class="brand">Library Management System</div>
    <div class="nav-links">
        <a href="<%= contextPath %>/dashboard" class="active-link">Dashboard</a>
        <a href="<%= contextPath %>/books">Books</a>
        <% if("ADMIN".equalsIgnoreCase(user.getRole())) { %>
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
    <div class="welcome-card">
        <h2>Welcome back, <%= user.getUsername() %>!</h2>
        <p>Role: <strong><%= user.getRole() %></strong></p>
    </div>

    <h3>Overview</h3>
    <div class="stats-grid">
    <% if("ADMIN".equalsIgnoreCase(user.getRole())) { %>
        <div class="stat-card">
            <h3>Total Book Copies</h3>
            <div class="value"><%= (totalBookCopies != null) ? totalBookCopies : 0 %></div>
            <div class="sub-value"><%= (totalBooks != null) ? totalBooks : 0 %> Unique Titles</div>
        </div>
        <div class="stat-card success">
            <h3>Registered Students</h3>
            <div class="value"><%= (totalUsers != null) ? totalUsers : 0 %></div>
        </div>
        <div class="stat-card warning">
            <h3>Active Books Issued</h3>
            <div class="value"><%= (activeLoans != null) ? activeLoans : 0 %></div>
        </div>
        <div class="stat-card danger">
            <h3>Pending Requests</h3>
            <div class="value"><%= (pendingRequests != null) ? pendingRequests : 0 %></div>
        </div>
    <% } else { %>
        <div class="stat-card warning">
            <h3>Books Currently Borrowed</h3>
            <div class="value"><%= (myActiveLoans != null) ? myActiveLoans : 0 %></div>
        </div>
        <div class="stat-card">
            <h3>My Book Requests</h3>
            <div class="value"><%= (myRequests != null) ? myRequests : 0 %></div>
        </div>
        <div class="stat-card danger">
            <h3>Outstanding Fine</h3>
            <div class="value">₹<%= (myFine != null) ? String.format("%.2f", myFine) : "0.00" %></div>
        </div>
    <% } %>
    </div>
</div>

</body>
</html>