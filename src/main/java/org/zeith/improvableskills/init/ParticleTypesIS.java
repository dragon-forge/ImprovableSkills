package org.zeith.improvableskills.init;

import net.minecraft.core.particles.ParticleType;
import org.zeith.hammerlib.annotations.RegistryName;
import org.zeith.hammerlib.annotations.SimplyRegister;
import org.zeith.improvableskills.custom.particles.ParticleDataSparkle;
import org.zeith.improvableskills.custom.particles.ParticleTypeWithCodec;

@SimplyRegister
public class ParticleTypesIS
{
	@RegistryName("sparkle")
	public static final ParticleType<ParticleDataSparkle> SPARKLE = new ParticleTypeWithCodec<>(false, ParticleDataSparkle.CODEC, ParticleDataSparkle.DESERIALIZER);
}