package com.github.ljacqu.ijpackagehighlighter.services

import com.intellij.openapi.ui.Messages
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.table.TableView
import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.ListTableModel
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import javax.swing.AbstractCellEditor
import javax.swing.JButton
import javax.swing.JColorChooser
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTable
import javax.swing.JTextField
import javax.swing.table.TableCellEditor
import javax.swing.table.TableCellRenderer

class AppSettingsComponent {

    // simple data holder (mutable to allow table edits)
    data class HighlightRule(var prefix: String = "", var rgb: Int = 0xFFFF00)

    // Column for prefix (editable text)
    class PrefixColumn : ColumnInfo<HighlightRule, String>("Prefix") {
        override fun valueOf(item: HighlightRule): String = item.prefix
        override fun setValue(item: HighlightRule, value: String?) {
            item.prefix = value ?: ""
        }
        override fun isCellEditable(item: HighlightRule): Boolean = true
    }

    // Column for color (shows swatch + hex). We'll provide renderer + editor.
    class ColorColumn : ColumnInfo<HighlightRule, Int>("Color") {
        override fun valueOf(item: HighlightRule): Int = item.rgb
        override fun isCellEditable(item: HighlightRule): Boolean = true
    }

    // Renderer: panel with swatch and hex text
    class ColorCellRenderer : TableCellRenderer {
        private val swatch = JPanel()
        private val tf = JTextField()

        init {
            swatch.preferredSize = Dimension(18, 14)
            tf.isEditable = false
            tf.border = null
        }

        override fun getTableCellRendererComponent(
            table: JTable, value: Any?, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int
        ): Component {
            val panel = JPanel(BorderLayout(6, 0))
            val rgb = when (value) {
                is Int -> value
                is Number -> value.toInt()
                else -> 0xFFFFFF
            }
            swatch.background = Color(rgb)
            panel.add(swatch, BorderLayout.WEST)
            tf.text = String.format("#%06X", rgb and 0xFFFFFF)
            panel.add(tf, BorderLayout.CENTER)
            if (isSelected) {
                panel.background = table.selectionBackground
                tf.foreground = table.selectionForeground
            } else {
                panel.background = table.background
                tf.foreground = table.foreground
            }
            return panel
        }
    }

    // Editor: clicking opens a JColorChooser dialog; commits chosen color
    class ColorCellEditor : AbstractCellEditor(), TableCellEditor {
        private val button = JButton("…")
        private var currentValue: Int = 0

        init {
            button.addActionListener {
                val chosen = JColorChooser.showDialog(button, "Choose color", Color(currentValue))
                if (chosen != null) {
                    currentValue = chosen.rgb and 0xFFFFFF
                    // stopCellEditing will be called by editor, so return value via getCellEditorValue
                    stopCellEditing()
                }
            }
        }

        override fun getCellEditorValue(): Any = currentValue

        override fun getTableCellEditorComponent(
            table: JTable, value: Any?, isSelected: Boolean, row: Int, column: Int
        ): Component {
            currentValue = when (value) {
                is Int -> value
                is Number -> value.toInt()
                else -> 0xFFFFFF
            }
            return button
        }
    }


    /**
     * Create a panel containing the table + toolbar for add/remove/move.
     * Use getModel() to read the list for persistence (apply/save).
     */
    fun createHighlightRulesPanel(initial: List<HighlightRule>): JPanel {
        // columns
        val cols = arrayOf(PrefixColumn(), ColorColumn())
        val model = ListTableModel<HighlightRule>(*cols)
        rulesModel = model // todo lj
        // load initial rows
        initial.forEach { model.addRow(it) }

        val table = TableView(model)
        table.setShowGrid(false)
        table.tableHeader.reorderingAllowed = false
        table.setRowHeight(22)

        // set renderer/editor for color column (it's index 1)
        val colorRenderer = ColorCellRenderer()
        table.setDefaultRenderer(Int::class.java, colorRenderer) // fallback
        table.columnModel.getColumn(1).cellRenderer = colorRenderer
        table.columnModel.getColumn(1).cellEditor = ColorCellEditor()

        // ensure prefix column uses text field editor (default works but explicit is fine)
        table.columnModel.getColumn(0).cellEditor = object : AbstractCellEditor(), TableCellEditor {
            private val tf = JTextField()
            override fun getCellEditorValue(): Any = tf.text
            override fun getTableCellEditorComponent(
                table: JTable, value: Any?, isSelected: Boolean, row: Int, column: Int
            ): Component {
                tf.text = value?.toString() ?: ""
                return tf
            }
        }

        // toolbar for add/remove/move
        val decorator = ToolbarDecorator.createDecorator(table)
        decorator.setAddAction {
            // add default item and start editing prefix cell
            model.addRow(HighlightRule("new.prefix.", 0xFFFF00))
            val newRow = model.rowCount - 1
            table.selectionModel.setSelectionInterval(newRow, newRow)
            table.editCellAt(newRow, 0)
        }
        decorator.setRemoveAction {
            val sel = table.selectedRow
            if (sel >= 0) model.removeRow(sel)
        }
        decorator.setMoveUpAction { model.exchangeRows(table.selectedRow, table.selectedRow - 1) }
        decorator.setMoveDownAction { model.exchangeRows(table.selectedRow, table.selectedRow + 1) }

        val panel = JPanel(BorderLayout())
        panel.add(decorator.createPanel(), BorderLayout.CENTER)
        return panel
    }


    var panel: JPanel
    var rulesModel: ListTableModel<HighlightRule>? = null

    constructor(state: HighlightSettings.State) {
        val initial = state.groups.map { HighlightRule(it.prefix, it.rgb) }
        panel = createHighlightRulesPanel(initial)

    }

    fun getRules(): List<HighlightSettings.HighlightRule> =
        (0 until rulesModel!!.rowCount).map { row ->
            val item = rulesModel!!.getItem(row)
            HighlightSettings.HighlightRule(item.prefix, item.rgb)
        }

    // replace UI rows from persisted data
    fun setRules(rules: List<HighlightSettings.HighlightRule>) {
        // clear
        while (rulesModel!!.rowCount > 0) rulesModel!!.removeRow(0)
        // add
        rules.forEach {
            rulesModel!!.addRow(HighlightRule(it.prefix, it.rgb))
        }
    }
}