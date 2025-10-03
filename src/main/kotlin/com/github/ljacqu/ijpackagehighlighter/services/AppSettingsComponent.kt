package com.github.ljacqu.ijpackagehighlighter.services

import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.EditableModel
import com.intellij.util.ui.FormBuilder
import javax.swing.AbstractListModel
import javax.swing.DefaultListModel
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.ListModel
import javax.swing.event.ListDataListener

class AppSettingsComponent {

    val panel: JPanel?
    private val myUserNameText = JBTextField()
    private val myIdeaUserStatus = JBCheckBox("IntelliJ IDEA user")
    private val rulesList = JBList(HighlightRulesModel())

    constructor(state: HighlightSettings.State) {
        // rulesList.model = DefaultListModel()

        rulesList.setListData(
            state.groups
                .map { HighlightRuleModel(it) }
                .toTypedArray()
        )
        rulesList.cellRenderer = HighlightRuleRenderer()
    }

    init {
        this.panel = FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("User name:"), myUserNameText, 1, false)
            .addComponent(myIdeaUserStatus, 1)
            //.addComponentFillVertically(JPanel(), 0)
            .addComponent(rulesList)
            .getPanel()
    }

    val preferredFocusedComponent: JComponent
        get() = myUserNameText


    var userNameText: String?
        get() = myUserNameText.getText()
        set(newText) {
            myUserNameText.setText(newText)
        }

    var ideaUserStatus: Boolean
        get() = myIdeaUserStatus.isSelected()
        set(newStatus) {
            myIdeaUserStatus.setSelected(newStatus)
        }

    class HighlightRulesModel : AbstractListModel<HighlightRuleModel>(), EditableModel {

        private val listOfRules = mutableListOf<HighlightRuleModel>()

        override fun addRow() {
            listOfRules.add(HighlightRuleModel())
        }

        override fun exchangeRows(oldIndex: Int, newIndex: Int) {
            val temp = listOfRules[newIndex]
            listOfRules[newIndex] = listOfRules[oldIndex]
            listOfRules[oldIndex] = temp
        }

        override fun canExchangeRows(oldIndex: Int, newIndex: Int): Boolean {
            return true
        }

        override fun removeRow(idx: Int) {
            listOfRules.removeAt(idx)
        }

        override fun getSize(): Int {
            return listOfRules.size
        }

        override fun getElementAt(index: Int): HighlightRuleModel {
            return listOfRules[index]
        }
    }

    class HighlightRuleModel {
        var prefix: String? = null
        var rgb: Int? = null

        constructor()

        constructor(rule: HighlightSettings.HighlightRule) {
            this.prefix = rule.prefix
            this.rgb = rule.rgb
        }
    }
}