package com.github.ljacqu.ijpackagehighlighter.startup

import com.github.ljacqu.ijpackagehighlighter.services.HighlightSettings.Section
import com.github.ljacqu.ijpackagehighlighter.services.HighlightSettingsService
import com.intellij.lang.annotation.AnnotationBuilder
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiImportStatement
import com.intellij.psi.PsiJavaCodeReferenceElement
import com.intellij.psi.PsiPackageStatement
import com.intellij.psi.util.PsiTreeUtil
import java.awt.Color
import java.awt.Font

/**
 * Annotates Java type references that appear in method signatures and catch clauses.
 */
class PackageHighlighter : Annotator {

    private val debugShowSection = false // TODO: Remove this
    private var settingsService: HighlightSettingsService? = null

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        val service = settingsService ?: initSettingsService(element.project)

        when (element) {
            is PsiPackageStatement -> {
                if (service.shouldHighlight(Section.PACKAGE))
                    annotateIfQualifiedNameMatches(element, holder, element.packageName)
            }
            is PsiImportStatement -> {
                if (service.shouldHighlight(Section.IMPORT))
                    annotateIfQualifiedNameMatches(element, holder, element.qualifiedName)
            }
            is PsiJavaCodeReferenceElement -> {
                if (isRelevantReferenceElement(element)) {
                    annotateIfQualifiedNameMatches(element, holder, resolveQualifiedName(element))
                }
            }
            is PsiIdentifier -> {
                val parent = element.parent
                if (parent is PsiClass) { // public class _Name_ ...
                    annotateIfQualifiedNameMatches(element, holder, parent.qualifiedName)
                }
            }
        }
    }

    private fun initSettingsService(project: Project): HighlightSettingsService {
        val service = project.getService(HighlightSettingsService::class.java)
        settingsService = service
        return service
    }

    private fun annotateIfQualifiedNameMatches(element: PsiElement, holder: AnnotationHolder, qualifiedName: String?) {
        val rule = settingsService!!.findRuleIfApplicable(element, qualifiedName)
        if (rule != null) {
            val bg = Color(rule.rgb)
            val attrs = TextAttributes(null, bg, null, null, Font.PLAIN)
            val sectionDescription = when (element) {
                is PsiJavaCodeReferenceElement -> settingsService!!.determineReferenceElementType(element).name
                else -> "Highlighted class"
            }
            val annotationBuilder = newAnnotation(holder, if (debugShowSection) sectionDescription else rule.name)
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

    private fun newAnnotation(holder: AnnotationHolder, name: String?): AnnotationBuilder {
        if (name.isNullOrEmpty()) {
            return holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
        }
        return holder.newAnnotation(HighlightSeverity.INFORMATION, name)
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
        return settingsService!!.highlightReferenceElement(elem)
    }
}
