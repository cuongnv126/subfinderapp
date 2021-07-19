package org.cuongnv.subfinder.view.systemtray

import dyorgio.runtime.macos.trayicon.fixer.MacOSTrayIconFixer
import dyorgio.runtime.macos.trayicon.fixer.jna.appkit.AppKit
import java.awt.Image
import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.util.Date
import javax.swing.SwingUtilities
import org.cuongnv.subfinder.reactx.TimedThread
import org.cuongnv.subfinder.view.main.dialog.MainDialog
import org.cuongnv.swingui.utils.ViewUtils


object SystemTrayScreen {
    var dialog = MainDialog(null).apply {
        isModal = true
    }

    fun show() {
        if (SystemTray.isSupported()) {
            val tray = SystemTray.getSystemTray()

            val trayIcon = TrayIcon(ViewUtils.getImageIcon("tray-dark.png").image, "Sub Finder")
            trayIcon.addActionListener {
                SwingUtilities.invokeLater {
                    if (dialog.isShowing) {
                        dialog.isVisible = false
                    } else {
                        dialog.show(0, 0)
                    }
                }
            }

            try {
                tray.add(trayIcon)
                updateTrayIconInterval(trayIcon, 5 * 1000L)

            } catch (ex: Throwable) {
                ex.printStackTrace()
            }
        }
    }

    private fun updateTrayIconInterval(trayIcon: TrayIcon, time: Long) {
        MacOSTrayIconFixer.fix(trayIcon, false, AppKit.NSSquareStatusItemLength)

    }
}