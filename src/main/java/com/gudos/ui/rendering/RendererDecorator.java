package com.gudos.ui.rendering;

import java.awt.Graphics2D;
import java.util.Objects;

/** Decorator: is a SceneRenderer and holds another SceneRenderer, including other decorators. */
public abstract class RendererDecorator implements SceneRenderer {
    private final SceneRenderer wrapped;

    protected RendererDecorator(SceneRenderer wrapped) {
        this.wrapped = Objects.requireNonNull(wrapped);
    }

    @Override public void render(Graphics2D graphics, double time, boolean reducedEffects) {
        wrapped.render(graphics, time, reducedEffects);
    }
}
