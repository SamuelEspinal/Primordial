import javax.swing.*;
import java.awt.*;
import java.util.function.IntFunction;

/** Plain-language explanations shared by menus, tooltips, and accessible descriptions. */
public final class UiHelp {
    private UiHelp() {}
    public static String tool(EcosystemView.Tool tool){return switch(tool){
        case INSPECT->"Click a creature to inspect its brain, energy, body traits, and ancestry. Enter or Right Arrow selects the next living creature.";
        case PLANTS->"Click or drag to scatter stationary green plants. Grazers gain the most energy from them.";
        case PLANKTON->"Click or drag to add blue, drifting plankton. Filter feeders digest it most efficiently.";
        case CARRION->"Add purple carcass fragments for scavengers. These food pieces decay after 30 simulated seconds.";
        case ROCKS->"Draw solid rock barriers with the brush. Rocks cannot be placed over creatures or eggs. Pause first for easier editing.";
        case ERASE->"Remove rocks, nests, and food sources touched by the brush. Use this to carve a channel through a barrier.";
        case NEST->"Place a gold nest. Eggs inside hatch twice as fast and are protected from predators; adults are not protected.";
        case PLANT_SOURCE->"Place a persistent plant source. It produces green food nearby every half simulated second when food capacity allows.";
        case PLANKTON_SOURCE->"Place a persistent plankton source. It releases drifting blue food nearby every half simulated second.";
    };}
    public static String colors(EcosystemView.Colors colors){return switch(colors){
        case OBJECT_TYPES->"Use stable colors: mint prey, coral hunters, lime plants, blue plankton, purple carrion, and cream eggs. Shape also identifies each object.";
        case FAMILIES->"Creature fill follows its founding family. Mint outlines still identify prey and coral outlines identify hunters. Resource colors stay fixed.";
        case GENETIC_GROUPS->"Creature fill identifies its genetic cluster. These are approximate groups based on brains, bodies, and diets, not a biological species definition.";
        case DIETS->"Creature fill shows diet: lime grazers, blue filter feeders, purple scavengers. The outline and shape still identify prey versus hunter.";
    };}
    public static String metric(int i){return switch(i){
        case 0->"Number of living prey (mint) and hunters (coral) at each sample. Eggs are counted separately in the census.";
        case 1->"Average lifespan of all creatures whose lives have ended. Generation selection also ends living lives; this is a lifetime cumulative average.";
        case 2->"Total energy gained from meals divided by energy spent on metabolism and incubation. Higher values indicate more energy recovered per unit spent.";
        case 3->"Mean body radius among living creatures. Larger bodies can store more energy but cost more to maintain.";
        case 4->"Mean inherited maximum swimming speed. Actual speed is controlled by the brain; faster movement consumes more energy.";
        case 5->"Mean inherited sensing range. Better vision detects targets farther away, but consumes more energy.";
        default->"Cumulative births (including the initial population) and deaths. Egg laying is not counted as a birth until the egg hatches.";
    };}
    public static <T> void choices(JComboBox<T> combo,IntFunction<String> explanation){
        combo.setRenderer(new DefaultListCellRenderer(){
            @Override public Component getListCellRendererComponent(JList<?> list,Object value,int index,boolean selected,boolean focus){
                JLabel label=(JLabel)super.getListCellRendererComponent(list,value,index,selected,focus);label.setBorder(BorderFactory.createEmptyBorder(8,9,8,9));
                var p=UiTheme.palette();label.setBackground(selected?p.selected():p.surface());label.setForeground(p.text());
                int i=index<0?combo.getSelectedIndex():index;if(i>=0)label.setToolTipText("<html><body style='width:270px'>"+explanation.apply(i)+"</body></html>");return label;
            }
        });
        Runnable update=()->{if(combo.getSelectedIndex()>=0)UiTheme.tip(combo,explanation.apply(combo.getSelectedIndex()));};combo.addActionListener(e->update.run());update.run();
    }
}
