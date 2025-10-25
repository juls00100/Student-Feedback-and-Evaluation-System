/*
 * This file contains the corrected database configuration and utility methods.
 * It is designed to work with the provided main.java file.
 */
package config;

import java.sql.*;

public class config {
    // Member variable to hold the database connection
    private Connection conn;

    /**
     * Establishes a connection to the SQLite database.
     */
    public void connectDB() {
        try {
            Class.forName("org.sqlite.JDBC");
            if (this.conn == null || this.conn.isClosed()) {
            this.conn = DriverManager.getConnection("jdbc:sqlite:Evaluation System.db");
            System.out.println("LET'S CREATE INFINITE LOOP OF ROMANCE, LOVE U <3");
            }      
        } catch (Exception e) {
            System.out.println("Connection Failed: " + e.getMessage());
        }
    }
    
    /**
     * Adds a new record to the database using a prepared statement.
     * @param sql The SQL INSERT statement with '?' placeholders.
     * @param values The values to be inserted, corresponding to the '?' placeholders.
     */
    public void addRecord(String sql, Object... values) {
    connectDB(); 
    if (this.conn == null) {
        System.out.println("cannot add record. Check Database connection! ");
        return;
    }
    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < values.length; i++) {
                if (values[i] instanceof Integer) {
                    pstmt.setInt(i + 1, (Integer) values[i]);
                } else if (values[i] instanceof Double) {
                    pstmt.setDouble(i + 1, (Double) values[i]);
                } else {
                    pstmt.setString(i + 1, values[i].toString());
                }
            }
            pstmt.executeUpdate();
            System.out.println("Record added successfully!");
        } catch (SQLException e) {
            System.out.println("Error adding record: " + e.getMessage());
        }
    }
    
    /**
     * Retrieves records from the database using a prepared statement.
     * This method is a general-purpose way to get data for any query with parameters.
     * @param sql The SQL query with '?' placeholders.
     * @param params The values to be set in the prepared statement.
     * @return A ResultSet containing the query results, or null if an error occurs.
     */
    /*public ResultSet getRecords(String sql, Object... params) {
        try {
            PreparedStatement pstmt = conn.prepareStatement(sql);
            
            // Loop through the parameters and set them in the PreparedStatement
            for (int i = 0; i < params.length; i++) {
                pstmt.setObject(i + 1, params[i]);
            }
            
            return pstmt.executeQuery();
        } catch (SQLException e) {
            System.err.println("Error executing query with parameters: " + e.getMessage());
            return null;
        }
    }
*/
    /**
     * Updates an existing record in the database using a prepared statement.
     * @param sql The SQL UPDATE statement with '?' placeholders.
     * @param values The values to be used in the UPDATE clause, followed by the WHERE clause value(s).
     */
    public void updateRecord(String sql, Object... values) {
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < values.length; i++) {
                if (values[i] instanceof Integer) {
                    pstmt.setInt(i + 1, (Integer) values[i]);
                } else if (values[i] instanceof Double) {
                    pstmt.setDouble(i + 1, (Double) values[i]);
                } else {
                    pstmt.setString(i + 1, values[i].toString());
                }
            }
            pstmt.executeUpdate();
            System.out.println("Record updated successfully!");
        } catch (SQLException e) {
            System.out.println("Error updating record: " + e.getMessage());
        }
    }
    
    /**
     * Deletes a record from the database using a prepared statement.
     * @param sql The SQL DELETE statement with '?' placeholders.
     * @param values The value(s) for the WHERE clause.
     */
    public void deleteRecord(String sql, Object... values) {
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < values.length; i++) {
                if (values[i] instanceof Integer) {
                    pstmt.setInt(i + 1, (Integer) values[i]);
                } else {
                    pstmt.setString(i + 1, values[i].toString());
                }
            }
            pstmt.executeUpdate();
            System.out.println("Record deleted successfully!");
        } catch (SQLException e) {
            System.out.println("Error deleting record: " + e.getMessage());
        }
    }

    /**
     * Checks for the existence of a record based on a query.
     * @param query The SQL query with '?' placeholders.
     * @param params The parameters to be set in the prepared statement.
     * @return true if a record exists, false otherwise.
     */
    public boolean checkRecord(String query, String... params) {
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = this.conn.prepareStatement(query);
            for (int i = 0; i < params.length; i++) {
                pstmt.setString(i + 1, params[i]);
            }
            rs = pstmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            System.err.println("Error checking record: " + e.getMessage());
            return false;
        } finally {
            try {
                if (rs != null) rs.close();
                if (pstmt != null) pstmt.close();
            } catch (SQLException e) {
                System.err.println("Error closing resources: " + e.getMessage());
            }
        }
    }

    /**
     * Views records from a table and prints them in a formatted table.
     * @param sqlQuery The SQL SELECT query.
     * @param columnHeaders An array of headers for the table.
     * @param columnNames An array of column names from the query results.
     */
    public void viewRecords(String sqlQuery, String[] columnHeaders, String[] columnNames) {
        if (columnHeaders.length != columnNames.length) {
            System.out.println("Error: Mismatch between column headers and column names.");
            return;
        }
        try (PreparedStatement pstmt = conn.prepareStatement(sqlQuery);
             ResultSet rs = pstmt.executeQuery()) {
            printTable(rs, columnHeaders, columnNames);
        } catch (SQLException e) {
            System.out.println("Error retrieving records: " + e.getMessage());
        }
    }

    /**
     * Overloaded viewRecords method for parameterized queries.
     * @param sqlQuery The SQL SELECT query with '?' placeholders.
     * @param columnHeaders An array of headers for the table.
     * @param columnNames An array of column names from the query results.
     * @param values The values to be set in the prepared statement.
     */
    public void viewRecords(String sqlQuery, String[] columnHeaders, String[] columnNames, Object... values) {
        if (columnHeaders.length != columnNames.length) {
            System.out.println("Error: Mismatch between column headers and column names.");
            return;
        }
        try (PreparedStatement pstmt = conn.prepareStatement(sqlQuery)) {
            for (int i = 0; i < values.length; i++) {
                if (values[i] instanceof Integer) {
                    pstmt.setInt(i + 1, (Integer) values[i]);
                } else {
                    pstmt.setString(i + 1, values[i].toString());
                }
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                printTable(rs, columnHeaders, columnNames);
            }
        } catch (SQLException e) {
            System.out.println("Error retrieving records: " + e.getMessage());
        }
    }

    /**
     * Helper method to print the result set in a formatted table.
     * @param rs The ResultSet to print.
     * @param columnHeaders The headers for the table columns.
     * @param columnNames The names of the columns to retrieve from the ResultSet.
     * @throws SQLException if a database access error occurs.
     */
    private void printTable(ResultSet rs, String[] columnHeaders, String[] columnNames) throws SQLException {
        StringBuilder headerLine = new StringBuilder();
        headerLine.append("----------------------------------------------------------------------------------------------------------------------------------------\n| ");
        for (String header : columnHeaders) {
            headerLine.append(String.format("%-20s | ", header));
        }
        headerLine.append("\n----------------------------------------------------------------------------------------------------------------------------------------");
        System.out.println(headerLine.toString());

        while (rs.next()) {
            StringBuilder row = new StringBuilder("| ");
            for (String colName : columnNames) {
                String value = rs.getString(colName);
                row.append(String.format("%-20s | ", value != null ? value : ""));
            }
            System.out.println(row.toString());
        }
        System.out.println("----------------------------------------------------------------------------------------------------------------------------------------");
    }

public java.util.List<java.util.Map<String, Object>> fetchRecords(String sqlQuery, Object... values) {
    java.util.List<java.util.Map<String, Object>> records = new java.util.ArrayList<>();
    PreparedStatement pstmt = null;
    ResultSet rs = null;

    // Use the member variable 'this.conn'
    try { 
        // 1. Check if connection is valid before proceeding
        if (this.conn == null || this.conn.isClosed()) {
             System.out.println("Error: Connection is not active for fetchRecords.");
             return records;
        }

        pstmt = this.conn.prepareStatement(sqlQuery); // Use the member connection

        for (int i = 0; i < values.length; i++) {
            pstmt.setObject(i + 1, values[i]);
        }

        rs = pstmt.executeQuery();
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();

        while (rs.next()) {
            java.util.Map<String, Object> row = new java.util.HashMap<>();
            for (int i = 1; i <= columnCount; i++) {
                row.put(metaData.getColumnName(i), rs.getObject(i));
            }
            records.add(row);
        }

    } catch (SQLException e) {
        System.out.println("Error fetching records: " + e.getMessage());
    } finally {
        // Important: Only close the PreparedStatement and ResultSet, NOT the Connection
        try {
            if (rs != null) rs.close();
        } catch (SQLException e) { /* Ignore */ }
        try {
            if (pstmt != null) pstmt.close();
        } catch (SQLException e) { /* Ignore */ }
    }

    return records;
}

public int addRecordAndReturnId(String query, Object... params) {
    int generatedId = -1;
    connectDB(); 

    if (this.conn == null) {
        System.out.println("Error: Database connection is not available.");
        return generatedId;
    }
    
    try (PreparedStatement pstmt = this.conn.prepareStatement(query, PreparedStatement.RETURN_GENERATED_KEYS)) { 
        
        for (int i = 0; i < params.length; i++) {
            pstmt.setObject(i + 1, params[i]);
        }

        int affectedRows = pstmt.executeUpdate();
        if (affectedRows > 0) {
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    generatedId = rs.getInt(1);
                }
            }
        }
    } catch (SQLException e) {
        // You should check for "database is closed" here, which often happens
        // if the client code is closing the connection too early.
        System.out.println("Error inserting record: " + e.getMessage());
    }
    return generatedId;
}


public static String hashPassword(String password) {
    try {
        java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
        byte[] hashedBytes = md.digest(password.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        // Convert byte array to hex string
        StringBuilder hexString = new StringBuilder();
        for (byte b : hashedBytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    } catch (java.security.NoSuchAlgorithmException e) {
        System.out.println("Error hashing password: " + e.getMessage());
        return null;
    }
}
/* SAMPLE HASH PASS IN LOG IN
System.out.print("Enter Email: ");
String email = sc.next();
System.out.print("Enter Password: ");
String pass = sc.next();


String hashedPass = dbConnect.hashPassword(pass);

String sql = "SELECT * FROM tbl_user WHERE u_email = ? AND u_pass = ?";
var result = con.fetchRecords(sql, email, hashedPass);

if (!result.isEmpty()) {
    var user = result.get(0);
    System.out.println("Login successful!");
    System.out.println("User Type: " + user.get("u_type"));
} else {
    System.out.println("Invalid email or password.");
}
*/

/*SAMPLE HASH PASS IN REGISTER
System.out.print("Enter Email: ");
String email = sc.next();
System.out.print("Enter Password: ");
String pass = sc.next();
System.out.print("Enter User Type: ");
String type = sc.next();

// Hash the password before saving
String hashedPass = dbConnect.hashPassword(pass);

String sql = "INSERT INTO tbl_user(u_email, u_pass, u_type, u_status) VALUES(?, ?, ?, ?)";
con.addRecord(sql, email, hashedPass, type, "Active");

System.out.println("User registered successfully!");

*/
public void closeDB() {
    try {
        if (this.conn != null && !this.conn.isClosed()) {
            this.conn.close();
            System.out.println("Database connection closed.");
        }
    } catch (SQLException e) {
        System.err.println("Error closing database connection: " + e.getMessage());
    }
}
}