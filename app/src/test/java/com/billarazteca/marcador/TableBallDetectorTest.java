package com.billarazteca.marcador;

import org.junit.Test;
import java.util.Arrays;
import static org.junit.Assert.*;

public class TableBallDetectorTest {
    private static final int W=640,H=360;
    private TableBallDetector detector(){return new TableBallDetector(new float[]{.05f,.05f,.95f,.05f,.95f,.95f,.05f,.95f});}
    private int[] scene(){
        int[] rgb=new int[W*H];Arrays.fill(rgb,0xff147344);
        disk(rgb,140,100,7,0xffeeeeee);disk(rgb,320,200,7,0xffe8cf18);disk(rgb,500,270,7,0xffb51d22);
        return rgb;
    }
    private void disk(int[] rgb,int cx,int cy,int r,int color){
        for(int y=cy-r;y<=cy+r;y++)for(int x=cx-r;x<=cx+r;x++)
            if((x-cx)*(x-cx)+(y-cy)*(y-cy)<=r*r)rgb[y*W+x]=color;
    }
    @Test public void findsThreeBallsAndCalibrates(){
        TableBallDetector d=detector();int[] rgb=scene();
        assertEquals(3,d.detect(rgb,W,H).length);assertTrue(d.calibrate(rgb,W,H));assertNotNull(d.detect(rgb,W,H));
    }
    @Test public void missingBallOrExtraWhiteObjectIsUncertain(){
        TableBallDetector d=detector();int[] rgb=scene();
        assertTrue(d.calibrate(rgb,W,H));
        disk(rgb,140,100,8,0xff147344);assertNull(d.detect(rgb,W,H));
        rgb=scene();disk(rgb,200,200,7,0xffeeeeee);assertNull(d.detect(rgb,W,H));
    }
    @Test public void ignoresOutsideTable(){
        int[] rgb=scene();disk(rgb,15,15,7,0xffeeeeee);assertNotNull(detector().detect(rgb,W,H));
    }
    @Test public void largeObstructionAndChangedResolutionAreUncertain(){
        TableBallDetector d=detector();int[] rgb=scene();assertTrue(d.calibrate(rgb,W,H));
        for(int y=20;y<340;y++)for(int x=180;x<440;x++)rgb[y*W+x]=0xff282828;
        assertNull(d.detect(rgb,W,H));assertNull(d.detect(scene(),H,W));
    }
    @Test public void supportsTwoWhiteBalls(){
        int[] rgb=scene();disk(rgb,320,200,7,0xffeeeeee);assertNotNull(detector().detect(rgb,W,H));
    }
    @Test public void rejectsCrossedOrTinyCalibration(){
        try{new TableBallDetector(new float[]{.1f,.1f,.9f,.9f,.9f,.1f,.1f,.9f});fail();}catch(IllegalArgumentException expected){}
        try{new TableBallDetector(new float[]{.1f,.1f,.11f,.1f,.11f,.11f,.1f,.11f});fail();}catch(IllegalArgumentException expected){}
    }
}
