package peerflow;

import java.util.Arrays;

public class PeerFlowSystem {
    public static final int MAX_USERS = 20;
    public static final int MAX_PAPERS = 50;

    private final User[] users = new User[MAX_USERS];
    private int userCount;
    private final Paper[] papers = new Paper[MAX_PAPERS];
    private int paperCount;

    public static PeerFlowSystem createWithDemoAccounts() {
        PeerFlowSystem system = new PeerFlowSystem();
        try {
            system.addUser(new Author("A-001", "Jan Lorenz Alfonso", "authorPass123"));
            system.addUser(new Author("A-002", "Unrelated Author", "pass000"));
            system.addUser(new Reviewer("R-001", "Gil Kelly Delgado", "reviewPass123", "School of Computing"));
            system.addUser(new Reviewer("R-002", "Sean Paulo Salta", "reviewPass456", "School of Computing"));
            system.addUser(new Administrator("AD-01", "Rhys Sean Gozum", "adminPass123", "Editorial Office"));
        } catch (WorkflowException e) {
            System.out.println("  [!] Could not load demo accounts: " + e.getMessage());
        }
        return system;
    }

    public void addUser(User user) throws WorkflowException {
        if (userCount >= MAX_USERS) {
            throw new WorkflowException("the account list is full (" + MAX_USERS + ").");
        }
        for (int i = 0; i < userCount; i++) {
            if (users[i].getUserID().equalsIgnoreCase(user.getUserID())) {
                throw new WorkflowException("user ID " + user.getUserID() + " is already taken.");
            }
        }
        users[userCount] = user;
        userCount++;
    }

    public User login(String userID, String password) throws UnauthorizedAccessException {
        for (int i = 0; i < userCount; i++) {
            if (users[i].login(userID.trim(), password)) {
                return users[i];
            }
        }
        throw new UnauthorizedAccessException("incorrect user ID or password.");
    }

    public Paper submitPaper(Author author, String title, String abstractText)
            throws InvalidSubmissionException, WorkflowException {
        if (paperCount >= MAX_PAPERS) {
            throw new WorkflowException("the system already holds " + MAX_PAPERS + " papers.");
        }
        Paper paper = author.submitPaper(title, abstractText);
        papers[paperCount] = paper;
        paperCount++;
        return paper;
    }

    public Paper findPaper(String paperID) throws InvalidInputException {
        String id = paperID == null ? "" : paperID.trim();
        for (int i = 0; i < paperCount; i++) {
            if (papers[i].getPaperID().equalsIgnoreCase(id)) {
                return papers[i];
            }
        }
        throw new InvalidInputException("no paper has ID \"" + id + "\".");
    }

    public Reviewer findReviewer(String userID) throws InvalidInputException {
        String id = userID == null ? "" : userID.trim();
        for (int i = 0; i < userCount; i++) {
            if (users[i] instanceof Reviewer && users[i].getUserID().equalsIgnoreCase(id)) {
                return (Reviewer) users[i];
            }
        }
        throw new InvalidInputException("no reviewer has ID \"" + id + "\".");
    }

    public User[] getUsers() {
        return Arrays.copyOf(users, userCount);
    }

    public Paper[] getPapers() {
        return Arrays.copyOf(papers, paperCount);
    }
}
