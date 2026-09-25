package com.billarazteca.marcador;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayDeque;
import java.util.Deque;

public class MainActivity extends Activity {

    private static final int YELLOW = Color.rgb(255, 214, 0);
    private static final int GREEN = Color.rgb(0, 145, 70);
    private static final int BRAND_RED = Color.rgb(225, 18, 36);
    private static final int BG_TOP = Color.rgb(0, 0, 0);
    private static final int BG_BOTTOM = Color.rgb(14, 14, 14);
    private static final int PANEL_DARK = Color.rgb(8, 8, 8);
    private static final int BORDER = Color.rgb(53, 68, 84);
    private static final int MUTED = Color.rgb(174, 185, 198);
    private static final int ORANGE = Color.rgb(255, 151, 28);
    private static final int RED = Color.rgb(225, 18, 36);
    private static final int LIGHT = Color.rgb(232, 236, 241);

    private static final int BREAK_ALTERNATE = 0;
    private static final int BREAK_WINNER = 1;

    private static final int GROUP_OPEN = 0;
    private static final int GROUP_SOLIDS = 1;
    private static final int GROUP_STRIPES = 2;

    private static final String RECOVERY_PREFS = "pool_recovery";

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
    private TextView meta1View;
    private TextView meta2View;
    private TextView score1View;
    private TextView score2View;
    private TextView rackView;
    private TextView activeView;
    private TimerRingView timerView;

    private Button extension1Button;
    private Button extension2Button;
    private Button winRack1Button;
    private Button winRack2Button;
    private Button undoButton;
    private Button foulButton;
    private Button endTurnButton;
    private Button pauseButton;
    private Button resetButton;
    private Button contextButton;

    private ToneGenerator toneGenerator;

    private String tableName = "Mesa 1";
    private String player1 = "Jugador 1";
    private String player2 = "Jugador 2";

    private int gameType = 9;
    private int raceTo = 7;
    private int score1 = 0;
    private int score2 = 0;
    private int rackNumber = 1;

    private int currentPlayer = 1;
    private int breaker = 1;
    private int firstBreaker = 1;
    private int breakMode = BREAK_ALTERNATE;

    private boolean breakShotPending = true;
    private boolean pushOutAvailable = false;
    private boolean pushOutInProgress = false;

    private int fouls1 = 0;
    private int fouls2 = 0;

    private int group1 = GROUP_OPEN;
    private int group2 = GROUP_OPEN;

    private boolean useTimer = true;
    private int shotSeconds = 35;
    private int secondsLeft = 35;
    private int extensions1 = 1;
    private int extensions2 = 1;

    private boolean paused = false;
    private boolean gameStarted = false;
    private boolean matchFinished = false;
    private boolean rackWaitingStart = false;

    private long lastTick = 0L;
    private boolean tenSecondWarningPlayed = false;
    private boolean timeExpiredDialogShown = false;

    private boolean recoveryActive = false;
    private boolean persistenceReady = false;
    private String lastPersistentCore = "";
    private int lastPersistedSeconds = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        enableFullscreen();

        try {
            boolean recoveredMatch = restoreSavedMatch();
            persistenceReady = true;

            setContentView(buildUi());
            lastTick = System.currentTimeMillis();
            handler.post(timerRunnable);
            refresh();

            if (recoveredMatch) {
                handler.post(this::showRecoveryDialog);
            }
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
    protected void onPause() {
        savePersistentState(true);
        super.onPause();
    }

    @Override
    protected void onStop() {
        savePersistentState(true);
        super.onStop();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) enableFullscreen();
    }

    @Override
    protected void onDestroy() {
        savePersistentState(true);
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
                LinearLayout.LayoutParams.MATCH_PARENT, dp(54)));

        root.addView(buildMatchHeader(), new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(46)));

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
                dp(190), LinearLayout.LayoutParams.MATCH_PARENT);
        centerParams.setMargins(dp(4), 0, dp(4), 0);

        gameRow.addView(panel1, playerParams);
        gameRow.addView(center, centerParams);
        gameRow.addView(panel2, playerParams);

        root.addView(gameRow, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        root.addView(buildActions(), new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(56)));

        return root;
    }

    private View buildHeader() {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(8), dp(2), dp(10), dp(2));
        header.setBackground(new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{Color.BLACK, Color.rgb(18, 18, 18)}
        ));

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.marcothon_logo);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        header.addView(logo, new LinearLayout.LayoutParams(dp(50), dp(50)));

        TextView title = text("Club Billar Marcothon · Pool", 22, Color.WHITE, true);
        title.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.MATCH_PARENT, 1f);
        titleParams.setMargins(dp(10), 0, 0, 0);
        header.addView(title, titleParams);

        Button closeButton = new Button(this);
        closeButton.setText("×");
        closeButton.setTextSize(24);
        closeButton.setTextColor(Color.WHITE);
        closeButton.setTypeface(Typeface.DEFAULT_BOLD);
        closeButton.setAllCaps(false);
        closeButton.setIncludeFontPadding(false);
        closeButton.setGravity(Gravity.CENTER);
        closeButton.setPadding(0, 0, 0, dp(2));
        closeButton.setMinHeight(0);
        closeButton.setMinimumHeight(0);
        closeButton.setBackground(roundRect(
                Color.rgb(210, 38, 45),
                Color.rgb(135, 20, 25),
                1,
                4
        ));
        try {
            closeButton.setStateListAnimator(null);
        } catch (Throwable ignored) {
        }

        closeButton.setOnClickListener(v -> confirmExitApp());
        header.addView(closeButton, new LinearLayout.LayoutParams(dp(52), dp(30)));

        return header;
    }

    private View buildMatchHeader() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(0, dp(1), 0, dp(1));

        tableView = text(tableName, 19, Color.WHITE, true);
        tableView.setGravity(Gravity.CENTER);
        tableView.setOnClickListener(v -> editTableName());

        matchView = text("", 12, MUTED, false);
        matchView.setGravity(Gravity.CENTER);

        box.addView(tableView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.05f));
        box.addView(matchView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, .95f));

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

        TextView meta = text("Faltas: 0", 10, MUTED, true);
        meta.setGravity(Gravity.CENTER);
        meta.setPadding(dp(3), 0, dp(3), 0);
        meta.setBackground(roundRect(PANEL_DARK, BORDER, 1, 8));
        meta.setOnClickListener(v -> {
            if (gameType == 8) showGroupDialog();
        });

        Button extension = styledButton("Ext. · 1", LIGHT, Color.rgb(14, 21, 29), 9);
        extension.setOnClickListener(v -> requestExtension(first ? 1 : 2));

        LinearLayout.LayoutParams activeParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.MATCH_PARENT, .95f);
        LinearLayout.LayoutParams metaParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.MATCH_PARENT, .95f);
        metaParams.setMargins(dp(3), 0, dp(3), 0);
        LinearLayout.LayoutParams extParams = new LinearLayout.LayoutParams(
                dp(72), LinearLayout.LayoutParams.MATCH_PARENT);

        statusRow.addView(active, activeParams);
        statusRow.addView(meta, metaParams);
        statusRow.addView(extension, extParams);

        panel.addView(statusRow, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(28)));

        TextView score = text("0", 104, Color.WHITE, true);
        score.setGravity(Gravity.CENTER);
        score.setSingleLine(true);
        score.setAutoSizeTextTypeUniformWithConfiguration(
                64, 118, 1, TypedValue.COMPLEX_UNIT_SP
        );

        panel.addView(score, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        Button winRack = styledButton("GANÓ RACK", YELLOW, Color.BLACK, 20);
        panel.addView(winRack, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(50)));

        if (first) {
            name1View = name;
            active1View = active;
            meta1View = meta;
            score1View = score;
            extension1Button = extension;
            winRack1Button = winRack;
            winRack.setOnClickListener(v -> awardRack(1));
        } else {
            name2View = name;
            active2View = active;
            meta2View = meta;
            score2View = score;
            extension2Button = extension;
            winRack2Button = winRack;
            winRack.setOnClickListener(v -> awardRack(2));
        }

        return panel;
    }

    private LinearLayout centerPanel() {
        LinearLayout center = new LinearLayout(this);
        center.setOrientation(LinearLayout.VERTICAL);
        center.setGravity(Gravity.CENTER_HORIZONTAL);
        center.setPadding(dp(7), dp(6), dp(7), dp(6));
        center.setBackground(roundRect(Color.rgb(12, 20, 29), BORDER, 1, 15));

        LinearLayout rackBox = new LinearLayout(this);
        rackBox.setOrientation(LinearLayout.VERTICAL);
        rackBox.setGravity(Gravity.CENTER);
        rackBox.setBackground(roundRect(PANEL_DARK, BORDER, 1, 11));

        TextView rackLabel = text("Rack", 12, MUTED, false);
        rackLabel.setGravity(Gravity.CENTER);

        rackView = text("1", 27, Color.WHITE, true);
        rackView.setGravity(Gravity.CENTER);

        rackBox.addView(rackLabel, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, .7f));
        rackBox.addView(rackView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.3f));

        center.addView(rackBox, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(53)));

        timerView = new TimerRingView();

        LinearLayout.LayoutParams timerParams = new LinearLayout.LayoutParams(dp(121), dp(121));
        timerParams.gravity = Gravity.CENTER_HORIZONTAL;
        timerParams.setMargins(0, dp(5), 0, dp(4));
        center.addView(timerView, timerParams);

        timerView.setOnClickListener(v -> {
            if (!useTimer || !gameStarted || matchFinished || rackWaitingStart) return;

            if (pushOutInProgress) {
                finishPushOutDialog();
            } else {
                recordLegalShotSamePlayer();
            }
        });

        timerView.setOnLongClickListener(v -> {
            if (useTimer) editShotSeconds();
            return true;
        });

        contextButton = styledButton("", LIGHT, Color.rgb(14, 21, 29), 10);
        contextButton.setVisibility(View.GONE);
        contextButton.setOnClickListener(v -> handleContextAction());

        center.addView(contextButton, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(36)));

        activeView = text("", 11, YELLOW, true);
        activeView.setGravity(Gravity.CENTER);

        center.addView(activeView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        return center;
    }

    private View buildActions() {
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setPadding(0, dp(4), 0, 0);

        undoButton = styledButton("↶ Deshacer", Color.rgb(150, 156, 164), Color.rgb(14, 21, 29), 13);
        foulButton = styledButton("FALTA", BRAND_RED, Color.WHITE, 15);
        endTurnButton = styledButton("FIN DE TURNO", YELLOW, Color.BLACK, 17);
        pauseButton = styledButton("INICIAR PARTIDA", LIGHT, Color.BLACK, 13);
        resetButton = styledButton("Reiniciar reloj", LIGHT, Color.rgb(14, 21, 29), 12);
        Button newGame = styledButton("Nuevo partido", LIGHT, Color.rgb(14, 21, 29), 12);

        undoButton.setOnClickListener(v -> undo());
        foulButton.setOnClickListener(v -> registerFoul());
        endTurnButton.setOnClickListener(v -> finishTurnLegal());
        pauseButton.setOnClickListener(v -> handleStartPause());
        resetButton.setOnClickListener(v -> resetTimerManual());
        newGame.setOnClickListener(v -> requestNewGame());

        actions.addView(undoButton, actionParams(.88f));
        actions.addView(foulButton, actionParams(.78f));
        actions.addView(endTurnButton, actionParams(1.42f));
        actions.addView(pauseButton, actionParams(1.12f));
        actions.addView(resetButton, actionParams(1.02f));
        actions.addView(newGame, actionParams(1.02f));

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
        d.setStroke(dp(strokeDp), stroke);
        return d;
    }

    private GradientDrawable playerBackground(boolean active) {
        return roundRect(
                PANEL_DARK,
                active ? YELLOW : BORDER,
                active ? 3 : 1,
                15
        );
    }

    private void refresh() {
        if (score1View == null) return;

        tableView.setText(tableName);
        matchView.setText(
                gameName() +
                " · Carrera a " + raceTo +
                " · Rack " + rackNumber +
                " · Rompe: " + playerName(breaker)
        );

        name1View.setText(player1);
        name2View.setText(player2);

        score1View.setText(String.valueOf(score1));
        score2View.setText(String.valueOf(score2));

        rackView.setText(String.valueOf(rackNumber));

        panel1.setBackground(playerBackground(currentPlayer == 1));
        panel2.setBackground(playerBackground(currentPlayer == 2));

        active1View.setVisibility(currentPlayer == 1 ? View.VISIBLE : View.INVISIBLE);
        active2View.setVisibility(currentPlayer == 2 ? View.VISIBLE : View.INVISIBLE);

        updateMeta(meta1View, 1);
        updateMeta(meta2View, 2);
        updateExtensionButton(extension1Button, 1, extensions1);
        updateExtensionButton(extension2Button, 2, extensions2);

        String currentName = playerName(currentPlayer);

        if (matchFinished) {
            activeView.setText("Partida terminada");
        } else if (!gameStarted) {
            activeView.setText("Rompe primero: " + playerName(breaker));
        } else if (rackWaitingStart) {
            activeView.setText("Rack " + rackNumber + " listo · rompe " + playerName(breaker));
        } else if (pushOutInProgress) {
            activeView.setText("Push out · " + currentName);
        } else {
            activeView.setText("En turno: " + currentName);
        }

        if (matchFinished) {
            pauseButton.setText("PARTIDA TERMINADA");
            pauseButton.setEnabled(false);
            pauseButton.setTextColor(Color.rgb(90, 95, 102));
            pauseButton.setBackground(roundRect(
                    Color.rgb(190, 194, 200),
                    Color.argb(35, 0, 0, 0),
                    1,
                    12
            ));
        } else if (!gameStarted) {
            pauseButton.setText("INICIAR PARTIDA");
            pauseButton.setEnabled(true);
            pauseButton.setTextColor(Color.BLACK);
            pauseButton.setBackground(roundRect(LIGHT, Color.argb(50, 255, 255, 255), 1, 12));
        } else if (rackWaitingStart) {
            pauseButton.setText("INICIAR RACK");
            pauseButton.setEnabled(true);
            pauseButton.setTextColor(Color.BLACK);
            pauseButton.setBackground(roundRect(YELLOW, Color.argb(65, 0, 0, 0), 1, 12));
        } else if (!useTimer) {
            pauseButton.setText("PARTIDA EN CURSO");
            pauseButton.setEnabled(false);
            pauseButton.setTextColor(Color.rgb(70, 76, 84));
            pauseButton.setBackground(roundRect(
                    Color.rgb(205, 209, 214),
                    Color.argb(30, 0, 0, 0),
                    1,
                    12
            ));
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

        boolean playControls = gameStarted && !matchFinished && !rackWaitingStart;
        foulButton.setEnabled(playControls);
        foulButton.setAlpha(playControls ? 1f : .5f);

        endTurnButton.setEnabled(playControls && !pushOutInProgress);
        endTurnButton.setAlpha((playControls && !pushOutInProgress) ? 1f : .5f);

        winRack1Button.setEnabled(playControls);
        winRack2Button.setEnabled(playControls);
        winRack1Button.setAlpha(playControls ? 1f : .55f);
        winRack2Button.setAlpha(playControls ? 1f : .55f);

        resetButton.setEnabled(useTimer && playControls);
        resetButton.setAlpha((useTimer && playControls) ? 1f : .55f);

        undoButton.setEnabled(!history.isEmpty());
        undoButton.setAlpha(history.isEmpty() ? .55f : 1f);

        updateContextButton();

        timerView.setTimer(
                secondsLeft,
                shotSeconds,
                paused,
                gameStarted,
                matchFinished,
                useTimer,
                rackWaitingStart
        );

        savePersistentState(false);
    }

    private void updateMeta(TextView view, int player) {
        if (gameType == 8) {
            int group = player == 1 ? group1 : group2;

            if (group == GROUP_SOLIDS) {
                view.setText("LISAS");
                view.setTextColor(Color.WHITE);
            } else if (group == GROUP_STRIPES) {
                view.setText("RAYADAS");
                view.setTextColor(Color.WHITE);
            } else {
                view.setText("MESA ABIERTA");
                view.setTextColor(MUTED);
            }
        } else {
            int fouls = player == 1 ? fouls1 : fouls2;

            if (fouls >= 2) {
                view.setText("⚠ 2 FALTAS");
                view.setTextColor(ORANGE);
            } else {
                view.setText("Faltas: " + fouls);
                view.setTextColor(fouls > 0 ? YELLOW : MUTED);
            }
        }
    }

    private void updateExtensionButton(Button button, int player, int remaining) {
        if (!useTimer) {
            button.setVisibility(View.GONE);
            return;
        }

        button.setVisibility(View.VISIBLE);
        button.setText("Ext. · " + remaining);

        boolean enabled =
                gameStarted &&
                !matchFinished &&
                !rackWaitingStart &&
                !paused &&
                currentPlayer == player &&
                remaining > 0;

        button.setEnabled(enabled);
        button.setAlpha(enabled ? 1f : .55f);
    }

    private void updateContextButton() {
        if (contextButton == null) return;

        if (!gameStarted || matchFinished || rackWaitingStart) {
            contextButton.setVisibility(View.GONE);
            return;
        }

        if (pushOutInProgress) {
            contextButton.setVisibility(View.VISIBLE);
            contextButton.setText("FINALIZAR PUSH OUT");
            contextButton.setTextColor(Color.WHITE);
            contextButton.setBackground(roundRect(GREEN, Color.argb(45, 255, 255, 255), 1, 10));
            return;
        }

        if (pushOutAvailable && isNineTen()) {
            contextButton.setVisibility(View.VISIBLE);
            contextButton.setText("PUSH OUT");
            contextButton.setTextColor(Color.BLACK);
            contextButton.setBackground(roundRect(LIGHT, Color.argb(45, 255, 255, 255), 1, 10));
            return;
        }

        if (gameType == 8 && group1 == GROUP_OPEN) {
            contextButton.setVisibility(View.VISIBLE);
            contextButton.setText("ASIGNAR GRUPOS");
            contextButton.setTextColor(Color.BLACK);
            contextButton.setBackground(roundRect(LIGHT, Color.argb(45, 255, 255, 255), 1, 10));
            return;
        }

        if (isNineTen() && currentFouls() > 0) {
            contextButton.setVisibility(View.VISIBLE);
            contextButton.setText("✓ TIRO LEGAL");
            contextButton.setTextColor(Color.BLACK);
            contextButton.setBackground(roundRect(YELLOW, Color.argb(65, 0, 0, 0), 1, 10));
            return;
        }

        contextButton.setVisibility(View.GONE);
    }

    private void handleContextAction() {
        if (pushOutInProgress) {
            finishPushOutDialog();
        } else if (pushOutAvailable && isNineTen()) {
            startPushOut();
        } else if (gameType == 8 && group1 == GROUP_OPEN) {
            showGroupDialog();
        } else if (isNineTen() && currentFouls() > 0) {
            recordLegalShotSamePlayer();
        }
    }

    private void awardRack(int player) {
        if (!gameStarted || matchFinished || rackWaitingStart) return;

        pushState();
        completeRack(player, "Rack para " + playerName(player));
    }

    private void completeRack(int winner, String reason) {
        if (winner == 1) {
            score1++;
        } else {
            score2++;
        }

        if ((winner == 1 ? score1 : score2) >= raceTo) {
            matchFinished = true;
            paused = true;
            rackWaitingStart = false;
            pushOutAvailable = false;
            pushOutInProgress = false;
            secondsLeft = 0;
            clearSavedMatch();
            refresh();
            showWinner(winner, reason);
            return;
        }

        rackNumber++;

        fouls1 = 0;
        fouls2 = 0;
        group1 = GROUP_OPEN;
        group2 = GROUP_OPEN;

        extensions1 = 1;
        extensions2 = 1;

        if (breakMode == BREAK_ALTERNATE) {
            breaker = otherPlayer(breaker);
        } else {
            breaker = winner;
        }

        currentPlayer = breaker;
        breakShotPending = true;
        pushOutAvailable = false;
        pushOutInProgress = false;

        secondsLeft = shotSeconds;
        tenSecondWarningPlayed = false;
        timeExpiredDialogShown = false;
        lastTick = System.currentTimeMillis();

        if (useTimer) {
            paused = true;
            rackWaitingStart = true;
        } else {
            paused = false;
            rackWaitingStart = false;
        }

        refresh();
        Toast.makeText(this, reason, Toast.LENGTH_SHORT).show();
    }

    private void finishTurnLegal() {
        if (!gameStarted || matchFinished || rackWaitingStart || pushOutInProgress) return;

        pushState();

        if (isNineTen()) {
            setFouls(currentPlayer, 0);
        }

        processCompletedLegalShot();
        currentPlayer = otherPlayer(currentPlayer);

        resetTimerInternal();
        refresh();
    }

    private void recordLegalShotSamePlayer() {
        if (!gameStarted || matchFinished || rackWaitingStart) return;

        if (pushOutInProgress) {
            finishPushOutDialog();
            return;
        }

        pushState();

        if (isNineTen()) {
            setFouls(currentPlayer, 0);
        }

        processCompletedLegalShot();
        resetTimerInternal();
        refresh();
    }

    private void processCompletedLegalShot() {
        if (breakShotPending) {
            breakShotPending = false;
            pushOutAvailable = isNineTen();
        } else if (pushOutAvailable) {
            pushOutAvailable = false;
        }
    }

    private void registerFoul() {
        if (!gameStarted || matchFinished || rackWaitingStart) return;

        pushState();

        if (breakShotPending) {
            breakShotPending = false;
            pushOutAvailable = false;
        } else if (pushOutAvailable) {
            pushOutAvailable = false;
        }

        pushOutInProgress = false;

        int offender = currentPlayer;
        int incoming = otherPlayer(offender);

        if (isNineTen()) {
            int updated = playerFouls(offender) + 1;
            setFouls(offender, updated);

            if (updated >= 3) {
                paused = true;
                refresh();
                showThirdFoulDialog(offender);
                return;
            }
        }

        currentPlayer = incoming;
        resetTimerInternal();
        refresh();

        Toast.makeText(
                this,
                "Falta de " + playerName(offender) + " · bola en mano para " + playerName(incoming),
                Toast.LENGTH_SHORT
        ).show();

        if (isNineTen() && playerFouls(offender) == 2) {
            showSecondFoulWarning(offender);
        }
    }

    private void showSecondFoulWarning(int player) {
        new AlertDialog.Builder(this)
                .setTitle("⚠ Segunda falta consecutiva")
                .setMessage(
                        playerName(player) +
                        " tiene 2 faltas consecutivas. Debe ser advertido antes de su siguiente tiro."
                )
                .setPositiveButton("Entendido", null)
                .show();
    }

    private void showThirdFoulDialog(int offender) {
        int winner = otherPlayer(offender);

        new AlertDialog.Builder(this)
                .setTitle("3.ª falta consecutiva")
                .setMessage(
                        playerName(offender) +
                        " llegó a tres faltas consecutivas y pierde el rack."
                )
                .setCancelable(false)
                .setNegativeButton("Corregir", (dialog, which) -> undo())
                .setPositiveButton("Confirmar pérdida", (dialog, which) -> {
                    paused = false;
                    completeRack(winner, "Rack por 3 faltas consecutivas");
                })
                .show();
    }

    private void startPushOut() {
        if (!pushOutAvailable || !isNineTen() || matchFinished) return;

        pushState();
        pushOutAvailable = false;
        pushOutInProgress = true;
        resetTimerInternal();
        refresh();

        Toast.makeText(
                this,
                "Push out declarado. Al terminar, indica quién realizará el siguiente tiro.",
                Toast.LENGTH_LONG
        ).show();
    }

    private void finishPushOutDialog() {
        if (!pushOutInProgress) return;

        final int shooter = currentPlayer;
        final int opponent = otherPlayer(shooter);

        new AlertDialog.Builder(this)
                .setTitle("Fin del push out")
                .setMessage("El rival decide quién realizará el siguiente tiro.")
                .setNegativeButton(
                        playerName(shooter) + " tira",
                        (dialog, which) -> finishPushOutChoice(shooter)
                )
                .setPositiveButton(
                        playerName(opponent) + " tira",
                        (dialog, which) -> finishPushOutChoice(opponent)
                )
                .show();
    }

    private void finishPushOutChoice(int nextPlayer) {
        pushState();
        currentPlayer = nextPlayer;
        pushOutInProgress = false;
        resetTimerInternal();
        refresh();
    }

    private void showGroupDialog() {
        if (gameType != 8) return;

        String[] options = new String[]{
                player1 + " = LISAS · " + player2 + " = RAYADAS",
                player1 + " = RAYADAS · " + player2 + " = LISAS",
                "Mesa abierta"
        };

        new AlertDialog.Builder(this)
                .setTitle("Grupos de Bola 8")
                .setItems(options, (dialog, which) -> {
                    pushState();

                    if (which == 0) {
                        group1 = GROUP_SOLIDS;
                        group2 = GROUP_STRIPES;
                    } else if (which == 1) {
                        group1 = GROUP_STRIPES;
                        group2 = GROUP_SOLIDS;
                    } else {
                        group1 = GROUP_OPEN;
                        group2 = GROUP_OPEN;
                    }

                    refresh();
                })
                .show();
    }

    private void requestExtension(int player) {
        if (!useTimer ||
                !gameStarted ||
                matchFinished ||
                rackWaitingStart ||
                paused ||
                currentPlayer != player) {
            return;
        }

        int remaining = player == 1 ? extensions1 : extensions2;
        if (remaining <= 0) return;

        pushState();

        if (player == 1) {
            extensions1 = 0;
        } else {
            extensions2 = 0;
        }

        secondsLeft = Math.min(shotSeconds + 25, secondsLeft + 25);
        tenSecondWarningPlayed = secondsLeft <= 10;
        lastTick = System.currentTimeMillis();

        refresh();

        Toast.makeText(this, "Extensión de 25 segundos", Toast.LENGTH_SHORT).show();
    }

    private void handleStartPause() {
        if (matchFinished) return;

        if (!gameStarted) {
            pushState();
            gameStarted = true;
            rackWaitingStart = false;
            paused = false;
            resetTimerInternal();
            refresh();
            return;
        }

        if (rackWaitingStart) {
            pushState();
            rackWaitingStart = false;
            paused = false;
            resetTimerInternal();
            refresh();
            return;
        }

        if (!useTimer) return;

        pushState();
        paused = !paused;
        lastTick = System.currentTimeMillis();
        refresh();
    }

    private void resetTimerManual() {
        if (!useTimer || !gameStarted || matchFinished || rackWaitingStart) return;

        pushState();
        resetTimerInternal();
        refresh();
    }

    private void resetTimerInternal() {
        secondsLeft = shotSeconds;
        paused = false;
        tenSecondWarningPlayed = false;
        timeExpiredDialogShown = false;
        lastTick = System.currentTimeMillis();
    }

    private void editTableName() {
        showTextDialog("Nombre de la mesa", tableName, value -> {
            tableName = value.isEmpty() ? "Mesa 1" : value;
            refresh();
        });
    }

    private void editPlayerName(int player) {
        String current = player == 1 ? player1 : player2;

        showTextDialog("Nombre del jugador", current, value -> {
            String name = value.trim();

            if (player == 1) {
                player1 = name.isEmpty() ? "Jugador 1" : name;
            } else {
                player2 = name.isEmpty() ? "Jugador 2" : name;
            }

            refresh();
        });
    }

    private void editShotSeconds() {
        showNumberDialog(
                "Segundos por tiro",
                shotSeconds,
                10,
                120,
                value -> {
                    shotSeconds = value;
                    secondsLeft = Math.min(secondsLeft, shotSeconds + 25);
                    refresh();
                }
        );
    }

    private void showTextDialog(String title, String value, StringSaver onSave) {
        EditText input = new EditText(this);
        input.setText(value);
        input.setSingleLine(true);
        input.setTextSize(18);
        input.setSelectAllOnFocus(true);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(input)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Guardar", null)
                .create();

        dialog.setOnShowListener(v ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v2 -> {
                    onSave.save(input.getText().toString().trim());
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
            IntSaver onSave
    ) {
        EditText input = new EditText(this);
        input.setText(String.valueOf(current));
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setSingleLine(true);
        input.setTextSize(18);
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
                        onSave.save(value);
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
                rackNumber > 1 ||
                fouls1 > 0 ||
                fouls2 > 0;

        if (!hasProgress) {
            showNewGameDialog();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Nuevo partido")
                .setMessage("Se borrará el partido actual. ¿Continuar?")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Continuar", (dialog, which) -> showNewGameDialog())
                .show();
    }

    private void showNewGameDialog() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(5), dp(20), 0);

        EditText p1 = field(player1, false);
        EditText p2 = field(player2, false);
        EditText race = field(String.valueOf(raceTo), true);
        EditText time = field(String.valueOf(shotSeconds), true);

        Spinner gameSpinner = dialogSpinner(
                new String[]{"Bola 8", "Bola 9", "Bola 10"},
                gameType == 8 ? 0 : (gameType == 9 ? 1 : 2)
        );

        CheckBox timerOption = new CheckBox(this);
        timerOption.setText("Usar cronómetro");
        timerOption.setTextSize(16);
        timerOption.setChecked(useTimer);
        timerOption.setPadding(0, dp(6), 0, dp(2));

        time.setEnabled(useTimer);
        time.setAlpha(useTimer ? 1f : .45f);

        timerOption.setOnCheckedChangeListener((buttonView, isChecked) -> {
            time.setEnabled(isChecked);
            time.setAlpha(isChecked ? 1f : .45f);
        });

        Spinner breakSpinner = dialogSpinner(
                new String[]{
                        "Saque alternado",
                        "Ganador del rack rompe el siguiente"
                },
                breakMode
        );

        RadioGroup breakerGroup = new RadioGroup(this);
        breakerGroup.setOrientation(RadioGroup.HORIZONTAL);
        breakerGroup.setGravity(Gravity.CENTER_VERTICAL);

        RadioButton breaker1 = new RadioButton(this);
        breaker1.setText("Jugador 1");
        breaker1.setTextSize(15);
        breaker1.setId(View.generateViewId());

        RadioButton breaker2 = new RadioButton(this);
        breaker2.setText("Jugador 2");
        breaker2.setTextSize(15);
        breaker2.setId(View.generateViewId());

        breakerGroup.addView(breaker1, new RadioGroup.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        breakerGroup.addView(breaker2, new RadioGroup.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        if (firstBreaker == 2) {
            breaker2.setChecked(true);
        } else {
            breaker1.setChecked(true);
        }

        box.addView(dialogLabel("Modalidad"));
        box.addView(gameSpinner);
        box.addView(dialogLabel("Jugador 1"));
        box.addView(p1);
        box.addView(dialogLabel("Jugador 2"));
        box.addView(p2);
        box.addView(dialogLabel("Carrera a racks"));
        box.addView(race);
        box.addView(timerOption);
        box.addView(dialogLabel("Segundos por tiro"));
        box.addView(time);
        box.addView(dialogLabel("Formato de saque"));
        box.addView(breakSpinner);
        box.addView(dialogLabel("¿Quién rompe primero?"));
        box.addView(breakerGroup);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Nuevo partido de pool")
                .setView(box)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Preparar", null)
                .create();

        dialog.setOnShowListener(v ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v2 -> {
                    try {
                        String n1 = p1.getText().toString().trim();
                        String n2 = p2.getText().toString().trim();

                        int newRace = Integer.parseInt(race.getText().toString().trim());
                        boolean newUseTimer = timerOption.isChecked();
                        int newTime = shotSeconds;

                        if (newUseTimer) {
                            newTime = Integer.parseInt(time.getText().toString().trim());
                        }

                        player1 = n1.isEmpty() ? "Jugador 1" : n1;
                        player2 = n2.isEmpty() ? "Jugador 2" : n2;

                        int gamePosition = gameSpinner.getSelectedItemPosition();
                        gameType = gamePosition == 0 ? 8 : (gamePosition == 1 ? 9 : 10);

                        raceTo = Math.max(1, Math.min(99, newRace));
                        useTimer = newUseTimer;

                        if (useTimer) {
                            shotSeconds = Math.max(10, Math.min(120, newTime));
                        }

                        breakMode = breakSpinner.getSelectedItemPosition() == 0
                                ? BREAK_ALTERNATE
                                : BREAK_WINNER;

                        firstBreaker = breaker2.isChecked() ? 2 : 1;
                        breaker = firstBreaker;
                        currentPlayer = breaker;

                        score1 = 0;
                        score2 = 0;
                        rackNumber = 1;

                        fouls1 = 0;
                        fouls2 = 0;

                        group1 = GROUP_OPEN;
                        group2 = GROUP_OPEN;

                        extensions1 = 1;
                        extensions2 = 1;

                        breakShotPending = true;
                        pushOutAvailable = false;
                        pushOutInProgress = false;

                        secondsLeft = shotSeconds;
                        paused = false;
                        gameStarted = false;
                        matchFinished = false;
                        rackWaitingStart = false;

                        tenSecondWarningPlayed = false;
                        timeExpiredDialogShown = false;

                        history.clear();
                        lastTick = System.currentTimeMillis();

                        recoveryActive = true;
                        lastPersistentCore = "";
                        lastPersistedSeconds = -1;

                        refresh();
                        dialog.dismiss();
                    } catch (Throwable ignored) {
                        Toast.makeText(
                                this,
                                "Revisa la carrera y los segundos por tiro",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                })
        );

        dialog.show();
    }

    private Spinner dialogSpinner(String[] options, int selection) {
        Spinner spinner = new Spinner(this);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                options
        );

        spinner.setAdapter(adapter);
        spinner.setSelection(Math.max(0, Math.min(options.length - 1, selection)));
        return spinner;
    }

    private EditText field(String value, boolean numeric) {
        EditText e = new EditText(this);
        e.setText(value);
        e.setSingleLine(true);
        e.setTextSize(17);

        if (numeric) {
            e.setInputType(InputType.TYPE_CLASS_NUMBER);
        }

        return e;
    }

    private TextView dialogLabel(String value) {
        TextView t = text(value, 13, Color.DKGRAY, false);
        t.setPadding(0, dp(5), 0, 0);
        return t;
    }

    private void showWinner(int winner, String reason) {
        new AlertDialog.Builder(this)
                .setTitle("Partido terminado")
                .setMessage(
                        playerName(winner) + " ganó la partida.\n\n" +
                        "Marcador final: " + score1 + " - " + score2 + "\n" +
                        reason
                )
                .setPositiveButton("Cerrar", null)
                .setNegativeButton("Nuevo partido", (dialog, which) -> showNewGameDialog())
                .show();
    }

    private SharedPreferences recoveryPrefs() {
        return getSharedPreferences(RECOVERY_PREFS, MODE_PRIVATE);
    }

    private boolean restoreSavedMatch() {
        try {
            SharedPreferences p = recoveryPrefs();

            if (!p.getBoolean("has_saved_match", false)) {
                return false;
            }

            tableName = p.getString("tableName", "Mesa 1");
            player1 = p.getString("player1", "Jugador 1");
            player2 = p.getString("player2", "Jugador 2");

            gameType = p.getInt("gameType", 9);
            raceTo = p.getInt("raceTo", 7);
            score1 = p.getInt("score1", 0);
            score2 = p.getInt("score2", 0);
            rackNumber = p.getInt("rackNumber", 1);

            currentPlayer = p.getInt("currentPlayer", 1);
            breaker = p.getInt("breaker", 1);
            firstBreaker = p.getInt("firstBreaker", 1);
            breakMode = p.getInt("breakMode", BREAK_ALTERNATE);

            breakShotPending = p.getBoolean("breakShotPending", true);
            pushOutAvailable = p.getBoolean("pushOutAvailable", false);
            pushOutInProgress = p.getBoolean("pushOutInProgress", false);

            fouls1 = p.getInt("fouls1", 0);
            fouls2 = p.getInt("fouls2", 0);

            group1 = p.getInt("group1", GROUP_OPEN);
            group2 = p.getInt("group2", GROUP_OPEN);

            useTimer = p.getBoolean("useTimer", true);
            shotSeconds = p.getInt("shotSeconds", 35);
            secondsLeft = p.getInt("secondsLeft", shotSeconds);

            extensions1 = p.getInt("extensions1", 1);
            extensions2 = p.getInt("extensions2", 1);

            gameStarted = p.getBoolean("gameStarted", false);
            rackWaitingStart = p.getBoolean("rackWaitingStart", false);
            matchFinished = false;

            paused = useTimer && gameStarted;
            if (rackWaitingStart) paused = true;

            history.clear();
            recoveryActive = true;
            lastPersistentCore = "";
            lastPersistedSeconds = -1;
            tenSecondWarningPlayed = secondsLeft <= 10;
            timeExpiredDialogShown = false;

            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private void showRecoveryDialog() {
        String message = useTimer && gameStarted
                ? "Se encontró un partido sin terminar. El marcador fue recuperado y el cronómetro quedó en pausa."
                : "Se encontró un partido sin terminar. ¿Deseas continuar desde donde quedó?";

        new AlertDialog.Builder(this)
                .setTitle("Partido interrumpido detectado")
                .setMessage(message)
                .setCancelable(false)
                .setNegativeButton("Descartar y nueva", (dialog, which) -> {
                    clearSavedMatch();
                    resetRecoveredMatch();
                    showNewGameDialog();
                })
                .setPositiveButton("Continuar partido", (dialog, which) -> {
                    recoveryActive = true;
                    savePersistentState(true);
                    refresh();
                })
                .show();
    }

    private void resetRecoveredMatch() {
        player1 = "Jugador 1";
        player2 = "Jugador 2";
        gameType = 9;
        raceTo = 7;
        score1 = 0;
        score2 = 0;
        rackNumber = 1;
        currentPlayer = 1;
        breaker = 1;
        firstBreaker = 1;
        breakMode = BREAK_ALTERNATE;
        breakShotPending = true;
        pushOutAvailable = false;
        pushOutInProgress = false;
        fouls1 = 0;
        fouls2 = 0;
        group1 = GROUP_OPEN;
        group2 = GROUP_OPEN;
        useTimer = true;
        shotSeconds = 35;
        secondsLeft = 35;
        extensions1 = 1;
        extensions2 = 1;
        paused = false;
        gameStarted = false;
        matchFinished = false;
        rackWaitingStart = false;
        tenSecondWarningPlayed = false;
        timeExpiredDialogShown = false;
        history.clear();
        recoveryActive = false;
        lastPersistentCore = "";
        lastPersistedSeconds = -1;
        lastTick = System.currentTimeMillis();
        refresh();
    }

    private String persistentCoreSnapshot() {
        return tableName + "\u0001" +
                player1 + "\u0001" +
                player2 + "\u0001" +
                gameType + "\u0001" +
                raceTo + "\u0001" +
                score1 + "\u0001" +
                score2 + "\u0001" +
                rackNumber + "\u0001" +
                currentPlayer + "\u0001" +
                breaker + "\u0001" +
                firstBreaker + "\u0001" +
                breakMode + "\u0001" +
                breakShotPending + "\u0001" +
                pushOutAvailable + "\u0001" +
                pushOutInProgress + "\u0001" +
                fouls1 + "\u0001" +
                fouls2 + "\u0001" +
                group1 + "\u0001" +
                group2 + "\u0001" +
                useTimer + "\u0001" +
                shotSeconds + "\u0001" +
                extensions1 + "\u0001" +
                extensions2 + "\u0001" +
                paused + "\u0001" +
                gameStarted + "\u0001" +
                rackWaitingStart;
    }

    private boolean hasMeaningfulMatchState() {
        return gameStarted ||
                score1 > 0 ||
                score2 > 0 ||
                rackNumber > 1 ||
                fouls1 > 0 ||
                fouls2 > 0 ||
                !"Jugador 1".equals(player1) ||
                !"Jugador 2".equals(player2) ||
                raceTo != 7 ||
                gameType != 9 ||
                !useTimer ||
                breakMode != BREAK_ALTERNATE;
    }

    private void savePersistentState(boolean force) {
        if (!persistenceReady) return;

        if (matchFinished) {
            clearSavedMatch();
            return;
        }

        if (!recoveryActive && hasMeaningfulMatchState()) {
            recoveryActive = true;
        }

        if (!recoveryActive) return;

        String core = persistentCoreSnapshot();
        boolean coreChanged = !core.equals(lastPersistentCore);
        boolean timerCheckpoint =
                lastPersistedSeconds < 0 ||
                Math.abs(secondsLeft - lastPersistedSeconds) >= 5 ||
                (useTimer && secondsLeft <= 10);

        if (!force && !coreChanged && !timerCheckpoint) {
            return;
        }

        SharedPreferences.Editor e = recoveryPrefs().edit()
                .putBoolean("has_saved_match", true)
                .putString("tableName", tableName)
                .putString("player1", player1)
                .putString("player2", player2)
                .putInt("gameType", gameType)
                .putInt("raceTo", raceTo)
                .putInt("score1", score1)
                .putInt("score2", score2)
                .putInt("rackNumber", rackNumber)
                .putInt("currentPlayer", currentPlayer)
                .putInt("breaker", breaker)
                .putInt("firstBreaker", firstBreaker)
                .putInt("breakMode", breakMode)
                .putBoolean("breakShotPending", breakShotPending)
                .putBoolean("pushOutAvailable", pushOutAvailable)
                .putBoolean("pushOutInProgress", pushOutInProgress)
                .putInt("fouls1", fouls1)
                .putInt("fouls2", fouls2)
                .putInt("group1", group1)
                .putInt("group2", group2)
                .putBoolean("useTimer", useTimer)
                .putInt("shotSeconds", shotSeconds)
                .putInt("secondsLeft", secondsLeft)
                .putInt("extensions1", extensions1)
                .putInt("extensions2", extensions2)
                .putBoolean("gameStarted", gameStarted)
                .putBoolean("rackWaitingStart", rackWaitingStart);

        if (force || coreChanged) {
            e.commit();
        } else {
            e.apply();
        }

        lastPersistentCore = core;
        lastPersistedSeconds = secondsLeft;
    }

    private void clearSavedMatch() {
        try {
            recoveryPrefs().edit().clear().commit();
        } catch (Throwable ignored) {
        }

        recoveryActive = false;
        lastPersistentCore = "";
        lastPersistedSeconds = -1;
    }

    private void confirmExitApp() {
        new AlertDialog.Builder(this)
                .setTitle("Cerrar aplicación")
                .setMessage("¿Deseas cerrar el marcador?")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Cerrar", (dialog, which) -> {
                    savePersistentState(true);
                    handler.removeCallbacks(timerRunnable);
                    finishAndRemoveTask();
                })
                .show();
    }

    private void pushState() {
        history.push(new State(
                player1,
                player2,
                gameType,
                raceTo,
                score1,
                score2,
                rackNumber,
                currentPlayer,
                breaker,
                firstBreaker,
                breakMode,
                breakShotPending,
                pushOutAvailable,
                pushOutInProgress,
                fouls1,
                fouls2,
                group1,
                group2,
                useTimer,
                shotSeconds,
                secondsLeft,
                extensions1,
                extensions2,
                paused,
                gameStarted,
                matchFinished,
                rackWaitingStart
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
        gameType = s.gameType;
        raceTo = s.raceTo;
        score1 = s.score1;
        score2 = s.score2;
        rackNumber = s.rackNumber;
        currentPlayer = s.currentPlayer;
        breaker = s.breaker;
        firstBreaker = s.firstBreaker;
        breakMode = s.breakMode;
        breakShotPending = s.breakShotPending;
        pushOutAvailable = s.pushOutAvailable;
        pushOutInProgress = s.pushOutInProgress;
        fouls1 = s.fouls1;
        fouls2 = s.fouls2;
        group1 = s.group1;
        group2 = s.group2;
        useTimer = s.useTimer;
        shotSeconds = s.shotSeconds;
        secondsLeft = s.secondsLeft;
        extensions1 = s.extensions1;
        extensions2 = s.extensions2;
        paused = s.paused;
        gameStarted = s.gameStarted;
        matchFinished = s.matchFinished;
        rackWaitingStart = s.rackWaitingStart;

        tenSecondWarningPlayed = secondsLeft <= 10;
        timeExpiredDialogShown = false;
        lastTick = System.currentTimeMillis();

        recoveryActive = !matchFinished;
        refresh();
    }

    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                long now = System.currentTimeMillis();

                if (useTimer &&
                        gameStarted &&
                        !matchFinished &&
                        !paused &&
                        !rackWaitingStart &&
                        secondsLeft > 0 &&
                        lastTick > 0) {

                    long elapsed = now - lastTick;

                    if (elapsed >= 1000L) {
                        int previousSeconds = secondsLeft;
                        int steps = (int) (elapsed / 1000L);

                        secondsLeft = Math.max(0, secondsLeft - steps);
                        lastTick += steps * 1000L;

                        if (!tenSecondWarningPlayed &&
                                previousSeconds > 10 &&
                                secondsLeft <= 10) {
                            tenSecondWarningPlayed = true;
                            playWarningBeep();
                        }

                        if (previousSeconds > 0 && secondsLeft == 0) {
                            playTimeExpiredBeep();
                            paused = true;
                            refresh();

                            if (!timeExpiredDialogShown) {
                                timeExpiredDialogShown = true;
                                handler.post(MainActivity.this::showTimeExpiredDialog);
                            }
                        } else {
                            refresh();
                        }
                    }
                } else {
                    lastTick = now;
                }
            } catch (Throwable ignored) {
            }

            handler.postDelayed(this, 100L);
        }
    };

    private void showTimeExpiredDialog() {
        if (matchFinished || !gameStarted) {
            timeExpiredDialogShown = false;
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Tiempo agotado")
                .setMessage("El tiempo agotado se considera falta. ¿Registrar la falta?")
                .setCancelable(false)
                .setNegativeButton("No registrar", (dialog, which) -> {
                    timeExpiredDialogShown = false;
                    paused = true;
                    refresh();
                })
                .setPositiveButton("Registrar falta", (dialog, which) -> {
                    timeExpiredDialogShown = false;
                    paused = false;
                    registerFoul();
                })
                .show();
    }

    private void playWarningBeep() {
        try {
            if (toneGenerator == null) {
                toneGenerator = new ToneGenerator(AudioManager.STREAM_ALARM, 80);
            }

            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 100);
        } catch (Throwable ignored) {
        }
    }

    private void playTimeExpiredBeep() {
        try {
            if (toneGenerator == null) {
                toneGenerator = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
            }

            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 300);
        } catch (Throwable ignored) {
        }
    }

    private boolean isNineTen() {
        return gameType == 9 || gameType == 10;
    }

    private int currentFouls() {
        return playerFouls(currentPlayer);
    }

    private int playerFouls(int player) {
        return player == 1 ? fouls1 : fouls2;
    }

    private void setFouls(int player, int value) {
        if (player == 1) {
            fouls1 = Math.max(0, value);
        } else {
            fouls2 = Math.max(0, value);
        }
    }

    private int otherPlayer(int player) {
        return player == 1 ? 2 : 1;
    }

    private String playerName(int player) {
        return player == 1 ? player1 : player2;
    }

    private String gameName() {
        if (gameType == 8) return "Bola 8";
        if (gameType == 10) return "Bola 10";
        return "Bola 9";
    }

    private void showFallback(Throwable error) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(20), dp(20), dp(20));
        box.setBackgroundColor(Color.WHITE);

        TextView title = text("Club Billar Marcothon · Pool", 24, Color.BLACK, true);
        TextView msg = text(
                "La interfaz no pudo iniciar.\n\n" +
                error.getClass().getSimpleName() + ": " +
                String.valueOf(error.getMessage()),
                15,
                Color.DKGRAY,
                false
        );

        box.addView(title);
        box.addView(msg);
        setContentView(box);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private interface StringSaver {
        void save(String value);
    }

    private interface IntSaver {
        void save(int value);
    }

    private class TimerRingView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF arc = new RectF();

        private int seconds = 35;
        private int total = 35;
        private boolean isPaused = false;
        private boolean isGameStarted = false;
        private boolean isMatchFinished = false;
        private boolean isTimerEnabled = true;
        private boolean isRackWaiting = false;

        TimerRingView() {
            super(MainActivity.this);
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

        void setTimer(
                int seconds,
                int total,
                boolean paused,
                boolean gameStarted,
                boolean matchFinished,
                boolean timerEnabled,
                boolean rackWaiting
        ) {
            this.seconds = Math.max(0, seconds);
            this.total = Math.max(1, total);
            this.isPaused = paused;
            this.isGameStarted = gameStarted;
            this.isMatchFinished = matchFinished;
            this.isTimerEnabled = timerEnabled;
            this.isRackWaiting = rackWaiting;
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

            if (seconds <= 5) {
                ringColor = RED;
            } else if (seconds <= 10) {
                ringColor = ORANGE;
            } else {
                ringColor = GREEN;
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
            paint.setTextSize(size * .09f);

            String sub;

            if (isMatchFinished) {
                paint.setColor(YELLOW);
                sub = "FINAL";
            } else if (!isGameStarted) {
                paint.setColor(YELLOW);
                sub = "LISTO";
            } else if (isRackWaiting) {
                paint.setColor(YELLOW);
                sub = "RACK LISTO";
            } else if (isPaused) {
                paint.setColor(YELLOW);
                sub = "PAUSA";
            } else if (seconds == 0) {
                paint.setColor(RED);
                sub = "TIEMPO";
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
        final int gameType;
        final int raceTo;
        final int score1;
        final int score2;
        final int rackNumber;
        final int currentPlayer;
        final int breaker;
        final int firstBreaker;
        final int breakMode;
        final boolean breakShotPending;
        final boolean pushOutAvailable;
        final boolean pushOutInProgress;
        final int fouls1;
        final int fouls2;
        final int group1;
        final int group2;
        final boolean useTimer;
        final int shotSeconds;
        final int secondsLeft;
        final int extensions1;
        final int extensions2;
        final boolean paused;
        final boolean gameStarted;
        final boolean matchFinished;
        final boolean rackWaitingStart;

        State(
                String player1,
                String player2,
                int gameType,
                int raceTo,
                int score1,
                int score2,
                int rackNumber,
                int currentPlayer,
                int breaker,
                int firstBreaker,
                int breakMode,
                boolean breakShotPending,
                boolean pushOutAvailable,
                boolean pushOutInProgress,
                int fouls1,
                int fouls2,
                int group1,
                int group2,
                boolean useTimer,
                int shotSeconds,
                int secondsLeft,
                int extensions1,
                int extensions2,
                boolean paused,
                boolean gameStarted,
                boolean matchFinished,
                boolean rackWaitingStart
        ) {
            this.player1 = player1;
            this.player2 = player2;
            this.gameType = gameType;
            this.raceTo = raceTo;
            this.score1 = score1;
            this.score2 = score2;
            this.rackNumber = rackNumber;
            this.currentPlayer = currentPlayer;
            this.breaker = breaker;
            this.firstBreaker = firstBreaker;
            this.breakMode = breakMode;
            this.breakShotPending = breakShotPending;
            this.pushOutAvailable = pushOutAvailable;
            this.pushOutInProgress = pushOutInProgress;
            this.fouls1 = fouls1;
            this.fouls2 = fouls2;
            this.group1 = group1;
            this.group2 = group2;
            this.useTimer = useTimer;
            this.shotSeconds = shotSeconds;
            this.secondsLeft = secondsLeft;
            this.extensions1 = extensions1;
            this.extensions2 = extensions2;
            this.paused = paused;
            this.gameStarted = gameStarted;
            this.matchFinished = matchFinished;
            this.rackWaitingStart = rackWaitingStart;
        }
    }
}
