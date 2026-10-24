package com.billarazteca.marcador;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

/** Live, unmirrored analysis preview; coordinates use the same rotated pixels as tracking. */
final class TableCalibrationView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF imageBounds = new RectF();
    private Bitmap bitmap;
    private int[] pixels;
    private int width, height;
    private final float[] corners = new float[8];
    private int count;
    private TableBallDetector candidate;
    private TableBallDetector.Ball[] balls;
    private final Runnable changed;
    private long clearSince;
    String error = "";

    TableCalibrationView(Context context, Runnable changed) {
        super(context); this.changed = changed;
        setContentDescription("Vista de la mesa. Toca sus cuatro esquinas en orden.");
        setLayerType(View.LAYER_TYPE_SOFTWARE,null);
    }
    void frame(int[] rgb, int w, int h, long now) {
        if(bitmap == null || w!=width || h!=height) {
            if(bitmap!=null) bitmap.recycle();
            bitmap=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);
        }
        pixels=rgb; width=w; height=h;
        bitmap.setPixels(rgb,0,w,0,0,w,h);
        balls=candidate==null?null:candidate.detect(rgb,w,h);
        if(balls==null) clearSince=0;
        else if(clearSince==0) clearSince=now;
        changed.run(); invalidate();
    }
    String status() {
        if(count<4) return "Esquina " + (count+1) + "/4: lejana izquierda → lejana derecha → cercana derecha → cercana izquierda";
        if(!error.isEmpty()) return error;
        return balls==null ? "No se distinguen las tres bolas. Marca solo el paño, sin madera ni bandas, y revisa luz/posición."
                : "Tres bolas detectadas. Comprueba que los círculos rodean las bolas reales.";
    }
    boolean hasFrame() { return bitmap != null && pixels != null; }
    boolean canActivate(long now) {
        return candidate != null && pixels != null && balls != null && clearSince != 0
                && now - clearSince >= 2000;
    }
    TableBallDetector calibration(long now) {
        if (!canActivate(now)) return null;
        TableBallDetector detector=new TableBallDetector(corners);
        return detector.calibrate(pixels,width,height)?detector:null;
    }
    void reset() { count=0;candidate=null;balls=null;clearSince=0;error="";changed.run();invalidate(); }
    void release() { if(bitmap!=null){bitmap.recycle();bitmap=null;} pixels=null; }
    @Override public boolean onTouchEvent(MotionEvent event) {
        if(event.getAction()!=MotionEvent.ACTION_UP) return true;
        performClick();
        if(bitmap==null || count>=4 || !imageBounds.contains(event.getX(),event.getY())) return true;
        corners[2*count]=(event.getX()-imageBounds.left)/imageBounds.width();
        corners[2*count+1]=(event.getY()-imageBounds.top)/imageBounds.height();
        count++;
        if(count==4) {
            try {candidate=new TableBallDetector(corners);}
            catch(IllegalArgumentException ex){error=ex.getMessage();}
        }
        changed.run();invalidate();return true;
    }
    @Override public boolean performClick(){super.performClick();return true;}
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);canvas.drawColor(Color.BLACK);
        if(bitmap==null)return;
        float scale=Math.min(getWidth()/(float)width,getHeight()/(float)height);
        float left=(getWidth()-width*scale)/2,top=(getHeight()-height*scale)/2;
        imageBounds.set(left,top,left+width*scale,top+height*scale);
        canvas.drawBitmap(bitmap,null,imageBounds,paint);
        Path path=new Path();
        for(int i=0;i<count;i++) {
            float x=left+corners[2*i]*width*scale,y=top+corners[2*i+1]*height*scale;
            if(i==0)path.moveTo(x,y);else path.lineTo(x,y);
            paint.setStyle(Paint.Style.FILL);paint.setColor(Color.CYAN);canvas.drawCircle(x,y,7,paint);
            paint.setTextSize(22);canvas.drawText(""+(i+1),x+9,y-9,paint);
        }
        if(count==4)path.close();
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(3);paint.setColor(Color.CYAN);canvas.drawPath(path,paint);
        if(balls!=null)for(TableBallDetector.Ball b:balls) {
            paint.setColor(Color.GREEN);
            canvas.drawCircle(left+b.imageX*width*scale,top+b.imageY*height*scale,(b.radius+3)*scale,paint);
        }
        paint.setStyle(Paint.Style.FILL);
    }
}
