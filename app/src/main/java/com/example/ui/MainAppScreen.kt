package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.media.UnifiedMediaStudioPipeline
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.ui.viewmodels.MainTab
import com.example.ui.viewmodels.VideoStudioViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    viewModel: VideoStudioViewModel
) {
    val currentTab by viewModel.currentTab.collectAsState()
    val activeProject by viewModel.activeProject.collectAsState()
    val userProfile by viewModel.userProfileState.collectAsState()
    val apiKeys by viewModel.apiKeysState.collectAsState()
    val highfieldSettings by viewModel.highfieldSettingsState.collectAsState()
    var showApiKeysDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        UnifiedMediaStudioPipeline.instance.initialize(context)
    }

    if (showApiKeysDialog) {
        com.example.ui.components.ApiKeysAndSettingsDialog(
            userProfile = userProfile,
            apiKeys = apiKeys,
            highfieldSettings = highfieldSettings,
            onDismiss = { showApiKeysDialog = false },
            onSaveProfileAndKeys = { updatedProfile, updatedKeys, updatedSettings ->
                viewModel.userProfileState.value = updatedProfile
                viewModel.apiKeysState.value = updatedKeys
                viewModel.highfieldSettingsState.value = updatedSettings
                com.example.data.api.ApiClient.setUserApiKey(updatedKeys.googleGeminiApiKey)
            }
        )
    }

    // Top-Level Device Back Navigation Handler
    BackHandler(enabled = currentTab != MainTab.PROJECTS_LIST && currentTab != MainTab.TIMELINE_EDITOR) {
        when (currentTab) {
            MainTab.EXPORT_STUDIO -> viewModel.selectTab(MainTab.TIMELINE_EDITOR)
            MainTab.STUDIO_GENERATOR, MainTab.STORYBOARD, MainTab.SETTINGS -> viewModel.selectTab(MainTab.PROJECTS_LIST)
            else -> viewModel.selectTab(MainTab.PROJECTS_LIST)
        }
    }

    FlowMonkeyTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        if (currentTab == MainTab.TIMELINE_EDITOR) {
                            IconButton(
                                onClick = { viewModel.selectTab(MainTab.PROJECTS_LIST) },
                                modifier = Modifier.testTag("timeline_back_to_projects_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Kembali ke Proyek",
                                    tint = StudioTextDark
                                )
                            }
                        }
                    },
                    title = {
                        Text(
                            text = if (currentTab == MainTab.TIMELINE_EDITOR) {
                                activeProject?.title ?: "Timeline Editor"
                            } else {
                                "FlowMonkey Studio"
                            },
                            color = StudioTextDark,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            letterSpacing = (-0.4).sp,
                            maxLines = 1
                        )
                    },
                    actions = {
                        if (currentTab == MainTab.TIMELINE_EDITOR) {
                            // Top-Right Export Button specifically for Timeline Tab
                            Button(
                                onClick = { viewModel.selectTab(MainTab.EXPORT_STUDIO) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = StudioDarkCTA,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(24.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .padding(end = 12.dp)
                                    .testTag("top_app_bar_export_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.IosShare,
                                    contentDescription = "Ekspor Video",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Ekspor",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        } else if (currentTab == MainTab.SETTINGS) {
                            // API Key Button only displayed on Settings screen
                            Surface(
                                onClick = { showApiKeysDialog = true },
                                shape = RoundedCornerShape(18.dp),
                                color = StudioPillBg,
                                border = BorderStroke(1.dp, StudioCardHairline),
                                modifier = Modifier
                                    .padding(end = 12.dp)
                                    .testTag("top_app_bar_api_key_button")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "API Key",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (apiKeys.googleGeminiApiKey.isNotBlank()) StudioEmeraldGreen else StudioElectricBlue
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            bottomBar = {
                // Hide bottom navigation bar when inside TIMELINE_EDITOR to give full screen width/height to timeline
                if (currentTab != MainTab.TIMELINE_EDITOR && currentTab != MainTab.EXPORT_STUDIO) {
                    NavigationBar(
                        containerColor = StudioGlassWhite,
                        contentColor = StudioTextDark,
                        tonalElevation = 6.dp,
                        modifier = Modifier
                            .testTag("main_bottom_navigation_bar")
                            .border(BorderStroke(1.dp, StudioCardHairline))
                    ) {
                        NavigationBarItem(
                            selected = currentTab == MainTab.STUDIO_GENERATOR,
                            onClick = { viewModel.selectTab(MainTab.STUDIO_GENERATOR) },
                            icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "Generator") },
                            label = { Text("Generator", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = StudioElectricBlue,
                                selectedTextColor = StudioElectricBlue,
                                unselectedIconColor = StudioTextMuted,
                                unselectedTextColor = StudioTextMuted,
                                indicatorColor = StudioElectricBlue.copy(alpha = 0.12f)
                            ),
                            modifier = Modifier.testTag("nav_item_generator")
                        )

                        NavigationBarItem(
                            selected = currentTab == MainTab.STORYBOARD,
                            onClick = { viewModel.selectTab(MainTab.STORYBOARD) },
                            icon = { Icon(Icons.Default.MovieFilter, contentDescription = "Storyboard") },
                            label = { Text("Storyboard", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = StudioElectricBlue,
                                selectedTextColor = StudioElectricBlue,
                                unselectedIconColor = StudioTextMuted,
                                unselectedTextColor = StudioTextMuted,
                                indicatorColor = StudioElectricBlue.copy(alpha = 0.12f)
                            ),
                            modifier = Modifier.testTag("nav_item_storyboard")
                        )

                        NavigationBarItem(
                            selected = currentTab == MainTab.PROJECTS_LIST,
                            onClick = { viewModel.selectTab(MainTab.PROJECTS_LIST) },
                            icon = { Icon(Icons.Default.VideoLibrary, contentDescription = "Proyek") },
                            label = { Text("Proyek", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = StudioElectricBlue,
                                selectedTextColor = StudioElectricBlue,
                                unselectedIconColor = StudioTextMuted,
                                unselectedTextColor = StudioTextMuted,
                                indicatorColor = StudioElectricBlue.copy(alpha = 0.12f)
                            ),
                            modifier = Modifier.testTag("nav_item_projects")
                        )

                        NavigationBarItem(
                            selected = currentTab == MainTab.SETTINGS,
                            onClick = { viewModel.selectTab(MainTab.SETTINGS) },
                            icon = { Icon(Icons.Default.Settings, contentDescription = "Pengaturan") },
                            label = { Text("Pengaturan", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = StudioElectricBlue,
                                selectedTextColor = StudioElectricBlue,
                                unselectedIconColor = StudioTextMuted,
                                unselectedTextColor = StudioTextMuted,
                                indicatorColor = StudioElectricBlue.copy(alpha = 0.12f)
                            ),
                            modifier = Modifier.testTag("nav_item_settings")
                        )
                    }
                }
            },
            containerColor = StudioCleanCanvas
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(StudioAmbientCanvasBrush)
            ) {
                when (currentTab) {
                    MainTab.STUDIO_GENERATOR -> QuickGenerateScreen(viewModel = viewModel)
                    MainTab.STORYBOARD -> StoryboardScreen(viewModel = viewModel)
                    MainTab.TIMELINE_EDITOR -> TimelineEditorScreen(viewModel = viewModel)
                    MainTab.EXPORT_STUDIO -> ExportStudioScreen(viewModel = viewModel)
                    MainTab.PROJECTS_LIST -> ProjectListScreen(viewModel = viewModel)
                    MainTab.SETTINGS -> SettingsScreen(viewModel = viewModel)
                }
            }
        }
    }
}
