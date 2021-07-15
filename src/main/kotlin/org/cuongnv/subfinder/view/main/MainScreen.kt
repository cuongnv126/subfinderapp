package org.cuongnv.subfinder.view.main

import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.ComponentOrientation
import java.awt.Desktop
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JFileChooser
import javax.swing.JLabel
import javax.swing.JMenuItem
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JPopupMenu
import javax.swing.JScrollBar
import javax.swing.JScrollPane
import javax.swing.JTable
import javax.swing.ListSelectionModel
import javax.swing.SwingUtilities
import javax.swing.border.EmptyBorder
import javax.swing.border.MatteBorder
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel
import kotlin.math.min
import org.cuongnv.swingui.Screen
import org.cuongnv.swingui.material.FlatButtonUI
import org.cuongnv.swingui.material.FlatScrollBarUI
import org.cuongnv.swingui.res.Colors
import org.cuongnv.swingui.res.Values
import org.cuongnv.swingui.view.ProgressView
import org.cuongnv.swingui.widget.EditText
import org.cuongnv.swingui.widget.TextView
import org.cuongnv.subfinder.model.SimpleSubtitle

class MainScreen : Screen(900, 600), MainMvpView {
    private lateinit var btnSearchSub: JButton
    private lateinit var txtPath: JButton
    private lateinit var edtQuery: EditText
    private lateinit var tableFiles: JTable
    private lateinit var contextMenu: ContextMenu
    private lateinit var progressView: ProgressView

    private val dataSet = ArrayList<SimpleSubtitle>()

    override fun getAppTitle(): String {
        return "SUB FINDER"
    }

    override fun onCreateView() {
        container.layout = BorderLayout()
        contextMenu = ContextMenu()

        val title = TextView("subtitles").apply {
            font = Font(Values.FONT, Font.BOLD, 30)
            setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15))
            foreground = Colors.darkPrimary
        }

        val right = JPanel().apply {
            background = Color.white
            border = BorderFactory.createEmptyBorder()
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
        }

        val configPanel = JPanel().apply {
            componentOrientation = ComponentOrientation.RIGHT_TO_LEFT
            background = Color.white
            border = BorderFactory.createEmptyBorder()
            layout = GridBagLayout()
        }

        edtQuery = EditText().apply {
            font = Font(Values.FONT, Font.BOLD, 14)
            foreground = Colors.darkPrimary
            setHint("Title")
            addActionListener { searchSub() }
        }

        // keyword
        val keywordPanel = JPanel().apply {
            background = Color.white
            border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
            layout = BoxLayout(this, BoxLayout.Y_AXIS)

            add(
                TextView("title").apply {
                    font = Font(Values.FONT, Font.BOLD, 10)
                    setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 5))
                    foreground = Colors.colorPrimary
                }
            )

            add(edtQuery)
        }

        // action
        btnSearchSub = JButton("SEARCH").apply {
            background = Color.white
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Colors.borderColor, 1),
                BorderFactory.createEmptyBorder(7, 14, 7, 14)
            )
            isFocusPainted = false
            foreground = Colors.darkPrimary
            ui = FlatButtonUI()
            font = Font(Values.FONT, Font.BOLD, 14)
            addActionListener { searchSub() }
        }
        val actionPanel = JPanel().apply {
            background = Color.white
            border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
            layout = BorderLayout()

            add(
                TextView("action").apply {
                    font = Font(Values.FONT, Font.BOLD, 10)
                    foreground = Colors.colorPrimary
                    setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 5))
                },
                BorderLayout.NORTH
            )

            add(btnSearchSub, BorderLayout.SOUTH)
        }

        configPanel.add(
            actionPanel,
            GridBagConstraints().apply {
                anchor = GridBagConstraints.WEST
            }
        )
        configPanel.add(
            keywordPanel,
            GridBagConstraints().apply {
                gridwidth = GridBagConstraints.REMAINDER
                weightx = 1.0
                fill = GridBagConstraints.HORIZONTAL
            }
        )

        val logPanel = JPanel().apply {
            preferredSize = Dimension(560, 400)
            border = BorderFactory.createEmptyBorder(0, 10, 5, 10)
            background = Color.white
        }

        val logTitle = TextView("list files").apply {
            setBorder(BorderFactory.createEmptyBorder(10, 5, 5, 5))
            font = Font(Values.FONT, Font.BOLD, 10)
            foreground = Colors.colorPrimary
        }

        val logScroll = createFilesTable().apply {
            horizontalScrollBar = null
            verticalScrollBar = JScrollBar().apply {
                background = Color.white
                ui = FlatScrollBarUI(1)
                unitIncrement = 18
            }
        }

        val bottomPanel = JPanel().apply {
            layout = BorderLayout()
            background = Color.white
            border = BorderFactory.createEmptyBorder(5, 0, 0, 0)
        }

        val pathPanel = JPanel().apply {
            layout = FlowLayout(FlowLayout.LEFT)
            background = Color.white
        }
        val btnChangePath = JButton("change").apply {
            ui = FlatButtonUI()
            background = Color.white
            border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
            font = Font(Values.FONT, Font.BOLD, 11)
            foreground = Colors.colorPrimary
            addActionListener { changeSavePath() }
        }

        txtPath = JButton("...").apply {
            border = BorderFactory.createEmptyBorder(5, 0, 5, 0)
            font = Font(Values.FONT, Font.BOLD, 11)
            foreground = Colors.darkPrimary
            addActionListener { openSavedFolder() }
        }

        pathPanel.add(txtPath)
        pathPanel.add(btnChangePath)

        progressView = ProgressView().apply {
            preferredSize = Dimension(30, 30)
            border = EmptyBorder(5, 5, 5, 5)
        }

        bottomPanel.add(pathPanel, BorderLayout.WEST)
        bottomPanel.add(progressView, BorderLayout.EAST)

        logPanel.apply {
            layout = BorderLayout()
            add(logTitle, BorderLayout.NORTH)
            add(logScroll, BorderLayout.CENTER)
            add(bottomPanel, BorderLayout.SOUTH)
        }

        right.add(configPanel)
        right.add(logPanel)
        container.add(title, BorderLayout.NORTH)
        container.add(right, BorderLayout.CENTER)
    }

    private fun createFilesTable(): JScrollPane {
        val titles = arrayOf("Name", "Owner", "Comment")
        val tableModel = DefaultTableModel(titles, 0)
        tableFiles = object : JTable(tableModel) {
            override fun isCellEditable(row: Int, column: Int): Boolean {
                return false
            }
        }.apply {
            rowMargin = 0
            fillsViewportHeight = true
            rowHeight = 30
            showVerticalLines = false
            showHorizontalLines = false
            columnModel.columnMargin = 0
            setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
            intercellSpacing = Dimension(0, 0)
        }

        tableFiles.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (!tableFiles.isEnabled) return

                if (SwingUtilities.isRightMouseButton(e)) {
                    val visibleRect = tableFiles.visibleRect
                    val firstRow = visibleRect.y / tableFiles.rowHeight
                    val visibleCount = visibleRect.height / tableFiles.rowHeight + 1
                    val lastRow = min(firstRow + visibleCount, tableFiles.rowCount - 1)

                    for (row in firstRow..lastRow) {
                        val cellRect = tableFiles.getCellRect(row, 0, true)
                        cellRect.width = visibleRect.width
                        if (cellRect.contains(e.x, e.y)) {
                            tableFiles.setRowSelectionInterval(0, row)
                            contextMenu.setData(row, dataSet[row])
                            contextMenu.show(e.component, e.x, e.y)
                            break
                        }
                    }
                }
            }
        })

        val header = tableFiles.tableHeader.apply {
            preferredSize = Dimension(tableFiles.getWidth(), 30)
            border = MatteBorder(1, 1, 1, 1, Colors.borderColor)
            background = Color.white
        }

        val headerCellRenderer = header.defaultRenderer
        header.defaultRenderer = object : DefaultTableCellRenderer() {
            override fun getTableCellRendererComponent(
                table: JTable,
                value: Any,
                isSelected: Boolean,
                hasFocus: Boolean,
                row: Int,
                column: Int
            ): Component {
                val lbl = headerCellRenderer.getTableCellRendererComponent(
                    table,
                    value,
                    isSelected,
                    hasFocus,
                    row,
                    column
                ) as JLabel
                lbl.foreground = Colors.darkPrimary
                lbl.font = Font(Values.FONT, Font.BOLD, 12)
                lbl.border = BorderFactory.createCompoundBorder(
                    MatteBorder(0, 0, 0, 1, Colors.borderColor),
                    EmptyBorder(0, 5, 0, 0)
                )
                lbl.horizontalAlignment = LEFT
                if (isSelected) {
                    lbl.foreground = Colors.darkPrimary
                    lbl.background = Colors.hoverLight
                } else {
                    lbl.foreground = Colors.darkPrimary
                    lbl.background = Color.white
                }
                return lbl
            }
        }

        tableFiles.setDefaultRenderer(Any::class.java, object : DefaultTableCellRenderer() {
            override fun getTableCellRendererComponent(
                table: JTable,
                value: Any,
                isSelected: Boolean,
                hasFocus: Boolean,
                row: Int,
                column: Int
            ): Component {
                val lbl = headerCellRenderer.getTableCellRendererComponent(
                    table,
                    value,
                    isSelected,
                    hasFocus,
                    row,
                    column
                ) as JLabel
                lbl.foreground = Colors.darkPrimary
                lbl.font = Font(Values.FONT, Font.PLAIN, 12)
                lbl.border = BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 1, Colors.borderColor),
                    BorderFactory.createEmptyBorder(0, 5, 0, 0)
                )
                lbl.horizontalAlignment = LEFT
                if (isSelected) {
                    lbl.foreground = Colors.darkPrimary
                    lbl.background = Colors.hoverLight
                } else {
                    lbl.foreground = Colors.darkPrimary
                    lbl.background = Color.white
                }
                return lbl
            }
        })

        tableFiles.columnModel.getColumn(0).preferredWidth = 400
        tableFiles.border = MatteBorder(0, 1, 0, 1, Colors.borderColor)

        return JScrollPane(tableFiles).apply {
            background = Color.WHITE
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Colors.borderColor),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)
            )
        }
    }

    internal inner class ContextMenu : JPopupMenu() {
        private val btnDownload: JMenuItem = JMenuItem("Download")

        private var row = 0

        fun setData(row: Int, item: SimpleSubtitle) {
            this.row = row
            btnDownload.isEnabled = true
        }

        init {
            btnDownload.addActionListener { downloadFile(row) }
            add(btnDownload)
        }
    }

    private fun changeSavePath() {
        try {
            val fileChooser = JFileChooser().apply {
                currentDirectory = File(".")
                fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                dialogTitle = "Download Folder"
                isAcceptAllFileFilterUsed = false
            }

            fileChooser.showOpenDialog(container)
            val selected = fileChooser.selectedFile
            if (selected != null) {
                txtPath.text = selected.toString()
            }
        } catch (ex: Throwable) {
            ex.printStackTrace()
        }
    }

    private fun searchSub() {
        progressView.startProgress()
        MainPresenter.getInstance().search(edtQuery.text)
    }

    override fun onSearchSuccess(data: List<SimpleSubtitle>) {
        progressView.cancelProgress()
        dataSet.clear()
        dataSet.addAll(data)

        val model = tableFiles.model as DefaultTableModel
        for (i in model.rowCount - 1 downTo 0) {
            model.removeRow(i)
        }

        for (row in data) {
            model.addRow(arrayOf(row.name, row.owner, row.comment))
        }
    }

    override fun onSearchFail() {
        progressView.cancelProgress()
        JOptionPane.showMessageDialog(
            container,
            "Search fail",
            "Error",
            JOptionPane.ERROR_MESSAGE
        )
    }

    private fun downloadFile(row: Int) {
        val path = txtPath.text
        val file = File(path)
        if (path == "..." || !file.exists() || !file.isDirectory) {
            JOptionPane.showMessageDialog(
                container,
                "Please choose download folder",
                "Error",
                JOptionPane.ERROR_MESSAGE
            )
            return
        }

        progressView.startProgress()
        val subtitle: SimpleSubtitle = dataSet[row]
        MainPresenter.getInstance().download(subtitle, path)
    }

    override fun onDownloadSuccess() {
        progressView.cancelProgress()
        val option = JOptionPane.showConfirmDialog(
            container,
            "Download success, open folder contain?",
            "Success",
            JOptionPane.YES_NO_OPTION
        )

        if (option == JOptionPane.YES_OPTION) {
            openSavedFolder()
        }
    }

    private fun openSavedFolder() {
        if (Desktop.isDesktopSupported()) {
            with(File(txtPath.text)) { if (exists()) Desktop.getDesktop().open(this) }
        }
    }

    override fun onDownloadFail() {
        progressView.cancelProgress()
        JOptionPane.showMessageDialog(
            container,
            "Download fail.",
            "Error",
            JOptionPane.ERROR_MESSAGE
        )
    }

    override fun onStart() {
        super.onStart()
        MainPresenter.getInstance().attachView(this)
        with(File("config.ini")) {
            if (exists()) runCatching {
                txtPath.text = FileInputStream(this).use { it.bufferedReader().readText() }
            }
        }
    }

    override fun onClose() {
        MainPresenter.getInstance().detachView()
        runCatching { FileOutputStream(File("config.ini")).use { it.bufferedWriter().write(txtPath.text) } }
        super.onClose()
    }
}