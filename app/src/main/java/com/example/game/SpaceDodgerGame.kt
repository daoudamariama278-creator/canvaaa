package com.example.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DangerRed
import com.example.ui.theme.NeonBlue
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.ShieldCyan
import com.example.ui.theme.SpaceBackground
import com.example.ui.theme.SpaceCard
import com.example.ui.theme.SpaceCardBorder
import com.example.ui.theme.StarGold
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.isActive
import kotlin.math.roundToInt

/**
 * Main Space Dodger Game Composable.
 * Houses the real-time game loop via LaunchedEffect + withFrameNanos,
 * interactive Canvas renderer, and UI overlay states.
 */
@Composable
fun SpaceDodgerGame(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val engine = remember { SpaceDodgerEngine(context) }

    // Continuous Real-Time Game Loop using Compose withFrameNanos
    LaunchedEffect(engine) {
        var previousFrameTime = 0L
        while (isActive) {
            withFrameNanos { frameTimeNanos ->
                if (previousFrameTime != 0L) {
                    val frameDelta = frameTimeNanos - previousFrameTime
                    engine.update(frameDelta)
                }
                previousFrameTime = frameTimeNanos
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF03050F),
                        Color(0xFF080D21),
                        Color(0xFF100C24),
                        Color(0xFF060918)
                    )
                )
            )
    ) {
        val screenWidthPx = constraints.maxWidth.toFloat()
        val screenHeightPx = constraints.maxHeight.toFloat()

        // 1. Interactive 2D Graphics Canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("game_canvas")
                .offset {
                    IntOffset(
                        engine.shakeOffset.first.roundToInt(),
                        engine.shakeOffset.second.roundToInt()
                    )
                }
                .pointerInput(engine.gameState) {
                    if (engine.gameState == GameState.PLAYING) {
                        detectTapGestures(
                            onPress = { offset ->
                                val normX = offset.x / size.width
                                engine.directTouchTargetX = normX
                                engine.inputDirection = if (normX < 0.5f) -1f else 1f
                                tryAwaitRelease()
                                engine.inputDirection = 0f
                                engine.directTouchTargetX = null
                            }
                        )
                    }
                }
                .pointerInput(engine.gameState) {
                    if (engine.gameState == GameState.PLAYING) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                engine.directTouchTargetX = offset.x / size.width
                            },
                            onDragEnd = {
                                engine.directTouchTargetX = null
                                engine.inputDirection = 0f
                            },
                            onDragCancel = {
                                engine.directTouchTargetX = null
                                engine.inputDirection = 0f
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                engine.directTouchTargetX = change.position.x / size.width
                            }
                        )
                    }
                }
        ) {
            val currentTime = engine.elapsedTime

            // Parallax Starfield
            SpaceDodgerRender.drawStarfield(this, engine.stars, currentTime)

            // Power-ups
            for (powerUp in engine.powerUps) {
                SpaceDodgerRender.drawPowerUp(this, powerUp, currentTime)
            }

            // Asteroids
            for (asteroid in engine.asteroids) {
                SpaceDodgerRender.drawAsteroid(this, asteroid)
            }

            // Player Spaceship (drawn if in Menu or Playing)
            if (engine.gameState != GameState.GAME_OVER) {
                SpaceDodgerRender.drawPlayer(this, engine.player, currentTime)
            }

            // Particles (Thruster, explosions, sparkles)
            SpaceDodgerRender.drawParticles(this, engine.particles)
        }

        // 2. Floating Text Overlay (e.g. "+50", "BOUCLIER !")
        for (ft in engine.floatingTexts) {
            Text(
                text = ft.text,
                color = ft.color,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (ft.x * screenWidthPx - 40f).roundToInt(),
                            (ft.y * screenHeightPx).roundToInt()
                        )
                    }
                    .alpha(ft.alpha)
            )
        }

        // 3. UI Layer by GameState
        when (engine.gameState) {
            GameState.PLAYING -> {
                PlayingHudOverlay(engine = engine)
            }
            GameState.MENU -> {
                MenuOverlay(engine = engine)
            }
            GameState.GAME_OVER -> {
                GameOverOverlay(engine = engine)
            }
        }
    }
}

/**
 * In-game HUD: Real-time score, high score, shield status indicator, and touch zone cues.
 */
@Composable
private fun PlayingHudOverlay(
    engine: SpaceDodgerEngine
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shield_pulse")
    val shieldPulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shield_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp)
    ) {
        // Top HUD Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // High Score Mini Badge
            Surface(
                color = SpaceCard.copy(alpha = 0.85f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SpaceCardBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = "Record",
                        tint = StarGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "${engine.highScore}",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Current Score Display
            Surface(
                color = SpaceCard.copy(alpha = 0.92f),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, NeonCyan.copy(alpha = 0.6f))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "SCORE",
                        color = NeonCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = "${engine.score}",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            // Shield Indicator
            if (engine.player.hasShield) {
                Surface(
                    color = ShieldCyan.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, ShieldCyan),
                    modifier = Modifier.scale(shieldPulseScale)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Bouclier Actif",
                            tint = ShieldCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "ACTIF",
                            color = ShieldCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.width(60.dp))
            }
        }

        // Bottom subtle touch control hints
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                color = Color.White.copy(alpha = 0.05f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { engine.inputDirection = -1f }
            ) {
                Text(
                    text = "◀ GAUCHE",
                    color = Color.White.copy(alpha = 0.45f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }

            Surface(
                color = Color.White.copy(alpha = 0.05f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { engine.inputDirection = 1f }
            ) {
                Text(
                    text = "DROITE ▶",
                    color = Color.White.copy(alpha = 0.45f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }
    }
}

/**
 * Start Menu screen overlay: Title, high score, how to play, and Play button.
 */
@Composable
private fun MenuOverlay(
    engine: SpaceDodgerEngine
) {
    val infiniteTransition = rememberInfiniteTransition(label = "menu_glow")
    val titlePulse by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "title_pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Main App Title with Glow
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.scale(titlePulse)
            ) {
                Text(
                    text = "SPACE",
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 4.sp
                )
                Text(
                    text = "DODGER",
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Black,
                    color = NeonCyan,
                    letterSpacing = 4.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    color = NeonPurple.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonPurple.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "ARCADE SPATIALE 2D",
                        color = Color(0xFFD8B4FE),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // High Score Badge
            if (engine.highScore > 0) {
                Surface(
                    color = SpaceCard,
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, StarGold.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = "Trophée",
                            tint = StarGold,
                            modifier = Modifier.size(22.dp)
                        )
                        Column {
                            Text(
                                text = "MEILLEUR SCORE",
                                fontSize = 10.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${engine.highScore} PTS",
                                fontSize = 18.sp,
                                color = StarGold,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }

            // Power-ups & Rules Mini Card
            Card(
                colors = CardDefaults.cardColors(containerColor = SpaceCard.copy(alpha = 0.85f)),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SpaceCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "RÈGLES DU JEU",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            color = StarGold.copy(alpha = 0.2f),
                            shape = CircleShape,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Étoile",
                                    tint = StarGold,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Orbe Stellaire",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "+50 points bonus au score",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            color = ShieldCyan.copy(alpha = 0.2f),
                            shape = CircleShape,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = "Bouclier",
                                    tint = ShieldCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Bouclier Protecteur",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Absorbe le prochain astéroïde percuté",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            color = DangerRed.copy(alpha = 0.2f),
                            shape = CircleShape,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "☄",
                                    fontSize = 16.sp,
                                    color = DangerRed
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Astéroïdes",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Touche la gauche ou droite pour esquiver",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // Primary Play Button
            Button(
                onClick = { engine.startGame() },
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("play_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Jouer",
                        tint = Color.Black,
                        modifier = Modifier.size(26.dp)
                    )
                    Text(
                        text = "JOUER",
                        color = Color.Black,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

/**
 * Game Over overlay screen: Final score, high score celebration, game statistics,
 * and Play Again / Return to Menu buttons.
 */
@Composable
private fun GameOverOverlay(
    engine: SpaceDodgerEngine
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.78f))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 400.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Game Over Banner
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "GAME OVER",
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Black,
                    color = DangerRed,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "Collision spatiale fatale",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }

            // New High Score Toast / Badge
            if (engine.isNewHighScore) {
                Surface(
                    color = StarGold.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, StarGold)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = "Nouveau Record",
                            tint = StarGold,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "NOUVEAU RECORD !",
                            color = StarGold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            // Score Summary Card
            Card(
                colors = CardDefaults.cardColors(containerColor = SpaceCard),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SpaceCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Big Score
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "SCORE FINAL",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "${engine.score}",
                            fontSize = 46.sp,
                            fontWeight = FontWeight.Black,
                            color = NeonCyan
                        )
                    }

                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(SpaceCardBorder)
                    )

                    // Breakdown Stats
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "RECORD",
                                fontSize = 10.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${engine.highScore}",
                                fontSize = 18.sp,
                                color = StarGold,
                                fontWeight = FontWeight.Black
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "ESQUIVES",
                                fontSize = 10.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${engine.asteroidsDodged}",
                                fontSize = 18.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Black
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "ÉTOILES",
                                fontSize = 10.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${engine.starsCollected}",
                                fontSize = 18.sp,
                                color = StarGold,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }

            // Action Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Replay Button
                Button(
                    onClick = { engine.startGame() },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("replay_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Rejouer",
                            tint = Color.Black,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "REJOUER",
                            color = Color.Black,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                // Return to Menu Button
                OutlinedButton(
                    onClick = { engine.returnToMenu() },
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SpaceCardBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("menu_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Menu Principal",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "MENU PRINCIPAL",
                            color = TextSecondary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
