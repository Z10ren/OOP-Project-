package peerflow;

public abstract class StaffMember extends User {

    // Instance variables
    private String department;

    // Constructor
    protected StaffMember(String userID, String userName, String password,
                          String userRole, String department) {
        super(userID, userName, password, userRole);
        this.department = department;
    }

    // Methods
    @Override
    public boolean canViewReviewerIdentity() {
        return true;
    }

    public String getDepartment() {
        return department;
    }
}
