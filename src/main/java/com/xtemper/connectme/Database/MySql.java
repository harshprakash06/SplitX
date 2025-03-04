package com.xtemper.connectme.Database;

import org.springframework.stereotype.Component;
import java.sql.*;

@Component
public class MySql {
    public static Connection runQuery() {
        try {
            return DriverManager.getConnection(""); //database url
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void closeConnection(Connection connection) {
        try {
            if (connection != null) connection.close();
        } catch (SQLException ignored) {}
    }

    public static void main(String[] args) {
        Connection connection = runQuery();
        if (connection != null) {
            try (Statement statement = connection.createStatement();
                 ResultSet resultSet = statement.executeQuery("SELECT * FROM userDetails WHERE userId = 1")) {

                while (resultSet.next()) {
                    System.out.println(resultSet.getString("messageIds"));
                }

            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                closeConnection(connection);
            }
        }
    }
}
