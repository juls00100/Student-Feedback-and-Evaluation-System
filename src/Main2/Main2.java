package Main2;

import config.config;
import java.util.Scanner;
import java.util.List;
import java.util.Map;

public class Main2 {

private static String superAdminEmailCache = null;

public static String getSuperAdminEmail(config db) {
    if (superAdminEmailCache != null) {
        return superAdminEmailCache;
    }
    String sql = "SELECT u_email FROM tbl_user WHERE u_type = 'SuperAdmin' LIMIT 1";
    List<Map<String, Object>> result = db.fetchRecords(sql); 
    
    if (!result.isEmpty()) {
        superAdminEmailCache = (String) result.get(0).get("u_email");
        return superAdminEmailCache;
    }
    
    return "";
}
  

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        config db = new config();
       
        db.connectDB(); 
        initializeSuperAdmin(db, sc);

        int choice;
        do {
            System.out.println("\n   --- MAIN MENU ---");
            System.out.println(" -----------------------");
            System.out.println("|  1. REGISTER NEW USER |");
            System.out.println("|  2. LOG IN            |");
            System.out.println("|  3. EXIT              |");
            System.out.println(" -----------------------");
            System.out.print("  Enter your choice: ");

            choice = getIntInput(sc, 1, 3); 

            switch (choice) {
                case 1: registerUser(sc, db); break;
                case 2: loginUser(sc, db); break;
                case 3: System.out.println("  SEE YAHH AROUND USERR!"); break;
            }
        } while (choice != 3);
    }

    private static void initializeSuperAdmin(config db, Scanner sc) {
    String sql = "SELECT u_id, u_email FROM tbl_user WHERE u_type = 'SuperAdmin'";
    List<Map<String, Object>> userExistsResult = db.fetchRecords(sql);

    if (userExistsResult.isEmpty()) {
        System.out.println("\n|--- SUPER ADMIN INITIALIZATION ---|");
        System.out.print("  Enter Super Admin Email: "); 
        String email = sc.next();
        System.out.print("  Enter Super Admin Password: ");
        String pass = sc.next();

        String hashedPass = db.hashPassword(pass);
        
        String insertSql = "INSERT INTO tbl_user(u_email, u_pass, u_type, u_status) VALUES(?, ?, ?, ?)";
        db.addRecord(insertSql, email, hashedPass, "SuperAdmin", "Active");

        System.out.println("  Super Admin account initialized with email: " + email);
    } else {
        System.out.println("  Feel free to reach admins and superadmin for guidance!");
    }
}


    private static void registerUser(Scanner sc, config db) {
        System.out.println("\n        --- REGISTER --- ");
        System.out.println("----------------------------------");
        System.out.println("|  Select User Type to Register: |");
        System.out.println("|  1. Student                    |");
        System.out.println("|  2. Instructor                 |");
        System.out.println("|  3. Admin                      |");
        System.out.println("|  4. Exit                       |");
        System.out.println("----------------------------------");
        System.out.print("   Enter your choice: ");

        int typeChoice = getIntInput(sc, 1, 4);
        sc.nextLine(); 

        String type;
        switch (typeChoice) {
            case 1: type = "Student"; break;
            case 2: type = "Instructor"; break;
            case 3: type = "Admin"; break;
            case 4: System.out.println("   Loggin out"); return;
            default: return; 
        }

        System.out.print("Enter Full Name: ");
        String name = sc.nextLine();
        
        String email;
        List<Map<String, Object>> checkResult;
        do {
            System.out.print("Enter Email: ");
            email = sc.nextLine();
            String checkSql = "SELECT u_id FROM tbl_user WHERE u_email = ?";
            checkResult = db.fetchRecords(checkSql, email);
            if (!checkResult.isEmpty()) {
                System.out.println("Error: Email already registered. Please use a different email.");
            }
        } while (!checkResult.isEmpty());
        
        System.out.print("Enter Password: ");
        String pass = sc.nextLine();
        
        String hashedPass = db.hashPassword(pass);
        if (hashedPass == null) {
            System.out.println("Error: Password hashing error.");
            return;
        }

        String userSql = "INSERT INTO tbl_user(u_email, u_pass, u_type, u_status) VALUES(?, ?, ?, ?)";
        db.addRecord(userSql, email, hashedPass, type, "Pending"); 
        
        String getIdSql = "SELECT u_id FROM tbl_user WHERE u_email = ?";
        List<Map<String, Object>> result = db.fetchRecords(getIdSql, email);
        if (result.isEmpty()) {
            System.out.println("ERROR. User record not found.");
            return;
        }
        int newUserId = (int) result.get(0).get("u_id");

        switch (type) {
            case "Student":
                System.out.print("Enter School ID: ");
                String schoolId = sc.nextLine();
                String studentSql = "INSERT INTO tbl_student(s_u_id, s_name, s_schoolID) VALUES(?, ?, ?)";
                db.addRecord(studentSql, newUserId, name, schoolId);
                System.out.println("Student account registered successfully! Status: Pending.");
                break;
            case "Instructor":
                String instructorSql = "INSERT INTO tbl_instructor(i_u_id, i_name) VALUES(?, ?)";
                db.addRecord(instructorSql, newUserId, name);
                System.out.println("Instructor account registered successfully! Status: Pending Approval.");
                break;
            case "Admin":
                String adminSql = "INSERT INTO tbl_admin(a_u_id, a_name) VALUES(?, ?)";
                db.addRecord(adminSql, newUserId, name);
                System.out.println("Admin account registered successfully! Status: Pending Approval.");
                break;
        }
    }

    private static void loginUser(Scanner sc, config db) {
        
        while (true) {
            sc.nextLine();

            System.out.println("\n           --- LOG IN ---");
            System.out.println("|------------------------------------|");
            System.out.println("  Type 'exit' to return to Main Menu.");
            System.out.println("|------------------------------------|");

            System.out.print("Enter Email: ");
            String email = sc.nextLine().trim();
            //sc.nextLine();

            if (email.equalsIgnoreCase("exit")) {
                System.out.println("Returning to Main Menu.");
                return;
            }

            System.out.print("Enter Password: ");
            String pass = sc.nextLine();

            String hashedPass = db.hashPassword(pass);
            if (hashedPass == null) {
                System.out.println("Error: Password hashing failed. Please try again.");
                continue;
            }

            String sql = "SELECT u_id, u_type, u_status FROM tbl_user WHERE u_email = ? AND u_pass = ?";
            List<Map<String, Object>> result = db.fetchRecords(sql, email, hashedPass);

            if (!result.isEmpty()) {
                Map<String, Object> user = result.get(0);
                String userType = (String) user.get("u_type");
                String userStatus = (String) user.get("u_status");
                int userId = (int) user.get("u_id");

                if (!userStatus.equalsIgnoreCase("Approved")) {
                    System.out.println("Login Failed: Your account status is " + userStatus + ". Please wait for approval.");
                    continue; 
                }

                System.out.println("Login successful! User Type: " + userType);

                switch (userType) {
                    case "Student":
                        studentdashboard studentDash = new studentdashboard(db, sc, userId);
                        studentDash.runStudentDashboard();
                        break;
                    case "Instructor":
                        instructordashboard instructorDash = new instructordashboard(db, sc, userId);
                        instructorDash.runInstructorDashboard();
                        break;
                    case "Admin":
                    case "SuperAdmin":
                        admindashboard adminDash = new admindashboard(db, sc, userId);
                        adminDash.runAdminDashboard();
                        break;
                    default:
                        System.out.println("Unknown user type. Access denied.");
                }
                return;

            } else {
                System.out.println("Invalid email or password. Please hit 'ENTER' button.");
            }
        }
    }

    public static int getIntInput(Scanner sc, int min, int max) {
        while (true) {
            if (sc.hasNextInt()) {
                int input = sc.nextInt();
                if (input >= min && input <= max) {
                    return input;
                } else {
                    System.out.print("Input must be between " + min + " and " + max + ". Try again: ");
                }
            } else {
                String trash = sc.next(); 
                System.out.print("Invalid input. Please enter a number. Try again: ");
            }
        }
    }
}