package com.kichikan.negah
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kichikan.negah.domain.inventory.InventoryCategory
@Composable
fun ShopScreen(viewModel: ShopViewModel, modifier: Modifier = Modifier) {
 val state by viewModel.state.collectAsStateWithLifecycle()
 val member = state.members.firstOrNull { it.id == state.selectedMemberId }
 val categoryItems = state.items.filter { it.category == state.category }
 Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
  Text("فروشگاه", style = MaterialTheme.typography.headlineSmall)
  Text("آواتارها، قاب‌ها و جایزه‌ها را برای اعضای حلقه تهیه کنید.")
  if (state.members.isEmpty()) Text("برای استفاده از فروشگاه ابتدا یک عضو ثبت کنید.")
  else {
   Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    state.members.forEach { person -> FilterChip(selected = person.id == state.selectedMemberId, onClick = { viewModel.selectMember(person.id) }, label = { Text(person.nickname?.takeIf(String::isNotBlank) ?: person.fullName) }) }
   }
   member?.let { Card(Modifier.fillMaxWidth()) { Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
    Column { Text("امتیاز قابل‌خرج", style = MaterialTheme.typography.bodySmall); Text(it.spendablePoints.toString(), style = MaterialTheme.typography.titleLarge) }
    Column { Text("الماس", style = MaterialTheme.typography.bodySmall); Text(it.diamonds.toString(), style = MaterialTheme.typography.titleLarge) }
   } } }
   Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
    listOf(InventoryCategory.AVATAR to "آواتار", InventoryCategory.FRAME to "قاب", InventoryCategory.REWARD to "جایزه").forEach { (cat, label) -> FilterChip(selected = state.category == cat, onClick = { viewModel.selectCategory(cat) }, label = { Text(label) }) }
   }
   Text(if (state.category == InventoryCategory.REWARD) "جایزه‌ها با امتیاز قابل‌خرج خریداری می‌شوند." else "آواتار و قاب با الماس خریداری می‌شوند.")
   state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
   if (categoryItems.isEmpty()) Text("در این دسته هنوز آیتم فعالی تعریف نشده است.")
   else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    items(categoryItems, key = { it.id }) { item ->
     val owned = item.id in state.ownedItemIds
     Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
      Text(item.title, style = MaterialTheme.typography.titleMedium)
      item.description?.takeIf(String::isNotBlank)?.let { Text(it) }
      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
       Text(item.priceAmount?.let { it.toString() + if (item.category == InventoryCategory.REWARD) " امتیاز" else " الماس" } ?: "قیمت تعیین نشده", color = MaterialTheme.colorScheme.secondary)
       Text(if (owned) "دریافت‌شده" else "قابل تهیه")
      }
      if (owned) Text("این آیتم در گنجینه عضو موجود است.")
      else Button(onClick = { viewModel.purchase(item.id) }, enabled = item.priceAmount?.let { it > 0L } == true) { Text("خرید") }
     } }
    }
   }
  }
 }
}
