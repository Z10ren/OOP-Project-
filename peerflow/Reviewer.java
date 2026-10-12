package peerflow;

import java.util.Arrays;

public class Reviewer extends StaffMember {
    public static final int MAX_ASSIGNMENTS = 20;
    public static final int MAX_CONFLICTS = 20;

    // Instance variables
    private String reviewerName;
    private final Review[] assignedReviews = new Review[MAX_ASSIGNMENTS];
    private int assignmentCount;
    private final Paper[] conflictedPapers = new Paper[MAX_CONFLICTS];
    private int conflictCount;

    // Constructor
    public Reviewer(String userID, String userName, String password, String department) {
        super(userID, userName, password, ROLE_REVIEWER, department);
        this.reviewerName = userName;
    }

    // Methods
    public void declareConflict(Paper paper) throws WorkflowException {
        if (paper.hasReviewer(this)) {
            throw new WorkflowException(reviewerName + " is already assigned to "
                    + paper.getPaperID() + "; ask the administrator to reassign it.");
        }
        if (hasConflictWith(paper)) {
            throw new WorkflowException("a conflict with " + paper.getPaperID()
                    + " was already declared.");
        }
        if (conflictCount >= MAX_CONFLICTS) {
            throw new WorkflowException("the conflict list is full.");
        }
        conflictedPapers[conflictCount] = paper;
        conflictCount++;
        System.out.println("  [!] " + reviewerName + " declared a conflict of interest with "
                + paper.getPaperID() + ".");
    }

    public boolean hasConflictWith(Paper paper) {
        for (int i = 0; i < conflictCount; i++) {
            if (conflictedPapers[i] == paper) {
                return true;
            }
        }
        return false;
    }

    public void startReview(Review review)
            throws UnauthorizedAccessException, WorkflowException, MissedReviewDeadlineException {
        checkOwnership(review);
        if (review.isSubmitted()) {
            throw new WorkflowException("this review was already submitted.");
        }
        if (review.isStarted()) {
            throw new WorkflowException("this review is already in progress.");
        }
        checkDeadline(review, "start this review");
        review.markStarted();
        review.getPaper().setPaperStatus(Paper.STATUS_UNDER_REVIEW);
        System.out.println("  [OK] " + reviewerName + " started reviewing \""
                + review.getPaper().getPaperTitle() + "\" (round " + review.getRoundNumber() + ").");
    }

    public void evaluatePaper(Review review, double[] criterionScores, String recommendation)
            throws UnauthorizedAccessException, WorkflowException,
                   MissedReviewDeadlineException, InvalidInputException {
        checkEditable(review, "score this paper");
        if (criterionScores == null || criterionScores.length != Review.CRITERIA.length) {
            throw new InvalidInputException("exactly " + Review.CRITERIA.length
                    + " criterion scores are required.");
        }
        for (int i = 0; i < Review.CRITERIA.length; i++) {
            Review.validateScore(Review.CRITERIA[i], criterionScores[i]);
        }
        String normalized = Review.validateRecommendation(recommendation);

        review.setCriterionScores(criterionScores);
        review.setRecommendation(normalized);
        System.out.println("  [OK] " + reviewerName + " scored " + review.getPaper().getPaperID()
                + ": overall " + String.format("%.2f", review.getReviewScore())
                + "/5.0 (" + normalized + ").");
    }

    public void addReviewComments(Review review, String reviewComments, String confidentialComments)
            throws UnauthorizedAccessException, WorkflowException,
                   MissedReviewDeadlineException, InvalidInputException {
        checkEditable(review, "add comments");
        String checked = Review.validateComments(reviewComments);
        review.setReviewComments(checked);
        review.setConfidentialComments(confidentialComments == null ? "" : confidentialComments.trim());
        System.out.println("  [OK] " + reviewerName + " added comments.");
    }

    public void submitReview(Review review)
            throws UnauthorizedAccessException, WorkflowException, MissedReviewDeadlineException {
        checkEditable(review, "submit this review");
        if (!review.isScored()) {
            throw new WorkflowException("score all four criteria before submitting.");
        }
        if (review.getReviewComments().isEmpty()) {
            throw new WorkflowException("add review comments before submitting.");
        }
        review.markSubmitted();
        System.out.println("  [OK] " + reviewerName + " submitted the review of "
                + review.getPaper().getPaperID() + ".");
        new Notification("Your review of " + review.getPaper().getPaperID()
                + " has been recorded. Thank you.").notifyUser(this);
    }

    private void checkEditable(Review review, String action)
            throws UnauthorizedAccessException, WorkflowException, MissedReviewDeadlineException {
        checkOwnership(review);
        if (review.isSubmitted()) {
            throw new WorkflowException("this review was already submitted and can no longer be changed.");
        }
        if (!review.isStarted()) {
            throw new WorkflowException("start the review before you " + action + ".");
        }
        checkDeadline(review, action);
    }

    private void checkOwnership(Review review) throws UnauthorizedAccessException {
        if (review.getReviewer() != this) {
            throw new UnauthorizedAccessException(reviewerName + " has not been assigned this review of \""
                    + review.getPaper().getPaperTitle() + "\".");
        }
    }

    private void checkDeadline(Review review, String action) throws MissedReviewDeadlineException {
        if (review.isOverdue()) {
            throw new MissedReviewDeadlineException(reviewerName + " cannot " + action
                    + ": the deadline was " + review.getReviewDeadline() + ".");
        }
    }

    @Override
    public void viewDashboard() {
        System.out.println("  === REVIEWER DASHBOARD: " + userName + " (" + getDepartment() + ") ===");
        System.out.println("  Conflicts declared: " + conflictCount);
        if (assignmentCount == 0) {
            System.out.println("  No papers assigned.");
            return;
        }
        for (int i = 0; i < assignmentCount; i++) {
            Review r = assignedReviews[i];
            String score = r.isScored() ? String.format("%.2f", r.getReviewScore()) : "-";
            System.out.println("   [" + (i + 1) + "] " + r.getPaper().getPaperID() + " \""
                    + r.getPaper().getPaperTitle() + "\"  round " + r.getRoundNumber()
                    + "  due " + r.getReviewDeadline() + "  " + r.getStatusLabel()
                    + "  your score: " + score);
        }
    }

    @Override
    public boolean canOpenPaper(Paper paper) {
        return paper.hasReviewer(this);
    }

    // Package-private
    void assignReview(Review review) throws WorkflowException {
        if (!hasCapacity()) {
            throw new WorkflowException(reviewerName + " already has " + MAX_ASSIGNMENTS
                    + " assignments.");
        }
        assignedReviews[assignmentCount] = review;
        assignmentCount++;
    }

    // Accessors
    public boolean hasCapacity() {
        return assignmentCount < MAX_ASSIGNMENTS;
    }

    public String getReviewerName() {
        return reviewerName;
    }

    public Review[] getAssignedReviews() {
        return Arrays.copyOf(assignedReviews, assignmentCount);
    }
}
