package com.billarazteca.marcador;
final class ReplayReadiness {
 enum State { ERROR, WAIT, READY }
 static State evaluate(String error, long startedAt, long now) {
  if (error != null && !error.isEmpty()) return State.ERROR;
  if (startedAt <= 0 || now - startedAt < 1000L) return State.WAIT;
  return State.READY;
 }
}
