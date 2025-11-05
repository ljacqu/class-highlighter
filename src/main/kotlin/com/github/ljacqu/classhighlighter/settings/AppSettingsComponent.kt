package com.github.ljacqu.classhighlighter.settings

import com.github.ljacqu.classhighlighter.services.DEFAULT_COLOR
import com.github.ljacqu.classhighlighter.services.HighlightSettings
import com.github.ljacqu.classhighlighter.services.HighlightSettings.HighlightRule.Companion.DEFAULT_STYLE
import com.github.ljacqu.classhighlighter.services.HighlightSettings.Style
import com.github.ljacqu.classhighlighter.utils.ColorUtil
import com.intellij.openapi.ui.ComboBox
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

    private val sectionCheckBoxes: List<SectionCheckBox>
    private val rulesModel: ListTableModel<Rule>
    val mainPanel: JComponent

    init {
        sectionCheckBoxes = createSectionCheckBoxes()
        val checkBoxesPanel = createSectionPanel(sectionCheckBoxes)
        rulesModel = createRulesModel()
        val rulesPanel = createRulesPanel(rulesModel)
        mainPanel = createMainPanel(checkBoxesPanel, rulesPanel)
    }

    private fun createRulesModel(): ListTableModel<Rule> {
        val cols = arrayOf(NameColumn(), PrefixColumn(), ColorColumn(), StyleColumn())
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
    private fun createRulesPanel(rulesModel: ListTableModel<Rule>): JPanel {
        val table = TableView(rulesModel)
        table.setShowGrid(false)
        table.tableHeader.reorderingAllowed = false
        table.rowHeight = 22

        table.columnModel.getColumn(0).cellEditor = TextCellEditor()
        table.columnModel.getColumn(1).cellEditor = TextCellEditor()
        // set renderer/editor for color column
        val colorRenderer = ColorCellRenderer()
        table.setDefaultRenderer(Int::class.java, colorRenderer) // fallback
        table.columnModel.getColumn(2).cellRenderer = colorRenderer
        table.columnModel.getColumn(2).cellEditor = ColorCellEditor()
        table.columnModel.getColumn(3).cellEditor = StyleCellEditor()

        // toolbar for add/remove/move
        val decorator = ToolbarDecorator.createDecorator(table)
        decorator.setAddAction {
            // add default item and start editing first cell
            rulesModel.addRow(Rule("", "", DEFAULT_COLOR, DEFAULT_STYLE.text))
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

    private fun createSectionCheckBoxes(): List<SectionCheckBox> {
        return listOf(
            SectionCheckBox(HighlightSettings.Section.PACKAGE, "Highlight package declarations"),
            SectionCheckBox(HighlightSettings.Section.IMPORT, "Highlight import statements"),
            SectionCheckBox(HighlightSettings.Section.JAVADOC, "Highlight classes in JavaDoc"),
            SectionCheckBox(HighlightSettings.Section.CONSTRUCTOR, "Highlight constructor declarations"),
            SectionCheckBox(HighlightSettings.Section.FIELD_TYPE, "Highlight field types"),
            SectionCheckBox(HighlightSettings.Section.METHOD_SIGNATURE, "Highlight types in method signatures"),
            SectionCheckBox(HighlightSettings.Section.CATCH, "Highlight types in catch clauses"),
            SectionCheckBox(HighlightSettings.Section.OTHER, "Highlight other places"),
        )
    }

    private fun createMainPanel(checkBoxesPanel: JPanel, rulesPanel: JPanel): JPanel {
        val mainPanel = JPanel()
        mainPanel.layout = BoxLayout(mainPanel, BoxLayout.Y_AXIS)
        mainPanel.add(checkBoxesPanel)
        mainPanel.add(rulesPanel)
        return mainPanel
    }

    fun getSections(): Set<HighlightSettings.Section> =
        sectionCheckBoxes.filter { it.isSelected }
            .map { it.section }
            .toSet()

    fun setSections(sections: Set<HighlightSettings.Section>) {
        sectionCheckBoxes.forEach {
            it.isSelected = sections.contains(it.section)
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
            rulesModel.addRow(Rule(it))
        }
    }

    /** Highlight rule model. */
    data class Rule(var name: String = "", var prefix: String = "", var rgb: Int = DEFAULT_COLOR, var style: String) {

        fun toHighlightRule(): HighlightSettings.HighlightRule =
            HighlightSettings.HighlightRule(name, prefix, ColorUtil.intToHexString(rgb), Style.fromText(style))

        constructor(highlightRule: HighlightSettings.HighlightRule) : this(
            highlightRule.name,
            highlightRule.prefix,
            ColorUtil.hexStringToInt(highlightRule.rgb),
            highlightRule.style.text
        )
    }

    /** Name column definition. */
    class NameColumn : ColumnInfo<Rule, String>("Name") {

        override fun valueOf(item: Rule) = item.name
        override fun isCellEditable(item: Rule) = true

        override fun setValue(item: Rule, value: String?) {
            item.name = value ?: ""
        }
    }

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
            item.rgb = value ?: DEFAULT_COLOR
        }
    }

    /** Style column definition. */
    class StyleColumn : ColumnInfo<Rule, String>("Style") {

        override fun valueOf(item: Rule) = item.style
        override fun isCellEditable(item: Rule) = true

        override fun setValue(item: Rule, value: String?) {
            item.style = value ?: DEFAULT_STYLE.text
        }
    }

    class TextCellEditor : AbstractCellEditor(), TableCellEditor {

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

    /** Editor: clicking opens a JColorChooser dialog. */
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
                else -> DEFAULT_COLOR
            }
            return button
        }
    }

    class StyleCellEditor : AbstractCellEditor(), TableCellEditor {

        private val comboBox: ComboBox<String> = ComboBox(Style.entries.map { e -> e.text }.toTypedArray())
        private var currentValue: String = DEFAULT_STYLE.text

        init {
            comboBox.addActionListener {
                currentValue = comboBox.selectedItem as? String ?: DEFAULT_STYLE.text
            }
        }

        override fun getCellEditorValue() = currentValue

        override fun getTableCellEditorComponent(table: JTable?, value: Any?, isSelected: Boolean,
                                                 row: Int, column: Int): Component {
            currentValue = when (value) {
                is String -> value
                else -> DEFAULT_STYLE.text
            }
            comboBox.selectedItem = currentValue
            return comboBox
        }
    }

    /** Checkbox that knows which section it configures. */
    class SectionCheckBox(val section: HighlightSettings.Section, name: String) : JBCheckBox(name)

}
