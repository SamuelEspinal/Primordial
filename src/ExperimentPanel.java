import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.nio.file.Files;
import java.util.concurrent.CancellationException;

/** Background experiment runner, isolated from the interactive ecosystem. */
public final class ExperimentPanel extends JPanel {
    private final JComboBox<ExperimentRunner.Variable> variable=new JComboBox<>(ExperimentRunner.Variable.values());
    private final JSpinner baseline=new JSpinner(new SpinnerNumberModel(55,0,100,1)),alternative=new JSpinner(new SpinnerNumberModel(25,0,100,1));
    private final JSpinner runs=new JSpinner(new SpinnerNumberModel(3,1,10,1)),seconds=new JSpinner(new SpinnerNumberModel(60,5,300,5));
    private final JSpinner seed=new JSpinner(new SpinnerNumberModel(42L,0L,Long.MAX_VALUE-10,1L));
    private final JButton start=new JButton("Run paired worlds"),cancel=new JButton("Cancel"),export=new JButton("Export CSV…");
    private final JProgressBar progress=new JProgressBar(0,100);
    private final JTextArea results=new JTextArea("Choose one setting. Each seed runs twice from an identical starting world.\n\nFood: 0–100\nMutation: 0–50 percent\nSeasons / predators: 0 off, 1 on\n\nExperiments include eggs, diet evolution, and genetic grouping. They use fresh default maps and do not modify the live world.");
    private SwingWorker<ExperimentRunner.Report,Void> worker;
    private ExperimentRunner.Report report;
    public ExperimentPanel(){
        super(new BorderLayout(8,10));setOpaque(false);setBorder(new EmptyBorder(10,8,8,8));
        JPanel form=new JPanel(new GridLayout(0,2,5,7));form.setOpaque(false);
        form.add(new JLabel("Change one setting"));form.add(variable);
        form.add(new JLabel("Baseline A"));form.add(baseline);form.add(new JLabel("Alternative B"));form.add(alternative);
        form.add(new JLabel("Paired seeds"));form.add(runs);form.add(new JLabel("Seconds / world"));form.add(seconds);form.add(new JLabel("First seed"));form.add(seed);
        JPanel buttons=new JPanel(new GridLayout(0,1,0,5));buttons.setOpaque(false);buttons.add(start);buttons.add(cancel);buttons.add(progress);buttons.add(export);
        JPanel top=new JPanel(new BorderLayout(0,10));top.setOpaque(false);top.add(form,BorderLayout.CENTER);top.add(buttons,BorderLayout.SOUTH);add(top,BorderLayout.NORTH);
        results.setEditable(false);results.setLineWrap(true);results.setWrapStyleWord(true);results.setFont(new Font("Monospaced",Font.PLAIN,11));add(new JScrollPane(results),BorderLayout.CENTER);
        UiTheme.primary(start);
        UiHelp.choices(variable,i->new String[]{"Vary food replenishment capacity (0–100). Both conditions start with the same food and populations.","Vary the chance of inherited mutation (0–50%). Existing founders remain identical between conditions.","Compare seasons off (0) against on (1). Seasons shift food supply and the fertile patch.","Compare hunters absent (0) against present (1). Removed initial hunters do not create corpses or count as deaths."}[i]);
        UiTheme.tip(baseline,"Value used in condition A. Food: 0–100; mutation: 0–50 percent; seasons and predators: 0 off or 1 on.");
        UiTheme.tip(alternative,"Value used in condition B. Only this selected setting differs from A; the initial world and random state are paired.");
        UiTheme.tip(runs,"Number of paired seeds, from 1 to 10. Each seed produces two worlds, one per condition. More pairs make averages less sensitive to chance.");
        UiTheme.tip(seconds,"Length of each world in simulated seconds, from 5 to 300. Longer experiments need more processing time.");
        UiTheme.tip(seed,"First random seed. Repeating the same settings and seeds reproduces the experiment. Further pairs use consecutive seeds.");
        UiTheme.tip(start,"Run independent paired worlds in the background. This does not replace or modify your live ecosystem.");
        UiTheme.tip(cancel,"Stop the current experiment. Completed reports remain available for export; the live world is unaffected.");
        UiTheme.tip(export,"Save the last completed report as CSV, with one row per seed and condition. Cancelled partial runs are not exported.");
        UiTheme.tip(progress,"Percentage of condition runs completed. A pair consists of one A world and one B world.");
        UiTheme.tip(results,"Experiment summary and individual outcomes. Ending adults exclude eggs; complete extinction requires no adults and no eggs.");
        cancel.setEnabled(false);export.setEnabled(false);progress.setStringPainted(true);
        variable.addActionListener(e->{int index=variable.getSelectedIndex();double max=index==0?100:index==1?50:1;baseline.setModel(new SpinnerNumberModel(index==0?55:index==1?12:1,0,max,1));alternative.setModel(new SpinnerNumberModel(index==0?25:index==1?30:0,0,max,1));});
        start.addActionListener(e->start());cancel.addActionListener(e->{if(worker!=null)worker.cancel(true);});export.addActionListener(e->export());
    }
    private void start(){
        ExperimentRunner.Config config;
        try{for(JSpinner spinner:new JSpinner[]{baseline,alternative,runs,seconds,seed})spinner.commitEdit();
            config=new ExperimentRunner.Config((ExperimentRunner.Variable)variable.getSelectedItem(),((Number)baseline.getValue()).doubleValue(),((Number)alternative.getValue()).doubleValue(),((Number)runs.getValue()).intValue(),((Number)seconds.getValue()).intValue(),((Number)seed.getValue()).longValue());}
        catch(Exception ex){JOptionPane.showMessageDialog(this,ex.getMessage(),"Invalid experiment",JOptionPane.ERROR_MESSAGE);return;}
        busy(true);progress.setValue(0);results.setText("Running independent paired worlds…\nThe live simulation remains separate.\n");
        worker=new SwingWorker<>(){
            @Override protected ExperimentRunner.Report doInBackground()throws Exception{return ExperimentRunner.run(config,this::isCancelled,this::setProgress);}
            @Override protected void done(){try{report=get();results.setText(report.summary());results.setCaretPosition(0);}catch(CancellationException ex){results.setText("Experiment cancelled. Your live world was not changed.");}catch(Exception ex){results.setText("Experiment failed: "+ex.getMessage());}finally{busy(false);}}
        };
        worker.addPropertyChangeListener(e->{if("progress".equals(e.getPropertyName()))progress.setValue((Integer)e.getNewValue());});worker.execute();
    }
    private void busy(boolean running){for(JComponent component:new JComponent[]{variable,baseline,alternative,runs,seconds,seed,start})component.setEnabled(!running);cancel.setEnabled(running);export.setEnabled(!running&&report!=null);}
    private void export(){
        if(report==null)return;JFileChooser chooser=new JFileChooser();chooser.setSelectedFile(new java.io.File("ecosystem-experiment.csv"));
        if(chooser.showSaveDialog(this)!=JFileChooser.APPROVE_OPTION)return;
        if(chooser.getSelectedFile().exists()&&JOptionPane.showConfirmDialog(this,"Replace this CSV?","Export",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;
        try{Files.writeString(chooser.getSelectedFile().toPath(),report.csv());}catch(Exception ex){JOptionPane.showMessageDialog(this,ex.getMessage(),"Could not export",JOptionPane.ERROR_MESSAGE);}
    }
}
