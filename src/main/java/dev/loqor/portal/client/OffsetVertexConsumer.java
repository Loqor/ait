package dev.loqor.portal.client;

import net.minecraft.client.render.VertexConsumer;

/**
 * A {@link VertexConsumer} that adds a constant positional offset to every vertex before forwarding to a delegate,
 * leaving colour / texture / light / normal / overlay untouched.
 * <p>
 * It exists for exactly one job: <b>fluids</b>. Unlike {@code renderBlock},
 * {@link net.minecraft.client.render.block.BlockRenderManager#renderFluid renderFluid} takes <em>no</em>
 * {@link net.minecraft.client.util.math.MatrixStack} - it bakes its vertices at <em>section-local</em> coordinates
 * ({@code pos.getX() & 15}, {@code pos.getY() & 15}, {@code pos.getZ() & 15}). That is how vanilla chunk meshing
 * works: a section's whole buffer is built at 0..15 and later drawn with a model-view translated to the section
 * origin. The BOTI geometry builder instead bakes <em>everything</em> relative to the portal centre and draws every
 * section with one shared view matrix, so the unshifted fluid quads came out at 0..15 in <em>every</em> section -
 * i.e. all the water piled up in a single 16-block box at the origin.
 * <p>
 * Wrapping the fluid's buffer in this consumer with {@code offset = sectionMin - centre} shifts those section-local
 * coordinates into the same centre-relative space the solid blocks already use, lining the water back up. The offset
 * is constant per section because {@code (worldPos - centre) - (worldPos & 15) == sectionMin - centre}.
 */
public class OffsetVertexConsumer implements VertexConsumer {
    private final VertexConsumer delegate;
    private final double offsetX;
    private final double offsetY;
    private final double offsetZ;

    public OffsetVertexConsumer(VertexConsumer delegate, double offsetX, double offsetY, double offsetZ) {
        this.delegate = delegate;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
    }

    @Override
    public VertexConsumer vertex(double x, double y, double z) {
        this.delegate.vertex(x + this.offsetX, y + this.offsetY, z + this.offsetZ);
        return this;
    }

    @Override
    public VertexConsumer color(int red, int green, int blue, int alpha) {
        this.delegate.color(red, green, blue, alpha);
        return this;
    }

    @Override
    public VertexConsumer texture(float u, float v) {
        this.delegate.texture(u, v);
        return this;
    }

    @Override
    public VertexConsumer overlay(int u, int v) {
        this.delegate.overlay(u, v);
        return this;
    }

    @Override
    public VertexConsumer light(int u, int v) {
        this.delegate.light(u, v);
        return this;
    }

    @Override
    public VertexConsumer normal(float x, float y, float z) {
        this.delegate.normal(x, y, z);
        return this;
    }

    @Override
    public void next() {
        this.delegate.next();
    }

    @Override
    public void fixedColor(int red, int green, int blue, int alpha) {
        this.delegate.fixedColor(red, green, blue, alpha);
    }

    @Override
    public void unfixColor() {
        this.delegate.unfixColor();
    }
}
