package com.cyberdyne.markdown.engine.rendering

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

/**
 * Inline native video player backed by Media3/ExoPlayer. Never uses a WebView.
 * The player is released when the composable leaves the composition.
 */
@Composable
internal fun InlineVideoPlayer(url: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val player = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            prepare()
            playWhenReady = false
        }
    }
    DisposableEffect(url) {
        onDispose { player.release() }
    }
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { ctx ->
            PlayerView(ctx).apply {
                this.player = player
                useController = true
            }
        },
    )
}
