package com.cronos

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.cronos.data.AccentMode
import com.cronos.data.AppInfo
import com.cronos.data.AppRepository
import com.cronos.data.AppSettings
import com.cronos.data.AppSettingsRepository
import com.cronos.ui.theme.CronosTheme
import com.cronos.util.HomeRoleGuard
import com.cronos.util.WheelMath
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    private lateinit var appRepository: AppRepository
    private lateinit var settingsRepository: AppSettingsRepository
    private lateinit var homeRoleGuard: HomeRoleGuard

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appRepository = AppRepository(applicationContext)
        settingsRepository = AppSettingsRepository(applicationContext)
        homeRoleGuard = HomeRoleGuard(applicationContext)

        setContent {
            val settings by settingsRepository.settingsFlow.collectAsState(initial = AppSettings())
            val apps by appRepository.apps.collectAsState()

            CronosTheme(
                accentColorMode = settings.accentMode,
                followSystemFontSize = settings.followSystemFontSize,
            ) {
                val isDefaultHome = homeRoleGuard.isCurrentHomeApp()
                HomeScreen(
                    apps = apps,
                    settings = settings,
                    isDefaultHome = isDefaultHome,
                    onOpenHomeSettings = {
                        startActivity(Intent(android.provider.Settings.ACTION_HOME_SETTINGS))
                    },
                    onSetIconSize = { lifecycleScope.launch { settingsRepository.setIconSize(it) } },
                    onSetAccentMode = { lifecycleScope.launch { settingsRepository.setAccentMode(it) } },
                    onToggleFollowSystemFontSize = { lifecycleScope.launch { settingsRepository.setFollowSystemFontSize(it) } },
                    onOpenApp = { appPackage ->
                        val launchIntent = packageManager.getLaunchIntentForPackage(appPackage)
                        if (launchIntent != null) startActivity(launchIntent)
                    },
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        appRepository.close()
    }
}

private fun filterApps(apps: List<AppInfo>, query: String): List<AppInfo> {
    if (query.isBlank()) return apps
    val lowerQuery = query.lowercase(Locale.getDefault())
    return apps.filter { app ->
        val lowerLabel = app.label.lowercase(Locale.getDefault())
        val lowerPackage = app.packageName.lowercase(Locale.getDefault())
        lowerLabel.startsWith(lowerQuery) ||
            lowerLabel.contains(lowerQuery) ||
            lowerPackage.startsWith(lowerQuery) ||
            lowerPackage.contains(lowerQuery)
    }
}

@Composable
private fun HomeScreen(
    apps: List<AppInfo>,
    settings: AppSettings,
    isDefaultHome: Boolean,
    onOpenHomeSettings: () -> Unit,
    onSetIconSize: (Float) -> Unit,
    onSetAccentMode: (AccentMode) -> Unit,
    onToggleFollowSystemFontSize: (Boolean) -> Unit,
    onOpenApp: (String) -> Unit,
) {
    val currentTime = remember { SimpleDateFormat("h:mm a | EEE, MMM d", Locale.getDefault()) }
    var now by remember { mutableStateOf(Date()) }
    var searchOpen by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf(TextFieldValue()) }
    var ringRotation by remember { mutableFloatStateOf(0f) }

    DisposableEffect(Unit) {
        val handler = Handler(Looper.getMainLooper())
        val tick = object : Runnable {
            override fun run() {
                now = Date()
                val nextCheck = 60000L - (now.time % 60000L)
                handler.postDelayed(this, nextCheck.coerceAtLeast(1000L))
            }
        }
        handler.post(tick)
        onDispose { handler.removeCallbacksAndMessages(null) }
    }

    val filteredApps = remember(apps, query.text) { filterApps(apps, query.text) }
    val iconSize = settings.iconSize.coerceIn(28f, 56f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        if (!isDefaultHome) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.86f)
                    .padding(top = 52.dp)
                    .align(Alignment.TopCenter),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF111111)),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "Set Cronos as your home app",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = "Cronos needs to be the default home launcher. This screen explains the requirement and opens your system home settings.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                    )
                    Button(onClick = onOpenHomeSettings) {
                        Text("Open default home settings")
                    }
                }
            }
        }

        Text(
            text = currentTime.format(now),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 18.dp),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp, end = 18.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            IconButton(onClick = { searchOpen = true }) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = "Search apps",
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
        }

        AppRing(
            apps = apps,
            rotation = ringRotation,
            onRotationChanged = { ringRotation = it },
            iconSize = iconSize,
            onOpenApp = onOpenApp,
        )

        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(bottom = 28.dp)
                .align(Alignment.BottomCenter),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF111111)),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Icon size", color = MaterialTheme.colorScheme.onBackground, fontSize = 12.sp)
                    Text("${iconSize.toInt()} dp", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                }
                Slider(
                    value = iconSize,
                    onValueChange = onSetIconSize,
                    valueRange = 28f..56f,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Accent follows system", color = MaterialTheme.colorScheme.onBackground, fontSize = 12.sp)
                    Switch(
                        checked = settings.accentMode == AccentMode.SYSTEM,
                        onCheckedChange = { enabled ->
                            onSetAccentMode(if (enabled) AccentMode.SYSTEM else AccentMode.FIXED)
                        },
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Follow system font size", color = MaterialTheme.colorScheme.onBackground, fontSize = 12.sp)
                    Switch(
                        checked = settings.followSystemFontSize,
                        onCheckedChange = onToggleFollowSystemFontSize,
                    )
                }
            }
        }

        if (searchOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .clickable { searchOpen = false },
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .padding(top = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF111111), RoundedCornerShape(16.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        IconButton(onClick = { searchOpen = false }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = "Close search",
                                tint = MaterialTheme.colorScheme.onBackground,
                            )
                        }
                        TextField(
                            value = query,
                            onValueChange = { query = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Search apps") },
                            singleLine = true,
                        )
                    }

                    val results = if (query.text.isBlank()) emptyList() else filteredApps
                    if (results.isEmpty()) {
                        Text(
                            text = "App not found",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 12.dp),
                        )
                    } else {
                        results.take(12).forEach { app ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF111111), RoundedCornerShape(12.dp))
                                    .clickable { onOpenApp(app.packageName); searchOpen = false }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (app.icon != null) {
                                    Image(
                                        bitmap = app.icon,
                                        contentDescription = app.label,
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape),
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = app.label.take(1).uppercase(Locale.getDefault()),
                                            color = MaterialTheme.colorScheme.onBackground,
                                            fontSize = 12.sp,
                                        )
                                    }
                                }

                                Text(
                                    text = app.label,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(start = 12.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppRing(
    apps: List<AppInfo>,
    rotation: Float,
    onRotationChanged: (Float) -> Unit,
    iconSize: Float,
    onOpenApp: (String) -> Unit,
) {
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    var lastFocusedIndex by remember { mutableIntStateOf(-1) }
    val radiusPx = with(density) { 150.dp.toPx() }
    val stepDegrees = if (apps.isNotEmpty()) 360f / apps.size.toFloat() else 360f
    val focusedIndex = if (apps.isNotEmpty()) {
        val target = (270f - rotation + 360f) % 360f
        ((target / stepDegrees).roundToInt() + apps.size) % apps.size
    } else {
        0
    }

    if (focusedIndex != lastFocusedIndex) {
        lastFocusedIndex = focusedIndex
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(apps.size) {
                detectDragGestures(
                    onDragEnd = {
                        val snapped = WheelMath.snapToNearestStep(rotation.toDouble(), stepDegrees.toDouble())
                        onRotationChanged(snapped.toFloat())
                    },
                    onDrag = { _, dragAmount ->
                        val adjusted = rotation - (dragAmount.y * 0.6f)
                        onRotationChanged(adjusted)
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .offset(x = (-72).dp, y = 12.dp)
                .size(380.dp),
            contentAlignment = Alignment.Center,
        ) {
            apps.forEachIndexed { index, app ->
                val angle = WheelMath.normalizedAngle((index * stepDegrees.toDouble()) + rotation.toDouble())
                val iconAlpha = WheelMath.opacityForRingAngle(angle)
                if (iconAlpha <= 0.01f) return@forEachIndexed

                val projected = WheelMath.angleToCartesian(angle, radiusPx.toDouble())
                val xPx = projected.first.toFloat()
                val yPx = projected.second.toFloat()
                val isFocused = index == focusedIndex
                val size = if (isFocused) 46.dp else iconSize.dp

                Box(
                    modifier = Modifier
                        .size(size)
                        .graphicsLayer {
                            translationX = xPx
                            translationY = yPx
                            alpha = iconAlpha
                            scaleX = WheelMath.scaleForRingAngle(angle)
                            scaleY = WheelMath.scaleForRingAngle(angle)
                        }
                        .clip(CircleShape)
                        .background(Color(0xFF1B1B1B))
                        .clickable { onOpenApp(app.packageName) },
                    contentAlignment = Alignment.Center,
                ) {
                    if (app.icon != null) {
                        Image(
                            bitmap = app.icon,
                            contentDescription = app.label,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Text(
                            text = app.label.take(1).uppercase(Locale.getDefault()),
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 12.sp,
                        )
                    }
                }
            }

            if (apps.isNotEmpty()) {
                val focusApp = apps[focusedIndex.coerceIn(0, apps.lastIndex)]
                Text(
                    text = focusApp.label,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .padding(top = 130.dp)
                        .align(Alignment.Center),
                )
            }
        }
    }
}
