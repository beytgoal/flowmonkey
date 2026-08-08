package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val transcodingJobs by viewModel.transcodingJobs.collectAsState()
    val activeProject by viewModel.activeProject.collectAsState()

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
                                    tint = StudioTextPrimary
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
                            color = StudioTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            letterSpacing = (-0.3).sp,
                            maxLines = 1
                        )
                    },
                    actions = {
                        if (currentTab == MainTab.TIMELINE_EDITOR) {
                            // Top-Right Export Button specifically for Timeline Tab
                            Button(
                                onClick = { viewModel.selectTab(MainTab.EXPORT_STUDIO) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = StudioSecondaryTeal,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(20.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
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
                        } else {
                            // Settings Button for other tabs
                            IconButton(
                                onClick = { viewModel.selectTab(MainTab.SETTINGS) },
                                modifier = Modifier.testTag("settings_proxy_toggle_button")
                            ) {
                                BadgedBox(
                                    badge = {
                                        val activeCount = transcodingJobs.count { !it.isCompleted }
                                        if (activeCount > 0) {
                                            Badge(containerColor = StudioSecondaryTeal) {
                                                Text("$activeCount", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Pengaturan",
                                        tint = if (currentTab == MainTab.SETTINGS) StudioPrimaryViolet else StudioTextSecondary
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = StudioDarkBg)
                )
            },
            bottomBar = {
                // Hide bottom navigation bar when inside TIMELINE_EDITOR to give full screen width/height to timeline
                if (currentTab != MainTab.TIMELINE_EDITOR && currentTab != MainTab.EXPORT_STUDIO) {
                    NavigationBar(
                        containerColor = StudioSurfaceDark,
                        contentColor = StudioTextPrimary,
                        tonalElevation = 8.dp,
                        modifier = Modifier.testTag("main_bottom_navigation_bar")
                    ) {
                        NavigationBarItem(
                            selected = currentTab == MainTab.STUDIO_GENERATOR,
                            onClick = { viewModel.selectTab(MainTab.STUDIO_GENERATOR) },
                            icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "Generator") },
                            label = { Text("Generator", fontSize = 10.sp, fontWeight = FontWeight.Medium) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = StudioPrimaryViolet,
                                selectedTextColor = StudioPrimaryViolet,
                                unselectedIconColor = StudioTextSecondary,
                                unselectedTextColor = StudioTextSecondary,
                                indicatorColor = StudioPrimaryViolet.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier.testTag("nav_item_generator")
                        )

                        NavigationBarItem(
                            selected = currentTab == MainTab.STORYBOARD,
                            onClick = { viewModel.selectTab(MainTab.STORYBOARD) },
                            icon = { Icon(Icons.Default.MovieFilter, contentDescription = "Storyboard") },
                            label = { Text("Storyboard", fontSize = 10.sp, fontWeight = FontWeight.Medium) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = StudioPrimaryViolet,
                                selectedTextColor = StudioPrimaryViolet,
                                unselectedIconColor = StudioTextSecondary,
                                unselectedTextColor = StudioTextSecondary,
                                indicatorColor = StudioPrimaryViolet.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier.testTag("nav_item_storyboard")
                        )

                        NavigationBarItem(
                            selected = currentTab == MainTab.PROJECTS_LIST,
                            onClick = { viewModel.selectTab(MainTab.PROJECTS_LIST) },
                            icon = { Icon(Icons.Default.VideoLibrary, contentDescription = "Proyek") },
                            label = { Text("Proyek", fontSize = 10.sp, fontWeight = FontWeight.Medium) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = StudioPrimaryViolet,
                                selectedTextColor = StudioPrimaryViolet,
                                unselectedIconColor = StudioTextSecondary,
                                unselectedTextColor = StudioTextSecondary,
                                indicatorColor = StudioPrimaryViolet.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier.testTag("nav_item_projects")
                        )

                        NavigationBarItem(
                            selected = currentTab == MainTab.SETTINGS,
                            onClick = { viewModel.selectTab(MainTab.SETTINGS) },
                            icon = { Icon(Icons.Default.Settings, contentDescription = "Pengaturan") },
                            label = { Text("Pengaturan", fontSize = 10.sp, fontWeight = FontWeight.Medium) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = StudioPrimaryViolet,
                                selectedTextColor = StudioPrimaryViolet,
                                unselectedIconColor = StudioTextSecondary,
                                unselectedTextColor = StudioTextSecondary,
                                indicatorColor = StudioPrimaryViolet.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier.testTag("nav_item_settings")
                        )
                    }
                }
            },
            containerColor = StudioDarkBg
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(StudioDarkBg)
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
