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

/** Concrete component: the current CCTV room, without animated interference or HUD. [p. 14: Decorator](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-14); [p. 28: CameraSceneRenderer](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-28). Related: [ui/GamePanel.java:108 (GamePanel)](../GamePanel.java#L108) (constructs and decorates this component); [game/GameSession.java:247 (sceneState)](../../game/GameSession.java#L247) (camera selection and occupancy). */
/*
 * CameraSceneRenderer
 *
 * Related: [ui/GamePanel.java:108 (GamePanel)](../GamePanel.java#L108) (constructs and decorates this component);
 * [game/GameSession.java:247 (sceneState)](../../game/GameSession.java#L247) (camera selection and occupancy);
 * [assets/PanoramaRenderer.java:88 (render)](../../assets/PanoramaRenderer.java#L88) (projects the room image);
 * [game/CameraMotion.java:45 (pan)](../../game/CameraMotion.java#L45) (automatic sweep).
 *
 * Purpose: Draws the current CCTV room without VHS or switching effects.
 *
 * Execution flow: GamePanel puts it inside VhsRendererDecorator; render reads selected camera, room
 * state and sweep from the live session.
 *
 * Design rationale: Composition keeps one core room renderer separate from optional
 * visual layers. The object is created once and reads updated model values each frame.
 *
 * State: session and assets are required; projection is a reusable PanoramaRenderer. It
 * applies the base shade and vignette after room projection.
 *
 * Summary: This is the concrete component in Decorator, not a decorator itself.
 * [p. 14: Decorator](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-14); [p. 28: CameraSceneRenderer](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-28).
 */
public final class CameraSceneRenderer implements SceneRenderer {
    private final GameSession session;
    private final AssetManager assets;
    private final PanoramaRenderer projection = new PanoramaRenderer();

    /*
     * Require nonnull GameSession and AssetManager, then retain them. Its projection buffer is owned
     * by this concrete component and reused per frame.
     * [p. 14: Decorator](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-14); [p. 28: CameraSceneRenderer](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-28).
     * Related: [ui/GamePanel.java:116 (CameraSceneRenderer call)](../GamePanel.java#L116) (caller of CameraSceneRenderer);
     * [game/GameSession.java:247 (sceneState)](../../game/GameSession.java#L247) (camera selection and occupancy);
     * [assets/PanoramaRenderer.java:88 (render)](../../assets/PanoramaRenderer.java#L88) (projects the room image).
     */
    public CameraSceneRenderer(GameSession session, AssetManager assets) {
        this.session = Objects.requireNonNull(session);
        this.assets = Objects.requireNonNull(assets);
    }

    /*
     * Read current camera and scene-state key from GameSession, compute sweep from session elapsed,
     * project the corresponding cached image and draw it at logical viewport size. Apply the room
     * shade/vignette, leaving VHS/switch effects to wrappers.
     * [p. 14: Decorator](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-14); [p. 28: CameraSceneRenderer](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-28).
     * Related: [ui/GamePanel.java:508 (renderCamera)](../GamePanel.java#L508) (uses the composed renderer);
     * [game/GameSession.java:247 (sceneState)](../../game/GameSession.java#L247) (camera occupancy key);
     * [assets/PanoramaRenderer.java:88 (render)](../../assets/PanoramaRenderer.java#L88) (projects selected room image).
     */
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
