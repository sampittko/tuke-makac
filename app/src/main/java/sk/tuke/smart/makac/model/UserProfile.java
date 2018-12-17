package sk.tuke.smart.makac.model;

import com.j256.ormlite.field.DatabaseField;

public class UserProfile {
    @DatabaseField(generatedId = true, unique = true)
    private long id;
    /**
     * foreign key to User model
     */
    @DatabaseField(foreign = true)
    private long user;
    /**
     * weight - weight in kg
     */
    @DatabaseField
    private float weight;

    public UserProfile() {

    }

    public long getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public long getUser() {
        return user;
    }

    public void setUser(int user) {
        this.user = user;
    }

    public float getWeight() {
        return weight;
    }

    public void setWeight(float weight) {
        this.weight = weight;
    }
}
