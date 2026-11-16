# Five Nights at Gudo's - Project Study Guide

The PDF edition is **42 pages**, compiled with XeLaTeX. The canonical typesetting source is [FiveNightsAtGudos_Reviewer.tex](FiveNightsAtGudos_Reviewer.tex). The PDF is supplied separately with the project review.

This edition covers all 45 application Java files, the important algorithms and decisions, exact demonstration paths, pattern participants and 60 practice questions. Detailed explanations of all 313 explicitly declared methods/constructors remain beside the source methods as ordinary documentation.

## Quick topic index (unnumbered front page)

The PDF begins with one unnumbered topic index, followed by the original printed pages 1–42. It contains 43 PDF sheets in total. Source-comment references use the printed page numbers. Click the index page numbers in the PDF to jump directly to the topic.

| Topic / feature | Printed page |
| --- | --- |
| **Start, navigate and demonstrate** | |
| Project overview / Java vocabulary | [3](Reviewer_Pages.md#page-3) |
| Build and launch / find code | [4](Reviewer_Pages.md#page-4) |
| Normal shift / controls walkthrough | [5](Reviewer_Pages.md#page-5) |
| Settings / profiles / records | [6](Reviewer_Pages.md#page-6) |
| Hidden game / five paper clicks | [7](Reviewer_Pages.md#page-7) |
| **Rules and visible behavior** | |
| Clock / power / event deadlines | [8](Reviewer_Pages.md#page-8) |
| Gudo route / difficulty / flashlight | [9](Reviewer_Pages.md#page-9) |
| Input coordinates / panorama | [10](Reviewer_Pages.md#page-10) |
| Pause / resume / abandonment | [17](Reviewer_Pages.md#page-17) |
| Monitor / camera / accepted input | [18](Reviewer_Pages.md#page-18) |
| HUD / camera feed / light drawing | [19](Reviewer_Pages.md#page-19) |
| **Data and saving** | |
| SQLite tables / relationships | [11](Reviewer_Pages.md#page-11) |
| Transactions / rollback / UUID retry | [12](Reviewer_Pages.md#page-12) |
| Save folder / final run receipt | [26](Reviewer_Pages.md#page-26) |
| Settings and shift-log text stores | [31](Reviewer_Pages.md#page-31) |
| Records table / preference screens | [25](Reviewer_Pages.md#page-25) |
| **Patterns and design decisions** | |
| New Game / startNight Facade | [13](Reviewer_Pages.md#page-13) |
| AudioService Facade | [13](Reviewer_Pages.md#page-13) |
| Renderer Decorator / effect order | [14](Reviewer_Pages.md#page-14) |
| SecretLevelBuilder / Builder | [15](Reviewer_Pages.md#page-15) |
| Template Method / inheritance / OOP | [15](Reviewer_Pages.md#page-15) |
| Threads / EDT / audio resources | [16](Reviewer_Pages.md#page-16) |
| **Find an implementation** | |
| Application: navigation and saves | [17](Reviewer_Pages.md#page-17) |
| GameSession: normal-night model | [18](Reviewer_Pages.md#page-18) |
| GamePanel: input and rendering | [19](Reviewer_Pages.md#page-19) |
| Main / MenuPanel / Navigation | [20](Reviewer_Pages.md#page-20) |
| Extras / night selection / info | [21](Reviewer_Pages.md#page-21) |
| ScenePanel / buttons / backdrop | [22](Reviewer_Pages.md#page-22) |
| Theme / loading / VHS overlay | [23](Reviewer_Pages.md#page-23) |
| Pause / result / jumpscare | [24](Reviewer_Pages.md#page-24) |
| Settings / RecordsPanel | [25](Reviewer_Pages.md#page-25) |
| Paths / Progress / RunRecord | [26](Reviewer_Pages.md#page-26) |
| Scene / Outcome / CameraMotion | [27](Reviewer_Pages.md#page-27) |
| Assets / panorama / camera renderer | [28](Reviewer_Pages.md#page-28) |
| Renderer interface and wrappers | [29](Reviewer_Pages.md#page-29) |
| Darkness / AudioService / Sound | [30](Reviewer_Pages.md#page-30) |
| Chiptune / settings / shift log | [31](Reviewer_Pages.md#page-31) |
| Database / SecretLevel / Builder | [32](Reviewer_Pages.md#page-32) |
| GudoEncounter / secret runtime | [33](Reviewer_Pages.md#page-33) |
| **Questions and presentation** | |
| Questions 1--10: overview / flow | [34](Reviewer_Pages.md#page-34) |
| Questions 11--20: patterns / Java | [35](Reviewer_Pages.md#page-35) |
| Questions 21--30: rules / timing | [36](Reviewer_Pages.md#page-36) |
| Questions 31--40: Gudo / secret | [37](Reviewer_Pages.md#page-37) |
| Questions 41--50: collision / data | [38](Reviewer_Pages.md#page-38) |
| Questions 51--60: failures / limits | [39](Reviewer_Pages.md#page-39) |
| Five-minute rehearsal / snags | [40](Reviewer_Pages.md#page-40) |
| Limitations / source evidence | [41](Reviewer_Pages.md#page-41) |
| Alphabetical index: all 45 Java files | [42](Reviewer_Pages.md#page-42) |

## Main ideas

Main prepares dependencies; Application coordinates screens and workers; GameSession/GudoEncounter enforce normal-night rules; GamePanel translates input and draws. SQLite owns local progress/history, settings.txt stores preferences, and shift-log.txt is a readable report.

New Game starts Night 1 and preserves history. Continue starts a fresh attempt of the next unlocked night. Only normal COMPLETED outcomes unlock progress. Both hidden-game endings save a secret outcome and restart the same normal night.

## Demonstration paths

| Feature | Steps |
| --- | --- |
| Start and play | Title -> New Game; A/D or edges pan; Space/Tab opens monitor; 1-6 selects feeds; lower monitor and hold F facing the approach. |
| Pause and leave | Escape -> Resume/Escape; or Return to Menu -> confirm -> wait for abandoned save. |
| Records | Title -> Extras -> Shift Records; latest 200 rows, totals include all attempts. |
| Preferences/profile | Title -> Extras -> Settings -> edit -> Apply; changing name selects local profile. |
| Save folder | Settings -> Copy Data Folder; inspect settings.txt, shift-log.txt and a copy of gudo.db. |
| Replay | Title -> Extras -> Select Night -> enabled night; eligibility is checked again by Application. |
| Hidden game | Nights 1-4 -> monitor -> CAM04 -> five valid left-clicks on the actual paper; A/D move, Space jumps, E picks up/drops/delivers, Escape pauses. |

## Patterns and decisions

- **Navigation facade:** MenuPanel calls Navigation.startNight(1); Application hides guard checks, model/view construction, callback wiring, card replacement and audio coordination.

- **Audio facade:** AudioService hides decoding, clip caching, worker scheduling, gain/pan, layers and cleanup.

- **Decorator:** SceneRenderer is the shared contract; CameraSceneRenderer is the component; RendererDecorator wraps/delegates; VHS/dim add layers. GamePanel composes the real chains.

- **Builder:** SecretLevelBuilder accumulates named/repeated parts, then build creates SecretLevel; Application.createSecretLevel is the production recipe. This pattern is creational.

- **Painting template:** ScenePanel fixes transform/render/cleanup and supplies render/renderOverlay hooks.

## Rules and persistence

A normal night is 360 active seconds. Drain is 0.085 plus 0.22 for monitor or 0.20 for light, percentage points per active second. Monitor/light are mutually exclusive. Effective defense also requires the right direction. Exposure interruption recovers at 0.65 per second. Exact-deadline update steps prevent skipping events; morning has priority over power failure then attack.

The SQLite attempt insert and unlock update share one transaction. The readable log append is separate. A partial failure retains one UUID for retry: the database conflict and log marker avoid ordinary duplicate records. Settings use temporary-file replacement, atomically when supported. There is no live checkpoint restore, online authentication or complete cross-file transaction.

## Every Java file

### AppPaths.java

**Source:** [src/main/java/com/gudos/persistence/AppPaths.java:26](https://github.com/itsnold/FiveNightsAtGudos/blob/e9f0ff45350572e7ba5fd0ff6185ab1251514e2f/src/main/java/com/gudos/persistence/AppPaths.java#L26)

Resolves writable player-data paths outside the packaged game.

**Main path:** Main calls resolve; SettingsPanel displays the folder; Database, SettingsStore and ShiftLogStore use the three child paths.

**Design choice:** A JAR contains read-only resources; user saves need a writable external folder. gudo.dataDir supports isolated demo runs without changing ordinary player data.

**Find:** resolve; database/settings/shiftLog.

### Application.java

**Source:** [src/main/java/com/gudos/Application.java:51](https://github.com/itsnold/FiveNightsAtGudos/blob/e9f0ff45350572e7ba5fd0ff6185ab1251514e2f/src/main/java/com/gudos/Application.java#L51)

Coordinates screens, active attempts, audio and asynchronous storage; implements the Navigation facade contract.

**Main path:** Panels call Navigation; Application checks guards, creates or reuses the required model/view, changes the visible card, requests audio, and sends storage work to one IO worker.

**Design choice:** One coordinator keeps panels from opening database connections or managing the window. busy/savingRun and pendingRun protect saves from conflicting navigation. This is a practical application coordinator with a facade role, not a claim that every method is a GoF pattern.

### AssetManager.java

**Source:** [src/main/java/com/gudos/assets/AssetManager.java:35](https://github.com/itsnold/FiveNightsAtGudos/blob/e9f0ff45350572e7ba5fd0ff6185ab1251514e2f/src/main/java/com/gudos/assets/AssetManager.java#L35)

Loads bundled images, the scene manifest and decoded TV-noise frames, then serves cached lookups.

**Main path:** Main constructs one manager in the boot worker. Screens ask scene(scene,state), menuRender(index) or noise(frame); resource opens a classpath stream.

**Design choice:** Decode each referenced path once, require a default for all rooms, and fail startup for a listed missing asset. Unknown state names fall back to a clean default. This separates a bad key from a broken packaged asset.

**Find:** constructor, resource, scene, noise/menuRender.

### AudioService.java

**Source:** [src/main/java/com/gudos/audio/AudioService.java:39](https://github.com/itsnold/FiveNightsAtGudos/blob/e9f0ff45350572e7ba5fd0ff6185ab1251514e2f/src/main/java/com/gudos/audio/AudioService.java#L39)

Provides the audio facade over Java Sound and a serial audio worker.

**Main path:** Application or a panel callback requests play, ambience, menuMusic or gameplay. The worker lazily decodes/caches a Clip, sets gain/pan, starts or loops it, and later stops/closes it.

**Design choice:** One facade hides stream decoding, mixer access, caching, thread scheduling, layers and cleanup. Missing hardware is remembered as unavailable and gameplay continues. The worker is daemon; the storage worker is not.

**Find:** clip, play, ambience/gameplay, gain/pan, stop/close.

### BackdropPanel.java

**Source:** [src/main/java/com/gudos/ui/BackdropPanel.java:26](https://github.com/itsnold/FiveNightsAtGudos/blob/e9f0ff45350572e7ba5fd0ff6185ab1251514e2f/src/main/java/com/gudos/ui/BackdropPanel.java#L26)

Shares the dim office background and navigation chrome for secondary pages.

**Main path:** Extras, settings, guide/credits, selection, pause, records and result reuse backdrop or pageChrome.

**Design choice:** Avoid duplicating the same drawing and Back binding. It is an abstract base class; inheritance alone is not the custom Decorator pattern.

**Find:** backdrop, pageChrome, backButton.

### CameraMotion.java

**Source:** [src/main/java/com/gudos/game/CameraMotion.java:21](https://github.com/itsnold/FiveNightsAtGudos/blob/e9f0ff45350572e7ba5fd0ff6185ab1251514e2f/src/main/java/com/gudos/game/CameraMotion.java#L21)

Computes the automatic CCTV sweep from camera identity and active shift time.

**Main path:** CameraSceneRenderer and the secret-paper hit test call pan(scene,elapsed).

**Design choice:** A cosine gives smooth reversal. Each camera has a different phase and period. Using elapsed means unseen cameras continue their sweep and all sweeps freeze during pause.

**Find:** pan.

### CameraSceneRenderer.java

**Source:** [src/main/java/com/gudos/ui/rendering/CameraSceneRenderer.java:32](https://github.com/itsnold/FiveNightsAtGudos/blob/e9f0ff45350572e7ba5fd0ff6185ab1251514e2f/src/main/java/com/gudos/ui/rendering/CameraSceneRenderer.java#L32)

Draws the current CCTV room without VHS or switching effects.

**Main path:** GamePanel puts it inside VhsRendererDecorator; render reads selected camera, room state and sweep from the live session.

**Design choice:** Composition keeps one core room renderer separate from optional visual layers. The object is created once and reads updated model values each frame.

**Find:** constructor; render.

### Chiptune.java

**Source:** [src/main/java/com/gudos/audio/Chiptune.java:26](https://github.com/itsnold/FiveNightsAtGudos/blob/e9f0ff45350572e7ba5fd0ff6185ab1251514e2f/src/main/java/com/gudos/audio/Chiptune.java#L26)

Generates only the hidden minigame audio in memory.

**Main path:** create returns an AudioInputStream for SECRET_JUMP, SECRET_PICKUP or SECRET_AMBIENCE. AudioService opens it through the same clip path used for WAVs.

**Design choice:** Square waves and envelopes create small retro cues without another library. The eight-second drone uses a deterministic noise sequence and quiet endpoints. Current ordinary encounter sounds are bundled WAVs.

**Find:** create; ambience.

### Database.java

**Source:** [src/main/java/com/gudos/persistence/Database.java:38](https://github.com/itsnold/FiveNightsAtGudos/blob/e9f0ff45350572e7ba5fd0ff6185ab1251514e2f/src/main/java/com/gudos/persistence/Database.java#L38)

Persists local profiles, unlock summaries and attempt history using SQLite/JDBC.

**Main path:** Constructor configures/migrates schema; loadProgress selects/creates a profile; saveRun inserts an attempt and updates unlocks transactionally; loadRuns returns latest rows.

**Design choice:** Prepared statements bind player values. Foreign keys and CHECK/UNIQUE constraints protect relations and ranges. A UUID primary key supports idempotent retries; MAX preserves unlocks when replaying earlier nights.

**Find:** constructor/migrations, connect/profile, loadProgress, saveRun/loadRuns.

### DimmedRendererDecorator.java

**Source:** [src/main/java/com/gudos/ui/rendering/DimmedRendererDecorator.java:25](https://github.com/itsnold/FiveNightsAtGudos/blob/e9f0ff45350572e7ba5fd0ff6185ab1251514e2f/src/main/java/com/gudos/ui/rendering/DimmedRendererDecorator.java#L25)

Adds a translucent black layer after its wrapped renderer draws.

**Main path:** GamePanel puts it around the normal camera chain for switching interference.

**Design choice:** Validate alpha=0-255, delegate first, and draw using a copied graphics context. Layer order changes pixels.

**Find:** constructor; render.

### ExtrasPanel.java

**Source:** [src/main/java/com/gudos/ui/ExtrasPanel.java:23](https://github.com/itsnold/FiveNightsAtGudos/blob/e9f0ff45350572e7ba5fd0ff6185ab1251514e2f/src/main/java/com/gudos/ui/ExtrasPanel.java#L23)

Routes five text entries to secondary pages.

**Main path:** Select Night, How to Play, Shift Records, Settings and Credits map in order to Navigation.Page values.

**Design choice:** Keep a small routing screen without placing database or gameplay logic here. Capturing index per loop keeps each callback attached to the correct page.

**Find:** constructor page callbacks; render.

### GameButton.java

**Source:** [src/main/java/com/gudos/ui/GameButton.java:24](https://github.com/itsnold/FiveNightsAtGudos/blob/e9f0ff45350572e7ba5fd0ff6185ab1251514e2f/src/main/java/com/gudos/ui/GameButton.java#L24)

Uses a real JButton with custom visual styles and a supplied action.

**Main path:** Screens construct the button with label/style/callback; Swing fires ActionListener; the callback requests navigation or model input.

**Design choice:** Preserve enabled state, keyboard/button behavior and accessibility while drawing the game style. Painting only changes appearance, not game rules.

**Find:** constructor listener; paintComponent.

### GamePanel.java

**Source:** [src/main/java/com/gudos/ui/GamePanel.java:47](https://github.com/itsnold/FiveNightsAtGudos/blob/e9f0ff45350572e7ba5fd0ff6185ab1251514e2f/src/main/java/com/gudos/ui/GamePanel.java#L47)

Translates gameplay input, draws office/CCTV/HUD and detects model transitions for audio cues.

**Main path:** Application advances GameSession, then calls this tick; input callbacks request model changes; render draws the current model.

**Design choice:** Keep rules in GameSession and visual/input feedback here. Use held-key flags, callbacks and Supplier<Settings>. Compose camera rendering effects through the custom Decorator objects.

### GameSession.java

**Source:** [src/main/java/com/gudos/game/GameSession.java:28](https://github.com/itsnold/FiveNightsAtGudos/blob/e9f0ff45350572e7ba5fd0ff6185ab1251514e2f/src/main/java/com/gudos/game/GameSession.java#L28)

Owns the UI-independent rules and state of one normal night attempt.

**Main path:** Application.tick calls update; GamePanel requests monitor/light/camera/pan changes; update drains power, advances encounter events and freezes one RunRecord at an ending.

**Design choice:** The simulation can run without a Swing window. It splits long frames at deadlines and defines tie priority: morning, then power failure, then encounter attack. UUID identity makes one attempt stable across save retries.

### GudoEncounter.java

**Source:** [src/main/java/com/gudos/game/GudoEncounter.java:23](https://github.com/itsnold/FiveNightsAtGudos/blob/e9f0ff45350572e7ba5fd0ff6185ab1251514e2f/src/main/java/com/gudos/game/GudoEncounter.java#L23)

Implements one deterministic enemy encounter state machine and three approach routes.

**Main path:** GameSession asks untilEvent, advances that exact step, then checks caught. The UI reads location, approach, exposure/fade and attack countdown.

**Design choice:** routeIndex=-1 represents waiting, camera indices represent travel, atOffice represents defense, and caught is terminal. Successful flashlight exposure repels Gudo; interrupted exposure decays rather than instantly resetting.

**Find:** untilEvent, advance, location, faces, exposure/timer accessors.

### InfoPanel.java

**Source:** [src/main/java/com/gudos/ui/InfoPanel.java:25](https://github.com/itsnold/FiveNightsAtGudos/blob/e9f0ff45350572e7ba5fd0ff6185ab1251514e2f/src/main/java/com/gudos/ui/InfoPanel.java#L25)

Draws the in-game guide or credits.

**Main path:** Application.page(GUIDE/CREDITS) constructs the same class with credits=false/true.

**Design choice:** Static information can share one view and backdrop. Guide controls are descriptive text; GamePanel/ScenePanel implement the actual actions.

**Find:** render, renderGuide, renderCredits.

### JumpscarePanel.java

**Source:** [src/main/java/com/gudos/ui/JumpscarePanel.java:29](https://github.com/itsnold/FiveNightsAtGudos/blob/e9f0ff45350572e7ba5fd0ff6185ab1251514e2f/src/main/java/com/gudos/ui/JumpscarePanel.java#L29)

Animates the caught still and calls back once after 2.1 seconds.

**Main path:** Application.finishShift on CAUGHT starts sound/save and shows this; completion opens the prepared ResultPanel.

**Design choice:** Precompute a horizontal blur once. Zoom/shake reuse the still; reducedEffects keeps it stationary. Guard delivered avoids repeated navigation.

**Find:** constructor, tick, render.

### LoadingPanel.java

**Source:** [src/main/java/com/gudos/ui/LoadingPanel.java:22](https://github.com/itsnold/FiveNightsAtGudos/blob/e9f0ff45350572e7ba5fd0ff6185ab1251514e2f/src/main/java/com/gudos/ui/LoadingPanel.java#L22)

Draws a waiting message and animated dots.

**Main path:** Main uses it during boot; Application uses it for asynchronous records/progress reads.

**Design choice:** The panel does not perform slow work. The worker and coordinator decide when loading is complete.

**Find:** constructor; render.

### Main.java

**Source:** [src/main/java/com/gudos/Main.java:31](https://github.com/itsnold/FiveNightsAtGudos/blob/e9f0ff45350572e7ba5fd0ff6185ab1251514e2f/src/main/java/com/gudos/Main.java#L31)

Starts the desktop application and loads its dependencies.

**Main path:** main schedules launch on Swing; launch shows LoadingPanel; SwingWorker loads paths, settings, database, progress and images; done constructs Application.

**Design choice:** The window can repaint while disk and image loading happen. Boot is a record that transfers the loaded values together. Startup failures display a message and dispose the window.

**Find:** main, launch, SwingWorker.doInBackground/done, icon.

### MenuPanel.java

**Source:** [src/main/java/com/gudos/ui/MenuPanel.java:33](https://github.com/itsnold/FiveNightsAtGudos/blob/e9f0ff45350572e7ba5fd0ff6185ab1251514e2f/src/main/java/com/gudos/ui/MenuPanel.java#L33)

Displays the title menu, enabled Continue state and random portrait flicker.

**Main path:** New Game -> startNight(1); Continue -> startNight(progress.nextNight()); Extras -> page(EXTRAS); Quit -> quit.

**Design choice:** New Game deliberately preserves existing history. Continue is enabled after any recorded attempt, even a loss. Keyboard selection skips disabled entries. Flicker randomness is visual only.

**Find:** constructor, entry, move/select, tick, render/renderOverlay.

### Navigation.java

**Source:** [src/main/java/com/gudos/ui/Navigation.java:24](https://github.com/itsnold/FiveNightsAtGudos/blob/e9f0ff45350572e7ba5fd0ff6185ab1251514e2f/src/main/java/com/gudos/ui/Navigation.java#L24)

Defines the small screen-facing facade contract implemented by Application.

**Main path:** MenuPanel, ExtrasPanel, NightSelectPanel, SettingsPanel, PausePanel and ResultPanel depend on this interface.

**Design choice:** Screens request intent without knowing JFrame, storage workers, GameSession construction or AudioService internals. An interface alone is not proof of Facade; the hidden orchestration in Application supplies the evidence.

**Find:** menu, page, startNight, saveSettings, resumeShift, leaveShift, quit.

### NightSelectPanel.java

**Source:** [src/main/java/com/gudos/ui/NightSelectPanel.java:25](https://github.com/itsnold/FiveNightsAtGudos/blob/e9f0ff45350572e7ba5fd0ff6185ab1251514e2f/src/main/java/com/gudos/ui/NightSelectPanel.java#L25)

Shows five night buttons gated by the loaded progress.

**Main path:** Extras -> Select Night -> enabled Night N -> navigation.startNight(N).

**Design choice:** UI disables locked nights; Application rechecks unlocked before starting. Capture the loop value in selected so each listener keeps its own night.

**Find:** constructor eligibility/callbacks; render.

### Outcome.java

**Source:** [src/main/java/com/gudos/game/Outcome.java:20](https://github.com/itsnold/FiveNightsAtGudos/blob/e9f0ff45350572e7ba5fd0ff6185ab1251514e2f/src/main/java/com/gudos/game/Outcome.java#L20)

Defines the six final result categories stored in SQLite.

**Main path:** GameSession.finish selects the enum; Database stores name(); UI/logs display label().

**Design choice:** Stable machine names separate storage from readable labels. Only COMPLETED advances normal-night progress; MINIGAME_COMPLETED is a different outcome.

**Find:** stable enum name; label.

### PanoramaRenderer.java

**Source:** [src/main/java/com/gudos/assets/PanoramaRenderer.java:33](https://github.com/itsnold/FiveNightsAtGudos/blob/e9f0ff45350572e7ba5fd0ff6185ab1251514e2f/src/main/java/com/gudos/assets/PanoramaRenderer.java#L33)

Projects a wide room image into a curved-looking view and maps clicks back to source pixels.

**Main path:** render clamps/quantizes pan, prepares sampling maps when the source changes, fills a reused 854x480 image, and returns it. sourcePoint reverses the latest sampling for a hit test.

**Design choice:** atan maps horizontal rays and cos changes vertical sampling near the edges. Cached pixel maps and one output buffer reduce allocation. A separate renderer is needed when two outputs must coexist for a blend.

**Find:** prepare, render, sourcePoint.

### PausePanel.java

**Source:** [src/main/java/com/gudos/ui/PausePanel.java:25](https://github.com/itsnold/FiveNightsAtGudos/blob/e9f0ff45350572e7ba5fd0ff6185ab1251514e2f/src/main/java/com/gudos/ui/PausePanel.java#L25)

Displays a frozen active attempt and routes Resume or Return to Menu.

**Main path:** Escape in gameplay -> Application.pauseShift -> PausePanel; Resume/Escape -> resumeShift; Return to Menu -> leaveShift confirmation/save.

**Design choice:** Reuse the same model when resuming. Leaving creates ABANDONED, so pause is different from completing an attempt.

**Find:** constructor callbacks; render.

### Progress.java

**Source:** [src/main/java/com/gudos/persistence/Progress.java:21](https://github.com/itsnold/FiveNightsAtGudos/blob/e9f0ff45350572e7ba5fd0ff6185ab1251514e2f/src/main/java/com/gudos/persistence/Progress.java#L21)

Provides a loaded summary for menu, night-selection and records screens.

**Main path:** Database.loadProgress calculates counts and reads unlock fields; Application passes the value to panels.

**Design choice:** Keep one summarized value for UI decisions instead of running SQL during every repaint. EMPTY represents a fresh profile.

**Find:** EMPTY; unlocked.

### RecordsPanel.java

**Source:** [src/main/java/com/gudos/ui/RecordsPanel.java:35](https://github.com/itsnold/FiveNightsAtGudos/blob/e9f0ff45350572e7ba5fd0ff6185ab1251514e2f/src/main/java/com/gudos/ui/RecordsPanel.java#L35)

Displays the current profile summary and read-only recent attempts table.

**Main path:** Extras -> Shift Records -> Application IO load -> constructor builds JTable from the returned list.

**Design choice:** No SQL is run while rendering. Absolute Instants are displayed in the local system zone. Only latest 200 rows are loaded; summary counts include all stored attempts.

**Find:** constructor/table model; render.

### RendererDecorator.java

**Source:** [src/main/java/com/gudos/ui/rendering/RendererDecorator.java:24](https://github.com/itsnold/FiveNightsAtGudos/blob/e9f0ff45350572e7ba5fd0ff6185ab1251514e2f/src/main/java/com/gudos/ui/rendering/RendererDecorator.java#L24)

Implements SceneRenderer while retaining another SceneRenderer and delegating to it.

**Main path:** Concrete effect constructors call super(wrapped); render forwards the original graphics/time/reducedEffects.

**Design choice:** Being the same component type and containing one component lets decorators wrap decorators. Null rejection fails invalid chains early.

**Find:** constructor; render delegation.

### ResultPanel.java

**Source:** [src/main/java/com/gudos/ui/ResultPanel.java:28](https://github.com/itsnold/FiveNightsAtGudos/blob/e9f0ff45350572e7ba5fd0ff6185ab1251514e2f/src/main/java/com/gudos/ui/ResultPanel.java#L28)

Displays an ended RunRecord and the pending/saved/failed storage status.

**Main path:** Application.finishShift builds it; savePendingRun drives saving/saved/failed; buttons request next/replay/menu/retry.

**Design choice:** Disable next/menu while saving; allow retry with the same pending UUID. A completed Night 1-4 offers next; other endings offer the same night; Night 5 remains capped.

**Find:** saving, saved, failed, render.

### RunRecord.java

**Source:** [src/main/java/com/gudos/game/RunRecord.java:23](https://github.com/itsnold/FiveNightsAtGudos/blob/e9f0ff45350572e7ba5fd0ff6185ab1251514e2f/src/main/java/com/gudos/game/RunRecord.java#L23)

Carries the immutable snapshot of one ended attempt.

**Main path:** GameSession creates it once; Application retains it; Database and ShiftLogStore save the same ID and values; RecordsPanel displays it.

**Design choice:** A record supplies value equality and accessors. Capture final values once so retries cannot silently turn the attempt into a new one.

**Find:** generated accessors; durationLabel.

### Scene.java

**Source:** [src/main/java/com/gudos/game/Scene.java:22](https://github.com/itsnold/FiveNightsAtGudos/blob/e9f0ff45350572e7ba5fd0ff6185ab1251514e2f/src/main/java/com/gudos/game/Scene.java#L22)

Defines the office and six canonical camera rooms.

**Main path:** Game controls validate a camera number with Scene.camera(number); logic and AssetManager share the enum id.

**Design choice:** Central identifiers keep camera labels, route locations and image manifest keys aligned. CAM 01 is currently Front entrance, despite some older dining filenames/text.

**Find:** id/label/camera; static camera(number).

### ScenePanel.java

**Source:** [src/main/java/com/gudos/ui/ScenePanel.java:30](https://github.com/itsnold/FiveNightsAtGudos/blob/e9f0ff45350572e7ba5fd0ff6185ab1251514e2f/src/main/java/com/gudos/ui/ScenePanel.java#L30)

Provides a common scaled scene, real Swing component layout and keyboard routing.

**Main path:** Subclasses place controls in 1280x720 coordinates and implement render; Swing invokes paintComponent/paintChildren/doLayout.

**Design choice:** Uniform scale and centered offsets preserve aspect ratio. Reverse transforms preserve input accuracy. The final paint skeleton is a Template Method style design with subclass hooks.

**Find:** place, doLayout, scenePoint, paintComponent/paintChildren, bind/dispatchKey.

### SceneRenderer.java

**Source:** [src/main/java/com/gudos/ui/rendering/SceneRenderer.java:24](https://github.com/itsnold/FiveNightsAtGudos/blob/e9f0ff45350572e7ba5fd0ff6185ab1251514e2f/src/main/java/com/gudos/ui/rendering/SceneRenderer.java#L24)

Defines the component interface shared by a room renderer and all custom renderer decorators.

**Main path:** GamePanel stores composed implementations through SceneRenderer and calls render(graphics,time,reducedEffects).

**Design choice:** The same interface allows nested wrappers and substitutability. Without it, effect layers would need to know each concrete renderer type.

**Find:** render contract.

### SecretGame.java

**Source:** [src/main/java/com/gudos/game/minigame/SecretGame.java:31](https://github.com/itsnold/FiveNightsAtGudos/blob/e9f0ff45350572e7ba5fd0ff6185ab1251514e2f/src/main/java/com/gudos/game/minigame/SecretGame.java#L31)

Runs the hidden platformer rules without owning Swing or audio.

**Main path:** SecretPanel calls jump/action/update and draws read-only model values. The model integrates motion, patrols, bundle visibility and the pickup-triggered timer.

**Design choice:** Twenty-millisecond substeps reduce missed collisions. Witnesses detect the bundle center, not the player body. The same beam polygon is used for drawing and detection.

**Find:** action, jump, update, bundleHidden/witnessBeam.

### SecretLevel.java

**Source:** [src/main/java/com/gudos/game/minigame/SecretLevel.java:25](https://github.com/itsnold/FiveNightsAtGudos/blob/e9f0ff45350572e7ba5fd0ff6185ab1251514e2f/src/main/java/com/gudos/game/minigame/SecretLevel.java#L25)

Stores the level configuration produced by SecretLevelBuilder.

**Main path:** Application creates the product; SecretGame reads dimensions, spawn points, cover, platforms, destination and patrols.

**Design choice:** The constructor copies incoming Rectangle objects and lists, so later changes to builder inputs do not change this product. Returned Rectangles remain mutable; this is not deep immutability after access.

**Find:** compact constructor; Patrol.

### SecretLevelBuilder.java

**Source:** [src/main/java/com/gudos/game/minigame/SecretLevelBuilder.java:30](https://github.com/itsnold/FiveNightsAtGudos/blob/e9f0ff45350572e7ba5fd0ff6185ab1251514e2f/src/main/java/com/gudos/game/minigame/SecretLevelBuilder.java#L30)

Constructs a SecretLevel step by step through a fluent Builder.

**Main path:** Application.createSecretLevel chains width/player/bundle/destination/platform/cover/witness/timeLimit, then build.

**Design choice:** Optional repeated parts are easier to read than a large positional constructor. Each setter returns this; build validates completeness and creates the product. Validation covers selected constraints, not every possible numeric input.

**Find:** fluent part methods; build.

### SecretPanel.java

**Source:** [src/main/java/com/gudos/ui/SecretPanel.java:35](https://github.com/itsnold/FiveNightsAtGudos/blob/e9f0ff45350572e7ba5fd0ff6185ab1251514e2f/src/main/java/com/gudos/ui/SecretPanel.java#L35)

Controls and renders the scrolling pixel-art minigame.

**Main path:** GamePanel paper trigger -> Application.openSecret -> SecretPanel; key events call SecretGame; tick advances it; outcome freezes, silences and reports after three seconds.

**Design choice:** Use an offscreen 640x360 image enlarged nearest-neighbor. World scrolling is isolated from fixed HUD. reported and outcomeSilenced prevent duplicate callbacks/audio.

**Find:** bindings, tick, pause, silenceOnOutcome, draw helpers.

### Settings.java

**Source:** [src/main/java/com/gudos/persistence/Settings.java:21](https://github.com/itsnold/FiveNightsAtGudos/blob/e9f0ff45350572e7ba5fd0ff6185ab1251514e2f/src/main/java/com/gudos/persistence/Settings.java#L21)

Validates and carries immutable user preferences.

**Main path:** SettingsStore loads it; SettingsPanel creates a replacement; Application persists and applies it.

**Design choice:** Sanitize control characters, trim/fallback/cap the name, and clamp volume at the value boundary as well as the UI. A record replacement makes updates explicit.

**Find:** compact constructor; withFullscreen.

### SettingsPanel.java

**Source:** [src/main/java/com/gudos/ui/SettingsPanel.java:39](https://github.com/itsnold/FiveNightsAtGudos/blob/e9f0ff45350572e7ba5fd0ff6185ab1251514e2f/src/main/java/com/gudos/ui/SettingsPanel.java#L39)

Edits preferences in real Swing controls and submits an immutable Settings value.

**Main path:** Extras -> Settings -> edit name/volume/effects/fullscreen -> Apply -> navigation.saveSettings(values()).

**Design choice:** DocumentFilter sanitizes typed/pasted names, and Settings validates again. Applying is explicit; Back does not save edits. The clipboard button copies the resolved data folder.

**Find:** values, setSaving/setFullscreen, DocumentFilter, checkbox icons.

### SettingsStore.java

**Source:** [src/main/java/com/gudos/persistence/SettingsStore.java:32](https://github.com/itsnold/FiveNightsAtGudos/blob/e9f0ff45350572e7ba5fd0ff6185ab1251514e2f/src/main/java/com/gudos/persistence/SettingsStore.java#L32)

Reads and replaces the UTF-8 settings.txt preferences file.

**Main path:** load creates defaults on first launch or parses Properties. save writes a temporary neighbor, then replaces the real file.

**Design choice:** Use explicit true/false parsing, volume fallback and Settings normalization. Atomic replacement reduces truncated settings; filesystems without atomic move use a regular replacement fallback.

**Find:** load, booleanValue, save.

### ShiftLogStore.java

**Source:** [src/main/java/com/gudos/persistence/ShiftLogStore.java:32](https://github.com/itsnold/FiveNightsAtGudos/blob/e9f0ff45350572e7ba5fd0ff6185ab1251514e2f/src/main/java/com/gudos/persistence/ShiftLogStore.java#L32)

Appends a readable one-line report for each ended attempt.

**Main path:** Application.savePendingRun calls append after Database.saveRun.

**Design choice:** A run=UUID marker detects an already-written report on retry. Locale.ROOT stabilizes decimal formatting; UTF-8 preserves names. This is best-effort application-level deduplication, not a transactional database.

**Find:** append.

### Sound.java

**Source:** [src/main/java/com/gudos/audio/Sound.java:21](https://github.com/itsnold/FiveNightsAtGudos/blob/e9f0ff45350572e7ba5fd0ff6185ab1251514e2f/src/main/java/com/gudos/audio/Sound.java#L21)

Names each cue and stores its classpath path and relative gain.

**Main path:** GamePanel/SecretPanel/Application choose a Sound enum; AudioService reads path and gain.

**Design choice:** Enum values avoid scattered filename strings. Master volume and per-sound gain are separate. Null paths mark the three Chiptune sounds.

**Find:** enum path/gain accessors.

### Theme.java

**Source:** [src/main/java/com/gudos/ui/Theme.java:25](https://github.com/itsnold/FiveNightsAtGudos/blob/e9f0ff45350572e7ba5fd0ff6185ab1251514e2f/src/main/java/com/gudos/ui/Theme.java#L25)

Centralizes the bundled pixel font, colors and reusable drawing utilities.

**Main path:** Panels/buttons/renderers call static helpers instead of reproducing font/image/text code.

**Design choice:** One style source keeps UI consistent. Explicit wrap/ellipsize handle Java2D text limits. cover crops to fill while contain preserves the entire image.

**Find:** font helpers, text/paragraph/ellipsize, cover/contain, quality/vignette.

### VhsOverlay.java

**Source:** [src/main/java/com/gudos/ui/VhsOverlay.java:25](https://github.com/itsnold/FiveNightsAtGudos/blob/e9f0ff45350572e7ba5fd0ff6185ab1251514e2f/src/main/java/com/gudos/ui/VhsOverlay.java#L25)

Draws animated noise, scanlines and a rolling band as a visual utility.

**Main path:** Menu/GamePanel and VhsRendererDecorator call paint.

**Design choice:** It returns immediately for reduced effects and restores composite/interpolation. The decorator uses a copied Graphics2D so color/paint changes do not leak into later HUD drawing.

**Find:** paint.

### VhsRendererDecorator.java

**Source:** [src/main/java/com/gudos/ui/rendering/VhsRendererDecorator.java:27](https://github.com/itsnold/FiveNightsAtGudos/blob/e9f0ff45350572e7ba5fd0ff6185ab1251514e2f/src/main/java/com/gudos/ui/rendering/VhsRendererDecorator.java#L27)

Adds optional VHS interference after its wrapped renderer draws.

**Main path:** Normal camera uses opacity 0.25; switching adds an outer opacity 0.85 layer at speed 3.

**Design choice:** Delegate with original time so effect speed affects only this wrapper. Skip noise when reducedEffects or opacity=0, but still render the wrapped room.

**Find:** constructors; render.

## Practice questions

### 1. What is the main objective?

Survive a six-minute active night until 6 AM while conserving power and defending against Gudo.

### 2. Why use Java Swing?

The implementation uses Java's desktop UI and drawing APIs to keep the game a self-contained offline desktop application.

### 3. Where does execution begin?

com.gudos.Main.main schedules launch on the Swing EDT.

### 4. Is GamePanel the entire game logic?

No. It translates input and draws; GameSession and GudoEncounter enforce normal-night rules.

### 5. Explain New Game as a facade.

The button calls one Navigation.startNight(1) operation. Application hides the checks, new session/view creation, callback wiring, navigation and audio coordination.

### 6. Does the interface itself make it a facade?

The interface gives the small contract; the implementation's hidden subsystem coordination is the evidence of the facade.

### 7. Why no separate newGame method?

New Game is a label. Starting Night 1 is the same operation as starting a selected eligible night with argument one.

### 8. Does New Game delete progress?

It starts a new Night 1 attempt and preserves records/unlocks.

### 9. What exactly does Continue continue?

Progression, not a live checkpoint. It starts a fresh attempt of progress.nextNight.

### 10. What are your structural patterns?

Facade at navigation/audio boundaries and a custom Decorator for CCTV effects.

### 11. Explain the audio facade beyond its name.

One playback request hides decoding, clip caching, volume/pan controls, looping, worker scheduling and cleanup.

### 12. Show the Decorator requirements.

Every wrapper implements SceneRenderer, holds another SceneRenderer, delegates its render, then adds an effect.

### 13. Why not just subclass a renderer for every effect combination?

Composition lets us stack and order effects without creating a new subclass for each combination.

### 14. Does decorator order matter?

Yes. Dimming after noise darkens the noise; noise drawn after dimming appears over the darkened room.

### 15. Why copy Graphics2D?

An effect can change paint/composite/hints on its own context and dispose it without leaking that effect state into later HUD rendering.

### 16. Why does reduced mode still render the room?

VHS delegates first, then returns before adding optional noise. GamePanel also skips the switching chain.

### 17. Explain the Builder with real participants.

SecretLevelBuilder collects named level parts; Application.createSecretLevel is the production recipe; build creates SecretLevel, which SecretGame uses.

### 18. Is the level fully immutable?

It copies construction inputs and lists, but Rectangle objects exposed through accessors remain mutable.

### 19. Is this the State pattern?

It uses finite state-machine logic, but not separate polymorphic GoF State objects.

### 20. Where is encapsulation?

GameSession fields are private; controls change them through guarded methods.

### 21. Where is runtime polymorphism?

Application can tick/render through a ScenePanel reference, and GamePanel can call a SceneRenderer whether it is plain or decorated.

### 22. Are you using a Singleton?

There is one shared AssetManager in the normal boot flow, but no enforced Singleton class.

### 23. Why callbacks instead of giving a panel all services?

The panel can request just the behavior it needs, such as sound or pause, without owning storage/audio/window lifecycles.

### 24. How does a button actually start anything?

GameButton installs an ActionListener that runs the supplied Runnable. The screen passes the actual navigation/model callback.

### 25. How is a night timed?

Application supplies measured active deltas; GameSession accumulates them to its normal 360-second duration.

### 26. Why not subtract one fixed amount per frame?

Frame delivery can vary, so time and power use actual supplied seconds and exact event-boundary steps.

### 27. What if attack and morning happen together?

The model explicitly gives morning priority, then power failure, then attack.

### 28. How does power work?

0.085 percentage points per active second, plus 0.22 for the monitor or 0.20 for light.

### 29. Is Gudo random or AI-trained?

He follows deterministic route and timing rules, not a trained model.

### 30. Why does light fail while I am holding F?

The monitor must be down, the session active and the office view facing the current approach. Then exposure must reach its threshold before the attack deadline.

### 31. Why does he reappear when I look away?

Interrupted effective exposure decays at 0.65 per second, so the fade reverses toward the occupied room.

### 32. Why can't he appear in two camera rooms at once?

The encounter returns one current location; sceneState checks that location for occupancy.

### 33. How does CCTV move when I am not watching?

Its pan is a pure function of camera identity and session elapsed time, not accumulated viewing time.

### 34. Is the office actually 3D?

The visual effect projects a wide 2D panorama using angle-based sampling. There is no 3D model/mesh here.

### 35. Why separate clean and occupied renderers?

Each PanoramaRenderer returns its reused mutable output. Two instances keep both projected images available during the blend.

### 36. How does the paper click remain aligned during camera sweep?

We reverse the window-to-scene transform, then reverse the current projection to a source image pixel and test the source-fraction hitbox.

### 37. What starts the hidden timer?

The first successful bundle pickup, not opening the minigame.

### 38. What exactly do witnesses detect?

The bundle center, carried or dropped, inside their direction-facing triangle unless a cover rectangle contains that center.

### 39. How do you win the minigame?

Carry the bundle into the stockroom destination and press E. Delivery is checked before dropping.

### 40. Does a secret win unlock the next night?

No. Only normal Outcome.COMPLETED executes the unlock update.

### 41. How does minigame collision handle a long frame?

SecretGame splits its supplied delta into steps no larger than0.02 seconds to reduce missed platforms or beam crossings.

### 42. Why SQLite and also text files?

SQLite is authoritative relational progress/history; settings are human-editable preferences; the log is a readable report.

### 43. Explain the database relations.

One profile has one progress summary and many night_runs. progress.profile_id is its primary/foreign key; each run has its own UUID and a profile foreign key.

### 44. Why PreparedStatement?

Player values are bound separately from SQL syntax, including quoted names.

### 45. What is atomic about saving?

The attempt insert and unlock update commit or roll back together inside SQLite.

### 46. Are SQLite and shift-log.txt one transaction?

No. They are separate writes coordinated by a retryable application workflow.

### 47. How do you stop duplicate records?

A stable run UUID is the night_runs primary key with ON CONFLICT DO NOTHING; the text report checks the same UUID marker.

### 48. Why not advance progress on every saved attempt?

An attempt count includes failures and secrets. Progression represents normal survival, so the update requires a newly inserted COMPLETED result.

### 49. Why is the table limited to 200?

The latest 200 rows keep the records view manageable; older rows are retained and totals still count them.

### 50. Are profile names secure accounts?

No. They select local records on this computer. There is no authentication or online service.

### 51. What happens if settings values are invalid?

Parsing uses defined fallbacks and Settings sanitizes/clamps the final values. IO failures still report errors.

### 52. Why a temporary settings file?

Write a complete replacement first, then swap it over the real file, atomically where supported.

### 53. Why can't an unexpected window focus loss keep the flashlight on?

Application pauses the active panel and clears held controls; GameSession.pause forces light off.

### 54. Does closing always save?

Normal close/quit asks and waits to save an active abandoned attempt. Force-kill or a process crash cannot guarantee that save.

### 55. What if the audio device is missing?

The failed cue is remembered and playback returns without stopping gameplay.

### 56. How are resources packaged for offline play?

Maven Shade bundles dependencies and runtime resources into the distributable JAR. AssetManager uses classpath resource lookup, so the built game does not download assets while playing.

### 57. Where should a new camera image/state be added?

Put the image under the resources image folder, add a scene.state mapping in scenes.properties and use the state through the model.

### 58. How would you add another enemy?

It would require extending the model for multiple independent encounters/occupancy and then adapting rendering, cues and state updates.

### 59. What are the main limitations?

Local-only profiles, no mid-shift restore, one deterministic enemy, hardware-dependent audio, basic minigame input validation, and separate DB/text writes.

### 60. How do you know the added comments did not change the game?

All 45 application Java files retain identical parsed executable syntax, and all 70 compiled production class files match the original byte-for-byte with debug metadata disabled.

## Validation and snapshot

Source links pin to `e9f0ff45350572e7ba5fd0ff6185ab1251514e2f`. Gameplay logic is unchanged by the documentation edits. Syntax comparison covers all 45 application Java files; 70 production class files match original bytes with debug metadata disabled (available JDK 21 compiler, source/target 17). The complete Maven verification could not download dependencies because the Maven host was unreachable, so no complete Maven package is claimed. Screenshots are real headless Swing renders with controlled model state, not certification of native display or speaker behavior.
