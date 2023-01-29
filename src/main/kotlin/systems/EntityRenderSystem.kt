package systems

import entities.Bullet
import entities.Decal
import no.njoh.pulseengine.core.PulseEngine
import no.njoh.pulseengine.core.asset.types.Texture
import no.njoh.pulseengine.core.graphics.Surface
import no.njoh.pulseengine.core.graphics.Surface2D
import no.njoh.pulseengine.core.graphics.api.BlendFunction
import no.njoh.pulseengine.core.graphics.api.ShaderProgram
import no.njoh.pulseengine.core.graphics.postprocessing.MultiPassEffect
import no.njoh.pulseengine.core.graphics.postprocessing.SinglePassEffect
import no.njoh.pulseengine.core.graphics.postprocessing.effects.BlurEffect
import no.njoh.pulseengine.core.scene.SceneEntity
import no.njoh.pulseengine.core.scene.systems.EntityRendererImpl
import no.njoh.pulseengine.core.shared.annotations.ScnProp
import no.njoh.pulseengine.core.shared.primitives.Color
import shared.BULLET_SURFACE
import shared.DECAL_SURFACE
import shared.DECAL_MASK_SURFACE
import shared.AO_SURFACE

class EntityRenderSystem : EntityRendererImpl()
{
    @ScnProp(min = 0f, max = 1f) var sizeAO = 0.24f
    @ScnProp(min = 0f, max = 1f) var opacityAO = 0.44f

    private lateinit var overlayEffect: OverlayEffect
    private lateinit var aoEffect: AmbientOcclusionEffect

    override fun onCreate(engine: PulseEngine)
    {
        val bulletSurface = engine.gfx.createSurface(
            name = BULLET_SURFACE,
            camera = engine.gfx.mainCamera,
            zOrder = engine.gfx.mainSurface.context.zOrder - 1, // On top of main surface
            blendFunction = BlendFunction.ADDITIVE,
        ).addPostProcessingEffect(BlurEffect(radius = 0.024f, blurPasses = 1))

        val decalMaskSurface = engine.gfx.createSurface(
            name = DECAL_MASK_SURFACE,
            camera = engine.gfx.mainCamera,
            zOrder = engine.gfx.mainSurface.context.zOrder + 2, // Before decal surface
            isVisible = false
        )

        val decalSurface = engine.gfx.createSurface(
            name = DECAL_SURFACE,
            camera = engine.gfx.mainCamera,
            zOrder = engine.gfx.mainSurface.context.zOrder + 1, // Before main surface
            isVisible = false
        ).addPostProcessingEffect(MaskEffect(decalMaskSurface))

        val aoSurface = engine.gfx.createSurface(
            name = AO_SURFACE,
            camera = engine.gfx.mainCamera,
            zOrder = engine.gfx.mainSurface.context.zOrder + 1,
            backgroundColor = Color(0f, 0f, 0f, 0f),
            isVisible = false
        )

        // Overlay decals and ambient occlusion effects on main surface
        overlayEffect = OverlayEffect(decalSurface)
        aoEffect = AmbientOcclusionEffect(aoSurface)
        engine.gfx.mainSurface
            .addPostProcessingEffect(overlayEffect)
            .addPostProcessingEffect(aoEffect)

        // Add render passes
        addRenderPass(RenderPass(surfaceName = aoSurface.name, targetType = AOMask::class))
        addRenderPass(RenderPass(surfaceName = decalMaskSurface.name, targetType = DecalMask::class))
        addRenderPass(RenderPass(surfaceName = decalSurface.name, targetType = Decal::class))
        addRenderPass(RenderPass(surfaceName = bulletSurface.name, targetType = Bullet::class))
        addRenderPass(RenderPass(
            surfaceName = engine.gfx.mainSurface.name,
            targetType = SceneEntity::class,
            drawCondition = { it !is Bullet && it !is Decal }
        ))
    }

    override fun onUpdate(engine: PulseEngine)
    {
        aoEffect.size = sizeAO
        aoEffect.opacity = opacityAO
        aoEffect.scale = engine.gfx.mainCamera.scale.x
    }

    override fun onDestroy(engine: PulseEngine)
    {
        super.onDestroy(engine)

        engine.gfx.deleteSurface(AO_SURFACE)
        engine.gfx.deleteSurface(BULLET_SURFACE)
        engine.gfx.deleteSurface(DECAL_MASK_SURFACE)
        engine.gfx.deleteSurface(DECAL_SURFACE)
        engine.gfx.mainSurface.removePostProcessingEffect(overlayEffect)
        engine.gfx.mainSurface.removePostProcessingEffect(aoEffect)
    }

    private class MaskEffect(private val maskSurface: Surface) : SinglePassEffect()
    {
        override fun loadShaderProgram() = ShaderProgram.create("/shaders/mask.vert", "/shaders/mask.frag")
        override fun applyEffect(texture: Texture): Texture
        {
            fbo.bind()
            fbo.clear()
            program.bind()
            renderer.render(texture, maskSurface.getTexture())
            fbo.release()
            return fbo.getTexture() ?: texture
        }
    }

    private class OverlayEffect(private val overlaySurface: Surface) : SinglePassEffect()
    {
        override fun loadShaderProgram() = ShaderProgram.create("/shaders/overlay.vert", "/shaders/overlay.frag")
        override fun applyEffect(texture: Texture): Texture
        {
            fbo.bind()
            program.bind()
            renderer.render(texture, overlaySurface.getTexture())
            fbo.release()
            return fbo.getTexture() ?: texture
        }
    }

    private class AmbientOcclusionEffect(val aoMaskSurface: Surface2D) : MultiPassEffect(3)
    {
        var size = 1f
        var opacity = 1f
        var scale = 1f

        override fun loadShaderPrograms() = listOf(
            ShaderProgram.create("/shaders/blur.vert", "/shaders/blur.frag"),
            ShaderProgram.create("/shaders/ao.vert", "/shaders/ao.frag")
        )

        override fun applyEffect(texture: Texture): Texture
        {
            val scale = 0.7f * (1f - size) / scale
            val opacity = 2f * opacity

            // Horizontal blur pass
            fbo[0].bind()
            fbo[0].clear()
            programs[0].bind()
            programs[0].setUniform("horizontal", 1f)
            programs[0].setUniform("vertical", 0f)
            programs[0].setUniform("width", texture.width.toFloat())
            programs[0].setUniform("height", texture.height.toFloat())
            programs[0].setUniform("scale", scale)
            renderers[0].render(aoMaskSurface.getTexture())
            fbo[0].release()

            // Vertical blur pass
            fbo[1].bind()
            fbo[1].clear()
            programs[0].bind()
            programs[0].setUniform("horizontal", 0f)
            programs[0].setUniform("vertical", 1f)
            programs[0].setUniform("width", texture.width.toFloat())
            programs[0].setUniform("height", texture.height.toFloat())
            programs[0].setUniform("scale", scale)
            renderers[0].render(fbo[0].getTexture() ?: texture)
            fbo[1].release()

            // Combine base texture + blur + mask
            fbo[2].bind()
            fbo[2].clear()
            programs[1].bind()
            programs[1].setUniform("opacity", opacity)
            renderers[1].render(texture, fbo[1].getTexture() ?: texture, aoMaskSurface.getTexture())
            fbo[2].release()

            return fbo[2].getTexture() ?: texture
        }
    }

    /**
     * Marked [SceneEntity] classes will be rendered to the decal mask surface
     */
    interface DecalMask

    /**
     * Marked [SceneEntity] classes will be rendered to the ambient occlusion surface
     */
    interface AOMask
}

