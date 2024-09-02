package org.zeith.improvableskills.net;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec2;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
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

public class PacketScrollUnlockedSkill
		implements INBTPacket
{
	private ResourceLocation[] skills;
	private ItemStack used;
	private int slot;
	
	public PacketScrollUnlockedSkill(int slot, ItemStack used, ResourceLocation... skills)
	{
		this.skills = skills;
		this.used = used;
		this.slot = slot;
	}
	
	public PacketScrollUnlockedSkill()
	{
	}
	
	@Override
	public void write(RegistryFriendlyByteBuf buf)
	{
		buf.writeVarInt(skills.length);
		for(var s : skills) buf.writeResourceLocation(s);
		ItemStack.STREAM_CODEC.encode(buf, used);
		buf.writeVarInt(slot);
	}
	
	@Override
	public void read(RegistryFriendlyByteBuf buf)
	{
		int l = buf.readVarInt();
		skills = new ResourceLocation[l];
		for(int i = 0; i < l; i++)
			skills[i] = buf.readResourceLocation();
		used = ItemStack.STREAM_CODEC.decode(buf);
		slot = buf.readVarInt();
	}
	
	@Override
	@OnlyIn(Dist.CLIENT)
	public void clientExecute(PacketContext net)
	{
		Player sp = Minecraft.getInstance().player;
		if(sp == null) return;
		
		List<PlayerSkillBase> base = new ArrayList<>();
		
		for(ResourceLocation skill : skills)
		{
			PlayerSkillBase sk = ImprovableSkills.SKILLS.get(skill);
			if(sk == null) continue;
			base.add(sk);
			sp.sendSystemMessage(Component.translatable("chat.improvableskills.page_unlocked",
					sk.getLocalizedName(SyncSkills.getData())
			));
		}
		
		if(slot >= 9) return;
		
		Random rand = new Random();
		Minecraft mc = Minecraft.getInstance();
		Window sr = mc.getWindow();
		Vec2 v = ItemToBookHandler.getPosOfSlot(slot);
		OTEBook.show(100 + 10 + 40 + 10 * base.size());
		OnTopEffects.effects.add(new OTEItemSkillScroll(v.x, v.y, sr.getGuiScaledWidth() - 20 - 48 + rand.nextFloat() * 32, sr.getGuiScaledHeight() - 12 - 24 - rand.nextFloat() * 32, 100, used, base.toArray(new PlayerSkillBase[0])));
	}
}