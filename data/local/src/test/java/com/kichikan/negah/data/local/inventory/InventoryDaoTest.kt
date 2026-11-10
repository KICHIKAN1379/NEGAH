package com.kichikan.negah.data.local.inventory

import androidx.room.Room
import com.kichikan.negah.data.local.NegahDatabase
import com.kichikan.negah.data.local.member.MemberEntity
import com.kichikan.negah.domain.inventory.InventoryCategory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class InventoryDaoTest {
    private lateinit var database: NegahDatabase
    private lateinit var dao: InventoryDao

    @Before
    fun setUp() = runBlocking {
        database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            NegahDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = database.inventoryDao()
        database.memberDao().insert(
            MemberEntity(
                id = MEMBER_ID,
                fullName = "Test member",
                nickname = null,
                createdAtEpochMillis = 1L,
                updatedAtEpochMillis = 1L,
            ),
        )
        dao.upsertCatalogItem(
            CatalogItemEntity("avatar-1", "AVATAR", "آواتار یک", null, true, 0),
        )
        dao.upsertCatalogItem(
            CatalogItemEntity("frame-1", "FRAME", "قاب یک", null, true, 1),
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun grantAvailableCatalogItemAddsInventoryEntryWithItsGrantSource() = runBlocking {
        val entry = MemberInventoryEntity(MEMBER_ID, "avatar-1", 5L, "COACH_GRANT", "grant-1")

        val granted = dao.grantAvailableItem(entry, InventoryCategory.AVATAR)

        assertTrue(granted)
        val stored = dao.observeMemberInventory(MEMBER_ID, "AVATAR").first().single()
        assertEquals("avatar-1", stored.catalogItemId)
        assertEquals("COACH_GRANT", stored.sourceType)
        assertEquals("grant-1", stored.sourceId)
    }

    @Test
    fun grantRejectsUnavailableCatalogItem() = runBlocking {
        dao.upsertCatalogItem(CatalogItemEntity("avatar-disabled", "AVATAR", "غیرفعال", null, false, 2))

        val granted = dao.grantAvailableItem(
            MemberInventoryEntity(MEMBER_ID, "avatar-disabled", 5L, "COACH_GRANT", "grant-2"),
            InventoryCategory.AVATAR,
        )

        assertFalse(granted)
        assertTrue(dao.observeMemberInventory(MEMBER_ID, "AVATAR").first().isEmpty())
    }

    @Test
    fun grantRejectsCatalogItemFromDifferentCategory() = runBlocking {
        val granted = dao.grantAvailableItem(
            MemberInventoryEntity(MEMBER_ID, "frame-1", 5L, "COACH_GRANT", "grant-3"),
            InventoryCategory.AVATAR,
        )

        assertFalse(granted)
        assertTrue(dao.observeMemberInventory(MEMBER_ID, "AVATAR").first().isEmpty())
    }

    @Test
    fun activationIsRejectedWhenMemberDoesNotOwnItem() = runBlocking {
        val activated = dao.activateOwnedItem(MEMBER_ID, InventoryCategory.AVATAR, "avatar-1", 10L)

        assertFalse(activated)
        assertTrue(dao.observeActiveItems(MEMBER_ID).first().isEmpty())
    }

    @Test
    fun activationRejectsItemFromDifferentCategory() = runBlocking {
        dao.insertInventoryEntry(MemberInventoryEntity(MEMBER_ID, "frame-1", 5L, "TEST", null))

        val activated = dao.activateOwnedItem(MEMBER_ID, InventoryCategory.AVATAR, "frame-1", 10L)

        assertFalse(activated)
        assertTrue(dao.observeActiveItems(MEMBER_ID).first().isEmpty())
    }

    @Test
    fun ownedItemCanBeActivatedAndRemainsInInventory() = runBlocking {
        dao.insertInventoryEntry(MemberInventoryEntity(MEMBER_ID, "avatar-1", 5L, "REWARD", "grant-1"))

        val activated = dao.activateOwnedItem(MEMBER_ID, InventoryCategory.AVATAR, "avatar-1", 10L)

        assertTrue(activated)
        assertEquals("avatar-1", dao.observeActiveItems(MEMBER_ID).first().single().catalogItemId)
        assertEquals(1, dao.observeMemberInventory(MEMBER_ID, "AVATAR").first().size)
    }

    private companion object {
        const val MEMBER_ID = "member-1"
    }
}
