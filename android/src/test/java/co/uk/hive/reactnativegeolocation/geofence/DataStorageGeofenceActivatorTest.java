package co.uk.hive.reactnativegeolocation.geofence;

import co.uk.hive.reactnativegeolocation.data.DataMarshaller;
import co.uk.hive.reactnativegeolocation.data.DataStorage;
import co.uk.hive.reactnativegeolocation.data.DataStorageGeofenceActivator;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static co.uk.hive.reactnativegeolocation.data.DataStorageGeofenceActivator.KEY_ACTIVATED;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@RunWith(MockitoJUnitRunner.class)
public class DataStorageGeofenceActivatorTest {

    @Mock
    private DataStorage mDataStorage;

    @Mock
    private DataMarshaller mDataMarshaller;

    private DataStorageGeofenceActivator mSut;

    @Before
    public void setUp() {
        givenDependencies();
        mSut = new DataStorageGeofenceActivator(mDataStorage, mDataMarshaller);
    }

    @Test
    public void storesEnabledState() {
        mSut.setGeofencesActivated(true);

        assertTrue(mSut.areGeofencesActivated());
    }

    private void givenDependencies() {
        given(mDataStorage.load(KEY_ACTIVATED)).willReturn("false");
        given(mDataMarshaller.unmarshal(eq("false"), eq(Boolean.class), any())).willReturn(false);
        given(mDataMarshaller.marshal(true)).willReturn("true");
    }
}