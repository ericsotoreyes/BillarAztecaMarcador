package com.billarazteca.marcador;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Color/shape tracking inside a user-defined table. No carom or score decisions. */
final class TableBallDetector {
    static final class Ball {
        final int color;
        final double x, y, diameter;
        final float imageX, imageY, radius;
        final int pixelArea;
        Ball(int color, double x, double y, double diameter, float imageX, float imageY, float radius) {
            this(color,x,y,diameter,imageX,imageY,radius,0);
        }
        Ball(int color, double x, double y, double diameter, float imageX, float imageY, float radius, int pixelArea) {
            this.pixelArea = pixelArea;
            this.color = color; this.x = x; this.y = y; this.diameter = diameter;
            this.imageX = imageX; this.imageY = imageY; this.radius = radius;
        }
    }
    final float[] corners;
    private final double[] transform;
    private double minDiameter, maxDiameter;
    private int[] expectedColors;
    private int[] reference;
    private int referenceWidth, referenceHeight;

    TableBallDetector(float[] corners) {
        if (corners == null || corners.length != 8) throw new IllegalArgumentException("Marca cuatro esquinas");
        this.corners = corners.clone();
        // Require a sufficiently large convex polygon in clockwise/counterclockwise order.
        double sign = 0, area = 0;
        for (int i = 0; i < 4; i++) {
            int j = (i + 1) % 4, k = (i + 2) % 4;
            float x = corners[i*2], y = corners[i*2+1];
            if (x < 0 || x > 1 || y < 0 || y > 1) throw new IllegalArgumentException("Esquina fuera de imagen");
            double cross = (corners[j*2]-x)*(corners[k*2+1]-corners[j*2+1])
                    - (corners[j*2+1]-y)*(corners[k*2]-corners[j*2]);
            if (Math.abs(cross) < .005 || (sign != 0 && cross * sign <= 0))
                throw new IllegalArgumentException("Marca las esquinas en orden alrededor de la mesa");
            sign = cross;
            area += x * corners[j*2+1] - y * corners[j*2];
        }
        if (Math.abs(area) < .12) throw new IllegalArgumentException("La mesa ocupa muy poca imagen");
        transform = homography(corners);
    }

    Ball[] detect(int[] rgb, int width, int height) {
        if (rgb.length != width * height || width < 8 || height < 8) return null;
        if (reference != null && (width != referenceWidth || height != referenceHeight || sceneObscured(rgb, width, height))) return null;
        byte[] labels = new byte[rgb.length];
        for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
            if (inside((float)x/width, (float)y/height)) labels[y*width+x] = (byte)color(rgb[y*width+x]);
        }
        int[] queue = new int[rgb.length];
        List<Ball> balls = new ArrayList<>();
        for (int pos = 0; pos < labels.length; pos++) {
            int label = labels[pos];
            if (label == 0) continue;
            int head = 0, tail = 1; queue[0] = pos; labels[pos] = 0;
            int minX = width, maxX = 0, minY = height, maxY = 0;
            long sumX = 0, sumY = 0;
            while (head < tail) {
                int p = queue[head++], x = p % width, y = p / width;
                minX = Math.min(minX,x); maxX = Math.max(maxX,x);
                minY = Math.min(minY,y); maxY = Math.max(maxY,y);
                sumX += x; sumY += y;
                if (x > 0 && labels[p-1] == label) { labels[p-1]=0; queue[tail++]=p-1; }
                if (x+1 < width && labels[p+1] == label) { labels[p+1]=0; queue[tail++]=p+1; }
                if (y > 0 && labels[p-width] == label) { labels[p-width]=0; queue[tail++]=p-width; }
                if (y+1 < height && labels[p+width] == label) { labels[p+width]=0; queue[tail++]=p+width; }
            }
            int bw = maxX-minX+1, bh = maxY-minY+1;
            if (tail < 5 || tail > rgb.length*.015 || bw < 2 || bh < 2 || bw > bh*2.2 || bh > bw*2.2
                    || tail < bw*bh*.35) continue;
            float cx = (float)sumX/tail/width, cy = (float)sumY/tail/height;
            double[] center = project(cx, cy);
            double[] left = project((float)minX/width, cy), right = project((float)(maxX+1)/width, cy);
            double[] top = project(cx,(float)minY/height), bottom = project(cx,(float)(maxY+1)/height);
            double diameter = (distance(left,right) + distance(top,bottom))/2;
            if (!Double.isFinite(diameter) || diameter < .006 || diameter > .15) continue;
            if (minDiameter > 0 && (diameter < minDiameter*.5 || diameter > maxDiameter*1.8)) continue;
            balls.add(new Ball(label, center[0], center[1], diameter, cx, cy, Math.max(bw,bh)/2f, tail));
            if (balls.size() > 24) return null;
        }
        return selectBallSet(balls,width,height);
    }

    private Ball[] selectBallSet(List<Ball> candidates, int width, int height) {
        List<Ball> filtered = new ArrayList<>();
        for (Ball ball : candidates) {
            boolean highlight = false;
            if (ball.color == 1) {
                for (Ball colored : candidates) {
                    if (colored.color == 1 || colored.pixelArea < ball.pixelArea * 2) continue;
                    double distance = Math.hypot((ball.imageX - colored.imageX) * width,
                            (ball.imageY - colored.imageY) * height);
                    // A small white specular region on a red/yellow surface is part of
                    // that same ball. A separate touching white ball is outside this radius.
                    if (ball.radius <= colored.radius * .65 && distance <= colored.radius + 1) {
                        highlight = true;
                        break;
                    }
                }
            }
            if (!highlight) filtered.add(ball);
        }
        Ball[] best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        double secondScore = Double.NEGATIVE_INFINITY;
        for (int i=0;i<filtered.size();i++) for (int j=i+1;j<filtered.size();j++)
            for (int k=j+1;k<filtered.size();k++) {
                Ball[] set = {filtered.get(i), filtered.get(j), filtered.get(k)};
                int[] colors = {set[0].color,set[1].color,set[2].color};
                Arrays.sort(colors);
                if (expectedColors != null && !Arrays.equals(colors,expectedColors)) continue;
                if (!Arrays.equals(colors,new int[]{1,2,3}) && !Arrays.equals(colors,new int[]{1,1,3})) continue;
                double min = Math.min(set[0].diameter,Math.min(set[1].diameter,set[2].diameter));
                double max = Math.max(set[0].diameter,Math.max(set[1].diameter,set[2].diameter));
                if (max > min * 2.5) continue;
                // Reward substantial ball-shaped color regions and consistency after
                // perspective correction. Small glints must not invalidate a clear set.
                double score = -4 * Math.log(max/min);
                for (Ball b : set) score += Math.log1p(b.pixelArea);
                if (score > bestScore) {
                    secondScore = bestScore;
                    bestScore = score;
                    best = set;
                } else if (score > secondScore) {
                    secondScore = score;
                }
            }
        // If a fourth similarly convincing object makes two sets plausible, remain
        // uncertain rather than guessing and granting a clock reset.
        return best == null || bestScore - secondScore < .85 ? null : best;
    }

    boolean calibrate(int[] rgb, int width, int height) {
        Ball[] balls = detect(rgb,width,height);
        if (balls == null) return false;
        minDiameter = Arrays.stream(balls).mapToDouble(b -> b.diameter).min().orElse(0);
        maxDiameter = Arrays.stream(balls).mapToDouble(b -> b.diameter).max().orElse(0);
        if (maxDiameter > minDiameter*2.5) { minDiameter = maxDiameter = 0; return false; }
        expectedColors = Arrays.stream(balls).mapToInt(b -> b.color).sorted().toArray();
        reference = rgb.clone(); referenceWidth=width; referenceHeight=height;
        return true;
    }

    private boolean sceneObscured(int[] rgb, int width, int height) {
        int total=0, changed=0;
        for (int y=3;y<height;y+=6) for(int x=3;x<width;x+=6) {
            if (!inside((float)x/width,(float)y/height)) continue;
            int before=reference[y*width+x], after=rgb[y*width+x];
            if (color(before)!=0 || color(after)!=0) continue;
            total++;
            int delta=Math.abs(((before>>16)&255)-((after>>16)&255))
                    +Math.abs(((before>>8)&255)-((after>>8)&255))+Math.abs((before&255)-(after&255));
            if(delta>100) changed++;
        }
        return total < 50 || changed > total*.22;
    }

    private static int color(int rgb) {
        int r=(rgb>>16)&255, g=(rgb>>8)&255, b=rgb&255;
        int hi=Math.max(r,Math.max(g,b)), lo=Math.min(r,Math.min(g,b));
        if(lo >= 100 && hi-lo < hi*.30) return 1;
        if(r>105 && g>80 && b<Math.min(r,g)*.70 && r<g*2.3) return 2;
        if(r>65 && r>g*1.5 && r>b*1.35) return 3;
        return 0;
    }
    boolean inside(float x,float y) {
        boolean in=false;
        for(int i=0,j=3;i<4;j=i++) {
            float xi=corners[2*i], yi=corners[2*i+1], xj=corners[2*j], yj=corners[2*j+1];
            if((yi>y)!=(yj>y) && x < (xj-xi)*(y-yi)/(yj-yi)+xi) in=!in;
        }
        return in;
    }
    private double[] project(float x,float y) {
        double d=transform[6]*x+transform[7]*y+1;
        return new double[]{(transform[0]*x+transform[1]*y+transform[2])/d,
                (transform[3]*x+transform[4]*y+transform[5])/d};
    }
    private static double distance(double[] a,double[] b){return Math.hypot(a[0]-b[0],a[1]-b[1]);}
    private static double[] homography(float[] c) {
        // Map the selected cloth quadrilateral to a 1 x 2 table (perspective correction).
        double[][] a=new double[8][9]; double[] dx={0,1,1,0}, dy={0,0,2,2};
        for(int i=0;i<4;i++) {
            double x=c[2*i],y=c[2*i+1],u=dx[i],v=dy[i];
            a[2*i]=new double[]{x,y,1,0,0,0,-u*x,-u*y,u};
            a[2*i+1]=new double[]{0,0,0,x,y,1,-v*x,-v*y,v};
        }
        for(int k=0;k<8;k++) {
            int pivot=k;
            for(int i=k+1;i<8;i++) if(Math.abs(a[i][k])>Math.abs(a[pivot][k])) pivot=i;
            double[] tmp=a[k];a[k]=a[pivot];a[pivot]=tmp;
            if(Math.abs(a[k][k])<1e-9) throw new IllegalArgumentException("Área de mesa inválida");
            double d=a[k][k];for(int j=k;j<=8;j++) a[k][j]/=d;
            for(int i=0;i<8;i++) if(i!=k){double f=a[i][k];for(int j=k;j<=8;j++)a[i][j]-=f*a[k][j];}
        }
        double[] h=new double[8];for(int i=0;i<8;i++)h[i]=a[i][8];return h;
    }
}
