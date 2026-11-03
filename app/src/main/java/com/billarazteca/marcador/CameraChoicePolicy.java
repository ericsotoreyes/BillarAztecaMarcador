package com.billarazteca.marcador;

/** Front-only selection from native metadata, with a legacy vendor compatibility path. */
final class CameraChoicePolicy {
    static int selectFront(String[] ids, int[] facing, String legacyFrontId) {
        if (ids.length != facing.length) throw new IllegalArgumentException("Invalid inventory");
        // Camera2 LENS_FACING_FRONT is 0. Never fall back to an arbitrary camera.
        for (int i = 0; i < ids.length; i++) if (facing[i] == 0) return i;
        if (legacyFrontId != null && !legacyFrontId.isEmpty()) {
            for (int i = 0; i < ids.length; i++) if (legacyFrontId.equals(ids[i])) return i;
        }
        return -1;
    }
}
