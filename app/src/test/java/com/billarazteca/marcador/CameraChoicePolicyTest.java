package com.billarazteca.marcador;
import org.junit.Test;
import static org.junit.Assert.*;
public class CameraChoicePolicyTest {
    @Test public void choosesNativeFrontInsteadOfRear() {
        assertEquals(1, CameraChoicePolicy.selectFront(new String[]{"0","1"},new int[]{1,0},""));
    }
    @Test public void usesIndependentLegacyFrontWhenVendorMislabelsCameraX() {
        assertEquals(1, CameraChoicePolicy.selectFront(new String[]{"0","1"},new int[]{1,1},"1"));
    }
    @Test public void neverAssumesIdOneIsFront() {
        assertEquals(-1, CameraChoicePolicy.selectFront(new String[]{"0","1"},new int[]{1,1},""));
    }
    @Test public void doesNotUseRearWhenFrontIsAbsent() {
        assertEquals(-1, CameraChoicePolicy.selectFront(new String[]{"0"},new int[]{1},"1"));
    }
    @Test public void neverUsesUsbAsFallback() {
        assertEquals(-1, CameraChoicePolicy.selectFront(new String[]{"usb"},new int[]{2},""));
    }
    @Test public void nativeFrontWinsOverLegacy() {
        assertEquals(0, CameraChoicePolicy.selectFront(new String[]{"front","1"},new int[]{0,1},"1"));
    }
    @Test public void reorderingDoesNotChangeLens() {
        assertEquals(0, CameraChoicePolicy.selectFront(new String[]{"1","0"},new int[]{1,1},"1"));
    }
    @Test public void noCamerasMeansNoSelection() {
        assertEquals(-1, CameraChoicePolicy.selectFront(new String[0],new int[0],""));
    }
}
