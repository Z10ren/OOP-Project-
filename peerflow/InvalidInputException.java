package peerflow;

public class InvalidInputException extends PeerFlowException {
    private static final long serialVersionUID = 1L;

    public InvalidInputException(String message) {
        super("Invalid input: " + message);
    }
}
