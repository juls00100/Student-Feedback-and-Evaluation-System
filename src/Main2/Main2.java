/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Main2;
import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Scanner;


class Evaluation {
    private int id;
    private int instructorId;
    private double averageRating;
    private String comments;

    public Evaluation(int id, int instructorId, double averageRating, String comments) {
        this.id = id;
        this.instructorId = instructorId;
        this.averageRating = averageRating;
        this.comments = comments;
    }

    public int getId() {
        return id;
    }

    public int getInstructorId() {
        return instructorId;
    }

    public double getAverageRating() {
        return averageRating;
    }

    public String getComments() {
        return comments;
    }

    public void setAverageRating(double averageRating) {
        this.averageRating = averageRating;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }
}

class Instructor {
    private int id;
    private String name;

    public Instructor(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

public class Main2 {

    private static List<Evaluation> evaluations = new ArrayList<>();
    private static List<Instructor> instructors = new ArrayList<>();
    private static int nextEvaluationId = 1;

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        // Pre-populate instructors for simplicity
        instructors.add(new Instructor(1, "Mrs. Cheng"));
        instructors.add(new Instructor(2, "Sir Dajay"));
        instructors.add(new Instructor(3, "Ms. Gamboa"));
        instructors.add(new Instructor(4, "Sir Fil"));

        System.out.println("🌟 Welcome to the Student Evaluation System! 🌟");

        int mainChoice;
        do {
            System.out.println("\n--- MAIN MENU ---");
            System.out.println("1. Submit a New Evaluation");
            System.out.println("2. Manage Evaluations (Instructor Only)");
            System.out.println("3. Exit");
            System.out.print("Choose an option: ");

            mainChoice = getIntInput(sc, 1, 3);

            switch (mainChoice) {
                case 1:
                    createEvaluation(sc);
                    break;
                case 2:
                    manageEvaluations(sc);
                    break;
                case 3:
                    System.out.println("Thank you for using the system. Goodbye!");
                    break;
            }
        } while (mainChoice != 3);

        sc.close();
    }

    // A helper method for safe integer input
    private static int getIntInput(Scanner sc, int min, int max) {
        while (true) {
            try {
                int input = sc.nextInt();
                sc.nextLine(); // consume newline
                if (input >= min && input <= max) {
                    return input;
                } else {
                    System.out.printf("Please enter a number between %d and %d: ", min, max);
                }
            } catch (InputMismatchException e) {
                System.out.print("Invalid input. Please enter a number: ");
                sc.nextLine(); // consume invalid input
            }
        }
    }

    // --- CRUD Operations ---

    // CREATE EVALUATION
    private static void createEvaluation(Scanner sc) {
        System.out.println("\n--- SUBMIT NEW EVALUATION ---");

        System.out.println("Select an instructor to evaluate:");
        for (Instructor i : instructors) {
            System.out.printf("%d. %s\n", i.getId(), i.getName());
        }
        System.out.print("Enter instructor ID: ");
        int instructorId = getIntInput(sc, 1, instructors.size());

        System.out.println("\n--- RATING SCALE ---");
        System.out.println("1 - Not Observed");
        System.out.println("2 - Much to be desired upon");
        System.out.println("3 - Observed to a minimal extent");
        System.out.println("4 - Observed");
        System.out.println("5 - Observed to a large extent");

        String[] questions = {
                "Is the instructor punctual?",
                "Are class activities meaningful?",
                "Does the instructor provide clear instructions?",
                "Is class time used efficiently?"
        };

        int totalRating = 0;
        for (int i = 0; i < questions.length; i++) {
            System.out.printf("\nQuestion %d: %s\n", (i + 1), questions[i]);
            System.out.print("Your rating (1-5): ");
            totalRating += getIntInput(sc, 1, 5);
        }

        double averageRating = (double) totalRating / questions.length;

        System.out.print("\nPlease provide any comments or recommendations: ");
        String comments = sc.nextLine();

        Evaluation newEvaluation = new Evaluation(nextEvaluationId++, instructorId, averageRating, comments);
        evaluations.add(newEvaluation);

        System.out.println("\n✅ Thank you! Your evaluation has been submitted.");
    }

    // VIEW/READ EVALUATIONS
    private static void viewAllEvaluations() {
        if (evaluations.isEmpty()) {
            System.out.println("😞 No evaluations have been submitted yet.");
            return;
        }

        System.out.println("\n--- ALL EVALUATIONS ---");
        System.out.printf("%-5s | %-15s | %-15s | %s\n", "ID", "Instructor", "Avg. Rating", "Comments");
        System.out.println("-----------------------------------------------------------------");

        for (Evaluation eval : evaluations) {
            String instructorName = "N/A";
            for (Instructor instructor : instructors) {
                if (instructor.getId() == eval.getInstructorId()) {
                    instructorName = instructor.getName();
                    break;
                }
            }
            System.out.printf("%-5d | %-15s | %-15.2f | %s\n", eval.getId(), instructorName, eval.getAverageRating(), eval.getComments());
        }
    }

    // MANAGE EVALUATIONS MENU (CRUD)
    private static void manageEvaluations(Scanner sc) {
        int choice;
        do {
            System.out.println("\n--- MANAGE EVALUATIONS ---");
            System.out.println("1. View All Evaluations");
            System.out.println("2. Edit an Evaluation");
            System.out.println("3. Delete Evaluations");
            System.out.println("4. Back to Main Menu");
            System.out.print("Enter your choice: ");
            choice = getIntInput(sc, 1, 4);

            switch (choice) {
                case 1:
                    viewAllEvaluations();
                    break;
                case 2:
                    editEvaluation(sc);
                    break;
                case 3:
                    deleteEvaluations(sc);
                    break;
                case 4:
                    System.out.println("Returning to main menu.");
                    break;
            }
        } while (choice != 4);
    }

    // EDIT EVALUATION
    private static void editEvaluation(Scanner sc) {
        viewAllEvaluations();
        if (evaluations.isEmpty()) {
            return;
        }

        System.out.print("Enter the ID of the evaluation to edit: ");
        int idToEdit = getIntInput(sc, 1, Integer.MAX_VALUE);

        Evaluation foundEval = null;
        for (Evaluation eval : evaluations) {
            if (eval.getId() == idToEdit) {
                foundEval = eval;
                break;
            }
        }

        if (foundEval == null) {
            System.out.println("❌ Error: Evaluation with ID " + idToEdit + " not found.");
            return;
        }

        System.out.println("\n--- EDITING EVALUATION ID " + foundEval.getId() + " ---");
        System.out.printf("Current Rating: %.2f\n", foundEval.getAverageRating());
        System.out.printf("Current Comments: %s\n", foundEval.getComments());

        System.out.print("Enter new average rating (or -1 to keep current): ");
        double newRating = sc.nextDouble();
        sc.nextLine(); // consume newline
        if (newRating != -1) {
            foundEval.setAverageRating(newRating);
        }

        System.out.print("Enter new comments (or leave blank to keep current): ");
        String newComments = sc.nextLine();
        if (!newComments.trim().isEmpty()) {
            foundEval.setComments(newComments);
        }

        System.out.println("✅ Evaluation updated successfully!");
    }

    // DELETE EVALUATIONS with looping
    private static void deleteEvaluations(Scanner sc) {
        viewAllEvaluations();
        if (evaluations.isEmpty()) {
            return;
        }

        System.out.print("How many evaluations do you want to delete? ");
        int numToDelete = getIntInput(sc, 1, evaluations.size());
        
        for (int i = 0; i < numToDelete; i++) {
            System.out.printf("Enter ID of evaluation to delete (item %d of %d): ", (i + 1), numToDelete);
            int idToDelete = getIntInput(sc, 1, Integer.MAX_VALUE);

            boolean removed = evaluations.removeIf(eval -> eval.getId() == idToDelete);
            if (removed) {
                System.out.println("🗑️ Evaluation with ID " + idToDelete + " deleted successfully.");
            } else {
                System.out.println("❌ Error: Evaluation with ID " + idToDelete + " not found. Skipping.");
            }
        }
    }
}