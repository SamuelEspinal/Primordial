import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/** Presentation preferences are local to the user and never part of a world save. */
final class GuidePreferences {
    private static final int VERSION = 1;
    private static Preferences node() { return Preferences.userRoot().node("primordial/ui"); }
    static boolean shouldOpen() {
        try { return node().getInt("gettingStartedVersion", 0) < VERSION || showAtStartup(); }
        catch (SecurityException | IllegalStateException ex) { return true; }
    }
    static boolean showAtStartup() {
        try { return node().getBoolean("showGettingStartedOnStartup", false); }
        catch (SecurityException | IllegalStateException ex) { return false; }
    }
    static void markSeen() {
        try { Preferences p = node(); p.putInt("gettingStartedVersion", VERSION); p.flush(); }
        catch (SecurityException | IllegalStateException | BackingStoreException ignored) { /* Help remains available if preferences cannot be saved. */ }
    }
    static void showAtStartup(boolean show) {
        try { Preferences p = node(); p.putBoolean("showGettingStartedOnStartup", show); p.flush(); }
        catch (SecurityException | IllegalStateException | BackingStoreException ignored) { /* Optional presentation setting. */ }
    }
    private GuidePreferences() {}
}
