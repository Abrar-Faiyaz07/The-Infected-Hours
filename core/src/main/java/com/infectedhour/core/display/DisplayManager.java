package com.infectedhour.core.display;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;

import java.lang.reflect.Method;

/**
 * Manages runtime display modes between Windows Fit (maximized desktop window)
 * and Full Screen, with safe reflection for LWJGL3 window maximization.
 */
public class DisplayManager {

    public static boolean isFullscreen() {
        return Gdx.graphics != null && Gdx.graphics.isFullscreen();
    }

    public static void setFullscreen() {
        if (Gdx.graphics == null) return;
        Graphics.DisplayMode mode = Gdx.graphics.getDisplayMode();
        Gdx.graphics.setFullscreenMode(mode);
    }

    public static void setWindowsFit() {
        if (Gdx.graphics == null) return;
        Gdx.graphics.setWindowedMode(1280, 720);
        try {
            Method getWindow = Gdx.graphics.getClass().getMethod("getWindow");
            Object win = getWindow.invoke(Gdx.graphics);
            if (win != null) {
                Method max = win.getClass().getMethod("maximizeWindow");
                max.invoke(win);
            }
        } catch (Throwable ignored) {
        }
    }

    public static void toggleDisplayMode() {
        if (isFullscreen()) {
            setWindowsFit();
        } else {
            setFullscreen();
        }
    }

    public static String getModeLabel() {
        return isFullscreen() ? "FULL SCREEN" : "WINDOWS FIT";
    }
}
