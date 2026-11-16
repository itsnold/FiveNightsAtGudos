# Project documentation

The project reviewer contains **42 numbered content pages plus one unnumbered front index** (43 PDF sheets), typeset with XeLaTeX, with readable typography, typeset equations, compact source-file cards, demonstration paths and 60 practice questions.

- [Typesetting source](FiveNightsAtGudos_Reviewer.tex)
- [Text reference and file locator](FiveNightsAtGudos_Complete_Defense_Reviewer.md)

All 45 application Java files are covered. The full explanations for 313 explicitly declared methods/constructors remain in ordinary source comments; repetitive method-by-method narration is not duplicated in the PDF. The cleaned source snapshot is `e9f0ff45350572e7ba5fd0ff6185ab1251514e2f`.

## Compile the PDF

Use XeLaTeX with a standard TeX Live or MiKTeX installation. From the repository root, create `target/reviewer`, then run twice:

```sh
xelatex -interaction=nonstopmode -halt-on-error -output-directory=target/reviewer docs/defense/FiveNightsAtGudos_Reviewer.tex
xelatex -interaction=nonstopmode -halt-on-error -output-directory=target/reviewer docs/defense/FiveNightsAtGudos_Reviewer.tex
```

The source uses Latin Modern fonts, standard LaTeX math, fontspec, geometry, booktabs/tabularx, enumitem, microtype, xcolor, graphicx, fancyhdr, titlesec, hyperref/xurl, lastpage, listings and tcolorbox/TikZ. It includes font-name fallbacks for non-Linux installations. Figure files are in `figures/`.

New Game, Continue, night selection and replay share the Navigation.startNight contract implemented by Application. That coordinating boundary and AudioService provide facade examples; renderer wrappers implement Decorator and SecretLevelBuilder implements Builder.

The Java cleanup removes presentation-oriented labels without changing executable syntax. All 70 production class files match original bytes when compiled without debug metadata. Full Maven verification remains blocked by dependency-host access; no complete Maven package/native GUI/audio verification is claimed.

## References from source comments

Every existing Java comment includes a `Reviewer (42-page edition)` reference to its file card and, where applicable, a related concept page. Follow its link to [the page index](Reviewer_Pages.md), which reproduces the supplied PDF page text. For example, New Game / `startNight` links to page 13 (Facade) and the relevant file card. These numbers refer to the supplied 42-page PDF, not the earlier 45- or 131-page editions.

The front topic index links directly to the existing printed pages 1–42; page 42 retains the alphabetical Java-file locator. The extra index sheet is not counted in the printed 42-page total. In a viewer that ignores PDF page labels, printed page N is physical sheet N+1. Existing source-comment references remain valid.
