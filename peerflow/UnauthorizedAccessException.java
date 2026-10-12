package peerflow;

public class UnauthorizedAccessException extends PeerFlowException {
    private static final long serialVersionUID = 1L;

    public UnauthorizedAccessException(String message) {
        super("Unauthorized access: " + message);
    }
}
