package com.infectedhour.core.display;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;

import java.lang.reflect.Method;

/**
 * Manages runtime display modes between Borderless Fullscreen (no window controls/title bar)
 * and Windowed mode, with safe reflection for LWJGL3/GLFW window operations.
 */
public class DisplayManager {

    private static boolean isFullscreenMode = true;

    public static boolean isFullscreen() {
        if (Gdx.graphics == null) return false;
        return isFullscreenMode || Gdx.graphics.isFullscreen();
    }

    public static void setFullscreen() {
        if (Gdx.graphics == null) return;
        Graphics.DisplayMode mode = Gdx.graphics.getDisplayMode();
        Gdx.graphics.setWindowedMode(mode.width, mode.height);
        setWindowDecorated(false);
        setWindowPosition(0, 0);
        isFullscreenMode = true;
    }

    public static void setWindowed() {
        if (Gdx.graphics == null) return;
        Gdx.graphics.setWindowedMode(1280, 720);
        setWindowDecorated(true);
        isFullscreenMode = false;
    }

    public static void toggleDisplayMode() {
        if (isFullscreen()) {
            setWindowed();
        } else {
            setFullscreen();
        }
    }

    public static String getModeLabel() {
        return isFullscreen() ? "FULL SCREEN" : "WINDOWED";
    }

    private static void setWindowDecorated(boolean decorated) {
        try {
            Method getWindow = Gdx.graphics.getClass().getMethod("getWindow");
            Object win = getWindow.invoke(Gdx.graphics);
            if (win != null) {
                Method getHandle = win.getClass().getMethod("getWindowHandle");
                long handle = (long) getHandle.invoke(win);
                Class<?> glfwClass = Class.forName("org.lwjgl.glfw.GLFW");
                Method setAttrib = glfwClass.getMethod("glfwSetWindowAttrib", long.class, int.class, int.class);
                setAttrib.invoke(null, handle, 0x00020002 /* GLFW_DECORATED */, decorated ? 1 : 0);
            }
        } catch (Throwable ignored) {
        }
    }

    private static void setWindowPosition(int x, int y) {
        try {
            Method getWindow = Gdx.graphics.getClass().getMethod("getWindow");
            Object win = getWindow.invoke(Gdx.graphics);
            if (win != null) {
                Method setPos = win.getClass().getMethod("setPosition", int.class, int.class);
                setPos.invoke(win, x, y);
            }
        } catch (Throwable ignored) {
        }
    }
}
