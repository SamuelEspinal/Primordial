import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Objects;
import java.util.function.Consumer;

/** A small, reusable tour. Reading it never changes the simulation. */
public final class GettingStartedPanel extends JPanel {
    public static final String TITLE = "Getting started";
    public record Actions(Runnable pause, Runnable newWorld, Runnable family,
                          Runnable parallel, Runnable experiments, Runnable rules, Runnable close) {
        public Actions {
            Objects.requireNonNull(pause); Objects.requireNonNull(newWorld); Objects.requireNonNull(family);
            Objects.requireNonNull(parallel); Objects.requireNonNull(experiments);
            Objects.requireNonNull(rules); Objects.requireNonNull(close);
        }
    }
    private record Step(String topic, String title, String copy, String hint, String action) {}
    private static final Step[] STEPS = {
        new Step("Look around", "Small lives, growing lineages",
            "Creatures spend energy swimming and finding food. Well-fed adults lay eggs. Their offspring inherit a brain and body, with small mutations. Over time, some lineages thrive and others disappear.\n\nMint ovals are prey; coral arrows are hunters. Your first world starts without hunters, giving you room to learn.",
            "Pause, click a swimmer, and turn on Senses. Its energy, diet and brain appear in Inspect. Trails show where it has been. Use Resume when you’re ready.",
            "Pause the dish"),
        new Step("Make a world", "Start simple, then make it yours",
            "Choose New World and pick a preset. Continuous ecology is a good first experiment; Steady observation offers a gentler food supply.\n\nAdvanced Settings lets you change food, populations, mutation, seasons, bodies and map size. The seed makes a starting setup repeatable.",
            "Change one setting at a time. Creating a world replaces the current dish, so Save world first if you want to keep it. New worlds open paused for review.",
            "Open New World…"),
        new Step("Choose evolution", "Two ways to see change",
            "Continuous reproduction lets well-fed adults have offspring while parents keep living. Extinction is a real outcome; the world does not automatically refill its population.\n\nGeneration selection breeds a new population from the strongest candidates after each round. It is assisted selection.",
            "Try the Selection exploration preset. Inspect each generation holds the finished round: inspect Best prey or Best hunter, then Breed & continue. Higher G means deeper ancestry, not necessarily smarter behavior.",
            "Choose an evolution preset…"),
        new Step("Meet a clan", "They’re more than a census",
            "Each creature has a name and belongs to a clan. Click one to see its age, energy, diet and offspring. Family lets you search names or clans and follow relatives on a card tree.\n\nDashboard and Open graphs show the population’s story in larger windows. You can leave those windows open beside the dish.",
            "Look for repeated births and surviving families. A high population or generation number alone does not prove improvement. Try comparing a creature with its founding ancestor from Dashboard.",
            "Open Family ↗"),
        new Step("Run worlds together", "Give evolution a few more chances",
            "Let the live world evolve, then open Parallel worlds. Each island copies the same best brains and bodies (up to 12 parents for each role by default), then uses its own recorded seed. They evolve independently without automatically sharing brains.\n\nWorlds is how many islands you want; workers is how many run at once. Start with 2–4 worlds and 2 workers, then choose an output folder.",
            "Start islands begins the runs. The main Pause button affects only the dish; use Stop selected or Stop all for islands. Open checkpoint loads a copy into the dish while its running island continues separately.",
            "Open Parallel worlds ↗"),
        new Step("Keep discoveries", "Save a world, ask a question",
            "Save world keeps the whole ecosystem so you can resume it. Save brain & body keeps one creature’s inherited brain and body for seeding a fresh population. They serve different purposes.\n\nExperiments compares fresh A/B worlds while changing one setting. Use it for questions like “Does more food help?” and compare several seeds. Background runs can keep CSV observations and checkpoints in their output folder.",
            "Your first session: inspect a swimmer → try a preset → watch a few generations → save a world. You can reopen this tour from Help whenever you need it.",
            "Open Experiments ↗")
    };

    private final Actions actions;
    private final JList<String> topics = new JList<>(java.util.Arrays.stream(STEPS).map(Step::topic).toArray(String[]::new));
    private final JLabel progress = new JLabel(), heading = new JLabel();
    private final JTextArea copy = text(15), hint = text(13);
    private final JButton back = new JButton("Back"), next = new JButton("Next"), tryStep = new JButton();
    private final JCheckBox showAtStartup = new JCheckBox("Show at every startup");
    private JScrollPane readingScroll;
    private int page;

    public GettingStartedPanel(Actions actions, boolean startup, Consumer<Boolean> startupChanged) {
        super(new BorderLayout(18, 16));
        this.actions = Objects.requireNonNull(actions);
        Objects.requireNonNull(startupChanged);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 16, 20));
        setPreferredSize(new Dimension(880, 640));

        JPanel introduction = new JPanel(new BorderLayout(0, 7));
        JLabel title = new JLabel("A tiny tour of Primordial");
        UiTheme.heading(title,23);
        introduction.add(title, BorderLayout.NORTH);
        JLabel subtitle = new JLabel("Six quick stops. Move this guide aside to try things in the dish.");
        UiTheme.muted(subtitle);
        introduction.add(subtitle, BorderLayout.CENTER);
        add(introduction, BorderLayout.NORTH);

        topics.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        topics.setOpaque(false);
        topics.setFixedCellHeight(43);
        topics.setCellRenderer((list, value, index, selected, focus) -> {
            JLabel label = new JLabel(String.format("%02d   %s", index + 1, value));
            label.setOpaque(true);
            label.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
            label.setBackground(selected ? UiTheme.palette().selected() : UiTheme.palette().surface());
            label.setForeground(UiTheme.palette().text());
            label.setFont(UiTheme.bodyFont(selected ? Font.BOLD : Font.PLAIN,13));
            return label;
        });
        UiTheme.tip(topics, "Jump to a topic, or use Back and Next. Reading the tour does not change your world.");
        topics.setPreferredSize(new Dimension(190, 270));
        JPanel navigation = new JPanel(new BorderLayout());
        JScrollPane topicScroll = new JScrollPane(topics);
        topicScroll.setBorder(BorderFactory.createEmptyBorder());
        navigation.setPreferredSize(new Dimension(200, 270));
        navigation.add(topicScroll, BorderLayout.CENTER);

        JPanel article = new JPanel(new BorderLayout(0, 12));
        JPanel articleTitle = new JPanel(new BorderLayout(0, 8));
        progress.setFont(UiTheme.bodyFont(Font.BOLD,12));
        UiTheme.muted(progress);
        UiTheme.heading(heading,21);
        articleTitle.add(progress, BorderLayout.NORTH);
        articleTitle.add(heading, BorderLayout.CENTER);
        article.add(articleTitle, BorderLayout.NORTH);

        JPanel reading = new ReadingPanel();
        reading.add(copy, BorderLayout.NORTH);
        UiTheme.Card practice = new UiTheme.Card(new BorderLayout(0, 8), 12);
        JLabel practiceTitle = new JLabel("TRY THIS");
        practiceTitle.setFont(UiTheme.bodyFont(Font.BOLD,12));
        practice.add(practiceTitle, BorderLayout.NORTH);
        practice.add(hint, BorderLayout.CENTER);
        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        actionRow.setOpaque(false);
        actionRow.add(tryStep);
        practice.add(actionRow, BorderLayout.SOUTH);
        reading.add(practice, BorderLayout.CENTER);
        JScrollPane scroll = new JScrollPane(reading);
        readingScroll = scroll;
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        article.add(scroll, BorderLayout.CENTER);
        JPanel center = new JPanel(new BorderLayout(18, 0));
        center.add(navigation, BorderLayout.WEST);
        center.add(article, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout(0, 8));
        showAtStartup.setSelected(startup);
        UiTheme.tip(showAtStartup, "The tour opens once for new users. Enable this to see it on every launch; it always remains available under Help.");
        showAtStartup.addActionListener(e -> startupChanged.accept(showAtStartup.isSelected()));
        footer.add(showAtStartup, BorderLayout.NORTH);
        JButton rules = new JButton("Detailed rules ↗"), close = new JButton("Close guide");
        UiTheme.tip(rules, "Open the full rules in a separate window when you want more detail.");
        UiTheme.tip(close, "Close the tour at any point. Reopen it from Help → Getting started.");
        UiTheme.tip(back, "Return to the previous topic.");
        UiTheme.tip(next, "Continue to the next topic, or finish the tour.");
        UiTheme.primary(next);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.add(close); buttons.add(back); buttons.add(next);
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(rules, BorderLayout.WEST); bottom.add(buttons, BorderLayout.EAST);
        footer.add(bottom, BorderLayout.SOUTH);
        add(footer, BorderLayout.SOUTH);
        rules.addActionListener(e -> actions.rules.run());
        close.addActionListener(e -> actions.close.run());
        back.addActionListener(e -> selectPage(page - 1));
        next.addActionListener(e -> { if (page == STEPS.length - 1) actions.close.run(); else selectPage(page + 1); });
        tryStep.addActionListener(e -> {
            switch (page) {
                case 0 -> actions.pause.run();
                case 1, 2 -> actions.newWorld.run();
                case 3 -> actions.family.run();
                case 4 -> actions.parallel.run();
                case 5 -> actions.experiments.run();
            }
        });
        topics.addListSelectionListener(e -> { if (!e.getValueIsAdjusting() && topics.getSelectedIndex() >= 0) showPage(topics.getSelectedIndex()); });
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("ESCAPE"), "closeGuide");
        getActionMap().put("closeGuide", new AbstractAction() { public void actionPerformed(ActionEvent e) { actions.close.run(); } });
        selectPage(0);
        UiTheme.apply(this);
    }

    private static JTextArea text(int size) {
        JTextArea area = new JTextArea();
        area.setFont(UiTheme.bodyFont(Font.PLAIN,size));
        area.setLineWrap(true); area.setWrapStyleWord(true); area.setEditable(false);
        return area;
    }
    private static final class ReadingPanel extends JPanel implements Scrollable {
        ReadingPanel() { super(new BorderLayout(0, 16)); }
        public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) { return 18; }
        public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) { return Math.max(18, visible.height - 18); }
        public boolean getScrollableTracksViewportWidth() { return true; }
        public boolean getScrollableTracksViewportHeight() { return false; }
    }
    void selectPage(int index) { topics.setSelectedIndex(Math.max(0, Math.min(STEPS.length - 1, index))); topics.ensureIndexIsVisible(topics.getSelectedIndex()); }
    private void showPage(int index) {
        page = index;
        Step step = STEPS[index];
        progress.setText("STEP " + (index + 1) + " OF " + STEPS.length);
        heading.setText(step.title);
        copy.setText(step.copy); copy.setCaretPosition(0);
        hint.setText(step.hint); hint.setCaretPosition(0);
        tryStep.setText(step.action);
        UiTheme.tip(tryStep, "Open the real control for this topic. Nothing runs automatically when you read a page.");
        back.setEnabled(index > 0);
        next.setText(index == STEPS.length - 1 ? "Finish" : "Next");
        revalidate(); repaint();
        SwingUtilities.invokeLater(() -> readingScroll.getVerticalScrollBar().setValue(0));
    }
}
