package sk.tuke.smart.makac.helpers;

import java.util.List;

public final class SportActivities {
    /**
     * Returns MET value for an activity.
     * @param activityType - sport activity type (0 - running, 1 - walking, 2 - cycling)
     * @param speed - speed in m/s
     * @return
     */
    public static double getMET(int activityType, Float speed) {

        // TODO getMET()

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

        // TODO countCalories()

        return 0;
    }
}
