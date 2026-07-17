package com.theinfectedhour.ui;

/** Base for full-screen UI states: menus, loading screens, story panels (Architecture.md §5). */
public abstract class UIScreen {

    public abstract void show();

    public abstract void hide();

    public abstract void render();
}
