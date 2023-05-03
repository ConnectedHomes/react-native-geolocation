package co.uk.hive.reactnativegeolocation.data;

import co.uk.hive.reactnativegeolocation.geofence.legacy.GeofenceActivator;

public class DataStorageGeofenceActivator implements GeofenceActivator {

    public static final String KEY_ACTIVATED = "key_activated";

    private boolean mActivated;

    private final DataStorage mDataStorage;
    private final DataMarshaller mDataMarshaller;

    public DataStorageGeofenceActivator(DataStorage dataStorage, DataMarshaller dataMarshaller) {
        mDataStorage = dataStorage;
        mDataMarshaller = dataMarshaller;
        load();
    }

    @Override
    public void setGeofencesActivated(boolean activated) {
        mActivated = activated;
        save();
    }

    @Override
    public boolean areGeofencesActivated() {
        return mActivated;
    }

    private void save() {
        mDataStorage.store(KEY_ACTIVATED, mDataMarshaller.marshal(mActivated));
    }

    private void load() {
        mActivated = mDataMarshaller.unmarshal(mDataStorage.load(KEY_ACTIVATED), Boolean.class, mActivated);
    }
}
