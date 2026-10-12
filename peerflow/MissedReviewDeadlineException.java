package peerflow;

public class MissedReviewDeadlineException extends PeerFlowException {
    private static final long serialVersionUID = 1L;

    public MissedReviewDeadlineException(String message) {
        super("Review deadline missed: " + message);
    }
}
