package com.billarazteca.marcador;

import org.junit.Test;
import static org.junit.Assert.*;

public class CameraChoicePolicyTest {
    @Test public void reportedDeviceWithTwoBackCamerasStillGetsARealCamera() {
        // Exact inventory in the user's error: IDs 0/1, both lens facing 1.
        assertEquals(0,CameraChoicePolicy.select(new String[]{"0","1"},new int[]{1,1},"",false));
    }
    @Test public void manualChoiceCanSelectEitherCameraWithIdenticalFacing() {
        assertEquals(1,CameraChoicePolicy.select(new String[]{"0","1"},new int[]{1,1},"1",false));
    }
    @Test public void prefersFrontWhenActuallyAdvertised() {
        assertEquals(1,CameraChoicePolicy.select(new String[]{"0","1"},new int[]{1,0},"",false));
    }
    @Test public void keepsExplicitChoiceAcrossInventoryReordering() {
        assertEquals(0,CameraChoicePolicy.select(new String[]{"1","0"},new int[]{1,1},"1",false));
    }
    @Test public void handlesMissingSavedCameraAndDisconnectedUsb() {
        assertEquals(0,CameraChoicePolicy.select(new String[]{"0","1"},new int[]{1,1},"usb",true));
    }
    @Test public void keepsUsbSelectionWhenItIsAvailable() {
        assertEquals(2,CameraChoicePolicy.select(new String[]{"0","1","usb"},new int[]{1,0,2},"",true));
    }
    @Test public void emptyInventoryReportsNoSelection() {
        assertEquals(-1,CameraChoicePolicy.select(new String[0],new int[0],"",false));
    }
}
