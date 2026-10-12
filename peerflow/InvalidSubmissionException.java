package peerflow;

public class InvalidSubmissionException extends PeerFlowException {
    private static final long serialVersionUID = 1L;

    public InvalidSubmissionException(String message) {
        super("Invalid submission: " + message);
    }
}
