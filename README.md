# Primordial

A local Java evolution sandbox: small recurrent neural networks steer swimming creatures, while hunger, predation, inherited bodies, and energy-funded eggs shape which lineages continue. It uses Swing and Java2D, runs without external libraries, and needs JDK 17 or newer.

## Run it

In IntelliJ, open this folder, select a JDK 17+ project SDK, mark `src` as the source folder if needed, and run `Main`.

From PowerShell, with a JDK's `java` and `javac` on your PATH:

```powershell
New-Item -ItemType Directory -Force out/classes | Out-Null
javac -encoding UTF-8 -d out/classes src/*.java
java -cp out/classes Main
```

Compile **all** source files together. There is no Maven/Gradle dependency or asset download. If the Java commands hang or resolve to an unexpected version, point them at the JDK directly, for example `& 'C:\Program Files\Java\jdk-17.0.3.1\bin\java.exe' -cp out/classes Main`.

## Your first world

The default 1,200 × 850 world starts with 60 prey, 10 hunters, four rocks, two nests, and a seasonal food patch. Food abundance is 55, mutation is 12%, and continuous reproduction, seasons, and hunters are enabled. New worlds use a random seed; experimental runs can use repeatable seeds.

Click **Pause** to inspect or edit without advancing time. Click a swimmer with **Inspect creatures** selected to see its brain, energy, body, diet, parents, and offspring. Click the dish first, then **Enter** or **Right Arrow** to select the next living creature. **Trails** shows recent paths; **Senses** shows the selected creature's sensing range and rays. **1× / 3× / 8× speed** changes how quickly simulated time advances, subject to CPU capacity. A larger application window enlarges the drawing, not the simulated world.

**New experiment** replaces the population, habitat edits, and statistics. It keeps the current food, mutation, seasons, hunter, and reproduction-mode settings; it also keeps the playback/pause state. Save a world before replacing it. **How life works** opens the in-app rules, and tooltips explain controls and objects. Theme and tooltip switches affect presentation.

## What lives in the dish

| Object | Appearance in Object colors | Behavior |
| --- | --- | --- |
| Prey | Mint oval | Forages for resources; can be caught by hunters. |
| Hunter | Coral arrow | Targets prey or carrion; can eat exposed prey eggs. Cannot eat plants or plankton. |
| Plants | Lime circles | Stationary food, especially useful to grazers. |
| Plankton | Blue diamonds | Drifts and bounces off walls and rocks; preferred by filter feeders. |
| Carrion | Purple squares | Remains from deaths, preferred by scavengers; expires after 30 simulated seconds. |
| Egg | Cream oval with progress arc | Hatches after 6 seconds, or 3 seconds inside a nest. |
| Nest | Dashed gold ring | Protects eggs from hunters and speeds incubation; adults remain vulnerable. |
| Rock | Slate circle | Blocks movement. Creatures have three directional obstacle sensors. |
| Food source | Colored ring with a plus | Continually attempts to produce nearby plants or plankton. |

Both roles inherit a diet: **grazer**, **filter feeder**, or **scavenger**. A preferred resource supplies 1.15× its base energy; another resource supplies 0.35×. Diet also influences which resource is sensed as the best target. Hunters receive energy from captures independently of this resource preference. Prey and hunter roles are inherited and do not mutate into one another.

### Brains and bodies

Each creature has 12 inputs, 8 recurrent hidden neurons, and 2 outputs, with 186 inherited weights including biases. Inputs describe a target's relative direction and proximity, the nearest hunter's direction and proximity, energy, three obstacle rays, age, and an internal oscillator. The outputs control turning and throttle. Even minimum throttle moves a little. Recurrent connections carry hidden state between ticks; this is short-term memory, not training within a lifetime.

Target selection, eating, collisions, and reproduction are model rules; the neural network determines movement. Offspring inherit mutated weights, size, maximum speed, vision, and diet. Hidden memory starts fresh. Larger bodies store more energy but cost more to maintain and turn more slowly; fast movement and longer vision also cost energy. Individuals die when energy runs out, when caught, or after 240 simulated seconds.

The **Mutation (%)** slider is the chance **per weight or trait** when offspring are created, not the percentage of creatures that mutate. At 12%, about 22 of the 186 weights change on average per offspring. Diet mutation is attempted at 0.4 times that rate and can randomly select the same diet. The slider never rewrites the current adults' genomes. Family hue remains inherited even when traits change.

### Reproduction modes

| Mode | What happens | When to use it |
| --- | --- | --- |
| **Continuous reproduction** | Adults reproduce from age 7.5 seconds when above 86% energy and off a 7.5-second cooldown. They invest 47% of current energy; the embryo receives 90% of that investment. Parents keep living. Eggs must hatch before counting as births. | Observe ecological survival, overlapping generations, and natural extinction. |
| **Generation selection** | Every 45 seconds, or when no adults or eggs remain, breed 60 prey and 10 hunters if hunters are enabled. Selection uses the top 12 candidates per role and a two-candidate tournament. One elite per role is copied unchanged. Others mutate. Food is refilled and the old population is replaced. | Keep selection running for a long observation session, including after a role disappears. |

Reproduction is asexual: each offspring copies one parent, with mutation where enabled. Generation selection favors meals, offspring, age, and distance, with meals carrying the largest weight. It is an assisted selection experiment, so its continued population is not evidence of a self-sustaining ecosystem. **Breed next generation** triggers the same replacement immediately and is enabled only in that mode. Dead candidates are kept in a bounded archive; stored champions provide a fallback.

Continuous mode has no automatic rescue. Its cap is **180 adults plus eggs**, with role limits of 150 prey and 30 hunters. Creature bodies do not collide with each other. The displayed **deepest G** is the deepest ancestry reached, not a synchronized round or a measure of intelligence.

### Food and seasons

Food abundance changes both the automatic food target and replenishment rate. The target is `floor((90 + 5 × abundance) × seasonFactor)`. Every ten simulation ticks, the environment attempts up to `1 + floor(abundance / 20)` new food pieces while below that target. At 60 ticks/second, abundance 55 permits up to 18 pieces/second and abundance 100 up to 36. Food already present is not instantly removed when you lower the slider.

About 75% of new automatic food is placed near the fertile patch; the remainder is distributed across the map. New resources are approximately 70% plants and 30% plankton. Each season lasts 30 seconds: spring ×1.15, summer ×1, autumn ×0.65, winter ×0.35. The patch moves between four locations, and one random food piece is removed every 20 ticks while seasons are enabled. Disabling seasons uses a fixed summer patch and ×1 supply.

Food sources are independent of the seasonal target: each attempts one piece every half second until the shared food count reaches 1,000. This is a global limit, so accumulated food in one region can suppress sources elsewhere. Food is not automatically moved to hungry creatures.

## Shape the habitat

Open **Shape the habitat**, select a tool, and click or drag in the dish. Pause first for precise edits.

| Tool | Effect |
| --- | --- |
| Paint plants / plankton / carrion | Scatters seven pieces per accepted brush event, subject to rocks and food limits. This is temporary food. |
| Draw rocks | Places circular solid barriers using the brush radius. Placement cannot overlap a living body or egg. Overlapping circles can form walls. |
| Erase habitat | Removes whole rocks, nests, and sources touched by the brush. It does not erase creatures or loose food. |
| Place nest | Adds an egg refuge. Creatures are not automatically directed toward it. |
| Plant source / Plankton source | Adds persistent feeding patches. Spread them between accessible regions rather than sealing food behind barriers. |

**Brush** affects rock and eraser size, not food scatter or nest size. Limits are 250 rocks, 50 nests, and 50 sources. Avoid narrow sealed compartments unless studying isolation; distant food is no help to a trapped lineage.

The color selector offers **Object colors**, **Family colors**, **Genetic groups**, and **Diet colors**. Resource colors stay fixed; creature outlines and shapes preserve prey/hunter identity.

## Inspect evolution and save progress

| Area | What it tells you |
| --- | --- |
| **Inspect** | Census, settings, selected creature's live neural activity, energy, body traits, diet, family, eggs, and genome file actions. Green neural links are positive, warm links negative, blue loops indicate recurrent memory. |
| **Dashboard** | Lifetime energy efficiency, completed lifespans, mean living traits, captures, births, deaths, eggs, and population limits. **Compare with founding ancestor** runs five paired 45-second trials in the background, with reproduction disabled and memory reset. It compares that genome and its original founder on the default map under current food/season settings; habitat edits are not copied into trials. |
| **Family** | A bounded family tree around the selected individual. The archive retains the latest 6,000 births. The view starts up to four ancestors above the selection, expands five levels from that root, and shows up to 250 nodes. Older relatives can disappear from the archive. |
| **Species** | Living reference-based genetic groups and their origins. Brain weights, body traits, and diet contribute to distance. New references appear beyond distance 0.48, up to 256 per role. These clusters are not biological species or proof of behavioral improvement. |
| **Experiments** | Isolated paired seeded worlds that vary one setting: food, mutation, seasons, or hunters. Each pair starts from the same snapshot. Choose 1–10 paired seeds and 5–300 seconds, cancel background work, and export a completed CSV. These start from fresh default worlds, not your edited dish. |
| **The story so far** | Population, completed lifespan, energy efficiency, mean size/speed/vision, or cumulative births and deaths, sampled every 5 seconds. Only the latest 240 samples are retained (about 20 minutes). |

Live births include the initial founders; the Experiments report subtracts founders. Eggs count as births only at hatching. Completed lifespan is a lifetime cumulative mean over deaths, including generation replacements. Energy efficiency is total meal energy recovered divided by metabolism and incubation energy spent; it is not a direct fitness score or a complete reproductive energy balance. Mean body traits combine both roles.

**Save brain & body…** writes a `.brain` genome: weights, body, role, diet, and family hue. **Seed from saved genome…** creates a fresh world, copying that genome into founders of its role with mutations after the first. This does not restore an individual's age, memory, family history, or surroundings. Legacy v1/v2 brains can be read; new files use v3.

**Save world…** writes a `.world` snapshot containing the simulation, including brain memory, eggs, resources, settings, ancestry, statistics, and exact random state. **Load world…** confirms replacement and opens the restored world paused. Presentation settings such as speed, colors, selection, and theme are separate. Keep saves with the source version used to create them: world snapshots currently rely on Java serialization and do not promise compatibility across model changes.

Turning **Predators** off removes existing hunters and their eggs. The live toggle counts adult removals as deaths and can leave carrion; the Experiments toggle omits initial hunters without those effects. Turning it on introduces ten random founding hunters if no hunters or hunter eggs remain. Use it as an intentional intervention and save first when preserving a lineage matters.

## Settings for longer observations

See [the audit](docs/AUDIT.md) for measured survival results, limitations, the world-size decision, and prioritized next steps. Survival and improvement are separate outcomes: look for repeated births over many generations, viable populations, distinct surviving families, and controlled ancestor comparisons.

Two continuous setups kept all five tested seeds alive for **30 simulated minutes**:

| Goal | Food | Mutation | Seasons | Hunters | Observed deepest ancestry at 30 minutes |
| --- | --- | --- | --- | --- | --- |
| More births and generational turnover | 55 | 12% | On | Off | G37–47 |
| A steadier, well-fed population | 100 | 5% | Off | Off | G13–18 |

Use **Continuous reproduction**, keep 60 starting prey, and start without hunters. Set the controls and choose **New experiment** for a clean habitat under those settings; save the previous world first. This fresh start refills food using the selected settings, whereas the audit's paired treatments retain the same initial food stock. Run at 3× or 8×, inspect diets/families, and save occasional world checkpoints. Avoid mutation 0% when you want new inherited variation. Five successful seeds support these starting points, not a promise of indefinite survival.

For both roles together, the tested setup used **food 100, mutation 5%, seasons off, hunters on**, plus eight plant sources and eight plankton sources spread across accessible top/bottom regions. All five tested worlds retained both roles at 30 minutes; [the audit](docs/AUDIT.md) gives exact locations. Simply raising food with hunters enabled can postpone a collapse rather than prevent it.

For a session that keeps explicitly selecting and breeding even after ecological failures, use **Generation selection**. Its continuing population reflects the explicit replacement rule.

**Keep the current map size.** Physical crowding is not the first limit because bodies do not collide, and enlarging the map without scaling food makes resources harder to reach. Reducing all founder counts did not consistently improve survival. The first practical reduction is hunter pressure, while retaining prey diversity. Custom founder counts and map dimensions currently require source changes; the hunter toggle supports their presence or absence.

The next development priorities are longer repeatable background experiments and checkpoints, clear role-specific survival/statistics, local food access and hunter-balance trials, then configurable map/population presets. The [audit](docs/AUDIT.md) gives the evidence, confirmed issues, and suggested order of work.

## Checks and reproducible audits

```powershell
java '-Djava.awt.headless=true' -cp out/classes Main --self-test
java '-Djava.awt.headless=true' -cp out/classes Main --render-ui out/ui-preview

javac -encoding UTF-8 -cp out/classes -d out/classes tools/SurvivalAudit.java
java -Xmx512m '-Djava.awt.headless=true' -cp out/classes SurvivalAudit docs/audit/local.csv 600 10
```

Quote `-Djava.awt.headless=true` in PowerShell. Self-tests cover recurrent memory, inheritance, energy-funded reproduction, predation, obstacles, seasons, persistence, deterministic continuation, genetic groups, paired experiments, and rendering. UI preview renders actual Swing components in both themes at 1,280 × 1,000 and 1,050 × 760, plus sidebar/editor views.

The audit tool records seeds 42 onward and observations at 60, 300, 600 seconds and the requested end time. Optional fourth argument selects comma-separated profiles, for example `default,gentle5,sources`. It changes no gameplay defaults. `-1` extinction timestamps mean extinction was not observed within the run (or the role started absent / selection mode was used). Its founder subsets retain the same initial food and RNG state; see the audit methodology before comparing results.

## Code map

| File | Responsibility |
| --- | --- |
| `src/Simulation.java` | Fixed 60 Hz model, brains, genetics, ecology, reproduction, habitat editing, statistics. |
| `src/Main.java` | Swing application, controls, dashboard, family tree, brain/history views. |
| `src/EcosystemView.java` | Dish rendering, selection, keyboard interaction, editing tools. |
| `src/UiTheme.java`, `UiHelp.java`, `ObjectLegend.java` | Themes, accessible hover help, color-and-shape object key. |
| `src/SpeciesTracker.java` | Fixed-reference genetic clustering. |
| `src/BrainIO.java`, `WorldIO.java` | Versioned genomes and validated, atomic world snapshots. |
| `src/Benchmark.java` | Paired current-versus-founder trials. |
| `src/ExperimentRunner.java`, `ExperimentPanel.java` | Background paired ecosystem experiments and CSV export. |
| `src/SimulationChecks.java`, `UiPreview.java` | Dependency-free checks and visual QA. |
| `tools/SurvivalAudit.java` | Longer survival sweeps with per-seed CSV evidence. |

Contributor guidance lives in [AGENTS.md](AGENTS.md).

## Version control

This folder is a local Git repository on the `main` branch. Git records project history on this computer; a GitHub remote can be connected later for an off-device copy and collaboration.

Source, documentation, published audit CSVs, and shared IntelliJ project configuration are tracked. `.gitignore` excludes generated `out/` files, personal IDE state, local credentials, and personal `.world`/`.brain` saves. Keep separate backups of simulation saves you want to preserve.

After making a change, review it and create a named checkpoint:

```powershell
git status
git diff
git add src README.md AGENTS.md docs tools
git diff --cached
git commit -m "Describe the change"
git log --oneline
```

Add new root-level project files explicitly when appropriate. A commit records staged files; it does not upload them. In IntelliJ, Git actions are available through the Commit and Git tool windows.
