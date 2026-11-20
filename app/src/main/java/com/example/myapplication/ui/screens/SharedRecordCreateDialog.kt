package com.example.myapplication.ui.screens

import android.content.Context
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
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

private fun readMobileRecordCatalogue(context: Context): JsonObject? = runCatching {
    context.assets.open("department-record-catalogue.json").bufferedReader().use { reader ->
        Json.parseToJsonElement(reader.readText()).jsonObject["departments"]?.jsonObject
    }
}.getOrNull()

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
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    val catalogue = remember(context) { readMobileRecordCatalogue(context) }
    val membership = snapshot?.get("membership") as? JsonObject
    val role = membership?.let { SharedAccess.text(it, "role") }.orEmpty()
    val isAdmin = role == "admin"
    val departments = SharedAccess.departments(snapshot)
    val primaryDepartment = viewDepartment ?: departments.firstOrNull { SharedAccess.text(it, "code").equals(SharedAccess.text(membership ?: JsonObject(emptyMap()), "department_code"), true) }
        ?.let { SharedAccess.text(it, "id") }
        ?: membership?.let { SharedAccess.text(it, "department_id") }.orEmpty()
    var departmentId by remember(primaryDepartment) { mutableStateOf(primaryDepartment) }
    val department = departments.firstOrNull { SharedAccess.text(it, "id") == departmentId }
    val departmentCode = department?.let { SharedAccess.text(it, "code") }
        ?: membership?.let { SharedAccess.text(it, "department_code") }.orEmpty()
    val spec = catalogue?.get(departmentCode)?.jsonObject
    val typeOptions = spec?.get("types") as? JsonArray
    val types = typeOptions?.mapNotNull { it.jsonPrimitive.contentOrNull }.orEmpty()
    var recordType by remember(departmentCode) { mutableStateOf(types.firstOrNull().orEmpty()) }
    var title by remember(recordType, departmentCode) { mutableStateOf("") }
    val allowedProjects = projectCatalog.filter { it.id != "ALL" }
    var projectId by remember(selectedProjectId, departmentCode) {
        mutableStateOf(selectedProjectId.takeIf { selected -> allowedProjects.any { it.id == selected } }.orEmpty())
    }
    var departmentMenu by remember { mutableStateOf(false) }
    var typeMenu by remember { mutableStateOf(false) }
    var values by remember(recordType, departmentCode) { mutableStateOf<Map<String, String>>(emptyMap()) }
    var error by remember { mutableStateOf<String?>(null) }
    val busy by viewModel.sharedBusy.collectAsState()
    val saved by viewModel.sharedSaved.collectAsState()
    val sharedError by viewModel.sharedError.collectAsState()
    var savedAtOpen by remember { mutableStateOf(saved) }
    LaunchedEffect(saved) { if (saved > savedAtOpen) onSaved() }

    val fields = ((spec?.get("fieldsByType") as? JsonObject)?.get(recordType) as? JsonArray)
        ?.mapNotNull { it as? JsonObject }.orEmpty()
    val projectOptional = recordType in setOf("Supplier", "Part", "Customer")
    val requiredMissing = fields.any { field ->
        field["required"]?.jsonPrimitive?.contentOrNull == "true" && values[SharedAccess.text(field, "key")].isNullOrBlank()
    }
    val needsProject = !projectOptional
    val canSave = title.isNotBlank() && recordType.isNotBlank() && departmentId.isNotBlank() &&
        (projectOptional || projectId.isNotBlank()) && !requiredMissing && !busy

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("New ${departmentCode.ifBlank { "department" }} record") },
        text = {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Record types and required fields are loaded from the Web record form catalogue. Save uses the authenticated shared record action; status remains Draft.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (isAdmin && viewDepartment == null) {
                    OutlinedButton(onClick = { departmentMenu = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Department · ${department?.let { SharedAccess.text(it, "code") } ?: "Choose"}")
                    }
                    DropdownMenu(expanded = departmentMenu, onDismissRequest = { departmentMenu = false }) {
                        departments.forEach { item -> DropdownMenuItem(text = { Text(SharedAccess.text(item, "name")) }, onClick = { departmentId = SharedAccess.text(item, "id"); departmentMenu = false }) }
                    }
                }
                if (types.isNotEmpty()) {
                    OutlinedButton(onClick = { typeMenu = true }, modifier = Modifier.fillMaxWidth()) { Text("Record type · $recordType") }
                    DropdownMenu(expanded = typeMenu, onDismissRequest = { typeMenu = false }) {
                        types.forEach { option -> DropdownMenuItem(text = { Text(option) }, onClick = { recordType = option; typeMenu = false }) }
                    }
                } else Text("No record form is configured for this department in the Web catalogue.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title · required") }, modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !busy)
                val projectPickerOptions = SharedAccess.masterPickerOptions(snapshot, "projects", allowedProjects) +
                    if (projectOptional) listOf(SharedPickerOption("", "", "Department work · no project")) else emptyList()
                SearchableMasterPicker(
                    label = if (needsProject) "Project · required" else "Project · optional",
                    options = projectPickerOptions,
                    selectedId = projectId,
                    required = needsProject,
                    enabled = !busy,
                    onSelect = { projectId = it }
                )
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
                            selectedId = values[key].orEmpty(),
                            required = field["required"]?.jsonPrimitive?.contentOrNull == "true",
                            enabled = !busy,
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
                                        Button(onClick = { values = values + (key to option) }, modifier = Modifier.weight(1f), enabled = !busy) {
                                            Text(option, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelSmall)
                                        }
                                    } else {
                                        OutlinedButton(onClick = { values = values + (key to option) }, modifier = Modifier.weight(1f), enabled = !busy) {
                                            Text(option, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }
                        } else {
                            var open by remember(key) { mutableStateOf(false) }
                            OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) { Text("$label · ${values[key] ?: "Choose"}") }
                            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                                options.forEach { option -> DropdownMenuItem(text = { Text(option) }, onClick = { values = values + (key to option); open = false }) }
                            }
                        }
                    } else {
                        OutlinedTextField(value = values[key].orEmpty(), onValueChange = { values = values + (key to it) }, label = { Text(label) }, modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !busy)
                    }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                sharedError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            Button(enabled = canSave && types.isNotEmpty(), onClick = {
                val selectedDepartment = departments.firstOrNull { SharedAccess.text(it, "id") == departmentId }
                if (selectedDepartment == null) { error = "Choose a permitted department."; return@Button }
                val reference = "HLPL-MOB-${SharedAccess.text(selectedDepartment, "code")}-${System.currentTimeMillis()}"
                val payload = buildJsonObject {
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
                }
                error = null
                savedAtOpen = saved
                viewModel.mutateSharedWorkspace("create_record", payload)
            }) { Text(if (busy) "Saving…" else "Save Draft") }
        },
        dismissButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text("Cancel") } }
    )
}
