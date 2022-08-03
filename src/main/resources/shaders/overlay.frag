#version 420 core

in vec2 textureCoord;
out vec4 fragColor;

layout(binding=0) uniform sampler2D sourceTexture;
layout(binding=1) uniform sampler2D blendTexture;

void main() {
    vec4 source = texture(sourceTexture, textureCoord);
    vec4 blend = texture(blendTexture, textureCoord);
    vec4 multiply = 2.0 * source * blend;
    vec4 screen = vec4(1.0) - (2.0 * (vec4(1.0) - source)) * (vec4(1.0) - blend);
    float gray = (source.r + source.b + source.g) / 3.0;
    float threshold = step(gray, 0.5);
    vec4 overlay = (multiply * (1.0 - threshold)) + (screen * threshold);

    fragColor = mix(source, overlay, blend.a);
}