package com.billarazteca.marcador;

/** Choose only among actual camera instances; never require an absent lens-facing label. */
final class CameraChoicePolicy {
    static int select(String[] ids, int[] facing, String preferredId, boolean preferExternal) {
        if (ids.length != facing.length) throw new IllegalArgumentException("Invalid camera list");
        if (preferredId != null && !preferredId.isEmpty()) {
            for (int i=0;i<ids.length;i++) if (preferredId.equals(ids[i])) return i;
        }
        if (preferExternal) {
            for (int i=0;i<facing.length;i++) if (facing[i]==2) return i;
        }
        for (int i=0;i<facing.length;i++) if (facing[i]==0) return i;
        for (int i=0;i<facing.length;i++) if (facing[i]!=2) return i;
        return ids.length==0 ? -1 : 0;
    }
}
