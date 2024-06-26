package loqor.ait.core.data.schema.console;

import com.google.gson.*;
import loqor.ait.client.models.consoles.ConsoleModel;
import loqor.ait.core.data.base.Identifiable;
import loqor.ait.registry.impl.console.variant.ClientConsoleVariantRegistry;
import loqor.ait.registry.impl.console.variant.ConsoleVariantRegistry;
import loqor.ait.tardis.console.variant.hartnell.HartnellVariant;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;
import org.joml.Vector3f;

import java.lang.reflect.Type;

@Environment(EnvType.CLIENT)
public abstract class ClientConsoleVariantSchema implements Identifiable {
	private final Identifier parent;
	private final Identifier id;

	protected ClientConsoleVariantSchema(Identifier parent, Identifier id) {
		this.parent = parent;
		this.id = id;
	}

	protected ClientConsoleVariantSchema(Identifier parent) {
		this.id = parent;
		this.parent = parent;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;

		ClientConsoleVariantSchema that = (ClientConsoleVariantSchema) o;

		return id.equals(that.id);
	}

	public ConsoleVariantSchema parent() {
		return ConsoleVariantRegistry.getInstance().get(this.parent);
	}

	public Identifier id() {
		return id;
	}

	public abstract Identifier texture();

	public abstract Identifier emission();

	@Environment(EnvType.CLIENT)
	public abstract ConsoleModel model();

	public static Object serializer() {
		return new Serializer();
	}

	public Vector3f sonicItemTranslations() {
		return new Vector3f(0.1f, 1.2f, 0.26f);
	}

	public float[] sonicItemRotations() {
		return new float[]{120f, 135f};
	}

	private static class Serializer implements JsonSerializer<ClientConsoleVariantSchema>, JsonDeserializer<ClientConsoleVariantSchema> {

		@Override
		public ClientConsoleVariantSchema deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			Identifier id;

			try {
				id = new Identifier(json.getAsJsonPrimitive().getAsString());
			} catch (InvalidIdentifierException e) {
				id = HartnellVariant.REFERENCE;
			}

			return ClientConsoleVariantRegistry.getInstance().get(id);
		}

		@Override
		public JsonElement serialize(ClientConsoleVariantSchema src, Type typeOfSrc, JsonSerializationContext context) {
			return new JsonPrimitive(src.id().toString());
		}
	}
}
