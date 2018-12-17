package sk.tuke.smart.makac.model;

public class User {
    private int id;
    /**
     * foreign key to User model
     */
    private int user;
    /**
     * weight - weight in kg
     */
    private float weight;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUser() {
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