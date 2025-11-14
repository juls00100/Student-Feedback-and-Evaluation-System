package Main2;

import config.config;
import java.util.Scanner;
import java.util.List;
import java.util.Map;
import java.sql.Connection; 
import java.sql.SQLException;

public class admindashboard {

    private final config db;
    private final Scanner sc;
    private final int userId;
    private final String userType; 

    public admindashboard(config db, Scanner sc, int userId) {
        this.db = db;
        this.sc = sc;
        this.userId = userId;
        
        String sql = "SELECT u_type FROM tbl_user WHERE u_id = ?";
        List<Map<String, Object>> result = db.fetchRecords(sql, userId);
        
        if (!result.isEmpty()) {
            this.userType = (String) result.get(0).get("u_type");
        } else {
            this.userType = "Unknown"; 
            System.out.println("  Error: Could not determine current user type. System access restricted.");
        }
    }

    public void runAdminDashboard() {
        int choice;
        int maxChoice = 8; 
        
        do {
            System.out.println("");
            System.out.println("\n     --- ADMIN DASHBOARD ---");
            System.out.println(" ---------------------------------");
            System.out.println("|  1. ADD A NEW USER              |"); 
            System.out.println("|  2. VIEW ALL THE USERS          |"); 
            System.out.println("|  3. EDIT USER's INFO            |"); 
            System.out.println("|  4. APPROVE A PENDING ACCOUNT   |"); 
            System.out.println("|  5. MANAGE ACCOUNT STATUS       |"); 
            System.out.println("|  6. VIEW EVALUATIONS LIST       |"); 
            System.out.println("|  7. ARCHIVE STUDENT's EVALUATION|"); 
            System.out.println("|  8. EXIT ADMIN DASHBOARD        |"); 
            System.out.println(" ---------------------------------");
            System.out.print("  Enter your choice (1-" + maxChoice + "): ");
            choice = Main2.getIntInput(sc, 1, maxChoice); 

            switch (choice) {
                case 1: addUserSubMenu(); break;
                case 2: viewUsersSubMenu(); break;
                case 3: editUserSubMenu(); break;
                case 4: approveUserSubMenu(); break;
                case 5: handleAccountStatus(); break;
                case 6: viewSystemEvaluations(); break; 
                case 7: archiveEvaluation(); break; 
                case 8: System.out.println("  LOGGING OUT..."); break;
            }
        } while (choice != maxChoice);
    }

    private Map<String, Object> getUserDetails(int id) {
        String sql = "SELECT u_id, u_email, u_type, u_status FROM tbl_user WHERE u_id = ?";
        List<Map<String, Object>> result = db.fetchRecords(sql, id);
        if (result.isEmpty()) return null;
        return result.get(0);
    }

    private void viewRecordsByType(String type) {
        String sql;
        String[] headers;
        String[] columns;

        if (type.equalsIgnoreCase("Student")) {
            
        System.out.println("\n                             --- ALL " + type.toUpperCase() + "S ---");
            sql = "SELECT t1.u_id, t2.s_name, t2.s_schoolID, t1.u_email, t1.u_status FROM tbl_user t1 " +
                  "INNER JOIN tbl_student t2 ON t1.u_id = t2.s_u_id " +
                  "WHERE t1.u_type = 'Student'";
            headers = new String[]{"User ID", "Name", "School ID", "Email", "Status"};
            columns = new String[]{"u_id", "s_name", "s_schoolID", "u_email", "u_status"};
        } else if (type.equalsIgnoreCase("Instructor")) {
            
        System.out.println("\n                     --- ALL " + type.toUpperCase() + "S ---");
            sql = "SELECT t1.u_id, t2.i_name, t1.u_email, t1.u_status FROM tbl_user t1 " +
                  "INNER JOIN tbl_instructor t2 ON t1.u_id = t2.i_u_id " +
                  "WHERE t1.u_type = 'Instructor'";
            headers = new String[]{"User ID", "Name", "Email", "Status"};
            columns = new String[]{"u_id", "i_name", "u_email", "u_status"};
        } else if (type.equalsIgnoreCase("Admin")) {
            if (this.userType.equalsIgnoreCase("SuperAdmin")) {
                 sql = "SELECT u_id, u_email, u_type, u_status FROM tbl_user WHERE u_type IN ('Admin', 'SuperAdmin')";
            } else {
                 System.out.println("\n               --- ALL " + type.toUpperCase() + "S ---");
                 sql = "SELECT u_id, u_email, u_type, u_status FROM tbl_user WHERE u_type = 'Admin'";
            }
            headers = new String[]{"User ID", "Email", "Type", "Status"};
            columns = new String[]{"u_id", "u_email", "u_type", "u_status"};
        } else {
            System.out.println("  Invalid user type specified for view.");
            return;
        }

        List<Map<String, Object>> records = db.fetchRecords(sql);
        if (records.isEmpty()) {
            System.out.println("  No " + type.toLowerCase() + " records found.");
        } else {
            db.viewRecords(records, headers, columns);
        }
    }
    
    private void addUserSubMenu() {
        int choice;
        int maxChoice = 4;
        
        do {
            System.out.println("\n  --- ADD NEW USER SUB-MENU ---");
            System.out.println(" -------------------------------");
            System.out.println("|  1. Add Student               |");
            System.out.println("|  2. Add Instructor            |");
            System.out.println("|  3. Add Admin                 |");
            System.out.println("|  4. Back to Admin Dashboard   |");
            System.out.println(" -------------------------------");
            System.out.print("  Enter your choice (1-" + maxChoice + "): ");
            choice = Main2.getIntInput(sc, 1, maxChoice); 

            switch (choice) {
                case 1: addStudent(); break;
                case 2: addInstructor(); break;
                case 3: addAdmin(); break;
                case 4: System.out.println("  Returning to Admin Dashboard..."); break;
            }
        } while (choice != maxChoice);
    }

    private int promptForCredentialsAndInsertUser(String type) {
        System.out.println("\n   --- ADD NEW " + type.toUpperCase() + " ---\n");
        sc.nextLine(); 

        String email;
        while (true) {
            System.out.print("  Enter " + type.toLowerCase() + " email: ");
            email = sc.nextLine().trim();
            String checkEmailSql = "SELECT u_email FROM tbl_user WHERE u_email = ?";
            if (db.fetchRecords(checkEmailSql, email).isEmpty()) {
                break;
            }
            System.out.println("  Error: Email already exists. Please enter a different email.");
        }

        System.out.print("  Enter password: ");
        String pass = sc.nextLine().trim();
        String hashedPass = db.hashPassword(pass);

        String userSql = "INSERT INTO tbl_user(u_email, u_pass, u_type, u_status) VALUES(?, ?, ?, ?)";
        db.addRecord(userSql, email, hashedPass, type, "Pending");
        
        String getIdSql = "SELECT u_id FROM tbl_user WHERE u_email = ?";
        List<Map<String, Object>> result = db.fetchRecords(getIdSql, email);
        
        return result.isEmpty() ? -1 : (int) result.get(0).get("u_id");
    }

    private void addStudent() {
        int newUserId = promptForCredentialsAndInsertUser("Student");
        if (newUserId == -1) return;

        System.out.print("  Enter Student ID (e.g., scc-00-01): ");
        String schoolId = sc.nextLine().trim();
        
        System.out.print("  Enter Student Name: ");
        String name = sc.nextLine().trim();

        String studentSql = "INSERT INTO tbl_student(s_u_id, s_schoolID, s_name) VALUES(?, ?, ?)";
        db.addRecord(studentSql, newUserId, schoolId, name);
        
        System.out.println("\n  SUCCESS: New Student added with User ID " + newUserId + ". Status is 'Pending' and requires approval.");
    }

    private void addInstructor() {
        int newUserId = promptForCredentialsAndInsertUser("Instructor");
        if (newUserId == -1) return;

        System.out.print("  Enter Instructor Name: ");
        String name = sc.nextLine().trim();
        
        String instructorSql = "INSERT INTO tbl_instructor(i_u_id, i_name) VALUES(?, ?)";
        db.addRecord(instructorSql, newUserId, name);
        
        System.out.println("\n  SUCCESS: New Instructor added with User ID " + newUserId + ". Status is 'Pending' and requires approval.");
    }

    private void addAdmin() {
        int newUserId = promptForCredentialsAndInsertUser("Admin");
        if (newUserId == -1) return;
        
        System.out.println("\n  SUCCESS: New Admin added with User ID " + newUserId + ". Status is 'Pending'. NOTE: Admin accounts must be approved by the SuperAdmin.");
    }

    private void viewUsersSubMenu() {
        int choice;
        int maxChoice = 4;
        do {
            System.out.println("\n --- VIEW USERS SUB-MENU ---");
            System.out.println(" -----------------------------");
            System.out.println("|  1. View Student List       |");
            System.out.println("|  2. View Instructor List    |");
            System.out.println("|  3. View Admin List         |");
            System.out.println("|  4. Back to Admin Dashboard |");
            System.out.println(" -----------------------------");
            System.out.print("  Enter your choice (1-" + maxChoice + "): ");
            choice = Main2.getIntInput(sc, 1, maxChoice); 

            switch (choice) {
                case 1: viewRecordsByType("Student"); break;
                case 2: viewRecordsByType("Instructor"); break;
                case 3: viewRecordsByType("Admin"); break;
                case 4: System.out.println("  Returning to Admin Dashboard..."); break;
            }
        } while (choice != maxChoice);
    }
    
    private void editUserSubMenu() {
        int choice;
        int maxChoice = 4;
        do {
            System.out.println("\n  --- EDIT USER SUB-MENU ---");
            System.out.println(" -------------------------------");
            System.out.println("|  1. Edit Student Info         |");
            System.out.println("|  2. Edit Instructor Info      |");
            System.out.println("|  3. Edit Admin Info           |");
            System.out.println("|  4. Back to Admin Dashboard   |");
            System.out.println(" -------------------------------");

            System.out.print("  Enter your choice (1-" + maxChoice + "): ");
            choice = Main2.getIntInput(sc, 1, maxChoice); 

            switch (choice) {
                case 1: editUserInfo("Student"); break;
                case 2: editUserInfo("Instructor"); break;
                case 3: editUserInfo("Admin"); break;
                case 4: System.out.println("  Returning to Admin Dashboard..."); break;
            }
        } while (choice != maxChoice);
    }

    private void editUserInfo(String type) {
        viewRecordsByType(type); 

        System.out.print("\n  Enter the User ID of the " + type + " to EDIT: ");
        int id = Main2.getIntInput(sc, 1, Integer.MAX_VALUE); 
        sc.nextLine(); 

        
        Map<String, Object> user = getUserDetails(id);
        if (user == null || !((String)user.get("u_type")).equalsIgnoreCase(type)) {
            System.out.println("  Error: User ID " + id + " not found or is not a " + type + ".");
            return;
        }
        
        String targetType = (String) user.get("u_type");
        if (targetType.equalsIgnoreCase("SuperAdmin") && !this.userType.equalsIgnoreCase("SuperAdmin")) {
            System.out.println("  Error: Only the SuperAdmin can modify the SuperAdmin account.");
            return;
        }
        
        System.out.print("  Enter NEW Email (Current: " + user.get("u_email") + ", leave blank to skip): ");
        String newEmail = sc.nextLine().trim();
        if (!newEmail.isEmpty()) {
            String checkEmailSql = "SELECT u_email FROM tbl_user WHERE u_email = ? AND u_id != ?";
            if (!db.fetchRecords(checkEmailSql, newEmail, id).isEmpty()) {
                System.out.println("  Error: New email already exists in the system. Edit cancelled.");
                return;
            }
            db.updateRecord("UPDATE tbl_user SET u_email = ? WHERE u_id = ?", newEmail, id);
            System.out.println("  Email updated.");
        }
        
        System.out.print("  Enter NEW Password (leave blank to keep current): ");
        String newPass = sc.nextLine().trim();
        if (!newPass.isEmpty()) {
            String hashedPass = db.hashPassword(newPass);
            db.updateRecord("UPDATE tbl_user SET u_pass = ? WHERE u_id = ?", hashedPass, id);
            System.out.println("  Password updated.");
        }

        if (type.equalsIgnoreCase("Student")) {
            String studentSql = "SELECT s_schoolID, s_name FROM tbl_student WHERE s_u_id = ?";
            List<Map<String, Object>> studentResult = db.fetchRecords(studentSql, id);
            if (!studentResult.isEmpty()) {
                Map<String, Object> studentInfo = studentResult.get(0);
                
                System.out.print("  Enter NEW School ID (Current: " + studentInfo.get("s_schoolID") + ", leave blank to skip): ");
                String newSchoolId = sc.nextLine().trim();
                if (!newSchoolId.isEmpty()) {
                    db.updateRecord("UPDATE tbl_student SET s_schoolID = ? WHERE s_u_id = ?", newSchoolId, id);
                    System.out.println("  Student ID updated.");
                }
                
                System.out.print("  Enter NEW Student Name (Current: " + studentInfo.get("s_name") + ", leave blank to skip): ");
                String newName = sc.nextLine().trim();
                if (!newName.isEmpty()) {
                    db.updateRecord("UPDATE tbl_student SET s_name = ? WHERE s_u_id = ?", newName, id);
                    System.out.println("  Student Name updated.");
                }
            }
        } else if (type.equalsIgnoreCase("Instructor")) {
            String instructorSql = "SELECT i_name FROM tbl_instructor WHERE i_u_id = ?";
            List<Map<String, Object>> instructorResult = db.fetchRecords(instructorSql, id);
            if (!instructorResult.isEmpty()) {
                Map<String, Object> instructorInfo = instructorResult.get(0);
                
                System.out.print("  Enter NEW Instructor Name (Current: " + instructorInfo.get("i_name") + ", leave blank to skip): ");
                String newName = sc.nextLine().trim();
                if (!newName.isEmpty()) {
                    db.updateRecord("UPDATE tbl_instructor SET i_name = ? WHERE i_u_id = ?", newName, id);
                    System.out.println("  Instructor Name updated.");
                }
            }
        } 
        

        System.out.println("\n  SUCCESS: " + type + " information for User ID " + id + " updated.");
    }
    
    private void approveUserSubMenu() {
        int choice;
        int maxChoice = 4;
        do {
            System.out.println("\n--- APPROVE USER SUB-MENU ---");
            System.out.println(" -------------------------------");
            System.out.println("|  1. Approve Student Account   |");
            System.out.println("|  2. Approve Instructor Account|");
            System.out.println("|  3. Approve Admin Account     |");
            System.out.println("|  4. Back to Admin Dashboard   |");
            System.out.println(" -------------------------------");
            System.out.print("  Enter your choice (1-" + maxChoice + "): ");
            choice = Main2.getIntInput(sc, 1, maxChoice); 

            switch (choice) {
                case 1: approveUserByType("Student"); break;
                case 2: approveUserByType("Instructor"); break;
                case 3: approveUserByType("Admin"); break;
                case 4: System.out.println("  Returning to Admin Dashboard..."); break;
            }
        } while (choice != maxChoice);
    }

    private void approveUserByType(String type) {
        System.out.println("\n   --- PENDING " + type.toUpperCase() + " ACCOUNTS ---\n");
        String sql;
        String[] headers;
        String[] columns;
        
        if (type.equalsIgnoreCase("Student")) {
            sql = "SELECT t1.u_id, t2.s_name, t2.s_schoolID, t1.u_email, t1.u_status FROM tbl_user t1 " +
                  "INNER JOIN tbl_student t2 ON t1.u_id = t2.s_u_id " +
                  "WHERE t1.u_type = 'Student' AND t1.u_status = 'Pending'";
            headers = new String[]{"User ID", "Name", "School ID", "Email", "Status"};
            columns = new String[]{"u_id", "s_name", "s_schoolID", "u_email", "u_status"};
        } else if (type.equalsIgnoreCase("Instructor")) {
            sql = "SELECT t1.u_id, t2.i_name, t1.u_email, t1.u_status FROM tbl_user t1 " +
                  "INNER JOIN tbl_instructor t2 ON t1.u_id = t2.i_u_id " +
                  "WHERE t1.u_type = 'Instructor' AND t1.u_status = 'Pending'";
            headers = new String[]{"User ID", "Name", "Email", "Status"};
            columns = new String[]{"u_id", "i_name", "u_email", "u_status"};
        } else if (type.equalsIgnoreCase("Admin")) {
            sql = "SELECT u_id, u_email, u_type, u_status FROM tbl_user WHERE u_type = 'Admin' AND u_status = 'Pending'";
            headers = new String[]{"User ID", "Email", "Type", "Status"};
            columns = new String[]{"u_id", "u_email", "u_type", "u_status"};
        } else {
            return; 
        }
        
        List<Map<String, Object>> records = db.fetchRecords(sql);
        if (records.isEmpty()) {
            System.out.println("  No " + type.toLowerCase() + " accounts are currently pending approval.");
            return;
        }
        db.viewRecords(records, headers, columns);

        System.out.print("\n  Enter the User ID of the " + type + " to APPROVE: ");
        int id = Main2.getIntInput(sc, 1, Integer.MAX_VALUE); 
        sc.nextLine(); 

        Map<String, Object> user = getUserDetails(id);
        if (user == null || !((String)user.get("u_type")).equalsIgnoreCase(type) || !((String)user.get("u_status")).equalsIgnoreCase("Pending")) {
            System.out.println("  Error: User ID " + id + " not found or is not a pending " + type + ".");
            return;
        }
        
        String updateSql = "UPDATE tbl_user SET u_status = 'Approved' WHERE u_id = ?";
        db.updateRecord(updateSql, id);
        System.out.println("\n  SUCCESS: User ID " + id + " (" + type + ") has been APPROVED (Status set to 'Approved').");
    }

    private void handleAccountStatus() {
        int choice;
        int maxChoice = 4;
        String targetType = null;
        
        do {
            System.out.println("\n--- MANAGE ACCOUNT STATUS SUB-MENU ---");
            System.out.println(" ------------------------------------------");
            System.out.println("|  1. Manage Student Account Status        |");
            System.out.println("|  2. Manage Instructor Account Status     |");
            System.out.println("|  3. Manage Admin Account Status          |");
            System.out.println("|  4. Back to Admin Dashboard              |");
            System.out.println(" ------------------------------------------");
            System.out.print("  Enter your choice (1-" + maxChoice + "): ");
            choice = Main2.getIntInput(sc, 1, maxChoice); 

            switch (choice) {
                case 1: targetType = "Student"; break;
                case 2: targetType = "Instructor"; break;
                case 3: targetType = "Admin"; break;
                case 4: System.out.println("  Returning to Admin Dashboard..."); return;
            }
            
            viewRecordsByType(targetType); 

            System.out.print("\n  Enter the User ID of the " + targetType + " to MANAGE: ");
            int id = Main2.getIntInput(sc, 1, Integer.MAX_VALUE); 
            sc.nextLine(); 

            Map<String, Object> user = getUserDetails(id);
            if (user == null || !((String)user.get("u_type")).equalsIgnoreCase(targetType)) {
                System.out.println("  Error: User ID " + id + " not found or is not a " + targetType + ".");
                continue;
            }
            
            String user_type = (String) user.get("u_type");
            if (user_type.equalsIgnoreCase("SuperAdmin") && !this.userType.equalsIgnoreCase("SuperAdmin")) {
                System.out.println("  Error: Only the SuperAdmin can manage the SuperAdmin account status.");
                continue;
            }
            if (id == this.userId && !user_type.equalsIgnoreCase("SuperAdmin")) {
                System.out.println("  Error: Cannot manage your own account status outside of SuperAdmin access. Log out to test changes.");
                continue;
            }
            if (id == this.userId && !user_type.equalsIgnoreCase("SuperAdmin")) {
                System.out.println("  Error: Cannot manage your own account status outside of SuperAdmin access. Log out to test changes.");
                continue;
            }
            String currentStatus = (String) user.get("u_status");
            String newStatus;
            
            if (currentStatus.equalsIgnoreCase("Approved") || currentStatus.equalsIgnoreCase("Pending")) {
                System.out.print("  Account is currently '" + currentStatus + "'. Do you want to DISABLE (set to Inactive) it? (y/n): ");
                String confirm = sc.nextLine().trim();
                if (confirm.equalsIgnoreCase("y")) {
                    newStatus = "Inactive";
                } else {
                    System.out.println("  Operation cancelled.");
                    continue;
                }
            } 
            else if (currentStatus.equalsIgnoreCase("Inactive")) {
                System.out.print("   Account is currently 'Inactive'. Do you want to RE-ACTIVATE (set to Approved) it? (y/n): ");
                String confirm = sc.nextLine().trim();
                if (confirm.equalsIgnoreCase("y")) {
                    newStatus = "Approved"; 
                } else {
                    System.out.println("  Operation cancelled.");
                    continue;
                }
            } else {
                System.out.println("  Unknown account status: " + currentStatus);
                continue;
            }

            String sql = "UPDATE tbl_user SET u_status = ? WHERE u_id = ?";
            db.updateRecord(sql, newStatus, id);
            System.out.println("\n  SUCCESS: User ID " + id + " (" + targetType + ") status updated to '" + newStatus + "'.");

        } while (choice != maxChoice);
    }
    
    private void viewSystemEvaluations() {
        System.out.println("\n   --- ALL EVALUATIONS (SYSTEM-WIDE) ---\n");
        String sql = "SELECT t1.e_id, t2.s_name AS student_name, t3.i_name AS instructor_name, " +
                     "t1.e_average_rating, t1.e_year, t1.e_sem, t1.e_date " +
                     "FROM tbl_evaluation t1 " +
                     "INNER JOIN tbl_student t2 ON t1.s_schoolID = t2.s_schoolID " +
                     "INNER JOIN tbl_instructor t3 ON t1.i_id = t3.i_id " +
                     "ORDER BY t1.e_id DESC";
                     
        List<Map<String, Object>> records = db.fetchRecords(sql);

        if (records.isEmpty()) {
            System.out.println("  No current evaluations found in the system.");
            return;
        }
        
        for (Map<String, Object> record : records) {
            Object avgObj = record.get("e_average_rating");
            if (avgObj != null) {
                try {
                    double rating = (avgObj instanceof Number) ? ((Number) avgObj).doubleValue() : Double.parseDouble(avgObj.toString());
                    record.put("e_average_rating", String.format("%.2f", rating));
                } catch (Exception e) {
                    record.put("e_average_rating", "N/A");
                }
            }
        }
        
        String[] headers = {"Eval ID", "Student", "Instructor", "Avg Rating", "Year", "Sem", "Date"};
        String[] columns = {"e_id", "student_name", "instructor_name", "e_average_rating", "e_year", "e_sem", "e_date"};
        
        db.viewRecords(records, headers, columns); 
        
        String totalAvgSql = "SELECT AVG(CAST(e_average_rating AS REAL)) AS overall_avg FROM tbl_evaluation";
        List<Map<String, Object>> avgResult = db.fetchRecords(totalAvgSql);
        
        if (!avgResult.isEmpty()) {
            Object avgObj = avgResult.get(0).get("overall_avg");
            double overallAvg = 0.0;
            
            if (avgObj != null) {
                try {
                    overallAvg = (avgObj instanceof Number) ? ((Number) avgObj).doubleValue() : Double.parseDouble(avgObj.toString());
                } catch (Exception e) { }
            }
            
            String e = "  *================================================*"; 
            System.out.println("\n" + e);
            System.out.printf("   TOTAL SYSTEM-WIDE AVERAGE RATING: %.2f / 5.0\n", overallAvg);
            System.out.println(e);
        }
    }


    private void archiveEvaluation() {
        System.out.println("\n   --- ARCHIVE SPECIFIC EVALUATION ---\n");

        viewEvaluations(); 

        System.out.print("\n  Enter the **Evaluation ID (e_id)** you want to ARCHIVE: ");

        int evalIdToArchive = Main2.getIntInput(sc, 1, 99999); 
        sc.nextLine();

        String findEvalSql = "SELECT s_schoolID FROM tbl_evaluation WHERE e_id = ?";

        List<Map<String, Object>> evalRecord = db.fetchRecords(findEvalSql, evalIdToArchive);

        if (evalRecord.isEmpty()) {
            System.out.println("  No active evaluation found with ID: " + evalIdToArchive + ". Nothing to archive.");
            return;
        }

        String schoolID = (String) evalRecord.get(0).get("s_schoolID");

        System.out.print("  WARNING: This will permanently move Evaluation ID " + evalIdToArchive + " (for student " + schoolID + ") to the archive. Continue? (y/n): ");
        String confirm = sc.nextLine().trim();
        if (!confirm.equalsIgnoreCase("y")) {
            System.out.println("  Archiving operation cancelled.");
            return;
        }

        Connection conn = null;
        try {
            conn = db.getConnection(); 
            if (conn == null) throw new SQLException("  Database connection failed.");

            conn.setAutoCommit(false);
            String evalInsertSql = "INSERT INTO tbl_archive_evaluation SELECT * FROM tbl_evaluation WHERE e_id = ?";
            db.addRecord(evalInsertSql, evalIdToArchive);

            String scoreInsertSql = "INSERT INTO tbl_archive_eval_scores SELECT * FROM tbl_eval_scores WHERE e_id = ?";
            db.addRecord(scoreInsertSql, evalIdToArchive); 

            String clearEvalSql = "DELETE FROM tbl_evaluation WHERE e_id = ?";
            db.deleteRecord(clearEvalSql, evalIdToArchive); 

            String clearScoreSql = "DELETE FROM tbl_eval_scores WHERE e_id = ?";
            db.deleteRecord(clearScoreSql, evalIdToArchive);

            conn.commit(); 

            System.out.println("\n  SUCCESS: Evaluation ID " + evalIdToArchive + " has been moved to the archive.");
            System.out.println("  NOTE: This evaluation is now removed from the current tables.");

        } catch (SQLException e) {
            System.out.println("  Archiving failed due to a database error. Rolling back changes: " + e.getMessage());
            try {
                if (conn != null) {
                   conn.rollback();
                }
            } catch (SQLException ex) {
                System.out.println("  Rollback failed.");
            }
        } finally {
            try {
                if (conn != null) {
                    conn.setAutoCommit(true);
                }
            } catch (SQLException e) {
                System.out.println("  Error resetting AutoCommit: " + e.getMessage());
            }
        }
    }
private void viewEvaluations() {
    System.out.println("\n   --- ALL EVALUATIONS IN THE SYSTEM ---\n");

    String sql = "SELECT t1.e_id, t2.s_name AS student_name, t3.i_name AS instructor_name, " +
                 "t1.e_average_rating, t1.e_year, t1.e_sem, t1.e_date " +
                 "FROM tbl_evaluation t1 " +
                 "INNER JOIN tbl_student t2 ON t1.s_schoolID = t2.s_schoolID " +
                 "INNER JOIN tbl_instructor t3 ON t1.i_id = t3.i_id " +
                 "ORDER BY t1.e_date DESC";

    List<Map<String, Object>> records = db.fetchRecords(sql); 

    for (Map<String, Object> record : records) {
        Object avg = record.get("e_average_rating");
        if (avg instanceof Number) {
            double rating = ((Number) avg).doubleValue();
            record.put("e_average_rating", String.format("%.2f", rating));
        } else {
            record.put("e_average_rating", "N/A");
        }
    }
    
    String[] headers = {"Eval ID", "Student", "Instructor", "Avg Rating", "Year", "Sem", "Date"};
    String[] columns = {"e_id", "student_name", "instructor_name", "e_average_rating", "e_year", "e_sem", "e_date"};
    db.viewRecords(records, headers, columns); 

    String totalAvgSql = "SELECT AVG(CAST(e_average_rating AS REAL)) AS overall_avg FROM tbl_evaluation";
    List<Map<String, Object>> avgResult = db.fetchRecords(totalAvgSql);
    
    if (!avgResult.isEmpty()) {
        Object avgObj = avgResult.get(0).get("overall_avg");
        double overallAvg = 0.0;
        if (avgObj instanceof Number) {
            overallAvg = ((Number) avgObj).doubleValue(); 
        } else if (avgObj != null) {
            try {
                overallAvg = Double.parseDouble(avgObj.toString());
            } catch (NumberFormatException e) { }
        }
        
        String e = "  *================================================*"; 
        System.out.println("\n" + e);
        System.out.printf("   TOTAL SYSTEM-WIDE AVERAGE RATING: %.2f / 5.0\n", overallAvg);
        System.out.println(e);
    }
}
}

    

