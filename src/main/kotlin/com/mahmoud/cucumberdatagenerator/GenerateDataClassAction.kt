package com.mahmoud.cucumberdatagenerator

import com.intellij.lang.Language
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.LanguageTextField
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.datatransfer.StringSelection
import java.math.BigDecimal
import java.util.*
import javax.swing.Action
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import kotlin.collections.ArrayList

class GenerateDataClassAction : AnAction() {

    override fun update(e: AnActionEvent) {
        super.update(e)
    }

    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.dataContext.getData(CommonDataKeys.EDITOR)

        if (editor != null) {
            val selectedText = editor.caretModel.currentCaret.selectedText

            // Parse and generate data class
            val generatedDataClass = parseAndPrintData(selectedText ?: "")

            val language = Language.findLanguageByID("kotlin") // Replace with the correct language ID
            val jsonOutputDialog = JsonOutputDialog(language!!, editor.project!!, generatedDataClass)

            jsonOutputDialog.show()
        }
    }
}

private fun parseAndPrintData(data: String): String {
    val rows = data.trim().split("\n")

    val dataTypes = ArrayList<String>()
    val paramType = ArrayList<String>()

    // Initiate data types
    val params = rows[0].trim().split("|")
    params.forEach { _ ->
        dataTypes.add("")
        paramType.add("")
    }

    // Parse and detect data types
    rows.forEachIndexed { rowIndex, row ->
        // Remove leading and trailing spaces and split the data by the delimiter
        val col = row.trim().split("|")

        if (rowIndex > 0) {
            col.forEachIndexed { index, property ->
                val value = property.split(" ").joinToString("")

                if(dataTypes[index].isEmpty() && value.isEmpty())
                    dataTypes[index] = "?"
                else if(paramType[index].isEmpty() && value.isNotEmpty())
                    paramType[index] = determineType(value)
            }
        }
    }

    var fullDataClass = "data class MyDataModel(\n"

    // Capitalize each property in camel case
    params.forEachIndexed { index, property ->
        var camelCaseProperty = property.split(" ").joinToString("") { word ->
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        }

        if (camelCaseProperty.isNotEmpty()) {
            camelCaseProperty = camelCaseProperty[0].toString().replaceFirstChar { it.lowercase(Locale.getDefault()) } +
                    camelCaseProperty.substring(1, camelCaseProperty.length)
            var prefix = "val"
            var suffix = ""
            if(dataTypes[index] == "?") {
                prefix = "var"
                suffix =  " = null"
            }
            fullDataClass += "    $prefix ${camelCaseProperty}: ${paramType[index]}${dataTypes[index]}$suffix"
            if (index < params.lastIndex - 1)
                fullDataClass += ","
            fullDataClass += "\n"
        }
    }

    fullDataClass += ")\n"
    return fullDataClass
}

private fun determineType(input: String): String {
    return if (input == "true" || input == "false")
        "Boolean"
    else try {
        input.toInt()
        "Int"
    } catch (e: NumberFormatException) {
        try {
            BigDecimal(input)
            "BigDecimal"
        } catch (e: NumberFormatException) {
            "String"
        }
    }
}

private class JsonOutputDialog(language: Language, project: Project, text: String) : DialogWrapper(project) {

    private val editorTextField = CustomEditorField(language, project, text)

    init {
        init()
        editorTextField.preferredSize = Dimension(600, 400)
    }

    override fun createCenterPanel(): JComponent {
        return JPanel(BorderLayout()).apply {
            add(editorTextField, BorderLayout.CENTER)
        }
    }

    override fun getPreferredFocusedComponent(): JComponent {
        return editorTextField
    }

    override fun createActions(): Array<Action> {
        return arrayOf(okAction) // Only OK action
    }
}

private class CustomEditorField(language: Language, project: Project, s: String) : LanguageTextField(language, project, s) {

    lateinit var editor: EditorEx

    override fun createEditor(): EditorEx {
        editor = super.createEditor()
        return editor
    }
}