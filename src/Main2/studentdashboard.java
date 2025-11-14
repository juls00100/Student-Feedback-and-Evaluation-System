package Main2;

import config.config;
import java.util.Scanner;
import java.util.List;
import java.util.Map;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class studentdashboard {

    private final config db;
    private final Scanner sc;
    private final int userId;

    private final String[] evalQuestions;
    
    public studentdashboard(config db, Scanner sc, int userId) {
        this.db = db;
        this.sc = sc;
        this.userId = userId;
        this.evalQuestions = fetchEvaluationQuestions(db);
    }
    
    private String[] fetchEvaluationQuestions(config db) {
        String sql = "SELECT q_order, q_text FROM tbl_evaluation_questions ORDER BY q_order";

        
        List<Map<String, Object>> records = db.fetchRecords(sql); 

        List<String> questionsList = new ArrayList<>();
        for (Map<String, Object> record : records) {
            
            int order = (int) record.get("q_order"); 
            String text = (String) record.get("q_text");

            
            questionsList.add(order + ". " + text + " (1-5): ");
        } 

        return questionsList.toArray(new String[0]);
    }
    public void runStudentDashboard() {
        int choice;
        int maxChoice =5;
        do {
            System.out.println("\n     --- STUDENT DASHBOARD ---");
            System.out.println(" ---------------------------------------");
            System.out.println("|  1. SUBMIT NEW INSTRUCTOR EVALUATION  |");
            System.out.println("|  2. VIEW MY PREVIOUS EVALUATIONS      |");
            System.out.println("|  3. I WANT TO ARCHIVE AN EVALUATION   |");
            System.out.println("|  4. VIEW MY ARCHIVED EVALUATIONS      |");
            System.out.println("|  5. EXIT STUDENT DASHBOARD            |");
            System.out.println(" ---------------------------------------");
            System.out.print("Enter your choice: ");

            choice = Main2.getIntInput(sc, 1, maxChoice); 

            switch (choice) {
                case 1: submitEvaluation(); break;
                case 2: viewMyEvaluations(); break;
                case 3: archiveEvaluation(); break;
                case 4: viewMyArchive(); break;
                case 5: System.out.println("LOGGING OUT..."); break;
                default: System.out.println("Entered invalid number.");
            }
        } while (choice != maxChoice);
    }
    
    private String getStudentSchoolID() {
        String sql = "SELECT s_schoolID FROM tbl_student WHERE s_u_id = ?";
        List<Map<String, Object>> result = db.fetchRecords(sql, userId);
        if (!result.isEmpty()) {
            return (String) result.get(0).get("s_schoolID");
        }
        return null;
    }

    private void submitEvaluation() {
        System.out.println("\n   --- SUBMIT EVALUATION ---");
        
        String schoolId = getStudentSchoolID();
        if (schoolId == null) {
            System.out.println("Error: Student record not found. Cannot proceed with evaluation.");
            return;
        }

        System.out.println("\n   --- Available Instructors ---");
        String instructorSql = "SELECT t1.i_id, t1.i_name FROM tbl_instructor t1 " +
                               "INNER JOIN tbl_user t2 ON t1.i_u_id = t2.u_id " + 
                               "WHERE t2.u_status = 'Approved'";
        
        List<Map<String, Object>> instructors = db.fetchRecords(instructorSql);

        if (instructors.isEmpty()) {
            System.out.println("No approved instructors available for evaluation.");
            return;
        }

        String[] headers = {"ID", "Instructor Name"};
        String[] columns = {"i_id", "i_name"};
        db.viewRecords(instructors, headers, columns); 

        System.out.print("\n  ENTER INSTRUCTOR ID TO EVALUATE: ");
        int instructorId = Main2.getIntInput(sc, 1, Integer.MAX_VALUE);
        sc.nextLine();

        String checkInstructorSql = "SELECT i_id FROM tbl_instructor WHERE i_id = ?";
        List<Map<String, Object>> checkResult = db.fetchRecords(checkInstructorSql, instructorId);
        if (checkResult.isEmpty()) {
             System.out.println("Error: Instructor ID " + instructorId + " not found.");
             return;
        }
        System.out.print("  Enter School Year (e.g., 2024): ");
        String year = sc.nextLine();
        System.out.print("  Enter Semester (e.g., 1st, 2nd, Summer): ");
        String sem = sc.nextLine();
        
        String checkEvalSql = "SELECT 1 FROM tbl_evaluation WHERE s_schoolID = ? AND i_id = ? AND e_year = ? AND e_sem = ?";
        List<Map<String, Object>> existingEval = db.fetchRecords(checkEvalSql, schoolId, instructorId, year, sem);
        if (!existingEval.isEmpty()) {
            System.out.println("Error: You have already submitted an evaluation for this instructor and semester.");
            return;
        }

        List<Integer> scores = new ArrayList<>();
        double totalScore = 0;
        System.out.println("\n   -- Evaluation Questions (Rate 1-5) --");
        
        
        for (String question : this.evalQuestions) {
            System.out.print(question);
            int score = Main2.getIntInput(sc, 1, 5);
            sc.nextLine();
            scores.add(score);
            totalScore += score;
        }
        
        System.out.print("\n  Enter Remarks/Comments: ");
        String remarks = sc.nextLine();

        
        double averageRating = totalScore / this.evalQuestions.length;
        String formattedAvgRating = String.format("%.2f", averageRating);
        
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd:mm:ss");
        String formattedDate = now.format(formatter);
       

try { 
    
    Connection conn = db.getConnection(); 
    
    conn.setAutoCommit(false); 
    
    String evalSql = "INSERT INTO tbl_evaluation(s_schoolID, i_id, e_average_rating, e_remarks, e_year, e_sem, e_date) VALUES(?, ?, ?, ?, ?, ?, ?)";
    db.addRecord(evalSql, schoolId, instructorId, formattedAvgRating, remarks, year, sem, formattedDate);

    String getIdSql = "SELECT e_id FROM tbl_evaluation WHERE s_schoolID = ? AND i_id = ? AND e_year = ? AND e_sem = ?";
    List<Map<String, Object>> result = db.fetchRecords(getIdSql, schoolId, instructorId, year, sem);
    int newEvalId = -1;

    if (!result.isEmpty()) {
        newEvalId = (int) result.get(0).get("e_id");
    } else {
        conn.rollback();
        System.out.println("Critical Error: Evaluation record not found after insertion.");
        return;
    }

    String scoresSql = "INSERT INTO tbl_eval_scores(e_id, es_q_number, es_score) VALUES(?, ?, ?)";
    
    
    try (PreparedStatement scoreStmt = conn.prepareStatement(scoresSql)) { 
        for (int i = 0; i < scores.size(); i++) {
            scoreStmt.setInt(1, newEvalId);
            scoreStmt.setInt(2, i + 1);
            scoreStmt.setInt(3, scores.get(i));
            scoreStmt.addBatch();
        }
        scoreStmt.executeBatch();
        
        conn.commit();
        System.out.println("10 individual scores saved successfully.");
    } catch (SQLException e) {
        conn.rollback();
        System.out.println("Error saving evaluation scores: " + e.getMessage());
        return; 
    } finally {
        conn.setAutoCommit(true);
    }
} catch (SQLException e) { 
     System.out.println("Error managing transaction: " + e.getMessage());
     return; 
}

System.out.println("Evaluation submitted successfully. Average Rating: " + formattedAvgRating); 


    }

    private void viewMyEvaluations() {
        System.out.println("\n                             --- SUBMITTED EVALUATIONS ---");
        
        String schoolId = getStudentSchoolID();
        if (schoolId == null) {
            System.out.println("Error: Student record not found.");
            return;
        }
        String sql = "SELECT t1.e_id, t2.i_name AS instructor_name, t1.e_average_rating, t1.e_remarks, t1.e_year, t1.e_sem, t1.e_date " +
                     "FROM tbl_evaluation t1 " +
                     "INNER JOIN tbl_instructor t2 ON t1.i_id = t2.i_id " +
                     "WHERE t1.s_schoolID = ?"; 
        
        List<Map<String, Object>> records = db.fetchRecords(sql, schoolId);

        if (records.isEmpty()) {
            System.out.println("You have not submitted any evaluations yet.");
            return;
        }

        for (Map<String, Object> record : records) {
            Object ratingObj = record.get("e_average_rating");
            if (ratingObj != null) {
                try {
                    double rating = Double.parseDouble(ratingObj.toString());
                    record.put("e_average_rating", String.format("%.2f", rating));
                } catch (NumberFormatException e) {
                    record.put("e_average_rating", "N/A");
                }
            }
        }
        
        String[] headers = {"Eval ID", "Instructor", "Avg Rating", "Remarks", "Year", "Sem", "Date"};
        String[] columns = {"e_id", "instructor_name", "e_average_rating", "e_remarks", "e_year", "e_sem", "e_date"};
        db.viewRecords(records, headers, columns);

        System.out.print("\nEnter EVAL ID to view individual scores (or 0 to return): ");
        int evalId = Main2.getIntInput(sc, 0, Integer.MAX_VALUE);
        sc.nextLine();

        if (evalId == 0) return;

        String checkOwnershipSql = "SELECT 1 FROM tbl_evaluation WHERE e_id = ? AND s_schoolID = ?";
        List<Map<String, Object>> ownershipCheck = db.fetchRecords(checkOwnershipSql, evalId, schoolId);

        if (ownershipCheck.isEmpty()) {
            System.out.println("Error: Evaluation ID " + evalId + " not found or does not belong to your account.");
            return;
        }

        viewEvaluationDetails(evalId);
    }

    private void viewEvaluationDetails(int evalId) {
        System.out.println("\n--- EVALUATION ID: " + evalId + " DETAILS ---");
        
        String detailsSql = "SELECT es_q_number, es_score FROM tbl_eval_scores WHERE e_id = ? ORDER BY es_q_number";
        List<Map<String, Object>> scores = db.fetchRecords(detailsSql, evalId);

        if (scores.isEmpty()) {
            System.out.println("No detailed scores found for this evaluation.");
            return;
        }
        
        System.out.println("Q# | Score | Question");
        System.out.println("---|-------|-------------------------------------------------------");
        
        for (Map<String, Object> scoreRecord : scores) {
            int qNum = (int) scoreRecord.get("es_q_number");
            int score = (int) scoreRecord.get("es_score");
            
            String question = "";
            
            
            if (qNum > 0 && qNum <= this.evalQuestions.length) { 
                question = this.evalQuestions[qNum - 1].substring(this.evalQuestions[qNum - 1].indexOf(" ") + 1).trim(); 
                question = question.substring(0, question.lastIndexOf("(")).trim();
            } else {
                 question = "Unknown Question";
            }
            
            System.out.printf("%-2d | %-5d | %s\n", qNum, score, question);
        }
    }
    
private void archiveEvaluation() {
        System.out.println("\n                    --- ARCHIVE EVALUATION ---");

        String schoolId = getStudentSchoolID();
        if (schoolId == null) {
            System.out.println("Error: Student record not found.");
            return;
        }
        
        viewMyEvaluations(); 

        System.out.print("\nEnter the Evaluation ID (e_id) to archive (or 0 to cancel): ");
        int evalId = Main2.getIntInput(sc, 0, Integer.MAX_VALUE);
        sc.nextLine();

        if (evalId == 0) {
            System.out.println("Archiving cancelled.");
            return;
        }

        String checkSql = "SELECT 1 FROM tbl_evaluation WHERE e_id = ? AND s_schoolID = ?";
        List<Map<String, Object>> result = db.fetchRecords(checkSql, evalId, schoolId);

        if (result.isEmpty()) {
            System.out.println("Error: Evaluation ID " + evalId + " not found or does not belong to your account.");
            return;
        }

        System.out.print("Are you sure you want to archive Evaluation ID " + evalId + "? (yes/no): ");
        String confirmation = sc.nextLine().trim();

        if (!confirmation.equalsIgnoreCase("yes")) {
            System.out.println("Archiving cancelled by user.");
            return;
        }

        Connection conn = null;
        try {
            conn = db.getConnection();
            conn.setAutoCommit(false);

            String copyEvalSql = "INSERT INTO tbl_archive_evaluation (e_id, i_id, s_schoolID, e_average_rating, e_year, e_sem, e_remarks, e_date) " +
                     "SELECT e_id, i_id, s_schoolID, e_average_rating, e_year, e_sem, e_remarks, e_date FROM tbl_evaluation WHERE e_id = ?";
            db.updateRecord(copyEvalSql, evalId); 
            String copyScoresSql = "INSERT INTO tbl_archive_eval_scores (es_id, e_id, es_q_number, es_score) " +
                                   "SELECT es_id, e_id, es_q_number, es_score FROM tbl_eval_scores WHERE e_id = ?";
            db.updateRecord(copyScoresSql, evalId);
            String deleteScoresSql = "DELETE FROM tbl_eval_scores WHERE e_id = ?";
            db.updateRecord(deleteScoresSql, evalId);
            String deleteEvalSql = "DELETE FROM tbl_evaluation WHERE e_id = ?";
            db.updateRecord(deleteEvalSql, evalId);

            conn.commit(); 
            System.out.println("\nSUCCESS: Evaluation ID " + evalId + " has been moved to the archive.");

        } catch (SQLException e) {
            try {
                if (conn != null) {
                    conn.rollback(); 
                }
            } catch (SQLException ex) {
                System.out.println("Error during rollback: " + ex.getMessage());
            }
            System.out.println("ERROR: Could not archive evaluation due to a database error. (Rollback performed).");
        } finally {
            try {
                if (conn != null && !conn.isClosed()) {
                    conn.setAutoCommit(true); 
                }
            } catch (SQLException e) {
            }
        }
    }

private void viewMyArchive() {
    System.out.println("\n--- MY ARCHIVED EVALUATIONS ---");
    
    String schoolId = getStudentSchoolID();
    if (schoolId == null) {
        System.out.println("Error: Student record not found.");
        return;
    }
    
    String sql = "SELECT t1.e_id, t2.i_name AS instructor_name, t1.e_average_rating, t1.e_remarks, t1.e_year, t1.e_sem, t1.e_date " +
                 "FROM tbl_archive_evaluation t1 " +
                 "INNER JOIN tbl_instructor t2 ON t1.i_id = t2.i_id " +
                 "WHERE t1.s_schoolID = ?"; 
    
    List<Map<String, Object>> records = db.fetchRecords(sql, schoolId);

    if (records.isEmpty()) {
        System.out.println("You have no evaluations in the archive.");
        return;
    }

    for (Map<String, Object> record : records) {
        Object ratingObj = record.get("e_average_rating");
        if (ratingObj != null) {
            try {
                double rating = Double.parseDouble(ratingObj.toString());
                record.put("e_average_rating", String.format("%.2f", rating));
            } catch (NumberFormatException e) {
                record.put("e_average_rating", "N/A");
            }
        }
    }
    
    String[] headers = {"Eval ID", "Instructor", "Avg Rating", "Remarks", "Year", "Sem", "Date"};
    String[] columns = {"e_id", "instructor_name", "e_average_rating", "e_remarks", "e_year", "e_sem", "e_date"};
    db.viewRecords(records, headers, columns);

    System.out.print("\nEnter ARCHIVE EVAL ID to view individual scores (or 0 to return): ");
    int evalId = Main2.getIntInput(sc, 0, Integer.MAX_VALUE);
    sc.nextLine(); 

    if (evalId == 0) return;

    String checkOwnershipSql = "SELECT 1 FROM tbl_archive_evaluation WHERE e_id = ? AND s_schoolID = ?";
    List<Map<String, Object>> ownershipCheck = db.fetchRecords(checkOwnershipSql, evalId, schoolId);

    if (ownershipCheck.isEmpty()) {
        System.out.println("Error: Archived Evaluation ID " + evalId + " not found or does not belong to your account.");
        return;
    }
    viewArchiveEvaluationDetails(evalId);
}
private void viewArchiveEvaluationDetails(int evalId) {
    System.out.println("\n--- ARCHIVED EVALUATION ID: " + evalId + " DETAILS ---");
    
    String detailsSql = "SELECT es_q_number, es_score FROM tbl_archive_eval_scores WHERE e_id = ? ORDER BY es_q_number";
    List<Map<String, Object>> scores = db.fetchRecords(detailsSql, evalId);

    if (scores.isEmpty()) {
        System.out.println("No detailed scores found for this archived evaluation.");
        return;
    }
    
    System.out.println("Q# | Score | Question");
    System.out.println("---|-------|-------------------------------------------------------");
    
    for (Map<String, Object> scoreRecord : scores) {
        int qNum = (int) scoreRecord.get("es_q_number");
        int score = (int) scoreRecord.get("es_score");
        
        String question = "";
        
        if (qNum > 0 && qNum <= this.evalQuestions.length) { 
            question = this.evalQuestions[qNum - 1].substring(this.evalQuestions[qNum - 1].indexOf(" ") + 1).trim(); 
            question = question.substring(0, question.lastIndexOf("(")).trim();
        } else {
             question = "Unknown Question";
        }
        
        System.out.printf("%-2d | %-5d | %s\n", qNum, score, question);
    }
}

}