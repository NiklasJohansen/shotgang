package entities
import com.fasterxml.jackson.annotation.JsonIgnore
import no.njoh.pulseengine.core.PulseEngine
import no.njoh.pulseengine.modules.lighting.entities.Lamp

class Flash : Lamp()
{
    var lifeTimeMillis = 200f

    @JsonIgnore private var spawnTime = System.currentTimeMillis()
    @JsonIgnore private var startIntensity = 0f

    override fun onStart(engine: PulseEngine)
    {
        startIntensity = intensity
    }

    override fun onUpdate(engine: PulseEngine)
    {
        val elapsedTime = System.currentTimeMillis() - spawnTime
        if (elapsedTime > lifeTimeMillis)
            set(DEAD)

        intensity = startIntensity * (1f - (elapsedTime / lifeTimeMillis).coerceIn(0f, 1f))
    }
}