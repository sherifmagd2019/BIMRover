package com.example.navigation

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.GpsFixed
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.BimSiteMapScreen
import com.example.ui.screens.PaywallScreen
import com.example.ui.screens.RevitCalibrationScreen
import com.example.ui.screens.StakedHistoryScreen
import com.example.ui.screens.StakeoutGuidanceScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SurveyorGold
import com.example.viewmodel.BimSurveyorViewModel

enum class BimNavTab(val title: String) {
    STAKEOUT("Stakeout"),
    MAP("Site Map"),
    CALIBRATION("Revit Anchor"),
    HISTORY("As-Built Log"),
    LICENSE("Pro License")
}

/**
 * Main Application Navigation with RevenueCat Subscription Interceptor.
 *
 * Verifies active "pro_access" entitlement before granting access to the
 * BIM Surveyor Stakeout module.
 */
@Composable
fun BimAppNavigation(
    viewModel: BimSurveyorViewModel,
    modifier: Modifier = Modifier
) {
    val customerInfo by viewModel.revenueCatManager.customerInfo.collectAsState()
    val isSunGlaze by viewModel.isSunGlazeMode.collectAsState()
    var currentTab by remember { mutableStateOf(BimNavTab.STAKEOUT) }
    var forceShowPaywallInTab by remember { mutableStateOf(false) }

    MyApplicationTheme(sunGlazeMode = isSunGlaze) {
        // SUBSCRIPTION INTERCEPTOR:
        // If unauthorized, redirect immediately to the Paywall screen.
        if (!customerInfo.hasProAccess) {
            PaywallScreen(
                revenueCatManager = viewModel.revenueCatManager,
                onProAccessGranted = {
                    currentTab = BimNavTab.STAKEOUT
                },
                modifier = modifier
            )
        } else {
            // Authorized User Dashboard
            Scaffold(
                modifier = modifier.fillMaxSize(),
                bottomBar = {
                    NavigationBar(
                        modifier = Modifier
                            .navigationBarsPadding()
                            .testTag("surveyor_bottom_nav"),
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 6.dp
                    ) {
                        NavigationBarItem(
                            selected = (currentTab == BimNavTab.STAKEOUT),
                            onClick = { currentTab = BimNavTab.STAKEOUT },
                            icon = {
                                Icon(
                                    imageVector = if (currentTab == BimNavTab.STAKEOUT) Icons.Filled.GpsFixed else Icons.Outlined.GpsFixed,
                                    contentDescription = "Stakeout"
                                )
                            },
                            label = { Text(BimNavTab.STAKEOUT.title, fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = ColorTextPrimary(),
                                indicatorColor = SurveyorGold
                            ),
                            modifier = Modifier.testTag("nav_tab_stakeout")
                        )

                        NavigationBarItem(
                            selected = (currentTab == BimNavTab.MAP),
                            onClick = { currentTab = BimNavTab.MAP },
                            icon = {
                                Icon(
                                    imageVector = if (currentTab == BimNavTab.MAP) Icons.Filled.Map else Icons.Outlined.Map,
                                    contentDescription = "Site Map"
                                )
                            },
                            label = { Text(BimNavTab.MAP.title, fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = ColorTextPrimary(),
                                indicatorColor = SurveyorGold
                            ),
                            modifier = Modifier.testTag("nav_tab_map")
                        )

                        NavigationBarItem(
                            selected = (currentTab == BimNavTab.CALIBRATION),
                            onClick = { currentTab = BimNavTab.CALIBRATION },
                            icon = {
                                Icon(
                                    imageVector = if (currentTab == BimNavTab.CALIBRATION) Icons.Filled.Tune else Icons.Outlined.Tune,
                                    contentDescription = "Calibration"
                                )
                            },
                            label = { Text(BimNavTab.CALIBRATION.title, fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = ColorTextPrimary(),
                                indicatorColor = SurveyorGold
                            ),
                            modifier = Modifier.testTag("nav_tab_calibration")
                        )

                        NavigationBarItem(
                            selected = (currentTab == BimNavTab.HISTORY),
                            onClick = { currentTab = BimNavTab.HISTORY },
                            icon = {
                                Icon(
                                    imageVector = if (currentTab == BimNavTab.HISTORY) Icons.Filled.Assessment else Icons.Outlined.Assessment,
                                    contentDescription = "As-Built Log"
                                )
                            },
                            label = { Text(BimNavTab.HISTORY.title, fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = ColorTextPrimary(),
                                indicatorColor = SurveyorGold
                            ),
                            modifier = Modifier.testTag("nav_tab_history")
                        )

                        NavigationBarItem(
                            selected = (currentTab == BimNavTab.LICENSE),
                            onClick = { currentTab = BimNavTab.LICENSE },
                            icon = {
                                Icon(
                                    imageVector = if (currentTab == BimNavTab.LICENSE) Icons.Filled.VerifiedUser else Icons.Outlined.VerifiedUser,
                                    contentDescription = "License"
                                )
                            },
                            label = { Text(BimNavTab.LICENSE.title, fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = ColorTextPrimary(),
                                indicatorColor = SurveyorGold
                            ),
                            modifier = Modifier.testTag("nav_tab_license")
                        )
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    Crossfade(targetState = currentTab, label = "tab_crossfade") { tab ->
                        when (tab) {
                            BimNavTab.STAKEOUT -> StakeoutGuidanceScreen(
                                viewModel = viewModel,
                                onNavigateToCalibration = { currentTab = BimNavTab.CALIBRATION },
                                onNavigateToMap = { currentTab = BimNavTab.MAP }
                            )

                            BimNavTab.MAP -> BimSiteMapScreen(
                                viewModel = viewModel,
                                onNavigateToStakeout = { currentTab = BimNavTab.STAKEOUT },
                                onNavigateToCalibration = { currentTab = BimNavTab.CALIBRATION }
                            )

                            BimNavTab.CALIBRATION -> RevitCalibrationScreen(
                                viewModel = viewModel,
                                onNavigateToStakeout = { currentTab = BimNavTab.STAKEOUT }
                            )

                            BimNavTab.HISTORY -> StakedHistoryScreen(
                                viewModel = viewModel
                            )

                            BimNavTab.LICENSE -> PaywallScreen(
                                revenueCatManager = viewModel.revenueCatManager,
                                onProAccessGranted = {
                                    currentTab = BimNavTab.STAKEOUT
                                },
                                onDismiss = {
                                    currentTab = BimNavTab.STAKEOUT
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorTextPrimary() = androidx.compose.ui.graphics.Color(0xFF101418)
