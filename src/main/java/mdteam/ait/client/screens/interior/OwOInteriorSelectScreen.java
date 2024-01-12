package mdteam.ait.client.screens.interior;

import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.TextureComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.*;
import mdteam.ait.AITMod;
import mdteam.ait.network.ClientAITNetworkManager;
import mdteam.ait.registry.DesktopRegistry;
import mdteam.ait.tardis.Tardis;
import mdteam.ait.tardis.TardisDesktopSchema;
import mdteam.ait.tardis.wrapper.client.manager.ClientTardisManager;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class OwOInteriorSelectScreen extends BaseOwoScreen<FlowLayout> {
    private static final Identifier MISSING_PREVIEW = new Identifier(AITMod.MOD_ID, "textures/gui/tardis/desktop/missing_preview.png");

    private Screen parent;
    private UUID tardisid;
    private TardisDesktopSchema selectedDesktop;
    int bgHeight = 166;
    int bgWidth = 256;
    int left, top;

    public OwOInteriorSelectScreen(UUID tardis, Screen parent) {
        this.parent = parent;
        this.tardisid = tardis;
        updateTardis();
    }

    protected Tardis tardis() {
        AITMod.LOGGER.error("Client side tardis should not be accessed!");
        return null;
    }

    protected Tardis updateTardis() {
        return tardis();
    }

    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, Containers::verticalFlow);
    }
    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    protected void init() {
        this.selectedDesktop = tardis().getDesktop().getSchema();
        this.top = (this.height - this.bgHeight) / 2; // this means everythings centered and scaling, same for below
        this.left = (this.width - this.bgWidth) / 2;

        super.init();
    }

    @Override
    protected void build(FlowLayout rootComponent) {
        rootComponent
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .verticalAlignment(VerticalAlignment.CENTER);
        FlowLayout container = Containers.horizontalFlow(Sizing.fixed(bgWidth), Sizing.fixed(bgHeight));
        Text applyText = Text.translatable("screen" + AITMod.MOD_ID + "monitor.apply");
        Component apply = Components.button(applyText, buttonComponent -> this.applyDesktop()).zIndex(-250);
        Component applyLabel = Components.label(applyText);
        Component back = Components.button(Text.literal("<"), buttonComponent -> this.backToExteriorChangeScreen()).zIndex(-250);
        Component backLabel = Components.label(Text.literal("<"));
        Component previous = Components.button(Text.literal("<"), buttonComponent -> this.previousDesktop()).zIndex(-250);
        Component previousLabel = Components.label(Text.literal("<"));
        Component next = Components.button(Text.literal(">"), buttonComponent -> this.nextDesktop()).zIndex(-250);
        Component nextLabel = Components.label(Text.literal(">"));
        container.child(apply.positioning(Positioning.absolute(
                        63,  9))
                .sizing(Sizing.fixed(128)));
        container.child(applyLabel.positioning(apply.positioning().get()).sizing(apply.horizontalSizing().get(), apply.verticalSizing().get()));
        container.child(back.positioning(Positioning.absolute(
                        (int) (bgWidth * 0.03f) - (textRenderer.getWidth("<") / 2), (int) (bgHeight * 0.03f)))
                .sizing(Sizing.fixed(textRenderer.getWidth("<") / 2), Sizing.fixed( 10)));
        container.child(backLabel.positioning(back.positioning().get()).sizing(back.horizontalSizing().get(), back.verticalSizing().get()));
        container.child(previous.positioning(Positioning.absolute(
                (int) (bgWidth * 0.3f) - (textRenderer.getWidth("<") / 2), (int) (bgHeight * 0.85f))));
        container.child(previousLabel.positioning(previous.positioning().get()).sizing(previous.horizontalSizing().get(), previous.verticalSizing().get()));
        container.child(next.positioning(Positioning.absolute(
                (int) (bgWidth * 0.7f) - (textRenderer.getWidth(">") / 2), (int) (bgHeight * 0.85f))));
        container.child(nextLabel.positioning(next.positioning().get()).sizing(next.horizontalSizing().get(), next.verticalSizing().get()));
        rootComponent.child(container.surface(Surface.DARK_PANEL).zIndex(-100));
    }

    private void applyDesktop() {
        if(this.selectedDesktop == null) return;
        ClientAITNetworkManager.send_request_interior_change_from_monitor(this.tardisid, this.selectedDesktop.id());

        MinecraftClient.getInstance().setScreen(null);
    }

    public void backToExteriorChangeScreen() {
        MinecraftClient.getInstance().setScreen(this.parent);
    }

    private static TardisDesktopSchema nextDesktop(TardisDesktopSchema current) {
        List<TardisDesktopSchema> list = DesktopRegistry.toList();

        int idx = list.indexOf(current);
        if (idx < 0 || idx + 1 == list.size()) return list.get(0);
        return list.get(idx + 1);
    }

    private void nextDesktop() {
        if(this.selectedDesktop == null) return;
        this.selectedDesktop = nextDesktop(this.selectedDesktop);

        if (!isCurrentUnlocked()) nextDesktop(); // ooo incursion crash
    }

    private static TardisDesktopSchema previousDesktop(TardisDesktopSchema current) {
        List<TardisDesktopSchema> list = DesktopRegistry.toList();

        int idx = list.indexOf(current);
        if (idx <= 0) return list.get(list.size() - 1);
        return list.get(idx - 1);
    }

    private void previousDesktop() {
        if(this.selectedDesktop == null) return;
        this.selectedDesktop = previousDesktop(this.selectedDesktop);

        if (!isCurrentUnlocked()) previousDesktop(); // ooo incursion crash
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderDesktop(context);
        super.render(context, mouseX, mouseY, delta);
    }

    private void renderDesktop(DrawContext context) {
        if(this.selectedDesktop == null) return;
        if (Objects.equals(this.selectedDesktop, DesktopRegistry.DEFAULT_CAVE)) this.nextDesktop();

        context.drawCenteredTextWithShadow(
                this.textRenderer,
                this.selectedDesktop.name(),
                (int) (left + (bgWidth * 0.5f)),
                (int) (top + (bgHeight * 0.85f)),
                0xadcaf7
        );

        context.drawTexture(doesTextureExist(this.selectedDesktop.previewTexture().texture()) ? this.selectedDesktop.previewTexture().texture() : MISSING_PREVIEW, left + 63, top + 9,128,128,0,0, this.selectedDesktop.previewTexture().width * 2, this.selectedDesktop.previewTexture().height * 2,this.selectedDesktop.previewTexture().width * 2, this.selectedDesktop.previewTexture().height * 2);
    }

    private boolean doesTextureExist(Identifier id) {
        return MinecraftClient.getInstance().getResourceManager().getResource(id).isPresent();
    }

    private boolean isCurrentUnlocked() {
        if(this.selectedDesktop == null) return true;
        return tardis().isDesktopUnlocked(this.selectedDesktop);
    }
}
