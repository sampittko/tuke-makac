package sk.tuke.smart.makac.helpers;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public final class SportActivities {
    private static HashMap<Integer, Double> runningMets = new HashMap<>();
    private static HashMap<Integer, Double> walkingMets = new HashMap<>();
    private static HashMap<Integer, Double> cyclingMets = new HashMap<>();

    private static final double RUNNING_MET_AVG = 1.535353535;
    private static final double WALKING_MET_AVG = 1.14;
    private static final double CYCLING_MET_AVG = 0.744444444;

    public static final int RUNNING = 0;
    public static final int WALKING = 1;
    public static final int CYCLING = 2;

    public static final int UNIT_KILOMETERS = 0;
    public static final int UNIT_MILES = 1;

    private static final String TAG = "SportActivities";

    public SportActivities() {
        runningMets.put(4, 6.0);
        runningMets.put(5, 8.3);
        runningMets.put(6, 9.8);
        runningMets.put(7, 11.0);
        runningMets.put(8, 11.8);
        runningMets.put(9, 12.8);
        runningMets.put(10, 14.5);
        runningMets.put(11, 16.0);
        runningMets.put(12, 19.0);
        runningMets.put(13, 19.8);
        runningMets.put(14, 23.0);

        walkingMets.put(1, 2.0);
        walkingMets.put(2, 2.8);
        walkingMets.put(3, 3.1);
        walkingMets.put(4, 3.5);

        cyclingMets.put(10,6.8);
        cyclingMets.put(12,8.0);
        cyclingMets.put(14,10.0);
        cyclingMets.put(16,12.8);
        cyclingMets.put(18,13.6);
        cyclingMets.put(20,15.8);
    }

    /**
     * Returns MET value for an activity.
     * @param activityType - sport activity type (0 - running, 1 - walking, 2 - cycling)
     * @param speed - speed in m/s
     * @return
     */
    public static double getMET(int activityType, Float speed) {
        try {
            int convertedSpeed = (int)Math.ceil(MainHelper.mpsToMiph(speed));
            switch (activityType) {
                case RUNNING:
                    return runningMets.get(convertedSpeed);
                case WALKING:
                    return walkingMets.get(convertedSpeed);
                case CYCLING:
                    return cyclingMets.get(convertedSpeed);
            }
        }
        catch (NullPointerException e) {
            double convertedSpeed = MainHelper.mpsToMiph(speed);
            switch (activityType) {
                case RUNNING:
                    return convertedSpeed * RUNNING_MET_AVG;
                case WALKING:
                    return convertedSpeed * WALKING_MET_AVG;
                case CYCLING:
                    return convertedSpeed * CYCLING_MET_AVG;
            }
        }

        return 0;
    }

    /**
     * Returns final calories computed from the data provided (returns value in kcal)
     * @param sportActivity - sport activity type (0 - running, 1 - walking, 2 - cycling)
     * @param weight - weight in kg
     * @param speedList - list of all speed values recorded (unit = m/s)
     * @param timeFillingSpeedListInHours - time of collecting speed list (duration of sport activity from first to last speedPoint in speedList)
     * @return
     */
    public static double countCalories(int sportActivity, float weight, List<Float> speedList, double timeFillingSpeedListInHours) {
        float averageSpeed = getAverageSpeed(speedList);
        Log.i(TAG, "Average speed: " + averageSpeed);

        double MET = getMET(sportActivity, averageSpeed);
        Log.i(TAG, "MET: " + MET);

        Log.i(TAG, "Weight: " + weight + "kg");

        Log.i(TAG, "Time: " + timeFillingSpeedListInHours + "h");

        double calories = MET * weight * timeFillingSpeedListInHours;
        Log.i(TAG, "Calories calculated. (" + calories + "kcal)");

        return calories;
    }

    private static float getAverageSpeed(List<Float> speedList) {
        float speedsSum = 0;
        for (float speed : speedList)
            speedsSum += speed;
        return speedsSum / speedList.size();
    }

    public static double getAveragePace(ArrayList<Double> paceList) {
        double paceCount = 0;
        for (double pace : paceList)
            paceCount += pace;
        return paceCount / paceList.size();
    }

    public static String getSportActivityStringFromInt(int n) {
        switch (n) {
            case 0: return "Running";
            case 1: return "Walking";
            case 2: return "Cycling";
            default: return "Unknown sport";
        }
    }
}
