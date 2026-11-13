package com.kichikan.negah

import androidx.compose.foundation.layout.Arrangement
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
        Button(onClick = { viewModel.spin(selectedMemberId) }, enabled = state.prizes.any { it.isActive }) {
            Text("چرخاندن گردونه")
        }
        if (state.prizes.none { it.isActive }) Text("برای چرخاندن، ابتدا حداقل یک جایزه فعال تعریف کنید.")
        state.lastWin?.let { win ->
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
