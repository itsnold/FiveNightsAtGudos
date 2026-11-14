package com.gudos.ui.rendering;

import com.gudos.assets.AssetManager;
import com.gudos.assets.PanoramaRenderer;
import com.gudos.game.CameraMotion;
import com.gudos.game.GameSession;
import com.gudos.game.Scene;
import com.gudos.ui.ScenePanel;
import com.gudos.ui.Theme;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.Objects;

/** Concrete component: the current CCTV room, without animated interference or HUD. */
public final class CameraSceneRenderer implements SceneRenderer {
    private final GameSession session;
    private final AssetManager assets;
    private final PanoramaRenderer projection = new PanoramaRenderer();

    public CameraSceneRenderer(GameSession session, AssetManager assets) {
        this.session = Objects.requireNonNull(session);
        this.assets = Objects.requireNonNull(assets);
    }

    @Override public void render(Graphics2D graphics, double time, boolean reducedEffects) {
        Scene scene = Scene.camera(session.camera());
        double sweep = CameraMotion.pan(scene, session.elapsed());
        graphics.drawImage(projection.render(assets.scene(scene, session.sceneState(scene)), sweep),
                0, 0, ScenePanel.WIDTH, ScenePanel.HEIGHT, null);
        graphics.setColor(new Color(0, 0, 0, 32));
        graphics.fillRect(0, 0, ScenePanel.WIDTH, ScenePanel.HEIGHT);
        Theme.vignette(graphics, 205);
    }
}
