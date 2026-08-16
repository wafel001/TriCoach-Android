package com.pixelempire.clicker;

import android.content.Context;
import android.widget.FrameLayout;

/** Final production composition: premium world + focal era art + continuous scrolling game sections. */
public final class FinalGameView extends FrameLayout {
    private final UltraGameView game;
    private final ScrollableSectionsView sections;

    public FinalGameView(Context context, ReleaseGameState state){
        super(context);
        game=new UltraGameView(context,state);
        EraFocusView focus=new EraFocusView(context,state,game);
        sections=new ScrollableSectionsView(context,state,game);
        addView(game,new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT));
        addView(focus,new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT));
        addView(sections,new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT));
    }

    public boolean handleBack(){if(sections.handleBack())return true;return game.handleBack();}
}
