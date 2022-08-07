package shared

// Collision layers
const val WALL_LAYER = 1
const val PLAYER_LAYER = 2
const val BULLET_LAYER = 4
const val SHELL_LAYER = 8
const val PARTICLE_LAYER = 16
const val NO_COLLISION_LAYER = 1024

// Asset names
const val FONT_BADABB = "badabb"
const val TEXTURE_SHOTGUN_SHELL = "shotgun_shell"
const val TEXTURE_SMOKE_PUFF = "smoke_puff"
const val TEXTURE_SPARK_0 = "spark_0"
const val TEXTURE_BLOOD_0 = "blood_0"
const val TEXTURE_BLOOD_1 = "blood_1"
const val TEXTURE_BURN_0 = "burn_decal"
const val SOUND_SHOTGUN = "shotgun"
const val SOUND_DEATH = "death"
const val SOUND_DING_0 = "countdown_ding_0"
const val SOUND_DING_1 = "countdown_ding_1"
const val SOUND_CHEER = "cheer_0"
const val SOUND_WIN = "win"
const val SOUND_VICTORY = "victory"
const val SOUND_DRAW = "draw"
const val SOUND_SHELL_DROP_0 = "shell_drop_0"
const val SOUND_SHELL_DROP_1 = "shell_drop_1"
const val SOUND_SHELL_DROP_2 = "shell_drop_2"
const val SOUND_STEP_0 = "step_0"
const val SOUND_STEP_1 = "step_1"
const val SOUND_STEP_2 = "step_2"

// Player
const val TEMPLATE_NAME = "TEMPLATE"

// Levels
val LEVEL_PLAY_THROUGH_COUNT = 2
val LOBBY_LEVEL = "levels/shotgang_start.scn"
val LEVELS = listOf(
    "levels/shotgang_level_concrete_0.scn",
    "levels/shotgang_level_concrete_1.scn",
    "levels/shotgang_level_concrete_2.scn",
    "levels/shotgang_level_grass_0.scn",
    "levels/shotgang_level_grass_1.scn",
    "levels/shotgang_level_grass_2.scn",
    "levels/shotgang_level_desert_0.scn",
    "levels/shotgang_level_desert_1.scn",
    "levels/shotgang_level_desert_2.scn"
)

// Surface names
const val OVERLAY_SURFACE = "overlay"
const val AO_SURFACE = "ao_surface"
const val BULLET_SURFACE = "bullet_surface"
const val DECAL_MASK_SURFACE = "decal_mask_surface"
const val DECAL_SURFACE = "decals"
