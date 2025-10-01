package com.github.ljacqu.ijpackagehighlighter.startup
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import java.awt.Color
import java.awt.Font

/**
 * Annotates Java type references that appear in method signatures and catch clauses.
 */
class PackageHighlighter : Annotator {

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (element !is PsiJavaCodeReferenceElement) return
        if (!isInTargetContext(element)) return
        val qualifier = resolveQualifiedName(element)

        if (qualifier != null && qualifier.startsWith("java.util")) {
            // Build TextAttributes with background color; keep foreground null so it doesn't change text color.
            val bg = Color(0xE2F0D9)
            val attrs = TextAttributes(null, bg, null, null, Font.PLAIN)

            val referenceNameElem = getReferenceNameForRange(element)
            if (referenceNameElem != null) {
                holder.newAnnotation(HighlightSeverity.INFORMATION, "Java.util class")
                    .enforcedTextAttributes(attrs)
                    .range(referenceNameElem.textRange)
                    .create()
            } else {
                holder.newAnnotation(HighlightSeverity.INFORMATION, "Java.util class")
                    .enforcedTextAttributes(attrs)
                    .create()
            }
        }
    }

    /**
     * Returns the reference name element, if available and desired to highlight only this part. This allows to limit
     * a declaration like {@code ArrayList<T>} to only highlight the {@code ArrayList} part.
     */
    private fun getReferenceNameForRange(element: PsiJavaCodeReferenceElement): PsiElement? {
        // Don't return the name if the element is part of a package or import declaration; looks nicer to highlight
        // the entire statement than the name parts without the dots.
        // todo lj: Can we detect a fqn and highlight it as one too?
        if (PsiTreeUtil.getParentOfType(element, PsiImportList::class.java, false) == null
            && PsiTreeUtil.getParentOfType(element, PsiPackageStatement::class.java, false) == null) {
            return element.referenceNameElement
        }
        return null
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

    private fun isInTargetContext(elem: PsiJavaCodeReferenceElement): Boolean {
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

        val importList = PsiTreeUtil.getParentOfType(elem, PsiImportList::class.java, false)
        if (importList != null) {
            return true
        }

        val refList = PsiTreeUtil.getParentOfType(elem, PsiReferenceList::class.java, false)
        if (refList != null) {
            return true
        }

        return false
    }
}
