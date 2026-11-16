package com.gudos.ui.rendering;

import java.awt.Graphics2D;

/** Component shared by plain renderers and decorators, in the 1280x720 scene space. Reviewer (42-page edition): [p. 14: Decorator](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-14); [p. 29: SceneRenderer](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-29). */
/*
 * SceneRenderer
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
 * Reviewer (42-page edition): [p. 14: Decorator](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-14); [p. 29: SceneRenderer](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-29).
 */
public interface SceneRenderer {
    /*
     * Draw the component in 1280x720 logical coordinates for the supplied animation time and
     * reduced-effects setting. Concrete room and wrapper classes must obey this same callable
     * contract.
     * Reviewer (42-page edition): [p. 14: Decorator](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-14); [p. 29: SceneRenderer](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-29).
     */
    void render(Graphics2D graphics, double time, boolean reducedEffects);
}
