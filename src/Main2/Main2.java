package Main2;

import config.config;
import java.util.Scanner;
import java.util.List; 
import java.util.Map;


public class Main2 {

    private static final String SUPER_ADMIN_EMAIL = "julioscampaner@gmail.com"; 

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        config db = new config();
        db.connectDB(); 

        initializeSuperAdmin(db, sc);

        int choice;
        do {
            System.out.println("\n--- MAIN MENU ---");
            System.out.println("1. REGISTER AS STUDENT");
            System.out.println("2. REGISTER AS ADMIN");
            System.out.println("3. REGISTER AS INSTRUCTOR");
            System.out.println("4. LOG IN AS SUPERADMIN");
            System.out.println("5. LOG IN AS ADMIN");
            System.out.println("6. LOG IN AS INSTRUCTOR");
            System.out.println("7. LOG IN AS STUDENT");
            System.out.println("8. EXIT");
            System.out.print("Enter your choice: ");

            choice = getIntInput(sc, 1, 8); 

            switch (choice) {
                case 1: registerUserByType(sc, db, "Student"); break;
                case 2: registerUserByType(sc, db, "Admin"); break;
                case 3: registerUserByType(sc, db, "Instructor"); break;
                case 4: userLoginFlow(sc, db, "SuperAdmin"); break;
                case 5: userLoginFlow(sc, db, "Admin"); break;
                case 6: userLoginFlow(sc, db, "Instructor"); break;
                case 7: userLoginFlow(sc, db, "Student"); break;
                case 8: System.out.println("Exiting application. Goodbye!"); break;
            }
        } while (choice != 8);

        db.closeDB(); 
        sc.close();
    }

    public static void initializeSuperAdmin(config db, Scanner sc) {
        String checkQry = "SELECT u_id FROM tbl_user WHERE u_email = ?";
        List<Map<String, Object>> result = db.fetchRecords(checkQry, SUPER_ADMIN_EMAIL);

        if (result.isEmpty()) {
            System.out.println("\n=======================================================");
            System.out.println("SUPER ADMIN ACCOUNT CREATION");
            
            System.out.print("Enter your SuperAdmin Name: ");
            String saName = sc.nextLine();
            
            System.out.print("Set the SuperAdmin Password: ");
            String saPass = sc.nextLine();

            String hashedSaPass = db.hashPassword(saPass); 
            
            String insertQry = "INSERT INTO tbl_user(u_name, u_email, u_type, u_status, u_pass) VALUES (?, ?, ?, ?, ?)";
            db.addRecord(insertQry, saName, SUPER_ADMIN_EMAIL, "SuperAdmin", "Approved", hashedSaPass);
            
            System.out.println("Super Admin Account CREATED and APPROVED.");
            System.out.println("=======================================================");
        }
    }

    public static void registerUserByType(Scanner sc, config db, String userType) {
        System.out.println("\n--- NEW " + userType.toUpperCase() + " REGISTRATION ---");
        sc.nextLine();
        System.out.print("Enter Email: ");
        String email = sc.nextLine();
        System.out.print("Enter Password: ");
        String pass = sc.nextLine();

        String hashedPass = db.hashPassword(pass);
        if (hashedPass == null) {
            System.out.println("Registration failed due to password hashing error.");
            return;
        }
        
        String status = "Pending";
        String name = ""; 
        
        String insertQry = "INSERT INTO tbl_user(u_name, u_email, u_type, u_status, u_pass) VALUES (?, ?, ?, ?, ?)";
        int generatedUserId = db.addRecordAndReturnId(insertQry, name, email, userType, status, hashedPass);

        if (generatedUserId > 0) {
            System.out.println("User registered successfully. Status: " + status);

            if (userType.equals("Student")) {
                System.out.print("Enter Student School ID (ex. scc-00-00): ");
                String schoolID = sc.nextLine();
                System.out.print("Enter First Name: ");
                String firstName = sc.nextLine();
                System.out.print("Enter Last Name: ");
                String lastName = sc.nextLine();
                System.out.print("Enter Year Level:");
                String yl = sc.nextLine();
              
                String studentQry = "INSERT INTO tbl_student(s_schoolID, s_first_name, s_last_name, s_year_level, s_u_id) VALUES (?, ?, ?, ?, ?)";
                db.addRecord(studentQry, schoolID, firstName, lastName, yl,  generatedUserId);

                name = firstName + " " + lastName;
                
           } else if (userType.equals("Instructor")) {
            System.out.print("Enter First Name: ");
            String firstName = sc.nextLine();
            System.out.print("Enter Last Name: ");
            String lastName = sc.nextLine();
            
            name = firstName + " " + lastName; 
           
            String instructorQry = "INSERT INTO tbl_instructor(i_first_name, i_last_name, i_u_id) VALUES (?, ?, ?, ?)";
            db.addRecord(instructorQry, firstName, lastName, generatedUserId);
            
            System.out.println("Instructor profile created for: " + name);
            
                
           }else if (userType.equals("Admin")) {
                System.out.print("Enter Full Name for Admin Account: ");
                name = sc.nextLine();
            } 

            if (!name.isEmpty()) {
                 String updateNameQry = "UPDATE tbl_user SET u_name = ? WHERE u_id = ?";
                 db.updateRecord(updateNameQry, name, generatedUserId);
            }

        } else {
            System.out.println("Registration failed. Email might already be in use.");
        }
    }
    
    public static void userLoginFlow(Scanner sc, config db, String expectedType) {
        System.out.println("\n--- " + expectedType.toUpperCase() + " LOGIN ---");
        sc.nextLine();
          
        System.out.print("Enter Email: ");
        String email = sc.nextLine();
        System.out.print("Enter Password: ");
        String pass = sc.nextLine();

        String hashedPass = db.hashPassword(pass);
        if (hashedPass == null) return;
  
        String sql = "SELECT * FROM tbl_user WHERE u_email = ? AND u_pass = ? AND u_type = ?";
        List<Map<String, Object>> result = db.fetchRecords(sql, email, hashedPass, expectedType);

        if (result.isEmpty()) {
            System.out.println("INVALID CREDENTIALS or you are not a registered " + expectedType.toUpperCase() + ".");
            return;
        } 

        Map<String, Object> user = result.get(0);
        String stat = user.get("u_status").toString();
        String type = user.get("u_type").toString();   
        String name = user.get("u_name").toString();
        Object userIdObj = user.get("u_id");
        int userId = -1;

        try {
            userId = Integer.parseInt(String.valueOf(userIdObj));
        } catch (NumberFormatException e) {
            System.out.println("ERROR: Could not process User ID. Login failed.");
            return;
        }

        if (stat.equals("Pending")) {
            System.out.println("YOUR ACCOUNT STATUS IS PENDING. Please contact the Admin (for Instructors and Students)/SuperAdmin(Admins).");
        } else if (stat.equals("Approved")) {
            System.out.println("LOGIN SUCCESS! Welcome, " + name + "!");

            if (type.equals("SuperAdmin")) { 
                superAdminMainActions(sc, db); 

            } else if (type.equals("Admin")) {
                adminDashboard(sc, db, userId);

            } else if (type.equals("Instructor")) {
                String idQry = "SELECT i_id FROM tbl_instructor WHERE i_first_name || ' ' || i_last_name = ?";
                List<Map<String, Object>> instructorResult = db.fetchRecords(idQry, name);

                if (!instructorResult.isEmpty()) {
                int instructorId = (int) instructorResult.get(0).get("i_id");  
                instructorMainActions(sc, db, instructorId); 

                } else {
                    System.out.println("Error: Instructor profile not found. Log in failed.");
                }
            } else if (type.equals("Student")) {
                studentMenu (sc, db, userId);
            }
        } else { 
            System.out.println("Your account status is: " + stat + ". Contact the Admin.");
        }
    }

    private static void superAdminMainActions(Scanner sc, config db) {
        int choice;
        do {
            System.out.println("\n===== SUPER ADMIN DASHBOARD =====");
            System.out.println("1. APPROVE ADMINS");
            System.out.println("2. MANAGE INSTRUCTOR AND STUDENT APPROVALS");
            System.out.println("3. MANAGE STUDENTS");
            System.out.println("4. MANAGE INSTRUCTORS");
            System.out.println("5. MANAGE EVALUATIONS");
            System.out.println("6. LOG OUT");
            System.out.print("Enter choice: ");

            choice = getIntInput(sc, 1, 6);

            switch (choice) {
                case 1:
                    approveAdmins(sc, db);
                    break;
                case 2:
                    manageApprovals(sc, db); 
                    break;
                case 3:
                    manageStudents(sc, db);
                    break;
                case 4:
                    manageInstructors(sc, db);
                    break;
                case 5:
                    manageEvaluations(sc, db);
                    break;
                case 6:
                    System.out.println("Super Admin logged out successfully.");
                    break;
            }
        } while (choice != 6);
    }
    
    private static void approveAdmins(Scanner sc, config db) {
        System.out.println("\n--- PENDING ADMIN ACCOUNTS ---");

        String sql = "SELECT u_id, u_email, u_type, u_status FROM tbl_user WHERE u_type = 'Admin' AND u_status = 'Pending'";
        String[] headers = {"User ID", "Email", "Type", "Status"};
        String[] columns = {"u_id", "u_email", "u_type", "u_status"};

        List<Map<String, Object>> pendingAdmins = db.fetchRecords(sql);

        if (pendingAdmins.isEmpty()) {
            System.out.println("No pending Admin accounts to approve.");
            System.out.println("\nPress Enter to continue...");
            sc.nextLine(); 
            sc.nextLine(); 
            return;
        }

        db.viewRecords(sql, headers, columns);

        System.out.print("\nEnter the User ID to Approve/Reject (or 0 to cancel): ");
        int targetId = getIntInput(sc, 0, Integer.MAX_VALUE);
        sc.nextLine(); 

        if (targetId == 0) {
            return;
        }

        System.out.print("Approve or Reject Admin (A/R): ");
        String action = sc.nextLine().trim().toUpperCase();

        String newStatus = null;
        if (action.equals("A")) {
            newStatus = "Active";
            System.out.println("Admin account approved.");
        } else if (action.equals("R")) {
            newStatus = "Rejected";
            System.out.println("Admin account rejected.");
        } else {
            System.out.println("Invalid action. Returning to menu.");
            return;
        }

        String updateSql = "UPDATE tbl_user SET u_status = ? WHERE u_id = ? AND u_type = 'Admin'";
        db.updateRecord(updateSql, newStatus, targetId);

        System.out.println("\nPress Enter to continue...");
        sc.nextLine(); 
    }
    
    private static void manageApprovals(Scanner sc, config db) {
        int choice;
        do {
            System.out.println("\n--- MANAGE APPROVALS ---");
            System.out.println("1. MANAGE PENDING STUDENT APPROVALS");
            System.out.println("2. MANAGE PENDING INSTRUCTOR APPROVALS");
            System.out.println("3. BACK TO MENU");
            System.out.print("Enter your choice: ");

            choice = getIntInput(sc, 1, 3);
            sc.nextLine(); 

            switch (choice) {
                case 1:
                    processApprovals(sc, db, "Student");
                    break;
                case 2:
                    processApprovals(sc, db, "Instructor");
                    break;
                case 3:
                    System.out.println("Returning to Dashboard...");
                    break;
            }
        } while (choice != 3);
    }
    
    private static void processApprovals(Scanner sc, config db, String userType) {
        System.out.println("\n--- PENDING " + userType.toUpperCase() + " ACCOUNTS ---");

        String sql = "SELECT u_id, u_email, u_status FROM tbl_user WHERE u_type = ? AND u_status = 'Pending'";
        String[] headers = {"User ID", "Email", "Status"};
        String[] columns = {"u_id", "u_email", "u_status"};

        List<Map<String, Object>> pendingUsers = db.fetchRecords(sql, userType);

        if (pendingUsers.isEmpty()) {
            System.out.println("No pending " + userType + " accounts to approve.");
            System.out.println("\nPress Enter to continue...");
            sc.nextLine(); // Wait for user
            return;
        }

        db.viewRecords(sql, headers, columns, userType);

        System.out.print("\nEnter the User ID to Approve/Reject (or 0 to cancel): ");
        int targetId = getIntInput(sc, 0, Integer.MAX_VALUE);
        sc.nextLine(); 

        if (targetId == 0) {
            return;
        }

        System.out.print("Approve or Reject " + userType + " (A/R): ");
        String action = sc.nextLine().trim().toUpperCase();

        String newStatus = null;
        if (action.equals("A")) {
            newStatus = "Active";
            System.out.println(userType + " account approved.");
        } else if (action.equals("R")) {
            newStatus = "Rejected";
            System.out.println(userType + " account rejected.");
        } else {
            System.out.println("Invalid action. Returning to menu.");
            System.out.println("\nPress Enter to continue...");
            sc.nextLine(); // Wait for user
            return;
        }

        String updateSql = "UPDATE tbl_user SET u_status = ? WHERE u_id = ? AND u_type = ?";
        db.updateRecord(updateSql, newStatus, targetId, userType);

        System.out.println("\nPress Enter to continue...");
        sc.nextLine(); 
    }

    private static void studentMenu (Scanner sc, config db, int userId){
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
                    studentEvaluation(sc, db, userId);
                    break;

                case 2:
                    viewEvaluation(sc, db, userId);
                    break;

                case 3: 
                    editEvaluation(sc, db, userId);
                    break;

                case 4: 
                    deleteEvaluation(sc, db, userId);
                    break;

                case 5:
                    viewMyAcc(sc, db, userId);
                    break;

                case 6:
                    System.out.println("Returning to Main Menu...");
                    break;
            }

        } while (choyens != 6);
    }

    private static void studentEvaluation(Scanner sc, config db, int userId){

        String[] instructors = {
            "Ching Archival", "Dalley Alterado", "Rose Gamboa",
            "Aries Dajay", "Fil Aripal", "Joseph Lanza",
            "Ramel Obejero", "Michael John Bustamante", "Zillah Nodalo"
        };
        String[] courses = {
            "ART APPRECIATION", "ECONOMICS, TAXATION, AND LAND REFORMS", "LOGIC",
            "INFORMATION MANAGEMENT", "PC ASSEMBLING AND DISASSEMBLING",
            "PRINCIPLES OF ACCOUNTING", "PE3", "OBJECT ORIENTED PROGRAMMING (OOP1)",
            "DATA STRUCTURES AND ALGORITHM", "CHRISTOLOGY"
        };


        //System.out.println("--- INSTRUCTOR SELECTION ---");

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
        sc.nextLine(); 

        System.out.print("\nPROVIDE COMMENTS AND RECOMMENDATIONS (for " + selectedInstructor + "): ");
        String comments = sc.nextLine();

        String currentYear = "2025";
        String currentSem = "1";

        String sSchoolID = getStudentSchoolID(db, userId);
        if (sSchoolID != null) {
        String sqlEvaluation = "INSERT INTO tbl_evaluation (i_id, e_average_rating, e_year, e_sem, e_remarks, s_schoolID) VALUES (?, ?, ?, ?, ?, ?)";
        db.addRecord(sqlEvaluation, String.valueOf(instructorChoice), formattedRating, currentYear, currentSem, comments, sSchoolID);
    }

        System.out.println("\nTHANKS FOR COMPLETING THE SURVEY FOR " + selectedInstructor + " IN " + selectedCourse + "!");
        System.out.println("REDIRECTING TO THE MENU...");
    }

    private static void viewEvaluation(Scanner sc, config db, int userId) { 
    System.out.println("\n--- VIEW MY EVALUATIONS (ID: " + userId + ") ---");

    String sSchoolID = getStudentSchoolID(db, userId); 
    if (sSchoolID == null) return; 

    String sql = "SELECT t2.i_first_name || ' ' || t2.i_last_name AS instructor_name, " +
                   "e_average_rating, e_year, e_sem, e_remarks " +
                   "FROM tbl_evaluation t1 " +
                   "JOIN tbl_instructor t2 ON t1.i_id = t2.i_id " +
                  "WHERE t1.s_schoolID = ?";

    String[] headers = {"Instructor", "Avg Rating", "Year", "Semester", "Remarks"};
    String[] columns = {"instructor_name", "e_average_rating", "e_year", "e_sem", "e_remarks"};

    db.viewRecords(sql, headers, columns, sSchoolID); 

    System.out.println("\nPress Enter to continue...");
    sc.nextLine(); 
}
    
    private static void editEvaluation(Scanner sc, config db, int userId) {
        System.out.println("\n--- EDIT EVALUATION (ID: " + userId + ") ---");

        String sqlView = "SELECT t1.e_id, t2.i_first_name || ' ' || t2.i_last_name AS instructor_name, " +
                         "e_average_rating, e_remarks " +
                         "FROM tbl_evaluation t1 " +
                         "JOIN tbl_instructor t2 ON t1.i_id = t2.i_id " +
                         "WHERE t1.s_schoolID = ?";

        String[] headers = {"Eval ID", "Instructor", "Avg Rating", "Remarks"};
        String[] columns = {"e_id", "instructor_name", "e_average_rating", "e_remarks"};

        System.out.println("YOUR EVALUATIONS:");
        db.viewRecords(sqlView, headers, columns, userId);

        System.out.print("\nEnter the Evaluation ID (Eval ID) you want to edit: ");
        int evalIdToEdit = getIntInput(sc, 1, Integer.MAX_VALUE);

        // ** FIX: Consume the leftover newline from getIntInput() **
        sc.nextLine(); 

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

        db.updateRecord(sqlUpdate, newComments, String.valueOf(evalIdToEdit), userId);

        System.out.println("EVALUATION ID " + evalIdToEdit + " HAVE BEEN SUCCESSFULLY UPDATEDD.");

        System.out.println("\nPREESS ENTER TO CONTINUEEE...");
        sc.nextLine();
    }

    private static void deleteEvaluation(Scanner sc, config db, int userId) {
        System.out.println("\n--- DELETE EVALUATION (ID: " + userId + ") ---");

        String sqlView = "SELECT t1.e_id, t2.i_first_name || ' ' || t2.i_last_name AS instructor_name, " +
                         "e_average_rating, e_remarks " +
                         "FROM tbl_evaluation t1 " +
                         "JOIN tbl_instructor t2 ON t1.i_id = t2.i_id " +
                         "WHERE t1.s_schoolID = ?";

        String[] headers = {"Eval ID", "Instructor", "Avg Rating", "Remarks"};
        String[] columns = {"e_id", "instructor_name", "e_average_rating", "e_remarks"};

        System.out.println("YOUR EVALUATIONS:");
        db.viewRecords(sqlView, headers, columns, userId);

        System.out.print("\nEnter Evaluation ID (Eval ID) you want to delete: ");
        int evalIdToDelete = getIntInput(sc, 1, Integer.MAX_VALUE);

        // ** FIX: Consume the leftover newline from getIntInput() **
        sc.nextLine(); 

        System.out.print("Are you sure you want to delete Evaluation ID " + evalIdToDelete + "? (Y/N): ");
        String confirmation = sc.nextLine().trim().toUpperCase();

        if (confirmation.equals("Y")) {

            String sqlDelete = "DELETE FROM tbl_evaluation WHERE e_id = ? AND s_schoolID = ?";

            db.deleteRecord(sqlDelete, String.valueOf(evalIdToDelete), userId);
            System.out.println(" EVALUATION ID " + evalIdToDelete + " HAS NOW BEEN DELETED.");
        } else {
            System.out.println("DELETION CNCELLED!");
        }

        System.out.println("\nPPPPRESS ENTER TO CONTINUE..");
        sc.nextLine();
    }


    private static void viewMyAcc(Scanner sc, config db, int userId) {
        System.out.println("\n--- VIEW ACCOUNT INFO (ID: " + userId + ") ---");

        String sql = "SELECT s_schoolID, s_first_name, s_last_name, s_email, s_year_level, s_status " +
                     "FROM tbl_student WHERE s_schoolID = ?";

        String[] headers = {"School ID", "First Name", "Last Name", "Email", "Year Level", "Status"};
        String[] columns = {"s_schoolID", "s_first_name", "s_last_name", "s_email", "s_year_level", "s_status"};

        db.viewRecords(sql, headers, columns, userId);

        System.out.println("\nPress Enter to continue...");
        sc.nextLine();
    }
    public static void studentMainActions(Scanner sc, config db, int studentUserId) {
        int choice;
        do {
            System.out.println("\n--- STUDENT DASHBOARD (User ID: " + studentUserId + ") ---");
            System.out.println("1. EVALUATE INSTRUCTOR");
            System.out.println("2. VIEW PAST EVALUATIONS");
            System.out.println("3. LOGOUT");
            System.out.print("Enter your choice: ");

            choice = getIntInput(sc, 1, 3);
            
            switch (choice) {
                case 1:
        	    evaluateInstructor(sc, db, studentUserId); 
                   break;
                case 2:
                    viewStudentEvals(db, studentUserId); 
                    break;
                case 3:
                    System.out.println("Logging out...");
                    break;
            }
        } while (choice != 3);
    }
    public static void evaluateInstructor(Scanner sc, config db, int studentUserId) {
        System.out.println("\n--- START INSTRUCTOR EVALUATION ---");
        String studentQry = "SELECT s_schoolID FROM tbl_student WHERE s_u_id = ?";
        List<Map<String, Object>> studentResult = db.fetchRecords(studentQry, studentUserId);
        if (studentResult.isEmpty()) {
            System.out.println("Error: Student profile not found. Cannot evaluate.");
            return;
        }
        String sSchoolID = studentResult.get(0).get("s_schoolID").toString();

        System.out.println("\n--- Available Instructors ---");
        String instructorListQry = "SELECT i_id, i_first_name || ' ' || i_last_name AS InstructorName FROM tbl_instructor";
        String[] listHeaders = {"ID", "Name"};
        String[] listColumns = {"i_id", "InstructorName"};
        db.viewRecords(instructorListQry, listHeaders, listColumns);

        System.out.print("Enter the ID of the instructor you want to evaluate: ");
        int instructorId = getIntInput(sc, 1, Integer.MAX_VALUE);

        System.out.print("Enter Average Rating (1-5): ");
        double rating = (double) getIntInput(sc, 1, 5); 
        System.out.print("Enter Remarks/Comments: ");
        String remarks = sc.nextLine();
        
        String currentYear = "2025";
        String currentSem = "1";

        String insertEvalQry = "INSERT INTO tbl_evaluation (i_id, s_schoolID, e_average_rating, e_remarks, e_year, e_sem) VALUES (?, ?, ?, ?, ?, ?)";
        db.addRecord(insertEvalQry, instructorId, sSchoolID, rating, remarks, currentYear, currentSem);

        System.out.println("\n✅ Evaluation successfully submitted for Instructor ID " + instructorId + "!");
    }

    public static void viewStudentEvals(config db, int studentUserId) {
        System.out.println("\n--- VIEW YOUR PAST EVALUATIONS ---");

        String studentQry = "SELECT s_schoolID FROM tbl_student WHERE s_u_id = ?";
        List<Map<String, Object>> studentResult = db.fetchRecords(studentQry, studentUserId);
        if (studentResult.isEmpty()) {
            System.out.println("Error: Student profile not found. Cannot view evaluations.");
            return;
        }
        String sSchoolID = studentResult.get(0).get("s_schoolID").toString();
        String sql = "SELECT t1.e_id, "
            + "t2.i_first_name || ' ' || t2.i_last_name AS instructor_name, "
            + "t1.e_average_rating, t1.e_remarks, t1.e_year, t1.e_sem "
            + "FROM tbl_evaluation t1 "
            + "JOIN tbl_instructor t2 ON t1.i_id = t2.i_id " // Join to get instructor name
            + "WHERE t1.s_schoolID = ?";

        String[] headers = {"Eval ID", "Instructor", "Avg Rating", "Remarks", "Year", "Sem"};
        String[] columns = {"e_id", "instructor_name", "e_average_rating", "e_remarks", "e_year", "e_sem"};

        db.viewRecords(sql, headers, columns, sSchoolID);

        System.out.println("END OF EVALUATION");
    }

    private static int getIntInput(Scanner sc, int min, int max) {
        int input = -1;
        while (true) {
            try {
                if (sc.hasNextInt()) {
                    input = sc.nextInt();
                    if (input >= min && input <= max) {
                        break;
                    } else {
                        System.out.print("Invalid choice. Enter a number between " + min + " and " + max + ": ");
                    }
                } else {
                    System.out.print("Invalid input. Please enter a number: ");
                    sc.next(); 
                }
            } catch (Exception e) {
                System.out.println("An unexpected error occurred: " + e.getMessage());
                sc.nextLine(); 
            }
        }
        return input;
    }

    private static void viewInstructors(Scanner sc, String[] instructors) {
        System.out.println("--- Available Instructors ---");
        for (int i = 0; i < instructors.length; i++) {
            System.out.println((i + 1) + ". " + instructors[i]);
        }
    }

    private static void viewAllUsersExceptSuperAdmin(config con) {
        String Query = "SELECT u_id, u_name, u_email, u_type, u_status FROM tbl_user WHERE u_email != ?";
        String[] headers = {"ID", "Name", "Email", "Type", "Status"};
        String[] columns = {"u_id", "u_name", "u_email", "u_type", "u_status"};
        
        System.out.println("\n--- ALL NON-SUPER USER ACCOUNTS ---");
        con.viewRecords(Query, headers, columns, SUPER_ADMIN_EMAIL);
    }

    public static void userRegistrationFlow(Scanner sc, config con) {
        System.out.println("\nWELCOME! PLEASE COMPLETE THE DETAILS.");
        System.out.print("ENTER USERNAME: ");
        String name = sc.nextLine();
        String email;
        
        while (true) {
            System.out.print("ENTER EMAIL: ");
            email = sc.nextLine();
            
            if (email.equalsIgnoreCase(SUPER_ADMIN_EMAIL)) {
                System.out.println("This email is reserved for system use.");
                continue;
            }
            
            String qry = "SELECT * FROM tbl_user WHERE u_email = ?";
            List<Map<String, Object>> result = con.fetchRecords(qry, email);

            if (result.isEmpty()) {
                break;
            } else {
                System.out.println("ENTERED EMAIL WAS ALREADY USED. PLEASE ENTER DIFFERENT ONE.");
            }
        }

        System.out.print("TYPE OF ACCOUNT (1 - Admin / 2 - Instructor): ");
        int typeChoice = getIntInput(sc, 1, 2);
        
        String tp = (typeChoice == 1) ? "Admin" : "Instructor";
        
        System.out.print("ENTER YOUR PASSWORD: ");
        String pass = sc.nextLine();
        String hashedPass = con.hashPassword(pass);
        
        String sql = "INSERT INTO tbl_user(u_name, u_email, u_type, u_status, u_pass) VALUES (?, ?, ?, ?, ?)";
        con.addRecord(sql, name, email, tp, "Pending", hashedPass);
        
        System.out.println("\nREGISTRATION SUCCESSFUL! Your account is Pending please wait for Admin's approval.");
        System.out.println("You will be able to log in once your account is Approved.");
    }
    
    public static void superAdminDashboard(Scanner sc, config con, int adminId) {
        int choice;
        do {
            System.out.println("\n===== SUPER ADMIN DASHBOARD =====");
            System.out.println("1. User Approvals and Deletions"); 
            System.out.println("2. Manage Students"); 
            System.out.println("3. Manage Instructors"); 
            System.out.println("4. Manage Evaluations"); 
            System.out.println("5. Logout");
            System.out.print("Enter choice: ");
            
            choice = getIntInput(sc, 1, 5);
            
            switch (choice) {
                case 1:
                    manageUserApprovals(sc, con, true); 
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
    
    public static void adminDashboard(Scanner sc, config con, int adminId) {
        int choice;
        do {
            System.out.println("\n===== ADMIN DASHBOARD =====");
            System.out.println("1. MANAGE INSTRUCTOR AND STUDENTS APPROVALS"); 
            System.out.println("2. MANAGE STUDENTS"); 
            System.out.println("3. MANAGE INSTRUCTORS"); 
            System.out.println("4. MANAGE EVALUATIONS"); 
            System.out.println("5. LOG OUT");
            System.out.print("Enter choice: ");
            
            choice = getIntInput(sc, 1, 5);
            
            switch (choice) {
                case 1:
                    manageUserApprovals(sc, con, false); 
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
    
    public static void manageUserApprovals(Scanner sc, config con, boolean isSuperAdmin) {
        int choice;
        do {
            System.out.println("\n--- MANAGE USER ACCOUNTS ---");
            System.out.println("1. VIEW ALL USER ACCOUNTS");
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
                    viewAllUsersExceptSuperAdmin(con);
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
                        String checkQry = "SELECT u_type FROM tbl_user WHERE u_id = ?";
                        List<Map<String, Object>> result = con.fetchRecords(checkQry, ids);

                    if (!result.isEmpty()) {
                        String userTypeToApprove = (String) result.get(0).get("u_type");
                    if (!isSuperAdmin && userTypeToApprove.equals("Admin")) {
                        System.out.println("\n!! ACCESS DENIED !! Only the Super Admin can approve other Admin accounts.");
                        break; // Stop execution here
                    }
                    String sql = "UPDATE tbl_user SET u_status = ? WHERE u_id = ? AND u_status = 'Pending'";
                    con.updateRecord(sql, "Approved", ids);
                    System.out.println("User ID " + ids + " has been **APPROVED**.");
                    } else {
                    System.out.println("Error: User ID " + ids + " not found or status changed.");
                        }
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
            System.out.println(">> ERROR: THE SUPER ADMIN ACCOUNT CANNOT BE DELETED. IT IS PROTECTED. <<33");
            System.out.println("-----------------------------------------------------------------------");
            return;
        }
        
        if (!isSuperAdmin && userType.equals("Admin")) {
             System.out.println("---------------------------------------------------------------");
             System.out.println(">> ERROR: Regular Admins cannot delete other Admin accounts. <<33");
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


    public static void instructorMainActions(Scanner sc, config db, int instructorID) {
        int choice;
        do {
            System.out.println("\n--- INSTRUCTOR MENU ---");
            System.out.println("1. VIEW EVALUATIONS");
            System.out.println("2. BACK TO MAIN MENU");
            System.out.print("Enter your choice: ");
            
            choice = getIntInput(sc, 1, 2);

            switch (choice) {
                case 1:
                    System.out.println("\n--- Julios YOUR OVERALL EVALUATION RATING ---");

                    String sql = "SELECT DISTINCT t1.e_id, t1.e_average_rating, t1.e_remarks, t1.e_year, t1.e_sem, t3.s_schoolID, t3.s_first_name || ' ' || t3.s_last_name AS student_name " +
                                "FROM tbl_evaluation t1 " +
                                "JOIN tbl_student t3 ON t1.s_schoolID = t3.s_schoolID " + 
                                "WHERE t1.i_id = ?";

                   String[] headers = {"Eval ID", "Avg Rating", "Remarks", "Year", "Sem", "Student ID", "Student Name"};
                   String[] columns = {"e_id", "e_average_rating", "e_remarks", "e_year", "e_sem", "s_schoolID", "student_name"};

                   db.viewRecords(sql, headers, columns, instructorID); 
                    sc.nextLine();
                    break;
                case 2:
                    System.out.println("Logging out...");
                    break;
            }
        } while (choice != 2);
    }
    
    public static String studentFlow(Scanner sc, config db) {
        System.out.println("\n--- STUDENT REGISTRATIONS AND LOG IN---");
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
            
            String checkQry = "SELECT s_schoolID FROM tbl_student WHERE s_schoolID = ?";
            if (!db.fetchRecords(checkQry, studentSchoolID).isEmpty()) {
                System.out.println("ERROR: A student with School ID " + studentSchoolID + " already exists.");
                return null;
            }

            String sqlStudent = "INSERT INTO tbl_student (s_schoolID, s_first_name, s_last_name, s_email, s_year_level, s_status) VALUES (?,?,?,?,?,?)";
            db.addRecord(sqlStudent, studentSchoolID, firstName, lastName, email, yearLevel, "Pending");

            studentName = firstName + " " + lastName;
            System.out.println("\nREGISTRATION SUCCESSFUL, " + studentName + "!");
            System.out.println("Your account status is **PENDING**. Please wait for an Admin to approve your registration.");
            return null; 
            
        } else {
            boolean valid = false;
            while (!valid) {
                System.out.print("Enter your School ID: ");
                studentSchoolID = sc.nextLine();
                System.out.print("Enter your Full Name: ");
                studentName = sc.nextLine();

                String sqlCheck = "SELECT s_schoolID, s_status FROM tbl_student WHERE s_schoolID = ? AND s_first_name || ' ' || s_last_name = ?";
                List<Map<String, Object>> studentResult = db.fetchRecords(sqlCheck, studentSchoolID, studentName);

                if (!studentResult.isEmpty()) {
                    String status = studentResult.get(0).get("s_status").toString();
                    
                    if (status.equals("Approved")) {
                        valid = true;
                        System.out.println("SUCCESSFUL LOG-IN for, " + studentName + "!");
                    } else {
                        System.out.println("Login failed. Your account status is: **" + status + "**.");
                        System.out.println("Please wait for an Admin to approve your registration.");
                        return null; 
                    }
                } else {
                    System.out.println("Invalid School ID or name. Try again or register as a new student.");
                }
            }
        }
    return studentSchoolID;
    }
    
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
            sc.nextLine();

            switch (choice) {
                case 1 : {
                    System.out.print("Enter instructor's First Name: ");
                    String firstName = sc.nextLine();
                    System.out.print("Enter instructor's Last Name: ");
                    String lastName = sc.nextLine();
                     System.out.print("Enter password: ");
                    String password = sc.nextLine();
                    String hashedPassword = db.hashPassword(password);
                    String sql = "INSERT INTO tbl_instructor (i_first_name, i_last_name, i_password) VALUES (?, ?, ?)";
                    db.addRecord(sql, firstName, lastName, hashedPassword);
                    System.out.println("\nInstructor " + firstName + " " + lastName + " ADDED.");
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
                    
                    String hashedPassword = "";
                    if (!password.isEmpty()) {
                    hashedPassword = db.hashPassword(password);
                    }

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
                        if (!password.isEmpty()) { if (count > 0) sqlBuilder.append(", "); 
                        sqlBuilder.append("i_password = ?"); count++; }
                        sqlBuilder.append(" WHERE i_id = ?");
                        
                        Object[] params = new Object[count + 1];
                        int paramIndex = 0;
                        if (!firstName.isEmpty()) params[paramIndex++] = firstName;
                        if (!lastName.isEmpty()) params[paramIndex++] = lastName;
                        if (!password.isEmpty()) params[paramIndex++] = hashedPassword;
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

    private static void manageStudents(Scanner sc, config db) {
        int choice;
        do {
            System.out.println("\n--- MANAGE STUDENTS ---");
            System.out.println("1. ADD STUDENT");
            System.out.println("2. VIEW STUDENT'S LIST");
            System.out.println("3. APPROVE PENDING STUDENTS"); 
            System.out.println("4. UPDATE STUDENT'S INFO.");
            System.out.println("5. DELETE STUDNET");
            System.out.println("6. BACK TO MENU"); 
            System.out.print("Enter your choice: ");

            choice = getIntInput(sc, 1, 6); 

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

                    String sql = "INSERT INTO tbl_student (s_schoolID, s_first_name, s_last_name, s_email, s_year_level, s_status) VALUES (?, ?, ?, ?, ?, ?)";
                    db.addRecord(sql, id, firstName, lastName, email, yearLevel, "Approved");
                }
                break;
                case 2 : {
                    String sql = "SELECT s_schoolID, s_first_name, s_last_name, s_year_level FROM tbl_student";
                    String[] headers = {"School ID", "First Name", "Last Name", "Year Level"};
                    String[] columns = {"s_schoolID", "s_first_name", "s_last_name", "s_year_level"};
                    db.viewRecords(sql, headers, columns);
                }
                break;
                case 3 : { 
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
                case 4 : { 
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
                case 5 : { 
                    System.out.print("ENTER SCHOOL ID TO DELETE: ");
                    String id = sc.nextLine();
                    String sql = "DELETE FROM tbl_student WHERE s_schoolID = ?";
                    db.deleteRecord(sql, id);
                }
                break;
                case 6 : System.out.println("RETURNING TO ADMIN MENU..."); 
                break;
            }
        } while (choice != 6);
    }

    private static void manageEvaluations(Scanner sc, config db) {
        int choice;
        do {
            System.out.println("\n--- MANAGE EVALUATIONS ---");
            System.out.println("1. VIEW ALL EVALUATIONS");
            System.out.println("2. VIEW EVALUATION PER INSTRUCTOR");
            System.out.println("3. DELETE EVALUATION");
            System.out.println("4. BACK TO MENU");
            System.out.print("Enter your choice: ");

            choice = getIntInput(sc, 1, 4);

            switch (choice) {
                case 1: {
                    String sql = "SELECT DISTINCT t1.e_id, t1.s_schoolID, t2.i_first_name || ' ' || t2.i_last_name AS instructor_name, " +
                 "t1.e_average_rating, t1.e_remarks, t1.e_year, t1.e_sem " +
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
                    String sql = "SELECT DISTINCT t1.e_id, t1.s_schoolID, t2.i_first_name || ' ' || t2.i_last_name AS instructor_name, " +
                             "t1.e_average_rating, t1.e_remarks, t1.e_year, t1.e_sem " +
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
                    sc.nextLine();
                    String sql = "DELETE FROM tbl_evaluation WHERE e_id = ?";
                    db.deleteRecord(sql, id);
                    System.out.println("Evaluation ID " + id + " has been deleted.");
                }
                break;
                case 4: System.out.println("RETURNING TO ADMIN MENU...");
                break;
            }
        } while (choice != 4);
    }

    private static String getStudentSchoolID(config db, int userId) {
    String studentQry = "SELECT s_schoolID FROM tbl_student WHERE s_u_id = ?";
    List<Map<String, Object>> studentResult = db.fetchRecords(studentQry, userId);
    if (!studentResult.isEmpty()) {
        // Return the actual String School ID
        return studentResult.get(0).get("s_schoolID").toString();
    }
    System.out.println("Error: Student School ID not found.");
    return null;
}
   
}