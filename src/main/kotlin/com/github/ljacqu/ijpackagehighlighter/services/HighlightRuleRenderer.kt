package com.github.ljacqu.ijpackagehighlighter.services

import com.intellij.ui.components.JBLabel
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import javax.swing.BorderFactory
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.ListCellRenderer

class HighlightRuleRenderer : ListCellRenderer<AppSettingsComponent.HighlightRuleModel> {
    override fun getListCellRendererComponent(
        list: JList<out AppSettingsComponent.HighlightRuleModel>?,
        value: AppSettingsComponent.HighlightRuleModel?,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
    ): Component {
        val panel = JPanel(BorderLayout(8, 0))
        panel.border = BorderFactory.createEmptyBorder(4, 6, 4, 6)

        val prefix = JBLabel(value?.prefix ?: "<empty>")
        panel.add(prefix, BorderLayout.CENTER)

        // color swatch on the right
        val swatch = JPanel()
        swatch.border = BorderFactory.createLineBorder(Color.BLACK)
        val rgb = value?.rgb ?: 0xFFFFFF
        swatch.background = Color(rgb)
        swatch.preferredSize = java.awt.Dimension(22, 16)
        panel.add(swatch, BorderLayout.EAST)

        // selection colors
        val bg = if (isSelected) list?.selectionBackground else list?.background
        val fg = if (isSelected) list?.selectionForeground else list?.foreground
        panel.background = bg
        prefix.foreground = fg

        return panel
    }
}
