package com.kichikan.negah.feature.refinery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kichikan.negah.domain.growth.GrowthItem
import com.kichikan.negah.domain.growth.GrowthMapRepository
import com.kichikan.negah.domain.growth.GrowthTrack
import com.kichikan.negah.domain.inventory.CatalogItem
import com.kichikan.negah.domain.inventory.InventoryCategory
import com.kichikan.negah.domain.inventory.InventoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class RefineryUiState(val growthItems: List<GrowthItem> = emptyList(), val catalogItems: List<CatalogItem> = emptyList())

@HiltViewModel
class RefineryViewModel @Inject constructor(
    private val growth: GrowthMapRepository,
    private val inventory: InventoryRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(RefineryUiState())
    val state = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                growth.observeAllItems(GrowthTrack.CAPABILITY),
                growth.observeAllItems(GrowthTrack.SELF_IMPROVEMENT),
                inventory.observeAllCatalog(InventoryCategory.AVATAR),
                inventory.observeAllCatalog(InventoryCategory.FRAME),
                inventory.observeAllCatalog(InventoryCategory.REWARD),
            ) { cap, self, avatar, frame, reward ->
                RefineryUiState(cap + self, avatar + frame + reward)
            }.collect { mutableState.value = it }
        }
    }

    fun saveGrowth(existing: GrowthItem?, track: GrowthTrack, title: String, description: String) {
        if (title.isBlank()) return
        val now = System.currentTimeMillis()
        viewModelScope.launch {
            growth.saveItem(GrowthItem(
                id = existing?.id ?: UUID.randomUUID().toString(), track = track, title = title.trim(),
                description = description.trim().ifEmpty { null },
                sortOrder = existing?.sortOrder ?: state.value.growthItems.count { it.track == track },
                isActive = existing?.isActive ?: true, createdAtEpochMillis = existing?.createdAtEpochMillis ?: now,
                updatedAtEpochMillis = now,
            ))
        }
    }

    fun toggleGrowth(item: GrowthItem) {
        viewModelScope.launch { growth.saveItem(item.copy(isActive = !item.isActive, updatedAtEpochMillis = System.currentTimeMillis())) }
    }

    fun saveCatalog(existing: CatalogItem?, category: InventoryCategory, title: String, description: String, priceAmount: Long?) {
        if (title.isBlank()) return
        viewModelScope.launch {
            inventory.saveCatalogItem(CatalogItem(
                id = existing?.id ?: UUID.randomUUID().toString(), category = category, title = title.trim(),
                description = description.trim().ifEmpty { null }, isAvailable = existing?.isAvailable ?: true,
                sortOrder = existing?.sortOrder ?: state.value.catalogItems.count { it.category == category },
                priceAmount = priceAmount,
            ))
        }
    }

    fun toggleCatalog(item: CatalogItem) {
        viewModelScope.launch { inventory.saveCatalogItem(item.copy(isAvailable = !item.isAvailable)) }
    }
}

@Composable
fun RefineryScreen(viewModel: RefineryViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var section by rememberSaveable { mutableStateOf(0) }
    var track by rememberSaveable { mutableStateOf(GrowthTrack.CAPABILITY.name) }
    var category by rememberSaveable { mutableStateOf(InventoryCategory.AVATAR.name) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var priceText by rememberSaveable { mutableStateOf("") }
    var editorOpen by rememberSaveable { mutableStateOf(false) }
    val selectedTrack = GrowthTrack.valueOf(track)
    val selectedCategory = InventoryCategory.valueOf(category)
    val growthItems = state.growthItems.filter { it.track == selectedTrack }
    val catalogItems = state.catalogItems.filter { it.category == selectedCategory }

    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("پالایشگاه", style = MaterialTheme.typography.headlineSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = section == 0, onClick = { section = 0 }, label = { Text("نقشه کمال") })
            FilterChip(selected = section == 1, onClick = { section = 1 }, label = { Text("کاتالوگ گنجینه") })
        }
        if (section == 0) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = selectedTrack == GrowthTrack.CAPABILITY, onClick = { track = GrowthTrack.CAPABILITY.name }, label = { Text("توانمندی") })
                FilterChip(selected = selectedTrack == GrowthTrack.SELF_IMPROVEMENT, onClick = { track = GrowthTrack.SELF_IMPROVEMENT.name }, label = { Text("خودسازی") })
            }
            Button(onClick = { editingId = null; title = ""; description = ""; priceText = ""; editorOpen = true }) { Text("افزودن آیتم") }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(growthItems, key = { it.id }) { item ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(item.title, style = MaterialTheme.typography.titleMedium)
                            item.description?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                            Text(if (item.isActive) "فعال" else "غیرفعال")
                            Row {
                                TextButton(onClick = { editingId = item.id; title = item.title; description = item.description.orEmpty(); priceText = item.priceAmount?.toString().orEmpty(); editorOpen = true }) { Text("ویرایش") }
                                TextButton(onClick = { viewModel.toggleGrowth(item) }) { Text(if (item.isActive) "غیرفعال‌سازی" else "فعال‌سازی") }
                            }
                        }
                    }
                }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(InventoryCategory.AVATAR to "آواتار", InventoryCategory.FRAME to "قاب", InventoryCategory.REWARD to "جایزه").forEach { (value, label) ->
                    FilterChip(selected = selectedCategory == value, onClick = { category = value.name }, label = { Text(label) })
                }
            }
            Button(onClick = { editingId = null; title = ""; description = ""; editorOpen = true }) { Text("افزودن آیتم") }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(catalogItems, key = { it.id }) { item ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(item.title, style = MaterialTheme.typography.titleMedium)
                            item.description?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                            Text(if (item.isAvailable) "در دسترس" else "غیرفعال")
                            Row {
                                TextButton(onClick = { editingId = item.id; title = item.title; description = item.description.orEmpty(); editorOpen = true }) { Text("ویرایش") }
                                TextButton(onClick = { viewModel.toggleCatalog(item) }) { Text(if (item.isAvailable) "غیرفعال‌سازی" else "فعال‌سازی") }
                            }
                        }
                    }
                }
            }
        }
    }

    if (editorOpen) {
        val isGrowth = section == 0
        val growthItem = state.growthItems.firstOrNull { it.id == editingId }
        val catalogItem = state.catalogItems.firstOrNull { it.id == editingId }
        AlertDialog(
            onDismissRequest = { editorOpen = false },
            title = { Text(if (isGrowth) "آیتم نقشه کمال" else "آیتم کاتالوگ") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(title, { title = it }, label = { Text("عنوان") }, singleLine = true)
                    OutlinedTextField(description, { description = it }, label = { Text("توضیحات (اختیاری)") }, minLines = 2)
                    if (!isGrowth) {
                        OutlinedTextField(priceText, { value -> priceText = value.filter(Char::isDigit) }, label = { Text(if (selectedCategory == InventoryCategory.REWARD) "قیمت (امتیاز)" else "قیمت (الماس)") }, singleLine = true)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (isGrowth) viewModel.saveGrowth(growthItem, selectedTrack, title, description)
                    else viewModel.saveCatalog(catalogItem, selectedCategory, title, description, priceText.toLongOrNull()?.takeIf { it > 0L })
                    editorOpen = false
                }) { Text("ذخیره") }
            },
            dismissButton = { TextButton(onClick = { editorOpen = false }) { Text("انصراف") } },
        )
    }
}
