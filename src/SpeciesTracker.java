import java.io.Serializable;
import java.util.*;

/** Reference-based genetic groups, not a biological species definition. References never drift. */
public final class SpeciesTracker implements Serializable {
    private static final long serialVersionUID=1L;
    public static final double DIVERGENCE_THRESHOLD=.48;
    public static final int MAX_GROUPS=512;
    public record Group(int id,int parentGroup,double appearedAt,Simulation.Genome reference) implements Serializable {}
    private final List<Group> groups=new ArrayList<>();
    public List<Group> groups(){return Collections.unmodifiableList(groups);}
    public void clear(){groups.clear();}
    public int assign(Simulation.Creature c,double seconds){
        Simulation.Genome genome=c.genome();Group nearest=null;double best=Double.POSITIVE_INFINITY;
        for(Group group:groups){double distance=distance(genome,group.reference);if(distance<best){best=distance;nearest=group;}}
        if((nearest==null||best>DIVERGENCE_THRESHOLD)&&groups.stream().filter(g->g.reference.species()==c.species).count()<MAX_GROUPS/2){
            int parent=c.clusterId;
            Group group=new Group(groups.size()+1,parent,seconds,genome);groups.add(group);return group.id;
        }
        return nearest==null?0:nearest.id;
    }
    public static double distance(Simulation.Genome a,Simulation.Genome b){
        if(a.species()!=b.species())return Double.POSITIVE_INFINITY;
        double sum=0;for(int i=0;i<Simulation.WEIGHT_COUNT;i++){double delta=a.weightAt(i)-b.weightAt(i);sum+=delta*delta;}
        double body=(Math.abs(a.size()-b.size())/7+Math.abs(a.speed()-b.speed())/2.6+Math.abs(a.vision()-b.vision())/290)/3;
        return Math.sqrt(sum/Simulation.WEIGHT_COUNT)*.65+body*.25+(a.diet()==b.diet()?0:.22);
    }
    public Map<Integer,Integer> livingCounts(Simulation simulation){
        Map<Integer,Integer> result=new TreeMap<>();for(var c:simulation.creatures)if(c.alive)result.merge(c.clusterId,1,Integer::sum);return result;
    }
}
