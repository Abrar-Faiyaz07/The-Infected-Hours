package com.infectedhour.core.ui;

import com.infectedhour.shared.constants.GameConstants;

/**
 * 4-slot hotbar (PRD §10, UI/UX doc §3/§4): keys 1-4 select, E interact/use,
 * Q drop. Rendering only — slot contents come from the local Player's
 * inventory array.
 */
public class InventoryHotbar {

    private int selectedSlot = 0;

    public void selectSlot(int index) {
        if (index < 0 || index >= GameConstants.INVENTORY_SLOTS) {
            throw new IllegalArgumentException("slot out of range: " + index);
        }
        selectedSlot = index;
    }

    public int getSelectedSlot() {
        return selectedSlot;
    }

    public void show() {
        // TODO(ui): build a Table/HorizontalGroup of 4 slot cells: panel-dark
        // background, accent-gold border on the SELECTED slot. Keys 1-4 call
        // selectSlot(i) — GameScreen forwards those key presses here.
    }

    public void render(float delta) {
        // TODO(ui): draw the item icon in each filled slot using the fixed
        // iconography of doc par.5 rule 6: shield=barricade, vial=sample,
        // cross=medicine, running figure=rescue, spray=sanitation.
        // Slot contents come from the local Player's inventory array —
        // build the Item type first (see Player.java TEAMMATE TASK).
    }
}
