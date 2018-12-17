package sk.tuke.smart.makac.model;

public class UserProfile {
    private int id;
    /**
     * integer, numeric identifier of account type (G+, Facebook, etc.)
     */
    private int accType;
    /**
     * 3rd party service account id (eg. G+ identifier)
     */
    private int accId;
    /**
     * auth token for accessing 3rd party service data about user
     */
    private String authToken;

    public int getId() {
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

    public int getAccId() {
        return accId;
    }

    public void setAccId(int accId) {
        this.accId = accId;
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }
}
