package peerflow;

import java.time.LocalDate;

public class PeerFlowDemo {
    public static void main(String[] args) {
        banner("PEERFLOW: ACADEMIC REVIEW MANAGEMENT SYSTEM (scripted walkthrough)");

        step("1. ACCOUNTS (stored in a User[] array) AND LOGIN");
        PeerFlowSystem system = PeerFlowSystem.createWithDemoAccounts();
        Author author = null;
        Author otherAuthor = null;
        Reviewer reviewer1 = null;
        Reviewer reviewer2 = null;
        Administrator admin = null;
        try {
            author = (Author) system.login("A-001", "authorPass123");
            otherAuthor = (Author) system.login("A-002", "pass000");
            reviewer1 = (Reviewer) system.login("R-001", "reviewPass123");
            reviewer2 = (Reviewer) system.login("R-002", "reviewPass456");
            admin = (Administrator) system.login("AD-01", "adminPass123");
        } catch (UnauthorizedAccessException e) {
            System.out.println("  [EXCEPTION] " + e.getMessage());
            return;
        }
        User[] accounts = system.getUsers();
        for (int i = 0; i < accounts.length; i++) {
            System.out.println("  users[" + i + "] " + accounts[i]);
        }
        System.out.println("\n  Inherited login() with a wrong password:");
        try {
            system.login("R-001", "wrongPassword");
        } catch (UnauthorizedAccessException e) {
            System.out.println("  [EXCEPTION CAUGHT] " + e.getMessage());
        }

        step("2. SUBMITTING PAPERS");
        Paper paper = null;
        try {
            paper = system.submitPaper(author,
                "Predicting Student Dropout Using Ensemble Learning",
                "This study compares three ensemble methods for early "
                + "identification of at-risk undergraduate students.");
        } catch (PeerFlowException e) {
            System.out.println("  [EXCEPTION] " + e.getMessage());
            return;
        }
        System.out.println("\n  Attempting a submission with no abstract:");
        try {
            system.submitPaper(author, "An Incomplete Paper", "   ");
        } catch (PeerFlowException e) {
            System.out.println("  [EXCEPTION CAUGHT] " + e.getMessage());
        }

        step("3. ASSIGNING REVIEWERS");
        LocalDate deadline = LocalDate.now().plusDays(14);
        Review review1 = null;
        Review review2 = null;

        System.out.println("  A deadline in the past is refused:");
        try {
            admin.assignReviewer(paper, reviewer1, LocalDate.now().minusDays(1));
        } catch (PeerFlowException e) {
            System.out.println("  [EXCEPTION CAUGHT] " + e.getMessage());
        }

        System.out.println();
        try {
            review1 = admin.assignReviewer(paper, reviewer1, deadline);
            review2 = admin.assignReviewer(paper, reviewer2, deadline);
        } catch (PeerFlowException e) {
            System.out.println("  [EXCEPTION] " + e.getMessage());
            return;
        }

        System.out.println("\n  A third reviewer declares a conflict, then is assigned:");
        Reviewer reviewer3 = new Reviewer("R-003", "Conflicted Reviewer", "pass789", "School of Computing");
        try {
            reviewer3.declareConflict(paper);
            admin.assignReviewer(paper, reviewer3, deadline);
        } catch (PeerFlowException e) {
            System.out.println("  [EXCEPTION CAUGHT] " + e.getMessage());
        }

        step("4. ROUND 1: REVIEWING THE PAPER");
        System.out.println("  Scoring before starting the review is refused:");
        try {
            reviewer1.evaluatePaper(review1, new double[] {4, 4, 3, 3}, "REVISE");
        } catch (PeerFlowException e) {
            System.out.println("  [EXCEPTION CAUGHT] " + e.getMessage());
        }

        System.out.println("\n  An out-of-range score is refused and nothing is stored:");
        try {
            reviewer1.startReview(review1);
            reviewer1.evaluatePaper(review1, new double[] {9.9, 4, 3, 3}, "REVISE");
        } catch (PeerFlowException e) {
            System.out.println("  [EXCEPTION CAUGHT] " + e.getMessage());
        }
        System.out.println("  Review still unscored: " + !review1.isScored());

        System.out.println("\n  Another reviewer tries to score Delgado's review:");
        try {
            reviewer2.evaluatePaper(review1, new double[] {1, 1, 1, 1}, "REJECT");
        } catch (PeerFlowException e) {
            System.out.println("  [EXCEPTION CAUGHT] " + e.getMessage());
        }

        System.out.println();
        try {
            reviewer1.evaluatePaper(review1, new double[] {4.0, 4.0, 3.0, 3.0}, "revise");
            reviewer1.addReviewComments(review1,
                "The ensemble comparison is well motivated, but the dropout "
                + "definition needs to be stated explicitly in Section 3.",
                "Solid contribution. Recommend acceptance after minor revision.");
            reviewer1.submitReview(review1);

            reviewer2.startReview(review2);
            reviewer2.evaluatePaper(review2, new double[] {3.0, 4.0, 3.0, 3.0}, "REVISE");
            reviewer2.addReviewComments(review2,
                "The sample size is not justified and no confidence intervals "
                + "are reported for the accuracy figures.",
                "Methodology is the weak point. Revision required.");
            reviewer2.submitReview(review2);
        } catch (PeerFlowException e) {
            System.out.println("  [EXCEPTION] " + e.getMessage());
        }
        System.out.println("\n  Average score across submitted reviews: "
                + String.format("%.2f", paper.calculateAverageScore()) + " / 5.0");

        step("5. A LATE REVIEW IS REFUSED");
        Review lateReview = new Review(paper, reviewer1, LocalDate.now().minusDays(5));
        System.out.println("  Deadline on this review was " + lateReview.getReviewDeadline()
                + "; today is " + LocalDate.now() + ".");
        try {
            reviewer1.startReview(lateReview);
        } catch (PeerFlowException e) {
            System.out.println("  [EXCEPTION CAUGHT] " + e.getMessage());
        }

        step("6. ROLE-RESTRICTED ACCESS");
        System.out.println("  The author views their own paper (reviewers are anonymous):");
        try {
            paper.viewPaperDetails(author);
        } catch (UnauthorizedAccessException e) {
            System.out.println("  [EXCEPTION] " + e.getMessage());
        }

        System.out.println("\n  A different author tries to open it:");
        try {
            paper.viewPaperDetails(otherAuthor);
        } catch (UnauthorizedAccessException e) {
            System.out.println("  [EXCEPTION CAUGHT] " + e.getMessage());
        }

        System.out.println("\n  The author requests the full review report:");
        try {
            System.out.println(review1.generateReviewReport(author));
        } catch (UnauthorizedAccessException e) {
            System.out.println("  [EXCEPTION CAUGHT] " + e.getMessage());
        }

        System.out.println("\n  The administrator requests the same report:");
        try {
            System.out.println(review1.generateReviewReport(admin));
        } catch (UnauthorizedAccessException e) {
            System.out.println("  [EXCEPTION] " + e.getMessage());
        }

        step("7. DECISION ROUND 1 AND RESUBMISSION");
        try {
            admin.makeFinalDecision(paper);
            System.out.println("\n  Deciding again before the author revises is refused:");
            admin.makeFinalDecision(paper);
        } catch (WorkflowException e) {
            System.out.println("  [EXCEPTION CAUGHT] " + e.getMessage());
        }

        System.out.println();
        try {
            author.resubmitPaper(paper,
                "Predicting Student Dropout Using Ensemble Learning (Revised)",
                "This study compares three ensemble methods for early "
                + "identification of at-risk undergraduate students. The "
                + "revised version states the dropout definition explicitly "
                + "and reports confidence intervals.");
        } catch (PeerFlowException e) {
            System.out.println("  [EXCEPTION] " + e.getMessage());
        }
        paper.trackSubmission();

        step("8. ROUND 2 AND FINAL DECISION");
        try {
            reviewer1.startReview(review1);
            reviewer1.evaluatePaper(review1, new double[] {4.5, 4.0, 4.0, 4.0}, "ACCEPT");
            reviewer1.addReviewComments(review1, "The dropout definition is now explicit.", "");
            reviewer1.submitReview(review1);

            reviewer2.startReview(review2);
            reviewer2.evaluatePaper(review2, new double[] {4.0, 4.5, 4.0, 4.0}, "ACCEPT");
            reviewer2.addReviewComments(review2, "Confidence intervals are now reported.", "");
            reviewer2.submitReview(review2);

            admin.makeFinalDecision(paper);
        } catch (PeerFlowException e) {
            System.out.println("  [EXCEPTION] " + e.getMessage());
        }

        System.out.println("\n  Attempting a decision on an unreviewed paper:");
        try {
            Paper orphan = system.submitPaper(author, "Unreviewed Draft", "No reviewers yet.");
            admin.makeFinalDecision(orphan);
        } catch (PeerFlowException e) {
            System.out.println("  [EXCEPTION CAUGHT] " + e.getMessage());
        }

        step("9. POLYMORPHISM: ONE CALL, DIFFERENT BEHAVIOURS");
        for (User account : system.getUsers()) {
            account.viewDashboard();
            System.out.println();
        }
        System.out.println("  The administrator's overloaded viewDashboard(Paper[]):");
        admin.viewDashboard(system.getPapers());

        step("10. POLYMORPHIC NOTIFICATION");
        Notification closing = new Notification("The review cycle for this venue is now closed.");
        for (User account : system.getUsers()) {
            closing.notifyUser(account);
        }
        System.out.println();
        System.out.println("  Notifications stored in the author's inbox array: " + author.getInboxCount());

        banner("END OF DEMONSTRATION");
    }

    // Output helpers
    private static final String LINE =
        "======================================================================";
    private static final String DASHES =
        "----------------------------------------------------------------------";

    private static void banner(String title) {
        System.out.println("\n" + LINE);
        System.out.println("  " + title);
        System.out.println(LINE);
    }

    private static void step(String title) {
        System.out.println("\n" + DASHES);
        System.out.println("  " + title);
        System.out.println(DASHES);
    }
}
