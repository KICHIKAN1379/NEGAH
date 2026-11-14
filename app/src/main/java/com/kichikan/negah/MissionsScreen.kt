package com.kichikan.negah

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
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
 val context=LocalContext.current
 val exportLauncher=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/jpeg")) { uri: Uri? ->
  if(uri!=null) {
   val bitmap=renderMissionCatalogJpeg(missions)
   runCatching { context.contentResolver.openOutputStream(uri)?.use { output -> bitmap.compress(Bitmap.CompressFormat.JPEG,94,output) } }
   bitmap.recycle()
  }
 }
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
   MissionKind.REGULAR, MissionKind.HIDDEN -> {
    val hidden=kind==MissionKind.HIDDEN
    Button(onClick={showCreate=true},modifier=Modifier.fillMaxWidth()){Text(if(hidden) "ایجاد مأموریت ویژه (مخفی)" else "ایجاد مأموریت")}
    if(hidden) Text("🔒 فقط برای مربی؛ این مأموریت‌ها در خروجی JPEG و نمایش عمومی نمی‌آیند.",style=MaterialTheme.typography.bodySmall)
    else OutlinedButton(onClick={exportLauncher.launch("فهرست-مأموریت‌ها.jpg")},enabled=missions.isNotEmpty(),modifier=Modifier.fillMaxWidth()) {
     Text("خروجی JPEG توضیحات و پاداش‌ها")
    }
    if(missions.isEmpty()) Text(if(hidden) "هنوز مأموریت ویژه‌ای ثبت نشده است." else "هنوز مأموریت عادی ثبت نشده است.")
    missions.forEach { mission ->
     Card(onClick={chosen=mission.id;viewModel.selectMission(mission.id)},modifier=Modifier.fillMaxWidth()) {
      Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
       Text((if(hidden) "🔒 " else "")+mission.title,style=MaterialTheme.typography.titleMedium)
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
   MissionKind.GROUP_PUZZLE -> {
    Button(onClick={showCreate=true},modifier=Modifier.fillMaxWidth()){Text("ایجاد پازل گروهی")}
    Text("هر پازل شش تکه دارد؛ با ثبت تکه ششم، پاداش یک‌بار به حساب خود گروه واریز می‌شود.",style=MaterialTheme.typography.bodySmall)
    if(missions.isEmpty()) Text("هنوز پازلی ثبت نشده است.")
    missions.forEach { mission ->
     Card(onClick={chosen=mission.id;viewModel.selectMission(mission.id)},modifier=Modifier.fillMaxWidth()) {
      Column(Modifier.padding(12.dp)) {
       Text(mission.title,style=MaterialTheme.typography.titleMedium)
       Text("پاداش گروه: "+mission.rewardAmount+" "+mission.rewardCurrency.titleFa())
      }
     }
    }
    state.puzzleMessage?.let { Text(it,color=MaterialTheme.colorScheme.primary) }
    chosen?.let { puzzleId ->
     if(state.groups.isEmpty()) Text("هنوز گروه فعالی ساخته نشده است.")
     state.groups.forEach { g ->
      val done=state.pieces.filter { it.missionId==puzzleId && it.groupId==g.group.id }.map { it.pieceIndex }.toSet()
      Card(Modifier.fillMaxWidth()) {
       Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Text(g.group.name+" — "+done.size+" از "+PUZZLE_PIECE_COUNT,style=MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
         (1..PUZZLE_PIECE_COUNT).forEach { i ->
          FilterChip(selected=i in done,onClick={viewModel.completePuzzlePiece(puzzleId,g.group.id,i)},enabled=i !in done,label={Text(i.toString())})
         }
        }
       }
      }
     }
    }
   }
  }
 }
 if(showCreate) NewMissionDialog(kind,{showCreate=false}) { title,desc,currency,amount ->
  viewModel.createMission(kind,title,desc,currency,amount);showCreate=false
 }
}
@Composable
private fun NewMissionDialog(kind:MissionKind,close:()->Unit,create:(String,String,EconomyCurrency,Long)->Unit) {
 var title by remember { mutableStateOf("") };var desc by remember { mutableStateOf("") }
 var amount by remember { mutableStateOf("1") };var currency by remember { mutableStateOf(if(kind==MissionKind.GROUP_PUZZLE) EconomyCurrency.POINTS else EconomyCurrency.XP) }
 val currencies=if(kind==MissionKind.GROUP_PUZZLE) listOf(EconomyCurrency.POINTS,EconomyCurrency.DIAMONDS) else EconomyCurrency.entries
 AlertDialog(onDismissRequest=close,title={Text(when(kind){MissionKind.HIDDEN->"مأموریت ویژه (مخفی) جدید";MissionKind.GROUP_PUZZLE->"پازل گروهی جدید";else->"مأموریت عادی جدید"})},text={
  Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
   OutlinedTextField(title,{title=it},label={Text("عنوان")},singleLine=true)
   OutlinedTextField(desc,{desc=it},label={Text("توضیحات")})
   Text("نوع پاداش")
   Row(horizontalArrangement=Arrangement.spacedBy(4.dp)){currencies.forEach { c -> FilterChip(selected=currency==c,onClick={currency=c},label={Text(c.titleFa())}) }}
   OutlinedTextField(amount,{amount=it.filter(Char::isDigit)},label={Text("مقدار پاداش")},singleLine=true)
  }
 },confirmButton={Button(onClick={amount.toLongOrNull()?.let { create(title,desc,currency,it) }},enabled=title.isNotBlank()&&(amount.toLongOrNull()?:0)>0){Text("ثبت")}},
 dismissButton={TextButton(onClick=close){Text("انصراف")}})
}

private fun renderMissionCatalogJpeg(missions: List<Mission>): Bitmap {
 val width=1200
 val outer=64f
 val cardPadding=28f
 val contentWidth=(width-outer*2-cardPadding*2).toInt()
 val titlePaint=TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color=android.graphics.Color.rgb(28,42,57); textSize=46f; typeface=Typeface.create("sans-serif",Typeface.BOLD) }
 val bodyPaint=TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color=android.graphics.Color.rgb(66,76,88); textSize=32f; typeface=Typeface.create("sans-serif",Typeface.NORMAL) }
 val rewardPaint=TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color=android.graphics.Color.rgb(25,105,91); textSize=30f; typeface=Typeface.create("sans-serif-medium",Typeface.BOLD) }
 fun layout(text:String,paint:TextPaint)=StaticLayout.Builder.obtain(text,0,text.length,paint,contentWidth)
  .setAlignment(Layout.Alignment.ALIGN_NORMAL).setTextDirection(TextDirectionHeuristics.RTL)
  .setIncludePad(false).setLineSpacing(7f,1f).build()
 val header=layout("فهرست مأموریت‌ها",titlePaint)
 data class Entry(val title:StaticLayout,val description:StaticLayout,val reward:StaticLayout,val height:Float)
 val entries=missions.map { mission ->
  val title=layout(mission.title,titlePaint)
  val description=layout(mission.description?.takeIf { it.isNotBlank() } ?: "بدون توضیحات",bodyPaint)
  val reward=layout("پاداش: ${mission.rewardAmount} ${mission.rewardCurrency.titleFa()}",rewardPaint)
  Entry(title,description,reward,cardPadding*2+title.height+description.height+reward.height+34f)
 }
 val gap=26f
 val totalHeight=(outer*2+header.height+40f+entries.sumOf { it.height.toDouble() }+gap*(entries.size-1).coerceAtLeast(0)).toInt().coerceAtLeast(500)
 val bitmap=Bitmap.createBitmap(width,totalHeight,Bitmap.Config.ARGB_8888)
 val canvas=Canvas(bitmap)
 canvas.drawColor(android.graphics.Color.rgb(247,248,250))
 val cardPaint=Paint(Paint.ANTI_ALIAS_FLAG).apply { color=android.graphics.Color.WHITE }
 val borderPaint=Paint(Paint.ANTI_ALIAS_FLAG).apply { color=android.graphics.Color.rgb(226,231,236); style=Paint.Style.STROKE; strokeWidth=2f }
 canvas.save();canvas.translate(outer,outer);header.draw(canvas);canvas.restore()
 var top=outer+header.height+40f
 entries.forEach { entry ->
  val rect=RectF(outer,top,width-outer,top+entry.height)
  canvas.drawRoundRect(rect,24f,24f,cardPaint)
  canvas.drawRoundRect(rect,24f,24f,borderPaint)
  var y=top+cardPadding
  canvas.save();canvas.translate(outer+cardPadding,y);entry.title.draw(canvas);canvas.restore();y+=entry.title.height+16f
  canvas.save();canvas.translate(outer+cardPadding,y);entry.description.draw(canvas);canvas.restore();y+=entry.description.height+16f
  canvas.save();canvas.translate(outer+cardPadding,y);entry.reward.draw(canvas);canvas.restore()
  top+=entry.height+gap
 }
 val footerPaint=Paint(Paint.ANTI_ALIAS_FLAG).apply { color=android.graphics.Color.rgb(120,130,140); textSize=22f; textAlign=Paint.Align.CENTER }
 canvas.drawText("نگاه · فهرست مأموریت‌ها",width/2f,totalHeight-outer/2f,footerPaint)
 return bitmap
}

private fun MissionKind.titleFa()=when(this){MissionKind.REGULAR->"مأموریت";MissionKind.HIDDEN->"مأموریت مخفی";MissionKind.GROUP_PUZZLE->"پازل گروهی"}
private fun EconomyCurrency.titleFa()=when(this){EconomyCurrency.XP->"XP";EconomyCurrency.POINTS->"امتیاز";EconomyCurrency.DIAMONDS->"الماس"}
private fun MissionAssignmentStatus.titleFa()=when(this){MissionAssignmentStatus.ASSIGNED->"اختصاص‌یافته";MissionAssignmentStatus.IN_PROGRESS->"در حال انجام (ثبت مربی)";MissionAssignmentStatus.COMPLETED->"در انتظار تأیید مربی";MissionAssignmentStatus.APPROVED->"تأیید و پاداش ثبت‌شده"}
