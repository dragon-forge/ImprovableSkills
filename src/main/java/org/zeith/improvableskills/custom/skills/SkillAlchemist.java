package org.zeith.improvableskills.custom.skills;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;
import org.zeith.improvableskills.api.PlayerSkillData;
import org.zeith.improvableskills.api.registry.PlayerSkillBase;
import org.zeith.improvableskills.mixins.BrewingStandBlockEntityAccessor;

public class SkillAlchemist
		extends PlayerSkillBase
{
	public SkillAlchemist()
	{
		super(15);
		setupScroll();
		setColor(0xDD783E);
		getLoot().chance.n = 10;
		getLoot().setLootTable(EntityType.WITCH.getDefaultLootTable());
		xpCalculator.xpValue = 2;
	}
	
	public static final DustParticleOptions BREWING_STAND_DUST = new DustParticleOptions(new Vector3f(1F, 1F, 0F), 1.0F);
	
	@Override
	public void tick(PlayerSkillData data, boolean isActive)
	{
		int lvl = data.getSkillLevel(this);
		boolean working = isActive && lvl > 0;
		
		Level level = data.player.level();
		
		if(!working || level.isClientSide)
			return;
		
		BlockPos center = data.player.blockPosition();
		
		int rad = 3;
		BlockPos.betweenClosed(center.offset(-rad, -rad, -rad), center.offset(rad, rad, rad))
				.forEach(pos ->
				{
					if(level.getBlockEntity(pos) instanceof BrewingStandBlockEntityAccessor tef)
					{
						int progress = tef.getBrewTime();
						
						if(progress > 0)
							tef.setBrewTime(Math.max(progress - 2 * (int) Math.sqrt(lvl * 2), 1));
						
						if(level.random.nextInt(9) == 0 && level instanceof ServerLevel sl)
							sl.sendParticles(BREWING_STAND_DUST, pos.getX() + 0.5, pos.getY() + 0.85, pos.getZ() + 0.5, 1, 0, 0, 0, 0);
					}
				});
	}
}