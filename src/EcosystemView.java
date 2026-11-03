import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

/** Resizable Java2D habitat. Click to inspect; toggle Paint food to feed by dragging. */
public final class EcosystemView extends JPanel {
    private Simulation sim;
    public enum Tool {
        INSPECT, PLANTS, PLANKTON, CARRION, ROCKS, ERASE, NEST, PLANT_SOURCE, PLANKTON_SOURCE;
        @Override public String toString(){return switch(this){case INSPECT->"Inspect creatures";case PLANTS->"Paint plants";case PLANKTON->"Paint plankton";case CARRION->"Paint carrion";case ROCKS->"Draw rocks";case ERASE->"Erase habitat";case NEST->"Place nest";case PLANT_SOURCE->"Plant source";case PLANKTON_SOURCE->"Plankton source";};}
    }
    public enum Colors {
        OBJECT_TYPES, FAMILIES, GENETIC_GROUPS, DIETS;
        @Override public String toString(){return switch(this){case OBJECT_TYPES->"Object colors";case FAMILIES->"Family colors";case GENETIC_GROUPS->"Genetic groups";case DIETS->"Diet colors";};}
    }
    public Tool tool=Tool.INSPECT;
    public Colors colors=Colors.OBJECT_TYPES;
    public double brushRadius=25;
    private double lastEditX=-1000,lastEditY=-1000;
    private java.awt.Point cursor;
    public void setSimulation(Simulation simulation){this.sim=simulation;selected=null;repaint();}
    public Simulation.Creature selected;
    public boolean painting, trails, showSenses;
    public EcosystemView(Simulation sim) {
        this.sim=sim;setBackground(UiTheme.HABITAT);setFocusable(true);
        setToolTipText("Click a creature to inspect it. Choose an editor tool to change the habitat.");
        MouseAdapter mouse=new MouseAdapter(){
            @Override public void mousePressed(MouseEvent e){requestFocusInWindow();lastEditX=lastEditY=-1000;interact(e);}
            @Override public void mouseDragged(MouseEvent e){cursor=e.getPoint();if(painting||tool!=Tool.INSPECT)interact(e);}
            @Override public void mouseMoved(MouseEvent e){cursor=e.getPoint();if(tool!=Tool.INSPECT)repaint();}
            @Override public void mouseExited(MouseEvent e){cursor=null;repaint();}
        };
        addMouseListener(mouse);addMouseMotionListener(mouse);
        getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke("RIGHT"),"selectNext");
        getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke("ENTER"),"selectNext");
        getActionMap().put("selectNext",new AbstractAction(){
            @Override public void actionPerformed(ActionEvent e){
                var living=sim.creatures.stream().filter(c->c.alive).toList();
                if(!living.isEmpty())selected=living.get((living.indexOf(selected)+1)%living.size());repaint();
            }
        });
    }
    private double scale(){return Math.max(.01,Math.min(getWidth()/(double)sim.width(),(getHeight()-50)/(double)sim.height()));}
    private double offsetX(){return (getWidth()-sim.width()*scale())/2;}
    private double offsetY(){return 25+(getHeight()-50-sim.height()*scale())/2;}
    private void interact(MouseEvent e){
        double x=(e.getX()-offsetX())/scale(),y=(e.getY()-offsetY())/scale();
        if(x<0||x>sim.width()||y<0||y>sim.height())return;
        Tool active=painting?Tool.PLANTS:tool;
        if(active!=Tool.INSPECT){
            double spacing=active==Tool.ROCKS?brushRadius*.8:active==Tool.NEST||active==Tool.PLANT_SOURCE||active==Tool.PLANKTON_SOURCE?45:8;
            if(Math.hypot(x-lastEditX,y-lastEditY)<spacing)return;
            lastEditX=x;lastEditY=y;
            switch(active){
                case PLANTS->sim.paintFood(x,y,Simulation.Resource.PLANT);
                case PLANKTON->sim.paintFood(x,y,Simulation.Resource.PLANKTON);
                case CARRION->sim.paintFood(x,y,Simulation.Resource.CARRION);
                case ROCKS->sim.addRock(x,y,brushRadius);
                case ERASE->sim.eraseHabitat(x,y,brushRadius);
                case NEST->sim.addNest(x,y);
                case PLANT_SOURCE->sim.addSource(x,y,Simulation.Resource.PLANT);
                case PLANKTON_SOURCE->sim.addSource(x,y,Simulation.Resource.PLANKTON);
                default->{}
            }
        }else {
            double closest=40;selected=null;
            for(var c:sim.creatures){double d=Math.hypot(c.x-x,c.y-y);if(c.alive&&d<closest){closest=d;selected=c;}}
        }
        repaint();
    }
    static void smooth(Graphics2D g){
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }
    @Override protected void paintComponent(Graphics graphics){
        Graphics2D background=(Graphics2D)graphics.create();smooth(background);background.setColor(UiTheme.HABITAT);background.fillRoundRect(0,0,getWidth(),getHeight(),16,16);background.dispose();
        Graphics2D g=(Graphics2D)graphics.create();smooth(g);g.clipRect(0,0,getWidth(),getHeight());
        g.translate(offsetX(),offsetY());g.scale(scale(),scale());
        g.setPaint(new RadialGradientPaint(sim.width()/2f,sim.height()/2f,Math.max(sim.width(),sim.height())*.6f,new float[]{0,1},new Color[]{new Color(27,56,40),UiTheme.HABITAT}));
        g.fillRect(0,0,sim.width(),sim.height());
        g.setColor(new Color(105,139,110,48));
        for(int x=20;x<sim.width();x+=32)for(int y=20;y<sim.height();y+=32)g.fill(new Ellipse2D.Double(x,y,1.4,1.4));
        g.setColor(new Color(110,147,116,95));g.drawRect(8,8,sim.width()-16,sim.height()-16);
        Simulation.Point patch=sim.foodPatch();
        g.setColor(new Color(153,184,109,9));g.fill(new Ellipse2D.Double(patch.x()-210,patch.y()-190,420,380));
        for(var nest:sim.nests){
            g.setColor(UiTheme.alpha(UiTheme.NEST,22));g.fill(new Ellipse2D.Double(nest.x()-nest.radius(),nest.y()-nest.radius(),nest.radius()*2,nest.radius()*2));
            g.setColor(UiTheme.alpha(UiTheme.NEST,190));g.setStroke(new BasicStroke(1.5f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND,1,new float[]{5,6},0));g.draw(new Ellipse2D.Double(nest.x()-nest.radius(),nest.y()-nest.radius(),nest.radius()*2,nest.radius()*2));
        }
        for(var source:sim.sources){
            g.setColor(UiTheme.alpha(source.type()==Simulation.Resource.PLANT?UiTheme.PLANT:UiTheme.PLANKTON,190));g.setStroke(new BasicStroke(2));g.draw(new Ellipse2D.Double(source.x()-source.radius(),source.y()-source.radius(),source.radius()*2,source.radius()*2));
            g.draw(new Line2D.Double(source.x()-5,source.y(),source.x()+5,source.y()));g.draw(new Line2D.Double(source.x(),source.y()-5,source.x(),source.y()+5));
        }
        for(var rock:sim.obstacles){
            g.setColor(new Color(8,28,22,120));g.fill(new Ellipse2D.Double(rock.x()-rock.radius()+8,rock.y()-rock.radius()+10,rock.radius()*2,rock.radius()*2));
            g.setColor(new Color(52,73,68));g.fill(new Ellipse2D.Double(rock.x()-rock.radius(),rock.y()-rock.radius(),rock.radius()*2,rock.radius()*2));
            g.setColor(UiTheme.alpha(UiTheme.ROCK,220));g.setStroke(new BasicStroke(2));g.draw(new Ellipse2D.Double(rock.x()-rock.radius(),rock.y()-rock.radius(),rock.radius()*2,rock.radius()*2));
        }
        for(var f:sim.food){
            Color color=f.type==Simulation.Resource.PLANT?UiTheme.PLANT:f.type==Simulation.Resource.PLANKTON?UiTheme.PLANKTON:UiTheme.CARRION;
            double r=Math.max(f.radius(),1.9/scale());g.setColor(UiTheme.alpha(color,22));g.fill(new Ellipse2D.Double(f.x()-r*2.6,f.y()-r*2.6,r*5.2,r*5.2));
            g.setColor(color);
            if(f.type==Simulation.Resource.CARRION)g.fill(new RoundRectangle2D.Double(f.x()-r,f.y()-r,r*2,r*2,1,1));
            else if(f.type==Simulation.Resource.PLANKTON){Path2D diamond=new Path2D.Double();diamond.moveTo(f.x(),f.y()-r*1.3);diamond.lineTo(f.x()+r*1.3,f.y());diamond.lineTo(f.x(),f.y()+r*1.3);diamond.lineTo(f.x()-r*1.3,f.y());diamond.closePath();g.fill(diamond);}
            else g.fill(new Ellipse2D.Double(f.x()-r,f.y()-r,r*2,r*2));
        }
        for(var egg:sim.eggs){
            var embryo=egg.embryo;g.setColor(UiTheme.EGG);g.fill(new Ellipse2D.Double(embryo.x-5,embryo.y-7,10,14));
            g.setColor(UiTheme.PREY);g.setStroke(new BasicStroke(2));g.draw(new Arc2D.Double(embryo.x-9,embryo.y-10,18,20,90,-360.0*egg.incubation/WorldConfig.ticks(sim.config.hatchSeconds),Arc2D.OPEN));
        }
        for(var c:sim.creatures){
            if(!c.alive)continue;
            float hue=colors==Colors.FAMILIES?c.hue:colors==Colors.GENETIC_GROUPS?(float)((c.clusterId*.61803398875)%1):c.diet==Simulation.Diet.GRAZER?.25f:c.diet==Simulation.Diet.FILTER_FEEDER?.52f:.06f;
            Color role=c.species==Simulation.Species.PREDATOR?UiTheme.PREDATOR:UiTheme.PREY;
            Color color=colors==Colors.OBJECT_TYPES?role:colors==Colors.DIETS?(c.diet==Simulation.Diet.GRAZER?UiTheme.PLANT:c.diet==Simulation.Diet.FILTER_FEEDER?UiTheme.PLANKTON:UiTheme.CARRION):Color.getHSBColor(hue,.5f,.94f);
            if(trails&&c.trail.size()>1){
                g.setColor(new Color(color.getRed(),color.getGreen(),color.getBlue(),100));g.setStroke(new BasicStroke(2));
                Path2D path=new Path2D.Double();path.moveTo(c.trail.get(0).x(),c.trail.get(0).y());
                for(var p:c.trail)path.lineTo(p.x(),p.y());g.draw(path);
            }
            if(selected==c&&showSenses){
                g.setColor(new Color(183,210,145,22));g.fill(new Ellipse2D.Double(c.x-c.vision,c.y-c.vision,c.vision*2,c.vision*2));
                g.setColor(new Color(183,210,145,65));g.setStroke(new BasicStroke(1));g.draw(new Ellipse2D.Double(c.x-c.vision,c.y-c.vision,c.vision*2,c.vision*2));
                for(int i=0;i<3;i++){double a=c.angle+(i==0?0:i==1?-sim.config.obstacleAngle:sim.config.obstacleAngle);double length=sim.config.obstacleSense*(1-c.inputs[7+i]);g.setColor(new Color(200,225,155,170));g.draw(new Line2D.Double(c.x,c.y,c.x+Math.cos(a)*length,c.y+Math.sin(a)*length));}
                g.setColor(new Color(207,229,152,160));double forward=c.inputs[0],side=c.inputs[1];
                if(c.inputs[2]>0)g.draw(new Line2D.Double(c.x,c.y,c.x+(forward*Math.cos(c.angle)-side*Math.sin(c.angle))*c.vision*(1-c.inputs[2]),c.y+(forward*Math.sin(c.angle)+side*Math.cos(c.angle))*c.vision*(1-c.inputs[2])));
                double dangerForward=c.inputs[3],dangerSide=c.inputs[4];g.setColor(new Color(236,141,99,180));
                if(c.inputs[5]>0)g.draw(new Line2D.Double(c.x,c.y,c.x+(dangerForward*Math.cos(c.angle)-dangerSide*Math.sin(c.angle))*c.vision*(1-c.inputs[5]),c.y+(dangerForward*Math.sin(c.angle)+dangerSide*Math.cos(c.angle))*c.vision*(1-c.inputs[5])));
            }
            if(selected==c){
                g.setColor(new Color(246,255,222));g.setStroke(new BasicStroke(1.5f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND,1,new float[]{4,5},0));
                g.draw(new Ellipse2D.Double(c.x-23,c.y-23,46,46));g.setFont(new Font(Font.SANS_SERIF,Font.BOLD,Math.max(13,(int)(11/scale()))));String tag=c.name+" · C"+c.clusterId;double tx=c.x+28,ty=c.y-15;g.setColor(new Color(8,24,18,225));g.fill(new RoundRectangle2D.Double(tx-5,ty-g.getFontMetrics().getAscent()-4,g.getFontMetrics().stringWidth(tag)+10,g.getFontMetrics().getHeight()+8,8,8));g.setColor(new Color(246,255,222));g.drawString(tag,(float)tx,(float)ty);
            }
            Graphics2D creature=(Graphics2D)g.create();creature.translate(c.x,c.y);creature.rotate(c.angle);
            creature.setStroke(new BasicStroke(1.4f));creature.setColor(new Color(color.getRed(),color.getGreen(),color.getBlue(),150));
            creature.draw(new QuadCurve2D.Double(-5,0,-14,Math.sin(c.age*.27)*6,-22,Math.sin(c.age*.27+1)*5));
            creature.setColor(new Color(color.getRed(),color.getGreen(),color.getBlue(),22));creature.fill(new Ellipse2D.Double(-18,-14,36,28));
            creature.setColor(color);
            Shape body;
            if(c.species==Simulation.Species.PREDATOR){
                Path2D hunter=new Path2D.Double();hunter.moveTo(c.size+3,0);hunter.lineTo(-c.size,-c.size*.8);hunter.lineTo(-c.size*.55,0);hunter.lineTo(-c.size,c.size*.8);hunter.closePath();body=hunter;
            }else body=new Ellipse2D.Double(-c.size,-c.size*.62,c.size*2,c.size*1.24);
            creature.fill(body);creature.setStroke(new BasicStroke((float)(1.1/scale())));creature.setColor(role);creature.draw(body);
            creature.setColor(new Color(240,247,223,200));creature.fill(new Ellipse2D.Double(1.4,-2.6,3.2,3.2));creature.dispose();
        }
        g.dispose();g=(Graphics2D)graphics.create();smooth(g);g.setFont(new Font("SansSerif",Font.PLAIN,10));g.setColor(UiTheme.HABITAT_TEXT);
        g.drawString("THE PETRI DISH  ·  "+sim.season().toUpperCase()+"  ·  G"+sim.generation,18,18);g.drawString("LIFE, FINDING A WAY.",18,getHeight()-12);
        String hint=painting||tool!=Tool.INSPECT?"Tool: "+(painting?Tool.PLANTS:tool)+" · click or drag":"Click a swimmer to meet it";
        if(getWidth()>650)g.drawString(hint,getWidth()-g.getFontMetrics().stringWidth(hint)-18,18);
        if(cursor!=null&&tool!=Tool.INSPECT){double radius=(tool==Tool.ROCKS||tool==Tool.ERASE?brushRadius:tool==Tool.NEST?50:tool==Tool.PLANT_SOURCE||tool==Tool.PLANKTON_SOURCE?40:24)*scale();g.setColor(UiTheme.alpha(tool==Tool.ERASE?UiTheme.PREDATOR:UiTheme.EGG,100));g.setStroke(new BasicStroke(1.3f));g.draw(new Ellipse2D.Double(cursor.x-radius,cursor.y-radius,radius*2,radius*2));}
        g.dispose();
    }
    @Override public String getToolTipText(MouseEvent event){
        double x=(event.getX()-offsetX())/scale(),y=(event.getY()-offsetY())/scale();
        if(tool!=Tool.INSPECT)return "<html><body style='width:270px'>"+UiHelp.tool(tool)+"</body></html>";
        for(var c:sim.creatures)if(c.alive&&Math.hypot(c.x-x,c.y-y)<Math.max(c.size+7,10/scale()))
            return String.format("<html><b>%s</b> · %s<br>%s · ID %d · G%d · C%d<br>Diet: %s · Energy %.0f / %.0f<br>Size %.1f · Speed %.2f · Vision %.0f<br>Click to inspect brain and family.</html>",c.name,c.clan,c.species==Simulation.Species.PREDATOR?"Hunter":"Prey",c.id,c.generation,c.clusterId,c.diet,Math.max(0,c.energy),c.capacity(),c.size,c.speed,c.vision);
        for(var egg:sim.eggs)if(Math.hypot(egg.embryo.x-x,egg.embryo.y-y)<12/scale())return "<html><b>Egg</b> · Parent #"+egg.embryo.parentId+"<br>Incubation "+Math.min(100,egg.incubation*100/WorldConfig.ticks(sim.config.hatchSeconds))+"% · "+(sim.inNest(x,y)?"Protected by a nest":"Exposed to hunters")+"</html>";
        for(var f:sim.food)if(Math.hypot(f.x-x,f.y-y)<Math.max(7,6/scale()))return switch(f.type){case PLANT->"Lime circle: stationary plant. Grazers digest it best.";case PLANKTON->"Blue diamond: drifting plankton. Filter feeders digest it best.";case CARRION->"Purple square: carrion. Scavengers digest it best; it decays in 30 seconds.";};
        for(var rock:sim.obstacles)if(Math.hypot(rock.x()-x,rock.y()-y)<rock.radius())return "Solid rock blocks movement. Use Erase habitat to remove it.";
        for(var nest:sim.nests)if(Math.hypot(nest.x()-x,nest.y()-y)<nest.radius())return "Gold nest: eggs hatch twice as fast and are protected from hunters. Adults are not protected.";
        for(var source:sim.sources)if(Math.hypot(source.x()-x,source.y()-y)<source.radius())return source.type()==Simulation.Resource.PLANT?"Plant source: continuously grows plants nearby.":"Plankton source: continuously releases drifting plankton nearby.";
        return "Click a swimmer to inspect it, or choose an editor tool. Enter cycles through living creatures.";
    }

}
