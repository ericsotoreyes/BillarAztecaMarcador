package com.billarazteca.marcador;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.Gravity;
import android.text.InputType;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;

public class MainActivity extends Activity {
    private ScoreboardView board;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        hideBars();
        board = new ScoreboardView();
        setContentView(board);
    }

    @Override protected void onResume() {
        super.onResume();
        hideBars();
    }

    private void hideBars() {
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            WindowInsetsController c = getWindow().getInsetsController();
            if (c != null) {
                c.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                c.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            );
        }
    }

    private class ScoreboardView extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Handler handler = new Handler(Looper.getMainLooper());
        private final Deque<State> history = new ArrayDeque<>();

        private String player1 = "Jugador 1";
        private String player2 = "Jugador 2";
        private int score1 = 0, score2 = 0;
        private int innings1 = 1, innings2 = 0;
        private int maxRun1 = 0, maxRun2 = 0;
        private int currentRun = 0;
        private int currentPlayer = 1;
        private int target = 30;
        private int shotTime = 40;
        private int seconds = 40;
        private boolean paused = false;
        private boolean finished = false;
        private long lastTick = System.currentTimeMillis();

        private final RectF minus1 = new RectF(), plus1 = new RectF();
        private final RectF minus2 = new RectF(), plus2 = new RectF();
        private final RectF undo = new RectF(), endTurn = new RectF();
        private final RectF pause = new RectF(), newGame = new RectF();
        private final RectF timer = new RectF();

        private final Runnable ticker = new Runnable() {
            @Override public void run() {
                long now = System.currentTimeMillis();
                if (!paused && !finished && seconds > 0) {
                    long diff = now - lastTick;
                    if (diff >= 1000) {
                        int elapsed = (int)(diff / 1000);
                        seconds = Math.max(0, seconds - elapsed);
                        lastTick += elapsed * 1000L;
                        invalidate();
                    }
                } else {
                    lastTick = now;
                }
                handler.postDelayed(this, 100);
            }
        };

        ScoreboardView() {
            super(MainActivity.this);
            setFocusable(true);
            handler.post(ticker);
        }

        @Override protected void onDetachedFromWindow() {
            handler.removeCallbacks(ticker);
            super.onDetachedFromWindow();
        }

        private int col(String s) { return Color.parseColor(s); }
        private float dp(float n) { return n * getResources().getDisplayMetrics().density; }

        @Override protected void onDraw(Canvas c) {
            super.onDraw(c);
            float w = getWidth(), h = getHeight();
            if (w <= 0 || h <= 0) return;

            int bg = col("#091018");
            int panel = col("#111A24");
            int yellow = col("#FFD500");
            int blue = col("#1267E8");
            int muted = col("#AAB4C0");
            int border = col("#344252");

            c.drawColor(bg);

            float headerH = h * .12f;
            p.setStyle(Paint.Style.FILL);
            p.setColor(yellow);
            c.drawRect(0, 0, w, headerH, p);

            float badgeR = headerH * .31f;
            float badgeX = w * .035f;
            float badgeY = headerH * .5f;
            p.setColor(Color.BLACK);
            c.drawCircle(badgeX, badgeY, badgeR, p);
            drawText(c, "BA", badgeX, badgeY + headerH * .11f, headerH * .31f, yellow, Paint.Align.CENTER);
            drawText(c, "Billar Azteca", badgeX + badgeR + w * .018f, headerH * .67f,
                    headerH * .46f, col("#092A8F"), Paint.Align.LEFT);

            float top = headerH + h * .018f;
            drawText(c, "Mesa 1", w/2f, top + h * .047f, h * .055f, Color.WHITE, Paint.Align.CENTER);
            drawText(c, "Partido a " + target, w/2f, top + h * .086f, h * .026f, muted, Paint.Align.CENTER);

            float margin = w * .024f;
            float gap = w * .017f;
            float centerW = w * .205f;
            float playerW = (w - 2*margin - 2*gap - centerW) / 2f;
            float cardsTop = top + h * .105f;
            float cardsBottom = h * .80f;

            RectF left = new RectF(margin, cardsTop, margin + playerW, cardsBottom);
            RectF mid = new RectF(left.right + gap, cardsTop, left.right + gap + centerW, cardsBottom);
            RectF right = new RectF(mid.right + gap, cardsTop, mid.right + gap + playerW, cardsBottom);

            panel(c, left, panel, currentPlayer == 1 ? yellow : border, currentPlayer == 1 ? dp(4) : dp(2));
            panel(c, right, panel, currentPlayer == 2 ? yellow : border, currentPlayer == 2 ? dp(4) : dp(2));
            panel(c, mid, col("#0D151E"), border, dp(2));

            drawPlayer(c, left, 1, player1, score1, average(score1, innings1), maxRun1, currentPlayer == 1);
            drawPlayer(c, right, 2, player2, score2, average(score2, innings2), maxRun2, currentPlayer == 2);

            float cx = mid.centerX();
            drawText(c, "Entrada", cx, mid.top + h*.05f, h*.026f, muted, Paint.Align.CENTER);
            drawText(c, String.valueOf(Math.max(innings1, innings2)), cx, mid.top + h*.115f,
                    h*.065f, Color.WHITE, Paint.Align.CENTER);

            float r = Math.min(mid.width()*.34f, mid.height()*.24f);
            float cy = mid.top + mid.height()*.54f;
            timer.set(cx-r, cy-r, cx+r, cy+r);

            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(dp(9));
            p.setStrokeCap(Paint.Cap.ROUND);
            p.setColor(col("#293746"));
            c.drawOval(timer, p);
            float frac = shotTime <= 0 ? 0 : seconds/(float)shotTime;
            p.setColor(seconds <= 5 ? col("#E53935") : seconds <= 10 ? col("#FF9F1C") : col("#168BFF"));
            c.drawArc(timer, -90, Math.max(0f, Math.min(1f, frac))*360f, false, p);
            p.setStyle(Paint.Style.FILL);

            drawText(c, String.valueOf(seconds), cx, cy + h*.025f, h*.085f, Color.WHITE, Paint.Align.CENTER);
            drawText(c, paused ? "PAUSA" : seconds == 0 ? "TIEMPO" : "segundos",
                    cx, cy + h*.083f, h*.024f, paused ? yellow : muted, Paint.Align.CENTER);
            drawText(c, "Tocando: " + (currentPlayer == 1 ? player1 : player2),
                    cx, mid.bottom - h*.032f, h*.021f, yellow, Paint.Align.CENTER);

            float y1 = h*.83f, y2 = h*.965f, g = w*.012f;
            float undoW = w*.19f, endW = w*.40f, pauseW = w*.15f;
            float newW = w - 2*margin - undoW - endW - pauseW - 3*g;

            undo.set(margin, y1, margin+undoW, y2);
            endTurn.set(undo.right+g, y1, undo.right+g+endW, y2);
            pause.set(endTurn.right+g, y1, endTurn.right+g+pauseW, y2);
            newGame.set(pause.right+g, y1, pause.right+g+newW, y2);

            button(c, undo, "Deshacer", col("#E7EBF0"), col("#111820"));
            button(c, endTurn, "FIN DE TURNO", blue, Color.WHITE);
            button(c, pause, paused ? "Reanudar" : "Pausa", col("#E7EBF0"), col("#111820"));
            button(c, newGame, "Nuevo partido", col("#E7EBF0"), col("#111820"));
        }

        private void drawPlayer(Canvas c, RectF card, int which, String name, int score, double avg, int maxRun, boolean active) {
            float h = getHeight();
            int yellow = col("#FFD500"), blue = col("#1267E8"), muted = col("#AAB4C0"), border = col("#344252");
            float pad = card.width()*.055f;
            float center = card.centerX();

            drawText(c, name, center, card.top+h*.062f, h*.038f, Color.WHITE, Paint.Align.CENTER);
            if (active) drawText(c, "● EN TURNO", center, card.top+h*.102f, h*.017f, yellow, Paint.Align.CENTER);
            drawText(c, String.valueOf(score), center, card.top+card.height()*.45f, h*.155f, Color.WHITE, Paint.Align.CENTER);

            float statTop = card.top + card.height()*.55f;
            float statBottom = card.top + card.height()*.74f;
            float sg = card.width()*.025f;
            RectF a = new RectF(card.left+pad, statTop, center-sg/2f, statBottom);
            RectF b = new RectF(center+sg/2f, statTop, card.right-pad, statBottom);
            panel(c,a,col("#0A1016"),border,dp(1));
            panel(c,b,col("#0A1016"),border,dp(1));
            drawText(c,"Promedio",a.centerX(),a.top+h*.036f,h*.020f,muted,Paint.Align.CENTER);
            drawText(c,String.format(Locale.US,"%.3f",avg),a.centerX(),a.bottom-h*.022f,h*.036f,yellow,Paint.Align.CENTER);
            drawText(c,"Serie mayor",b.centerX(),b.top+h*.036f,h*.020f,muted,Paint.Align.CENTER);
            drawText(c,String.valueOf(maxRun),b.centerX(),b.bottom-h*.022f,h*.040f,Color.WHITE,Paint.Align.CENTER);

            float bt = card.top+card.height()*.79f, bb = card.bottom-h*.02f;
            RectF minus = new RectF(card.left+pad,bt,center-sg/2f,bb);
            RectF plus = new RectF(center+sg/2f,bt,card.right-pad,bb);
            button(c,minus,"−1",blue,Color.WHITE);
            button(c,plus,"+1",yellow,Color.BLACK);

            if (which==1) { minus1.set(minus); plus1.set(plus); }
            else { minus2.set(minus); plus2.set(plus); }
        }

        private void panel(Canvas c, RectF r, int fill, int stroke, float sw) {
            p.setStyle(Paint.Style.FILL);
            p.setColor(fill);
            c.drawRoundRect(r,dp(16),dp(16),p);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(sw);
            p.setColor(stroke);
            c.drawRoundRect(r,dp(16),dp(16),p);
            p.setStyle(Paint.Style.FILL);
        }

        private void button(Canvas c, RectF r, String s, int fill, int text) {
            p.setColor(fill);
            c.drawRoundRect(r,dp(14),dp(14),p);
            drawText(c,s,r.centerX(),r.centerY()+getHeight()*.012f,getHeight()*.032f,text,Paint.Align.CENTER);
        }

        private void drawText(Canvas c, String s, float x, float y, float size, int color, Paint.Align align) {
            p.setStyle(Paint.Style.FILL);
            p.setColor(color);
            p.setTextSize(size);
            p.setTextAlign(align);
            p.setTypeface(android.graphics.Typeface.create("sans",android.graphics.Typeface.BOLD));
            c.drawText(s,x,y,p);
        }

        private double average(int score, int innings) {
            return innings <= 0 ? 0.0 : score/(double)innings;
        }

        @Override public boolean onTouchEvent(MotionEvent e) {
            if (e.getAction()!=MotionEvent.ACTION_UP) return true;
            float x=e.getX(), y=e.getY();

            if (plus1.contains(x,y) && currentPlayer==1 && !finished) { addPoint(1); return true; }
            if (plus2.contains(x,y) && currentPlayer==2 && !finished) { addPoint(2); return true; }
            if (minus1.contains(x,y) && !finished) { subtract(1); return true; }
            if (minus2.contains(x,y) && !finished) { subtract(2); return true; }
            if (endTurn.contains(x,y) && !finished) { finishTurn(); return true; }
            if (pause.contains(x,y)) {
                paused=!paused; lastTick=System.currentTimeMillis(); invalidate(); return true;
            }
            if (undo.contains(x,y)) { undo(); return true; }
            if (newGame.contains(x,y)) { showNewGame(); return true; }
            if (timer.contains(x,y) && !finished) { save(); resetTimer(); return true; }
            return true;
        }

        private void addPoint(int player) {
            save();
            if (player==1) score1++; else score2++;
            currentRun++;
            if (player==1) maxRun1=Math.max(maxRun1,currentRun); else maxRun2=Math.max(maxRun2,currentRun);
            resetTimer();
            int s = player==1 ? score1 : score2;
            if (s>=target) {
                finished=true; paused=true; invalidate(); showWinner(player);
            }
        }

        private void subtract(int player) {
            int s = player==1 ? score1 : score2;
            if (s<=0) return;
            save();
            if (player==1) score1--; else score2--;
            if (player==currentPlayer && currentRun>0) currentRun--;
            invalidate();
        }

        private void finishTurn() {
            save();
            if (currentPlayer==1) {
                maxRun1=Math.max(maxRun1,currentRun);
                currentPlayer=2;
                innings2++;
            } else {
                maxRun2=Math.max(maxRun2,currentRun);
                currentPlayer=1;
                innings1++;
            }
            currentRun=0;
            resetTimer();
        }

        private void resetTimer() {
            seconds=shotTime;
            paused=false;
            lastTick=System.currentTimeMillis();
            invalidate();
        }

        private void save() {
            history.push(new State(player1,player2,score1,score2,innings1,innings2,maxRun1,maxRun2,currentRun,
                    currentPlayer,target,shotTime,seconds,paused,finished));
            while (history.size()>50) history.removeLast();
        }

        private void undo() {
            if (history.isEmpty()) return;
            State s=history.pop();
            player1=s.player1; player2=s.player2;
            score1=s.score1; score2=s.score2;
            innings1=s.innings1; innings2=s.innings2;
            maxRun1=s.maxRun1; maxRun2=s.maxRun2;
            currentRun=s.currentRun; currentPlayer=s.currentPlayer;
            target=s.target; shotTime=s.shotTime; seconds=s.seconds;
            paused=s.paused; finished=s.finished;
            lastTick=System.currentTimeMillis();
            invalidate();
        }

        private void showWinner(int player) {
            String name = player==1 ? player1 : player2;
            new AlertDialog.Builder(MainActivity.this)
                .setTitle("Distancia alcanzada")
                .setMessage(name+" llegó a "+target+" puntos.\n\nMarcador: "+score1+" - "+score2)
                .setPositiveButton("Cerrar",null)
                .setNeutralButton("Continuar",(d,w)->{ finished=false; paused=false; resetTimer(); })
                .setNegativeButton("Nuevo partido",(d,w)->showNewGame())
                .show();
        }

        private EditText edit(String value, int type) {
            EditText e=new EditText(MainActivity.this);
            e.setText(value);
            e.setTextSize(19);
            e.setSingleLine(true);
            e.setInputType(type);
            e.setPadding((int)dp(10),(int)dp(7),(int)dp(10),(int)dp(7));
            return e;
        }

        private TextView label(String s) {
            TextView t=new TextView(MainActivity.this);
            t.setText(s);
            t.setTextSize(15);
            t.setTextColor(Color.DKGRAY);
            t.setPadding(0,(int)dp(8),0,0);
            return t;
        }

        private void showNewGame() {
            LinearLayout box=new LinearLayout(MainActivity.this);
            box.setOrientation(LinearLayout.VERTICAL);
            box.setPadding((int)dp(22),(int)dp(8),(int)dp(22),0);
            box.setGravity(Gravity.CENTER_HORIZONTAL);

            EditText p1=edit(player1,InputType.TYPE_CLASS_TEXT);
            EditText p2=edit(player2,InputType.TYPE_CLASS_TEXT);
            EditText tg=edit(String.valueOf(target),InputType.TYPE_CLASS_NUMBER);
            EditText st=edit(String.valueOf(shotTime),InputType.TYPE_CLASS_NUMBER);

            box.addView(label("Jugador 1")); box.addView(p1);
            box.addView(label("Jugador 2")); box.addView(p2);
            box.addView(label("Distancia del partido")); box.addView(tg);
            box.addView(label("Segundos por tiro")); box.addView(st);

            AlertDialog d=new AlertDialog.Builder(MainActivity.this)
                .setTitle("Nuevo partido")
                .setView(box)
                .setNegativeButton("Cancelar",null)
                .setPositiveButton("Iniciar",null)
                .create();

            d.setOnShowListener(v->d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v2->{
                String n1=p1.getText().toString().trim();
                String n2=p2.getText().toString().trim();
                int nt=30, ns=40;
                try { nt=Integer.parseInt(tg.getText().toString().trim()); } catch(Exception ignored) {}
                try { ns=Integer.parseInt(st.getText().toString().trim()); } catch(Exception ignored) {}
                nt=Math.max(1,Math.min(999,nt));
                ns=Math.max(5,Math.min(300,ns));
                if (n1.isEmpty()) n1="Jugador 1";
                if (n2.isEmpty()) n2="Jugador 2";

                player1=n1; player2=n2; target=nt; shotTime=ns;
                score1=0; score2=0; innings1=1; innings2=0;
                maxRun1=0; maxRun2=0; currentRun=0; currentPlayer=1;
                finished=false; history.clear(); resetTimer(); d.dismiss();
            }));
            d.show();
        }
    }

    private static class State {
        final String player1,player2;
        final int score1,score2,innings1,innings2,maxRun1,maxRun2,currentRun,currentPlayer,target,shotTime,seconds;
        final boolean paused,finished;
        State(String p1,String p2,int s1,int s2,int i1,int i2,int m1,int m2,int run,int cp,int target,int shot,int sec,boolean paused,boolean finished) {
            this.player1=p1; this.player2=p2; this.score1=s1; this.score2=s2;
            this.innings1=i1; this.innings2=i2; this.maxRun1=m1; this.maxRun2=m2;
            this.currentRun=run; this.currentPlayer=cp; this.target=target; this.shotTime=shot;
            this.seconds=sec; this.paused=paused; this.finished=finished;
        }
    }
}
