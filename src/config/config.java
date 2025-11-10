package config;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException; // Added for Hashing

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
            this.conn = DriverManager.getConnection("jdbc:sqlite:Evaluation_System.db");
            System.out.println("LET'S CREATE INFINITE LOOP OF ROMANCE, LOVE U <3");
            }      
        } catch (Exception e) {
            System.out.println("Connection Failed: " + e.getMessage());
        }
    }
    
    public Connection getConnection() {
        connectDB();
        return this.conn;
    }
    
    /**
     * Adds a new record to the database using a prepared statement.
     * @param sql The SQL INSERT statement with '?' placeholders.
     * @param values The values to be inserted, corresponding to the '?' placeholders.
     */
    public void addRecord(String sql, Object... values) {
    connectDB(); 
    if (this.conn == null) {
        System.out.println("Error: Database connection is not available for adding records.");
        return;
    }

    try (PreparedStatement pstmt = this.conn.prepareStatement(sql)) {
        for (int i = 0; i < values.length; i++) {
            pstmt.setObject(i + 1, values[i]);
        }
        pstmt.executeUpdate();
    } catch (SQLException e) {
        System.out.println("Error adding record: " + e.getMessage());
    }
}
    
    // =========================================================================
    // ADDED FUNCTIONS (Read/Fetch)
    // =========================================================================

    /**
     * Fetches multiple records from the database (SELECT).
     * @param sql The SQL SELECT statement with optional '?' placeholders.
     * @param values The values for the '?' placeholders.
     * @return A List of Maps, where each Map represents a row (columnName -> value).
     */
    public List<Map<String, Object>> fetchRecords(String sql, Object... values) {
        connectDB();
        List<Map<String, Object>> results = new ArrayList<>();
        if (this.conn == null) return results;

        try (PreparedStatement pstmt = this.conn.prepareStatement(sql)) {
            for (int i = 0; i < values.length; i++) {
                pstmt.setObject(i + 1, values[i]);
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();

                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = metaData.getColumnLabel(i);
                        row.put(columnName, rs.getObject(i));
                    }
                    results.add(row);
                }
            }
        } catch (SQLException e) {
            System.out.println("Error fetching records: " + e.getMessage());
        }
        return results;
    }

    // =========================================================================
    // ADDED FUNCTIONS (Update)
    // =========================================================================
    
    /**
     * Updates an existing record in the database using a prepared statement (UPDATE).
     * @param sql The SQL UPDATE statement with '?' placeholders.
     * @param values The values for the '?' placeholders.
     * @return true if the record was updated, false otherwise.
     */
    public boolean updateRecord(String sql, Object... values) {
        connectDB(); 
        if (this.conn == null) {
            System.out.println("Error: Database connection is not available for updating records.");
            return false;
        }

        try (PreparedStatement pstmt = this.conn.prepareStatement(sql)) {
            for (int i = 0; i < values.length; i++) {
                pstmt.setObject(i + 1, values[i]);
            }
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.out.println("Error updating record: " + e.getMessage());
            return false;
        }
    }

    // =========================================================================
    // ADDED FUNCTIONS (Delete)
    // =========================================================================

    /**
     * Deletes a record from the database using a prepared statement (DELETE).
     * @param sql The SQL DELETE statement with '?' placeholders.
     * @param values The values for the '?' placeholders (e.g., the ID).
     * @return true if the record was deleted, false otherwise.
     */
    public boolean deleteRecord(String sql, Object... values) {
        connectDB(); 
        if (this.conn == null) {
            System.out.println("Error: Database connection is not available for deleting records.");
            return false;
        }

        try (PreparedStatement pstmt = this.conn.prepareStatement(sql)) {
            for (int i = 0; i < values.length; i++) {
                pstmt.setObject(i + 1, values[i]);
            }
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.out.println("Error deleting record: " + e.getMessage());
            return false;
        }
    }
    
    // =========================================================================
    // ADDED UTILITY FUNCTION (String.repeat() replacement for Java < 11)
    // =========================================================================
    
    /**
     * Repeats a character 'count' times. Used to fix String.repeat() error.
     */
    private String repeatChar(char c, int count) {
        StringBuilder sb = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            sb.append(c);
        }
        return sb.toString();
    }

    // =========================================================================
    // ADDED FUNCTIONS (Presentation/Utility)
    // =========================================================================

    /**
     * Prints database records in a formatted table to the console.
     * @param records The List of Maps returned by fetchRecords.
     * @param headers An array of friendly header names.
     * @param columns An array of corresponding database column names.
     */
    public void viewRecords(List<Map<String, Object>> records, String[] headers, String[] columns) {
        if (records.isEmpty()) {
            System.out.println("No records to display.");
            return;
        }

        // 1. Determine column widths
        int[] widths = new int[columns.length];
        for (int i = 0; i < columns.length; i++) {
            widths[i] = headers[i].length(); // Start with header length
        }

        for (Map<String, Object> record : records) {
            for (int i = 0; i < columns.length; i++) {
                Object value = record.get(columns[i]);
                String sValue = (value == null) ? "NULL" : value.toString();
                if (sValue.length() > widths[i]) {
                    widths[i] = sValue.length();
                }
            }
        }

        // Add padding
        for (int i = 0; i < widths.length; i++) {
            widths[i] += 2;
        }
        
        // 2. Print Separator Line (Fix applied here)
        StringBuilder separator = new StringBuilder();
        for (int width : widths) {
            separator.append("+").append(repeatChar('-', width));
        }
        separator.append("+");
        
        System.out.println(separator);

        // 3. Print Headers
        StringBuilder headerLine = new StringBuilder();
        for (int i = 0; i < headers.length; i++) {
            headerLine.append("| ");
            String format = "%-" + (widths[i] - 1) + "s"; 
            headerLine.append(String.format(format, headers[i]));
        }
        headerLine.append("|");
        System.out.println(headerLine);
        System.out.println(separator);

        // 4. Print Records
        for (Map<String, Object> record : records) {
            StringBuilder recordLine = new StringBuilder();
            for (int i = 0; i < columns.length; i++) {
                recordLine.append("| ");
                Object value = record.get(columns[i]);
                String sValue = (value == null) ? "NULL" : value.toString();
                String format = "%-" + (widths[i] - 1) + "s";
                recordLine.append(String.format(format, sValue));
            }
            recordLine.append("|");
            System.out.println(recordLine);
        }

        // 5. Print Footer
        System.out.println(separator);
    }
    
    // =========================================================================
    // ADDED FUNCTIONS (Hasing with standard Java, fixed BCrypt error)
    // =========================================================================

    /**
     * Hashes a password using SHA-256 (Standard Java implementation).
     * @param password The raw password string.
     * @return The hashed password string.
     */
    public String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(password.getBytes());
            byte[] bytes = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                // Convert byte to hex
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // This should not happen since SHA-256 is a standard algorithm
            System.out.println("Error: Hashing algorithm not found: " + e.getMessage());
            return null; 
        }
    }
    
    /**
     * Verifies a password against a hash using SHA-256 comparison.
     * @param candidate The password submitted by the user.
     * @param hashed The hashed password from the database.
     * @return true if passwords match, false otherwise.
     */
    public boolean checkPassword(String candidate, String hashed) {
        String candidateHash = hashPassword(candidate);
        // Compare the generated hash of the candidate password with the stored hash
        return candidateHash != null && candidateHash.equals(hashed);
    }
}