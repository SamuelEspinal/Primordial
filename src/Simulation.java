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
    private final EnumMap<Species,Genome> champions=new EnumMap<>(Species.class);
    private final List<Creature> epochArchive=new ArrayList<>();
    private final Random random;

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
                         double size,double speed,double vision,long births,long deaths) implements java.io.Serializable {}
    public static final class Ancestor implements java.io.Serializable {
        private static final long serialVersionUID=1L;
        public final int id,parentId,rootId,generation;
        public final Species species;
        public final double bornAt;
        public int children,meals;
        public double diedAt=-1;
        Ancestor(Creature c,double now){id=c.id;parentId=c.parentId;rootId=c.rootId;generation=c.generation;species=c.species;bornAt=now;}
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
    }
    public static final class Creature implements java.io.Serializable {
        private static final long serialVersionUID=1L;
        public int id,parentId,rootId,clusterId,generation=1,age,eaten,kills,children,eggsLaid,cooldown,huntCooldown;
        public double x,y,angle,energy,distance,spent,gained,size,speed,vision,bornAt;
        public float hue;
        public Species species;
        public Diet diet=Diet.GRAZER;
        public boolean alive=true;
        public double[] weights,inputs=new double[INPUTS],hidden=new double[HIDDEN],outputs=new double[OUTPUTS];
        public final List<Point> trail=new ArrayList<>();
        public Genome genome(){return new Genome(weights,hue,size,speed,vision,species,diet);}
        public double capacity(){return 100+size*9;}
        public double fitness(){return eaten*100+children*30+age*.015+distance*.002;}
    }
    public Simulation(){this(new Random());}
    public Simulation(Random random){this.random=random;reset(null);}
    double between(double a,double b){return a+random.nextDouble()*(b-a);}
    public static double clamp(double v,double a,double b){return Math.max(a,Math.min(b,v));}
    private Genome randomGenome(Species species){
        double[] w=new double[WEIGHT_COUNT];
        for(int i=0;i<w.length;i++)w[i]=between(-1.4,1.4);
        // Recurrent links begin small to avoid saturating every hidden neuron.
        for(int i=RECURRENT_OFFSET;i<OUTPUT_OFFSET;i++)w[i]*=.25;
        return new Genome(w,species==Species.PREDATOR?(float)between(.01,.09):(float)between(.22,.65),
                between(6,10),between(1.8,3.2),between(140,300),species,Diet.values()[random.nextInt(3)]);
    }
    private Genome mutate(Genome parent){
        double[] w=parent.weights();
        for(int i=0;i<w.length;i++)if(random.nextDouble()<mutationRate)w[i]=clamp(w[i]+between(-.5,.5),-5,5);
        double size=parent.size,speed=parent.speed,vision=parent.vision;
        if(random.nextDouble()<mutationRate)size=clamp(size+between(-1,1),5,12);
        if(random.nextDouble()<mutationRate)speed=clamp(speed+between(-.25,.25),1.2,3.8);
        if(random.nextDouble()<mutationRate)vision=clamp(vision+between(-30,30),70,360);
        Diet diet=random.nextDouble()<mutationRate*.4?Diet.values()[random.nextInt(3)]:parent.diet;
        return new Genome(w,parent.hue,size,speed,vision,parent.species,diet);
    }
    private Creature spawn(Genome genome,Creature parent,boolean mutate){
        if(mutate)genome=mutate(genome);
        Creature c=new Creature();c.id=++nextId;c.weights=genome.weights();c.hue=genome.hue;
        c.size=genome.size;c.speed=genome.speed;c.vision=genome.vision;c.species=genome.species;c.diet=genome.diet;
        c.x=between(30,WIDTH-30);c.y=between(30,HEIGHT-30);c.angle=between(-Math.PI,Math.PI);
        c.energy=c.capacity()*.68;c.bornAt=seconds();
        if(parent!=null){c.parentId=parent.id;c.rootId=parent.rootId;c.clusterId=parent.clusterId;c.generation=parent.generation+1;}
        else c.rootId=c.id;
        placeOutsideRocks(c);
        return c;
    }
    private void register(Creature c){
        c.clusterId=tracker.assign(c,seconds());
        ancestry.put(c.id,new Ancestor(c,seconds()));
        if(c.parentId==0)founders.put(c.rootId,c.genome());
        else {Ancestor parent=ancestry.get(c.parentId);if(parent!=null)parent.children++;}
        while(ancestry.size()>MAX_ANCESTRY)ancestry.remove(ancestry.keySet().iterator().next());
        totalBirths++;
    }
    public void reset(Genome seed){
        generation=1;tick=nextId=0;elapsedTicks=totalBirths=totalDeaths=totalMeals=totalKills=0;
        totalEnergySpent=totalEnergyGained=deadLifetime=0;
        history.clear();ancestry.clear();founders.clear();champions.clear();epochArchive.clear();creatures.clear();food.clear();obstacles.clear();eggs.clear();nests.clear();sources.clear();tracker.clear();
        totalEggsLaid=totalEggLoss=0;
        nests.add(new Nest(210,620,65));nests.add(new Nest(1040,700,65));
        obstacles.addAll(List.of(new Obstacle(400,265,65),new Obstacle(795,560,80),new Obstacle(580,640,42),new Obstacle(940,230,45)));
        for(Species species:Species.values()){
            if(species==Species.PREDATOR&&!predatorsEnabled)continue;
            int count=species==Species.PREY?POPULATION:10;
            for(int i=0;i<count;i++){
                Genome g=seed!=null&&seed.species==species?seed:randomGenome(species);
                Creature c=spawn(g,null,seed!=null&&seed.species==species&&i>0);
                creatures.add(c);register(c);champions.putIfAbsent(species,c.genome());
            }
        }
        fillFood();sample();
    }
    public double seconds(){return elapsedTicks/60.0;}
    public String season(){return !seasons?"Constant summer":new String[]{"Spring","Summer","Autumn","Winter"}[seasonIndex()];}
    private int seasonIndex(){return (int)(elapsedTicks/1800%4);}
    public double seasonFactor(){return !seasons?1:new double[]{1.15,1,.65,.35}[seasonIndex()];}
    public Point foodPatch(){
        int i=seasons?seasonIndex():1;
        return new Point(new double[]{260,930,850,300}[i],new double[]{230,230,650,640}[i]);
    }
    private int foodTarget(){return (int)((90+abundance*5)*seasonFactor());}
    private void addRandomFood(){
        Point patch=foodPatch();
        for(int tries=0;tries<30;tries++){
            double x,y;
            if(random.nextDouble()<.75){x=clamp(patch.x+random.nextGaussian()*155,18,WIDTH-18);y=clamp(patch.y+random.nextGaussian()*135,18,HEIGHT-18);}
            else{x=between(18,WIDTH-18);y=between(18,HEIGHT-18);}
            if(!blocked(x,y,4)){Resource type=random.nextDouble()<.3?Resource.PLANKTON:Resource.PLANT;food.add(new Food(x,y,between(1.6,3.3),type,type==Resource.PLANT?48:36,between(-Math.PI,Math.PI)));return;}
        }
    }
    private void fillFood(){for(int i=0;i<foodTarget();i++)addRandomFood();}
    public boolean blocked(double x,double y,double radius){
        for(Obstacle o:obstacles)if(Math.hypot(x-o.x,y-o.y)<radius+o.radius)return true;return false;
    }
    private void placeOutsideRocks(Creature c){
        for(int i=0;i<100&&blocked(c.x,c.y,c.size);i++){c.x=between(30,WIDTH-30);c.y=between(30,HEIGHT-30);}
        if(blocked(c.x,c.y,c.size)){c.x=25;c.y=25;}
    }
    public void paintFood(double x,double y){paintFood(x,y,Resource.PLANT);}
    public void paintFood(double x,double y,Resource type){
        for(int i=0;i<7&&food.size()<1000;i++){
            double fx=clamp(x+between(-24,24),10,WIDTH-10),fy=clamp(y+between(-24,24),10,HEIGHT-10);
            if(!blocked(fx,fy,4))food.add(new Food(fx,fy,between(1.6,3.3),type,type==Resource.CARRION?70:type==Resource.PLANKTON?36:48,between(-Math.PI,Math.PI)));
        }
    }
    public int count(Species species){return (int)creatures.stream().filter(c->c.alive&&c.species==species).count();}
    public int aliveCount(){return count(Species.PREY)+count(Species.PREDATOR);}
    public Creature best(Species species){return creatures.stream().filter(c->c.alive&&c.species==species).max(Comparator.comparingDouble(Creature::fitness)).orElse(null);}
    public double efficiency(){return totalEnergySpent==0?0:totalEnergyGained/totalEnergySpent;}
    public double meanLifespan(){return totalDeaths==0?0:deadLifetime/totalDeaths;}
    public double meanTrait(int trait){return creatures.stream().filter(c->c.alive).mapToDouble(c->trait==0?c.size:trait==1?c.speed:c.vision).average().orElse(0);}
    private void sample(){
        history.add(new Sample(seconds(),count(Species.PREY),count(Species.PREDATOR),meanLifespan(),efficiency(),meanTrait(0),meanTrait(1),meanTrait(2),totalBirths,totalDeaths));
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
        double range=90,dx=Math.cos(angle),dy=Math.sin(angle);
        double distance=range;
        if(dx>1e-8)distance=Math.min(distance,(WIDTH-c.size-c.x)/dx);
        if(dx< -1e-8)distance=Math.min(distance,(c.size-c.x)/dx);
        if(dy>1e-8)distance=Math.min(distance,(HEIGHT-c.size-c.y)/dy);
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
    private void die(Creature c){
        if(!c.alive)return;c.alive=false;totalDeaths++;deadLifetime+=c.age/60.0;
        if(food.size()<1400&&!blocked(c.x,c.y,4))food.add(new Food(c.x,c.y,4,Resource.CARRION,Math.max(15,c.size*5),0));
        Ancestor a=ancestry.get(c.id);if(a!=null){a.diedAt=seconds();a.meals=c.eaten;}
        Genome champion=champions.get(c.species);
        // Archive each species' best observed individual for explicit epoch breeding.
        if(champion==null||epochArchive.stream().noneMatch(x->x.species==c.species&&x.fitness()>c.fitness()))champions.put(c.species,c.genome());
        epochArchive.add(c);if(epochArchive.size()>400)epochArchive.remove(0);
    }
    private void eat(Creature c,double energy){
        c.eaten++;totalMeals++;double gain=Math.min(energy,c.capacity()-c.energy);
        c.energy+=gain;c.gained+=gain;totalEnergyGained+=gain;
        Ancestor a=ancestry.get(c.id);if(a!=null)a.meals=c.eaten;
    }
    /** Both species breed within their own parent pool. Explicit in GENERATIONS mode only. */
    public void evolve(){
        if(mode!=Mode.GENERATIONS)return;
        totalEggLoss+=eggs.size();eggs.clear();
        Set<Creature> candidates=new LinkedHashSet<>(epochArchive);candidates.addAll(creatures);
        List<Creature> children=new ArrayList<>();
        for(Species species:Species.values()){
            if(species==Species.PREDATOR&&!predatorsEnabled)continue;
            List<Creature> pool=candidates.stream().filter(c->c.species==species).sorted(Comparator.comparingDouble(Creature::fitness).reversed()).limit(12).toList();
            int target=species==Species.PREY?POPULATION:10;
            for(int i=0;i<target;i++){
                Creature a=pool.isEmpty()?null:pool.get(random.nextInt(pool.size()));
                Creature b=pool.isEmpty()?null:pool.get(random.nextInt(pool.size()));
                Creature parent=pool.isEmpty()?null:i==0?pool.get(0):a.fitness()>b.fitness()?a:b;
                Genome genome=parent!=null?parent.genome():champions.getOrDefault(species,randomGenome(species));
                Creature child=spawn(genome,parent,i>0);children.add(child);register(child);if(parent!=null)parent.children++;generation=Math.max(generation,child.generation);
            }
        }
        for(Creature c:creatures)die(c);
        creatures.clear();creatures.addAll(children);epochArchive.clear();tick=0;
        food.clear();fillFood();sample();
    }
    public void step(){
        tick++;elapsedTicks++;
        updateResources();updateEggs();
        if(elapsedTicks%10==0){int target=foodTarget();for(int i=0;i<1+abundance/20&&food.size()<target;i++)addRandomFood();}
        // Old seasonal food decays gradually, moving the productive patch without teleporting it.
        if(seasons&&elapsedTicks%20==0&&!food.isEmpty())food.remove(random.nextInt(food.size()));
        List<Creature> newborns=new ArrayList<>();
        int prey=count(Species.PREY)+eggCount(Species.PREY),predators=count(Species.PREDATOR)+eggCount(Species.PREDATOR);
        for(Creature c:creatures){
            if(!c.alive)continue;
            Arrays.fill(c.inputs,0);
            int nearestFood=-1;Creature target=null,threat=null;
            double foodD=Double.POSITIVE_INFINITY,targetD=Double.POSITIVE_INFINITY,threatD=Double.POSITIVE_INFINITY;
            {
                for(int i=0;i<food.size();i++){Food f=food.get(i);if(c.species==Species.PREDATOR&&f.type!=Resource.CARRION)continue;
                    double actual=(f.x-c.x)*(f.x-c.x)+(f.y-c.y)*(f.y-c.y);if(actual>c.vision*c.vision)continue;
                    double d=actual/dietEfficiency(c,f.type);if(d<foodD){foodD=d;nearestFood=i;}}
                if(nearestFood>=0){Food f=food.get(nearestFood);senseDirection(c,f.x,f.y,0);}
            }
            for(Creature other:creatures){
                if(!other.alive||other==c)continue;
                double d=(other.x-c.x)*(other.x-c.x)+(other.y-c.y)*(other.y-c.y);
                if(other.species==Species.PREY&&c.species==Species.PREDATOR&&d<=c.vision*c.vision&&d<targetD){targetD=d;target=other;}
                if(other.species==Species.PREDATOR&&d<threatD){threatD=d;threat=other;}
            }
            // Hunters can choose a nearby carcass over a live target; no movement is scripted.
            if(target!=null&&(nearestFood<0||targetD<foodD*.65||c.diet!=Diet.SCAVENGER)){senseDirection(c,target.x,target.y,0);nearestFood=-1;}
            if(threat!=null)senseDirection(c,threat.x,threat.y,3);
            c.inputs[6]=clamp(c.energy/c.capacity(),0,1);
            c.inputs[7]=ray(c,c.angle);c.inputs[8]=ray(c,c.angle-.65);c.inputs[9]=ray(c,c.angle+.65);
            c.inputs[10]=Math.min(c.age/7200.0,1);c.inputs[11]=Math.sin(c.age/35.0);
            think(c);c.angle+=c.outputs[0]*.13*(8/c.size);
            double velocity=.18+(c.outputs[1]+1)*.5*c.speed,oldX=c.x,oldY=c.y;
            double nx=clamp(c.x+Math.cos(c.angle)*velocity,c.size,WIDTH-c.size);
            double ny=clamp(c.y+Math.sin(c.angle)*velocity,c.size,HEIGHT-c.size);
            if(!blocked(nx,ny,c.size)){c.x=nx;c.y=ny;}
            else if(!blocked(nx,c.y,c.size))c.x=nx;
            else if(!blocked(c.x,ny,c.size))c.y=ny;
            c.distance+=Math.hypot(c.x-oldX,c.y-oldY);
            // Larger bodies, better senses, and higher speed each have an energy price.
            double cost=.018+c.size*c.size*.00032+velocity*velocity*.009+c.vision*.000045;
            if(c.species==Species.PREDATOR)cost*=.80;
            c.energy-=cost;c.spent+=cost;totalEnergySpent+=cost;c.age++;
            if(c.cooldown>0)c.cooldown--;if(c.huntCooldown>0)c.huntCooldown--;
            if(nearestFood>=0&&Math.hypot(food.get(nearestFood).x-c.x,food.get(nearestFood).y-c.y)<c.size+5){Food meal=food.remove(nearestFood);eat(c,meal.energy*dietEfficiency(c,meal.type));}
            if(target!=null&&target.alive&&c.huntCooldown==0&&Math.hypot(target.x-c.x,target.y-c.y)<c.size+target.size){
                die(target);for(int i=food.size()-1;i>=0;i--){Food remains=food.get(i);if(remains.type==Resource.CARRION&&remains.x==target.x&&remains.y==target.y){remains.energy*=.25;break;}}
                prey--;c.kills++;totalKills++;c.huntCooldown=120;eat(c,90+target.size*3);
            }
            if(c.species==Species.PREDATOR&&c.huntCooldown==0){
                for(Iterator<Egg> it=eggs.iterator();it.hasNext();){Egg egg=it.next();if(egg.embryo.species==Species.PREY&&!inNest(egg.embryo.x,egg.embryo.y)&&Math.hypot(egg.embryo.x-c.x,egg.embryo.y-c.y)<c.size+7){
                    it.remove();totalEggLoss++;prey--;eat(c,Math.max(0,egg.embryo.energy)*.7);c.huntCooldown=120;break;}}
            }
            if(tick%5==0){c.trail.add(new Point(c.x,c.y));if(c.trail.size()>24)c.trail.remove(0);}
            if(c.energy<=0||c.age>=14400){die(c);if(c.species==Species.PREY)prey--;else predators--;continue;}
            int speciesCount=c.species==Species.PREY?prey:predators;
            if(reproductionEnabled&&mode==Mode.CONTINUOUS&&c.age>=450&&c.cooldown==0&&c.energy>c.capacity()*.86&&prey+predators<MAX_POPULATION&&speciesCount<(c.species==Species.PREY?150:30)){
                Creature child=spawn(c.genome(),c,true);
                if(!positionOffspring(c,child))continue;
                // Splitting conserves energy; birth does not create free food.
                double budget=c.energy*.47;c.energy-=budget;child.energy=Math.min(child.capacity(),budget*.9);
                c.cooldown=450;
                if(eggsEnabled){c.eggsLaid++;totalEggsLaid++;eggs.add(new Egg(child,seconds()));}
                else{c.children++;newborns.add(child);register(child);generation=Math.max(generation,child.generation);}
                if(child.species==Species.PREY)prey++;else predators++;
            }
        }
        creatures.addAll(newborns);
        if(mode==Mode.CONTINUOUS)creatures.removeIf(c->!c.alive);
        if(elapsedTicks%300==0)sample();
        if(mode==Mode.GENERATIONS&&(tick>=GENERATION_TICKS||(aliveCount()==0&&eggs.isEmpty())))evolve();
    }
    public void setPredators(boolean enabled){
        predatorsEnabled=enabled;
        if(!enabled){for(Creature c:creatures)if(c.species==Species.PREDATOR)die(c);creatures.removeIf(c->c.species==Species.PREDATOR);eggs.removeIf(e->e.embryo.species==Species.PREDATOR);}
        else if(count(Species.PREDATOR)+eggCount(Species.PREDATOR)==0){for(int i=0;i<10&&aliveCount()+eggs.size()<MAX_POPULATION;i++){Creature c=spawn(randomGenome(Species.PREDATOR),null,false);creatures.add(c);register(c);}}
    }
    public void setMode(Mode mode){this.mode=mode;tick=0;epochArchive.clear();}
    public List<Ancestor> ancestorsOf(Creature c){
        List<Ancestor> chain=new ArrayList<>();int id=c.id;
        while(id!=0&&chain.size()<16){Ancestor a=ancestry.get(id);if(a==null)break;chain.add(a);id=a.parentId;}
        return chain;
    }
    /** Isolated evaluation worlds use the same map, positions, opponents, and RNG seed. */
    public Creature prepareTrial(Genome genome,long seed){
        random.setSeed(seed);reset(null);mode=Mode.CONTINUOUS;reproductionEnabled=false;eggs.clear();tracker.clear();
        creatures.clear();epochArchive.clear();ancestry.clear();founders.clear();champions.clear();
        nextId=0;totalBirths=totalDeaths=totalMeals=totalKills=0;totalEnergySpent=totalEnergyGained=deadLifetime=0;
        random.setSeed(seed);food.clear();fillFood();
        if(genome.species==Species.PREDATOR){
            for(int i=0;i<24;i++){Creature prey=spawn(randomGenome(Species.PREY),null,false);creatures.add(prey);register(prey);}
        }
        Creature focal=spawn(genome,null,false);creatures.add(focal);register(focal);history.clear();return focal;
    }
    public int eggCount(Species species){return (int)eggs.stream().filter(e->e.embryo.species==species).count();}
    public boolean inNest(double x,double y){for(Nest nest:nests)if(Math.hypot(x-nest.x,y-nest.y)<nest.radius)return true;return false;}
    public static double dietEfficiency(Creature c,Resource type){
        Resource preferred=switch(c.diet){case GRAZER->Resource.PLANT;case FILTER_FEEDER->Resource.PLANKTON;case SCAVENGER->Resource.CARRION;};
        return type==preferred?1.15:.35;
    }
    private void updateResources(){
        for(Iterator<Food> it=food.iterator();it.hasNext();){Food f=it.next();f.age++;
            if(f.type==Resource.CARRION&&f.age>1800){it.remove();continue;}
            if(f.type==Resource.PLANKTON){
                if(elapsedTicks%30==0)f.angle+=between(-.6,.6);
                double x=f.x+Math.cos(f.angle)*.65,y=f.y+Math.sin(f.angle)*.65;
                if(x<10||x>WIDTH-10||y<10||y>HEIGHT-10||blocked(x,y,4))f.angle+=Math.PI;
                else{f.x=x;f.y=y;}
            }
        }
        if(elapsedTicks%30==0)for(FoodSource source:sources)if(food.size()<1000){
            double a=between(-Math.PI,Math.PI),r=between(0,source.radius);
            double x=clamp(source.x+Math.cos(a)*r,10,WIDTH-10),y=clamp(source.y+Math.sin(a)*r,10,HEIGHT-10);
            if(!blocked(x,y,4))food.add(new Food(x,y,2.5,source.type,source.type==Resource.PLANKTON?36:48,between(-Math.PI,Math.PI)));
        }
    }
    private void updateEggs(){
        for(Iterator<Egg> it=eggs.iterator();it.hasNext();){Egg egg=it.next();Creature child=egg.embryo;
            egg.incubation+=inNest(child.x,child.y)?2:1;child.energy-=.003;child.spent+=.003;totalEnergySpent+=.003;
            if(child.energy<=0){totalEggLoss++;it.remove();continue;}
            if(egg.incubation>=HATCH_TICKS&&!blocked(child.x,child.y,child.size)){
                child.bornAt=seconds();creatures.add(child);register(child);generation=Math.max(generation,child.generation);
                Creature parent=creatures.stream().filter(c->c.id==child.parentId).findFirst().orElseGet(()->epochArchive.stream().filter(c->c.id==child.parentId).findFirst().orElse(null));
                if(parent!=null)parent.children++;
                it.remove();
            }
        }
    }
    /** An obstacle stroke only succeeds if it leaves every live body and egg untouched. */
    public boolean addRock(double x,double y,double radius){
        radius=clamp(radius,10,70);
        if(obstacles.size()>=250||x-radius<5||x+radius>WIDTH-5||y-radius<5||y+radius>HEIGHT-5)return false;
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
        if(random==null||champions==null||epochArchive==null||epochArchive.size()>400)throw new java.io.IOException("Invalid internal world state.");
        for(Creature c:epochArchive){if(c==null||c.alive||!Double.isFinite(c.energy))throw new java.io.IOException("Invalid archived creature.");c.genome();}
        for(Genome genome:champions.values())if(genome==null)throw new java.io.IOException("Invalid champion genome.");
    }

    private boolean positionOffspring(Creature parent,Creature child){
        for(int attempt=0;attempt<24;attempt++){
            double x=clamp(parent.x+between(-25,25),child.size,WIDTH-child.size),y=clamp(parent.y+between(-25,25),child.size,HEIGHT-child.size);
            boolean clear=true;
            for(int step=0;step<=8;step++){double t=step/8.0;if(blocked(parent.x+(x-parent.x)*t,parent.y+(y-parent.y)*t,child.size)){clear=false;break;}}
            if(clear){child.x=x;child.y=y;return true;}
        }
        return false;
    }

}
