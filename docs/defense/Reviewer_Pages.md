# Reviewer: 42-page edition

This page index reproduces the text of the attached 42-page study guide. Each numbered heading matches the PDF viewer page number and the printed page number. Source comments link to these headings so the explanation can be opened directly on GitHub. Tables retain their PDF text layout; use the PDF for the designed layout.

[Open/download the matching 42-page PDF](https://chatgpt.com/api/library/files/libfile_c59b1402de108191a3cc566de1df1139/download). This link requires access to the original ChatGPT file. The page excerpts below are available with the repository.

The PDF describes source snapshot `e9f0ff45`. Its source links intentionally remain pinned to that snapshot, so adding comments does not invalidate its original line references. The PDF itself has not been repaginated.

## Page 1

PDF page **1 / 42**.

```text
JAVA DESKTOP PROJECT         /   STUDY EDITION




FIVE NIGHTS
AT GUDO’S
Architecture, source code and demonstration guide

A compact reference for understanding the implementation, finding each feature, explaining the impor-
tant decisions and preparing for project questions.




 45 application Java files        Runtime features             One coherent walkthrough
 Rules, screens, rendering, audio Gameplay, cameras, sound and Click paths, worked examples
 and persistence                  local data                   and source links




  How this edition stays concise
  Every Java file is covered. Repeated getter explanations and full per-method narration are kept
  in the source comments; the guide concentrates on responsibilities, important methods, execution
  order, reasons and demonstrations.

15 November 2026     • Checked source: 38bad499
```

## Page 2

PDF page **2 / 42**.

```text
Reading map

Choose a route

 Need                          Read                                                       Page
 Start from zero               Project vocabulary and ownership                           3
 Run and navigate              Build, locate code, normal/secret demonstrations           4–7
 Explain the rules             Clock/power, Gudo, input and projection                    8–10
 Explain saves                 Schema, transaction, text files and retry                  11–12
 Explain patterns              Both facades, Decorator, Builder and OOP                   13–15
 Trace major classes           Application, GameSession and GamePanel                     17–19
 Find a Java file              All application source-file cards                          20–33
 Practice questions            Sixty short, code-grounded answers                         34–39
 Rehearse                      Presentation sequence and limits                           40–41
 Locate quickly                Alphabetical file and page index                           42

A useful answer structure

What it does → where it lives → how the code works → why the arrangement helps. For
example: “The flashlight protects the oﬀice. GameSession and GudoEncounter enforce the rule. Correct-
facing light builds exposure until a threshold; interruption reduces it. Keeping this outside drawing code
makes the rule testable.”

Use the clickable references

A source link opens the exact annotated revision. In an IDE, expand src/main/java/com/gudos, open
the named file and search for the method declaration with Ctrl+F. If line numbers change later, the
exact filename and method name still locate the feature.

  The three facts that prevent confusing answers
  New Game preserves history. Continue starts a fresh attempt rather than restoring a checkpoint. A
  hidden-minigame win does not unlock the next normal night.

Scope of explanations

Rationales describe benefits and tradeoffs visible in the implementation, not undocumented claims about
the original author’s intentions. Screenshot examples are actual Swing renders with controlled demon-
stration state; they do not certify native fullscreen, focus handling or speaker output on every computer.
```

## Page 3

PDF page **3 / 42**.

```text
The whole project in one page

Purpose. An offline Java survival game set in a carinderia. Last six active minutes, displayed as 12
AM–6 AM, manage power and repel one enemy. Five normal nights unlock progressively; a hidden
platformer is a separate interaction.

 Owner                            Responsibility
 Main                             Creates the loading window and prepares dependencies.
 Application                      Coordinates screens, attempts, audio, storage and orderly exit.
 GameSession/GudoEncoun           Normal-night rules: time, power, routes, directional defense and final out-
 ter                              come.
 GamePanel/SecretPanel            Translate input and draw the current model.
 SecretGame/SecretLevel           Changing platformer state versus its configuration.
 Database/textstores              Local progress/history, preferences and readable reports.
 AssetManager/renderers           Cached resources, visual composition and sound playback.
 /AudioService


Essential Java vocabulary

Class / object
          A type and one instance of that type: newGameSession(1) creates an attempt.
Method / field
        An operation versus retained data: toggleMonitor() changes accepted monitor state; power
        stores a number.
Interface
             A small callable contract: panels depend on Navigation; renderers share SceneRenderer.
Callback Behavior supplied for later use: ()->navigation.startNight(1) runs when the button is
         activated.
Enum / record
        Named finite values versus concise data carriers: Outcome.CAUGHT; RunRecord.
EDT / executor
        Swing’s UI thread versus a worker queue. UI changes return to EDT; IO and sound use
        workers.
Transaction / retry
         Related database changes commit together; an idempotent retry repeats a request without
         duplicating its intended result.


  Keep these values separate
  Settings are preferences. Progress is the loaded unlock/count summary. GameSession is one
  live attempt. RunRecord is its immutable final receipt, not a resumable live save.

Open in source: Main.java:31; Application.java:51; game/GameSession.java:28; game/RunRecord.java:23
```

## Page 4

PDF page **4 / 42**.

```text
Build, launch and find the implementation

Windows preparation

1. Open the repository root containing pom.xml, run.cmd, mvnw.cmd and src. Install a JDK 17 or newer;
   building needs compiler tooling.
2. Build the application before the demonstration. First build needs internet for Maven/dependencies.

.\mvnw.cmd clean package


3. Double-click run.cmd for ordinary launch. It chooses a JDK, packages the application and runs the
   game. The command above performs the same packaging explicitly.
4. For an isolated demo, launch the distributable with its own data directory:

java --enable-native-access=ALL-UNNAMED `
  "-Dgudo.dataDir=target/defense-demo-data" `
  -jar target/five-nights-at-gudos.jar


The backticks continue a command in PowerShell. On macOS/Linux build with sh./mvnwcleanpackage;
macOS also has run.command. The shaded five-nights-at-gudos.jar bundles dependencies/resources
and runs offline after building.

Find a feature, step by step

1. Open the entire root as a Maven project. Expand src/main/java/com/gudos.
2. Choose game for rules, ui for screens, persistence for saves, assets for images/projection or audio
   for playback.
3. To explain a button, open its panel constructor and locate the callback passed to GameButton. Follow
   that callback into Application or the model.
4. Search the exact method name. Read its guard first, then changed state, then called operations. Use
   Go to Definition if available.
5. Follow the call into the model and identify its guard, state changes and next operation. Those
   implementation details are the evidence for the behavior.

Explain the build accurately

pom.xml targets Java 17. SQLite JDBC supplies embedded storage and the selected SLF4J backend
handles logging integration. The compiler builds application classes; Shade produces the distributable
with com.gudos.Main as its manifest entry point. Swing/Java2D provide the UI and drawing stack; no
browser or separate game engine is involved.
Open in source: Main.java:43; Main.java:54
```

## Page 5

PDF page **5 / 42**.

```text
Normal shift: the exact demonstration path




Title: New Game, Continue and Extras                           Office: clock, power, light and monitor tab

1. New Game. Click it, or use Up/Down and Enter. It calls navigation.startNight(1). Application
   checks guards, constructs a fresh model/view, wires callbacks, switches screen and requests audio.
   History remains.
2. Look around. Hold A/D or arrows, or move the pointer near either horizontal edge above the
   bottom controls. Oﬀice pan eases smoothly instead of snapping.
3. CCTV. Press Space/Tab or the bottom chevron tab. Press 1–6 or click CAM buttons. Each feed
   sweeps automatically using the active shift clock, not the mouse.
4. Light. Lower the monitor; hold F or the light button, then release. Raised monitor blocks light in
   the model. Holding F while facing the wrong door still costs power.
5. First defense. On Night 1: CAM04 at 18 s, CAM03 at 29 s, CAM02 at 40 s, left oﬀice at 51 s.
   Lower the monitor, face left and hold F for at least 1.6 effective seconds. Release after he disappears.
6. Pause. Escape opens PausePanel. Escape/Resume restores the same in-memory attempt. Clock,
   drain, enemy and camera sweep stop while paused.
7. Leave. Escape → RETURN TO MENU → confirm Yes. The attempt becomes ABANDONED,
   saves, then returns to title. It does not unlock a night.

  Continue and replay
  Continue enables after any recorded attempt and starts progress.nextNight() fresh. Extras →
  Select Night offers unlocked nights; Application rechecks eligibility. Replaying an earlier night
  cannot reduce saved unlocks.

Ending demonstration. Normal victory needs six active minutes. Ignoring the first Night 1 encounter
naturally catches at 60 s, then a 2.1 s scare plays while saving starts. Power failure is separate; there is
no runtime “force power failure” button.
Open in source: ui/MenuPanel.java:86; Application.java:306; ui/GamePanel.java:266; Application.java:436
```

## Page 6

PDF page **6 / 42**.

```text
Preferences, records and profile navigation




Extras → Settings                                              Extras → Shift Records


Apply settings and select a local profile

1. Title → Extras → Settings. Edit player name, volume, reduced effects or fullscreen.
2. Click APPLY SETTINGS and wait for title. Application loads/creates the named profile and writes
   preferences on IO; success replaces loaded settings/progress and updates volume/display.
3. Reopen Settings or restart to confirm persistence. Back without Apply discards form edits. F11
   separately persists/toggles fullscreen.
4. A different name selects separate local records. Names are not authenticated accounts. The model
   removes control characters, trims/caps names and clamps volume.

Locate the actual save files

1. Settings → COPY DATA FOLDER. Paste into File Explorer’s address bar. The explicit demo-
   directory override wins over ordinary locations.
2. Open settings.txt for preferences and shift-log.txt for readable receipts. Inspect a copy of gudo.db
   in a SQLite viewer if needed.
3. Title → Extras → Shift Records. Application loads rows off EDT; RecordsPanel displays an already-
   loaded list. The table is read-only.

  Totals versus rows
  The screen shows the latest 200 attempts, but older rows remain stored and totals count them.
  Only normal COMPLETED outcomes increase completed counts/progression. Names use SQLite
  NOCASE behavior, not full Unicode account identity.

Other Extras routes. Select Night gates buttons from Progress; How to Play and Credits are static
InfoPanel variants. Back/Escape on these secondary pages returns to the title, not to a previous-page
history stack.
Open in source: Application.java:274; Application.java:529; ui/SettingsPanel.java:192; persistence/Database.java:283
```

## Page 7

PDF page **7 / 42**.

```text
Hidden game: trigger, rules and restart




CAM04 stockroom: click the small left-side paper               The scrolling hidden minigame

1. Start Night 1–4, raise monitor and choose CAM04. Left-click the actual paper five valid times. Follow
   its position as the feed sweeps; camera-map clicks are not the trigger.
2. The code reverse-maps the click into source-image pixels. Valid bounds are x=28.5–32.5% and y=24–
   40% of source size. Invalid clicks neither increment nor reset valid count. The fifth valid click runs
   the callback. Application rejects Night 5.
3. Application releases normal controls, pauses the normal session, stops audio and constructs the hidden
   model/panel. Construction failure restores the existing normal shift.
4. A/D or arrows move, Space jumps and E picks up/drops/delivers. Escape pauses this minigame;
   focus loss also pauses and clears held keys.
5. Explore before pickup freely. The first successful pickup sets started=true; 60 s then count down
   even after dropping or hiding. Re-pickup never resets time.
6. Witnesses detect the bundle center (carried or dropped) in their triangular beams unless a cover
   rectangle contains it. The player’s unladen body alone does not cause detection. The view draws the
   same beam polygon the model checks.
7. Reach the right-hand stockroom while carrying and press E. Delivery is checked before dropping.
   Timeout or beam detection loses.
8. Both outcomes stop sound and freeze with fixed signal lines for three unpaused seconds. Application
   saves the secret outcome and waits for both stores, then starts a fresh attempt of the same normal
   night.

  Important distinction
  MINIGAME_COMPLETED is not normal COMPLETED. It does not unlock the next night and
  does not permanently prevent replay. The misleadingly named nextNightAfterSecret stores the
  same night to restart.

Open in source: ui/GamePanel.java:183; Application.java:348; game/minigame/SecretGame.java:92; Application.java:396
```

## Page 8

PDF page **8 / 42**.

```text
Clock, power and event-boundary updates

Six active minutes, six fictional hours

A normal duration is D = 360 seconds. Progress is min(1, t/D). The HUD maps it to 12 AM–6
AM; preciseClock adds fictional minutes. RunRecord duration instead formats actual active seconds.
Wall-clock Instants can be far apart after a long pause without increasing active duration.

Power is a rate, not a bar count

With monitor flag M and light flag L:

                            d = 0.085 + 0.22M + 0.20L,            Pnew = max(0, P − d∆t).

The model excludes simultaneous M = L = 1. Idle costs 0.085, monitor 0.305 and light 0.285 percentage
points per active second. Ten idle plus ten monitor seconds leave 100 − 0.85 − 3.05 = 96.10%.

 Example                        Calculation                                                  Meaning
 Idle full shift                100 − 0.085(360) = 69.4                                      Defense adds extra
                                                                                             light cost.
 Always-monitor budget          100/0.305 ≈ 327.87 s                                         Gudo may catch
                                                                                             earlier.
 Usage display                  1 + 2M + 2L                                                  Normally 1 or 3 bars,
                                                                                             not drain.

Why a frame is split into smaller events

1. Reject a negative/nonfinite delta; ignore paused or finished sessions.
2. Compute morning time remaining, power-depletion time and Gudo’s next boundary. Effective light
   also requires the correct approach direction.
3. Advance by the minimum of those deadlines and remaining frame time. Subtract drain for that exact
   step.
4. Test morning first, power failure second; otherwise advance the encounter and check caught. Repeat
   for the rest of the frame.

This prevents a large supplied delta from skipping an arrival, attack or repel. On a simultaneous deadline
morning wins, then power failure, then attack. finish freezes one immutable RunRecord and disables
equipment; later finish requests cannot change it.

  UI timer versus model accuracy
  Application requests ticks around every 33 ms, measures nanoTime and caps one delayed delta at
  0.25 s. Severe stalls can make simulation lag real wall time. Within each supplied delta, the model
  still handles exact boundaries.

Open in source: game/GameSession.java:80; game/GameSession.java:215; game/GameSession.java:111
```

## Page 9

PDF page **9 / 42**.

```text
Gudo: routes, difficulty and effective light

 Approach              Route then oﬀice                                                        Facing condition
 LEFT                  04 stockroom →03 kitchen →02 counter                                    p ≤ 0.30; target 0.12
 RIGHT                 06 alley →05 corridor                                                   p ≥ 0.70; target 0.88
 FRONT                 01 front entrance                                                       |p − 0.50| ≤ 0.22
Pan p is normalized: 0=left, 0.5=center, 1=right. Only the current approach is checked; small edge
overlaps do not create multiple simultaneous enemies. CAM01 currently means front entrance, despite
some retained dining filenames/older prose.

State transition, in order

Waiting uses routeIndex= −1 and location=null. When timer expires, advance to the next camera; after
the route, enter oﬀice with exposure 0 and an attack countdown. At oﬀice, correct light adds exposure.
Releasing/looking away removes exposure at 0.65 per second, floored at zero. Enough exposure repels;
otherwise expired timer catches. After repel: clear occupancy, reset exposure/routeIndex, increment
repelled and cycle LEFT → RIGHT → FRONT. Location is singular, so the previous room automatically
becomes clean/custom again.

 Night First wait Initial route step Attack window Light needed
    1         18.0 s               11.0 s                   9.0 s                 1.6 s
    2          16.0                 9.7                      8.2                   1.8
    3          14.0                 8.4                      7.4                   2.0
    4          12.0                 7.1                      6.6                   2.2
    5          10.0                 5.8                      5.8                   2.4

                 Tf lash = 1.6 + 0.2(n − 1),                                 Tattack = 9 − 0.8(n − 1),
                   Tstep = max(4, 11 − 1.3(n − 1) − min(3, 0.3r)),
                  Twait = max(5, 13 − (n − 1) − min(4, r)).
Here n is night and r is repelled count. First-wait formula is 18 − 2(n − 1); later waits use the separate
formula above.

  Worked interrupted defense
  Night 1: 0.8 s effective exposure gives 50% fade. Release for 0.4 s loses 0.4(0.65) = 0.26, leaving 0.54
  s. Another 1.06 effective seconds is needed, while the attack countdown continues.

First Night 1 timeline:18 s CAM04;29 s CAM03;40 s CAM02;51 s left oﬀice; earliest immediate repel
52.6 s; unattended caught 60 s.
Open in source: game/GudoEncounter.java:114; game/GudoEncounter.java:72; game/GameSession.java:191
```

## Page 10

PDF page **10 / 42**.

```text
Input, scaling and image projection

Two coordinate transforms

ScenePanel defines a logical 1280 × 720 scene. For physical width w, height h:
                                                                     x − ox             y − oy
                            s = min(w/1280, h/720),           xs =          ,    ys =          .
                                                                       s                  s
Offsets center the scaled scene; doLayout applies the same transform to real Swing controls. Storing
original bounds/font avoids cumulative resize error. scenePoint reverses the transform for mouse input.

CCTV and office use different movement

Oﬀice pan is requested by keys/pointer edges and eased with a factor 1 − e−9∆t . The panel then sends
it to GameSession for directional checks. CCTV instead uses CameraMotion:

                              p = 0.5 − 0.46 cos(2πϕ),       ϕ = t/(22 + 2.3c) + 0.137c.

Camera number c changes phase/period; p ∈ [0.04, 0.96]. Session time makes unseen feeds continue
sweeping and all sweeps freeze on pause.

A panorama effect, not a 3D room

PanoramaRenderer uses atan for horizontal rays and cos for vertical sampling near edges. It writes a
reused 854×480 image scaled to the logical viewport. Source pixels/lookups are cached; pan is quantized
to2049 positions. This differs from Theme.cover, which simply fills/crops an image rectangle. HUD text
never goes through room projection. Buffer ownership. render returns the same mutable output.
Clean and occupied room images therefore need separate projection instances to coexist during a fade.
Occupied alpha = 1−fade is blended over clean.

Why the paper click stays attached to the artwork

Convert event pixels →logical scene →projection pixel. Recompute CAM04 at current elapsed sweep,
call sourcePoint and test the original-image fractional hitbox. A fixed window rectangle would drift
during sweep/resizing.

  Reduced effects preserve rules
  Optional flicker, noise, blinking, intro and scare motion are reduced/bypassed. A disrupted feed
  still shows a static SIGNAL LOST screen for its two active seconds, preserving information hiding.
  Gameplay timing and power are unchanged.

Open in source: ui/ScenePanel.java:89; assets/PanoramaRenderer.java:68; ui/GamePanel.java:183
```

## Page 11

PDF page **11 / 42**.

```text
Three save files and the relational schema

 File                           Actual responsibility
 settings.txt                   UTF-8 preference keys: playerName, volume, reducedEffects, fullscreen.
 gudo.db                        Authoritative local profiles, unlock summary and ended attempt history.
 shift-log.txt                  Readable per-attempt report; not a checkpoint or progress replacement.
Directory priority. Explicit gudo.dataDir; else LOCALAPPDATA/FiveNightsAtGudos when that
environment value exists; else user.home/.five-nights-at-gudos. Files are outside the read-only JAR.
SQLite WAL/SHM sidecars are normal in WAL mode.

Tables and relationships

 Table                      Keys, data and constraints
 profiles                   id INTEGER primary key; name required, UNIQUE COLLATE NOCASE; cre-
                            ated_at required.
 progress                   profile_id primary/foreign key; next_night 1–5 default 1; highest_completed
                            0–5 default 0; updated_at required.
 night_runs                 id TEXT UUID primary key; profile_id foreign key; night 1–5;
                            elapsed_seconds≥ 0; power_remaining 0–100; six allowed outcome names;
                            start/end Instant text.
One initialized profile has one progress row and many runs. Index runs_by_profile(profile_id,ended
_atDESC) supports the recent-history query. loadRuns returns newest 200, while COUNT queries include
older retained rows.

Constraint vocabulary

PK: unique row identity. FK: points to another table’s row. NOT NULL: value required. U-
NIQUE: duplicates rejected under that comparison. CHECK: value must satisfy a condition. Index:
lookup/order structure, not a new copy of game logic.

Connections and safe values

Each operation uses a short-lived connection, enabling foreign keys and a 5000 ms busy timeout. Pre-
paredStatement binds player values separately from SQL syntax, including quoted names. It does not
provide encryption/authentication. An internally formatted migration table name is not user input.

  Local identity, not an account system
  Changing playerName selects/creates a local profile. SQLite’s built-in NOCASE is not complete
  Unicode case folding. There is no password table, online authentication or synchronization here.

Open in source: persistence/AppPaths.java:40; persistence/Database.java:133; persistence/Database.java:154; persistence/Data
base.java:203
```

## Page 12

PDF page **12 / 42**.

```text
Save, retry, migration and preference writes

An ended attempt, from model to disk

1. GameSession finishes once, creating its stable UUID RunRecord. Application retains it and the profile
   captured when the shift began.
2. ResultPanel disables conflicting navigation. The IO worker selects/creates profile/progress, then
   begins the attempt/unlock transaction.
3. INSERT night_runs uses ON CONFLICT(id) DO NOTHING. If newly inserted and out-
   come=COMPLETED, update unlock fields using MAX and cap next night at 5.
4. Commit those two database changes together, or rollback on SQLException. Close connection and
   reload Progress. Earlier profile setup is outside this transaction.
5. Append the readable log line after checking its run=UUID marker. Return success to EDT only when
   both operations finish.
6. Enable next/menu or honor queued exit/menu/same-night restart. Failure retains the pending result
   and exposes Retry Save.

  A partial save is possible
  SQLite may commit before text append fails. Retry uses the same UUID: the database conflict
  inserts nothing, and the log marker prevents the ordinary duplicate report. This is retry coordination
  across separate stores, not one atomic cross-file transaction. A crash during plain append may leave
  a partial line.

Replay and result invariants

Already next_night4 plus a Night 1 replay stays4 because MAX(4,2)=4. Completed Night 5 stays
capped at 5. CAUGHT, POWER_OUT, ABANDONED and both secret outcomes record history but
do not unlock a normal night. Normal close can save ABANDONED after confirmation; force-kill cannot
guarantee an unfinished attempt is saved.

Schema and text operations

Database rejects schema versions above5. Supported legacy outcome-constrained tables are rebuilt/-
copied inside migration transactions; old rows and progress are preserved. Legacy secret flags remain
but no longer gate replay. The entire constructor is not one all-encompassing transaction. SettingsStore
writes all keys to a temporary neighbor, then atomically replaces settings.txt where supported; other-
wise regular replacement is used and temporary residue is cleaned. Loading uses parse fallbacks plus
Settings normalization; IO failures still propagate. ShiftLogStore appends reports and scans existing
UUID markers; it is not multi-process crash-proof journaling.
Open in source: Application.java:486; persistence/Database.java:231; persistence/SettingsStore.java:83; persistence/ShiftLog
Store.java:47
```

## Page 13

PDF page **13 / 42**.

```text
Facade: New Game navigation and audio

Meaning and participants

A facade exposes a small client operation while hiding coordination of a larger subsystem. Navigation is
the screen-facing contract; Application is its concrete coordinator/facade. A method name or interface
alone is not suﬀicient proof: the implementation must hide meaningful work.

 Client action                    Request through the same boundary
 New Game                         navigation.startNight(1)
 Continue                         navigation.startNight(progress.nextNight())
 Select Night                     navigation.startNight(selected)
 Next / Play Again                navigation.startNight(nextNight)


What Application.startNight hides

1. Guard busy/save/active-session/locked-night/incomplete-save cases.
2. Clear prior result state and capture the active profile name.
3. Construct GameSession with a new attempt identity.
4. Construct GamePanel with current-settings, sound and pause callbacks; attach hidden-game callback.
5. Replace the visible card, stop obsolete effects and request gameplay audio.

The menu supplies intention, not window/storage/audio machinery. “New Game” is the label; a separate
newGame method is unnecessary because starting Night 1 reuses validated night startup. Neither history
deletion nor checkpoint restoration happens here.

A second facade over a different subsystem

AudioService exposes play, ambience, menuMusic and gameplay. Its worker hides WAV/synthesis selec-
tion, stream decoding, Clip cache, volume/pan/mute controls, one-shots, loop replacement and cleanup.
A client asks for a cue/layer, not a sequence of mixer calls.

  A concise explanation
  “The New Game button is a client of our navigation facade. Application.startNight hides the checks
  and construction needed to show a fresh playable night. AudioService separately hides Java Sound
  complexity behind cue requests.”

Tradeoff. Central coordination prevents duplicated startup but gives Application many responsibilities.
That is a practical coordinator design, not a claim that every method in it independently implements a
pattern.
Open in source: ui/MenuPanel.java:86; ui/Navigation.java:46; Application.java:306; audio/AudioService.java:122
```

## Page 14

PDF page **14 / 42**.

```text
Decorator: real composable camera effects

 Pattern role                        Project type
 Component                           SceneRenderer interface
 Concrete component                  CameraSceneRenderer
 Base decorator                      RendererDecorator: is a SceneRenderer and has a wrapped SceneRender-
                                     er
 Concrete decorators                 VhsRendererDecorator; DimmedRendererDecorator
 Composition client                  GamePanel constructor

SceneRenderer camera = new VhsRendererDecorator(
    new CameraSceneRenderer(session, assets), assets, .25f);
SceneRenderer switching = new VhsRendererDecorator(
    new DimmedRendererDecorator(camera, 115),
    assets, .85f, 3);


These locals illustrate the exact shapes used for GamePanel’s two fields. Each wrapper delegates first,
then adds its own layer. Normal draws room then light VHS. Switching draws room → normal VHS →
darkness 115 → stronger outer VHS at triple speed.

Why this is Decorator

The same render interface lets a wrapper replace a component and wrap another wrapper. It combines
effects without one subclass for every combination. Layer order is observable: darkness after noise
darkens noise; noise after darkness sits on top. The delegated drawing order determines those final
pixels.

Details that support follow-up answers

• RendererDecorator holds a nonnull SceneRenderer and delegates once with original time/reduced flag.
• VhsRendererDecorator always draws the inner object, then skips optional noise for reduced mode/zero
  opacity. Its speed changes only its own effect time, not the inner scene or enemy rules.
• DimmedRendererDecorator dims whenever called. GamePanel bypasses the switching chain in reduced
  mode; do not claim every wrapper individually checks the preference.
• Effect drawing uses graphics.create() and finally/dispose to isolate wrapper-specific paint/compos-
  ite/hints from later HUD drawing.

  What is not the same example
  VhsOverlay is a drawing utility, not itself a wrapper. BufferedInputStream is a Java-library decora-
  tor; the renderer classes are the project’s custom implementation.

Open in source: ui/rendering/RendererDecorator.java:40; ui/rendering/VhsRendererDecorator.java:60; ui/rendering/DimmedRendere
rDecorator.java:43; ui/GamePanel.java:98
```

## Page 15

PDF page **15 / 42**.

```text
Builder, painting template and OOP choices

Builder: create a level by named steps

SecretLevelBuilder collects player/bundle spawn, destination, platforms, cover, patrols, width and count-
down. Each setter returns this for chaining; repeated part calls append rather than replace. Applica-
tion.createSecretLevel is the production recipe; build creates SecretLevel; SecretGame consumes that
product.

new SecretLevelBuilder().width(2560).player(80).bundle(240)
    .destination(new Rectangle(2410, 510, 125, 130))
    // repeated platform / cover / witness steps
    .timeLimit(60).build();


The recipe has three platforms, six cover regions and three independent patrols. build checks selected
completeness/range conditions, not every possible NaN or geometry error. Construction copies input
rectangles/lists, but returned Rectangle members remain mutable; the level is not deeply immutable
after access.

Template Method style painting

ScenePanel’s final paintComponent fixes setup → transform/clip → subclass render → cleanup.
paintChildren draws real controls then optional renderOverlay. Subclasses supply hooks without replac-
ing the common transform skeleton. This is an additional behavioral structure; Builder is creational,
while facade/decorator are structural.

 OOP concept                     Concrete evidence
 Encapsulation                   Private GameSession state; guarded light/monitor methods.
 Abstraction                     Navigation and SceneRenderer small contracts.
 Polymorphism                    Current ScenePanel and plain/wrapped SceneRenderer share calls with d-
                                 ifferent implementations.
 Composition                     GameSession owns GudoEncounter; decorator owns renderer.
 Dependency injection            Main supplies dependencies; panels receive callbacks/settings supplier.
 Value transfer                  Settings, Progress and RunRecord records with generated accessors/equal-
                                 ity.
 Generics                        submit<T> returns different worker values; place<T> preserves compo-
                                 nent type.

  Avoid overclaiming patterns
  One shared AssetManager is not enforced Singleton. Enum/conditional state machines are not
  separate polymorphic GoF State objects. Callbacks/listeners do not establish a separate custom
  Observer framework. Navigation is not adapting an incompatible interface.

Open in source: Application.java:374; game/minigame/SecretLevelBuilder.java:87; ui/ScenePanel.java:116
```

## Page 16

PDF page **16 / 42**.

```text
Threads, audio layers and resource decisions

 Context                        Owned work
 Swing EDT                      Window/screen changes, input callbacks, timer ticks and painting.
 Boot SwingWorker               doInBackground loads files/assets; done installs UI on EDT.
 gudo-storage                   One serial worker for JDBC/settings/report operations; callbacks return to
                                EDT.
 gudo-audio                     One serial daemon worker for decoding, mixer controls, playback and
                                cleanup.

Why one game cue happens only once

GamePanel compares location with lastGudoLocation for steps and oﬀice arrival cues. LEFT uses
knock, RIGHT pound, FRONT whisper. At three attack seconds or less, rushPlayed gates urgency. A
changed repelled count plays retreat. A one-shot warning occurs at power≤ 20; room creaks/rattles
have active-clock scheduling. Watched arrival/departure disrupts only that viewed feed for two active
seconds. Switching away silences it; returning before expiry preserves original variant/deadline. Mere
selection of an already occupied feed does not create a sighting sting.

Playback implementation

A cue is lazily decoded/cached as Clip. One-shots stop/reset/start the cached cue; repeated requests
restart it rather than create unlimited voices. Layer requests compare sets, stopping removed loops and
starting added ones while preserving shared positions. Master level times cue gain becomes dB through
20 log10 (v), clamped to supported controls. Approach pan maps by 2p − 1. Missing mixer/cue failures
enter unavailable and degrade to silence. Unsupported pan/gain controls are skipped. Only the three
secret cues use Chiptune; current ordinary encounter cues use Sound-mapped WAVs. Archived source
WAVs are not automatically active.

Resource and cleanup tradeoffs

AssetManager caches decoded paths and requires clean defaults for all scenes. Unknown state names
fall back; an explicitly listed missing asset fails boot. Buffers/blur are reused to reduce frame alloca-
tion, trading memory for work. Try-with-resources closes JDBC/streams; copied graphics contexts are
disposed.

  Orderly exit
  Application stops timer/key dispatcher, queues audio cleanup, shuts down IO and disposes frame.
  Normal active-shift exit confirms and saves abandonment first; force-kill cannot execute this cleanup
  reliably.

Open in source: Application.java:645; ui/GamePanel.java:320; audio/AudioService.java:183; assets/AssetManager.java:99
```

## Page 17

PDF page **17 / 42**.

```text
Application.java: follow a request end to end

Role. The Navigation implementation owns the current screen, live session/view, settings/progress,
pending save and services. Panels ask for intent; this coordinator decides whether/how it can happen.

 Method group                        Order and reasoning
 constructor / dispatchKey           Receive boot dependencies; install close/focus/key handling; create
                                     timer; show menu. Ignore other-window events and debounce Es-
                                     cape/F11 before panel bindings.
 tick / show                         Measure/cap delta; update active normal model; handle ending; tick-
                                     /repaint current screen; refresh audio. show replaces card and resets
                                     frame clock/focus.
 menu / page / showMenu              Guard conflicting IO/active attempts. Resolve abandonment or in-
                                     complete saves; records needs background reads. Clearing a live UI
                                     reference does not delete saved progress.
 startNight                          Guard eligibility/pending state, capture player, create model and
                                     callback-driven view, attach secret, show and request audio. Reused
                                     by all normal start routes.
 pauseShift / resumeShift            Release held controls and pause the same model. Resume reuses it; no
                                     new UUID or checkpoint is loaded.
 openSecret / finishSecret           Pause normal session, create hidden model/view. Ending marks a se-
                                     cret outcome and retains the same normal night for restart after save.
 finishShift /                       Capture one final record/profile, choose result/scare, start IO. Keep
 savePendingRun                      UUID across failure; navigate/restart/quit only after the required save
                                     result.
 saveSettings / display              Background preference/profile work, then replace UI values/apply dis-
                                     play on EDT. F11 persists before changing native decorations; guard
                                     temporary focus loss.
 quit / close / submit               Confirm/save abandonment on orderly exit. Stop timers/listeners/ser-
                                     vices. submit executes work on IO and posts success/failure to EDT.

State names worth recognizing

busy blocks overlapping loads; savingRun blocks duplicate in-flight saves; pendingRun/pendingProfile
survive retry; activeProfile fixes ownership at shift start. quitAfterSave/menuAfterSave defer
navigation. nextNightAfterSecret is actually the same night. displaySavePending/changingDisplay
guard native display changes.

  Trace this path aloud
  MenuPanel callback → startNight → GameSession/GamePanel → tick → finishShift → savePend-
  ingRun → Database then log → EDT result/navigation.

Open in source: Application.java:306; Application.java:454; Application.java:486
```

## Page 18

PDF page **18 / 42**.

```text
GameSession.java: rules and the final receipt

Role. One normal attempt with UUID/start Instant, night/duration, GudoEncounter, elapsed/power,
monitor/light/pan/camera and optional scene overrides. A nonnull result is the finished marker.

Construction and update

The one-argument constructor delegates with 360 s. The full constructor rejects night outside 1–5 and
invalid duration. update validates delta, exits for pause/finish and processes exact deadlines as explained
on page8. No JFrame, audio service or database is needed to run these rules.

 Method                                Guard / effect / consequence
 toggleMonitor                         Ignore paused/finished. Flip monitor and clear light. Lowering moni-
                                       tor does not automatically restore held light.
 setLight                              Always permit off; on requires active unfinished model and monitor
                                       closed. Correct facing is checked separately during update.
 selectCamera                          Validate camera1–6, then accept only with monitor up and active
                                       model. Closed-monitor shortcuts leave selection unchanged.
 setOfficePan                          Reject nonfinite input, clamp 0–1 during active play. Encounter uses
                                       this accepted direction.
 setSceneState/sceneState              Store valid custom state. Actual singular enemy occupancy overrides
                                       it; otherwise use custom/default key. Images do not determine enemy
                                       logic.
 pause/resume                          Pause preserves memory and releases light; resume clears pause only
                                       if unfinished. Neither writes disk.
 finish/result                         Finish only once, freeze final values and equipment off. result throws
                                       before ending; later calls return the same receipt.
 abandon/secretendings                 Select distinct ABANDONED/MINIGAME outcome, not normal
                                       COMPLETED. Database progression depends on that distinction.
 clock/getters                         progress derives elapsed/duration. HUD clock is fictional; durationLa-
                                       bel is actual active time. Read accessors do not update/save.

Why immutable result identity matters

Application can retry a failed storage sequence while presenting the same values and UUID. Creating
a new receipt/UUID on retry would turn one attempt into multiple records. startedAt/endedAt are
absolute timestamps, while elapsed excludes pause.

  What it intentionally cannot do
  There is no serialized current encounter/power/input checkpoint to restore after closing. Continue
  creates a new GameSession. The custom-duration constructor is a configuration facility, not a
  gameplay debug button.

Open in source: game/GameSession.java:80; game/GameSession.java:150; game/GameSession.java:271
```

## Page 19

PDF page **19 / 42**.

```text
GamePanel.java: input, view and accepted feedback

Role. This ScenePanel receives a model, shared assets, a current-settings supplier, sound callback and
pause callback. It owns screen/input animation state, not authoritative save/progression rules.

Follow a button or key

Constructor builds real monitor/light/camera GameButtons and press/release bindings. Space/Tab
have held flags to avoid repeat toggles. F and mouse light combine into one requested light state;
releaseControls clears all held flags on screen changes. The model still rejects illegal device combinations.

 Method group                          Responsibility
 updateLight/toggleMonitor/            Ask the model first; emit audio/transition only for a real accepted
 selectCamera                          change. Reselecting current camera or blocked F stays silent.
 syncControls                          Mirror model state into button visibility, selection, label and acces-
                                       sibility. Camera buttons appear with monitor up; light appears in
                                       oﬀice.
 tick                                  After model update, ease oﬀice input pan and send it back. De-
                                       tect new locations/repelled for one-time cues, feed disruption, low-
                                       power/urgency and scheduled room noises.
 render/renderHud                      Choose oﬀice/CCTV then draw flat clock, night, power, usage, ap-
                                       proach instructions and fade progress. Drawing does not advance the
                                       model.
 renderOffice                          Blend separate clean/occupied projection outputs during exposure,
                                       then light/tint/vignette and readability shading.
 renderCamera                          Obscure a disrupted feed, otherwise choose normal/switching renderer
                                       chain. Add border, record dot, room label and schematic map.
 mouseClicked/onSecret                 Reverse-map five valid CAM04 paper clicks and run Application’s sup-
                                       plied callback. The panel never constructs the hidden level itself.
 renderOverlay                         After Swing children, add optional subtle static and intro; reduced
                                       mode bypasses motion.

Presentation state to recognize

targetPan/pan are eased oﬀice direction; separate projection buffers support blending. lastGudoLoca
tion/lastRepelled gate cues. garbledCamera/garbleUntil/garbleSound retain one feed’s disruption
deadline/variant. rushPlayed/lowPowerPlayed prevent repeated warnings. paperClicks counts valid
hits.

  A subtle update detail
  Application updates GameSession before GamePanel.tick sends its newest pan back, so direction
  can have one small update-step delay. All of this occurs on EDT; it is not a separate model/render
  thread.

Open in source: ui/GamePanel.java:320; ui/GamePanel.java:423; ui/GamePanel.java:183
```

## Page 20

PDF page **20 / 42**.

```text
Startup and screen requests

Each card gives the purpose, execution path and reason. Detailed per-method explanations remain immediately
above the actual source methods.

  Main.java                                                                                          Main.java:31
  Starts the desktop application and loads its dependencies.
  Find: main, launch, SwingWorker.doInBackground/done, icon
  Main path. main schedules launch on Swing; launch shows LoadingPanel; SwingWorker loads paths, settings,
  database, progress and images; done constructs Application.
  Design choice. The window can repaint while disk and image loading happen. Boot is a record that
  transfers the loaded values together. Startup failures display a message and dispose the window.


  Navigation.java                                                                           ui/Navigation.java:24
  Defines the small screen-facing facade contract implemented by Application.
  Find: menu, page, startNight, saveSettings, resumeShift, leaveShift, quit
  Main path. MenuPanel, ExtrasPanel, NightSelectPanel, SettingsPanel, PausePanel and ResultPanel depend
  on this interface.
  Design choice. Screens request intent without knowing JFrame, storage workers, GameSession construction
  or AudioService internals. An interface alone is not proof of Facade; the hidden orchestration in Application
  supplies the evidence.


  MenuPanel.java                                                                             ui/MenuPanel.java:33
  Displays the title menu, enabled Continue state and random portrait flicker.
  Find: constructor, entry, move/select, tick, render/renderOverlay
  Main path. New Game -> startNight(1); Continue -> startNight(progress.nextNight()); Extras ->
  page(EXTRAS); Quit -> quit.
  Design choice. New Game deliberately preserves existing history. Continue is enabled after any recorded
  attempt, even a loss. Keyboard selection skips disabled entries. Flicker randomness is visual only.
```

## Page 21

PDF page **21 / 42**.

```text
Extras and static screens

Each card gives the purpose, execution path and reason. Detailed per-method explanations remain immediately
above the actual source methods.

  ExtrasPanel.java                                                                       ui/ExtrasPanel.java:23
  Routes five text entries to secondary pages.
  Find: constructor page callbacks; render
  Main path. Select Night, How to Play, Shift Records, Settings and Credits map in order to Navigation.Page
  values.
  Design choice. Keep a small routing screen without placing database or gameplay logic here. Capturing
  index per loop keeps each callback attached to the correct page.


  NightSelectPanel.java                                                             ui/NightSelectPanel.java:25
  Shows five night buttons gated by the loaded progress.
  Find: constructor eligibility/callbacks; render
  Main path. Extras -> Select Night -> enabled Night N -> navigation.startNight(N).
  Design choice. UI disables locked nights; Application rechecks unlocked before starting. Capture the loop
  value in selected so each listener keeps its own night.


  InfoPanel.java                                                                           ui/InfoPanel.java:25
  Draws the in-game guide or credits.
  Find: render, renderGuide, renderCredits
  Main path. Application.page(GUIDE/CREDITS) constructs the same class with credits=false/true.
  Design choice. Static information can share one view and backdrop. Guide controls are descriptive text;
  GamePanel/ScenePanel implement the actual actions.
```

## Page 22

PDF page **22 / 42**.

```text
Shared Swing infrastructure

Each card gives the purpose, execution path and reason. Detailed per-method explanations remain immediately
above the actual source methods.

  ScenePanel.java                                                                          ui/ScenePanel.java:30
  Provides a common scaled scene, real Swing component layout and keyboard routing.
  Find: place, doLayout, scenePoint, paintComponent/paintChildren, bind/dispatchKey
  Main path. Subclasses place controls in 1280x720 coordinates and implement render; Swing invokes paint-
  Component/paintChildren/doLayout.
  Design choice. Uniform scale and centered offsets preserve aspect ratio. Reverse transforms preserve input
  accuracy. The final paint skeleton is a Template Method style design with subclass hooks.


  BackdropPanel.java                                                                    ui/BackdropPanel.java:26
  Shares the dim oﬀice background and navigation chrome for secondary pages.
  Find: backdrop, pageChrome, backButton
  Main path. Extras, settings, guide/credits, selection, pause, records and result reuse backdrop or
  pageChrome.
  Design choice. Avoid duplicating the same drawing and Back binding. It is an abstract base class; inheri-
  tance alone is not the custom Decorator pattern.


  GameButton.java                                                                          ui/GameButton.java:24
  Uses a real JButton with custom visual styles and a supplied action.
  Find: constructor listener; paintComponent
  Main path. Screens construct the button with label/style/callback; Swing fires ActionListener; the callback
  requests navigation or model input.
  Design choice. Preserve enabled state, keyboard/button behavior and accessibility while drawing the game
  style. Painting only changes appearance, not game rules.
```

## Page 23

PDF page **23 / 42**.

```text
Common drawing and loading

Each card gives the purpose, execution path and reason. Detailed per-method explanations remain immediately
above the actual source methods.

  Theme.java                                                                                  ui/Theme.java:25
  Centralizes the bundled pixel font, colors and reusable drawing utilities.
  Find: font helpers, text/paragraph/ellipsize, cover/contain, quality/vignette
  Main path. Panels/buttons/renderers call static helpers instead of reproducing font/image/text code.
  Design choice. One style source keeps UI consistent. Explicit wrap/ellipsize handle Java2D text limits.
  cover crops to fill while contain preserves the entire image.


  VhsOverlay.java                                                                        ui/VhsOverlay.java:25
  Draws animated noise, scanlines and a rolling band as a visual utility.
  Find: paint
  Main path. Menu/GamePanel and VhsRendererDecorator call paint.
  Design choice. It returns immediately for reduced effects and restores composite/interpolation. The deco-
  rator uses a copied Graphics2D so color/paint changes do not leak into later HUD drawing.


  LoadingPanel.java                                                                    ui/LoadingPanel.java:22
  Draws a waiting message and animated dots.
  Find: constructor; render
  Main path. Main uses it during boot; Application uses it for asynchronous records/progress reads.
  Design choice. The panel does not perform slow work. The worker and coordinator decide when loading is
  complete.
```

## Page 24

PDF page **24 / 42**.

```text
Pause, result and scare screens

Each card gives the purpose, execution path and reason. Detailed per-method explanations remain immediately
above the actual source methods.

  PausePanel.java                                                                        ui/PausePanel.java:25
  Displays a frozen active attempt and routes Resume or Return to Menu.
  Find: constructor callbacks; render
  Main path. Escape in gameplay -> Application.pauseShift -> PausePanel; Resume/Escape -> resumeShift;
  Return to Menu -> leaveShift confirmation/save.
  Design choice. Reuse the same model when resuming. Leaving creates ABANDONED, so pause is different
  from completing an attempt.


  ResultPanel.java                                                                      ui/ResultPanel.java:28
  Displays an ended RunRecord and the pending/saved/failed storage status.
  Find: saving, saved, failed, render
  Main path. Application.finishShift builds it; savePendingRun drives saving/saved/failed; buttons request
  next/replay/menu/retry.
  Design choice. Disable next/menu while saving; allow retry with the same pending UUID. A completed
  Night 1-4 offers next; other endings offer the same night; Night 5 remains capped.


  JumpscarePanel.java                                                                ui/JumpscarePanel.java:29
  Animates the caught still and calls back once after 2.1 seconds.
  Find: constructor, tick, render
  Main path. Application.finishShift on CAUGHT starts sound/save and shows this; completion opens the
  prepared ResultPanel.
  Design choice. Precompute a horizontal blur once. Zoom/shake reuse the still; reducedEffects keeps it
  stationary. Guard delivered avoids repeated navigation.
```

## Page 25

PDF page **25 / 42**.

```text
Records and preference views

Each card gives the purpose, execution path and reason. Detailed per-method explanations remain immediately
above the actual source methods.

  RecordsPanel.java                                                                        ui/RecordsPanel.java:35
  Displays the current profile summary and read-only recent attempts table.
  Find: constructor/table model; render
  Main path. Extras -> Shift Records -> Application IO load -> constructor builds JTable from the returned
  list.
  Design choice. No SQL is run while rendering. Absolute Instants are displayed in the local system zone.
  Only latest 200 rows are loaded; summary counts include all stored attempts.


  SettingsPanel.java                                                                      ui/SettingsPanel.java:39
  Edits preferences in real Swing controls and submits an immutable Settings value.
  Find: values, setSaving/setFullscreen, DocumentFilter, checkbox icons
  Main path.         Extras -> Settings -> edit name/volume/effects/fullscreen -> Apply -> naviga-
  tion.saveSettings(values()).
  Design choice. DocumentFilter sanitizes typed/pasted names, and Settings validates again. Applying is
  explicit; Back does not save edits. The clipboard button copies the resolved data folder.


  Settings.java                                                                       persistence/Settings.java:21
  Validates and carries immutable user preferences.
  Find: compact constructor; withFullscreen
  Main path. SettingsStore loads it; SettingsPanel creates a replacement; Application persists and applies it.
  Design choice. Sanitize control characters, trim/fallback/cap the name, and clamp volume at the value
  boundary as well as the UI. A record replacement makes updates explicit.
```

## Page 26

PDF page **26 / 42**.

```text
Data paths and value carriers

Each card gives the purpose, execution path and reason. Detailed per-method explanations remain immediately
above the actual source methods.

  AppPaths.java                                                                     persistence/AppPaths.java:26
  Resolves writable player-data paths outside the packaged game.
  Find: resolve; database/settings/shiftLog
  Main path. Main calls resolve; SettingsPanel displays the folder; Database, SettingsStore and ShiftLogStore
  use the three child paths.
  Design choice. A JAR contains read-only resources; user saves need a writable external folder. gudo.dataDir
  supports isolated demo runs without changing ordinary player data.


  Progress.java                                                                     persistence/Progress.java:21
  Provides a loaded summary for menu, night-selection and records screens.
  Find: EMPTY; unlocked
  Main path. Database.loadProgress calculates counts and reads unlock fields; Application passes the value
  to panels.
  Design choice. Keep one summarized value for UI decisions instead of running SQL during every repaint.
  EMPTY represents a fresh profile.


  RunRecord.java                                                                          game/RunRecord.java:23
  Carries the immutable snapshot of one ended attempt.
  Find: generated accessors; durationLabel
  Main path. GameSession creates it once; Application retains it; Database and ShiftLogStore save the same
  ID and values; RecordsPanel displays it.
  Design choice. A record supplies value equality and accessors. Capture final values once so retries cannot
  silently turn the attempt into a new one.
```

## Page 27

PDF page **27 / 42**.

```text
Named states and camera motion

Each card gives the purpose, execution path and reason. Detailed per-method explanations remain immediately
above the actual source methods.

  Outcome.java                                                                          game/Outcome.java:20
  Defines the six final result categories stored in SQLite.
  Find: stable enum name; label
  Main path. GameSession.finish selects the enum; Database stores name(); UI/logs display label().
  Design choice. Stable machine names separate storage from readable labels. Only COMPLETED advances
  normal-night progress; MINIGAME_COMPLETED is a different outcome.


  Scene.java                                                                              game/Scene.java:22
  Defines the oﬀice and six canonical camera rooms.
  Find: id/label/camera; static camera(number)
  Main path. Game controls validate a camera number with Scene.camera(number); logic and AssetManager
  share the enum id.
  Design choice. Central identifiers keep camera labels, route locations and image manifest keys aligned.
  CAM 01 is currently Front entrance, despite some older dining filenames/text.


  CameraMotion.java                                                                game/CameraMotion.java:21
  Computes the automatic CCTV sweep from camera identity and active shift time.
  Find: pan
  Main path. CameraSceneRenderer and the secret-paper hit test call pan(scene,elapsed).
  Design choice. A cosine gives smooth reversal. Each camera has a different phase and period. Using
  elapsed means unseen cameras continue their sweep and all sweeps freeze during pause.
```

## Page 28

PDF page **28 / 42**.

```text
Image loading and projection

Each card gives the purpose, execution path and reason. Detailed per-method explanations remain immediately
above the actual source methods.

  AssetManager.java                                                                   assets/AssetManager.java:35
  Loads bundled images, the scene manifest and decoded TV-noise frames, then serves cached lookups.
  Find: constructor, resource, scene, noise/menuRender
  Main path. Main constructs one manager in the boot worker. Screens ask scene(scene,state), menuRen-
  der(index) or noise(frame); resource opens a classpath stream.
  Design choice. Decode each referenced path once, require a default for all rooms, and fail startup for a
  listed missing asset. Unknown state names fall back to a clean default. This separates a bad key from a
  broken packaged asset.


  PanoramaRenderer.java                                                           assets/PanoramaRenderer.java:33
  Projects a wide room image into a curved-looking view and maps clicks back to source pixels.
  Find: prepare, render, sourcePoint
  Main path. render clamps/quantizes pan, prepares sampling maps when the source changes, fills a reused
  854x480 image, and returns it. sourcePoint reverses the latest sampling for a hit test.
  Design choice. atan maps horizontal rays and cos changes vertical sampling near the edges. Cached pixel
  maps and one output buffer reduce allocation. A separate renderer is needed when two outputs must coexist
  for a blend.


  CameraSceneRenderer.java                                               ui/rendering/CameraSceneRenderer.java:32
  Draws the current CCTV room without VHS or switching effects.
  Find: constructor; render
  Main path. GamePanel puts it inside VhsRendererDecorator; render reads selected camera, room state and
  sweep from the live session.
  Design choice. Composition keeps one core room renderer separate from optional visual layers. The object
  is created once and reads updated model values each frame.
```

## Page 29

PDF page **29 / 42**.

```text
Renderer contracts and wrappers

Each card gives the purpose, execution path and reason. Detailed per-method explanations remain immediately
above the actual source methods.

  SceneRenderer.java                                                            ui/rendering/SceneRenderer.java:24
  Defines the component interface shared by a room renderer and all custom renderer decorators.
  Find: render contract
  Main path.       GamePanel stores composed implementations through SceneRenderer and calls ren-
  der(graphics,time,reducedEffects).
  Design choice. The same interface allows nested wrappers and substitutability. Without it, effect layers
  would need to know each concrete renderer type.


  RendererDecorator.java                                                    ui/rendering/RendererDecorator.java:24
  Implements SceneRenderer while retaining another SceneRenderer and delegating to it.
  Find: constructor; render delegation
  Main path. Concrete effect constructors call super(wrapped); render forwards the original graphics/time/re-
  ducedEffects.
  Design choice. Being the same component type and containing one component lets decorators wrap deco-
  rators. Null rejection fails invalid chains early.


  VhsRendererDecorator.java                                              ui/rendering/VhsRendererDecorator.java:27
  Adds optional VHS interference after its wrapped renderer draws.
  Find: constructors; render
  Main path. Normal camera uses opacity 0.25; switching adds an outer opacity 0.85 layer at speed 3.
  Design choice. Delegate with original time so effect speed affects only this wrapper. Skip noise when
  reducedEffects or opacity=0, but still render the wrapped room.
```

## Page 30

PDF page **30 / 42**.

```text
Darkness and audio services

Each card gives the purpose, execution path and reason. Detailed per-method explanations remain immediately
above the actual source methods.

  DimmedRendererDecorator.java                                        ui/rendering/DimmedRendererDecorator.java:25
  Adds a translucent black layer after its wrapped renderer draws.
  Find: constructor; render
  Main path. GamePanel puts it around the normal camera chain for switching interference.
  Design choice. Validate alpha=0-255, delegate first, and draw using a copied graphics context. Layer order
  changes pixels.


  AudioService.java                                                                     audio/AudioService.java:39
  Provides the audio facade over Java Sound and a serial audio worker.
  Find: clip, play, ambience/gameplay, gain/pan, stop/close
  Main path. Application or a panel callback requests play, ambience, menuMusic or gameplay. The worker
  lazily decodes/caches a Clip, sets gain/pan, starts or loops it, and later stops/closes it.
  Design choice. One facade hides stream decoding, mixer access, caching, thread scheduling, layers and
  cleanup. Missing hardware is remembered as unavailable and gameplay continues. The worker is daemon;
  the storage worker is not.


  Sound.java                                                                                   audio/Sound.java:21
  Names each cue and stores its classpath path and relative gain.
  Find: enum path/gain accessors
  Main path. GamePanel/SecretPanel/Application choose a Sound enum; AudioService reads path and gain.
  Design choice. Enum values avoid scattered filename strings. Master volume and per-sound gain are
  separate. Null paths mark the three Chiptune sounds.
```

## Page 31

PDF page **31 / 42**.

```text
Synthesis and text stores

Each card gives the purpose, execution path and reason. Detailed per-method explanations remain immediately
above the actual source methods.

  Chiptune.java                                                                            audio/Chiptune.java:26
  Generates only the hidden minigame audio in memory.
  Find: create; ambience
  Main path. create returns an AudioInputStream for SECRET_JUMP, SECRET_PICKUP or SE-
  CRET_AMBIENCE. AudioService opens it through the same clip path used for WAVs.
  Design choice. Square waves and envelopes create small retro cues without another library. The eight-
  second drone uses a deterministic noise sequence and quiet endpoints. Current ordinary encounter sounds
  are bundled WAVs.


  SettingsStore.java                                                            persistence/SettingsStore.java:32
  Reads and replaces the UTF-8 settings.txt preferences file.
  Find: load, booleanValue, save
  Main path. load creates defaults on first launch or parses Properties. save writes a temporary neighbor,
  then replaces the real file.
  Design choice. Use explicit true/false parsing, volume fallback and Settings normalization. Atomic replace-
  ment reduces truncated settings; filesystems without atomic move use a regular replacement fallback.


  ShiftLogStore.java                                                            persistence/ShiftLogStore.java:32
  Appends a readable one-line report for each ended attempt.
  Find: append
  Main path. Application.savePendingRun calls append after Database.saveRun.
  Design choice. A run=UUID marker detects an already-written report on retry. Locale.ROOT stabi-
  lizes decimal formatting; UTF-8 preserves names. This is best-effort application-level deduplication, not a
  transactional database.
```

## Page 32

PDF page **32 / 42**.

```text
Database and level construction

Each card gives the purpose, execution path and reason. Detailed per-method explanations remain immediately
above the actual source methods.

  Database.java                                                                               persistence/Database.java:38
  Persists local profiles, unlock summaries and attempt history using SQLite/JDBC.
  Find: constructor/migrations, connect/profile, loadProgress, saveRun/loadRuns
  Main path. Constructor configures/migrates schema; loadProgress selects/creates a profile; saveRun inserts
  an attempt and updates unlocks transactionally; loadRuns returns latest rows.
  Design choice. Prepared statements bind player values. Foreign keys and CHECK/UNIQUE constraints
  protect relations and ranges. A UUID primary key supports idempotent retries; MAX preserves unlocks
  when replaying earlier nights.


  SecretLevelBuilder.java                                                         game/minigame/SecretLevelBuilder.java:30
  Constructs a SecretLevel step by step through a fluent Builder.
  Find: fluent part methods; build
  Main path. Application.createSecretLevel chains width/player/bundle/destination/platform/cover/wit-
  ness/timeLimit, then build.
  Design choice. Optional repeated parts are easier to read than a large positional constructor. Each setter
  returns this; build validates completeness and creates the product. Validation covers selected constraints, not
  every possible numeric input.


  SecretLevel.java                                                                       game/minigame/SecretLevel.java:25
  Stores the level configuration produced by SecretLevelBuilder.
  Find: compact constructor; Patrol
  Main path. Application creates the product; SecretGame reads dimensions, spawn points, cover, platforms,
  destination and patrols.
  Design choice. The constructor copies incoming Rectangle objects and lists, so later changes to builder
  inputs do not change this product. Returned Rectangles remain mutable; this is not deep immutability after
  access.
```

## Page 33

PDF page **33 / 42**.

```text
Enemy and hidden-game runtime

Each card gives the purpose, execution path and reason. Detailed per-method explanations remain immediately
above the actual source methods.

  GudoEncounter.java                                                                    game/GudoEncounter.java:23
  Implements one deterministic enemy encounter state machine and three approach routes.
  Find: untilEvent, advance, location, faces, exposure/timer accessors
  Main path. GameSession asks untilEvent, advances that exact step, then checks caught. The UI reads
  location, approach, exposure/fade and attack countdown.
  Design choice. routeIndex=-1 represents waiting, camera indices represent travel, atOﬀice represents de-
  fense, and caught is terminal. Successful flashlight exposure repels Gudo; interrupted exposure decays rather
  than instantly resetting.


  SecretGame.java                                                                 game/minigame/SecretGame.java:31
  Runs the hidden platformer rules without owning Swing or audio.
  Find: action, jump, update, bundleHidden/witnessBeam
  Main path. SecretPanel calls jump/action/update and draws read-only model values. The model integrates
  motion, patrols, bundle visibility and the pickup-triggered timer.
  Design choice. Twenty-millisecond substeps reduce missed collisions. Witnesses detect the bundle center,
  not the player body. The same beam polygon is used for drawing and detection.


  SecretPanel.java                                                                          ui/SecretPanel.java:35
  Controls and renders the scrolling pixel-art minigame.
  Find: bindings, tick, pause, silenceOnOutcome, draw helpers
  Main path. GamePanel paper trigger -> Application.openSecret -> SecretPanel; key events call Se-
  cretGame; tick advances it; outcome freezes, silences and reports after three seconds.
  Design choice. Use an offscreen 640x360 image enlarged nearest-neighbor. World scrolling is isolated from
  fixed HUD. reported and outcomeSilenced prevent duplicate callbacks/audio.
```

## Page 34

PDF page **34 / 42**.

```text
Practice questions: 1–10

1. What is the main objective?
Survive a six-minute active night until 6 AM while conserving power and defending against Gudo. Locate:
GameSession.update.

2. Why use Java Swing?
The implementation uses Java’s desktop UI and drawing APIs to keep the game a self-contained offline
desktop application. Locate: Main.launch / ScenePanel.

3. Where does execution begin?
com.gudos.Main.main schedules launch on the Swing EDT. Locate: Main.main / SwingWorker.

4. Is GamePanel the entire game logic?
No. It translates input and draws; GameSession and GudoEncounter enforce normal-night rules. Locate:
GamePanel and GameSession.

5. Explain New Game as a facade.
The button calls one Navigation.startNight(1) operation. Application hides the checks, new session/view
creation, callback wiring, navigation and audio coordination. Locate: MenuPanel -> Application.startNight.

6. Does the interface itself make it a facade?
The interface gives the small contract; the implementation’s hidden subsystem coordination is the evi-
dence of the facade. Locate: Application.startNight.

7. Why no separate newGame method?
New Game is a label. Starting Night 1 is the same operation as starting a selected eligible night with
argument one. Locate: MenuPanel / NightSelectPanel / ResultPanel.

8. Does New Game delete progress?
It starts a new Night 1 attempt and preserves records/unlocks. Locate: Application.startNight / Database.saveRun.

9. What exactly does Continue continue?
Progression, not a live checkpoint. It starts a fresh attempt of progress.nextNight.              Locate: MenuPanel
constructor.

10. What are your structural patterns?
Facade at navigation/audio boundaries and a custom Decorator for CCTV effects.                  Locate: Application /
AudioService / rendering package.
```

## Page 35

PDF page **35 / 42**.

```text
Practice questions: 11–20

11. Explain the audio facade beyond its name.
One playback request hides decoding, clip caching, volume/pan controls, looping, worker scheduling and
cleanup. Locate: AudioService.play / clip.

12. Show the Decorator requirements.
Every wrapper implements SceneRenderer, holds another SceneRenderer, delegates its render, then adds
an effect. Locate: RendererDecorator / render overrides.

13. Why not just subclass a renderer for every effect combination?
Composition lets us stack and order effects without creating a new subclass for each combination. Locate:
GamePanel constructor.

14. Does decorator order matter?
Yes. Dimming after noise darkens the noise; noise drawn after dimming appears over the darkened room.
Locate: GamePanel renderer chain.

15. Why copy Graphics2D?
An effect can change paint/composite/hints on its own context and dispose it without leaking that effect
state into later HUD rendering. Locate: VhsRendererDecorator.render.

16. Why does reduced mode still render the room?
VHS delegates first, then returns before adding optional noise. GamePanel also skips the switching chain.
Locate: GamePanel.renderCamera.

17. Explain the Builder with real participants.
SecretLevelBuilder collects named level parts; Application.createSecretLevel is the production recipe;
build creates SecretLevel, which SecretGame uses. Locate: Application.createSecretLevel.

18. Is the level fully immutable?
It copies construction inputs and lists, but Rectangle objects exposed through accessors remain mutable.
Locate: SecretLevel constructor.

19. Is this the State pattern?
It uses finite state-machine logic, but not separate polymorphic GoF State objects. Locate: GudoEncounter /
SecretGame.State.

20. Where is encapsulation?
GameSession fields are private; controls change them through guarded methods. Locate: GameSession.setLight.
```

## Page 36

PDF page **36 / 42**.

```text
Practice questions: 21–30

21. Where is runtime polymorphism?
Application can tick/render through a ScenePanel reference, and GamePanel can call a SceneRenderer
whether it is plain or decorated. Locate: ScenePanel / SceneRenderer.

22. Are you using a Singleton?
There is one shared AssetManager in the normal boot flow, but no enforced Singleton class.                  Locate:
AssetManager constructor.

23. Why callbacks instead of giving a panel all services?
The panel can request just the behavior it needs, such as sound or pause, without owning storage/au-
dio/window lifecycles. Locate: GamePanel constructor callbacks.

24. How does a button actually start anything?
GameButton installs an ActionListener that runs the supplied Runnable. The screen passes the actual
navigation/model callback. Locate: GameButton constructor.

25. How is a night timed?
Application supplies measured active deltas; GameSession accumulates them to its normal 360-second
duration. Locate: Application.tick / GameSession.

26. Why not subtract one fixed amount per frame?
Frame delivery can vary, so time and power use actual supplied seconds and exact event-boundary steps.
Locate: GameSession.update.

27. What if attack and morning happen together?
The model explicitly gives morning priority, then power failure, then attack. Locate: GameSession.update condition
order.

28. How does power work?
0.085 percentage points per active second, plus 0.22 for the monitor or 0.20 for light.            Locate: GameSes-
sion.drainPerSecond.

29. Is Gudo random or AI-trained?
He follows deterministic route and timing rules, not a trained model. Locate: GudoEncounter.advance.

30. Why does light fail while I am holding F?
The monitor must be down, the session active and the oﬀice view facing the current approach. Then
exposure must reach its threshold before the attack deadline. Locate: GameSession.update / Approach.faces.
```

## Page 37

PDF page **37 / 42**.

```text
Practice questions: 31–40

31. Why does he reappear when I look away?
Interrupted effective exposure decays at 0.65 per second, so the fade reverses toward the occupied room.
Locate: GudoEncounter.advance.

32. Why can’t he appear in two camera rooms at once?
The encounter returns one current location; sceneState checks that location for occupancy. Locate: Game-
Session.sceneState.

33. How does CCTV move when I am not watching?
Its pan is a pure function of camera identity and session elapsed time, not accumulated viewing time.
Locate: CameraMotion.pan.

34. Is the office actually 3D?
The visual effect projects a wide 2D panorama using angle-based sampling. There is no 3D model/mesh
here. Locate: PanoramaRenderer.prepare.

35. Why separate clean and occupied renderers?
Each PanoramaRenderer returns its reused mutable output. Two instances keep both projected images
available during the blend. Locate: GamePanel.renderOﬀice.

36. How does the paper click remain aligned during camera sweep?
We reverse the window-to-scene transform, then reverse the current projection to a source image pixel
and test the source-fraction hitbox. Locate: GamePanel.mouseClicked.

37. What starts the hidden timer?
The first successful bundle pickup, not opening the minigame. Locate: SecretGame.action.

38. What exactly do witnesses detect?
The bundle center, carried or dropped, inside their direction-facing triangle unless a cover rectangle
contains that center. Locate: SecretGame.update / witnessBeam.

39. How do you win the minigame?
Carry the bundle into the stockroom destination and press E. Delivery is checked before dropping. Locate:
SecretGame.action / finishSecret.

40. Does a secret win unlock the next night?
No. Only normal Outcome.COMPLETED executes the unlock update. Locate: Database.saveRun.
```

## Page 38

PDF page **38 / 42**.

```text
Practice questions: 41–50

41. How does minigame collision handle a long frame?
SecretGame splits its supplied delta into steps no larger than0.02 seconds to reduce missed platforms or
beam crossings. Locate: SecretGame.update.

42. Why SQLite and also text files?
SQLite is authoritative relational progress/history; settings are human-editable preferences; the log is a
readable report. Locate: AppPaths and store classes.

43. Explain the database relations.
One profile has one progress summary and many night_runs. progress.profile_id is its primary/foreign
key; each run has its own UUID and a profile foreign key. Locate: Database.runsTable / schema.

44. Why PreparedStatement?
Player values are bound separately from SQL syntax, including quoted names.          Locate: Database.profile /
PreparedStatement.

45. What is atomic about saving?
The attempt insert and unlock update commit or roll back together inside SQLite. Locate: Database.saveRun.

46. Are SQLite and shift-log.txt one transaction?
No.      They are separate writes coordinated by a retryable application workflow.           Locate:   Applica-
tion.savePendingRun.

47. How do you stop duplicate records?
A stable run UUID is the night_runs primary key with ON CONFLICT DO NOTHING; the text report
checks the same UUID marker. Locate: RunRecord.id / store conflict/marker.

48. Why not advance progress on every saved attempt?
An attempt count includes failures and secrets. Progression represents normal survival, so the update
requires a newly inserted COMPLETED result. Locate: Database.saveRun.

49. Why is the table limited to 200?
The latest 200 rows keep the records view manageable; older rows are retained and totals still count
them. Locate: Database.loadRuns / loadProgress.

50. Are profile names secure accounts?
No. They select local records on this computer. There is no authentication or online service.           Locate:
Settings / Database.profile.
```

## Page 39

PDF page **39 / 42**.

```text
Practice questions: 51–60

51. What happens if settings values are invalid?
Parsing uses defined fallbacks and Settings sanitizes/clamps the final values. IO failures still report
errors. Locate: Settings constructor / store.load.

52. Why a temporary settings file?
Write a complete replacement first, then swap it over the real file, atomically where supported. Locate:
SettingsStore.save.

53. Why can’t an unexpected window focus loss keep the flashlight on?
Application pauses the active panel and clears held controls; GameSession.pause forces light off. Locate:
windowLostFocus / releaseControls.

54. Does closing always save?
Normal close/quit asks and waits to save an active abandoned attempt. Force-kill or a process crash
cannot guarantee that save. Locate: Application.quit / close.

55. What if the audio device is missing?
The failed cue is remembered and playback returns without stopping gameplay. Locate: AudioService.clip.

56. How are resources packaged for offline play?
Maven Shade bundles dependencies and runtime resources in the distributable JAR. AssetManager opens
images, fonts and audio through classpath resources, so the built game does not need live asset downloads.
Locate: pom.xml Shade configuration and AssetManager.resource.

57. Where should a new camera image/state be added?
Put the image under the resources image folder, add a scene.state mapping in scenes.properties and use
the state through the model. Locate: scenes.properties / AssetManager.

58. How would you add another enemy?
It would require extending the model for multiple independent encounters/occupancy and then adapting
rendering, cues and state updates. Locate: GameSession encounter ownership.

59. What are the main limitations?
Local-only profiles, no mid-shift restore, one deterministic enemy, hardware-dependent audio, basic
minigame input validation, and separate DB/text writes. Locate: Limits page.

60. How do you know the added comments did not change the game?
The cleaned comments retain identical parsed executable syntax, and all 70 production class files still
match the original byte-for-byte with debug metadata disabled. Locate: Validation note on limits page.
```

## Page 40

PDF page **40 / 42**.

```text
A five-minute rehearsal and common snags

 Time             Do                                  Explain
 0:00–0:30        Show title/project folders          Offline Swing game; separate model, screen and
                                                      coordinator.
 0:30–1:00        Click New Game; open startNight     One navigation facade hides checks and mod-
                                                      el/view/audio coordination. History stays.
 1:00–1:40        Pan, monitor, camera, light         Model rejects incompatible devices; correct fac-
                                                      ing/exposure matters. CCTV uses session time.
 1:40–2:20        Pause; show update/encounter        Active-time freeze and exact event deadlines.
                                                      Morning wins an exact tie.
 2:20–3:00        Open renderer construction          Same SceneRenderer contract supports nested ef-
                                                      fects; draw order matters.
 3:00–3:45        Confirm abandonment; show           One final UUID receipt; background saves;
                  Records                             newest 200 versus total.
 3:45–4:20        Show saveRun and log marker         SQLite transaction is separate from report ap-
                                                      pend; ordinary retry avoids duplicates.
 4:20–5:00        Show level recipe / hidden path     Builder product; pickup-triggered timer; both se-
                                                      cret outcomes restart same normal night.

If something does not match expectations

• Continue disabled: this profile has no recorded attempt. Night locked: check name and resolved
  data folder.
• F ineffective: monitor down, active session, face current doorway, enough light before deadline.
  Wrong direction still drains.
• Frequent pause: the window is losing focus. Reduced camera darkness: SIGNAL LOST still
  hides a disrupted feed for two active seconds.
• Secret trigger fails: CAM04, actual paper, five valid left-clicks, Nights 1–4. Timer keeps running
  in cover: expected after first pickup.
• Save failed but row exists: database may have committed before text; Retry keeps the same UUID.
  Fewer table rows than totals: newest 200 limit, not deletion.
• No sound: check volume/mixer and stderr. Build download failure: dependency access is needed
  once; the built JAR itself is offline.

  Prepare the presentation machine
  Build before the session, use an isolated demo folder, pre-open key files, verify actual speakers/fo-
  cus/fullscreen, and decide whether demonstrating a six-minute win or an honest abandonment/loss.
```

## Page 41

PDF page **41 / 42**.

```text
Implementation limits and source evidence

Boundaries to explain accurately

1. No mid-shift restore. Saved runs are ended receipts, not complete live-session serialization. Con-
   tinue starts fresh.
2. Separate stores. Attempt/unlock is transactional within SQLite; text append is separate. UUID
   retries address normal partial success, not all crash/journaling cases.
3. Local profiles. Names are labels on one computer, without passwords, online identity or synchro-
   nization. File access permits external local edits.
4. One deterministic enemy. Multiple independent enemies need model/occupancy/render/cue
   changes; they are not already supported.
5. Input/config assumptions. SecretGame assumes valid caller deltas; Builder checks selected con-
   straints. Returned level Rectangles remain mutable despite input copies.
6. Time and hardware. UI delta cap can slow simulation during stalls. Optional mixer controls vary;
   silent fallback is deliberate.

What the verification establishes

All 45 application Java files parse with unchanged executable syntax after comment cleanup. All 70
production class files match original byte-for-byte when compiled with debug metadata disabled, using
the available JDK 21 compiler with source/target 17. Whitespace checks pass. This is not a full –
release 17 API check or a run on an installed JDK 17 runtime. Maven packaging could not download
dependencies because repo.maven.apache.org was unreachable. No complete Maven package or native-
window/speaker verification is claimed. The document uses actual headless Swing renders with controlled
state. The PDF is compiled from this edition’s XeLaTeX source.

Version and authority

Repository: https://github.com/itsnold/FiveNightsAtGudos. Executable base: b27297ab9e687518
fc1543908740e5926969cf96. Application source: e9f0ff45350572e7ba5fd0ff6185ab1251514e2f. Links
pin to that immutable source revision; later documentation-only commits do not alter those line refer-
ences. Current Java, active scene manifest, Sound enum, pom/launcher configuration determines actual
behavior. Some older README/asset names retain obsolete dining or synthesized-audio descriptions.
Benefits/decisions in this guide are reasoned from code, not undocumented author interviews.

  Where the exhaustive detail went
  Every explicitly declared method/constructor still has its explanation beside the code. This edition
  keeps all 45 application Java-file responsibilities and the important algorithms, decisions, navigation
  and questions while avoiding repetitive per-getter narration in the PDF.
```

## Page 42

PDF page **42 / 42**.

```text
Alphabetical Java file locator

Click a page number to open its file card. All 45 application files live under src/main/java/com/gudos.

 Java file                                      Page
                                                           Java file                                      Page
 AppPaths.java                                     26
                                                           PanoramaRenderer.java                            28
 Application.java                                  17
                                                           PausePanel.java                                  24
 AssetManager.java                                 28
                                                           Progress.java                                    26
 AudioService.java                                 30
                                                           RecordsPanel.java                                25
 BackdropPanel.java                                22
                                                           RendererDecorator.java                           29
 CameraMotion.java                                 27
                                                           ResultPanel.java                                 24
 CameraSceneRenderer.java                          28
                                                           RunRecord.java                                   26
 Chiptune.java                                     31
                                                           Scene.java                                       27
 Database.java                                     32
                                                           ScenePanel.java                                  22
 DimmedRendererDecorator.java                      30
                                                           SceneRenderer.java                               29
 ExtrasPanel.java                                  21
                                                           SecretGame.java                                  33
 GameButton.java                                   22
                                                           SecretLevel.java                                 32
 GamePanel.java                                    19
                                                           SecretLevelBuilder.java                          32
 GameSession.java                                  18
                                                           SecretPanel.java                                 33
 GudoEncounter.java                                33
                                                           Settings.java                                    25
 InfoPanel.java                                    21
                                                           SettingsPanel.java                               25
 JumpscarePanel.java                               24
                                                           SettingsStore.java                               31
 LoadingPanel.java                                 23
                                                           ShiftLogStore.java                               31
 Main.java                                         20
                                                           Sound.java                                       30
 MenuPanel.java                                    20
                                                           Theme.java                                       23
 Navigation.java                                   20
                                                           VhsOverlay.java                                  23
 NightSelectPanel.java                             21
                                                           VhsRendererDecorator.java                        29
 Outcome.java                                      27
```
