import java.util.Random;

/** Paired trials, separate from the live simulation. Results are measurements, not guarantees. */
public final class Benchmark {
    public record Result(double meals,double lifespan,double efficiency) {}
    public record Comparison(Result current,Result founder,int trials,int seconds) {
        public String description(){
            return String.format("""
                    CURRENT CREATURE vs GENERATION-1 ANCESTOR

                    Mean over %d paired trials of %d simulated seconds:
                                               Current      Ancestor
                    Meals / captures          %7.2f      %7.2f
                    Survival (seconds)         %7.2f      %7.2f
                    Energy gained / spent      %7.2f      %7.2f

                    Same seeds, starting positions, food, seasons, and rocks.
                    Predators face identical initial prey populations.
                    Reproduction is disabled in both trials. Memory starts fresh.
                    Bodies and brains are evaluated together.

                    This measures these two genomes in controlled conditions;
                    it does not establish improvement across the whole population.
                    """,trials,seconds,current.meals,founder.meals,current.lifespan,founder.lifespan,current.efficiency,founder.efficiency);
        }
    }
    private Benchmark() {}
    public static Comparison compare(Simulation.Genome current,Simulation.Genome founder,int abundance,boolean seasons){
        if(current.species()!=founder.species())throw new IllegalArgumentException("Comparison requires matching species.");
        int trials=5,seconds=45;
        return new Comparison(evaluate(current,abundance,seasons,trials,seconds),evaluate(founder,abundance,seasons,trials,seconds),trials,seconds);
    }
    static Result evaluate(Simulation.Genome genome,int abundance,boolean seasons,int trials,int seconds){
        double meals=0,life=0,efficiency=0;
        for(int trial=0;trial<trials;trial++){
            Simulation s=new Simulation(new Random(7301+trial));s.abundance=abundance;s.seasons=seasons;
            Simulation.Creature focal=s.prepareTrial(genome,7301+trial);
            for(int t=0;t<seconds*60&&focal.alive;t++)s.step();
            meals+=focal.eaten;life+=focal.age/60.0;efficiency+=focal.spent==0?0:focal.gained/focal.spent;
        }
        return new Result(meals/trials,life/trials,efficiency/trials);
    }
}
