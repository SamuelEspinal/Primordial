import java.io.Serializable;
import java.lang.annotation.*;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.*;

/** Versioned biological settings. Numeric limits protect geometry, genomes and memory. */
public final class WorldConfig implements Serializable {
    private static final long serialVersionUID = 1L;
    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.FIELD)
    public @interface Setting { String group(); double min() default 0; double max() default 1; String help(); }
    @Setting(group="World",min=300,max=6000,help="World width, independent of window size.") public int width=1200;
    @Setting(group="World",min=300,max=6000,help="World height. Habitat coordinates scale with dimensions.") public int height=850;
    @Setting(group="Population",max=1000,help="Initial prey and selection replacement population.") public int initialPrey=60;
    @Setting(group="Population",max=1000,help="Initial hunters and selection replacement population.") public int initialHunters=10;
    @Setting(group="Population",min=1,max=2000,help="Maximum adults plus eggs across both roles.") public int populationLimit=180;
    @Setting(group="Population",max=2000,help="Continuous prey adults plus eggs limit.") public int preyLimit=150;
    @Setting(group="Population",max=2000,help="Continuous hunter adults plus eggs limit.") public int hunterLimit=30;
    @Setting(group="World",help="Enable the hunter role.") public boolean hunters=true;
    @Setting(group="World",help="Enable shifting seasons and food patches.") public boolean seasons=true;
    @Setting(group="World",help="Enable delayed egg hatching; otherwise offspring are born immediately.") public boolean eggs=true;
    @Setting(group="World",help="Continuous ecology or explicit synchronized selection.") public Simulation.Mode mode=Simulation.Mode.CONTINUOUS;
    @Setting(group="Selection",help="Hold the completed population before breeding so champions can be inspected.") public boolean pauseAtGenerationEnd=false;
    @Setting(group="Selection",min=1,max=3600,help="Simulated seconds per selection round.") public double generationSeconds=45;
    @Setting(group="Selection",min=1,max=100,help="Top candidates per role; tournament chooses one parent, without crossover.") public int parentPool=12;
    @Setting(group="Selection",max=100,help="Unmutated elite copies per role, capped by the replacement population.") public int eliteCount=1;
    @Setting(group="Selection",min=0,max=10000,help="Selection fitness reward per meal/capture.") public double fitnessMeal=100;
    @Setting(group="Selection",min=0,max=10000,help="Selection fitness reward per successful child.") public double fitnessChild=30;
    @Setting(group="Selection",min=0,max=10,help="Selection fitness reward per tick alive.") public double fitnessAge=.015;
    @Setting(group="Selection",min=0,max=10,help="Selection fitness reward per unit travelled.") public double fitnessDistance=.002;
    @Setting(group="Food",max=100,help="Automatic food abundance; also controls replenishment.") public int abundance=55;
    @Setting(group="Food",max=5000,help="Base automatic target before abundance and seasonal factors.") public int foodBase=90;
    @Setting(group="Food",max=50,help="Additional food target per abundance point.") public double foodPerAbundance=5;
    @Setting(group="Food",min=1,max=600,help="Ticks between automatic replenishment attempts (60 ticks/second).") public int refillTicks=10;
    @Setting(group="Food",min=1,max=100,help="Abundance points per additional food piece each refill.") public int refillAbundanceStep=20;
    @Setting(group="Food",min=.1,max=16,help="Multiplier for automatic refill batch size, rounded to whole pieces.") public double refillMultiplier=1;
    @Setting(group="Food",min=50,max=10000,help="Global loose-food limit for painting and persistent sources.") public int foodLimit=1000;
    @Setting(group="Food",min=50,max=15000,help="Maximum loose food including death remains.") public int remainsLimit=1400;
    @Setting(group="Food",help="Fraction of automatic food generated near the fertile patch.") public double patchFraction=.75;
    @Setting(group="Food",min=10,max=1000,help="Horizontal patch spread in default-map units; scales with width.") public double patchSpreadX=155;
    @Setting(group="Food",min=10,max=1000,help="Vertical patch spread in default-map units; scales with height.") public double patchSpreadY=135;
    @Setting(group="Food",help="Probability an automatic resource is plankton.") public double planktonFraction=.3;
    @Setting(group="Food",min=1,max=500,help="Energy in an unmodified plant meal.") public double plantEnergy=48;
    @Setting(group="Food",min=1,max=500,help="Energy in an unmodified plankton meal.") public double planktonEnergy=36;
    @Setting(group="Food",min=1,max=500,help="Energy in a painted carrion fragment.") public double paintedCarrionEnergy=70;
    @Setting(group="Food",min=.1,max=10,help="Energy multiplier for preferred diet resources.") public double preferredDigest=1.15;
    @Setting(group="Food",min=.01,max=10,help="Energy multiplier for other resource types.") public double otherDigest=.35;
    @Setting(group="Food",max=10,help="Plankton movement per tick.") public double planktonDrift=.65;
    @Setting(group="Food",min=1,max=3600,help="Carrion lifetime in simulated seconds.") public double carrionSeconds=30;
    @Setting(group="Food",max=3600,help="Plant/plankton lifetime; zero preserves legacy unlimited lifetime.") public double resourceLifetime=0;
    @Setting(group="Food",min=.05,max=60,help="Seconds between each source's production attempts.") public double sourceSeconds=.5;
    @Setting(group="Food",max=500,help="Optional per-source nearby food quota; zero uses only the global limit.") public int localSourceQuota=0;
    @Setting(group="Habitat",max=25,help="Grid sites with one plant and one plankton source each.") public int initialSourceSites=0;
    @Setting(group="Habitat",max=50,help="Initial rocks; first four use the scaled classic layout.") public int initialRocks=4;
    @Setting(group="Habitat",max=50,help="Initial nests; first two use the scaled classic layout.") public int initialNests=2;
    @Setting(group="Habitat",min=.25,max=2,help="Rock radii multiplier (maximum 100 world units).") public double rockScale=1;
    @Setting(group="Seasons",min=1,max=3600,help="Seconds per season.") public double seasonSeconds=30;
    @Setting(group="Seasons",max=5,help="Spring food multiplier.") public double springFactor=1.15;
    @Setting(group="Seasons",max=5,help="Summer food multiplier (also used with seasons off).") public double summerFactor=1;
    @Setting(group="Seasons",max=5,help="Autumn food multiplier.") public double autumnFactor=.65;
    @Setting(group="Seasons",max=5,help="Winter food multiplier.") public double winterFactor=.35;
    @Setting(group="Seasons",max=3600,help="Ticks per seasonal random food removal; zero disables removal.") public int seasonalRemovalTicks=20;
    @Setting(group="Reproduction",min=.1,max=3600,help="Minimum adult age for continuous reproduction, seconds.") public double maturitySeconds=7.5;
    @Setting(group="Reproduction",min=.1,max=3600,help="Seconds between egg investments by one parent.") public double reproductionSeconds=7.5;
    @Setting(group="Reproduction",min=.1,max=3600,help="Egg incubation time outside nests, seconds.") public double hatchSeconds=6;
    @Setting(group="Reproduction",min=1,max=10,help="Incubation speed multiplier inside nests.") public int nestHatchMultiplier=2;
    @Setting(group="Reproduction",min=.1,max=1,help="Fraction of capacity required before investing in offspring.") public double reproductionThreshold=.86;
    @Setting(group="Reproduction",min=.01,max=.9,help="Fraction of parental energy invested in an offspring.") public double offspringInvestment=.47;
    @Setting(group="Reproduction",min=.01,max=1,help="Fraction of investment reaching the embryo. No free reproductive energy.") public double offspringEfficiency=.9;
    @Setting(group="Reproduction",max=1,help="Embryo energy spent per incubation tick.") public double incubationCost=.003;
    @Setting(group="Life & movement",min=1,max=86400,help="Maximum individual age, seconds.") public double maximumAge=240;
    @Setting(group="Life & movement",min=.01,max=1,help="Initial founder energy as a fraction of capacity.") public double founderEnergy=.68;
    @Setting(group="Life & movement",min=10,max=1000,help="Base energy capacity.") public double capacityBase=100;
    @Setting(group="Life & movement",min=0,max=100,help="Energy capacity per unit body radius.") public double capacityPerSize=9;
    @Setting(group="Life & movement",max=1,help="Base metabolic energy spent per tick.") public double baseMetabolism=.018;
    @Setting(group="Life & movement",max=.1,help="Metabolic cost per squared body radius.") public double sizeCost=.00032;
    @Setting(group="Life & movement",max=1,help="Movement cost per squared velocity.") public double movementCost=.009;
    @Setting(group="Life & movement",max=.01,help="Sensing cost per unit vision range.") public double visionCost=.000045;
    @Setting(group="Life & movement",max=2,help="Minimum movement per tick even at minimum throttle.") public double minimumVelocity=.18;
    @Setting(group="Life & movement",min=.001,max=1,help="Turning rate, scaled inversely with body size.") public double turnRate=.13;
    @Setting(group="Life & movement",min=1,max=30,help="Reference body radius used to scale turning.") public double turnReferenceSize=8;
    @Setting(group="Sensing",min=1,max=2000,help="Obstacle ray range in world units.") public double obstacleSense=90;
    @Setting(group="Sensing",min=.01,max=3.14,help="Side obstacle-ray offset in radians.") public double obstacleAngle=.65;
    @Setting(group="Sensing",min=1,max=86400,help="Age at which the normalized age input reaches one, seconds.") public double ageSenseSeconds=120;
    @Setting(group="Sensing",min=1,max=10000,help="Age ticks per oscillator radian; 60 ticks is one simulated second.") public double oscillatorTicks=35;
    @Setting(group="Hunters",min=.05,max=5,help="Hunter metabolic cost multiplier.") public double hunterMetabolism=.8;
    @Setting(group="Hunters",min=.1,max=120,help="Seconds between captures or egg meals.") public double huntSeconds=2;
    @Setting(group="Hunters",max=1000,help="Base energy reward for a live capture.") public double captureEnergy=90;
    @Setting(group="Hunters",max=100,help="Additional capture energy per prey body radius.") public double capturePerSize=3;
    @Setting(group="Hunters",help="Fraction of death remains left after a capture.") public double captureRemains=.25;
    @Setting(group="Hunters",help="Fraction of an exposed prey egg's energy received by a hunter.") public double eggMealEfficiency=.7;
    @Setting(group="Genetics",help="Mutation probability per inherited weight/body trait.") public double mutation=.12;
    @Setting(group="Genetics",max=5,help="Founder weights are drawn uniformly between negative and positive this magnitude.") public double initialWeightRange=1.4;
    @Setting(group="Genetics",help="Initial recurrent weight multiplier; small values reduce hidden-neuron saturation.") public double initialRecurrence=.25;
    @Setting(group="Genetics",min=5,max=12,help="Minimum founder radius; must fit inherited size bounds.") public double founderMinimumSize=6;
    @Setting(group="Genetics",min=5,max=12,help="Maximum founder radius; must fit inherited size bounds.") public double founderMaximumSize=10;
    @Setting(group="Genetics",min=1.2,max=3.8,help="Minimum founder speed; must fit inherited speed bounds.") public double founderMinimumSpeed=1.8;
    @Setting(group="Genetics",min=1.2,max=3.8,help="Maximum founder speed; must fit inherited speed bounds.") public double founderMaximumSpeed=3.2;
    @Setting(group="Genetics",min=70,max=360,help="Minimum founder vision; must fit inherited vision bounds.") public double founderMinimumVision=140;
    @Setting(group="Genetics",min=70,max=360,help="Maximum founder vision; must fit inherited vision bounds.") public double founderMaximumVision=300;
    @Setting(group="Genetics",max=5,help="Maximum absolute perturbation to a mutated neural weight.") public double weightMutation=.5;
    @Setting(group="Genetics",max=7,help="Maximum size perturbation at mutation.") public double sizeMutation=1;
    @Setting(group="Genetics",max=2.6,help="Maximum speed perturbation at mutation.") public double speedMutation=.25;
    @Setting(group="Genetics",max=290,help="Maximum vision perturbation at mutation.") public double visionMutation=30;
    @Setting(group="Genetics",help="Diet mutation probability multiplier.") public double dietMutation=.4;
    @Setting(group="Genetics",min=5,max=12,help="Minimum inherited body radius.") public double minimumSize=5;
    @Setting(group="Genetics",min=5,max=12,help="Maximum inherited body radius.") public double maximumSize=12;
    @Setting(group="Genetics",min=1.2,max=3.8,help="Minimum inherited speed trait.") public double minimumSpeed=1.2;
    @Setting(group="Genetics",min=1.2,max=3.8,help="Maximum inherited speed trait.") public double maximumSpeed=3.8;
    @Setting(group="Genetics",min=70,max=360,help="Minimum inherited vision.") public double minimumVision=70;
    @Setting(group="Genetics",min=70,max=360,help="Maximum inherited vision.") public double maximumVision=360;

    public static List<Field> fields() { return Arrays.stream(WorldConfig.class.getFields()).filter(f->f.isAnnotationPresent(Setting.class)).toList(); }
    public WorldConfig copy() { WorldConfig c=new WorldConfig(); try {for(Field f:fields())f.set(c,f.get(this));} catch(ReflectiveOperationException e){throw new IllegalStateException(e);} return c; }
    public void validate() {
        try {for(Field f:fields()){Object v=f.get(this);Setting s=f.getAnnotation(Setting.class);if(v instanceof Number n&&(!Double.isFinite(n.doubleValue())||n.doubleValue()<s.min()||n.doubleValue()>s.max()))throw new IllegalArgumentException(label(f)+" must be between "+s.min()+" and "+s.max());if(v==null)throw new IllegalArgumentException(label(f)+" is required");}}
        catch(IllegalAccessException e){throw new IllegalStateException(e);}
        if(initialPrey+initialHunters>populationLimit||initialPrey>preyLimit||initialHunters>hunterLimit)throw new IllegalArgumentException("Configured founder counts must fit total and role limits, including a role currently disabled.");
        if(preyLimit>populationLimit||hunterLimit>populationLimit)throw new IllegalArgumentException("Role limits must fit the total population limit.");
        if(minimumSize>maximumSize||minimumSpeed>maximumSpeed||minimumVision>maximumVision)throw new IllegalArgumentException("Trait minimum must not exceed its maximum.");
        if(founderMinimumSize<minimumSize||founderMaximumSize>maximumSize||founderMinimumSize>founderMaximumSize||founderMinimumSpeed<minimumSpeed||founderMaximumSpeed>maximumSpeed||founderMinimumSpeed>founderMaximumSpeed||founderMinimumVision<minimumVision||founderMaximumVision>maximumVision||founderMinimumVision>founderMaximumVision)throw new IllegalArgumentException("Founder trait ranges must be ordered and fit the inherited trait bounds.");
        if(remainsLimit<foodLimit)throw new IllegalArgumentException("Remains limit must be at least the regular food limit.");
        if(initialPrey+(hunters?initialHunters:0)==0)throw new IllegalArgumentException("A new world needs at least one founder.");
    }
    public static String label(Field f){String s=f.getName().replaceAll("([a-z])([A-Z])","$1 $2");return Character.toUpperCase(s.charAt(0))+s.substring(1);}
    public static WorldConfig preset(String name) {WorldConfig c=new WorldConfig();switch(name){case "Classic","Classic (unchanged default)"->{}case "Continuous ecology"->c.hunters=false;case "Steady observation"->{c.hunters=false;c.abundance=100;c.mutation=.05;c.seasons=false;}case "Supported coexistence","Coexistence experiment"->{c.abundance=100;c.mutation=.05;c.seasons=false;c.initialSourceSites=8;}case "Selection exploration"->{c.mode=Simulation.Mode.GENERATIONS;c.pauseAtGenerationEnd=true;}default->throw new IllegalArgumentException("Unknown preset: "+name);}return c;}
    public static final String[] PRESETS={"Classic","Continuous ecology","Steady observation","Coexistence experiment","Selection exploration"};
    public WorldConfig scaledDensity(double factor){if(!Double.isFinite(factor)||factor<=0)throw new IllegalArgumentException("Scale must be positive");WorldConfig c=copy();double area=factor*factor;c.width=(int)Math.round(width*factor);c.height=(int)Math.round(height*factor);c.initialPrey=(int)Math.round(initialPrey*area);c.initialHunters=(int)Math.round(initialHunters*area);c.populationLimit=(int)Math.round(populationLimit*area);c.preyLimit=(int)Math.round(preyLimit*area);c.hunterLimit=(int)Math.round(hunterLimit*area);c.foodBase=(int)Math.round(foodBase*area);c.foodPerAbundance=foodPerAbundance*area;c.foodLimit=(int)Math.round(foodLimit*area);c.remainsLimit=(int)Math.round(remainsLimit*area);c.refillMultiplier=refillMultiplier*area;c.initialSourceSites=(int)Math.round(initialSourceSites*area);c.validate();return c;}
    public Properties properties(){Properties p=new Properties();p.setProperty("format","primordial-settings-v1");try{for(Field f:fields())p.setProperty(f.getName(),f.get(this).toString());}catch(IllegalAccessException e){throw new IllegalStateException(e);}return p;}
    public void write(Path path)throws Exception{try(var w=Files.newBufferedWriter(path)){properties().store(w,"Primordial advanced world settings");}}
    public void write(Path path,long seed)throws Exception{Properties p=properties();p.setProperty("seed",Long.toString(seed));try(var w=Files.newBufferedWriter(path)){p.store(w,"Primordial advanced world settings and replay seed");}}
    public static Long readSeed(Path path)throws Exception{Properties p=new Properties();try(var r=Files.newBufferedReader(path)){p.load(r);}return p.containsKey("seed")?Long.valueOf(p.getProperty("seed")):null;}
    public static WorldConfig read(Path path)throws Exception {Properties p=new Properties();try(var r=Files.newBufferedReader(path)){p.load(r);}if(!"primordial-settings-v1".equals(p.getProperty("format")))throw new IllegalArgumentException("Unsupported settings format");WorldConfig c=new WorldConfig();for(Field f:fields()){String v=p.getProperty(f.getName());if(v==null)continue;f.set(c,f.getType()==int.class?Integer.valueOf(v):f.getType()==double.class?Double.valueOf(v):f.getType()==boolean.class?parseBoolean(v):Simulation.Mode.valueOf(v));}c.validate();return c;}
    private static boolean parseBoolean(String v){if(!v.equals("true")&&!v.equals("false"))throw new IllegalArgumentException("Invalid boolean setting");return Boolean.parseBoolean(v);}
    public static int ticks(double seconds){return Math.max(1,(int)Math.round(seconds*60));}
}
