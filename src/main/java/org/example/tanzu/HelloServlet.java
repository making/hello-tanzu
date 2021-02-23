package org.example.tanzu;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

@WebServlet(name = "hello", urlPatterns = "/")
public class HelloServlet extends HttpServlet {
	private DataSource dataSource;

	private DataSource dataSourceReadOnly;

	@Override
	public void init(ServletConfig config) throws ServletException {
		this.dataSource = (DataSource) config.getServletContext().getAttribute("dataSource");
		this.dataSourceReadOnly = (DataSource) config.getServletContext().getAttribute("dataSourceReadOnly");
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse res)
			throws ServletException, IOException {
		res.setContentType("text/html;charset=utf-8");
		PrintWriter pw = res.getWriter();
		pw.print("<!doctype html>");
		pw.print("<html>");
		pw.print("<head>");
		pw.print("<meta charset=\"utf-8\"/>");
		pw.print("<link rel=\"shortcut icon\" type=\"image/x-icon\" href=\"https://avatars.githubusercontent.com/u/54452117?s=64\">");
		pw.print("<title>Hello Tanzu</title>");
		pw.print("</head>");
		pw.print("<body>");
		this.printHello(pw);
		if (this.dataSource != null) {
			this.printAccessLog(pw, req);
		}
		pw.print("</body>");
		pw.print("</html>");
		pw.flush();
	}

	void printHello(PrintWriter pw) {
		pw.print("<h3>");
		pw.print("Hello Tanzu!");
		pw.print("</h3>");
		pw.print("<p>");
		pw.print("<img src=\"https://avatars.githubusercontent.com/u/54452117?s=160\"/>");
		pw.print("<br/>");
		pw.print("<br/>");
		pw.print("It works!");
		pw.print("</p>");
	}

	void printAccessLog(PrintWriter pw, HttpServletRequest req) {
		pw.print("<h4>");
		pw.print("Access Log");
		pw.print("</h4>");
		final List<String> servers = new ArrayList<>();
		try (final Connection connection = this.dataSource.getConnection()) {
			connection.setAutoCommit(false);
			try (final PreparedStatement prepareStatement = connection.prepareStatement("INSERT INTO access_log(ip) VALUES (?)")) {
				prepareStatement.setString(1, req.getRemoteAddr());
				prepareStatement.executeUpdate();
				connection.commit();
			}
			catch (SQLException e) {
				connection.rollback();
				throw e;
			}
			try (final PreparedStatement serverStatement = connection.prepareStatement("SELECT inet_server_addr(), inet_server_port()");
				 final ResultSet serverResultSet = serverStatement.executeQuery()) {
				serverResultSet.next();
				servers.add(String.format("<tr><td>Write</td><td>%s:%s</td></tr>", serverResultSet.getString(1), serverResultSet.getString(2)));
			}
		}
		catch (SQLException e) {
			servers.add(String.format("<tr><td>Write</td><td>%s</td></tr>", e.getMessage()));
		}
		try (final Connection connection = this.dataSourceReadOnly.getConnection();
			 final PreparedStatement preparedStatement = connection.prepareStatement("SELECT ip, created_at FROM access_log ORDER BY created_at DESC LIMIT 10");
			 final ResultSet resultSet = preparedStatement.executeQuery()) {
			pw.print("<table>");
			pw.print("<tr><th>IP</th><th>Timestamp</th></tr>");
			while (resultSet.next()) {
				pw.print("<tr>");
				pw.print("<td>");
				pw.print(resultSet.getString("ip"));
				pw.print("</td>");
				pw.print("<td>");
				pw.print(resultSet.getTimestamp("created_at"));
				pw.print("</td>");
				pw.print("</tr>");
			}
			pw.print("</table>");
			try (final PreparedStatement serverStatement = connection.prepareStatement("SELECT inet_server_addr(), inet_server_port()");
				 final ResultSet serverResultSet = serverStatement.executeQuery()) {
				serverResultSet.next();
				servers.add(String.format("<tr><td>Read</td><td>%s:%s</td></tr>", serverResultSet.getString(1), serverResultSet.getString(2)));
			}
		}
		catch (SQLException e) {
			servers.add(String.format("<tr><td>Read</td><td>%s</td></tr>", e.getMessage()));
		}
		pw.print("<h4>");
		pw.print("PostgreSQL Info");
		pw.print("</h4>");
		pw.print("<table>");
		pw.print("<tr><th>Type</th><th>Server</th></tr>");
		servers.forEach(pw::print);
		pw.print("</table>");
	}
}