package com.example.myapplication.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.viewmodel.MainViewModel
import kotlinx.serialization.json.*

private fun JsonObject.text(key:String)=this[key]?.jsonPrimitive?.contentOrNull.orEmpty()
private fun JsonObject.rows(key:String)=(this[key] as? JsonArray)?.mapNotNull { it as? JsonObject }.orEmpty()

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

/** Mobile participation in the same shared records as web; authoring remains on web. */
@Composable
fun SharedBusinessScreen(viewModel:MainViewModel,initialView:String="profile") {
    val snapshot by viewModel.businessSnapshot.collectAsState()
    val busy by viewModel.businessBusy.collectAsState()
    val failure by viewModel.businessError.collectAsState()
    val saved by viewModel.businessSaved.collectAsState()
    var section by remember(initialView) { mutableStateOf(initialView) }
    var action by remember { mutableStateOf<String?>(null) }
    var target by remember { mutableStateOf<JsonObject?>(null) }
    var form by remember { mutableStateOf<Map<String,String>>(emptyMap()) }
    var validationMessage by remember { mutableStateOf("") }
    var selectedChannel by remember { mutableStateOf("") }
    var sentGeneration by remember { mutableIntStateOf(-1) }
    LaunchedEffect(Unit) { viewModel.refreshBusinessWorkspace() }
    LaunchedEffect(saved) { if(sentGeneration>=0 && saved>sentGeneration) { action=null;form=emptyMap();sentGeneration=-1 } }
    val data=snapshot
    val actor=data?.get("membership") as? JsonObject
    val all=data?.rows("items").orEmpty()
    val me=actor?.text("membership_id").orEmpty()
    val role=actor?.text("role").orEmpty()
    val dept=actor?.text("department_code").orEmpty()
    val expenseCategories by viewModel.expenseCategories.collectAsState()
    val expenseServices by viewModel.expenseServices.collectAsState()
    val expenseOrganizationId=actor?.text("organization_id")?.ifBlank{actor.text("organizationId")}.orEmpty()
    val expenseDepartmentId=data?.rows("departments")?.firstOrNull { it.text("code")==dept }?.text("id").orEmpty()
    val expenseDepartmentScope=form["departmentId"]?.takeIf{it.isNotBlank()}?:expenseDepartmentId
    val expenseProjects=data?.rows("projects").orEmpty()
    LaunchedEffect(section,expenseDepartmentScope,expenseOrganizationId) { if(section=="budgets") viewModel.refreshExpenseCategories(expenseDepartmentScope,expenseOrganizationId) }
    val finance=role=="admin" || (role=="department_lead" && dept=="FIN")
    val service=role=="admin" || dept=="SIT"
    fun kind(k:String)=all.filter { it.text("kind")==k }
    fun payload(item:JsonObject)=item["data"] as? JsonObject ?: buildJsonObject {}
    fun open(a:String,item:JsonObject?=null,defaults:Map<String,String> = emptyMap()) { action=a;target=item;form=defaults;validationMessage="";viewModel.clearBusinessError() }
    fun submit(extra:JsonObject?=null) {
        val a=action?:return
        val entry=target
        if(a=="request_expense") {
            val requestType=form["requestType"]?:"travel"
            val missing=when {
                form["category"].isNullOrBlank()->"Choose a Finance category."
                (form["amount"]?.toDoubleOrNull()?:0.0)<=0.0->"Enter an estimate greater than zero."
                form["currency"].isNullOrBlank()->"Choose a currency."
                form["requiredDate"].isNullOrBlank()->"Enter a required date."
                requestType=="travel"&&form["destination"].isNullOrBlank()->"Enter the travel destination."
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
        val details=extra?:buildJsonObject { submittedForm.forEach { (k,v)->
            when(k) { "tags"->put(k,JsonArray(v.split(",").map{it.trim()}.filter{it.isNotBlank()}.map{JsonPrimitive(it)}));"amount","actualAmount"->put(k,v.toDoubleOrNull()?:-1.0);"isInternalNote","acknowledged"->put(k,v=="true");else->put(k,v) }
        } }
        sentGeneration=saved
        viewModel.mutateBusinessWorkspace(a,buildJsonObject { if(entry!=null){ put("id",entry.text("id"));put("version",entry["version"]!!) };put("data",details) })
    }
    Column(Modifier.fillMaxSize().padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
            Column { Text("${actor?.text("display_name").orEmpty()}",style=MaterialTheme.typography.titleMedium);Text("Authenticated shared workspace",style=MaterialTheme.typography.labelSmall) }
            TextButton(onClick={viewModel.refreshBusinessWorkspace()},enabled=!busy){Text("Refresh")}
        }
        LazyRow(horizontalArrangement=Arrangement.spacedBy(6.dp)){items(listOf("budgets","tickets","profile","communication")){id->FilterChip(selected=section==id,onClick={section=id},label={Text(when(id){"budgets"->"Expenses";"tickets"->"Tickets";"profile"->"My Profile";else->"Team Chat"})})}}
        if(busy)LinearProgressIndicator(Modifier.fillMaxWidth())
        failure?.let { Text(it,color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodySmall) }
        if(data==null) { Text("Shared data is unavailable until the business-workspace migration is deployed. No sample data is substituted.");return@Column }
        if(role=="viewer"&&section!="tickets"){Text("Access restricted to internal employees.");return@Column}
        if(section=="budgets"&&role!="viewer")Button(onClick={open("request_expense",defaults=mapOf("requestType" to "travel","purchaseKind" to "goods","currency" to "INR","requiredDate" to java.time.LocalDate.now(java.time.ZoneId.of("Asia/Kolkata")).toString()))},enabled=!busy){Text("New expense request")}
        if(section=="tickets")Button(onClick={open("ticket_create",defaults=mapOf("priority" to "Normal","category" to "General"))},enabled=!busy){Text("New ticket")}
        LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp),modifier=Modifier.weight(1f)) {
            if(section=="profile") {
                val profile=kind("profile").firstOrNull { it.text("ownerId")==me }?.let(::payload)
                item { Card(Modifier.fillMaxWidth()){Column(Modifier.padding(12.dp)){Text(actor?.text("display_name").orEmpty());listOf("employeeCode","email","phone","roleTitle","joiningDate").forEach {key->Text("$key: ${profile?.text(key)?.ifBlank { "Not recorded" }?:"Not recorded"}",style=MaterialTheme.typography.bodySmall)}}} }
                items(kind("assignment").filter { it.text("ownerId")==me }) { assignment->
                    val a=payload(assignment);val template=all.firstOrNull { it.text("id")==a.text("templateId") }?.let(::payload)
                    Card(Modifier.fillMaxWidth()){Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){Text(template?.text("title")?:"Assignment");Text("${a.text("status")} · due ${a.text("dueDate")}",style=MaterialTheme.typography.bodySmall);if(a.text("status") !in listOf("Completed","Submitted"))OutlinedButton(onClick={open("submit_assignment",assignment)},enabled=!busy){Text("Open")}}}
                }
            }
            if(section=="budgets") {
                items(kind("allocation")){entry->val a=payload(entry);Card(Modifier.fillMaxWidth()){Column(Modifier.padding(12.dp)){Text("${a.text("category")} · ${a.text("currency")}");Text("Allocated ${a.text("allocatedAmount")} · committed ${a.text("committedAmount")} · spent ${a.text("spentAmount")}",style=MaterialTheme.typography.bodySmall)}}}
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
    if(action!=null)AlertDialog(onDismissRequest={if(!busy)action=null},title={Text(action!!.replace('_',' '))},text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        @Composable fun field(key:String,label:String){OutlinedTextField(value=form[key].orEmpty(),onValueChange={form=form+(key to it)},label={Text(label)},modifier=Modifier.fillMaxWidth(),enabled=!busy)}
        @Composable fun select(key:String,label:String,options:List<Pair<String,String>>) {Text(label,style=MaterialTheme.typography.labelSmall);options.forEach{(id,name)->FilterChip(selected=form[key]==id,onClick={form=form+(key to id)},label={Text(name)})}}
        when(action) {
            "request_expense"->{
                val requestType=form["requestType"]?:"travel"
                val selectedProject=expenseProjects.firstOrNull{it.text("id")==form["projectId"]}
                val projectPhase=selectedProject?.let{it.text("companyPhase").ifBlank{it.text("phase")}}.orEmpty()
                val projectStage=selectedProject?.let{it.text("currentStage").ifBlank{it.text("stage")}}.orEmpty()
                Text("Project context (optional)",style=MaterialTheme.typography.labelSmall)
                LazyRow(horizontalArrangement=Arrangement.spacedBy(6.dp)){item{FilterChip(selected=form["projectId"].isNullOrBlank(),onClick={form=form-("projectId")},label={Text("Department work")})};items(expenseProjects){project->FilterChip(selected=form["projectId"]==project.text("id"),onClick={form=form+("projectId" to project.text("id"))},label={Text(project.text("reference"))})}}
                if(selectedProject!=null)Text("${selectedProject.text("reference")} · Phase: ${projectPhase.ifBlank{"Not recorded"}} · Stage: ${projectStage.ifBlank{"Not recorded"}}",style=MaterialTheme.typography.bodySmall)
                Text("Request type",style=MaterialTheme.typography.labelSmall)
                Row(horizontalArrangement=Arrangement.spacedBy(4.dp)){listOf("travel" to "Travel / advance","reimbursement" to "Reimbursement","purchase" to "Purchase").forEach{(id,label)->FilterChip(selected=requestType==id,onClick={form=form+("requestType" to id)+("category" to "")},label={Text(label)})}}
                Text("Finance category · choose manually",style=MaterialTheme.typography.labelSmall)
                if(expenseCategories.isEmpty())Text("Loading configured Finance categories…",style=MaterialTheme.typography.bodySmall) else LazyRow(horizontalArrangement=Arrangement.spacedBy(6.dp)){items(expenseCategories){category->FilterChip(selected=form["category"]==category,onClick={form=form+("category" to category)},label={Text(category)})}}
                val suggestion=suggestedFinanceCategory("$projectPhase $projectStage",requestType,expenseCategories)
                if(suggestion!=null&&form["category"]!=suggestion)TextButton(onClick={form=form+("category" to suggestion)}){Text("Use suggested category: $suggestion")}
                Text("Travel and reimbursement go to Finance. Purchases go to Procurement first. Category suggestions never approve spend.",style=MaterialTheme.typography.bodySmall)
                when(requestType){
                    "travel"->{field("destination","Destination / visit location");field("travelStartDate","Travel start YYYY-MM-DD");field("travelEndDate","Travel end YYYY-MM-DD")}
                    "reimbursement"->{field("merchant","Merchant / payee");field("expenseDate","Expense date YYYY-MM-DD");field("receiptReference","Receipt link / reference (optional)")}
                    else->{Text("Purchase type",style=MaterialTheme.typography.labelSmall);Row{FilterChip(selected=(form["purchaseKind"]?:"goods")=="goods",onClick={form=form+("purchaseKind" to "goods")},label={Text("Goods / materials")});FilterChip(selected=form["purchaseKind"]=="service",onClick={form=form+("purchaseKind" to "service")+("itemOrService" to "")+("serviceAction" to "new")},label={Text("Service")})};if(form["purchaseKind"]=="service") {field("serviceSearch","Find a company service");val matches=expenseServices.filter{it.text("name").contains(form["serviceSearch"].orEmpty(),ignoreCase=true)};LazyRow(horizontalArrangement=Arrangement.spacedBy(6.dp)){items(matches){serviceOption->val name=serviceOption.text("name");FilterChip(selected=form["itemOrService"]==name,onClick={form=form+("itemOrService" to name)+("serviceCatalogueId" to serviceOption.text("id"))+("supplierName" to serviceOption.text("provider"))+("billingCycle" to serviceOption.text("cycle"))+("serviceOwner" to serviceOption.text("owner"))},label={Text(name)})}};field("itemOrService","Service needed (or enter another)");select("serviceAction","Request",listOf("new" to "New service","renew" to "Renew existing service","seats" to "Add seats / capacity"));select("billingCycle","Billing cycle",listOf("Monthly" to "Monthly","Annual" to "Annual","One-time" to "One-time"));field("serviceSeats","Seats / units");field("serviceOwner","Responsible owner");if(form["serviceAction"]!="new")field("existingServiceReference","Subscription / contract reference")} else {field("itemOrService","Goods / materials needed");field("quantity","Quantity")};field("supplierName","Preferred supplier (optional)");field("quotationReference","Quote / invoice link or reference (optional)")}
                }
                field("amount","Estimate");field("currency","Currency");field("requiredDate","Required date YYYY-MM-DD")
                if(dept.isBlank())select("departmentId","Department",data?.rows("departments").orEmpty().map{it.text("id") to it.text("name")})
            }
            "approve_expense"->{if(payload(target!!).text("status")=="Pending_Dept_Review")Text("Forward to Finance review")else select("allocationId","Exact allocation",kind("allocation").filter{it.text("departmentId")==target!!.text("departmentId")&&it.text("projectId")==target!!.text("projectId")&&payload(it).text("category")==payload(target!!).text("category")&&payload(it).text("currency")==payload(target!!).text("currency")}.map{it.text("id") to "${payload(it).text("periodId")} · ${payload(it).text("category")}"})}
            "post_expense"->{field("actualAmount","Verified actual amount");field("postingRef","Finance posting reference");field("evidenceRef","Invoice / receipt evidence reference")}
            "reject_expense"->field("reason","Reason")
            "ticket_create"->{field("subject","Subject");field("description","Description");field("category","Category");select("priority","Priority",listOf("Critical","High","Normal","Low").map{it to it})}
            "ticket_reply"->{field("message","Reply");if(service||dept=="SAL")select("isInternalNote","Visibility",listOf("false" to "Requester reply","true" to "Internal note"))}
            "ticket_status"->{select("status","Status",listOf("Assigned","In Progress","Waiting","Resolved","Closed","Open").map{it to it});field("reason","Reason / reopen reason")}
            "send_message"->{field("text","Message");field("tags","Tags, separated by commas")}
            "submit_assignment"->{val assignment=payload(target!!);val template=all.firstOrNull{it.text("id")==assignment.text("templateId")}?.let(::payload);Text(template?.text("title").orEmpty());Text(template?.text("description").orEmpty());if(template?.text("isAnonymous")=="true")Text("Anonymous answers are stored separately; avoid identifying details. Completion is recorded separately.");if(template?.text("type")=="policy")select("acknowledged","Acknowledge this policy version",listOf("true" to "I acknowledge"))else template?.rows("questions")?.forEach {q->if(q.text("type")=="boolean")select(q.text("id"),q.text("label"),listOf("true" to "Yes","false" to "No"))else if(q.text("type")=="choice")select(q.text("id"),q.text("label"),(q["options"] as? JsonArray)?.map{it.jsonPrimitive.content to it.jsonPrimitive.content}.orEmpty())else field(q.text("id"),q.text("label"))}}
        }
        validationMessage.takeIf{it.isNotBlank()}?.let{Text(it,color=MaterialTheme.colorScheme.error)}
        failure?.let{Text(it,color=MaterialTheme.colorScheme.error)}
    }},confirmButton={TextButton(enabled=!busy,onClick={
        if(action=="submit_assignment") {val template=all.firstOrNull{it.text("id")==payload(target!!).text("templateId")}?.let(::payload);submit(buildJsonObject {put("acknowledged",form["acknowledged"]=="true");put("answers",buildJsonObject{template?.rows("questions")?.forEach{q->val key=q.text("id");val value=form[key];if(value!=null)when(q.text("type")){"boolean"->put(key,value=="true");"number","rating"->put(key,value.toDoubleOrNull()?:-1.0);else->put(key,value)}}})})}else submit()
    }){Text(if(busy)"Saving…" else "Save")}},dismissButton={TextButton(enabled=!busy,onClick={action=null}){Text("Cancel")}})
}
