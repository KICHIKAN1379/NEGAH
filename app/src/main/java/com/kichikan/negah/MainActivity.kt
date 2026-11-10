package com.kichikan.negah

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.Surface
import com.kichikan.negah.core.designsystem.NegahTheme
import com.kichikan.negah.feature.members.MembersViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val membersViewModel: MembersViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NegahTheme {
                Surface {
                    NegahRoot(membersViewModel)
                }
            }
        }
    }
}
