package peerflow;

import java.time.LocalDate;
import java.util.Arrays;

public class Administrator extends StaffMember {

    // Decision rules
    public static final double ACCEPT_THRESHOLD = 4.0;
    public static final double REVISION_THRESHOLD = 3.0;
    public static final int MIN_SUBMITTED_REVIEWS = 2;
    public static final int MAX_HANDLED_PAPERS = 50;

    // Instance variables
    private final Paper[] handledPapers = new Paper[MAX_HANDLED_PAPERS];
    private int handledCount;
    private String lastDecision = "None";

    // Constructor
    public Administrator(String userID, String userName, String password, String department) {
        super(userID, userName, password, ROLE_ADMINISTRATOR, department);
    }

    // Methods
    public Review assignReviewer(Paper paper, Reviewer reviewer, LocalDate reviewDeadline)
            throws InvalidInputException, UnauthorizedAccessException, WorkflowException {
        if (reviewDeadline == null || !reviewDeadline.isAfter(LocalDate.now())) {
            throw new InvalidInputException("the review deadline must be after today ("
                    + LocalDate.now() + "); received " + reviewDeadline + ".");
        }
        if (paper.isClosed() || Paper.STATUS_REVISION_REQUIRED.equals(paper.getPaperStatus())) {
            throw new WorkflowException("reviewers cannot be assigned to " + paper.getPaperID()
                    + " while it is " + paper.getPaperStatus() + ".");
        }
        if (reviewer.hasConflictWith(paper)) {
            throw new UnauthorizedAccessException(
                reviewer.getReviewerName() + " has declared a conflict of interest "
                + "and cannot be assigned \"" + paper.getPaperTitle() + "\".");
        }
        if (reviewer.getUserName().equalsIgnoreCase(paper.getAuthorName())) {
            throw new UnauthorizedAccessException("a reviewer may not be assigned to their own paper.");
        }
        if (paper.hasReviewer(reviewer)) {
            throw new WorkflowException(reviewer.getReviewerName() + " is already assigned to "
                    + paper.getPaperID() + ".");
        }
        if (!paper.hasReviewCapacity()) {
            throw new WorkflowException(paper.getPaperID() + " already has the maximum of "
                    + Paper.MAX_REVIEWS + " reviewers.");
        }
        if (!reviewer.hasCapacity()) {
            throw new WorkflowException(reviewer.getReviewerName() + " cannot take more assignments.");
        }

        Review review = new Review(paper, reviewer, reviewDeadline);
        paper.addReview(review);
        reviewer.assignReview(review);
        rememberPaper(paper);

        System.out.println("  [OK] " + userName + " assigned " + reviewer.getReviewerName()
                + " to \"" + paper.getPaperTitle() + "\" (due " + reviewDeadline + ").");
        new Notification("You have been assigned to review " + paper.getPaperID() + " \""
                + paper.getPaperTitle() + "\". Deadline: " + reviewDeadline + ".").notifyUser(reviewer);
        return review;
    }

    public String makeFinalDecision(Paper paper) throws WorkflowException {
        if (paper.isClosed()) {
            throw new WorkflowException(paper.getPaperID() + " already has a final decision ("
                    + paper.getFinalDecision() + ").");
        }
        if (Paper.STATUS_REVISION_REQUIRED.equals(paper.getPaperStatus())) {
            throw new WorkflowException(paper.getPaperID() + " is waiting for the author's revision.");
        }
        int submitted = paper.countSubmittedReviews();
        if (submitted < MIN_SUBMITTED_REVIEWS) {
            throw new WorkflowException("at least " + MIN_SUBMITTED_REVIEWS
                    + " submitted reviews are required; " + paper.getPaperID() + " has " + submitted + ".");
        }
        if (submitted < paper.getReviewCount()) {
            throw new WorkflowException("wait for all assigned reviews (" + submitted + " of "
                    + paper.getReviewCount() + " submitted).");
        }

        double average = paper.calculateAverageScore();
        String averageText = String.format("%.2f", average);
        String decision;
        if (average >= ACCEPT_THRESHOLD) {
            decision = Paper.DECISION_ACCEPTED;
            paper.setPaperStatus(Paper.STATUS_ACCEPTED);
            paper.setFinalDecision(decision);
            new Notification("Decision on \"" + paper.getPaperTitle() + "\": ACCEPTED (average "
                    + averageText + "/5.0).").notifyUser(paper.getAuthor());
        } else if (average >= REVISION_THRESHOLD) {
            decision = Paper.DECISION_REVISION_REQUIRED;
            requestRevision(paper, averageText);
        } else {
            decision = Paper.DECISION_REJECTED;
            paper.setPaperStatus(Paper.STATUS_REJECTED);
            paper.setFinalDecision(decision);
            new Notification("Decision on \"" + paper.getPaperTitle() + "\": REJECTED (average "
                    + averageText + "/5.0).").notifyUser(paper.getAuthor());
        }
        for (Review review : paper.getReviews()) {
            new Notification("Editorial outcome for " + paper.getPaperID() + ": " + decision + ".")
                    .notifyUser(review.getReviewer());
        }
        lastDecision = paper.getPaperID() + " -> " + decision;
        System.out.println("  [OK] " + userName + " recorded the decision on " + paper.getPaperID()
                + ": " + decision + " (average " + averageText + "/5.0).");
        return decision;
    }

    private void requestRevision(Paper paper, String averageText) {
        paper.setPaperStatus(Paper.STATUS_REVISION_REQUIRED);
        paper.setFinalDecision(Paper.DECISION_REVISION_REQUIRED);
        new Notification("Revision required for \"" + paper.getPaperTitle() + "\" (average "
                + averageText + "/5.0). Please address the reviewer comments and resubmit.")
                .notifyUser(paper.getAuthor());
    }

    private void rememberPaper(Paper paper) {
        for (int i = 0; i < handledCount; i++) {
            if (handledPapers[i] == paper) {
                return;
            }
        }
        if (handledCount < MAX_HANDLED_PAPERS) {
            handledPapers[handledCount] = paper;
            handledCount++;
        }
    }

    @Override
    public void viewDashboard() {
        System.out.println("  === ADMINISTRATOR DASHBOARD: " + userName + " (" + getDepartment() + ") ===");
        System.out.println("  Rules: accept >= " + ACCEPT_THRESHOLD + ", revise >= " + REVISION_THRESHOLD
                + ", reject below; at least " + MIN_SUBMITTED_REVIEWS + " submitted reviews.");
        System.out.println("  Last decision: " + lastDecision);
        printPapers(Arrays.copyOf(handledPapers, handledCount));
    }

    public void viewDashboard(Paper[] allPapers) {
        System.out.println("  === ALL SUBMISSIONS (" + allPapers.length + ") ===");
        printPapers(allPapers);
    }

    private void printPapers(Paper[] papers) {
        if (papers.length == 0) {
            System.out.println("  No papers.");
        }
        for (Paper p : papers) {
            System.out.println("   - " + p.getPaperID() + " \"" + p.getPaperTitle() + "\"  ["
                    + p.getPaperStatus() + "]  reviews " + p.countSubmittedReviews() + "/"
                    + p.getReviewCount() + "  avg " + String.format("%.2f", p.calculateAverageScore())
                    + "  decision: " + p.getFinalDecision());
        }
    }

    @Override
    public boolean canOpenPaper(Paper paper) {
        return true;
    }

    public String getLastDecision() {
        return lastDecision;
    }
}
