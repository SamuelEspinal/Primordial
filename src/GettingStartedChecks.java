import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

/** Navigation must be passive; only an explicit shortcut may operate a real control. */
final class GettingStartedChecks {
    static void run() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                Main app = new Main(false);
                byte[] original = WorldIO.encode(app.sim);
                app.showGettingStarted();
                GettingStartedPanel guide = (GettingStartedPanel) app.dataPanels.get(GettingStartedPanel.TITLE);
                for (int page = 0; page < 6; page++) guide.selectPage(page);
                if (!Arrays.equals(original, WorldIO.encode(app.sim))) throw new AssertionError("Reading the guide changed the world");
                guide.selectPage(0); press(guide, "Pause the dish");
                if (!app.paused) throw new AssertionError("Guide shortcut did not pause the dish");
                guide.selectPage(3); press(guide, "Open Family ↗");
                guide.selectPage(4); press(guide, "Open Parallel worlds ↗");
                guide.selectPage(5); press(guide, "Open Experiments ↗");
                if (!app.dataPanels.keySet().containsAll(java.util.List.of("Family", "Parallel worlds", "Experiments"))) throw new AssertionError("Guide links did not open the existing views");
                if (!Arrays.equals(original, WorldIO.encode(app.sim))) throw new AssertionError("Guide view shortcuts changed biology or random state");
                app.timer.stop();
            } catch (Exception ex) { throw new RuntimeException(ex); }
        });
        System.out.println("PASS: getting-started navigation is passive; shortcuts use the existing controls without changing biological state.");
    }
    private static void press(Container root, String text) {
        AbstractButton button = find(root, text);
        if (button == null) throw new AssertionError("Missing guide action: " + text);
        button.doClick();
    }
    private static AbstractButton find(Container root, String text) {
        for (Component component : root.getComponents()) {
            if (component instanceof AbstractButton button && text.equals(button.getText())) return button;
            if (component instanceof Container child) {
                AbstractButton result = find(child, text);
                if (result != null) return result;
            }
        }
        return null;
    }
    private GettingStartedChecks() {}
}
