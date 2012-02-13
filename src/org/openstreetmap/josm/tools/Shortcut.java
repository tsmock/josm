//License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.tools;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

import org.openstreetmap.josm.Main;

/**
 * Global shortcut class.
 *
 * Note: This class represents a single shortcut, contains the factory to obtain
 *       shortcut objects from, manages shortcuts and shortcut collisions, and
 *       finally manages loading and saving shortcuts to/from the preferences.
 *
 * Action authors: You only need the {@see #registerShortcut} factory. Ignore everything
 *                 else.
 *
 * All: Use only public methods that are also marked to be used. The others are
 *      public so the shortcut preferences can use them.
 *
 */
public class Shortcut {
    @Deprecated
    public static final int SHIFT_DEFAULT = 1;
    private String shortText;        // the unique ID of the shortcut
    private String longText;         // a human readable description that will be shown in the preferences
    private int requestedKey;        // the key, the caller requested
    private int requestedGroup;      // the group, the caller requested
    private int assignedKey;         // the key that actually is used
    private int assignedModifier;    // the modifiers that are used
    private boolean assignedDefault; // true if it got assigned what was requested. (Note: modifiers will be ignored in favour of group when loading it from the preferences then.)
    private boolean assignedUser;    // true if the user changed this shortcut
    private boolean automatic;       // true if the user cannot change this shortcut (Note: it also will not be saved into the preferences)
    private boolean reset;           // true if the user requested this shortcut to be set to its default value (will happen on next restart, as this shortcut will not be saved to the preferences)

    // simple constructor
    private Shortcut(String shortText, String longText, int requestedKey, int requestedGroup, int assignedKey, int assignedModifier, boolean assignedDefault, boolean assignedUser) {
        this.shortText = shortText;
        this.longText = longText;
        this.requestedKey = requestedKey;
        this.requestedGroup = requestedGroup;
        this.assignedKey = assignedKey;
        this.assignedModifier = assignedModifier;
        this.assignedDefault = assignedDefault;
        this.assignedUser = assignedUser;
        this.automatic = false;
        this.reset = false;
    }

    public String getShortText() {
        return shortText;
    }

    public String getLongText() {
        return longText;
    }

    // a shortcut will be renamed when it is handed out again, because the original name
    // may be a dummy
    private void setLongText(String longText) {
        this.longText = longText;
    }

    private int getRequestedKey() {
        return requestedKey;
    }

    public int getRequestedGroup() {
        return requestedGroup;
    }

    public int getAssignedKey() {
        return assignedKey;
    }

    public int getAssignedModifier() {
        return assignedModifier;
    }

    public boolean getAssignedDefault() {
        return assignedDefault;
    }

    public boolean getAssignedUser() {
        return assignedUser;
    }

    public boolean getAutomatic() {
        return automatic;
    }

    public boolean isChangeable() {
        return !automatic && !shortText.equals("core:none");
    }

    private boolean getReset() {
        return reset;
    }

    /**
     * FOR PREF PANE ONLY
     */
    public void setAutomatic() {
        automatic = true;
    }

    /**
     * FOR PREF PANE ONLY
     */
    public void setAssignedModifier(int assignedModifier) {
        this.assignedModifier = assignedModifier;
    }

    /**
     * FOR PREF PANE ONLY
     */
    public void setAssignedKey(int assignedKey) {
        this.assignedKey = assignedKey;
    }

    /**
     * FOR PREF PANE ONLY
     */
    public void setAssignedUser(boolean assignedUser) {
        this.reset = (this.assignedUser || reset) && !assignedUser;
        if (assignedUser) {
            assignedDefault = false;
        } else if (reset) {
            assignedKey = requestedKey;
            assignedModifier = findModifier(requestedGroup, null);
        }
        this.assignedUser = assignedUser;
    }

    /**
     * Use this to register the shortcut with Swing
     */
    public KeyStroke getKeyStroke() {
        if (assignedModifier != -1)
            return KeyStroke.getKeyStroke(assignedKey, assignedModifier);
        return null;
    }

    private boolean isSame(int isKey, int isModifier) {
        // -1 --- an unassigned shortcut is different from any other shortcut
        return( isKey == assignedKey && isModifier == assignedModifier && assignedModifier != getGroupModifier(GROUP_NONE));
    }

    // create a shortcut object from an string as saved in the preferences
    private Shortcut(String prefString) {
        ArrayList<String> s = (new ArrayList<String>(Main.pref.getCollection(prefString)));
        this.shortText = prefString.substring(15);
        this.longText = s.get(0);
        this.requestedKey = Integer.parseInt(s.get(1));
        this.requestedGroup = Integer.parseInt(s.get(2));
        this.assignedKey = Integer.parseInt(s.get(3));
        this.assignedModifier = Integer.parseInt(s.get(4));
        this.assignedDefault = Boolean.parseBoolean(s.get(5));
        this.assignedUser = Boolean.parseBoolean(s.get(6));
    }

    private void saveDefault(int modifier) {
        Main.pref.getCollection("shortcut.entry."+shortText, Arrays.asList(new String[]{longText,
        String.valueOf(requestedKey), String.valueOf(requestedGroup), String.valueOf(requestedKey),
        String.valueOf(modifier), String.valueOf(true), String.valueOf(false)}));
    }

    // get a string that can be put into the preferences
    private boolean save() {
        if (getAutomatic() || getReset() || !getAssignedUser()) {
            return Main.pref.putCollection("shortcut.entry."+shortText, null);
        } else {
            return Main.pref.putCollection("shortcut.entry."+shortText, Arrays.asList(new String[]{longText,
            String.valueOf(requestedKey), String.valueOf(requestedGroup), String.valueOf(assignedKey),
            String.valueOf(assignedModifier), String.valueOf(assignedDefault), String.valueOf(assignedUser)}));
        }
    }

    private boolean isSame(Shortcut other) {
        return assignedKey == other.assignedKey && assignedModifier == other.assignedModifier;
    }

    /**
     * use this to set a menu's mnemonic
     */
    public void setMnemonic(JMenu menu) {
        if (requestedGroup == GROUP_MNEMONIC && assignedModifier == getGroupModifier(requestedGroup + GROUPS_DEFAULT) && getKeyStroke() != null && KeyEvent.getKeyText(assignedKey).length() == 1) {
            menu.setMnemonic(KeyEvent.getKeyText(assignedKey).charAt(0)); //getKeyStroke().getKeyChar() seems not to work here
        }
    }
    /**
     * use this to set a buttons's mnemonic
     */
    public void setMnemonic(AbstractButton button) {
        if (requestedGroup == GROUP_MNEMONIC && assignedModifier == getGroupModifier(requestedGroup + GROUPS_DEFAULT) && getKeyStroke() != null && KeyEvent.getKeyText(assignedKey).length() == 1) {
            button.setMnemonic(KeyEvent.getKeyText(assignedKey).charAt(0)); //getKeyStroke().getKeyChar() seems not to work here
        }
    }
    /**
     * use this to set a actions's accelerator
     */
    public void setAccelerator(AbstractAction action) {
        if (getKeyStroke() != null) {
            action.putValue(AbstractAction.ACCELERATOR_KEY, getKeyStroke());
        }
    }

    /**
     * use this to get a human readable text for your shortcut
     */
    public String getKeyText() {
        KeyStroke keyStroke = getKeyStroke();
        if (keyStroke == null) return "";
        String modifText = KeyEvent.getKeyModifiersText(keyStroke.getModifiers());
        if ("".equals (modifText)) return KeyEvent.getKeyText (keyStroke.getKeyCode ());
        return modifText + "+" + KeyEvent.getKeyText(keyStroke.getKeyCode ());
    }

    @Override
    public String toString() {
        return getKeyText();
    }

    ///////////////////////////////
    // everything's static below //
    ///////////////////////////////

    // here we store our shortcuts
    private static Map<String, Shortcut> shortcuts = new LinkedHashMap<String, Shortcut>();

    // and here our modifier groups
    private static Map<Integer, Integer> groups;

    // check if something collides with an existing shortcut
    private static Shortcut findShortcut(int requestedKey, int modifier) {
        if (modifier == getGroupModifier(GROUP_NONE))
            return null;
        for (Shortcut sc : shortcuts.values()) {
            if (sc.isSame(requestedKey, modifier))
                return sc;
        }
        return null;
    }

    /**
     * FOR PREF PANE ONLY
     */
    public static List<Shortcut> listAll() {
        List<Shortcut> l = new ArrayList<Shortcut>();
        for(Shortcut c : shortcuts.values())
        {
            if(!c.shortText.equals("core:none")) {
                l.add(c);
            }
        }
        return l;
    }

    // try to find an unused shortcut
    private static Shortcut findRandomShortcut(String shortText, String longText, int requestedKey, int requestedGroup) {
        int[] mods = {getGroupModifier(requestedGroup + GROUPS_DEFAULT), getGroupModifier(requestedGroup + GROUPS_ALT1), getGroupModifier(requestedGroup + GROUPS_ALT2)};
        for (int m : mods) {
            for (int k = KeyEvent.VK_A; k < KeyEvent.VK_Z; k++) { // we'll limit ourself to 100% safe keys
                if ( findShortcut(k, m) == null )
                    return new Shortcut(shortText, longText, requestedKey, requestedGroup, k, m, false, false);
            }
        }
        return new Shortcut(shortText, longText, requestedKey, requestedGroup, requestedKey, getGroupModifier(GROUP_NONE), false, false);
    }

    // use these constants to request shortcuts
    /**
     * no shortcut.
     */
    public static final int GROUP_NONE = 0;
    /**
     * a button action, will use another modifier than MENU on system with a meta key.
     */
    public static final int GROUP_HOTKEY = 1;
    /**
     * a menu action, e.g. "ctrl-e" (export).
     */
    public static final int GROUP_MENU = 2;
    /**
     * direct edit key, e.g. "a" (add).
     */
    public static final int GROUP_EDIT = 3;
    /**
     * toggle one of the right-hand-side windows, e.g. "alt-l" (layers).
     */
    public static final int GROUP_LAYER = 4;
    /**
     * for non-letter keys, preferable without modifier, e.g. F5.
     */
    public static final int GROUP_DIRECT = 5;
    /**
     * for use with {@see #setMnemonic} only!
     */
    public static final int GROUP_MNEMONIC = 6;
    /**
     * for direct access, with alt modifier.
     */
    public static final int GROUP_DIRECT2 = 7;
    /**
     * for direct access, remaining modifiers.
     */
    public static final int GROUP_DIRECT3 = 8;
    public static final int GROUP__MAX = 9;
    public static final int GROUP_RESERVED = 1000;
    public static final int GROUPS_DEFAULT = 0;
    public static final int GROUPS_ALT1 = GROUP__MAX;
    public static final int GROUPS_ALT2 = GROUP__MAX * 2;

    // bootstrap
    private static boolean initdone = false;
    private static void doInit() {
        if (initdone) return;
        initdone = true;
        groups = Main.platform.initShortcutGroups(true);
        // (1) System reserved shortcuts
        Main.platform.initSystemShortcuts();
        // (2) User defined shortcuts
        LinkedList<Shortcut> shortcuts = new LinkedList<Shortcut>();
        for(String s : Main.pref.getAllPrefixCollectionKeys("shortcut.entry.")) {
            shortcuts.add(new Shortcut(s));
        }
        for(Shortcut sc : shortcuts) {
            if (sc.getAssignedUser()) {
                registerShortcut(sc);
            }
        }
        // Shortcuts at their default values
        for(Shortcut sc : shortcuts) {
            if (!sc.getAssignedUser() && sc.getAssignedDefault()) {
                registerShortcut(sc);
            }
        }
        // Shortcuts that were automatically moved
        for(Shortcut sc : shortcuts) {
            if (!sc.getAssignedUser() && !sc.getAssignedDefault()) {
                registerShortcut(sc);
            }
        }
    }

    private static int getGroupModifier(int group) {
        Integer m = groups.get(group);
        if(m == null)
            m = -1;
        return m;
    }

    // shutdown handling
    public static boolean savePrefs() {
        boolean changed = false;
        for (Shortcut sc : shortcuts.values()) {
            changed = changed | sc.save();
        }
        return changed;
    }

    // this is used to register a shortcut that was read from the preferences
    private static void registerShortcut(Shortcut sc) {
        // put a user configured shortcut in as-is -- unless there's a conflict
        if(sc.getAssignedUser() && findShortcut(sc.getAssignedKey(),
                sc.getAssignedModifier()) == null) {
            shortcuts.put(sc.getShortText(), sc);
        } else {
            registerShortcut(sc.getShortText(), sc.getLongText(), sc.getRequestedKey(),
                    sc.getRequestedGroup(), sc.getAssignedModifier(), sc);
        }
    }

    /**
     * FOR PLATFORMHOOK USE ONLY
     *
     * This registers a system shortcut. See PlatformHook for details.
     */
    public static Shortcut registerSystemShortcut(String shortText, String longText, int key, int modifier) {
        if (shortcuts.containsKey(shortText))
            return shortcuts.get(shortText);
        Shortcut potentialShortcut = findShortcut(key, modifier);
        if (potentialShortcut != null) {
            // this always is a logic error in the hook
            System.err.println("CONFLICT WITH SYSTEM KEY "+shortText);
            return null;
        }
        potentialShortcut = new Shortcut(shortText, longText, key, GROUP_RESERVED, key, modifier, true, false);
        shortcuts.put(shortText, potentialShortcut);
        return potentialShortcut;
    }

    /**
     * Register a shortcut.
     *
     * Here you get your shortcuts from. The parameters are:
     *
     * @param shortText an ID. re-use a {@code "system:*"} ID if possible, else use something unique.
     * {@code "menu:*"} is reserved for menu mnemonics, {@code "core:*"} is reserved for
     * actions that are part of JOSM's core. Use something like
     * {@code <pluginname>+":"+<actionname>}.
     * @param longText this will be displayed in the shortcut preferences dialog. Better
     * use something the user will recognize...
     * @param requestedKey the key you'd prefer. Use a {@link KeyEvent KeyEvent.VK_*} constant here.
     * @param requestedGroup the group this shortcut fits best. This will determine the
     * modifiers your shortcut will get assigned. Use the {@code GROUP_*}
     * constants defined above.
     * @param modifier to register a {@code ctrl+shift} command, use {@see #SHIFT_DEFAULT}.
     */
    @Deprecated
    public static Shortcut registerShortcut(String shortText, String longText, int requestedKey, int requestedGroup, int modifier) {
        return registerShortcut(shortText, longText, requestedKey, requestedGroup, modifier, null);
    }

    /**
     * Register a shortcut.
     *
     * Here you get your shortcuts from. The parameters are:
     *
     * @param shortText an ID. re-use a {@code "system:*"} ID if possible, else use something unique.
     * {@code "menu:*"} is reserved for menu mnemonics, {@code "core:*"} is reserved for
     * actions that are part of JOSM's core. Use something like
     * {@code <pluginname>+":"+<actionname>}.
     * @param longText this will be displayed in the shortcut preferences dialog. Better
     * use something the user will recognize...
     * @param requestedKey the key you'd prefer. Use a {@link KeyEvent KeyEvent.VK_*} constant here.
     * @param requestedGroup the group this shortcut fits best. This will determine the
     * modifiers your shortcut will get assigned. Use the {@code GROUP_*}
     * constants defined above.
     */
    public static Shortcut registerShortcut(String shortText, String longText, int requestedKey, int requestedGroup) {
        return registerShortcut(shortText, longText, requestedKey, requestedGroup, null, null);
    }

    private static int findModifier(int group, Integer modifier) {
        Integer defaultModifier = getGroupModifier(group);
        if(modifier != null) {
            if(modifier == SHIFT_DEFAULT) {
                defaultModifier |= KeyEvent.SHIFT_DOWN_MASK;
            } else {
                defaultModifier = modifier;
            }
        }
        else if (defaultModifier == null) { // garbage in, no shortcut out
            defaultModifier = getGroupModifier(GROUP_NONE + GROUPS_DEFAULT);
        }
        return defaultModifier;
    }

    // and now the workhorse. same parameters as above, just one more: if originalShortcut is not null and
    // is different from the shortcut that will be assigned, a popup warning will be displayed to the user.
    // This is used when registering shortcuts that have been visible to the user before (read: have been
    // read from the preferences file). New shortcuts will never warn, even when they land on some funny
    // random fallback key like Ctrl+Alt+Shift+Z for "File Open..." <g>
    private static Shortcut registerShortcut(String shortText, String longText, int requestedKey, int requestedGroup, Integer modifier,
            Shortcut originalShortcut) {
        doInit();
        Integer defaultModifier = findModifier(requestedGroup, modifier);
        if (shortcuts.containsKey(shortText)) { // a re-register? maybe a sc already read from the preferences?
            Shortcut sc = shortcuts.get(shortText);
            sc.setLongText(longText); // or set by the platformHook, in this case the original longText doesn't match the real action
            sc.saveDefault(defaultModifier);
            return sc;
        }
        Shortcut conflictsWith = null;
        Shortcut potentialShortcut = findShortcut(requestedKey, defaultModifier);
        if (potentialShortcut != null) { // 3 stage conflict handling
            conflictsWith = potentialShortcut;
            defaultModifier = getGroupModifier(requestedGroup + GROUPS_ALT1);
            if (defaultModifier == null) { // garbage in, no shortcut out
                defaultModifier = getGroupModifier(GROUP_NONE + GROUPS_DEFAULT);
            }
            potentialShortcut = findShortcut(requestedKey, defaultModifier);
            if (potentialShortcut != null) {
                defaultModifier = getGroupModifier(requestedGroup + GROUPS_ALT2);
                if (defaultModifier == null) { // garbage in, no shortcut out
                    defaultModifier = getGroupModifier(GROUP_NONE + GROUPS_DEFAULT);
                }
                potentialShortcut = findShortcut(requestedKey, defaultModifier);
                if (potentialShortcut != null) { // if all 3 modifiers for a group are used, we give up
                    potentialShortcut = findRandomShortcut(shortText, longText, requestedKey, requestedGroup);
                } else {
                    potentialShortcut = new Shortcut(shortText, longText, requestedKey, requestedGroup, requestedKey, defaultModifier, false, false);
                }
            } else {
                potentialShortcut = new Shortcut(shortText, longText, requestedKey, requestedGroup, requestedKey, defaultModifier, false, false);
            }
            if (originalShortcut != null && !originalShortcut.isSame(potentialShortcut)) {
                displayWarning(conflictsWith, potentialShortcut, shortText, longText);
            } else if (originalShortcut == null) {
                System.out.println("Silent shortcut conflict: '"+shortText+"' moved by '"+conflictsWith.getShortText()+"' to '"+potentialShortcut.getKeyText()+"'.");
            }
        } else {
            potentialShortcut = new Shortcut(shortText, longText, requestedKey, requestedGroup, requestedKey, defaultModifier, true, false);
        }

        potentialShortcut.saveDefault(defaultModifier);
        shortcuts.put(shortText, potentialShortcut);
        return potentialShortcut;
    }

    // a lengthy warning message
    private static void displayWarning(Shortcut conflictsWith, Shortcut potentialShortcut, String shortText, String longText) {
        JOptionPane.showMessageDialog(Main.parent,
                tr("Setting the keyboard shortcut ''{0}'' for the action ''{1}'' ({2}) failed\n"+
                        "because the shortcut is already taken by the action ''{3}'' ({4}).\n\n",
                        conflictsWith.getKeyText(), longText, shortText,
                        conflictsWith.getLongText(), conflictsWith.getShortText())+
                        (potentialShortcut.getKeyText().equals("") ?
                                tr("This action will have no shortcut.\n\n")
                                :
                                    tr("Using the shortcut ''{0}'' instead.\n\n", potentialShortcut.getKeyText())
                                )+
                                tr("(Hint: You can edit the shortcuts in the preferences.)"),
                                tr("Error"),
                                JOptionPane.ERROR_MESSAGE
                );
    }

    /**
     * Replies the platform specific key stroke for the 'Copy' command, i.e.
     * 'Ctrl-C' on windows or 'Meta-C' on a Mac. null, if the platform specific
     * copy command isn't known.
     *
     * @return the platform specific key stroke for the  'Copy' command
     */
    static public KeyStroke getCopyKeyStroke() {
        Shortcut sc = shortcuts.get("system:copy");
        if (sc == null) return null;
        return sc.getKeyStroke();
    }

    /**
     * Replies the platform specific key stroke for the 'Paste' command, i.e.
     * 'Ctrl-V' on windows or 'Meta-V' on a Mac. null, if the platform specific
     * paste command isn't known.
     *
     * @return the platform specific key stroke for the 'Paste' command
     */
    static public KeyStroke getPasteKeyStroke() {
        Shortcut sc = shortcuts.get("system:paste");
        if (sc == null) return null;
        return sc.getKeyStroke();
    }

    /**
     * Replies the platform specific key stroke for the 'Cut' command, i.e.
     * 'Ctrl-X' on windows or 'Meta-X' on a Mac. null, if the platform specific
     * 'Cut' command isn't known.
     *
     * @return the platform specific key stroke for the 'Cut' command
     */
    static public KeyStroke getCutKeyStroke() {
        Shortcut sc = shortcuts.get("system:cut");
        if (sc == null) return null;
        return sc.getKeyStroke();
    }
}
