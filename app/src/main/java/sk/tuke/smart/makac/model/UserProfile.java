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
    @DatabaseField
    private float height;
    @DatabaseField
    private int age;

    public UserProfile() {

    }

    public UserProfile(float weight, int age, float height) {
        this.weight = weight;
        this.age = age;
        this.height = height;
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

    public float getHeight() {
        return height;
    }

    public void setHeight(float height) {
        this.height = height;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }
}
