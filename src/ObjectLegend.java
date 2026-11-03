import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;

/** Persistent color-and-shape key: identifiers never depend on color alone. */
public final class ObjectLegend extends JPanel {
    public ObjectLegend(){
        super(new GridLayout(2,4,14,8));setOpaque(false);setBorder(BorderFactory.createEmptyBorder(8,6,5,6));
        item("Prey",UiTheme.PREY,0,"Horizontal oval with a mint outline. Prey forage and can be caught by hunters.");
        item("Hunter",UiTheme.PREDATOR,1,"Coral arrow-shaped creature. Hunters catch prey and can scavenge carcasses.");
        item("Plants",UiTheme.PLANT,2,"Small lime circles: stationary food preferred by grazers.");
        item("Plankton",UiTheme.PLANKTON,3,"Small blue diamonds: moving food preferred by filter feeders.");
        item("Carrion",UiTheme.CARRION,4,"Purple squares: carcass fragments preferred by scavengers.");
        item("Eggs",UiTheme.EGG,5,"Cream vertical ovals with a hatching progress arc. Eggs take 6 seconds to hatch, or 3 inside nests.");
        item("Nests",UiTheme.NEST,6,"Dashed gold rings. Eggs inside hatch faster and are protected from hunters.");
        item("Rocks",UiTheme.ROCK,7,"Slate circles with bright edges. Solid obstacles block swimming; erase them to carve channels.");
        UiTheme.tip(this,"Sources are solid colored rings with a plus symbol. Creature fill can be recolored by family, genetic group, or diet; outlines always preserve role identity.");
    }
    private void item(String text,Color color,int shape,String help){JLabel label=new JLabel(text,new Marker(color,shape),SwingConstants.LEFT);label.setIconTextGap(7);label.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,10));UiTheme.tip(label,help);add(label);}
    private record Marker(Color color,int shape) implements Icon {
        public int getIconWidth(){return 19;}public int getIconHeight(){return 17;}
        public void paintIcon(Component c,Graphics graphics,int x,int y){Graphics2D g=(Graphics2D)graphics.create();EcosystemView.smooth(g);g.translate(x,y);g.setColor(color);g.setStroke(new BasicStroke(1.5f));
            // A dark backing keeps pale object swatches readable in either application theme.
            g.setColor(UiTheme.HABITAT);g.fillRoundRect(0,0,19,17,5,5);g.setColor(color);
            switch(shape){case 0->g.fillOval(3,5,13,7);case 1->{Path2D p=new Path2D.Double();p.moveTo(16,8);p.lineTo(3,3);p.lineTo(6,8);p.lineTo(3,13);p.closePath();g.fill(p);}case 2->g.fillOval(6,5,7,7);case 3->{Path2D p=new Path2D.Double();p.moveTo(9,3);p.lineTo(14,8);p.lineTo(9,13);p.lineTo(4,8);p.closePath();g.fill(p);}case 4->g.fillRect(5,4,9,9);case 5->g.fillOval(6,3,7,11);case 6->{g.setStroke(new BasicStroke(1.5f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND,1,new float[]{2,2},0));g.drawOval(3,2,13,13);}default->{g.setColor(new Color(65,86,81));g.fillOval(3,2,13,13);g.setColor(color);g.drawOval(3,2,13,13);}}
            g.dispose();}
    }
}
