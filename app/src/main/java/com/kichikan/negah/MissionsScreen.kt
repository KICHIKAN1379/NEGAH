package com.kichikan.negah

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kichikan.negah.domain.economy.EconomyCurrency
import com.kichikan.negah.domain.mission.*

@Composable
fun MissionsScreen(viewModel: ProgressViewModel, modifier: Modifier = Modifier) {
 val state by viewModel.state.collectAsStateWithLifecycle()
 var tab by rememberSaveable { mutableStateOf(MissionKind.REGULAR.name) }
 var showCreate by remember { mutableStateOf(false) }
 var chosen by rememberSaveable { mutableStateOf<String?>(null) }
 val kind=MissionKind.valueOf(tab)
 val missions=state.missions.filter { it.kind==kind }
 LaunchedEffect(tab,missions.firstOrNull()?.id) {
  val id=chosen?.takeIf { candidate -> missions.any { it.id==candidate } } ?: missions.firstOrNull()?.id
  chosen=id
  id?.let(viewModel::selectMission)
 }
 Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
  Text("مأموریت‌ها",style=MaterialTheme.typography.headlineSmall)
  Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
   MissionKind.entries.forEach { item -> FilterChip(selected=kind==item,onClick={tab=item.name;chosen=null},label={Text(item.titleFa())}) }
  }
  when(kind) {
   MissionKind.REGULAR -> {
    Button(onClick={showCreate=true},modifier=Modifier.fillMaxWidth()){Text("ایجاد مأموریت")}
    if(missions.isEmpty()) Text("هنوز مأموریت عادی ثبت نشده است.")
    missions.forEach { mission ->
     Card(onClick={chosen=mission.id;viewModel.selectMission(mission.id)},modifier=Modifier.fillMaxWidth()) {
      Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
       Text(mission.title,style=MaterialTheme.typography.titleMedium)
       mission.description?.let { Text(it) }
       Text("پاداش: "+mission.rewardAmount+" "+mission.rewardCurrency.titleFa())
      }
     }
    }
    chosen?.let { missionId ->
     Button(onClick={viewModel.assignSelectedMember(missionId)},enabled=state.selectedMemberId!=null,modifier=Modifier.fillMaxWidth()){Text("اختصاص به عضو انتخاب‌شده در بخش پیشرفت")}
     state.assignments.forEach { assignment ->
      Card(Modifier.fillMaxWidth()) {
       Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Text(state.members.firstOrNull { it.id==assignment.memberId }?.fullName ?: "عضو بایگانی‌شده")
        Text(assignment.status.titleFa())
        var note by remember(assignment.id,assignment.coachProgressNote){mutableStateOf(assignment.coachProgressNote.orEmpty())}
        OutlinedTextField(note,{note=it},label={Text("گزارش پیشرفت مربی")},modifier=Modifier.fillMaxWidth())
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
         OutlinedButton(onClick={viewModel.updateMissionProgress(assignment.id,note,false)},enabled=assignment.status!=MissionAssignmentStatus.APPROVED){Text("ثبت پیشرفت")}
         Button(onClick={viewModel.updateMissionProgress(assignment.id,note,true)},enabled=assignment.status!=MissionAssignmentStatus.APPROVED){Text("اعلام تکمیل")}
        }
        Button(onClick={viewModel.approveMission(assignment.id)},enabled=assignment.status==MissionAssignmentStatus.COMPLETED,modifier=Modifier.fillMaxWidth()){Text("تأیید مربی و پرداخت پاداش")}
       }
      }
     }
    }
   }
   MissionKind.HIDDEN -> Text("مأموریت مخفی؛ قواعد اختصاصی و نحوه نمایش آن هنوز نیازمند تصمیم مربی است.")
   MissionKind.GROUP_PUZZLE -> Text("پازل گروهی؛ قواعد گروه‌بندی و تکمیل گروهی هنوز تعیین نشده است.")
  }
 }
 if(showCreate) NewMissionDialog({showCreate=false}) { title,desc,currency,amount ->
  viewModel.createMission(MissionKind.REGULAR,title,desc,currency,amount);showCreate=false
 }
}
@Composable
private fun NewMissionDialog(close:()->Unit,create:(String,String,EconomyCurrency,Long)->Unit) {
 var title by remember { mutableStateOf("") };var desc by remember { mutableStateOf("") }
 var amount by remember { mutableStateOf("1") };var currency by remember { mutableStateOf(EconomyCurrency.XP) }
 AlertDialog(onDismissRequest=close,title={Text("مأموریت عادی جدید")},text={
  Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
   OutlinedTextField(title,{title=it},label={Text("عنوان")},singleLine=true)
   OutlinedTextField(desc,{desc=it},label={Text("توضیحات")})
   Text("نوع پاداش")
   Row(horizontalArrangement=Arrangement.spacedBy(4.dp)){EconomyCurrency.entries.forEach { c -> FilterChip(selected=currency==c,onClick={currency=c},label={Text(c.titleFa())}) }}
   OutlinedTextField(amount,{amount=it.filter(Char::isDigit)},label={Text("مقدار پاداش")},singleLine=true)
  }
 },confirmButton={Button(onClick={amount.toLongOrNull()?.let { create(title,desc,currency,it) }},enabled=title.isNotBlank()&&(amount.toLongOrNull()?:0)>0){Text("ثبت")}},
 dismissButton={TextButton(onClick=close){Text("انصراف")}})
}
private fun MissionKind.titleFa()=when(this){MissionKind.REGULAR->"مأموریت";MissionKind.HIDDEN->"مأموریت مخفی";MissionKind.GROUP_PUZZLE->"پازل گروهی"}
private fun EconomyCurrency.titleFa()=when(this){EconomyCurrency.XP->"XP";EconomyCurrency.POINTS->"امتیاز";EconomyCurrency.DIAMONDS->"الماس"}
private fun MissionAssignmentStatus.titleFa()=when(this){MissionAssignmentStatus.ASSIGNED->"اختصاص‌یافته";MissionAssignmentStatus.IN_PROGRESS->"در حال انجام (ثبت مربی)";MissionAssignmentStatus.COMPLETED->"در انتظار تأیید مربی";MissionAssignmentStatus.APPROVED->"تأیید و پاداش ثبت‌شده"}
