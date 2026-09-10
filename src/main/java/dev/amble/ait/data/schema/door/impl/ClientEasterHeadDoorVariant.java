package dev.amble.ait.data.schema.door.impl;

import dev.amble.ait.AITMod;
import dev.amble.ait.client.models.AnimatedModel;
import dev.amble.ait.client.models.doors.BedrockDoorModel;
import dev.amble.ait.data.schema.door.ClientDoorSchema;
import dev.amble.lib.client.bedrock.BedrockModelRegistry;

public class ClientEasterHeadDoorVariant extends ClientDoorSchema {
    public ClientEasterHeadDoorVariant() {
        super(EasterHeadDoorVariant.REFERENCE);
    }

    @Override
    public AnimatedModel model() {
        return new BedrockDoorModel(BedrockModelRegistry.getInstance().get(AITMod.id("easter_head_door")));
    }
}

