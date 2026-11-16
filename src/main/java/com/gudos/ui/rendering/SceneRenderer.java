package com.gudos.ui.rendering;

import java.awt.Graphics2D;

/** Component shared by plain renderers and decorators, in the 1280x720 scene space. [p. 14: Decorator](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-14); [p. 29: SceneRenderer](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-29). Related: [ui/rendering/CameraSceneRenderer.java:65 (render)](CameraSceneRenderer.java#L65) (concrete room renderer); [ui/rendering/RendererDecorator.java:54 (render)](RendererDecorator.java#L54) (same-interface wrapper). */
/*
 * SceneRenderer
 *
 * Related: [ui/rendering/CameraSceneRenderer.java:65 (render)](CameraSceneRenderer.java#L65) (concrete room renderer);
 * [ui/rendering/RendererDecorator.java:54 (render)](RendererDecorator.java#L54) (same-interface wrapper);
 * [ui/GamePanel.java:508 (renderCamera)](../GamePanel.java#L508) (chooses and invokes a renderer chain).
 *
 * Purpose: Defines the component interface shared by a room renderer and all custom renderer
 * decorators.
 *
 * Execution flow: GamePanel stores composed implementations through SceneRenderer and calls
 * render(graphics,time,reducedEffects).
 *
 * Design rationale: The same interface allows nested wrappers and substitutability.
 * Without it, effect layers would need to know each concrete renderer type.
 *
 * State: The contract draws in ScenePanel 1280x720 coordinates; time is animation
 * context and reducedEffects controls optional interference.
 *
 * Summary: Component = SceneRenderer; concrete component = CameraSceneRenderer; wrappers =
 * RendererDecorator subclasses.
 * [p. 14: Decorator](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-14); [p. 29: SceneRenderer](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-29).
 */
public interface SceneRenderer {
    /*
     * Draw the component in 1280x720 logical coordinates for the supplied animation time and
     * reduced-effects setting. Concrete room and wrapper classes must obey this same callable
     * contract.
     * [p. 14: Decorator](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-14); [p. 29: SceneRenderer](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-29).
     * Related: [ui/GamePanel.java:522 (render call)](../GamePanel.java#L522) (caller of render);
     * [ui/rendering/RendererDecorator.java:55 (render call)](RendererDecorator.java#L55) (caller of render);
     * [ui/rendering/CameraSceneRenderer.java:65 (render)](CameraSceneRenderer.java#L65) (concrete room renderer).
     */
    void render(Graphics2D graphics, double time, boolean reducedEffects);
}
