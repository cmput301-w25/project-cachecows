package com.example.feelink;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;



import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ConnectivityReceiverTest {

    @Mock
    Context mockContext;
    @Mock
    Network mockNetwork;
    @Mock NetworkCapabilities mockNetworkCapabilities;
    @Mock
    ConnectivityManager mockConnectivityManager;
    @Mock
    ConnectivityReceiver.ConnectivityReceiverListener mockListener;

    private ConnectivityReceiver connectivityReceiver;

    @Before
    public void setUp() {
        when(mockContext.getSystemService(Context.CONNECTIVITY_SERVICE))
                .thenReturn(mockConnectivityManager);
        connectivityReceiver = new ConnectivityReceiver(mockListener);
    }

    @Test
    public void testIsNetworkAvailableReturnsTrueWhenConnected() {
        when(mockConnectivityManager.getActiveNetwork()).thenReturn(mockNetwork);
        when(mockConnectivityManager.getNetworkCapabilities(mockNetwork))
                .thenReturn(mockNetworkCapabilities);
        when(mockNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI))
                .thenReturn(true);

        boolean result = ConnectivityReceiver.isNetworkAvailable(mockContext);
        assertTrue(result);
    }

    @Test
    public void testIsNetworkAvailableReturnsFalseWhenNoNetwork() {
        boolean result = ConnectivityReceiver.isNetworkAvailable(mockContext);
        assertFalse(result);
    }

    @Test
    public void testIsNetworkAvailableReturnsFalseWhenNoCapabilities() {
        when(mockConnectivityManager.getActiveNetwork()).thenReturn(mockNetwork);
        when(mockConnectivityManager.getNetworkCapabilities(mockNetwork))
                .thenReturn(null);

        boolean result = ConnectivityReceiver.isNetworkAvailable(mockContext);
        assertFalse(result);
    }

    @Test
    public void testOnReceiveWhenDisconnectedCallsListener() {
        connectivityReceiver.onReceive(mockContext, new Intent());
        verify(mockListener, times(1)).onNetworkConnectionChanged(false);
    }

    @After
    public void tearDown() {
        Mockito.framework().clearInlineMocks();
        try {
            com.google.firebase.FirebaseApp.getInstance().delete();
        } catch (Exception ignored) {}
    }
}