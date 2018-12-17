package sk.tuke.smart.makac.model.config;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import java.sql.SQLException;

import sk.tuke.smart.makac.R;
import sk.tuke.smart.makac.model.GpsPoint;
import sk.tuke.smart.makac.model.Workout;
import sk.tuke.smart.makac.model.User;
import sk.tuke.smart.makac.model.UserProfile;

public class DatabaseHelper extends OrmLiteSqliteOpenHelper{

    private static final String DATABASE_NAME = "tracker";
    private static final int DATABASE_VERSION = 1;

    /**
     * The data access object used to interact with the Sqlite database to do C.R.U.D operations.
     */
    private Dao<Workout, Long> workoutsDao;
    private Dao<GpsPoint, Long> gpsPointsDao;
    private Dao<User, Long> userDao;
    private Dao<UserProfile, Long> userProfileDao;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION,
                /**
                 * R.raw.ormlite_config is a reference to the ormlite_config.txt file in the
                 * /res/raw/ directory of this project
                 * */
                R.raw.ormlite_config);
    }

    @Override
    public void onCreate(SQLiteDatabase database, ConnectionSource connectionSource) {
        try {

            /**
             * creates the Todo database table
             */
            TableUtils.createTable(connectionSource, Workout.class);
            TableUtils.createTable(connectionSource, GpsPoint.class);
            TableUtils.createTable(connectionSource, User.class);
            TableUtils.createTable(connectionSource, UserProfile.class);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, ConnectionSource connectionSource,
                          int oldVersion, int newVersion) {
        try {
            /**
             * Recreates the database when onUpgrade is called by the framework
             */
            TableUtils.dropTable(connectionSource, Workout.class, true);
            TableUtils.dropTable(connectionSource, GpsPoint.class, true);
            TableUtils.dropTable(connectionSource, User.class, true);
            TableUtils.dropTable(connectionSource, UserProfile.class, true);
            onCreate(database, connectionSource);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns an instance of the data access object
     * @return
     * @throws SQLException
     */
    public Dao<Workout, Long> workoutDao() throws SQLException {
        if(workoutsDao == null) {
            workoutsDao = getDao(Workout.class);
        }
        return workoutsDao;
    }

    /**
     * Returns an instance of the data access object
     * @return
     * @throws SQLException
     */
    public Dao<GpsPoint, Long> gpsPointDao() throws SQLException {
        if(gpsPointsDao == null) {
            gpsPointsDao = getDao(GpsPoint.class);
        }
        return gpsPointsDao;
    }

    /**
     * Returns an instance of the data access object
     * @return
     * @throws SQLException
     */
    public Dao<User, Long> userDao() throws SQLException {
        if(userDao == null) {
            userDao = getDao(User.class);
        }
        return userDao;
    }

    /**
     * Returns an instance of the data access object
     * @return
     * @throws SQLException
     */
    public Dao<UserProfile, Long> userProfileDao() throws SQLException {
        if(userProfileDao == null) {
            userProfileDao = getDao(UserProfile.class);
        }
        return userProfileDao;
    }
}