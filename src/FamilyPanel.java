import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.CubicCurve2D;
import java.util.*;
import java.util.List;
import java.util.function.*;

/** Searchable named genealogy with a focused, scrollable card graph. */
public final class FamilyPanel extends JPanel {
    private final Supplier<Simulation> source;
    private final Consumer<Simulation.Creature> select;
    private final JTextField search=new JTextField();
    private final JComboBox<String> filter=new JComboBox<>(new String[]{"All lives","Living","Prey","Hunters"});
    private final DefaultListModel<Simulation.Ancestor> model=new DefaultListModel<>();
    private final JList<Simulation.Ancestor> results=new JList<>(model);
    private final JLabel details=new JLabel();
    private final Canvas canvas=new Canvas();
    private Simulation previous;
    private int focus;
    private boolean updating;
    public FamilyPanel(Supplier<Simulation> source,Consumer<Simulation.Creature> select){
        super(new BorderLayout(12,12));this.source=source;this.select=select;setBorder(BorderFactory.createEmptyBorder(16,16,16,16));
        JPanel top=new JPanel(new BorderLayout(8,8));top.add(new JLabel("Find a creature, clan, or internal ID"),BorderLayout.WEST);top.add(search);filter.setPreferredSize(new Dimension(140,34));top.add(filter,BorderLayout.EAST);add(top,BorderLayout.NORTH);
        UiTheme.tip(search,"Search named creatures, their clan, or a stable numeric ID. Dead lives remain searchable until archive pruning.");UiTheme.tip(filter,"Limit search by living state or ecological role.");
        results.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);results.setFixedCellHeight(64);results.setCellRenderer((list,a,index,selected,hasFocus)->{JLabel l=new JLabel("<html><b>"+a.name+"</b> · G"+a.generation+"<br>"+a.clan+" · "+(a.diedAt<0?"Living":"Completed")+" · "+a.species+"</html>");l.setOpaque(true);l.setBackground(selected?UiTheme.palette().selected():UiTheme.palette().surface());l.setForeground(UiTheme.palette().text());l.setBorder(BorderFactory.createEmptyBorder(5,8,5,8));return l;});
        JScrollPane list=new JScrollPane(results);list.setPreferredSize(new Dimension(310,500));JSplitPane split=new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,list,new JScrollPane(canvas));split.setResizeWeight(.25);add(split);add(details,BorderLayout.SOUTH);
        results.addListSelectionListener(e->{if(!updating&&!e.getValueIsAdjusting()&&results.getSelectedValue()!=null)focus(results.getSelectedValue().id);});
        search.getDocument().addDocumentListener(new DocumentListener(){public void insertUpdate(DocumentEvent e){refresh();}public void removeUpdate(DocumentEvent e){refresh();}public void changedUpdate(DocumentEvent e){refresh();}});filter.addActionListener(e->refresh());refresh();UiTheme.apply(this);
    }
    public void focus(int id){focus=id;Simulation s=source.get();Simulation.Creature c=s.findCreature(id);if(c!=null)select.accept(c);canvas.rebuild();}
    public void refresh(){Simulation s=source.get();if(s!=previous){previous=s;focus=s.creatures.isEmpty()?0:s.creatures.get(0).id;}String q=search.getText().trim().toLowerCase(Locale.ROOT);int kind=filter.getSelectedIndex();updating=true;model.clear();s.ancestry.values().stream().filter(a->(a.name+" "+a.clan+" "+a.id).toLowerCase(Locale.ROOT).contains(q)).filter(a->kind!=1||a.diedAt<0).filter(a->kind!=2||a.species==Simulation.Species.PREY).filter(a->kind!=3||a.species==Simulation.Species.PREDATOR).sorted(Comparator.comparing((Simulation.Ancestor a)->a.diedAt>=0).thenComparing(a->a.name)).limit(500).forEach(model::addElement);for(int i=0;i<model.size();i++)if(model.get(i).id==focus){results.setSelectedIndex(i);break;}updating=false;canvas.rebuild();details.setText("Archive: "+s.ancestry.size()+" / "+Simulation.MAX_ANCESTRY+" births · search shows up to 500 matches · graph shows up to 200 relatives · old branches may be pruned");}
    private final class Canvas extends JPanel {
        private record Card(Simulation.Ancestor a,Rectangle bounds){}
        private final List<Card> cards=new ArrayList<>();
        private final List<int[]> edges=new ArrayList<>();
        private int base,topRow;
        Canvas(){setOpaque(true);setToolTipText("Select a life card to explore its descendants.");addMouseListener(new MouseAdapter(){public void mouseClicked(MouseEvent e){for(Card card:cards)if(card.bounds.contains(e.getPoint())){focus(card.a.id);return;}}});}
        @Override public String getToolTipText(MouseEvent e){for(Card card:cards)if(card.bounds.contains(e.getPoint())){var a=card.a;return "<html><b>"+a.name+"</b><br>"+a.clan+"<br>ID "+a.id+" · G"+a.generation+" · Meals "+a.meals+" · Children "+a.children+"</html>";}return "Click a life card to focus its family.";}
        void rebuild(){cards.clear();edges.clear();Simulation s=source.get();Simulation.Ancestor a=s.ancestry.get(focus);if(a==null){setPreferredSize(new Dimension(900,500));repaint();return;}Simulation.Ancestor parent=s.ancestry.get(a.parentId),grand=parent==null?null:s.ancestry.get(parent.parentId);base=parent==null?0:235;topRow=grand==null?60:170;int selected=add(a,base+20,topRow);if(parent!=null){int p=add(parent,20,topRow);edges.add(new int[]{p,selected});if(grand!=null){int g=add(grand,20,60);edges.add(new int[]{g,p});}}
            Map<Integer,List<Simulation.Ancestor>> children=new HashMap<>();for(var relative:s.ancestry.values())children.computeIfAbsent(relative.parentId,k->new ArrayList<>()).add(relative);
            int[] row={0};descendants(a,selected,1,row,children);setPreferredSize(new Dimension(base+730,Math.max(500,topRow+row[0]*100+100)));revalidate();repaint();}
        int add(Simulation.Ancestor a,int x,int y){cards.add(new Card(a,new Rectangle(x,y,210,78)));return cards.size()-1;}
        void descendants(Simulation.Ancestor a,int from,int level,int[] row,Map<Integer,List<Simulation.Ancestor>> children){if(level>2)return;for(var child:children.getOrDefault(a.id,List.of())){if(cards.size()>=200)return;int to=add(child,base+20+level*235,topRow+row[0]*100);edges.add(new int[]{from,to});if(level==1){int start=row[0];descendants(child,to,level+1,row,children);if(row[0]==start)row[0]++;}else row[0]++;}}
        @Override protected void paintComponent(Graphics graphics){super.paintComponent(graphics);Graphics2D g=(Graphics2D)graphics.create();EcosystemView.smooth(g);g.setColor(UiTheme.palette().background());g.fillRect(0,0,getWidth(),getHeight());g.setColor(UiTheme.palette().muted());g.setFont(UiTheme.bodyFont(Font.PLAIN,13));if(base>0)g.drawString("Ancestors",20,28);g.drawString("Selected life",base+20,28);g.drawString("Children",base+255,28);g.drawString("Grandchildren",base+490,28);g.setStroke(new BasicStroke(2));for(int[] edge:edges){Rectangle a=cards.get(edge[0]).bounds,b=cards.get(edge[1]).bounds;g.setColor(UiTheme.palette().line());if(a.x==b.x)g.drawLine((int)a.getCenterX(),a.y+a.height,(int)b.getCenterX(),b.y);else g.draw(new CubicCurve2D.Double(a.x+a.width,a.getCenterY(),a.x+a.width+24,a.getCenterY(),b.x-24,b.getCenterY(),b.x,b.getCenterY()));}
            for(Card card:cards){var a=card.a;Rectangle b=card.bounds;g.setColor(a.id==focus?UiTheme.palette().selected():UiTheme.palette().surface());g.fillRoundRect(b.x,b.y,b.width,b.height,16,16);g.setColor(a.species==Simulation.Species.PREY?UiTheme.PREY:UiTheme.PREDATOR);g.drawRoundRect(b.x,b.y,b.width,b.height,16,16);g.setColor(UiTheme.palette().text());g.setFont(UiTheme.headingFont(14));g.drawString(shorten(a.name,27),b.x+12,b.y+22);g.setFont(UiTheme.bodyFont(Font.PLAIN,12));g.drawString(shorten(a.clan,31),b.x+12,b.y+42);g.setColor(UiTheme.palette().muted());g.drawString("G"+a.generation+" · "+a.children+" children · "+(a.diedAt<0?"living":String.format("%.1fs",a.diedAt-a.bornAt)),b.x+12,b.y+62);}if(cards.isEmpty())g.drawString("Select a searchable life to explore its family.",40,100);g.dispose();}
        String shorten(String s,int n){return s.length()>n?s.substring(0,n-1)+"…":s;}
    }
}
