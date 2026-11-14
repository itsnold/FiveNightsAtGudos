package com.gudos.ui.rendering;

import java.awt.Graphics2D;

/** Component shared by plain renderers and decorators, in the 1280x720 scene space. */
public interface SceneRenderer {
    void render(Graphics2D graphics, double time, boolean reducedEffects);
}
