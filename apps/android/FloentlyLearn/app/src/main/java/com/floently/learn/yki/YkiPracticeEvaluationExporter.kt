package com.floently.learn.yki

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

internal enum class YkiPracticeExportFormat {
    Pdf,
    Word
}

internal data class YkiPracticeEvaluationReport(
    val title: String,
    val createdAt: String,
    val scoreLine: String,
    val summary: String,
    val lines: List<String>
) {
    fun plainText(): String =
        buildString {
            appendLine(title)
            appendLine(createdAt)
            appendLine(scoreLine)
            appendLine()
            appendLine(summary)
            appendLine()
            lines.forEach { appendLine(it) }
        }
}

internal object YkiPracticeEvaluation {
    fun build(
        tasks: List<YkiPracticeBankTask>,
        selectedAnswers: Map<Int, Int>,
        checkedAnswers: Map<Int, Boolean>,
        writingAnswers: Map<Int, String>,
        recordingPaths: Map<Int, String>
    ): YkiPracticeEvaluationReport {
        val choiceIndexes = tasks.withIndex()
            .filter { (_, task) -> task.options.isNotEmpty() && task.correctIndex >= 0 }

        val correctCount = choiceIndexes.count { (index, task) ->
            checkedAnswers[index] == true && selectedAnswers[index] == task.correctIndex
        }

        val writingIndexes = tasks.withIndex().filter { (_, task) -> task.skill == YkiPracticeSkill.Writing }
        val speakingIndexes = tasks.withIndex().filter { (_, task) -> task.skill == YkiPracticeSkill.Speaking }

        val writingSubmitted = writingIndexes.count { (index, _) ->
            writingAnswers[index].orEmpty().trim().isNotBlank()
        }

        val speakingSubmitted = speakingIndexes.count { (index, _) ->
            recordingPaths[index].orEmpty().isNotBlank()
        }

        val lines = mutableListOf<String>()

        lines += "Choice tasks"
        choiceIndexes.forEach { (index, task) ->
            val selected = selectedAnswers[index]
            val selectedText = selected?.let { task.options.getOrNull(it) }.orEmpty().ifBlank { "No answer selected" }
            val result = if (selected == task.correctIndex && checkedAnswers[index] == true) "Correct" else "Needs review"
            lines += "- ${task.number} ${task.title}: $result. Selected: $selectedText"
        }

        lines += ""
        lines += "Writing tasks"
        writingIndexes.forEach { (index, task) ->
            val answer = writingAnswers[index].orEmpty().trim()
            val words = answer.split(Regex("\\s+")).filter { it.isNotBlank() }.size
            val status = if (answer.isBlank()) "Not submitted" else "Submitted, $words words"
            lines += "- ${task.number} ${task.title}: $status"
        }

        lines += ""
        lines += "Speaking tasks"
        speakingIndexes.forEach { (index, task) ->
            val status = if (recordingPaths[index].orEmpty().isBlank()) "Not recorded" else "Recorded and sent"
            lines += "- ${task.number} ${task.title}: $status"
        }

        lines += ""
        lines += "Feedback"
        lines += "- Reading/listening score: $correctCount / ${choiceIndexes.size}"
        lines += "- Writing submitted: $writingSubmitted / ${writingIndexes.size}"
        lines += "- Speaking recorded: $speakingSubmitted / ${speakingIndexes.size}"
        lines += "- Continue practising any task marked Needs review."

        val created = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))

        return YkiPracticeEvaluationReport(
            title = "YKI Practice Evaluation",
            createdAt = "Created: $created",
            scoreLine = "Reading and listening score: $correctCount / ${choiceIndexes.size}",
            summary = "Writing submitted: $writingSubmitted / ${writingIndexes.size}. Speaking recorded: $speakingSubmitted / ${speakingIndexes.size}.",
            lines = lines
        )
    }
}

internal object YkiPracticeEvaluationExporter {
    fun share(
        context: Context,
        report: YkiPracticeEvaluationReport,
        format: YkiPracticeExportFormat
    ) {
        val reportsDir = File(context.cacheDir, "yki-reports")
        reportsDir.mkdirs()

        val file = when (format) {
            YkiPracticeExportFormat.Pdf -> writePdf(reportsDir, report)
            YkiPracticeExportFormat.Word -> writeWord(reportsDir, report)
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val mime = when (format) {
            YkiPracticeExportFormat.Pdf -> "application/pdf"
            YkiPracticeExportFormat.Word -> "application/msword"
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(
            Intent.createChooser(intent, "Export YKI evaluation")
        )
    }

    private fun writePdf(
        reportsDir: File,
        report: YkiPracticeEvaluationReport
    ): File {
        val file = File(reportsDir, "yki-practice-evaluation.pdf")
        val document = PdfDocument()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 12f
        }
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 18f
            isFakeBoldText = true
        }

        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNumber).create())
        var canvas = page.canvas
        var y = 44f

        fun newPage() {
            document.finishPage(page)
            pageNumber += 1
            page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNumber).create())
            canvas = page.canvas
            y = 44f
        }

        fun drawLine(line: String, useTitle: Boolean = false) {
            val activePaint = if (useTitle) titlePaint else paint
            val chunks = line.chunked(if (useTitle) 52 else 82)
            chunks.forEach { chunk ->
                if (y > 795f) newPage()
                canvas.drawText(chunk, 42f, y, activePaint)
                y += if (useTitle) 24f else 18f
            }
        }

        drawLine(report.title, useTitle = true)
        drawLine(report.createdAt)
        drawLine(report.scoreLine)
        y += 10f
        drawLine(report.summary)
        y += 10f
        report.lines.forEach { drawLine(it) }

        document.finishPage(page)
        file.outputStream().use { out -> document.writeTo(out) }
        document.close()
        return file
    }

    private fun writeWord(
        reportsDir: File,
        report: YkiPracticeEvaluationReport
    ): File {
        val file = File(reportsDir, "yki-practice-evaluation.doc")
        val body = report.plainText()
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\n", "<br/>")

        file.writeText(
            """
            <html>
            <head>
              <meta charset="utf-8">
              <title>${report.title}</title>
            </head>
            <body>
              <h1>${report.title}</h1>
              <p>$body</p>
            </body>
            </html>
            """.trimIndent()
        )

        return file
    }
}
