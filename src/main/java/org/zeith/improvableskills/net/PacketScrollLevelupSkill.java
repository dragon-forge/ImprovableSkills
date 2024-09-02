package org.zeith.improvableskills.net;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec2;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.zeith.hammerlib.net.INBTPacket;
import org.zeith.hammerlib.net.PacketContext;
import org.zeith.improvableskills.ImprovableSkills;
import org.zeith.improvableskills.SyncSkills;
import org.zeith.improvableskills.api.registry.PlayerSkillBase;
import org.zeith.improvableskills.client.rendering.ItemToBookHandler;
import org.zeith.improvableskills.client.rendering.OnTopEffects;
import org.zeith.improvableskills.client.rendering.ote.OTEBook;
import org.zeith.improvableskills.client.rendering.ote.OTEItemSkillScroll;

import java.util.*;

public class PacketScrollLevelupSkill
		implements INBTPacket
{
	private ResourceLocation skill;
	private ItemStack used;
	private int slot;
	
	public PacketScrollLevelupSkill(int slot, ItemStack used, ResourceLocation skill)
	{
		this.skill = skill;
		this.used = used;
		this.slot = slot;
	}
	
	public PacketScrollLevelupSkill()
	{
	}
	
	@Override
	public void write(CompoundTag nbt)
	{
		nbt.putString("s", skill.toString());
		nbt.putInt("i", slot);
		nbt.put("u", used.serializeNBT());
	}
	
	@Override
	public void read(CompoundTag nbt)
	{
		skill = ResourceLocation.tryParse(nbt.getString("s"));
		slot = nbt.getInt("i");
		used = ItemStack.of(nbt.getCompound("u"));
	}
	
	@Override
	@OnlyIn(Dist.CLIENT)
	public void clientExecute(PacketContext net)
	{
		Player sp = Minecraft.getInstance().player;
		if(sp == null) return;
		
		PlayerSkillBase sk = ImprovableSkills.SKILLS().getValue(skill);
		if(sk == null) return;
		
		List<PlayerSkillBase> base = new ArrayList<>();
		base.add(sk);
		sp.sendSystemMessage(Component.translatable("chat.improvableskills.page_upgraded",
				sk.getLocalizedName(SyncSkills.getData())
		));
		
		if(slot >= 9) return;
		
		Random rand = new Random();
		Minecraft mc = Minecraft.getInstance();
		Window sr = mc.getWindow();
		Vec2 v = ItemToBookHandler.getPosOfSlot(slot);
		OTEBook.show(100 + 10 + 40 + 10 * base.size());
		OnTopEffects.effects.add(new OTEItemSkillScroll(v.x, v.y, sr.getGuiScaledWidth() - 20 - 48 + rand.nextFloat() * 32, sr.getGuiScaledHeight() - 12 - 24 - rand.nextFloat() * 32, 100, used, base.toArray(new PlayerSkillBase[0])));
	}
}