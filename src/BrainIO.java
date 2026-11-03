import java.io.*;
import java.nio.file.*;
import java.util.Properties;

/** Versioned genome files, including conversion of the original 7-input feed-forward brains. */
public final class BrainIO {
    private BrainIO() {}
    public static void write(Path path,Simulation.Genome genome)throws IOException {
        Properties p=new Properties();p.setProperty("format","primordial-java-v3");
        p.setProperty("hue",Float.toString(genome.hue()));p.setProperty("species",genome.species().name());
        p.setProperty("diet",genome.diet().name());
        p.setProperty("size",Double.toString(genome.size()));p.setProperty("speed",Double.toString(genome.speed()));p.setProperty("vision",Double.toString(genome.vision()));
        double[] weights=genome.weights();for(int i=0;i<weights.length;i++)p.setProperty("weight."+i,Double.toString(weights[i]));
        try(Writer w=Files.newBufferedWriter(path)){p.store(w,"Primordial recurrent brain and body");}
    }
    public static Simulation.Genome read(Path path)throws IOException {
        Properties p=new Properties();try(Reader r=Files.newBufferedReader(path)){p.load(r);}
        try {
            String version=p.getProperty("format");
            double[] weights=new double[Simulation.WEIGHT_COUNT];
            if("primordial-java-v1".equals(version)){
                int[] map={0,1,2,6,7,10,11};
                for(int h=0;h<8;h++){
                    weights[h*13]=number(p,"weight."+(h*8));
                    for(int i=0;i<7;i++)weights[h*13+1+map[i]]=number(p,"weight."+(h*8+1+i));
                }
                for(int i=0;i<18;i++)weights[Simulation.OUTPUT_OFFSET+i]=number(p,"weight."+(64+i));
                return new Simulation.Genome(weights,(float)number(p,"hue"));
            }
            if(!"primordial-java-v2".equals(version)&&!"primordial-java-v3".equals(version))throw new IllegalArgumentException("Unsupported brain file format.");
            for(int i=0;i<weights.length;i++)weights[i]=number(p,"weight."+i);
            return new Simulation.Genome(weights,(float)number(p,"hue"),number(p,"size"),number(p,"speed"),number(p,"vision"),Simulation.Species.valueOf(p.getProperty("species","")),Simulation.Diet.valueOf(p.getProperty("diet","GRAZER")));
        }catch(NullPointerException|NumberFormatException ex){throw new IllegalArgumentException("This brain file is incomplete or damaged.",ex);}
    }
    private static double number(Properties p,String key){return Double.parseDouble(p.getProperty(key));}
}
