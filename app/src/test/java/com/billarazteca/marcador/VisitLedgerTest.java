package com.billarazteca.marcador;
import org.junit.Test;
import static org.junit.Assert.*;
public class VisitLedgerTest {
 @Test public void bulkTapsAreOneVisitAndStatisticsWaitForOpponent() {
  VisitLedger l=new VisitLedger(); l.record(1,3,30); l.record(1,5,30);
  assertEquals(8,l.total(1)); assertEquals(0,l.innings(1)); assertEquals(0,l.best(1)); assertEquals(0,l.average(1),0);
  l.record(2,1,30); assertEquals(1,l.innings(1)); assertEquals(8,l.best(1)); assertEquals(8,l.average(1),0);
  l.record(2,5,30); assertEquals(1,l.innings(1)); assertEquals(0,l.innings(2));
 }
 @Test public void zeroVisitsCountWhenOpponentNextRecords() {
  VisitLedger l=new VisitLedger(); l.record(1,8,30); l.record(2,0,30);
  assertEquals(1,l.innings(1)); assertEquals(0,l.innings(2));
  l.record(1,0,30); assertEquals(1,l.innings(2)); assertEquals(0,l.average(2),0);
  l.record(2,3,30); assertEquals(2,l.innings(1)); assertEquals(4,l.average(1),0);
 }
 @Test public void repeatedZeroDoesNotCreateExtraVisits() {
  VisitLedger l=new VisitLedger(); assertTrue(l.record(1,0,30)); assertFalse(l.record(1,0,30));
  l.record(2,0,30); assertEquals(1,l.innings(1));
 }
 @Test public void targetImmediatelyFinalizesWinnerAndCapsBulkAddition() {
  VisitLedger l=new VisitLedger(); l.record(1,8,10); l.record(2,0,10); l.record(1,5,10);
  assertEquals(10,l.total(1)); assertEquals(2,l.innings(1)); assertEquals(5,l.average(1),0);
  assertEquals(8,l.best(1)); assertEquals(1,l.innings(2)); assertEquals(0,l.pendingPlayer());
 }
 @Test public void pendingScoresDoNotInflateAlreadyDisplayedAverage() {
  VisitLedger l=new VisitLedger(); l.record(1,4,30); l.record(2,2,30); l.record(1,5,30);
  assertEquals(9,l.total(1)); assertEquals(4,l.average(1),0); assertEquals(4,l.best(1));
 }
 @Test public void correctionsRecalculateCompletedBestAndAverage() {
  VisitLedger l=new VisitLedger(); l.record(1,5,30); l.record(2,0,30); l.subtract(1);
  assertEquals(4,l.best(1)); assertEquals(4,l.average(1),0); assertEquals(4,l.total(1));
 }
 @Test public void pendingCorrectionDoesNotChangeCompletedStatistics() {
  VisitLedger l=new VisitLedger(); l.record(1,5,30); l.subtract(1);
  assertEquals(4,l.total(1)); assertEquals(0,l.innings(1)); assertEquals(0,l.best(1));
 }
 @Test public void snapshotRestoresPendingVisitForRecoveryAndUndo() {
  VisitLedger l=new VisitLedger(); l.record(1,3,30); String before=l.encode(); l.record(2,0,30);
  VisitLedger restored=VisitLedger.decode(before); assertEquals(3,restored.total(1)); assertEquals(0,restored.innings(1));
  assertEquals(1,restored.pendingPlayer()); restored.record(1,5,30); restored.record(2,0,30);
  assertEquals(8,restored.best(1)); assertEquals(1,restored.innings(1));
 }
 @Test public void receiptRemainsWhileOpponentScores() {
  VisitLedger l=new VisitLedger(); l.record(1,3,30); l.record(2,1,30); l.record(1,5,30);
  assertEquals(3,l.receiptBase(1)); l.record(2,3,30); assertEquals(3,l.receiptBase(1));
 }
 @Test public void firstScorerDoesNotInventAnUnrecordedOpponentVisit() {
  VisitLedger l=new VisitLedger(); l.record(2,3,30); assertEquals(0,l.innings(1));
 }
 @Test(expected=IllegalArgumentException.class) public void rejectsPendingVisitInMiddleOfSnapshot() {
  VisitLedger.decode("1,3,0;2,0,0;");
 }
}
