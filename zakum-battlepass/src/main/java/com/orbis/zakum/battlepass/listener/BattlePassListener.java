package com.orbis.zakum.battlepass.listener;

import com.orbis.zakum.api.action.ActionEvent;
import com.orbis.zakum.api.action.ActionListener;
import com.orbis.zakum.battlepass.model.QuestIndex;

public class BattlePassListener implements ActionListener {
    private final QuestIndex index;

    public BattlePassListener(QuestIndex index) {
        this.index = index;
    }

    @Override
    public void onAction(ActionEvent event) {
        // 1. Lookup candidates
        // 2. Process progress
    }
}
