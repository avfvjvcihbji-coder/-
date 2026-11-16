package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesomeMosaic
import androidx.compose.material.icons.filled.DownloadForOffline
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AutoAwesomeMosaic
import androidx.compose.material.icons.outlined.DownloadForOffline
import androidx.compose.material.icons.outlined.MovieCreation
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.motion.ExportFormat
import com.example.ui.ExportScreen
import com.example.ui.ScenesScreen
import com.example.ui.SettingsScreen
import com.example.ui.StudioScreen
import com.example.ui.theme.CharcoalBlack
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.MotionStudioTheme
import com.example.ui.theme.NoticeAmberBg
import com.example.ui.theme.NoticeAmberInk
import com.example.ui.theme.SurfaceCardBorder
import com.example.ui.theme.SurfaceStudioGray
import com.example.viewmodel.MotionStudioViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MotionStudioTheme {
                MotionStudioApp()
            }
        }
    }
}

private sealed class StudioRoute(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    data object Studio : StudioRoute(
        "studio", R.string.tab_studio,
        Icons.Filled.MovieCreation, Icons.Outlined.MovieCreation, "nav_studio"
    )
    data object Scenes : StudioRoute(
        "scenes", R.string.tab_scenes,
        Icons.Filled.AutoAwesomeMosaic, Icons.Outlined.AutoAwesomeMosaic, "nav_scenes"
    )
    data object Export : StudioRoute(
        "export", R.string.tab_export,
        Icons.Filled.DownloadForOffline, Icons.Outlined.DownloadForOffline, "nav_export"
    )
    data object Settings : StudioRoute(
        "settings", R.string.tab_settings,
        Icons.Filled.Settings, Icons.Outlined.Settings, "nav_settings"
    )

    companion object {
        val all = listOf(Studio, Scenes, Export, Settings)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MotionStudioApp(viewModel: MotionStudioViewModel = viewModel()) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val context = LocalContext.current

    val scriptText by viewModel.scriptText.collectAsStateWithLifecycle()
    val videoSettings by viewModel.videoSettings.collectAsStateWithLifecycle()
    val providerConfig by viewModel.providerConfig.collectAsStateWithLifecycle()
    val generationState by viewModel.generationState.collectAsStateWithLifecycle()
    val selectedSceneIndex by viewModel.selectedSceneIndex.collectAsStateWithLifecycle()
    val currentFrame by viewModel.currentFrame.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val previewBackground by viewModel.previewBackground.collectAsStateWithLifecycle()
    val previewResolution by viewModel.previewResolution.collectAsStateWithLifecycle()
    val exportState by viewModel.exportState.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(toastMessage) {
        val msg = toastMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.consumeToast()
        }
    }

    fun navigateTo(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 700.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = Color.White,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                CenterAlignedTopAppBar(
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.White,
                        titleContentColor = CharcoalBlack
                    ),
                    title = {
                        Text(
                            text = "Motion Studio",
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    actions = {
                        if (viewModel.isSampleAiMode) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = NoticeAmberBg,
                                modifier = Modifier.padding(end = 12.dp)
                            ) {
                                Text(
                                    text = "Sample AI",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NoticeAmberInk,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                )
            },
            bottomBar = {
                if (!useNavigationRail) {
                    NavigationBar(
                        containerColor = SurfaceStudioGray,
                        tonalElevation = 0.dp
                    ) {
                        StudioRoute.all.forEach { dest ->
                            val selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTo(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = stringResource(dest.labelRes)
                                    )
                                },
                                label = { Text(stringResource(dest.labelRes)) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavigationRail) {
                    NavigationRail(containerColor = SurfaceStudioGray) {
                        StudioRoute.all.forEach { dest ->
                            val selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTo(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = stringResource(dest.labelRes)
                                    )
                                },
                                label = { Text(stringResource(dest.labelRes)) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    NavHost(
                        navController = navController,
                        startDestination = StudioRoute.Studio.route
                    ) {
                        composable(StudioRoute.Studio.route) {
                            StudioScreen(
                                scriptText = scriptText,
                                videoSettings = videoSettings,
                                generationState = generationState,
                                selectedSceneIndex = selectedSceneIndex,
                                currentFrame = currentFrame,
                                isPlaying = isPlaying,
                                previewBackground = previewBackground,
                                previewResolution = previewResolution,
                                exportState = exportState,
                                isSampleAiMode = viewModel.isSampleAiMode,
                                renderer = viewModel.frameRenderer,
                                onScriptChange = viewModel::updateScriptText,
                                onApplySampleScript = viewModel::applySamplePrompt,
                                onDurationChange = viewModel::updateDurationSeconds,
                                onGenerateMotion = viewModel::generateMotion,
                                onSelectScene = viewModel::selectSceneForPreview,
                                onTogglePlayPause = viewModel::togglePlayPause,
                                onRestart = viewModel::restartPlayback,
                                onStepFrame = viewModel::stepFrame,
                                onSeekFrame = viewModel::seekToFrame,
                                onChangeBackground = viewModel::setPreviewBackground,
                                onChangeResolution = viewModel::setPreviewResolution,
                                onRegenerateScene = viewModel::regenerateScene,
                                onStartExport = { fmt -> viewModel.startExport(fmt) },
                                onNavigateToExport = { navigateTo(StudioRoute.Export.route) },
                                onNavigateToSettings = { navigateTo(StudioRoute.Settings.route) }
                            )
                        }

                        composable(StudioRoute.Scenes.route) {
                            ScenesScreen(
                                generationState = generationState,
                                onPreviewScene = { idx ->
                                    viewModel.selectSceneForPreview(idx)
                                    viewModel.startPlayback()
                                    navigateTo(StudioRoute.Studio.route)
                                },
                                onRegenerateScene = viewModel::regenerateScene,
                                onChangeSceneDuration = viewModel::updateSceneDuration,
                                onRenderSingleScene = { idx ->
                                    viewModel.selectSceneForPreview(idx)
                                    viewModel.startExport(ExportFormat.DUAL_MOV_AND_MP4)
                                    navigateTo(StudioRoute.Export.route)
                                },
                                onRenderUnifiedProject = {
                                    viewModel.selectSceneForPreview(null)
                                    viewModel.startExport(ExportFormat.DUAL_MOV_AND_MP4)
                                    navigateTo(StudioRoute.Export.route)
                                },
                                onGoToStudio = { navigateTo(StudioRoute.Studio.route) }
                            )
                        }

                        composable(StudioRoute.Export.route) {
                            ExportScreen(
                                generationState = generationState,
                                selectedSceneIndex = selectedSceneIndex,
                                exportState = exportState,
                                remotionTsxCode = viewModel.getRemotionTsxPreview(),
                                onSelectSceneScope = viewModel::selectSceneForPreview,
                                onStartExport = { fmt -> viewModel.startExport(fmt) },
                                onSaveToDownloads = viewModel::saveArtifactToDownloads,
                                onShareArtifact = { artifact ->
                                    val intent = viewModel.buildShareIntent(artifact)
                                    context.startActivity(Intent.createChooser(intent, artifact.label))
                                },
                                onGoToStudio = { navigateTo(StudioRoute.Studio.route) }
                            )
                        }

                        composable(StudioRoute.Settings.route) {
                            SettingsScreen(
                                videoSettings = videoSettings,
                                providerConfig = providerConfig,
                                onUpdateVideoSettings = viewModel::updateVideoSettings,
                                onSelectProvider = viewModel::selectProvider,
                                onSaveCustomProvider = viewModel::saveCustomProvider,
                                onClearCustomKey = viewModel::clearCustomApiKey
                            )
                        }
                    }
                }
            }
        }
    }
}
