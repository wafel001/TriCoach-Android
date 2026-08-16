package com.pixelempire.clicker;

import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;

public final class MainActivity extends Activity {
    private V3State state;
    private V3View gameView;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        state = new V3State();
        state.load(this);

        boolean debuggable = (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        if (debuggable) {
            int demoStage = getIntent().getIntExtra("demo_stage", -1);
            if (demoStage >= 0) {
                demoStage = Math.max(0, Math.min(V3Content.STAGE_COUNT - 1, demoStage));
                state.stage = demoStage;
                state.level = Math.min(V3Content.MAX_LEVEL, demoStage * V3Content.LEVELS_PER_STAGE + 1);
                state.buildXp = V3Content.xpForLevel(Math.max(1, state.level)) * 0.64;
                state.coins = Math.pow(10, Math.min(28, demoStage / 2 + 3));
                state.science = Math.max(state.science, 500);
                state.crystals = Math.max(state.crystals, 250);
                state.tutorialSeen = true;
            }
        }

        gameView = new V3View(this, state);
        setContentView(gameView);
        getWindow().getDecorView().post(this::enterFullscreen);
    }

    private void enterFullscreen() {
        View decor = getWindow().getDecorView();
        if (decor == null) return;
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            WindowInsetsController controller = decor.getWindowInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            decor.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
    }

    @Override protected void onResume() {
        super.onResume();
        View decor=getWindow().getDecorView();
        if(decor!=null)decor.post(this::enterFullscreen);
    }

    @Override public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            View decor=getWindow().getDecorView();
            if(decor!=null)decor.post(this::enterFullscreen);
        }
    }

    @Override protected void onPause() {
        if (state != null) state.save(this);
        super.onPause();
    }

    @Override protected void onStop() {
        if (state != null) state.save(this);
        super.onStop();
    }
}
