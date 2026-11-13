package com.kichikan.negah

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.Surface
import com.kichikan.negah.core.designsystem.NegahTheme
import com.kichikan.negah.feature.members.MembersViewModel
import com.kichikan.negah.feature.refinery.RefineryViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val membersViewModel: MembersViewModel by viewModels()
    private val refineryViewModel: RefineryViewModel by viewModels()
    private val progressViewModel: ProgressViewModel by viewModels()
    private val shopViewModel: ShopViewModel by viewModels()
    private val classDaysViewModel: ClassDaysViewModel by viewModels()
    private val homeShortcutsViewModel: HomeShortcutsViewModel by viewModels()
    private val ringProfileViewModel: RingProfileViewModel by viewModels()
    private val sessionsViewModel: SessionsViewModel by viewModels()
    private val homeDashboardViewModel: HomeDashboardViewModel by viewModels()
    private val dailyQuotesViewModel: DailyQuotesViewModel by viewModels()
    private val readingViewModel: ReadingViewModel by viewModels()

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            // Re-run the check right after the coach grants notifications.
            if (granted) BirthdayReminderWorker.schedule(applicationContext)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !BirthdayReminderWorker.canNotify(this)) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            NegahTheme {
                Surface {
                    NegahRoot(
                        membersViewModel,
                        refineryViewModel,
                        progressViewModel,
                        shopViewModel,
                        classDaysViewModel,
                        homeShortcutsViewModel,
                        ringProfileViewModel,
                        sessionsViewModel,
                        homeDashboardViewModel,
                        dailyQuotesViewModel,
                        readingViewModel,
                    )
                }
            }
        }
    }
}
