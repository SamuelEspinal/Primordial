import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;

/** Paired seeded worlds: a fresh common state for each seed, with exactly one setting changed. */
public final class ExperimentRunner {
    public enum Variable {
        FOOD_ABUNDANCE, MUTATION_PERCENT, SEASONS, PREDATORS;
        @Override public String toString(){return switch(this){case FOOD_ABUNDANCE->"Food abundance";case MUTATION_PERCENT->"Mutation (%)";case SEASONS->"Seasons (0/1)";case PREDATORS->"Predators (0/1)";};}
    }
    public record Config(Variable variable,double baseline,double alternative,int runs,int seconds,long seed) {
        public Config {
            if(variable==null||!Double.isFinite(baseline)||!Double.isFinite(alternative)||runs<1||runs>10||seconds<1||seconds>300)throw new IllegalArgumentException("Use 1–10 runs and 1–300 seconds.");
            double max=variable==Variable.FOOD_ABUNDANCE?100:variable==Variable.MUTATION_PERCENT?50:1;
            if(baseline<0||alternative<0||baseline>max||alternative>max)throw new IllegalArgumentException("Setting is outside its allowed range.");
            if((variable==Variable.SEASONS||variable==Variable.PREDATORS)&&(baseline%1!=0||alternative%1!=0))throw new IllegalArgumentException("Use 0 (off) or 1 (on).");
        }
    }
    public record Outcome(long seed,String condition,int prey,int predators,int eggs,long births,long deaths,
                          double lifespan,double efficiency,int generation,int groups,boolean extinct) {}
    public record Report(Config config,List<Outcome> outcomes) {
        public Report {outcomes=List.copyOf(outcomes);}
        public String csv(){
            StringBuilder text=new StringBuilder("seed,condition,prey,predators,eggs,births,deaths,lifespan_seconds,energy_efficiency,generation,genetic_groups,extinct\n");
            for(var o:outcomes)text.append(String.format(Locale.US,"%d,%s,%d,%d,%d,%d,%d,%.6f,%.6f,%d,%d,%s%n",o.seed,o.condition,o.prey,o.predators,o.eggs,o.births,o.deaths,o.lifespan,o.efficiency,o.generation,o.groups,o.extinct));return text.toString();
        }
        public String summary(){
            double[] delta=new double[config.runs];double base=0,alt=0;int extA=0,extB=0;
            for(int i=0;i<config.runs;i++){Outcome a=outcomes.get(i*2),b=outcomes.get(i*2+1);double ac=a.prey+a.predators,bc=b.prey+b.predators;base+=ac;alt+=bc;delta[i]=bc-ac;if(a.extinct)extA++;if(b.extinct)extB++;}
            double mean=Arrays.stream(delta).average().orElse(0),sum=0;for(double d:delta)sum+=(d-mean)*(d-mean);
            double se=config.runs>1?Math.sqrt(sum/(config.runs-1)/config.runs):0;
            return String.format(Locale.US,"""
                    PAIRED ECOSYSTEM EXPERIMENT
                    Setting: %s · A %.2f vs B %.2f
                    %d seeds · %d simulated seconds per condition

                    Mean ending adults: A %.2f / B %.2f
                    Paired difference (B − A): %+.2f
                    Standard error of paired differences: %.2f
                    Complete extinctions (no adults or eggs): A %d / B %d

                    Each pair begins from the same world snapshot, including
                    genomes, food, map, and random state. Only the named
                    setting is changed. Predator-off removes initial hunters;
                    it does not add corpses or count removals as deaths.
                    Births exclude founders. Experiments run separately
                    from your live world. These are small stochastic samples,
                    not proof that one condition is universally better.

                    """,config.variable,config.baseline,config.alternative,config.runs,config.seconds,base/config.runs,alt/config.runs,mean,se,extA,extB)+csv();
        }
    }
    private ExperimentRunner() {}
    public static Report run(Config config,BooleanSupplier cancelled,IntConsumer progress)throws Exception {
        List<Outcome> outcomes=new ArrayList<>();
        for(int run=0;run<config.runs;run++){
            long seed=config.seed+run;Simulation initial=new Simulation(new Random(seed));byte[] common=WorldIO.encode(initial);
            for(int condition=0;condition<2;condition++){
                Simulation s=WorldIO.decode(common);apply(s,config.variable,condition==0?config.baseline:config.alternative);
                long births=s.totalBirths,deaths=s.totalDeaths;
                for(int tick=0;tick<config.seconds*60;tick++){
                    if(tick%60==0&&(cancelled.getAsBoolean()||Thread.currentThread().isInterrupted()))throw new java.util.concurrent.CancellationException();s.step();
                }
                outcomes.add(new Outcome(seed,condition==0?"A":"B",s.count(Simulation.Species.PREY),s.count(Simulation.Species.PREDATOR),s.eggs.size(),s.totalBirths-births,s.totalDeaths-deaths,s.meanLifespan(),s.efficiency(),s.generation,s.tracker.livingCounts(s).size(),s.aliveCount()==0&&s.eggs.isEmpty()));
                progress.accept((outcomes.size()*100)/(config.runs*2));
            }
        }
        return new Report(config,outcomes);
    }
    private static void apply(Simulation s,Variable variable,double value){
        switch(variable){
            case FOOD_ABUNDANCE->s.abundance=(int)value;
            case MUTATION_PERCENT->s.mutationRate=value/100;
            case SEASONS->s.seasons=value==1;
            case PREDATORS->{s.predatorsEnabled=value==1;if(!s.predatorsEnabled){s.creatures.removeIf(c->c.species==Simulation.Species.PREDATOR);s.eggs.removeIf(e->e.embryo.species==Simulation.Species.PREDATOR);}}
        }
    }
}
