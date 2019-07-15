package co.uk.hive.reactnativegeolocation.location;

import java.util.concurrent.TimeUnit;

/**
 * No parameters are used at the moment.
 *
 * See https://transistorsoft.github.io/react-native-background-geolocation/interfaces/_react_native_background_geolocation_.currentpositionrequest.html
 */
public class CurrentPositionRequest {

    public static final int DEFAULT_TIMEOUT = (int) TimeUnit.SECONDS.toMillis(30);

    private long mTimeout;

    public CurrentPositionRequest(int timeout) {
        mTimeout = timeout;
    }

    public long getTimeout() {
        return mTimeout;
    }
}
