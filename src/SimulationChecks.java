import javax.swing.SwingUtilities;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;

/** Dependency-free regression checks: java Main --self-test. Uses fixed seeds and temp files. */
public final class SimulationChecks {
    private SimulationChecks() {}
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    private static Simulation fresh(){return new Simulation(new Random(42));}
    private static void stable(Simulation s){
        check(s.aliveCount()+s.eggs.size()<=Simulation.MAX_POPULATION,"Population bounded");
        for(var c:s.creatures){
            check(Double.isFinite(c.energy)&&Double.isFinite(c.x)&&Double.isFinite(c.y),"Finite creature state");
            check(c.x>=c.size&&c.x<=Simulation.WIDTH-c.size&&c.y>=c.size&&c.y<=Simulation.HEIGHT-c.size,"World bounds");
            check(!s.blocked(c.x,c.y,c.size-1e-6),"Creature outside rocks");
            c.genome(); // validates all inherited traits and weights
        }
    }
    public static void run(){
        neuralMemory();inheritance();reproduction();predation();sensesAndRocks();seasons();longRuns();persistence();benchmark();ecology();eggsAndNests();editor();worldPersistence();experiments();geneticGroups();render();
        System.out.println("PASS: recurrent memory, body inheritance, elitism, births, energy conservation, predation,");
        System.out.println("sensing, rocks, seasons, extinction, long runs, ancestry bounds, persistence, paired trials, diets, eggs, nests, editor, exact world continuation, experiments, genetic groups, and Java2D rendering.");
    }
    static void neuralMemory(){
        Simulation.Creature c=fresh().creatures.get(0);c.weights=new double[Simulation.WEIGHT_COUNT];Arrays.fill(c.inputs,1);Simulation.think(c);
        check(Arrays.equals(c.outputs,new double[2]),"Zero-weight network");
        c.weights[1]=1;c.weights[Simulation.RECURRENT_OFFSET]=.8;c.weights[Simulation.OUTPUT_OFFSET+1]=1;
        Arrays.fill(c.inputs,0);c.inputs[0]=1;Simulation.think(c);double first=c.hidden[0];
        c.inputs[0]=0;Simulation.think(c);check(c.hidden[0]>0&&c.hidden[0]<first,"Memory persists without stimulus");
        Arrays.fill(c.hidden,0);Simulation.think(c);check(c.hidden[0]==0,"Fresh memory removes past stimulus");
    }
    static void inheritance(){
        Simulation s=fresh();s.mode=Simulation.Mode.GENERATIONS;s.mutationRate=1;
        var parent=s.creatures.get(0);parent.eaten=1000;var elite=parent.genome();int id=parent.id,root=parent.rootId;s.evolve();
        var child=s.creatures.get(0);check(Arrays.equals(elite.weights(),child.weights),"Elite exact weights");
        check(child.size==elite.size()&&child.speed==elite.speed()&&child.vision==elite.vision(),"Elite exact body");
        check(child.parentId==id&&child.rootId==root&&child.generation==2,"Inherited ancestry");
        check(s.creatures.stream().anyMatch(c->c.size!=elite.size()&&c.species==Simulation.Species.PREY),"Bodies mutate");
        s=fresh();s.mode=Simulation.Mode.GENERATIONS;s.mutationRate=0;
        var parents=s.creatures.stream().map(Simulation.Creature::genome).toList();s.evolve();
        for(var c:s.creatures)check(parents.stream().anyMatch(p->p.species()==c.species&&Arrays.equals(p.weights(),c.weights)&&p.size()==c.size&&p.speed()==c.speed&&p.vision()==c.vision),"Zero mutation copies complete genome");
    }
    static void reproduction(){
        Simulation s=fresh();s.eggsEnabled=false;s.creatures.removeIf(c->c.species==Simulation.Species.PREDATOR);s.food.clear();
        var parent=s.creatures.get(0);s.creatures.clear();s.creatures.add(parent);
        parent.age=500;parent.energy=parent.capacity()*.98;double before=parent.energy;int parentId=parent.id;long births=s.totalBirths;s.step();
        check(s.creatures.size()==2&&s.totalBirths==births+1,"Continuous birth");
        var child=s.creatures.get(1);check(child.parentId==parentId&&child.age==0&&parent.alive,"Child born without replacing parent");
        check(child.energy+parent.energy<before,"Reproduction conserves energy");
        check(s.ancestry.get(parentId).children==1,"Family tracks child");
        for(int i=0;i<30;i++)s.step();check(parent.children==1,"Birth cooldown");
        s.creatures.clear();int generation=s.generation;long total=s.totalBirths;for(int i=0;i<100;i++)s.step();
        check(s.aliveCount()==0&&s.totalBirths==total&&s.generation==generation,"No automatic extinction rescue");
    }
    static void predation(){
        Simulation s=fresh();var prey=s.creatures.get(0);var hunter=s.creatures.stream().filter(c->c.species==Simulation.Species.PREDATOR).findFirst().orElseThrow();
        s.creatures.clear();s.creatures.add(prey);s.creatures.add(hunter);s.food.clear();s.reproductionEnabled=false;
        prey.x=hunter.x=100;prey.y=hunter.y=100;prey.angle=hunter.angle=0;prey.weights=new double[Simulation.WEIGHT_COUNT];hunter.weights=new double[Simulation.WEIGHT_COUNT];hunter.energy=50;
        long deaths=s.totalDeaths;s.step();check(!prey.alive&&hunter.kills==1&&s.totalKills==1&&s.totalDeaths==deaths+1,"Predation kills prey once");
        check(hunter.energy>50&&prey.inputs[5]>0,"Predator gains energy; prey senses danger");
        s.step();check(s.totalKills==1,"A dead prey cannot be consumed twice");
        s.setPredators(false);check(s.count(Simulation.Species.PREDATOR)==0,"Predator toggle removes hunters");
        s.setPredators(true);check(s.count(Simulation.Species.PREDATOR)==10,"Predator toggle introduces founders explicitly");
    }
    static void sensesAndRocks(){
        Simulation s=fresh();var c=s.creatures.get(0);s.creatures.clear();s.creatures.add(c);s.food.clear();s.reproductionEnabled=false;
        c.weights=new double[Simulation.WEIGHT_COUNT];c.angle=0;c.x=400-65-c.size-.5;c.y=265;
        for(int i=0;i<30;i++)s.step();check(!s.blocked(c.x,c.y,c.size-1e-6)&&c.inputs[7]>.9,"Rock collision and directional sensor");
        c.x=100;c.y=100;c.vision=70;s.food.clear();s.food.add(new Simulation.Food(300,100,2));s.step();
        check(c.inputs[0]==0&&c.inputs[1]==0&&c.inputs[2]==0,"Finite eyesight");
        s.food.clear();s.food.add(new Simulation.Food(c.x+30,c.y,2));c.angle=0;s.step();check(c.inputs[0]>.99&&c.inputs[2]>0,"Visible target sensed");
    }
    static void seasons(){
        Simulation s=fresh();String first=s.season();var patch=s.foodPatch();double initial=s.seasonFactor();s.elapsedTicks=1800;
        check(!s.season().equals(first)&&!s.foodPatch().equals(patch),"Seasons move fertile patch");s.elapsedTicks=5400;check(s.seasonFactor()<initial,"Winter scarcity");
        s.seasons=false;var constant=s.foodPatch();s.elapsedTicks=0;check(s.foodPatch().equals(constant)&&s.seasonFactor()==1,"Constant environment option");
    }
    static void longRuns(){
        Simulation s=fresh();for(int i=0;i<Simulation.GENERATION_TICKS*8;i++){s.step();if(i%1000==0)stable(s);}stable(s);
        check(s.totalBirths>70,"Unassisted continuous reproduction occurs");
        System.out.printf("Continuous: %d births, %d deaths, deepest G%d, %d alive%n",s.totalBirths,s.totalDeaths,s.generation,s.aliveCount());
        s=fresh();s.mode=Simulation.Mode.GENERATIONS;
        for(int i=0;i<Simulation.GENERATION_TICKS*12;i++)s.step();check(s.generation>=13,"12 selection rounds");stable(s);
        for(int i=0;i<90;i++)s.evolve();check(s.ancestry.size()<=Simulation.MAX_ANCESTRY,"Bounded ancestry archive");
        check(s.history.stream().allMatch(h->Double.isFinite(h.efficiency())&&Double.isFinite(h.lifespan())),"Finite statistics");
        System.out.printf("Selection: G%d, archive %d records%n",s.generation,s.ancestry.size());
    }
    static void persistence(){
        Path path=null;
        try{
            path=Files.createTempFile("primordial-genome-",".brain");var genome=fresh().creatures.get(0).genome();BrainIO.write(path,genome);var restored=BrainIO.read(path);
            check(Arrays.equals(genome.weights(),restored.weights())&&genome.size()==restored.size()&&genome.species()==restored.species()&&genome.diet()==restored.diet(),"Body and memory weights persist");
            double[] copy=genome.weights();copy[0]=99;check(genome.weights()[0]!=99,"Immutable saved genome");
            Properties legacy=new Properties();legacy.setProperty("format","primordial-java-v1");legacy.setProperty("hue","0.4");for(int i=0;i<82;i++)legacy.setProperty("weight."+i,"0.25");
            try(var writer=Files.newBufferedWriter(path)){legacy.store(writer,"legacy");}var converted=BrainIO.read(path);
            check(converted.weights()[Simulation.OUTPUT_OFFSET]==.25&&converted.weights()[Simulation.RECURRENT_OFFSET]==0,"Legacy brain conversion");
            Files.writeString(path,"format=primordial-java-v2\nhue=NaN\n");boolean rejected=false;try{BrainIO.read(path);}catch(IllegalArgumentException ex){rejected=true;}check(rejected,"Malformed genome rejected");
        }catch(Exception ex){throw new AssertionError("Persistence",ex);}finally{if(path!=null)try{Files.deleteIfExists(path);}catch(Exception ignored){}}
    }
    static void benchmark(){
        var genome=fresh().creatures.get(0).genome();var result=Benchmark.compare(genome,genome,55,true);
        check(result.current().equals(result.founder()),"Identical paired genomes yield identical results");
        var hunter=fresh().creatures.stream().filter(c->c.species==Simulation.Species.PREDATOR).findFirst().orElseThrow().genome();
        var a=Benchmark.evaluate(hunter,55,true,1,10);var b=Benchmark.evaluate(hunter,55,true,1,10);check(a.equals(b),"Predator trials have identical opponents");
        System.out.println("Paired ancestor benchmark: deterministic for identical genomes.");
    }
    static void ecology(){
        Simulation s=fresh();s.creatures.clear();s.food.clear();s.seasons=false;
        var plant=new Simulation.Food(100,100,2);var plankton=new Simulation.Food(200,100,2,Simulation.Resource.PLANKTON,36,0);
        var carrion=new Simulation.Food(300,100,4,Simulation.Resource.CARRION,60,0);carrion.age=1800;
        s.food.add(plant);s.food.add(plankton);s.food.add(carrion);s.step();
        check(plant.x==100&&plankton.x>200&&!s.food.contains(carrion),"Stationary plants, moving plankton, carrion expiry");
        var c=fresh().creatures.get(0);c.diet=Simulation.Diet.GRAZER;
        check(Simulation.dietEfficiency(c,Simulation.Resource.PLANT)>Simulation.dietEfficiency(c,Simulation.Resource.CARRION),"Grazing specialization");
        c.diet=Simulation.Diet.FILTER_FEEDER;check(Simulation.dietEfficiency(c,Simulation.Resource.PLANKTON)>Simulation.dietEfficiency(c,Simulation.Resource.PLANT),"Plankton specialization");
        c.diet=Simulation.Diet.SCAVENGER;check(Simulation.dietEfficiency(c,Simulation.Resource.CARRION)>Simulation.dietEfficiency(c,Simulation.Resource.PLANKTON),"Scavenging specialization");
        s=fresh();var dying=s.creatures.get(0);s.creatures.clear();s.creatures.add(dying);s.food.clear();dying.energy=-10;s.step();
        check(s.food.stream().anyMatch(f->f.type==Simulation.Resource.CARRION),"Deaths feed scavengers");
    }
    static Simulation worldWithEgg(){
        Simulation s=fresh();s.food.clear();s.nests.clear();var parent=s.creatures.get(0);s.creatures.clear();s.creatures.add(parent);
        parent.x=100;parent.y=100;parent.age=500;parent.energy=parent.capacity()*.98;parent.weights=new double[Simulation.WEIGHT_COUNT];
        long births=s.totalBirths;double energy=parent.energy;s.step();
        check(s.eggs.size()==1&&s.creatures.size()==1&&s.totalBirths==births,"An egg waits before becoming a birth");
        check(parent.energy+s.eggs.get(0).embryo.energy<energy,"Egg energy comes from parent");
        s.reproductionEnabled=false;return s;
    }
    static void eggsAndNests(){
        Simulation s=worldWithEgg();int id=s.eggs.get(0).embryo.id,parent=s.eggs.get(0).embryo.parentId;
        for(int i=0;i<Simulation.HATCH_TICKS-1;i++)s.step();check(s.eggs.size()==1,"Incubation delay");s.step();
        check(s.eggs.isEmpty()&&s.creatures.stream().anyMatch(c->c.id==id)&&s.ancestry.get(parent).children==1,"Hatching registers family");
        s=worldWithEgg();var egg=s.eggs.get(0);s.nests.add(new Simulation.Nest(egg.embryo.x,egg.embryo.y,50));
        for(int i=0;i<Simulation.HATCH_TICKS/2;i++)s.step();check(s.eggs.isEmpty(),"Nest accelerates hatching");
        s=worldWithEgg();egg=s.eggs.get(0);var hunter=fresh().creatures.stream().filter(c->c.species==Simulation.Species.PREDATOR).findFirst().orElseThrow();
        s.creatures.clear();s.creatures.add(hunter);hunter.x=egg.embryo.x;hunter.y=egg.embryo.y;hunter.weights=new double[Simulation.WEIGHT_COUNT];hunter.energy=50;
        s.nests.add(new Simulation.Nest(hunter.x,hunter.y,50));s.step();check(s.eggs.size()==1,"Nest shields egg from predator");
        s.nests.clear();s.step();check(s.eggs.isEmpty()&&s.totalEggLoss==1,"Exposed egg is vulnerable");
    }
    static void editor(){
        Simulation s=fresh();var c=s.creatures.get(0);check(!s.addRock(c.x,c.y,25),"Editor cannot crush living creatures");
        s.creatures.clear();s.food.clear();s.obstacles.clear();s.nests.clear();
        check(s.addRock(400,400,25)&&s.blocked(400,400,1),"Rock drawing");s.eraseHabitat(400,400,5);check(!s.blocked(400,400,1),"Carving channel");
        check(s.addNest(300,300)&&s.inNest(300,300),"Nest placement");
        check(s.addSource(700,300,Simulation.Resource.PLANKTON),"Food source placement");
        for(int i=0;i<30;i++)s.step();check(s.food.stream().anyMatch(f->f.type==Simulation.Resource.PLANKTON&&Math.hypot(f.x-700,f.y-300)<45),"Source generates local food");
        s.eraseHabitat(700,300,5);check(s.sources.isEmpty(),"Source erasure");
    }
    static void worldPersistence(){
        try{
            Simulation s=worldWithEgg();s.addSource(700,300,Simulation.Resource.PLANKTON);s.mutationRate=.127;s.abundance=31;s.seasons=false;
            s.creatures.get(0).hidden[0]=.75;s.food.add(new Simulation.Food(200,100,2,Simulation.Resource.PLANKTON,36,.7));
            byte[] snapshot=WorldIO.encode(s);Simulation copy=WorldIO.decode(snapshot);
            check(copy.eggs.size()==1&&copy.sources.size()==1&&copy.creatures.get(0).hidden[0]==.75&&copy.mutationRate==.127,"Complete world restored");
            check(Arrays.equals(snapshot,WorldIO.encode(copy)),"Snapshot round trip is exact");
            for(int i=0;i<900;i++){s.step();copy.step();}
            check(Arrays.equals(WorldIO.encode(s),WorldIO.encode(copy)),"Loaded world continues identically, including RNG and hatching");
            Path path=Files.createTempFile("primordial-world-",".world");try{WorldIO.write(path,s);check(Arrays.equals(WorldIO.encode(s),WorldIO.encode(WorldIO.read(path))),"Atomic file snapshot");}finally{Files.delete(path);}
            boolean rejected=false;try{WorldIO.decode(new byte[]{1,2,3,4});}catch(java.io.IOException ex){rejected=true;}check(rejected,"Malformed world rejected");
            s.creatures.get(0).energy=Double.NaN;rejected=false;try{WorldIO.decode(WorldIO.encode(s));}catch(java.io.IOException ex){rejected=true;}check(rejected,"Non-finite world state rejected");
            Simulation epochs=fresh();epochs.mode=Simulation.Mode.GENERATIONS;for(int i=0;i<100;i++)epochs.step();epochs.evolve();
            Simulation epochCopy=WorldIO.decode(WorldIO.encode(epochs));for(int i=0;i<200;i++){epochs.step();epochCopy.step();}
            check(Arrays.equals(WorldIO.encode(epochs),WorldIO.encode(epochCopy)),"Selection-mode state also resumes exactly");
        }catch(Exception ex){throw new AssertionError("World snapshots",ex);}
        System.out.println("World snapshots: byte-identical continuation after 900 ticks, including eggs and random state.");
    }
    static void experiments(){
        try{
            var config=new ExperimentRunner.Config(ExperimentRunner.Variable.FOOD_ABUNDANCE,55,55,2,3,42);
            var report=ExperimentRunner.run(config,()->false,p->{});check(report.outcomes().size()==4,"Paired runs complete");
            for(int i=0;i<2;i++){var a=report.outcomes().get(i*2);var b=report.outcomes().get(i*2+1);check(a.prey()==b.prey()&&a.predators()==b.predators()&&a.eggs()==b.eggs()&&a.efficiency()==b.efficiency()&&a.births()==b.births(),"Identical experiment conditions match");}
            check(report.csv().contains("genetic_groups")&&report.summary().contains("Standard error"),"Experiment reporting");
            boolean cancelled=false;try{ExperimentRunner.run(config,()->true,p->{});}catch(java.util.concurrent.CancellationException ex){cancelled=true;}check(cancelled,"Experiment cancellation");
        }catch(Exception ex){throw new AssertionError("Experiments",ex);}
    }
    static void geneticGroups(){
        Simulation s=fresh();var c=s.creatures.get(0);SpeciesTracker tracker=new SpeciesTracker();int first=tracker.assign(c,0);c.clusterId=first;
        check(tracker.assign(c,1)==first,"Same genome retains group");
        double[] original=c.weights;c.weights=Arrays.stream(c.weights).map(w->w>0?-5:5).toArray();int divergent=tracker.assign(c,2);
        check(divergent!=first&&tracker.groups().get(divergent-1).parentGroup()==first,"Genetic divergence creates a child group");c.weights=original;
    }
    static void render(){
        try{SwingUtilities.invokeAndWait(()->{Simulation s=fresh();for(int i=0;i<60;i++)s.step();EcosystemView view=new EcosystemView(s);view.setSize(820,540);view.selected=s.creatures.get(0);view.showSenses=true;view.trails=true;
            BufferedImage image=new BufferedImage(820,540,BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();view.paint(g);g.dispose();check(image.getRGB(400,250)!=0,"Habitat rendered");});}
        catch(Exception ex){throw new AssertionError("Java2D rendering",ex);}
    }
}
