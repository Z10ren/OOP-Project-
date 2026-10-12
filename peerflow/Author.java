package peerflow;

import java.util.Arrays;

public class Author extends User {
    public static final int MAX_PAPERS = 10;

    // Instance variables
    private String authorName;
    private final Paper[] submittedPapers = new Paper[MAX_PAPERS];
    private int paperCount;

    // Constructor
    public Author(String userID, String userName, String password) {
        super(userID, userName, password, ROLE_AUTHOR);
        this.authorName = userName;
    }

    // Methods
    public Paper submitPaper(String paperTitle, String abstractText)
            throws InvalidSubmissionException, WorkflowException {
        checkContent(paperTitle, abstractText);
        if (paperCount >= MAX_PAPERS) {
            throw new WorkflowException(authorName + " already has the maximum of "
                    + MAX_PAPERS + " papers.");
        }

        Paper paper = new Paper(paperTitle.trim(), abstractText.trim(), this);
        submittedPapers[paperCount] = paper;
        paperCount++;

        System.out.println("  [OK] " + authorName + " submitted \"" + paper.getPaperTitle()
                + "\" as " + paper.getPaperID() + ".");
        new Notification("Your submission \"" + paper.getPaperTitle() + "\" was received (ID "
                + paper.getPaperID() + ").").notifyUser(this);
        return paper;
    }

    public void resubmitPaper(Paper paper, String newTitle, String newAbstractText)
            throws InvalidSubmissionException, UnauthorizedAccessException {
        if (paper.getAuthor() != this) {
            throw new UnauthorizedAccessException(authorName + " may not resubmit "
                    + paper.getPaperID() + ", which belongs to " + paper.getAuthorName() + ".");
        }
        if (!Paper.STATUS_REVISION_REQUIRED.equals(paper.getPaperStatus())) {
            throw new InvalidSubmissionException(
                "\"" + paper.getPaperTitle() + "\" is not awaiting revision "
                + "(current status: " + paper.getPaperStatus() + ").");
        }
        checkContent(newTitle, newAbstractText);

        paper.updateContent(newTitle.trim(), newAbstractText.trim());
        paper.incrementRevisionCount();
        paper.reopenReviews();
        paper.setPaperStatus(Paper.STATUS_SUBMITTED);
        paper.setFinalDecision(Paper.DECISION_PENDING);

        System.out.println("  [OK] " + authorName + " resubmitted \"" + paper.getPaperTitle()
                + "\" (revision " + paper.getRevisionCount() + ").");
        for (Review review : paper.getReviews()) {
            new Notification(paper.getPaperID() + " was revised (revision "
                    + paper.getRevisionCount() + "). Please review round "
                    + review.getRoundNumber() + ".").notifyUser(review.getReviewer());
        }
    }

    public Paper findOwnPaper(String paperID) throws InvalidInputException {
        for (int i = 0; i < paperCount; i++) {
            if (submittedPapers[i].getPaperID().equalsIgnoreCase(paperID.trim())) {
                return submittedPapers[i];
            }
        }
        throw new InvalidInputException(authorName + " has no paper with ID \""
                + paperID.trim() + "\".");
    }

    private void checkContent(String paperTitle, String abstractText)
            throws InvalidSubmissionException {
        if (paperTitle == null || paperTitle.trim().isEmpty()) {
            throw new InvalidSubmissionException("the paper title is required.");
        }
        if (abstractText == null || abstractText.trim().isEmpty()) {
            throw new InvalidSubmissionException(
                "the abstract is required for \"" + paperTitle.trim() + "\".");
        }
        if (paperTitle.trim().length() > 150) {
            throw new InvalidSubmissionException("the title must be at most 150 characters.");
        }
        if (abstractText.trim().length() > 500) {
            throw new InvalidSubmissionException("the abstract must be at most 500 characters.");
        }
    }

    @Override
    public void viewDashboard() {
        System.out.println("  === AUTHOR DASHBOARD: " + userName + " ===");
        if (paperCount == 0) {
            System.out.println("  No submissions yet.");
            return;
        }
        for (int i = 0; i < paperCount; i++) {
            Paper p = submittedPapers[i];
            System.out.println("   - " + p.getPaperID() + " \"" + p.getPaperTitle() + "\"  ["
                    + p.getPaperStatus() + "]  revisions: " + p.getRevisionCount()
                    + "  reviews submitted: " + p.countSubmittedReviews() + "/" + p.getReviewCount());
        }
    }

    @Override
    public boolean canViewReviewerIdentity() {
        return false;
    }

    @Override
    public boolean canOpenPaper(Paper paper) {
        return paper.getAuthor() == this;
    }

    // Accessors
    public String getAuthorName() {
        return authorName;
    }

    public Paper[] getSubmittedPapers() {
        return Arrays.copyOf(submittedPapers, paperCount);
    }
}
