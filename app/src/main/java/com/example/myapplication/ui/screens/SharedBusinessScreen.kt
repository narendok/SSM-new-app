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
    val finance=role=="admin" || (role=="department_lead" && dept=="FIN")
    val service=role=="admin" || dept=="SIT"
    fun kind(k:String)=all.filter { it.text("kind")==k }
    fun payload(item:JsonObject)=item["data"] as? JsonObject ?: buildJsonObject {}
    fun open(a:String,item:JsonObject?=null,defaults:Map<String,String> = emptyMap()) { action=a;target=item;form=defaults;viewModel.clearBusinessError() }
    fun submit(extra:JsonObject?=null) {
        val a=action?:return
        val entry=target
        val details=extra?:buildJsonObject { form.forEach { (k,v)->
            when(k) { "tags"->put(k,JsonArray(v.split(",").map{it.trim()}.filter{it.isNotBlank()}.map{JsonPrimitive(it)}));"amount","actualAmount"->put(k,v.toDoubleOrNull()?:-1.0);"isInternalNote","acknowledged"->put(k,v=="true");else->put(k,v) }
        } }
        sentGeneration=saved
        viewModel.mutateBusinessWorkspace(a,buildJsonObject { if(entry!=null){ put("id",entry.text("id"));put("version",entry["version"]!!) };put("data",details) })
    }
    Column(Modifier.fillMaxSize().padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
            Column { Text("${actor?.text("display_name").orEmpty()}",style=MaterialTheme.typography.titleMedium);Text("Shared TEST",style=MaterialTheme.typography.labelSmall) }
            TextButton(onClick={viewModel.refreshBusinessWorkspace()},enabled=!busy){Text("Refresh")}
        }
        LazyRow(horizontalArrangement=Arrangement.spacedBy(6.dp)){items(listOf("budgets","tickets","profile","communication")){id->FilterChip(selected=section==id,onClick={section=id},label={Text(when(id){"budgets"->"Expenses";"tickets"->"Tickets";"profile"->"My Profile";else->"Team Chat"})})}}
        if(busy)LinearProgressIndicator(Modifier.fillMaxWidth())
        failure?.let { Text(it,color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodySmall) }
        if(data==null) { Text("Shared data is unavailable until the business-workspace migration is deployed. No sample data is substituted.");return@Column }
        if(role=="viewer"&&section!="tickets"){Text("Access restricted to internal employees.");return@Column}
        if(section=="budgets"&&role!="viewer")Button(onClick={open("request_expense",defaults=mapOf("currency" to "INR","requiredDate" to java.time.LocalDate.now(java.time.ZoneId.of("Asia/Kolkata")).toString()))},enabled=!busy){Text("New expense / travel request")}
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
            "request_expense"->{field("purpose","Purpose");field("category","Category");field("amount","Estimate");field("currency","Currency");field("requiredDate","Required date YYYY-MM-DD");if(dept.isBlank())select("departmentId","Department",data?.rows("departments").orEmpty().map{it.text("id") to it.text("name")});select("projectId","Project (optional)",listOf("" to "Department work")+data?.rows("projects").orEmpty().map{it.text("id") to it.text("reference")})}
            "approve_expense"->{if(payload(target!!).text("status")=="Pending_Dept_Review")Text("Forward to Finance review")else select("allocationId","Exact allocation",kind("allocation").filter{it.text("departmentId")==target!!.text("departmentId")&&it.text("projectId")==target!!.text("projectId")&&payload(it).text("category")==payload(target!!).text("category")&&payload(it).text("currency")==payload(target!!).text("currency")}.map{it.text("id") to "${payload(it).text("periodId")} · ${payload(it).text("category")}"})}
            "post_expense"->{field("actualAmount","Verified actual amount");field("postingRef","Finance posting reference");field("evidenceRef","Invoice / receipt evidence reference")}
            "reject_expense"->field("reason","Reason")
            "ticket_create"->{field("subject","Subject");field("description","Description");field("category","Category");select("priority","Priority",listOf("Critical","High","Normal","Low").map{it to it})}
            "ticket_reply"->{field("message","Reply");if(service||dept=="SAL")select("isInternalNote","Visibility",listOf("false" to "Requester reply","true" to "Internal note"))}
            "ticket_status"->{select("status","Status",listOf("Assigned","In Progress","Waiting","Resolved","Closed","Open").map{it to it});field("reason","Reason / reopen reason")}
            "send_message"->{field("text","Message");field("tags","Tags, separated by commas")}
            "submit_assignment"->{val assignment=payload(target!!);val template=all.firstOrNull{it.text("id")==assignment.text("templateId")}?.let(::payload);Text(template?.text("title").orEmpty());Text(template?.text("description").orEmpty());if(template?.text("isAnonymous")=="true")Text("Anonymous answers are stored separately; avoid identifying details. Completion is recorded separately.");if(template?.text("type")=="policy")select("acknowledged","Acknowledge this policy version",listOf("true" to "I acknowledge"))else template?.rows("questions")?.forEach {q->if(q.text("type")=="boolean")select(q.text("id"),q.text("label"),listOf("true" to "Yes","false" to "No"))else if(q.text("type")=="choice")select(q.text("id"),q.text("label"),(q["options"] as? JsonArray)?.map{it.jsonPrimitive.content to it.jsonPrimitive.content}.orEmpty())else field(q.text("id"),q.text("label"))}}
        }
        failure?.let{Text(it,color=MaterialTheme.colorScheme.error)}
    }},confirmButton={TextButton(enabled=!busy,onClick={
        if(action=="submit_assignment") {val template=all.firstOrNull{it.text("id")==payload(target!!).text("templateId")}?.let(::payload);submit(buildJsonObject {put("acknowledged",form["acknowledged"]=="true");put("answers",buildJsonObject{template?.rows("questions")?.forEach{q->val key=q.text("id");val value=form[key];if(value!=null)when(q.text("type")){"boolean"->put(key,value=="true");"number","rating"->put(key,value.toDoubleOrNull()?:-1.0);else->put(key,value)}}})})}else submit()
    }){Text(if(busy)"Saving…" else "Save")}},dismissButton={TextButton(enabled=!busy,onClick={action=null}){Text("Cancel")}})
}
