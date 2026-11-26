package com.example.myapplication.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.viewmodel.MainViewModel
import kotlinx.serialization.json.*
import kotlinx.coroutines.launch
import java.time.LocalDate

private fun JsonObject.text(key:String)=this[key]?.jsonPrimitive?.contentOrNull.orEmpty()
private fun JsonObject.rows(key:String)=(this[key] as? JsonArray)?.mapNotNull { it as? JsonObject }.orEmpty()

@Composable
private fun SharedSearchPicker(
    label: String,
    selectedLabel: String,
    options: List<Pair<String, String>>,
    enabled: Boolean = true,
    emptyText: String = "No authorized options are available.",
    onSelect: (String) -> Unit
) {
    var open by remember(label) { mutableStateOf(false) }
    var query by remember(label, open) { mutableStateOf("") }
    OutlinedButton(onClick = { open = true }, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
        Text(selectedLabel.ifBlank { "Select $label" }, maxLines = 1)
    }
    if (open) {
        val filtered = options.filter { (_, display) -> display.contains(query.trim(), ignoreCase = true) }
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text("Select $label") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = query, onValueChange = { query = it }, label = { Text("Search $label") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    if (filtered.isEmpty()) Text(if (options.isEmpty()) emptyText else "No matches.", style = MaterialTheme.typography.bodySmall)
                    else LazyColumn(modifier = Modifier.heightIn(max = 360.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        items(filtered, key = { it.first }) { (id, display) ->
                            TextButton(onClick = { onSelect(id); open = false }, modifier = Modifier.fillMaxWidth()) { Text(display, modifier = Modifier.fillMaxWidth(), maxLines = 2) }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { open = false }) { Text("Close") } }
        )
    }
}

internal fun suggestedFinanceCategory(stage:String,requestType:String,categories:List<String>):String? {
    fun find(pattern:Regex)=categories.firstOrNull { pattern.containsMatchIn(it) }
    if(requestType=="travel") return find(Regex("travel\\s*&\\s*transport",RegexOption.IGNORE_CASE))
    if(requestType=="reimbursement") return null
    val normalized=stage.lowercase().replace(Regex("[\\s-]"),"_")
    val phase=Regex("tg_?0?([0-9])").find(normalized)?.groupValues?.getOrNull(1)
    return when {
        "feasibility" in normalized || "enquiry" in normalized || phase in setOf("0","1") -> find(Regex("engineering\\s*&\\s*professional services",RegexOption.IGNORE_CASE))
        listOf("schematic","design","development","prototype").any { it in normalized } || phase in setOf("2","3","4") -> find(Regex("electronic components|pcb fabrication|mechanical prototypes",RegexOption.IGNORE_CASE))
        listOf("pilot","bring_up","build","manufactur").any { it in normalized } || phase=="5" -> find(Regex("consumables|production tooling|equipment",RegexOption.IGNORE_CASE))
        listOf("dvp","test","validation","certification","compliance").any { it in normalized } || phase in setOf("6","7") -> find(Regex("testing|product certification|calibration",RegexOption.IGNORE_CASE))
        listOf("service","field","warranty").any { it in normalized } || phase in setOf("8","9") -> find(Regex("maintenance|freight|travel",RegexOption.IGNORE_CASE))
        else -> null
    }
}

internal fun travelDateValidationMessage(startDate: String?, endDate: String?): String? {
    if (startDate.isNullOrBlank()) return "Choose a travel start date."
    if (endDate.isNullOrBlank()) return "Choose a travel end date."
    val start = runCatching { LocalDate.parse(startDate) }.getOrNull()
        ?: return "Choose a valid travel start date."
    val end = runCatching { LocalDate.parse(endDate) }.getOrNull()
        ?: return "Choose a valid travel end date."
    if (end.isBefore(start)) return "Travel end date must be on or after the start date."
    return null
}

internal fun budgetAllocationValidationMessage(
    departmentId: String?, category: String?, allowedCategories: List<String>, allocatedAmount: String?,
    currency: String?, periodId: String?, startDate: String?, endDate: String?, financeLimit: String?
): String? {
    if (departmentId.isNullOrBlank()) return "Choose a department."
    if (category.isNullOrBlank() || category !in allowedCategories) return "Choose a Finance category from the authorized catalogue."
    val amount = allocatedAmount?.toDoubleOrNull()
    if (amount == null || !amount.isFinite() || amount <= 0.0) return "Enter an approved allocation greater than zero."
    if (currency?.matches(Regex("^[A-Z]{3}$")) != true) return "Enter a 3-letter currency code, such as INR."
    if (periodId.isNullOrBlank()) return "Enter the approved budget period reference."
    val start = runCatching { LocalDate.parse(startDate) }.getOrNull()
        ?: return "Choose a valid allocation start date."
    val end = runCatching { LocalDate.parse(endDate) }.getOrNull()
        ?: return "Choose a valid allocation end date."
    if (end.isBefore(start)) return "Allocation end date must be on or after the start date."
    if (!financeLimit.isNullOrBlank()) {
        val limit = financeLimit.toDoubleOrNull()
        if (limit == null || !limit.isFinite() || limit < 0.0) return "Finance limit must be zero or greater."
    }
    return null
}

/** Mobile participation in the same shared records as web; authoring remains on web. */
@Composable
fun SharedBusinessScreen(viewModel:MainViewModel,initialView:String="profile") {
    val uriHandler=LocalUriHandler.current
    val coroutineScope=rememberCoroutineScope()
    val snapshot by viewModel.businessSnapshot.collectAsState()
    val sharedSnapshot by viewModel.sharedSnapshot.collectAsState()
    val busy by viewModel.businessBusy.collectAsState()
    val failure by viewModel.businessError.collectAsState()
    val saved by viewModel.businessSaved.collectAsState()
    val openNewExpense = initialView == "new_expense"
    var section by remember(initialView) { mutableStateOf(if (openNewExpense) "budgets" else initialView) }
    var action by remember(initialView) { mutableStateOf<String?>(if (openNewExpense) "request_expense" else null) }
    var target by remember { mutableStateOf<JsonObject?>(null) }
    var form by remember(initialView) {
        mutableStateOf<Map<String, String>>(
            if (openNewExpense) mapOf(
                "requestType" to "travel",
                "purchaseKind" to "goods",
                "currency" to "INR",
                "requiredDate" to java.time.LocalDate.now(java.time.ZoneId.of("Asia/Kolkata")).toString()
            ) else emptyMap()
        )
    }
    var datePickerField by remember { mutableStateOf<String?>(null) }
    val datePickerState = rememberDatePickerState()
    var validationMessage by remember { mutableStateOf("") }
    var showLocationRequest by remember { mutableStateOf(false) }
    var handledLocationRequestSaved by remember { mutableIntStateOf(viewModel.expenseLocationRequestSaved.value) }
    var selectedChannel by remember { mutableStateOf("") }
    var travelPolicyDetailsExpanded by remember { mutableStateOf(false) }
    var sentGeneration by remember { mutableIntStateOf(-1) }
    var openingLetterId by remember { mutableStateOf<String?>(null) }
    var letterOpenError by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { viewModel.refreshBusinessWorkspace() }
    LaunchedEffect(saved) { if(sentGeneration>=0 && saved>sentGeneration) { action=null;form=emptyMap();sentGeneration=-1 } }
    val data=snapshot
    val actor=data?.get("membership") as? JsonObject
    val all=data?.rows("items").orEmpty()
    val me=actor?.text("membership_id").orEmpty()
    val role=actor?.text("role").orEmpty()
    val dept=actor?.text("department_code").orEmpty()
    val hrReviewer=role=="admin" || (role=="department_lead" && dept in setOf("HR","HROPS"))
    val hrDepartmentIds=data?.rows("departments").orEmpty().filter { it.text("code").uppercase() in setOf("HR","HROPS") }.map { it.text("id") }.filter(String::isNotBlank)
    var hrVerifiedLetters by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var hrCatalogueError by remember { mutableStateOf<String?>(null) }
    var hrCatalogueLoading by remember { mutableStateOf(false) }
    LaunchedEffect(section, hrReviewer, me, hrDepartmentIds.joinToString()) {
        if (section == "hr_admin" && hrReviewer) {
            hrCatalogueLoading = true
            hrCatalogueError = null
            try {
                hrVerifiedLetters = viewModel.verifiedHrLetterCatalogue(hrDepartmentIds)
            } catch (e: Exception) {
                hrCatalogueError = e.message ?: "Verified HR letter originals are unavailable."
                hrVerifiedLetters = emptyList()
            } finally { hrCatalogueLoading = false }
        }
    }
    val expenseCategories by viewModel.expenseCategories.collectAsState()
    val expenseServices by viewModel.expenseServices.collectAsState()
    val expenseSuppliers by viewModel.expenseSuppliers.collectAsState()
    val expenseLocations by viewModel.expenseLocations.collectAsState()
    val locationRequestBusy by viewModel.expenseLocationRequestBusy.collectAsState()
    val locationRequestSaved by viewModel.expenseLocationRequestSaved.collectAsState()
    val locationRequestError by viewModel.expenseLocationRequestError.collectAsState()
    val expenseCatalogueBusy by viewModel.expenseCatalogueBusy.collectAsState()
    val expenseCatalogueLoaded by viewModel.expenseCatalogueLoaded.collectAsState()
    val expenseCatalogueError by viewModel.expenseCatalogueError.collectAsState()
    val travelPolicy by viewModel.travelPolicy.collectAsState()
    val travelPolicyError by viewModel.travelPolicyError.collectAsState()
    val travelPolicySaving by viewModel.travelPolicySaving.collectAsState()
    val travelPolicySaveMessage by viewModel.travelPolicySaveMessage.collectAsState()
    val expenseOrganizationId=actor?.text("organization_id")?.ifBlank{actor.text("organizationId")}.orEmpty()
    val expenseDepartmentId=data?.rows("departments")?.firstOrNull { it.text("code")==dept }?.text("id").orEmpty()
    val expenseDepartmentScope=form["departmentId"]?.takeIf{it.isNotBlank()}?:expenseDepartmentId
    var policyScopeDepartment by remember(role,expenseDepartmentId) { mutableStateOf(if(role=="admin")"" else expenseDepartmentId) }
    var policyEditorVisible by remember { mutableStateOf(false) }
    val expenseProjects=data?.rows("projects").orEmpty()
    LaunchedEffect(section,expenseDepartmentScope,expenseOrganizationId,policyScopeDepartment) { if(section=="budgets") { viewModel.refreshExpenseCategories(expenseDepartmentScope,expenseOrganizationId);viewModel.refreshTravelPolicy(policyScopeDepartment.ifBlank{null},force=true) } }
    LaunchedEffect(locationRequestSaved) { if(locationRequestSaved>handledLocationRequestSaved){handledLocationRequestSaved=locationRequestSaved;showLocationRequest=false;form=form-"newLocationName"-"newLocationCity"-"newLocationReason";viewModel.refreshExpenseCategories(expenseDepartmentScope,expenseOrganizationId,true)} }
    val finance=role=="admin" || (role=="department_lead" && dept=="FIN")
    val service=role=="admin" || dept=="SIT"
    fun kind(k:String)=all.filter { it.text("kind")==k }
    fun payload(item:JsonObject)=item["data"] as? JsonObject ?: buildJsonObject {}
    fun open(a:String,item:JsonObject?=null,defaults:Map<String,String> = emptyMap()) { action=a;target=item;form=defaults;validationMessage="";viewModel.clearBusinessError() }
    fun submit(extra:JsonObject?=null) {
        val a=action?:return
        val entry=target
        if(a=="allocate") {
            if(!finance) { validationMessage="Only Admin or Finance may create a shared allocation.";return }
            val allocationError=budgetAllocationValidationMessage(
                form["departmentId"],form["category"],expenseCategories,form["allocatedAmount"],form["currency"],
                form["periodId"],form["startDate"],form["endDate"],form["financeLimit"]
            )
            if(allocationError!=null) { validationMessage=allocationError;return }
        }
        if(a in setOf("approve_allocation","reject_allocation") && !finance) { validationMessage="Only Admin or Finance may review an allocation.";return }
        if(a=="reject_allocation" && form["reason"].orEmpty().trim().length<3) { validationMessage="Enter a reason of at least 3 characters.";return }
        if(a=="assign_employee_asset" && (form["memberId"].isNullOrBlank() || form["assetRecordId"].isNullOrBlank())) { validationMessage="Choose an employee and a canonical serial Asset record.";return }
        if(a=="issue_employee_letter") {
            if(form["memberId"].isNullOrBlank() || form["letterFileId"].isNullOrBlank() || form["letterType"].isNullOrBlank() || form["referenceNumber"].orEmpty().trim().length<3 || form["effectiveDate"].isNullOrBlank()) {
                validationMessage="Choose the employee and verified HR original, then enter letter type, reference, and effective date.";return
            }
            if(hrVerifiedLetters.none { it.text("fileId")==form["letterFileId"] }) { validationMessage="Select an original from the verified HR catalogue.";return }
        }
        if(a=="return_employee_asset" && form["reason"].orEmpty().trim().length<3) { validationMessage="Enter a short return condition or handover note.";return }
        if(a=="request_expense") {
            val requestType=form["requestType"]?:"travel"
            val travelDateError=if(requestType=="travel") travelDateValidationMessage(form["travelStartDate"],form["travelEndDate"]) else null
            val missing=when {
                form["category"].isNullOrBlank()->"Choose a Finance category."
                (form["amount"]?.toDoubleOrNull()?:0.0)<=0.0->"Enter an estimate greater than zero."
                form["currency"].isNullOrBlank()->"Choose a currency."
                form["requiredDate"].isNullOrBlank()->"Enter a required date."
                travelDateError!=null->travelDateError
                requestType=="travel"&&form["locationId"].isNullOrBlank()->"Choose an approved shared travel location."
                requestType=="reimbursement"&&form["merchant"].isNullOrBlank()->"Enter the merchant or payee."
                requestType=="purchase"&&form["itemOrService"].isNullOrBlank()->"Enter the good or service needed."
                requestType=="purchase"&&form["purchaseKind"]=="service"&&(form["serviceSeats"]?.toDoubleOrNull()?:0.0)<=0.0->"Enter positive service seats or units."
                else->""
            }
            if(missing.isNotBlank()){validationMessage=missing;return}
        }
        val submittedForm=if(a=="request_expense") {
            val type=form["requestType"]?:"travel"
            val detail=when(type) {
                "travel"->listOfNotNull(form["destination"]?.takeIf{it.isNotBlank()},listOfNotNull(form["travelStartDate"],form["travelEndDate"]).takeIf{it.isNotEmpty()}?.joinToString(" to "),form["quotationReference"]?.takeIf{it.isNotBlank()}).joinToString(" · ")
                "reimbursement"->listOfNotNull(form["merchant"]?.takeIf{it.isNotBlank()},form["expenseDate"]?.takeIf{it.isNotBlank()},form["receiptReference"]?.takeIf{it.isNotBlank()}).joinToString(" · ")
                else->if(form["purchaseKind"]=="service") listOfNotNull(form["itemOrService"]?.takeIf{it.isNotBlank()},form["serviceAction"]?.takeIf{it.isNotBlank()}?.let{"Request: $it"},form["billingCycle"]?.takeIf{it.isNotBlank()}?.let{"Billing: $it"},form["serviceSeats"]?.takeIf{it.isNotBlank()}?.let{"Seats / units: $it"},form["serviceOwner"]?.takeIf{it.isNotBlank()}?.let{"Owner: $it"},form["existingServiceReference"]?.takeIf{it.isNotBlank()}?.let{"Reference: $it"},form["supplierName"]?.takeIf{it.isNotBlank()},form["quotationReference"]?.takeIf{it.isNotBlank()}).joinToString(" · ") else listOfNotNull(form["itemOrService"]?.takeIf{it.isNotBlank()},form["quantity"]?.takeIf{it.isNotBlank()}?.let{"Qty $it"},form["supplierName"]?.takeIf{it.isNotBlank()},form["quotationReference"]?.takeIf{it.isNotBlank()}).joinToString(" · ")
            }
            form+mapOf("requestType" to type,"purchaseKind" to (form["purchaseKind"]?:"goods"),"departmentId" to (form["departmentId"]?:expenseDepartmentId),"purpose" to detail)
        } else form
        val details=extra?:when(a) {
            "issue_employee_letter" -> {
                val source=hrVerifiedLetters.firstOrNull { it.text("fileId")==form["letterFileId"] }
                buildJsonObject {
                    put("memberId",form["memberId"].orEmpty());put("documentId",source?.text("documentId").orEmpty());put("fileId",source?.text("fileId").orEmpty())
                    put("letterType",form["letterType"].orEmpty());put("referenceNumber",form["referenceNumber"].orEmpty().trim());put("effectiveDate",form["effectiveDate"].orEmpty())
                }
            }
            "assign_employee_asset" -> buildJsonObject { put("memberId",form["memberId"].orEmpty());put("assetRecordId",form["assetRecordId"].orEmpty()) }
            else -> buildJsonObject { submittedForm.forEach { (k,v)->
            when(k) { "tags"->put(k,JsonArray(v.split(",").map{it.trim()}.filter{it.isNotBlank()}.map{JsonPrimitive(it)}));"amount","actualAmount","allocatedAmount"->put(k,v.toDoubleOrNull()?:-1.0);"financeLimit"->if(v.isBlank())put(k,JsonNull) else put(k,v.toDoubleOrNull()?:-1.0);"isInternalNote","acknowledged"->put(k,v=="true");else->put(k,v) }
            } }
        }
        sentGeneration=saved
        viewModel.mutateBusinessWorkspace(a,buildJsonObject { if(entry!=null){ put("id",entry.text("id"));put("version",entry["version"]!!) };put("data",details) })
    }
    Column(Modifier.fillMaxSize().padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
            Column { Text("${actor?.text("display_name").orEmpty()}",style=MaterialTheme.typography.titleMedium);Text("Company workspace",style=MaterialTheme.typography.labelSmall) }
            TextButton(onClick={viewModel.refreshBusinessWorkspace();if(section=="budgets")viewModel.refreshExpenseCategories(expenseDepartmentScope,expenseOrganizationId,force=true)},enabled=!busy){Text("Refresh")}
        }
        val sections=(if(hrReviewer)listOf("hr_admin")else emptyList())+listOf("budgets","tickets","profile","communication")
        LazyRow(horizontalArrangement=Arrangement.spacedBy(6.dp)){items(sections){id->FilterChip(selected=section==id,onClick={section=id},label={Text(when(id){"budgets"->"Expenses";"tickets"->"Tickets";"profile"->"My Profile";"hr_admin"->"HR & People";else->"Team Chat"})})}}
        if(busy)LinearProgressIndicator(Modifier.fillMaxWidth())
        failure?.let { Text(it,color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodySmall) }
        if(data==null) { Text("Shared data is unavailable until the business-workspace migration is deployed. No sample data is substituted.");return@Column }
        if(role=="viewer"&&section!="tickets"){Text("Access restricted to internal employees.");return@Column}
        if(section=="budgets"&&role!="viewer")Button(onClick={open("request_expense",defaults=mapOf("requestType" to "travel","purchaseKind" to "goods","currency" to "INR","requiredDate" to java.time.LocalDate.now(java.time.ZoneId.of("Asia/Kolkata")).toString()))},enabled=!busy){Text("New expense request")}
        if(section=="budgets"&&finance)OutlinedButton(onClick={open("allocate",defaults=mapOf("currency" to "INR","startDate" to LocalDate.now(java.time.ZoneId.of("Asia/Kolkata")).toString()))},enabled=!busy){Text("Add allocation")}
        if(section=="tickets")Button(onClick={open("ticket_create",defaults=mapOf("priority" to "Normal","category" to "General"))},enabled=!busy){Text("New ticket")}
        LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp),modifier=Modifier.weight(1f)) {
            if(section=="tickets"&&kind("ticket").isEmpty()) item {
                Text(
                    "No shared tickets were returned in this access scope yet. A new ticket will be saved to the shared workspace.",
                    style=MaterialTheme.typography.bodySmall,
                    color=MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if(section=="profile") {
                val profile=kind("profile").firstOrNull { it.text("ownerId")==me }?.let(::payload)
                val ownChange=kind("employee_profile_change").firstOrNull { it.text("ownerId")==me && payload(it).text("status") in setOf("Pending_HR_Review","Pending_Employee_Acknowledgement") }
                item {
                    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                        Text(actor?.text("display_name").orEmpty(),style=MaterialTheme.typography.titleSmall)
                        listOf("employeeCode","email","phone","roleTitle","joiningDate","facilityLocation","emergencyContact").forEach { key ->
                            Text("${key.replace(Regex("([A-Z])")," $1").replaceFirstChar { it.uppercase() }}: ${profile?.text(key)?.ifBlank { "Not recorded" }?:"Not recorded"}",style=MaterialTheme.typography.bodySmall)
                        }
                        if(ownChange==null) OutlinedButton(onClick={open("request_profile_change",defaults=mapOf("memberId" to me)+listOf("employeeCode","email","phone","roleTitle","joiningDate","facilityLocation","emergencyContact").associateWith{profile?.text(it).orEmpty()})},enabled=!busy){Text("Request profile change")}
                    } }
                }
                if(ownChange!=null) item {
                    val change=payload(ownChange)
                    val proposed=change["proposedProfile"] as? JsonObject ?: buildJsonObject {}
                    val baseline=change["baseProfile"] as? JsonObject ?: buildJsonObject {}
                    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(5.dp)) {
                        Text("Profile change · ${change.text("status").replace('_',' ')}",style=MaterialTheme.typography.titleSmall)
                        Text(change.text("reason"),style=MaterialTheme.typography.bodySmall)
                        proposed.keys.filter { proposed[it] != baseline[it] }.forEach { key -> Text("${key.replace(Regex("([A-Z])")," $1")}: ${baseline[key]?.jsonPrimitive?.contentOrNull?:"—"} → ${proposed[key]?.jsonPrimitive?.contentOrNull?:"—"}",style=MaterialTheme.typography.bodySmall) }
                        if(change.text("status")=="Pending_Employee_Acknowledgement") Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                            Button(onClick={viewModel.mutateBusinessWorkspace("acknowledge_profile_change",buildJsonObject{put("id",ownChange.text("id"));put("version",ownChange["version"]!!);put("data",buildJsonObject{})})},enabled=!busy){Text("Accept HR update")}
                            OutlinedButton(onClick={open("reject_profile_change",ownChange)},enabled=!busy){Text("Request correction")}
                        } else Text("Waiting for HR/Admin review. Your current profile stays active until approval.",style=MaterialTheme.typography.labelSmall)
                    } }
                }
                val pendingHrChanges=if(hrReviewer)kind("employee_profile_change").filter { payload(it).text("status")=="Pending_HR_Review" && it.text("ownerId")!=me } else emptyList()
                if(pendingHrChanges.isNotEmpty()) item { Text("Employee changes awaiting HR review",style=MaterialTheme.typography.titleSmall) }
                items(pendingHrChanges,key={it.text("id")}) { changeItem ->
                    val change=payload(changeItem)
                    val proposed=change["proposedProfile"] as? JsonObject ?: buildJsonObject {}
                    val baseline=change["baseProfile"] as? JsonObject ?: buildJsonObject {}
                    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(5.dp)) {
                        Text("Employee profile update",style=MaterialTheme.typography.titleSmall)
                        Text("${change.text("reason")} · baseline v${change.text("baseVersion")}",style=MaterialTheme.typography.bodySmall)
                        proposed.keys.filter { proposed[it] != baseline[it] }.forEach { key -> Text("${key.replace(Regex("([A-Z])")," $1")}: ${baseline[key]?.jsonPrimitive?.contentOrNull?:"—"} → ${proposed[key]?.jsonPrimitive?.contentOrNull?:"—"}",style=MaterialTheme.typography.bodySmall) }
                        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                            Button(onClick={viewModel.mutateBusinessWorkspace("approve_profile_change",buildJsonObject{put("id",changeItem.text("id"));put("version",changeItem["version"]!!);put("data",buildJsonObject{})})},enabled=!busy){Text("Approve")}
                            OutlinedButton(onClick={open("reject_profile_change",changeItem)},enabled=!busy){Text("Reject")}
                        }
                    } }
                }
                val ownLetters=kind("employee_letter").filter { it.text("ownerId")==me }
                if(ownLetters.isNotEmpty()) item { Text("My HR letters",style=MaterialTheme.typography.titleSmall) }
                items(ownLetters,key={it.text("id")}) { letter ->
                    val info=payload(letter)
                    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(5.dp)) {
                        Text(info.text("title").ifBlank{info.text("letterType").ifBlank{"Company letter"}},style=MaterialTheme.typography.titleSmall)
                        Text("${info.text("referenceNumber")} · Effective ${info.text("effectiveDate")} · ${info.text("status").replace('_',' ')}",style=MaterialTheme.typography.bodySmall)
                        if(info.text("status")=="Correction_Requested") Text("Correction requested: ${info.text("correctionReason")}",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.error)
                        if(info.text("status") in setOf("Pending_Employee_Acknowledgement","Issued")) {
                            OutlinedButton(enabled=!busy&&openingLetterId!=letter.text("id"),onClick={
                                letterOpenError=null
                                openingLetterId=letter.text("id")
                                coroutineScope.launch {
                                    try { uriHandler.openUri(viewModel.employeeLetterDownloadUrl(letter.text("id"))) }
                                    catch(e:Exception) { letterOpenError=e.message?:"The verified original could not be opened." }
                                    finally { openingLetterId=null }
                                }
                            }) { Text(if(openingLetterId==letter.text("id"))"Opening original…" else "Open verified original") }
                            letterOpenError?.let { Text(it,color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodySmall) }
                        }
                        if(info.text("status")=="Pending_Employee_Acknowledgement") Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                            Button(onClick={viewModel.mutateBusinessWorkspace("acknowledge_employee_letter",buildJsonObject{put("id",letter.text("id"));put("version",letter["version"]!!);put("data",buildJsonObject{put("acknowledged",true)})})},enabled=!busy){Text("Accept letter")}
                            OutlinedButton(onClick={open("reject_employee_letter",letter)},enabled=!busy){Text("Request correction")}
                        }
                    } }
                }
                val ownAssets=kind("employee_asset").filter { it.text("ownerId")==me }
                item { Text("My assigned equipment",style=MaterialTheme.typography.titleSmall) }
                if(ownAssets.isEmpty()) item {
                    Text("No serialized company equipment is assigned to your profile yet.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
                items(ownAssets,key={it.text("id")}) { asset ->
                    val info=payload(asset)
                    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(5.dp)) {
                        Text(info.text("assetName").ifBlank { info.text("assetTag").ifBlank { "Company equipment" } },style=MaterialTheme.typography.titleSmall)
                        Text("${info.text("assetTag")} · SN ${info.text("serialNumber")}",style=MaterialTheme.typography.bodySmall)
                        Text("${info.text("category").ifBlank { "Equipment" }} · ${info.text("custodyStatus").ifBlank { "Assigned" }}${if(info.text("acknowledged")=="true") " · Acknowledged" else " · Awaiting acknowledgement"}",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                        if(info.text("custodyStatus")=="Assigned" && info.text("acknowledged")!="true") {
                            Button(onClick={viewModel.mutateBusinessWorkspace("acknowledge_employee_asset",buildJsonObject{put("id",asset.text("id"));put("version",asset["version"]!!);put("data",buildJsonObject{put("acknowledged",true)})})},enabled=!busy){Text("Acknowledge receipt")}
                        }
                    } }
                }
                items(kind("assignment").filter { it.text("ownerId")==me }) { assignment->
                    val a=payload(assignment);val template=all.firstOrNull { it.text("id")==a.text("templateId") }?.let(::payload)
                    Card(Modifier.fillMaxWidth()){Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){Text(template?.text("title")?:"Assignment");Text("${a.text("status")} · due ${a.text("dueDate")}",style=MaterialTheme.typography.bodySmall);if(a.text("status") !in listOf("Completed","Submitted"))OutlinedButton(onClick={open("submit_assignment",assignment)},enabled=!busy){Text("Open")}}}
                }
            }
            if(section=="hr_admin"&&hrReviewer) {
                val employees=data.rows("members").filter { it.text("id").isNotBlank() }
                val hrAssets=(sharedSnapshot?.get("records") as? JsonArray).orEmpty().mapNotNull { it as? JsonObject }
                    .filter { it.text("record_type").equals("Asset",ignoreCase=true) && it.text("archived_at").isBlank() }
                    .filter { ((it["fields"] as? JsonObject)?.text("serialNumber").orEmpty()).isNotBlank() }
                item {
                    Text("Letters and serial assets",style=MaterialTheme.typography.titleSmall)
                    Text(when {
                        hrCatalogueLoading -> "Checking HR letter originals and shared asset records…"
                        hrCatalogueError != null -> "Verified HR letter catalogue unavailable: $hrCatalogueError"
                        else -> "${hrVerifiedLetters.size} verified HR letter originals · ${hrAssets.size} serial Asset records available."
                    },style=MaterialTheme.typography.bodySmall,color=if(hrCatalogueError!=null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                    if(hrVerifiedLetters.isEmpty()&&!hrCatalogueLoading&&hrCatalogueError==null) Text("Upload and verify an HR Letter in controlled Documents before issuing a letter.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
                item {
                    Text("Employee register",style=MaterialTheme.typography.titleSmall)
                    Text("HR/Admin changes stay pending until the employee reviews and acknowledges them. Employee edits continue to require HR/Admin review.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if(employees.isEmpty()) item { Text("No employees are available in the authorized shared register.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant) }
                items(employees,key={it.text("id")}) { member ->
                    val memberId=member.text("id")
                    val memberName=member.text("name").ifBlank { member.text("display_name").ifBlank { member.text("full_name").ifBlank { "Employee" } } }
                    val memberDepartmentId=member.text("departmentId").ifBlank { member.text("department_id") }
                    val department=data.rows("departments").firstOrNull { it.text("id")==memberDepartmentId }
                    val memberDepartment=member.text("departmentCode").ifBlank { member.text("department_code") }.ifBlank { department?.let { it.text("name").ifBlank { it.text("code") } }.orEmpty() }.ifBlank { "Department not recorded" }
                    val profileItem=kind("profile").firstOrNull { it.text("ownerId")==memberId }
                    val profile=profileItem?.let(::payload)
                    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(5.dp)) {
                        Text(memberName,style=MaterialTheme.typography.titleSmall)
                        Text("$memberDepartment · ${member.text("role").replace('_',' ').ifBlank { "Member" }}",style=MaterialTheme.typography.bodySmall)
                        Text("${profile?.text("employeeCode").orEmpty().ifBlank { "Employee code not recorded" }} · ${profile?.text("roleTitle").orEmpty().ifBlank { "Role title not recorded" }}",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedButton(onClick={open("save_profile",profileItem,defaults=mapOf("memberId" to memberId)+listOf("employeeCode","email","phone","roleTitle","joiningDate","facilityLocation","emergencyContact").associateWith{profile?.text(it).orEmpty()}+mapOf("reason" to ""))},enabled=!busy){Text(if(profileItem==null)"Create employee profile" else "Propose profile edit")}
                        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick={open("issue_employee_letter",defaults=mapOf("memberId" to memberId,"effectiveDate" to LocalDate.now(java.time.ZoneId.of("Asia/Kolkata")).toString()))},enabled=!busy&&!hrCatalogueLoading&&hrVerifiedLetters.isNotEmpty()){Text("Issue letter")}
                            OutlinedButton(onClick={open("assign_employee_asset",defaults=mapOf("memberId" to memberId))},enabled=!busy&&hrAssets.isNotEmpty()){Text("Assign asset")}
                        }
                    } }
                }
                val pendingLetters=kind("employee_letter").filter { payload(it).text("status") in setOf("Review","Correction_Requested") }
                item { Text("Letters awaiting HR review · ${pendingLetters.size}",style=MaterialTheme.typography.titleSmall) }
                items(pendingLetters,key={it.text("id")}) { letter ->
                    val info=payload(letter)
                    val owner=employees.firstOrNull { it.text("id")==letter.text("ownerId") }?.let { it.text("display_name").ifBlank{it.text("name")} }.orEmpty()
                    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(5.dp)) {
                        Text("${owner.ifBlank{"Employee"}} · ${info.text("letterType")}",style=MaterialTheme.typography.titleSmall)
                        Text("${info.text("referenceNumber")} · Effective ${info.text("effectiveDate")} · ${info.text("status").replace('_',' ')}",style=MaterialTheme.typography.bodySmall)
                        if(info.text("status")=="Correction_Requested") Text("Correction: ${info.text("correctionReason")}",style=MaterialTheme.typography.bodySmall)
                        if(letter.text("ownerId")!=me&&info.text("status")=="Review") Button(onClick={viewModel.mutateBusinessWorkspace("approve_employee_letter",buildJsonObject{put("id",letter.text("id"));put("version",letter["version"]!!);put("data",buildJsonObject{})})},enabled=!busy){Text("Approve for employee review")}
                        if(info.text("status")=="Correction_Requested") OutlinedButton(onClick={open("issue_employee_letter",defaults=mapOf("memberId" to letter.text("ownerId"),"effectiveDate" to LocalDate.now(java.time.ZoneId.of("Asia/Kolkata")).toString()))},enabled=!busy&&!hrCatalogueLoading&&hrVerifiedLetters.isNotEmpty()){Text("Issue revised letter")}
                    } }
                }
                val custody=kind("employee_asset").filter { payload(it).text("custodyStatus")=="Assigned" }
                item { Text("Current employee asset custody · ${custody.size}",style=MaterialTheme.typography.titleSmall) }
                items(custody,key={it.text("id")}) { assigned ->
                    val info=payload(assigned)
                    val owner=employees.firstOrNull { it.text("id")==assigned.text("ownerId") }?.let { it.text("display_name").ifBlank{it.text("name")} }.orEmpty()
                    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                        Text("${owner.ifBlank{"Employee"}} · ${info.text("assetTag")}",style=MaterialTheme.typography.titleSmall)
                        Text("${info.text("assetName")} · SN ${info.text("serialNumber")} · ${if(info["acknowledged"]?.jsonPrimitive?.booleanOrNull==true)"Acknowledged" else "Awaiting acknowledgment"}",style=MaterialTheme.typography.bodySmall)
                        OutlinedButton(onClick={open("return_employee_asset",assigned,defaults=mapOf("reason" to ""))},enabled=!busy){Text("Record return")}
                    } }
                }
                val pendingChanges=kind("employee_profile_change").filter { payload(it).text("status")=="Pending_HR_Review"&&it.text("ownerId")!=me }
                item { Text("Employee changes awaiting review · ${pendingChanges.size}",style=MaterialTheme.typography.titleSmall) }
                items(pendingChanges,key={it.text("id")}) { changeItem ->
                    val change=payload(changeItem)
                    val proposed=change["proposedProfile"] as? JsonObject ?: buildJsonObject {}
                    val baseline=change["baseProfile"] as? JsonObject ?: buildJsonObject {}
                    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(5.dp)) {
                        Text(employees.firstOrNull{it.text("id")==changeItem.text("ownerId")}?.let{it.text("name").ifBlank{it.text("display_name")}}.orEmpty().ifBlank{"Employee profile update"},style=MaterialTheme.typography.titleSmall)
                        Text(change.text("reason"),style=MaterialTheme.typography.bodySmall)
                        proposed.keys.filter { proposed[it]!=baseline[it] }.forEach { key->Text("${key.replace(Regex("([A-Z])")," $1")}: ${baseline[key]?.jsonPrimitive?.contentOrNull?:"—"} → ${proposed[key]?.jsonPrimitive?.contentOrNull?:"—"}",style=MaterialTheme.typography.bodySmall) }
                        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                            Button(onClick={viewModel.mutateBusinessWorkspace("approve_profile_change",buildJsonObject{put("id",changeItem.text("id"));put("version",changeItem["version"]!!);put("data",buildJsonObject{})})},enabled=!busy){Text("Approve")}
                            OutlinedButton(onClick={open("reject_profile_change",changeItem)},enabled=!busy){Text("Reject")}
                        }
                    } }
                }
            }
            if(section=="budgets") {
                if(travelPolicy?.get("canView")?.jsonPrimitive?.booleanOrNull==true) {
                item {
                    val company=travelPolicy?.get("company") as? JsonObject
                    val departmentPolicy=travelPolicy?.get("department") as? JsonObject
                    val policy=departmentPolicy ?: company
                    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                        Text("Travel policy",style=MaterialTheme.typography.titleSmall)
                        when {
                            travelPolicyError!=null -> Text("Policy unavailable: $travelPolicyError",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.error)
                            policy==null -> Text("Loading policy from the authorized shared workspace…",style=MaterialTheme.typography.bodySmall)
                            else -> {
                                Text("${policy.text("status")} · ${policy.text("currency")} · ${if(departmentPolicy!=null) "Department limits" else "Company policy"}",style=MaterialTheme.typography.bodySmall)
                                val bands=policy.rows("bands")
                                Text("${bands.size} role bands · INR limits are illustrative until Finance confirms and publishes.",style=MaterialTheme.typography.bodySmall)
                                TextButton(onClick={travelPolicyDetailsExpanded=!travelPolicyDetailsExpanded},contentPadding=PaddingValues(horizontal=0.dp,vertical=0.dp)) {
                                    Text(if(travelPolicyDetailsExpanded) "Hide rate details" else "View rate details")
                                }
                                if(travelPolicyDetailsExpanded) bands.forEach { band->Text("${band.text("label")}: hotel ₹${band.text("hotelMetro")} / night (Metro), ₹${band.text("hotelOther")} / night (Other); meals ₹${band.text("mealsPerDay")} / day; local ₹${band.text("localPerDay")} / day",style=MaterialTheme.typography.bodySmall) }
                                if(policy.text("status")!="Active") Text("Draft only; not used to approve or cap requests.",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.error)
                                Text("Server check: one unsettled advance per requester when an Active policy enables it. Amount caps and exception routing are not yet enforced.",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } }
                }
                item {
                    Column(verticalArrangement=Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick={policyEditorVisible=!policyEditorVisible},enabled=!travelPolicySaving) {
                            Text(if(policyEditorVisible)"Close travel policy settings" else "Configure travel policy")
                        }
                        if(policyEditorVisible) {
                            val scopeMatches=travelPolicy?.text("scopeDepartment").orEmpty()==policyScopeDepartment
                            if(!scopeMatches) Text("Loading the selected policy scope…",style=MaterialTheme.typography.bodySmall)
                            else SharedTravelPolicyEditor(
                                policyData=travelPolicy!!,
                                departments=data.rows("departments"),
                                selectedDepartmentId=policyScopeDepartment,
                                onDepartmentSelected={policyScopeDepartment=it},
                                isAdmin=role=="admin",
                                saving=travelPolicySaving,
                                saveMessage=travelPolicySaveMessage,
                                onSave={departmentId,value,version,publish->viewModel.saveTravelPolicy(departmentId,value,version,publish)}
                            )
                        }
                    }
                }
            } else if(section=="budgets" && travelPolicyError==null && travelPolicy!=null) {
                item {
                    Text(
                        "Travel policy details are managed by Admin and Operations.",
                        style=MaterialTheme.typography.bodySmall,
                        color=MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                }
                if(kind("allocation").isEmpty())item { Text("No shared budget allocations are configured for this view.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant) }
                items(kind("allocation")) { entry ->
                    val allocation = payload(entry)
                    val status = allocation.text("status").ifBlank { "Approved" }
                    val approved = status == "Approved"
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("${allocation.text("category")} · ${allocation.text("currency")}")
                            Text("${allocation.text("periodId")} · $status", style = MaterialTheme.typography.bodySmall)
                            Text(
                                if (approved) "Allocated ${allocation.text("allocatedAmount")} · committed ${allocation.text("committedAmount")} · spent ${allocation.text("spentAmount")}"
                                else "Amounts are excluded until an independent reviewer approves this allocation.",
                                style = MaterialTheme.typography.bodySmall
                            )
                            if (status == "Pending_Approval" && finance && entry.text("ownerId") != me) {
                                Row {
                                    TextButton(onClick = { open("approve_allocation", entry) }, enabled = !busy) { Text("Approve") }
                                    TextButton(onClick = { open("reject_allocation", entry) }, enabled = !busy) { Text("Reject") }
                                }
                            }
                        }
                    }
                }
                if(kind("expense").isEmpty())item { Text("No shared expense requests are available in this authorized view yet.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant) }
                items(kind("expense")){entry->val a=payload(entry);val status=a.text("status");Card(Modifier.fillMaxWidth()){Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){Text(a.text("purpose"));Text("${a.text("currency")} ${a.text("amount")} · $status",style=MaterialTheme.typography.bodySmall)
                    if(entry.text("ownerId")!=me&&((status=="Pending_Dept_Review"&&role in listOf("admin","department_lead"))||(status=="Pending_Finance"&&finance)||(status=="Pending_Admin"&&role=="admin")))Row { TextButton(onClick={open("approve_expense",entry)},enabled=!busy){Text("Review")};TextButton(onClick={open("reject_expense",entry)},enabled=!busy){Text("Reject")} }
                    if(status=="Approved"&&finance&&entry.text("ownerId")!=me)TextButton(onClick={open("post_expense",entry, mapOf("actualAmount" to a.text("amount")))},enabled=!busy){Text("Post actuals")}
                }}}
            }
            if(section=="tickets")items(kind("ticket")){entry->val a=payload(entry);Card(Modifier.fillMaxWidth()){Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){Text(a.text("subject"),style=MaterialTheme.typography.titleSmall);Text("${a.text("source")} · ${a.text("status")}");Text(a.text("description"));a.rows("replies").forEach { reply->Text("${reply.text("authorName")}: ${reply.text("message")}",style=MaterialTheme.typography.bodySmall) };Row { TextButton(onClick={open("ticket_reply",entry, mapOf("isInternalNote" to "false"))},enabled=!busy){Text("Reply")};if(service)TextButton(onClick={open("ticket_status",entry)},enabled=!busy){Text("Status")} }}}}
            if(section=="communication") {
                item { LazyRow { items(kind("channel")){c->FilterChip(selected=selectedChannel==c.text("id"),onClick={selectedChannel=c.text("id")},label={Text("#${payload(c).text("name")}")})} } }
                if(selectedChannel.isNotBlank())item { Button(onClick={open("send_message",defaults=mapOf("channelId" to selectedChannel,"type" to "update"))},enabled=!busy){Text("New message")} }
                items(kind("message").filter { payload(it).text("channelId")==selectedChannel }){entry->val a=payload(entry);Card(Modifier.fillMaxWidth()){Column(Modifier.padding(12.dp)){Text(a.text("senderName"),style=MaterialTheme.typography.labelSmall);Text(a.text("text"));val poll=a["poll"] as? JsonObject;poll?.let { p->
                    val closed=runCatching{java.time.Instant.parse(p.text("closesAt")).toEpochMilli()<=System.currentTimeMillis()}.getOrDefault(true)
                    Text(p.text("question"));(p["options"] as? JsonArray)?.forEachIndexed {index,opt->val vote=data.rows("votes").firstOrNull{it.text("message_id")==entry.text("id")&&it.text("choice")==index.toString()};OutlinedButton(enabled=!busy&&!closed,onClick={viewModel.mutateBusinessWorkspace("poll_vote",buildJsonObject {put("id",entry.text("id"));put("version",entry["version"]!!);put("data",buildJsonObject{put("choice",index)})})}){Text("${opt.jsonPrimitive.content} · ${vote?.text("total")?:"0"}${if(closed)" · Closed" else ""}")}}
                }}}}
            }
        }
    }
    if(action!=null)AlertDialog(onDismissRequest={if(!busy)action=null},title={Text(action!!.replace('_',' '),style=MaterialTheme.typography.titleMedium)},text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(5.dp)) {
        @Composable fun field(key:String,label:String,modifier:Modifier=Modifier,numeric:Boolean=false){OutlinedTextField(value=form[key].orEmpty(),onValueChange={value->form=form+(key to if(key=="currency")value.uppercase() else value)},label={Text(label,style=MaterialTheme.typography.bodySmall)},modifier=modifier.fillMaxWidth().heightIn(min=52.dp,max=58.dp),textStyle=MaterialTheme.typography.bodyMedium,singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=if(numeric)KeyboardType.Decimal else KeyboardType.Text),enabled=!busy)}
        @Composable fun dateField(key:String,label:String,modifier:Modifier=Modifier){
            OutlinedButton(onClick={datePickerField=key},enabled=!busy,modifier=modifier.fillMaxWidth().heightIn(min=44.dp)){
                Text(if(form[key].isNullOrBlank())"$label · Select" else "$label · ${form[key]}",maxLines=1,style=MaterialTheme.typography.labelMedium)
            }
        }
        @Composable fun select(key:String,label:String,options:List<Pair<String,String>>) {Text(label,style=MaterialTheme.typography.labelSmall);options.forEach{(id,name)->FilterChip(selected=form[key]==id,onClick={form=form+(key to id)},label={Text(name)})}}
        when(action) {
            "request_expense"->{
                val requestType=form["requestType"]?:"travel"
                val selectedProject=expenseProjects.firstOrNull{it.text("id")==form["projectId"]}
                val projectPhase=selectedProject?.let{it.text("companyPhase").ifBlank{it.text("phase")}}.orEmpty()
                val projectStage=selectedProject?.let{it.text("currentStage").ifBlank{it.text("stage")}}.orEmpty()
                Text("Project context (optional)",style=MaterialTheme.typography.labelSmall)
                SharedSearchPicker(
                    label = "project",
                    selectedLabel = expenseProjects.firstOrNull { it.text("id") == form["projectId"] }?.let { "${it.text("reference")} · ${it.text("title")}" } ?: "Department work · no project",
                    options = listOf("" to "Department work · no project") + expenseProjects
                        .filter { it.text("id").isNotBlank() && it.text("status").lowercase() !in setOf("archived", "cancelled", "canceled", "inactive", "retired") }
                        .map { it.text("id") to "${it.text("reference")} · ${it.text("title")} · ${it.text("status")}" }
                        .distinctBy { it.first },
                    enabled = !busy
                ) { id -> form = if (id.isBlank()) form - "projectId" else form + ("projectId" to id) }
                if(selectedProject!=null)Text("${selectedProject.text("reference")} · Phase: ${projectPhase.ifBlank{"Not recorded"}} · Stage: ${projectStage.ifBlank{"Not recorded"}}",style=MaterialTheme.typography.bodySmall)
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                    listOf("travel" to "Travel","reimbursement" to "Reimburse","purchase" to "Purchase").forEach { (id,label) ->
                        FilterChip(
                            selected=requestType==id,
                            onClick={if(!busy)form=form+("requestType" to id)+("category" to "")},
                            enabled=!busy,
                            label={Text(label,maxLines=1)},
                            modifier=Modifier.weight(1f)
                        )
                    }
                }
                Text("Finance category",style=MaterialTheme.typography.labelSmall)
                when {
                    expenseCatalogueBusy || !expenseCatalogueLoaded -> Text("Loading configured Finance categories…",style=MaterialTheme.typography.bodySmall)
                    expenseCatalogueError!=null -> Text("Finance categories could not be loaded: $expenseCatalogueError. Refresh and try again.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.error)
                    expenseCategories.isEmpty() -> Text("No Finance categories are configured for this department. Ask the department lead or Finance to configure the shared catalogue.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.error)
                    else -> SharedSearchPicker(
                        label = "Finance category",
                        selectedLabel = form["category"].orEmpty(),
                        options = expenseCategories.filter(String::isNotBlank).distinct().map { it to it },
                        enabled = !busy,
                        emptyText = "No Finance categories are configured for this department."
                    ) { category -> form = form + ("category" to category) }
                }
                val suggestion=suggestedFinanceCategory("$projectPhase $projectStage",requestType,expenseCategories)
                if(suggestion!=null&&form["category"]!=suggestion)TextButton(onClick={form=form+("category" to suggestion)}){Text("Use suggested category: $suggestion")}
                Text(if(requestType=="purchase")"Purchase requests go to Procurement first." else "Travel and reimbursements go to Finance.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                when(requestType){
                    "travel"->{
                        Text("Approved accounting location",style=MaterialTheme.typography.labelSmall)
                        SharedSearchPicker(
                            label="approved travel location",
                            selectedLabel=expenseLocations.firstOrNull{it.text("id")==form["locationId"]}?.let{"${it.text("reference")} · ${it.text("name")} · ${it.text("city")}"}?:"Search approved locations…",
                            options=expenseLocations.filter{it.text("active")!="false"}.map{it.text("id") to "${it.text("reference")} · ${it.text("name")} · ${it.text("city")} · ${if(it.text("cityTier")=="metro")"Metro" else "Other city"}"},
                            enabled=!busy&&!locationRequestBusy,
                            emptyText="No approved accounting locations are configured yet."
                        ){id->val location=expenseLocations.firstOrNull{it.text("id")==id};form=form+mapOf("locationId" to id,"destination" to (location?.let{"${it.text("name")} · ${it.text("city")}"}.orEmpty()))}
                        TextButton(enabled=!busy&&!locationRequestBusy,onClick={showLocationRequest=!showLocationRequest}){Text(if(showLocationRequest)"Cancel location request" else "Location not listed? Request it")}
                        if(showLocationRequest){
                            Text("Finance/Admin must approve a requested location before it can be selected.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                            field("newLocationName","Facility / location name")
                            field("newLocationCity","City")
                            field("newLocationReason","Why is it needed?")
                            Button(enabled=!busy&&!locationRequestBusy&&form["newLocationName"].orEmpty().trim().length>=2&&form["newLocationCity"].orEmpty().trim().length>=2&&form["newLocationReason"].orEmpty().trim().length>=3,onClick={viewModel.requestExpenseLocation(form["newLocationName"].orEmpty(),form["newLocationCity"].orEmpty(),form["newLocationReason"].orEmpty(),expenseDepartmentScope,expenseOrganizationId)}){Text(if(locationRequestBusy)"Sending…" else "Send for review")}
                            locationRequestError?.let{Text(it,color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodySmall)}
                        }
                        if(expenseLocations.isEmpty())Text("No approved locations yet; use the review request or ask Admin/Finance to add one.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.error)
                        Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){dateField("travelStartDate","Start",Modifier.weight(1f));dateField("travelEndDate","End",Modifier.weight(1f))}
                    }
                    "reimbursement"->{field("merchant","Merchant / payee");dateField("expenseDate","Expense date");field("receiptReference","Receipt link / reference (optional)")}
                    else->{Text("Purchase type",style=MaterialTheme.typography.labelSmall);Row{FilterChip(selected=(form["purchaseKind"]?:"goods")=="goods",onClick={form=form+("purchaseKind" to "goods")},label={Text("Goods / materials")});FilterChip(selected=form["purchaseKind"]=="service",onClick={form=form+("purchaseKind" to "service")+("itemOrService" to "")+("serviceAction" to "new")},label={Text("Service")})};if(form["purchaseKind"]=="service") {field("serviceSearch","Find a company service");val matches=expenseServices.filter{it.text("name").contains(form["serviceSearch"].orEmpty(),ignoreCase=true)};LazyRow(horizontalArrangement=Arrangement.spacedBy(6.dp)){items(matches){serviceOption->val name=serviceOption.text("name");FilterChip(selected=form["itemOrService"]==name,onClick={form=form+("itemOrService" to name)+("serviceCatalogueId" to serviceOption.text("id"))+("supplierId" to serviceOption.text("supplierId"))+("supplierName" to serviceOption.text("provider"))+ ("billingCycle" to serviceOption.text("cycle"))+ ("serviceOwner" to serviceOption.text("owner"))},label={Text(name)})}};field("itemOrService","Service needed (or enter another)");select("serviceAction","Request",listOf("new" to "New service","renew" to "Renew existing service","seats" to "Add seats / capacity"));select("billingCycle","Billing cycle",listOf("Monthly" to "Monthly","Annual" to "Annual","One-time" to "One-time"));field("serviceSeats","Seats / units");field("serviceOwner","Responsible owner");if(form["serviceAction"]!="new")field("existingServiceReference","Subscription / contract reference")} else {field("itemOrService","Goods / materials needed");field("quantity","Quantity")};
                        SharedSearchPicker(
                            label = "supplier",
                            selectedLabel = expenseSuppliers.firstOrNull { it.text("id") == form["supplierId"] }?.let { "${it.text("reference")} · ${it.text("title")}" } ?: if (form["supplierOther"] == "yes") form["supplierName"].orEmpty() else "Other / not listed (optional)",
                            options = listOf("__other__" to "Other / not listed (optional)") + expenseSuppliers
                                .filter { it.text("id").isNotBlank() && it.text("status").lowercase() !in setOf("archived", "obsolete", "rejected", "inactive", "cancelled", "canceled") }
                                .map { it.text("id") to "${it.text("reference")} · ${it.text("title")} · ${it.text("status")}" }
                                .distinctBy { it.first },
                            enabled = !busy,
                            emptyText = "No authorized supplier options are available."
                        ) { id ->
                            form = if (id == "__other__") form - "supplierId" + ("supplierOther" to "yes")
                            else form + ("supplierId" to id) + ("supplierOther" to "no") + ("supplierName" to (expenseSuppliers.firstOrNull { it.text("id") == id }?.text("title").orEmpty()))
                        }
                        if (form["supplierOther"] == "yes") field("supplierName", "Supplier name / reference")
                        field("quotationReference","Quote / invoice link or reference (optional)")}
                }
                Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                    field("amount","Estimate",Modifier.weight(1.25f))
                    field("currency","Currency",Modifier.weight(0.75f))
                }
                dateField("requiredDate","Required date")
                if(role=="admin") {
                    val departments=data?.rows("departments").orEmpty()
                    val selectedDepartmentId=form["departmentId"]?:expenseDepartmentId
                    val selectedDepartmentName=departments.firstOrNull{it.text("id")==selectedDepartmentId}?.text("name")?:"Select department"
                    SharedSearchPicker(
                        label = "department",
                        selectedLabel = selectedDepartmentName,
                        options = departments.filter { it.text("id").isNotBlank() }.map { it.text("id") to "${it.text("name")} · ${it.text("code")}" },
                        enabled = !busy,
                        emptyText = "No departments are authorized for this profile."
                    ) { id -> form = form + ("departmentId" to id) + ("category" to "") }
                } else Text("Department: ${actor?.text("department_name")?.ifBlank{dept}?:dept} · restricted to your authorized profile",style=MaterialTheme.typography.bodySmall)
            }
            "allocate"->{
                val departments=data?.rows("departments").orEmpty().filter{it.text("id").isNotBlank()&&it.text("code")!="CLIENT"}
                val selectedDepartment=form["departmentId"].orEmpty()
                Text("Create a shared budget allocation. It will appear in Web and Android after the server confirms the save.",style=MaterialTheme.typography.bodySmall)
                SharedSearchPicker(
                    label="department",
                    selectedLabel=departments.firstOrNull{it.text("id")==selectedDepartment}?.text("name")?:"Select department",
                    options=departments.map{it.text("id") to "${it.text("name")} · ${it.text("code")}"},
                    enabled=!busy,
                    emptyText="No active departments are available."
                ){id->form=form+mapOf("departmentId" to id,"category" to "")}
                if(selectedDepartment.isBlank()) Text("Choose a department before selecting its Finance category.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                else if(expenseCatalogueBusy) LinearProgressIndicator(Modifier.fillMaxWidth())
                else if(expenseCategories.isEmpty()) Text("No Finance categories are configured for this department.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.error)
                else SharedSearchPicker(
                    label="Finance category",
                    selectedLabel=form["category"]?:"Select Finance category",
                    options=expenseCategories.filter(String::isNotBlank).distinct().map{it to it},
                    enabled=!busy,
                    emptyText="No Finance categories are configured for this department."
                ){id->form=form+("category" to id)}
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                    field("allocatedAmount","Approved amount",Modifier.weight(1.2f),numeric=true)
                    field("currency","Currency",Modifier.weight(0.8f))
                }
                field("periodId","Approved period reference (for example FY2026-27)")
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                    dateField("startDate","Start date",Modifier.weight(1f))
                    dateField("endDate","End date",Modifier.weight(1f))
                }
                field("financeLimit","Finance approval limit (blank requires Admin)",numeric=true)
                SharedSearchPicker(
                    label="project",
                    selectedLabel=expenseProjects.firstOrNull{it.text("id")==form["projectId"]}?.let{"${it.text("reference")} · ${it.text("title")}"}?:"Department operating budget · no project",
                    options=listOf("" to "Department operating budget · no project")+expenseProjects.filter{it.text("id").isNotBlank()&&it.text("status").lowercase() !in setOf("archived","cancelled","canceled","inactive","retired")}.map{it.text("id") to "${it.text("reference")} · ${it.text("title")} · ${it.text("status")}"}.distinctBy{it.first},
                    enabled=!busy,
                    emptyText="No active projects are available."
                ){id->form=if(id.isBlank())form-"projectId" else form+("projectId" to id)}
            }
            "approve_allocation"->Text("This allocation will become available only after your independent approval is saved.",style=MaterialTheme.typography.bodySmall)
            "reject_allocation"->field("reason","Reason for rejection")
            "approve_expense"->{if(payload(target!!).text("status")=="Pending_Dept_Review")Text("Forward to Finance review")else select("allocationId","Exact allocation",kind("allocation").filter{(payload(it).text("status").isBlank()||payload(it).text("status")=="Approved")&&it.text("departmentId")==target!!.text("departmentId")&&it.text("projectId")==target!!.text("projectId")&&payload(it).text("category")==payload(target!!).text("category")&&payload(it).text("currency")==payload(target!!).text("currency")}.map{it.text("id") to "${payload(it).text("periodId")} · ${payload(it).text("category")}"})}
            "post_expense"->{field("actualAmount","Verified actual amount");field("postingRef","Finance posting reference");field("evidenceRef","Invoice / receipt evidence reference")}
            "reject_expense"->field("reason","Reason")
            "request_profile_change"->{
                Text("Your proposed changes go to HR/Admin for review. The current profile remains official until the reviewer approves.",style=MaterialTheme.typography.bodySmall)
                listOf("employeeCode","email","phone","roleTitle","joiningDate","facilityLocation","emergencyContact").forEach { key -> field(key,key.replace(Regex("([A-Z])")," $1").replaceFirstChar{it.uppercase()}) }
                field("reason","Reason for update")
            }
            "save_profile"->{
                Text("This HR/Admin proposal will not replace the current baseline until the employee acknowledges it.",style=MaterialTheme.typography.bodySmall)
                listOf("employeeCode","email","phone","roleTitle","joiningDate","facilityLocation","emergencyContact").forEach { key -> field(key,key.replace(Regex("([A-Z])")," $1").replaceFirstChar{it.uppercase()}) }
                field("reason","Reason for update")
            }
            "assign_employee_asset"->{
                val employees=data?.rows("members").orEmpty().filter{it.text("id").isNotBlank()}
                val employeeOptions=employees.map{member->member.text("id") to "${member.text("display_name").ifBlank{member.text("name").ifBlank{"Employee"}}} · ${member.text("department_code")}"}
                val assets=(sharedSnapshot?.get("records") as? JsonArray).orEmpty().mapNotNull{it as? JsonObject}
                    .filter{it.text("record_type").equals("Asset",true)&&it.text("archived_at").isBlank()}
                    .filter{((it["fields"] as? JsonObject)?.text("serialNumber").orEmpty()).isNotBlank()}
                Text("Assign an existing shared Asset record. The employee must acknowledge receipt; this does not issue stock.",style=MaterialTheme.typography.bodySmall)
                SharedSearchPicker("employee",employees.firstOrNull{it.text("id")==form["memberId"]}?.let{it.text("display_name").ifBlank{it.text("name")}}?:"Select employee",employeeOptions,enabled=!busy,emptyText="No employees in the authorized register."){id->form=form+("memberId" to id)}
                SharedSearchPicker("canonical serial asset",assets.firstOrNull{it.text("id")==form["assetRecordId"]}?.let{record->val f=record["fields"] as? JsonObject;"${f?.text("assetTag").orEmpty().ifBlank{record.text("reference")}} · ${record.text("title")} · SN ${f?.text("serialNumber").orEmpty()}"}?:"Select serial Asset record",assets.map{record->val f=record["fields"] as? JsonObject;record.text("id") to "${f?.text("assetTag").orEmpty().ifBlank{record.text("reference")}} · ${record.text("title")} · SN ${f?.text("serialNumber").orEmpty()}"},enabled=!busy,emptyText="Create an Asset record with a serial number first."){id->form=form+("assetRecordId" to id)}
            }
            "issue_employee_letter"->{
                val employees=data?.rows("members").orEmpty().filter{it.text("id").isNotBlank()}
                val employeeOptions=employees.map{member->member.text("id") to "${member.text("display_name").ifBlank{member.text("name").ifBlank{"Employee"}}} · ${member.text("department_code")}"}
                Text("Issue only from a verified HR controlled original. This creates a Review item; HR approval and employee acknowledgment are separate steps.",style=MaterialTheme.typography.bodySmall)
                SharedSearchPicker("employee",employees.firstOrNull{it.text("id")==form["memberId"]}?.let{it.text("display_name").ifBlank{it.text("name")}}?:"Select employee",employeeOptions,enabled=!busy,emptyText="No employees in the authorized register."){id->form=form+("memberId" to id)}
                SharedSearchPicker("verified HR original",hrVerifiedLetters.firstOrNull{it.text("fileId")==form["letterFileId"]}?.let{"${it.text("title")} · Rev ${it.text("revision")} · ${it.text("originalName")}"}?:"Select verified original",hrVerifiedLetters.map{it.text("fileId") to "${it.text("title")} · Rev ${it.text("revision")} · ${it.text("originalName")}"},enabled=!busy&&!hrCatalogueLoading,emptyText="No verified HR letter originals available."){id->form=form+("letterFileId" to id)}
                select("letterType","Letter type",listOf("Appointment","Confirmation","Promotion","Relieving","Experience","SOP_Notice","Bonus_Award").map{it to it.replace('_',' ')})
                field("referenceNumber","Official letter reference")
                dateField("effectiveDate","Effective date")
            }
            "reject_profile_change","reject_employee_letter"->field("reason","Reason / correction needed")
            "return_employee_asset"->field("reason","Return condition / handover note")
            "ticket_create"->{field("subject","Subject");field("description","Description");field("category","Category");select("priority","Priority",listOf("Critical","High","Normal","Low").map{it to it})}
            "ticket_reply"->{field("message","Reply");if(service||dept=="SAL")select("isInternalNote","Visibility",listOf("false" to "Requester reply","true" to "Internal note"))}
            "ticket_status"->{select("status","Status",listOf("Assigned","In Progress","Waiting","Resolved","Closed","Open").map{it to it});field("reason","Reason / reopen reason")}
            "send_message"->{field("text","Message");field("tags","Tags, separated by commas")}
            "submit_assignment"->{val assignment=payload(target!!);val template=all.firstOrNull{it.text("id")==assignment.text("templateId")}?.let(::payload);Text(template?.text("title").orEmpty());Text(template?.text("description").orEmpty());if(template?.text("isAnonymous")=="true")Text("Anonymous answers are stored separately; avoid identifying details. Completion is recorded separately.");if(template?.text("type")=="policy")select("acknowledged","Acknowledge this policy version",listOf("true" to "I acknowledge"))else template?.rows("questions")?.forEach {q->if(q.text("type")=="boolean")select(q.text("id"),q.text("label"),listOf("true" to "Yes","false" to "No"))else if(q.text("type")=="choice")select(q.text("id"),q.text("label"),(q["options"] as? JsonArray)?.map{it.jsonPrimitive.content to it.jsonPrimitive.content}.orEmpty())else field(q.text("id"),q.text("label"))}}
        }
        validationMessage.takeIf{it.isNotBlank()}?.let{Text(it,color=MaterialTheme.colorScheme.error)}
        failure?.let{Text(it,color=MaterialTheme.colorScheme.error)}
    }},confirmButton={TextButton(enabled=!busy&&(action!="request_expense"||expenseCategories.isNotEmpty()),onClick={
        if(action=="submit_assignment") {val template=all.firstOrNull{it.text("id")==payload(target!!).text("templateId")}?.let(::payload);submit(buildJsonObject {put("acknowledged",form["acknowledged"]=="true");put("answers",buildJsonObject{template?.rows("questions")?.forEach{q->val key=q.text("id");val value=form[key];if(value!=null)when(q.text("type")){"boolean"->put(key,value=="true");"number","rating"->put(key,value.toDoubleOrNull()?:-1.0);else->put(key,value)}}})})}else submit()
    }){Text(if(busy)"Saving…" else "Save")}},dismissButton={TextButton(enabled=!busy,onClick={action=null}){Text("Cancel")}})
    if(datePickerField!=null) DatePickerDialog(onDismissRequest={datePickerField=null},confirmButton={TextButton(onClick={datePickerState.selectedDateMillis?.let{millis->form=form+(datePickerField!! to java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneOffset.UTC).toLocalDate().toString())};datePickerField=null}){Text("Select")}},dismissButton={TextButton(onClick={datePickerField=null}){Text("Cancel")}}){DatePicker(state=datePickerState)}
}
