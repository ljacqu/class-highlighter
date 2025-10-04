package com.github.ljacqu.ijpackagehighlighter.services

import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.table.TableView
import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.ListTableModel
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import javax.swing.AbstractCellEditor
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JColorChooser
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTable
import javax.swing.JTextField
import javax.swing.table.TableCellEditor
import javax.swing.table.TableCellRenderer

/**
 * UI component for the highlight settings.
 */
class AppSettingsComponent {

    val sectionCheckBoxes: List<SectionCheckBox>
    val rulesModel: ListTableModel<Rule>
    val mainPanel: JComponent

    constructor() {
        sectionCheckBoxes = createCheckBoxes()
        val checkBoxesPanel = createSectionPanel(sectionCheckBoxes)
        rulesModel = createRulesModel()
        val rulesPanel = createHighlightRulesPanel(rulesModel)

        val mainPanel = JPanel()
        mainPanel.setLayout(BoxLayout(mainPanel, BoxLayout.Y_AXIS))
        mainPanel.add(checkBoxesPanel)
        mainPanel.add(rulesPanel)
        this.mainPanel = mainPanel
    }

    private fun createRulesModel(): ListTableModel<Rule> {
        val cols = arrayOf(PrefixColumn(), ColorColumn())
        return ListTableModel<Rule>(*cols)
    }

    private fun createSectionPanel(sectionCheckBoxes: List<SectionCheckBox>): JPanel {
        val buttonPanel = FormBuilder.createFormBuilder()
        sectionCheckBoxes.forEach { box -> buttonPanel.addComponent(box) }
        return buttonPanel.panel
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

    private fun createCheckBoxes(): List<SectionCheckBox> {
        return listOf(
            SectionCheckBox(HighlightSettings.Section.PACKAGE, "Highlight package declarations"),
            SectionCheckBox(HighlightSettings.Section.IMPORT, "Highlight import statements"),
            SectionCheckBox(HighlightSettings.Section.JAVADOC, "Highlight classes in JavaDoc"),
        )
    }

    fun getSections(): Set<HighlightSettings.Section> =
        sectionCheckBoxes.filter { it.isSelected }
            .map { it.section }
            .toSet()

    fun setSections(sections: Set<HighlightSettings.Section>) {
        sectionCheckBoxes.forEach {
            it.setSelected(sections.contains(it.section))
        }
    }

    fun getRules(): List<Rule> =
        (0 until rulesModel.rowCount).map { row ->
            rulesModel.getItem(row)
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

    class SectionCheckBox(val section: HighlightSettings.Section, name: String) : JBCheckBox(name) {

    }
}
