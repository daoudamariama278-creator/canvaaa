package com.example.game

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.AsteroidRock
import com.example.ui.theme.DangerRed
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.ShieldCyan
import com.example.ui.theme.StarGold
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Core game engine holding state, updating physics, collisions, and spawns.
 */
class SpaceDodgerEngine(
    context: Context? = null
) {
    private val prefs = context?.getSharedPreferences("space_dodger_prefs", Context.MODE_PRIVATE)
    private val feedback = context?.let { SpaceDodgerFeedback(it) }

    // Game states
    var gameState by mutableStateOf(GameState.MENU)
        private set

    var score by mutableIntStateOf(0)
        private set

    var highScore by mutableIntStateOf(prefs?.getInt("high_score", 0) ?: 0)
        private set

    var isNewHighScore by mutableStateOf(false)
        private set

    var elapsedTime by mutableFloatStateOf(0f)
        private set

    var asteroidsDodged by mutableIntStateOf(0)
        private set

    var starsCollected by mutableIntStateOf(0)
        private set

    var player by mutableStateOf(Player())
        private set

    // Entities lists
    val asteroids = mutableStateListOf<Asteroid>()
    val powerUps = mutableStateListOf<PowerUp>()
    val particles = mutableStateListOf<Particle>()
    val floatingTexts = mutableStateListOf<FloatingText>()
    val stars = mutableStateListOf<StarPoint>()

    // Timers
    private var asteroidSpawnTimer = 0f
    private var powerUpSpawnTimer = 0f
    private var passiveScoreTimer = 0f
    private var entityIdCounter = 0L

    // Screen Shake effect
    var shakeOffset by mutableStateOf(Pair(0f, 0f))
        private set
    private var shakeDuration = 0f

    // Touch control input state: -1f (left), +1f (right), or 0f (neutral)
    var inputDirection by mutableFloatStateOf(0f)
    var directTouchTargetX by mutableStateOf<Float?>(null)

    init {
        generateStarfield(80)
    }

    private fun generateStarfield(count: Int) {
        stars.clear()
        for (i in 0 until count) {
            stars.add(
                StarPoint(
                    x = Random.nextFloat(),
                    y = Random.nextFloat(),
                    size = Random.nextFloat() * 2.2f + 0.8f,
                    speed = Random.nextFloat() * 0.12f + 0.04f,
                    baseAlpha = Random.nextFloat() * 0.65f + 0.35f,
                    twinkleSpeed = Random.nextFloat() * 4f + 2f,
                    phase = Random.nextFloat() * 6.28f
                )
            )
        }
    }

    fun startGame() {
        score = 0
        elapsedTime = 0f
        asteroidsDodged = 0
        starsCollected = 0
        isNewHighScore = false
        asteroidSpawnTimer = 0.5f // Quick first asteroid
        powerUpSpawnTimer = 3.5f // First powerup in 3.5s
        passiveScoreTimer = 0f
        shakeDuration = 0f
        shakeOffset = Pair(0f, 0f)

        player = Player(
            x = 0.5f,
            y = 0.84f,
            radiusNorm = 0.05f,
            hasShield = false,
            velocityX = 0f,
            tilt = 0f
        )

        asteroids.clear()
        powerUps.clear()
        particles.clear()
        floatingTexts.clear()

        gameState = GameState.PLAYING
    }

    fun returnToMenu() {
        gameState = GameState.MENU
        asteroids.clear()
        powerUps.clear()
        particles.clear()
        floatingTexts.clear()
    }

    /**
     * Primary tick updated every frame from LaunchedEffect withFrameNanos.
     */
    fun update(deltaTimeNanos: Long) {
        val dt = (deltaTimeNanos / 1_000_000_000f).coerceIn(0.001f, 0.05f)
        elapsedTime += dt

        // Always update starfield so background moves smoothly even in menu
        updateStars(dt)

        if (gameState == GameState.PLAYING) {
            updatePlayer(dt)
            updateAsteroids(dt)
            updatePowerUps(dt)
            checkCollisions()
            updatePassiveScore(dt)
            updateDifficultyAndSpawns(dt)
        }

        updateParticles(dt)
        updateFloatingTexts(dt)
        updateScreenShake(dt)
    }

    private fun updateStars(dt: Float) {
        val speedFactor = if (gameState == GameState.PLAYING) 1.5f else 0.5f
        for (i in stars.indices) {
            val s = stars[i]
            var newY = s.y + s.speed * speedFactor * dt
            var newX = s.x
            if (newY > 1.02f) {
                newY = -0.02f
                newX = Random.nextFloat()
            }
            stars[i] = s.copy(x = newX, y = newY)
        }
    }

    private fun updatePlayer(dt: Float) {
        var targetVx = 0f

        // 1. Direct Touch Target has priority (smooth glide towards finger)
        directTouchTargetX?.let { targetX ->
            val diff = targetX - player.x
            val dir = when {
                diff > 0.015f -> 1f
                diff < -0.015f -> -1f
                else -> diff / 0.015f
            }
            targetVx = dir * 1.15f
        } ?: run {
            // 2. Button / Screen half tap controls
            targetVx = inputDirection * 1.15f
        }

        // Smooth velocity acceleration and deceleration
        val currentVx = player.velocityX + (targetVx - player.velocityX) * (dt * 14f)

        // Clamping to screen boundaries
        val halfRadius = player.radiusNorm
        val newX = (player.x + currentVx * dt).coerceIn(halfRadius, 1f - halfRadius)

        // Banking tilt (-24 degrees for left, +24 degrees for right)
        val targetTilt = (currentVx / 1.15f).coerceIn(-1f, 1f) * 24f
        val newTilt = player.tilt + (targetTilt - player.tilt) * (dt * 12f)

        // Shield pulse animation
        val newShieldPulse = player.shieldPulse + dt

        player = player.copy(
            x = newX,
            velocityX = currentVx,
            tilt = newTilt,
            shieldPulse = newShieldPulse
        )

        // Occasional thruster sparks
        if (Random.nextFloat() < 0.35f) {
            spawnThrusterParticle(player.x, player.y + 0.035f)
        }
    }

    private fun updateDifficultyAndSpawns(dt: Float) {
        // Difficulty scaling formula:
        // Spawn rate scales with score: from 1.15s down to 0.38s
        val currentSpawnInterval = (1.15f - (score / 1800f) * 0.77f).coerceAtLeast(0.38f)

        asteroidSpawnTimer -= dt
        if (asteroidSpawnTimer <= 0f) {
            spawnAsteroid()
            asteroidSpawnTimer = currentSpawnInterval * (0.85f + Random.nextFloat() * 0.35f)
        }

        // Power-up spawn timer: every 6 to 9 seconds
        powerUpSpawnTimer -= dt
        if (powerUpSpawnTimer <= 0f) {
            spawnPowerUp()
            powerUpSpawnTimer = 6.5f + Random.nextFloat() * 3.5f
        }
    }

    private fun spawnAsteroid() {
        entityIdCounter++
        // Falling speed increases with score and time
        val speedMultiplier = 1.0f + (score / 800f) * 0.35f + (elapsedTime / 90f) * 0.25f
        val baseSpeed = Random.nextFloat() * 0.22f + 0.28f
        val finalSpeed = (baseSpeed * speedMultiplier).coerceAtMost(0.85f)

        // Radius: variance from fast small rock (0.04) to large heavy asteroid (0.075)
        val radiusNorm = Random.nextFloat() * 0.035f + 0.040f

        // Procedural polygon vertices (10 points around radial circumference)
        val vertexCount = 10
        val points = List(vertexCount) {
            0.78f + Random.nextFloat() * 0.44f // 0.78 to 1.22
        }

        // Procedural craters
        val craterCount = Random.nextInt(1, 4)
        val craters = List(craterCount) {
            Pair(
                Random.nextFloat() * 1.2f - 0.6f,
                Random.nextFloat() * 1.2f - 0.6f
            )
        }

        asteroids.add(
            Asteroid(
                id = entityIdCounter,
                x = Random.nextFloat() * 0.84f + 0.08f,
                y = -radiusNorm - 0.02f,
                radiusNorm = radiusNorm,
                speedY = finalSpeed,
                rotation = Random.nextFloat() * 360f,
                rotationSpeed = (Random.nextFloat() * 120f - 60f),
                shapePoints = points,
                craterOffsets = craters
            )
        )
    }

    private fun spawnPowerUp() {
        entityIdCounter++
        // Choose type: Star (70%) or Shield (30%)
        // If player already has shield, give 90% star
        val type = if (player.hasShield) {
            if (Random.nextFloat() < 0.10f) PowerUpType.SHIELD else PowerUpType.STAR
        } else {
            if (Random.nextFloat() < 0.32f) PowerUpType.SHIELD else PowerUpType.STAR
        }

        powerUps.add(
            PowerUp(
                id = entityIdCounter,
                x = Random.nextFloat() * 0.80f + 0.10f,
                y = -0.05f,
                type = type,
                radiusNorm = 0.042f,
                speedY = 0.26f + Random.nextFloat() * 0.06f,
                pulsePhase = Random.nextFloat() * 6.28f
            )
        )
    }

    private fun updateAsteroids(dt: Float) {
        val iterator = asteroids.listIterator()
        while (iterator.hasNext()) {
            val asteroid = iterator.next()
            val newY = asteroid.y + asteroid.speedY * dt
            val newRot = (asteroid.rotation + asteroid.rotationSpeed * dt) % 360f

            if (newY > 1.08f) {
                // Dodged! Award points
                score += 10
                asteroidsDodged++
                iterator.remove()
            } else {
                iterator.set(asteroid.copy(y = newY, rotation = newRot))
            }
        }
    }

    private fun updatePowerUps(dt: Float) {
        val iterator = powerUps.listIterator()
        while (iterator.hasNext()) {
            val pu = iterator.next()
            val newY = pu.y + pu.speedY * dt
            val newPulse = pu.pulsePhase + dt

            if (newY > 1.08f) {
                iterator.remove()
            } else {
                iterator.set(pu.copy(y = newY, pulsePhase = newPulse))
            }
        }
    }

    private fun checkCollisions() {
        val px = player.x
        val py = player.y
        val pr = player.radiusNorm

        // 1. Collisions: Player vs Power-ups
        val puIterator = powerUps.listIterator()
        while (puIterator.hasNext()) {
            val pu = puIterator.next()
            val dx = px - pu.x
            // Adjust vertical distance scale for typical 16:9 or 20:9 aspect ratio (~1.8x)
            val dy = (py - pu.y) * 1.85f
            val dist = sqrt(dx * dx + dy * dy)
            val hitDist = pr + pu.radiusNorm

            if (dist <= hitDist) {
                puIterator.remove()
                feedback?.playPickupHaptic()

                when (pu.type) {
                    PowerUpType.STAR -> {
                        score += 50
                        starsCollected++
                        spawnSparkleBurst(pu.x, pu.y, StarGold, 18)
                        addFloatingText("+50", pu.x, pu.y, StarGold)
                    }
                    PowerUpType.SHIELD -> {
                        player = player.copy(hasShield = true)
                        spawnSparkleBurst(pu.x, pu.y, ShieldCyan, 20)
                        addFloatingText("BOUCLIER !", pu.x, pu.y, ShieldCyan)
                    }
                }
            }
        }

        // 2. Collisions: Player vs Asteroids
        val astIterator = asteroids.listIterator()
        while (astIterator.hasNext()) {
            val asteroid = astIterator.next()
            val dx = px - asteroid.x
            val dy = (py - asteroid.y) * 1.85f
            val dist = sqrt(dx * dx + dy * dy)
            val hitDist = (pr * 0.85f) + (asteroid.radiusNorm * 0.85f)

            if (dist <= hitDist) {
                if (player.hasShield) {
                    // Shield absorbs collision!
                    astIterator.remove()
                    player = player.copy(hasShield = false)
                    score += 25
                    feedback?.playShieldBreakHaptic()
                    triggerScreenShake(0.25f, 14f)

                    // Shield explosion particles (rock debris + cyan sparks)
                    spawnRockDebris(asteroid.x, asteroid.y, 22)
                    spawnSparkleBurst(player.x, player.y, ShieldCyan, 18)
                    addFloatingText("BOUCLIER CONSOMMÉ", player.x, player.y - 0.06f, ShieldCyan)
                } else {
                    // FATAL COLLISION -> GAME OVER!
                    astIterator.remove()
                    triggerGameOver()
                    break
                }
            }
        }
    }

    private fun triggerGameOver() {
        gameState = GameState.GAME_OVER
        feedback?.playGameOverHaptic()
        triggerScreenShake(0.5f, 24f)

        // Ship explosion particles
        spawnShipExplosion(player.x, player.y)

        // Check and save High Score
        if (score > highScore) {
            highScore = score
            isNewHighScore = true
            prefs?.edit()?.putInt("high_score", score)?.apply()
        }
    }

    private fun updatePassiveScore(dt: Float) {
        passiveScoreTimer += dt
        if (passiveScoreTimer >= 0.25f) {
            score += 2
            passiveScoreTimer = 0f
        }
    }

    private fun triggerScreenShake(duration: Float, intensity: Float) {
        shakeDuration = duration
    }

    private fun updateScreenShake(dt: Float) {
        if (shakeDuration > 0f) {
            shakeDuration -= dt
            val intensity = (shakeDuration / 0.5f).coerceIn(0f, 1f) * 18f
            shakeOffset = Pair(
                (Random.nextFloat() * 2f - 1f) * intensity,
                (Random.nextFloat() * 2f - 1f) * intensity
            )
        } else {
            shakeOffset = Pair(0f, 0f)
        }
    }

    private fun updateParticles(dt: Float) {
        val iterator = particles.listIterator()
        while (iterator.hasNext()) {
            val p = iterator.next()
            val newLifetime = p.lifetime - dt
            if (newLifetime <= 0f) {
                iterator.remove()
            } else {
                iterator.set(
                    p.copy(
                        x = p.x + p.vx * dt,
                        y = p.y + p.vy * dt,
                        lifetime = newLifetime
                    )
                )
            }
        }
    }

    private fun updateFloatingTexts(dt: Float) {
        val iterator = floatingTexts.listIterator()
        while (iterator.hasNext()) {
            val ft = iterator.next()
            val newLife = ft.lifetime - dt
            if (newLife <= 0f) {
                iterator.remove()
            } else {
                iterator.set(
                    ft.copy(
                        y = ft.y - 0.08f * dt,
                        alpha = (newLife / 0.9f).coerceIn(0f, 1f),
                        lifetime = newLife
                    )
                )
            }
        }
    }

    private fun addFloatingText(text: String, x: Float, y: Float, color: Color) {
        entityIdCounter++
        floatingTexts.add(
            FloatingText(
                id = entityIdCounter,
                text = text,
                x = x.coerceIn(0.12f, 0.88f),
                y = y.coerceIn(0.1f, 0.85f),
                color = color
            )
        )
    }

    private fun spawnThrusterParticle(x: Float, y: Float) {
        if (particles.size > 120) return
        val angle = Math.PI.toFloat() / 2f + (Random.nextFloat() * 0.4f - 0.2f)
        val speed = Random.nextFloat() * 0.15f + 0.10f
        particles.add(
            Particle(
                x = x + (Random.nextFloat() * 0.02f - 0.01f),
                y = y,
                vx = cos(angle) * speed,
                vy = sin(angle) * speed,
                color = if (Random.nextBoolean()) Color(0xFFFF9500) else NeonCyan,
                radius = Random.nextFloat() * 3.5f + 2f,
                lifetime = 0.28f,
                maxLifetime = 0.28f
            )
        )
    }

    private fun spawnSparkleBurst(x: Float, y: Float, color: Color, count: Int) {
        for (i in 0 until count) {
            val angle = Random.nextFloat() * 6.28f
            val speed = Random.nextFloat() * 0.35f + 0.10f
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = cos(angle) * speed,
                    vy = sin(angle) * speed,
                    color = color,
                    radius = Random.nextFloat() * 4.5f + 2f,
                    lifetime = 0.55f,
                    maxLifetime = 0.55f
                )
            )
        }
    }

    private fun spawnRockDebris(x: Float, y: Float, count: Int) {
        for (i in 0 until count) {
            val angle = Random.nextFloat() * 6.28f
            val speed = Random.nextFloat() * 0.45f + 0.12f
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = cos(angle) * speed,
                    vy = sin(angle) * speed,
                    color = if (i % 2 == 0) AsteroidRock else Color(0xFFD6CEB5),
                    radius = Random.nextFloat() * 5f + 2.5f,
                    lifetime = 0.65f,
                    maxLifetime = 0.65f
                )
            )
        }
    }

    private fun spawnShipExplosion(x: Float, y: Float) {
        for (i in 0 until 35) {
            val angle = Random.nextFloat() * 6.28f
            val speed = Random.nextFloat() * 0.65f + 0.15f
            val color = when {
                i % 4 == 0 -> NeonCyan
                i % 4 == 1 -> DangerRed
                i % 4 == 2 -> Color(0xFFFF9500)
                else -> Color.White
            }
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = cos(angle) * speed,
                    vy = sin(angle) * speed,
                    color = color,
                    radius = Random.nextFloat() * 6f + 3f,
                    lifetime = 0.85f,
                    maxLifetime = 0.85f
                )
            )
        }
    }
}
