package com.kichikan.negah.feature.members

import com.kichikan.negah.domain.economy.EconomyCurrency
import com.kichikan.negah.domain.economy.EconomyRepository
import com.kichikan.negah.domain.economy.EconomyTransaction
import com.kichikan.negah.domain.economy.EconomyTransactionKind
import com.kichikan.negah.domain.member.Member
import com.kichikan.negah.domain.member.MemberRepository
import com.kichikan.negah.domain.member.RegisterMember
import com.kichikan.negah.domain.growth.GrowthItem
import com.kichikan.negah.domain.growth.GrowthMapRepository
import com.kichikan.negah.domain.growth.GrowthTrack
import com.kichikan.negah.domain.growth.MemberGrowthProgress
import com.kichikan.negah.domain.growth.UpdateGrowthProgress
import com.kichikan.negah.domain.inventory.ActiveInventoryItem
import com.kichikan.negah.domain.inventory.CatalogItem
import com.kichikan.negah.domain.inventory.InventoryCategory
import com.kichikan.negah.domain.inventory.InventoryRepository
import com.kichikan.negah.domain.inventory.MemberInventoryEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MembersViewModelTest {
    private val mainDispatcher = UnconfinedTestDispatcher()
    private lateinit var members: FakeMemberRepository
    private lateinit var economy: FakeEconomyRepository
    private lateinit var growth: FakeGrowthMapRepository
    private lateinit var inventory: FakeInventoryRepository
    private lateinit var viewModel: MembersViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
        members = FakeMemberRepository()
        economy = FakeEconomyRepository()
        growth = FakeGrowthMapRepository()
        inventory = FakeInventoryRepository()
        viewModel = MembersViewModel(
            members,
            RegisterMember(members),
            economy,
            growth,
            UpdateGrowthProgress(growth),
            inventory,
        )
    }

    @After
    fun tearDown() {
        viewModel.onProfileClosed()
        Dispatchers.resetMain()
    }

    @Test
    fun visibleMembers_filtersByFullNameAndNickname_caseInsensitively() = runTest {
        members.emit(listOf(member("1", "آرمان رضایی", "آرمانک"), member("2", "سارا احمدی", null)))
        viewModel.onSearchChanged("  آرمانک  ")

        assertEquals(listOf("1"), viewModel.state.value.visibleMembers.map(Member::id))
    }

    @Test
    fun visibleMembers_canSwitchToArchivedCollection() = runTest {
        members.emit(listOf(member("active", "عضو فعال")))
        viewModel.showArchivedMembers(true)
        assertEquals(emptyList<String>(), viewModel.state.value.visibleMembers.map(Member::id))
    }

    @Test
    fun receivedHistoryFilterShowsOnlySelectedCurrency() = runTest {
        val xp = transaction("xp", "member-1", 10).copy(currency = EconomyCurrency.XP)
        val points = transaction("points", "member-1", 20).copy(currency = EconomyCurrency.POINTS)
        val diamonds = transaction("diamonds", "member-1", 2).copy(currency = EconomyCurrency.DIAMONDS)
        economy.emit("member-1", listOf(xp, points, diamonds))
        viewModel.onMemberSelected("member-1")

        viewModel.onHistoryFilterChanged(MemberHistoryFilter.XP)
        assertEquals(listOf(xp), viewModel.state.value.visibleEconomyTransactions)

        viewModel.onHistoryFilterChanged(MemberHistoryFilter.POINTS)
        assertEquals(listOf(points), viewModel.state.value.visibleEconomyTransactions)

        viewModel.onHistoryFilterChanged(MemberHistoryFilter.DIAMONDS)
        assertEquals(listOf(diamonds), viewModel.state.value.visibleEconomyTransactions)

        viewModel.onHistoryFilterChanged(MemberHistoryFilter.ALL)
        assertEquals(listOf(xp, points, diamonds), viewModel.state.value.visibleEconomyTransactions)
    }

    @Test
    fun visibleMembers_sortsByLevelThenXp() = runTest {
        members.emit(
            listOf(
                member("low", "الف", xp = 19),
                member("levelOneLowXp", "ب", xp = 20),
                member("levelOneHighXp", "پ", xp = 49),
            ),
        )
        viewModel.onSortChanged(MemberSortOption.LEVEL)

        assertEquals(
            listOf("levelOneHighXp", "levelOneLowXp", "low"),
            viewModel.state.value.visibleMembers.map(Member::id),
        )
    }

    @Test
    fun visibleMembers_sortsBySpendablePoints() = runTest {
        members.emit(
            listOf(
                member("a", "الف", points = 3),
                member("b", "ب", points = 12),
                member("c", "پ", points = 7),
            ),
        )
        viewModel.onSortChanged(MemberSortOption.POINTS)

        assertEquals(listOf("b", "c", "a"), viewModel.state.value.visibleMembers.map(Member::id))
    }

    @Test
    fun coachGrantAddsCatalogItemToSelectedMembersInventory() = runTest {
        val item = CatalogItem(
            id = "avatar-1",
            category = InventoryCategory.AVATAR,
            title = "آواتار آزمایشی",
            description = null,
            isAvailable = true,
            sortOrder = 0,
        )
        viewModel.onMemberSelected("member-1")

        viewModel.grantCatalogItem(item)

        val granted = inventory.grantedEntries.single()
        assertEquals("member-1", granted.memberId)
        assertEquals("avatar-1", granted.catalogItemId)
        assertEquals("COACH_GRANT", granted.sourceType)
        assertEquals("آیتم به گنجینه عضو افزوده شد", viewModel.state.value.inventoryMessage)
    }

    @Test
    fun selectingMemberObservesOnlyThatMembersEconomyHistory() = runTest {
        val first = transaction("tx-1", "member-1", 10)
        val second = transaction("tx-2", "member-2", 20)
        economy.emit("member-1", listOf(first))
        economy.emit("member-2", listOf(second))

        viewModel.onMemberSelected("member-1")
        assertEquals(listOf(first), viewModel.state.value.economyTransactions)

        viewModel.onMemberSelected("member-2")
        assertEquals("member-2", viewModel.state.value.selectedMemberId)
        assertEquals(listOf(second), viewModel.state.value.economyTransactions)
    }

    @Test
    fun clearingBirthdayFromProfilePersistsNullDate() = runTest {
        viewModel.onMemberSelected("member-1")

        viewModel.updateSelectedMemberBirthDate("")

        assertEquals("member-1", members.updatedBirthDateMemberId)
        assertNull(members.updatedBirthDate)
        assertEquals("تاریخ تولد ذخیره شد", viewModel.state.value.profileMessage)
    }

    @Test
    fun closingProfileClearsSelectionAndHistory() = runTest {
        val entry = transaction("tx-1", "member-1", 10)
        economy.emit("member-1", listOf(entry))
        viewModel.onMemberSelected("member-1")

        viewModel.onProfileClosed()

        assertNull(viewModel.state.value.selectedMemberId)
        assertEquals(emptyList<EconomyTransaction>(), viewModel.state.value.economyTransactions)
    }

    private fun member(
        id: String,
        name: String,
        nickname: String? = null,
        xp: Long = 0,
        points: Long = 0,
    ) = Member(
        id = id,
        fullName = name,
        nickname = nickname,
        totalXp = xp,
        spendablePoints = points,
        diamonds = 0,
        createdAtEpochMillis = 1,
        updatedAtEpochMillis = 1,
        isArchived = false,
    )

    private fun transaction(id: String, memberId: String, amount: Long) = EconomyTransaction(
        id = id,
        memberId = memberId,
        currency = EconomyCurrency.POINTS,
        amountDelta = amount,
        kind = EconomyTransactionKind.EARN,
        sourceType = "test",
        sourceId = null,
        createdAtEpochMillis = 1,
    )
}

private class FakeMemberRepository : MemberRepository {
    private val data = MutableStateFlow<List<Member>>(emptyList())
    fun emit(value: List<Member>) { data.value = value }
    override fun observeActiveMembers(): Flow<List<Member>> = data.asStateFlow()
    override fun observeArchivedMembers(): Flow<List<Member>> = MutableStateFlow(emptyList<Member>()).asStateFlow()
    var updatedBirthDateMemberId: String? = null
    var updatedBirthDate: String? = "not-updated"
    override suspend fun create(member: Member) = Unit
    override suspend fun updateBirthDate(memberId: String, birthDateJalali: String?, updatedAtEpochMillis: Long) {
        updatedBirthDateMemberId = memberId
        updatedBirthDate = birthDateJalali
    }
    override suspend fun archive(memberId: String) = Unit
    override suspend fun restore(memberId: String) = Unit
}

private class FakeEconomyRepository : EconomyRepository {
    private val histories = mutableMapOf<String, MutableStateFlow<List<EconomyTransaction>>>()
    fun emit(memberId: String, value: List<EconomyTransaction>) {
        histories.getOrPut(memberId) { MutableStateFlow(emptyList()) }.value = value
    }
    override fun observeForMember(memberId: String): Flow<List<EconomyTransaction>> =
        histories.getOrPut(memberId) { MutableStateFlow(emptyList()) }.asStateFlow()
    override suspend fun appendAtomically(transaction: EconomyTransaction) =
        com.kichikan.negah.domain.economy.LedgerAppendResult.APPENDED
}


private class FakeGrowthMapRepository : GrowthMapRepository {
    override fun observeAllItems(track: GrowthTrack): Flow<List<GrowthItem>> = MutableStateFlow(emptyList<GrowthItem>()).asStateFlow()

    override fun observeItems(track: GrowthTrack): Flow<List<GrowthItem>> =
        MutableStateFlow(emptyList<GrowthItem>()).asStateFlow()

    override fun observeProgress(memberId: String): Flow<List<MemberGrowthProgress>> =
        MutableStateFlow(emptyList<MemberGrowthProgress>()).asStateFlow()

    override suspend fun saveItem(item: GrowthItem) = Unit
    override suspend fun updateProgress(progress: MemberGrowthProgress) = Unit
}

private class FakeInventoryRepository : InventoryRepository {
    val grantedEntries = mutableListOf<MemberInventoryEntry>()
    override fun observeAllCatalog(category: InventoryCategory): Flow<List<CatalogItem>> = MutableStateFlow(emptyList<CatalogItem>()).asStateFlow()

    override fun observeCatalog(category: InventoryCategory): Flow<List<CatalogItem>> =
        MutableStateFlow(emptyList<CatalogItem>()).asStateFlow()

    override fun observeMemberInventory(
        memberId: String,
        category: InventoryCategory,
    ): Flow<List<MemberInventoryEntry>> =
        MutableStateFlow(emptyList<MemberInventoryEntry>()).asStateFlow()

    override fun observeActiveItems(memberId: String): Flow<List<ActiveInventoryItem>> =
        MutableStateFlow(emptyList<ActiveInventoryItem>()).asStateFlow()

    override suspend fun saveCatalogItem(item: CatalogItem) = Unit
    override suspend fun grantItem(entry: MemberInventoryEntry) { grantedEntries += entry }
    override suspend fun activateItem(
        memberId: String,
        category: InventoryCategory,
        catalogItemId: String,
        nowEpochMillis: Long,
    ): Boolean = false

    override suspend fun purchaseItem(
        memberId: String,
        category: InventoryCategory,
        catalogItemId: String,
        transactionId: String,
        nowEpochMillis: Long,
    ) = com.kichikan.negah.domain.inventory.ShopPurchaseResult.ITEM_UNAVAILABLE
}
