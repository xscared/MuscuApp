package com.example.muscuapp.ui.components

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sign

/**
 * Une forme Superellipse (Squircle) qui se rapproche du design iOS/Xiaomi.
 * n = 3.0 donne un aspect organique et moderne, loin du Material.
 */
class SquircleShape(private val n: Float = 3.0f) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            val a = size.width / 2f
            val b = size.height / 2f
            
            for (i in 0..360) {
                val angle = Math.toRadians(i.toDouble())
                val cos = Math.cos(angle)
                val sin = Math.sin(angle)
                
                val x = abs(cos).pow(2.0 / n).copySign(cos) * a + a
                val y = abs(sin).pow(2.0 / n).copySign(sin) * b + b
                
                if (i == 0) moveTo(x.toFloat(), y.toFloat())
                else lineTo(x.toFloat(), y.toFloat())
            }
            close()
        }
        return Outline.Generic(path)
    }

    private fun Double.copySign(signSource: Double): Double {
        return abs(this) * sign(signSource)
    }
}

/**
 * Une forme asymétrique personnalisée pour les éléments de liste.
 * Arrondit seulement le haut-gauche et le bas-droite de manière prononcée.
 */
class AsymmetricCardShape(val radius: Float = 32f) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            moveTo(radius, 0f)
            lineTo(size.width, 0f)
            lineTo(size.width, size.height - radius)
            quadraticTo(size.width, size.height, size.width - radius, size.height)
            lineTo(0f, size.height)
            lineTo(0f, radius)
            quadraticTo(0f, 0f, radius, 0f)
            close()
        }
        return Outline.Generic(path)
    }
}
