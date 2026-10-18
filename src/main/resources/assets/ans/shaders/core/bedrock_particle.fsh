#version 330

// PARTICLES / PFX: Java's core/particle fragment shader with the Bedrock material's alpha cut-off (ALPHA_CUTOFF define):
// particles_alpha / particles_opaque discard below 0.5 (Bedrock ALPHA_TEST), particles_blend / particles_add only fully
// transparent texels (Bedrock does not alpha-test blended particles). Java's own particles discard below 0.1.

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>

uniform sampler2D Sampler0;

in float sphericalVertexDistance;
in float cylindricalVertexDistance;
in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    vec4 color = texture(Sampler0, texCoord0) * vertexColor * ColorModulator;
    if (color.a < ALPHA_CUTOFF) {
        discard;
    }
    fragColor = apply_fog(color, sphericalVertexDistance, cylindricalVertexDistance, FogEnvironmentalStart, FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd, FogColor);
}
