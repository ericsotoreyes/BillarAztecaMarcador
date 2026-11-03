package com.billarazteca.marcador;
import org.junit.Test;
import static org.junit.Assert.*;
public class ReplayReadinessTest {
 @Test public void failedCameraShowsActualError() { assertEquals(ReplayReadiness.State.ERROR,ReplayReadiness.evaluate("no front",0,9000)); }
 @Test public void unstartedRecordingCannotBeStoppedForReplay() { assertEquals(ReplayReadiness.State.WAIT,ReplayReadiness.evaluate("",0,9000)); }
 @Test public void earlyTapKeepsFirstRecordingRunning() { assertEquals(ReplayReadiness.State.WAIT,ReplayReadiness.evaluate("",8000,8500)); }
 @Test public void oneSecondOfVideoAllowsFinalizing() { assertEquals(ReplayReadiness.State.READY,ReplayReadiness.evaluate("",8000,9000)); }
}
