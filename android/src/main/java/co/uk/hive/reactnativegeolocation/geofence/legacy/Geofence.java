package co.uk.hive.reactnativegeolocation.geofence.legacy;

@SuppressWarnings("WeakerAccess")
public class Geofence {
    private final String mId;
    private final int mRadius;
    private final double mLatitude;
    private final double mLongitude;
    private final boolean mNotifyOnEnter;
    private final boolean mNotifyOnExit;
    private final boolean mNotifyOnDwell;
    private final int mLoiteringDelay;

    public Geofence(String id, int radius, double latitude, double longitude, boolean notifyOnEnter,
            boolean notifyOnExit, boolean notifyOnDwell, int loiteringDelay) {
        mId = id;
        mRadius = radius;
        mLatitude = latitude;
        mLongitude = longitude;
        mNotifyOnEnter = notifyOnEnter;
        mNotifyOnExit = notifyOnExit;
        mNotifyOnDwell = notifyOnDwell;
        mLoiteringDelay = loiteringDelay;
    }

    public String getId() {
        return mId;
    }

    public int getRadius() {
        return mRadius;
    }

    public double getLatitude() {
        return mLatitude;
    }

    public double getLongitude() {
        return mLongitude;
    }

    public boolean isNotifyOnEnter() {
        return mNotifyOnEnter;
    }

    public boolean isNotifyOnExit() {
        return mNotifyOnExit;
    }

    public boolean isNotifyOnDwell() {
        return mNotifyOnDwell;
    }

    public int getLoiteringDelay() {
        return mLoiteringDelay;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Geofence geofence = (Geofence) o;

        if (mRadius != geofence.mRadius) return false;
        if (Double.compare(geofence.mLatitude, mLatitude) != 0) return false;
        if (Double.compare(geofence.mLongitude, mLongitude) != 0) return false;
        if (mNotifyOnEnter != geofence.mNotifyOnEnter) return false;
        if (mNotifyOnExit != geofence.mNotifyOnExit) return false;
        if (mNotifyOnDwell != geofence.mNotifyOnDwell) return false;
        if (mLoiteringDelay != geofence.mLoiteringDelay) return false;
        return mId.equals(geofence.mId);
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = mId.hashCode();
        result = 31 * result + mRadius;
        temp = Double.doubleToLongBits(mLatitude);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(mLongitude);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + (mNotifyOnEnter ? 1 : 0);
        result = 31 * result + (mNotifyOnExit ? 1 : 0);
        result = 31 * result + (mNotifyOnDwell ? 1 : 0);
        result = 31 * result + mLoiteringDelay;
        return result;
    }
}
