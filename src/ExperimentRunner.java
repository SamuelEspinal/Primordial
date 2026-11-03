import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.*;

/** Paired snapshots with bounded parallel workers and optional incremental evidence. */
public final class ExperimentRunner {
    public enum Variable {
        FOOD_ABUNDANCE, MUTATION_PERCENT, SEASONS, PREDATORS, INITIAL_HUNTERS, LOCAL_SOURCE_QUOTA, RESOURCE_LIFETIME;
        @Override public String toString(){return switch(this){case FOOD_ABUNDANCE->"Food abundance";case MUTATION_PERCENT->"Mutation (%)";case SEASONS->"Seasons (0/1)";case PREDATORS->"Predators (0/1)";case INITIAL_HUNTERS->"Initial hunters";case LOCAL_SOURCE_QUOTA->"Local source quota";case RESOURCE_LIFETIME->"Resource lifetime (s; 0 unlimited)";};}
        public double maximum(){return switch(this){case FOOD_ABUNDANCE,MUTATION_PERCENT->100;case INITIAL_HUNTERS->1000;case LOCAL_SOURCE_QUOTA->500;case RESOURCE_LIFETIME->3600;default->1;};}
    }
    public record Config(Variable variable,double baseline,double alternative,int runs,int seconds,long seed,WorldConfig world,int workers,Path output,int sampleSeconds,int checkpointSeconds){
        public Config(Variable v,double a,double b,int runs,int seconds,long seed){this(v,a,b,runs,seconds,seed,new WorldConfig(),2,null,30,300);}
        public Config {if(variable==null||!Double.isFinite(baseline)||!Double.isFinite(alternative)||runs<1||runs>32||seconds<1||seconds>86400||workers<1||workers>8||sampleSeconds<1||checkpointSeconds<1)throw new IllegalArgumentException("Use 1–32 pairs, 1–86400 seconds and 1–8 workers.");if(baseline<0||alternative<0||baseline>variable.maximum()||alternative>variable.maximum())throw new IllegalArgumentException("Setting is outside its allowed range.");if(variable!=Variable.RESOURCE_LIFETIME&&variable!=Variable.MUTATION_PERCENT&&(baseline%1!=0||alternative%1!=0))throw new IllegalArgumentException("Use whole values for this setting.");world=world.copy();world.pauseAtGenerationEnd=false;world.validate();Math.addExact(seed,runs-1);}
    }
    public record Outcome(long seed,String condition,int prey,int predators,int eggs,long births,long deaths,double lifespan,double efficiency,int generation,int groups,boolean extinct,String observations){}
    public record Report(Config config,List<Outcome> outcomes){
        public Report {outcomes=List.copyOf(outcomes);}
        public String csv(){StringBuilder b=new StringBuilder("seed,condition,prey,predators,eggs,births,deaths,lifespan_seconds,energy_efficiency,generation,genetic_groups,extinct,"+Arrays.stream(RunRecorder.HEADER.split(",")).map(name->"sample_"+name).collect(java.util.stream.Collectors.joining(","))+"\n");for(var o:outcomes)b.append(String.format(Locale.US,"%d,%s,%d,%d,%d,%d,%d,%.6f,%.6f,%d,%d,%s,%s%n",o.seed,o.condition,o.prey,o.predators,o.eggs,o.births,o.deaths,o.lifespan,o.efficiency,o.generation,o.groups,o.extinct,o.observations));return b.toString();}
        public String summary(){double a=0,b=0;int ea=0,eb=0;double[] delta=new double[config.runs];for(int i=0;i<config.runs;i++){var x=outcomes.get(i*2);var y=outcomes.get(i*2+1);a+=x.prey+x.predators;b+=y.prey+y.predators;delta[i]=y.prey+y.predators-x.prey-x.predators;if(x.extinct)ea++;if(y.extinct)eb++;}double mean=Arrays.stream(delta).average().orElse(0),sq=0;for(double d:delta)sq+=(d-mean)*(d-mean);double se=config.runs>1?Math.sqrt(sq/(config.runs-1)/config.runs):0;return String.format(Locale.US,"PAIRED ECOSYSTEM EXPERIMENT\n%s: A %.2f / B %.2f\n%d paired seeds · %d simulated seconds · %d workers\n\nMean ending adults: A %.2f / B %.2f\nPaired difference: %+.2f\nStandard error: %.2f\nComplete extinctions: A %d / B %d\n\nEach pair shares a starting snapshot and RNG state; only the named treatment changes. Founder omissions create no corpses or deaths. Births exclude founders. Selection auto-continues in paired experiments.\n\nPer-role extinction, minima, hatches, natural/admin deaths, diversity and reproductive success are included in CSV. With an output folder, partial observations and rolling checkpoints survive cancellation.\n\nSmall samples do not establish universal improvement.\n\n",config.variable,config.baseline,config.alternative,config.runs,config.seconds,config.workers,a/config.runs,b/config.runs,mean,se,ea,eb)+csv();}
    }
    public static Report run(Config config,BooleanSupplier cancelled,IntConsumer progress)throws Exception{
        if(cancelled.getAsBoolean())throw new CancellationException();ExecutorService pool=Executors.newFixedThreadPool(config.workers);AtomicInteger done=new AtomicInteger();List<Future<List<Outcome>>> futures=new ArrayList<>();
        try{
            for(int i=0;i<config.runs;i++){long seed=config.seed+i;futures.add(pool.submit(()->{
                WorldConfig initialConfig=config.world.copy();if(config.variable==Variable.PREDATORS)initialConfig.hunters=true;if(config.variable==Variable.INITIAL_HUNTERS){initialConfig.hunters=true;initialConfig.initialHunters=(int)Math.max(config.baseline,config.alternative);initialConfig.validate();}
                Simulation initial=new Simulation(initialConfig,seed);byte[] common=WorldIO.encode(initial);List<Outcome> outcomes=new ArrayList<>();
                for(int condition=0;condition<2;condition++){
                    Simulation s=WorldIO.decode(common);apply(s,config.variable,condition==0?config.baseline:config.alternative);s.history.clear();s.sample();long births=s.totalBirths,deaths=s.totalDeaths;
                    Path dir=config.output==null?null:config.output.resolve("seed-"+seed+"-"+(condition==0?"A":"B"));
                    try(RunRecorder recorder=dir==null?null:new RunRecorder(dir,s,"Paired "+config.variable+" value="+(condition==0?config.baseline:config.alternative))){
                        for(int tick=0;tick<config.seconds*60;tick++){
                            if(tick%60==0&&(cancelled.getAsBoolean()||Thread.currentThread().isInterrupted())){if(recorder!=null){Thread.interrupted();recorder.sample(s);recorder.checkpoint(s);}throw new CancellationException();}
                            s.step();if(recorder!=null){if(s.elapsedTicks%(config.sampleSeconds*60L)==0)recorder.sample(s);if(s.elapsedTicks%(config.checkpointSeconds*60L)==0)recorder.checkpoint(s);}
                        }
                        if(recorder!=null){recorder.sample(s);recorder.checkpoint(s);}
                    }
                    outcomes.add(new Outcome(seed,condition==0?"A":"B",s.count(Simulation.Species.PREY),s.count(Simulation.Species.PREDATOR),s.eggs.size(),s.totalBirths-births,s.totalDeaths-deaths,s.meanLifespan(),s.efficiency(),s.generation,s.tracker.livingCounts(s).size(),s.aliveCount()==0&&s.eggs.isEmpty(),RunRecorder.row(s)));
                    progress.accept(done.incrementAndGet()*100/(config.runs*2));
                }return outcomes;
            }));}
            List<Outcome> result=new ArrayList<>();for(var future:futures)try{result.addAll(future.get());}catch(ExecutionException e){if(e.getCause() instanceof Exception ex)throw ex;throw e;}return new Report(config,result);
        }finally{pool.shutdownNow();pool.awaitTermination(10,TimeUnit.SECONDS);}
    }
    private static void apply(Simulation s,Variable variable,double value){switch(variable){case FOOD_ABUNDANCE->s.abundance=(int)value;case MUTATION_PERCENT->s.mutationRate=value/100;case SEASONS->s.seasons=value==1;case LOCAL_SOURCE_QUOTA->s.config.localSourceQuota=(int)value;case RESOURCE_LIFETIME->s.config.resourceLifetime=value;case INITIAL_HUNTERS,PREDATORS->{int desired=variable==Variable.PREDATORS?(value==0?0:s.config.initialHunters):(int)value;int[] count={0};s.creatures.removeIf(c->{if(c.species!=Simulation.Species.PREDATOR||++count[0]<=desired)return false;s.stats(c.species).founders--;return true;});s.predatorsEnabled=desired>0;s.config.initialHunters=desired;s.stats(Simulation.Species.PREDATOR).minimumAdults=s.count(Simulation.Species.PREDATOR);s.stats(Simulation.Species.PREDATOR).minimumViable=s.count(Simulation.Species.PREDATOR);}}}
    private ExperimentRunner(){}
}
