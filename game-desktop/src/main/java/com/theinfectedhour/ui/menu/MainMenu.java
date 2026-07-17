package com.theinfectedhour.ui.menu;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.theinfectedhour.ui.UIScreen;

/**
 * The title/main menu screen: start, join, settings, quit. Built with Scene2D
 * (Stage + Table layout + Skin) so buttons, click events, and resize handling
 * come from libGDX instead of hand-rolled coordinate math.
 */
public class MainMenu extends UIScreen {

    private Stage stage;
    private Skin skin;

    @Override
    public void show() {
        // TODO: stage = new Stage(viewport); Gdx.input.setInputProcessor(stage);
        //       skin = new Skin(Gdx.files.internal("ui/uiskin.json"));
        //       Table with TextButtons: Host Game, Join Game, Settings, Quit
    }

    @Override
    public void hide() {
        // TODO: dispose stage and skin, clear input processor
    }

    @Override
    public void render() {
        // TODO: stage.act(Gdx.graphics.getDeltaTime()); stage.draw();
    }
}
