package com.billarazteca.marcador;

/** Decide whether a tap can safely stop recording, must wait, or should restart capture. */
final class ReplayAvailability {
    enum Action { PLAY, WAIT, RETRY }
    static final long MIN_CAPTURE_MS = 1000;
    static Action onRequest(int clips, boolean recording, long startedAt, long now, boolean finalizing) {
        if (clips > 0) return Action.PLAY;
        if (finalizing) return Action.WAIT;
        if (!recording) return Action.RETRY;
        if (startedAt <= 0 || now - startedAt < MIN_CAPTURE_MS) return Action.WAIT;
        return Action.PLAY;
    }
}
