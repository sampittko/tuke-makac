package sk.tuke.smart.makac.model;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable
public class UserProfile {
    @DatabaseField(generatedId = true, unique = true)
    private long id;
    /**
     * foreign key to User model
     */
    @DatabaseField(foreign = true)
    private User user;
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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public float getWeight() {
        return weight;
    }

    public void setWeight(float weight) {
        this.weight = weight;
    }
}
