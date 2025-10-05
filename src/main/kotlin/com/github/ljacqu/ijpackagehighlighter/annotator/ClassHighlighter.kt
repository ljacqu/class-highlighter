package com.github.ljacqu.ijpackagehighlighter.annotator

import com.github.ljacqu.ijpackagehighlighter.services.HighlightSettings
import com.github.ljacqu.ijpackagehighlighter.services.HighlightSettingsService
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaTokenType
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiImportStatement
import com.intellij.psi.PsiJavaCodeReferenceElement
import com.intellij.psi.PsiJavaToken
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiPackageStatement
import com.intellij.psi.util.PsiTreeUtil

/**
 * Annotates Java classes based on the plugin's highlight rules (based on package name).
 */
class ClassHighlighter : Annotator {

    private val debugShowSection = false // TODO: Remove this
    private var settingsService: HighlightSettingsService? = null

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        val service = settingsService ?: initSettingsService(element.project)

        when (element) {
            is PsiPackageStatement -> {
                if (service.shouldHighlight(HighlightSettings.Section.PACKAGE)) {
                    // Quick fix to highlight packages, e.g. if a highlight rule specifies "java.util.*"
                    // we still want a match for "package java.util;"
                    annotateIfQualifiedNameMatches(element, holder, element.packageName + ".$")
                }
            }

            is PsiImportStatement -> {
                if (service.shouldHighlight(HighlightSettings.Section.IMPORT)) {
                    annotateIfQualifiedNameMatches(element, holder, getQualifiedNameOfImport(element))
                }
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
                } else if (service.shouldHighlight(HighlightSettings.Section.CONSTRUCTOR)) {
                    annotateIfQualifiedNameMatches(element, holder, getQualifiedNameOfConstructorIfApplicable(parent))
                }
            }
        }
    }

    private fun annotateIfQualifiedNameMatches(element: PsiElement, holder: AnnotationHolder, qualifiedName: String?) {
        val rule = settingsService!!.findRuleIfApplicable(qualifiedName)
        if (rule != null) {
            val annotationName = if (debugShowSection) getSectionNameForDebug(element) else rule.getName()
            val annotationBuilder = rule.newAnnotationBuilder(holder, annotationName)

            if (element is PsiJavaCodeReferenceElement) {
                val referenceNameElem = getReferenceNameForRange(element)
                if (referenceNameElem != null) {
                    annotationBuilder.range(referenceNameElem.textRange)
                }
            }
            annotationBuilder.create()
        }
    }

    private fun getSectionNameForDebug(element: PsiElement): String? {
        return when (element) {
            is PsiJavaCodeReferenceElement -> settingsService?.determineReferenceElementType(element)?.name
            else -> element.javaClass.simpleName
        }
    }

    private fun getQualifiedNameOfImport(element: PsiImportStatement): String? {
        if (element.qualifiedName == null) {
            return null
        }

        val hasAsterisk = PsiTreeUtil.getChildrenOfType(element, PsiJavaToken::class.java)
            ?.any { token -> token.tokenType == JavaTokenType.ASTERISK }
        if (hasAsterisk == true) {
            // Return java.util.* if the line is "import java.util.*;"
            return element.qualifiedName + ".*"
        }
        return element.qualifiedName
    }

    private fun getQualifiedNameOfConstructorIfApplicable(parent: PsiElement): String? {
        if ((parent as? PsiMethod)?.isConstructor == true) {
            return (parent.parent as? PsiClass)?.qualifiedName
        }
        return null
    }

    /**
     * Returns the reference name element, if available and desired to highlight only this part. This allows to limit
     * a declaration like {@code ArrayList<T>} to only highlight the {@code ArrayList} part.
     */
    private fun getReferenceNameForRange(element: PsiJavaCodeReferenceElement): PsiElement? {
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
        if (elem.parent is PsiJavaCodeReferenceElement) {
            // Fully qualified names like a variable declaration of "java.lang.Comparable" or references in JavaDoc like
            // "@see java.util.function.Predicate" seem to produce a PsiJavaCodeReferenceElement that has a parent of
            // the same type. We just need to handle the parent so that only the class name is highlighted.
            return false
        }
        return settingsService!!.highlightReferenceElement(elem)
    }

    /** Called the first time ot keep a SettingsService reference. */
    private fun initSettingsService(project: Project): HighlightSettingsService {
        val service = project.getService(HighlightSettingsService::class.java)
        settingsService = service
        return service
    }
}
