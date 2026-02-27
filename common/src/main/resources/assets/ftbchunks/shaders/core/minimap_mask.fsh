#version 330

#moj_import <minecraft:dynamictransforms.glsl>

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    vec2 maskUV = vertexColor.rg;
    float maskA = texture(Sampler1, maskUV).a;
    vec4 mapColor = texture(Sampler0, texCoord0);
    float alpha = mapColor.a * vertexColor.a * maskA;
    if (alpha <= 0.0) {
        discard;
    }
    fragColor = vec4(mapColor.rgb, alpha) * ColorModulator;
}
