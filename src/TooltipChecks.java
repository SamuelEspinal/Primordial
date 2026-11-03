import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.concurrent.atomic.AtomicInteger;

/** Exercise actual hover listeners/timers; run without headless mode to also check popup delivery. */
public final class TooltipChecks {
    public static void main(String[] args) throws Exception { run(); }

    static void run() throws Exception {
        AtomicInteger queries=new AtomicInteger(),popups=new AtomicInteger();
        JPanel root=new JPanel();
        JComponent dynamic=new JPanel(){
            @Override public boolean isShowing(){return GraphicsEnvironment.isHeadless()||super.isShowing();}
            @Override public String getToolTipText(MouseEvent event){queries.incrementAndGet();return "Dynamic creature help";}
        };
        JButton toggle=new JButton("Toggle help");
        ToolTipManager manager=ToolTipManager.sharedInstance();
        int initial=manager.getInitialDelay(),dismiss=manager.getDismissDelay();
        boolean enabled=manager.isEnabled(),dark=UiTheme.isDark();Object mode=UIManager.get("ToolTipManager.enableToolTipMode");
        PopupFactory original=PopupFactory.getSharedInstance();JFrame[] window={null};
        try {
            SwingUtilities.invokeAndWait(()->{
                UiTheme.tip(dynamic,"Dynamic help registration");dynamic.setPreferredSize(new Dimension(250,160));
                toggle.addActionListener(e->UiTheme.setTooltipsEnabled(!manager.isEnabled(),root));
                root.add(toggle);root.add(dynamic);dynamic.setSize(250,160);
                manager.setInitialDelay(40);manager.setDismissDelay(15000);UiTheme.setTooltipsEnabled(true,root);
                if(GraphicsEnvironment.isHeadless())UIManager.put("ToolTipManager.enableToolTipMode","activeApplication");
                else {
                    UIManager.put("ToolTipManager.enableToolTipMode","allWindows");
                    PopupFactory.setSharedInstance(new PopupFactory(){
                        @Override public Popup getPopup(Component owner,Component contents,int x,int y){
                            return new Popup(){@Override public void show(){popups.incrementAndGet();}@Override public void hide(){}};
                        }
                    });
                    window[0]=new JFrame("Primordial tooltip check");window[0].setContentPane(root);window[0].pack();window[0].setLocationRelativeTo(null);window[0].setVisible(true);
                }
                hover(dynamic,true);
            });
            settle();require(queries.get()>0,"Initial hover never requested dynamic help");
            if(!GraphicsEnvironment.isHeadless())require(popups.get()>0,"Initial hover never displayed a popup");
            for(int cycle=0;cycle<3;cycle++){
                SwingUtilities.invokeAndWait(()->{toggle.doClick(0);hover(dynamic,true);});
                int before=popups.get();settle();require(popups.get()==before,"Disabled help displayed a popup");
                // No fresh ENTERED event: turning help back on must restore mouse-move detection.
                SwingUtilities.invokeAndWait(()->{
                    dynamic.dispatchEvent(event(dynamic,MouseEvent.MOUSE_EXITED,-1,-1));toggle.doClick(0);
                    UiTheme.install(!UiTheme.isDark());UiTheme.apply(root);manager.setInitialDelay(40);
                });
                int oldQueries=queries.get(),oldPopups=popups.get();
                SwingUtilities.invokeAndWait(()->hover(dynamic,false));settle();
                require(manager.isEnabled()&&queries.get()>oldQueries,"Help did not recover after off/on and theme change");
                if(!GraphicsEnvironment.isHeadless())require(popups.get()>oldPopups,"Restored hover never displayed a popup");
            }
            System.out.println("PASS: repeated tooltip off/on restores dynamic hover help across themes"+(GraphicsEnvironment.isHeadless()?".":" and delivers real Swing popups."));
        } finally {
            SwingUtilities.invokeAndWait(()->{
                UiTheme.setTooltipsEnabled(false,root);manager.unregisterComponent(dynamic);dynamic.removeMouseMotionListener(manager);
                UiTheme.install(dark);manager.setInitialDelay(initial);manager.setDismissDelay(dismiss);manager.setEnabled(enabled);
                UIManager.put("ToolTipManager.enableToolTipMode",mode);PopupFactory.setSharedInstance(original);
                if(window[0]!=null)window[0].dispose();
            });
        }
    }
    private static void hover(JComponent component,boolean enter){
        if(enter)component.dispatchEvent(event(component,MouseEvent.MOUSE_ENTERED,20,20));
        component.dispatchEvent(event(component,MouseEvent.MOUSE_MOVED,21,20));
        component.dispatchEvent(event(component,MouseEvent.MOUSE_MOVED,22,20));
    }
    private static MouseEvent event(JComponent component,int type,int x,int y){return new MouseEvent(component,type,System.currentTimeMillis(),0,x,y,100+x,100+y,0,false,MouseEvent.NOBUTTON);}
    private static void settle() throws Exception {Thread.sleep(180);SwingUtilities.invokeAndWait(()->{});}
    private static void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    private TooltipChecks() {}
}
