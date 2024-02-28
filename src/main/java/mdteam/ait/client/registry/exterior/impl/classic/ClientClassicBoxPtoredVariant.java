package mdteam.ait.client.registry.exterior.impl.classic;

import org.joml.Vector3f;

public class ClientClassicBoxPtoredVariant extends ClientClassicBoxVariant {
	public ClientClassicBoxPtoredVariant() {
		super("ptored");
	}

	@Override
	public Vector3f sonicItemTranslations() {
		return new Vector3f(0.56f, 1.325f, 1.165f);
	}
}
