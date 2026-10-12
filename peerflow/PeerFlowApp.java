package peerflow;

import java.time.LocalDate;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class PeerFlowApp {
    private final PeerFlowSystem system;
    private final ConsoleInput input;

    public PeerFlowApp(PeerFlowSystem system, ConsoleInput input) {
        this.system = system;
        this.input = input;
    }

    public static void main(String[] args) {
        PeerFlowApp app = new PeerFlowApp(PeerFlowSystem.createWithDemoAccounts(),
                new ConsoleInput(new Scanner(System.in)));
        try {
            app.run();
        } catch (NoSuchElementException e) {
            System.out.println();
            System.out.println("  [INFO] Input ended. Goodbye.");
        }
    }

    public void run() {
        System.out.println("======================================================================");
        System.out.println("  PEERFLOW: ACADEMIC REVIEW MANAGEMENT SYSTEM");
        System.out.println("  Scores 1.0-5.0 on " + Review.CRITERIA.length + " criteria | accept >= "
                + Administrator.ACCEPT_THRESHOLD + " | revise >= " + Administrator.REVISION_THRESHOLD
                + " | reject below");
        System.out.println("======================================================================");
        while (true) {
            System.out.println();
            System.out.println("===== MAIN MENU =====");
            System.out.println("1. Login");
            System.out.println("2. Show demo accounts");
            System.out.println("0. Exit");
            int choice = input.readInt("Choose: ", 0, 2);
            if (choice == 0) {
                System.out.println("  Goodbye from PeerFlow.");
                return;
            }
            if (choice == 2) {
                showDemoAccounts();
                continue;
            }
            User user = attemptLogin();
            if (user != null) {
                openMenuFor(user);
                user.logout();
                System.out.println("  Logged out " + user.getUserID() + ".");
            }
        }
    }

    private User attemptLogin() {
        String userID = input.readLine("User ID : ");
        String password = input.readLine("Password: ");
        try {
            User user = system.login(userID, password);
            System.out.println("  [OK] Welcome, " + user.getUserName() + " (" + user.getUserRole() + ").");
            return user;
        } catch (UnauthorizedAccessException e) {
            System.out.println("  [LOGIN FAILED] " + e.getMessage());
            return null;
        }
    }

    private void openMenuFor(User user) {
        if (user instanceof Author) {
            authorMenu((Author) user);
        } else if (user instanceof Reviewer) {
            reviewerMenu((Reviewer) user);
        } else if (user instanceof Administrator) {
            administratorMenu((Administrator) user);
        }
    }

    // Author menu
    private void authorMenu(Author author) {
        while (true) {
            System.out.println();
            System.out.println("----- AUTHOR MENU (" + author.getUserID() + ") -----");
            System.out.println("1. View dashboard");
            System.out.println("2. Submit a new paper");
            System.out.println("3. Resubmit a revised paper");
            System.out.println("4. View paper details and reviewer feedback");
            System.out.println("5. Track a submission");
            System.out.println("6. View my notifications");
            System.out.println("0. Logout");
            int choice = input.readInt("Choose: ", 0, 6);
            if (choice == 0) {
                return;
            }
            try {
                switch (choice) {
                    case 1:
                        author.viewDashboard();
                        break;
                    case 2:
                        String title = input.readLine("Title   : ");
                        String abstractText = input.readLine("Abstract: ");
                        system.submitPaper(author, title, abstractText);
                        break;
                    case 3:
                        Paper paper = system.findPaper(input.readLine("Paper ID: "));
                        String newTitle = input.readLine("Revised title   : ");
                        String newAbstract = input.readLine("Revised abstract: ");
                        author.resubmitPaper(paper, newTitle, newAbstract);
                        break;
                    case 4:
                        system.findPaper(input.readLine("Paper ID: ")).viewPaperDetails(author);
                        break;
                    case 5:
                        author.findOwnPaper(input.readLine("Paper ID: ")).trackSubmission();
                        break;
                    default:
                        author.viewNotifications();
                        break;
                }
            } catch (PeerFlowException e) {
                printError(e);
            }
        }
    }

    // Reviewer menu
    private void reviewerMenu(Reviewer reviewer) {
        while (true) {
            System.out.println();
            System.out.println("----- REVIEWER MENU (" + reviewer.getUserID() + ") -----");
            System.out.println("1. View dashboard");
            System.out.println("2. Declare a conflict of interest with a paper");
            System.out.println("3. Start a review");
            System.out.println("4. Evaluate a paper (4 scores, recommendation, comments)");
            System.out.println("5. Submit a review");
            System.out.println("6. View paper details and reports");
            System.out.println("7. View my notifications");
            System.out.println("0. Logout");
            int choice = input.readInt("Choose: ", 0, 7);
            if (choice == 0) {
                return;
            }
            try {
                switch (choice) {
                    case 1:
                        reviewer.viewDashboard();
                        break;
                    case 2:
                        reviewer.declareConflict(system.findPaper(input.readLine("Paper ID: ")));
                        break;
                    case 3:
                        Review toStart = chooseReview(reviewer);
                        if (toStart != null) {
                            reviewer.startReview(toStart);
                        }
                        break;
                    case 4:
                        Review toEvaluate = chooseReview(reviewer);
                        if (toEvaluate != null) {
                            evaluate(reviewer, toEvaluate);
                        }
                        break;
                    case 5:
                        Review toSubmit = chooseReview(reviewer);
                        if (toSubmit != null) {
                            reviewer.submitReview(toSubmit);
                        }
                        break;
                    case 6:
                        system.findPaper(input.readLine("Paper ID: ")).viewPaperDetails(reviewer);
                        break;
                    default:
                        reviewer.viewNotifications();
                        break;
                }
            } catch (PeerFlowException e) {
                printError(e);
            }
        }
    }

    private Review chooseReview(Reviewer reviewer) {
        Review[] reviews = reviewer.getAssignedReviews();
        if (reviews.length == 0) {
            System.out.println("  No papers are assigned to you.");
            return null;
        }
        reviewer.viewDashboard();
        return reviews[input.readInt("Select assignment number: ", 1, reviews.length) - 1];
    }

    private void evaluate(Reviewer reviewer, Review review) throws PeerFlowException {
        if (!review.isStarted() || review.isSubmitted()) {
            throw new WorkflowException("only a review that is IN PROGRESS can be evaluated (this one is "
                    + review.getStatusLabel() + ").");
        }
        double[] scores = new double[Review.CRITERIA.length];
        for (int i = 0; i < scores.length; i++) {
            while (true) {
                double value = input.readDouble("  " + Review.CRITERIA[i] + " (1.0-5.0): ");
                try {
                    Review.validateScore(Review.CRITERIA[i], value);
                    scores[i] = value;
                    break;
                } catch (InvalidInputException e) {
                    printError(e);
                }
            }
        }
        while (true) {
            try {
                reviewer.evaluatePaper(review, scores,
                        input.readLine("  Recommendation (ACCEPT/REVISE/REJECT): "));
                break;
            } catch (InvalidInputException e) {
                printError(e);
            }
        }
        String comments;
        while (true) {
            comments = input.readLine("  Comments for the author: ");
            try {
                Review.validateComments(comments);
                break;
            } catch (InvalidInputException e) {
                printError(e);
            }
        }
        String confidential = input.readLine("  Confidential note for the editor (optional): ");
        reviewer.addReviewComments(review, comments, confidential);
    }

    // Administrator menu
    private void administratorMenu(Administrator admin) {
        while (true) {
            System.out.println();
            System.out.println("----- ADMINISTRATOR MENU (" + admin.getUserID() + ") -----");
            System.out.println("1. View my dashboard");
            System.out.println("2. View all submissions");
            System.out.println("3. Assign a reviewer");
            System.out.println("4. Make the decision on a paper");
            System.out.println("5. View paper details and full reports");
            System.out.println("6. List all accounts (User[] array)");
            System.out.println("7. Show every account's dashboard (polymorphism)");
            System.out.println("8. View my notifications");
            System.out.println("0. Logout");
            int choice = input.readInt("Choose: ", 0, 8);
            if (choice == 0) {
                return;
            }
            try {
                switch (choice) {
                    case 1:
                        admin.viewDashboard();
                        break;
                    case 2:
                        admin.viewDashboard(system.getPapers());
                        break;
                    case 3:
                        Paper paper = system.findPaper(input.readLine("Paper ID   : "));
                        Reviewer reviewer = system.findReviewer(input.readLine("Reviewer ID: "));
                        LocalDate deadline = input.readDate("Deadline (YYYY-MM-DD): ");
                        admin.assignReviewer(paper, reviewer, deadline);
                        break;
                    case 4:
                        admin.makeFinalDecision(system.findPaper(input.readLine("Paper ID: ")));
                        break;
                    case 5:
                        system.findPaper(input.readLine("Paper ID: ")).viewPaperDetails(admin);
                        break;
                    case 6:
                        listAccounts();
                        break;
                    case 7:
                        for (User account : system.getUsers()) {
                            account.viewDashboard();
                            System.out.println();
                        }
                        break;
                    default:
                        admin.viewNotifications();
                        break;
                }
            } catch (PeerFlowException e) {
                printError(e);
            }
        }
    }

    private void listAccounts() {
        User[] users = system.getUsers();
        System.out.println("  User[] holds " + users.length + " objects:");
        for (int i = 0; i < users.length; i++) {
            System.out.println("   users[" + i + "]  " + users[i] + "  (" + users[i].getClass().getSimpleName() + ")");
        }
    }

    // Shared helpers
    private void showDemoAccounts() {
        System.out.println("  Author        A-001 / authorPass123   (Jan Lorenz Alfonso)");
        System.out.println("  Author        A-002 / pass000         (Unrelated Author)");
        System.out.println("  Reviewer      R-001 / reviewPass123   (Gil Kelly Delgado)");
        System.out.println("  Reviewer      R-002 / reviewPass456   (Sean Paulo Salta)");
        System.out.println("  Administrator AD-01 / adminPass123    (Rhys Sean Gozum)");
    }

    private void printError(PeerFlowException e) {
        System.out.println("  [" + e.getClass().getSimpleName() + "] " + e.getMessage());
    }
}
