package com.billarazteca.marcador;

import android.Manifest;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.os.SystemClock;
import android.util.Size;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.video.Quality;
import androidx.camera.video.QualitySelector;
import androidx.camera.video.FallbackStrategy;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.Uri;
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
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;


import androidx.activity.ComponentActivity;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraSelector;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.video.FileOutputOptions;
import androidx.camera.video.Recorder;
import androidx.camera.video.Recording;
import androidx.camera.video.VideoCapture;
import androidx.camera.video.VideoRecordEvent;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;

public class MainActivity extends ComponentActivity {

    private static final int YELLOW = Color.rgb(255, 214, 0);
    private static final int YELLOW_SOFT = Color.rgb(255, 227, 64);
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

    private static final int CAMERA_PERMISSION_REQUEST = 7001;
    private static final int REPLAY_SEGMENT_MS = 10_000;
    private static final int REPLAY_MAX_SEGMENTS = 12;
    private static final int CAMERA_SOURCE_FRONT = 0;
    private static final int CAMERA_SOURCE_EXTERNAL = 1;

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
    private TextView scoreBreakdown1View;
    private TextView scoreBreakdown2View;
    private TextView avg1View;
    private TextView avg2View;
    private TextView run1View;
    private TextView run2View;
    private TextView inningView;
    private TextView activeView;
    private TimerRingView timerView;
    private ImageView noTimerLogoView;
    private Button pauseButton;
    private Button resetButton;
    private Button undoButton;
    private Button extension1Button;
    private Button extension2Button;
    private Button replayButton;
    private Button cameraClockButton;
    private boolean cameraClockEnabled = false;
    private volatile boolean cameraSetupOpen = false;
    private boolean cameraSetupRequested = false;
    private boolean foreground = true;
    private boolean cameraClockHold = true;
    private boolean visionObserving = false;
    private long visionLastFrameAt = 0;
    private String cameraClockStatus = "Configurar cámara";
    private ImageAnalysis clockAnalysis;
    private int cameraGeneration = 0;
    private final ExecutorService visionExecutor = Executors.newSingleThreadExecutor();
    private volatile VisionSession visionSession;
    private TableBallDetector calibratedTable;
    private TableCalibrationView calibrationView;
    private TextView calibrationStatusView;
    private AlertDialog calibrationDialog;
    private long calibrationFrameAt = 0;
    private long lastAnalysisProblemAt = 0;

    private static final class VisionSession {
        final TableBallDetector detector;
        final ShotMotionGate gate = new ShotMotionGate();
        VisionSession(TableBallDetector detector) { this.detector = detector; }
    }

    private ToneGenerator toneGenerator;
    private ToneGenerator warningToneGenerator;

    private final Deque<File> replaySegments = new ArrayDeque<>();
    private ProcessCameraProvider replayCameraProvider;
    private VideoCapture<Recorder> replayVideoCapture;
    private Recording replayRecording;
    private final ExecutorService replayFileExecutor = Executors.newSingleThreadExecutor();
    private File activeReplayFile;
    private int replayBufferEpoch = 0;
    private long replayRecordingToken = 0;
    private long replayStartedAt = 0;
    private boolean replayFinalizing = false;
    private int replayFailures = 0;
    private String replayLastProblem = "";
    private boolean replayCaptureDesired = false;
    private boolean replayPlaybackOpen = false;
    private boolean replayOptionsPending = false;
    private boolean resumeTimerAfterReplay = false;
    private boolean cameraProviderInitInProgress = false;
    private boolean cameraPermissionRequestInFlight = false;
    private int replayCameraSource = CAMERA_SOURCE_FRONT;

    private final Runnable rotateReplayRunnable = () -> {
        if (replayRecording != null) {
            try {
                replayRecording.stop();
            } catch (Throwable ignored) {
            }
        }
    };

    private String tableName = "Mesa 1";
    private String player1 = "Jugador 1";
    private String player2 = "Jugador 2";

    private int score1 = 0;
    private int score2 = 0;
    // Totals at the start of each player's latest turn; retained during the rival's turn.
    private int turnBase1 = 0;
    private int turnBase2 = 0;
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
    private boolean timeFoulDialogShown = false;

    private static final String RECOVERY_PREFS = "billiards_recovery";
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

        replayCameraSource = getSharedPreferences("replay_settings", MODE_PRIVATE)
                .getInt("camera_source", CAMERA_SOURCE_FRONT);
        clearReplayCacheDirectory();

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
        foreground = true;
        invalidateVision();
        refresh();
        enableFullscreen();

        if (gameStarted && !matchFinished && !replayPlaybackOpen) {
            startReplayCaptureIfReady();
        }
    }

    @Override
    protected void onPause() {
        foreground = false;
        invalidateVision();
        if (cameraSetupOpen) {
            // A backgrounded calibration cannot be trusted after returning.
            calibratedTable = null;
            if (calibrationView != null) calibrationView.reset();
        }
        refresh();
        stopReplayCapture();
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
        handler.removeCallbacks(rotateReplayRunnable);
        stopReplayCapture();

        if (replayCameraProvider != null) {
            try {
                replayCameraProvider.unbindAll();
            } catch (Throwable ignored) {
            }
        }
        if (toneGenerator != null) {
            try {
                toneGenerator.release();
            } catch (Throwable ignored) {
            }
            toneGenerator = null;
        }
        if (warningToneGenerator != null) {
            try {
                warningToneGenerator.release();
            } catch (Throwable ignored) {
            }
            warningToneGenerator = null;
        }
        cameraGeneration++;
        if (clockAnalysis != null) clockAnalysis.clearAnalyzer();
        visionExecutor.shutdownNow();
        handler.removeCallbacks(replayStartTimeout);
        handler.removeCallbacks(recoverReplayRunnable);
        handler.removeCallbacks(calibrationFrameTimeout);
        // Finish any already queued MP4 checks; their callbacks discard files after destruction.
        replayFileExecutor.shutdown();
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
                new int[]{Color.rgb(0, 0, 0), Color.rgb(18, 18, 18)}
        ));

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.marcothon_logo);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        header.addView(logo, new LinearLayout.LayoutParams(dp(50), dp(50)));

        TextView title = text("Club Billar Marcothon", 23, Color.WHITE, true);
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

        TextView score = text("0", 104, Color.WHITE, true);
        score.setGravity(Gravity.CENTER);
        score.setSingleLine(true);
        score.setAutoSizeTextTypeUniformWithConfiguration(
                64, 112, 1, TypedValue.COMPLEX_UNIT_SP
        );
        // Overlay the tiny receipt in the existing score area: no new row and no
        // change to the main number's size, bounds, or the surrounding controls.
        FrameLayout scoreArea = new FrameLayout(this);
        scoreArea.addView(score, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        TextView breakdown = text("", 9, MUTED, false);
        breakdown.setGravity(Gravity.CENTER);
        breakdown.setSingleLine(true);
        breakdown.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        FrameLayout.LayoutParams breakdownParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, dp(12), Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        scoreArea.addView(breakdown, breakdownParams);
        panel.addView(scoreArea, new LinearLayout.LayoutParams(
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

        Button minus = styledButton("−1", RED, Color.WHITE, 22);
        Button plus = styledButton("+1", YELLOW, Color.BLACK, 22);
        Button plusThree = styledButton("+3", YELLOW, Color.BLACK, 22);
        Button plusFive = styledButton("+5", YELLOW, Color.BLACK, 22);

        Button[] scoreButtons = {minus, plus, plusThree, plusFive};
        for (int i = 0; i < scoreButtons.length; i++) {
            Button button = scoreButtons[i];
            button.setMinWidth(0);
            button.setMinimumWidth(0);
            button.setSingleLine(true);
            button.setAutoSizeTextTypeUniformWithConfiguration(12, 22, 1, TypedValue.COMPLEX_UNIT_SP);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.MATCH_PARENT, 1f);
            params.setMargins(i == 0 ? 0 : dp(2), dp(4), i == 3 ? 0 : dp(2), 0);
            buttons.addView(button, params);
        }
        int player = first ? 1 : 2;
        minus.setContentDescription("Restar una carambola al jugador " + player);
        plus.setContentDescription("Sumar una carambola al jugador " + player);
        plusThree.setContentDescription("Sumar tres carambolas al jugador " + player);
        plusFive.setContentDescription("Sumar cinco carambolas al jugador " + player);
        minus.setOnClickListener(v -> subtractPoint(player));
        plus.setOnClickListener(v -> addPoints(player, 1));
        plusThree.setOnClickListener(v -> addPoints(player, 3));
        plusFive.setOnClickListener(v -> addPoints(player, 5));

        panel.addView(buttons, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(48)));

        if (first) {
            name1View = name;
            active1View = active;
            extension1Button = extension;
            score1View = score;
            scoreBreakdown1View = breakdown;
            avg1View = avg;
            run1View = run;
        } else {
            name2View = name;
            active2View = active;
            extension2Button = extension;
            score2View = score;
            scoreBreakdown2View = breakdown;
            avg2View = avg;
            run2View = run;
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

        noTimerLogoView = new ImageView(this);
        noTimerLogoView.setImageResource(R.drawable.marcothon_logo);
        noTimerLogoView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        noTimerLogoView.setAdjustViewBounds(true);
        noTimerLogoView.setVisibility(View.GONE);

        LinearLayout.LayoutParams logoParams = new LinearLayout.LayoutParams(dp(121), dp(121));
        logoParams.gravity = Gravity.CENTER_HORIZONTAL;
        logoParams.setMargins(0, dp(5), 0, 0);
        center.addView(noTimerLogoView, logoParams);

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

        cameraClockButton = styledButton("Cámara · reloj", LIGHT, Color.rgb(14, 21, 29), 9);
        cameraClockButton.setOnClickListener(v -> showCameraClockOptions());
        center.addView(cameraClockButton, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(32)));
        return center;
    }

    private View buildActions() {
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setPadding(0, dp(4), 0, 0);

        undoButton = styledButton("↶  Deshacer", Color.rgb(150, 156, 164), Color.rgb(14, 21, 29), 13);
        Button endTurn = styledButton("FIN DE TURNO", YELLOW, Color.BLACK, 17);
        pauseButton = styledButton("INICIAR PARTIDA", YELLOW, Color.BLACK, 12);
        resetButton = styledButton("Reiniciar reloj", LIGHT, Color.rgb(14, 21, 29), 12);
        replayButton = styledButton("▶ Repetición", GREEN, Color.WHITE, 12);
        Button newGame = styledButton("Nuevo partido", LIGHT, Color.rgb(14, 21, 29), 12);

        undoButton.setOnClickListener(v -> undo());
        endTurn.setOnClickListener(v -> finishTurn());
        pauseButton.setOnClickListener(v -> handleStartPause());
        resetButton.setOnClickListener(v -> {
            if (useTimer) {
                resetTimerManual();
            }
        });
        replayButton.setOnClickListener(v -> beginReplayReview());
        replayButton.setOnLongClickListener(v -> {
            showReplayCameraSourceDialog();
            return true;
        });
        newGame.setOnClickListener(v -> requestNewGame());

        actions.addView(undoButton, actionParams(.82f));
        actions.addView(endTurn, actionParams(1.72f));
        actions.addView(pauseButton, actionParams(.78f));
        actions.addView(resetButton, actionParams(.92f));
        actions.addView(replayButton, actionParams(1.02f));
        actions.addView(newGame, actionParams(.96f));

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

        boolean observe = cameraClockEnabled && useTimer && gameStarted && !matchFinished
                && !paused && !replayPlaybackOpen && !cameraSetupOpen && foreground;
        if (observe != visionObserving) {
            visionObserving = observe;
            invalidateVision();
        }
        if (cameraClockButton != null) {
            cameraClockButton.setVisibility(useTimer ? View.VISIBLE : View.GONE);
            cameraClockButton.setText(cameraClockEnabled ? "Cámara · " + cameraClockStatus : "Cámara · reloj");
            cameraClockButton.setTextColor(cameraClockEnabled && cameraClockHold ? ORANGE : LIGHT);
        }
        tableView.setText(tableName);
        matchView.setText("Partido a " + target);

        name1View.setText(player1);
        name2View.setText(player2);

        score1View.setText(String.valueOf(score1));
        score2View.setText(String.valueOf(score2));
        updateScoreBreakdown(scoreBreakdown1View, turnBase1, score1);
        updateScoreBreakdown(scoreBreakdown2View, turnBase2, score2);

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

        if (replayButton != null) {
            replayButton.setEnabled(gameStarted && !matchFinished && !replayPlaybackOpen);
            replayButton.setAlpha((gameStarted && !matchFinished && !replayPlaybackOpen) ? 1f : .55f);
            replayButton.setText(
                    replayCameraSource == CAMERA_SOURCE_EXTERNAL
                            ? "▶ Replay USB"
                            : "▶ Repetición"
            );
        }

        timerView.setVisibility(useTimer ? View.VISIBLE : View.GONE);
        noTimerLogoView.setVisibility(useTimer ? View.GONE : View.VISIBLE);

        if (useTimer) {
            timerView.setTimer(
                    secondsLeft,
                    shotSeconds,
                    paused || (cameraClockEnabled && cameraClockHold),
                    extensionArmed,
                    gameStarted,
                    matchFinished,
                    true
            );
        }

        savePersistentState(false);
    }

    private void updateScoreBreakdown(TextView view, int base, int total) {
        int added = total - base;
        String receipt = added > 0 ? base + " + " + added + " = " + total : "";
        view.setText(receipt);
        view.setVisibility(added > 0 ? View.VISIBLE : View.INVISIBLE);
        view.setContentDescription(added > 0
                ? base + " carambolas anteriores, más " + added + " de esta serie, total " + total
                : null);
    }

    private void updateExtensionButton(Button button, int player, int remaining) {
        if (button == null) return;

        if (!useTimer) {
            button.setVisibility(View.GONE);
            return;
        }

        button.setVisibility(View.VISIBLE);

        if (remaining <= 0) {
            button.setText("Sin ext.");
            button.setEnabled(false);
            button.setTextColor(Color.rgb(80, 85, 92));
            button.setBackground(roundRect(
                    Color.rgb(185, 189, 194),
                    Color.argb(30, 0, 0, 0),
                    1,
                    10
            ));
            button.setAlpha(.75f);
            return;
        }

        button.setText("Extensiones · " + remaining);
        button.setEnabled(false);
        button.setTextColor(Color.rgb(45, 50, 58));
        button.setBackground(roundRect(
                Color.rgb(205, 209, 214),
                Color.argb(30, 0, 0, 0),
                1,
                10
        ));
        button.setAlpha(.85f);
    }

    private void addPoints(int player, int points) {
        if (!gameStarted || matchFinished) {
            Toast.makeText(this, "Primero inicia la partida", Toast.LENGTH_SHORT).show();
            return;
        }
        if (player != currentPlayer || points <= 0) return;
        int currentScore = player == 1 ? score1 : score2;
        int added = Math.min(points, Math.max(0, target - currentScore));
        if (added == 0) return;
        // A bulk addition is one undoable action, with the same turn/timer rules as +1.
        pushState();
        currentRun += added;

        if (player == 1) {
            score1 += added;
            maxRun1 = Math.max(maxRun1, currentRun);
        } else {
            score2 += added;
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

        if (cameraClockEnabled && useTimer) refresh();
        else resetTimerInternal();
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
            turnBase1 = Math.min(turnBase1, score1);
            if (currentPlayer == 1 && currentRun > 0) currentRun--;
        } else {
            score2--;
            turnBase2 = Math.min(turnBase2, score2);
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
        invalidateVision();

        if (currentPlayer == 1) {
            currentPlayer = 2;
            innings2++;
            turnBase2 = score2;
        } else {
            currentPlayer = 1;
            innings1++;
            turnBase1 = score1;
        }

        resetTimerInternal();
    }

    private void requestExtension(int player) {
        if (!useTimer) {
            Toast.makeText(this, "Esta partida no usa cronómetro", Toast.LENGTH_SHORT).show();
            return;
        }

        int remaining = player == 1 ? extensions1 : extensions2;

        Toast.makeText(
                this,
                remaining > 0
                        ? "Las extensiones se aplican automáticamente al llegar a 0"
                        : "Este jugador ya agotó sus extensiones",
                Toast.LENGTH_SHORT
        ).show();
    }

    private void handleStartPause() {
        if (matchFinished) return;

        if (!gameStarted) {
            gameStarted = true;
            paused = false;
            secondsLeft = shotSeconds;
            extensionArmed = false;
            timeFoulDialogShown = false;
            lastTick = System.currentTimeMillis();
            refresh();
            Toast.makeText(
                    this,
                    useTimer ? "Partida iniciada" : "Partida iniciada sin cronómetro",
                    Toast.LENGTH_SHORT
            ).show();

            startReplayCaptureIfReady();
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
        invalidateVision();
        extensionArmed = false;
        timeFoulDialogShown = false;
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
            timeFoulDialogShown = false;
            refresh();
            return;
        }
        secondsLeft = shotSeconds;
        paused = false;
        timeFoulDialogShown = false;
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

            score1 = p.getInt("score1", 0);
            score2 = p.getInt("score2", 0);
            innings1 = p.getInt("innings1", 1);
            innings2 = p.getInt("innings2", 0);
            maxRun1 = p.getInt("maxRun1", 0);
            maxRun2 = p.getInt("maxRun2", 0);
            currentRun = p.getInt("currentRun", 0);
            currentPlayer = p.getInt("currentPlayer", 1);
            turnBase1 = Math.max(0, Math.min(score1, p.getInt("turnBase1",
                    currentPlayer == 1 ? score1 - currentRun : score1)));
            turnBase2 = Math.max(0, Math.min(score2, p.getInt("turnBase2",
                    currentPlayer == 2 ? score2 - currentRun : score2)));
            target = p.getInt("target", 30);
            shotSeconds = p.getInt("shotSeconds", 40);
            secondsLeft = p.getInt("secondsLeft", shotSeconds);

            extensions1 = p.getInt("extensions1", 2);
            extensions2 = p.getInt("extensions2", 2);
            extensionArmed = p.getBoolean("extensionArmed", false);

            useTimer = p.getBoolean("useTimer", true);
            gameStarted = p.getBoolean("gameStarted", false);
            matchFinished = false;

            paused = useTimer && gameStarted;

            history.clear();
            recoveryActive = true;
            lastPersistentCore = "";
            lastPersistedSeconds = -1;
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private void showRecoveryDialog() {
        String message;
        if (useTimer && gameStarted) {
            message = "Se encontró una partida sin terminar. El marcador fue recuperado y el cronómetro quedó en pausa.";
        } else {
            message = "Se encontró una partida sin terminar. ¿Deseas continuar desde donde quedó?";
        }

        new AlertDialog.Builder(this)
                .setTitle("Partida interrumpida detectada")
                .setMessage(message)
                .setCancelable(false)
                .setNegativeButton("Descartar y nueva", (dialog, which) -> {
                    clearSavedMatch();
                    resetRecoveredMatch();
                    showNewGameDialog();
                })
                .setPositiveButton("Continuar partida", (dialog, which) -> {
                    recoveryActive = true;
                    savePersistentState(true);
                    refresh();
                })
                .show();
    }

    private void resetRecoveredMatch() {
        player1 = "Jugador 1";
        player2 = "Jugador 2";
        score1 = 0;
        score2 = 0;
        turnBase1 = 0;
        turnBase2 = 0;
        innings1 = 1;
        innings2 = 0;
        maxRun1 = 0;
        maxRun2 = 0;
        currentRun = 0;
        currentPlayer = 1;
        target = 30;
        shotSeconds = 40;
        secondsLeft = 40;
        extensions1 = 2;
        extensions2 = 2;
        extensionArmed = false;
        paused = false;
        gameStarted = false;
        matchFinished = false;
        useTimer = true;
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
                score1 + "\u0001" +
                score2 + "\u0001" +
                turnBase1 + "\u0001" +
                turnBase2 + "\u0001" +
                innings1 + "\u0001" +
                innings2 + "\u0001" +
                maxRun1 + "\u0001" +
                maxRun2 + "\u0001" +
                currentRun + "\u0001" +
                currentPlayer + "\u0001" +
                target + "\u0001" +
                shotSeconds + "\u0001" +
                extensions1 + "\u0001" +
                extensions2 + "\u0001" +
                extensionArmed + "\u0001" +
                paused + "\u0001" +
                gameStarted + "\u0001" +
                useTimer;
    }

    private boolean hasMeaningfulMatchState() {
        return gameStarted ||
                score1 != 0 ||
                score2 != 0 ||
                innings1 != 1 ||
                innings2 != 0 ||
                maxRun1 != 0 ||
                maxRun2 != 0 ||
                currentRun != 0 ||
                currentPlayer != 1 ||
                extensions1 != 2 ||
                extensions2 != 2 ||
                !"Jugador 1".equals(player1) ||
                !"Jugador 2".equals(player2) ||
                target != 30 ||
                shotSeconds != 40 ||
                !useTimer;
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
                .putInt("score1", score1)
                .putInt("score2", score2)
                .putInt("turnBase1", turnBase1)
                .putInt("turnBase2", turnBase2)
                .putInt("innings1", innings1)
                .putInt("innings2", innings2)
                .putInt("maxRun1", maxRun1)
                .putInt("maxRun2", maxRun2)
                .putInt("currentRun", currentRun)
                .putInt("currentPlayer", currentPlayer)
                .putInt("target", target)
                .putInt("shotSeconds", shotSeconds)
                .putInt("secondsLeft", secondsLeft)
                .putInt("extensions1", extensions1)
                .putInt("extensions2", extensions2)
                .putBoolean("extensionArmed", extensionArmed)
                .putBoolean("paused", paused)
                .putBoolean("gameStarted", gameStarted)
                .putBoolean("useTimer", useTimer);

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
                    stopReplayCapture();
                    handler.removeCallbacks(timerRunnable);
                    finishAndRemoveTask();
                })
                .show();
    }

    private void requestNewGame() {
        final boolean resumeTimerOnCancel =
                useTimer &&
                gameStarted &&
                !paused &&
                !matchFinished;

        if (resumeTimerOnCancel) {
            paused = true;
            lastTick = System.currentTimeMillis();
            refresh();
        }

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
            showNewGameDialog(resumeTimerOnCancel);
            return;
        }

        AlertDialog confirmation = new AlertDialog.Builder(this)
                .setTitle("Nuevo partido")
                .setMessage("Se borrará el marcador actual, las entradas y las extensiones utilizadas. ¿Continuar?")
                .setNegativeButton("Cancelar", (dialog, which) ->
                        resumeTimerAfterNewGameCancel(resumeTimerOnCancel))
                .setPositiveButton("Continuar", (dialog, which) ->
                        showNewGameDialog(resumeTimerOnCancel))
                .create();

        confirmation.setOnCancelListener(dialog ->
                resumeTimerAfterNewGameCancel(resumeTimerOnCancel));

        confirmation.show();
    }

    private void resumeTimerAfterNewGameCancel(boolean shouldResume) {
        if (shouldResume && useTimer && gameStarted && !matchFinished) {
            paused = false;
            lastTick = System.currentTimeMillis();
            refresh();
        }
    }

    private void showWinner(int player) {
        stopReplayCapture();
        recoveryActive = false;
        clearSavedMatch();

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
        showNewGameDialog(false);
    }

    private void showNewGameDialog(boolean resumeTimerOnCancel) {
        final boolean resumeAfterCancel =
                resumeTimerOnCancel ||
                (useTimer && gameStarted && !paused && !matchFinished);

        if (useTimer && gameStarted && !paused && !matchFinished) {
            paused = true;
            lastTick = System.currentTimeMillis();
            refresh();
        }

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(7), dp(20), dp(12));
        scroll.addView(box);

        EditText p1 = field(player1, false);
        EditText p2 = field(player2, false);
        EditText distance = field(String.valueOf(target), true);
        EditText time = field(String.valueOf(shotSeconds), true);

        CheckBox timerOption = new CheckBox(this);
        timerOption.setText("Usar cronómetro");
        timerOption.setTextSize(16);
        timerOption.setChecked(false);
        timerOption.setPadding(0, dp(8), 0, dp(4));

        CheckBox cameraOption = new CheckBox(this);
        cameraOption.setText("Cronómetro automático con cámara (prueba)");
        cameraOption.setTextSize(14);
        cameraOption.setEnabled(false);
        time.setEnabled(false);
        time.setAlpha(.45f);

        timerOption.setOnCheckedChangeListener((buttonView, isChecked) -> {
            time.setEnabled(isChecked);
            time.setAlpha(isChecked ? 1f : .45f);
            cameraOption.setEnabled(isChecked);
            if (!isChecked) cameraOption.setChecked(false);
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
        box.addView(cameraOption);

        final boolean[] prepared = {false};

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Nuevo partido")
                .setView(scroll)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Preparar", null)
                .create();

        dialog.setOnDismissListener(v -> {
            if (!prepared[0]) {
                resumeTimerAfterNewGameCancel(resumeAfterCancel);
            }
        });

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
                        cameraClockEnabled = false;
                        calibratedTable = null;
                        invalidateVision();

                        if (newUseTimer) {
                            shotSeconds = Math.max(5, Math.min(300, newTime));
                        }

                        score1 = 0;
                        score2 = 0;
                        turnBase1 = 0;
                        turnBase2 = 0;
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
                        timeFoulDialogShown = false;

                        stopReplayCapture();
                        clearReplayBuffer();

                        paused = false;
                        gameStarted = false;
                        matchFinished = false;
                        history.clear();
                        lastTick = System.currentTimeMillis();

                        recoveryActive = true;
                        lastPersistentCore = "";
                        lastPersistedSeconds = -1;

                        prepared[0] = true;
                        refresh();
                        dialog.dismiss();
                        if (newUseTimer && cameraOption.isChecked()) {
                            handler.post(this::requestCameraClockSetup);
                        }
                    } catch (Throwable ignored) {
                        Toast.makeText(
                                this,
                                "Revisa la distancia y el tiempo",
                                Toast.LENGTH_SHORT
                        ).show();
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
                turnBase1, turnBase2,
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
        invalidateVision();
        if (history.isEmpty()) return;

        State s = history.pop();

        player1 = s.player1;
        player2 = s.player2;
        score1 = s.score1;
        score2 = s.score2;
        turnBase1 = s.turnBase1;
        turnBase2 = s.turnBase2;
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

    private boolean hasCameraPermission() {
        return checkSelfPermission(Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED;
    }

    private void invalidateVision() {
        cameraClockHold = true;
        visionLastFrameAt = 0;
        cameraClockStatus = calibratedTable == null ? "Configurar mesa" : "Verificando bolas";
        visionSession = visionObserving && calibratedTable != null ? new VisionSession(calibratedTable) : null;
    }

    private void showCameraClockOptions() {
        if (!useTimer || cameraSetupOpen || replayPlaybackOpen) return;
        new AlertDialog.Builder(this)
                .setTitle("Cronómetro con cámara · prueba")
                .setMessage("La cámara pausa el reloj cuando las bolas se mueven y lo reinicia tras 2 segundos quietas. " +
                        "Debe ver las tres bolas y toda la mesa desde una posición fija y elevada. " +
                        "Las carambolas y Fin de turno siguen siendo manuales.\n\n" +
                        "Primero marca el paño de la mesa y comprueba los tres círculos de detección. " +
                        "Si falta visión, el reloj queda en pausa. Usa Desactivar para volver al reloj manual.")
                .setPositiveButton("Configurar y activar", (d, w) -> requestCameraClockSetup())
                .setNeutralButton("Desactivar", (d, w) -> {
                    cameraClockEnabled = false;
                    invalidateVision();
                    lastTick = System.currentTimeMillis();
                    rebindAfterRecording();
                    refresh();
                })
                .setNegativeButton("Cerrar", null).show();
    }

    private void requestCameraClockSetup() {
        if (!useTimer || cameraSetupOpen || !foreground || isFinishing()) return;
        cameraSetupRequested = true;
        if (!hasCameraPermission()) { requestReplayCameraPermission(); return; }
        if (replayCameraProvider == null) { initializeReplayCameraProvider(); return; }
        cameraSetupRequested = false;
        final boolean wasPaused = paused;
        paused = true;
        cameraSetupOpen = true;
        cameraClockEnabled = false;
        calibratedTable = null;
        refresh();
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(6), dp(2), dp(6), dp(2));
        TextView instructions = text("Esperando imagen de la cámara…", 11, Color.DKGRAY, false);
        instructions.setGravity(Gravity.CENTER);
        box.addView(instructions);
        final TableCalibrationView[] view = new TableCalibrationView[1];
        view[0] = new TableCalibrationView(this, () -> instructions.setText(view[0].status()));
        calibrationView = view[0];
        calibrationStatusView = instructions;
        calibrationFrameAt = 0;
        int height = Math.max(dp(140), Math.min(dp(340), getResources().getDisplayMetrics().heightPixels - dp(180)));
        box.addView(view[0], new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, height));
        final boolean[] activated = {false};
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Marca las cuatro esquinas interiores del paño")
                .setView(box)
                .setNegativeButton("Cancelar", null)
                .setNeutralButton("Reintentar cámara", null)
                .setPositiveButton("Activar", null).create();
        calibrationDialog = dialog;
        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
                if (!view[0].hasFrame() || SystemClock.elapsedRealtime() - calibrationFrameAt > 2000L) {
                    view[0].reset();
                    calibrationFrameAt = 0;
                    instructions.setText("Reiniciando la cámara…");
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                    rebindAfterRecording();
                    handler.removeCallbacks(calibrationFrameTimeout);
                    handler.postDelayed(calibrationFrameTimeout, 6000L);
                } else {
                    view[0].reset();
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                }
            });
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                TableBallDetector table = view[0].calibration(SystemClock.elapsedRealtime());
                if (table == null || clockAnalysis == null) {
                    Toast.makeText(this, "Deben verse las tres bolas durante 2 segundos. Comprueba que los círculos corresponden a las bolas.", Toast.LENGTH_LONG).show();
                    return;
                }
                calibratedTable = table;
                cameraClockEnabled = true;
                activated[0] = true;
                dialog.dismiss();
            });
        });
        dialog.setOnDismissListener(d -> {
            cameraSetupOpen = false;
            handler.removeCallbacks(calibrationFrameTimeout);
            calibrationView = null;
            calibrationStatusView = null;
            calibrationDialog = null;
            view[0].release();
            if (!activated[0]) calibratedTable = null;
            // Calibration is a setup pause; it must not silently resume a user's pause.
            paused = wasPaused;
            lastTick = System.currentTimeMillis();
            invalidateVision();
            refresh();
            if (foreground && gameStarted && !matchFinished) startReplayCaptureIfReady();
        });
        dialog.show();
        rebindAfterRecording();
        handler.removeCallbacks(calibrationFrameTimeout);
        handler.postDelayed(calibrationFrameTimeout, 6000L);
    }

    private final Runnable calibrationFrameTimeout = () -> {
        if (cameraSetupOpen && calibrationStatusView != null &&
                (calibrationFrameAt == 0 || SystemClock.elapsedRealtime() - calibrationFrameAt > 4000L)) {
            calibrationStatusView.setText("No se recibió imagen. Pulsa Reintentar cámara. Si continúa, envía una foto de este aviso.");
            if (calibrationDialog != null) {
                calibrationDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setText("Reintentar cámara");
                calibrationDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            }
        }
    };

    private boolean rebindPending = false;
    private void rebindAfterRecording() {
        if (replayRecording != null || replayFinalizing) {
            rebindPending = true;
            stopReplayCapture();
        } else {
            bindReplayCamera();
        }
    }

    private void analyzeClockFrame(ImageProxy image, int generation) {
        Bitmap source = null, scaled = null, upright = null;
        try {
            if (visionSession == null && !cameraSetupOpen) return;
            long now = SystemClock.elapsedRealtime();
            // Throttle to 15 fps without blocking CameraX or the UI thread.
            if (now - lastAnalyzedFrame < 65) return;
            lastAnalyzedFrame = now;
            source = image.toBitmap();
            int sourceWidth = source.getWidth(), sourceHeight = source.getHeight();
            double scale = Math.min(1.0, 640.0 / Math.max(sourceWidth, sourceHeight));
            int w = Math.max(1, (int)Math.round(sourceWidth * scale));
            int h = Math.max(1, (int)Math.round(sourceHeight * scale));
            scaled = Bitmap.createScaledBitmap(source, w, h, false);
            int rotation = image.getImageInfo().getRotationDegrees();
            if (rotation == 0) {
                upright = scaled;
            } else {
                Matrix matrix = new Matrix();
                matrix.postRotate(rotation);
                upright = Bitmap.createBitmap(scaled, 0, 0, w, h, matrix, false);
            }
            int outputWidth = upright.getWidth(), outputHeight = upright.getHeight();
            int[] pixels = new int[outputWidth * outputHeight];
            upright.getPixels(pixels, 0, outputWidth, 0, 0, outputWidth, outputHeight);
            VisionSession session = visionSession;
            ShotMotionGate.Result result = session == null ? null :
                    session.gate.update(session.detector.detect(pixels,outputWidth,outputHeight),now);
            handler.post(() -> {
                if (generation != cameraGeneration || !foreground || isFinishing()) return;
                if (calibrationView != null) {
                    calibrationFrameAt = now;
                    handler.removeCallbacks(calibrationFrameTimeout);
                    calibrationView.frame(pixels, outputWidth, outputHeight, now);
                    if (calibrationDialog != null) {
                        calibrationDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setText("Volver a marcar");
                        calibrationDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                                .setEnabled(calibrationView.canActivate(now));
                    }
                }
                if (session == null || session != visionSession || !visionObserving || result == null) return;
                // Do not apply results that waited in the UI queue: freshness is a condition for running.
                if (SystemClock.elapsedRealtime()-now > ShotMotionGate.MAX_GAP_MS) return;
                boolean wasHeld = cameraClockHold;
                visionLastFrameAt = now;
                cameraClockHold = result.holdClock();
                String previous = cameraClockStatus;
                switch(result.state) {
                    case READY: cameraClockStatus="Lista"; break;
                    case MOVING: cameraClockStatus="Bolas en juego"; break;
                    case LOST: cameraClockStatus="Sin visión · pausa"; break;
                    default: cameraClockStatus="Verificando bolas";
                }
                if (result.resetClock && !paused && !timeFoulDialogShown && secondsLeft > 0) {
                    // No score/turn/extension mutation: the same player gets a fresh shot.
                    secondsLeft = shotSeconds;
                    extensionArmed = false;
                    lastTick = System.currentTimeMillis();
                } else if (wasHeld || cameraClockHold) {
                    lastTick = System.currentTimeMillis();
                }
                if (wasHeld != cameraClockHold || !previous.equals(cameraClockStatus) || result.resetClock) refresh();
            });
        } catch (RuntimeException error) {
            // Preserve the hold on the clock, and expose conversion failures instead of
            // leaving the configuration screen silently black forever.
            long now = SystemClock.elapsedRealtime();
            if (now - lastAnalysisProblemAt > 2000L) {
                lastAnalysisProblemAt = now;
                android.util.Log.w("MarcothonCamera", "Unable to read analysis image", error);
                handler.post(() -> {
                    if (generation != cameraGeneration || !foreground) return;
                    if (calibrationStatusView != null) {
                        calibrationStatusView.setText("No se pudo leer la imagen (" +
                                error.getClass().getSimpleName() + "). Pulsa Reintentar cámara.");
                        if (calibrationDialog != null) {
                            calibrationDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setText("Reintentar cámara");
                            calibrationDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                        }
                    }
                });
            }
        } finally {
            if (upright != null && upright != scaled && upright != source) upright.recycle();
            if (scaled != null && scaled != source) scaled.recycle();
            if (source != null) source.recycle();
            image.close();
        }
    }
    private long lastAnalyzedFrame = 0;


    private void requestReplayCameraPermission() {
        if (cameraPermissionRequestInFlight) return;

        cameraPermissionRequestInFlight = true;
        requestPermissions(
                new String[]{Manifest.permission.CAMERA},
                CAMERA_PERMISSION_REQUEST
        );
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode != CAMERA_PERMISSION_REQUEST) return;

        cameraPermissionRequestInFlight = false;

        boolean granted =
                grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED;

        if (granted) {
            Toast.makeText(
                    this,
                    "Cámara habilitada para repetición instantánea",
                    Toast.LENGTH_SHORT
            ).show();
            if (cameraSetupRequested) requestCameraClockSetup();
            else startReplayCaptureIfReady();
        } else {
            cameraSetupRequested = false;
            Toast.makeText(
                    this,
                    "La repetición necesita permiso para usar la cámara",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void startReplayCaptureIfReady() {
        if (!gameStarted || matchFinished || replayPlaybackOpen) return;

        replayCaptureDesired = true;
        if (!foreground || cameraSetupOpen || replayFinalizing) return;

        if (!hasCameraPermission()) {
            requestReplayCameraPermission();
            return;
        }

        if (replayCameraProvider == null) {
            initializeReplayCameraProvider();
            return;
        }

        if (replayVideoCapture == null) {
            bindReplayCamera();
            return;
        }

        if (replayRecording == null) {
            startReplaySegment();
        }
    }

    private void initializeReplayCameraProvider() {
        if (cameraProviderInitInProgress || replayCameraProvider != null) return;

        cameraProviderInitInProgress = true;
        ListenableFuture<ProcessCameraProvider> future =
                ProcessCameraProvider.getInstance(this);

        future.addListener(() -> {
            cameraProviderInitInProgress = false;

            try {
                replayCameraProvider = future.get();
                bindReplayCamera();
                if (cameraSetupRequested) requestCameraClockSetup();
            } catch (Throwable error) {
                cameraSetupRequested = false;
                replayCaptureDesired = false;
                Toast.makeText(
                        MainActivity.this,
                        "No se pudo iniciar la cámara de repetición",
                        Toast.LENGTH_LONG
                ).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @androidx.annotation.OptIn(markerClass = androidx.camera.core.ExperimentalLensFacing.class)
    private CameraSelector selectedReplayCameraSelector() {
        if (replayCameraSource == CAMERA_SOURCE_EXTERNAL &&
                replayCameraProvider != null) {

            for (CameraInfo info : replayCameraProvider.getAvailableCameraInfos()) {
                try {
                    if (info.getLensFacing() == CameraSelector.LENS_FACING_EXTERNAL) {
                        return info.getCameraSelector();
                    }
                } catch (Throwable ignored) {
                }
            }
        }

        return CameraSelector.DEFAULT_FRONT_CAMERA;
    }

    @androidx.annotation.OptIn(markerClass = androidx.camera.core.ExperimentalLensFacing.class)
    private boolean hasExternalReplayCamera() {
        if (replayCameraProvider == null) return false;

        for (CameraInfo info : replayCameraProvider.getAvailableCameraInfos()) {
            try {
                if (info.getLensFacing() == CameraSelector.LENS_FACING_EXTERNAL) {
                    return true;
                }
            } catch (Throwable ignored) {
            }
        }

        return false;
    }

    private ImageAnalysis createClockAnalysis(int generation) {
        ImageAnalysis analysis = new ImageAnalysis.Builder()
                .setTargetResolution(new Size(640, 480))
                .setTargetRotation(getWindowManager().getDefaultDisplay().getRotation())
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                .build();
        analysis.setAnalyzer(visionExecutor, image -> analyzeClockFrame(image, generation));
        return analysis;
    }

    private void releaseCameraBindings() {
        if (clockAnalysis != null) {
            clockAnalysis.clearAnalyzer();
            clockAnalysis = null;
        }
        replayCameraProvider.unbindAll();
        replayVideoCapture = null;
        cameraGeneration++;
        invalidateVision();
    }

    private String cameraFailureDetails(Throwable error) {
        StringBuilder text = new StringBuilder(error.getClass().getSimpleName());
        Throwable detail = error;
        for (int i = 0; i < 3 && detail != null; i++, detail = detail.getCause()) {
            String message = detail.getMessage();
            if (message != null && !message.isEmpty()) {
                text.append(": ").append(message.replace('\n', ' '));
                break;
            }
        }
        return text.length() > 500 ? text.substring(0, 500) : text.toString();
    }

    private boolean bindRecordingCamera(CameraSelector selector, boolean withClock) {
        // Every attempt creates a fresh Recorder and new analysis use case. A Recorder
        // from a rejected combination must not be reused for the video-only fallback.
        Quality[] choices = withClock
                ? new Quality[]{Quality.SD, Quality.HD, Quality.FHD}
                : new Quality[]{null, Quality.HD, Quality.SD};
        for (Quality quality : choices) {
            try {
                releaseCameraBindings();
                Recorder.Builder builder = new Recorder.Builder();
                if (quality != null) builder.setQualitySelector(QualitySelector.from(quality));
                replayVideoCapture = VideoCapture.withOutput(builder.build());
                if (withClock) {
                    clockAnalysis = createClockAnalysis(cameraGeneration);
                    replayCameraProvider.bindToLifecycle(this, selector, replayVideoCapture, clockAnalysis);
                } else {
                    // First attempt is the default video-only configuration used by v17.
                    replayCameraProvider.bindToLifecycle(this, selector, replayVideoCapture);
                }
                replayLastProblem = "";
                return true;
            } catch (RuntimeException error) {
                android.util.Log.w("MarcothonCamera", "Camera binding rejected; clock=" + withClock
                        + ", quality=" + quality, error);
                replayLastProblem = "No se pudo conectar la cámara para grabar.\n" + cameraFailureDetails(error);
            }
        }
        // Discard the last failed use cases too, so retry never sees an unbound capture.
        try { releaseCameraBindings(); } catch (RuntimeException ignored) { replayVideoCapture = null; }
        return false;
    }

    private void bindReplayCamera() {
        if (replayCameraProvider == null || !hasCameraPermission() || !foreground) return;
        if (replayRecording != null || replayFinalizing) {
            rebindPending = true;
            stopReplayCapture();
            return;
        }
        try {
            CameraSelector selector = selectedReplayCameraSelector();
            if (cameraSetupOpen) {
                releaseCameraBindings();
                calibrationFrameAt = 0;
                lastAnalyzedFrame = 0;
                clockAnalysis = createClockAnalysis(cameraGeneration);
                replayCameraProvider.bindToLifecycle(this, selector, clockAnalysis);
                handler.removeCallbacks(calibrationFrameTimeout);
                handler.postDelayed(calibrationFrameTimeout, 6000L);
                return;
            }
            boolean clockRequested = cameraClockEnabled;
            boolean bound = bindRecordingCamera(selector, clockRequested);
            boolean clockFallback = false;
            String combinedProblem = replayLastProblem;
            if (!bound && clockRequested) {
                // Keep the working replay path available if the device rejects all
                // combined stream sizes. Never silently resume the manual clock.
                cameraClockEnabled = false;
                calibratedTable = null;
                if (useTimer && gameStarted && !matchFinished) paused = true;
                invalidateVision();
                bound = bindRecordingCamera(selector, false);
                clockFallback = bound;
                refresh();
            }
            if (!bound) {
                if (replayCameraSource == CAMERA_SOURCE_EXTERNAL) {
                    replayCameraSource = CAMERA_SOURCE_FRONT;
                    saveReplayCameraSource();
                    cameraClockEnabled = false;
                    calibratedTable = null;
                    bindReplayCamera();
                    return;
                }
                replayCaptureDesired = false;
                replayVideoCapture = null;
                if (clockRequested && !combinedProblem.isEmpty()) {
                    replayLastProblem += "\nTambién falló la combinación con reconocimiento de bolas.";
                }
                Toast.makeText(this, "No se pudo conectar la cámara. Repetición muestra el detalle del error.", Toast.LENGTH_LONG).show();
                return;
            }
            if (clockFallback) {
                new AlertDialog.Builder(this).setTitle("Repetición recuperada · reloj manual")
                        .setMessage("Esta configuración de cámara no pudo iniciar la grabación junto con el reconocimiento de bolas. "
                                + "La repetición se mantiene disponible y el cronómetro quedó en pausa. "
                                + "Pulsa Reanudar para usarlo de forma manual.")
                        .setPositiveButton("Entendido", null).show();
            }
            if (replayCaptureDesired && !replayPlaybackOpen) startReplaySegment();
        } catch (RuntimeException error) {
            replayLastProblem = "No se pudo abrir la cámara seleccionada.\n" + cameraFailureDetails(error);
            replayVideoCapture = null;
            if (replayCameraSource == CAMERA_SOURCE_EXTERNAL) {
                replayCameraSource = CAMERA_SOURCE_FRONT;
                saveReplayCameraSource();
                calibratedTable = null;
                cameraClockEnabled = false;
                invalidateVision();
                bindReplayCamera();
                return;
            }
            replayCaptureDesired = false;
            if (cameraSetupOpen && calibrationStatusView != null) {
                handler.removeCallbacks(calibrationFrameTimeout);
                calibrationStatusView.setText(replayLastProblem + "\nPulsa Reintentar cámara.");
            }
            android.util.Log.w("MarcothonCamera", "Unable to connect camera", error);
            Toast.makeText(this, "No se pudo abrir la cámara. Se guardó el detalle para Repetición.", Toast.LENGTH_LONG).show();
        }
    }

    private void recoverReplayWithoutClock() {
        cameraClockEnabled = false;
        calibratedTable = null;
        if (useTimer && gameStarted && !matchFinished) paused = true;
        invalidateVision();
        replayFailures = 0;
        replayCaptureDesired = true;
        rebindAfterRecording();
        refresh();
        Toast.makeText(this, "Intentando recuperar la grabación con el reloj manual en pausa", Toast.LENGTH_LONG).show();
    }

    private void startReplaySegment() {
        if (!replayCaptureDesired || !foreground || cameraSetupOpen || replayFinalizing ||
                replayPlaybackOpen || replayVideoCapture == null || replayRecording != null ||
                !gameStarted || matchFinished) return;

        File directory = new File(getCacheDir(), "instant_replay");
        if (!directory.exists() && !directory.mkdirs()) {
            replayLastProblem = "No se pudo crear la carpeta temporal del video. Comprueba el almacenamiento libre.";
            replayCaptureDesired = false;
            return;
        }
        final File segmentFile = new File(directory, "segment_" + System.nanoTime() + ".mp4");
        final int bufferEpoch = replayBufferEpoch;
        final long token = ++replayRecordingToken;
        activeReplayFile = segmentFile;
        replayStartedAt = 0;
        FileOutputOptions outputOptions = new FileOutputOptions.Builder(segmentFile).build();
        try {
            replayRecording = replayVideoCapture.getOutput().prepareRecording(this, outputOptions)
                    .start(ContextCompat.getMainExecutor(this), event -> {
                        if (token != replayRecordingToken) return;
                        if (event instanceof VideoRecordEvent.Start) {
                            replayStartedAt = SystemClock.elapsedRealtime();
                            handler.removeCallbacks(replayStartTimeout);
                            // Rotate from the actual first recording event, not from camera setup.
                            handler.postDelayed(rotateReplayRunnable, REPLAY_SEGMENT_MS);
                            return;
                        }
                        if (!(event instanceof VideoRecordEvent.Finalize)) return;
                        handler.removeCallbacks(rotateReplayRunnable);
                        handler.removeCallbacks(replayStartTimeout);
                        replayRecording = null;
                        replayStartedAt = 0;
                        replayFinalizing = true;
                        if (replayFileExecutor.isShutdown() || isFinishing() || isDestroyed()) {
                            replayFinalizing = false;
                            activeReplayFile = null;
                            segmentFile.delete();
                            return;
                        }
                        VideoRecordEvent.Finalize result = (VideoRecordEvent.Finalize) event;
                        // CameraX can produce a playable file even with SOURCE_INACTIVE or
                        // a recording limit. Inspect the MP4 instead of deleting all error files.
                        replayFileExecutor.execute(() -> {
                            boolean playable = hasPlayableReplayVideo(segmentFile);
                            handler.post(() -> finishReplaySegment(segmentFile, bufferEpoch,
                                    result.getError(), playable));
                        });
                    });
            handler.postDelayed(replayStartTimeout, 10_000L);
        } catch (Throwable error) {
            replayRecording = null;
            replayStartedAt = 0;
            activeReplayFile = null;
            replayFailures++;
            replayLastProblem = "La cámara no pudo iniciar la grabación de video.";
            segmentFile.delete();
            scheduleReplayRecovery();
        }
    }

    private final Runnable replayStartTimeout = () -> {
        if (replayRecording != null && replayStartedAt == 0) {
            replayLastProblem = "La cámara tardó demasiado en empezar a enviar video.";
            try { replayRecording.stop(); } catch (RuntimeException ignored) { }
        }
    };

    private boolean hasPlayableReplayVideo(File file) {
        if (!file.isFile() || file.length() == 0) return false;
        android.media.MediaExtractor extractor = new android.media.MediaExtractor();
        try {
            extractor.setDataSource(file.getAbsolutePath());
            for (int i = 0; i < extractor.getTrackCount(); i++) {
                android.media.MediaFormat format = extractor.getTrackFormat(i);
                String mime = format.getString(android.media.MediaFormat.KEY_MIME);
                if (mime != null && mime.startsWith("video/")) {
                    extractor.selectTrack(i);
                    // At least one encoded sample must exist; a nonempty MP4 header isn't video.
                    return extractor.getSampleTime() >= 0;
                }
            }
        } catch (Exception ignored) {
        } finally {
            extractor.release();
        }
        return false;
    }

    private void finishReplaySegment(File file, int bufferEpoch, int error, boolean playable) {
        replayFinalizing = false;
        activeReplayFile = null;
        boolean currentBuffer = bufferEpoch == replayBufferEpoch;
        if (playable && currentBuffer && !isFinishing() && !isDestroyed()) {
            addReplaySegment(file);
            replayFailures = 0;
            replayLastProblem = "";
        } else {
            file.delete();
            if (currentBuffer && !playable) {
                replayFailures++;
                replayLastProblem = replayErrorDescription(error);
            }
        }
        if (isFinishing() || isDestroyed()) return;
        if (rebindPending) {
            rebindPending = false;
            if (foreground) {
                bindReplayCamera();
                if (gameStarted && !matchFinished && !cameraSetupOpen) startReplayCaptureIfReady();
            }
            return;
        }
        if (replayOptionsPending && replayPlaybackOpen) {
            replayOptionsPending = false;
            showReplayOptionsDialog();
            return;
        }
        if (playable || !currentBuffer) {
            if (replayCaptureDesired && foreground && !replayPlaybackOpen && !cameraSetupOpen
                    && gameStarted && !matchFinished) handler.post(this::startReplaySegment);
        } else {
            scheduleReplayRecovery();
        }
    }

    private String replayErrorDescription(int error) {
        switch (error) {
            case VideoRecordEvent.Finalize.ERROR_NONE:
                return "La cámara no guardó ninguna imagen de video en este fragmento.";
            case VideoRecordEvent.Finalize.ERROR_SOURCE_INACTIVE:
                return "La cámara dejó de enviar imágenes y no se guardó un fragmento reproducible.";
            case VideoRecordEvent.Finalize.ERROR_NO_VALID_DATA:
                return "La grabación terminó sin recibir fotogramas de video válidos.";
            case VideoRecordEvent.Finalize.ERROR_INSUFFICIENT_STORAGE:
                return "No hay suficiente espacio libre para guardar el video temporal.";
            case VideoRecordEvent.Finalize.ERROR_ENCODING_FAILED:
            case VideoRecordEvent.Finalize.ERROR_RECORDER_ERROR:
                return "El dispositivo no pudo codificar el video. Se intentará una calidad más compatible.";
            default:
                return "No se pudo guardar un fragmento de video reproducible (código " + error + ").";
        }
    }

    private void scheduleReplayRecovery() {
        if (replayFailures >= 3) return;
        handler.removeCallbacks(recoverReplayRunnable);
        handler.postDelayed(recoverReplayRunnable, 1200L);
    }

    private final Runnable recoverReplayRunnable = () -> {
        if (!foreground || !replayCaptureDesired || replayPlaybackOpen || cameraSetupOpen ||
                replayRecording != null || replayFinalizing || !gameStarted || matchFinished) return;
        // Re-create a failed recorder; retries use SD as a compatibility fallback.
        replayVideoCapture = null;
        bindReplayCamera();
    };

    private void addReplaySegment(File file) {
        replaySegments.addLast(file);

        while (replaySegments.size() > REPLAY_MAX_SEGMENTS) {
            File old = replaySegments.removeFirst();

            try {
                old.delete();
            } catch (Throwable ignored) {
            }
        }
    }

    private void stopReplayCapture() {
        replayCaptureDesired = false;
        handler.removeCallbacks(rotateReplayRunnable);
        handler.removeCallbacks(recoverReplayRunnable);
        handler.removeCallbacks(replayStartTimeout);

        if (replayRecording != null) {
            try {
                replayRecording.stop();
            } catch (Throwable ignored) {
            }
        }
    }

    private void clearReplayBuffer() {
        replayBufferEpoch++;
        replayFailures = 0;
        replayLastProblem = "";
        replayOptionsPending = false;

        while (!replaySegments.isEmpty()) {
            File file = replaySegments.removeFirst();

            try {
                file.delete();
            } catch (Throwable ignored) {
            }
        }

        clearReplayCacheDirectory();
    }

    private void clearReplayCacheDirectory() {
        File directory = new File(getCacheDir(), "instant_replay");
        File[] files = directory.listFiles();

        if (files == null) return;

        for (File file : files) {
            if (file.equals(activeReplayFile)) continue;
            try {
                file.delete();
            } catch (Throwable ignored) {
            }
        }
    }

    private void beginReplayReview() {
        if (!gameStarted || matchFinished) {
            Toast.makeText(
                    this,
                    "La repetición está disponible durante una partida",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        if (!hasCameraPermission()) {
            requestReplayCameraPermission();
            return;
        }

        ReplayAvailability.Action availability = ReplayAvailability.onRequest(
                replaySegments.size(), replayRecording != null, replayStartedAt,
                SystemClock.elapsedRealtime(), replayFinalizing);
        if (availability == ReplayAvailability.Action.WAIT) {
            Toast.makeText(this, replayFinalizing ? "Guardando el fragmento de video… Intenta de nuevo en un momento."
                    : "La cámara está iniciando la grabación. Espera unos segundos sin salir de la app.", Toast.LENGTH_LONG).show();
            return;
        }
        if (availability == ReplayAvailability.Action.RETRY) {
            // Attempt recovery first, so this dialog reflects the actual binding result,
            // rather than promising a restart before a hidden camera exception occurs.
            replayFailures = 0;
            replayVideoCapture = null;
            startReplayCaptureIfReady();
            boolean connecting = replayCaptureDesired && replayVideoCapture != null;
            String problem = replayLastProblem.isEmpty()
                    ? "Se está iniciando la grabación. Mantén la app abierta unos 10 segundos y vuelve a tocar Repetición."
                    : replayLastProblem;
            AlertDialog.Builder notice = new AlertDialog.Builder(this)
                    .setTitle(connecting ? "Preparando repetición" : "No se pudo iniciar la cámara")
                    .setMessage(problem).setPositiveButton("Entendido", null);
            if (!connecting) notice.setNeutralButton("Recuperar solo repetición", (d, w) -> recoverReplayWithoutClock());
            notice.show();
            return;
        }

        resumeTimerAfterReplay =
                useTimer &&
                gameStarted &&
                !paused &&
                !matchFinished;

        if (resumeTimerAfterReplay) {
            paused = true;
            lastTick = System.currentTimeMillis();
            refresh();
        }

        replayPlaybackOpen = true;
        replayCaptureDesired = false;

        if (replayRecording != null) {
            replayOptionsPending = true;
            handler.removeCallbacks(rotateReplayRunnable);

            try {
                replayRecording.stop();
            } catch (Throwable ignored) {
                replayOptionsPending = false;
                showReplayOptionsDialog();
            }
        } else if (replayFinalizing) {
            replayOptionsPending = true;
        } else {
            showReplayOptionsDialog();
        }
    }

    private void showReplayOptionsDialog() {
        if (!replayPlaybackOpen) return;

        if (replaySegments.isEmpty()) {
            String problem = replayLastProblem.isEmpty()
                    ? "Todavía no se ha guardado un fragmento reproducible. Deja la partida iniciada unos 10 segundos sin salir de la app."
                    : replayLastProblem;
            new AlertDialog.Builder(this).setTitle("Repetición no disponible")
                    .setMessage(problem).setPositiveButton("Entendido", null).show();
            endReplayReview();
            return;
        }

        String[] options = new String[]{
                "Últimos 30 segundos",
                "Últimos 60 segundos",
                "Últimos 90 segundos"
        };

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Repetición instantánea")
                .setItems(options, (d, which) -> {
                    int seconds = which == 0 ? 30 : (which == 1 ? 60 : 90);
                    showReplayPlayer(seconds);
                })
                .setNegativeButton("Cancelar", (d, which) -> endReplayReview())
                .create();

        dialog.setOnCancelListener(d -> endReplayReview());
        dialog.show();
    }

    private List<File> recentReplayFiles(int requestedSeconds) {
        ArrayList<File> all = new ArrayList<>(replaySegments);

        int requestedSegments = Math.max(
                1,
                (int) Math.ceil(
                        requestedSeconds * 1000.0 / REPLAY_SEGMENT_MS
                )
        );

        int start = Math.max(0, all.size() - requestedSegments);
        return new ArrayList<>(all.subList(start, all.size()));
    }

    private void showReplayPlayer(int requestedSeconds) {
        List<File> files = recentReplayFiles(requestedSeconds);

        if (files.isEmpty()) {
            endReplayReview();
            return;
        }

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(6), dp(3), dp(6), dp(2));

        TextView status = text("", 11, Color.DKGRAY, true);
        status.setGravity(Gravity.CENTER);

        final int[] index = {0};
        final float[] playbackSpeed = {1.0f};
        final android.media.MediaPlayer[] activePlayer = {null};

        LinearLayout replayControls = new LinearLayout(this);
        replayControls.setOrientation(LinearLayout.HORIZONTAL);
        replayControls.setGravity(Gravity.CENTER);
        replayControls.setPadding(0, dp(2), 0, dp(2));

        Button previousButton = styledButton(
                "⏮ Anterior",
                LIGHT,
                Color.rgb(14, 21, 29),
                10
        );

        Button speedButton = styledButton(
                "Velocidad 1×",
                YELLOW,
                Color.BLACK,
                10
        );

        Button nextButton = styledButton(
                "Siguiente ⏭",
                LIGHT,
                Color.rgb(14, 21, 29),
                10
        );

        replayControls.addView(previousButton, actionParams(1f));
        replayControls.addView(speedButton, actionParams(1f));
        replayControls.addView(nextButton, actionParams(1f));

        VideoView video = new VideoView(this);
        MediaController controls = new MediaController(this);
        controls.setAnchorView(video);
        video.setMediaController(controls);

        box.addView(status, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(20)
        ));

        box.addView(replayControls, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(36)
        ));

        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        int replayVideoHeight = Math.max(
                dp(180),
                Math.min(dp(320), screenHeight - dp(155))
        );

        box.addView(video, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                replayVideoHeight
        ));

        TextView note = text(
                "Anterior/Siguiente cambian de fragmento. La velocidad se conserva al cambiar de clip.",
                9,
                Color.DKGRAY,
                false
        );
        note.setPadding(dp(4), dp(1), dp(4), 0);
        note.setGravity(Gravity.CENTER);
        box.addView(note);

        speedButton.setOnClickListener(v -> {
            playbackSpeed[0] = playbackSpeed[0] == 1.0f ? 0.5f : 1.0f;

            speedButton.setText(
                    playbackSpeed[0] == 0.5f
                            ? "Velocidad 0.5×"
                            : "Velocidad 1×"
            );

            if (activePlayer[0] != null) {
                try {
                    activePlayer[0].setPlaybackParams(
                            activePlayer[0]
                                    .getPlaybackParams()
                                    .setSpeed(playbackSpeed[0])
                    );
                } catch (Throwable ignored) {
                    Toast.makeText(
                            MainActivity.this,
                            "Este dispositivo no permite cambiar la velocidad",
                            Toast.LENGTH_SHORT
                    ).show();
                }
            }
        });

        previousButton.setOnClickListener(v -> {
            if (index[0] > 0) {
                index[0]--;
                playReplayFile(
                        video,
                        status,
                        files,
                        index,
                        playbackSpeed,
                        activePlayer,
                        controls
                );
            } else {
                Toast.makeText(
                        MainActivity.this,
                        "Ya estás en el primer fragmento",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });

        nextButton.setOnClickListener(v -> {
            if (index[0] < files.size() - 1) {
                index[0]++;
                playReplayFile(
                        video,
                        status,
                        files,
                        index,
                        playbackSpeed,
                        activePlayer,
                        controls
                );
            } else {
                Toast.makeText(
                        MainActivity.this,
                        "Ya estás en el último fragmento",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Repetición · hasta " + requestedSeconds + " s")
                .setView(box)
                .setPositiveButton("Volver al marcador", null)
                .create();

        dialog.setOnShowListener(v -> {
            Button backButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            if (backButton != null) {
                backButton.setTextSize(11);
                backButton.setMinHeight(0);
                backButton.setMinimumHeight(0);
                backButton.setPadding(dp(10), dp(2), dp(10), dp(2));
            }

            playReplayFile(
                    video,
                    status,
                    files,
                    index,
                    playbackSpeed,
                    activePlayer,
                    controls
            );
        });

        dialog.setOnDismissListener(v -> endReplayReview());
        dialog.show();
    }

    private void playReplayFile(
            VideoView video,
            TextView status,
            List<File> files,
            int[] index,
            float[] playbackSpeed,
            android.media.MediaPlayer[] activePlayer,
            MediaController controls
    ) {
        if (index[0] < 0 || index[0] >= files.size()) return;

        File file = files.get(index[0]);

        status.setText(
                "Fragmento " + (index[0] + 1) + " de " + files.size()
        );

        try {
            controls.hide();
        } catch (Throwable ignored) {
        }

        // VideoView may show its controller while preparing a new clip.
        // Reattach only after its internal prepared callback has finished.
        video.setMediaController(null);
        activePlayer[0] = null;
        video.setOnPreparedListener(mediaPlayer -> {
            activePlayer[0] = mediaPlayer;

            try {
                mediaPlayer.setPlaybackParams(
                        mediaPlayer
                                .getPlaybackParams()
                                .setSpeed(playbackSpeed[0])
                );
            } catch (Throwable ignored) {
            }

            video.start();

            video.post(() -> {
                if (!replayPlaybackOpen || activePlayer[0] != mediaPlayer) return;
                video.setMediaController(controls);
            });
        });

        video.setOnCompletionListener(mediaPlayer -> {
            index[0]++;

            if (index[0] < files.size()) {
                playReplayFile(
                        video,
                        status,
                        files,
                        index,
                        playbackSpeed,
                        activePlayer,
                        controls
                );
            } else {
                status.setText("Repetición terminada");
            }
        });
        video.setVideoURI(Uri.fromFile(file));
    }

    private void endReplayReview() {
        if (!replayPlaybackOpen && !replayOptionsPending) return;

        replayPlaybackOpen = false;
        replayOptionsPending = false;

        if (resumeTimerAfterReplay &&
                useTimer &&
                gameStarted &&
                !matchFinished) {
            paused = false;
            lastTick = System.currentTimeMillis();
        }

        resumeTimerAfterReplay = false;
        refresh();

        if (gameStarted && !matchFinished) {
            startReplayCaptureIfReady();
        }
    }

    private void showReplayCameraSourceDialog() {
        if (!hasCameraPermission()) {
            requestReplayCameraPermission();
            return;
        }

        if (replayCameraProvider == null) {
            initializeReplayCameraProvider();
            Toast.makeText(
                    this,
                    "Inicializando cámaras. Mantén presionado Repetición nuevamente.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        boolean externalAvailable = hasExternalReplayCamera();

        if (!externalAvailable) {
            new AlertDialog.Builder(this)
                    .setTitle("Cámara para repetición")
                    .setMessage(
                            "Actualmente se usa la cámara frontal. " +
                            "Cuando Android detecte una cámara USB externa compatible, " +
                            "podrás seleccionarla desde este mismo menú."
                    )
                    .setPositiveButton("Entendido", null)
                    .show();
            return;
        }

        String[] options = new String[]{
                "Cámara frontal",
                "Cámara USB / externa"
        };

        new AlertDialog.Builder(this)
                .setTitle("Cámara para repetición")
                .setSingleChoiceItems(
                        options,
                        replayCameraSource == CAMERA_SOURCE_EXTERNAL ? 1 : 0,
                        (dialog, which) -> {
                            replayCameraSource =
                                    which == 1
                                            ? CAMERA_SOURCE_EXTERNAL
                                            : CAMERA_SOURCE_FRONT;

                            saveReplayCameraSource();
                            calibratedTable = null;
                            cameraClockEnabled = false;
                            invalidateVision();
                            stopReplayCapture();
                            clearReplayBuffer();

                            if (replayCameraProvider != null) {
                                try {
                                    replayCameraProvider.unbindAll();
                                } catch (Throwable ignored) {
                                }
                            }

                            replayVideoCapture = null;
                            dialog.dismiss();
                            refresh();

                            if (gameStarted && !matchFinished) {
                                startReplayCaptureIfReady();
                            }
                        }
                )
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void saveReplayCameraSource() {
        getSharedPreferences("replay_settings", MODE_PRIVATE)
                .edit()
                .putInt("camera_source", replayCameraSource)
                .apply();
    }

    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                long now = System.currentTimeMillis();
                if (cameraClockEnabled && visionObserving && !cameraClockHold &&
                        (visionLastFrameAt == 0 || SystemClock.elapsedRealtime() - visionLastFrameAt > ShotMotionGate.MAX_GAP_MS)) {
                    invalidateVision();
                    cameraClockStatus = "Sin imagen · pausa";
                    refresh();
                }

                if (useTimer &&
                        gameStarted &&
                        !matchFinished &&
                        !paused &&
                        (!cameraClockEnabled || !cameraClockHold) &&
                        foreground &&
                        secondsLeft > 0 &&
                        lastTick > 0) {

                    long elapsed = now - lastTick;

                    if (elapsed >= 1000L) {
                        int previousSeconds = secondsLeft;
                        int steps = (int) (elapsed / 1000L);

                        secondsLeft = Math.max(0, secondsLeft - steps);
                        lastTick += steps * 1000L;

                        for (int second = previousSeconds - 1; second >= secondsLeft; second--) {
                            if (second <= 10 && second > 0) {
                                playWarningBeep();
                            }
                        }

                        if (previousSeconds > 0 && secondsLeft == 0) {
                            int remainingExtensions =
                                    currentPlayer == 1 ? extensions1 : extensions2;

                            if (remainingExtensions > 0) {
                                playWarningBeep();

                                if (currentPlayer == 1) {
                                    extensions1--;
                                } else {
                                    extensions2--;
                                }

                                extensionArmed = false;
                                timeFoulDialogShown = false;
                                secondsLeft = shotSeconds;
                                paused = false;
                                lastTick = now;

                                int leftAfter =
                                        currentPlayer == 1 ? extensions1 : extensions2;

                                Toast.makeText(
                                        MainActivity.this,
                                        "Extensión automática aplicada · quedan " + leftAfter,
                                        Toast.LENGTH_SHORT
                                ).show();

                                refresh();
                            } else {
                                paused = true;
                                playTimeExpiredBeep();
                                refresh();

                                if (!timeFoulDialogShown) {
                                    timeFoulDialogShown = true;
                                    handler.post(MainActivity.this::showTimeFoulDialog);
                                }
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

    private void playWarningBeep() {
        try {
            if (warningToneGenerator == null) {
                warningToneGenerator = new ToneGenerator(AudioManager.STREAM_ALARM, 65);
            }
            warningToneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 90);
        } catch (Throwable ignored) {
        }
    }

    private void playTimeExpiredBeep() {
        try {
            if (toneGenerator == null) {
                toneGenerator = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
            }
            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP2, 500);
        } catch (Throwable ignored) {
        }
    }

    private void showTimeFoulDialog() {
        if (!useTimer || !gameStarted || matchFinished || secondsLeft > 0) {
            timeFoulDialogShown = false;
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Tiempo agotado")
                .setMessage("El cronómetro llegó a cero. ¿Confirmar falta por tiempo?")
                .setCancelable(false)
                .setNegativeButton("Cancelar", (dialog, which) -> {
                    timeFoulDialogShown = false;
                    extensionArmed = false;
                    secondsLeft = shotSeconds;
                    paused = false;
                    lastTick = System.currentTimeMillis();
                    refresh();
                })
                .setPositiveButton("Confirmar falta", (dialog, which) -> {
                    timeFoulDialogShown = false;
                    finishTurn();
                })
                .show();
    }

    private void showFallback(Throwable error) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(20), dp(20), dp(20));
        box.setBackgroundColor(Color.WHITE);

        TextView title = text("Club Billar Marcothon", 24, Color.BLACK, true);
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
        final int turnBase1;
        final int turnBase2;
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
                int turnBase1,
                int turnBase2,
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
            this.turnBase1 = turnBase1;
            this.turnBase2 = turnBase2;
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
