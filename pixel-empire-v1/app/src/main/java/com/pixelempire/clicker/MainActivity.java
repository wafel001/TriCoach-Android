package com.pixelempire.clicker;

import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;

public final class MainActivity extends Activity {
    private ReleaseGameState state;
    private PremiumGameView gameView;

    @Override protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        state=new ReleaseGameState();
        state.load(this);
        boolean debug=(getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE)!=0;
        if(debug){
            int ds=getIntent().getIntExtra("demo_stage",-1);
            if(ds>=0){
                ds=Math.max(0,Math.min(ReleaseContent.STAGES-1,ds));
                state.stage=ds;
                state.level=Math.min(ReleaseContent.MAX_LEVEL,ds*ReleaseContent.LEVELS_PER_STAGE+1);
                state.buildXp=ReleaseContent.xpForLevel(state.level)*.58;
                state.coins=Math.pow(10,Math.min(18,ds/2+4));
                state.crystals=999;state.researchPoints=999;state.tutorialSeen=true;
            }
        }
        gameView=new PremiumGameView(this,state);
        setContentView(gameView);
        getWindow().getDecorView().post(this::immersive);
    }

    private void immersive(){
        View decor=getWindow().getDecorView();if(decor==null)return;
        if(android.os.Build.VERSION.SDK_INT>=30){
            getWindow().setDecorFitsSystemWindows(false);
            WindowInsetsController ctl=decor.getWindowInsetsController();
            if(ctl!=null){
                ctl.hide(WindowInsets.Type.statusBars()|WindowInsets.Type.navigationBars());
                ctl.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        }else{
            decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
    }

    @Override protected void onResume(){super.onResume();View d=getWindow().getDecorView();if(d!=null)d.post(this::immersive);}
    @Override public void onWindowFocusChanged(boolean focus){super.onWindowFocusChanged(focus);if(focus){View d=getWindow().getDecorView();if(d!=null)d.post(this::immersive);}}
    @Override protected void onPause(){if(state!=null)state.save(this);super.onPause();}
    @Override protected void onStop(){if(state!=null)state.save(this);super.onStop();}
    @Override public void onBackPressed(){if(gameView!=null&&gameView.handleBack())return;super.onBackPressed();}
}
