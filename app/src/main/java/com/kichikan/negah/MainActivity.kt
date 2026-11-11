package com.kichikan.negah

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
    private val sessionsViewModel: SessionsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                        sessionsViewModel,
                    )
                }
            }
        }
    }
}
