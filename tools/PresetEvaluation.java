import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** Held-out evaluation of real new-world presets, with no rescue or live interventions. */
public final class PresetEvaluation {
    private record Result(String preset,long seed,String row){}
    public static void main(String[] args)throws Exception{
        if(args.length<1||args.length>5)throw new IllegalArgumentException("PresetEvaluation output.csv [seconds=1800] [seeds=5] [firstSeed=10001] [workers=3]");
        int seconds=args.length>1?Integer.parseInt(args[1]):1800,runs=args.length>2?Integer.parseInt(args[2]):5,workers=args.length>4?Integer.parseInt(args[4]):3;long first=args.length>3?Long.parseLong(args[3]):10001;
        if(seconds<1||seconds>86400||runs<1||runs>100||workers<1||workers>8)throw new IllegalArgumentException("Invalid evaluation bounds");Math.addExact(first,runs-1);Path output=Path.of(args[0]).toAbsolutePath();Files.createDirectories(output.getParent());ExecutorService pool=Executors.newFixedThreadPool(workers);List<Future<Result>> tasks=new ArrayList<>();
        try{for(String preset:new String[]{"Continuous ecology","Steady observation","Supported coexistence"})for(int i=0;i<runs;i++){long seed=first+i;tasks.add(pool.submit(()->{Simulation s=new Simulation(WorldConfig.preset(preset),seed);for(int tick=0;tick<seconds*60;tick++){s.step();if(s.aliveCount()==0&&s.eggs.isEmpty())break;}return new Result(preset,seed,RunRecorder.row(s));}));}
            try(PrintWriter csv=new PrintWriter(Files.newBufferedWriter(output))){csv.println("preset,seed,requested_seconds,"+RunRecorder.HEADER);for(var task:tasks){Result r=task.get();csv.println(r.preset+","+r.seed+","+seconds+","+r.row);csv.flush();if(csv.checkError())throw new IOException("CSV write failed");System.out.println(r.preset+" seed="+r.seed+": "+r.row);}}
        }finally{pool.shutdownNow();}
    }
}
