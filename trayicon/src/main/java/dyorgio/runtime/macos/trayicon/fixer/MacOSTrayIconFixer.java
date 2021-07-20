/*
 * The MIT License
 *
 * Copyright 2020 dyorgio.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package dyorgio.runtime.macos.trayicon.fixer;

import com.sun.jna.NativeLong;
import dyorgio.runtime.macos.trayicon.fixer.jna.appkit.AppKit;
import dyorgio.runtime.macos.trayicon.fixer.jna.foundation.ActionCallback;
import dyorgio.runtime.macos.trayicon.fixer.jna.foundation.Foundation;
import dyorgio.runtime.macos.trayicon.fixer.jna.foundation.FoundationUtil;
import dyorgio.runtime.macos.trayicon.fixer.jna.foundation.NSDictionary;
import dyorgio.runtime.macos.trayicon.fixer.jna.foundation.NSString;
import dyorgio.runtime.macos.trayicon.fixer.jna.foundation.NSUserDefaults;
import java.awt.Image;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;

/**
 * @author dyorgio
 */
public final class MacOSTrayIconFixer {
    private static final Logger log = Logger.getLogger(MacOSTrayIconFixer.class.getName());
    private static final String OS_VERSION = new NSString(NSDictionary
            .dictionaryWithContentsOfFile(new NSString("/System/Library/CoreServices/SystemVersion.plist"))
            .objectForKey(new NSString("ProductVersion")).getId()
    ).toString();

    MacOSTrayIconFixer() {
    }

    public static Image getInitialIcon(Image blackImage, Image whiteImage) {
        return !isImageTemplateSupported() && isDarkTheme() ? whiteImage : blackImage;
    }

    public static void fix(TrayIcon icon) {
        fix(icon, false, AppKit.NSSquareStatusItemLength);
    }

    @SuppressWarnings("UseSpecificCatch")
    public static void fix(final TrayIcon icon, boolean needsMenu, final double length) {
        if (isImageTemplateSupportedJdk()) {
            log.log(Level.INFO, "JDK has support for template icons, skipping fix");
            return;
        }
        // Check length
        if (length == 0) {
            throw new IllegalArgumentException("Status item length cannot be zero");
        }
        // Check if icon has a menu
        if (needsMenu && icon.getPopupMenu() == null) {
            throw new IllegalStateException("PopupMenu needs to be set on TrayIcon first");
        }

        // Check if icon is on SystemTray
        if (!Arrays.asList(SystemTray.getSystemTray().getTrayIcons()).contains(icon)) {
            throw new IllegalStateException("TrayIcon needs to be added on SystemTray first");
        }

        try {
            Field ptrField = Class.forName("sun.lwawt.macosx.CFRetainedResource").getDeclaredField("ptr");
            ptrField.setAccessible(true);

            Field field = TrayIcon.class.getDeclaredField("peer");
            field.setAccessible(true);
            long cTrayIconAddress = ptrField.getLong(field.get(icon));

//            long cPopupMenuAddressTmp = 0;
//            if (needsMenu || icon.getPopupMenu() != null) {
//                field = MenuComponent.class.getDeclaredField("peer");
//                field.setAccessible(true);
//                cPopupMenuAddressTmp = ptrField.getLong(field.get(icon.getPopupMenu()));
//            }
//            final long cPopupMenuAddress = cPopupMenuAddressTmp;

            final NativeLong statusItem = FoundationUtil.invoke(new NativeLong(cTrayIconAddress), "theItem");
            NativeLong awtView = FoundationUtil.invoke(statusItem, "view");
            final NativeLong image = Foundation.INSTANCE.object_getIvar(
                    awtView,
                    Foundation.INSTANCE.class_getInstanceVariable(
                            FoundationUtil.invoke(awtView, "class")
                            , "image")
            );
            FoundationUtil.invoke(image, "setTemplate:", true);
            FoundationUtil.runOnMainThreadAndWait(() -> {
                FoundationUtil.invoke(statusItem, "setView:", FoundationUtil.NULL);
                NativeLong target;
                if (isStatusItemButtonSupported()) {
                    target = FoundationUtil.invoke(statusItem, "button");
                } else {
                    target = statusItem;
                }

                FoundationUtil.invoke(target, "setImage:", image);
                FoundationUtil.invoke(statusItem, "setLength:", length);

//                if (cPopupMenuAddress != 0) {
//                    FoundationUtil.invoke(statusItem, "setMenu:", FoundationUtil.invoke(new NativeLong(cPopupMenuAddress), "menu"));
//                } else {
                new ActionCallback(() -> {
                    final ActionListener[] listeners = icon.getActionListeners();
                    final int now = (int) System.currentTimeMillis();
                    for (int i = 0; i < listeners.length; i++) {
                        final int iF = i;
                        SwingUtilities.invokeLater(() -> {
                            listeners[iF].actionPerformed(new ActionEvent(icon, now + iF, null));
                        });
                    }
                }).installActionOnNSControl(target);
//                }
            });
        } catch (Throwable t) {
            // ignore all
        }
    }

    public static boolean isImageTemplateSupported() {
        return compareOsVersionTo("10.5") >= 0;
    }

    public static boolean isStatusItemButtonSupported() {
        return compareOsVersionTo("10.10") >= 0;
    }

    /**
     * JDK-8252015 added native support for template images
     */
    public static boolean isImageTemplateSupportedJdk() {
        try {
            // before JDK-8252015: setNativeImage(long, long, boolean)
            // after  JDK-8252015: setNativeImage(long, long, boolean, boolean)
            Class.forName("sun.lwawt.macosx.CTrayIcon").getDeclaredMethod("setNativeImage", long.class, long.class, boolean.class, boolean.class);
            // JDK will default to non-template behavior unless property is specified
            if (Boolean.getBoolean(System.getProperty("apple.awt.enableTemplateImages"))) {
                return true;
            } else {
                log.warning("JDK has support for native icons, use \"apple.awt.enableTemplateImages\" instead.");
            }
        } catch (ClassNotFoundException | NoSuchMethodException ignore) {
        }
        return false;
    }

    public static boolean isDarkTheme() {
        return "Dark".equals(NSUserDefaults.standard().stringForKey(new NSString("AppleInterfaceStyle")).toString());
    }

    /**
     * Crude major.minor version comparator
     */
    private static int compareOsVersionTo(String compare) {
        int[] OS_VERSION_SPLIT = new int[2];
        int[] COMPARE_VERSION_SPLIT = new int[2];

        int counter = 0;
        for (String s : OS_VERSION.split("\\.")) {
            try {
                if (counter < 2) {
                    OS_VERSION_SPLIT[counter++] = Integer.parseInt(s);
                } else {
                    break;
                }
            } catch (NumberFormatException ignore) {
                OS_VERSION_SPLIT[counter++] = -1;
            }
        }

        counter = 0;
        for (String s : compare.split("\\.")) {
            try {
                if (counter < 2) {
                    COMPARE_VERSION_SPLIT[counter++] = Integer.parseInt(s);
                } else {
                    break;
                }
            } catch (NumberFormatException ignore) {
                COMPARE_VERSION_SPLIT[counter++] = -1;
            }
        }


        int compareTo = OS_VERSION_SPLIT[0] - COMPARE_VERSION_SPLIT[0];
        if (compareTo == 0) {
            compareTo = OS_VERSION_SPLIT[1] - COMPARE_VERSION_SPLIT[1];
        }
        return compareTo;

    }
}
