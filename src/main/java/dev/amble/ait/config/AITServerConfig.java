package dev.amble.ait.config;

import java.util.List;

import com.google.common.collect.Lists;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.controller.ControllerBuilder;
import dev.isxander.yacl3.api.controller.StringControllerBuilder;
import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.ConfigField;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.autogen.*;
import dev.isxander.yacl3.config.v2.api.autogen.Boolean;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import dev.isxander.yacl3.platform.YACLPlatform;

import dev.amble.ait.AITMod;
import dev.amble.ait.core.AITDimensions;

public class AITServerConfig {

    public static final String CATEGORY = "server";

    public static final ConfigClassHandler<AITServerConfig> INSTANCE = ConfigClassHandler.createBuilder(AITServerConfig.class)
            .id(YACLPlatform.rl(AITMod.MOD_ID, "server"))
            .serializer(config -> GsonConfigSerializerBuilder.create(config)
                    .setPath(YACLPlatform.getConfigDir().resolve("ait-server.json5"))
                    .setJson5(true)
                    .build())
            .build();

    @AutoGen(category = CATEGORY)
    @Boolean(formatter = Boolean.Formatter.YES_NO, colored = true)
    @SerialEntry public boolean minifyJson = false;

    @AutoGen(category = CATEGORY)
    @Boolean(formatter = Boolean.Formatter.YES_NO, colored = true)
    @SerialEntry public boolean ghostMonument = true;

    @AutoGen(category = CATEGORY)
    @Boolean(formatter = Boolean.Formatter.YES_NO, colored = true)
    @SerialEntry public boolean lockDimensions = true;

    @AutoGen(category = CATEGORY)
    @Boolean(formatter = Boolean.Formatter.YES_NO, colored = true)
    @SerialEntry public boolean rwfEnabled = false;

    @AutoGen(category = CATEGORY)
    @Boolean(formatter = Boolean.Formatter.YES_NO, colored = true)
    @SerialEntry public boolean allowPortalsBoti = true;

    @AutoGen(category = CATEGORY)
    @IntSlider(min = 1, max = 8, step = 1)
    @CustomDescription(value = "How many chunks in each direction the built-in BOTI streams through the doorway. Higher values show more of the other side but cost more server chunk-loading.")
    @SerialEntry public int botiRenderDistance = 3;

    @AutoGen(category = CATEGORY)
    @Boolean(formatter = Boolean.Formatter.YES_NO, colored = true)
    @SerialEntry public boolean tntCanTeleportThroughDoors = true;

    @AutoGen(category = CATEGORY)
    @Boolean(formatter = Boolean.Formatter.YES_NO, colored = true)
    @SerialEntry public boolean hypercubesEnabled = true;

    @AutoGen(category = CATEGORY)
    @IntField(min = 0)
    @CustomDescription(value = "The levenshtein distance allows for typos when using handles. Distances lower to 0 are stricter while higher values like 5 are more lenient.")
    @CustomImage(value = "textures/yacl3/server/levenshtein.webp", width = 1909, height = 349)
    @SerialEntry public int handlesLevenshteinDistance = 2;

    @AutoGen(category = CATEGORY)
    @CustomDescription(value = "Dimensions listed here will be excluded. Ignored when the whitelist has entries.")
    @ListGroup(valueFactory = StringListFactory.class, controllerFactory = StringListFactory.class)
    @SerialEntry public List<String> projectorBlacklist = Lists.newArrayList(
            "ait-tardis");

    @AutoGen(category = CATEGORY)
    @CustomDescription(value = "When populated, only these dimensions will be allowed and the blacklist is ignored.")
    @ListGroup(valueFactory = StringListFactory.class, controllerFactory = StringListFactory.class)
    @SerialEntry public List<String> projectorWhitelist = Lists.newArrayList();

    @AutoGen(category = CATEGORY)
    @CustomDescription(value = "Dimensions listed here will be excluded. Ignored when the whitelist has entries.")
    @ListGroup(valueFactory = StringListFactory.class, controllerFactory = StringListFactory.class)
    @SerialEntry public List<String> travelBlacklist = Lists.newArrayList(
            "ait-tardis", "ait:tardis_dimension_type", AITDimensions.TIME_VORTEX_WORLD.getValue().toString(), "ait:space");

    @AutoGen(category = CATEGORY)
    @CustomDescription(value = "When populated, only these dimensions will be allowed and the blacklist is ignored.")
    @ListGroup(valueFactory = StringListFactory.class, controllerFactory = StringListFactory.class)
    @SerialEntry public List<String> travelWhitelist = Lists.newArrayList();

    @AutoGen(category = CATEGORY)
    @IntField(min = 1)
    @SerialEntry public int travelPerTick = 2;

    @AutoGen(category = CATEGORY)
    @IntField(min = 256, max = 65536)
    @SerialEntry public int astralMapBiomeLocatorRange = 6400;

    @AutoGen(category = CATEGORY)
    @Boolean(formatter = Boolean.Formatter.YES_NO, colored = true)
    @SerialEntry public boolean sendBulk = true;

    @AutoGen(category = CATEGORY)
    @IntField(min = -1)
    @SerialEntry public int maxTardises = -1;

    @AutoGen(category = CATEGORY)
    @Boolean(formatter = Boolean.Formatter.YES_NO, colored = true)
    @SerialEntry public boolean disableSafeguards = false;

    @AutoGen(category = CATEGORY)
    @FloatSlider(min = 0, max = 16, step = 0.1f)
    @SerialEntry public float crashSoundVolume = 1f;

    @AutoGen(category = CATEGORY)
    @FloatSlider(min = 0, max = 16, step = 0.1f)
    @SerialEntry public float flightSoundVolume = 2f;

    @AutoGen(category = CATEGORY)
    @IntSlider(min = 1, max = 128, step = 1)
    @SerialEntry public int maxStabilizedSpeed = 4;

    public static class StringListFactory implements ListGroup.ValueFactory<String>, ListGroup.ControllerFactory<String> {

        // used by the reflections
        public StringListFactory() { }

        @Override
        public ControllerBuilder<String> createController(ListGroup annotation, ConfigField<List<String>> field, OptionAccess storage, Option<String> option) {
            return StringControllerBuilder.create(option);
        }

        @Override
        public String provideNewValue() {
            return "";
        }
    }
}
