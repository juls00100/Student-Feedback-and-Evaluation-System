package main4;

import config.config;
import java.util.InputMismatchException;
import java.util.Scanner;
import java.util.List; 
import java.util.Map;


public class main4 {

    // --- NEW: Super Admin Logic ---
    // This email is the permanent, protected identifier for the system account.
    private static final String SUPER_ADMIN_EMAIL = "developer@system.com"; 
    
    /**
     * Initializes the Super Admin account if it does not already exist in the database.
     * If missing, it prompts the user (developer) to set the name and password once.
     */
    public static void initializeSuperAdmin(config con, Scanner sc) {
        // Check if Super Admin exists using the unique email identifier
        String checkQry = "SELECT u_id FROM tbl_user WHERE u_email = ?";
        List<Map<String, Object>> result = con.fetchRecords(checkQry, SUPER_ADMIN_EMAIL);

        if (result.isEmpty()) {
            System.out.println("\n=======================================================");
            System.out.println(">>> FIRST RUN SETUP: SUPER ADMIN ACCOUNT CREATION <<<");
            System.out.println("The protected Super Admin account is missing.");
            System.out.println("This account will be created with the email: " + SUPER_ADMIN_EMAIL);
            
            System.out.print("Enter your Name (e.g., System Developer): ");
            String saName = sc.nextLine();
            
            System.out.print("Set the Super Admin Password: ");
            String saPass = sc.nextLine();
            
            // Create the Super Admin account in the database
            String insertQry = "INSERT INTO tbl_user(u_name, u_email, u_type, u_status, u_pass) VALUES (?, ?, ?, ?, ?)";
            // Using 'SuperAdmin' as the type. This user is automatically Approved.
            con.addRecord(insertQry, saName, SUPER_ADMIN_EMAIL, "SuperAdmin", "Approved", saPass);
            System.out.println("SUCCESS: Super Admin account initialized and stored in the database.");
            System.out.println("=======================================================\n");
        }
    }
    // --- END Super Admin Logic ---


    private static void viewInstructors(Scanner sc, String[] instructors) {
            System.out.println("\n--- INSTRUCTOR LIST ---");
        for (int i = 0; i < instructors.length; i++) {
            System.out.println((i + 1) + ". " + instructors[i]);
        }
    }

    public static void main(String[] args) {
        config db = new config();
        db.connectDB();
        
        Scanner sc = new Scanner(System.in);
        
        // NEW: Initialize Super Admin on startup (prompts for credentials if not found)
        initializeSuperAdmin(db, sc); 
        
        System.out.println("Sweetiest Welcome to you! In my Evaluation System");

        int mainChoice;
        do {
            System.out.println("\n--- MAIN MENU ---");
            System.out.println("1. STUDENT Login/Register"); 
            System.out.println("2. USER Login (Admin/Instructor/SuperAdmin)"); 
            System.out.println("3. USER Register"); 
            System.out.println("4. EXIT");
            System.out.print("Choose an option: ");

            mainChoice = getIntInput(sc, 1, 4);

            switch (mainChoice) {
                case 1:
                    // NOTE: Student registration currently does not require Admin approval.
                    String loggedInStudentID = studentFlow(sc, db); 
                    if (loggedInStudentID != null) {
                        studentMenu(sc, db, loggedInStudentID);
                    }
                    break;
                case 2:
                    userLoginFlow(sc, db);
                    break;
                case 3:
                    // Admin/Instructor registration is set to Pending by userRegistrationFlow
                    userRegistrationFlow(sc, db);
                    break;
                case 4:
                    System.out.println("YOU CHOSEN EXIT. SEE YOU AGAIN!");
                    break;
            }
        } while (mainChoice != 4);
        sc.close();
    }

    public static void userLoginFlow(Scanner sc, config con) {
        System.out.println("\n--- USER LOGIN (ADMIN/INSTRUCTOR/SUPER ADMIN) ---");
        System.out.print("ENTRE EMAIL: ");
        String em = sc.nextLine(); 
        System.out.print("ENTER PASSWORD: ");
        String pas = sc.nextLine(); 

        String qry = "SELECT * FROM tbl_user WHERE u_email = ? AND u_pass = ?";
        
        List<Map<String, Object>> result = con.fetchRecords(qry, em, pas); 

        if (result.isEmpty()) {
            System.out.println("INVALID CREDENTIALS");
        } else {
            System.out.println("**********************************************");
            System.out.println("***WELCOME*** ");
            System.out.println(" **********************************************");
            Map<String, Object> user = result.get(0);
            String stat = user.get("u_status").toString();
            String type = user.get("u_type").toString();   
            String name = user.get("u_name").toString();   

            if (stat.equals("Pending")) {
                // Account Approval Requirement Check
                System.out.println("YOU'RE ACCOUNT IS PENDING. PLEASE TELL ADMIN THANKLYOUU.");
            } else if (stat.equals("Approved")) {
                System.out.println("LOGIN SUCCESS! Welcome, " + name + "!");
                
                Object userIdObj = user.get("u_id");
                
                try{
                    // FIX: Safely convert the Object (which might be String or Long) to an int
                    int userId = Integer.parseInt(String.valueOf(userIdObj)); 
                
                    if (type.equals("Admin")) {
                        adminDashboard(sc, con, userId);
                    } else if (type.equals("SuperAdmin")) { // Super Admin Login
                        superAdminDashboard(sc, con, userId);
                    } else if (type.equals("Instructor")) {
                        instructorMainActions(sc, con, userId);
                    } else {
                        System.out.println("USER UNKNOWN.");
                    }
                } 
                catch (NumberFormatException | NullPointerException e) { 
                    System.out.println("ERROR: Could not process User ID. Data corruption suspected. Log in failed.");
                } 
            } else { 
                System.out.println("Your account status is: " + stat + ". Please initiate to contact the Admin.");
            }
        }
    }

    public static void userRegistrationFlow(Scanner sc, config con) {
        System.out.println("\n--- USER REGISTRATION (ADMIN/INSTRUCTOR) ---");
        System.out.print("Enter user name: ");
        String name = sc.nextLine();
        String email;
        
        while (true) {
            System.out.print("Enter user email: ");
            email = sc.nextLine();
            
            // Do not allow registration of the Super Admin email
            if (email.equalsIgnoreCase(SUPER_ADMIN_EMAIL)) {
                System.out.println("This email is reserved for system use.");
                continue;
            }
            
            String qry = "SELECT * FROM tbl_user WHERE u_email = ?";
            List<Map<String, Object>> result = con.fetchRecords(qry, email);

            if (result.isEmpty()) {
                break;
            } else {
                System.out.println("Email already exists. Enter a different email.");
            }
        }

        System.out.print("Enter user Type (1 - Admin / 2 - Instructor): ");
        int typeChoice = getIntInput(sc, 1, 2);
        
        String tp = (typeChoice == 1) ? "Admin" : "Instructor";
        
        System.out.print("Enter Password: ");
        String pass = sc.nextLine();
        
        // All new Admin/Instructor accounts start as 'Pending'
        String sql = "INSERT INTO tbl_user(u_name, u_email, u_type, u_status, u_pass) VALUES (?, ?, ?, ?, ?)";
        con.addRecord(sql, name, email, tp, "Pending", pass);
        
        System.out.println("\nREGISTRATION SUCCESSFUL! Your account is **Pending** Admin approval.");
        System.out.println("You will be able to log in once your account is Approved.");
    }
    
    // Super Admin Dashboard
    public static void superAdminDashboard(Scanner sc, config con, int adminId) {
        int choice;
        do {
            System.out.println("\n===== SUPER ADMIN DASHBOARD =====");
            System.out.println("1. Manage User Approvals and Deletions"); // Can manage all users including Admins
            System.out.println("2. Manage Students"); 
            System.out.println("3. Manage Instructors"); 
            System.out.println("4. Manage Evaluations"); 
            System.out.println("5. Logout");
            System.out.print("Enter choice: ");
            
            choice = getIntInput(sc, 1, 5);
            
            switch (choice) {
                case 1:
                    manageUserApprovals(sc, con, true); // true indicates Super Admin (allows deletion of Admins)
                    break;
                case 2:
                    manageStudents(sc, con);
                    break;
                case 3:
                    manageInstructors(sc, con);
                    break;
                case 4:
                    manageEvaluations(sc, con);
                    break;
                case 5:
                    System.out.println("Logging out from Super Admin Dashboard...");
                    break;
            }
        } while (choice != 5);
    }
    // End Super Admin Dashboard

    public static void adminDashboard(Scanner sc, config con, int adminId) {
        int choice;
        do {
            System.out.println("\n===== ADMIN DASHBOARD =====");
            System.out.println("1. Manage User Approvals"); // Restricted from deleting Admins/SuperAdmin
            System.out.println("2. Manage Students"); 
            System.out.println("3. Manage Instructors"); 
            System.out.println("4. Manage Evaluations"); 
            System.out.println("5. Logout");
            System.out.print("Enter choice: ");
            
            choice = getIntInput(sc, 1, 5);
            
            switch (choice) {
                case 1:
                    manageUserApprovals(sc, con, false); // false indicates regular Admin
                    break;
                case 2:
                    manageStudents(sc, con);
                    break;
                case 3:
                    manageInstructors(sc, con);
                    break;
                case 4:
                    manageEvaluations(sc, con);
                    break;
                case 5:
                    System.out.println("Logging out from Admin Dashboard...");
                    break;
            }
        } while (choice != 5);
    }
    
    // MANAGE USER APPROVALS AND DELETION
    public static void manageUserApprovals(Scanner sc, config con, boolean isSuperAdmin) {
        int choice;
        do {
            System.out.println("\n--- MANAGE USER ACCOUNTS ---");
            System.out.println("1. VIEW ALL USER ACCOUNTS (Excluding System Dev)");
            System.out.println("2. APPROVE PENDING ACCOUNT");
            if (isSuperAdmin) {
                 System.out.println("3. DELETE USER (Includes Admin/Instructor)");
            } else {
                 System.out.println("3. DELETE USER (Instructors Only)");
            }
           
            System.out.println("4. BACK TO MENU");
            System.out.print("Enter choice: ");
            
            choice = getIntInput(sc, 1, 4);
            
            switch (choice) {
                case 1: 
                    // View all non-Super Admin users regardless of status
                    String Query = "SELECT u_id, u_name, u_email, u_type, u_status FROM tbl_user WHERE u_email != ?";
                    String[] headers = {"ID", "Name", "Email", "Type", "Status"};
                    String[] columns = {"u_id", "u_name", "u_email", "u_type", "u_status"};
                    
                    System.out.println("\n--- ALL NON-SUPER USER ACCOUNTS ---");
                    con.viewRecords(Query, headers, columns, SUPER_ADMIN_EMAIL); 
                    break;
                case 2:
                    System.out.println("\n--- APPROVE PENDING USER ACCOUNTS ---");
                    String pendingQuery = "SELECT u_id, u_name, u_email, u_type FROM tbl_user WHERE u_status = 'Pending' AND u_email != ?";
                    String[] pendHeaders = {"ID", "Name", "Email", "Type"};
                    String[] pendColumns = {"u_id", "u_name", "u_email", "u_type"};
                    
                    con.viewRecords(pendingQuery, pendHeaders, pendColumns, SUPER_ADMIN_EMAIL); 

                    System.out.print("Enter ID to Approve (or 0 to cancel): ");
                    int ids = getIntInput(sc, 0, Integer.MAX_VALUE);
                    
                    if (ids > 0) {
                        String sql = "UPDATE tbl_user SET u_status = ? WHERE u_id = ? AND u_status = 'Pending'";
                        con.updateRecord(sql, "Approved", ids);
                        System.out.println("User ID " + ids + " has been **APPROVED**.");
                    } else {
                        System.out.println("Account approval cancelled.");
                    }
                    break;
                case 3:
                    System.out.println("\n--- DELETE USER ACCOUNT ---");
                    System.out.print("Enter User ID to Delete (or 0 to cancel): ");
                    int idToDelete = getIntInput(sc, 0, Integer.MAX_VALUE);

                    if (idToDelete > 0) {
                        deleteUser(con, idToDelete, isSuperAdmin);
                    }
                    break;
                case 4:
                    System.out.println("Returning to Dashboard...");
                    break;
            }
        } while (choice != 4);
    }
    
    /**
     * Handles the deletion of a user and enforces the Super Admin protection rule.
     * @param con Database config
     * @param userIdToDelete The ID of the user to delete
     * @param isSuperAdmin If the caller is a Super Admin
     */
    private static void deleteUser(config con, int userIdToDelete, boolean isSuperAdmin) {
        String getEmailQry = "SELECT u_email, u_type FROM tbl_user WHERE u_id = ?";
        List<Map<String, Object>> userResult = con.fetchRecords(getEmailQry, userIdToDelete);
        
        if (userResult.isEmpty()) {
            System.out.println("Error: User ID not found.");
            return;
        }
        
        String userEmail = userResult.get(0).get("u_email").toString();
        String userType = userResult.get(0).get("u_type").toString();
        
        if (userEmail.equalsIgnoreCase(SUPER_ADMIN_EMAIL)) {
            System.out.println("-----------------------------------------------------------------------");
            System.out.println(">> ERROR: THE SUPER ADMIN ACCOUNT CANNOT BE DELETED. IT IS PROTECTED. <<");
            System.out.println("-----------------------------------------------------------------------");
            return;
        }
        
        if (!isSuperAdmin && userType.equals("Admin")) {
             System.out.println("---------------------------------------------------------------");
             System.out.println(">> ERROR: Regular Admins cannot delete other Admin accounts. <<");
             System.out.println("---------------------------------------------------------------");
             return;
        }

        Scanner sc = new Scanner(System.in);
        System.out.print("Are you sure you want to delete User ID " + userIdToDelete + " (" + userEmail + ")? (Y/N): ");
        String confirmation = sc.nextLine().trim().toUpperCase();

        if (confirmation.equals("Y")) {
            String sql = "DELETE FROM tbl_user WHERE u_id = ?";
            con.deleteRecord(sql, userIdToDelete);
            System.out.println("User ID " + userIdToDelete + " has been **DELETED**.");
        } else {
            System.out.println("Deletion cancelled.");
        }
    }


    private static void instructorMainActions(Scanner sc, config db, int instructorID) {
        int choice;
        do {
            System.out.println("\n--- INSTRUCTOR MENU ---");
            System.out.println("1. VIEW MY EVALUATIONS");
            System.out.println("2. BACK TO MAIN MENU");
            System.out.print("Enter your choice: ");
            
            choice = getIntInput(sc, 1, 2);

            switch (choice) {
                case 1:
                    viewMyEvaluations(sc, db, instructorID);
                    break;
                case 2:
                    System.out.println("Logging out...");
                    break;
            }
        } while (choice != 2);
    }
    
    private static void instructorMenu(Scanner sc, config db) {
        // NOTE: Commented out to avoid confusion
    }
    
    // ... (getIntInput method)
    private static int getIntInput(Scanner sc, int min, int max) {
        int input;
        while (true) {
            try {
                // Check if there's an int to read
                if(sc.hasNextInt()){
                    input = sc.nextInt();
                    sc.nextLine(); // Consume the newline
                    if (input < min || input > max) {
                        System.out.print("PLEASE ENTER A NUMBER BETWEEN " + min + " AND " + max + " ");
                    } else {
                        return input;
                    }
                } else {
                    // Handle non-integer input
                    System.out.print("PROPERLY CHECK YOUR INPUTS... ");
                    System.out.println("PLEASE ENTER A NUMBER:");
                    sc.nextLine(); // Consume the invalid input
                }
            } catch (InputMismatchException e) {
                System.out.print("PROPERLY CHECK YOUR INPUTS... ");
                System.out.println("PLEASE ENTER A NUMBER:");
                sc.nextLine(); 
            }
        }
    }
    
    // (Existing studentFlow method)
    public static String studentFlow(Scanner sc, config db) {
        System.out.println("\n--- STUDENT LOGIN/REGISTRATION ---");
        System.out.print("NEW STUDENT? (Y for yes / N for no): ");
        
        String newStudent = sc.nextLine().trim().toUpperCase();
        
        String studentSchoolID = null; 
        String studentName = "";

        if (newStudent.equals("Y")) {
            System.out.print("ENTER YOUR SCHOOL ID:");
            studentSchoolID = sc.nextLine();
            System.out.print("YOUR FIRST NAME: ");
            String firstName = sc.nextLine();
            System.out.print("YOUR LAST NAME: ");
            String lastName = sc.nextLine();
            System.out.print("YOUR EMAIL: ");
            String email = sc.nextLine();
            System.out.print("YEAR LEVEL (1,2,3, or 4): ");
            String yearLevel = sc.nextLine();
            
            // NOTE: Must check if School ID already exists before registering
            String checkQry = "SELECT s_schoolID FROM tbl_student WHERE s_schoolID = ?";
            if (!db.fetchRecords(checkQry, studentSchoolID).isEmpty()) {
                System.out.println("ERROR: A student with School ID " + studentSchoolID + " already exists.");
                return null;
            }

            // Student registration now includes s_status set to 'Pending'
            String sqlStudent = "INSERT INTO tbl_student (s_schoolID, s_first_name, s_last_name, s_email, s_year_level, s_status) VALUES (?,?,?,?,?,?)";
            db.addRecord(sqlStudent, studentSchoolID, firstName, lastName, email, yearLevel, "Pending");

            studentName = firstName + " " + lastName;
            System.out.println("\nREGISTRATION SUCCESSFUL, " + studentName + "!");
            System.out.println("Your account status is **PENDING**. Please wait for an Admin to approve your registration.");
            return null; // Return null so they can't proceed to the menu yet
            
        } else {
            boolean valid = false;
            while (!valid) {
                System.out.print("Enter your School ID: ");
                studentSchoolID = sc.nextLine();
                System.out.print("Enter your Full Name: ");
                studentName = sc.nextLine();

                // Modified query to also fetch s_status
                String sqlCheck = "SELECT s_schoolID, s_status FROM tbl_student WHERE s_schoolID = ? AND s_first_name || ' ' || s_last_name = ?";
                List<Map<String, Object>> studentResult = db.fetchRecords(sqlCheck, studentSchoolID, studentName);

                if (!studentResult.isEmpty()) {
                    String status = studentResult.get(0).get("s_status").toString();
                    
                    if (status.equals("Approved")) {
                        valid = true;
                        System.out.println("SUCCESSFUL LOG-IN for, " + studentName + "!");
                    } else {
                        // Reject login if status is not Approved (e.g., Pending)
                        System.out.println("Login failed. Your account status is: **" + status + "**.");
                        System.out.println("Please wait for an Admin to approve your registration.");
                        // Exit the login loop without setting 'valid' to true
                        return null; 
                    }
                } else {
                    System.out.println("Invalid School ID or name. Try again or register as a new student.");
                }
            }
        }
    return studentSchoolID;
    }

    // (Existing studentMenu method)
    private static void studentMenu (Scanner sc, config db, String studentSchoolID){
        int choyens;
    do {
        System.out.println("\n--- STUDENT MENU ---");
        System.out.println("1. CREATE EVALUATION");
        System.out.println("2. VIEW EVALUATIONS");
        System.out.println("3. EDIT EVALUATION");
        System.out.println("4. DELETE EVALUATION");
        System.out.println("5. VIEW ACCOUT INFO.");
        System.out.println("6. EXIT");


        System.out.print("Choose an option: ");

        choyens = getIntInput(sc, 1, 6);

        switch (choyens) {
            case 1:
                studentEvaluation(sc, db, studentSchoolID);
                break;
                
             case 2:
                viewEvaluation(sc, db, studentSchoolID);
                break;
                
             case 3: 
                editEvaluation(sc, db, studentSchoolID);
                break;
             
             case 4: 
                deleteEvaluation(sc, db, studentSchoolID);
                break;
             
             case 5:
                 viewMyAcc(sc, db, studentSchoolID);
                 break;
                 
             case 6:
                System.out.println("Returning to Main Menu...");
                break;
        }
      
    } while (choyens != 6);
  
}
       
    // (Existing studentEvaluation method)
    private static void studentEvaluation(Scanner sc, config db, String studentSchoolID){
        
        String[] instructors = {
            "Ching Archival", "Dalley Alterado", "Rose Gamboa",
            "Aries Dajay", "Fil Aripal", "Joseph Lanza",
            "Ramel Obejero", "Michael John Bustamante", "Aries Dajay", "Zillah Nodalo"
        };
        String[] courses = {
            "ART APPRECIATION", "ECONOMICS, TAXATION, AND LAND REFORMS", "LOGIC",
            "INFORMATION MANAGEMENT", "PC ASSEMBLING AND DISASSEMBLING",
            "PRINCIPLES OF ACCOUNTING", "PE3", "OBJECT ORIENTED PROGRAMMING (OOP1)",
            "DATA STRUCTURES AND ALGORITHM", "CHRISTOLOGY"
        };


        System.out.println("--- INSTRUCTOR SELECTION ---");
    
        viewInstructors(sc, instructors);
        System.out.print("Enter the number of the Instructor you want to evaluate: ");
        int instructorChoice = getIntInput(sc, 1, instructors.length);
        String selectedInstructor = instructors[instructorChoice - 1];



        System.out.println("\n--- COURSE SELECTION ---");
        for (int i = 0; i < courses.length; i++) {
            System.out.println((i + 1) + ". " + courses[i]);
        }
        System.out.print("Enter the number of the Course/Subject taught by " + selectedInstructor + ": ");
        int courseChoice = getIntInput(sc, 1, courses.length);
        String selectedCourse = courses[courseChoice - 1];
        

        System.out.println("\n--- EVALUATION ---");
        System.out.println("YOU ARE EVALUATING **" + selectedInstructor + "** FOR THE COURSE **" + selectedCourse + "**.");
    
        System.out.println("\n--- RATING SCALE ---");
        System.out.println("5 - Observed to a large extent");
        System.out.println("4 - Observed");
        System.out.println("3 - Observed to a minimal extent");
        System.out.println("2 - Much to be desired upon");
        System.out.println("1 - Not Observed");

        String[] questions = {
            "Class activities are meaningful and directly related to the course material.",
            "The instructor is adaptable and adjusts their approach to meet individual student needs.",
            "The instructor provides clear instructions and expectations for all assignments and exams.",
            "The instructor actively involves students in the learning process.",
            "Class time is used efficiently and effectively by the instructor.",
            "Assignments and homework are returned promptly with feedback.",
            "The instructor has clear procedures in place that prevent time from being wasted."
        };

        int totalRating = 0;
        for (int i = 0; i < questions.length; i++) {
            System.out.println("\nQuestion " + (i + 1) + " (for " + selectedCourse + "): " + questions[i]);
            System.out.print("Your rating (1-5): ");
            int rating = getIntInput(sc, 1, 5);
            totalRating += rating;
        }

        double averageRating = (double) totalRating / questions.length;
        String formattedRating = String.format("%.2f", averageRating);
    
        
        System.out.print("\nPROVIDE COMMENTS AND RECOMMENDATIONS (for " + selectedInstructor + "): ");
        String comments = sc.nextLine();

        String currentYear = "2025";
        String currentSem = "1";

        
        String sqlEvaluation = "INSERT INTO tbl_evaluation (i_id, e_average_rating, e_year, e_sem, e_remarks, s_schoolID) VALUES (?, ?, ?, ?, ?, ?)";
        db.addRecord(sqlEvaluation, String.valueOf(instructorChoice), formattedRating, currentYear, currentSem, comments, studentSchoolID);

        System.out.println("\nTHANKS FOR COMPLETING THE SURVEY FOR " + selectedInstructor + " IN " + selectedCourse + "!");
        System.out.println("REDIRECTING TO THE MENU...");
    }

    // (Existing viewEvaluation method)
    private static void viewEvaluation(Scanner sc, config db, String studentSchoolID) { 
        System.out.println("\n--- VIEW MY EVALUATIONS (ID: " + studentSchoolID + ") ---");

        if (studentSchoolID == null || studentSchoolID.isEmpty()) {
            System.out.println("ERROR: Could not retrieve your School ID. Please re-login.");
            System.out.println("\nPress Enter to continue...");
            sc.nextLine();
            return;
        }

        
    String sql = "SELECT t2.i_first_name || ' ' || t2.i_last_name AS instructor_name, " +
                 "e_average_rating, e_year, e_sem, e_remarks " +
                 "FROM tbl_evaluation t1 " +
                 "JOIN tbl_instructor t2 ON t1.i_id = t2.i_id " +
                 "WHERE t1.s_schoolID = ?";

        String[] headers = {"Instructor", "Avg Rating", "Year", "Semester", "Remarks"};
        String[] columns = {"instructor_name", "e_average_rating", "e_year", "e_sem", "e_remarks"};

        db.viewRecords(sql, headers, columns, studentSchoolID);

        System.out.println("\nPress Enter to continue...");
        sc.nextLine();
    }
    
    // (Existing editEvaluation method)
    private static void editEvaluation(Scanner sc, config db, String studentSchoolID) {
    System.out.println("\n--- EDIT EVALUATION (ID: " + studentSchoolID + ") ---");
    
    String sqlView = "SELECT t1.e_id, t2.i_first_name || ' ' || t2.i_last_name AS instructor_name, " +
                     "e_average_rating, e_remarks " +
                     "FROM tbl_evaluation t1 " +
                     "JOIN tbl_instructor t2 ON t1.i_id = t2.i_id " +
                     "WHERE t1.s_schoolID = ?";

    String[] headers = {"Eval ID", "Instructor", "Avg Rating", "Remarks"};
    String[] columns = {"e_id", "instructor_name", "e_average_rating", "e_remarks"};

    System.out.println("YOUR EVALUATIONS:");
    db.viewRecords(sqlView, headers, columns, studentSchoolID);
    
    System.out.print("\nEnter the Evaluation ID (Eval ID) you want to edit: ");
    int evalIdToEdit = getIntInput(sc, 1, Integer.MAX_VALUE);
    
    System.out.println("\n--- EDITING EVALUATION ID " + evalIdToEdit + " ---");
    System.out.print("Enter NEW COMMENTS and RECOMMENDATIONS (Current will be replaced): ");
    String newComments = sc.nextLine();
    
    if (newComments.isEmpty()) {
        System.out.println("YOU CHANGED NOTHING1");
        System.out.println("\nPRESSS ENTER TO CONTINUUEE..");
        sc.nextLine();
        return;
    }

    String sqlUpdate = "UPDATE tbl_evaluation SET e_remarks = ? WHERE e_id = ? AND s_schoolID = ?";
    
    db.updateRecord(sqlUpdate, newComments, String.valueOf(evalIdToEdit), studentSchoolID);
    
    System.out.println("EVALUATION ID " + evalIdToEdit + " HAVE BEEN SUCCESSFULLY UPDATEDD.");

    System.out.println("\nPREESS ENTER TO CONTINUEEE...");
    sc.nextLine();
    }
    
    
    // (Existing deleteEvaluation method)
    private static void deleteEvaluation(Scanner sc, config db, String studentSchoolID) {
    System.out.println("\n--- DELETE EVALUATION (ID: " + studentSchoolID + ") ---");

    String sqlView = "SELECT t1.e_id, t2.i_first_name || ' ' || t2.i_last_name AS instructor_name, " +
                     "e_average_rating, e_remarks " +
                     "FROM tbl_evaluation t1 " +
                     "JOIN tbl_instructor t2 ON t1.i_id = t2.i_id " +
                     "WHERE t1.s_schoolID = ?";

    String[] headers = {"Eval ID", "Instructor", "Avg Rating", "Remarks"};
    String[] columns = {"e_id", "instructor_name", "e_average_rating", "e_remarks"};
    
    System.out.println("YOUR EVALUATIONS:");
    db.viewRecords(sqlView, headers, columns, studentSchoolID);

    System.out.print("\nEnter Evaluation ID (Eval ID) you want to delete: ");
    int evalIdToDelete = getIntInput(sc, 1, Integer.MAX_VALUE);
    
    System.out.print("Are you sure you want to delete Evaluation ID " + evalIdToDelete + "? (Y/N): ");
    String confirmation = sc.nextLine().trim().toUpperCase();

    if (confirmation.equals("Y")) {
      
        String sqlDelete = "DELETE FROM tbl_evaluation WHERE e_id = ? AND s_schoolID = ?";
        
        db.deleteRecord(sqlDelete, String.valueOf(evalIdToDelete), studentSchoolID);
        System.out.println(" EVALUATION ID " + evalIdToDelete + " HAS NOW BEEN DELETED.");
    } else {
        System.out.println("DELETION CNCELLED!");
    }
    
    System.out.println("\nPPPPRESS ENTER TO CONTINUE..");
    sc.nextLine();
    }
    
    
    // (Existing viewMyAcc method)
    private static void viewMyAcc(Scanner sc, config db, String studentSchoolID) {
    System.out.println("\n--- VIEW ACCOUNT INFO (ID: " + studentSchoolID + ") ---");

    // Modified to include status
    String sql = "SELECT s_schoolID, s_first_name, s_last_name, s_email, s_year_level, s_status " +
                 "FROM tbl_student WHERE s_schoolID = ?";

    String[] headers = {"School ID", "First Name", "Last Name", "Email", "Year Level", "Status"};
    String[] columns = {"s_schoolID", "s_first_name", "s_last_name", "s_email", "s_year_level", "s_status"};

    db.viewRecords(sql, headers, columns, studentSchoolID);

    System.out.println("\nPress Enter to continue...");
    sc.nextLine();
    }
    
    // (Existing viewMyEvaluations method)
    private static void viewMyEvaluations(Scanner sc, config db, int instructorID) {
        System.out.println("\n--- MY EVALUATIONS ---"); 
        String sql = "SELECT e_average_rating, e_year, e_sem, e_remarks FROM tbl_evaluation WHERE i_id = ?";
        String[] headers = {"Avg Rating", "Year", "Semester", "Remarks"};
        String[] columns = {"e_average_rating", "e_year", "e_sem", "e_remarks"};
        
        db.viewRecords(sql, headers, columns, String.valueOf(instructorID));
        
        System.out.println("\nPress Enter to continue...");
        sc.nextLine();
    }
    
    // (Existing manageInstructors method)
    private static void manageInstructors(Scanner sc, config db) {
        int choice;
        do {
            System.out.println("\n--- MANAGE INSTRUCTORS ---");
            System.out.println("1. ADD INSTRUCTOR");
            System.out.println("2. VIEW INSTRUCTOR LIST");
            System.out.println("3. UPDATE INSTRUCTOR DETAILS");
            System.out.println("4. DELETE INSTRUCTOR");
            System.out.println("5. BACK TO ADMINISTRATOR MENU...");
            System.out.print("Enter your choice: ");

            choice = getIntInput(sc, 1, 5);

            switch (choice) {
                case 1 : {
                    System.out.print("Enter instructor's First Name: ");
                    String firstName = sc.nextLine();
                    System.out.print("Enter instructor's Last Name: ");
                    String lastName = sc.nextLine();
                     System.out.print("Enter password: ");
                    String password = sc.nextLine();
                    String sql = "INSERT INTO tbl_instructor (i_first_name, i_last_name, i_password) VALUES (?, ?, ?)";
                    db.addRecord(sql, firstName, lastName, password);
                } break;
                
                case 2 : {
                    String sql = "SELECT i_id, i_first_name, i_last_name FROM tbl_instructor";
                    String[] headers = {"ID", "First Name", "Last Name"};
                    String[] columns = {"i_id", "i_first_name", "i_last_name"};
                    db.viewRecords(sql, headers, columns);
                }break;
                
                case 3 :{
                    System.out.print("ENTER INSTRUCTOR ID TO UPDATE: ");
                    int id = getIntInput(sc, 1, Integer.MAX_VALUE);
                    System.out.print("Enter new first name (leave blank to keep current): ");
                    String firstName = sc.nextLine();
                    System.out.print("Enter new last name (leave blank to keep current): ");
                    String lastName = sc.nextLine();
                    System.out.print("Enter new password (leave blank to keep current): ");
                    String password = sc.nextLine();

                    if (!firstName.isEmpty() || !lastName.isEmpty() || !password.isEmpty()) {
                        StringBuilder sqlBuilder = new StringBuilder("UPDATE tbl_instructor SET ");
                        int count = 0;
                        if (!firstName.isEmpty()) { 
                            sqlBuilder.append("i_first_name = ?"); 
                            count++; 
                        }
                        if (!lastName.isEmpty()) { 
                            if (count > 0) sqlBuilder.append(", ");
                            sqlBuilder.append("i_last_name = ?"); 
                            count++; 
                        }
                        if (!password.isEmpty()) {
                            if (count > 0) sqlBuilder.append(", ");
                            sqlBuilder.append("i_password = ?");
                            count++;
                        }
                        sqlBuilder.append(" WHERE i_id = ?");
                        
                        // Collect parameters
                        Object[] params = new Object[count + 1];
                        int paramIndex = 0;
                        if (!firstName.isEmpty()) params[paramIndex++] = firstName;
                        if (!lastName.isEmpty()) params[paramIndex++] = lastName;
                        if (!password.isEmpty()) params[paramIndex++] = password;
                        params[paramIndex] = id;
                        
                        db.updateRecord(sqlBuilder.toString(), params);
                        System.out.println("INSTRUCTOR ID " + id + " UPDATED.");
                    } else {
                        System.out.println("NO CHANGES MADE.");
                    }
                }break;
                case 4 : {
                    System.out.print("ENTER INSTRUCTOR ID TO DELETE: ");
                    int id = getIntInput(sc, 1, Integer.MAX_VALUE);
                    String sql = "DELETE FROM tbl_instructor WHERE i_id = ?";
                    db.deleteRecord(sql, id);
                }break;
                case 5 : System.out.println("RETURNING TO ADMIN MENU...");
                break;
            }
        } while (choice != 5);
    }

    // (Updated manageStudents method)
    private static void manageStudents(Scanner sc, config db) {
        int choice;
        do {
            System.out.println("\n--- MANAGE STUDENTS ---");
            System.out.println("1. ADD STUDENT");
            System.out.println("2. VIEW STUDENT'S LIST");
            System.out.println("3. APPROVE PENDING STUDENTS"); // NEW OPTION
            System.out.println("4. UPDATE STUDENT'S INFO.");
            System.out.println("5. DELETE STUDNET");
            System.out.println("6. BACK TO MENU"); // Option count increased to 6
            System.out.print("Enter your choice: ");

            choice = getIntInput(sc, 1, 6); // Max choice is now 6

            switch (choice) {
                case 1 : {
                    System.out.print("ENTER SCHOOL ID: ");
                    String id = sc.nextLine();
                    System.out.print("FIRST NAME: ");
                    String firstName = sc.nextLine();
                    System.out.print("LAST NAME: ");
                    String lastName = sc.nextLine();
                    System.out.print("EMAIL: ");
                    String email = sc.nextLine();
                    System.out.print("YEAR LEVEL: ");
                    String yearLevel = sc.nextLine();

                    // Adding new student with status 'Approved' (assuming Admin adding means they are approved)
                    String sql = "INSERT INTO tbl_student (s_schoolID, s_first_name, s_last_name, s_email, s_year_level, s_status) VALUES (?, ?, ?, ?, ?, ?)";
                    db.addRecord(sql, id, firstName, lastName, email, yearLevel, "Approved");
                }
                break;
                case 2 : {
                    // View all students including status
                    String sql = "SELECT s_schoolID, s_first_name, s_last_name, s_email, s_year_level, s_status FROM tbl_student";
                    String[] headers = {"School ID", "First Name", "Last Name", "Email", "Year Level", "Status"};
                    String[] columns = {"s_schoolID", "s_first_name", "s_last_name", "s_email", "s_year_level", "s_status"};
                    db.viewRecords(sql, headers, columns);
                }
                break;
                case 3 : { // New: Approve Pending Students
                    System.out.println("\n--- APPROVE PENDING STUDENT ACCOUNTS ---");
                    
                    String pendingQuery = "SELECT s_schoolID, s_first_name, s_last_name, s_email FROM tbl_student WHERE s_status = 'Pending'";
                    String[] pendHeaders = {"School ID", "First Name", "Last Name", "Email"};
                    String[] pendColumns = {"s_schoolID", "s_first_name", "s_last_name", "s_email"};
                    
                    db.viewRecords(pendingQuery, pendHeaders, pendColumns); 
                    
                    System.out.print("Enter School ID to Approve (or 0 to cancel): ");
                    String idToApprove = sc.nextLine().trim();
                    
                    if (!idToApprove.equals("0") && !idToApprove.isEmpty()) {
                        String sql = "UPDATE tbl_student SET s_status = ? WHERE s_schoolID = ? AND s_status = 'Pending'";
                        db.updateRecord(sql, "Approved", idToApprove);
                        System.out.println("Student ID " + idToApprove + " has been **APPROVED**.");
                    } else {
                        System.out.println("Student approval cancelled.");
                    }
                }
                break;
                case 4 : { // Update Student Info (was 3)
                    System.out.print("ENTER SCHOOL ID TO UPDATE: ");
                    String id = sc.nextLine();
                    System.out.print("NEW FIRST NAME(leave blank to keep current): ");
                    String firstName = sc.nextLine();
                    System.out.print("NEW LAST NAME(leave blank to keep current): ");
                    String lastName = sc.nextLine();
                    System.out.print("NEW EMAIL(leave blank to keep current): ");
                    String email = sc.nextLine();
                    System.out.print("NEW YEAR LEVEL(leave blank to keep current): ");
                    String yearLevel = sc.nextLine();

                    StringBuilder sql = new StringBuilder("UPDATE tbl_student SET ");
                    int count = 0;
                    
                    if (!firstName.isEmpty()) { sql.append("s_first_name = ?"); count++; }
                    if (!lastName.isEmpty()) { if (count > 0) sql.append(", "); sql.append("s_last_name = ?"); count++; }
                    if (!email.isEmpty()) { if (count > 0) sql.append(", "); sql.append("s_email = ?"); count++; }
                    if (!yearLevel.isEmpty()) { if (count > 0) sql.append(", "); sql.append("s_year_level = ?"); count++; }

                    if (count > 0) {
                        sql.append(" WHERE s_schoolID = ?");
                        
                        Object[] params = new Object[count + 1];
                        int paramIndex = 0;
                        if (!firstName.isEmpty()) params[paramIndex++] = firstName;
                        if (!lastName.isEmpty()) params[paramIndex++] = lastName;
                        if (!email.isEmpty()) params[paramIndex++] = email;
                        if (!yearLevel.isEmpty()) params[paramIndex++] = yearLevel;
                        params[paramIndex] = id;

                        db.updateRecord(sql.toString(), params);
                        System.out.println("STUDENT ID " + id + " UPDATED.");
                    } else {
                        System.out.println("NO CHANGES MADE.");
                    }
                }
                break;
                case 5 : { // Delete Student (was 4)
                    System.out.print("ENTER SCHOOL ID TO DELETE: ");
                    String id = sc.nextLine();
                    String sql = "DELETE FROM tbl_student WHERE s_schoolID = ?";
                    db.deleteRecord(sql, id);
                }
                break;
                case 6 : System.out.println("RETURNING TO ADMIN MENU..."); // Back (was 5)
                break;
            }
        } while (choice != 6);
    }

    private static void manageEvaluations(Scanner sc, config db) {
        int choice;
        do {
            System.out.println("\n--- MANAGE EVALUATIONS ---");
            System.out.println("1. VIEW ALL EVALUATIONS");
            System.out.println("2. VIEW EVALUATION BY INSTRUCTOR");
            System.out.println("3. DELETE EVALUATION");
            System.out.println("4. BACK TO MENU");
            System.out.print("Enter your choice: ");

            choice = getIntInput(sc, 1, 4);

            switch (choice) {
                case 1: {
                    String sql = "SELECT t1.e_id, t3.s_schoolID, t2.i_first_name || ' ' || t2.i_last_name AS instructor_name, " +
                                 "e_average_rating, e_remarks, e_year, e_sem " +
                                 "FROM tbl_evaluation t1 " +
                                 "JOIN tbl_instructor t2 ON t1.i_id = t2.i_id " +
                                 "JOIN tbl_student t3 ON t1.s_schoolID = t3.s_schoolID";
                    String[] headers = {"Eval ID", "Student ID", "Instructor", "Avg Rating", "Remarks", "Year", "Sem"};
                    String[] columns = {"e_id", "s_schoolID", "instructor_name", "e_average_rating", "e_remarks", "e_year", "e_sem"};
                    db.viewRecords(sql, headers, columns);
                }
                break;
                case 2: {
                    System.out.print("ENTER INSTRUCTOR ID TO VIEW EVALUATIONS: ");
                    int id = getIntInput(sc, 1, Integer.MAX_VALUE);
                    String sql = "SELECT t1.e_id, t3.s_schoolID, t2.i_first_name || ' ' || t2.i_last_name AS instructor_name, " +
                                 "e_average_rating, e_remarks, e_year, e_sem " +
                                 "FROM tbl_evaluation t1 " +
                                 "JOIN tbl_instructor t2 ON t1.i_id = t2.i_id " +
                                 "JOIN tbl_student t3 ON t1.s_schoolID = t3.s_schoolID " +
                                 "WHERE t1.i_id = ?";
                    String[] headers = {"Eval ID", "Student ID", "Instructor", "Avg Rating", "Remarks", "Year", "Sem"};
                    String[] columns = {"e_id", "s_schoolID", "instructor_name", "e_average_rating", "e_remarks", "e_year", "e_sem"};
                    db.viewRecords(sql, headers, columns, id);
                }
                break;
                case 3: {
                    System.out.print("ENTER EVALUATION ID TO DELETE: ");
                    int id = getIntInput(sc, 1, Integer.MAX_VALUE);
                    String sql = "DELETE FROM tbl_evaluation WHERE e_id = ?";
                    db.deleteRecord(sql, id);
                }
                break;
                case 4: System.out.println("RETURNING TO ADMIN MENU...");
                break;
            }
        } while (choice != 4);
    }
}
