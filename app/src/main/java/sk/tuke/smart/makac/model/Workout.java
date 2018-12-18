package sk.tuke.smart.makac.model;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.Date;

@DatabaseTable
public class Workout {
    // initial status of workout
    public static final int statusUnknown = 0;
    // ended workout
    public static final int statusEnded = 1;
    // paused workout
    public static final int statusPaused = 2;
    // deleted workout
    public static final int statusDeleted= 3;

    @DatabaseField(generatedId = true, unique = true)
    private long id;
    /**
     * Foreign key to User model
     */
    @DatabaseField(foreign = true)
    private User user;
    @DatabaseField
    private String title;
    @DatabaseField
    private Date created;
    /**
     * 0 - unknown (active workout)
     * 1 - ended
     * 2 - paused
     * 3 - deleted
     */
    @DatabaseField
    private int status;
    @DatabaseField
    private double distance;
    @DatabaseField
    private long duration;
    @DatabaseField
    private double totalCalories;
    @DatabaseField
    private double paceAvg;
    @DatabaseField
    private int sportActivity;
    @DatabaseField
    private Date lastUpdate;

    public Workout() {

    }

    public Workout(String title, int sportActivity) {
        this.title = title;
        this.sportActivity = sportActivity;
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public double getTotalCalories() {
        return totalCalories;
    }

    public void setTotalCalories(double totalCalories) {
        this.totalCalories = totalCalories;
    }

    public double getPaceAvg() {
        return paceAvg;
    }

    public void setPaceAvg(double paceAvg) {
        this.paceAvg = paceAvg;
    }

    public int getSportActivity() {
        return sportActivity;
    }

    public void setSportActivity(int sportActivity) {
        this.sportActivity = sportActivity;
    }

    public Date getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(Date lastUpdate) {
        this.lastUpdate = lastUpdate;
    }
}
