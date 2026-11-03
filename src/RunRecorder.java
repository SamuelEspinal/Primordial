import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Incremental, bounded checkpoints plus an append-only observation CSV per world. */
public final class RunRecorder implements AutoCloseable {
    public static final String HEADER="seconds,prey,hunters,prey_eggs,hunter_eggs,descendant_births,prey_hatches,hunter_hatches,natural_deaths,administrative_removals,selection_replacements,prey_extinct_at,hunters_extinct_at,min_prey,min_hunters,min_viable_prey,min_viable_hunters,prey_efficiency,hunter_efficiency,prey_lifespan,hunter_lifespan,diets,families,genetic_groups,deepest_generation,selection_round,food";
    private final Path folder;
    private final PrintWriter csv;
    public RunRecorder(Path folder,Simulation s,String provenance)throws IOException {
        this.folder=folder;Files.createDirectories(folder);
        Properties metadata=s.settings().properties();metadata.setProperty("simulationVersion","2");metadata.setProperty("seed",s.seedKnown?Long.toString(s.seed):"legacy-unrecorded");metadata.setProperty("source",provenance);metadata.setProperty("initialElapsedTicks",Long.toString(s.elapsedTicks));
        metadata.setProperty("statisticsSinceMigration",Boolean.toString(s.statisticsSinceMigration));
        if(!Files.exists(folder.resolve("manifest.properties")))try(var w=Files.newBufferedWriter(folder.resolve("manifest.properties"))){metadata.store(w,"Replay settings; checkpoints preserve exact state");}
        if(!Files.exists(folder.resolve("initial.world")))WorldIO.write(folder.resolve("initial.world"),s);
        csv=new PrintWriter(Files.newBufferedWriter(folder.resolve("observations.csv"),StandardCharsets.UTF_8,StandardOpenOption.CREATE,StandardOpenOption.APPEND));
        if(Files.size(folder.resolve("observations.csv"))==0)csv.println(HEADER);
        sample(s);
    }
    public static String row(Simulation s){var p=s.stats(Simulation.Species.PREY);var h=s.stats(Simulation.Species.PREDATOR);long diets=s.creatures.stream().filter(c->c.alive).map(c->c.diet).distinct().count(),families=s.creatures.stream().filter(c->c.alive).map(c->c.rootId).distinct().count();return String.format(Locale.US,"%.3f,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%.3f,%.3f,%d,%d,%d,%d,%.6f,%.6f,%.3f,%.3f,%d,%d,%d,%d,%d,%d",s.seconds(),s.count(Simulation.Species.PREY),s.count(Simulation.Species.PREDATOR),s.eggCount(Simulation.Species.PREY),s.eggCount(Simulation.Species.PREDATOR),p.births+h.births,p.hatches,h.hatches,p.deaths+h.deaths,p.removals+h.removals,p.replacements+h.replacements,p.extinctAt,h.extinctAt,p.minimumAdults,h.minimumAdults,p.minimumViable,h.minimumViable,p.efficiency(),h.efficiency(),p.lifespan(),h.lifespan(),diets,families,s.tracker.livingCounts(s).size(),s.generation,s.selectionRound,s.food.size());}
    public void sample(Simulation s)throws IOException{csv.println(row(s));csv.flush();if(csv.checkError())throw new IOException("Could not write observations");}
    public void checkpoint(Simulation s)throws IOException{WorldIO.write(folder.resolve("latest.world"),s);}
    @Override public void close(){csv.close();}
}
