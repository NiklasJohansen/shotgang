#version 420 core

layout(binding=0) uniform sampler2D img;

in vec2 textureCoord;
out vec4 fragColor;

uniform float width, height, scale;
uniform float horizontal, vertical;

float weights[33] = {
    0.040, 0.039, 0.038, 0.037, 0.036, 0.035, 0.034, 0.033, 0.032, 0.031, 0.03,
    0.029, 0.028, 0.027, 0.026, 0.025, 0.024, 0.023, 0.022, 0.021, 0.020, 0.019,
    0.018, 0.017, 0.016, 0.015, 0.014, 0.013, 0.012, 0.011, 0.01, 0.009, 0.008
};

void main()
{
    float px = horizontal / (width * scale);
    float py = vertical / (height * scale);
    vec4 sum = texture(img, vec2(textureCoord.x, textureCoord.y)) * weights[0];

    for (int i = 1; i < 33; i++)
    {
        sum += texture(img, vec2(textureCoord.x + px * i, textureCoord.y + py * i)) * weights[i];
        sum += texture(img, vec2(textureCoord.x - px * i, textureCoord.y - py * i)) * weights[i];
    }

    fragColor = sum;
}