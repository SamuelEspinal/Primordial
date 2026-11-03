import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.nio.file.Files;
import java.util.concurrent.CancellationException;

/** Background experiment runner, isolated from the interactive ecosystem. */
public final class ExperimentPanel extends JPanel {
    private final JComboBox<ExperimentRunner.Variable> variable=new JComboBox<>(ExperimentRunner.Variable.values());
    private final JSpinner baseline=new JSpinner(new SpinnerNumberModel(55,0,100,1)),alternative=new JSpinner(new SpinnerNumberModel(25,0,100,1));
    private final JSpinner runs=new JSpinner(new SpinnerNumberModel(3,1,32,1)),seconds=new JSpinner(new SpinnerNumberModel(1800,5,86400,5));
    private final JSpinner seed=new JSpinner(new SpinnerNumberModel(42L,0L,Long.MAX_VALUE-32,1L));
    private final JButton start=new JButton("Run paired worlds"),cancel=new JButton("Cancel"),export=new JButton("Export CSV…");
    private final JProgressBar progress=new JProgressBar(0,100);
    private final JTextArea results=new JTextArea("Choose one setting. Each seed runs twice from an identical starting world.\n\nFood: 0–100\nMutation: 0–100 percent\nSeasons / predators: 0 off, 1 on\n\nExperiments include eggs, diet evolution, and genetic grouping. They use fresh default maps and do not modify the live world.");
    private SwingWorker<ExperimentRunner.Report,Void> worker;
    private final java.util.concurrent.atomic.AtomicBoolean stop=new java.util.concurrent.atomic.AtomicBoolean();
    private ExperimentRunner.Report report;
    private final java.util.function.Supplier<WorldConfig> source;
    private WorldConfig custom;
    private final JSpinner workers=new JSpinner(new SpinnerNumberModel(2,1,8,1)),sample=new JSpinner(new SpinnerNumberModel(30,1,3600,1)),checkpoints=new JSpinner(new SpinnerNumberModel(300,1,3600,1));
    private final JCheckBox recording=new JCheckBox("Record CSV & checkpoints",true);
    private final JButton settings=new JButton("Advanced experiment settings…"),folder=new JButton("Output folder…");
    private java.nio.file.Path output=java.nio.file.Path.of("exports");
    public ExperimentPanel(){this(WorldConfig::new);}
    public ExperimentPanel(java.util.function.Supplier<WorldConfig> source){
        super(new BorderLayout(8,10));this.source=source;setOpaque(false);setBorder(new EmptyBorder(10,8,8,8));
        JPanel form=new JPanel(new GridLayout(0,4,8,8));form.setOpaque(false);
        form.add(new JLabel("Change one setting"));form.add(variable);
        form.add(new JLabel("Baseline A"));form.add(baseline);form.add(new JLabel("Alternative B"));form.add(alternative);
        form.add(new JLabel("Paired seeds"));form.add(runs);form.add(new JLabel("Seconds / world"));form.add(seconds);form.add(new JLabel("First seed"));form.add(seed);
        form.add(new JLabel("CPU workers"));form.add(workers);form.add(new JLabel("CSV interval (s)"));form.add(sample);form.add(new JLabel("Checkpoint interval (s)"));form.add(checkpoints);form.add(recording);form.add(settings);form.add(folder);
        UiTheme.tip(workers,"Run up to eight paired-seed jobs concurrently, with independent model instances.");UiTheme.tip(sample,"Period between streamed observations in simulated seconds.");UiTheme.tip(checkpoints,"Period between rolling exact world checkpoints.");UiTheme.tip(recording,"Write incremental observations and checkpoints so cancelled runs retain evidence.");UiTheme.tip(settings,"Customize common world settings; both paired treatments share this configuration.");UiTheme.tip(folder,"Choose the parent folder for timestamped experiment output directories.");
        settings.addActionListener(e->{var choice=WorldSettingsPanel.edit(this,custom==null?source.get():custom,((Number)seed.getValue()).longValue(),"Common experiment settings",false);if(choice!=null){custom=choice.config();seed.setValue(choice.seed());}});
        folder.addActionListener(e->{JFileChooser c=new JFileChooser();c.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);if(c.showSaveDialog(this)==JFileChooser.APPROVE_OPTION)output=c.getSelectedFile().toPath();});
        JPanel buttons=new JPanel(new GridLayout(1,0,8,5));buttons.setOpaque(false);buttons.add(start);buttons.add(cancel);buttons.add(progress);buttons.add(export);
        JPanel top=new JPanel(new BorderLayout(0,10));top.setOpaque(false);top.add(form,BorderLayout.CENTER);top.add(buttons,BorderLayout.SOUTH);add(top,BorderLayout.NORTH);
        results.setEditable(false);results.setLineWrap(true);results.setWrapStyleWord(true);results.setFont(new Font("Monospaced",Font.PLAIN,11));add(new JScrollPane(results),BorderLayout.CENTER);
        UiTheme.primary(start);
        UiHelp.choices(variable,i->"Compare "+variable.getItemAt(i)+" from the same seeded snapshot. Range 0–"+variable.getItemAt(i).maximum()+". Configure persistent sources before testing local quotas.");
        UiTheme.tip(baseline,"Value used in condition A. Food: 0–100; mutation: 0–100 percent; seasons and predators: 0 off or 1 on.");
        UiTheme.tip(alternative,"Value used in condition B. Only this selected setting differs from A; the initial world and random state are paired.");
        UiTheme.tip(runs,"Number of paired seeds, from 1 to 32. Each seed produces two worlds, one per condition. More pairs make averages less sensitive to chance.");
        UiTheme.tip(seconds,"Length of each world in simulated seconds, from 5 to 86400. Longer experiments need more processing time.");
        UiTheme.tip(seed,"First random seed. Repeating the same settings and seeds reproduces the experiment. Further pairs use consecutive seeds.");
        UiTheme.tip(start,"Run independent paired worlds in the background. This does not replace or modify your live ecosystem.");
        UiTheme.tip(cancel,"Stop the current experiment. Completed reports remain available for export; the live world is unaffected.");
        UiTheme.tip(export,"Save the last completed report as CSV, with one row per seed and condition. Cancelled partial runs are not exported.");
        UiTheme.tip(progress,"Percentage of condition runs completed. A pair consists of one A world and one B world.");
        UiTheme.tip(results,"Experiment summary and individual outcomes. Ending adults exclude eggs; complete extinction requires no adults and no eggs.");
        cancel.setEnabled(false);export.setEnabled(false);progress.setStringPainted(true);
        variable.addActionListener(e->{int index=variable.getSelectedIndex();double max=variable.getItemAt(index).maximum();baseline.setModel(new SpinnerNumberModel(index==0?55:index==1?12:index==4?10:0,0,max,1));alternative.setModel(new SpinnerNumberModel(index==0?25:index==1?30:index==4?5:index==5?30:index==6?120:1,0,max,1));});
        start.addActionListener(e->start());cancel.addActionListener(e->{stop.set(true);cancel.setEnabled(false);});export.addActionListener(e->export());
    }
    private void start(){
        ExperimentRunner.Config config;
        try{for(JSpinner spinner:new JSpinner[]{baseline,alternative,runs,seconds,seed,workers,sample,checkpoints})spinner.commitEdit();
            config=new ExperimentRunner.Config((ExperimentRunner.Variable)variable.getSelectedItem(),((Number)baseline.getValue()).doubleValue(),((Number)alternative.getValue()).doubleValue(),((Number)runs.getValue()).intValue(),((Number)seconds.getValue()).intValue(),((Number)seed.getValue()).longValue(),custom==null?source.get():custom,((Number)workers.getValue()).intValue(),recording.isSelected()?output.resolve("paired-"+System.currentTimeMillis()):null,((Number)sample.getValue()).intValue(),((Number)checkpoints.getValue()).intValue());}
        catch(Exception ex){JOptionPane.showMessageDialog(this,ex.getMessage(),"Invalid experiment",JOptionPane.ERROR_MESSAGE);return;}
        stop.set(false);busy(true);progress.setValue(0);results.setText("Running independent paired worlds…\nOutput: "+config.output()+"\nThe live simulation remains separate.\n");
        worker=new SwingWorker<>(){
            @Override protected ExperimentRunner.Report doInBackground()throws Exception{return ExperimentRunner.run(config,()->stop.get()||isCancelled(),this::setProgress);}
            @Override protected void done(){try{report=get();results.setText("Output: "+config.output()+"\n\n"+report.summary());results.setCaretPosition(0);}catch(CancellationException ex){results.setText("Experiment cancelled. Completed observations and rolling checkpoints remain in the selected output folder.");}catch(Exception ex){results.setText((stop.get()?"Experiment cancelled; partial files remain in "+config.output():"Experiment failed: "+ex.getMessage()));}finally{busy(false);}}
        };
        worker.addPropertyChangeListener(e->{if("progress".equals(e.getPropertyName()))progress.setValue((Integer)e.getNewValue());});worker.execute();
    }
    private void busy(boolean running){for(JComponent component:new JComponent[]{variable,baseline,alternative,runs,seconds,seed,workers,sample,checkpoints,settings,folder,recording,start})component.setEnabled(!running);cancel.setEnabled(running);export.setEnabled(!running&&report!=null);}
    private void export(){
        if(report==null)return;JFileChooser chooser=new JFileChooser();chooser.setSelectedFile(new java.io.File("ecosystem-experiment.csv"));
        if(chooser.showSaveDialog(this)!=JFileChooser.APPROVE_OPTION)return;
        if(chooser.getSelectedFile().exists()&&JOptionPane.showConfirmDialog(this,"Replace this CSV?","Export",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;
        try{Files.writeString(chooser.getSelectedFile().toPath(),report.csv());}catch(Exception ex){JOptionPane.showMessageDialog(this,ex.getMessage(),"Could not export",JOptionPane.ERROR_MESSAGE);}
    }
}
