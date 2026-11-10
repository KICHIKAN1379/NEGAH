package com.kichikan.negah

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kichikan.negah.feature.home.HomeScreen
import com.kichikan.negah.feature.members.MembersScreen
import com.kichikan.negah.feature.members.MembersViewModel
import com.kichikan.negah.feature.refinery.RefineryScreen
import com.kichikan.negah.feature.refinery.RefineryViewModel

private enum class MainDestination(val title: String) {
    HOME("خانه"), MEMBERS("اعضا"), PROGRESS("پیشرفت"), SHOP("فروشگاه"), SETTINGS("تنظیمات"),
}

@Composable
fun NegahRoot(
    membersViewModel: MembersViewModel,
    refineryViewModel: RefineryViewModel,
    progressViewModel: ProgressViewModel,
    shopViewModel: ShopViewModel,
    classDaysViewModel: ClassDaysViewModel,
    sessionsViewModel: SessionsViewModel,
) {
    var selected by rememberSaveable { mutableStateOf(MainDestination.HOME.name) }
    var openRefinery by rememberSaveable { mutableStateOf(false) }
    var openClassDays by rememberSaveable { mutableStateOf(false) }
    var openSessions by rememberSaveable { mutableStateOf(false) }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(modifier = Modifier.fillMaxSize(), bottomBar = {
            NavigationBar {
                MainDestination.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = selected == destination.name,
                        onClick = {
                            selected = destination.name
                            openRefinery = false
                            openClassDays = false
                            openSessions = false
                        },
                        icon = { Text(destination.title.take(1)) },
                        label = { Text(destination.title) },
                    )
                }
            }
        }) { padding ->
            when (MainDestination.valueOf(selected)) {
                MainDestination.HOME -> HomeScreen(Modifier.padding(padding))
                MainDestination.MEMBERS -> MembersScreen(membersViewModel, Modifier.padding(padding))
                MainDestination.PROGRESS -> {
                    if (openSessions) {
                        SessionsScreen(
                            viewModel = sessionsViewModel,
                            onBack = { openSessions = false },
                            onOpenClassDays = {
                                selected = MainDestination.SETTINGS.name
                                openSessions = false
                                openRefinery = false
                                openClassDays = true
                            },
                            modifier = Modifier.padding(padding),
                        )
                    } else {
                        ProgressScreen(
                            viewModel = progressViewModel,
                            onOpenSessions = { openSessions = true },
                            modifier = Modifier.padding(padding),
                        )
                    }
                }
                MainDestination.SETTINGS -> {
                    when {
                        openRefinery -> RefineryScreen(refineryViewModel, Modifier.padding(padding))
                        openClassDays -> ClassDaysScreen(
                            viewModel = classDaysViewModel,
                            onBack = { openClassDays = false },
                            modifier = Modifier.padding(padding),
                        )
                        else -> {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Text("تنظیمات", style = MaterialTheme.typography.headlineSmall)
                                Button(onClick = { openRefinery = true }, modifier = Modifier.fillMaxWidth()) {
                                    Text("پالایشگاه")
                                }
                                Button(onClick = { openClassDays = true }, modifier = Modifier.fillMaxWidth()) {
                                    Text("روزهای کلاس")
                                }
                            }
                        }
                    }
                }
                MainDestination.SHOP -> ShopScreen(shopViewModel, Modifier.padding(padding))
            }
        }
    }
}
