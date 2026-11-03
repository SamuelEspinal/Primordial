# Project direction

Primordial is a dependency-free Java desktop evolution sandbox. Preserve its central idea: inherited neural controllers and bodies face hunger, resource competition, predation, and the energy cost of offspring. Make behavior and ancestry observable, and let users shape habitats and run reproducible experiments.

Use the current implementation as the starting point, not a promise that every constant is optimal. Read `README.md` and `docs/AUDIT.md` before changing ecology or long-run behavior.

## Architecture and scope

- Target JDK 17+ and the existing default-package layout. The app uses Swing/Java2D and has no external libraries or build framework. Keep ordinary changes consistent with that setup; introduce dependencies only when the task benefits enough to justify them.
- Keep model rules in `Simulation`, rendering/input in `EcosystemView`, and application controls in `Main`. Keep benchmark/experiment worlds separate from the live simulation.
- Movement comes from the recurrent network. Target selection, ingestion, collision, and breeding are explicit model rules. Do not silently replace neural movement with scripted food seeking or fleeing.
- Preserve both reproduction modes. Continuous mode allows genuine extinction without automatic rescue; generation selection explicitly replaces populations and can restore roles. Explain any intervention or new rescue mode in the UI and docs.
- Evolution is inherited variation and selection, not guaranteed progress. Genetic clusters, deepest ancestry, and high population alone do not prove better behavior.

## Simulation changes

- Advance the model at a fixed 60 ticks per simulated second. Presentation speed must not change per-tick biology.
- Route model randomness through the simulation's seeded `Random`. Avoid wall-clock randomness or random draws from painting/UI refresh. Identical starting state and inputs should produce identical continuation.
- Offspring must be funded by parental energy. Keep adult/egg population caps, trait bounds, finite values, world bounds, and rock exclusion coherent. Account for births at hatching, deaths once, and egg losses when applicable.
- Read the previous hidden state before updating recurrent neurons. Offspring inherit weights and body traits, not the parent's current hidden activity.
- Treat world size, resource density, replenishment, seasonal patch coordinates, initial role counts, and population caps as related configuration. Increasing dimensions alone can make food harder to find. Initial count and dimensions are currently source constants, not UI settings.
- For balance changes, compare multiple seeds over several seasonal cycles and enough time for founders to die (240 seconds). Report complete extinction, role extinction, births after founders, ancestry depth, and population troughs. Test long enough to distinguish early proliferation from sustained reproduction.
- Keep paired experiments paired: begin with matching genomes, map, resources, and RNG state, then vary the named setting. Label multi-setting presets separately. State whether administrative removals count as deaths or create food.

## Persistence and history

- `.brain` stores inherited genomes; `.world` stores resumable simulation state. Do not conflate them.
- World snapshots include RNG state and recurrent memory and use filtered Java serialization. Review format/version and `serialVersionUID` implications when changing serialized classes, collections, or brain dimensions. Add migration or clearly document incompatibility where required; do not assume a new field preserves old saves correctly.
- Validate loaded data before exposing it to the live world. Preserve atomic save replacement and the serialization allowlist/size limits.
- Keep archives and visual history bounded. Current limits include 6,000 ancestry records, 240 graph samples, 400 archived selection candidates, and 256 genetic references per role. A cap is a measurement limitation that the UI/docs should disclose.

## Interface and explanations

- Follow the current quiet nature-themed layout, light/dark palettes, dark dish, sidebar tabs, and bottom history graph. Use `UiTheme` colors and shared helpers rather than scattering new styling.
- Identify objects by shape as well as color. Preserve role outlines when changing creature fills. Give new controls useful tooltips and accessible descriptions via existing helpers.
- Keep expensive experiments/comparisons off the Swing event dispatch thread. Do not mutate the live simulation from a background worker; capture or clone the input first.
- Clearly label simulated versus real time, cumulative versus current statistics, adults versus eggs, and founders versus descendant births. Explain reset/load side effects accurately.
- Keep README controls, formulas, defaults, and audit recommendations consistent with the source. Document known shortcomings without presenting untested proposals as implemented features.

## Validation and delivery

```powershell
New-Item -ItemType Directory -Force out/classes | Out-Null
javac -encoding UTF-8 -d out/classes src/*.java
java '-Djava.awt.headless=true' -cp out/classes Main --self-test
```

For UI changes, also run `Main --render-ui out/ui-preview` with headless mode enabled and inspect the generated PNGs in both themes and both sizes. Rendering without an exception does not establish that a layout is usable.

For ecology changes, compile `tools/SurvivalAudit.java` against the compiled app and run a seeded survival sweep; record the exact settings, horizon, seeds, and limitations alongside any recommendation. Keep generated classes/screenshots under `out/`; retain deliberately published audit evidence under `docs/audit/`.

Add focused regression checks for changed model invariants or reproducibility bugs. Documentation-only edits need source/command accuracy checks, not tests that mirror prose. Do not tune defaults just to make an existing assertion pass. Report what changed, what was verified, and any unresolved limitations. Preserve unrelated user work.
