package com.mahmoud.scenariodatagenerator

import com.intellij.lang.Language
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.LanguageTextField
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JPanel

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

    // Initiate data types
    val columns = rows[0].trim().split("|")
    columns.forEach { _ ->
        dataTypes.add("")
    }

    // Parse and detect data types
    rows.forEachIndexed { rowIndex, row ->
        // Remove leading and trailing spaces and split the data by the delimiter
        val col = row.trim().split("|")

        if (rowIndex > 0) {
            col.forEachIndexed { index, property ->
                val value = property.split(" ").joinToString("")

                val oldDataType = dataTypes[index]
                dataTypes[index] = if (oldDataType.isEmpty() && value.isEmpty())
                    "?"
                else if (oldDataType.isEmpty() && value.isNotEmpty())
                    determineType(value)
                else if (oldDataType == "?" && value.isEmpty())
                    oldDataType
                else if (oldDataType == "?" && value.isNotEmpty())
                    "${determineType(value)}?"
                else if (oldDataType != "?" && value.isEmpty())
                    "$oldDataType?"
                else if (oldDataType != "?" && value.isNotEmpty())
                    determineType(value)
                else
                    ""

                if (dataTypes[index].endsWith("??"))
                    dataTypes[index] = dataTypes[index].removeSuffix("?")
            }
        }
    }

    var fullDataClass = "data class MyDataClass(\n"

    // Capitalize each property in camel case, remove spaces, and print the result
    columns.forEachIndexed { index, property ->
        var camelCaseProperty = property.split(" ").joinToString("") { word ->
            word.capitalize()
        }

        if (camelCaseProperty.isNotEmpty()) {
            camelCaseProperty = camelCaseProperty[0].toString().decapitalize() +
                    camelCaseProperty.substring(1, camelCaseProperty.length)
            fullDataClass += "    val ${camelCaseProperty}: ${dataTypes[index]}"
            if (index < columns.lastIndex - 1)
                fullDataClass += ","
            fullDataClass += "\n"
        }
    }

    fullDataClass += ")\n"
    return fullDataClass
}

private fun determineType(input: String): String {
    return try {
        input.toInt()
        "Int"
    } catch (e: NumberFormatException) {
        try {
            input.toLong()
            "Long"
        } catch (e: NumberFormatException) {
            "String"
        }
    }
}

private class JsonOutputDialog(language: Language, project: Project, text: String) : DialogWrapper(project) {

    private val panel = JPanel(BorderLayout())

    init {
        super.OKActionEnabled(false)

        init()

        val editorTextField = CustomEditorField(language, project, text)
        editorTextField.OneLineMode(false)
        editorTextField.preferredSize = Dimension(800, 600)

        editorTextField.isVisible = true

        panel.add(editorTextField)

        editorTextField.CaretPosition(0)
    }

    override fun createCenterPanel() = panel
}

private class CustomEditorField(language: Language, project: Project, s: String) : LanguageTextField(language, project, s) {

    override fun createEditor(): EditorEx {
        val editor = super.createEditor()
        editor.VerticalScrollbarVisible(true)
        editor.HorizontalScrollbarVisible(true)

        val tings = editor.tings
        tings.isLineNumbersShown = true
        tings.isAutoCodeFoldingEnabled = true
        tings.isFoldingOutlineShown = true
        tings.isAllowSingleLogicalLineFolding = true
        tings.isRightMarginShown=true
        return editor
    }
}

