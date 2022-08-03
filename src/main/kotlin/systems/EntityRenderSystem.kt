package systems

import com.fasterxml.jackson.annotation.JsonIgnore
import entities.Bullet
import entities.Decal
import entities.SpikeWall
import no.njoh.pulseengine.core.PulseEngine
import no.njoh.pulseengine.core.asset.types.Texture
import no.njoh.pulseengine.core.graphics.Surface
import no.njoh.pulseengine.core.graphics.api.BlendFunction
import no.njoh.pulseengine.core.graphics.api.ShaderProgram
import no.njoh.pulseengine.core.graphics.postprocessing.SinglePassEffect
import no.njoh.pulseengine.core.graphics.postprocessing.effects.BlurEffect
import no.njoh.pulseengine.core.scene.SceneEntity
import no.njoh.pulseengine.core.scene.systems.EntityRendererImpl
import util.BULLET_SURFACE
import util.DECAL_SURFACE
import util.DECAL_MASK_SURFACE

class EntityRenderSystem : EntityRendererImpl()
{
    @JsonIgnore
    private lateinit var overlayEffect: OverlayEffect

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

        // Overlay decals on main surface
        overlayEffect = OverlayEffect(decalSurface)
        engine.gfx.mainSurface.addPostProcessingEffect(overlayEffect)

        // Add render passes
        addRenderPass(RenderPass(surfaceName = decalMaskSurface.name, targetType = SpikeWall::class))
        addRenderPass(RenderPass(surfaceName = decalSurface.name, targetType = Decal::class))
        addRenderPass(RenderPass(surfaceName = bulletSurface.name, targetType = Bullet::class))
        addRenderPass(RenderPass(
            surfaceName = engine.gfx.mainSurface.name,
            targetType = SceneEntity::class,
            drawCondition = { it !is Bullet && it !is Decal }
        ))
    }

    override fun onDestroy(engine: PulseEngine)
    {
        engine.gfx.deleteSurface(BULLET_SURFACE)
        engine.gfx.deleteSurface(DECAL_MASK_SURFACE)
        engine.gfx.deleteSurface(DECAL_SURFACE)
        engine.gfx.mainSurface.removePostProcessingEffect(overlayEffect)
    }

    private class MaskEffect(private val maskSurface: Surface) : SinglePassEffect()
    {
        override fun loadShaderProgram() =
            ShaderProgram.create("/shaders/mask.vert", "/shaders/mask.frag")

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
        override fun loadShaderProgram() =
            ShaderProgram.create("/shaders/overlay.vert", "/shaders/overlay.frag")

        override fun applyEffect(texture: Texture): Texture
        {
            fbo.bind()
            program.bind()
            renderer.render(texture, overlaySurface.getTexture())
            fbo.release()

            return fbo.getTexture() ?: texture
        }
    }
}

