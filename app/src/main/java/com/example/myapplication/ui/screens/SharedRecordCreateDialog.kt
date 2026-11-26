package com.example.myapplication.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.model.ProjectMetadata
import com.example.myapplication.data.model.SharedAccess
import com.example.myapplication.data.model.SharedPickerOption
import com.example.myapplication.ui.viewmodel.MainViewModel
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import com.example.myapplication.BuildConfig
import kotlinx.coroutines.launch
import java.util.Locale

private fun readMobileRecordCatalogue(context: Context): JsonObject? = runCatching {
    context.assets.open("department-record-catalogue.json").bufferedReader().use { reader ->
        Json.parseToJsonElement(reader.readText()).jsonObject["departments"]?.jsonObject
    }
}.getOrNull()

private data class RecordSourceFile(val uri: Uri, val name: String, val mimeType: String, val bytes: ByteArray)

internal fun recordProjectLinkMode(
    departmentCode: String,
    recordType: String,
    configuredMode: String?,
    fields: List<JsonObject>,
    lifecycleRequired: Boolean = false,
): String {
    val code = departmentCode.uppercase(Locale.ROOT).replace("/", "")
    if ((code == "SAL" && recordType == "Enquiry") || code in setOf("HR", "HROPS")) return "none"
    if (lifecycleRequired) return "required"
    configuredMode?.takeIf { it in setOf("optional", "required") }?.let { return it }
    val projectField = fields.firstOrNull { field ->
        SharedAccess.text(field, "master") == "projects" ||
            SharedAccess.text(field, "key") in setOf("projectId", "project_id", "linkedProjectId")
    } ?: return "none"
    return if (projectField["required"]?.jsonPrimitive?.contentOrNull == "true") "required" else "optional"
}

private fun resolveMasterSelectionId(
    master: String,
    key: String,
    values: Map<String, String>,
    options: List<SharedPickerOption>
): String {
    val identityKeys = when (master) {
        "customers" -> listOf("customerId", "customer_id")
        "suppliers" -> listOf("supplierId", "supplier_id")
        "parts" -> listOf("partId", "part_id")
        "products" -> listOf("productId", "product_id")
        "projects" -> listOf("projectId", "project_id", "linkedProjectId")
        "people", "locations", "routes" -> listOf("${key}Id")
        else -> emptyList()
    }
    val candidates = (identityKeys + key).mapNotNull { values[it]?.trim()?.takeIf(String::isNotBlank) }.toSet()
    if (candidates.isEmpty()) return ""
    val matches = options.filter { option ->
        option.id in candidates || option.reference in candidates || option.title in candidates || option.mpn in candidates
    }.map { it.id }.distinct()
    return matches.singleOrNull().orEmpty()
}

@Composable
private fun SearchableMasterPicker(
    label: String,
    options: List<SharedPickerOption>,
    selectedId: String,
    required: Boolean,
    enabled: Boolean,
    onSelect: (String) -> Unit
) {
    var open by remember(label) { mutableStateOf(false) }
    var search by remember(label) { mutableStateOf("") }
    val selected = options.firstOrNull { it.id == selectedId }
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        OutlinedButton(
            onClick = { open = true },
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled && options.isNotEmpty()
        ) {
            Text(
                text = "$label · ${selected?.label ?: if (options.isEmpty()) "No authorized entries" else "Search or select"}",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (options.isEmpty()) {
            Text(
                "No authorized $label entries are configured. Manage the master list first; this form will not accept an unlinked value.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    if (open) {
        val filtered = options.filter { option ->
            val query = search.trim()
            query.isBlank() || "${option.label} ${option.id}".contains(query, ignoreCase = true)
        }
        AlertDialog(
            onDismissRequest = { open = false; search = "" },
            title = { Text("Select $label") },
            text = {
                Column(
                    Modifier.fillMaxWidth().heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    OutlinedTextField(
                        value = search,
                        onValueChange = { search = it },
                        label = { Text("Search $label") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    if (!required) {
                        TextButton(onClick = { onSelect(""); open = false; search = "" }) { Text("Clear selection") }
                    }
                    if (filtered.isEmpty()) Text("No matching authorized entries.", style = MaterialTheme.typography.bodySmall)
                    filtered.forEach { option ->
                        TextButton(
                            onClick = { onSelect(option.id); open = false; search = "" },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(option.label.ifBlank { option.id }, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { open = false; search = "" }) { Text("Close") } }
        )
    }
}

/** Shared creation form sourced from the Web record-form-catalogue export. */
@Composable
fun SharedRecordCreateDialog(
    viewModel: MainViewModel,
    snapshot: JsonObject?,
    projectCatalog: List<ProjectMetadata>,
    selectedProjectId: String,
    viewDepartment: String?,
    initialDepartmentId: String? = null,
    initialRecordType: String? = null,
    initialRecord: JsonObject? = null,
    lifecycleRequiredRecordTypes: Set<String> = emptySet(),
    smartUploadMode: Boolean = false,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val catalogue = remember(context) { readMobileRecordCatalogue(context) }
    var sourceFile by remember { mutableStateOf<RecordSourceFile?>(null) }
    var sourceBusy by remember { mutableStateOf(false) }
    var sourceStatus by remember { mutableStateOf<String?>(null) }
    var ocrText by remember { mutableStateOf("") }
    var showOcrText by remember { mutableStateOf(false) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var pendingCameraFile by remember { mutableStateOf<java.io.File?>(null) }
    var attachmentAttemptPending by remember { mutableStateOf(false) }
    var pendingCreatePayload by remember { mutableStateOf<JsonObject?>(null) }
    var pendingCreateReference by remember { mutableStateOf<String?>(null) }
    fun acceptSource(uri: Uri, fallbackName: String? = null, cleanup: (() -> Unit)? = null) {
        coroutineScope.launch {
            sourceBusy = true
            sourceStatus = null
            sourceFile = null
            var acceptedSource: RecordSourceFile? = null
            try {
                val mime = context.contentResolver.getType(uri)?.lowercase(Locale.ROOT) ?: "application/octet-stream"
                val stream = context.contentResolver.openInputStream(uri) ?: error("Could not open selected file.")
                val bytes = stream.use { input ->
                    val output = java.io.ByteArrayOutputStream()
                    val buffer = ByteArray(16 * 1024)
                    var total = 0
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        require(total <= 25 * 1024 * 1024) { "Choose a file up to 25 MB." }
                        output.write(buffer, 0, count)
                    }
                    output.toByteArray()
                }
                require(bytes.isNotEmpty()) { "The selected file is empty." }
                val name = fallbackName ?: runCatching {
                    context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) cursor.getString(0) else null
                    }
                }.getOrNull() ?: "record-source-${System.currentTimeMillis()}"
                acceptedSource = RecordSourceFile(uri, name, mime, bytes)
                sourceFile = acceptedSource
                ocrText = ""
                val extension = name.substringAfterLast('.', "").lowercase(Locale.ROOT)
                val isImage = mime.startsWith("image/") || extension in setOf("png", "jpg", "jpeg", "webp", "bmp", "tif", "tiff", "heic")
                val isPdf = mime == "application/pdf" || extension == "pdf"
                val structuredText = extractStructuredRecordSource(name, mime, bytes)
                if (isImage || isPdf) {
                    ocrText = recognizeRecordSource(context, uri)
                    sourceStatus = if (ocrText.isBlank()) "No readable text was found. Fill the record fields manually; the original can still be attached." else "On-device OCR found text. Matching labeled fields will be suggested for your review."
                } else if (structuredText != null) {
                    ocrText = structuredText
                    sourceStatus = if (ocrText.isBlank()) "File attached. No readable text rows were found; fill the record fields manually." else "Text extracted locally. Review and map suggested values before saving."
                } else {
                    sourceStatus = "File attached. This format is not text-extracted here; complete the fields manually and keep the original linked."
                }
            } catch (e: Exception) {
                ocrText = ""
                sourceFile = acceptedSource
                sourceStatus = if (acceptedSource != null) {
                    "Original kept. On-device OCR could not read it; fill the record fields manually."
                } else {
                    e.message ?: "Could not read the selected file."
                }
            } finally {
                sourceBusy = false
                cleanup?.invoke()
            }
        }
    }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) acceptSource(uri)
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { captured ->
        val uri = pendingCameraUri
        val file = pendingCameraFile
        pendingCameraUri = null
        pendingCameraFile = null
        if (captured && uri != null) acceptSource(uri, file?.name, cleanup = { file?.delete() })
        else file?.delete()
    }
    val membership = snapshot?.get("membership") as? JsonObject
    val role = membership?.let { SharedAccess.text(it, "role") }.orEmpty()
    val isAdmin = role == "admin"
    val departments = SharedAccess.departments(snapshot)
    val primaryDepartment = viewDepartment ?: departments.firstOrNull { SharedAccess.text(it, "code").equals(SharedAccess.text(membership ?: JsonObject(emptyMap()), "department_code"), true) }
        ?.let { SharedAccess.text(it, "id") }
        ?: membership?.let { SharedAccess.text(it, "department_id") }.orEmpty()
    val startingDepartment = initialDepartmentId ?: primaryDepartment
    var departmentId by remember(primaryDepartment, initialDepartmentId) { mutableStateOf(startingDepartment) }
    val department = departments.firstOrNull { SharedAccess.text(it, "id") == departmentId }
    val departmentCode = department?.let { SharedAccess.text(it, "code") }
        ?: membership?.let { SharedAccess.text(it, "department_code") }.orEmpty()
    val spec = catalogue?.get(recordCatalogueDepartmentCode(departmentCode))?.jsonObject
    val typeOptions = spec?.get("types") as? JsonArray
    val types = typeOptions?.mapNotNull { it.jsonPrimitive.contentOrNull }.orEmpty()
    var recordType by remember(departmentCode, initialRecordType) {
        mutableStateOf(initialRecordType?.takeIf { it in types } ?: types.firstOrNull().orEmpty())
    }
    var title by remember(recordType, departmentCode) { mutableStateOf("") }
    val fields = ((spec?.get("fieldsByType") as? JsonObject)?.get(recordType) as? JsonArray)
        ?.mapNotNull { it as? JsonObject }.orEmpty()
    val ocrPairs = remember(ocrText) { parseRecordSourcePairs(ocrText) }
    val simpleOcrFields = fields.filter { field ->
        SharedAccess.text(field, "master").isBlank() &&
            field["readOnly"]?.jsonPrimitive?.contentOrNull != "true" &&
            (field["options"] as? JsonArray).isNullOrEmpty() &&
            SharedAccess.text(field, "key") != "projectId"
    }
    val initialOcrTargets = remember(ocrText, recordType, departmentCode) {
        ocrPairs.associate { pair ->
            pair.id to if (isRecordTitleSourceLabel(pair.label)) "__record_title"
            else suggestRecordSourceField(pair, simpleOcrFields)
        }
    }
    var ocrTargets by remember(ocrText, recordType, departmentCode) { mutableStateOf(initialOcrTargets) }
    val editingRecord = initialRecord != null
    val rawInitialFieldValues = remember(initialRecord) {
        (initialRecord?.get("fields") as? JsonObject)?.mapValues { (_, value) ->
            (value as? JsonPrimitive)?.contentOrNull.orEmpty()
        }.orEmpty()
    }
    val allowedProjects = projectCatalog.filter { it.id != "ALL" }
    val resolvedInitialFieldValues: Map<String, String> = remember(initialRecord, fields, snapshot, allowedProjects) {
        rawInitialFieldValues.toMutableMap().apply {
            fields.forEach { field ->
                val key = SharedAccess.text(field, "key")
                val master = SharedAccess.text(field, "master")
                if (key.isNotBlank() && master.isNotBlank()) {
                    val options = SharedAccess.masterPickerOptions(snapshot, master, allowedProjects)
                    val id = resolveMasterSelectionId(master, key, this, options)
                    if (id.isNotBlank()) this[key] = id
                }
            }
        }.toMap()
    }
    val originalUpdatedAt = initialRecord?.let { SharedAccess.text(it, "updated_at") }.orEmpty()
    var notes by remember(initialRecord?.toString()) { mutableStateOf(initialRecord?.let { SharedAccess.text(it, "notes") }.orEmpty()) }
    val currentRevision = rawInitialFieldValues["revision"].orEmpty().ifBlank { "01" }
    val originalReference = initialRecord?.let { SharedAccess.text(it, "reference") }.orEmpty()
    val revisionFamily = Regex("-\\d{2,}$").replace(originalReference.trim(), "")
    val maxFamilyRevision = SharedAccess.rows(snapshot, "records")
        .filter { record -> Regex("-\\d{2,}$").replace(SharedAccess.text(record, "reference").trim(), "") == revisionFamily }
        .mapNotNull { record -> ((record["fields"] as? JsonObject)?.get("revision") as? JsonPrimitive)?.contentOrNull?.toIntOrNull() }
        .maxOrNull() ?: currentRevision.toIntOrNull() ?: 1
    val nextRevision = (maxFamilyRevision + 1).toString().padStart(2, '0')
    val revisionChoices = listOf(currentRevision, nextRevision).distinct()
    var selectedRevision by remember(initialRecord?.toString()) { mutableStateOf(currentRevision) }
    val projectLinkMode = recordProjectLinkMode(
        departmentCode,
        recordType,
        (spec?.get("projectLinkByType") as? JsonObject)?.get(recordType)?.jsonPrimitive?.contentOrNull,
        fields,
        "${recordCatalogueDepartmentCode(departmentCode)}|$recordType" in lifecycleRequiredRecordTypes,
    )
    val projectApplicable = projectLinkMode != "none"
    val needsProject = projectLinkMode == "required"
    var projectId by remember(selectedProjectId, departmentCode, recordType) {
        mutableStateOf(selectedProjectId.takeIf { selected -> projectApplicable && allowedProjects.any { it.id == selected } }.orEmpty())
    }
    var departmentMenu by remember { mutableStateOf(false) }
    var typeMenu by remember { mutableStateOf(false) }
    var values by remember(recordType, departmentCode, initialRecord?.toString(), snapshot) { mutableStateOf(resolvedInitialFieldValues) }
    LaunchedEffect(ocrText, recordType, departmentCode) {
        ocrPairs.forEach { pair ->
            val target = ocrTargets[pair.id].orEmpty()
            if (target == "__record_title" && title.isBlank()) title = pair.value
            else if (target.isNotBlank() && values[target].isNullOrBlank()) values = values + (target to pair.value)
        }
    }
    var error by remember { mutableStateOf<String?>(null) }
    val busy by viewModel.sharedBusy.collectAsState()
    val saved by viewModel.sharedSaved.collectAsState()
    val sharedError by viewModel.sharedError.collectAsState()
    var savedAtOpen by remember { mutableStateOf(saved) }
    LaunchedEffect(saved) { if (saved > savedAtOpen) onSaved() }

    val requiredMissing = fields.any { field ->
        if (field["required"]?.jsonPrimitive?.contentOrNull != "true") false else {
            val key = SharedAccess.text(field, "key")
            val master = SharedAccess.text(field, "master")
            if (master.isNotBlank()) resolveMasterSelectionId(master,key,values,SharedAccess.masterPickerOptions(snapshot,master,allowedProjects)).isBlank()
            else values[key].isNullOrBlank()
        }
    }
    val canSave = if (editingRecord) {
        SharedAccess.text(initialRecord!!, "id").isNotBlank() && originalUpdatedAt.isNotBlank() && !requiredMissing && !busy
    } else {
        title.isNotBlank() && recordType.isNotBlank() && departmentId.isNotBlank() &&
            (!needsProject || projectId.isNotBlank()) && !requiredMissing && !busy && !sourceBusy &&
            (!smartUploadMode || sourceFile != null)
    }

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(if (editingRecord) "Edit original record" else "New ${departmentCode.ifBlank { "department" }} record") },
        text = {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    when {
                        editingRecord -> "This updates the existing shared record with its current version check. Its reference, department, project, and status are kept; formal approvals stay in their workflow."
                        smartUploadMode -> "Scan or choose a source file. On-device OCR suggests only explicitly labeled values; review them, complete the required fields, then save the Draft record with its private source reference."
                        else -> "Record types and required fields are loaded from the Web record form catalogue. Save uses the authenticated shared record action; status remains Draft."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!editingRecord) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = {
                                runCatching {
                                    val directory = java.io.File(context.cacheDir, "record-scans").apply { mkdirs() }
                                    val file = java.io.File.createTempFile("record-source-", ".jpg", directory)
                                    val uri = FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.provider", file)
                                    pendingCameraUri = uri
                                    pendingCameraFile = file
                                    cameraLauncher.launch(uri)
                                }.onFailure { failure -> sourceStatus = failure.message ?: "Could not start camera." }
                            },
                            enabled = !busy && !sourceBusy && !attachmentAttemptPending,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null)
                            Text(" Scan")
                        }
                        OutlinedButton(
                            onClick = { filePicker.launch(arrayOf("application/pdf", "image/*", "text/*", "text/csv", "application/json", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "application/vnd.ms-excel", "application/msword", "application/zip", "application/octet-stream", "model/step", "model/stl")) },
                            enabled = !busy && !sourceBusy && !attachmentAttemptPending,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.AttachFile, contentDescription = null)
                            Text(" Choose file")
                        }
                    }
                    if (sourceBusy) Text("Reading source and checking for text on this device…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    sourceFile?.let { file ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Source · ${file.name} · ${file.bytes.size / 1024} KB", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, maxLines = 2)
                            TextButton(enabled = !busy && !sourceBusy && !attachmentAttemptPending, onClick = { sourceFile = null; ocrText = ""; sourceStatus = null }) { Text("Remove") }
                        }
                    }
                    sourceStatus?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    if (ocrText.isNotBlank()) {
                        Text("Recognized text · ${ocrText.length} characters · ${ocrTargets.values.count(String::isNotBlank)} field suggestion${if (ocrTargets.values.count(String::isNotBlank) == 1) "" else "s"}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Text("Review the values, choose a destination for unmatched labels, and apply them. Project must still be selected from the authorized list.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (ocrPairs.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                ocrPairs.take(20).forEach { pair ->
                                    var mappingOpen by remember(pair.id, recordType) { mutableStateOf(false) }
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text("${pair.label}: ${pair.value}", style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                        val target = ocrTargets[pair.id].orEmpty()
                                        val targetLabel = if (target == "__record_title") "Record title" else simpleOcrFields.firstOrNull { SharedAccess.text(it, "key") == target }?.let { SharedAccess.text(it, "label") }.orEmpty()
                                        OutlinedButton(onClick = { mappingOpen = true }, modifier = Modifier.fillMaxWidth(), enabled = !attachmentAttemptPending && !busy) {
                                            Text("${pair.label} → ${targetLabel.ifBlank { "Choose field" }}", maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                        DropdownMenu(expanded = mappingOpen, onDismissRequest = { mappingOpen = false }) {
                                            DropdownMenuItem(text = { Text("Do not map") }, onClick = { ocrTargets = ocrTargets + (pair.id to ""); mappingOpen = false })
                                            DropdownMenuItem(text = { Text("Record title") }, onClick = { ocrTargets = ocrTargets + (pair.id to "__record_title"); mappingOpen = false })
                                            simpleOcrFields.forEach { field ->
                                                val key = SharedAccess.text(field, "key")
                                                DropdownMenuItem(text = { Text(SharedAccess.text(field, "label").ifBlank { key }) }, onClick = { ocrTargets = ocrTargets + (pair.id to key); mappingOpen = false })
                                            }
                                        }
                                    }
                                }
                            }
                            Button(onClick = {
                                ocrPairs.forEach { pair ->
                                    val target = ocrTargets[pair.id].orEmpty()
                                    if (target == "__record_title") title = pair.value
                                    else if (target.isNotBlank()) values = values + (target to pair.value)
                                }
                            }, enabled = !busy && !attachmentAttemptPending && ocrTargets.values.any(String::isNotBlank), modifier = Modifier.fillMaxWidth()) {
                                Text("Apply mapped values to form")
                            }
                        } else {
                            Text("No labeled field/value pairs were found. You can still fill the form manually and attach this source.", style = MaterialTheme.typography.bodySmall)
                        }
                        if (showOcrText) OutlinedTextField(value = ocrText, onValueChange = {}, readOnly = true, label = { Text("Recognized text · review only") }, modifier = Modifier.fillMaxWidth(), minLines = 3, maxLines = 7)
                        TextButton(onClick = { showOcrText = !showOcrText }) { Text(if (showOcrText) "Hide recognized text" else "Review recognized text") }
                    }
                }
                if (editingRecord) {
                    Text("${department?.let { SharedAccess.text(it, "code") } ?: departmentCode} · ${SharedAccess.text(initialRecord!!, "record_type")} · ${SharedAccess.text(initialRecord!!, "reference")}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (originalUpdatedAt.isBlank()) Text("This record is missing its server version timestamp, so edits are disabled until the workspace refreshes.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    Text("Revision · choose Current or Next", style=MaterialTheme.typography.labelMedium)
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                        revisionChoices.forEachIndexed { index, revision ->
                            val label=if(index==0) "$revision · Current" else "$revision · Next revision"
                            if(selectedRevision==revision) Button(onClick={selectedRevision=revision},modifier=Modifier.weight(1f),enabled=!busy) { Text(label,maxLines=1) }
                            else OutlinedButton(onClick={selectedRevision=revision},modifier=Modifier.weight(1f),enabled=!busy) { Text(label,maxLines=1) }
                        }
                    }
                }
                if (!editingRecord && isAdmin && viewDepartment == null) {
                    OutlinedButton(onClick = { departmentMenu = true }, modifier = Modifier.fillMaxWidth(), enabled = !busy && !attachmentAttemptPending) {
                        Text("Department · ${department?.let { SharedAccess.text(it, "code") } ?: "Choose"}")
                    }
                    DropdownMenu(expanded = departmentMenu, onDismissRequest = { departmentMenu = false }) {
                        departments.forEach { item -> DropdownMenuItem(text = { Text(SharedAccess.text(item, "name")) }, onClick = { departmentId = SharedAccess.text(item, "id"); departmentMenu = false }) }
                    }
                }
                if (types.isNotEmpty() && !editingRecord) {
                    OutlinedButton(onClick = { typeMenu = true }, modifier = Modifier.fillMaxWidth(), enabled = !busy && !attachmentAttemptPending) { Text("Record type · $recordType") }
                    DropdownMenu(expanded = typeMenu, onDismissRequest = { typeMenu = false }) {
                        types.forEach { option -> DropdownMenuItem(text = { Text(option) }, onClick = { recordType = option; typeMenu = false }) }
                    }
                } else if (!editingRecord) Text("No record form is configured for this department in the Web catalogue.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                if (!editingRecord) OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title · required") }, modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !busy && !attachmentAttemptPending)
                if (!editingRecord && projectApplicable) {
                    val projectPickerOptions = SharedAccess.masterPickerOptions(snapshot, "projects", allowedProjects) +
                        if (!needsProject) listOf(SharedPickerOption("", "", "Department work · no project")) else emptyList()
                    SearchableMasterPicker(
                        label = if (needsProject) "Project · required" else "Project · optional",
                        options = projectPickerOptions,
                        selectedId = projectId,
                        required = needsProject,
                        enabled = !busy && !attachmentAttemptPending,
                        onSelect = { projectId = it }
                    )
                }
                fields.forEach { field ->
                    val key = SharedAccess.text(field, "key")
                    val label = SharedAccess.text(field, "label") + if (field["required"]?.jsonPrimitive?.contentOrNull == "true") " · required" else ""
                    val options = (field["options"] as? JsonArray)?.mapNotNull { it.jsonPrimitive.contentOrNull }.orEmpty()
                    val master = SharedAccess.text(field, "master")
                    if (field["readOnly"]?.jsonPrimitive?.contentOrNull == "true") {
                        val automatic = SharedAccess.text(field, "automaticValue")
                        if (automatic.isNotBlank()) Text("$label · $automatic", style = MaterialTheme.typography.bodySmall)
                    } else if (master.isNotBlank()) {
                        val masterOptions = SharedAccess.masterPickerOptions(snapshot, master, allowedProjects)
                        SearchableMasterPicker(
                            label = SharedAccess.text(field, "label").ifBlank { key },
                            options = masterOptions,
                            selectedId = if (editingRecord) resolveMasterSelectionId(master,key,values,masterOptions).ifBlank { values[key].orEmpty() } else values[key].orEmpty(),
                            required = field["required"]?.jsonPrimitive?.contentOrNull == "true",
                            enabled = !busy && !attachmentAttemptPending,
                            onSelect = { selectedId ->
                                val selected = masterOptions.firstOrNull { it.id == selectedId }
                                values = values + (key to selectedId)
                                when (master) {
                                    "customers" -> values = values + mapOf("customerId" to selectedId, "customer_id" to selectedId, "customerName" to selected?.title.orEmpty(), "customerCode" to selected?.reference.orEmpty())
                                    "suppliers" -> values = values + mapOf("supplierId" to selectedId, "supplier_id" to selectedId, "supplierName" to selected?.title.orEmpty(), "supplierCode" to selected?.reference.orEmpty())
                                    "parts" -> values = values + mapOf("partId" to selectedId, "part_id" to selectedId, "partCode" to selected?.reference.orEmpty(), "mpn" to selected?.mpn.orEmpty())
                                    "products" -> values = values + mapOf("productId" to selectedId, "product_id" to selectedId, "productCode" to selected?.reference.orEmpty())
                                    "projects" -> values = values + mapOf("project_id" to selectedId)
                                    "people" -> values = values + ("${key}Id" to selectedId)
                                    "locations" -> values = values + ("${key}Id" to selectedId)
                                    "routes" -> values = values + ("${key}Id" to selectedId)
                                }
                            }
                        )
                    } else if (options.isNotEmpty()) {
                        if (options.size in 2..3) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                options.forEach { option ->
                                    val selected = values[key] == option
                                    if (selected) {
                                        Button(onClick = { values = values + (key to option) }, modifier = Modifier.weight(1f), enabled = !busy && !attachmentAttemptPending) {
                                            Text(option, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelSmall)
                                        }
                                    } else {
                                        OutlinedButton(onClick = { values = values + (key to option) }, modifier = Modifier.weight(1f), enabled = !busy && !attachmentAttemptPending) {
                                            Text(option, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }
                        } else {
                            var open by remember(key) { mutableStateOf(false) }
                            OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth(), enabled = !busy && !attachmentAttemptPending) { Text("$label · ${values[key] ?: "Choose"}") }
                            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                                options.forEach { option -> DropdownMenuItem(text = { Text(option) }, onClick = { values = values + (key to option); open = false }) }
                            }
                        }
                    } else {
                        OutlinedTextField(value = values[key].orEmpty(), onValueChange = { values = values + (key to it) }, label = { Text(label) }, modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !busy && !attachmentAttemptPending)
                    }
                }
                if (editingRecord) OutlinedTextField(value=notes,onValueChange={notes=it},label={Text("Notes")},modifier=Modifier.fillMaxWidth(),enabled=!busy,minLines=2,maxLines=4)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                sharedError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            Button(enabled = canSave && (editingRecord || types.isNotEmpty()), onClick = {
                if (editingRecord) {
                    val original = initialRecord!!
                    val existingFields = original["fields"] as? JsonObject ?: JsonObject(emptyMap())
                    val updatedFields = buildJsonObject {
                        existingFields.forEach { (key, value) -> put(key, value) }
                        put("revision", selectedRevision)
                        fields.forEach { field ->
                            val key = SharedAccess.text(field, "key")
                            if (key.isNotBlank() && field["readOnly"]?.jsonPrimitive?.contentOrNull != "true") {
                                val master = SharedAccess.text(field, "master")
                                val options = if (master.isNotBlank()) SharedAccess.masterPickerOptions(snapshot,master,allowedProjects) else emptyList()
                                val selectedId = if (master.isNotBlank()) resolveMasterSelectionId(master,key,values,options) else ""
                                val selected = options.firstOrNull { it.id == selectedId }
                                put(key, (selected?.title ?: values[key].orEmpty()).trim())
                            }
                        }
                        listOf("customerId", "customer_id", "customerName", "customerCode", "supplierId", "supplier_id", "supplierName", "supplierCode", "partId", "part_id", "partCode", "mpn", "productId", "product_id", "productCode", "project_id")
                            .forEach { key -> values[key]?.let { put(key, it) } }
                        fields.forEach { field ->
                            val master = SharedAccess.text(field, "master")
                            val key = SharedAccess.text(field, "key")
                            val id = values[key].orEmpty()
                            if (id.isNotBlank() && master in setOf("people", "locations", "routes")) put("${key}Id", id)
                        }
                    }
                    error = null
                    savedAtOpen = saved
                    viewModel.mutateSharedWorkspace("update_record", buildJsonObject {
                        put("record_id", SharedAccess.text(original, "id"))
                        put("expected_updated_at", originalUpdatedAt)
                        put("fields", updatedFields)
                        put("notes", notes.trim())
                    })
                    return@Button
                }
                val selectedDepartment = departments.firstOrNull { SharedAccess.text(it, "id") == departmentId }
                if (selectedDepartment == null) { error = "Choose a permitted department."; return@Button }
                val reference = pendingCreateReference ?: "HLPL-MOB-${SharedAccess.text(selectedDepartment, "code")}-${System.currentTimeMillis()}".also { pendingCreateReference = it }
                val payload = pendingCreatePayload ?: buildJsonObject {
                    put("record_type", recordType)
                    put("reference", reference)
                    put("title", title.trim())
                    put("department_id", departmentId)
                    if (projectId.isNotBlank()) put("project_id", projectId)
                    put("fields", buildJsonObject {
                        put("revision", "01")
                        fields.forEach { field ->
                            val key = SharedAccess.text(field, "key")
                            val value = if (field["readOnly"]?.jsonPrimitive?.contentOrNull == "true") SharedAccess.text(field, "automaticValue") else values[key].orEmpty().trim()
                            if (value.isNotBlank()) put(key, value)
                        }
                        listOf("customerId", "customer_id", "customerName", "customerCode", "supplierId", "supplier_id", "supplierName", "supplierCode", "partId", "part_id", "partCode", "mpn", "productId", "product_id", "productCode", "project_id")
                            .forEach { key -> values[key]?.takeIf(String::isNotBlank)?.let { put(key, it) } }
                        fields.forEach { field ->
                            val master = SharedAccess.text(field, "master")
                            val key = SharedAccess.text(field, "key")
                            val id = values[key].orEmpty()
                            if (id.isNotBlank() && master in setOf("people", "locations", "routes")) put("${key}Id", id)
                        }
                    })
                }.also { pendingCreatePayload = it }
                error = null
                savedAtOpen = saved
                val selectedSource = sourceFile
                if (selectedSource != null) {
                    attachmentAttemptPending = true
                    coroutineScope.launch {
                        try {
                            viewModel.createSharedRecordWithOriginal(
                                recordPayload = payload,
                                departmentId = departmentId,
                                projectId = projectId.takeIf(String::isNotBlank),
                                recordTitle = title.trim(),
                                recordType = recordType,
                                originalName = selectedSource.name,
                                contentType = selectedSource.mimeType,
                                bytes = selectedSource.bytes
                            )
                        } catch (e: Exception) {
                            error = (e.message ?: "Record source upload failed.") + " Keep the same source and retry; the form is locked to prevent duplicate records."
                        }
                    }
                } else {
                    viewModel.mutateSharedWorkspace("create_record", payload)
                }
            }) { Text(if (busy) "Saving record & source…" else if (editingRecord) "Save changes" else if (sourceFile != null) "Create record & attach source" else "Save Draft") }
        },
        dismissButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text("Cancel") } }
    )
}
