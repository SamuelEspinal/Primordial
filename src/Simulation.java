import java.util.*;

/** Fixed 60 Hz ecosystem. Neural decisions, inherited bodies, and resource-limited reproduction. */
public final class Simulation implements java.io.Serializable {
    private static final long serialVersionUID=3L;
    public static final int WIDTH=1200, HEIGHT=850, POPULATION=60, GENERATION_TICKS=2700;
    public static final int INPUTS=12, HIDDEN=8, OUTPUTS=2;
    public static final int RECURRENT_OFFSET=(INPUTS+1)*HIDDEN;
    public static final int OUTPUT_OFFSET=RECURRENT_OFFSET+HIDDEN*HIDDEN;
    public static final int WEIGHT_COUNT=OUTPUT_OFFSET+(HIDDEN+1)*OUTPUTS;
    public static final int MAX_POPULATION=180, MAX_ANCESTRY=6000;
    public enum Species { PREY, PREDATOR }
    public enum Mode { CONTINUOUS, GENERATIONS }
    public enum Diet { GRAZER, FILTER_FEEDER, SCAVENGER }
    public enum Resource { PLANT, PLANKTON, CARRION }
    public static final int HATCH_TICKS=360;
    public final List<Egg> eggs=new ArrayList<>();
    public final List<Nest> nests=new ArrayList<>();
    public final List<FoodSource> sources=new ArrayList<>();
    public final SpeciesTracker tracker=new SpeciesTracker();
    public boolean eggsEnabled=true;
    public long totalEggsLaid,totalEggLoss;
    public Mode mode=Mode.CONTINUOUS;
    public boolean seasons=true, predatorsEnabled=true, reproductionEnabled=true;
    public int abundance=55, generation, tick, nextId;
    public long elapsedTicks, totalBirths, totalDeaths, totalMeals, totalKills;
    public double mutationRate=.12, totalEnergySpent, totalEnergyGained, deadLifetime;
    public final List<Creature> creatures=new ArrayList<>();
    public final List<Food> food=new ArrayList<>();
    public final List<Obstacle> obstacles=new ArrayList<>();
    public final List<Sample> history=new ArrayList<>();
    /** Bounded history; old branches may be pruned after 6,000 births. */
    public final LinkedHashMap<Integer,Ancestor> ancestry=new LinkedHashMap<>();
    public final Map<Integer,Genome> founders=new HashMap<>();
    public Map<String,long[]> lineageBirths=new HashMap<>();
    public long lineageBirths(int root){long[] value=lineageBirths.get("root-"+root);return value==null?0:value[0];}
    private final EnumMap<Species,Genome> champions=new EnumMap<>(Species.class);
    private final List<Creature> epochArchive=new ArrayList<>();
    private final Random random;
    public WorldConfig config;
    public long seed;
    public boolean seedKnown, statisticsSinceMigration, awaitingSelection;
    transient boolean migratedOnRead;
    public int selectionRound=1;
    public RoleStats[] roles={new RoleStats(),new RoleStats()};
    public List<Creature> completedLeaders=new ArrayList<>();
    private transient SpatialGrid<Food> foodGrid;
    private transient SpatialGrid<Creature> creatureGrid;
    public static final class RoleStats implements java.io.Serializable {
        private static final long serialVersionUID=1L;
        public long founders,births,hatches,deaths,removals,replacements,eggLosses;
        public double gained,spent,lifetime,extinctAt=-1;
        public int minimumAdults=Integer.MAX_VALUE,minimumViable=Integer.MAX_VALUE;
        public double efficiency(){return spent==0?0:gained/spent;}
        public double lifespan(){return deaths==0?0:lifetime/deaths;}
    }
    public RoleStats stats(Species species){return roles[species.ordinal()];}
    public Creature bestCandidate(Species role){Set<Creature> all=new LinkedHashSet<>(epochArchive);all.addAll(creatures);return all.stream().filter(c->c.species==role).max(Comparator.comparingDouble(Creature::fitness)).orElse(null);}
    public Creature findCreature(int id){for(Creature c:creatures)if(c.id==id)return c;for(Creature c:epochArchive)if(c.id==id)return c;for(Creature c:completedLeaders)if(c.id==id)return c;return null;}
    public int width(){return config.width;}
    public int height(){return config.height;}
    public WorldConfig settings(){WorldConfig c=config.copy();c.abundance=abundance;c.mutation=mutationRate;c.hunters=predatorsEnabled;c.seasons=seasons;c.mode=mode;c.eggs=eggsEnabled;return c;}
    public String creatureName(int id){Ancestor a=ancestry.get(id);return a==null?CreatureNames.name(seed,id):a.name;}
    public List<Genome> topGenomes(Species species){Set<Creature> all=new LinkedHashSet<>(epochArchive);all.addAll(creatures);return all.stream().filter(c->c.species==species).sorted(Comparator.comparingDouble(Creature::fitness).reversed()).limit(config.parentPool).map(Creature::genome).toList();}
    public void seedPools(Map<Species,List<Genome>> pools){
        if(elapsedTicks!=0||!eggs.isEmpty())throw new IllegalStateException("Parent pools can only seed a fresh world");
        int[] indices=new int[2];for(Creature c:creatures){List<Genome> pool=pools.getOrDefault(c.species,List.of());if(!pool.isEmpty()){Genome g=pool.get(indices[c.species.ordinal()]++%pool.size());c.weights=g.weights();c.size=clamp(g.size(),config.minimumSize,config.maximumSize);c.speed=clamp(g.speed(),config.minimumSpeed,config.maximumSpeed);c.vision=clamp(g.vision(),config.minimumVision,config.maximumVision);c.diet=g.diet();c.hue=g.hue();c.energy=c.capacity()*config.founderEnergy;placeOutsideRocks(c);}}
        ancestry.clear();founders.clear();lineageBirths.clear();champions.clear();epochArchive.clear();tracker.clear();totalBirths=0;roles=new RoleStats[]{new RoleStats(),new RoleStats()};for(Creature c:creatures){register(c);champions.putIfAbsent(c.species,c.genome());}history.clear();sample();updateMetrics();
    }
    private void updateMetrics(){for(Species role:Species.values()){RoleStats r=stats(role);int adults=count(role),viable=adults+eggCount(role);r.minimumAdults=Math.min(r.minimumAdults,adults);r.minimumViable=Math.min(r.minimumViable,viable);if(r.founders+r.births>0&&viable==0&&r.extinctAt<0)r.extinctAt=seconds();}}
    private void readObject(java.io.ObjectInputStream in)throws java.io.IOException,ClassNotFoundException{
        in.defaultReadObject();boolean legacy=config==null;migratedOnRead=legacy;if(legacy){config=new WorldConfig();statisticsSinceMigration=true;roles=new RoleStats[]{new RoleStats(),new RoleStats()};selectionRound=1;}config.validate();
        if(completedLeaders==null)completedLeaders=new ArrayList<>();if(lineageBirths==null){lineageBirths=new HashMap<>();for(Ancestor a:ancestry.values())if(a.parentId!=0)lineageBirths.computeIfAbsent("root-"+a.rootId,k->new long[1])[0]++;}
        if(roles==null||roles.length!=2)throw new java.io.IOException("Invalid role statistics");
        for(Creature c:creatures)restoreCreature(c);for(Egg e:eggs)restoreCreature(e.embryo);for(Creature c:epochArchive)restoreCreature(c);
        for(Ancestor a:ancestry.values()){if(a.name==null)a.name=CreatureNames.name(seed,a.id);if(a.clan==null)a.clan=CreatureNames.clan(seed,a.rootId);}
        if(legacy){for(Species role:Species.values())stats(role).founders=count(role);updateMetrics();}
    }
    private void restoreCreature(Creature c){c.settings=config;if(c.name==null)c.name=CreatureNames.name(seed,c.id);if(c.clan==null)c.clan=CreatureNames.clan(seed,c.rootId);}


    public static final class Food implements java.io.Serializable {
        private static final long serialVersionUID=1L;
        public double x,y,angle,energy;
        public final double radius;
        public final Resource type;
        public int age;
        public Food(double x,double y,double radius){this(x,y,radius,Resource.PLANT,48,0);}
        public Food(double x,double y,double radius,Resource type,double energy,double angle){this.x=x;this.y=y;this.radius=radius;this.type=type;this.energy=energy;this.angle=angle;}
        public double x(){return x;}public double y(){return y;}public double radius(){return radius;}
    }
    public static final class Egg implements java.io.Serializable {
        private static final long serialVersionUID=1L;
        public final Creature embryo;
        public int incubation;
        public final double laidAt;
        Egg(Creature embryo,double laidAt){this.embryo=embryo;this.laidAt=laidAt;}
    }
    public record Nest(double x,double y,double radius) implements java.io.Serializable {}
    public record FoodSource(double x,double y,double radius,Resource type) implements java.io.Serializable {}
    public record Point(double x,double y) implements java.io.Serializable {}
    public record Obstacle(double x,double y,double radius) implements java.io.Serializable {}
    public record Sample(double seconds,int prey,int predators,double lifespan,double efficiency,
                         double size,double speed,double vision,long births,long deaths,double preyLifespan,double hunterLifespan,double preyEfficiency,double hunterEfficiency,boolean roleMetricsAvailable) implements java.io.Serializable {
        public Sample(double seconds,int prey,int predators,double lifespan,double efficiency,double size,double speed,double vision,long births,long deaths){this(seconds,prey,predators,lifespan,efficiency,size,speed,vision,births,deaths,0,0,0,0,false);}
        public Sample(double seconds,int prey,int predators,double lifespan,double efficiency,double size,double speed,double vision,long births,long deaths,double preyLifespan,double hunterLifespan,double preyEfficiency,double hunterEfficiency){this(seconds,prey,predators,lifespan,efficiency,size,speed,vision,births,deaths,preyLifespan,hunterLifespan,preyEfficiency,hunterEfficiency,true);}
    }
    public static final class Ancestor implements java.io.Serializable {
        private static final long serialVersionUID=1L;
        public final int id,parentId,rootId,generation;
        public final Species species;
        public final double bornAt;
        public String name,clan;
        public int children,meals;
        public double diedAt=-1;
        Ancestor(Creature c,double now){id=c.id;parentId=c.parentId;rootId=c.rootId;generation=c.generation;species=c.species;bornAt=now;name=c.name;clan=c.clan;}
    }
    public record Genome(double[] weights,float hue,double size,double speed,double vision,Species species,Diet diet) implements java.io.Serializable {
        public Genome {
            if(weights==null||weights.length!=WEIGHT_COUNT||!Float.isFinite(hue)||hue<0||hue>1||species==null||diet==null)
                throw new IllegalArgumentException("Invalid brain or species");
            for(double w:weights)if(!Double.isFinite(w)||Math.abs(w)>10)throw new IllegalArgumentException("Invalid neural weight");
            if(!Double.isFinite(size)||size<5||size>12||!Double.isFinite(speed)||speed<1.2||speed>3.8||!Double.isFinite(vision)||vision<70||vision>360)
                throw new IllegalArgumentException("Body traits are outside supported limits");
            weights=weights.clone();
        }
        public Genome(double[] weights,float hue){this(weights,hue,8,2.5,220,Species.PREY,Diet.GRAZER);}
        public Genome(double[] weights,float hue,double size,double speed,double vision,Species species){this(weights,hue,size,speed,vision,species,Diet.GRAZER);}
        @Override public double[] weights(){return weights.clone();}
        public double weightAt(int i){return weights[i];}
    }
    public static final class Creature implements java.io.Serializable {
        private static final long serialVersionUID=1L;
        public int id,parentId,rootId,clusterId,generation=1,age,eaten,kills,children,eggsLaid,cooldown,huntCooldown;
        public double x,y,angle,energy,distance,spent,gained,size,speed,vision,bornAt;
        public float hue;
        public String name,clan;
        public WorldConfig settings;
        public Species species;
        public Diet diet=Diet.GRAZER;
        public boolean alive=true;
        public double[] weights,inputs=new double[INPUTS],hidden=new double[HIDDEN],outputs=new double[OUTPUTS];
        public final List<Point> trail=new ArrayList<>();
        public Genome genome(){return new Genome(weights,hue,size,speed,vision,species,diet);}
        public double capacity(){return settings==null?100+size*9:settings.capacityBase+size*settings.capacityPerSize;}
        public double fitness(){return settings==null?eaten*100+children*30+age*.015+distance*.002:eaten*settings.fitnessMeal+children*settings.fitnessChild+age*settings.fitnessAge+distance*settings.fitnessDistance;}
    }
    public Simulation(){this(new WorldConfig(),new Random().nextLong());}
    public Simulation(Random random){this(new WorldConfig(),random,0,false);}
    public Simulation(WorldConfig config,long seed){this(config,new Random(seed),seed,true);}
    private Simulation(WorldConfig settings,Random random,long seed,boolean known){settings.validate();this.config=settings.copy();this.random=Objects.requireNonNull(random);this.seed=seed;this.seedKnown=known;abundance=config.abundance;mutationRate=config.mutation;seasons=config.seasons;predatorsEnabled=config.hunters;mode=config.mode;eggsEnabled=config.eggs;reset(null);}
    double between(double a,double b){return a+random.nextDouble()*(b-a);}
    public static double clamp(double v,double a,double b){return Math.max(a,Math.min(b,v));}
    private Genome randomGenome(Species species){
        double[] w=new double[WEIGHT_COUNT];
        for(int i=0;i<w.length;i++)w[i]=between(-config.initialWeightRange,config.initialWeightRange);
        // Recurrent links begin small to avoid saturating every hidden neuron.
        for(int i=RECURRENT_OFFSET;i<OUTPUT_OFFSET;i++)w[i]*=config.initialRecurrence;
        return new Genome(w,species==Species.PREDATOR?(float)between(.01,.09):(float)between(.22,.65),
                between(config.founderMinimumSize,config.founderMaximumSize),between(config.founderMinimumSpeed,config.founderMaximumSpeed),between(config.founderMinimumVision,config.founderMaximumVision),species,Diet.values()[random.nextInt(3)]);
    }
    private Genome mutate(Genome parent){
        double[] w=parent.weights();
        for(int i=0;i<w.length;i++)if(random.nextDouble()<mutationRate)w[i]=clamp(w[i]+between(-config.weightMutation,config.weightMutation),-5,5);
        double size=parent.size,speed=parent.speed,vision=parent.vision;
        if(random.nextDouble()<mutationRate)size=clamp(size+between(-config.sizeMutation,config.sizeMutation),config.minimumSize,config.maximumSize);
        if(random.nextDouble()<mutationRate)speed=clamp(speed+between(-config.speedMutation,config.speedMutation),config.minimumSpeed,config.maximumSpeed);
        if(random.nextDouble()<mutationRate)vision=clamp(vision+between(-config.visionMutation,config.visionMutation),config.minimumVision,config.maximumVision);
        Diet diet=random.nextDouble()<mutationRate*config.dietMutation?Diet.values()[random.nextInt(3)]:parent.diet;
        return new Genome(w,parent.hue,size,speed,vision,parent.species,diet);
    }
    private Creature spawn(Genome genome,Creature parent,boolean mutate){
        if(mutate)genome=mutate(genome);
        Creature c=new Creature();c.settings=config;c.id=++nextId;c.name=CreatureNames.name(seed,c.id);c.weights=genome.weights();c.hue=genome.hue;
        c.size=clamp(genome.size,config.minimumSize,config.maximumSize);c.speed=clamp(genome.speed,config.minimumSpeed,config.maximumSpeed);c.vision=clamp(genome.vision,config.minimumVision,config.maximumVision);c.species=genome.species;c.diet=genome.diet;
        c.x=between(30,width()-30);c.y=between(30,height()-30);c.angle=between(-Math.PI,Math.PI);
        c.energy=c.capacity()*config.founderEnergy;c.bornAt=seconds();
        if(parent!=null){c.parentId=parent.id;c.rootId=parent.rootId;c.clusterId=parent.clusterId;c.generation=parent.generation+1;}
        else c.rootId=c.id;
        c.clan=parent==null?CreatureNames.clan(seed,c.rootId):parent.clan;
        placeOutsideRocks(c);
        return c;
    }
    private void register(Creature c){
        c.clusterId=tracker.assign(c,seconds());
        ancestry.put(c.id,new Ancestor(c,seconds()));
        if(c.parentId==0)founders.put(c.rootId,c.genome());
        else {Ancestor parent=ancestry.get(c.parentId);if(parent!=null)parent.children++;}
        while(ancestry.size()>MAX_ANCESTRY)ancestry.remove(ancestry.keySet().iterator().next());
        totalBirths++;if(c.parentId==0)stats(c.species).founders++;else{stats(c.species).births++;lineageBirths.computeIfAbsent("root-"+c.rootId,k->new long[1])[0]++;}
    }
    public void reset(Genome seed){
        generation=selectionRound=1;awaitingSelection=false;completedLeaders.clear();roles=new RoleStats[]{new RoleStats(),new RoleStats()};tick=nextId=0;elapsedTicks=totalBirths=totalDeaths=totalMeals=totalKills=0;
        totalEnergySpent=totalEnergyGained=deadLifetime=0;
        history.clear();ancestry.clear();founders.clear();lineageBirths.clear();champions.clear();epochArchive.clear();creatures.clear();food.clear();obstacles.clear();eggs.clear();nests.clear();sources.clear();tracker.clear();
        totalEggsLaid=totalEggLoss=0;
        double[][] rocks={{400,265,65},{795,560,80},{580,640,42},{940,230,45}};
        for(int i=0;i<config.initialRocks;i++){double[] o=rocks[i%4];obstacles.add(new Obstacle(i<4?o[0]*width()/WIDTH:between(40,width()-40),i<4?o[1]*height()/HEIGHT:between(40,height()-40),Math.min(100,o[2]*config.rockScale)));}
        for(int i=0;i<config.initialNests;i++)nests.add(new Nest(i==0?210.0*width()/WIDTH:i==1?1040.0*width()/WIDTH:between(50,width()-50),i==0?620.0*height()/HEIGHT:i==1?700.0*height()/HEIGHT:between(50,height()-50),65));
        for(int i=0;i<config.initialSourceSites;i++){double x=config.initialSourceSites==8?new double[]{160,400,700,1040}[i%4]*width()/WIDTH:(.1+(i%5)*.2)*width(),y=config.initialSourceSites==8?(i<4?150.0:720.0)*height()/HEIGHT:(.15+(i/5)*.17)*height();if(!blocked(x,y,4)){addSource(x,y,Resource.PLANT);addSource(x,y,Resource.PLANKTON);}}
        for(Species species:Species.values()){
            if(species==Species.PREDATOR&&!predatorsEnabled)continue;
            int count=species==Species.PREY?config.initialPrey:config.initialHunters;
            for(int i=0;i<count;i++){
                Genome g=seed!=null&&seed.species==species?seed:randomGenome(species);
                Creature c=spawn(g,null,seed!=null&&seed.species==species&&i>0);
                creatures.add(c);register(c);champions.putIfAbsent(species,c.genome());
            }
        }
        fillFood();sample();updateMetrics();
    }
    public double seconds(){return elapsedTicks/60.0;}
    public String season(){return !seasons?"Constant summer":new String[]{"Spring","Summer","Autumn","Winter"}[seasonIndex()];}
    private int seasonIndex(){return (int)(elapsedTicks/WorldConfig.ticks(config.seasonSeconds)%4);}
    public double seasonFactor(){return !seasons?config.summerFactor:new double[]{config.springFactor,config.summerFactor,config.autumnFactor,config.winterFactor}[seasonIndex()];}
    public Point foodPatch(){
        int i=seasons?seasonIndex():1;
        return new Point(new double[]{260,930,850,300}[i]*width()/WIDTH,new double[]{230,230,650,640}[i]*height()/HEIGHT);
    }
    private int foodTarget(){return Math.min(config.foodLimit,(int)((config.foodBase+abundance*config.foodPerAbundance)*seasonFactor()));}
    private void addRandomFood(){
        Point patch=foodPatch();
        for(int tries=0;tries<30;tries++){
            double x,y;
            if(random.nextDouble()<config.patchFraction){x=clamp(patch.x+random.nextGaussian()*config.patchSpreadX*(width()/(double)WIDTH),18,width()-18);y=clamp(patch.y+random.nextGaussian()*config.patchSpreadY*(height()/(double)HEIGHT),18,height()-18);}
            else{x=between(18,width()-18);y=between(18,height()-18);}
            if(!blocked(x,y,4)){Resource type=random.nextDouble()<config.planktonFraction?Resource.PLANKTON:Resource.PLANT;food.add(new Food(x,y,between(1.6,3.3),type,type==Resource.PLANT?config.plantEnergy:config.planktonEnergy,between(-Math.PI,Math.PI)));return;}
        }
    }
    private void fillFood(){for(int i=0;i<foodTarget();i++)addRandomFood();}
    public boolean blocked(double x,double y,double radius){
        for(Obstacle o:obstacles)if(Math.hypot(x-o.x,y-o.y)<radius+o.radius)return true;return false;
    }
    private void placeOutsideRocks(Creature c){
        for(int i=0;i<100&&blocked(c.x,c.y,c.size);i++){c.x=between(30,width()-30);c.y=between(30,height()-30);}
        if(blocked(c.x,c.y,c.size)){c.x=25;c.y=25;if(blocked(c.x,c.y,c.size)){for(double y=c.size;y<=height()-c.size;y+=c.size*2+1)for(double x=c.size;x<=width()-c.size;x+=c.size*2+1)if(!blocked(x,y,c.size)){c.x=x;c.y=y;return;}throw new IllegalArgumentException("The configured rocks leave no clear space for a creature. Reduce rocks or their radius.");}}
    }
    public void paintFood(double x,double y){paintFood(x,y,Resource.PLANT);}
    public void paintFood(double x,double y,Resource type){
        for(int i=0;i<7&&food.size()<config.foodLimit;i++){
            double fx=clamp(x+between(-24,24),10,width()-10),fy=clamp(y+between(-24,24),10,height()-10);
            if(!blocked(fx,fy,4))food.add(new Food(fx,fy,between(1.6,3.3),type,type==Resource.CARRION?config.paintedCarrionEnergy:type==Resource.PLANKTON?config.planktonEnergy:config.plantEnergy,between(-Math.PI,Math.PI)));
        }
    }
    public int count(Species species){return (int)creatures.stream().filter(c->c.alive&&c.species==species).count();}
    public int aliveCount(){return count(Species.PREY)+count(Species.PREDATOR);}
    public Creature best(Species species){return creatures.stream().filter(c->c.alive&&c.species==species).max(Comparator.comparingDouble(Creature::fitness)).orElse(null);}
    public double efficiency(){return totalEnergySpent==0?0:totalEnergyGained/totalEnergySpent;}
    public double meanLifespan(){return totalDeaths==0?0:deadLifetime/totalDeaths;}
    public double meanTrait(int trait){return creatures.stream().filter(c->c.alive).mapToDouble(c->trait==0?c.size:trait==1?c.speed:c.vision).average().orElse(0);}
    void sample(){
        history.add(new Sample(seconds(),count(Species.PREY),count(Species.PREDATOR),meanLifespan(),efficiency(),meanTrait(0),meanTrait(1),meanTrait(2),totalBirths,totalDeaths,stats(Species.PREY).lifespan(),stats(Species.PREDATOR).lifespan(),stats(Species.PREY).efficiency(),stats(Species.PREDATOR).efficiency()));
        if(history.size()>240)history.remove(0);
    }
    /** Hidden state is read from the previous tick before any recurrent neuron is updated. */
    public static void think(Creature c){
        double[] previous=c.hidden.clone();int k=0;
        for(int h=0;h<HIDDEN;h++){
            double sum=c.weights[k++];for(int i=0;i<INPUTS;i++)sum+=c.inputs[i]*c.weights[k++];
            for(int j=0;j<HIDDEN;j++)sum+=previous[j]*c.weights[RECURRENT_OFFSET+h*HIDDEN+j];
            c.hidden[h]=Math.tanh(sum);
        }
        k=OUTPUT_OFFSET;
        for(int o=0;o<OUTPUTS;o++){double sum=c.weights[k++];for(int h=0;h<HIDDEN;h++)sum+=c.hidden[h]*c.weights[k++];c.outputs[o]=Math.tanh(sum);}
    }
    private double ray(Creature c,double angle){
        double range=config.obstacleSense,dx=Math.cos(angle),dy=Math.sin(angle);
        double distance=range;
        if(dx>1e-8)distance=Math.min(distance,(width()-c.size-c.x)/dx);
        if(dx< -1e-8)distance=Math.min(distance,(c.size-c.x)/dx);
        if(dy>1e-8)distance=Math.min(distance,(height()-c.size-c.y)/dy);
        if(dy< -1e-8)distance=Math.min(distance,(c.size-c.y)/dy);
        for(Obstacle o:obstacles){
            double ox=c.x-o.x,oy=c.y-o.y,b=ox*dx+oy*dy,r=o.radius+c.size;
            double disc=b*b-(ox*ox+oy*oy-r*r);
            if(disc>=0){double near=-b-Math.sqrt(disc);if(near>=0)distance=Math.min(distance,near);}
        }
        return 1-clamp(distance/range,0,1);
    }
    private void senseDirection(Creature c,double x,double y,int index){
        double dx=x-c.x,dy=y-c.y,d=Math.hypot(dx,dy);
        if(d>c.vision)return;
        double cos=Math.cos(c.angle),sin=Math.sin(c.angle),denom=Math.max(1,d);
        c.inputs[index]=(dx*cos+dy*sin)/denom;c.inputs[index+1]=(-dx*sin+dy*cos)/denom;c.inputs[index+2]=1-d/c.vision;
    }
    private void die(Creature c){die(c,0);}
    private void die(Creature c,int administrative){
        if(!c.alive)return;c.alive=false;totalDeaths++;deadLifetime+=c.age/60.0;
        RoleStats rs=stats(c.species);if(administrative==0){rs.deaths++;rs.lifetime+=c.age/60.0;}else if(administrative==1)rs.removals++;else rs.replacements++;
        if(creatureGrid!=null)creatureGrid.remove(c);
        if(administrative==0&&food.size()<config.remainsLimit&&!blocked(c.x,c.y,4)){Food remains=new Food(c.x,c.y,4,Resource.CARRION,Math.max(15,c.size*5),0);food.add(remains);if(foodGrid!=null)foodGrid.add(remains,remains.x,remains.y);}
        Ancestor a=ancestry.get(c.id);if(a!=null){a.diedAt=seconds();a.meals=c.eaten;}
        Genome champion=champions.get(c.species);
        // Archive each species' best observed individual for explicit epoch breeding.
        if(champion==null||epochArchive.stream().noneMatch(x->x.species==c.species&&x.fitness()>c.fitness()))champions.put(c.species,c.genome());
        epochArchive.add(c);if(epochArchive.size()>400)epochArchive.remove(0);
    }
    private void eat(Creature c,double energy){
        c.eaten++;totalMeals++;double gain=Math.min(energy,c.capacity()-c.energy);
        c.energy+=gain;c.gained+=gain;totalEnergyGained+=gain;stats(c.species).gained+=gain;
        Ancestor a=ancestry.get(c.id);if(a!=null)a.meals=c.eaten;
    }
    /** Both species breed within their own parent pool. Explicit in GENERATIONS mode only. */
    public void evolve(){
        if(mode!=Mode.GENERATIONS)return;
        totalEggLoss+=eggs.size();for(Egg e:eggs)stats(e.embryo.species).eggLosses++;eggs.clear();
        completedLeaders.clear();for(Species role:Species.values()){Creature leader=bestCandidate(role);if(leader!=null)completedLeaders.add(leader);}
        awaitingSelection=false;selectionRound++;
        Set<Creature> candidates=new LinkedHashSet<>(epochArchive);candidates.addAll(creatures);
        List<Creature> children=new ArrayList<>();
        for(Species species:Species.values()){
            if(species==Species.PREDATOR&&!predatorsEnabled)continue;
            List<Creature> pool=candidates.stream().filter(c->c.species==species).sorted(Comparator.comparingDouble(Creature::fitness).reversed()).limit(config.parentPool).toList();
            int target=species==Species.PREY?config.initialPrey:config.initialHunters;
            for(int i=0;i<target;i++){
                Creature a=pool.isEmpty()?null:pool.get(random.nextInt(pool.size()));
                Creature b=pool.isEmpty()?null:pool.get(random.nextInt(pool.size()));
                Creature parent=pool.isEmpty()?null:i<config.eliteCount?pool.get(i%pool.size()):a.fitness()>b.fitness()?a:b;
                Genome genome=parent!=null?parent.genome():champions.getOrDefault(species,randomGenome(species));
                Creature child=spawn(genome,parent,i>=config.eliteCount);children.add(child);register(child);if(parent!=null)parent.children++;generation=Math.max(generation,child.generation);
            }
        }
        for(Creature c:creatures)die(c,2);
        creatures.clear();creatures.addAll(children);epochArchive.clear();tick=0;
        food.clear();fillFood();sample();updateMetrics();
    }
    public void step(){
        if(awaitingSelection)return;
        tick++;elapsedTicks++;
        updateResources();updateEggs();
        if(elapsedTicks%config.refillTicks==0){int target=foodTarget();for(int i=0;i<Math.round((1+abundance/config.refillAbundanceStep)*config.refillMultiplier)&&food.size()<target;i++)addRandomFood();}
        // Old seasonal food decays gradually, moving the productive patch without teleporting it.
        if(seasons&&config.seasonalRemovalTicks>0&&elapsedTicks%config.seasonalRemovalTicks==0&&!food.isEmpty())food.remove(random.nextInt(food.size()));
        foodGrid=new SpatialGrid<>();for(Food f:food)foodGrid.add(f,f.x,f.y);creatureGrid=new SpatialGrid<>();for(Creature c:creatures)if(c.alive)creatureGrid.add(c,c.x,c.y);
        List<Creature> newborns=new ArrayList<>();
        int prey=count(Species.PREY)+eggCount(Species.PREY),predators=count(Species.PREDATOR)+eggCount(Species.PREDATOR);
        for(Creature c:creatures){
            if(!c.alive)continue;
            Arrays.fill(c.inputs,0);
            Food nearestFood=null;Creature target=null,threat=null;
            double foodD=Double.POSITIVE_INFINITY,targetD=Double.POSITIVE_INFINITY,threatD=Double.POSITIVE_INFINITY;
            {
                for(Food f:foodGrid.near(c.x,c.y,c.vision)){if(c.species==Species.PREDATOR&&f.type!=Resource.CARRION)continue;
                    double actual=(f.x-c.x)*(f.x-c.x)+(f.y-c.y)*(f.y-c.y);if(actual>c.vision*c.vision)continue;
                    double d=actual/dietEfficiency(c,f.type);if(d<foodD){foodD=d;nearestFood=f;}}
                if(nearestFood!=null){Food f=nearestFood;senseDirection(c,f.x,f.y,0);}
            }
            for(Creature other:creatureGrid.near(c.x,c.y,c.vision)){
                if(!other.alive||other==c)continue;
                double d=(other.x-c.x)*(other.x-c.x)+(other.y-c.y)*(other.y-c.y);
                if(other.species==Species.PREY&&c.species==Species.PREDATOR&&d<=c.vision*c.vision&&d<targetD){targetD=d;target=other;}
                if(other.species==Species.PREDATOR&&d<threatD){threatD=d;threat=other;}
            }
            // Hunters can choose a nearby carcass over a live target; no movement is scripted.
            if(target!=null&&(nearestFood==null||targetD<foodD*.65||c.diet!=Diet.SCAVENGER)){senseDirection(c,target.x,target.y,0);nearestFood=null;}
            if(threat!=null)senseDirection(c,threat.x,threat.y,3);
            c.inputs[6]=clamp(c.energy/c.capacity(),0,1);
            c.inputs[7]=ray(c,c.angle);c.inputs[8]=ray(c,c.angle-config.obstacleAngle);c.inputs[9]=ray(c,c.angle+config.obstacleAngle);
            c.inputs[10]=Math.min(c.age/(config.ageSenseSeconds*60),1);c.inputs[11]=Math.sin(c.age/config.oscillatorTicks);
            think(c);c.angle+=c.outputs[0]*config.turnRate*(config.turnReferenceSize/c.size);
            double velocity=config.minimumVelocity+(c.outputs[1]+1)*.5*c.speed,oldX=c.x,oldY=c.y;
            double nx=clamp(c.x+Math.cos(c.angle)*velocity,c.size,width()-c.size);
            double ny=clamp(c.y+Math.sin(c.angle)*velocity,c.size,height()-c.size);
            if(!blocked(nx,ny,c.size)){c.x=nx;c.y=ny;}
            else if(!blocked(nx,c.y,c.size))c.x=nx;
            else if(!blocked(c.x,ny,c.size))c.y=ny;
            creatureGrid.add(c,c.x,c.y);
            c.distance+=Math.hypot(c.x-oldX,c.y-oldY);
            // Larger bodies, better senses, and higher speed each have an energy price.
            double cost=config.baseMetabolism+c.size*c.size*config.sizeCost+velocity*velocity*config.movementCost+c.vision*config.visionCost;
            if(c.species==Species.PREDATOR)cost*=config.hunterMetabolism;
            c.energy-=cost;c.spent+=cost;totalEnergySpent+=cost;stats(c.species).spent+=cost;c.age++;
            if(c.cooldown>0)c.cooldown--;if(c.huntCooldown>0)c.huntCooldown--;
            if(nearestFood!=null&&Math.hypot(nearestFood.x-c.x,nearestFood.y-c.y)<c.size+5){Food meal=nearestFood;food.remove(meal);foodGrid.remove(meal);eat(c,meal.energy*dietEfficiency(c,meal.type));}
            if(target!=null&&target.alive&&c.huntCooldown==0&&Math.hypot(target.x-c.x,target.y-c.y)<c.size+target.size){
                die(target);for(int i=food.size()-1;i>=0;i--){Food remains=food.get(i);if(remains.type==Resource.CARRION&&remains.x==target.x&&remains.y==target.y){remains.energy*=config.captureRemains;break;}}
                prey--;c.kills++;totalKills++;c.huntCooldown=WorldConfig.ticks(config.huntSeconds);eat(c,config.captureEnergy+target.size*config.capturePerSize);
            }
            if(c.species==Species.PREDATOR&&c.huntCooldown==0){
                for(Iterator<Egg> it=eggs.iterator();it.hasNext();){Egg egg=it.next();if(egg.embryo.species==Species.PREY&&!inNest(egg.embryo.x,egg.embryo.y)&&Math.hypot(egg.embryo.x-c.x,egg.embryo.y-c.y)<c.size+7){
                    it.remove();totalEggLoss++;stats(egg.embryo.species).eggLosses++;prey--;eat(c,Math.max(0,egg.embryo.energy)*config.eggMealEfficiency);c.huntCooldown=WorldConfig.ticks(config.huntSeconds);break;}}
            }
            if(tick%5==0){c.trail.add(new Point(c.x,c.y));if(c.trail.size()>24)c.trail.remove(0);}
            if(c.energy<=0||c.age>=WorldConfig.ticks(config.maximumAge)){die(c);if(c.species==Species.PREY)prey--;else predators--;continue;}
            int speciesCount=c.species==Species.PREY?prey:predators;
            if(reproductionEnabled&&mode==Mode.CONTINUOUS&&c.age>=WorldConfig.ticks(config.maturitySeconds)&&c.cooldown==0&&c.energy>c.capacity()*config.reproductionThreshold&&prey+predators<config.populationLimit&&speciesCount<(c.species==Species.PREY?config.preyLimit:config.hunterLimit)){
                Creature child=spawn(c.genome(),c,true);
                if(!positionOffspring(c,child))continue;
                // Splitting conserves energy; birth does not create free food.
                double budget=c.energy*config.offspringInvestment;c.energy-=budget;child.energy=Math.min(child.capacity(),budget*config.offspringEfficiency);
                c.cooldown=WorldConfig.ticks(config.reproductionSeconds);
                if(eggsEnabled){c.eggsLaid++;totalEggsLaid++;eggs.add(new Egg(child,seconds()));}
                else{c.children++;newborns.add(child);register(child);generation=Math.max(generation,child.generation);}
                if(child.species==Species.PREY)prey++;else predators++;
            }
        }
        creatures.addAll(newborns);
        if(mode==Mode.CONTINUOUS)creatures.removeIf(c->!c.alive);
        if(elapsedTicks%300==0)sample();
        updateMetrics();
        if(mode==Mode.GENERATIONS&&(tick>=WorldConfig.ticks(config.generationSeconds)||(aliveCount()==0&&eggs.isEmpty()))){if(config.pauseAtGenerationEnd)awaitingSelection=true;else evolve();}
    }
    public void setPredators(boolean enabled){
        predatorsEnabled=enabled;
        if(!enabled){for(Creature c:creatures)if(c.species==Species.PREDATOR)die(c,1);creatures.removeIf(c->c.species==Species.PREDATOR);eggs.removeIf(e->{if(e.embryo.species!=Species.PREDATOR)return false;totalEggLoss++;stats(Species.PREDATOR).eggLosses++;return true;});}
        else if(count(Species.PREDATOR)+eggCount(Species.PREDATOR)==0){for(int i=0;i<config.initialHunters&&aliveCount()+eggs.size()<config.populationLimit&&count(Species.PREDATOR)+eggCount(Species.PREDATOR)<config.hunterLimit;i++){Creature c=spawn(randomGenome(Species.PREDATOR),null,false);creatures.add(c);register(c);}}
    }
    public void setMode(Mode mode){this.mode=mode;awaitingSelection=false;tick=0;epochArchive.clear();}
    public List<Ancestor> ancestorsOf(Creature c){
        List<Ancestor> chain=new ArrayList<>();int id=c.id;
        while(id!=0&&chain.size()<16){Ancestor a=ancestry.get(id);if(a==null)break;chain.add(a);id=a.parentId;}
        return chain;
    }
    /** Isolated evaluation worlds use the same map, positions, opponents, and RNG seed. */
    public Creature prepareTrial(Genome genome,long seed){
        random.setSeed(seed);reset(null);mode=Mode.CONTINUOUS;reproductionEnabled=false;eggs.clear();tracker.clear();
        creatures.clear();epochArchive.clear();ancestry.clear();founders.clear();champions.clear();
        nextId=0;totalBirths=totalDeaths=totalMeals=totalKills=0;totalEnergySpent=totalEnergyGained=deadLifetime=0;roles=new RoleStats[]{new RoleStats(),new RoleStats()};lineageBirths.clear();
        random.setSeed(seed);food.clear();fillFood();
        if(genome.species==Species.PREDATOR){
            for(int i=0;i<Math.min(24,Math.min(config.preyLimit,config.populationLimit-1));i++){Creature prey=spawn(randomGenome(Species.PREY),null,false);creatures.add(prey);register(prey);}
        }
        Creature focal=spawn(genome,null,false);creatures.add(focal);register(focal);history.clear();return focal;
    }
    public void addTrialHunters(int count){for(int i=0;i<count&&aliveCount()+eggs.size()<config.populationLimit&&count(Species.PREDATOR)<config.hunterLimit;i++){Creature c=spawn(randomGenome(Species.PREDATOR),null,false);creatures.add(c);register(c);}}
    public int eggCount(Species species){return (int)eggs.stream().filter(e->e.embryo.species==species).count();}
    public boolean inNest(double x,double y){for(Nest nest:nests)if(Math.hypot(x-nest.x,y-nest.y)<nest.radius)return true;return false;}
    public static double dietEfficiency(Creature c,Resource type){
        Resource preferred=switch(c.diet){case GRAZER->Resource.PLANT;case FILTER_FEEDER->Resource.PLANKTON;case SCAVENGER->Resource.CARRION;};
        return type==preferred?(c.settings==null?1.15:c.settings.preferredDigest):(c.settings==null?.35:c.settings.otherDigest);
    }
    private void updateResources(){
        for(Iterator<Food> it=food.iterator();it.hasNext();){Food f=it.next();f.age++;
            if(f.type==Resource.CARRION&&f.age>WorldConfig.ticks(config.carrionSeconds)){it.remove();continue;}
            if(config.resourceLifetime>0&&f.type!=Resource.CARRION&&f.age>WorldConfig.ticks(config.resourceLifetime)){it.remove();continue;}
            if(f.type==Resource.PLANKTON){
                if(elapsedTicks%30==0)f.angle+=between(-.6,.6);
                double x=f.x+Math.cos(f.angle)*config.planktonDrift,y=f.y+Math.sin(f.angle)*config.planktonDrift;
                if(x<10||x>width()-10||y<10||y>height()-10||blocked(x,y,4))f.angle+=Math.PI;
                else{f.x=x;f.y=y;}
            }
        }
        if(elapsedTicks%WorldConfig.ticks(config.sourceSeconds)==0)for(FoodSource source:sources)if(food.size()<config.foodLimit){
            if(config.localSourceQuota>0&&food.stream().filter(f->Math.hypot(f.x-source.x,f.y-source.y)<source.radius*2).count()>=config.localSourceQuota)continue;
            double a=between(-Math.PI,Math.PI),r=between(0,source.radius);
            double x=clamp(source.x+Math.cos(a)*r,10,width()-10),y=clamp(source.y+Math.sin(a)*r,10,height()-10);
            if(!blocked(x,y,4))food.add(new Food(x,y,2.5,source.type,source.type==Resource.PLANKTON?config.planktonEnergy:config.plantEnergy,between(-Math.PI,Math.PI)));
        }
    }
    private void updateEggs(){
        for(Iterator<Egg> it=eggs.iterator();it.hasNext();){Egg egg=it.next();Creature child=egg.embryo;
            egg.incubation+=inNest(child.x,child.y)?config.nestHatchMultiplier:1;child.energy-=config.incubationCost;child.spent+=config.incubationCost;totalEnergySpent+=config.incubationCost;stats(child.species).spent+=config.incubationCost;
            if(child.energy<=0){totalEggLoss++;stats(child.species).eggLosses++;it.remove();continue;}
            if(egg.incubation>=WorldConfig.ticks(config.hatchSeconds)&&!blocked(child.x,child.y,child.size)){
                child.bornAt=seconds();creatures.add(child);register(child);stats(child.species).hatches++;generation=Math.max(generation,child.generation);
                Creature parent=creatures.stream().filter(c->c.id==child.parentId).findFirst().orElseGet(()->epochArchive.stream().filter(c->c.id==child.parentId).findFirst().orElse(null));
                if(parent!=null)parent.children++;
                it.remove();
            }
        }
    }
    /** An obstacle stroke only succeeds if it leaves every live body and egg untouched. */
    public boolean addRock(double x,double y,double radius){
        radius=clamp(radius,10,70);
        if(obstacles.size()>=250||x-radius<5||x+radius>width()-5||y-radius<5||y+radius>height()-5)return false;
        for(Creature c:creatures)if(c.alive&&Math.hypot(x-c.x,y-c.y)<radius+c.size+3)return false;
        for(Egg egg:eggs)if(Math.hypot(x-egg.embryo.x,y-egg.embryo.y)<radius+egg.embryo.size+3)return false;
        for(Obstacle rock:obstacles)if(Math.hypot(x-rock.x,y-rock.y)<Math.min(radius,rock.radius)*.7)return false;
        obstacles.add(new Obstacle(x,y,radius));food.removeIf(f->blocked(f.x,f.y,4));return true;
    }
    /** Erase rocks, nests, and sources under the brush to carve channels or remove habitats. */
    public void eraseHabitat(double x,double y,double radius){
        obstacles.removeIf(o->Math.hypot(x-o.x,y-o.y)<radius+o.radius);
        nests.removeIf(n->Math.hypot(x-n.x,y-n.y)<radius+n.radius);
        sources.removeIf(f->Math.hypot(x-f.x,y-f.y)<radius+f.radius);
    }
    public boolean addNest(double x,double y){
        if(nests.size()>=50||blocked(x,y,10))return false;
        for(Nest n:nests)if(Math.hypot(x-n.x,y-n.y)<45)return false;nests.add(new Nest(x,y,50));return true;
    }
    public boolean addSource(double x,double y,Resource type){
        if(type==Resource.CARRION||sources.size()>=50||blocked(x,y,4))return false;
        for(FoodSource f:sources)if(f.type==type&&Math.hypot(x-f.x,y-f.y)<30)return false;sources.add(new FoodSource(x,y,40,type));return true;
    }

    /** Called after decoding to check private containers that drive future selection. */
    void validateInternals()throws java.io.IOException {
        if(config==null||roles==null||roles.length!=2||completedLeaders==null)throw new java.io.IOException("Missing configuration or statistics");
        config.validate();
        for(RoleStats rs:roles)if(rs==null||!Double.isFinite(rs.gained)||!Double.isFinite(rs.spent)||!Double.isFinite(rs.lifetime)||!Double.isFinite(rs.extinctAt)||rs.deaths<0||rs.hatches<0||rs.removals<0)throw new java.io.IOException("Invalid role statistics");
        if(random==null||champions==null||epochArchive==null||epochArchive.size()>400)throw new java.io.IOException("Invalid internal world state.");
        for(Creature c:epochArchive){if(c==null||c.alive||!Double.isFinite(c.energy))throw new java.io.IOException("Invalid archived creature.");c.genome();}
        for(Genome genome:champions.values())if(genome==null)throw new java.io.IOException("Invalid champion genome.");
    }

    private boolean positionOffspring(Creature parent,Creature child){
        for(int attempt=0;attempt<24;attempt++){
            double x=clamp(parent.x+between(-25,25),child.size,width()-child.size),y=clamp(parent.y+between(-25,25),child.size,height()-child.size);
            boolean clear=true;
            for(int step=0;step<=8;step++){double t=step/8.0;if(blocked(parent.x+(x-parent.x)*t,parent.y+(y-parent.y)*t,child.size)){clear=false;break;}}
            if(clear){child.x=x;child.y=y;return true;}
        }
        return false;
    }

}
