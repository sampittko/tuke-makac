package sk.tuke.smart.makac.model;

import android.location.Location;

import java.util.Date;

public class GpsPoint {
    private long id;
    /**
     * Foreign key to Workout model
     */
    private Workout workout;

    /**
     * sessionNumber - number of workout session (new session is
     * created after pause/start click). Lower number is earlier session.
     */
    private long sessionNumber;
    private double latitude;
    private double longitude;
    private long duration;
    private float speed;
    private double pace;
    private double totalCalories;
    private Date created;
    private Date lastUpdate;

    public GpsPoint() {

    }

    public GpsPoint(Workout workout, long sessionNumber, Location location, long duration, float speed, double pace, double totalCalories) {
        this.workout = workout;
        this.sessionNumber = sessionNumber;
        this.latitude = location.getLatitude();
        this.longitude = location.getLongitude();
        this.duration = duration;
        this.speed = speed;
        this.pace = pace;
        this.totalCalories = totalCalories;
    }

    public long getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Workout getWorkout() {
        return workout;
    }

    public void setWorkout(Workout workout) {
        this.workout = workout;
    }

    public long getSessionNumber() {
        return sessionNumber;
    }

    public void setSessionNumber(int sessionNumber) {
        this.sessionNumber = sessionNumber;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public float getSpeed() {
        return speed;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public double getPace() {
        return pace;
    }

    public void setPace(double pace) {
        this.pace = pace;
    }

    public double getTotalCalories() {
        return totalCalories;
    }

    public void setTotalCalories(double totalCalories) {
        this.totalCalories = totalCalories;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public Date getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(Date lastUpdate) {
        this.lastUpdate = lastUpdate;
    }
}
