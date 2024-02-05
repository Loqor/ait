package mdteam.ait.client.screens;

import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.core.*;
import mdteam.ait.AITMod;
import mdteam.ait.client.util.ClientTardisUtil;
import mdteam.ait.tardis.Tardis;
import mdteam.ait.tardis.wrapper.client.manager.ClientTardisManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class OwOFindPlayerScreen extends BaseOwoScreen<FlowLayout> {
    private static final Identifier TEXTURE = new Identifier(AITMod.MOD_ID, "textures/gui/tardis/consoles/monitors/exterior_changer.png");
    private final UUID tardisid;

    public OwOFindPlayerScreen(UUID tardisid) {
        this.tardisid = tardisid;
        this.updateTardis();
    }

    protected Tardis tardis() {
        return ClientTardisManager.getInstance().getLookup().get(this.tardisid);
    }

    protected void updateTardis() {
        ClientTardisManager.getInstance().ask(this.tardisid);
        tardis();
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
    protected void build(FlowLayout rootComponent) {
        rootComponent
                .surface(Surface.VANILLA_TRANSLUCENT)
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .verticalAlignment(VerticalAlignment.CENTER);
        FlowLayout container = Containers.horizontalFlow(Sizing.fixed(236), Sizing.fixed(133));
        if (MinecraftClient.getInstance().player == null) return;
        List<PlayerListEntry> list = this.collectPlayerEntries();
        ScrollContainer<Component> scrollContainer = Containers.verticalScroll(
                Sizing.content(), Sizing.fill(100), Components.list(
                        list, (layout) -> {
                            Containers.verticalFlow(Sizing.fill(100), Sizing.content());
                        }, (s) -> Containers.stack(Sizing.content(),
                                Sizing.content()).alignment(HorizontalAlignment.CENTER, VerticalAlignment.CENTER).padding(Insets.of(10)).surface(Surface.TOOLTIP), false).alignment(HorizontalAlignment.LEFT, VerticalAlignment.TOP));
        Component left = Components.button(Text.literal("<"), buttonComponent -> System.out.println(this.tardisid)).sizing(Sizing.fixed(15))
                .positioning(Positioning.absolute(20, 20));
        Component right = Components.button(Text.literal(">"), buttonComponent -> System.out.println(this.tardisid)).sizing(Sizing.fixed(15))
                .positioning(Positioning.absolute(25, 20));
        for (PlayerListEntry playerListEntry : list) {
            container.child(scrollContainer.child(Components.label(this.getPlayerName(playerListEntry))).child(Components.button(this.getPlayerName(playerListEntry), buttonComponent -> this.onPress(this.getPlayerUuid(list))).zIndex(-250)).alignment(HorizontalAlignment.CENTER, VerticalAlignment.CENTER)
                    .padding(Insets.of(10)).surface(Surface.TOOLTIP)).alignment(HorizontalAlignment.LEFT, VerticalAlignment.TOP);
        }
        /*container.child(left);
        container.child(right);*/
        rootComponent.child(container.surface(Surface.DARK_PANEL));
    }


    public void onPress(UUID uuid) {
        if (tardis() != null) {
            if (this.tardis().getTravel() != null) {
                ClientTardisUtil.setDestinationFromScreen(this.tardis().getUuid(), uuid);
            }
        }
    }

    private List<PlayerListEntry> collectPlayerEntries() {
        return MinecraftClient.getInstance().getNetworkHandler().getListedPlayerListEntries().stream().toList();
    }

    public Text getPlayerName(PlayerListEntry entry) {
        if (entry.getDisplayName() != null) {
            return entry.getDisplayName().copy();
        } else {
            return Text.literal(entry.getProfile().getName());
        }
    }

    @Nullable
    public UUID getPlayerUuid(List<PlayerListEntry> listOfEntries) {
        if(listOfEntries.isEmpty()) return null;
        for(PlayerListEntry entry : listOfEntries) {
            if (entry != null) {
                return entry.getProfile().getId();
            }
        }
        return null;
    }
}