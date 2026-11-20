package com.example.myapplication.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@Composable
internal fun SharedTravelPolicyEditor(
    policyData: JsonObject,
    departments: List<JsonObject>,
    selectedDepartmentId: String,
    onDepartmentSelected: (String) -> Unit,
    isAdmin: Boolean,
    saving: Boolean,
    saveMessage: String?,
    onSave: (departmentId: String?, value: JsonObject, version: Int, publish: Boolean) -> Unit
) {
    val company = policyData["company"] as? JsonObject ?: return
    val departmentPolicy = policyData["department"] as? JsonObject
    val selectedPolicy = if (selectedDepartmentId.isNotBlank()) departmentPolicy ?: company else company
    val selectedDraft = if (selectedDepartmentId.isNotBlank())
        (policyData["departmentDraft"] as? JsonObject) ?: selectedPolicy
    else (policyData["companyDraft"] as? JsonObject) ?: selectedPolicy
    val key = "$selectedDepartmentId:${policyData["companyVersion"]}:${policyData["departmentVersion"]}"
    var draft by remember(key, selectedDraft) { mutableStateOf(selectedDraft) }
    var scopeOpen by remember { mutableStateOf(false) }
    var dateOpen by remember { mutableStateOf(false) }
    var localMessage by remember(key) { mutableStateOf<String?>(null) }

    val canManage = if (selectedDepartmentId.isBlank())
        policyData["canManageCompany"]?.jsonPrimitive?.contentOrNull == "true"
    else policyData["canManageDepartment"]?.jsonPrimitive?.contentOrNull == "true"
    val version = if (selectedDepartmentId.isBlank())
        policyData["companyVersion"]?.jsonPrimitive?.intOrNull ?: 0
    else policyData["departmentVersion"]?.jsonPrimitive?.intOrNull ?: 0
    val scopeLabel = if (selectedDepartmentId.isBlank()) "Company policy"
    else departments.firstOrNull { it.string("id") == selectedDepartmentId }?.string("name")?.let { "$it override" } ?: "Department override"
    val bands = (draft["bands"] as? JsonArray)?.mapNotNull { it as? JsonObject }.orEmpty()

    fun updateTop(key: String, value: JsonPrimitive?) {
        draft = JsonObject(draft.toMutableMap().apply {
            if (value == null) remove(key) else put(key, value)
        })
        localMessage = null
    }
    fun updateBand(index: Int, field: String, value: String) {
        val amount = value.toDoubleOrNull() ?: return
        if (!amount.isFinite() || amount < 0.0 || amount > 1_000_000.0) return
        val updated = bands.toMutableList()
        val band = updated.getOrNull(index) ?: return
        updated[index] = JsonObject(band.toMutableMap().apply { put(field, JsonPrimitive(amount)) })
        draft = JsonObject(draft.toMutableMap().apply { put("bands", JsonArray(updated)) })
        localMessage = null
    }

    Card(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Travel policy · INR", style = MaterialTheme.typography.titleSmall)
            Text("Illustrative India-company defaults. Confirm rates with Finance before publishing; these amounts are not yet enforced as request caps.", style = MaterialTheme.typography.bodySmall)
            if (isAdmin) {
                androidx.compose.foundation.layout.Box {
                    OutlinedButton(onClick = { scopeOpen = true }, enabled = !saving) { Text(scopeLabel, maxLines = 1) }
                    DropdownMenu(expanded = scopeOpen, onDismissRequest = { scopeOpen = false }) {
                        DropdownMenuItem(text = { Text("Company policy") }, onClick = { scopeOpen = false; onDepartmentSelected("") })
                        departments.filter { it.string("code") != "CLIENT" }.forEach { item ->
                            DropdownMenuItem(text = { Text("${item.string("name")} override") }, onClick = {
                                scopeOpen = false
                                onDepartmentSelected(item.string("id"))
                            })
                        }
                    }
                }
            } else Text(scopeLabel, style = MaterialTheme.typography.labelMedium)

            Text(
                if (selectedPolicy.string("status") == "Active") "${selectedPolicy.string("status")} · effective ${selectedPolicy.string("effectiveFrom").ifBlank { "date not set" }}"
                else "Draft only · not used to approve or cap expense requests",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary
            )
            if (selectedDepartmentId.isNotBlank()) Text(
                "Department rates must stay within the published company matrix. Operations leads can edit only their own department; Admin can manage any department.",
                style = MaterialTheme.typography.bodySmall
            )

            bands.forEachIndexed { index, band ->
                Text(band.string("label"), style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    PolicyAmountField("Hotel · Metro / night", band.string("hotelMetro"), canManage && !saving, Modifier.weight(1f)) { updateBand(index, "hotelMetro", it) }
                    PolicyAmountField("Hotel · Other / night", band.string("hotelOther"), canManage && !saving, Modifier.weight(1f)) { updateBand(index, "hotelOther", it) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    PolicyAmountField("Meals / day", band.string("mealsPerDay"), canManage && !saving, Modifier.weight(1f)) { updateBand(index, "mealsPerDay", it) }
                    PolicyAmountField("Local travel / day", band.string("localPerDay"), canManage && !saving, Modifier.weight(1f)) { updateBand(index, "localPerDay", it) }
                }
            }

            PolicyAmountField("Advance ceiling · %", draft.string("advancePercent"), canManage && !saving, Modifier.fillMaxWidth(), max = 100.0) {
                val amount = it.toDoubleOrNull() ?: return@PolicyAmountField
                updateTop("advancePercent", JsonPrimitive(amount))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = (draft["oneOpenAdvance"] as? JsonPrimitive)?.contentOrNull == "true",
                    onCheckedChange = { updateTop("oneOpenAdvance", JsonPrimitive(it)) },
                    enabled = canManage && !saving && selectedDepartmentId.isBlank()
                )
                Text("One unsettled advance per requester", style = MaterialTheme.typography.bodySmall)
            }
            if (isAdmin) {
                OutlinedButton(onClick = { dateOpen = true }, enabled = canManage && !saving) {
                    Text(draft.string("effectiveFrom").ifBlank { "Choose effective date" })
                }
            }
            if (!canManage) Text("This role can view this policy but cannot edit it.", style = MaterialTheme.typography.bodySmall)
            (saveMessage ?: localMessage)?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            if (canManage) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onSave(selectedDepartmentId.ifBlank { null }, draft, version, false) }, enabled = !saving) {
                    Text(if (selectedDepartmentId.isNotBlank() && !isAdmin) "Save limits" else "Save draft")
                }
                if (isAdmin) Button(onClick = {
                    if (draft.string("effectiveFrom").isBlank()) localMessage = "Choose an effective date before publishing."
                    else onSave(selectedDepartmentId.ifBlank { null }, draft, version, true)
                }, enabled = !saving) { Text("Publish") }
            }
        }
    }

    if (dateOpen) {
        val initialMillis = runCatching { LocalDate.parse(draft.string("effectiveFrom")).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli() }.getOrNull()
        val picker = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { dateOpen = false },
            confirmButton = { TextButton(onClick = {
                picker.selectedDateMillis?.let { millis -> updateTop("effectiveFrom", JsonPrimitive(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toString())) }
                dateOpen = false
            }) { Text("Select") } },
            dismissButton = { TextButton(onClick = { dateOpen = false }) { Text("Cancel") } }
        ) { DatePicker(state = picker) }
    }
}

@Composable
private fun PolicyAmountField(label: String, value: String, enabled: Boolean, modifier: Modifier, max: Double = 1_000_000.0, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { raw ->
            if (raw.isEmpty() || raw.toDoubleOrNull()?.let { it in 0.0..max } == true) onChange(raw)
        },
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        enabled = enabled,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier
    )
}

private fun JsonObject.string(key: String): String = this[key]?.jsonPrimitive?.contentOrNull.orEmpty()
