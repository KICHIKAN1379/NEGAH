package com.kichikan.negah

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kichikan.negah.domain.economy.EconomyCurrency
import com.kichikan.negah.domain.wheel.WheelPrize

private fun currencyName(currency: EconomyCurrency) = when (currency) {
    EconomyCurrency.XP -> "XP"
    EconomyCurrency.POINTS -> "امتیاز"
    EconomyCurrency.DIAMONDS -> "الماس"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WheelScreen(
    viewModel: WheelViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var selectedMemberId by rememberSaveable { mutableStateOf<String?>(null) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    val selected = state.members.firstOrNull { it.id == selectedMemberId }
    val names = state.members.associate { it.id to (it.nickname?.takeIf { n -> n.isNotBlank() } ?: it.fullName) }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("گردونه", style = MaterialTheme.typography.headlineSmall)
        Text("هزینه هر چرخش: ${state.spinCost} امتیاز قابل‌خرج")
        state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }

        Text("عضو", style = MaterialTheme.typography.titleMedium)
        if (state.members.isEmpty()) Text("هنوز عضوی ثبت نشده است.")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            state.members.forEach { member ->
                FilterChip(
                    selected = member.id == selectedMemberId,
                    onClick = { selectedMemberId = member.id; viewModel.clearMessage() },
                    label = { Text(names[member.id] ?: member.fullName) },
                )
            }
        }
        selected?.let { Text("امتیاز قابل‌خرج: ${it.spendablePoints} · الماس: ${it.diamonds}") }
        val activePrizes = state.prizes.filter { it.isActive && it.weight > 0 }
        val rotation = remember { Animatable(0f) }
        var spinning by remember { mutableStateOf(false) }
        var revealedToken by rememberSaveable { mutableStateOf(state.spinToken) }
        LaunchedEffect(state.spinToken) {
            val win = state.lastWin
            if (state.spinToken == revealedToken || win == null) return@LaunchedEffect
            val total = activePrizes.sumOf { it.weight }.toFloat()
            val index = activePrizes.indexOfFirst { it.id == win.prizeId }
            if (total <= 0f || index < 0) { revealedToken = state.spinToken; return@LaunchedEffect }
            val before = activePrizes.take(index).sumOf { it.weight }
            val center = (before + activePrizes[index].weight / 2f) / total * 360f
            val current = rotation.value
            val toTop = (((360f - center) - current) % 360f + 360f) % 360f
            // Turns and duration come from the phone-clock second at the tap.
            val turns = 4 + state.spinSecond % 3
            val duration = 2500 + (state.spinSecond % 5) * 300
            spinning = true
            rotation.animateTo(current + turns * 360f + toTop, tween(duration, easing = CubicBezierEasing(0.15f, 0.7f, 0.2f, 1f)))
            spinning = false
            revealedToken = state.spinToken
        }
        if (activePrizes.isNotEmpty()) WheelDisk(activePrizes, rotation.value)
        Button(onClick = { viewModel.spin(selectedMemberId) }, enabled = state.prizes.any { it.isActive } && !spinning) {
            Text("چرخاندن گردونه")
        }
        if (state.prizes.none { it.isActive }) Text("برای چرخاندن، ابتدا حداقل یک جایزه فعال تعریف کنید.")
        state.lastWin?.takeIf { !spinning && revealedToken == state.spinToken }?.let { win ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("نتیجه: ${win.prizeLabel}", style = MaterialTheme.typography.titleMedium)
                    Text("${win.amount} ${currencyName(win.currency)} برای ${names[win.memberId] ?: ""}")
                }
            }
        }

        Text("آخرین برندگان", style = MaterialTheme.typography.titleMedium)
        if (state.spins.isEmpty()) Text("هنوز چرخشی ثبت نشده است.")
        state.spins.take(10).forEach { spin ->
            Text("${names[spin.memberId] ?: "عضو"} — ${spin.prizeLabel} (${spin.amount} ${currencyName(spin.currency)})")
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("تنظیمات گردونه (مربی)", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            Switch(checked = showSettings, onCheckedChange = { showSettings = it })
        }
        if (showSettings) WheelSettings(state.prizes, state.spinCost, viewModel)
        TextButton(onClick = onBack) { Text("بازگشت") }
    }
}

@Composable
private fun WheelSettings(prizes: List<WheelPrize>, spinCost: Long, viewModel: WheelViewModel) {
    var costText by rememberSaveable(spinCost) { mutableStateOf(spinCost.toString()) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(costText, { costText = it }, label = { Text("هزینه چرخش (امتیاز)") }, singleLine = true, modifier = Modifier.weight(1f))
        Button(onClick = { viewModel.setSpinCost(costText.trim().toLongOrNull()) }) { Text("ذخیره") }
    }
    Text("جوایز (حذف نمی‌شوند؛ فقط غیرفعال می‌شوند تا سابقه چرخش‌ها حفظ شود)", style = MaterialTheme.typography.bodySmall)
    prizes.forEach { prize ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${prize.label} · ${prize.amount} ${currencyName(prize.currency)} · وزن ${prize.weight}", Modifier.weight(1f))
            Switch(checked = prize.isActive, onCheckedChange = { viewModel.setPrizeActive(prize, it) })
        }
    }
    var label by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableStateOf("") }
    var weight by rememberSaveable { mutableStateOf("") }
    var currency by rememberSaveable { mutableStateOf(EconomyCurrency.POINTS.name) }
    Text("جایزه جدید", style = MaterialTheme.typography.titleSmall)
    OutlinedTextField(label, { label = it }, label = { Text("عنوان") }, singleLine = true, modifier = Modifier.fillMaxWidth())
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(EconomyCurrency.POINTS, EconomyCurrency.DIAMONDS).forEach { c ->
            FilterChip(selected = currency == c.name, onClick = { currency = c.name }, label = { Text(currencyName(c)) })
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(amount, { amount = it }, label = { Text("مقدار") }, singleLine = true, modifier = Modifier.weight(1f))
        OutlinedTextField(weight, { weight = it }, label = { Text("وزن") }, singleLine = true, modifier = Modifier.weight(1f))
    }
    Button(onClick = {
        viewModel.savePrize(null, label, EconomyCurrency.valueOf(currency), amount.trim().toLongOrNull(), weight.trim().toIntOrNull(), true)
        label = ""; amount = ""; weight = ""
    }) { Text("افزودن جایزه") }
}

private val wheelColors = listOf(
    Color(0xFF3F6FB5), Color(0xFFE08A2E), Color(0xFF3E9B6A), Color(0xFFB5513F), Color(0xFF7A5BB5), Color(0xFFC9A227),
)

@Composable
private fun WheelDisk(prizes: List<WheelPrize>, rotationDegrees: Float) {
    val measurer = rememberTextMeasurer()
    val total = prizes.sumOf { it.weight }.toFloat()
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Canvas(Modifier.fillMaxWidth(0.85f).aspectRatio(1f).padding(top = 14.dp)) {
            val radius = size.minDimension / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            rotate(rotationDegrees, center) {
                var start = -90f
                prizes.forEachIndexed { i, prize ->
                    val sweep = prize.weight / total * 360f
                    drawArc(wheelColors[i % wheelColors.size], start, sweep, true, Offset.Zero, Size(size.width, size.height))
                    val mid = Math.toRadians((start + sweep / 2f).toDouble())
                    val label = measurer.measure(prize.label, TextStyle(color = Color.White, fontSize = 12.sp), maxLines = 1)
                    val tx = center.x + (radius * 0.6f) * Math.cos(mid).toFloat() - label.size.width / 2f
                    val ty = center.y + (radius * 0.6f) * Math.sin(mid).toFloat() - label.size.height / 2f
                    drawText(label, topLeft = Offset(tx, ty))
                    start += sweep
                }
            }
            drawCircle(Color.White, radius * 0.08f, center)
            val pointer = Path().apply {
                moveTo(center.x - 14f, 0f); lineTo(center.x + 14f, 0f); lineTo(center.x, 34f); close()
            }
            drawPath(pointer, Color(0xFFD32F2F))
        }
    }
}
