package com.billarazteca.marcador;

import java.util.ArrayList;
import java.util.List;

/** Scores are live; only closed visits contribute to averages and best runs. */
final class VisitLedger {
    private static final class Visit {
        final int player;
        int points;
        boolean closed;
        Visit(int player, int points, boolean closed) {
            this.player = player; this.points = points; this.closed = closed;
        }
    }
    private final List<Visit> visits = new ArrayList<>();
    private Visit pending() {
        if (visits.isEmpty()) return null;
        Visit last = visits.get(visits.size() - 1);
        return last.closed ? null : last;
    }
    int pendingPlayer() { return pending() == null ? 0 : pending().player; }
    int pendingPoints() { return pending() == null ? 0 : pending().points; }
    boolean record(int player, int points, int target) {
        if (player != 1 && player != 2) throw new IllegalArgumentException("player");
        if (points < 0 || target < 1) throw new IllegalArgumentException("points/target");
        Visit visit = pending();
        if (visit != null && visit.player == player && points == 0) return false;
        if (visit == null || visit.player != player) {
            if (visit != null) visit.closed = true;
            visit = new Visit(player, 0, false);
            visits.add(visit);
        }
        visit.points += Math.min(points, Math.max(0, target - total(player)));
        if (total(player) >= target) visit.closed = true;
        return true;
    }
    boolean subtract(int player) {
        for (int i = visits.size() - 1; i >= 0; i--) {
            Visit visit = visits.get(i);
            if (visit.player == player && visit.points > 0) {
                visit.points--;
                return true;
            }
        }
        return false;
    }
    boolean hasVisit(int player) {
        for (Visit v : visits) if (v.player == player) return true;
        return false;
    }
    int total(int player) {
        int n = 0;
        for (Visit v : visits) if (v.player == player) n += v.points;
        return n;
    }
    int completedPoints(int player) {
        int n = 0;
        for (Visit v : visits) if (v.player == player && v.closed) n += v.points;
        return n;
    }
    int innings(int player) {
        int n = 0;
        for (Visit v : visits) if (v.player == player && v.closed) n++;
        return n;
    }
    double average(int player) {
        int n = innings(player);
        return n == 0 ? 0.0 : completedPoints(player) / (double) n;
    }
    int best(int player) {
        int n = 0;
        for (Visit v : visits) if (v.player == player && v.closed) n = Math.max(n, v.points);
        return n;
    }
    int receiptBase(int player) {
        for (int i = visits.size() - 1; i >= 0; i--) {
            Visit v = visits.get(i);
            if (v.player == player) return total(player) - v.points;
        }
        return 0;
    }
    String encode() {
        StringBuilder s = new StringBuilder();
        for (Visit v : visits) s.append(v.player).append(',').append(v.points)
                .append(',').append(v.closed ? 1 : 0).append(';');
        return s.toString();
    }
    static VisitLedger decode(String encoded) {
        VisitLedger ledger = new VisitLedger();
        if (encoded == null || encoded.isEmpty()) return ledger;
        String[] rows = encoded.split(";");
        for (int i = 0; i < rows.length; i++) {
            String[] cells = rows[i].split(",");
            if (cells.length != 3) throw new IllegalArgumentException("visit snapshot");
            int p = Integer.parseInt(cells[0]), n = Integer.parseInt(cells[1]), closed = Integer.parseInt(cells[2]);
            if ((p != 1 && p != 2) || n < 0 || n > 999 || (closed != 0 && closed != 1)
                    || (closed == 0 && i != rows.length - 1)) throw new IllegalArgumentException("visit snapshot");
            ledger.visits.add(new Visit(p, n, closed == 1));
        }
        return ledger;
    }
}
