package dev.amble.ait.client.renderers;

import java.util.function.Function;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;


@Environment(EnvType.CLIENT)
public class AITRenderLayers extends RenderLayer {

    /**
     * The emissive layer, always with the quad sorter.
     *
     * <p>The sorter is not an optimisation to be skipped when the alpha is opaque. This layer writes
     * no depth ({@code COLOR_MASK}) and disables back face culling, so nothing in the batch occludes
     * anything else in it and both faces of every part are submitted. Whichever quad is drawn last
     * wins, which leaves submission order as the only thing deciding what ends up on top: a part's
     * own back face, or a light sitting behind a panel, will paint over the front of it.
     *
     * <p>That is independent of the texture's alpha. Binary alpha makes the <em>blend</em> order
     * independent, not the occlusion, and dropping the sorter on that reasoning put lights through
     * panels and left the animated monitor glow on Renaissance and Toyota not reading as animated.
     *
     * <p>It does cost. {@code BufferBuilder.setSorter} allocates a primitive centre per quad and
     * writes an explicit index buffer instead of reusing the shared sequential one, which on Copper's
     * roughly 6800 quad emission pass measured about 0.6 ms a frame. That is the price of drawing it
     * in the right order.
     */
    private static RenderLayer emissive(Identifier texture) {
        RenderPhase.Texture texture2 = new RenderPhase.Texture(texture, false, false);
        MultiPhaseParameters multiPhaseParameters = RenderLayer.MultiPhaseParameters.builder()
                .program(RenderPhase.EYES_PROGRAM)
                .texture(texture2)
                .cull(DISABLE_CULLING)
                .transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY)
                .layering(RenderPhase.VIEW_OFFSET_Z_LAYERING)
                .lightmap(ENABLE_LIGHTMAP)
                .writeMaskState(COLOR_MASK)
                .depthTest(RenderPhase.LEQUAL_DEPTH_TEST)
                .build(false);

        return RenderLayer.of("emissive_cull_z_offset",
                VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, VertexFormat.DrawMode.QUADS, 256,
                false, true, multiPhaseParameters);
    }

    private static final Function<Identifier, RenderLayer> EMISSIVE = Util.memoize(AITRenderLayers::emissive);

    /** One layer per emission texture. See {@link #emissive} for why the sort is not optional. */
    public static RenderLayer tardisEmissiveCullZOffset(Identifier texture) {
        return EMISSIVE.apply(texture);
    }

    private AITRenderLayers(String name, VertexFormat vertexFormat, VertexFormat.DrawMode drawMode,
                            int expectedBufferSize, boolean hasCrumbling, boolean translucent, Runnable startAction,
                            Runnable endAction) {
        super(name, vertexFormat, drawMode, expectedBufferSize, hasCrumbling, translucent, startAction, endAction);
    }

    public static RenderLayer getBoti() {
        MultiPhaseParameters parameters = MultiPhaseParameters.builder()
                .texture(RenderPhase.MIPMAP_BLOCK_ATLAS_TEXTURE)
                .transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY)
                .layering(RenderPhase.NO_LAYERING)
                .build(false);
        return RenderLayer.of("boti", VertexFormats.POSITION_COLOR_TEXTURE_LIGHT,
                VertexFormat.DrawMode.QUADS, 256, false, true, parameters);
    }

    public static RenderLayer getBotiInteriorEmission(Identifier texture) {
        MultiPhaseParameters parameters = MultiPhaseParameters.builder()
                .texture(new Texture(texture, false, false))
                .program(ENTITY_CUTOUT_NONULL_OFFSET_Z_PROGRAM)
                .transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY)
                .cull(DISABLE_CULLING)
                .layering(RenderPhase.VIEW_OFFSET_Z_LAYERING)
                .lightmap(ENABLE_LIGHTMAP)
                .overlay(ENABLE_OVERLAY_COLOR)
                .depthTest(RenderPhase.LEQUAL_DEPTH_TEST)
                .build(false);
        return RenderLayer.of("boti_interior_emission", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL,
                VertexFormat.DrawMode.QUADS, 256, false, true, parameters);
    }

    public static RenderLayer getBotiInterior(Identifier texture) {
        // Not memoized, unlike EMISSIVE_CULL_Z_OFFSET above. Counted so the per-frame allocation rate is
        // visible rather than inferred.
        MinecraftClient.getInstance().getProfiler().visit("ait_renderlayer_alloc");

        MultiPhaseParameters parameters = MultiPhaseParameters.builder()
                .texture(new Texture(texture, false, false))
                .program(ENTITY_CUTOUT_NONULL_PROGRAM)
                .transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY)
                .cull(ENABLE_CULLING)
                .layering(RenderPhase.NO_LAYERING)
                .lightmap(ENABLE_LIGHTMAP)
                .overlay(ENABLE_OVERLAY_COLOR)
                .depthTest(RenderPhase.LEQUAL_DEPTH_TEST)
                .build(false);
        return RenderLayer.of("boti_interior", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL,
                VertexFormat.DrawMode.QUADS, 256, false, true, parameters);
    }
}
