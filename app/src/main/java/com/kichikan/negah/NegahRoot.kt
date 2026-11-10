package com.kichikan.negah

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.kichikan.negah.feature.home.HomeScreen

private enum class MainDestination(val title: String) {
    HOME("خانه"),
    MEMBERS("اعضا"),
    PROGRESS("پیشرفت"),
    SHOP("فروشگاه"),
    SETTINGS("تنظیمات),
}

@Composable
fun NegahRoot() {
    var selected by rememberSaveable { mutableStateOf(MainDestination.HOME.name) }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                NavigationBar {
                    MainDestination.entries.forEach { destination ->
                        NavigationBarItem(
                            selected = selected == destination.name,
                            onClick = { selected = destination.name },
                            icon = { Text(destination.title.take(1)) },
                            label = { Text(destination.title) },
                        )
                    }
                }
            },
        ) { innerPadding ->
            when (MainDestination.valueOf(selected)) {
                MainDestination.HOME -> HomeScreen(Modifier.padding(innerPadding))
                else -> Text(
                    text = "بخش " + MainDestination.valueOf(selected).title + " در حال ساخت است",
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }
    }
}
