# Primordial project audit

Audit date: November 2, 2026. Scope: all Java sources, project setup, existing checks, real Swing renders, and seeded headless simulation runs. This is an audit of the current implementation; gameplay constants and defaults were not changed. The added survival runner is an observation tool.

## Overall assessment

The project already has a coherent identity and a useful experimental foundation: inherited recurrent controllers and bodies, energy-funded reproduction, distinct ecological resources, an editable habitat, lineage inspection, genetic grouping, paired experiments, and exact world continuation. The code compiles on JDK 17 without dependencies. The main development need is to turn short, interesting runs into measurable long-lived ecosystems, with clear separation between ecological survival and assisted generation selection.

The world does not currently run out of physical space in the usual sense: creature bodies do not collide. Food access, reproduction, predation, and lineage survival are the first things to tune. Keep the existing dimensions while testing those relationships.

## Validation

- Compiled every file in `src` with UTF-8 using JDK 17.0.3.1.
- Ran `Main --self-test`: passed all existing checks, including recurrent memory, inheritance, reproduction, ecology, deterministic paired trials, and exact world continuation after 900 ticks. The first sandboxed attempt hit a Windows temporary-file replacement permission error; the unchanged suite passed when rerun outside that sandbox.
- Ran `Main --render-ui`: passed theme/model-preservation and tooltip checks. Inspected the actual light/dark Swing renders at 1,280 × 1,000 and 1,050 × 760. The large layout is readable; the minimum layout gives the dish much less space and requires sidebar scrolling.
- Ran the simulation directly in headless Java processes. This exercises the real model, and the preview exercises the real Swing components; this audit did not use a separate VM or manually play a native window.

Existing tests establish model invariants and determinism, not guaranteed population survival. In fact, the checked seed-42 default continuous world finishes the existing 360-second long-run test extinct, after 166 descendant births and deepest G6. The assertion requires reproduction to occur, not sustained survival.

## Survival experiment method

`tools/SurvivalAudit.java` uses the existing `Simulation` directly, with seeds starting at 42. All conditions for a seed begin with the same default-generated genomes, food, habitat, and random state. Settings are applied immediately afterward, matching how a user adjusts a newly opened world. In particular, abundance 100 does not begin with a freshly filled abundance-100 food stock.

Reduced-count conditions retain the first N founders of each role; omitted founders cause no deaths, carrion, or RNG consumption. This is an initial-subset experiment, not a claim that drawing fewer new random founders would produce the same results. Existing founder/reference archives remain present, so report descendant births relative to the starting counter and count **living** genetic groups. These worlds are observational and are not offered as saved production presets.

Source conditions place one plant source and one plankton source at each of eight sites: x = 160, 400, 700, 1,040 and y = 150, 720. That is 16 sources, attempting up to 32 additional pieces per second before the shared 1,000-piece limit. Default nests and rocks remain. No rescue or manual feeding occurs during a run.

Profiles are defined in the runner. Several change multiple settings and are labeled as presets; comparisons between those presets do not isolate a single cause. `food100` versus `default`, `summer100` versus `food100`, and the mutation presets with matching other settings provide narrower comparisons. Independent seeds are the replication unit, not individual creatures or time samples.

An ecosystem survives if at least one adult **or egg** remains at the horizon. Hunter coexistence means both roles have an adult or egg. Extinction timestamps include eggs, so an empty adult census awaiting hatching is not classified as extinction. Generation selection is reported separately because replacement can prevent permanent extinction by design. Neither ancestry depth nor birth count establishes behavioral improvement.

## Results and settings

The matched sweep ran **13 profiles × 5 seeds (42–46) × 600 simulated seconds**. [Raw observations](audit/survival-600s.csv) include 60-, 300-, and 600-second samples. Settings below are changes from the default (60 prey, 10 hunters, food 55, mutation 12%, seasons on, no extra sources).

| Profile | Changes from default | Surviving worlds / 5 | Both roles survive / 5 | Mean ending adults | Deepest G range |
| --- | --- | --- | --- | --- | --- |
| `default` | None | 1 | 1 | 20.6 | 5–26 |
| `food100` | Food 100 | 4 | 3 | 112.8 | 13–25 |
| `summer100` | Food 100, seasons off | 5 | 5 | 96.6 | 19–26 |
| `no_hunters` | Hunters absent | 5 | — | 108.8 | 18–26 |
| `gentle12` | Food 100, seasons off, hunters absent | 5 | — | 147.0 | 9–11 |
| `gentle5` | As gentle12, mutation 5% | 5 | — | 145.4 | 8–9 |
| `gentle0` | As gentle12, mutation 0% | 5 | — | 146.2 | 9–10 |
| `half_prey` | 30 prey, 10 hunters | 3 | 2 | 51.2 | 15–24 |
| `half_both` | 30 prey, 5 hunters | 1 | 0 | 21.6 | 7–22 |
| `five_hunters` | 60 prey, 5 hunters | 1 | 1 | 3.8 | 5–24 |
| `sources` | Food 100, mutation 5%, seasons off, hunters absent, 16 sources | 5 | — | 146.4 | 6–8 |
| `sources_hunters` | Food 100, mutation 5%, seasons off, 16 sources | 5 | 5 | 157.0 | 14–21 |
| `selection` | Generation selection mode | 5, assisted | 5, assisted | 46.6 | 14 |

An additional [10-seed comparison](audit/baseline-10seeds.csv), seeds 42–51, gives default survival **3/10**, versus **5/10** with food 100. Both roles survived in **1/10** default worlds and **4/10** high-food worlds. The five-seed table is a subset of those baseline trials, not independent additional evidence. This variation is a reason to avoid choosing defaults from a single seed.

The longer sweep extended **eight profiles × the same five seeds to 1,800 seconds** (30 simulated minutes). [Raw observations](audit/survival-1800s.csv) include intermediate samples. All 120 observations shared with the shorter sweep matched exactly; these extensions are follow-up observations of the same deterministic worlds, not new independent replicates.

| Profile | Surviving worlds / 5 | Both roles survive / 5 | Mean ending adults | Mean descendant births | Deepest G range |
| --- | --- | --- | --- | --- | --- |
| `default` | 1 | 1 | 8.4 | 1,153 | 5–51 |
| `summer100` | 2 | 2 | 45.4 | 4,537 | 27–61 |
| `half_prey` | 2 | 1 | 33.8 | 2,325 | 15–64 |
| `no_hunters` | 5 | — | 135.6 | 4,514 | 37–47 |
| `gentle12` | 5 | — | 147.4 | 1,482 | 15–21 |
| `gentle5` | 5 | — | 145.8 | 1,452 | 13–18 |
| `sources` | 5 | — | 145.0 | 1,282 | 12–14 |
| `sources_hunters` | 5 | 5 | 160.6 | 6,101 | 30–44 |

Means include failed worlds. In `summer100`, all five worlds were alive at ten minutes, but three collapsed between approximately 15.6 and 25.3 minutes. High birth counts before those failures did not make the ecosystems sustainable. The source-supported hunter preset retained both roles in all five 30-minute trials, with the total adult population never below 135 after the first two minutes. That is promising evidence for local food support, although it changes mutation as well as sources compared with `summer100`; a one-factor follow-up is needed before attributing the result to sources alone.

These are small exploratory samples using the same seeds for tuning and evaluation. A five-out-of-five result does not guarantee indefinite survival or establish superiority on fresh seeds. Increasing births or ancestry depth does not prove improved behavior. The 0% mutation treatment preserves selection among existing founders but introduces no new inherited variation, so it is a control rather than an evolution recommendation.

### Practical settings

- **To observe lots of turnover:** continuous reproduction, 60 prey, hunters off, food 55, mutation 12%, seasons on. All five worlds survived 30 minutes, producing 4,005–5,006 descendant births and reaching G37–47. The lowest adult population after two minutes across these runs was 68.
- **For a steadier population:** continuous reproduction, 60 prey, hunters off, food 100, mutation 5%, seasons off. All five survived 30 minutes, ending with 141–148 adults; their lowest adult census after two minutes was 129. They still produced 1,329–1,545 descendant births. Mutation 12% also survived all five high-food, constant-summer trials, so the evidence does not identify 5% as uniquely optimal.
- **To keep hunters and prey together:** continuous reproduction, 60 prey/10 hunters, food 100, mutation 5%, seasons off, plus eight plant sources and eight plankton sources spread between the eight sites described above. All five worlds retained both roles at 30 minutes and reached G30–44. Keep default nests and accessible paths. Treat this as a supported habitat preset, not the result of changing a single slider.
- **For continuing assisted selection:** generation selection, food 55 or higher, mutation around the current 12%, and hunters/seasons as desired. The replacement rule keeps breeding even after ecological failure. The tested default selection preset survived all five ten-minute trials; this is expected assisted behavior and does not establish continuous coexistence under other settings.

In the UI, pause, choose the settings, and use New experiment for a clean start. Initial hunter presence is controlled by the toggle; arbitrary founder counts are not exposed. A reset after setting food/seasons fills a new stock using those settings, while the paired audit deliberately retained common initial food. The exact audited setup is reproducible through the runner. Save worlds periodically and compare descendants with founders rather than interpreting more births as automatic improvement.

Reproduce the published data after compiling the app and runner as described in the README:

```powershell
java -Xmx512m '-Djava.awt.headless=true' -cp out/classes SurvivalAudit out/replay-600s.csv 600 5
java -Xmx512m '-Djava.awt.headless=true' -cp out/classes SurvivalAudit out/replay-baselines.csv 600 10 'default,food100'
java -Xmx512m '-Djava.awt.headless=true' -cp out/classes SurvivalAudit out/replay-1800s.csv 1800 5 'default,summer100,half_prey,no_hunters,gentle12,gentle5,sources,sources_hunters'
```

CSV row order/quoting may differ from the consolidated evidence files; compare values by profile, seed, and simulated time. Full sweeps can take several minutes. Console lines report completed worlds, and the runner flushes CSV at each observation.

## World size and initial population decision

**Keep the 1,200 × 850 map and 60 initial prey for the next iteration.** Do not make enlargement the first survival fix. The default four rocks cover about 4.4% of the rectangular world. Even at the 180-individual cap and maximum body radius 12, the sum of body-circle areas is only about 8% of the world, before allowing overlaps. This is a geometric bound, not a measurement of how creatures cluster near food; food-patch congestion can still matter through competition.

More space without more food lowers resource density and increases travel distances. Scaling each dimension by 1.5 would provide 2.25 times the area and only 44% of the previous average food density at the same food count. Vision and energy costs would remain unchanged. Keeping a larger map ecologically comparable would require scaling supply, food distribution, refuge placement, initial population, and carrying capacity together, rather than editing two constants.

Dimensions are compile-time constants; there is no world-size slider. Seasonal patch positions, nest/rock locations, patch spreads, the dish gradient, and role counts also contain fixed coordinates or constants. World snapshots validate against the current dimensions rather than storing a per-world size. A configurable world-size feature therefore needs a coherent configuration and save-format design.

Reduced founder counts are tested in the sweep, but reducing prey also discards starting genetic diversity. Prioritize food distribution and hunter pressure before choosing a smaller prey population. Keep any reduced-hunter treatment explicit, since it changes the evolutionary problem as well as survival odds.

The count results do not support a blanket instruction to halve the population: 30 prey/10 hunters improved the ten-minute matched result from 1/5 to 3/5, but 30 prey/5 hunters and 60 prey/5 hunters each survived only 1/5. At 30 minutes the 30-prey treatment retained 2/5 worlds, with both roles in just 1/5. Starting with fewer hunters also does not lower the eventual 30-hunter role cap. Keep count controls as an experiment to add, rather than treating an initial reduction as a guaranteed long-run balance fix.

No larger-map variant was run in this audit. The retain-size recommendation follows from the current collision rules, geometry, density effects, and tested alternatives, rather than an empirical claim that every larger configuration performs worse.

## Findings worth addressing

### High: survival has no quantitative acceptance target

The default long-run regression can pass with an extinct ecosystem. The in-app experiment limit is 300 seconds, only 2.5 seasonal cycles and just beyond the maximum individual lifespan. Some default extinctions in the audit occurred later than that. Add a separate repeatable balance evaluation over at least 1,800 seconds and multiple seeds, with survival/coexistence fractions and population troughs. This should be an evaluation target for a chosen preset, not a hidden rescue added to continuous mode.

### High: successful reproduction and measured improvement are easy to confuse

At 12% mutation, an offspring changes about 22 of its 186 weights on average, before body/diet changes. Early birth bursts can coexist with later collapse; many clusters can simply reflect random founders. Current-versus-founder comparison is a useful start, but evaluates one genome in five short default-map trials with reproduction disabled. Add population-level sampled comparisons, held-out seeds, and reproductive success over a longer interval. Keep selection-assisted and continuous statistics separate.

### Medium: a global food cap can hide local starvation

`Simulation.updateResources` stops all sources at a total of 1,000 food pieces. Uneaten plants behind a wall or in an unused region can suppress production near surviving lineages. Automatic food similarly uses a world-wide target, while 75% of additions favor one patch. Before adding a bigger world, expose regional food availability and test local source quotas or gradual resource turnover. Broad plant decay is a proposed ecology change; it is not currently implemented when seasons are off.

### Medium: hunter removal undercounts egg loss

`Simulation.setPredators(false)` records adult removals through `die`, but removes hunter eggs without increasing `totalEggLoss`. Egg removal during generation replacement does update that counter. A save-before/after toggle can therefore show missing eggs without the corresponding loss statistic. Record administrative removals separately or consistently count them, and add a focused regression. Live toggling also creates adult carrion, whereas paired experiments intentionally omit initial hunters without corpses or deaths; document or expose that intervention distinction.

### Medium: long-run measurements and history are bounded

The graph retains about 20 minutes, ancestry the latest 6,000 births, and references at most 256 genetic groups per role. Fast breeding can prune family history during an otherwise successful observation session. Once references reach their cap, new divergence is assigned to an existing reference, which limits interpretation. Add optional streamed CSV/event logs and checkpoint saves before increasing in-memory limits. Display reference-cap saturation and archive pruning clearly.

### Medium: important ecological inputs are scattered constants

Map size, 60 prey, 10 hunters, 150/30 role caps, food thresholds, season positions, and reproduction costs are distributed through the model and UI. A small validated world-configuration object would make presets, seed replay, density experiments, and per-world dimensions reviewable. Persist that configuration and keep a deliberate migration plan for existing Java-serialized snapshots.

### Low: small-window usability and replacement actions

At 1,050 × 760 the dish becomes small while controls and the sidebar consume most space. A collapsible sidebar or focus-on-dish mode would help observation without changing simulated dimensions. New experiment and genome seeding replace the world immediately; world loading already confirms. Consider a consistent unsaved-world confirmation or an automatic recovery checkpoint. New experiment currently retains ecological settings and pause/speed state, so a labeled reset-to-defaults action would also reduce ambiguity.

### Low: project reproducibility needs a lightweight baseline

At audit time, this folder had IntelliJ project files but no Git repository or build wrapper. The follow-up setup created a local Git repository on `main`, with generated `out/`, personal IDE state, and personal saves ignored. The dependency-free command line is documented in the README. Add a small compile/check script or CI job before making larger model changes. A new build framework is optional, not a prerequisite.

## Recommended next steps

1. **Define two honest presets:** one for continuous ecology and one for selection-assisted exploration. Show their exact settings and a replayable seed. Select continuous defaults using the recorded survival evidence, then test fresh held-out seeds before changing the current default.
2. **Make long observations repeatable:** extend background experiments beyond 300 seconds, add periodic CSV/checkpoints, and report role-specific extinction, population minima, successful hatches, diet/family diversity, and births after founders.
3. **Fix measurement inconsistencies:** account for hunter-egg removals, distinguish administrative deaths, show archive/reference limits, and separate per-role efficiency and lifespan.
4. **Tune food access and predator pressure:** test local feeding/turnover and explicit initial hunter counts with one-factor comparisons. Keep reproduction funded, and preserve continuous extinction as a real outcome.
5. **Measure evolved behavior:** compare representative descendants and founders on held-out seeds, with and without hunters, across full seasonal cycles. Track reproductive success as well as meal efficiency and survival.
6. **Then add configurable space:** introduce a world configuration and scalable habitat generation, followed by density-controlled size experiments. Add a spatial grid for local food/creature queries before substantially raising population caps; current sensing scans food and creatures each tick.

Do not prioritize extra creature roles, more visual effects, or a larger neural network until survival and measurements are repeatable. The existing feature set already supports useful evolutionary experiments.
