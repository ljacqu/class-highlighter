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
 * Applies background highlighting based on package prefix configured in PackageHighlightSettings.
 */
class PackageHighlighter : Annotator {

    override fun annotate(element: PsiElement, holder: AnnotationHolder): Unit {
        if (element !is PsiJavaCodeReferenceElement) return
        if (!isInTargetContext(element)) return
        val qualifier = resolveQualifiedName(element)

        if (qualifier != null && qualifier.startsWith("java.util")) {
            // Build TextAttributes with background color; keep foreground null so it doesn't change text color.
            val bg = Color(0xE2F0D9)
            val attrs = TextAttributes(null, bg, null, null, Font.PLAIN)

            // Create an info annotation and enforce the attributes (background)
            holder.newAnnotation(HighlightSeverity.INFORMATION, "Java.util class")
                .enforcedTextAttributes(attrs)
                .create()
        }
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
        val containingMethod = PsiTreeUtil.getParentOfType<PsiMethod?>(elem, PsiMethod::class.java, false)
        if (containingMethod != null) {
            val returnType = containingMethod.getReturnTypeElement()
            if (returnType != null && PsiTreeUtil.isAncestor(returnType, elem, false)) return true

            val param = PsiTreeUtil.getParentOfType<PsiParameter?>(elem, PsiParameter::class.java, false)
            if (param != null && param.getDeclarationScope() === containingMethod) return true

            val throwsList = PsiTreeUtil.getParentOfType<PsiReferenceList?>(elem, PsiReferenceList::class.java, false)
            if (throwsList != null && throwsList.getRole() == PsiReferenceList.Role.THROWS_LIST
                && throwsList.getParent() === containingMethod) {
                return true
            }
        }

        val catchParam = PsiTreeUtil.getParentOfType<PsiParameter?>(elem, PsiParameter::class.java, false)
        if (catchParam != null && catchParam.getDeclarationScope() is PsiCatchSection) {
            return true
        }

        val refList = PsiTreeUtil.getParentOfType<PsiReferenceList?>(elem, PsiReferenceList::class.java, false)
        if (refList != null && refList.getRole() == PsiReferenceList.Role.THROWS_LIST) {
            return true
        }

        return false
    }
}