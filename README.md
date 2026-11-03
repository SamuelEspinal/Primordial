# Primordial

A local Java evolution sandbox. Small recurrent brains steer swimmers while food, inherited bodies, predation, and energy-funded offspring determine which clans continue. It uses Swing/Java2D, requires JDK 17+, and has no external libraries.

## Run and check

Open the folder in IntelliJ, choose a JDK 17+ SDK, and run `Main`. From PowerShell in this folder:

```powershell
New-Item -ItemType Directory -Force out/classes | Out-Null
javac -encoding UTF-8 -d out/classes src/*.java
java -cp out/classes Main
```

Compile every source together. If PATH points at an Oracle launcher that hangs, use the JDK binaries directly. A repeatable compile/check script is included:

```powershell
.\check.ps1 -JdkHome 'C:\Program Files\Java\jdk-17.0.3.1' -RenderUI
```

Or run the checks directly after compilation:

```powershell
java '-Djava.awt.headless=true' -cp out/classes Main --self-test
java '-Djava.awt.headless=true' -cp out/classes Main --render-ui out/ui-preview
```

Quote the `-D` argument in PowerShell. UI preview renders both themes, minimum/large main-window sizes, every Getting started topic, Advanced Settings, genealogy, experiments, background islands, and the population study. Generated classes and images stay under `out/`.

## Getting started

The short **Getting started** tour opens on your first launch. Reopen it at any time using **Help ↗** at the top of the app. Six quick topics introduce the dish, new-world presets and Advanced Settings, reproduction modes, named clans, parallel worlds, and saving/comparing results.

Use **Back / Next**, or jump straight to a topic. Each page has a small suggestion and a shortcut to the real controls. The guide is a resizable window you can move beside the dish and close at any point. It does not change settings or start jobs automatically; the simulation can keep running while you read. **Pause the dish** pauses only when clicked. Resume from the main controls when ready.

**Show at every startup** is optional; otherwise the tour opens once. This preference is stored locally for your user, separately from world saves. **Detailed rules ↗** opens the full rules when you want more depth.

## Create a world

**New World…** opens a resizable dialog with **New World** and **Advanced Settings** tabs. The current dish pauses while you configure its replacement. Creation opens the new world paused for review; Cancel restores the previous playback state. Each new world has an editable signed 64-bit seed. Identical seed, configuration, and source genomes reproduce the same experiment.

| Preset | Mode | Prey / hunters | Food | Mutation | Seasons | Added sources |
| --- | --- | --- | --- | --- | --- | --- |
| **Continuous ecology** — app starting preset | Continuous | 60 / 0 | 55 | 12% | On | None |
| **Steady observation** | Continuous | 60 / 0 | 100 | 5% | Off | None |
| **Coexistence experiment** | Continuous | 60 / 10 | 100 | 5% | Off | 8 plant + 8 plankton |
| **Selection exploration** | Generation selection | 60 / 10 | 55 | 12% | On | None; holds each round for inspection |
| **Classic** | Continuous | 60 / 10 | 55 | 12% | On | None |

Fresh seeds 10001–10005 kept both hunter-free presets alive for 30 simulated minutes in all five trials. The continuous preset reached G38–50; steady observation reached G14–19. Coexistence retained both roles in only **2/5** fresh trials, so it remains experimental. The small sample supports a starting point, not indefinite survival or proof of better behavior. [Results and limitations](docs/ROADMAP.md), [raw held-out data](docs/audit/presets-held-out.csv), [original audit](docs/AUDIT.md).

### Advanced Settings

Every biological setting represented in `WorldConfig` is editable through a grouped form with hover explanations. Settings cover dimensions, founder counts, role/total caps, seasons, food targets and distribution, persistent sources, local food quotas, resource lifetime, body/vision/speed bounds, founder trait ranges, metabolism, energy capacity, reproduction, eggs, hunting, genetic mutation, selection duration, parent pools, elites, and fitness weights.

**Save settings…** stores a reusable `.settings` file including the displayed seed. **Load settings…** restores it. **Reset to selected preset** restores the entire template. **Scale space & density…** scales dimensions and adjusts population and food supply by area, with integer rounding; review the resulting values. Rock/nest positions scale with dimensions, while their counts and physical radii remain separately configurable. This is population/resource density matching, not identical terrain coverage.

Validation rejects incompatible ranges or counts before replacing the world. Runtime safety bounds include dimensions 300–6000, at most 2,000 adults plus eggs, and bounded food/archive sizes. The fixed 60 Hz timestep and 12 → 8 → 2 neural architecture remain compatibility invariants; changing brain topology requires a new genome format. Advanced Settings configures a **new** world. The small live sliders/toggles are intentional interventions in the current world.

## Observe and inspect

- **Pause / Resume**, **1× / 3× / 8× / Max speed**: control simulated time. Max speed uses a bounded frame time budget so controls remain responsive. Speed depends on CPU capacity; biological rules always run at 60 ticks per simulated second.
- **Predators**: off administratively removes hunters and their eggs without adding carrion; on can introduce the configured number of random hunter founders when none remain, subject to caps. This is an explicit intervention, tracked separately from natural deaths.
- **Inspect creatures**: click a swimmer. With the dish focused, Enter or Right Arrow selects the next living swimmer. **Trails** shows recent paths; **Senses** shows the selected creature's range and rays.
- **Generation banner**: selection mode shows the current round; continuous mode shows the range of living lineage generations. Both display the deepest ancestry reached. Overlapping continuous generations do not have one shared current round.
- **Names and clans**: names are deterministic identity labels, never draws from the ecological RNG. Children retain their clan. Stable numeric IDs remain in details for precise tracing. Names/clans persist in snapshots.
- **Theme / Tooltips**: affect presentation; the dish remains dark. Headers use Georgia and body text uses Segoe UI when installed, with portable fallbacks and larger minimum control text. Dark mode pairs warm light-beige text with green surfaces. Hover help can be turned off and back on without reopening the app. **Help ↗** opens the quick Getting started tour, with **Detailed rules ↗** available inside it.

The right-hand tabs launch resizable, nonmodal windows that can stay open while the world runs:

| View | Contents |
| --- | --- |
| **Creature details** | Enlarged live brain and selected life/body details. Save its brain/body from Inspect. |
| **Dashboard** | Per-role natural deaths, administrative removals, selection replacements, successful hatches, descendant births, egg losses, minima, first role absence, lifespan, efficiency, and archive/reference limits. Also opens genome/population comparisons. |
| **Family** | Search by name, clan, or ID; filter living/prey/hunters. Click named cards to focus ancestors, children, and grandchildren. Recent dead candidates can still be inspected; older archived lives retain genealogy rather than a complete brain. |
| **Species** | Living reference-based genetic clusters, origin times, parent references, and counts. |
| **Graphs** | Full-size history plots, including separate prey/hunter natural lifespan and efficiency, plus retained-history CSV export. |
| **Experiments** | Paired background treatments with long horizons, independent workers, incremental logs, and checkpoints. |
| **Parallel worlds** | Independent evolutionary islands copied from the current top parent pools; stop, resume, inspect, or open a checkpoint in the dish. |

Family history retains the latest 6,000 births; search shows up to 500 matches and the focused graph up to 200 relatives. Selection candidates retain up to 400 recent dead lives. Genetic references stop expanding at 256 per role. Graphs retain 240 samples at five-second intervals (about 20 minutes); background CSV logs preserve longer observations. These limits are visible in the data windows.

## How life works

| Object | Default appearance | Behavior |
| --- | --- | --- |
| Prey | Mint oval | Forages for resources; can be captured. |
| Hunter | Coral arrow | Hunts prey, scavenges carrion, and eats exposed prey eggs. Plants/plankton are unavailable to hunters. |
| Plants | Lime circles | Stationary resource preferred by grazers. |
| Plankton | Blue diamonds | Drifting resource preferred by filter feeders. |
| Carrion | Purple squares | Death remains preferred by scavengers; default lifetime 30 seconds. |
| Eggs | Cream ovals with progress arcs | Default incubation six seconds, three inside a nest. Count as births only after hatching. |
| Nests | Dashed gold rings | Protect eggs from hunters and accelerate incubation. Adults remain vulnerable. |
| Rocks | Slate circles | Block movement. Bodies do not collide with each other. |
| Sources | Colored rings with a plus | Repeatedly attempt to generate nearby plants/plankton. |

Creature fill can show object type, clan/family, genetic group, or diet. Outlines and shapes retain prey/hunter identity. Resource colors remain fixed.

### Brains, inheritance, and energy

The network has 12 inputs, eight recurrent hidden neurons, two outputs, and 186 inherited weights including biases. Inputs describe the chosen target, nearest hunter, energy, three obstacle rays, age, and an oscillator. Outputs control turning and throttle; minimum throttle still moves slightly. Recurrent hidden state supplies short-term memory. There is no within-lifetime weight training.

Target choice, ingestion, collision, and reproduction are explicit rules; movement comes from the network. Asexual offspring inherit weights, size, speed, vision, and diet, with fresh hidden memory. Roles remain prey/hunter; they do not mutate into each other. Larger bodies store more energy but cost more to maintain and turn more slowly; speed and vision have costs too.

Mutation is a chance **per weight or trait**, not a percentage of creatures. At 12%, about 22 weights change per offspring on average. The slider does not alter existing adults. Diet mutation is attempted at 0.4 times the mutation rate by default. A preferred resource supplies 1.15× energy; another supplies 0.35×. Body limits, mutation amplitudes, digestion and metabolic factors are configurable.

In continuous mode, default maturity/cooldown are 7.5 seconds. A parent above 86% capacity invests 47% of current energy; the embryo receives 90% of that investment. Incubation also costs energy. Parents keep living; reproduction does not create free energy. Death occurs through predation, energy depletion, or the configured age limit (default 240 seconds). Caps include **adults plus eggs**: default total 180, prey 150, hunters 30. Continuous extinction is permanent until the user explicitly intervenes, loads, or creates a world.

### Generation selection and inspection

Default selection rounds last 45 simulated seconds. Each role selects from its top 12 candidates in the living/recent-dead pool, with two-candidate tournaments. Default fitness favors meals (100), offspring (30), age (0.015 per tick), and distance (0.002 per unit). One elite per role is copied unchanged by default; the remaining offspring mutate. Parent-pool size, elite count, fitness weights, and round duration are configurable.

**Inspect each generation** stops time **before** the old population is replaced. **Best prey / Best hunter** opens a champion's brain and body, including a retained dead candidate when it scored best. Save or compare it, then choose **Breed & continue**. The hold survives saving/loading. Continuous mode disables this inspection toggle because its births overlap rather than finishing synchronized rounds.

Generation selection replaces populations and refills food, and can restore roles from retained genomes. That is assisted selection. Continued population in this mode does not establish self-sustaining ecology.

### Food and seasons

Default food target: `floor((90 + 5 × abundance) × seasonFactor)`, bounded by the food limit. Every ten ticks, replenish up to `1 + floor(abundance / 20)` pieces while below target (and apply any configured refill multiplier). Abundance 55 permits up to 18 pieces/second; 100 permits up to 36. Lowering abundance does not instantly delete existing food.

Default automatic food is 70% plants/30% plankton, with 75% near the fertile patch and the rest scattered. Seasons last 30 seconds each: spring ×1.15, summer ×1, autumn ×0.65, winter ×0.35. The patch moves, with gradual random seasonal removal. Seasons off uses the summer patch/multiplier.

Sources default to one piece every half second, subject to the shared 1,000-piece food cap. **Local source quota** optionally limits food near each source, helping test regional access. **Resource lifetime** adds optional plant/plankton turnover; zero preserves unlimited lifetime. These are experimental controls, not automatic rescue.

### Habitat tools

Pause, open **Shape the habitat**, choose a tool, and click/drag:

- Plants/plankton/carrion scatter temporary food.
- Rocks use the brush radius and cannot cover a living body or egg.
- Erase removes whole rocks, nests, and sources touched by the brush; it does not remove swimmers or loose food.
- Nests add egg refuges. Brains are not automatically directed toward them.
- Plant/plankton sources add persistent feeding sites.

Brush size affects rocks/erasure, not food scatter. Maintain accessible paths; globally abundant food can still be unreachable. Habitat edits are part of world saves.

## Background experiments and evolutionary islands

**Experiments** varies one setting between paired A/B worlds: abundance, mutation, seasons, hunter presence/count, local source quota, or resource lifetime. Each pair shares a starting snapshot/RNG state; initial hunter-count comparisons generate the larger founder pool first and omit surplus founders without deaths/carrion. Choose common advanced settings, 1–32 paired seeds, 5–86,400 seconds, and 1–8 workers. Pair order/results remain reproducible regardless of worker count. Selection auto-continues in paired experiments.

Recording defaults on. Each timestamped output directory contains per-condition `manifest.properties`, `initial.world`, rolling `latest.world`, and `observations.csv`. CSV records per-role extinction/minima, hatches, natural/admin deaths, descendant births, efficiency/lifespan, diet/clan diversity, genetic groups, and rounds. Cancellation retains completed observations and checkpoints. These use fresh maps generated from the common settings, rather than copying edits from the dish.

**Parallel worlds** captures the current top genomes per role (default pool size 12), including bodies/diets and available recent candidates. All islands start from copies of those pools, then use different recorded seeds. There is no automatic exchange or merging of genomes. Imported body traits fit the chosen island bounds. A requested role needs an available source pool; the UI reports missing source genomes instead of silently introducing unrelated founders.

Choose up to 32 worlds and eight workers, a duration, observation/checkpoint intervals, and an output folder. Workers run as fast as available CPU allows. Island generations count from their seeded fork, with the source world's depth recorded as provenance. **Stop selected / Stop all** requests a checkpoint; **Breed / resume selected** resumes a stopped island or a held selection round. **Open checkpoint** replaces the dish with a paused copy; a running worker continues independently. After an app restart, Load world can resume any individual checkpoint. Batch controls retain their job list within the current application session.

Each island also records its source `.brain` files and provenance. A selection island configured to inspect generations releases its worker while waiting, leaving room for other worlds to run.

## Measure evolved behavior

The quick **Compare with founding ancestor** benchmark retains five paired 45-second trials with reproduction disabled and fresh memory. It evaluates one genome, not population-wide improvement, on the default map with current food/season settings.

**Population study on held-out seeds…** samples living genomes across clans/roles and compares each with its own founder. Trials cover at least one configured seasonal cycle and enable reproduction. Both background-hunter conditions are tested; a focal hunter is retained when other hunters are off. Results report meals, survival, efficiency, direct offspring, and lineage descendant births. Lineage birth totals survive pruning of old family records.

Studies stream `trials.csv` and retain source genome pairs and settings under `exports/study-*`; cancellation preserves completed rows. The visible prose report can be saved as text. Sampling living genomes introduces survivor bias; clan stratification is not abundance weighting. More births, genetic groups, or ancestry depth alone does not prove better behavior.

## Save and resume

- **Save brain & body…** writes a `.brain` genome: weights, body, role, diet, hue. **Seed from saved genome…** creates a fresh population, rather than resuming an individual's age or memory. Legacy v1/v2 brains remain readable; new brains use v3.
- **Save world…** writes an exact `.world` snapshot: settings, seed/RNG state, brains and memory, names/clans, eggs, resources, habitat, lineage, counters, and generation holds. Loading confirms replacement and opens paused. Playback speed, theme, colors, and selection remain presentation choices.
- World format **v2** reads original **v1** saves. Old saves receive classic configuration and names. Their original RNG continues; unavailable per-role history is labeled as beginning at migration. Old graph samples leave per-role plots blank and export empty cells with an availability flag. The new selection-round counter starts at one on migration (marked `*`); the original deepest lineage generation is preserved. Keep important saves backed up when upgrading.

Natural deaths, administrative hunter removals, and generation replacements have separate per-role counters. Hunter removal also counts removed eggs. Aggregate legacy births/deaths graphs retain their original definitions, including founders and administrative deaths; per-role lifespan graphs exclude administrative deaths. Efficiency includes meal income, metabolism, and incubation, and excludes parental reproductive investment.

## Reproduce evaluations

```powershell
javac -encoding UTF-8 -cp out/classes -d out/classes tools/*.java
java -Xmx1024m -cp out/classes PresetEvaluation out/presets.csv 1800 5 10001 3
java -Xmx1024m -cp out/classes BalanceEvaluation out/balance 600 3
java -Xmx512m -cp out/classes SurvivalAudit out/original-sweep.csv 600 5
```

`PresetEvaluation` tests real new-world presets on fresh seeds. `BalanceEvaluation` runs one-factor treatments and population/resource density-matched 1×/1.5× worlds. `SurvivalAudit` preserves the original audit's setup: common classic founder/food stock, subset omissions and later setting changes. These setup differences matter when comparing survival. Evaluations can take several minutes.

## Project and Git

`Simulation` owns biology; `WorldConfig` defines settings; `WorldSettingsPanel` edits them. `EcosystemView` draws/edits the dish. `Main`, `FamilyPanel`, and data panels handle the desktop UI. `ExperimentRunner`, `WorldBatch`, `RunRecorder`, and `EvolutionStudyPanel` handle isolated experiments. `BrainIO`/`WorldIO` own persistence. `SimulationChecks`, `RoadmapChecks`, and `UiPreview` provide validation. `SpatialGrid` indexes nearby food/creatures in deterministic encounter order.

This folder is a local Git repository on `main`. Source, docs, fixtures, and published evaluation CSVs are tracked. Generated `out/`, personal IDE state, credentials, exports, and personal `.world`/`.brain` saves are ignored. Keep separate backups of personal simulation saves. GitHub can be added later as a remote.

```powershell
git status
git diff
git add src tools tests README.md AGENTS.md docs check.ps1
git diff --cached
git commit -m "Describe the change"
```

A commit records staged changes locally. Contributor guidance: [AGENTS.md](AGENTS.md). Implementation status and remaining limits: [docs/ROADMAP.md](docs/ROADMAP.md).
