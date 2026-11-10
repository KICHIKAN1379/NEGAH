package com.kichikan.negah.data.local.inventory

import androidx.room.Room
import com.kichikan.negah.data.local.NegahDatabase
import com.kichikan.negah.data.local.economy.EconomyTransactionEntity
import com.kichikan.negah.data.local.member.MemberEntity
import com.kichikan.negah.domain.inventory.InventoryCategory
import com.kichikan.negah.domain.inventory.ShopPurchaseResult
import com.kichikan.negah.domain.economy.LedgerAppendResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ShopDaoTest {
    private lateinit var database: NegahDatabase
    private lateinit var shop: ShopDao

    @Before
    fun setUp() = runBlocking {
        database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            NegahDatabase::class.java,
        ).allowMainThreadQueries().build()
        shop = database.shopDao()
        database.memberDao().insert(
            MemberEntity(
                id = MEMBER_ID,
                fullName = "Shop member",
                nickname = null,
                createdAtEpochMillis = 1L,
                updatedAtEpochMillis = 1L,
            ),
        )
        database.inventoryDao().upsertCatalogItem(
            CatalogItemEntity("reward-1", "REWARD", "جایزه", null, true, 0, 20L),
        )
        database.inventoryDao().upsertCatalogItem(
            CatalogItemEntity("avatar-1", "AVATAR", "آواتار", null, true, 1, 15L),
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun purchaseReward_spendsPointsAndAddsInventoryAtomically() = runBlocking {
        assertEquals(LedgerAppendResult.APPENDED, database.economyTransactionDao().appendAtomically(
            transaction("points-earn", "POINTS", 50L),
        ))

        val result = shop.purchase(MEMBER_ID, InventoryCategory.REWARD, "reward-1", "purchase-1", 10L)

        assertEquals(ShopPurchaseResult.PURCHASED, result)
        assertEquals(30L, database.memberDao().findById(MEMBER_ID)?.spendablePoints)
        assertEquals(1, database.inventoryDao().observeMemberInventory(MEMBER_ID, "REWARD").first().size)
        val ledger = database.economyTransactionDao().observeForMember(MEMBER_ID).first()
        assertEquals(2, ledger.size)
        assertEquals(-20L, ledger.first { it.id == "purchase-1" }.amount)
        assertEquals("STORE_PURCHASE", ledger.first { it.id == "purchase-1" }.sourceType)
    }

    @Test
    fun purchaseAvatar_spendsDiamondsAndNeverXp() = runBlocking {
        database.economyTransactionDao().appendAtomically(transaction("diamonds-earn", "DIAMONDS", 40L))
        database.economyTransactionDao().appendAtomically(transaction("xp-earn", "XP", 90L))

        val result = shop.purchase(MEMBER_ID, InventoryCategory.AVATAR, "avatar-1", "purchase-avatar", 11L)

        assertEquals(ShopPurchaseResult.PURCHASED, result)
        val member = database.memberDao().findById(MEMBER_ID)!!
        assertEquals(25L, member.diamonds)
        assertEquals(90L, member.totalXp)
    }

    @Test
    fun insufficientBalance_doesNotWriteLedgerOrInventory() = runBlocking {
        database.economyTransactionDao().appendAtomically(transaction("points-earn", "POINTS", 10L))

        val result = shop.purchase(MEMBER_ID, InventoryCategory.REWARD, "reward-1", "purchase-fail", 12L)

        assertEquals(ShopPurchaseResult.INSUFFICIENT_BALANCE, result)
        assertEquals(10L, database.memberDao().findById(MEMBER_ID)?.spendablePoints)
        assertTrue(database.economyTransactionDao().observeForMember(MEMBER_ID).first().none { it.id == "purchase-fail" })
        assertTrue(database.inventoryDao().observeMemberInventory(MEMBER_ID, "REWARD").first().isEmpty())
    }

    @Test
    fun secondPurchaseOfSameItem_isRejected() = runBlocking {
        database.economyTransactionDao().appendAtomically(transaction("points-earn", "POINTS", 100L))
        assertEquals(ShopPurchaseResult.PURCHASED, shop.purchase(MEMBER_ID, InventoryCategory.REWARD, "reward-1", "purchase-1", 20L))

        val duplicate = shop.purchase(MEMBER_ID, InventoryCategory.REWARD, "reward-1", "purchase-2", 21L)

        assertEquals(ShopPurchaseResult.ALREADY_OWNED, duplicate)
        assertEquals(80L, database.memberDao().findById(MEMBER_ID)?.spendablePoints)
        assertEquals(2, database.economyTransactionDao().observeForMember(MEMBER_ID).first().size)
    }

    private fun transaction(id: String, currency: String, amount: Long) = EconomyTransactionEntity(
        id = id,
        memberId = MEMBER_ID,
        currency = currency,
        amount = amount,
        transactionType = "EARN",
        sourceType = "TEST",
        sourceId = null,
        createdAtEpochMillis = 2L,
    )

    private companion object {
        const val MEMBER_ID = "shop-member"
    }
}
