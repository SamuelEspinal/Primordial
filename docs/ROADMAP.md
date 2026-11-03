# Roadmap implementation

This implementation follows the twelve requested items. It keeps neural movement, fixed 60 Hz biology, funded reproduction, and genuine continuous extinction. The original audit remains a historical baseline rather than a description of every current control.

| Item | Implemented behavior |
| --- | --- |
| 1. Honest presets | Continuous ecology, explicit selection exploration, steady observation, classic, and an experimental coexistence setup. All new worlds display a seed. App startup uses Continuous ecology after the fresh-seed evaluation below. |
| 2. Repeatable long observations | Paired experiments up to 86,400 simulated seconds, 32 pairs, and eight workers. Incremental CSV, initial snapshots, rolling checkpoints and manifests. Per-role extrema/extinction/hatches, descendant births and diversity. |
| 3. Measurement fixes | Hunter eggs count as losses on removal. Natural deaths, administrative removals and selection replacements are separate. Per-role lifespan/efficiency in Dashboard, logs and graphs; archive/reference limits visible. |
| 4. Food/predator tuning | Configurable hunter founder counts/caps, hunting costs/cooldown, local source quotas and resource turnover. One-factor experiments and recorded initial trials, with no extinction rescue added. |
| 5. Evolved behavior | Clan-stratified living genome samples versus their own founders on fresh seeds, both background-hunter conditions, at least a complete seasonal cycle, reproduction enabled, source genome export and incremental trial CSV. |
| 6. Configurable space | Per-world dimensions, scalable classic habitat positions, linked population/resource density scaling, deterministic local food/creature grids, and recorded size comparisons. |
| 7. Advanced creation | Resizable New World dialog with presets, seed and grouped Advanced Settings; annotated validated configuration; reusable settings files; reset and density-scaling tools. |
| 8. Spacious data views | Nonmodal resizable windows for graphs, dashboard, genetic groups, named genealogy, experiments, background islands, detailed brains, long help and study reports. |
| 9. Named lives and clans | Deterministic nonnumeric names, inherited unique clan identities, stable internal IDs, search/filter and focused genealogy cards. Recent dead candidates retain inspectable brains. |
| 10. Visible generation | Prominent selection round or living lineage range, deepest ancestry, and a clear inspection hold state. |
| 11. Parallel evolution | Independent islands share captured top genomes per role, default top 12, with individual seeds, bounded CPU workers, checkpoints and stop/resume/inspection controls. No implicit cross-world migration. |
| 12. End-of-round pause | Hold before replacement, inspect/save/compare best prey/hunter, then Breed & continue. Works in saved worlds and selection islands. |

### Getting started follow-up

The top **Help** button now opens a six-topic beginner tour with Back/Next navigation and shortcuts to the existing controls. It appears on first launch, has an optional every-startup preference, and keeps the full rules one click away. Reading and navigating the tour do not change biological state or RNG; preferences stay outside world saves. Every page is included in light/dark and compact/large UI previews.

## Next feature roadmap

These are planned additions, not implemented features. Build them in this order so observations and saved runs become more useful before adding new biology.

1. **Favorites and Hall of Fame.** Bookmark creatures and clans, and deliberately preserve notable genomes, names, records, source settings and seeds beyond the rolling ancestry/candidate limits. Keep a favorite's genome distinct from a resumable world checkpoint.
2. **Life journals.** Give a creature a readable timeline of birth, notable meals, offspring, death cause and clan milestones. Bound routine history and retain selected milestones for favorites. Start with births, offspring and deaths before adding expensive per-meal recording.
3. **Persistent evolution lab.** Save the multi-world job queue and its progress so jobs can resume after restarting the app. Add side-by-side comparisons of independent dishes, with clear seeds, settings, elapsed simulated time and source parent pools. Existing islands already run in parallel; this extends restart management and comparison.
4. **Death causes and energy use.** Report starvation, predation and old age separately from administrative removal and selection replacement. Show energy spent on movement, maintenance, hunting and reproduction. Use these measurements with held-out runs to investigate coexistence collapses before tuning predator pressure.
5. **Evolution replay.** Record bounded milestones and let users scrub through saved observations, then consider time-lapse export. Label sampled replay separately from an exact, resumable checkpoint; preserve the configuration and seed needed to interpret each recording.
6. **More inherited body traits.** After survival testing and measurement improvements, explore traits such as armor with explicit maintenance and movement tradeoffs. Compare reproductive success and survival across fresh seeds, and version genome/world formats if inherited data changes.

### Completed interface polish

- Hover explanations recover after turning tooltips off and back on, including controls already under the pointer and open data windows. Regression checks exercise repeated toggles and theme changes; the native Swing check also verifies popup delivery.
- Distinct serif headings and readable sans-serif body text use installed-font and Unicode fallbacks, with warm light-beige text over the dark green theme. Data reports remain monospaced where alignment matters.

## Fresh-seed preset evaluation

[Raw CSV](audit/presets-held-out.csv): seeds **10001–10005**, 1,800 simulated seconds each, no intervention. These seeds were not used in the original 42–51 audit. The real new-world configuration is applied before founder/food generation. This differs from the original common-stock audit's later culling/settings changes.

| Preset | Survived / 5 | Both roles / 5 | Ending adult range | Deepest G range |
| --- | --- | --- | --- | --- |
| Continuous ecology | 5 | Hunters absent | 117–135 | 38–50 |
| Steady observation | 5 | Hunters absent | 140–147 | 14–19 |
| Coexistence experiment | 2 | 2 | 0–160 | 5–37 |

The CSV retains the earlier name “Supported coexistence”; the UI now calls it **Coexistence experiment**. Three of its five worlds collapsed by approximately 142, 298 and 686 seconds. Surviving worlds reached G31 and G37. This failure is reported rather than hidden by rescue or tuning on the same evaluation seeds.

The app now starts with 60 prey, hunters off, food 55, mutation 12%, and seasons on. The original classic constructor/settings remain available for old saves, audit replay, and controlled tests. Selection exploration uses explicit replacement and is not evidence of ecological sustainability. Five fresh seeds are exploratory evidence, not a guarantee; larger held-out samples and longer horizons remain valuable.

## Balance and space evaluation

`tools/BalanceEvaluation.java` runs three one-factor comparisons with common starting snapshots: initial hunters 10 versus 5 on the classic environment; local source quota 0 versus 30 in the coexistence environment; resource lifetime 0 versus 120 seconds in that environment. Seeds are 20001–20003, horizon 600 seconds. Results and exact common settings are under [audit/balance](audit/balance/).

The size experiment uses the steady preset at 1× and 1.5× linear dimensions, seeds 30001–30003, horizon 600 seconds. Population counts/caps and automatic food capacity/refill supply scale approximately by area. Counts/batches are rounded; terrain counts/radii are held fixed while positions/spreads scale. It therefore controls population/resource density, not every geometric or ecological feature. Wall times include concurrent contention and are not a controlled performance benchmark. Larger maps remain optional, not the default survival remedy.

| Comparison | Survival A / B | Mean ending adults A / B |
| --- | --- | --- |
| Initial hunters 10 / 5 | 1/3 / 1/3 | 40.3 / 12.0 |
| Local source quota 0 / 30 | 2/3 / 2/3 | 104.0 / 101.7 |
| Resource lifetime unlimited / 120s | 2/3 / 2/3 | 104.0 / 102.7 |
| Linear size 1× / 1.5× with density scaling | 3/3 / 3/3 | 145.7 / 327.7 |

These small comparisons do not establish a survival benefit from halving hunters, adding a quota, or enabling turnover. The larger worlds supported approximately the density-scaled population and required more processing time; they did not establish a need to enlarge the default map.

## Validation and compatibility

- Existing regression suite plus new configuration, named identity, hold/breed, administrative accounting, checkpoint, source-pool, scaled-world and parallel-versus-serial checks.
- World v1 fixture from baseline `1209c0e`, with a stored biological-state projection after another 900 ticks. Migration preserves the original continuation; missing historical per-role counters are marked unavailable rather than invented.
- Exact v2 world snapshot continuation, including recurrent state, RNG, eggs, counters and held generations.
- The original seed-42 classic run still matches its baseline biological projection after 21,600 ticks; no scripted movement or hidden survival rescue was introduced.
- Actual Swing components rendered in both themes, including Advanced Settings and the expanded family/experiment/island/study panels. Native OS window chrome is not part of those renders.

Run `check.ps1 -JdkHome <jdk-folder> -RenderUI` from the project folder, or use the commands in the README. Published evaluations have reproducible CLI entry points. Personal exports/checkpoints remain ignored by Git; the legacy fixture uses `.snapshot` so the deliberately committed test fixture is retained.

## Deliberate limits and further work

- Brain topology remains 12 inputs, eight recurrent neurons and two outputs. It is a genome-format invariant, not a per-world control. Runtime archive and safety bounds remain enforced.
- Family archives and genetic references remain bounded. The focused card graph shows ancestors, children and grandchildren, with scrollable branches and a 200-card limit. Ancient brains beyond retained candidate history are unavailable.
- Within a session, stopped islands can resume in the batch view. Across app restarts, an individual `.world` checkpoint resumes through Load world; a persistent multi-job queue is a future improvement.
- Studies intentionally sample living clans, which introduces survivor bias. They report comparisons rather than declaring universal improvement. A focal hunter remains present when other hunters are disabled.
- Density scaling and the small balance trials provide experiment infrastructure and initial evidence. They do not settle optimal predator pressure or justify increasing the default map/population.
- Increase held-out sample sizes, improve batch history/restart management, and investigate why the coexistence preset collapses before adding more ecological roles or neural complexity.
