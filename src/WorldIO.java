import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Exact, versioned world snapshots, including Random's state and cached Gaussian sample. */
public final class WorldIO {
    private static final String MAGIC="PRIMORDIAL-WORLD";
    private static final int VERSION=2,MAX_BYTES=48*1024*1024;
    private WorldIO() {}
    public static byte[] encode(Simulation simulation)throws IOException {
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        DataOutputStream header=new DataOutputStream(bytes);header.writeUTF(MAGIC);header.writeInt(VERSION);
        try(ObjectOutputStream out=new ObjectOutputStream(bytes)){out.writeObject(simulation);}
        if(bytes.size()>MAX_BYTES)throw new IOException("World snapshot is too large.");return bytes.toByteArray();
    }
    public static Simulation decode(byte[] bytes)throws IOException {
        if(bytes.length>MAX_BYTES)throw new IOException("World snapshot exceeds the size limit.");
        ByteArrayInputStream input=new ByteArrayInputStream(bytes);DataInputStream header=new DataInputStream(input);
        if(!MAGIC.equals(header.readUTF()))throw new IOException("Unsupported or damaged world file.");int version=header.readInt();if(version!=1&&version!=VERSION)throw new IOException("Unsupported world version: "+version);
        try(ObjectInputStream in=new ObjectInputStream(input)){
            in.setObjectInputFilter(info->{
                if(info.depth()>60||info.references()>400000||info.streamBytes()>MAX_BYTES||info.arrayLength()>100000)return ObjectInputFilter.Status.REJECTED;
                Class<?> type=info.serialClass();if(type==null)return ObjectInputFilter.Status.UNDECIDED;
                while(type.isArray())type=type.getComponentType();
                if(type.isPrimitive())return ObjectInputFilter.Status.ALLOWED;
                String name=type.getName();
                boolean own=name.equals("Simulation")||name.startsWith("Simulation$")||name.equals("WorldConfig")||name.equals("SpeciesTracker")||name.equals("SpeciesTracker$Group");
                boolean library=Set.of("java.util.ArrayList","java.util.HashMap","java.util.LinkedHashMap","java.util.EnumMap","java.util.Random","java.util.CollSer","java.util.ImmutableCollections$ListN","java.util.ImmutableCollections$List12","java.util.Map$Entry","java.lang.Object","java.lang.Enum","java.lang.Number","java.lang.Integer","java.lang.Long","java.lang.Double","java.lang.Float","java.lang.String").contains(name);
                return own||library?ObjectInputFilter.Status.ALLOWED:ObjectInputFilter.Status.REJECTED;
            });
            Object value=in.readObject();if(!(value instanceof Simulation s))throw new IOException("This file does not contain a world.");
            if(version==2&&s.migratedOnRead)throw new IOException("Version 2 world is missing its configuration.");validate(s);return s;
        }catch(ClassNotFoundException|RuntimeException ex){throw new IOException("World data is invalid or incompatible.",ex);}
    }
    public static void write(Path path,Simulation simulation)throws IOException {
        byte[] bytes=encode(simulation);Path absolute=path.toAbsolutePath(),parent=absolute.getParent();
        Path temporary=Files.createTempFile(parent,".primordial-",".tmp");
        try{Files.write(temporary,bytes);try{Files.move(temporary,absolute,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}catch(AtomicMoveNotSupportedException ex){Files.move(temporary,absolute,StandardCopyOption.REPLACE_EXISTING);}}
        finally{Files.deleteIfExists(temporary);}
    }
    public static Simulation read(Path path)throws IOException {
        if(Files.size(path)>MAX_BYTES)throw new IOException("World snapshot exceeds the size limit.");return decode(Files.readAllBytes(path));
    }
    private static void require(boolean condition,String message)throws IOException {if(!condition)throw new IOException("Invalid world: "+message);}
    private static boolean finite(double... values){for(double value:values)if(!Double.isFinite(value))return false;return true;}
    private static void creature(Simulation.Creature c,Simulation s)throws IOException {
        require(c!=null&&c.id>0&&c.id<=s.nextId&&c.rootId>0&&c.parentId>=0&&c.parentId<c.id&&c.generation>0&&c.age>=0,"creature identity");
        require(finite(c.x,c.y,c.angle,c.energy,c.distance,c.spent,c.gained,c.bornAt)&&c.x>=0&&c.x<=s.width()&&c.y>=0&&c.y<=s.height(),"creature state");
        try{c.genome();}catch(RuntimeException ex){throw new IOException("Invalid creature genome",ex);}
        require(c.name!=null&&c.name.matches("[\\p{L}\\p{N} .'-]{1,100}")&&c.clan!=null&&c.clan.matches("[\\p{L}\\p{N} .'-]{1,100}"),"creature names");
        require(c.inputs!=null&&c.inputs.length==Simulation.INPUTS&&c.hidden!=null&&c.hidden.length==Simulation.HIDDEN&&c.outputs!=null&&c.outputs.length==Simulation.OUTPUTS,"brain dimensions");
        require(finite(c.inputs)&&finite(c.hidden)&&finite(c.outputs)&&c.trail!=null&&c.trail.size()<=24,"brain memory or trail");
        for(var point:c.trail)require(point!=null&&finite(point.x(),point.y()),"trail point");
    }
    static void validate(Simulation s)throws IOException {
        require(s.mode!=null&&s.abundance>=0&&s.abundance<=100&&s.mutationRate>=0&&s.mutationRate<=1&&s.elapsedTicks>=0&&s.tick>=0&&s.generation>0,"settings");
        require(s.config!=null,"configuration");s.config.validate();
        require(s.creatures!=null&&s.creatures.size()<=s.config.populationLimit&&s.eggs!=null&&s.eggs.size()<=s.config.populationLimit&&s.aliveCount()+s.eggs.size()<=s.config.populationLimit,"population");
        require(s.food!=null&&s.food.size()<=s.config.remainsLimit&&s.obstacles!=null&&s.obstacles.size()<=250&&s.nests!=null&&s.nests.size()<=50&&s.sources!=null&&s.sources.size()<=50,"habitat limits");
        require(s.history!=null&&s.history.size()<=240&&s.ancestry!=null&&s.ancestry.size()<=Simulation.MAX_ANCESTRY&&s.founders!=null&&s.founders.size()<=100000&&s.tracker!=null&&s.tracker.groups().size()<=SpeciesTracker.MAX_GROUPS,"history limits");
        require(finite(s.totalEnergyGained,s.totalEnergySpent,s.deadLifetime)&&s.totalBirths>=0&&s.totalDeaths>=0&&s.totalMeals>=0&&s.totalKills>=0&&s.totalEggsLaid>=0&&s.totalEggLoss>=0,"statistics");
        Set<Integer> ids=new HashSet<>();
        for(var c:s.creatures){creature(c,s);require(ids.add(c.id),"duplicate creature identity");}
        for(var egg:s.eggs){require(egg!=null&&egg.incubation>=0&&finite(egg.laidAt),"egg");creature(egg.embryo,s);require(ids.add(egg.embryo.id),"duplicate egg identity");}
        for(var f:s.food)require(f!=null&&f.type!=null&&finite(f.x,f.y,f.radius,f.angle,f.energy)&&f.radius>0&&f.radius<=20&&f.energy>=0&&f.x>=0&&f.x<=s.width()&&f.y>=0&&f.y<=s.height()&&f.age>=0,"resource");
        for(var rock:s.obstacles)require(rock!=null&&finite(rock.x(),rock.y(),rock.radius())&&rock.radius()>0&&rock.radius()<=100,"rock");
        for(var nest:s.nests)require(nest!=null&&finite(nest.x(),nest.y(),nest.radius())&&nest.radius()>0&&nest.radius()<=100,"nest");
        for(var source:s.sources)require(source!=null&&source.type()!=null&&finite(source.x(),source.y(),source.radius())&&source.radius()>0&&source.radius()<=100,"food source");
        for(var h:s.history)require(h!=null&&finite(h.seconds(),h.lifespan(),h.efficiency(),h.size(),h.speed(),h.vision()),"history sample");
        for(var a:s.ancestry.values())require(a!=null&&a.id>0&&a.id<=s.nextId&&a.parentId>=0&&a.parentId<a.id&&a.species!=null&&finite(a.bornAt,a.diedAt),"ancestry record");
        for(var g:s.founders.values())require(g!=null,"founder genome");
        for(var group:s.tracker.groups())require(group!=null&&group.reference()!=null&&group.id()>0&&finite(group.appearedAt()),"genetic group");
        s.validateInternals();
    }
}
