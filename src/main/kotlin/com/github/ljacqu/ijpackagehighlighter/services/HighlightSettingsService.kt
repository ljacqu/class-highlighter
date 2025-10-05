package com.github.ljacqu.ijpackagehighlighter.services

import com.github.ljacqu.ijpackagehighlighter.services.HighlightSettings.Section
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiCatchSection
import com.intellij.psi.PsiField
import com.intellij.psi.PsiJavaCodeReferenceElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiParameter
import com.intellij.psi.PsiReferenceList
import com.intellij.psi.javadoc.PsiDocComment
import com.intellij.psi.util.PsiTreeUtil

class HighlightSettingsService(project: Project) {

    private val state: HighlightSettings.State = project.getService(HighlightSettings::class.java).state
    private var rules: List<RuleApplication> = initRules()

    fun shouldHighlight(section: Section): Boolean {
        return state.sectionsToHighlight.contains(section)
    }

    fun reload() {
        rules = initRules()
    }

    private fun initRules(): List<RuleApplication> {
        return state.rules
            .filter { it.prefix.isNotEmpty() }
            .map { RuleApplication(it) }
    }

    fun highlightReferenceElement(element: PsiJavaCodeReferenceElement): Boolean {
        return shouldHighlight(determineReferenceElementType(element))
    }

    fun determineReferenceElementType(element: PsiJavaCodeReferenceElement): Section {
        val containingMethod = PsiTreeUtil.getParentOfType(element, PsiMethod::class.java, false)
        if (containingMethod != null) {
            val returnType = containingMethod.returnTypeElement
            if (returnType != null && PsiTreeUtil.isAncestor(returnType, element, false)) {
                return Section.METHOD_SIGNATURE
            }
            val param = PsiTreeUtil.getParentOfType(element, PsiParameter::class.java, false)
            if (param != null && (param.declarationScope is PsiMethod || param.declarationScope is PsiReferenceList)) {
                return Section.METHOD_SIGNATURE
            }
        }

        val catchParam = PsiTreeUtil.getParentOfType(element, PsiParameter::class.java, false)
        if (catchParam != null && catchParam.declarationScope is PsiCatchSection) {
            return Section.CATCH
        }
        if (PsiTreeUtil.getParentOfType(element, PsiDocComment::class.java, false) != null) {
            return Section.JAVADOC
        }
        if (PsiTreeUtil.getParentOfType(element, PsiField::class.java, false) != null) {
            return Section.FIELD_TYPE
        }

        return Section.OTHER
    }

    fun findRuleIfApplicable(qualifiedName: String?): RuleApplication? {
        if (qualifiedName == null) return null
        return rules.firstOrNull { r -> r.matches(qualifiedName) }
    }
}
