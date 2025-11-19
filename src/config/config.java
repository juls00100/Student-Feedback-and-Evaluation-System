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
            System.out.println("LET'S CREATE INFINITE LOOP OF ROMANCE <3");
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

        // --- ENFORCING FIXED-WIDTH FORMATTING AS REQUESTED ---
        final int FIXED_COLUMN_WIDTH = 20;
        final String FIXED_FORMAT = "%-" + FIXED_COLUMN_WIDTH + "s";
        
        // Construct the fixed separator line (80 chars total) from the teacher's code
        // This assumes 4 columns of 20 chars each, plus pipe/space separators, which roughly matches 80 characters.
        String fixedSeparator = "-------------------------------------------------------------------------------------------------------------------"; 

        // 1. Print the top separator
        System.out.println(fixedSeparator);

        // 2. Print Headers
        StringBuilder headerLine = new StringBuilder("| ");
        for (String header : headers) {
            // Truncate header if it exceeds fixed width
            String displayHeader = header.length() > FIXED_COLUMN_WIDTH ? 
                                   header.substring(0, FIXED_COLUMN_WIDTH) : header;
            // Apply the requested fixed formatting: %-20s
            headerLine.append(String.format(FIXED_FORMAT, displayHeader)).append(" | ");
        }
        
        // The loop above adds a trailing "| " which we need to clean up
        if (headerLine.length() > 2) {
            headerLine.setLength(headerLine.length() - 2); 
        }
        headerLine.append("|");
        System.out.println(headerLine);
        
        // 3. Print the mid separator
        System.out.println(fixedSeparator);

        // 4. Print Records
        for (Map<String, Object> record : records) {
            StringBuilder recordLine = new StringBuilder("| ");
            for (String colName : columns) {
                Object value = record.get(colName);
                String sValue = (value == null) ? "NULL" : value.toString();
                
                // Truncate value if it's too long for the fixed width
                if (sValue.length() > FIXED_COLUMN_WIDTH) {
                    sValue = sValue.substring(0, FIXED_COLUMN_WIDTH);
                }
                
                // Apply the requested fixed formatting: %-20s
                recordLine.append(String.format(FIXED_FORMAT, sValue)).append(" | ");
            }
            
            // Clean up the trailing "| "
            if (recordLine.length() > 2) {
                recordLine.setLength(recordLine.length() - 2); 
            }
            recordLine.append("|");
            System.out.println(recordLine);
        }

        // 5. Print Footer
        System.out.println(fixedSeparator);
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