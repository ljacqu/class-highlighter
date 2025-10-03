package com.github.ljacqu.ijpackagehighlighter.startup

import com.github.ljacqu.ijpackagehighlighter.services.HighlightSettings
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import java.awt.Color
import java.awt.Font

/**
 * Annotates Java type references that appear in method signatures and catch clauses.
 */
class PackageHighlighter : Annotator {

    private var rules: Map<String, HighlightSettings.HighlightRule>? = null

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        when (element) {
            is PsiImportStatement -> {
                annotateIfQualifiedNameMatches(element, holder, element.qualifiedName)
            }
            is PsiPackageStatement -> {
                annotateIfQualifiedNameMatches(element, holder, element.packageName)
            }
            is PsiJavaCodeReferenceElement -> {
                if (isRelevantReferenceElement(element)) {
                    annotateIfQualifiedNameMatches(element, holder, resolveQualifiedName(element))
                }
            }
        }
    }

    private fun loadRules(project: Project): Map<String, HighlightSettings.HighlightRule> {
        val state = project.getService(HighlightSettings::class.java).state

        val rules = HashMap<String, HighlightSettings.HighlightRule>()
        state.groups.forEach { rules[it.prefix] = it }
        this.rules = rules
        project.thisLogger().info("Loaded ${rules.size} highlight rules")
        return rules
    }

    private fun getRuleForQualifiedName(element: PsiElement,
                                        qualifiedName: String?): HighlightSettings.HighlightRule? {
        if (qualifiedName == null) {
            return null
        }
        val rules = this.rules ?: loadRules(element.project)
        for (entry in rules) {
            if (qualifiedName.startsWith(entry.key)) {
                return entry.value
            }
        }
        return null
    }

    private fun annotateIfQualifiedNameMatches(element: PsiElement, holder: AnnotationHolder, qualifiedName: String?) {
        val rule = getRuleForQualifiedName(element, qualifiedName)
        if (rule != null) {
            val bg = Color(rule.rgb)
            val attrs = TextAttributes(null, bg, null, null, Font.PLAIN)
            val annotationBuilder = holder.newAnnotation(HighlightSeverity.INFORMATION, "Java.util class")
                .enforcedTextAttributes(attrs)
            if (element is PsiJavaCodeReferenceElement) {
                val referenceNameElem = getReferenceNameForRange(element)
                if (referenceNameElem != null) {
                    annotationBuilder.range(referenceNameElem.textRange)
                }
            }
            annotationBuilder.create()
        }
    }

    /**
     * Returns the reference name element, if available and desired to highlight only this part. This allows to limit
     * a declaration like {@code ArrayList<T>} to only highlight the {@code ArrayList} part.
     */
    private fun getReferenceNameForRange(element: PsiJavaCodeReferenceElement): PsiElement? {
        // todo lj: Can we detect a fqn and highlight it as one too?
        return element.referenceNameElement
    }

    private fun resolveQualifiedName(element: PsiJavaCodeReferenceElement): String? {
        val resolved = element.resolve()
        if (resolved is PsiClass) {
            return resolved.qualifiedName
        }
        // best-effort fallback
        val q = element.qualifiedName
        if (q != null) return q
        // try to synthesize via qualifier
        val qualifier = element.qualifier
        if (qualifier is PsiJavaCodeReferenceElement) {
            val qname = qualifier.qualifiedName
            if (qname != null) return qname + "." + element.referenceName
        }
        return null
    }

    private fun isRelevantReferenceElement(elem: PsiJavaCodeReferenceElement): Boolean {
        // PsiImportStatement & PsiPackageStatement are handled as a whole, so ignore the children elements
        if (PsiTreeUtil.getParentOfType(elem, PsiImportStatement::class.java, false) != null
            || PsiTreeUtil.getParentOfType(elem, PsiPackageStatement::class.java, false) != null) {
            return false
        }

        return true

        // todo lj: remove this? Or try to skip Class.method() calls?
        val containingMethod = PsiTreeUtil.getParentOfType(elem, PsiMethod::class.java, false)
        if (containingMethod != null) {
            val returnType = containingMethod.returnTypeElement
            if (returnType != null && PsiTreeUtil.isAncestor(returnType, elem, false)) {
                return true
            }

            val param = PsiTreeUtil.getParentOfType(elem, PsiParameter::class.java, false)
            if (param != null && param.declarationScope === containingMethod) {
                return true
            }
        }

        val catchParam = PsiTreeUtil.getParentOfType(elem, PsiParameter::class.java, false)
        if (catchParam != null && catchParam.declarationScope is PsiCatchSection) {
            return true
        }

        val field = PsiTreeUtil.getParentOfType(elem, PsiField::class.java, false)
        if (field != null) {
            return true
        }

        val importStatement = PsiTreeUtil.getParentOfType(elem, PsiImportStatement::class.java, false)
        if (importStatement != null) {
            return true
        }

        val refList = PsiTreeUtil.getParentOfType(elem, PsiReferenceList::class.java, false)
        if (refList != null) {
            return true
        }

        return false
    }
}
