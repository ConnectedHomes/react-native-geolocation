package co.uk.hive.reactnativegeolocation;

import co.uk.hive.reactnativegeolocation.data.DataMarshaller;
import co.uk.hive.reactnativegeolocation.data.DataStorage;
import co.uk.hive.reactnativegeolocation.geofence.DataStorageGeofenceRepository;
import co.uk.hive.reactnativegeolocation.geofence.Geofence;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static co.uk.hive.reactnativegeolocation.geofence.DataStorageGeofenceRepository.KEY_GEOFENCES;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@RunWith(MockitoJUnitRunner.class)
public class DataStorageGeofenceRepositoryTest {

    private static final String STORED_DATA = "{serialized_geofences}";

    @Mock
    private DataStorage mDataStorage;

    @Mock
    private DataMarshaller mDataMarshaller;

    private DataStorageGeofenceRepository mSut;

    private List<Geofence> mGeofences = new LinkedList<>();

    @Before
    public void setUp() {
        givenDependencies();
        givenGeofences();
        mSut = new DataStorageGeofenceRepository(mDataStorage, mDataMarshaller);
    }

    @Test
    public void retrievesGeofences() {
        List<Geofence> geofences = mSut.getGeofences();

        assertEquals(mGeofences, geofences);
    }

    @Test
    public void addsGeofencesWithoutDuplicates() {
        mSut.addGeofences(mGeofences);
        mSut.addGeofences(Collections.singletonList(TestData.createGeofence("1")));

        assertEquals(mGeofences, mSut.getGeofences());
    }

    @Test
    public void removesGeofences() {
        mSut.addGeofences(mGeofences);
        mSut.removeAllGeofences();
        List<Geofence> geofences = mSut.getGeofences();

        assertTrue(geofences.isEmpty());
    }

    private void givenDependencies() {
        given(mDataStorage.load(KEY_GEOFENCES)).willReturn(STORED_DATA);
        given(mDataMarshaller.unmarshalList(eq(STORED_DATA), eq(Geofence.class), any())).willReturn(mGeofences);
        given(mDataMarshaller.marshal(mGeofences)).willReturn(STORED_DATA);
    }

    private void givenGeofences() {
        mGeofences.add(TestData.createGeofence("1"));
        mGeofences.add(TestData.createGeofence("2"));
    }
}