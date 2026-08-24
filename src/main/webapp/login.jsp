<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8"><title>University Library Portal Login</title>
<style>
  body { margin: 0; font-family: 'Segoe UI', Tahoma, sans-serif; background: linear-gradient(135deg, #1e3c72, #2a5298); min-height: 100vh; display: flex; align-items: center; justify-content: center; }
  .card { background: #fff; width: 340px; padding: 35px 30px; border-radius: 12px; box-shadow: 0 10px 25px rgba(0,0,0,0.2); text-align: center; }
  h2 { color: #1e3c72; font-size: 20px; font-weight: 700; margin: 0 0 4px; text-transform: uppercase; }
  .sub { color: #444; font-size: 13px; font-weight: 600; margin-bottom: 22px; }
  input { width: 100%; padding: 11px 12px; margin-bottom: 15px; border: 1px solid #ccc; border-radius: 6px; box-sizing: border-box; font-size: 14px; outline: none; transition: 0.3s; }
  input:focus { border-color: #1e3c72; box-shadow: 0 0 5px rgba(30,60,114,0.3); }
  button { width: 100%; padding: 12px; background: #1e3c72; color: #fff; border: none; border-radius: 6px; font-size: 15px; font-weight: 600; cursor: pointer; transition: 0.3s; }
  button:hover { background: #2a5298; }
</style>
</head>
<body>
<div class="card">
  <h2>LAMRIN TECH SKILLS UNIVERSITY, PUNJAB</h2>
  <div class="sub">University Library Management Portal</div>
  <% if(request.getParameter("error") != null) { %>
    <p style="color:#dc3545; font-size:13px; margin: 0 0 15px;"><%= request.getParameter("error") %></p>
  <% } %>
  <form action="auth" method="post">
    <input type="hidden" name="action" value="login">
    <input type="text" name="username" placeholder="admin1,2 or student1,2,3..9." required>
    <input type="password" name="password" placeholder="password: 123" required>
    <button type="submit">Sign In</button>
  </form>
</div>
</body>
</html>