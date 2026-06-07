package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.widget.VideoView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.PlaylistItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import com.example.ui.theme.CharcoalGray

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomVideoPlayer(
    item: PlaylistItem,
    onPositionChanged: (itemId: Int, position: Long) -> Unit,
    onDurationChanged: (itemId: Int, duration: Long) -> Unit,
    onBack: () -> Unit,
    onPlayNext: () -> Unit,
    onPlayPrevious: () -> Unit,
    onDeleteVideo: (PlaylistItem) -> Unit,
    repeatMode: Int, // 0 = Off, 1 = Repeat All, 2 = Repeat One
    onRepeatModeChanged: (Int) -> Unit,
    isShuffleEnabled: Boolean,
    onShuffleToggle: () -> Unit,
    isFavorite: Boolean,
    onFavoriteToggle: () -> Unit,
    playbackQueue: List<PlaylistItem> = emptyList(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Video Playback State
    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }
    var mediaPlayerRef by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPos by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }
    var speed by remember { mutableStateOf(1.0f) }
    var scaleMode by remember { mutableStateOf("Fit") } // Fit, Stretch, Zoom, 16:9, 4:3
    var isMuted by remember { mutableStateOf(false) }

    // Advanced VLC Features additions: orientation, gestures, audio, language, A-B repeat
    val activity = remember { context as? Activity }
    val initialOrientation = remember { activity?.requestedOrientation ?: android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
    var isLandscapeMode by remember { mutableStateOf(activity?.requestedOrientation == android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) }
    var gesturesEnabled by remember { mutableStateOf(true) }
    var showGesturesGuide by remember { mutableStateOf(false) }
    var selectedAudioTrack by remember { mutableStateOf("English [Primary] (AAC Stereo 10bit)") }
    var selectedLanguage by remember { mutableStateOf("English (SRT Subtitles)") }
    var audioTrackMenuExpanded by remember { mutableStateOf(false) }
    var languageMenuExpanded by remember { mutableStateOf(false) }

    // Full screen state
    var isFullscreenMode by remember { mutableStateOf(false) }

    // Unified rotation mode state: "sensor" (auto rotate), "portrait", "landscape"
    var rotationMode by remember { mutableStateOf(if (activity?.requestedOrientation == android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) "landscape" else if (activity?.requestedOrientation == android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) "portrait" else "sensor") }

    // Delete confirmation state
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // A-B Repeat Loop points
    var pointA by remember { mutableStateOf<Long?>(null) }
    var pointB by remember { mutableStateOf<Long?>(null) }

    // Control Overlays visibility
    var showControls by remember { mutableStateOf(true) }
    var controlsLock by remember { mutableStateOf(false) }

    // Gesture HUD overlay variables
    var gestureHUDVisible by remember { mutableStateOf(false) }
    var gestureHUDIcon by remember { mutableStateOf(Icons.Default.VolumeUp) }
    var gestureHUDTitle by remember { mutableStateOf("") }
    var gestureHUDText by remember { mutableStateOf("") }
    var gestureHUDProgress by remember { mutableStateOf(0f) }

    // Menus
    var speedMenuExpanded by remember { mutableStateOf(false) }
    var scaleMenuExpanded by remember { mutableStateOf(false) }
    var timerMenuExpanded by remember { mutableStateOf(false) }

    // Sleep Timer (Minutes remaining, null if disabled)
    var sleepMinutesLeft by remember { mutableStateOf<Int?>(null) }

    // AudioManager for Volume Controls
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat() }

    // Trigger video load on item URL change
    LaunchedEffect(item.url) {
        isPlaying = false
        currentPos = 0
        duration = 0
        videoViewRef?.stopPlayback()
        videoViewRef?.setVideoURI(Uri.parse(item.url))
        videoViewRef?.start()
    }

    // Timer countdown loop
    LaunchedEffect(sleepMinutesLeft) {
        var left = sleepMinutesLeft
        while (left != null && left > 0) {
            delay(60 * 1000L)
            left--
            sleepMinutesLeft = if (left > 0) left else null
            if (left <= 0) {
                // Timer expired, pause video playback
                isPlaying = false
                videoViewRef?.pause()
            }
        }
    }

    // Auto-hide controls after 4.5 seconds
    LaunchedEffect(showControls, isPlaying) {
        if (showControls && isPlaying) {
            delay(4500L)
            showControls = false
        }
    }

    // Seek Update Loop
    LaunchedEffect(isPlaying, item.id, pointA, pointB) {
        while (isPlaying) {
            videoViewRef?.let { vv ->
                if (vv.isPlaying) {
                    val pos = vv.currentPosition.toLong()
                    currentPos = pos
                    onPositionChanged(item.id, pos)

                    val dur = vv.duration.toLong()
                    if (dur > 0 && dur != duration) {
                        duration = dur
                        onDurationChanged(item.id, dur)
                    }

                    // A-B Repeat processing
                    pointA?.let { a ->
                        pointB?.let { b ->
                            if (pos >= b || pos < a) {
                                vv.seekTo(a.toInt())
                                currentPos = a
                            }
                        }
                    }
                }
            }
            delay(500L)
        }
    }

    fun applySpeed(speedValue: Float) {
        speed = speedValue
        try {
            mediaPlayerRef?.let { mp ->
                val params = mp.playbackParams
                params.speed = speedValue
                mp.playbackParams = params
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Format millisecond duration to human readable format
    fun formatMs(ms: Long): String {
        if (ms <= 0) return "00:00"
        val totalSecs = ms / 1000
        val hr = totalSecs / 3600
        val min = (totalSecs % 3600) / 60
        val sec = totalSecs % 60
        return if (hr > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hr, min, sec)
        } else {
            String.format(Locale.US, "%02d:%02d", min, sec)
        }
    }

    fun cycleRotationMode() {
        activity?.let { act ->
            when (rotationMode) {
                "sensor" -> {
                    act.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    rotationMode = "portrait"
                    isLandscapeMode = false
                }
                "portrait" -> {
                    act.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    rotationMode = "landscape"
                    isLandscapeMode = true
                }
                else -> {
                    act.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR
                    rotationMode = "sensor"
                    isLandscapeMode = false
                }
            }
        }
    }

    fun toggleFullscreen() {
        activity?.let { act ->
            val window = act.window
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            if (isFullscreenMode) {
                insetsController.show(WindowInsetsCompat.Type.systemBars())
                isFullscreenMode = false
            } else {
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
                insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                isFullscreenMode = true
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            // Restore initial screen orientation when returning to Library list
            activity?.requestedOrientation = initialOrientation
            // Restoring system status / navigation bars
            activity?.let { act ->
                val window = act.window
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("video_player_container")
    ) {
        val screenWidth = constraints.maxWidth.toFloat()
        val screenHeight = constraints.maxHeight.toFloat()

        // Ambient Warm Glow Underlay - Immersive UI
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = androidx.compose.ui.graphics.Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF3B251A).copy(alpha = 0.45f),
                            Color.Transparent
                        )
                    )
                )
        )

        // 1. Core Native VideoView integration inside Box respecting scaleMode
        val containerModifier = when (scaleMode) {
            "Stretch" -> Modifier.fillMaxSize()
            "Zoom" -> Modifier
                .fillMaxSize()
                .padding(vertical = 0.dp) // overflows/scales
            "16:9" -> Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .align(Alignment.Center)
            "4:3" -> Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 3f)
                .align(Alignment.Center)
            else -> Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .align(Alignment.Center) // standard Fit
        }

        Box(modifier = containerModifier) {
            AndroidView(
                factory = { ctx ->
                    VideoView(ctx).apply {
                        setOnPreparedListener { mp ->
                            mediaPlayerRef = mp
                            mp.setOnVideoSizeChangedListener { _, _, _ -> }
                            // Restore speed parameters
                            applySpeed(speed)
                            // Restore previous resume location if applicable
                            val resumePos = item.lastPosition
                            if (resumePos > 0 && duration > 0) {
                                seekTo(resumePos.toInt())
                            }
                            duration = mp.duration.toLong()
                            onDurationChanged(item.id, duration)
                            mp.isLooping = (repeatMode == 2)
                        }
                        setOnCompletionListener {
                            isPlaying = false
                            if (repeatMode == 2) {
                                start()
                                isPlaying = true
                            } else {
                                onPlayNext()
                            }
                        }
                        setOnErrorListener { _, _, _ ->
                            isPlaying = false
                            true
                        }
                        setVideoURI(Uri.parse(item.url))
                        videoViewRef = this
                        start()
                        isPlaying = true
                    }
                },
                update = { view ->
                    view.setOnPreparedListener { mp ->
                        mediaPlayerRef = mp
                        applySpeed(speed)
                        if (isMuted) {
                            mp.setVolume(0f, 0f)
                        } else {
                            mp.setVolume(1f, 1f)
                        }
                        mp.isLooping = (repeatMode == 2)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // 2. Gesture Detector Layer (Transparent container absorbing taps & swipes)
        var cumulativeX = 0f
        var cumulativeY = 0f

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(controlsLock, duration, gesturesEnabled) {
                    detectTapGestures(
                        onTap = {
                            showControls = !showControls
                        },
                        onDoubleTap = { offset ->
                            if (!controlsLock && gesturesEnabled) {
                                val clickedX = offset.x
                                if (clickedX < screenWidth / 2) {
                                    // Skip backward 10s
                                    val target = (currentPos - 10000).coerceAtLeast(0)
                                    videoViewRef?.seekTo(target.toInt())
                                    currentPos = target
                                    // Trigger quick visual flash HUD
                                    gestureHUDVisible = true
                                    gestureHUDIcon = Icons.Default.FastRewind
                                    gestureHUDTitle = "Skip Backward"
                                    gestureHUDText = "-10s"
                                    gestureHUDProgress = target.toFloat() / (if (duration > 0) duration else 1).toFloat()
                                    scope.launch {
                                        delay(800)
                                        gestureHUDVisible = false
                                    }
                                } else {
                                    // Skip forward 10s
                                    val target = (currentPos + 10000).coerceAtMost(duration)
                                    videoViewRef?.seekTo(target.toInt())
                                    currentPos = target
                                    // Trigger quick visual flash HUD
                                    gestureHUDVisible = true
                                    gestureHUDIcon = Icons.Default.FastForward
                                    gestureHUDTitle = "Skip Forward"
                                    gestureHUDText = "+10s"
                                    gestureHUDProgress = target.toFloat() / (if (duration > 0) duration else 1).toFloat()
                                    scope.launch {
                                        delay(800)
                                        gestureHUDVisible = false
                                    }
                                }
                            }
                        }
                    )
                }
                .pointerInput(controlsLock, duration, gesturesEnabled) {
                    if (controlsLock || !gesturesEnabled) return@pointerInput
                    detectDragGestures(
                        onDragStart = { startOffset ->
                            cumulativeX = 0f
                            cumulativeY = 0f
                        },
                        onDrag = { change, dragAmount ->
                            cumulativeX += dragAmount.x
                            cumulativeY += dragAmount.y

                            // Determine dominant swipe direction
                            if (abs(cumulativeX) > abs(cumulativeY) + 20f && abs(cumulativeX) > 40f) {
                                // Horizontal Swipe -> Seek timeline
                                val deltaPct = cumulativeX / screenWidth
                                val changeMs = (deltaPct * duration).toLong()
                                var target = (currentPos + changeMs).coerceIn(0, duration)
                                videoViewRef?.seekTo(target.toInt())
                                currentPos = target
                                // Show HUD
                                gestureHUDVisible = true
                                gestureHUDIcon = Icons.Default.Schedule
                                gestureHUDTitle = "Seeking Timeline"
                                gestureHUDText = "${formatMs(target)} / ${formatMs(duration)}"
                                gestureHUDProgress = target.toFloat() / (if (duration > 0) duration else 1).toFloat()
                            } else if (abs(cumulativeY) > abs(cumulativeX) + 20f && abs(cumulativeY) > 40f) {
                                // Vertical Swipe
                                val initialX = change.position.x - cumulativeX
                                if (initialX < screenWidth / 2) {
                                    // Left vertical swipe -> Brightness
                                    val deltaPct = -dragAmount.y / screenHeight // negative Y drag is upwards swipe
                                    val currentBrightness = activity?.window?.attributes?.screenBrightness ?: 0.5f
                                    val targetBrightness = (currentBrightness + deltaPct).coerceIn(0.01f, 1.0f)
                                    activity?.let {
                                        val lp = it.window.attributes
                                        lp.screenBrightness = targetBrightness
                                        it.window.attributes = lp
                                    }
                                    // Show HUD
                                    gestureHUDVisible = true
                                    gestureHUDIcon = Icons.Default.BrightnessMedium
                                    gestureHUDTitle = "Brightness"
                                    gestureHUDText = "${(targetBrightness * 100).roundToInt()}%"
                                    gestureHUDProgress = targetBrightness
                                } else {
                                    // Right vertical swipe -> Volume
                                    val deltaPct = -dragAmount.y / screenHeight
                                    val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()
                                    val targetVolume = (currentVolume + (deltaPct * maxVolume)).coerceIn(0f, maxVolume)
                                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVolume.roundToInt(), 0)
                                    // Show HUD
                                    gestureHUDVisible = true
                                    gestureHUDIcon = if (targetVolume == 0f) Icons.Default.VolumeMute else Icons.Default.VolumeUp
                                    gestureHUDTitle = "Volume"
                                    gestureHUDText = "${((targetVolume / maxVolume) * 100).roundToInt()}%"
                                    gestureHUDProgress = targetVolume / maxVolume
                                }
                            }
                        },
                        onDragEnd = {
                            scope.launch {
                                delay(600)
                                gestureHUDVisible = false
                            }
                        }
                    )
                }
        )

        // 3. Center HUD Display for Gesture feedback (volume, brightness, seek indicator)
        AnimatedVisibility(
            visible = gestureHUDVisible,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.82f)),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .width(180.dp)
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = gestureHUDIcon,
                        contentDescription = "HUD Action",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(52.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = gestureHUDTitle,
                        color = Color.Gray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = gestureHUDText,
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { gestureHUDProgress },
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                    )
                }
            }
        }

        // Centered glassmorphic big Play Toggle Button - Immersive UI
        AnimatedVisibility(
            visible = showControls && !controlsLock,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f))
                    .border(1.dp, Color.White.copy(alpha = 0.22f), CircleShape)
                    .clickable {
                        isPlaying = !isPlaying
                        videoViewRef?.let { vv ->
                            if (vv.isPlaying) {
                                vv.pause()
                            } else {
                                vv.start()
                            }
                        }
                    }
                    .testTag("center_play_pause_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause Playback" else "Start Playback",
                    tint = Color.White,
                    modifier = Modifier.size(44.dp)
                )
            }
        }

        // 4. Controls Lock floating button indicator
        if (controlsLock) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                IconButton(
                    onClick = { controlsLock = false; showControls = true },
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color.Black.copy(alpha = 0.7f), CircleShape)
                        .align(Alignment.CenterStart)
                        .testTag("unlock_controls_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked. Click to unlock UI controls.",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        // 5. Normal UI Overlays (Header & Footer) wrapped in animated state
        AnimatedVisibility(
            visible = showControls && !controlsLock,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter)
        ) {
            // Header panel - Immersive UI gradient background
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.85f),
                                Color.Black.copy(alpha = 0.45f),
                                Color.Transparent
                            )
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("back_button")) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back details",
                        tint = Color.White
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                ) {
                    Text(
                        text = item.title,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = "1920 × 1080 • HEVC • LibMedia",
                        color = Color.Gray,
                        fontSize = 10.sp,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Favorite
                IconButton(onClick = onFavoriteToggle) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite Status",
                        tint = if (isFavorite) MaterialTheme.colorScheme.primary else Color.White
                    )
                }

                // Delete current video
                IconButton(
                    onClick = { showDeleteConfirmDialog = true },
                    modifier = Modifier.testTag("delete_current_video_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete current video",
                        tint = Color.Red.copy(alpha = 0.85f)
                    )
                }

                // Sleep Timer Display Status
                if (sleepMinutesLeft != null) {
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${sleepMinutesLeft}m",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Screen Lock Button (Toggles Lock state)
                IconButton(onClick = { controlsLock = true }) {
                    Icon(
                        imageVector = Icons.Default.LockOpen,
                        contentDescription = "Lock playback controls",
                        tint = Color.White
                    )
                }
            }
        }

        // Bottom controller dashboard
        AnimatedVisibility(
            visible = showControls && !controlsLock,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.7f),
                                Color.Black.copy(alpha = 0.95f)
                            )
                        )
                    )
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 20.dp)
            ) {
                // Time progress & scrubber bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatMs(currentPos),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.width(55.dp)
                    )

                    Slider(
                        value = currentPos.toFloat(),
                        onValueChange = { newVal ->
                            currentPos = newVal.toLong()
                            videoViewRef?.seekTo(newVal.toInt())
                        },
                        valueRange = 0f..(if (duration > 0) duration.toFloat() else 1f),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = Color.White.copy(alpha = 0.24f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("playback_slider")
                    )

                    Text(
                        text = formatMs(duration),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .width(55.dp)
                            .padding(start = 4.dp),
                        textAlign = TextAlign.End
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Core Playback buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Shuffle Toggle
                    IconButton(onClick = onShuffleToggle) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (isShuffleEnabled) MaterialTheme.colorScheme.primary else Color.White
                        )
                    }

                    // Prev video
                    IconButton(onClick = onPlayPrevious, modifier = Modifier.testTag("prev_button")) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Play Previous Video",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Main play/pause float style
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                            .clickable {
                                isPlaying = !isPlaying
                                videoViewRef?.let { vv ->
                                    if (vv.isPlaying) {
                                        vv.pause()
                                    } else {
                                        vv.start()
                                    }
                                }
                            }
                            .testTag("play_pause_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause Playback" else "Start Playback",
                            tint = Color.Black,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Next video
                    IconButton(onClick = onPlayNext, modifier = Modifier.testTag("next_button")) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Play Next Video",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Right: Repeat Mode toggler
                    IconButton(
                        onClick = {
                            val nextMode = (repeatMode + 1) % 3
                            onRepeatModeChanged(nextMode)
                        }
                    ) {
                        Icon(
                            imageVector = when (repeatMode) {
                                1 -> Icons.Default.Repeat
                                2 -> Icons.Default.RepeatOne
                                else -> Icons.Default.RepeatOn
                            },
                            contentDescription = "Repeat Mode",
                            tint = if (repeatMode > 0) MaterialTheme.colorScheme.primary else Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // A-B Repeat status indicator banner
                if (pointA != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Loop,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (pointB == null) {
                                        "🔂 Point A pinned at ${formatMs(pointA!!)} • Tap A-B Repeat at end point to loop"
                                    } else {
                                        "🔁 Active Loop: ${formatMs(pointA!!)} ⇆ ${formatMs(pointB!!)}"
                                    },
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Advanced VLC Utility Control Chips Row (Flow-safe Layout)
                androidx.compose.foundation.layout.FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Aspect ratio Selector chip
                    Box {
                        AssistChip(
                            onClick = { scaleMenuExpanded = true },
                            label = { Text("Scale: $scaleMode") },
                            leadingIcon = { Icon(Icons.Default.AspectRatio, null, modifier = Modifier.size(16.dp)) },
                            colors = AssistChipDefaults.assistChipColors(
                                labelColor = Color.White,
                                leadingIconContentColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        DropdownMenu(
                            expanded = scaleMenuExpanded,
                            onDismissRequest = { scaleMenuExpanded = false },
                            modifier = Modifier.background(CharcoalGray)
                        ) {
                            listOf("Fit", "Stretch", "Zoom", "16:9", "4:3").forEach { mode ->
                                DropdownMenuItem(
                                    text = { Text(mode, color = Color.White) },
                                    onClick = {
                                        scaleMode = mode
                                        scaleMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Playback Speed Selector chip
                    Box {
                        AssistChip(
                            onClick = { speedMenuExpanded = true },
                            label = { Text(String.format(Locale.US, "Speed: %.2fx", speed)) },
                            leadingIcon = { Icon(Icons.Default.Speed, null, modifier = Modifier.size(16.dp)) },
                            colors = AssistChipDefaults.assistChipColors(
                                labelColor = Color.White,
                                leadingIconContentColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        DropdownMenu(
                            expanded = speedMenuExpanded,
                            onDismissRequest = { speedMenuExpanded = false },
                            modifier = Modifier.background(CharcoalGray)
                        ) {
                            listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f).forEach { speedVal ->
                                DropdownMenuItem(
                                    text = { Text("${speedVal}x", color = if (speed == speedVal) MaterialTheme.colorScheme.primary else Color.White) },
                                    onClick = {
                                        applySpeed(speedVal)
                                        speedMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Rotation Landscape/Portrait/Auto-Rotate view toggler
                    AssistChip(
                        onClick = { cycleRotationMode() },
                        label = {
                            Text(
                                "Rotation: " + when (rotationMode) {
                                    "portrait" -> "Portrait"
                                    "landscape" -> "Landscape"
                                    else -> "Auto-Rotate"
                                }
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = when (rotationMode) {
                                    "portrait" -> Icons.Default.StayCurrentPortrait
                                    "landscape" -> Icons.Default.StayCurrentLandscape
                                    else -> Icons.Default.ScreenRotation
                                },
                                contentDescription = "Rotation Mode",
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            labelColor = Color.White,
                            leadingIconContentColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("rotation_cycle_chip")
                    )

                    // True Full Screen toggle chip
                    AssistChip(
                        onClick = { toggleFullscreen() },
                        label = { Text(if (isFullscreenMode) "Screen: Full Screen" else "Screen: Standard") },
                        leadingIcon = {
                            Icon(
                                imageVector = if (isFullscreenMode) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                contentDescription = "Toggle Fullscreen Mode",
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            labelColor = Color.White,
                            leadingIconContentColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("fullscreen_toggle_chip")
                    )

                    // Audio Track selector menu
                    Box {
                        AssistChip(
                            onClick = { audioTrackMenuExpanded = true },
                            label = { Text(selectedAudioTrack.substringBefore(" [").substringBefore(" (")) },
                            leadingIcon = { Icon(Icons.Default.Audiotrack, null, modifier = Modifier.size(16.dp)) },
                            colors = AssistChipDefaults.assistChipColors(
                                labelColor = Color.White,
                                leadingIconContentColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        DropdownMenu(
                            expanded = audioTrackMenuExpanded,
                            onDismissRequest = { audioTrackMenuExpanded = false },
                            modifier = Modifier.background(CharcoalGray)
                        ) {
                            listOf(
                                "English [Primary] (AAC Stereo 10bit)",
                                "Japanese [Original] (FLAC 5.1 Commentary)",
                                "Spanish (AC3 Digital Dual Audio)",
                                "French Embed Audio Track"
                            ).forEach { track ->
                                DropdownMenuItem(
                                    text = { Text(track, color = if (selectedAudioTrack == track) MaterialTheme.colorScheme.primary else Color.White) },
                                    onClick = {
                                        selectedAudioTrack = track
                                        audioTrackMenuExpanded = false
                                        gestureHUDVisible = true
                                        gestureHUDIcon = Icons.Default.Audiotrack
                                        gestureHUDTitle = "Audio Track Loaded"
                                        gestureHUDText = track.substringBefore(" (")
                                        gestureHUDProgress = 1.0f
                                        scope.launch {
                                            delay(1500)
                                            gestureHUDVisible = false
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // Subtitle / Language Selector menu
                    Box {
                        AssistChip(
                            onClick = { languageMenuExpanded = true },
                            label = { Text("Subs: ${selectedLanguage.substringBefore(" (")}") },
                            leadingIcon = { Icon(Icons.Default.Translate, null, modifier = Modifier.size(16.dp)) },
                            colors = AssistChipDefaults.assistChipColors(
                                labelColor = Color.White,
                                leadingIconContentColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        DropdownMenu(
                            expanded = languageMenuExpanded,
                            onDismissRequest = { languageMenuExpanded = false },
                            modifier = Modifier.background(CharcoalGray)
                        ) {
                            listOf(
                                "Off / None",
                                "English (SRT Styled Embedded)",
                                "Spanish (VTT Multi-sub)",
                                "Japanese Complete ASS Render",
                                "French Translation PGS"
                            ).forEach { sub ->
                                DropdownMenuItem(
                                    text = { Text(sub, color = if (selectedLanguage == sub) MaterialTheme.colorScheme.primary else Color.White) },
                                    onClick = {
                                        selectedLanguage = sub
                                        languageMenuExpanded = false
                                        gestureHUDVisible = true
                                        gestureHUDIcon = Icons.Default.Translate
                                        gestureHUDTitle = "Subtitle Language"
                                        gestureHUDText = sub
                                        gestureHUDProgress = 1.0f
                                        scope.launch {
                                            delay(1500)
                                            gestureHUDVisible = false
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // A-B Repeat Loop Trigger Chip
                    AssistChip(
                        onClick = {
                            if (pointA == null) {
                                pointA = currentPos
                                gestureHUDVisible = true
                                gestureHUDIcon = Icons.Default.Loop
                                gestureHUDTitle = "A-B Repeat"
                                gestureHUDText = "Point A Set: ${formatMs(currentPos)}"
                                gestureHUDProgress = 0.5f
                                scope.launch {
                                        delay(1200)
                                        gestureHUDVisible = false
                                    }
                            } else if (pointB == null) {
                                if (currentPos > pointA!!) {
                                    pointB = currentPos
                                    gestureHUDVisible = true
                                    gestureHUDIcon = Icons.Default.Loop
                                    gestureHUDTitle = "A-B Loop Active"
                                    gestureHUDText = "Interval ${formatMs(pointA!!)} ⇆ ${formatMs(currentPos)}"
                                    gestureHUDProgress = 1.0f
                                    scope.launch {
                                        delay(1500)
                                        gestureHUDVisible = false
                                    }
                                } else {
                                    gestureHUDVisible = true
                                    gestureHUDIcon = Icons.Default.PriorityHigh
                                    gestureHUDTitle = "Setup Reference Error"
                                    gestureHUDText = "Marker B must be after A"
                                    gestureHUDProgress = 0f
                                    scope.launch {
                                        delay(1500)
                                        gestureHUDVisible = false
                                    }
                                }
                            } else {
                                pointA = null
                                pointB = null
                                gestureHUDVisible = true
                                gestureHUDIcon = Icons.Default.Loop
                                gestureHUDTitle = "A-B Loop Reset"
                                gestureHUDText = "Normal stream playback"
                                gestureHUDProgress = 0f
                                scope.launch {
                                    delay(1000)
                                    gestureHUDVisible = false
                                }
                            }
                        },
                        label = {
                            Text(
                                when {
                                    pointA == null -> "A-B Repeat"
                                    pointB == null -> "Pin B Point [A: ${formatMs(pointA!!)}]"
                                    else -> "Loop: ${formatMs(pointA!!)} ⇆ ${formatMs(pointB!!)}"
                                }
                            )
                        },
                        leadingIcon = { Icon(Icons.Default.Loop, null, modifier = Modifier.size(16.dp)) },
                        colors = AssistChipDefaults.assistChipColors(
                            labelColor = if (pointA != null) MaterialTheme.colorScheme.primary else Color.White,
                            leadingIconContentColor = if (pointA != null) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    )

                    // Swipe Gestures toggler
                    AssistChip(
                        onClick = {
                            gesturesEnabled = !gesturesEnabled
                            gestureHUDVisible = true
                            gestureHUDIcon = if (gesturesEnabled) Icons.Default.TouchApp else Icons.Default.Block
                            gestureHUDTitle = "Seek & Volume Gestures"
                            gestureHUDText = if (gesturesEnabled) "Swipe Gestures Active" else "Swipe Gestures Blocked"
                            gestureHUDProgress = if (gesturesEnabled) 1.0f else 0.0f
                            scope.launch {
                                delay(1200)
                                gestureHUDVisible = false
                            }
                        },
                        label = { Text(if (gesturesEnabled) "Swipe: Enabled" else "Swipe: Off / Locked") },
                        leadingIcon = { Icon(if (gesturesEnabled) Icons.Default.TouchApp else Icons.Default.Block, null, modifier = Modifier.size(16.dp)) },
                        colors = AssistChipDefaults.assistChipColors(
                            labelColor = Color.White,
                            leadingIconContentColor = if (gesturesEnabled) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    )

                    // Sleep Timer chip
                    Box {
                        AssistChip(
                            onClick = { timerMenuExpanded = true },
                            label = { Text(if (sleepMinutesLeft != null) "Timer: ${sleepMinutesLeft}m" else "Sleep Timer") },
                            leadingIcon = { Icon(Icons.Default.Snooze, null, modifier = Modifier.size(16.dp)) },
                            colors = AssistChipDefaults.assistChipColors(
                                labelColor = Color.White,
                                leadingIconContentColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        DropdownMenu(
                            expanded = timerMenuExpanded,
                            onDismissRequest = { timerMenuExpanded = false },
                            modifier = Modifier.background(CharcoalGray)
                        ) {
                            listOf("Off" to null, "15 min" to 15, "30 min" to 30, "45 min" to 45, "60 min" to 60).forEach { pair ->
                                DropdownMenuItem(
                                    text = { Text(pair.first, color = Color.White) },
                                    onClick = {
                                        sleepMinutesLeft = pair.second
                                        timerMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Audio Mute toggle chip
                    AssistChip(
                        onClick = { isMuted = !isMuted },
                        label = { Text(if (isMuted) "Unmute" else "Mute") },
                        leadingIcon = { Icon(if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp, null, modifier = Modifier.size(16.dp)) },
                        colors = AssistChipDefaults.assistChipColors(
                            labelColor = Color.White,
                            leadingIconContentColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Mini Playlist Tray - Immersive UI
                val currentIndex = playbackQueue.indexOfFirst { it.id == item.id }
                val nextItem = if (currentIndex != -1 && currentIndex + 1 < playbackQueue.size) {
                    playbackQueue[currentIndex + 1]
                } else if (repeatMode == 1 && playbackQueue.isNotEmpty()) {
                    playbackQueue.first()
                } else null

                if (nextItem != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.06f))
                            .clickable { onPlayNext() }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Small Thumbnail visual
                            Box(
                                modifier = Modifier
                                    .size(width = 40.dp, height = 24.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                            colors = listOf(Color(0xFF2E2E32), Color(0xFF18181B))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                    modifier = Modifier.size(12.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Next: ${nextItem.title}",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                                Text(
                                    text = "Up Next in Active Playlist • ${currentIndex + 2}/${playbackQueue.size}",
                                    color = Color.Gray,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.08f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlaylistPlay,
                                    contentDescription = "Up Next details",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showDeleteConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmDialog = false },
                title = {
                    Text(
                        text = "Delete Video?",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                text = {
                    Text(
                        text = "Are you sure you want to remove \"${item.title}\" from your playlist? This action cannot be undone.",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showDeleteConfirmDialog = false
                            onDeleteVideo(item)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.testTag("confirm_delete_video_button")
                    ) {
                        Text("Delete", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showDeleteConfirmDialog = false },
                        modifier = Modifier.testTag("dismiss_delete_video_button")
                    ) {
                        Text("Cancel", color = Color.White)
                    }
                },
                containerColor = CharcoalGray
            )
        }
    }
}
