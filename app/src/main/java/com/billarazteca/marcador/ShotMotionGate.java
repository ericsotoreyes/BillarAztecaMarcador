package com.billarazteca.marcador;

/** Pure state machine. Only a visible, observed shot can award a fresh shot clock. */
final class ShotMotionGate {
    static final long STILL_MS = 2000;
    static final long MAX_GAP_MS = 800;
    enum State { ACQUIRING, READY, MOVING, LOST }
    static final class Result {
        final State state;
        final boolean resetClock;
        Result(State state, boolean resetClock) { this.state = state; this.resetClock = resetClock; }
        boolean holdClock() { return state != State.READY; }
    }
    private State state = State.ACQUIRING;
    private TableBallDetector.Ball[] anchor;
    private long stillSince = -1;
    private long lastAt = -1;
    private boolean shot;

    Result update(TableBallDetector.Ball[] balls, long now) {
        // Never award a reset across a missing-camera interval. A new session reacquires
        // the balls without interpreting their new positions as a shot.
        if (lastAt >= 0 && (now - lastAt > MAX_GAP_MS || now < lastAt)) {
            anchor = null;
            stillSince = -1;
            if (state != State.MOVING && state != State.LOST) shot = false;
        }
        lastAt = now;
        if (balls == null || balls.length != 3) {
            state = State.LOST;
            anchor = null;
            stillSince = -1;
            return new Result(state, false);
        }
        if (anchor == null) {
            anchor = balls;
            stillSince = now;
            state = shot ? State.MOVING : State.ACQUIRING;
            return new Result(state, false);
        }
        if (hasMoved(anchor, balls)) {
            if (state == State.READY) shot = true;
            anchor = balls;
            stillSince = now;
            state = shot ? State.MOVING : State.ACQUIRING;
            return new Result(state, false);
        }
        if (now - stillSince >= STILL_MS) {
            boolean reset = shot;
            shot = false;
            state = State.READY;
            // Keep the stationary anchor, so very slow cumulative movement is seen.
            return new Result(state, reset);
        }
        return new Result(state, false);
    }

    private boolean hasMoved(TableBallDetector.Ball[] a, TableBallDetector.Ball[] b) {
        // Exhaustive 3! matching also supports tables with two white balls.
        double best = Double.POSITIVE_INFINITY;
        int[] bestPermutation = null;
        for (int i = 0; i < 3; i++) for (int j = 0; j < 3; j++) {
            if (i == j) continue;
            int k = 3 - i - j;
            int[] p = {i, j, k};
            double cost = 0;
            for (int n = 0; n < 3; n++) {
                if (a[n].color != b[p[n]].color) { cost = Double.POSITIVE_INFINITY; break; }
                cost += Math.hypot(a[n].x - b[p[n]].x, a[n].y - b[p[n]].y);
            }
            if (cost < best) { best = cost; bestPermutation = p; }
        }
        if (bestPermutation == null) return true;
        for (int n = 0; n < 3; n++) {
            TableBallDetector.Ball next = b[bestPermutation[n]];
            // Coordinates are rectified to the table, rather than raw image pixels.
            double tolerance = Math.max(.001, Math.min(a[n].diameter, next.diameter) * .12);
            if (Math.hypot(a[n].x - next.x, a[n].y - next.y) > tolerance) return true;
        }
        return false;
    }
}
