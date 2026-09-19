package com.example.game

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.example.ui.theme.AsteroidRock
import com.example.ui.theme.DangerRed
import com.example.ui.theme.NeonBlue
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.ShieldCyan
import com.example.ui.theme.StarGold
import kotlin.math.cos
import kotlin.math.sin

/**
 * Procedural declarative 2D Canvas rendering for Space Dodger.
 */
object SpaceDodgerRender {

    /**
     * Renders the deep space starfield with parallax depth and twinkling stars.
     */
    fun drawStarfield(
        scope: DrawScope,
        stars: List<StarPoint>,
        timeSeconds: Float
    ) {
        val width = scope.size.width
        val height = scope.size.height

        for (star in stars) {
            val sx = star.x * width
            val sy = star.y * height

            // Twinkle brightness modulation
            val twinkle = (sin((timeSeconds * star.twinkleSpeed) + star.phase) + 1f) * 0.5f
            val alpha = (star.baseAlpha * 0.5f + twinkle * 0.5f).coerceIn(0.15f, 1f)

            val starColor = when {
                star.size > 2.5f -> Color.White.copy(alpha = alpha)
                star.size > 1.8f -> NeonCyan.copy(alpha = alpha * 0.85f)
                else -> Color(0xFF90CAF9).copy(alpha = alpha * 0.7f)
            }

            scope.drawCircle(
                color = starColor,
                radius = star.size,
                center = Offset(sx, sy)
            )

            // Cross flare on bright large stars
            if (star.size > 2.6f && alpha > 0.7f) {
                val flareLen = star.size * 2.8f
                scope.drawLine(
                    color = Color.White.copy(alpha = alpha * 0.6f),
                    start = Offset(sx - flareLen, sy),
                    end = Offset(sx + flareLen, sy),
                    strokeWidth = 1f
                )
                scope.drawLine(
                    color = Color.White.copy(alpha = alpha * 0.6f),
                    start = Offset(sx, sy - flareLen),
                    end = Offset(sx, sy + flareLen),
                    strokeWidth = 1f
                )
            }
        }
    }

    /**
     * Renders the player spaceship, its thruster fire, banking tilt, and energy shield.
     */
    fun drawPlayer(
        scope: DrawScope,
        player: Player,
        timeSeconds: Float
    ) {
        val width = scope.size.width
        val height = scope.size.height
        val px = player.x * width
        val py = player.y * height
        val shipRadiusPx = player.radiusNorm * width
        val shipH = shipRadiusPx * 2.4f
        val shipW = shipRadiusPx * 2.1f

        scope.rotate(degrees = player.tilt, pivot = Offset(px, py)) {
            // 1. Thruster Plume (Flame)
            val flicker = (sin(timeSeconds * 30f) * 0.2f + 0.8f)
            val flameHeight = shipH * 0.65f * flicker
            val flameWidth = shipW * 0.35f

            val outerFlamePath = Path().apply {
                moveTo(px - flameWidth * 0.5f, py + shipH * 0.35f)
                quadraticTo(
                    px, py + shipH * 0.35f + flameHeight,
                    px + flameWidth * 0.5f, py + shipH * 0.35f
                )
                close()
            }
            scope.drawPath(
                path = outerFlamePath,
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFFF9500), DangerRed, Color.Transparent),
                    startY = py + shipH * 0.35f,
                    endY = py + shipH * 0.35f + flameHeight
                )
            )

            val innerFlamePath = Path().apply {
                moveTo(px - flameWidth * 0.25f, py + shipH * 0.35f)
                quadraticTo(
                    px, py + shipH * 0.35f + flameHeight * 0.6f,
                    px + flameWidth * 0.25f, py + shipH * 0.35f
                )
                close()
            }
            scope.drawPath(
                path = innerFlamePath,
                brush = Brush.verticalGradient(
                    colors = listOf(Color.White, NeonCyan),
                    startY = py + shipH * 0.35f,
                    endY = py + shipH * 0.35f + flameHeight * 0.6f
                )
            )

            // 2. Spaceship Wings (Left & Right Delta Wings)
            val wingPath = Path().apply {
                // Nose
                moveTo(px, py - shipH * 0.55f)
                // Right wingtip
                lineTo(px + shipW * 0.55f, py + shipH * 0.38f)
                // Right inner engine intake
                lineTo(px + shipW * 0.22f, py + shipH * 0.28f)
                // Engine rear
                lineTo(px + shipW * 0.16f, py + shipH * 0.38f)
                lineTo(px - shipW * 0.16f, py + shipH * 0.38f)
                // Left inner engine intake
                lineTo(px - shipW * 0.22f, py + shipH * 0.28f)
                // Left wingtip
                lineTo(px - shipW * 0.55f, py + shipH * 0.38f)
                close()
            }

            // Dark metallic hull gradient
            scope.drawPath(
                path = wingPath,
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF2A3A5E), Color(0xFF10182C), Color(0xFF0C111E)),
                    startY = py - shipH * 0.55f,
                    endY = py + shipH * 0.38f
                )
            )

            // Hull border outline with neon cyan glow
            scope.drawPath(
                path = wingPath,
                color = NeonCyan.copy(alpha = 0.95f),
                style = Stroke(
                    width = 2.5f,
                    join = StrokeJoin.Round,
                    cap = StrokeCap.Round
                )
            )

            // Wingtip glow lamps
            scope.drawCircle(
                color = NeonPink,
                radius = 3.5f,
                center = Offset(px - shipW * 0.54f, py + shipH * 0.36f)
            )
            scope.drawCircle(
                color = NeonCyan,
                radius = 3.5f,
                center = Offset(px + shipW * 0.54f, py + shipH * 0.36f)
            )

            // 3. Central Cockpit Fuselage
            val cockpitPath = Path().apply {
                moveTo(px, py - shipH * 0.48f)
                lineTo(px + shipW * 0.14f, py + shipH * 0.05f)
                lineTo(px - shipW * 0.14f, py + shipH * 0.05f)
                close()
            }
            scope.drawPath(
                path = cockpitPath,
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFE0F7FA), NeonCyan, Color(0xFF006064)),
                    startY = py - shipH * 0.48f,
                    endY = py + shipH * 0.05f
                )
            )

            // Specular canopy reflection line
            scope.drawLine(
                color = Color.White.copy(alpha = 0.85f),
                start = Offset(px - shipW * 0.04f, py - shipH * 0.42f),
                end = Offset(px - shipW * 0.02f, py - shipH * 0.10f),
                strokeWidth = 2f
            )
        }

        // 4. Energy Shield (if active)
        if (player.hasShield) {
            val pulse = (sin(player.shieldPulse * 5f) * 0.08f + 1f)
            val shieldRadius = shipRadiusPx * 1.55f * pulse

            // Outer glow aura
            scope.drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        ShieldCyan.copy(alpha = 0.18f),
                        ShieldCyan.copy(alpha = 0.06f),
                        Color.Transparent
                    ),
                    center = Offset(px, py),
                    radius = shieldRadius * 1.25f
                ),
                radius = shieldRadius * 1.25f,
                center = Offset(px, py)
            )

            // Shield main forcefield rim
            scope.drawCircle(
                color = ShieldCyan.copy(alpha = 0.8f),
                radius = shieldRadius,
                center = Offset(px, py),
                style = Stroke(width = 3f)
            )

            // Rotating orbital shield energy arcs
            val orbitAngle = (timeSeconds * 120f) % 360f
            scope.rotate(degrees = orbitAngle, pivot = Offset(px, py)) {
                scope.drawArc(
                    color = Color.White.copy(alpha = 0.9f),
                    startAngle = 0f,
                    sweepAngle = 45f,
                    useCenter = false,
                    topLeft = Offset(px - shieldRadius, py - shieldRadius),
                    size = Size(shieldRadius * 2, shieldRadius * 2),
                    style = Stroke(width = 4f, cap = StrokeCap.Round)
                )
                scope.drawArc(
                    color = Color.White.copy(alpha = 0.9f),
                    startAngle = 180f,
                    sweepAngle = 45f,
                    useCenter = false,
                    topLeft = Offset(px - shieldRadius, py - shieldRadius),
                    size = Size(shieldRadius * 2, shieldRadius * 2),
                    style = Stroke(width = 4f, cap = StrokeCap.Round)
                )
            }
        }
    }

    /**
     * Renders a procedurally generated rocky asteroid with craters, shading and rotation.
     */
    fun drawAsteroid(
        scope: DrawScope,
        asteroid: Asteroid
    ) {
        val width = scope.size.width
        val height = scope.size.height
        val ax = asteroid.x * width
        val ay = asteroid.y * height
        val r = asteroid.radiusNorm * width

        if (ay + r < 0 || ay - r > height) return

        scope.rotate(degrees = asteroid.rotation, pivot = Offset(ax, ay)) {
            val path = Path()
            val pointsCount = asteroid.shapePoints.size
            if (pointsCount >= 6) {
                val angleStep = (2f * Math.PI.toFloat()) / pointsCount
                for (i in 0 until pointsCount) {
                    val angle = i * angleStep
                    val perturbedRadius = r * asteroid.shapePoints[i]
                    val px = ax + perturbedRadius * cos(angle)
                    val py = ay + perturbedRadius * sin(angle)
                    if (i == 0) {
                        path.moveTo(px, py)
                    } else {
                        path.lineTo(px, py)
                    }
                }
                path.close()
            } else {
                // Fallback circle
                path.addOval(androidx.compose.ui.geometry.Rect(ax - r, ay - r, ax + r, ay + r))
            }

            // Rocky asteroid lighting gradient: lit top-left, shadowed bottom-right
            scope.drawPath(
                path = path,
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFFB0A898), AsteroidRock, Color(0xFF38332C), Color(0xFF1E1A16)),
                    start = Offset(ax - r * 0.8f, ay - r * 0.8f),
                    end = Offset(ax + r * 0.8f, ay + r * 0.8f)
                ),
                style = Fill
            )

            // Outline highlight
            scope.drawPath(
                path = path,
                color = Color(0xFFC7BFA6).copy(alpha = 0.5f),
                style = Stroke(width = 1.8f)
            )

            // Procedural craters
            for (crater in asteroid.craterOffsets) {
                val cx = ax + crater.first * r * 0.55f
                val cy = ay + crater.second * r * 0.55f
                val cr = r * 0.22f

                // Crater shadow
                scope.drawCircle(
                    color = Color(0xFF221E19),
                    radius = cr,
                    center = Offset(cx, cy)
                )
                // Crater highlighted rim edge
                scope.drawArc(
                    color = Color(0xFFD6CEB5).copy(alpha = 0.6f),
                    startAngle = 135f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(cx - cr, cy - cr),
                    size = Size(cr * 2, cr * 2),
                    style = Stroke(width = 1.5f)
                )
            }
        }
    }

    /**
     * Renders bonus power-ups:
     * - STAR: Golden sparkling orb (+50 points).
     * - SHIELD: Cyan energy bubble with defensive crest.
     */
    fun drawPowerUp(
        scope: DrawScope,
        powerUp: PowerUp,
        timeSeconds: Float
    ) {
        val width = scope.size.width
        val height = scope.size.height
        val px = powerUp.x * width
        val py = powerUp.y * height
        val r = powerUp.radiusNorm * width

        if (py + r < 0 || py - r > height) return

        when (powerUp.type) {
            PowerUpType.STAR -> {
                val pulse = (sin(powerUp.pulsePhase * 6f) * 0.15f + 1f)
                val starRadius = r * pulse

                // Golden outer glow
                scope.drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            StarGold.copy(alpha = 0.45f),
                            StarGold.copy(alpha = 0.15f),
                            Color.Transparent
                        ),
                        center = Offset(px, py),
                        radius = starRadius * 2.2f
                    ),
                    radius = starRadius * 2.2f,
                    center = Offset(px, py)
                )

                // 4-Pointed Golden Star
                val rotationAngle = (timeSeconds * 90f) % 360f
                scope.rotate(degrees = rotationAngle, pivot = Offset(px, py)) {
                    val starPath = Path().apply {
                        val outerR = starRadius * 1.35f
                        val innerR = starRadius * 0.45f
                        val points = 8 // 4 outer tips, 4 inner notches
                        val step = (2f * Math.PI.toFloat()) / points
                        for (i in 0 until points) {
                            val curR = if (i % 2 == 0) outerR else innerR
                            val angle = i * step - Math.PI.toFloat() / 2f
                            val sx = px + curR * cos(angle)
                            val sy = py + curR * sin(angle)
                            if (i == 0) moveTo(sx, sy) else lineTo(sx, sy)
                        }
                        close()
                    }

                    scope.drawPath(
                        path = starPath,
                        brush = Brush.radialGradient(
                            colors = listOf(Color.White, StarGold, Color(0xFFFF9500)),
                            center = Offset(px, py),
                            radius = starRadius * 1.35f
                        )
                    )
                    scope.drawPath(
                        path = starPath,
                        color = Color.White,
                        style = Stroke(width = 1.5f)
                    )
                }

                // Core brilliant spark
                scope.drawCircle(
                    color = Color.White,
                    radius = starRadius * 0.35f,
                    center = Offset(px, py)
                )
            }

            PowerUpType.SHIELD -> {
                val pulse = (sin(powerUp.pulsePhase * 5f) * 0.12f + 1f)
                val shieldR = r * pulse

                // Cyan outer aura
                scope.drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            ShieldCyan.copy(alpha = 0.5f),
                            NeonBlue.copy(alpha = 0.18f),
                            Color.Transparent
                        ),
                        center = Offset(px, py),
                        radius = shieldR * 2.0f
                    ),
                    radius = shieldR * 2.0f,
                    center = Offset(px, py)
                )

                // Outer sphere ring
                scope.drawCircle(
                    color = ShieldCyan,
                    radius = shieldR,
                    center = Offset(px, py),
                    style = Stroke(width = 2.5f)
                )

                // Translucent energy sphere fill
                scope.drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(ShieldCyan.copy(alpha = 0.4f), NeonBlue.copy(alpha = 0.15f)),
                        center = Offset(px, py),
                        radius = shieldR
                    ),
                    radius = shieldR,
                    center = Offset(px, py)
                )

                // Inner Shield Crest Icon
                val iconH = shieldR * 1.1f
                val iconW = shieldR * 0.9f
                val crestPath = Path().apply {
                    moveTo(px, py - iconH * 0.5f)
                    lineTo(px + iconW * 0.5f, py - iconH * 0.25f)
                    lineTo(px + iconW * 0.42f, py + iconH * 0.20f)
                    lineTo(px, py + iconH * 0.55f)
                    lineTo(px - iconW * 0.42f, py + iconH * 0.20f)
                    lineTo(px - iconW * 0.5f, py - iconH * 0.25f)
                    close()
                }
                scope.drawPath(
                    path = crestPath,
                    color = Color.White,
                    style = Stroke(width = 2f, join = StrokeJoin.Round)
                )
            }
        }
    }

    /**
     * Renders visual explosion, thruster, and spark particles.
     */
    fun drawParticles(
        scope: DrawScope,
        particles: List<Particle>
    ) {
        val width = scope.size.width
        val height = scope.size.height

        for (p in particles) {
            val px = p.x * width
            val py = p.y * height
            val alpha = (p.lifetime / p.maxLifetime).coerceIn(0f, 1f)

            scope.drawCircle(
                color = p.color.copy(alpha = alpha),
                radius = p.radius * (0.4f + 0.6f * alpha),
                center = Offset(px, py)
            )
        }
    }
}
