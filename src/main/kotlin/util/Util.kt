package util

import no.njoh.pulseengine.core.PulseEngine
import no.njoh.pulseengine.core.asset.AssetManager
import no.njoh.pulseengine.core.asset.types.Asset
import no.njoh.pulseengine.core.asset.types.Font
import no.njoh.pulseengine.core.asset.types.Sound
import no.njoh.pulseengine.core.asset.types.Texture
import no.njoh.pulseengine.core.graphics.Surface2D
import no.njoh.pulseengine.core.shared.primitives.Color
import no.njoh.pulseengine.core.shared.utils.Extensions.loadFileNames
import kotlin.math.*

val PIf = PI.toFloat()

fun filterStickInput(xStick: Float, yStick: Float): Vec2
{
    val deadZone = 0.2f
    val length = sqrt(xStick * xStick + yStick * yStick)
    return if (length >= deadZone)
    {
        val angle = atan2(yStick, xStick)
        val scaledLength = (length - deadZone) / (1f - deadZone)
        val x = cos(angle) * scaledLength
        val y = -sin(angle) * scaledLength
        Vec2(x, y)
    }
    else Vec2(0f, 0f)
}

fun Surface2D.setDrawColor(color: Color, alpha: Float) =
    setDrawColor(color.red, color.green, color.blue, color.alpha * alpha)

fun PulseEngine.playSoundWithName(name: String)
{
    asset.getOrNull<Sound>(name)?.let { shotgunSound ->
        audio.play(audio.createSource(shotgunSound))
    }
}

inline fun <reified T: Asset> AssetManager.loadAll(directory: String)
{
    for (fileName in directory.loadFileNames())
    {
        val assetName = fileName.substringAfterLast("/").substringBeforeLast(".")
        when (T::class)
        {
            Font::class ->
                if (fileName.endsWith("ttf")) loadFont(fileName, assetName, arrayOf(80f).toFloatArray())
            Sound::class ->
                if (fileName.endsWith("ogg")) loadSound(fileName, assetName)
            Texture::class ->
                if (Texture.SUPPORTED_FORMATS.any { fileName.endsWith(it) }) loadTexture(fileName, assetName)
        }
    }
}

