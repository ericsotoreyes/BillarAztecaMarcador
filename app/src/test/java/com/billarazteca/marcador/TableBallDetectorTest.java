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

    @Test public void whiteHighlightsOnRedAndYellowAreNotExtraBalls(){
        int[] rgb=scene();
        disk(rgb,320,197,2,0xfffafafa);disk(rgb,500,267,2,0xfffafafa);
        TableBallDetector.Ball[] found=detector().detect(rgb,W,H);
        assertNotNull(found);assertEquals(3,found.length);
    }
    @Test public void thinRailGlintsDoNotBlockCalibration(){
        int[] rgb=scene();
        for(int y=50;y<52;y++)for(int x=300;x<305;x++)rgb[y*W+x]=0xffeeeeee;
        assertTrue(detector().calibrate(rgb,W,H));
    }
    @Test public void smallClothGlintDoesNotReplaceRealWhiteBall(){
        int[] rgb=scene();disk(rgb,210,100,2,0xffeeeeee);
        TableBallDetector.Ball[] found=detector().detect(rgb,W,H);
        assertNotNull(found);
        TableBallDetector.Ball white=Arrays.stream(found).filter(b->b.color==1).findFirst().get();
        assertEquals(140.0/W,white.imageX,.005);
    }
    @Test public void highlightsCannotStandInForAMissingWhiteBall(){
        int[] rgb=scene();disk(rgb,140,100,8,0xff147344);
        disk(rgb,320,197,2,0xfffafafa);disk(rgb,500,267,2,0xfffafafa);
        assertNull(detector().detect(rgb,W,H));
    }
    @Test public void blueClothWithShinyBallsAndRailGlintRegression(){
        // Synthetic geometry/colors reproducing the reported photo. No user image or
        // identifying content is stored in the repository.
        int[] rgb=new int[W*H];Arrays.fill(rgb,0xff217de8);
        disk(rgb,288,135,5,0xffe0edff);
        disk(rgb,340,147,6,0xffffdc5a);
        disk(rgb,233,156,7,0xffdc0f64);
        disk(rgb,336,149,2,0xfffafafa);
        disk(rgb,234,153,2,0xfffafafa);
        for(int y=80;y<82;y++)for(int x=332;x<337;x++)rgb[y*W+x]=0xffeeeeee;
        disk(rgb,508,165,2,0xffdddddd);
        TableBallDetector d=new TableBallDetector(new float[]{.10f,.55f,.37f,.21f,.617f,.227f,.886f,.567f});
        TableBallDetector.Ball[] found=d.detect(rgb,W,H);
        assertNotNull(found);assertEquals(3,found.length);assertTrue(d.calibrate(rgb,W,H));
    }
}
