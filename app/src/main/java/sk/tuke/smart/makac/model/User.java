package sk.tuke.smart.makac.model;

public class User {
    private long id;
    /**
     * integer, numeric identifier of account type (G+, Facebook, etc.)
     */
    private int accType;
    /**
     * 3rd party service account id (eg. G+ identifier)
     */
    private String accId;
    /**
     * auth token for accessing 3rd party service data about user
     */
    private String authToken;

    public User() {

    }

    public User(int accType, String accId) {
        this.accType = accType;
        this.accId = accId;
    }

    public long getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getAccType() {
        return accType;
    }

    public void setAccType(int accType) {
        this.accType = accType;
    }

    public String getAccId() {
        return accId;
    }

    public void setAccId(String accId) {
        this.accId = accId;
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }
}