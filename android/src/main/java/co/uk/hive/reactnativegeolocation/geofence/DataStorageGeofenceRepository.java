package co.uk.hive.reactnativegeolocation.geofence;

import co.uk.hive.reactnativegeolocation.data.DataMarshaller;
import co.uk.hive.reactnativegeolocation.data.DataStorage;
import com.annimon.stream.Optional;
import com.annimon.stream.Stream;

import java.util.LinkedList;
import java.util.List;

public class DataStorageGeofenceRepository implements GeofenceRepository {

    public static final String KEY_GEOFENCES = "key_geofences";

    private final DataStorage mDataStorage;
    private final DataMarshaller mDataMarshaller;

    private List<Geofence> mGeofences = new LinkedList<>();

    public DataStorageGeofenceRepository(DataStorage dataStorage, DataMarshaller dataMarshaller) {
        mDataStorage = dataStorage;
        mDataMarshaller = dataMarshaller;
        load();
    }

    @Override
    public List<Geofence> getGeofences() {
        return mGeofences;
    }

    @Override
    public Optional<Geofence> getGeofenceById(String id) {
        return Stream.of(mGeofences).filter(geofence -> geofence.getId().equals(id)).findFirst();
    }

    @Override
    public void addGeofences(List<Geofence> geofences) {
        Stream.of(geofences)
                .filter(geofence -> !mGeofences.contains(geofence))
                .forEach(geofence -> mGeofences.add(geofence));
        save();
    }

    @Override
    public void removeAllGeofences() {
        mGeofences = new LinkedList<>();
        save();
    }

    private void save() {
        String data = mDataMarshaller.marshal(mGeofences);
        mDataStorage.store(KEY_GEOFENCES, data);
    }

    private void load() {
        mGeofences = mDataMarshaller.unmarshalList(mDataStorage.load(KEY_GEOFENCES), Geofence.class, mGeofences);
    }
}
