package peerflow;

import java.time.LocalDate;

public class Review {

    // Constants
    public static final String RECOMMEND_ACCEPT = "ACCEPT";
    public static final String RECOMMEND_REVISE = "REVISE";
    public static final String RECOMMEND_REJECT = "REJECT";
    public static final String[] CRITERIA = {"Originality", "Relevance", "Clarity", "Methodology"};
    public static final double MIN_SCORE = 1.0;
    public static final double MAX_SCORE = 5.0;

    // Instance variables
    private Paper paper;
    private Reviewer reviewer;
    private LocalDate reviewDeadline;
    private final double[] criterionScores = new double[CRITERIA.length];
    private double reviewScore;
    private String reviewComments;
    private String recommendation;
    private String confidentialComments;
    private boolean started;
    private boolean scored;
    private boolean submitted;
    private int roundNumber;

    // Constructor
    Review(Paper paper, Reviewer reviewer, LocalDate reviewDeadline) {
        this.paper = paper;
        this.reviewer = reviewer;
        this.reviewDeadline = reviewDeadline;
        this.roundNumber = 1;
        clear();
    }

    // Validation
    public static void validateScore(String criterion, double score) throws InvalidInputException {
        if (Double.isNaN(score) || score < MIN_SCORE || score > MAX_SCORE) {
            throw new InvalidInputException(criterion + " score must be between " + MIN_SCORE
                    + " and " + MAX_SCORE + " (received " + score + ").");
        }
    }

    public static String validateComments(String comments) throws InvalidInputException {
        if (comments == null || comments.trim().isEmpty()) {
            throw new InvalidInputException("review comments cannot be empty.");
        }
        if (comments.trim().length() > 500) {
            throw new InvalidInputException("review comments must be at most 500 characters.");
        }
        return comments.trim();
    }

    public static String validateRecommendation(String recommendation) throws InvalidInputException {
        String value = recommendation == null ? "" : recommendation.trim().toUpperCase();
        if (value.equals(RECOMMEND_ACCEPT) || value.equals(RECOMMEND_REVISE)
                || value.equals(RECOMMEND_REJECT)) {
            return value;
        }
        throw new InvalidInputException("recommendation must be ACCEPT, REVISE, or REJECT (received \""
                + (recommendation == null ? "" : recommendation.trim()) + "\").");
    }

    // Reports
    public boolean canBeReadInFullBy(User requester) {
        return User.ROLE_ADMINISTRATOR.equals(requester.getUserRole()) || requester == reviewer;
    }

    public String generateReviewReport(User requester) throws UnauthorizedAccessException {
        if (!canBeReadInFullBy(requester)) {
            throw new UnauthorizedAccessException(requester.getUserName() + " ("
                    + requester.getUserRole() + ") may not read the full review report for "
                    + paper.getPaperID() + ".");
        }
        StringBuilder report = new StringBuilder();
        report.append("    == Review by ").append(reviewer.getReviewerName())
              .append(" (round ").append(roundNumber).append(", due ").append(reviewDeadline)
              .append(") -- ").append(getStatusLabel()).append(" ==\n");
        appendScores(report);
        report.append("      Confidential  : ").append(confidentialComments.isEmpty() ? "-" : confidentialComments)
              .append("\n");
        report.append("      Final decision: ").append(paper.getFinalDecision());
        return report.toString();
    }

    public String generateAuthorFeedback(int reviewerNumber, boolean showIdentity) {
        StringBuilder report = new StringBuilder();
        report.append("    == ").append(showIdentity ? reviewer.getReviewerName()
                : "Reviewer " + reviewerNumber + " [identity hidden]")
              .append(" (round ").append(roundNumber).append(") -- ").append(getStatusLabel()).append(" ==\n");
        if (submitted) {
            appendScores(report);
        } else {
            report.append("      (shown once the review is submitted)\n");
        }
        return report.toString().replaceAll("\n$", "");
    }

    private void appendScores(StringBuilder report) {
        for (int i = 0; i < CRITERIA.length; i++) {
            report.append(String.format("      %-14s: %s%n", CRITERIA[i],
                    scored ? String.format("%.1f / 5.0", criterionScores[i]) : "-"));
        }
        report.append("      Overall       : ").append(scored ? String.format("%.2f / 5.0", reviewScore) : "-")
              .append("\n");
        report.append("      Recommendation: ").append(recommendation.isEmpty() ? "-" : recommendation).append("\n");
        report.append("      Comments      : ").append(reviewComments.isEmpty() ? "-" : reviewComments).append("\n");
    }

    // Package-private mutators
    void markStarted() {
        this.started = true;
    }

    void setCriterionScores(double[] scores) {
        double total = 0.0;
        for (int i = 0; i < CRITERIA.length; i++) {
            criterionScores[i] = scores[i];
            total += scores[i];
        }
        this.reviewScore = total / CRITERIA.length;
        this.scored = true;
    }

    void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }

    void setReviewComments(String reviewComments) {
        this.reviewComments = reviewComments;
    }

    void setConfidentialComments(String confidentialComments) {
        this.confidentialComments = confidentialComments;
    }

    void markSubmitted() {
        this.submitted = true;
    }

    void reopenForNextRound() {
        roundNumber++;
        clear();
    }

    private void clear() {
        for (int i = 0; i < criterionScores.length; i++) {
            criterionScores[i] = 0.0;
        }
        reviewScore = 0.0;
        reviewComments = "";
        recommendation = "";
        confidentialComments = "";
        started = false;
        scored = false;
        submitted = false;
    }

    // Accessors
    public Paper getPaper() {
        return paper;
    }

    public Reviewer getReviewer() {
        return reviewer;
    }

    public String getReviewerName() {
        return reviewer.getReviewerName();
    }

    public LocalDate getReviewDeadline() {
        return reviewDeadline;
    }

    public double getReviewScore() {
        return reviewScore;
    }

    public String getReviewComments() {
        return reviewComments;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public int getRoundNumber() {
        return roundNumber;
    }

    public boolean isStarted() {
        return started;
    }

    public boolean isScored() {
        return scored;
    }

    public boolean isSubmitted() {
        return submitted;
    }

    public String getStatusLabel() {
        if (submitted) {
            return "SUBMITTED";
        }
        return started ? "IN PROGRESS" : "ASSIGNED";
    }

    public boolean isOverdue() {
        return LocalDate.now().isAfter(reviewDeadline);
    }
}
