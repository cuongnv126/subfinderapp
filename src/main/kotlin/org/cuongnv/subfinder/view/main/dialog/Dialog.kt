package org.cuongnv.subfinder.view.main.dialog

import java.awt.Color
import java.awt.Dimension
import java.awt.Frame
import java.awt.GraphicsEnvironment
import java.awt.Toolkit
import javax.swing.JDialog
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.WindowConstants


abstract class Dialog(
    owner: Frame?,
    initWidth: Int,
    initHeight: Int
) : JDialog(owner ?: DummyFrame()) {

    val container: JPanel = JPanel()
    private val screenSize = Toolkit.getDefaultToolkit().screenSize

    init {
        container.apply {
            preferredSize = Dimension(initWidth, initHeight)
            background = Color.white
            isFocusable = false
        }

        this.onCreateView()
        this.onStart()

        this.onViewCreated()

        this.apply {
            isUndecorated = true
            contentPane = container
            size = Dimension(initWidth, initHeight)
            isResizable = false
            isFocusable = false
            defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
            isAlwaysOnTop = true

            pack()
        }
    }

    override fun dispose() {
        onClose()
        super.dispose()
    }
    open fun showOnScreen(screen: Int, frame: JFrame?) {
        val gs = GraphicsEnvironment.getLocalGraphicsEnvironment().screenDevices
        if (screen > -1 && screen < gs.size) {
            gs[screen].fullScreenWindow = frame
        } else if (gs.isNotEmpty()) {
            gs[0].fullScreenWindow = frame
        } else {
            throw RuntimeException("No Screens Found")
        }
    }
    open fun show(x: Int, y: Int) {
        setLocation(screenSize.width - this.width - 20, 40)
        isVisible = true
    }

    override fun setVisible(b: Boolean) {
        super.setVisible(b)
        if (b) {
            onStart()
        }
    }

    open fun onStart() {}
    open fun onClose() {}
    abstract fun onCreateView()
    open fun onViewCreated() {}
}

internal class DummyFrame : JFrame() {
    init {
        isUndecorated = true
        isVisible = true
        setLocationRelativeTo(null)
    }
}