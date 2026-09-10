package dev.amble.ait.core.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

public record Transformations(Vec3d offset, Vec3d scale, Vec3d rotation) {
	public static final Transformations DEFAULT = new Transformations(
			new Vec3d(0.0, 0.0, 0.0),
			new Vec3d(1.0, 1.0, 1.0),
			new Vec3d(0.0, 0.0, 0.0)
	);

	public static final Transformations MISSING = new Transformations(
			new Vec3d(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE),
			new Vec3d(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE),
			new Vec3d(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE)
	);

	public static final Codec<Transformations> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Vec3d.CODEC.optionalFieldOf("offset", DEFAULT.offset()).forGetter(Transformations::offset),
			Vec3d.CODEC.optionalFieldOf("scale", DEFAULT.scale()).forGetter(Transformations::scale),
			Vec3d.CODEC.optionalFieldOf("rotation", DEFAULT.rotation()).forGetter(Transformations::rotation)
	).apply(instance, Transformations::new));


	@Environment(EnvType.CLIENT)
	public void apply(MatrixStack stack) {
		stack.translate(offset.x, offset.y * -1, offset.z);
		stack.scale((float) scale.x, (float) scale.y, (float) scale.z);
		stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees((float) rotation.x));
		stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) rotation.y));
		stack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) rotation.z));
	}
}
