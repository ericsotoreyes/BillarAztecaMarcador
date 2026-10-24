package com.billarazteca.marcador;

import org.junit.Test;
import static org.junit.Assert.*;

public class ReplayAvailabilityTest {
    @Test public void fastTapDoesNotStopAnUnstartedRecording() {
        assertEquals(ReplayAvailability.Action.WAIT, ReplayAvailability.onRequest(0,true,0,20000,false));
        assertEquals(ReplayAvailability.Action.WAIT, ReplayAvailability.onRequest(0,true,10000,10999,false));
    }
    @Test public void currentClipCanBeFinalizedWithoutWaitingForTenSecondRotation() {
        assertEquals(ReplayAvailability.Action.PLAY, ReplayAvailability.onRequest(0,true,10000,11000,false));
    }
    @Test public void savingLastClipMustNotReportEmptyBufferOrStartAnotherRecorder() {
        assertEquals(ReplayAvailability.Action.WAIT, ReplayAvailability.onRequest(0,false,0,20000,true));
    }
    @Test public void existingClipsStayAvailableEvenIfCameraIsStopped() {
        assertEquals(ReplayAvailability.Action.PLAY, ReplayAvailability.onRequest(2,false,0,20000,false));
        assertEquals(ReplayAvailability.Action.PLAY, ReplayAvailability.onRequest(2,true,0,20000,true));
    }
    @Test public void emptyStoppedCameraRequestsRecovery() {
        assertEquals(ReplayAvailability.Action.RETRY, ReplayAvailability.onRequest(0,false,0,20000,false));
    }
}
