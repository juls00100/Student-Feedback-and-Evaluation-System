package Main2;

import config.config;
import java.util.Scanner;
import java.util.List;
import java.util.Map;

public class instructordashboard {

    private final config db;
    private final Scanner sc;
    private final int userId;

    public instructordashboard(config db, Scanner sc, int userId) {
        this.db = db;
        this.sc = sc;
        this.userId = userId;
    }

    public void runInstructorDashboard() {
        int choice;
        do {
            System.out.println("\n     --- INSTRUCTOR DASHBOARD ---");
            System.out.println(" -----------------------------------");
            System.out.println("|  1. VIEW EVALUATIONS RECEIVED     |");
            System.out.println("|  2. EXIT                          |");
            System.out.println(" -----------------------------------");
            System.out.print("  Enter your choice: ");

            choice = Main2.getIntInput(sc, 1, 2);

            switch (choice) {
                case 1: viewEvaluations(); break;
                case 2: System.out.println("  LOGGING OUT..."); break;
            }
        } while (choice != 2);
    }

    private int getInstructorId() {
        String sql = "SELECT i_id FROM tbl_instructor WHERE i_u_id = ?";
        List<Map<String, Object>> result = db.fetchRecords(sql, userId);
        if (!result.isEmpty()) {
            return (int) result.get(0).get("i_id");
        }
        return -1;
    }


    private void viewEvaluations() {
        System.out.println("\n                    --- EVALUATIONS RECEIVED ---");
        
        int instructorId = getInstructorId();
        if (instructorId == -1) {
            System.out.println("Error: Instructor record not found.");
            return;
        }

        
        String sql = "SELECT t1.e_id, t1.e_average_rating, t1.e_remarks, t1.e_year, t1.e_sem, t1.e_date, t3.s_schoolID, t3.s_name " +
                 "FROM tbl_evaluation t1 " +
                 "INNER JOIN tbl_instructor t2 ON t1.i_id = t2.i_id " +
                 "INNER JOIN tbl_student t3 ON t1.s_schoolID = t3.s_schoolID " +
                 "WHERE t1.i_id = ?";

        List<Map<String, Object>> records = db.fetchRecords(sql, instructorId);

        if (records.isEmpty()) {
            System.out.println("  No evaluation records found.");
            return;
        }
        
        String[] headers = {"Eval ID", "Avg Rating", "Remarks", "Year", "Sem", "Date"};
        String[] columns = {"e_id", "e_average_rating", "e_remarks", "e_year", "e_sem", "e_date"};


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
        
        db.viewRecords(records, headers, columns); 
        
        String totalAvgSql = "SELECT AVG(CAST(e_average_rating AS REAL)) AS overall_avg FROM tbl_evaluation WHERE i_id = ?";
        
        List<Map<String, Object>> avgResult = db.fetchRecords(totalAvgSql, instructorId);
        
        if (!avgResult.isEmpty()) {
            Object avgObj = avgResult.get(0).get("overall_avg");
            double overallAvg = 0.0;
            
            if (avgObj != null) {
                try {
                    if (avgObj instanceof Number) {
                        overallAvg = ((Number) avgObj).doubleValue(); 
                    } else {
                        
                        overallAvg = Double.parseDouble(avgObj.toString());
                    }
                } catch (NumberFormatException e) {
                     System.out.println("  Warning: Could not parse overall average rating.");
                }
            }

            
            //String f = "   ----------------------------------------------";
            
            System.out.println("--------------------------------------------");
            System.out.printf("| TOTAL OVERALL AVERAGE RATING: %.2f / 5.0 |\n", overallAvg);
            System.out.println("--------------------------------------------");

            
        }
    }
}