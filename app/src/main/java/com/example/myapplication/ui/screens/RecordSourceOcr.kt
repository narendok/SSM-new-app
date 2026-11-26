package com.example.myapplication.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONArray
import org.json.JSONObject
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.xml.sax.InputSource
import java.io.ByteArrayInputStream
import java.io.StringReader
import java.io.IOException
import java.util.Locale
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** On-device OCR for record source scans/photos. The source bytes are never sent to OCR services. */
internal suspend fun recognizeRecordSource(context: Context, uri: Uri): String {
    val resolver = context.contentResolver
    val mimeType = resolver.getType(uri).orEmpty().lowercase()
    val isPdf = mimeType == "application/pdf" || uri.lastPathSegment.orEmpty().substringAfterLast('.', "").equals("pdf", true)
    val latin = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    val devanagari = TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build())
    try {
        val pageTexts = mutableListOf<String>()
        if (isPdf) {
            val descriptor = resolver.openFileDescriptor(uri, "r") ?: throw IOException("Could not open PDF")
            descriptor.use { file ->
                PdfRenderer(file).use { renderer ->
                    val pageCount = renderer.pageCount.coerceAtMost(12)
                    for (index in 0 until pageCount) {
                        renderer.openPage(index).use { page ->
                            val scale = (1500f / page.width.coerceAtLeast(1)).coerceAtMost(2.5f)
                            val width = (page.width * scale).toInt().coerceAtLeast(1)
                            val height = (page.height * scale).toInt().coerceAtLeast(1)
                            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                            try {
                                Canvas(bitmap).apply { drawColor(Color.WHITE) }
                                page.render(bitmap, null, Matrix().apply { setScale(scale, scale) }, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                pageTexts += recognizePage(InputImage.fromBitmap(bitmap, 0), latin, devanagari)
                            } finally {
                                bitmap.recycle()
                            }
                        }
                    }
                    if (renderer.pageCount > pageCount) pageTexts += "Only the first 12 pages were scanned; review remaining pages manually."
                }
            }
        } else {
            pageTexts += recognizePage(InputImage.fromFilePath(context, uri), latin, devanagari)
        }
        return pageTexts.mapIndexedNotNull { index, text -> text.takeIf(String::isNotBlank)?.let { if (isPdf && !it.startsWith("Only the first 12")) "Page ${index + 1}\n$it" else it } }
            .joinToString("\n\n").take(30_000)
    } finally {
        latin.close()
        devanagari.close()
    }
}

private suspend fun recognizePage(input: InputImage, latin: com.google.mlkit.vision.text.TextRecognizer, devanagari: com.google.mlkit.vision.text.TextRecognizer): String {
    val latinText = latin.process(input).awaitResult().text.trim()
    val devanagariText = devanagari.process(input).awaitResult().text.trim()
    return listOf(latinText, devanagariText).distinct().filter(String::isNotBlank).joinToString("\n")
}

internal data class RecordSourcePair(val id: String, val label: String, val value: String)

internal fun parseRecordSourcePairs(text: String): List<RecordSourcePair> {
    val lines = text.replace("\r", "").lines().map(String::trim).filter(String::isNotBlank)
    val regex = Regex("^([^:：=\\t]{2,80})\\s*[:：=\\t]\\s*(.*)$")
    val results = linkedMapOf<String, RecordSourcePair>()
    lines.forEachIndexed { index, line ->
        if (results.size >= 40) return@forEachIndexed
        val match = regex.find(line) ?: return@forEachIndexed
        val label = match.groupValues[1].trim().trimStart('•', '*', '-', ' ').trimEnd(':', '：', '=')
        var value = match.groupValues[2].trim()
        if (value.isBlank()) {
            val next = lines.getOrNull(index + 1).orEmpty()
            if (next.length <= 180 && next.isNotBlank() && regex.find(next) == null) value = next
        }
        if (label.isBlank() || value.isBlank() || value.length > 240) return@forEachIndexed
        val id = label.normalizedRecordLabel()
        if (id.isNotBlank() && id !in results) results[id] = RecordSourcePair(id, label, value)
    }
    return results.values.toList()
}

internal fun suggestRecordSourceField(pair: RecordSourcePair, fields: List<kotlinx.serialization.json.JsonObject>): String {
    val aliases = mapOf(
        "controllednumber" to listOf("record number", "record no", "document number", "document no", "reference number", "reference no", "enquiry number", "enquiry no", "po number", "po no", "purchase order number"),
        "title" to listOf("record title", "document title", "request title", "enquiry title", "subject"),
        "targetdate" to listOf("target date", "required date", "planned date", "due date"),
        "duedate" to listOf("due date", "required date", "target date"),
        "invoicenumber" to listOf("invoice no", "invoice number", "bill number", "bill no"),
        "ponumber" to listOf("po", "po no", "po number", "purchase order", "purchase order number"),
        "customerreference" to listOf("customer id", "customer code", "client id"),
        "supplierreference" to listOf("supplier id", "supplier code", "vendor id", "vendor code")
    )
    val matches = fields.filter { field ->
        if (field["master"]?.jsonPrimitive?.contentOrNull?.isNotBlank() == true || field["readOnly"]?.jsonPrimitive?.contentOrNull == "true") return@filter false
        if ((field["options"] as? kotlinx.serialization.json.JsonArray)?.isNotEmpty() == true) return@filter false
        val key = field["key"]?.jsonPrimitive?.contentOrNull.orEmpty()
        val label = field["label"]?.jsonPrimitive?.contentOrNull.orEmpty()
        (listOf(key, label) + aliases[key.normalizedRecordLabel()].orEmpty()).any { it.normalizedRecordLabel() == pair.id }
    }
    return matches.singleOrNull()?.get("key")?.jsonPrimitive?.contentOrNull.orEmpty()
}

internal fun isRecordTitleSourceLabel(label: String): Boolean =
    label.normalizedRecordLabel() in setOf("title", "recordtitle", "documenttitle", "requesttitle", "enquirytitle", "subject")

private fun String.normalizedRecordLabel(): String = lowercase(Locale.ROOT).filter(Char::isLetterOrDigit)

/** Text-bearing office formats are read locally; legacy DOC/XLS remain attachable for manual entry. */
internal fun extractStructuredRecordSource(name: String, mimeType: String, bytes: ByteArray): String? {
    val extension = name.substringAfterLast('.', "").lowercase(Locale.ROOT)
    return when (extension) {
        "txt" -> bytes.toString(Charsets.UTF_8).take(30_000)
        "csv", "tsv" -> {
            val rows = parseSourceTable(bytes.toString(Charsets.UTF_8), if (extension == "tsv") '\t' else ',')
            val header = rows.firstOrNull().orEmpty()
            rows.drop(1).take(40).flatMap { row -> header.mapIndexedNotNull { i, label -> row.getOrNull(i)?.takeIf(String::isNotBlank)?.let { "$label: $it" } } }.joinToString("\n").take(30_000)
        }
        "json" -> runCatching { buildList { flattenJson(JSONObject(bytes.toString(Charsets.UTF_8)), "", this) }.take(40).joinToString("\n").take(30_000) }.getOrNull()
        "docx" -> zipEntry(bytes, "word/document.xml")?.let(::docxPlainText)?.take(30_000)
        "xlsx", "xlsm" -> xlsxFirstRowText(bytes)?.take(30_000)
        else -> if (mimeType.startsWith("text/")) bytes.toString(Charsets.UTF_8).take(30_000) else null
    }
}

private fun flattenJson(value: Any?, prefix: String, rows: MutableList<String>) {
    if (rows.size >= 40 || value == null || value == JSONObject.NULL) return
    when (value) {
        is JSONObject -> value.keys().forEach { key -> flattenJson(value.opt(key), if (prefix.isBlank()) key else "$prefix $key", rows) }
        is JSONArray -> if (value.length() > 0) flattenJson(value.opt(0), prefix, rows)
        else -> if (prefix.isNotBlank()) rows += "$prefix: $value"
    }
}

private fun parseSourceTable(text: String, delimiter: Char): List<List<String>> {
    return text.lineSequence().filter(String::isNotBlank).take(41).map { line ->
        val cells = mutableListOf<String>(); val cell = StringBuilder(); var quoted = false; var index = 0
        while (index < line.length) {
            val ch = line[index]
            if (ch == '"') {
                if (quoted && line.getOrNull(index + 1) == '"') { cell.append('"'); index++ } else quoted = !quoted
            } else if (ch == delimiter && !quoted) { cells += cell.toString().trim(); cell.clear() } else cell.append(ch)
            index++
        }
        cells += cell.toString().trim(); cells
    }.toList()
}

private fun zipEntry(bytes: ByteArray, wanted: String): ByteArray? = runCatching {
    ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
        while (true) {
            val entry = zip.nextEntry ?: break
            if (entry.name == wanted) return@use zip.readBytes()
        }
        null
    }
}.getOrNull()

private fun secureXml(bytes: ByteArray): org.w3c.dom.Document? = runCatching {
    val factory = DocumentBuilderFactory.newInstance().apply {
        isNamespaceAware = true
        setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        setFeature("http://xml.org/sax/features/external-general-entities", false)
        setFeature("http://xml.org/sax/features/external-parameter-entities", false)
        setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
    }
    factory.newDocumentBuilder().parse(ByteArrayInputStream(bytes))
}.getOrNull()

private fun org.w3c.dom.Document.elements(local: String): List<Element> {
    val nodes = getElementsByTagNameNS("*", local)
    return (0 until nodes.length).mapNotNull { nodes.item(it) as? Element }
}

private fun Element.elements(local: String): List<Element> {
    val nodes = getElementsByTagNameNS("*", local)
    return (0 until nodes.length).mapNotNull { nodes.item(it) as? Element }
}

private fun docxPlainText(bytes: ByteArray): String {
    val doc = secureXml(bytes) ?: return ""
    return doc.elements("p").map { p -> p.elements("t").joinToString("") { it.textContent.orEmpty() } }.filter(String::isNotBlank).joinToString("\n")
}

private fun xlsxFirstRowText(bytes: ByteArray): String {
    val shared = zipEntry(bytes, "xl/sharedStrings.xml")?.let(::secureXml)?.elements("si")?.map { si -> si.elements("t").joinToString("") { it.textContent.orEmpty() } }.orEmpty()
    val sheet = ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
        var found: ByteArray? = null
        while (true) { val entry = zip.nextEntry ?: break; if (entry.name.matches(Regex("xl/worksheets/sheet\\d+\\.xml"))) { found = zip.readBytes(); break } }
        found
    } ?: return ""
    val rows = secureXml(sheet)?.elements("row").orEmpty()
    val header = rows.firstOrNull()?.elements("c").orEmpty().map { cell -> xlsxCell(cell, shared) }
    val values = rows.drop(1).firstOrNull { row -> row.elements("c").any { xlsxCell(it, shared).isNotBlank() } }?.elements("c")?.map { xlsxCell(it, shared) }.orEmpty()
    return header.mapIndexedNotNull { index, label -> values.getOrNull(index)?.takeIf(String::isNotBlank)?.let { "$label: $it" } }.joinToString("\n")
}

private fun xlsxCell(cell: Element, shared: List<String>): String {
    val raw = cell.getElementsByTagNameNS("*", "v").item(0)?.textContent.orEmpty()
    return if (cell.getAttribute("t") == "s") shared.getOrNull(raw.toIntOrNull() ?: -1).orEmpty()
    else cell.getElementsByTagNameNS("*", "t").item(0)?.textContent.orEmpty().ifBlank { raw }
}

private suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { result -> if (continuation.isActive) continuation.resume(result) }
    addOnFailureListener { error -> if (continuation.isActive) continuation.resumeWithException(error) }
    addOnCanceledListener { if (continuation.isActive) continuation.cancel() }
}
