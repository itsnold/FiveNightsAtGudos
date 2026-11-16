package com.gudos.ui.rendering;

import java.awt.Graphics2D;
import java.util.Objects;

/** Decorator: is a SceneRenderer and holds another SceneRenderer, including other decorators. [p. 14: Decorator](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-14); [p. 29: RendererDecorator](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-29). Related: [ui/rendering/SceneRenderer.java:39 (render)](SceneRenderer.java#L39) (common component contract); [ui/rendering/VhsRendererDecorator.java:77 (render)](VhsRendererDecorator.java#L77) (noise wrapper subclass). */
/*
 * RendererDecorator
 *
 * Related: [ui/rendering/SceneRenderer.java:39 (render)](SceneRenderer.java#L39) (common component contract);
 * [ui/rendering/VhsRendererDecorator.java:77 (render)](VhsRendererDecorator.java#L77) (noise wrapper subclass);
 * [ui/rendering/DimmedRendererDecorator.java:55 (render)](DimmedRendererDecorator.java#L55) (darkness wrapper subclass);
 * [ui/GamePanel.java:108 (GamePanel)](../GamePanel.java#L108) (composes the wrappers).
 *
 * Purpose: Implements SceneRenderer while retaining another SceneRenderer and delegating to it.
 *
 * Execution flow: Concrete effect constructors call super(wrapped); render forwards the original
 * graphics/time/reducedEffects.
 *
 * Design rationale: Being the same component type and containing one component lets
 * decorators wrap decorators. Null rejection fails invalid chains early.
 *
 * State: wrapped is private final and nonnull; the base has no visual effect of its
 * own.
 *
 * Summary: Inheritance supplies shared delegation, and composition holds the wrapped
 * renderer. Both are visible here.
 * [p. 14: Decorator](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-14); [p. 29: RendererDecorator](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-29).
 */
public abstract class RendererDecorator implements SceneRenderer {
    private final SceneRenderer wrapped;

    /*
     * Require and retain one wrapped SceneRenderer. A wrapped decorator is valid because it
     * implements that same interface.
     * [p. 14: Decorator](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-14); [p. 29: RendererDecorator](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-29).
     * Related: [ui/rendering/SceneRenderer.java:39 (render)](SceneRenderer.java#L39) (common component contract);
     * [ui/rendering/VhsRendererDecorator.java:77 (render)](VhsRendererDecorator.java#L77) (noise wrapper subclass);
     * [ui/rendering/DimmedRendererDecorator.java:55 (render)](DimmedRendererDecorator.java#L55) (darkness wrapper subclass).
     */
    protected RendererDecorator(SceneRenderer wrapped) {
        this.wrapped = Objects.requireNonNull(wrapped);
    }

    /*
     * Delegate exactly once to the wrapped SceneRenderer with the original graphics, time and
     * reducedEffects. A subclass calls super first, then adds its own layer; this preserves the base
     * room and supports nesting.
     * [p. 14: Decorator](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-14); [p. 29: RendererDecorator](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-29).
     * Related: [ui/rendering/SceneRenderer.java:39 (render)](SceneRenderer.java#L39) (shared component contract);
     * [ui/rendering/VhsRendererDecorator.java:77 (render)](VhsRendererDecorator.java#L77) (subclass adding noise);
     * [ui/rendering/DimmedRendererDecorator.java:55 (render)](DimmedRendererDecorator.java#L55) (subclass adding darkness).
     */
    @Override public void render(Graphics2D graphics, double time, boolean reducedEffects) {
        wrapped.render(graphics, time, reducedEffects);
    }
}
