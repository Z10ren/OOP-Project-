package peerflow;

public class WorkflowException extends PeerFlowException {
    private static final long serialVersionUID = 1L;

    public WorkflowException(String message) {
        super("Workflow rule: " + message);
    }
}
