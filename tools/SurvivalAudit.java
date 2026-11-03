import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

/** Headless observational sweep. Changes no gameplay defaults; see docs/AUDIT.md. */
public final class SurvivalAudit {
    private record Profile(String name, int prey, int hunters, int food, double mutation,
                           boolean seasons, boolean sources, Simulation.Mode mode) {}

    private static final List<Profile> PROFILES = List.of(
            new Profile("default", 60, 10, 55, .12, true, false, Simulation.Mode.CONTINUOUS),
            new Profile("food100", 60, 10, 100, .12, true, false, Simulation.Mode.CONTINUOUS),
            new Profile("summer100", 60, 10, 100, .12, false, false, Simulation.Mode.CONTINUOUS),
            new Profile("no_hunters", 60, 0, 55, .12, true, false, Simulation.Mode.CONTINUOUS),
            new Profile("gentle12", 60, 0, 100, .12, false, false, Simulation.Mode.CONTINUOUS),
            new Profile("gentle5", 60, 0, 100, .05, false, false, Simulation.Mode.CONTINUOUS),
            new Profile("gentle0", 60, 0, 100, 0, false, false, Simulation.Mode.CONTINUOUS),
            new Profile("half_prey", 30, 10, 55, .12, true, false, Simulation.Mode.CONTINUOUS),
            new Profile("half_both", 30, 5, 55, .12, true, false, Simulation.Mode.CONTINUOUS),
            new Profile("five_hunters", 60, 5, 55, .12, true, false, Simulation.Mode.CONTINUOUS),
            new Profile("sources", 60, 0, 100, .05, false, true, Simulation.Mode.CONTINUOUS),
            new Profile("sources_hunters", 60, 10, 100, .05, false, true, Simulation.Mode.CONTINUOUS),
            new Profile("selection", 60, 10, 55, .12, true, false, Simulation.Mode.GENERATIONS));

    public static void main(String[] args) throws Exception {
        Locale.setDefault(Locale.US);
        if (args.length < 1 || args.length > 4) {
            System.err.println("Usage: SurvivalAudit output.csv [seconds=1800] [seeds=10] [profile,profile,...]");
            System.exit(2);
        }
        int seconds = args.length > 1 ? Integer.parseInt(args[1]) : 1800;
        int runs = args.length > 2 ? Integer.parseInt(args[2]) : 10;
        if (seconds < 1 || seconds > 86400 || runs < 1 || runs > 100) {
            throw new IllegalArgumentException("Use 1-86400 seconds and 1-100 seeds.");
        }
        Set<String> names = args.length > 3
                ? Arrays.stream(args[3].split(",")).collect(Collectors.toSet())
                : PROFILES.stream().map(Profile::name).collect(Collectors.toSet());
        if (names.isEmpty() || names.stream().anyMatch(n -> PROFILES.stream().noneMatch(p -> p.name.equals(n)))) {
            throw new IllegalArgumentException("Unknown profile; see PROFILES in tools/SurvivalAudit.java.");
        }
        Path output = Path.of(args[0]).toAbsolutePath();
        Files.createDirectories(output.getParent());
        try (PrintWriter csv = new PrintWriter(Files.newBufferedWriter(output, StandardCharsets.UTF_8))) {
            csv.println("profile,seed,seconds,initial_prey,initial_hunters,abundance,mutation,seasons,sources,mode,prey,hunters,eggs,births_after_start,eggs_laid,egg_losses,kills,deepest_generation,living_groups,food,min_adults_after_120s,peak_adults,extinction_second,prey_extinction_second,hunter_extinction_second");
            for (Profile p : PROFILES) {
                if (!names.contains(p.name)) continue;
                for (int run = 0; run < runs; run++) {
                    long seed = 42L + run;
                    Simulation s = new Simulation(new Random(seed));
                    // Retain the first N founders of each role from the same seeded world.
                    // Omitted founders cause no deaths, corpses, or RNG consumption.
                    int[] retained = new int[2];
                    s.creatures.removeIf(c -> ++retained[c.species.ordinal()] >
                            (c.species == Simulation.Species.PREY ? p.prey : p.hunters));
                    if (s.count(Simulation.Species.PREY) != p.prey ||
                            s.count(Simulation.Species.PREDATOR) != p.hunters) {
                        throw new IllegalStateException("Profile requires more founders than the model creates.");
                    }
                    s.abundance = p.food;
                    s.mutationRate = p.mutation;
                    s.seasons = p.seasons;
                    s.predatorsEnabled = p.hunters > 0;
                    s.setMode(p.mode);
                    if (p.sources) {
                        for (int x : new int[]{160, 400, 700, 1040}) {
                            for (int y : new int[]{150, 720}) {
                                if (!s.addSource(x, y, Simulation.Resource.PLANT) ||
                                        !s.addSource(x, y, Simulation.Resource.PLANKTON)) {
                                    throw new IllegalStateException("Audit source overlaps an obstacle.");
                                }
                            }
                        }
                    }
                    long initialBirths = s.totalBirths;
                    int min = Integer.MAX_VALUE, peak = s.aliveCount();
                    double extinct = -1, preyExtinct = -1, hunterExtinct = -1;
                    for (int tick = 1; tick <= seconds * 60; tick++) {
                        s.step();
                        int adults = 0, preyAndEggs = 0, huntersAndEggs = 0;
                        for (Simulation.Creature c : s.creatures) {
                            if (!c.alive) continue;
                            adults++;
                            if (c.species == Simulation.Species.PREY) preyAndEggs++;
                            else huntersAndEggs++;
                        }
                        for (Simulation.Egg egg : s.eggs) {
                            if (egg.embryo.species == Simulation.Species.PREY) preyAndEggs++;
                            else huntersAndEggs++;
                        }
                        peak = Math.max(peak, adults);
                        if (tick >= 120 * 60) min = Math.min(min, adults);
                        if (s.mode == Simulation.Mode.CONTINUOUS) {
                            if (adults == 0 && s.eggs.isEmpty() && extinct < 0) extinct = s.seconds();
                            if (preyAndEggs == 0 && preyExtinct < 0) preyExtinct = s.seconds();
                            if (p.hunters > 0 && huntersAndEggs == 0 && hunterExtinct < 0) hunterExtinct = s.seconds();
                        }
                        if (tick == seconds * 60 || tick == 60 * 60 || tick == 300 * 60 || tick == 600 * 60) {
                            csv.printf("%s,%d,%.0f,%d,%d,%d,%.2f,%s,%d,%s,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%.3f,%.3f,%.3f%n",
                                    p.name, seed, s.seconds(), p.prey, p.hunters, p.food, p.mutation,
                                    p.seasons, s.sources.size(), p.mode, s.count(Simulation.Species.PREY),
                                    s.count(Simulation.Species.PREDATOR), s.eggs.size(), s.totalBirths - initialBirths,
                                    s.totalEggsLaid, s.totalEggLoss, s.totalKills, s.generation,
                                    s.tracker.livingCounts(s).size(), s.food.size(),
                                    min == Integer.MAX_VALUE ? -1 : min, peak, extinct, preyExtinct, hunterExtinct);
                            csv.flush();
                            if (csv.checkError()) throw new java.io.IOException("Could not write audit CSV.");
                        }
                        // Extinction is absorbing in continuous mode. Continue resource updates
                        // to preserve food counts at each scheduled observation.
                    }
                    System.out.printf("%s seed=%d: %d prey, %d hunters, G%d, %d descendants, extinct=%.1fs%n",
                            p.name, seed, s.count(Simulation.Species.PREY), s.count(Simulation.Species.PREDATOR),
                            s.generation, s.totalBirths - initialBirths, extinct);
                }
            }
        }
        System.out.println(output);
    }
}
