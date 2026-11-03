import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.Arrays;

/** Render real Swing components offscreen for visual QA; no native-window automation. */
public final class UiPreview {
    private UiPreview() {}
    public static void render(String directory){
        try{SwingUtilities.invokeAndWait(()->{
            try{
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());UiTheme.install(false);
                Main app=new Main(false);for(int i=0;i<300;i++)app.sim.step();app.world.selected=app.sim.creatures.stream().filter(c->c.alive).findFirst().orElse(null);app.world.showSenses=true;selectSenses(app.root);app.refresh();
                byte[] state=WorldIO.encode(app.sim);Path folder=Path.of(directory);Files.createDirectories(folder);
                for(boolean dark:new boolean[]{false,true}){
                    UiTheme.install(dark);UiTheme.apply(app.root);
                    for(int[] size:new int[][]{{1280,1000},{1050,760}}){
                        app.root.setSize(size[0],size[1]);for(int i=0;i<5;i++)layout(app.root);
                        BufferedImage image=new BufferedImage(size[0],size[1],BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();EcosystemView.smooth(g);app.root.printAll(g);g.dispose();
                        Path output=folder.resolve("primordial-"+(dark?"dark":"light")+"-"+size[0]+".png");ImageIO.write(image,"png",output.toFile());System.out.println(output);
                    }
                }
                app.root.setSize(1280,1000);
                for(JTabbedPane tabs:tabsIn(app.root)){
                    if(tabs.getTabCount()==5){for(int i=1;i<tabs.getTabCount();i++){tabs.setSelectedIndex(i);for(int j=0;j<4;j++)layout(app.root);write(app.root,folder.resolve("primordial-dark-"+tabs.getTitleAt(i).toLowerCase()+".png"));}tabs.setSelectedIndex(0);}
                    else if(tabs.getTabCount()==2){tabs.setSelectedIndex(1);for(int j=0;j<4;j++)layout(app.root);write(app.root,folder.resolve("primordial-dark-editor.png"));tabs.setSelectedIndex(0);}
                }
                if(!Arrays.equals(state,WorldIO.encode(app.sim)))throw new AssertionError("Theme changes altered simulation state");
                if(contrast(UiTheme.palette().text(),UiTheme.palette().surface())<7)throw new AssertionError("Insufficient dark-theme text contrast");
                checkHelp(app.root);
                Simulation familyWorld=new Simulation(WorldConfig.preset("Continuous ecology"),73);for(int i=0;i<3000;i++)familyWorld.step();FamilyPanel family=new FamilyPanel(()->familyWorld,c->{});familyWorld.ancestry.values().stream().max(java.util.Comparator.comparingInt(a->a.children)).ifPresent(a->family.focus(a.id));
                java.util.Map<String,JComponent> views=new java.util.LinkedHashMap<>();views.put("advanced-settings",new WorldSettingsPanel(new WorldConfig(),42));views.put("family-window",family);views.put("experiment-window",new ExperimentPanel(()->app.sim.settings()));views.put("parallel-window",new BatchPanel(()->app.sim,s->{}));views.put("study-window",new EvolutionStudyPanel(()->app.sim));
                for(boolean dark:new boolean[]{false,true}){UiTheme.install(dark);for(var entry:views.entrySet()){JComponent component=entry.getValue();UiTheme.apply(component);component.setSize(1100,780);for(int i=0;i<5;i++)layout(component);write(component,folder.resolve("primordial-"+(dark?"dark":"light")+"-"+entry.getKey()+".png"));checkHelp(component);}}
                GettingStartedPanel tour=new GettingStartedPanel(new GettingStartedPanel.Actions(()->{},()->{},()->{},()->{},()->{},()->{},()->{}),false,b->{});
                for(boolean dark:new boolean[]{false,true}){UiTheme.install(dark);UiTheme.apply(tour);for(int page=0;page<6;page++){tour.selectPage(page);for(int[] size:new int[][]{{880,640},{720,480}}){tour.setSize(size[0],size[1]);for(int i=0;i<6;i++)layout(tour);write(tour,folder.resolve("primordial-"+(dark?"dark":"light")+"-getting-started-"+(page+1)+"-"+size[0]+".png"));}checkHelp(tour);}}
                if(!Arrays.equals(state,WorldIO.encode(app.sim)))throw new AssertionError("Guide renders altered simulation state");
                app.timer.stop();System.out.println("PASS: both themes render at desktop/minimum sizes; advanced settings, data windows and every getting-started page render; control help and model state preserved.");
            }catch(Exception ex){throw new RuntimeException(ex);}
        });}catch(Exception ex){throw new RuntimeException("UI preview failed",ex);}
    }
    private static void selectSenses(Container root){for(Component c:root.getComponents()){if(c instanceof JCheckBox b&&"Senses".equals(b.getText()))b.setSelected(true);if(c instanceof Container child)selectSenses(child);}}
    private static java.util.List<JTabbedPane> tabsIn(Container root){java.util.List<JTabbedPane> tabs=new java.util.ArrayList<>();if(root instanceof JTabbedPane p)tabs.add(p);for(Component c:root.getComponents())if(c instanceof Container child)tabs.addAll(tabsIn(child));return tabs;}
    private static void write(JComponent root,Path path)throws Exception {BufferedImage image=new BufferedImage(root.getWidth(),root.getHeight(),BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();EcosystemView.smooth(g);root.printAll(g);g.dispose();ImageIO.write(image,"png",path.toFile());}
    private static void layout(Container container){container.doLayout();for(Component c:container.getComponents())if(c instanceof Container child)layout(child);}
    private static void checkHelp(Component component){
        if(component instanceof AbstractButton b&&b.isVisible()&&b.getText()!=null&&!b.getText().isBlank()&&!(b instanceof javax.swing.plaf.basic.BasicArrowButton)&&b.getToolTipText()==null)
            throw new AssertionError("Missing help: "+b.getText());
        if(component instanceof Container container)for(Component child:container.getComponents())checkHelp(child);
    }
    private static double contrast(Color a,Color b){double x=luminance(a),y=luminance(b);return (Math.max(x,y)+.05)/(Math.min(x,y)+.05);}
    private static double luminance(Color c){double[] v={c.getRed()/255.0,c.getGreen()/255.0,c.getBlue()/255.0};for(int i=0;i<3;i++)v[i]=v[i]<=.04045?v[i]/12.92:Math.pow((v[i]+.055)/1.055,2.4);return v[0]*.2126+v[1]*.7152+v[2]*.0722;}
}
