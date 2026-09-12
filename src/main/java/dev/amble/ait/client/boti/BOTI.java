package dev.amble.ait.client.boti;

import java.util.Collection;
import java.util.LinkedList;

import com.mojang.blaze3d.platform.GlConst;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.GL11;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import dev.amble.ait.client.AITModClient;
import dev.amble.ait.compat.DependencyChecker;
import dev.amble.ait.core.blockentities.DoorBlockEntity;
import dev.amble.ait.core.blockentities.ExteriorBlockEntity;
import dev.amble.ait.core.entities.BOTIPaintingEntity;
import dev.amble.ait.core.entities.RiftEntity;

public class BOTI {
    public static final MinecraftClient client = MinecraftClient.getInstance();
    public static final Collection<RiftEntity> RIFT_RENDERING_QUEUE = new LinkedList<>();
    public static BOTIInit BOTI_HANDLER = new BOTIInit();
    public static AITBufferBuilderStorage AIT_BUF_BUILDER_STORAGE = new AITBufferBuilderStorage();
    public static Collection<DoorBlockEntity> DOOR_RENDER_QUEUE = new LinkedList<>();
    public static Collection<BOTIPaintingEntity> GALLIFREYAN_RENDER_QUEUE = new LinkedList<>();
    public static Collection<BOTIPaintingEntity> TRENZALORE_PAINTING_QUEUE = new LinkedList<>();
    public static Collection<ExteriorBlockEntity> EXTERIOR_RENDER_QUEUE = new LinkedList<>();
    private static boolean HAS_BEEN_WARNED = false;

    public static void copyFramebuffer(Framebuffer src, Framebuffer dest) {
        GlStateManager._glBindFramebuffer(GlConst.GL_READ_FRAMEBUFFER, src.fbo);
        GlStateManager._glBindFramebuffer(GlConst.GL_DRAW_FRAMEBUFFER, dest.fbo);
        GlStateManager._glBlitFrameBuffer(0, 0, src.textureWidth, src.textureHeight, 0, 0, dest.textureWidth, dest.textureHeight, GlConst.GL_DEPTH_BUFFER_BIT | GlConst.GL_COLOR_BUFFER_BIT, GlConst.GL_NEAREST);
    }

    public static void copyColor(Framebuffer src, Framebuffer dest) {
        GlStateManager._glBindFramebuffer(GlConst.GL_READ_FRAMEBUFFER, src.fbo);
        GlStateManager._glBindFramebuffer(GlConst.GL_DRAW_FRAMEBUFFER, dest.fbo);
        GlStateManager._glBlitFrameBuffer(0, 0, src.textureWidth, src.textureHeight, 0, 0, dest.textureWidth, dest.textureHeight, GlConst.GL_COLOR_BUFFER_BIT, GlConst.GL_NEAREST);
    }

    public static void copyDepth(Framebuffer src, Framebuffer dest) {
        GlStateManager._glBindFramebuffer(GlConst.GL_READ_FRAMEBUFFER, src.fbo);
        GlStateManager._glBindFramebuffer(GlConst.GL_DRAW_FRAMEBUFFER, dest.fbo);
        GlStateManager._glBlitFrameBuffer(0, 0, src.textureWidth, src.textureHeight, 0, 0, dest.textureWidth, dest.textureHeight, GlConst.GL_DEPTH_BUFFER_BIT, GlConst.GL_NEAREST);
    }

    public static void setFramebufferColor(Framebuffer src, float r, float g, float b, float a) {
        src.setClearColor(r, g, b, a);
    }

    /** Restores core GL state after a BOTI pass so a failure can never leave the screen black. */
    public static void resetState() {
        GL11.glDisable(GL11.GL_STENCIL_TEST);
        RenderSystem.depthMask(true);
        client.getFramebuffer().beginWrite(true);
    }

    /** Solid white block texture, tinted to fill the portal background with a flat sky colour. */
    public static final Identifier SKY_TEXTURE = new Identifier("textures/block/white_concrete.png");

    /** Snaps a portal offset (block centre → portal) to a cardinal outward normal for BOTI transforms. */
    public static float[] horizontalNormal(double dx, double dz, float fallbackFacing) {
        if (Math.abs(dx) < 1.0E-4 && Math.abs(dz) < 1.0E-4) {
            Direction dir = Direction.fromRotation(fallbackFacing);
            return new float[]{dir.getOffsetX(), dir.getOffsetZ()};
        }

        if (Math.abs(dx) >= Math.abs(dz))
            return new float[]{Math.signum((float) dx), 0};

        return new float[]{0, Math.signum((float) dz)};
    }

    /**
     * Warns the user if they are missing Indium and have a non-Nvidia card.
     */
    public static void tryWarn(MinecraftClient client) {
        if (HAS_BEEN_WARNED)
            return;

        if (warn(client)) AITModClient.CONFIG.enableTardisBOTI = false;

        HAS_BEEN_WARNED = true;
    }

    /**
     * @return {@code true} if successfully warned the player, {@code false} otherwise
     */
    private static boolean warn(MinecraftClient client) {
        if (DependencyChecker.hasMacOs()) {
            tryWarnMac(client);
            return true;
        }

        if (DependencyChecker.hasIndium())
            return false;

        if (!DependencyChecker.hasNvidiaCard()) {
            tryWarnAmd(client);
            return true;
        }

        return false;
    }

    private static void tryWarnMac(MinecraftClient client) {
        client.player.sendMessage(Text.translatable("message.ait.boti.indium_required.mac").formatted(Formatting.RED), false);
    }

    private static void tryWarnAmd(MinecraftClient client) {
        client.player.sendMessage(Text.translatable("message.ait.boti.indium_required.amd").formatted(Formatting.RED), false);
    }
}
