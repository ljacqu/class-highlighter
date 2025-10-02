package com.github.ljacqu.ijpackagehighlighter.services

import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import javax.swing.JComponent
import javax.swing.JPanel

class AppSettingsComponent {

    val panel: JPanel?
    private val myUserNameText = JBTextField()
    private val myIdeaUserStatus = JBCheckBox("IntelliJ IDEA user")

    init {
        this.panel = FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("User name:"), myUserNameText, 1, false)
            .addComponent(myIdeaUserStatus, 1)
            .addComponentFillVertically(JPanel(), 0)
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
}