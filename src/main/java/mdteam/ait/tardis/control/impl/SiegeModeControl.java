package mdteam.ait.tardis.control.impl;

import mdteam.ait.core.AITSounds;
import mdteam.ait.tardis.Tardis;
import mdteam.ait.tardis.control.Control;
import mdteam.ait.tardis.data.properties.PropertiesHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;

public class SiegeModeControl extends Control {
    public SiegeModeControl() {
        // Ⓢ ?
        super("protocol_1913");
    }

    @Override
    public boolean runServer(Tardis tardis, ServerPlayerEntity player, ServerWorld world) {
        tardis.setSiegeMode(!tardis.isSiegeMode());
        PropertiesHandler.set(tardis, PropertiesHandler.ALARM_ENABLED, false);
        Text enabled = Text.translatable("tardis.message.control.siege.enabled");
        Text disabled = Text.translatable("tardis.message.control.siege.disabled");
        player.sendMessage((tardis.isSiegeMode() ? enabled : disabled), true);
        return false;
    }

    @Override
    public SoundEvent getSound() {
        return AITSounds.HANDBRAKE_LEVER_PULL;
    }
    @Override
    public boolean shouldFailOnNoPower() {
        return false;
    }

    @Override
    public long getDelayLength() {
        return 10000L;
    }
}