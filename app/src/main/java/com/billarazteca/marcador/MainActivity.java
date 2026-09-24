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
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

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
    private static final int PANEL_DARK = Color.rgb(8, 14, 21);
    private static final int BORDER = Color.rgb(53, 68, 84);
    private static final int MUTED = Color.rgb(174, 185, 198);
    private static final int ORANGE = Color.rgb(255, 151, 28);
    private static final int RED = Color.rgb(236, 64, 64);
    private static final int LIGHT = Color.rgb(232, 236, 241);

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Deque<State> history = new ArrayDeque<>();

    private LinearLayout panel1;
    private LinearLayout panel2;
    private TextView tableView;
    private TextView matchView;
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
    private TimerRingView timerView;
    private Button pauseButton;
    private Button resetButton;
    private Button undoButton;
    private Button extension1Button;
    private Button extension2Button;

    private ToneGenerator toneGenerator;

    private String tableName = "Mesa 1";
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

    private int extensions1 = 2;
    private int extensions2 = 2;
    private boolean extensionArmed = false;

    private boolean paused = false;
    private boolean gameStarted = false;
    private boolean matchFinished = false;
    private boolean useTimer = true;
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
        if (toneGenerator != null) {
            try {
                toneGenerator.release();
            } catch (Throwable ignored) {
            }
            toneGenerator = null;
        }
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
                LinearLayout.LayoutParams.MATCH_PARENT, dp(50)));

        root.addView(buildMatchHeader(), new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(42)));

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
        header.addView(logo, new LinearLayout.LayoutParams(dp(46), dp(46)));

        TextView title = text("Billar Azteca Club", 23, BRAND_BLUE, true);
        title.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.MATCH_PARENT, 1f);
        titleParams.setMargins(dp(10), 0, 0, 0);
        header.addView(title, titleParams);

        TextView badge = text("MARCADOR", 11, Color.BLACK, true);
        badge.setGravity(Gravity.CENTER);
        badge.setBackground(roundRect(Color.argb(32, 0, 0, 0), Color.argb(55, 0, 0, 0), 1, 14));
        header.addView(badge, new LinearLayout.LayoutParams(dp(82), dp(27)));

        return header;
    }

    private View buildMatchHeader() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(0, dp(2), 0, dp(1));

        tableView = text(tableName, 22, Color.WHITE, true);
        tableView.setGravity(Gravity.CENTER);
        tableView.setOnClickListener(v -> editTableName());

        matchView = text("Partido a " + target, 13, MUTED, false);
        matchView.setGravity(Gravity.CENTER);
        matchView.setOnClickListener(v -> editTarget());

        box.addView(tableView, new LinearLayout.LayoutParams(
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
        name.setSingleLine(true);
        name.setAutoSizeTextTypeUniformWithConfiguration(
                11, 19, 1, TypedValue.COMPLEX_UNIT_SP
        );
        name.setOnClickListener(v -> editPlayerName(first ? 1 : 2));

        panel.addView(name, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(28)));

        LinearLayout statusRow = new LinearLayout(this);
        statusRow.setOrientation(LinearLayout.HORIZONTAL);
        statusRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView active = text("● EN TURNO", 10, YELLOW, true);
        active.setGravity(Gravity.CENTER);

        Button extension = styledButton("Extensión · 2", LIGHT, Color.rgb(14, 21, 29), 10);
        extension.setOnClickListener(v -> requestExtension(first ? 1 : 2));

        LinearLayout.LayoutParams activeParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.MATCH_PARENT, 1f);
        LinearLayout.LayoutParams extParams = new LinearLayout.LayoutParams(
                dp(96), LinearLayout.LayoutParams.MATCH_PARENT);
        extParams.setMargins(dp(4), 0, 0, 0);

        statusRow.addView(active, activeParams);
        statusRow.addView(extension, extParams);

        panel.addView(statusRow, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(26)));

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
            extension1Button = extension;
            score1View = score;
            avg1View = avg;
            run1View = run;
            minus.setOnClickListener(v -> subtractPoint(1));
            plus.setOnClickListener(v -> addPoint(1));
        } else {
            name2View = name;
            active2View = active;
            extension2Button = extension;
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

        timerView.setOnClickListener(v -> {
            if (useTimer) resetTimerManual();
        });
        timerView.setOnLongClickListener(v -> {
            if (useTimer) {
                editShotSeconds();
            }
            return true;
        });

        activeView = text("En turno: Jugador 1", 11, YELLOW, true);
        activeView.setGravity(Gravity.CENTER);
        center.addView(activeView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        return center;
    }

    private View buildActions() {
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setPadding(0, dp(4), 0, 0);

        undoButton = styledButton("↶  Deshacer", Color.rgb(150, 156, 164), Color.rgb(14, 21, 29), 14);
        Button endTurn = styledButton("FIN DE TURNO", BLUE, Color.WHITE, 19);
        pauseButton = styledButton("INICIAR PARTIDA", YELLOW, Color.BLACK, 14);
        resetButton = styledButton("Reiniciar reloj", LIGHT, Color.rgb(14, 21, 29), 13);
        Button newGame = styledButton("Nuevo partido", LIGHT, Color.rgb(14, 21, 29), 13);

        undoButton.setOnClickListener(v -> undo());
        endTurn.setOnClickListener(v -> finishTurn());
        pauseButton.setOnClickListener(v -> handleStartPause());
        resetButton.setOnClickListener(v -> {
            if (useTimer) {
                resetTimerManual();
            }
        });
        newGame.setOnClickListener(v -> requestNewGame());

        actions.addView(undoButton, actionParams(1.0f));
        actions.addView(endTurn, actionParams(2.15f));
        actions.addView(pauseButton, actionParams(.85f));
        actions.addView(resetButton, actionParams(1.12f));
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
        d.setCornerRadius(dp(16));
        d.setStroke(dp(active ? 4 : 1), active ? YELLOW : BORDER);
        return d;
    }

    private void refresh() {
        if (score1View == null) return;

        tableView.setText(tableName);
        matchView.setText("Partido a " + target);

        name1View.setText(player1);
        name2View.setText(player2);

        score1View.setText(String.valueOf(score1));
        score2View.setText(String.valueOf(score2));

        avg1View.setText(String.format(
                Locale.US, "%.3f",
                innings1 > 0 ? score1 / (double) innings1 : 0.0
        ));
        avg2View.setText(String.format(
                Locale.US, "%.3f",
                innings2 > 0 ? score2 / (double) innings2 : 0.0
        ));

        run1View.setText(String.valueOf(maxRun1));
        run2View.setText(String.valueOf(maxRun2));

        inningView.setText(String.valueOf(Math.max(innings1, innings2)));

        panel1.setBackground(playerBackground(currentPlayer == 1));
        panel2.setBackground(playerBackground(currentPlayer == 2));

        active1View.setVisibility(currentPlayer == 1 ? View.VISIBLE : View.INVISIBLE);
        active2View.setVisibility(currentPlayer == 2 ? View.VISIBLE : View.INVISIBLE);

        String currentName = currentPlayer == 1 ? player1 : player2;
        if (matchFinished) {
            activeView.setText("Partida terminada");
        } else if (!gameStarted) {
            activeView.setText("Primer turno: " + currentName);
        } else {
            activeView.setText("En turno: " + currentName);
        }

        updateExtensionButton(extension1Button, 1, extensions1);
        updateExtensionButton(extension2Button, 2, extensions2);

        if (matchFinished) {
            pauseButton.setText("PARTIDA TERMINADA");
            pauseButton.setEnabled(false);
            pauseButton.setTextColor(Color.rgb(90, 95, 102));
            pauseButton.setBackground(roundRect(Color.rgb(190, 194, 200), Color.argb(35, 0, 0, 0), 1, 12));
        } else if (!gameStarted) {
            pauseButton.setText("INICIAR PARTIDA");
            pauseButton.setEnabled(true);
            pauseButton.setTextColor(Color.BLACK);
            pauseButton.setBackground(roundRect(YELLOW, Color.argb(65, 0, 0, 0), 1, 12));
        } else if (!useTimer) {
            pauseButton.setText("PARTIDA EN CURSO");
            pauseButton.setEnabled(false);
            pauseButton.setTextColor(Color.rgb(70, 76, 84));
            pauseButton.setBackground(roundRect(Color.rgb(205, 209, 214), Color.argb(30, 0, 0, 0), 1, 12));
        } else {
            pauseButton.setEnabled(true);
            pauseButton.setText(paused ? "Reanudar" : "Pausa");
            if (paused) {
                pauseButton.setTextColor(Color.BLACK);
                pauseButton.setBackground(roundRect(YELLOW, Color.argb(65, 0, 0, 0), 1, 12));
            } else {
                pauseButton.setTextColor(Color.rgb(14, 21, 29));
                pauseButton.setBackground(roundRect(LIGHT, Color.argb(50, 255, 255, 255), 1, 12));
            }
        }

        resetButton.setEnabled(useTimer && gameStarted && !matchFinished);
        resetButton.setAlpha(useTimer ? 1f : .55f);

        undoButton.setEnabled(!history.isEmpty());
        undoButton.setAlpha(history.isEmpty() ? .55f : 1f);

        timerView.setTimer(
                secondsLeft,
                shotSeconds,
                paused,
                extensionArmed,
                gameStarted,
                matchFinished,
                useTimer
        );
    }

    private void updateExtensionButton(Button button, int player, int remaining) {
        if (button == null) return;

        boolean isCurrent = player == currentPlayer;

        if (!useTimer || !gameStarted || matchFinished) {
            button.setText(useTimer ? "Extensión · " + remaining : "Sin reloj");
            button.setEnabled(false);
            button.setTextColor(Color.rgb(45, 50, 58));
            button.setBackground(roundRect(Color.rgb(205, 209, 214), Color.argb(30, 0, 0, 0), 1, 10));
            button.setAlpha(.65f);
            return;
        }

        boolean armedHere = isCurrent && extensionArmed;

        if (armedHere) {
            button.setText("EXT. ACTIVA");
            button.setEnabled(false);
            button.setTextColor(Color.BLACK);
            button.setBackground(roundRect(ORANGE, Color.argb(70, 0, 0, 0), 1, 10));
            button.setAlpha(1f);
            return;
        }

        if (remaining <= 0) {
            button.setText("Sin ext.");
            button.setEnabled(false);
            button.setTextColor(Color.rgb(80, 85, 92));
            button.setBackground(roundRect(Color.rgb(185, 189, 194), Color.argb(30, 0, 0, 0), 1, 10));
            button.setAlpha(.75f);
            return;
        }

        button.setText("Extensión · " + remaining);
        button.setEnabled(isCurrent);

        if (isCurrent) {
            button.setTextColor(Color.BLACK);
            button.setBackground(roundRect(YELLOW, Color.argb(70, 0, 0, 0), 1, 10));
            button.setAlpha(1f);
        } else {
            button.setTextColor(Color.rgb(45, 50, 58));
            button.setBackground(roundRect(Color.rgb(205, 209, 214), Color.argb(30, 0, 0, 0), 1, 10));
            button.setAlpha(.65f);
        }
    }

    private void addPoint(int player) {
        if (!gameStarted || matchFinished) {
            Toast.makeText(this, "Primero inicia la partida", Toast.LENGTH_SHORT).show();
            return;
        }
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

        extensionArmed = false;

        if ((player == 1 ? score1 : score2) >= target) {
            paused = true;
            gameStarted = false;
            matchFinished = true;
            lastTick = System.currentTimeMillis();
            refresh();
            showWinner(player);
            return;
        }

        resetTimerInternal();
    }

    private void subtractPoint(int player) {
        if (!gameStarted || matchFinished) {
            Toast.makeText(this, "Primero inicia la partida", Toast.LENGTH_SHORT).show();
            return;
        }
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
        if (!gameStarted || matchFinished) {
            Toast.makeText(this, "Primero inicia la partida", Toast.LENGTH_SHORT).show();
            return;
        }
        pushState();

        currentRun = 0;
        extensionArmed = false;

        if (currentPlayer == 1) {
            currentPlayer = 2;
            innings2++;
        } else {
            currentPlayer = 1;
            innings1++;
        }

        resetTimerInternal();
    }

    private void requestExtension(int player) {
        if (!useTimer) {
            Toast.makeText(this, "Esta partida no usa cronómetro", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!gameStarted || matchFinished) {
            Toast.makeText(this, "Primero inicia la partida", Toast.LENGTH_SHORT).show();
            return;
        }
        if (player != currentPlayer) return;

        int remaining = player == 1 ? extensions1 : extensions2;

        if (extensionArmed) {
            Toast.makeText(this, "Ya hay una extensión activada", Toast.LENGTH_SHORT).show();
            return;
        }

        if (remaining <= 0) {
            Toast.makeText(this, "Este jugador ya usó sus 2 extensiones", Toast.LENGTH_SHORT).show();
            return;
        }

        pushState();

        if (player == 1) {
            extensions1--;
        } else {
            extensions2--;
        }

        if (secondsLeft <= 0) {
            secondsLeft = shotSeconds;
            extensionArmed = false;
            paused = false;
            lastTick = System.currentTimeMillis();
            Toast.makeText(this, "Extensión aplicada", Toast.LENGTH_SHORT).show();
        } else {
            extensionArmed = true;
            Toast.makeText(
                    this,
                    "Extensión activada: al llegar a 0 el reloj reinicia una vez",
                    Toast.LENGTH_SHORT
            ).show();
        }

        refresh();
    }

    private void handleStartPause() {
        if (matchFinished) return;

        if (!gameStarted) {
            gameStarted = true;
            paused = false;
            secondsLeft = shotSeconds;
            extensionArmed = false;
            lastTick = System.currentTimeMillis();
            refresh();
            Toast.makeText(
                    this,
                    useTimer ? "Partida iniciada" : "Partida iniciada sin cronómetro",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        if (useTimer) togglePause();
    }

    private void togglePause() {
        paused = !paused;
        lastTick = System.currentTimeMillis();
        refresh();
    }

    private void resetTimerManual() {
        if (!useTimer) return;
        extensionArmed = false;
        secondsLeft = shotSeconds;
        if (gameStarted && !matchFinished) {
            paused = false;
        }
        lastTick = System.currentTimeMillis();
        refresh();
    }

    private void resetTimerInternal() {
        if (!useTimer) {
            paused = false;
            extensionArmed = false;
            refresh();
            return;
        }
        secondsLeft = shotSeconds;
        paused = false;
        lastTick = System.currentTimeMillis();
        refresh();
    }

    private void editTableName() {
        showTextDialog(
                "Nombre de mesa",
                tableName,
                value -> {
                    tableName = value.isEmpty() ? "Mesa 1" : value;
                    refresh();
                }
        );
    }

    private void editPlayerName(int player) {
        String current = player == 1 ? player1 : player2;
        showTextDialog(
                player == 1 ? "Nombre del jugador 1" : "Nombre del jugador 2",
                current,
                value -> {
                    String finalName = value.isEmpty()
                            ? (player == 1 ? "Jugador 1" : "Jugador 2")
                            : value;

                    if (player == 1) player1 = finalName;
                    else player2 = finalName;

                    refresh();
                }
        );
    }

    private void editTarget() {
        showNumberDialog(
                "Distancia del partido",
                target,
                1,
                999,
                value -> {
                    target = value;
                    refresh();
                }
        );
    }

    private void editShotSeconds() {
        if (!useTimer) {
            Toast.makeText(this, "Esta partida está configurada sin cronómetro", Toast.LENGTH_SHORT).show();
            return;
        }
        showNumberDialog(
                "Segundos por tiro",
                shotSeconds,
                5,
                300,
                value -> {
                    shotSeconds = value;
                    extensionArmed = false;
                    resetTimerManual();
                }
        );
    }

    private interface StringConsumer {
        void accept(String value);
    }

    private interface IntConsumer {
        void accept(int value);
    }

    private void showTextDialog(String title, String current, StringConsumer onSave) {
        EditText input = field(current, false);
        input.setSelectAllOnFocus(true);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(input)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Guardar", null)
                .create();

        dialog.setOnShowListener(v ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v2 -> {
                    onSave.accept(input.getText().toString().trim());
                    dialog.dismiss();
                })
        );

        dialog.show();
    }

    private void showNumberDialog(
            String title,
            int current,
            int min,
            int max,
            IntConsumer onSave
    ) {
        EditText input = field(String.valueOf(current), true);
        input.setSelectAllOnFocus(true);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(input)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Guardar", null)
                .create();

        dialog.setOnShowListener(v ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v2 -> {
                    try {
                        int value = Integer.parseInt(input.getText().toString().trim());
                        value = Math.max(min, Math.min(max, value));
                        onSave.accept(value);
                        dialog.dismiss();
                    } catch (Throwable ignored) {
                        Toast.makeText(this, "Escribe un número válido", Toast.LENGTH_SHORT).show();
                    }
                })
        );

        dialog.show();
    }

    private void requestNewGame() {
        boolean hasProgress =
                gameStarted ||
                matchFinished ||
                score1 > 0 ||
                score2 > 0 ||
                innings1 > 1 ||
                innings2 > 0 ||
                extensions1 < 2 ||
                extensions2 < 2;

        if (!hasProgress) {
            showNewGameDialog();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Nuevo partido")
                .setMessage("Se borrará el marcador actual, las entradas y las extensiones utilizadas. ¿Continuar?")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Continuar", (dialog, which) -> showNewGameDialog())
                .show();
    }

    private void showWinner(int player) {
        String winner = player == 1 ? player1 : player2;

        String finalNote = useTimer
                ? "\n\nEl cronómetro quedó detenido."
                : "\n\nPartida jugada sin cronómetro.";

        new AlertDialog.Builder(this)
                .setTitle("Partido terminado")
                .setMessage(
                        winner + " alcanzó " + target + " puntos.\n\n" +
                        "Marcador final: " + score1 + " - " + score2 +
                        finalNote
                )
                .setPositiveButton("Cerrar", null)
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

        CheckBox timerOption = new CheckBox(this);
        timerOption.setText("Usar cronómetro");
        timerOption.setTextSize(16);
        timerOption.setChecked(useTimer);
        timerOption.setPadding(0, dp(8), 0, dp(4));

        time.setEnabled(useTimer);
        time.setAlpha(useTimer ? 1f : .45f);

        timerOption.setOnCheckedChangeListener((buttonView, isChecked) -> {
            time.setEnabled(isChecked);
            time.setAlpha(isChecked ? 1f : .45f);
        });

        box.addView(dialogLabel("Jugador 1"));
        box.addView(p1);
        box.addView(dialogLabel("Jugador 2"));
        box.addView(p2);
        box.addView(dialogLabel("Distancia del partido"));
        box.addView(distance);
        box.addView(timerOption);
        box.addView(dialogLabel("Segundos por tiro"));
        box.addView(time);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Nuevo partido")
                .setView(box)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Preparar", null)
                .create();

        dialog.setOnShowListener(v ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v2 -> {
                    try {
                        String n1 = p1.getText().toString().trim();
                        String n2 = p2.getText().toString().trim();
                        int newTarget = Integer.parseInt(distance.getText().toString().trim());
                        boolean newUseTimer = timerOption.isChecked();
                        int newTime = shotSeconds;
                        if (newUseTimer) {
                            newTime = Integer.parseInt(time.getText().toString().trim());
                        }

                        player1 = n1.isEmpty() ? "Jugador 1" : n1;
                        player2 = n2.isEmpty() ? "Jugador 2" : n2;

                        target = Math.max(1, Math.min(999, newTarget));
                        useTimer = newUseTimer;
                        if (newUseTimer) {
                            shotSeconds = Math.max(5, Math.min(300, newTime));
                        }

                        score1 = 0;
                        score2 = 0;
                        innings1 = 1;
                        innings2 = 0;
                        maxRun1 = 0;
                        maxRun2 = 0;
                        currentRun = 0;
                        currentPlayer = 1;
                        secondsLeft = shotSeconds;

                        extensions1 = 2;
                        extensions2 = 2;
                        extensionArmed = false;

                        paused = false;
                        gameStarted = false;
                        matchFinished = false;
                        history.clear();
                        lastTick = System.currentTimeMillis();

                        refresh();
                        dialog.dismiss();
                    } catch (Throwable ignored) {
                        Toast.makeText(this, "Revisa la distancia y el tiempo", Toast.LENGTH_SHORT).show();
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

    private void pushState() {
        history.push(new State(
                player1, player2,
                score1, score2,
                innings1, innings2,
                maxRun1, maxRun2,
                currentRun, currentPlayer,
                target, shotSeconds,
                secondsLeft, paused,
                gameStarted, matchFinished,
                extensions1, extensions2,
                extensionArmed
        ));

        while (history.size() > 60) {
            history.removeLast();
        }
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
        gameStarted = s.gameStarted;
        matchFinished = s.matchFinished;
        extensions1 = s.extensions1;
        extensions2 = s.extensions2;
        extensionArmed = s.extensionArmed;

        lastTick = System.currentTimeMillis();
        refresh();
    }

    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                long now = System.currentTimeMillis();

                if (useTimer && gameStarted && !matchFinished && !paused && secondsLeft > 0 && lastTick > 0) {
                    long elapsed = now - lastTick;

                    if (elapsed >= 1000L) {
                        int previousSeconds = secondsLeft;
                        int steps = (int) (elapsed / 1000L);
                        secondsLeft = Math.max(0, secondsLeft - steps);
                        lastTick += steps * 1000L;

                        if (previousSeconds > 0 && secondsLeft == 0) {
                            playTimeExpiredBeep();

                            if (extensionArmed) {
                                secondsLeft = shotSeconds;
                                extensionArmed = false;
                                lastTick = now;
                                Toast.makeText(
                                        MainActivity.this,
                                        "Extensión: tiempo reiniciado",
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                        }

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

    private void playTimeExpiredBeep() {
        try {
            if (toneGenerator == null) {
                toneGenerator = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
            }
            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 250);
        } catch (Throwable ignored) {
        }
    }

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

    private class TimerRingView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF arc = new RectF();

        private int seconds = 40;
        private int total = 40;
        private boolean isPaused = false;
        private boolean isExtensionArmed = false;
        private boolean isGameStarted = false;
        private boolean isMatchFinished = false;
        private boolean isTimerEnabled = true;

        TimerRingView() {
            super(MainActivity.this);
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

        void setTimer(
                int seconds,
                int total,
                boolean paused,
                boolean extensionArmed,
                boolean gameStarted,
                boolean matchFinished,
                boolean timerEnabled
        ) {
            this.seconds = Math.max(0, seconds);
            this.total = Math.max(1, total);
            this.isPaused = paused;
            this.isExtensionArmed = extensionArmed;
            this.isGameStarted = gameStarted;
            this.isMatchFinished = matchFinished;
            this.isTimerEnabled = timerEnabled;
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

            if (!isTimerEnabled) {
                paint.setStyle(Paint.Style.FILL);
                paint.setTextAlign(Paint.Align.CENTER);
                paint.setTypeface(Typeface.DEFAULT_BOLD);
                paint.setTextSize(size * .20f);
                paint.setColor(Color.WHITE);
                canvas.drawText("SIN", cx, cy - dp(2), paint);

                paint.setTextSize(size * .115f);
                paint.setColor(YELLOW);
                canvas.drawText("RELOJ", cx, cy + size * .23f, paint);
                return;
            }

            int ringColor;
            if (seconds == 0) {
                ringColor = RED;
            } else if (seconds <= 5) {
                ringColor = RED;
            } else if (seconds <= 10) {
                ringColor = ORANGE;
            } else {
                ringColor = Color.rgb(21, 143, 255);
            }

            float fraction = Math.min(1f, Math.max(0f, seconds / (float) total));
            float sweep = seconds == 0 ? 360f : 360f * fraction;

            paint.setColor(ringColor);
            canvas.drawArc(arc, -90, sweep, false, paint);

            paint.setStyle(Paint.Style.FILL);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTypeface(Typeface.DEFAULT_BOLD);
            paint.setTextSize(size * .34f);
            paint.setColor(Color.WHITE);

            Paint.FontMetrics fm = paint.getFontMetrics();
            float numberY = cy - fm.ascent / 2f - dp(3);
            canvas.drawText(String.valueOf(seconds), cx, numberY, paint);

            paint.setTypeface(Typeface.DEFAULT);
            paint.setTextSize(size * .10f);

            String sub;
            if (isMatchFinished) {
                paint.setColor(YELLOW);
                sub = "FINAL";
            } else if (!isGameStarted) {
                paint.setColor(YELLOW);
                sub = "LISTO";
            } else if (isPaused) {
                paint.setColor(YELLOW);
                sub = "PAUSA";
            } else if (seconds == 0) {
                paint.setColor(RED);
                sub = "TIEMPO";
            } else if (isExtensionArmed) {
                paint.setColor(ORANGE);
                sub = "EXTENSIÓN";
            } else {
                paint.setColor(MUTED);
                sub = "segundos";
            }

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
        final boolean gameStarted;
        final boolean matchFinished;
        final int extensions1;
        final int extensions2;
        final boolean extensionArmed;

        State(
                String player1,
                String player2,
                int score1,
                int score2,
                int innings1,
                int innings2,
                int maxRun1,
                int maxRun2,
                int currentRun,
                int currentPlayer,
                int target,
                int shotSeconds,
                int secondsLeft,
                boolean paused,
                boolean gameStarted,
                boolean matchFinished,
                int extensions1,
                int extensions2,
                boolean extensionArmed
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
            this.gameStarted = gameStarted;
            this.matchFinished = matchFinished;
            this.extensions1 = extensions1;
            this.extensions2 = extensions2;
            this.extensionArmed = extensionArmed;
        }
    }
}
