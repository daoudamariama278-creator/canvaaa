package com.example.game

import androidx.compose.ui.graphics.Color

/**
 * Game states of Space Dodger.
 */
enum class GameState {
    MENU,
    PLAYING,
    GAME_OVER
}

/**
 * Types of bonus pickups in space.
 */
enum class PowerUpType {
    STAR,    // +50 score bonus
    SHIELD   // Absorbs the next asteroid collision
}

/**
 * Player spaceship representation.
 * Coordinates x and y are normalized to [0.0, 1.0] across the screen.
 */
data class Player(
    val x: Float = 0.5f,
    val y: Float = 0.85f,
    val radiusNorm: Float = 0.05f,
    val hasShield: Boolean = false,
    val velocityX: Float = 0f,
    val tilt: Float = 0f, // Banking angle in degrees (-25f to 25f)
    val shieldPulse: Float = 0f
)

/**
 * Procedurally shaped space asteroid.
 */
data class Asteroid(
    val id: Long,
    val x: Float,
    val y: Float,
    val radiusNorm: Float,
    val speedY: Float,
    val rotation: Float = 0f,
    val rotationSpeed: Float = 45f,
    val shapePoints: List<Float> = emptyList(), // Normalized radial perturbations for rocky polygon
    val craterOffsets: List<Pair<Float, Float>> = emptyList()
)

/**
 * Power-up pickup entity.
 */
data class PowerUp(
    val id: Long,
    val x: Float,
    val y: Float,
    val type: PowerUpType,
    val radiusNorm: Float = 0.042f,
    val speedY: Float = 0.30f,
    val pulsePhase: Float = 0f
)

/**
 * Visual particle for thrusters, explosions, and pickups.
 */
data class Particle(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val color: Color,
    val radius: Float,
    val alpha: Float = 1f,
    val lifetime: Float = 0.6f,
    val maxLifetime: Float = 0.6f
)

/**
 * Floating score text indicator (e.g., "+50" or "BOUCLIER !").
 */
data class FloatingText(
    val id: Long,
    val text: String,
    val x: Float,
    val y: Float,
    val color: Color,
    val alpha: Float = 1f,
    val lifetime: Float = 0.9f
)

/**
 * Background star for parallax starfield.
 */
data class StarPoint(
    val x: Float,
    val y: Float,
    val size: Float,
    val speed: Float,
    val baseAlpha: Float,
    val twinkleSpeed: Float,
    val phase: Float
)
