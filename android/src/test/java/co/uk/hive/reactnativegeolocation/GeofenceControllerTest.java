package co.uk.hive.reactnativegeolocation;

import com.annimon.stream.Stream;
import com.annimon.stream.function.Function;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.util.Arrays;
import java.util.List;

import co.uk.hive.reactnativegeolocation.geofence.Geofence;
import co.uk.hive.reactnativegeolocation.geofence.GeofenceActivator;
import co.uk.hive.reactnativegeolocation.geofence.GeofenceController;
import co.uk.hive.reactnativegeolocation.geofence.GeofenceEngine;
import co.uk.hive.reactnativegeolocation.geofence.GeofenceRepository;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
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

    private List<Geofence> mMixedGeofences = Arrays.asList(
            TestData.createGeofence("1"),
            TestData.createGeofence("2"),
            TestData.createInvalidGeofence("3")
    );

    private List<Geofence> mGeofences = mMixedGeofences.subList(0, mMixedGeofences.size() - 1);

    @Mock
    private Function<? super Object, ? super Object> mSuccessCallback;

    private Function<? super Object, ? super Object> mFailureCallback = new Function<Object, Object>() {
        @Override
        public Object apply(Object ignored) {
            return null;
        }
    };

    @Test
    public void startsGeofences() {
        given(mGeofenceRepository.getGeofences()).willReturn(mGeofences);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                return ((Function) invocation.getArgument(1)).apply(null);
            }
        }).when(mGeofenceEngine).addGeofences(anyList(), any(Function.class), any(Function.class));

        mSut.start(mSuccessCallback, mFailureCallback);

        verify(mGeofenceEngine).addGeofences(eq(mGeofences), any(Function.class), eq(mFailureCallback));
        verify(mSuccessCallback).apply(null);
        verify(mGeofenceActivator).setGeofencesActivated(true);
    }

    @Test
    public void stopsGeofences() {
        given(mGeofenceRepository.getGeofences()).willReturn(mGeofences);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                return ((Function) invocation.getArgument(1)).apply(null);
            }
        }).when(mGeofenceEngine).removeGeofences(anyList(), any(Function.class), any(Function.class));

        mSut.stop(mSuccessCallback, mFailureCallback);

        verify(mGeofenceEngine).removeGeofences(eq(Arrays.asList("1", "2")), any(Function.class), eq(mFailureCallback));
        verify(mSuccessCallback).apply(null);
        verify(mGeofenceActivator).setGeofencesActivated(false);
    }

    @Test
    public void restartsGeofences() {
        given(mGeofenceRepository.getGeofences()).willReturn(mGeofences);
        given(mGeofenceActivator.areGeofencesActivated()).willReturn(true);

        mSut.restart(mSuccessCallback, mFailureCallback);

        verify(mGeofenceActivator).areGeofencesActivated();
        verify(mGeofenceEngine).addGeofences(eq(mGeofences), any(), any());
    }

    @Test
    public void filtersInvalidGeofencesOnRestart() {
        given(mGeofenceRepository.getGeofences()).willReturn(mMixedGeofences);
        given(mGeofenceActivator.areGeofencesActivated()).willReturn(true);

        mSut.restart(mSuccessCallback, mFailureCallback);

        verify(mGeofenceActivator).areGeofencesActivated();
        verify(mGeofenceEngine).addGeofences(eq(mGeofences), any(), any());
    }

    @Test
    public void filtersInvalidGeofencesOnStop() {
        given(mGeofenceRepository.getGeofences()).willReturn(mMixedGeofences);

        mSut.stop(mSuccessCallback, mFailureCallback);

        final List<String> expectedIds = Stream.of(mGeofences).map(Geofence::getId).toList();
        verify(mGeofenceEngine).removeGeofences(eq(expectedIds), any(Function.class), eq(mFailureCallback));
    }

    @Test
    public void interactsWithRepository() {
        mSut.addGeofences(mGeofences);
        verify(mGeofenceRepository).addGeofences(mGeofences);

        mSut.removeAllGeofences();
        verify(mGeofenceRepository).removeAllGeofences();
    }
}