package com.billarazteca.marcador;

import org.junit.Test;
import static org.junit.Assert.*;

public class ShotMotionGateTest {
    private TableBallDetector.Ball[] balls(double x) {
        return new TableBallDetector.Ball[]{ball(1,x,.3),ball(2,.5,.6),ball(3,.8,1.5)};
    }
    private TableBallDetector.Ball ball(int color,double x,double y) {
        return new TableBallDetector.Ball(color,x,y,.035,0,0,5);
    }
    private ShotMotionGate.Result feed(ShotMotionGate gate, TableBallDetector.Ball[] b,long from,long to) {
        ShotMotionGate.Result r=null;
        for(long t=from;t<=to;t+=100)r=gate.update(b,t);
        return r;
    }
    @Test public void initialStationarySceneNeverAwardsTime() {
        ShotMotionGate g=new ShotMotionGate();
        assertTrue(g.update(balls(.2),0).holdClock());
        assertEquals(ShotMotionGate.State.READY,feed(g,balls(.2),100,2000).state);
        for(long t=2100;t<20000;t+=100)assertFalse(g.update(balls(.2),t).resetClock);
    }
    @Test public void pauseImmediatelyAndResetOnceAfterTwoSecondsStill() {
        ShotMotionGate g=new ShotMotionGate();feed(g,balls(.2),0,2000);
        ShotMotionGate.Result moving=g.update(balls(.3),2100);
        assertTrue(moving.holdClock());assertEquals(ShotMotionGate.State.MOVING,moving.state);
        for(long t=2200;t<4100;t+=100)assertFalse(g.update(balls(.3),t).resetClock);
        assertTrue(g.update(balls(.3),4100).resetClock);
        assertFalse(g.update(balls(.3),4200).resetClock);
        assertFalse(g.update(balls(.3),4300).holdClock());
    }
    @Test public void movingBallsNeverReachReady() {
        ShotMotionGate g=new ShotMotionGate();feed(g,balls(.2),0,2000);
        for(int i=0;i<500;i++) {
            ShotMotionGate.Result r=g.update(balls(.2+.01*((i+1)%40)),2100+i*100);
            assertTrue(r.holdClock());assertFalse(r.resetClock);
        }
    }
    @Test public void slowCumulativeMotionResetsSettleWindow() {
        ShotMotionGate g=new ShotMotionGate();feed(g,balls(.2),0,2000);
        g.update(balls(.4),2100);
        for(int i=1;i<50;i++)assertFalse(g.update(balls(.4+i*.0006),2100+i*100).resetClock);
    }
    @Test public void occlusionCannotLookLikeStoppedBalls() {
        ShotMotionGate g=new ShotMotionGate();feed(g,balls(.2),0,2000);g.update(balls(.4),2100);
        for(long t=2200;t<=8000;t+=100)assertTrue(g.update(null,t).holdClock());
        assertFalse(g.update(balls(.4),8100).resetClock);
        feed(g,balls(.4),8200,10000);
        assertTrue(g.update(balls(.4),10100).resetClock);
    }
    @Test public void occlusionWithoutObservedShotDoesNotAwardTime() {
        ShotMotionGate g=new ShotMotionGate();feed(g,balls(.2),0,2000);
        feed(g,null,2100,5000);
        for(long t=5100;t<=8000;t+=100)assertFalse(g.update(balls(.7),t).resetClock);
    }
    @Test public void missingFramesHoldAndReacquireWithoutAwardingTime() {
        ShotMotionGate g=new ShotMotionGate();feed(g,balls(.2),0,2000);
        assertTrue(g.update(balls(.7),4000).holdClock());
        for(long t=4100;t<=7000;t+=100)assertFalse(g.update(balls(.7),t).resetClock);
    }
    @Test public void separateSessionCancelsOldShotOnTurnOrManualPause() {
        ShotMotionGate old=new ShotMotionGate();feed(old,balls(.2),0,2000);old.update(balls(.4),2100);
        ShotMotionGate next=new ShotMotionGate();
        for(long t=2200;t<=5000;t+=100)assertFalse(next.update(balls(.4),t).resetClock);
    }
    @Test public void visibleRollingAfterBriefOcclusionStillCountsAsObservedShot() {
        ShotMotionGate g=new ShotMotionGate();feed(g,balls(.2),0,2000);
        g.update(null,2100);g.update(balls(.3),2200);
        assertEquals(ShotMotionGate.State.MOVING,g.update(balls(.4),2300).state);
        for(long t=2400;t<4300;t+=100)assertFalse(g.update(balls(.4),t).resetClock);
        assertTrue(g.update(balls(.4),4300).resetClock);
    }
    @Test public void twoWhiteBallsCanExchangeDetectionOrder() {
        ShotMotionGate g=new ShotMotionGate();
        TableBallDetector.Ball[] a={ball(1,.2,.3),ball(1,.5,.6),ball(3,.8,1.5)};
        feed(g,a,0,2000);
        TableBallDetector.Ball[] b={a[1],a[2],a[0]};
        assertFalse(g.update(b,2100).holdClock());
    }
}
