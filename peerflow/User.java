package peerflow;

public abstract class User {

    // Role constants
    public static final String ROLE_AUTHOR = "AUTHOR";
    public static final String ROLE_REVIEWER = "REVIEWER";
    public static final String ROLE_ADMINISTRATOR = "ADMINISTRATOR";
    public static final int INBOX_CAPACITY = 50;

    // Instance variables
    protected String userID;
    protected String userName;
    private String passwordHash;
    protected String userRole;
    private boolean sessionActive;
    private final Notification[] inbox = new Notification[INBOX_CAPACITY];
    private int inboxCount;

    // Constructor
    protected User(String userID, String userName, String password, String userRole) {
        this.userID = userID;
        this.userName = userName;
        this.passwordHash = hashPassword(password);
        this.userRole = userRole;
        this.sessionActive = false;
    }

    // Methods
    public boolean login(String userID, String password) {
        boolean match = this.userID.equals(userID)
                     && this.passwordHash.equals(hashPassword(password));
        if (match) {
            this.sessionActive = true;
        }
        return match;
    }

    public void logout() {
        this.sessionActive = false;
    }

    public abstract void viewDashboard();

    public abstract boolean canViewReviewerIdentity();

    public abstract boolean canOpenPaper(Paper paper);

    void receive(Notification notification) {
        if (inboxCount == INBOX_CAPACITY) {
            for (int i = 1; i < INBOX_CAPACITY; i++) {
                inbox[i - 1] = inbox[i];
            }
            inboxCount--;
        }
        inbox[inboxCount] = notification;
        inboxCount++;
    }

    public void viewNotifications() {
        System.out.println("  === NOTIFICATIONS FOR " + userName + " (" + inboxCount + ") ===");
        if (inboxCount == 0) {
            System.out.println("  No notifications.");
        }
        for (int i = 0; i < inboxCount; i++) {
            System.out.println("  " + (i + 1) + ". " + inbox[i]);
        }
    }

    // Accessors
    public String getUserID() {
        return userID;
    }

    public String getUserName() {
        return userName;
    }

    public String getUserRole() {
        return userRole;
    }

    public boolean isSessionActive() {
        return sessionActive;
    }

    public int getInboxCount() {
        return inboxCount;
    }

    @Override
    public String toString() {
        return String.format("%-6s %-22s %s", userID, userName, userRole);
    }

    // Private helper
    private String hashPassword(String plainText) {
        if (plainText == null) {
            return "";
        }
        return Integer.toHexString(plainText.hashCode());
    }
}
