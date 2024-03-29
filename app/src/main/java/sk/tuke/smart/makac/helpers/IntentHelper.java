package sk.tuke.smart.makac.helpers;

public final class IntentHelper {
    public static final String ACTION_START = "sk.tuke.smart.makac.COMMAND_START";
    public static final String ACTION_PAUSE = "sk.tuke.smart.makac.COMMAND_PAUSE";
    public static final String ACTION_CONTINUE = "sk.tuke.smart.makac.COMMAND_CONTINUE";
    public static final String ACTION_STOP = "sk.tuke.smart.makac.COMMAND_STOP";
    public static final String ACTION_TICK = "sk.tuke.smart.makac.TICK";
    public static final String ACTION_GPS = "sk.tuke.smart.makac.COMMAND_GPS";

    public static final String DATA_WORKOUT_SPORT_ACTIVITY = "sportActivity";
    public static final String DATA_WORKOUT_DURATION = "duration";
    public static final String DATA_WORKOUT_DISTANCE = "distance";
    public static final String DATA_WORKOUT_PACE = "pace";
    public static final String DATA_WORKOUT_CALORIES = "calories";
    public static final String DATA_WORKOUT_LOCATION = "location";
    public static final String DATA_WORKOUT_ID = "workoutId";
    public static final String DATA_WORKOUT_TITLE = "workoutTitle";
    public static final String DATA_SERVICE_STATE = "state";
    public static final String DATA_HISTORY_REQUEST = "historyRequest";

    public static final int STATE_STOPPED = 1;
    public static final int STATE_RUNNING = 2;
    public static final int STATE_PAUSED = 3;
    public static final int STATE_CONTINUE = 4;
}
