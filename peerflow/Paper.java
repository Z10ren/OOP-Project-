package peerflow;

import java.time.LocalDate;
import java.util.Arrays;

public class Paper {

    // Status and decision constants
    public static final String STATUS_SUBMITTED = "SUBMITTED";
    public static final String STATUS_UNDER_REVIEW = "UNDER REVIEW";
    public static final String STATUS_REVISION_REQUIRED = "REVISION REQUIRED";
    public static final String STATUS_ACCEPTED = "ACCEPTED";
    public static final String STATUS_REJECTED = "REJECTED";
    public static final String DECISION_PENDING = "PENDING";
    public static final String DECISION_ACCEPTED = "ACCEPTED";
    public static final String DECISION_REJECTED = "REJECTED";
    public static final String DECISION_REVISION_REQUIRED = "REVISION REQUIRED";
    public static final int MAX_REVIEWS = 3;
    private static int nextPaperNumber = 1;

    // Instance variables
    private String paperID;
    private String paperTitle;
    private String abstractText;
    private Author author;
    private LocalDate submissionDate;
    private String paperStatus;
    private String finalDecision;
    private int revisionCount;
    private final Review[] reviews = new Review[MAX_REVIEWS];
    private int reviewCount;

    // Constructor
    Paper(String paperTitle, String abstractText, Author author) {
        this.paperID = String.format("PF-%03d", nextPaperNumber);
        nextPaperNumber++;
        this.paperTitle = paperTitle;
        this.abstractText = abstractText;
        this.author = author;
        this.submissionDate = LocalDate.now();
        this.paperStatus = STATUS_SUBMITTED;
        this.finalDecision = DECISION_PENDING;
        this.revisionCount = 0;
    }

    // Public methods
    public void viewPaperDetails(User requester) throws UnauthorizedAccessException {
        if (!requester.canOpenPaper(this)) {
            throw new UnauthorizedAccessException(requester.getUserName() + " ("
                    + requester.getUserRole() + ") may not open " + paperID + ".");
        }

        System.out.println("  ---- Paper details ----");
        System.out.println("  Paper ID     : " + paperID);
        System.out.println("  Title        : " + paperTitle);
        System.out.println("  Author       : " + author.getAuthorName());
        System.out.println("  Abstract     : " + abstractText);
        System.out.println("  Submitted    : " + submissionDate);
        System.out.println("  Status       : " + paperStatus);
        System.out.println("  Revisions    : " + revisionCount);
        System.out.println("  Reviews      : " + countSubmittedReviews() + " of " + reviewCount + " submitted");
        System.out.println("  Average      : " + String.format("%.2f", calculateAverageScore()) + " / 5.0");
        System.out.println("  Decision     : " + finalDecision);

        for (int i = 0; i < reviewCount; i++) {
            Review r = reviews[i];
            if (r.canBeReadInFullBy(requester)) {
                System.out.println(r.generateReviewReport(requester));
            } else {
                System.out.println(r.generateAuthorFeedback(i + 1, requester.canViewReviewerIdentity()));
            }
        }
    }

    public void trackSubmission() {
        System.out.println("  Tracking " + paperID + " \"" + paperTitle + "\" -> " + paperStatus
                + " (revision " + revisionCount + ", reviews " + countSubmittedReviews() + "/"
                + reviewCount + ", decision: " + finalDecision + ")");
    }

    public double calculateAverageScore() {
        double total = 0.0;
        int counted = 0;
        for (int i = 0; i < reviewCount; i++) {
            if (reviews[i].isSubmitted()) {
                total += reviews[i].getReviewScore();
                counted++;
            }
        }
        return counted == 0 ? 0.0 : total / counted;
    }

    public int countSubmittedReviews() {
        int counted = 0;
        for (int i = 0; i < reviewCount; i++) {
            if (reviews[i].isSubmitted()) {
                counted++;
            }
        }
        return counted;
    }

    public boolean hasReviewer(Reviewer reviewer) {
        for (int i = 0; i < reviewCount; i++) {
            if (reviews[i].getReviewer() == reviewer) {
                return true;
            }
        }
        return false;
    }

    public boolean hasReviewCapacity() {
        return reviewCount < MAX_REVIEWS;
    }

    public boolean isClosed() {
        return STATUS_ACCEPTED.equals(paperStatus) || STATUS_REJECTED.equals(paperStatus);
    }

    // Package-private mutators
    void addReview(Review review) throws WorkflowException {
        if (!hasReviewCapacity()) {
            throw new WorkflowException(paperID + " already has " + MAX_REVIEWS + " reviewers.");
        }
        reviews[reviewCount] = review;
        reviewCount++;
    }

    void reopenReviews() {
        for (int i = 0; i < reviewCount; i++) {
            reviews[i].reopenForNextRound();
        }
    }

    void setPaperStatus(String paperStatus) {
        this.paperStatus = paperStatus;
    }

    void setFinalDecision(String finalDecision) {
        this.finalDecision = finalDecision;
    }

    void incrementRevisionCount() {
        this.revisionCount++;
    }

    void updateContent(String paperTitle, String abstractText) {
        this.paperTitle = paperTitle;
        this.abstractText = abstractText;
        this.submissionDate = LocalDate.now();
    }

    // Accessors
    public String getPaperID() {
        return paperID;
    }

    public String getPaperTitle() {
        return paperTitle;
    }

    public String getAbstractText() {
        return abstractText;
    }

    public Author getAuthor() {
        return author;
    }

    public String getAuthorName() {
        return author.getAuthorName();
    }

    public LocalDate getSubmissionDate() {
        return submissionDate;
    }

    public String getPaperStatus() {
        return paperStatus;
    }

    public String getFinalDecision() {
        return finalDecision;
    }

    public int getRevisionCount() {
        return revisionCount;
    }

    public int getReviewCount() {
        return reviewCount;
    }

    public Review[] getReviews() {
        return Arrays.copyOf(reviews, reviewCount);
    }
}
