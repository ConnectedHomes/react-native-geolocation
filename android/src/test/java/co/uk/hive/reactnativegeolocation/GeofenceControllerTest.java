package co.uk.hive.reactnativegeolocation;

import co.uk.hive.reactnativegeolocation.data.GeofenceRepository;
import co.uk.hive.reactnativegeolocation.geofence.legacy.Geofence;
import co.uk.hive.reactnativegeolocation.geofence.legacy.GeofenceActivator;
import co.uk.hive.reactnativegeolocation.geofence.legacy.GeofenceController;
import co.uk.hive.reactnativegeolocation.geofence.legacy.GeofenceEngine;

import com.annimon.stream.function.Function;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@SuppressWarnings("Convert2Lambda")
@RunWith(MockitoJUnitRunner.class)
public class GeofenceControllerTest {

    @Mock
    private GeofenceEngine mGeofenceEngine;

    @Mock
    private GeofenceRepository mGeofenceRepository;

    @Mock
    private GeofenceActivator mGeofenceActivator;

    @InjectMocks
    private GeofenceController mSut;

    private List<Geofence> mGeofences = Arrays.asList(
            TestData.createGeofence("1"),
            TestData.createGeofence("2"));

    private Function<? super Object, ? super Object> mCallback = new Function<Object, Object>() {
        @Override
        public Object apply(Object ignored) {
            return null;
        }
    };

    @Test
    public void startsGeofences() {
        given(mGeofenceRepository.getGeofences()).willReturn(mGeofences);

        mSut.start(mCallback, mCallback);

        verify(mGeofenceEngine).addGeofences(mGeofences, mCallback, mCallback);
        verify(mGeofenceActivator).setGeofencesActivated(true);
    }

    @Test
    public void stopsGeofences() {
        given(mGeofenceRepository.getGeofences()).willReturn(mGeofences);

        mSut.stop(mCallback, mCallback);

        verify(mGeofenceEngine).removeGeofences(Arrays.asList("1", "2"), mCallback, mCallback);
        verify(mGeofenceActivator).setGeofencesActivated(false);
    }

    @Test
    public void restartsGeofences() {
        given(mGeofenceRepository.getGeofences()).willReturn(mGeofences);
        given(mGeofenceActivator.areGeofencesActivated()).willReturn(true);

        mSut.restart(mCallback, mCallback);

        verify(mGeofenceActivator).areGeofencesActivated();
        verify(mGeofenceEngine).addGeofences(eq(mGeofences), any(), any());
    }

    @Test
    public void interactsWithRepository() {
        mSut.addGeofences(mGeofences);
        verify(mGeofenceRepository).addGeofences(mGeofences);

        mSut.removeAllGeofences();
        verify(mGeofenceRepository).removeAllGeofences();
    }
}