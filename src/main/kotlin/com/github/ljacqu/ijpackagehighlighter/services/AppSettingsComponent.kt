package com.github.ljacqu.ijpackagehighlighter.services

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
import javax.swing.JPanel
import javax.swing.JTable
import javax.swing.JTextField
import javax.swing.table.TableCellEditor
import javax.swing.table.TableCellRenderer

class AppSettingsComponent {

    val rulesModel: ListTableModel<Rule>
    val panel: JPanel

    constructor(state: HighlightSettings.State) {
        rulesModel = createRulesModel(state.groups)
        panel = createHighlightRulesPanel(rulesModel)
    }

    private fun createRulesModel(initialRules: List<HighlightSettings.HighlightRule>): ListTableModel<Rule> {
        val cols = arrayOf(PrefixColumn(), ColorColumn())
        val rulesModel = ListTableModel<Rule>(*cols)
        initialRules.forEach { rulesModel.addRow(Rule(it.prefix, it.rgb)) }
        return rulesModel
    }

    /**
     * Create a panel containing the table + toolbar for add/remove/move.
     */
    private fun createHighlightRulesPanel(rulesModel: ListTableModel<Rule>): JPanel {
        val table = TableView(rulesModel)
        table.setShowGrid(false)
        table.tableHeader.reorderingAllowed = false
        table.setRowHeight(22)

        // set renderer/editor for color column (it's index 1)
        val colorRenderer = ColorCellRenderer()
        table.setDefaultRenderer(Int::class.java, colorRenderer) // fallback
        table.columnModel.getColumn(1).cellRenderer = colorRenderer
        table.columnModel.getColumn(1).cellEditor = ColorCellEditor()

        // ensure prefix column uses text field editor (default works but explicit is fine)
        table.columnModel.getColumn(0).cellEditor = PrefixCellEditor()

        // toolbar for add/remove/move
        val decorator = ToolbarDecorator.createDecorator(table)
        decorator.setAddAction {
            // add default item and start editing prefix cell
            rulesModel.addRow(Rule("", 0xFFFF00))
            val newRow = rulesModel.rowCount - 1
            table.selectionModel.setSelectionInterval(newRow, newRow)
            table.editCellAt(newRow, 0)
        }
        decorator.setRemoveAction {
            val sel = table.selectedRow
            if (sel >= 0) rulesModel.removeRow(sel)
        }
        decorator.setMoveUpAction { rulesModel.exchangeRows(table.selectedRow, table.selectedRow - 1) }
        decorator.setMoveDownAction { rulesModel.exchangeRows(table.selectedRow, table.selectedRow + 1) }

        val panel = JPanel(BorderLayout())
        panel.add(decorator.createPanel(), BorderLayout.CENTER)
        return panel
    }

    fun getRules(): List<HighlightSettings.HighlightRule> =
        (0 until rulesModel.rowCount).map { row ->
            val item = rulesModel.getItem(row)
            HighlightSettings.HighlightRule(item.prefix, item.rgb)
        }

    // replace UI rows from persisted data
    fun setRules(rules: List<HighlightSettings.HighlightRule>) {
        // clear
        while (rulesModel.rowCount > 0) rulesModel.removeRow(0)
        // add
        rules.forEach {
            rulesModel.addRow(Rule(it.prefix, it.rgb))
        }
    }

    /** Highlight rule model. */
    data class Rule(var prefix: String = "", var rgb: Int = 0xFFFF00)

    /** Prefix column definition. */
    class PrefixColumn : ColumnInfo<Rule, String>("Prefix") {

        override fun valueOf(item: Rule) = item.prefix
        override fun isCellEditable(item: Rule) = true

        override fun setValue(item: Rule, value: String?) {
            item.prefix = value ?: ""
        }
    }

    /** Color column definition. */
    class ColorColumn : ColumnInfo<Rule, Int>("Color") {

        override fun valueOf(item: Rule) = item.rgb
        override fun isCellEditable(item: Rule) = true

        override fun setValue(item: Rule, value: Int?) {
            item.rgb = value ?: 0xFFFF00
        }
    }

    class PrefixCellEditor : AbstractCellEditor(), TableCellEditor {

        private val tf = JTextField()

        override fun getCellEditorValue(): String = tf.text

        override fun getTableCellEditorComponent(table: JTable, value: Any?, isSelected: Boolean, row: Int,
                                                 column: Int): Component {
            tf.text = value?.toString() ?: ""
            return tf
        }
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

        override fun getTableCellRendererComponent(table: JTable, value: Any?, isSelected: Boolean,
                                                   hasFocus: Boolean, row: Int, column: Int): Component {
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

        override fun getCellEditorValue() = currentValue

        override fun getTableCellEditorComponent(table: JTable, value: Any?, isSelected: Boolean,
                                                 row: Int, column: Int): Component {
            currentValue = when (value) {
                is Int -> value
                is Number -> value.toInt()
                else -> 0xFFFFFF
            }
            return button
        }
    }
}
