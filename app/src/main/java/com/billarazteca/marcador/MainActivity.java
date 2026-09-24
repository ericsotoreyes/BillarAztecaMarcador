package com.billarazteca.marcador;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.pm.ActivityInfo;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final int YELLOW = Color.rgb(255, 213, 0);
    private static final int YELLOW_SOFT = Color.rgb(255, 226, 55);
    private static final int BLUE = Color.rgb(18, 103, 232);
    private static final int BRAND_BLUE = Color.rgb(10, 43, 145);
    private static final int BG_TOP = Color.rgb(9, 16, 24);
    private static final int BG_BOTTOM = Color.rgb(14, 25, 36);
    private static final int PANEL = Color.rgb(15, 24, 34);
    private static final int PANEL_DARK = Color.rgb(8, 14, 21);
    private static final int BORDER = Color.rgb(53, 68, 84);
    private static final int MUTED = Color.rgb(174, 185, 198);
    private static final int ORANGE = Color.rgb(255, 151, 28);
    private static final int RED = Color.rgb(236, 64, 64);

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Deque<State> history = new ArrayDeque<>();

    private LinearLayout panel1;
    private LinearLayout panel2;
    private TextView name1View;
    private TextView name2View;
    private TextView active1View;
    private TextView active2View;
    private TextView score1View;
    private TextView score2View;
    private TextView avg1View;
    private TextView avg2View;
    private TextView run1View;
    private TextView run2View;
    private TextView inningView;
    private TextView activeView;
    private TextView matchView;
    private TimerRingView timerView;
    private Button pauseButton;
    private Button undoButton;

    private String player1 = "Jugador 1";
    private String player2 = "Jugador 2";
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
    private long lastTick = 0L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        enableFullscreen();

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
    protected void onResume() {
        super.onResume();
        enableFullscreen();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) enableFullscreen();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(timerRunnable);
        super.onDestroy();
    }

    private void enableFullscreen() {
        try {
            getWindow().setFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN
            );
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            );
        } catch (Throwable ignored) {
        }
    }

    private View buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(8), dp(5), dp(8), dp(5));
        root.setBackground(new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{BG_TOP, BG_BOTTOM}
        ));

        root.addView(buildHeader(), new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(44)));

        root.addView(buildMatchHeader(), new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(40)));

        LinearLayout gameRow = new LinearLayout(this);
        gameRow.setOrientation(LinearLayout.HORIZONTAL);
        gameRow.setPadding(0, dp(4), 0, dp(3));

        panel1 = playerPanel(true);
        LinearLayout center = centerPanel();
        panel2 = playerPanel(false);

        LinearLayout.LayoutParams playerParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.MATCH_PARENT, 1f);
        playerParams.setMargins(dp(3), 0, dp(3), 0);

        LinearLayout.LayoutParams centerParams = new LinearLayout.LayoutParams(
                dp(178), LinearLayout.LayoutParams.MATCH_PARENT);
        centerParams.setMargins(dp(4), 0, dp(4), 0);

        gameRow.addView(panel1, playerParams);
        gameRow.addView(center, centerParams);
        gameRow.addView(panel2, playerParams);

        root.addView(gameRow, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        root.addView(buildActions(), new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(54)));

        return root;
    }

    private View buildHeader() {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(8), dp(2), dp(10), dp(2));
        header.setBackground(new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{YELLOW_SOFT, YELLOW}
        ));

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.billar_azteca_logo);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        header.addView(logo, new LinearLayout.LayoutParams(dp(39), dp(39)));

        TextView title = text("Billar Azteca Club", 23, BRAND_BLUE, true);
        title.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.MATCH_PARENT, 1f);
        titleParams.setMargins(dp(9), 0, 0, 0);
        header.addView(title, titleParams);

        TextView badge = text("MARCADOR", 11, Color.BLACK, true);
        badge.setGravity(Gravity.CENTER);
        badge.setBackground(roundRect(Color.argb(32, 0, 0, 0), Color.argb(55, 0, 0, 0), 1, 12));
        header.addView(badge, new LinearLayout.LayoutParams(dp(82), dp(27)));

        return header;
    }

    private View buildMatchHeader() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(0, dp(1), 0, 0);

        TextView mesa = text("Mesa 1", 22, Color.WHITE, true);
        mesa.setGravity(Gravity.CENTER);
        matchView = text("Partido a 30", 13, MUTED, false);
        matchView.setGravity(Gravity.CENTER);

        box.addView(mesa, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.25f));
        box.addView(matchView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, .75f));

        return box;
    }

    private LinearLayout playerPanel(boolean first) {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(9), dp(6), dp(9), dp(7));
        panel.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView name = text(first ? player1 : player2, 19, Color.WHITE, true);
        name.setGravity(Gravity.CENTER);
        panel.addView(name, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(28)));

        TextView active = text("● EN TURNO", 10, YELLOW, true);
        active.setGravity(Gravity.CENTER);
        panel.addView(active, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(16)));

        TextView score = text("0", 62, Color.WHITE, true);
        score.setGravity(Gravity.CENTER);
        panel.addView(score, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout stats = new LinearLayout(this);
        stats.setOrientation(LinearLayout.HORIZONTAL);

        LinearLayout avgBox = statBox("Promedio");
        LinearLayout runBox = statBox("Serie mayor");
        TextView avg = (TextView) avgBox.getChildAt(1);
        TextView run = (TextView) runBox.getChildAt(1);

        LinearLayout.LayoutParams statParams1 = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.MATCH_PARENT, 1f);
        statParams1.setMargins(0, 0, dp(3), 0);
        LinearLayout.LayoutParams statParams2 = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.MATCH_PARENT, 1f);
        statParams2.setMargins(dp(3), 0, 0, 0);

        stats.addView(avgBox, statParams1);
        stats.addView(runBox, statParams2);

        panel.addView(stats, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(54)));

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);

        Button minus = styledButton("−1", BLUE, Color.WHITE, 24);
        Button plus = styledButton("+1", YELLOW, Color.BLACK, 24);

        LinearLayout.LayoutParams leftButton = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.MATCH_PARENT, 1f);
        leftButton.setMargins(0, dp(4), dp(3), 0);
        LinearLayout.LayoutParams rightButton = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.MATCH_PARENT, 1f);
        rightButton.setMargins(dp(3), dp(4), 0, 0);

        buttons.addView(minus, leftButton);
        buttons.addView(plus, rightButton);
        panel.addView(buttons, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(48)));

        if (first) {
            name1View = name;
            active1View = active;
            score1View = score;
            avg1View = avg;
            run1View = run;
            minus.setOnClickListener(v -> subtractPoint(1));
            plus.setOnClickListener(v -> addPoint(1));
        } else {
            name2View = name;
            active2View = active;
            score2View = score;
            avg2View = avg;
            run2View = run;
            minus.setOnClickListener(v -> subtractPoint(2));
            plus.setOnClickListener(v -> addPoint(2));
        }

        return panel;
    }

    private LinearLayout statBox(String labelText) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(dp(4), dp(3), dp(4), dp(3));
        box.setBackground(roundRect(PANEL_DARK, BORDER, 1, 12));

        TextView label = text(labelText, 12, MUTED, false);
        label.setGravity(Gravity.CENTER);

        TextView value = text("0", 25, YELLOW, true);
        value.setGravity(Gravity.CENTER);

        box.addView(label, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, .8f));
        box.addView(value, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.2f));

        return box;
    }

    private LinearLayout centerPanel() {
        LinearLayout center = new LinearLayout(this);
        center.setOrientation(LinearLayout.VERTICAL);
        center.setGravity(Gravity.CENTER_HORIZONTAL);
        center.setPadding(dp(7), dp(6), dp(7), dp(6));
        center.setBackground(roundRect(Color.rgb(12, 20, 29), BORDER, 1, 15));

        LinearLayout entry = new LinearLayout(this);
        entry.setOrientation(LinearLayout.VERTICAL);
        entry.setGravity(Gravity.CENTER);
        entry.setBackground(roundRect(PANEL_DARK, BORDER, 1, 11));

        TextView entryLabel = text("Entrada", 12, MUTED, false);
        entryLabel.setGravity(Gravity.CENTER);

        inningView = text("1", 27, Color.WHITE, true);
        inningView.setGravity(Gravity.CENTER);

        entry.addView(entryLabel, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, .7f));
        entry.addView(inningView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.3f));

        center.addView(entry, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(53)));

        timerView = new TimerRingView();
        LinearLayout.LayoutParams timerParams = new LinearLayout.LayoutParams(dp(121), dp(121));
        timerParams.gravity = Gravity.CENTER_HORIZONTAL;
        timerParams.setMargins(0, dp(5), 0, 0);
        center.addView(timerView, timerParams);
        timerView.setOnClickListener(v -> resetTimerManual());

        activeView = text("En turno: Jugador 1", 12, YELLOW, true);
        activeView.setGravity(Gravity.CENTER);
        center.addView(activeView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        return center;
    }

    private View buildActions() {
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setPadding(0, dp(4), 0, 0);

        undoButton = styledButton("↶  Deshacer", Color.rgb(232, 236, 241), Color.rgb(14, 21, 29), 14);
        Button endTurn = styledButton("FIN DE TURNO", BLUE, Color.WHITE, 19);
        pauseButton = styledButton("Pausa", Color.rgb(232, 236, 241), Color.rgb(14, 21, 29), 14);
        Button reset = styledButton("Reiniciar reloj", Color.rgb(232, 236, 241), Color.rgb(14, 21, 29), 13);
        Button newGame = styledButton("Nuevo partido", Color.rgb(232, 236, 241), Color.rgb(14, 21, 29), 13);

        undoButton.setOnClickListener(v -> undo());
        endTurn.setOnClickListener(v -> finishTurn());
        pauseButton.setOnClickListener(v -> togglePause());
        reset.setOnClickListener(v -> resetTimerManual());
        newGame.setOnClickListener(v -> showNewGameDialog());

        actions.addView(undoButton, actionParams(1.0f));
        actions.addView(endTurn, actionParams(2.15f));
        actions.addView(pauseButton, actionParams(.85f));
        actions.addView(reset, actionParams(1.12f));
        actions.addView(newGame, actionParams(1.05f));

        return actions;
    }

    private LinearLayout.LayoutParams actionParams(float weight) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.MATCH_PARENT, weight);
        p.setMargins(dp(3), 0, dp(3), 0);
        return p;
    }

    private Button styledButton(String label, int fill, int textColor, int textSp) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(textSp);
        b.setTextColor(textColor);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setAllCaps(false);
        b.setIncludeFontPadding(false);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(4), 0, dp(4), 0);
        b.setMinHeight(0);
        b.setMinimumHeight(0);
        b.setBackground(roundRect(fill, Color.argb(50, 255, 255, 255), 1, 12));
        try {
            b.setStateListAnimator(null);
        } catch (Throwable ignored) {
        }
        return b;
    }

    private TextView text(String value, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setIncludeFontPadding(false);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    private GradientDrawable roundRect(int fill, int stroke, int strokeDp, int radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(dp(radiusDp));
        if (strokeDp > 0) d.setStroke(dp(strokeDp), stroke);
        return d;
    }

    private GradientDrawable playerBackground(boolean active) {
        GradientDrawable d = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{Color.rgb(16, 25, 35), Color.rgb(9, 15, 22)}
        );
        d.setCornerRadius(dp(15));
        d.setStroke(dp(active ? 4 : 1), active ? YELLOW : BORDER);
        return d;
    }

    private void refresh() {
        if (score1View == null) return;

        name1View.setText(player1);
        name2View.setText(player2);
        score1View.setText(String.valueOf(score1));
        score2View.setText(String.valueOf(score2));

        avg1View.setText(String.format(Locale.US, "%.3f", innings1 > 0 ? score1 / (double) innings1 : 0.0));
        avg2View.setText(String.format(Locale.US, "%.3f", innings2 > 0 ? score2 / (double) innings2 : 0.0));

        run1View.setText(String.valueOf(maxRun1));
        run2View.setText(String.valueOf(maxRun2));

        inningView.setText(String.valueOf(Math.max(innings1, innings2)));
        matchView.setText("Partido a " + target);

        panel1.setBackground(playerBackground(currentPlayer == 1));
        panel2.setBackground(playerBackground(currentPlayer == 2));

        active1View.setVisibility(currentPlayer == 1 ? View.VISIBLE : View.INVISIBLE);
        active2View.setVisibility(currentPlayer == 2 ? View.VISIBLE : View.INVISIBLE);

        activeView.setText("En turno: " + (currentPlayer == 1 ? player1 : player2));
        pauseButton.setText(paused ? "Reanudar" : "Pausa");
        undoButton.setEnabled(!history.isEmpty());
        undoButton.setAlpha(history.isEmpty() ? .55f : 1f);

        timerView.setTimer(secondsLeft, shotSeconds, paused);
    }

    private void addPoint(int player) {
        if (player != currentPlayer) return;
        pushState();

        if (player == 1) {
            score1++;
            currentRun++;
            maxRun1 = Math.max(maxRun1, currentRun);
        } else {
            score2++;
            currentRun++;
            maxRun2 = Math.max(maxRun2, currentRun);
        }

        resetTimerInternal();

        if ((player == 1 ? score1 : score2) >= target) {
            paused = true;
            refresh();
            showWinner(player);
        }
    }

    private void subtractPoint(int player) {
        if (player == 1 && score1 <= 0) return;
        if (player == 2 && score2 <= 0) return;

        pushState();

        if (player == 1) {
            score1--;
            if (currentPlayer == 1 && currentRun > 0) currentRun--;
        } else {
            score2--;
            if (currentPlayer == 2 && currentRun > 0) currentRun--;
        }

        refresh();
    }

    private void finishTurn() {
        pushState();

        currentRun = 0;
        if (currentPlayer == 1) {
            currentPlayer = 2;
            innings2++;
        } else {
            currentPlayer = 1;
            innings1++;
        }

        resetTimerInternal();
    }

    private void togglePause() {
        paused = !paused;
        lastTick = System.currentTimeMillis();
        refresh();
    }

    private void resetTimerManual() {
        secondsLeft = shotSeconds;
        paused = false;
        lastTick = System.currentTimeMillis();
        refresh();
    }

    private void resetTimerInternal() {
        secondsLeft = shotSeconds;
        paused = false;
        lastTick = System.currentTimeMillis();
        refresh();
    }

    private void pushState() {
        history.push(new State(
                player1, player2,
                score1, score2,
                innings1, innings2,
                maxRun1, maxRun2,
                currentRun, currentPlayer,
                target, shotSeconds,
                secondsLeft, paused
        ));
        while (history.size() > 60) history.removeLast();
    }

    private void undo() {
        if (history.isEmpty()) return;
        State s = history.pop();

        player1 = s.player1;
        player2 = s.player2;
        score1 = s.score1;
        score2 = s.score2;
        innings1 = s.innings1;
        innings2 = s.innings2;
        maxRun1 = s.maxRun1;
        maxRun2 = s.maxRun2;
        currentRun = s.currentRun;
        currentPlayer = s.currentPlayer;
        target = s.target;
        shotSeconds = s.shotSeconds;
        secondsLeft = s.secondsLeft;
        paused = s.paused;
        lastTick = System.currentTimeMillis();

        refresh();
    }

    private void showWinner(int player) {
        String winner = player == 1 ? player1 : player2;
        new AlertDialog.Builder(this)
                .setTitle("Partido terminado")
                .setMessage(winner + " alcanzó " + target + " puntos.\n\nMarcador final: " + score1 + " - " + score2)
                .setPositiveButton("Cerrar", null)
                .setNeutralButton("Continuar", (dialog, which) -> {
                    paused = false;
                    resetTimerInternal();
                })
                .setNegativeButton("Nuevo partido", (dialog, which) -> showNewGameDialog())
                .show();
    }

    private void showNewGameDialog() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(7), dp(20), 0);

        EditText p1 = field(player1, false);
        EditText p2 = field(player2, false);
        EditText distance = field(String.valueOf(target), true);
        EditText time = field(String.valueOf(shotSeconds), true);

        box.addView(dialogLabel("Jugador 1"));
        box.addView(p1);
        box.addView(dialogLabel("Jugador 2"));
        box.addView(p2);
        box.addView(dialogLabel("Distancia del partido"));
        box.addView(distance);
        box.addView(dialogLabel("Segundos por tiro"));
        box.addView(time);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Nuevo partido")
                .setView(box)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Iniciar", null)
                .create();

        dialog.setOnShowListener(v ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v2 -> {
                    try {
                        String n1 = p1.getText().toString().trim();
                        String n2 = p2.getText().toString().trim();
                        int newTarget = Integer.parseInt(distance.getText().toString().trim());
                        int newTime = Integer.parseInt(time.getText().toString().trim());

                        player1 = n1.isEmpty() ? "Jugador 1" : n1;
                        player2 = n2.isEmpty() ? "Jugador 2" : n2;
                        target = Math.max(1, Math.min(999, newTarget));
                        shotSeconds = Math.max(5, Math.min(300, newTime));

                        score1 = 0;
                        score2 = 0;
                        innings1 = 1;
                        innings2 = 0;
                        maxRun1 = 0;
                        maxRun2 = 0;
                        currentRun = 0;
                        currentPlayer = 1;
                        secondsLeft = shotSeconds;
                        paused = false;
                        history.clear();
                        lastTick = System.currentTimeMillis();

                        refresh();
                        dialog.dismiss();
                    } catch (Throwable ignored) {
                    }
                })
        );

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

    private TextView dialogLabel(String value) {
        TextView t = text(value, 14, Color.DKGRAY, false);
        t.setPadding(0, dp(6), 0, 0);
        return t;
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

    private void showFallback(Throwable error) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(20), dp(20), dp(20));
        box.setBackgroundColor(Color.WHITE);

        TextView title = text("Billar Azteca Marcador", 24, Color.BLACK, true);
        TextView msg = text(
                "La interfaz no pudo iniciar.\n\n" +
                        error.getClass().getSimpleName() + ": " +
                        String.valueOf(error.getMessage()),
                15, Color.DKGRAY, false
        );

        box.addView(title);
        box.addView(msg);
        setContentView(box);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private float sp(float value) {
        return value * getResources().getDisplayMetrics().scaledDensity;
    }

    private class TimerRingView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF arc = new RectF();
        private int seconds = 40;
        private int total = 40;
        private boolean isPaused = false;

        TimerRingView() {
            super(MainActivity.this);
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

        void setTimer(int seconds, int total, boolean paused) {
            this.seconds = Math.max(0, seconds);
            this.total = Math.max(1, total);
            this.isPaused = paused;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            float w = getWidth();
            float h = getHeight();
            float size = Math.min(w, h);
            float cx = w / 2f;
            float cy = h / 2f;
            float ring = dp(8);
            float inset = ring + dp(3);

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.rgb(6, 12, 19));
            canvas.drawCircle(cx, cy, size / 2f - dp(2), paint);

            arc.set(inset, inset, w - inset, h - inset);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(ring);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setColor(Color.rgb(42, 56, 72));
            canvas.drawArc(arc, -90, 360, false, paint);

            int ringColor = seconds <= 5 ? RED : (seconds <= 10 ? ORANGE : Color.rgb(21, 143, 255));
            float fraction = Math.min(1f, Math.max(0f, seconds / (float) total));

            paint.setColor(ringColor);
            canvas.drawArc(arc, -90, 360f * fraction, false, paint);

            paint.setStyle(Paint.Style.FILL);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTypeface(Typeface.DEFAULT_BOLD);
            paint.setTextSize(size * .34f);
            paint.setColor(Color.WHITE);

            Paint.FontMetrics fm = paint.getFontMetrics();
            float numberY = cy - fm.ascent / 2f - dp(3);
            canvas.drawText(String.valueOf(seconds), cx, numberY, paint);

            paint.setTypeface(Typeface.DEFAULT);
            paint.setTextSize(size * .115f);
            paint.setColor(isPaused ? YELLOW : MUTED);
            String sub = isPaused ? "PAUSA" : (seconds == 0 ? "TIEMPO" : "segundos");
            canvas.drawText(sub, cx, cy + size * .25f, paint);
        }
    }

    private static class State {
        final String player1;
        final String player2;
        final int score1;
        final int score2;
        final int innings1;
        final int innings2;
        final int maxRun1;
        final int maxRun2;
        final int currentRun;
        final int currentPlayer;
        final int target;
        final int shotSeconds;
        final int secondsLeft;
        final boolean paused;

        State(
                String player1, String player2,
                int score1, int score2,
                int innings1, int innings2,
                int maxRun1, int maxRun2,
                int currentRun, int currentPlayer,
                int target, int shotSeconds,
                int secondsLeft, boolean paused
        ) {
            this.player1 = player1;
            this.player2 = player2;
            this.score1 = score1;
            this.score2 = score2;
            this.innings1 = innings1;
            this.innings2 = innings2;
            this.maxRun1 = maxRun1;
            this.maxRun2 = maxRun2;
            this.currentRun = currentRun;
            this.currentPlayer = currentPlayer;
            this.target = target;
            this.shotSeconds = shotSeconds;
            this.secondsLeft = secondsLeft;
            this.paused = paused;
        }
    }
}
