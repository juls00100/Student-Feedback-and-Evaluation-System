/* config2.java */

package config2;

import java.sql.*; 
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List; 
import java.util.Map;

public class config2 {
    
    public static Connection connectDB() {
        Connection con = null;
        try {
            Class.forName("org.sqlite.JDBC"); // Load the SQLite JDBC driver
            con = DriverManager.getConnection("jdbc:sqlite:Evaluation System.db"); // Establish connection
            //System.out.println("Connection Successful");
        } catch (Exception e) {
            System.out.println("Connection Failed: " + e);
        }
        return con;
    }
    
    // Helper method to set values in PreparedStatement
    private void setPreparedStatementValues(PreparedStatement pstmt, Object... values) throws SQLException {
        for (int i = 0; i < values.length; i++) {
            // Using setObject is generally safer and handles most types
            pstmt.setObject(i + 1, values[i]); 
        }
    }
    
    public void addRecord(String sql, Object... values) {
        try (Connection conn = this.connectDB(); 
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            this.setPreparedStatementValues(pstmt, values);
            pstmt.executeUpdate();
            System.out.println("Record added successfully!");
        } catch (SQLException e) {
            System.out.println("Error adding record: " + e.getMessage());
        }
    }
    
    // --- FIX 2: Returns rows affected for ID validation ---
    public int updateRecord(String sql, Object... values) {
        int rowsAffected = 0;
        try (Connection conn = this.connectDB();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            this.setPreparedStatementValues(pstmt, values);
            rowsAffected = pstmt.executeUpdate();
            // Removed: System.out.println("Record updated successfully!");
        } catch (SQLException e) {
            System.out.println("Database Error: " + e.getMessage());
        }
        return rowsAffected; // Return the count
    }
    
    // --- FIX 2: Returns rows affected for ID validation ---
    public int deleteRecord(String sql, Object... values) {
        int rowsAffected = 0;
        try (Connection conn = this.connectDB();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            this.setPreparedStatementValues(pstmt, values);
            rowsAffected = pstmt.executeUpdate();
            // Removed: System.out.println("Record deleted successfully!");
        } catch (SQLException e) {
            System.out.println("Database Error: " + e.getMessage());
        }
        return rowsAffected; // Return the count
    }
    
    // --- FIX 3: Helper for Duplication Check ---
    public int getSingleInt(String sql, Object... values) {
        int result = 0;
        try (Connection conn = this.connectDB();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            this.setPreparedStatementValues(pstmt, values);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    result = rs.getInt(1); // Get the COUNT(*) value
                }
            }
        } catch (SQLException e) {
            System.out.println("Database Error in getSingleInt: " + e.getMessage());
        }
        return result;
    }

    // --- Primary viewRecords method (COMPLETE IMPLEMENTATION) ---
    public void viewRecords(String sql, String[] headers, String[] columns, Object... values) {
        try (Connection conn = this.connectDB();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            this.setPreparedStatementValues(pstmt, values); 

            try (ResultSet rs = pstmt.executeQuery()) {
                
                if (!rs.isBeforeFirst()) { 
                    System.out.println("❌ No records found for this query.");
                    return;
                }

                // Print Table Headers
                System.out.println("\n--------------------------------------------------------------------------------------------------------------------------------------");
                // Calculate max length to avoid table overflow
                int colWidth = 20; 
                for (String header : headers) {
                    System.out.printf("| %-" + colWidth + "s ", header);
                }
                System.out.println("|\n--------------------------------------------------------------------------------------------------------------------------------------");

                // Iterate through results and print data
                while (rs.next()) {
                    for (String column : columns) {
                        Object value = rs.getObject(column); 
                        String displayValue = (value == null) ? "" : value.toString();
                        System.out.printf("| %-" + colWidth + "s ", displayValue);
                    }
                    System.out.println("|"); // End of row
                }
                System.out.println("--------------------------------------------------------------------------------------------------------------------------------------");

            } 
        } catch (SQLException e) {
            System.out.println("❌ Database Error while viewing records: " + e.getMessage());
        }
    }
    
    // --- FIX 1: Overload for the single-line display function ---
    public void viewRecords(String sql, String[] headers, String[] columns) {
        this.viewRecords(sql, headers, columns, new Object[]{});
    }

    // --- Previous stubs implemented for delegation ---
    public void viewRecords(String sql, String[] headers, String[] columns, String stringValue) {
        this.viewRecords(sql, headers, columns, (Object) stringValue); 
    }
    public void viewRecords(String sql, String[] headers, String[] columns, int id) {
        this.viewRecords(sql, headers, columns, (Object) id); 
    }

    // (The rest of the class, like fetchRecords, hashPassword, and executeDDL, remains the same)
    // ...

  //-----------------------------------------------
    // GET SINGLE VALUE METHOD
    //-----------------------------------------------

    public double getSingleValue(String sql, Object... params) {
        double result = 0.0;
        try (Connection conn = connectDB();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            setPreparedStatementValues(pstmt, params);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                result = rs.getDouble(1);
            }

        } catch (SQLException e) {
            System.out.println("Error retrieving single value: " + e.getMessage());
        }
        return result;
    }
    
    public int addRecordAndReturnId(String query, Object... params) {
        int generatedId = -1;
        try (Connection conn = connectDB();
             PreparedStatement pstmt = conn.prepareStatement(query, PreparedStatement.RETURN_GENERATED_KEYS)) {

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
            System.out.println("Error inserting record: " + e.getMessage());
        }
        return generatedId;
    }
    
    public java.util.List<java.util.Map<String, Object>> fetchRecords(String sqlQuery, Object... values) {
    java.util.List<java.util.Map<String, Object>> records = new java.util.ArrayList<>();

    try (Connection conn = this.connectDB();
         PreparedStatement pstmt = conn.prepareStatement(sqlQuery)) {

        for (int i = 0; i < values.length; i++) {
            pstmt.setObject(i + 1, values[i]);
        }

        ResultSet rs = pstmt.executeQuery();
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
    }

    return records;
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

    public void executeDDL(String sql) {
    try (Connection conn = this.connectDB();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {

        pstmt.executeUpdate();
        // **NO PRINT STATEMENT HERE**
    } catch (SQLException e) {
        System.out.println("Error executing DDL: " + e.getMessage());
    }
}
    
    
    
    
}
