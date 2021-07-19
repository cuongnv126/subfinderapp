package org.cuongnv.subfinder

import javax.swing.UIManager
import org.cuongnv.subfinder.view.main.MainScreen
import org.cuongnv.subfinder.view.systemtray.SystemTrayScreen

fun main() {
//    MainScreen().visibility(true)
    System.setProperty("apple.awt.UIElement", "true")
    SystemTrayScreen.show()
}