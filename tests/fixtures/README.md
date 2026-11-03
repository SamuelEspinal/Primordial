# Legacy continuation fixture

`legacy-v1.snapshot` was generated using the Java classes from Git baseline `1209c0e`, before world configuration, names, and per-role measurements were introduced. The setup is the baseline `SimulationChecks.worldWithEgg()` helper: seed 42, a single retained parent, a newly laid egg, cleared food/nests, and further reproduction disabled. Existing founder/reference archives are deliberately retained, as in the original test helper.

`legacy-after-900.txt` records that original implementation's biological projection after 900 more steps: clock/counters/energy expenditure, creature IDs/positions/energy/weights, then resource positions/headings in encounter order. `RoadmapChecks` loads the old snapshot, continues it and compares against that projection. New metadata is excluded from this comparison because it did not exist in v1. Separate checks enforce exact v2 snapshot continuation including the new metadata.

The snapshot uses `.snapshot` so the deliberate test fixture is tracked while personal `.world` saves remain ignored. It contains only generated test-world data.
