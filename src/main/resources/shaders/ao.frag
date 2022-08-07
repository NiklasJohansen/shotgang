#version 420 core

in vec2 textureCoord;
out vec4 fragColor;

layout(binding=0) uniform sampler2D baseTex;
layout(binding=1) uniform sampler2D blurTex;
layout(binding=2) uniform sampler2D maskTex;

uniform float opacity;

void main() {
    vec4 baseColor = texture(baseTex, textureCoord);
    vec4 blurColor = texture(blurTex, textureCoord);
    vec4 maskColor = texture(maskTex, textureCoord);

    float threshold = step(maskColor.a, 0.5);
    float aoStrength = 1.0 - clamp(blurColor.a * opacity * threshold, 0.0, 1.0);

    fragColor = vec4(baseColor.rgb * aoStrength, baseColor.a);

    //fragColor = vec4(vec3(aoStrength), baseColor.a);
}