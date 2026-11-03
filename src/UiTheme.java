import javax.swing.*;
import javax.swing.border.*;
import javax.swing.plaf.basic.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;

/** Dependency-free light/dark design system. Theme changes affect presentation only. */
public final class UiTheme {
    public record Palette(Color background,Color surface,Color input,Color text,Color muted,
                          Color line,Color accent,Color accentText,Color hover,Color selected) {}
    private static boolean dark;
    public static Palette palette(){return dark?DARK:LIGHT;}
    public static boolean isDark(){return dark;}
    private static Color hex(int rgb){return new Color(rgb);}
    private static final Palette LIGHT=new Palette(hex(0xf3f4ed),hex(0xfbfcf7),hex(0xeff2e8),hex(0x223e34),hex(0x65765f),hex(0xd6ded0),hex(0x355c45),hex(0xf4f9e9),hex(0xe4ecd9),hex(0xdcebc9));
    private static final Palette DARK=new Palette(hex(0x0e1916),hex(0x172923),hex(0x20382e),hex(0xe8f0e1),hex(0xa3b79f),hex(0x385246),hex(0xc5e99a),hex(0x173123),hex(0x2b4537),hex(0x344f3b));
    public static final Color PREY=hex(0x71e2bc),PREDATOR=hex(0xff997f),PLANT=hex(0xc6e98c),PLANKTON=hex(0x82caff),CARRION=hex(0xd59ada),EGG=hex(0xffe1a0),NEST=hex(0xe2b86c),ROCK=hex(0x829c9a);
    public static final Color HABITAT=hex(0x102820),HABITAT_TEXT=hex(0xc1d9c2);
    private UiTheme() {}
    public static void install(boolean night){
        dark=night;Palette p=palette();
        Font font=new Font(Font.SANS_SERIF,Font.PLAIN,12);
        for(String key:new String[]{"Label","Button","ToggleButton","CheckBox","ComboBox","TabbedPane","Table","TableHeader","Tree","ToolTip","Spinner","TextField","FormattedTextField","OptionPane","MenuItem","FileChooser","ProgressBar"}){
            UIManager.put(key+".font",font);UIManager.put(key+".foreground",p.text);UIManager.put(key+".background",p.surface);
        }
        for(String key:new String[]{"Panel.background","Viewport.background","ScrollPane.background","OptionPane.background"})UIManager.put(key,p.surface);
        UIManager.put("control",p.surface);UIManager.put("text",p.surface);UIManager.put("textText",p.text);UIManager.put("textHighlight",p.selected);UIManager.put("textHighlightText",p.text);
        UIManager.put("TextArea.background",p.input);UIManager.put("TextArea.foreground",p.text);UIManager.put("TextField.caretForeground",p.text);UIManager.put("FormattedTextField.caretForeground",p.text);
        UIManager.put("ComboBox.selectionBackground",p.selected);UIManager.put("ComboBox.selectionForeground",p.text);UIManager.put("List.selectionBackground",p.selected);UIManager.put("List.selectionForeground",p.text);UIManager.put("List.background",p.surface);UIManager.put("List.foreground",p.text);
        UIManager.put("ToolTip.background",p.surface);UIManager.put("ToolTip.foreground",p.text);UIManager.put("ToolTip.border",new CompoundBorder(new LineBorder(p.line),new EmptyBorder(7,9,7,9)));
        UIManager.put("OptionPane.messageForeground",p.text);UIManager.put("Button.disabledText",p.muted);UIManager.put("CheckBox.disabledText",p.muted);UIManager.put("ComboBox.disabledForeground",p.muted);
        UIManager.put("ScrollBar.width",11);UIManager.put("ProgressBar.foreground",p.accent);UIManager.put("ProgressBar.background",p.input);UIManager.put("ProgressBar.selectionForeground",p.accentText);UIManager.put("ProgressBar.selectionBackground",p.text);
        ToolTipManager.sharedInstance().setInitialDelay(300);ToolTipManager.sharedInstance().setDismissDelay(15000);
    }
    public static void toggle(){install(!dark);for(Window window:Window.getWindows()){apply(window);window.repaint();}}
    public static <T extends JComponent> T tip(T component,String description){component.setToolTipText("<html><body style='width:280px'>"+description+"</body></html>");component.getAccessibleContext().setAccessibleDescription(description.replaceAll("<[^>]*>"," "));return component;}
    public static void muted(JComponent component){component.putClientProperty("tone","muted");component.setForeground(palette().muted);}
    public static void primary(AbstractButton button){button.putClientProperty("primary",true);}
    public static Color alpha(Color c,int a){return new Color(c.getRed(),c.getGreen(),c.getBlue(),a);}
    public static void apply(Component component){apply(component,false);}
    private static void apply(Component component,boolean hero){
        Palette p=palette();
        if(component instanceof JComponent c){
            if(c.getClientProperty("tone")==null&&new Color(120,130,121).equals(c.getForeground()))c.putClientProperty("tone","muted");
            hero=hero||Boolean.TRUE.equals(c.getClientProperty("hero"));
            if(component instanceof JButton theme&&Boolean.TRUE.equals(c.getClientProperty("themeToggle")))theme.setText(dark?"Light mode":"Dark mode");
            c.setForeground(hero?hex(0xecf5df):"muted".equals(c.getClientProperty("tone"))?p.muted:p.text);
        }
        if(component instanceof Window w)w.setBackground(p.background);
        if(component instanceof JPanel panel){if(panel.isOpaque())panel.setBackground(Boolean.TRUE.equals(panel.getClientProperty("root"))?p.background:p.surface);}
        if(component instanceof EcosystemView)component.setBackground(HABITAT);
        if(component instanceof JButton button&&!"ComboBox.arrowButton".equals(button.getName())){if(!(button.getUI() instanceof SoftButtonUI))button.setUI(new SoftButtonUI());button.setOpaque(false);button.setContentAreaFilled(false);button.setBorder(new EmptyBorder(9,13,9,13));button.setRolloverEnabled(true);button.setFont(new Font(Font.SANS_SERIF,Font.BOLD,11));}
        else if(component instanceof JCheckBox check){check.setOpaque(false);check.setIcon(new CheckIcon());check.setSelectedIcon(new CheckIcon());check.setIconTextGap(7);check.setBorder(new EmptyBorder(5,2,5,4));check.setFocusPainted(false);}
        if(component instanceof JComboBox<?> combo){if(!(combo.getUI() instanceof SoftComboUI))combo.setUI(new SoftComboUI());combo.setBackground(p.input);combo.setForeground(p.text);combo.setBorder(new RoundBorder());combo.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,11));}
        if(component instanceof JSlider slider){if(!(slider.getUI() instanceof SoftSliderUI))slider.setUI(new SoftSliderUI(slider));slider.setOpaque(false);slider.setForeground(p.accent);}
        if(component instanceof JTabbedPane tabs){if(!(tabs.getUI() instanceof SoftTabsUI))tabs.setUI(new SoftTabsUI());tabs.setBackground(p.surface);tabs.setForeground(p.muted);tabs.setFont(new Font(Font.SANS_SERIF,Font.BOLD,11));}
        if(component instanceof JScrollPane scroll){scroll.setBorder(BorderFactory.createEmptyBorder());scroll.getViewport().setBackground(p.surface);}
        if(component instanceof JViewport viewport)viewport.setBackground(p.surface);
        if(component instanceof JScrollBar bar){if(!(bar.getUI() instanceof SoftScrollUI))bar.setUI(new SoftScrollUI());bar.setPreferredSize(new Dimension(10,10));bar.setBackground(p.surface);}
        if(component instanceof javax.swing.text.JTextComponent text){text.setBackground(p.input);text.setForeground(p.text);text.setCaretColor(p.text);text.setSelectionColor(p.selected);text.setSelectedTextColor(p.text);if(text instanceof JTextArea)text.setBorder(new EmptyBorder(10,10,10,10));else if(text.getParent() instanceof JSpinner.DefaultEditor)text.setBorder(new EmptyBorder(6,7,6,7));else text.setBorder(new CompoundBorder(new RoundBorder(),new EmptyBorder(5,7,5,7)));}
        if(component instanceof JSpinner spinner){if(!(spinner.getUI() instanceof SoftSpinnerUI))spinner.setUI(new SoftSpinnerUI());spinner.setBackground(p.input);spinner.setBorder(new RoundBorder());}
        if(component instanceof JTable table){table.setBackground(p.surface);table.setForeground(p.text);table.setSelectionBackground(p.selected);table.setSelectionForeground(p.text);table.setGridColor(p.line);table.setShowVerticalLines(false);table.setRowHeight(29);table.getTableHeader().setBackground(p.input);table.getTableHeader().setForeground(p.muted);table.getTableHeader().setFont(new Font(Font.SANS_SERIF,Font.BOLD,10));}
        if(component instanceof JTree tree){tree.setBackground(p.surface);tree.setForeground(p.text);tree.setRowHeight(26);DefaultTreeCellRenderer r=new DefaultTreeCellRenderer();r.setBackgroundNonSelectionColor(p.surface);r.setBackgroundSelectionColor(p.selected);r.setTextNonSelectionColor(p.text);r.setTextSelectionColor(p.text);r.setBorderSelectionColor(p.line);r.setLeafIcon(null);r.setClosedIcon(null);r.setOpenIcon(null);tree.setCellRenderer(r);}
        if(component instanceof JProgressBar progress){if(!(progress.getUI() instanceof SoftProgressUI))progress.setUI(new SoftProgressUI());progress.setForeground(p.accent);progress.setBackground(p.input);progress.setBorder(new EmptyBorder(2,2,2,2));}
        if(component instanceof Container parent)for(Component child:parent.getComponents())apply(child,hero);
    }
    public static final class Card extends JPanel {
        public Card(LayoutManager layout,int padding){super(layout);setOpaque(false);setBorder(new EmptyBorder(padding,padding,padding,padding));}
        @Override public Dimension getMaximumSize(){return new Dimension(Integer.MAX_VALUE,getPreferredSize().height);}
        @Override protected void paintComponent(Graphics graphics){Graphics2D g=(Graphics2D)graphics.create();EcosystemView.smooth(g);Palette p=palette();boolean hero=Boolean.TRUE.equals(getClientProperty("hero"));g.setColor(hero?new Color(49,82,62):p.surface);g.fillRoundRect(0,0,getWidth()-1,getHeight()-1,22,22);g.setColor(hero?new Color(74,108,76):p.line);g.drawRoundRect(0,0,getWidth()-1,getHeight()-1,22,22);g.dispose();super.paintComponent(graphics);}
    }
    private static final class RoundBorder extends AbstractBorder {
        @Override public Insets getBorderInsets(Component c){return new Insets(3,6,3,6);}
        @Override public void paintBorder(Component c,Graphics graphics,int x,int y,int w,int h){Graphics2D g=(Graphics2D)graphics.create();EcosystemView.smooth(g);g.setColor(c.isFocusOwner()?palette().accent:palette().line);g.drawRoundRect(x,y,w-1,h-1,10,10);g.dispose();}
    }
    private static final class SoftButtonUI extends BasicButtonUI {
        @Override public void paint(Graphics graphics,JComponent component){AbstractButton button=(AbstractButton)component;Graphics2D g=(Graphics2D)graphics.create();EcosystemView.smooth(g);Palette p=palette();boolean primary=Boolean.TRUE.equals(button.getClientProperty("primary"));boolean selected=button.isSelected()||button.getModel().isPressed();
            Color bg=primary?p.accent:selected?p.selected:button.getModel().isRollover()?p.hover:p.input;g.setColor(bg);g.fillRoundRect(0,0,button.getWidth()-1,button.getHeight()-1,12,12);g.setColor(button.hasFocus()?p.accent:p.line);g.drawRoundRect(0,0,button.getWidth()-1,button.getHeight()-1,12,12);g.dispose();button.setForeground(!button.isEnabled()?p.muted:primary?p.accentText:p.text);super.paint(graphics,component);}
        @Override protected void paintButtonPressed(Graphics g,AbstractButton b){}
        @Override protected void paintText(Graphics g,AbstractButton b,Rectangle r,String text){g.setColor(b.isEnabled()?b.getForeground():palette().muted);g.setFont(b.getFont());BasicGraphicsUtils.drawStringUnderlineCharAt(g,text,-1,r.x,r.y+g.getFontMetrics().getAscent());}
    }
    private static final class CheckIcon implements Icon {
        public int getIconWidth(){return 16;}public int getIconHeight(){return 16;}
        public void paintIcon(Component c,Graphics graphics,int x,int y){Graphics2D g=(Graphics2D)graphics.create();EcosystemView.smooth(g);boolean on=((AbstractButton)c).isSelected();g.setColor(on?palette().accent:palette().input);g.fillRoundRect(x,y,15,15,5,5);g.setColor(on?palette().accent:palette().muted);g.drawRoundRect(x,y,15,15,5,5);if(on){g.setColor(palette().accentText);g.setStroke(new BasicStroke(1.8f));g.drawLine(x+3,y+8,x+6,y+11);g.drawLine(x+6,y+11,x+12,y+4);}g.dispose();}
    }
    private static final class SoftComboUI extends BasicComboBoxUI {
        @Override protected JButton createArrowButton(){JButton arrow=new JButton(){
            @Override protected void paintComponent(Graphics graphics){Graphics2D g=(Graphics2D)graphics.create();EcosystemView.smooth(g);g.setColor(palette().input);g.fillRect(0,0,getWidth(),getHeight());g.setColor(palette().muted);int x=getWidth()/2,y=getHeight()/2;g.setStroke(new BasicStroke(1.5f));g.drawLine(x-4,y-2,x,y+2);g.drawLine(x,y+2,x+4,y-2);g.dispose();}
        };arrow.setName("ComboBox.arrowButton");arrow.setPreferredSize(new Dimension(24,28));arrow.setBorder(null);return arrow;}
        @Override public void paintCurrentValueBackground(Graphics g,Rectangle bounds,boolean focus){g.setColor(palette().input);g.fillRect(bounds.x,bounds.y,bounds.width,bounds.height);}
    }
    private static final class SoftSpinnerUI extends BasicSpinnerUI {
        private JButton arrow(boolean up){JButton button=new JButton(){
            @Override protected void paintComponent(Graphics graphics){Graphics2D g=(Graphics2D)graphics.create();EcosystemView.smooth(g);g.setColor(palette().input);g.fillRect(0,0,getWidth(),getHeight());g.setColor(isEnabled()?palette().muted:palette().line);g.setStroke(new BasicStroke(1.3f));int x=getWidth()/2,y=getHeight()/2;g.drawLine(x-3,y+(up?1:-1),x,y+(up?-2:2));g.drawLine(x,y+(up?-2:2),x+3,y+(up?1:-1));g.dispose();}
        };button.setName("ComboBox.arrowButton");button.setFocusable(false);button.setBorder(null);button.setPreferredSize(new Dimension(20,13));return button;}
        @Override protected Component createNextButton(){JButton button=arrow(true);installNextButtonListeners(button);return button;}
        @Override protected Component createPreviousButton(){JButton button=arrow(false);installPreviousButtonListeners(button);return button;}
    }
    private static final class SoftSliderUI extends BasicSliderUI {
        SoftSliderUI(JSlider slider){super(slider);}
        @Override public void paintTrack(Graphics graphics){Graphics2D g=(Graphics2D)graphics.create();EcosystemView.smooth(g);int y=trackRect.y+trackRect.height/2;g.setColor(palette().line);g.fillRoundRect(trackRect.x,y-2,trackRect.width,4,4,4);g.setColor(palette().accent);g.fillRoundRect(trackRect.x,y-2,Math.max(0,thumbRect.x+thumbRect.width/2-trackRect.x),4,4,4);g.dispose();}
        @Override public void paintThumb(Graphics graphics){Graphics2D g=(Graphics2D)graphics.create();EcosystemView.smooth(g);g.setColor(palette().accent);g.fillOval(thumbRect.x+2,thumbRect.y+3,12,12);g.dispose();}
        @Override public void paintFocus(Graphics g){}
        @Override protected Dimension getThumbSize(){return new Dimension(16,18);}
    }
    private static final class SoftTabsUI extends BasicTabbedPaneUI {
        @Override protected void installDefaults(){super.installDefaults();tabInsets=new Insets(11,12,11,12);contentBorderInsets=new Insets(10,0,0,0);tabAreaInsets=new Insets(0,0,0,0);}
        @Override protected boolean shouldRotateTabRuns(int placement){return false;}
        @Override protected void paintTabBackground(Graphics graphics,int placement,int index,int x,int y,int w,int h,boolean selected){Graphics2D g=(Graphics2D)graphics.create();EcosystemView.smooth(g);g.setColor(selected?palette().selected:palette().surface);g.fillRoundRect(x+2,y+2,w-4,h-4,10,10);g.dispose();}
        @Override protected void paintTabBorder(Graphics g,int placement,int index,int x,int y,int w,int h,boolean selected){}
        @Override protected void paintContentBorder(Graphics g,int placement,int selected){}
        @Override protected void paintFocusIndicator(Graphics g,int placement,Rectangle[] rects,int index,Rectangle icon,Rectangle text,boolean selected){}
        @Override protected void paintText(Graphics g,int placement,Font font,FontMetrics metrics,int index,String title,Rectangle text,boolean selected){g.setFont(font);g.setColor(selected?palette().text:palette().muted);g.drawString(title,text.x,text.y+metrics.getAscent());}
    }
    public static final class Stack extends JPanel implements Scrollable {
        public Stack(){setOpaque(false);setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));}
        public Dimension getPreferredScrollableViewportSize(){return getPreferredSize();}
        public int getScrollableUnitIncrement(Rectangle visible,int orientation,int direction){return 16;}
        public int getScrollableBlockIncrement(Rectangle visible,int orientation,int direction){return Math.max(16,visible.height-32);}
        public boolean getScrollableTracksViewportWidth(){return true;}
        public boolean getScrollableTracksViewportHeight(){return false;}
    }
    private static final class SoftProgressUI extends BasicProgressBarUI {
        @Override protected Color getSelectionForeground(){return palette().accentText;}
        @Override protected Color getSelectionBackground(){return palette().text;}
    }
    private static final class SoftScrollUI extends BasicScrollBarUI {
        @Override protected void configureScrollBarColors(){thumbColor=palette().line;trackColor=palette().surface;}
        @Override protected JButton createDecreaseButton(int orientation){return zero();}@Override protected JButton createIncreaseButton(int orientation){return zero();}
        private JButton zero(){JButton b=new JButton();b.setPreferredSize(new Dimension(0,0));b.setMinimumSize(new Dimension(0,0));b.setMaximumSize(new Dimension(0,0));return b;}
        @Override protected void paintTrack(Graphics graphics,JComponent c,Rectangle r){graphics.setColor(palette().surface);graphics.fillRect(r.x,r.y,r.width,r.height);}
        @Override protected void paintThumb(Graphics graphics,JComponent c,Rectangle r){Graphics2D g=(Graphics2D)graphics.create();EcosystemView.smooth(g);g.setColor(palette().line);g.fillRoundRect(r.x+2,r.y+2,Math.max(3,r.width-4),Math.max(3,r.height-4),7,7);g.dispose();}
    }
}
