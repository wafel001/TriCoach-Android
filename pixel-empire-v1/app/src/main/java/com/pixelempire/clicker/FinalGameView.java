package com.pixelempire.clicker;

import android.content.Context;
import android.widget.FrameLayout;

/** Final production composition: premium game + dense dashboard + high-impact era monument layer. */
public final class FinalGameView extends FrameLayout {
    private final UltraGameView game;

    public FinalGameView(Context context, ReleaseGameState state){
        super(context);
        game=new UltraGameView(context,state);
        EraFocusView focus=new EraFocusView(context,state,game);
        addView(game,new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT));
        addView(focus,new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT));
    }

    public boolean handleBack(){return game.handleBack();}
}
