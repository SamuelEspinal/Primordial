import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Independent evolutionary islands. Workers never touch the live Swing world. */
public final class WorldBatch implements AutoCloseable {
    public record Status(int index,long seed,String state,double seconds,int prey,int hunters,int generation,String detail,Path checkpoint){}
    private final ExecutorService executor;
    private final List<Job> jobs=new ArrayList<>();
    private final WorldConfig settings;
    private final Map<Simulation.Species,List<Simulation.Genome>> pools;
    private final int duration,sampleSeconds,checkpointSeconds;
    private final Path folder;
    private final String provenance;
    private final class Job {
        final int index;final long seed;final AtomicBoolean stop=new AtomicBoolean();volatile Status status;volatile Future<?> future;
        Job(int i,long seed){index=i;this.seed=seed;status=new Status(i,seed,"Queued",0,0,0,1,"",null);}
        void status(Simulation s,String state,String detail,Path checkpoint){status=new Status(index,seed,state,s.seconds(),s.count(Simulation.Species.PREY),s.count(Simulation.Species.PREDATOR),s.generation,detail,checkpoint);}
    }
    public WorldBatch(WorldConfig settings,Map<Simulation.Species,List<Simulation.Genome>> pools,long firstSeed,int worlds,int workers,int duration,int sampleSeconds,int checkpointSeconds,Path folder,String provenance){
        settings.validate();if(worlds<1||worlds>32||workers<1||workers>8||duration<1||duration>86400||sampleSeconds<1||checkpointSeconds<1)throw new IllegalArgumentException("Use 1–32 worlds, 1–8 workers, and 1–86400 seconds.");
        Math.addExact(firstSeed,worlds-1);this.settings=settings.copy();EnumMap<Simulation.Species,List<Simulation.Genome>> copy=new EnumMap<>(Simulation.Species.class);pools.forEach((k,v)->copy.put(k,List.copyOf(v)));this.pools=Collections.unmodifiableMap(copy);this.duration=duration;this.sampleSeconds=sampleSeconds;this.checkpointSeconds=checkpointSeconds;this.folder=folder;this.provenance=provenance;
        executor=Executors.newFixedThreadPool(workers,r->{Thread t=new Thread(r,"primordial-world");t.setDaemon(true);return t;});
        for(int i=0;i<worlds;i++){Job j=new Job(i+1,Math.addExact(firstSeed,i));jobs.add(j);j.future=executor.submit(()->run(j,false));}
    }
    private void run(Job j,boolean resume){Simulation s=null;Path dir=folder.resolve("world-"+j.index),checkpoint=dir.resolve("latest.world");
        try{
            if(resume){s=WorldIO.read(checkpoint);if(s.awaitingSelection)s.evolve();}
            else{s=new Simulation(settings,j.seed);s.seedPools(pools);Files.createDirectories(dir);int id=0;for(var list:pools.values())for(var genome:list)BrainIO.write(dir.resolve("source-"+(++id)+".brain"),genome);}
            try(RunRecorder recorder=new RunRecorder(dir,s,provenance)){
                recorder.checkpoint(s);j.status(s,"Running","",checkpoint);long end=duration*60L;
                while(s.elapsedTicks<end&&!j.stop.get()){
                    s.step();
                    if(s.awaitingSelection){recorder.sample(s);recorder.checkpoint(s);j.status(s,s.elapsedTicks>=end?"Complete":"Inspect generation","Generation held before replacement",checkpoint);return;}
                    if(s.elapsedTicks%(sampleSeconds*60L)==0){recorder.sample(s);j.status(s,"Running","",checkpoint);}
                    if(s.elapsedTicks%(checkpointSeconds*60L)==0)recorder.checkpoint(s);
                    if(s.aliveCount()==0&&s.eggs.isEmpty()&&s.mode==Simulation.Mode.CONTINUOUS)break;
                    if(Thread.interrupted())j.stop.set(true);
                }
                recorder.sample(s);recorder.checkpoint(s);j.status(s,j.stop.get()?"Stopped":s.aliveCount()==0&&s.eggs.isEmpty()?"Extinct":"Complete","",checkpoint);
            }
        }catch(Exception ex){j.status=new Status(j.index,j.seed,"Failed",s==null?0:s.seconds(),0,0,s==null?1:s.generation,ex.toString(),Files.exists(checkpoint)?checkpoint:null);}
    }
    public List<Status> statuses(){return jobs.stream().map(j->j.status).toList();}
    public void stop(int index){jobs.get(index-1).stop.set(true);}
    public boolean busy(){return jobs.stream().anyMatch(j->j.future!=null&&!j.future.isDone());}
    public synchronized void resume(int index){Job j=jobs.get(index-1);if(j.future!=null&&!j.future.isDone())throw new IllegalStateException("World is still running");if(j.status.checkpoint==null||j.status.state.equals("Complete")||j.status.state.equals("Extinct"))throw new IllegalStateException("Select a stopped world or a generation awaiting inspection");j.stop.set(false);j.future=executor.submit(()->run(j,true));}
    public void stopAll(){jobs.forEach(j->j.stop.set(true));}
    @Override public void close(){stopAll();executor.shutdown();}
}
