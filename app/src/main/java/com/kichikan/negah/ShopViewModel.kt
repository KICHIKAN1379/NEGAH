package com.kichikan.negah
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kichikan.negah.domain.inventory.*
import com.kichikan.negah.domain.member.Member
import com.kichikan.negah.domain.member.MemberRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
data class ShopUiState(val members: List<Member> = emptyList(), val selectedMemberId: String? = null, val items: List<CatalogItem> = emptyList(), val ownedItemIds: Set<String> = emptySet(), val category: InventoryCategory = InventoryCategory.AVATAR, val message: String? = null, val catalogImages: Map<String, ByteArray> = emptyMap())
@HiltViewModel
class ShopViewModel @Inject constructor(private val members: MemberRepository, private val inventory: InventoryRepository, private val catalogMedia: CatalogItemMediaRepository) : ViewModel() {
 private val mutableState = MutableStateFlow(ShopUiState())
 val state = mutableState.asStateFlow()
 private var memberJob: Job? = null
 private var observedMemberId: String? = null
 init {
  viewModelScope.launch { members.observeActiveMembers().collect { list ->
   val previous = mutableState.value.selectedMemberId
   val selected = previous?.takeIf { id -> list.any { it.id == id } } ?: list.firstOrNull()?.id
   mutableState.update { it.copy(members = list, selectedMemberId = selected) }
   if (selected != observedMemberId) {
    if (selected != null) observeMember(selected) else { memberJob?.cancel(); memberJob = null; observedMemberId = null; mutableState.update { it.copy(ownedItemIds = emptySet()) } }
   }
  } }
  viewModelScope.launch { combine(inventory.observeCatalog(InventoryCategory.AVATAR), inventory.observeCatalog(InventoryCategory.FRAME), inventory.observeCatalog(InventoryCategory.REWARD)) { a, f, r -> a + f + r }.collect { all ->
   mutableState.update { it.copy(items = all, catalogImages = it.catalogImages.filterKeys { id -> all.any { item -> item.id == id } }) }
   val images = all.mapNotNull { item ->
    runCatching { catalogMedia.loadCatalogImage(item.id) }.getOrNull()?.let { item.id to it }
   }.toMap()
   mutableState.update { it.copy(catalogImages = images) }
  } }
 }
 fun selectMember(id: String) { if (state.value.members.none { it.id == id }) return; mutableState.update { it.copy(selectedMemberId = id, message = null) }; if (observedMemberId != id) observeMember(id) }
 private fun observeMember(id: String) {
  memberJob?.cancel()
  observedMemberId = id
  memberJob = viewModelScope.launch {
   combine(inventory.observeMemberInventory(id, InventoryCategory.AVATAR), inventory.observeMemberInventory(id, InventoryCategory.FRAME), inventory.observeMemberInventory(id, InventoryCategory.REWARD)) { a, f, r -> (a + f + r).map { it.catalogItemId }.toSet() }
    .collect { owned -> mutableState.update { if (it.selectedMemberId == id) it.copy(ownedItemIds = owned) else it } }
  }
 }
 fun selectCategory(category: InventoryCategory) { mutableState.update { it.copy(category = category, message = null) } }
 fun purchase(itemId: String) {
  val current = state.value; val memberId = current.selectedMemberId ?: return
  val item = current.items.firstOrNull { it.id == itemId && it.category == current.category } ?: return
  viewModelScope.launch {
   val result = inventory.purchaseItem(memberId, item.category, item.id, UUID.randomUUID().toString(), System.currentTimeMillis())
   val message = when (result) {
    ShopPurchaseResult.PURCHASED -> "خرید انجام شد و آیتم به گنجینه عضو اضافه شد."
    ShopPurchaseResult.ITEM_UNAVAILABLE -> "این آیتم دیگر در دسترس نیست."
    ShopPurchaseResult.PRICE_NOT_SET -> "قیمت این آیتم هنوز در پالایشگاه تعیین نشده است."
    ShopPurchaseResult.ALREADY_OWNED -> "این آیتم قبلاً در گنجینه عضو ثبت شده است."
    ShopPurchaseResult.INSUFFICIENT_BALANCE -> "موجودی عضو برای خرید کافی نیست."
   }
   mutableState.update { it.copy(message = message) }
  }
 }
}
