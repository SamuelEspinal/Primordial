import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.io.File;
import java.util.*;
import java.util.List;

/**
 * Local Java desktop entry point (JDK 17+; no external libraries).
 * Run Main in IntelliJ or compile every Java file in src and launch Main.
 * Pass --self-test to verify the model without opening a window.
 *
 * Inspect: select swimmers, view brains, save/load complete genomes and exact world snapshots.
 * Dashboard: life statistics and five paired trials against a founder.
 * Family: a bounded, explorable ancestor/descendant tree.
 * Bottom graph: population, completed lifespan, efficiency, traits, births/deaths.
 * Species: reference-based genetic groups; Experiments: isolated paired seeded worlds.
 *
 * Continuous reproduction is the default. The mode selector also supports
 * the original synchronized generation selection. The simulation uses fixed
 * time steps; benchmark work runs in a separate SwingWorker.
 */
public final class Main {
    static final Color BACKGROUND=new Color(245,245,239),INK=new Color(35,62,53),MUTED=new Color(120,130,121),GREEN=new Color(52,86,68);
    Simulation sim=new Simulation();
    final JFrame frame;
    final JPanel root;
    final EcosystemView world=new EcosystemView(sim);
    final BrainView brain=new BrainView();
    final HistoryView history=new HistoryView();
    final JLabel census=new JLabel(),clock=new JLabel(),creatureInfo=new JLabel(),dashboard=new JLabel(),comparisonStatus=new JLabel("Five paired 45-second trials; runs in the background.");
    final JButton pause=new JButton("Pause"),save=new JButton("Save brain & body…"),compare=new JButton("Compare with founding ancestor");
    final JButton next=new JButton("Breed next generation");
    JSlider foodSlider,mutationSlider;
    JCheckBox seasonControl,predatorControl;
    JComboBox<String> modeControl;
    final javax.swing.table.DefaultTableModel speciesModel=new javax.swing.table.DefaultTableModel(new String[]{"Group","Role / diet","Living","Origin","Parent"},0){@Override public boolean isCellEditable(int row,int column){return false;}};
    final JLabel speciesSummary=new JLabel();
    final JTree family=new JTree(new DefaultMutableTreeNode("Select a creature to explore its family"));
    final JComboBox<String> metric=new JComboBox<>(new String[]{"Population","Completed lifespan","Energy efficiency","Body size","Maximum speed","Vision range","Births & deaths"});
    boolean paused,benchmarkRunning;
    int speed=1;
    long last=System.nanoTime(),lastUi,lastFamily;
    double accumulator;
    final javax.swing.Timer timer;

    public static void main(String[] args){
        Locale.setDefault(Locale.US);
        if(args.length>0&&args[0].equals("--self-test")){SimulationChecks.run();return;}
        if(args.length>0&&args[0].equals("--render-ui")){UiPreview.render(args.length>1?args[1]:System.getProperty("java.io.tmpdir"));return;}
        SwingUtilities.invokeLater(()->{
            try{UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());}catch(Exception ignored){}
            UiTheme.install(false);new Main();
        });
    }
    Main(){this(true);}
    Main(boolean showWindow){
        frame=showWindow?new JFrame("Primordial — evolving minds & bodies"):null;
        if(frame!=null)frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        root=new JPanel(new BorderLayout(18,16));root.putClientProperty("root",true);root.setBorder(new EmptyBorder(22,24,20,24));if(frame!=null)frame.setContentPane(root);
        JPanel top=panel(new BorderLayout(10,12));JPanel masthead=panel(new BorderLayout());JLabel title=label("✳  primordial",24,Font.BOLD);JPanel brand=panel(new FlowLayout(FlowLayout.LEFT,0,0));brand.add(title);JLabel tag=label("    /    EVOLUTION SANDBOX",10,Font.BOLD);UiTheme.muted(tag);brand.add(tag);masthead.add(brand,BorderLayout.WEST);top.add(masthead,BorderLayout.NORTH);
        JPanel intro=panel();intro.setLayout(new BoxLayout(intro,BoxLayout.Y_AXIS));intro.add(label("Small brains. Endless possibilities.",29,Font.PLAIN));intro.add(Box.createVerticalStrut(7));JLabel subtitle=label("No instructions. Just hunger, tiny neural networks, and a chance to evolve.",12,Font.PLAIN);UiTheme.muted(subtitle);intro.add(subtitle);top.add(intro,BorderLayout.SOUTH);
        JPanel headerActions=panel(new FlowLayout(FlowLayout.RIGHT,8,0));JButton theme=new JButton("Dark mode");UiTheme.tip(theme,"Switch the application between light and dark themes. The dish stays dark so all object colors remain easy to distinguish. This does not change the simulation.");theme.putClientProperty("themeToggle",true);theme.addActionListener(e->{UiTheme.toggle();theme.setText(UiTheme.isDark()?"Light mode":"Dark mode");});headerActions.add(theme);
        JButton tooltips=new JButton(ToolTipManager.sharedInstance().isEnabled()?"Tooltips: On":"Tooltips: Off");
        UiTheme.tip(tooltips,"Turn all hover explanations on or off, including controls, menu choices, and objects in the dish. This setting is independent of light or dark mode.");
        tooltips.addActionListener(e->{ToolTipManager manager=ToolTipManager.sharedInstance();manager.setEnabled(!manager.isEnabled());tooltips.setText(manager.isEnabled()?"Tooltips: On":"Tooltips: Off");});
        headerActions.add(tooltips);JButton help=new JButton("How life works ↗");UiTheme.tip(help,"Read the rules for brains, bodies, diets, eggs, seasons, and genetic groups.");help.addActionListener(e->showHelp());headerActions.add(help);masthead.add(headerActions,BorderLayout.EAST);root.add(top,BorderLayout.NORTH);
        JPanel center=panel(new BorderLayout(0,12));UiTheme.Card habitat=new UiTheme.Card(new BorderLayout(0,5),8);world.setPreferredSize(new Dimension(820,490));habitat.add(world,BorderLayout.CENTER);habitat.add(new ObjectLegend(),BorderLayout.SOUTH);center.add(habitat,BorderLayout.CENTER);
        JTabbedPane controls=new JTabbedPane();controls.setTabLayoutPolicy(JTabbedPane.WRAP_TAB_LAYOUT);UiTheme.Card observe=new UiTheme.Card(new GridLayout(2,1,0,7),10);UiTheme.Card editTools=new UiTheme.Card(new GridLayout(2,1,0,7),10);
        JPanel row=panel(new FlowLayout(FlowLayout.LEFT,5,0));
        pause.addActionListener(e->{paused=!paused;pause.setText(paused?"Resume":"Pause");});UiTheme.tip(pause,"Pause or resume simulated time. You can inspect creatures and edit the habitat while paused.");UiTheme.primary(pause);row.add(pause);
        JComboBox<String> speeds=new JComboBox<>(new String[]{"1× speed","3× speed","8× speed"});speeds.addActionListener(e->speed=new int[]{1,3,8}[speeds.getSelectedIndex()]);UiHelp.choices(speeds,i->new String[]{"Run at normal speed: one simulated second per real second when the computer keeps up.","Run three simulated seconds per real second. Useful for observing behavior without waiting as long.","Run up to eight simulated seconds per real second. Evolution happens sooner; actual speed depends on available processing power."}[i]);row.add(speeds);

        JCheckBox trails=check("Trails",true);world.trails=true;trails.addActionListener(e->world.trails=trails.isSelected());UiTheme.tip(trails,"Show recent swimming paths. Trails help reveal circling, searching, and chasing behavior.");row.add(trails);
        JCheckBox senses=check("Senses",false);senses.addActionListener(e->world.showSenses=senses.isSelected());UiTheme.tip(senses,"Show sensing range and target / danger rays for the selected creature. Click a swimmer first.");row.add(senses);
        JButton reset=new JButton("New experiment");reset.addActionListener(e->reset(null));UiTheme.tip(reset,"Replace the current ecosystem with a fresh random population and default habitat. Save the world first if you want to return to it.");row.add(reset);observe.add(row);
        JPanel second=panel(new FlowLayout(FlowLayout.LEFT,5,0));
        JComboBox<String> modes=new JComboBox<>(new String[]{"Continuous reproduction","Generation selection"});modeControl=modes;
        modes.addActionListener(e->{sim.setMode(modes.getSelectedIndex()==0?Simulation.Mode.CONTINUOUS:Simulation.Mode.GENERATIONS);next.setEnabled(sim.mode==Simulation.Mode.GENERATIONS);refresh();});UiHelp.choices(modes,i->i==0?"Adults lay energy-funded eggs while parents keep living. Selection comes from survival and reproduction; extinct populations are not automatically rescued.":"Every 45 simulated seconds, select the best foragers within each role and breed the next population. One elite genome per role is copied unchanged. This mode can restore an extinct role from archived genomes.");second.add(modes);
        next.setEnabled(false);next.addActionListener(e->{sim.evolve();world.selected=null;refresh();});UiTheme.tip(next,"Immediately select parents and breed a new population. Available only in Generation selection mode; continuous reproduction has no synchronized rounds.");second.add(next);
        JCheckBox seasons=check("Seasons",true);seasonControl=seasons;seasons.addActionListener(e->sim.seasons=seasons.isSelected());UiTheme.tip(seasons,"Move the fertile food patch and vary food availability every 30 simulated seconds. Turning this off uses a constant summer environment.");second.add(seasons);
        JCheckBox predators=check("Predators",true);predatorControl=predators;predators.addActionListener(e->{sim.setPredators(predators.isSelected());if(world.selected!=null&&!world.selected.alive)world.selected=null;refresh();});UiTheme.tip(predators,"Turn hunters on or off. Turning off removes current hunters and their eggs; turning on can introduce ten new founding hunters when none remain.");second.add(predators);
        observe.add(second);
        JPanel editor=panel(new FlowLayout(FlowLayout.LEFT,5,0));editor.add(label("Edit",11,Font.PLAIN));
        JComboBox<EcosystemView.Tool> tools=new JComboBox<>(EcosystemView.Tool.values());tools.addActionListener(e->{world.tool=(EcosystemView.Tool)tools.getSelectedItem();world.painting=false;});UiHelp.choices(tools,i->UiHelp.tool(EcosystemView.Tool.values()[i]));editor.add(tools);
        editor.add(label("Brush",11,Font.PLAIN));JSlider brush=new JSlider(10,60,25);brush.setOpaque(false);brush.setPreferredSize(new Dimension(75,25));brush.addChangeListener(e->world.brushRadius=brush.getValue());UiTheme.tip(brush,"Set the rock-drawing and eraser radius, in world units. Larger brushes build thicker barriers or erase wider paths.");editor.add(brush);
        JComboBox<EcosystemView.Colors> colors=new JComboBox<>(EcosystemView.Colors.values());colors.addActionListener(e->world.colors=(EcosystemView.Colors)colors.getSelectedItem());UiHelp.choices(colors,i->UiHelp.colors(EcosystemView.Colors.values()[i]));editor.add(colors);
        JPanel files=panel(new FlowLayout(FlowLayout.LEFT,5,0));
        JButton saveWorld=new JButton("Save world…");saveWorld.addActionListener(e->saveWorld());UiTheme.tip(saveWorld,"Save an exact world snapshot: creatures, brains and memory, eggs, families, food, map, settings, and random state.");files.add(saveWorld);
        JButton loadWorld=new JButton("Load world…");loadWorld.addActionListener(e->loadWorld());UiTheme.tip(loadWorld,"Resume a saved world snapshot. You will confirm replacing the current world, and the restored world opens paused.");files.add(loadWorld);
        files.add(label("Pause to sculpt habitats; nests protect eggs.",10,Font.PLAIN));
        editTools.add(editor);editTools.add(files);controls.addTab("Observe & evolve",observe);controls.setToolTipTextAt(0,"Playback speed, reproduction mode, seasons, and hunters.");controls.addTab("Shape the habitat",editTools);controls.setToolTipTextAt(1,"Paint food, draw barriers, choose colors, and save or load complete worlds.");center.add(controls,BorderLayout.SOUTH);root.add(center,BorderLayout.CENTER);
        JTabbedPane tabs=new JTabbedPane();tabs.setPreferredSize(new Dimension(330,540));
        tabs.addTab("Inspect",scroll(inspector(predators)));tabs.addTab("Dashboard",scroll(dashboardPanel()));tabs.addTab("Family",familyPanel());tabs.addTab("Species",speciesPanel());tabs.addTab("Experiments",new ExperimentPanel());tabs.setTabLayoutPolicy(JTabbedPane.WRAP_TAB_LAYOUT);String[] tabHelp={"Inspect a creature, tune food and mutation, or save a genome.","Live statistics and controlled comparisons with founding ancestors.","Explore the selected creature’s ancestors and descendants.","View living genetic clusters and when their references first appeared.","Run several paired seeded worlds while changing one setting."};for(int i=0;i<tabHelp.length;i++)tabs.setToolTipTextAt(i,tabHelp[i]);UiTheme.Card sidebar=new UiTheme.Card(new BorderLayout(),10);sidebar.add(tabs);root.add(sidebar,BorderLayout.EAST);
        JPanel bottom=new UiTheme.Card(new BorderLayout(18,0),14);JPanel graphTitle=panel();graphTitle.setLayout(new BoxLayout(graphTitle,BoxLayout.Y_AXIS));
        graphTitle.add(label("THE STORY SO FAR",10,Font.BOLD));graphTitle.add(Box.createVerticalStrut(10));metric.setMaximumSize(new Dimension(175,28));metric.setAlignmentX(Component.LEFT_ALIGNMENT);metric.addActionListener(e->history.repaint());UiHelp.choices(metric,UiHelp::metric);graphTitle.add(metric);
        JLabel caption=label("Sampled every 5 seconds",10,Font.PLAIN);caption.setForeground(MUTED);graphTitle.add(Box.createVerticalStrut(7));graphTitle.add(caption);bottom.add(graphTitle,BorderLayout.WEST);
        history.setPreferredSize(new Dimension(600,80));bottom.add(history,BorderLayout.CENTER);root.add(bottom,BorderLayout.SOUTH);
        UiTheme.apply(root);
        if(frame!=null){frame.pack();Rectangle screen=GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
            frame.setSize(Math.min(frame.getWidth(),screen.width),Math.min(frame.getHeight(),screen.height));frame.setMinimumSize(new Dimension(Math.min(1050,screen.width),Math.min(760,screen.height)));frame.setLocationRelativeTo(null);frame.setVisible(true);}
        timer=new javax.swing.Timer(16,e->update());if(showWindow)timer.start();refresh();
    }
    JPanel inspector(JCheckBox predatorToggle){
        JPanel p=stack();
        UiTheme.Card censusCard=new UiTheme.Card(new BorderLayout(0,12),18);censusCard.putClientProperty("hero",true);censusCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        censusCard.add(label("THE LIVING WORLD",10,Font.BOLD),BorderLayout.NORTH);censusCard.add(census,BorderLayout.CENTER);censusCard.add(clock,BorderLayout.SOUTH);p.add(censusCard);p.add(Box.createVerticalStrut(12));
        UiTheme.Card settings=new UiTheme.Card(new BorderLayout(),16);settings.setAlignmentX(Component.LEFT_ALIGNMENT);JPanel settingsBody=stack();settingsBody.add(label("SHAPE THEIR WORLD",10,Font.BOLD));
        foodSlider=addSlider(settingsBody,"Food abundance",0,100,55,v->sim.abundance=v);mutationSlider=addSlider(settingsBody,"Mutation (%)",0,50,12,v->sim.mutationRate=v/100.0);settings.add(settingsBody);p.add(settings);p.add(Box.createVerticalStrut(12));
        UiTheme.Card mind=new UiTheme.Card(new BorderLayout(),16);mind.setAlignmentX(Component.LEFT_ALIGNMENT);JPanel body=stack();body.add(label("A MIND OF ITS OWN",10,Font.BOLD));brain.setPreferredSize(new Dimension(235,145));brain.setMaximumSize(new Dimension(500,145));brain.setAlignmentX(Component.LEFT_ALIGNMENT);body.add(brain);
        UiTheme.tip(brain,"Live neural activity: 12 sensory inputs, 8 recurrent hidden neurons, and 2 movement outputs. Blue loops represent memory; green links are positive and warm links are negative.");
        creatureInfo.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,11));body.add(creatureInfo);body.add(Box.createVerticalStrut(12));save.addActionListener(e->saveBrain());UiTheme.tip(save,"Save the selected creature’s inherited brain, diet, and body traits. To preserve the entire running ecosystem, use Save world instead.");body.add(save);
        body.add(Box.createVerticalStrut(8));JButton restore=new JButton("Seed from saved genome…");restore.addActionListener(e->loadBrain(predatorToggle));UiTheme.tip(restore,"Start a fresh population from a saved genome. This replaces the current world; it is different from resuming a full-world snapshot.");body.add(restore);
        body.add(Box.createVerticalStrut(12));JLabel hint=label("Click a swimmer. Enter cycles selection.",10,Font.PLAIN);UiTheme.muted(hint);body.add(hint);mind.add(body);p.add(mind);return p;
    }
    JPanel dashboardPanel(){
        JPanel p=stack();p.add(label("NATURAL SELECTION, MEASURED",11,Font.BOLD));p.add(Box.createVerticalStrut(15));p.add(dashboard);p.add(Box.createVerticalStrut(20));
        p.add(label("A FAIRER RACE AGAINST THE PAST",11,Font.BOLD));p.add(Box.createVerticalStrut(10));
        JLabel explanation=label("<html>Select a creature, or use the strongest living prey.<br>Race its genome against its own founding ancestor.<br>Both receive matching trial conditions.</html>",11,Font.PLAIN);explanation.setForeground(MUTED);p.add(explanation);p.add(Box.createVerticalStrut(12));
        compare.addActionListener(e->compare());UiTheme.tip(compare,"Run five paired 45-second trials against the selected creature’s founding ancestor. Trials use matching starting conditions and do not alter the live world.");p.add(compare);p.add(Box.createVerticalStrut(8));comparisonStatus.setFont(new Font("SansSerif",Font.PLAIN,10));p.add(comparisonStatus);return p;
    }
    JPanel familyPanel(){
        JPanel p=panel(new BorderLayout(0,10));p.setBorder(new EmptyBorder(10,8,8,8));family.setRootVisible(true);family.setBackground(BACKGROUND);family.setFont(new Font("SansSerif",Font.PLAIN,11));p.add(new JScrollPane(family),BorderLayout.CENTER);
        JLabel note=label("<html>Selected creature marked ★. Up to 4 ancestors above it,<br>5 descendant levels and 250 nodes. Archive retains<br>the latest 6,000 births; older branches may be pruned.</html>",10,Font.PLAIN);note.setForeground(MUTED);p.add(note,BorderLayout.SOUTH);return p;
    }
    static JPanel panel(){JPanel p=new JPanel();p.setOpaque(false);return p;}
    static JPanel panel(LayoutManager l){JPanel p=panel();p.setLayout(l);return p;}
    static JPanel stack(){JPanel p=new UiTheme.Stack();p.setBorder(new EmptyBorder(8,4,8,4));return p;}
    static JScrollPane scroll(JPanel panel){JScrollPane s=new JScrollPane(panel);s.setBorder(null);s.getViewport().setBackground(BACKGROUND);s.getVerticalScrollBar().setUnitIncrement(14);return s;}
    static JCheckBox check(String name,boolean selected){JCheckBox c=new JCheckBox(name,selected);c.setOpaque(false);return c;}
    static JLabel label(String text,int size,int weight){JLabel l=new JLabel(text);l.setFont(new Font("SansSerif",weight,size));l.setForeground(INK);return l;}
    JSlider addSlider(JPanel p,String text,int min,int max,int initial,java.util.function.IntConsumer change){
        p.add(Box.createVerticalStrut(10));JLabel label=label(text+": "+initial,11,Font.PLAIN);p.add(label);
        JSlider slider=new JSlider(min,max,initial);slider.setOpaque(false);slider.setAlignmentX(Component.LEFT_ALIGNMENT);slider.setMaximumSize(new Dimension(500,25));
        slider.addChangeListener(e->{label.setText(text+": "+slider.getValue());change.accept(slider.getValue());});UiTheme.tip(slider,text.startsWith("Food")?"Controls how much food the environment can replenish. Existing food remains until eaten or decayed; higher abundance makes foraging easier.":"Chance that each inherited weight or body trait mutates when offspring are created. Higher values add more variation; existing creatures are not changed.");p.add(slider);return slider;
    }
    void update(){
        long now=System.nanoTime();double elapsed=Math.min((now-last)/1e9,.08);last=now;
        if(paused)accumulator=0;
        else{accumulator+=elapsed*speed;int steps=0;while(accumulator>=1.0/60&&steps++<40){sim.step();accumulator-=1.0/60;}}
        world.repaint();brain.repaint();
        if(now-lastUi>150_000_000){refresh();lastUi=now;}
    }
    void refresh(){
        census.setText(String.format("<html><span style='font-size:25px'>%d</span> living creatures · %d eggs<br>%d prey · %d predators<br>Births: %d · Deaths: %d<br>Meals: %d · Captures: %d</html>",sim.aliveCount(),sim.eggs.size(),sim.count(Simulation.Species.PREY),sim.count(Simulation.Species.PREDATOR),sim.totalBirths,sim.totalDeaths,sim.totalMeals,sim.totalKills));
        clock.setText(String.format("<html>%s · %.0fs · deepest G%d<br>%s</html>",sim.season(),sim.seconds(),sim.generation,sim.aliveCount()==0&&sim.eggs.isEmpty()?"Extinct — start or load a new experiment":sim.mode==Simulation.Mode.CONTINUOUS?"Continuous births · no reseeding":(45-sim.tick/60)+"s until selection"));
        Simulation.Creature c=world.selected;
        creatureInfo.setText(c==null?"<html>12 senses → 8 recurrent neurons → 2 actions<br>Arrow-shaped hunters · oval prey<br>Bodies and brains mutate together.</html>":String.format("<html>#%d · %s · G%d · %s<br>Parent #%d · Family #%d · Children %d<br>Age %.1fs · Energy %.0f / %.0f · Meals %d<br>Size %.1f · Speed %.2f · Vision %.0f<br>Turn %+.2f · Throttle %+.2f</html>",c.id,c.species,c.generation,c.alive?"Living":"Dead",c.parentId,c.rootId,c.children,c.age/60.0,Math.max(0,c.energy),c.capacity(),c.eaten,c.size,c.speed,c.vision,c.outputs[0],c.outputs[1]));
        if(c!=null)creatureInfo.setText(creatureInfo.getText().replace("</html>","<br>Diet "+c.diet+" · Group C"+c.clusterId+"<br>Eggs laid "+c.eggsLaid+"</html>"));
        save.setEnabled(c!=null);compare.setEnabled(!benchmarkRunning&&(c!=null||sim.best(Simulation.Species.PREY)!=null));
        dashboard.setText(String.format("<html><b>Life & resources</b><br>Completed lifespan: %s<br>Energy gained / spent: %.2f×<br>Energy consumed: %.0f<br><br><b>Mean living body traits</b><br>Body radius: %.2f<br>Maximum speed: %.2f<br>Vision range: %.0f<br><br><b>Evolution</b><br>Deepest generation: %d<br>Births (includes founders): %d<br>Deaths: %d · Captures: %d<br>Population limit: %d<br><br>Reproduction consumes parental energy.<br>Age limit: 240s. Founders are not reintroduced<br>automatically in continuous mode.<br>Lifetime statistics include both species;<br>generation selection also ends living lives.</html>",sim.totalDeaths==0?"awaiting first death":String.format("%.1fs",sim.meanLifespan()),sim.efficiency(),sim.totalEnergySpent,sim.meanTrait(0),sim.meanTrait(1),sim.meanTrait(2),sim.generation,sim.totalBirths,sim.totalDeaths,sim.totalKills,Simulation.MAX_POPULATION));
        long now=System.nanoTime();if(now-lastFamily>1_000_000_000){updateFamily();updateSpecies();lastFamily=now;}
        dashboard.setText(dashboard.getText().replace("</html>","<br><br>Eggs waiting: "+sim.eggs.size()+"<br>Laid: "+sim.totalEggsLaid+" · Lost: "+sim.totalEggLoss+"<br>Nests: "+sim.nests.size()+" · Food sources: "+sim.sources.size()+"</html>"));
        history.repaint();
    }
    void updateFamily(){
        Simulation.Creature selected=world.selected;if(selected==null){family.setModel(new DefaultTreeModel(new DefaultMutableTreeNode("Select a creature in the dish")));return;}
        List<Simulation.Ancestor> chain=sim.ancestorsOf(selected);if(chain.isEmpty()){family.setModel(new DefaultTreeModel(new DefaultMutableTreeNode("This family has left the archive")));return;}
        Simulation.Ancestor root=chain.get(Math.min(4,chain.size()-1));Map<Integer,List<Simulation.Ancestor>> children=new HashMap<>();
        for(var a:sim.ancestry.values())children.computeIfAbsent(a.parentId,k->new ArrayList<>()).add(a);
        int[] budget={250};family.setModel(new DefaultTreeModel(familyNode(root,children,0,budget,selected.id)));
        for(int row=0;row<family.getRowCount()&&row<40;row++)family.expandRow(row);
    }
    DefaultMutableTreeNode familyNode(Simulation.Ancestor a,Map<Integer,List<Simulation.Ancestor>> children,int depth,int[] budget,int selected){
        budget[0]--;DefaultMutableTreeNode n=new DefaultMutableTreeNode((a.id==selected?"★ ":"")+"#"+a.id+" G"+a.generation+" "+a.species+" · "+a.children+" children"+(a.diedAt>=0?String.format(" · lived %.1fs",a.diedAt-a.bornAt):""));
        if(depth<5)for(var child:children.getOrDefault(a.id,List.of())){if(budget[0]<=0){n.add(new DefaultMutableTreeNode("More branches…"));break;}n.add(familyNode(child,children,depth+1,budget,selected));}return n;
    }
    void reset(Simulation.Genome seed){sim.reset(seed);world.selected=null;accumulator=0;lastFamily=0;refresh();}
    void showHelp(){JOptionPane.showMessageDialog(frame,"""
            Bodies and brains both evolve. Size increases energy capacity but
            costs more to maintain. Speed and vision also consume energy.
            The 12 → 8 → 2 network has recurrent connections: its previous
            hidden state influences its next decision. There is no food-seeking
            or fleeing rule; movement comes from the neural network.

            Prey sense food and hunters. Predators sense prey and rivals.
            Rocks block movement; 3 directional sensors detect obstacles.
            Each season lasts 30 simulated seconds and changes food supply
            and the fertile patch. Existing food decays gradually.

            CONTINUOUS: well-fed adults split energy with mutated offspring.
            Parents keep living. Eggs hatch in 6s, or 3s inside a nest.
            Nests protect eggs from hunters. No automatic extinction rescue.
            GENERATIONS: every 45s, each species breeds from its strongest
            foragers. One elite brain and body per species is copied exactly.
            This mode can restore an extinct species from its archived brains.

            Use Inspect for brains, Dashboard for statistics and a controlled
            ancestor comparison, and Family for ancestry. Select a graph below
            to track populations, lifespan, efficiency, or body traits.

            Plants, moving plankton, and carcasses support three inherited diets.
            A preferred diet extracts more energy. Carcasses decay in 30s.
            Diets are body traits; their preferences influence target sensing.

            Editor: draw ROCKS, ERASE to carve channels, place NESTS,
            and add PLANT_SOURCE or PLANKTON_SOURCE feeding patches.
            Rocks cannot be drawn over live creatures or eggs. Pause first
            to build a partition, then feed each side differently.

            Save world preserves creatures, eggs, families, brain memory,
            map, settings, and the exact random state. Loading replaces
            the current world and pauses it. Brain files remain separate.
            Genetic groups use a distance threshold, not biological speciation.
            Experiments run paired seeds in the background and export CSV.

            Try 8× speed, Trails, Senses, and DIETS or GENETIC_GROUPS colors.
            Evolution is stochastic; better behavior is not guaranteed.
            ""","The rules of life",JOptionPane.PLAIN_MESSAGE);}
    void saveBrain(){
        if(world.selected==null)return;JFileChooser chooser=new JFileChooser();chooser.setSelectedFile(new File("creature.brain"));if(chooser.showSaveDialog(frame)!=JFileChooser.APPROVE_OPTION)return;
        File file=chooser.getSelectedFile();if(file.exists()&&JOptionPane.showConfirmDialog(frame,"Replace "+file.getName()+"?","Save genome",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;
        try{BrainIO.write(file.toPath(),world.selected.genome());}catch(Exception ex){error(ex);}
    }
    void loadBrain(JCheckBox predatorToggle){
        JFileChooser chooser=new JFileChooser();if(chooser.showOpenDialog(frame)!=JFileChooser.APPROVE_OPTION)return;
        try{Simulation.Genome genome=BrainIO.read(chooser.getSelectedFile().toPath());if(genome.species()==Simulation.Species.PREDATOR){predatorToggle.setSelected(true);sim.predatorsEnabled=true;}reset(genome);}catch(Exception ex){error(ex);}
    }
    void error(Exception ex){JOptionPane.showMessageDialog(frame,ex.getMessage(),"Could not complete operation",JOptionPane.ERROR_MESSAGE);}
    void compare(){
        Simulation.Creature c=world.selected!=null?world.selected:sim.best(Simulation.Species.PREY);if(c==null)return;
        Simulation.Genome founder=sim.founders.get(c.rootId);if(founder==null){comparisonStatus.setText("Founding genome is unavailable for this family.");return;}
        Simulation.Genome current=c.genome();int abundance=sim.abundance,id=c.id,root=c.rootId;boolean seasons=sim.seasons;
        benchmarkRunning=true;compare.setEnabled(false);comparisonStatus.setText("Testing #"+id+" vs founder #"+root+"…");
        new SwingWorker<Benchmark.Comparison,Void>(){
            @Override protected Benchmark.Comparison doInBackground(){return Benchmark.compare(current,founder,abundance,seasons);}
            @Override protected void done(){benchmarkRunning=false;try{Benchmark.Comparison result=get();comparisonStatus.setText("Completed: #"+id+" vs founder #"+root);JTextArea text=new JTextArea(result.description());text.setEditable(false);text.setFont(new Font("Monospaced",Font.PLAIN,12));JOptionPane.showMessageDialog(frame,new JScrollPane(text),"Ancestor comparison",JOptionPane.PLAIN_MESSAGE);}catch(Exception ex){comparisonStatus.setText("Comparison failed");error(ex);}refresh();}
        }.execute();
    }
    JPanel speciesPanel(){
        JPanel panel=panel(new BorderLayout(0,10));panel.setBorder(new EmptyBorder(10,8,8,8));panel.add(speciesSummary,BorderLayout.NORTH);
        JTable table=new JTable(speciesModel);table.setFont(new Font("SansSerif",Font.PLAIN,10));table.setRowHeight(23);table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);int[] widths={50,190,50,65,65};for(int i=0;i<widths.length;i++)table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);panel.add(new JScrollPane(table),BorderLayout.CENTER);
        panel.add(label("<html>Reference-based genetic clusters, not biological species.<br>Weights, body traits, and diet determine distance.<br>New group above distance 0.48. Up to 256 per role.<br>Only living groups shown; group colors are in the editor.</html>",10,Font.PLAIN),BorderLayout.SOUTH);return panel;
    }
    void updateSpecies(){
        Map<Integer,Integer> counts=sim.tracker.livingCounts(sim);speciesSummary.setText("<html><b>"+counts.size()+" living genetic groups</b><br>"+sim.tracker.groups().size()+" references recorded · "+sim.eggs.size()+" eggs</html>");speciesModel.setRowCount(0);
        sim.tracker.groups().stream().filter(group->counts.containsKey(group.id())).sorted(Comparator.comparingInt((SpeciesTracker.Group group)->counts.get(group.id())).reversed()).forEach(group->speciesModel.addRow(new Object[]{"C"+group.id(),group.reference().species()+" / "+group.reference().diet(),counts.get(group.id()),String.format("%.0fs",group.appearedAt()),group.parentGroup()==0?"Founder":"C"+group.parentGroup()}));
    }
    void saveWorld(){
        JFileChooser chooser=new JFileChooser();chooser.setSelectedFile(new File("ecosystem.world"));if(chooser.showSaveDialog(frame)!=JFileChooser.APPROVE_OPTION)return;
        File file=chooser.getSelectedFile();if(file.exists()&&JOptionPane.showConfirmDialog(frame,"Replace this saved world?","Save world",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;
        try{WorldIO.write(file.toPath(),sim);}catch(Exception ex){error(ex);}
    }
    void loadWorld(){
        JFileChooser chooser=new JFileChooser();if(chooser.showOpenDialog(frame)!=JFileChooser.APPROVE_OPTION)return;
        try{Simulation loaded=WorldIO.read(chooser.getSelectedFile().toPath());
            if(JOptionPane.showConfirmDialog(frame,"Replace the current world with this snapshot?","Load world",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;
            sim=loaded;world.setSimulation(loaded);paused=true;pause.setText("Resume");accumulator=0;last=System.nanoTime();lastFamily=0;
            double preciseMutation=sim.mutationRate;foodSlider.setValue(sim.abundance);mutationSlider.setValue((int)Math.round(sim.mutationRate*100));sim.mutationRate=preciseMutation;seasonControl.setSelected(sim.seasons);predatorControl.setSelected(sim.predatorsEnabled);
            // Setting a combo box fires its listener. Preserve tick and selection archives on load.
            for(var listener:modeControl.getActionListeners())modeControl.removeActionListener(listener);
            modeControl.setSelectedIndex(sim.mode==Simulation.Mode.CONTINUOUS?0:1);
            modeControl.addActionListener(e->{sim.setMode(modeControl.getSelectedIndex()==0?Simulation.Mode.CONTINUOUS:Simulation.Mode.GENERATIONS);next.setEnabled(sim.mode==Simulation.Mode.GENERATIONS);refresh();});
            next.setEnabled(sim.mode==Simulation.Mode.GENERATIONS);refresh();
        }catch(Exception ex){error(ex);}
    }
    final class BrainView extends JPanel {
        BrainView(){setOpaque(false);}
        @Override protected void paintComponent(Graphics graphics){super.paintComponent(graphics);Graphics2D g=(Graphics2D)graphics.create();EcosystemView.smooth(g);Simulation.Creature c=world.selected;
            double[][] values=c==null?new double[][]{new double[Simulation.INPUTS],new double[8],new double[2]}:new double[][]{c.inputs,c.hidden,c.outputs};int[] x={14,getWidth()/2,getWidth()-15};
            for(int l=0;l<2;l++)for(int a=0;a<values[l].length;a++)for(int b=0;b<values[l+1].length;b++){
                double weight=c==null?0:c.weights[l==0?b*13+1+a:Simulation.OUTPUT_OFFSET+b*9+1+a];int alpha=c==null?45:55+(int)(Math.abs(values[l][a])*100);
                g.setColor(weight>=0?new Color(100,140,75,alpha):new Color(165,110,85,alpha));g.drawLine(x[l],nodeY(a,values[l].length),x[l+1],nodeY(b,values[l+1].length));}
            g.setColor(new Color(98,145,164,130));for(int i=0;i<8;i++){int y=nodeY(i,8);g.drawArc(x[1]-5,y-9,17,15,0,300);}
            for(int l=0;l<3;l++)for(int i=0;i<values[l].length;i++){double v=values[l][i];g.setColor(c==null?UiTheme.palette().line():Color.getHSBColor(v>=0?.24f:.07f,.25f+(float)Math.abs(v)*.3f,.85f));g.fillOval(x[l]-3,nodeY(i,values[l].length)-3,6,6);}
            g.setFont(new Font("SansSerif",Font.PLAIN,9));g.setColor(UiTheme.palette().muted());g.drawString("12 senses",0,getHeight()-3);g.drawString("8 + memory",getWidth()/2-23,getHeight()-3);g.drawString("2 actions",getWidth()-45,getHeight()-3);g.dispose();}
        int nodeY(int i,int n){return 8+(int)((getHeight()-30)*(i+.5)/n);}
    }
    final class HistoryView extends JPanel {
        HistoryView(){setOpaque(false);}
        double value(Simulation.Sample s,int series){return switch(metric.getSelectedIndex()){case 0->series==0?s.prey():s.predators();case 1->s.lifespan();case 2->s.efficiency();case 3->s.size();case 4->s.speed();case 5->s.vision();default->series==0?s.births():s.deaths();};}
        @Override protected void paintComponent(Graphics graphics){super.paintComponent(graphics);Graphics2D g=(Graphics2D)graphics.create();EcosystemView.smooth(g);int w=getWidth(),h=getHeight();g.setFont(new Font("SansSerif",Font.PLAIN,10));
            for(int i=1;i<=3;i++){g.setColor(UiTheme.palette().line());g.drawLine(35,i*(h-25)/3,w-8,i*(h-25)/3);}
            if(sim.history.isEmpty()){g.dispose();return;}int series=metric.getSelectedIndex()==0||metric.getSelectedIndex()==6?2:1;double max=1;
            for(var s:sim.history)for(int j=0;j<series;j++)max=Math.max(max,value(s,j));max*=1.1;
            double start=sim.history.get(0).seconds(),end=Math.max(start+5,sim.history.get(sim.history.size()-1).seconds());
            for(int j=0;j<series;j++){g.setColor(j==0?(UiTheme.isDark()?UiTheme.PREY:new Color(46,119,87)):(UiTheme.isDark()?UiTheme.PREDATOR:new Color(175,80,52)));g.setStroke(new BasicStroke(2));int px=0,py=0;
                for(int i=0;i<sim.history.size();i++){var s=sim.history.get(i);int x=38+(int)((s.seconds()-start)/(end-start)*(w-50)),y=h-25-(int)(value(s,j)/max*(h-35));if(i>0)g.drawLine(px,py,x,y);g.fillOval(x-2,y-2,4,4);px=x;py=y;}}
            g.setColor(UiTheme.palette().muted());g.drawString(String.format("%.1f",max),0,12);g.drawString("0",15,h-24);g.drawString(String.format("%.0fs",start),35,h-6);g.drawString(String.format("%.0fs",end),w-35,h-6);
            String legend=metric.getSelectedIndex()==0?"Green: prey · Orange: predators":metric.getSelectedIndex()==6?"Green: births · Orange: deaths":metric.getSelectedIndex()==1?"Mean completed lifespan (seconds)":metric.getSelectedIndex()==2?"Lifetime energy gained / energy spent":"Mean living trait";
            g.drawString(legend,Math.max(85,w/2-g.getFontMetrics().stringWidth(legend)/2),h-6);g.dispose();}
    }
}
