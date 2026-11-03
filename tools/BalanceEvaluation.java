import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** One-factor ecology sweeps and explicitly density-normalized size comparisons. */
public final class BalanceEvaluation {
    public static void main(String[] args)throws Exception{
        if(args.length<1||args.length>3)throw new IllegalArgumentException("BalanceEvaluation output-directory [seconds=600] [pairedSeeds=3]");Path folder=Path.of(args[0]);Files.createDirectories(folder);int seconds=args.length>1?Integer.parseInt(args[1]):600,runs=args.length>2?Integer.parseInt(args[2]):3;
        for(var variable:new ExperimentRunner.Variable[]{ExperimentRunner.Variable.INITIAL_HUNTERS,ExperimentRunner.Variable.LOCAL_SOURCE_QUOTA,ExperimentRunner.Variable.RESOURCE_LIFETIME}){
            WorldConfig world=WorldConfig.preset(variable==ExperimentRunner.Variable.INITIAL_HUNTERS?"Classic (unchanged default)":"Supported coexistence");double a=variable==ExperimentRunner.Variable.INITIAL_HUNTERS?10:0,b=variable==ExperimentRunner.Variable.INITIAL_HUNTERS?5:variable==ExperimentRunner.Variable.LOCAL_SOURCE_QUOTA?30:120;String name=variable.name().toLowerCase(Locale.ROOT);var config=new ExperimentRunner.Config(variable,a,b,runs,seconds,20001,world,3,null,30,300);var report=ExperimentRunner.run(config,()->false,p->{});Files.writeString(folder.resolve(name+".csv"),report.csv());world.write(folder.resolve(name+".settings"));System.out.println(name+": "+report.outcomes().stream().map(o->o.seed()+o.condition()+"="+(o.prey()+o.predators())+" adults").toList());
        }
        ExecutorService pool=Executors.newFixedThreadPool(3);try{List<Future<String>> tasks=new ArrayList<>();for(int run=0;run<runs;run++){long seed=30001+run;for(double scale:new double[]{1,1.5}){WorldConfig world=WorldConfig.preset("Steady observation").scaledDensity(scale);tasks.add(pool.submit(()->{long start=System.nanoTime();Simulation s=new Simulation(world,seed);for(int t=0;t<seconds*60;t++)s.step();return String.format(Locale.US,"%.1f,%d,%d,%d,%d,%d,%d,%d,%.3f,%s",scale,seed,world.width,world.height,world.initialPrey,world.populationLimit,world.foodLimit,Math.round((1+world.abundance/world.refillAbundanceStep)*world.refillMultiplier),(System.nanoTime()-start)/1e9,RunRecorder.row(s));}));}}StringBuilder csv=new StringBuilder("linear_scale,seed,width,height,initial_prey,population_limit,food_limit,refill_batch,wall_seconds,"+RunRecorder.HEADER+"\n");for(var task:tasks){String row=task.get();csv.append(row).append('\n');System.out.println("density: "+row);}Files.writeString(folder.resolve("density-size.csv"),csv);}
        finally{pool.shutdownNow();}
    }
}
