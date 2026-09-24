package com.billarazteca.marcador;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Locale;

public class MainActivity extends Activity {

    private final Handler handler = new Handler(Looper.getMainLooper());

    private TextView score1View, score2View, avg1View, avg2View, run1View, run2View;
    private TextView timerView, inningView, activeView;
    private Button pauseButton;

    private int score1 = 0;
    private int score2 = 0;
    private int innings1 = 1;
    private int innings2 = 0;
    private int maxRun1 = 0;
    private int maxRun2 = 0;
    private int currentRun = 0;
    private int currentPlayer = 1;
    private int target = 30;
    private int shotSeconds = 40;
    private int secondsLeft = 40;
    private boolean paused = false;

    private String player1 = "Jugador 1";
    private String player2 = "Jugador 2";

    private long lastTick = 0L;

    private static final int YELLOW = Color.rgb(255, 213, 0);
    private static final int BLUE = Color.rgb(18, 103, 232);
    private static final int BG = Color.rgb(9, 16, 24);
    private static final int PANEL = Color.rgb(17, 26, 36);
    private static final int MUTED = Color.rgb(180, 188, 198);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        try {
            setContentView(buildUi());
            lastTick = System.currentTimeMillis();
            handler.post(timerRunnable);
            refresh();
        } catch (Throwable t) {
            showFallback(t);
        }
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(timerRunnable);
        super.onDestroy();
    }

    private View buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        root.setPadding(dp(14), dp(10), dp(14), dp(10));

        TextView brand = new TextView(this);
        brand.setText("BILLAR AZTECA");
        brand.setTextColor(Color.rgb(8, 42, 143));
        brand.setTextSize(28);
        brand.setTypeface(Typeface.DEFAULT_BOLD);
        brand.setGravity(Gravity.CENTER_VERTICAL);
        brand.setPadding(dp(18), 0, 0, 0);
        brand.setBackgroundColor(YELLOW);
        root.addView(brand, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(58)));

        TextView match = new TextView(this);
        match.setText("Mesa 1   •   Partido a 30");
        match.setTextColor(Color.WHITE);
        match.setTextSize(20);
        match.setGravity(Gravity.CENTER);
        match.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(match, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(44)));

        LinearLayout game = new LinearLayout(this);
        game.setOrientation(LinearLayout.HORIZONTAL);
        root.addView(game, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout p1 = playerPanel(true);
        LinearLayout center = centerPanel();
        LinearLayout p2 = playerPanel(false);

        LinearLayout.LayoutParams playerParams = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.MATCH_PARENT, 1f);
        playerParams.setMargins(dp(4), 0, dp(4), 0);

        LinearLayout.LayoutParams centerParams = new LinearLayout.LayoutParams(dp(210),
                LinearLayout.LayoutParams.MATCH_PARENT);
        centerParams.setMargins(dp(5), 0, dp(5), 0);

        game.addView(p1, playerParams);
        game.addView(center, centerParams);
        game.addView(p2, playerParams);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setPadding(0, dp(8), 0, 0);

        Button endTurn = actionButton("FIN DE TURNO", BLUE, Color.WHITE);
        endTurn.setOnClickListener(v -> finishTurn());

        pauseButton = actionButton("Pausa", Color.LTGRAY, Color.BLACK);
        pauseButton.setOnClickListener(v -> {
            paused = !paused;
            lastTick = System.currentTimeMillis();
            refresh();
        });

        Button resetTime = actionButton("Reiniciar reloj", Color.LTGRAY, Color.BLACK);
        resetTime.setOnClickListener(v -> resetTimer());

        Button newGame = actionButton("Nuevo partido", Color.LTGRAY, Color.BLACK);
        newGame.setOnClickListener(v -> showNewGameDialog());

        actions.addView(endTurn, weighted(2f));
        actions.addView(pauseButton, weighted(1f));
        actions.addView(resetTime, weighted(1.2f));
        actions.addView(newGame, weighted(1.2f));

        root.addView(actions, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(64)));

        return root;
    }

    private LinearLayout playerPanel(boolean first) {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(12), dp(10), dp(12), dp(10));
        panel.setGravity(Gravity.CENTER_HORIZONTAL);
        panel.setBackground(rounded(PANEL, Color.rgb(55, 67, 82), 2));

        TextView name = text(first ? player1 : player2, 22, Color.WHITE, true);
        panel.addView(name, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(42)));

        TextView score = text("0", 82, Color.WHITE, true);
        score.setGravity(Gravity.CENTER);
        panel.addView(score, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout stats = new LinearLayout(this);
        stats.setOrientation(LinearLayout.HORIZONTAL);

        LinearLayout avgBox = statBox("Promedio");
        LinearLayout runBox = statBox("Serie mayor");

        TextView avgValue = (TextView) avgBox.getChildAt(1);
        TextView runValue = (TextView) runBox.getChildAt(1);

        stats.addView(avgBox, weighted(1f));
        stats.addView(runBox, weighted(1f));
        panel.addView(stats, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(90)));

        LinearLayout scoreButtons = new LinearLayout(this);
        scoreButtons.setOrientation(LinearLayout.HORIZONTAL);

        Button minus = actionButton("−1", BLUE, Color.WHITE);
        Button plus = actionButton("+1", YELLOW, Color.BLACK);

        if (first) {
            minus.setOnClickListener(v -> { if (score1 > 0) { score1--; if (currentPlayer == 1 && currentRun > 0) currentRun--; refresh(); } });
            plus.setOnClickListener(v -> { if (currentPlayer == 1) addPoint(1); });
            score1View = score;
            avg1View = avgValue;
            run1View = runValue;
        } else {
            minus.setOnClickListener(v -> { if (score2 > 0) { score2--; if (currentPlayer == 2 && currentRun > 0) currentRun--; refresh(); } });
            plus.setOnClickListener(v -> { if (currentPlayer == 2) addPoint(2); });
            score2View = score;
            avg2View = avgValue;
            run2View = runValue;
        }

        scoreButtons.addView(minus, weighted(1f));
        scoreButtons.addView(plus, weighted(1f));

        panel.addView(scoreButtons, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(70)));

        return panel;
    }

    private LinearLayout centerPanel() {
        LinearLayout center = new LinearLayout(this);
        center.setOrientation(LinearLayout.VERTICAL);
        center.setGravity(Gravity.CENTER);
        center.setPadding(dp(8), dp(10), dp(8), dp(10));
        center.setBackground(rounded(Color.rgb(13, 21, 30), Color.rgb(55, 67, 82), 2));

        TextView entryLabel = text("Entrada", 17, MUTED, false);
        entryLabel.setGravity(Gravity.CENTER);
        center.addView(entryLabel, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(30)));

        inningView = text("1", 38, Color.WHITE, true);
        inningView.setGravity(Gravity.CENTER);
        center.addView(inningView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(50)));

        timerView = text("40", 76, Color.WHITE, true);
        timerView.setGravity(Gravity.CENTER);
        timerView.setBackground(rounded(Color.rgb(6, 11, 17), BLUE, 4));
        timerView.setOnClickListener(v -> resetTimer());
        center.addView(timerView, new LinearLayout.LayoutParams(dp(160), dp(160)));

        TextView sec = text("segundos", 17, MUTED, false);
        sec.setGravity(Gravity.CENTER);
        center.addView(sec, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(34)));

        activeView = text("En turno: Jugador 1", 15, YELLOW, true);
        activeView.setGravity(Gravity.CENTER);
        center.addView(activeView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(52)));

        return center;
    }

    private LinearLayout statBox(String labelText) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(dp(4), dp(4), dp(4), dp(4));
        box.setBackground(rounded(Color.rgb(9, 15, 22), Color.rgb(55, 67, 82), 1));

        TextView label = text(labelText, 15, MUTED, false);
        label.setGravity(Gravity.CENTER);
        TextView value = text("0", 28, YELLOW, true);
        value.setGravity(Gravity.CENTER);

        box.addView(label, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        box.addView(value, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.4f));
        return box;
    }

    private Button actionButton(String label, int bg, int fg) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(17);
        b.setTextColor(fg);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setAllCaps(false);
        b.setBackground(rounded(bg, bg, 0));
        b.setPadding(dp(8), 0, dp(8), 0);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        b.setLayoutParams(lp);
        return b;
    }

    private LinearLayout.LayoutParams weighted(float weight) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.MATCH_PARENT, weight);
        lp.setMargins(dp(4), dp(3), dp(4), dp(3));
        return lp;
    }

    private TextView text(String value, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    private GradientDrawable rounded(int fill, int stroke, int strokeDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(dp(16));
        if (strokeDp > 0) d.setStroke(dp(strokeDp), stroke);
        return d;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void addPoint(int player) {
        if (player != currentPlayer) return;

        if (player == 1) {
            score1++;
            currentRun++;
            if (currentRun > maxRun1) maxRun1 = currentRun;
        } else {
            score2++;
            currentRun++;
            if (currentRun > maxRun2) maxRun2 = currentRun;
        }

        resetTimer();

        if ((player == 1 ? score1 : score2) >= target) {
            paused = true;
            refresh();
            String winner = player == 1 ? player1 : player2;
            new AlertDialog.Builder(this)
                    .setTitle("Partido terminado")
                    .setMessage(winner + " alcanzó " + target + " puntos.")
                    .setPositiveButton("Aceptar", null)
                    .show();
        }
    }

    private void finishTurn() {
        currentRun = 0;

        if (currentPlayer == 1) {
            currentPlayer = 2;
            innings2++;
        } else {
            currentPlayer = 1;
            innings1++;
        }

        resetTimer();
    }

    private void resetTimer() {
        secondsLeft = shotSeconds;
        paused = false;
        lastTick = System.currentTimeMillis();
        refresh();
    }

    private void refresh() {
        if (score1View == null) return;

        score1View.setText(String.valueOf(score1));
        score2View.setText(String.valueOf(score2));

        avg1View.setText(String.format(Locale.US, "%.3f", innings1 > 0 ? score1 / (double) innings1 : 0.0));
        avg2View.setText(String.format(Locale.US, "%.3f", innings2 > 0 ? score2 / (double) innings2 : 0.0));

        run1View.setText(String.valueOf(maxRun1));
        run2View.setText(String.valueOf(maxRun2));

        inningView.setText(String.valueOf(Math.max(innings1, innings2)));
        timerView.setText(String.valueOf(secondsLeft));

        if (secondsLeft <= 5) {
            timerView.setTextColor(Color.rgb(235, 66, 66));
        } else if (secondsLeft <= 10) {
            timerView.setTextColor(Color.rgb(255, 159, 28));
        } else {
            timerView.setTextColor(Color.WHITE);
        }

        activeView.setText("En turno: " + (currentPlayer == 1 ? player1 : player2));
        pauseButton.setText(paused ? "Reanudar" : "Pausa");
    }

    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                long now = System.currentTimeMillis();
                if (!paused && secondsLeft > 0 && lastTick > 0) {
                    long elapsed = now - lastTick;
                    if (elapsed >= 1000L) {
                        int steps = (int) (elapsed / 1000L);
                        secondsLeft = Math.max(0, secondsLeft - steps);
                        lastTick += steps * 1000L;
                        refresh();
                    }
                } else {
                    lastTick = now;
                }
            } catch (Throwable ignored) {
            }
            handler.postDelayed(this, 100L);
        }
    };

    private void showNewGameDialog() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(8), dp(20), 0);

        EditText p1 = field(player1, false);
        EditText p2 = field(player2, false);
        EditText distance = field(String.valueOf(target), true);
        EditText time = field(String.valueOf(shotSeconds), true);

        box.addView(label("Jugador 1"));
        box.addView(p1);
        box.addView(label("Jugador 2"));
        box.addView(p2);
        box.addView(label("Distancia"));
        box.addView(distance);
        box.addView(label("Segundos por tiro"));
        box.addView(time);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Nuevo partido")
                .setView(box)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Iniciar", null)
                .create();

        dialog.setOnShowListener(v -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v2 -> {
            try {
                String n1 = p1.getText().toString().trim();
                String n2 = p2.getText().toString().trim();
                int d = Integer.parseInt(distance.getText().toString().trim());
                int s = Integer.parseInt(time.getText().toString().trim());

                player1 = n1.isEmpty() ? "Jugador 1" : n1;
                player2 = n2.isEmpty() ? "Jugador 2" : n2;
                target = Math.max(1, Math.min(999, d));
                shotSeconds = Math.max(5, Math.min(300, s));

                score1 = score2 = 0;
                innings1 = 1;
                innings2 = 0;
                maxRun1 = maxRun2 = 0;
                currentRun = 0;
                currentPlayer = 1;
                secondsLeft = shotSeconds;
                paused = false;
                lastTick = System.currentTimeMillis();
                refresh();
                dialog.dismiss();
            } catch (Throwable ignored) {
            }
        }));

        dialog.show();
    }

    private EditText field(String value, boolean numeric) {
        EditText e = new EditText(this);
        e.setText(value);
        e.setSingleLine(true);
        e.setTextSize(18);
        if (numeric) e.setInputType(InputType.TYPE_CLASS_NUMBER);
        return e;
    }

    private TextView label(String value) {
        TextView t = text(value, 14, Color.DKGRAY, false);
        t.setPadding(0, dp(6), 0, 0);
        return t;
    }

    private void showFallback(Throwable error) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(20), dp(20), dp(20));
        box.setBackgroundColor(Color.WHITE);

        TextView title = text("Billar Azteca Marcador", 24, Color.BLACK, true);
        TextView msg = text("La interfaz principal no pudo iniciar.\n\n" + error.getClass().getSimpleName() + ": " + String.valueOf(error.getMessage()),
                16, Color.DKGRAY, false);

        box.addView(title);
        box.addView(msg);
        setContentView(box);
    }
}
