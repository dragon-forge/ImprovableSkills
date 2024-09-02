package org.zeith.improvableskills.custom.items;

import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import org.zeith.hammerlib.api.items.ITabItem;
import org.zeith.hammerlib.net.Network;
import org.zeith.hammerlib.util.java.Chars;
import org.zeith.improvableskills.ImprovableSkills;
import org.zeith.improvableskills.SyncSkills;
import org.zeith.improvableskills.api.registry.PlayerSkillBase;
import org.zeith.improvableskills.api.tooltip.SkillTooltip;
import org.zeith.improvableskills.cfg.ConfigsIS;
import org.zeith.improvableskills.custom.items.data.StoredSkill;
import org.zeith.improvableskills.data.PlayerDataManager;
import org.zeith.improvableskills.init.ItemsIS;
import org.zeith.improvableskills.net.PacketScrollLevelupSkill;
import org.zeith.improvableskills.net.PacketScrollUnlockedSkill;

import javax.annotation.Nullable;
import java.util.*;

public class ItemSkillScroll
		extends Item
		implements ITabItem
{
	public ItemSkillScroll()
	{
		super(new Properties().stacksTo(1));
		ImprovableSkills.TAB.add(this);
	}
	
	@Override
	public @Nullable String getCreatorModId(ItemStack stack)
	{
		var v = getSkillFromScroll(stack);
		if(v != null)
		{
			var k = ImprovableSkills.SKILLS.getKey(v);
			if(k != null) return k.getNamespace();
		}
		return null;
	}
	
	@Nullable
	public static PlayerSkillBase getSkillFromScroll(ItemStack stack)
	{
		if(!stack.isEmpty() && stack.has(StoredSkill.TYPE))
		{
			var ss = stack.get(StoredSkill.TYPE);
			return ss != null ? ss.skill() : null;
		}
		return null;
	}
	
	public static ItemStack of(PlayerSkillBase base)
	{
		if(base.getScrollState().hasScroll())
		{
			ItemStack stack = new ItemStack(ItemsIS.SKILL_SCROLL);
			stack.set(StoredSkill.TYPE, new StoredSkill(base));
			return stack;
		}
		return ItemStack.EMPTY;
	}
	
	public static void getItems(Set<ItemStack> items)
	{
		ImprovableSkills.SKILLS
				.stream()
				.filter(skill -> skill.getScrollState().hasScroll())
				.sorted(Comparator.comparing(PlayerSkillBase::getUnlocalizedName))
				.forEach(skill -> items.add(ItemSkillScroll.of(skill)));
	}
	
	@Override
	public CreativeModeTab getItemCategory()
	{
		return ImprovableSkills.TAB.tab();
	}
	
	@Override
	public void fillItemCategory(CreativeModeTab tab, Set<ItemStack> items)
	{
		if(allowedIn(tab))
			getItems(items);
	}
	
	@Override
	public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flagIn)
	{
		PlayerSkillBase base = getSkillFromScroll(stack);
		if(base == null)
			return;
		tooltip.add(base.getLocalizedName(SyncSkills.getData()).withStyle(ChatFormatting.GRAY));
		if(flagIn.isAdvanced())
			tooltip.add(Component.literal(" - " + base.getRegistryName()).withStyle(ChatFormatting.DARK_GRAY));
		
		if(ImprovableSkills.PROXY.hasShiftDown())
			tooltip.add(Component.literal(I18n.get(
							"recipe." + base.getRegistryName().getNamespace() + ":skill." + base.getRegistryName().getPath())
					.replace('&', Chars.SECTION_SIGN)).withStyle(ChatFormatting.GRAY));
		else
			tooltip.add(Component.literal(I18n.get("text.improvableskills:shiftfrecipe")
					.replace('&', Chars.SECTION_SIGN)).withStyle(ChatFormatting.GRAY));
	}
	
	@Override
	public InteractionResultHolder<ItemStack> use(Level worldIn, Player playerIn, InteractionHand handIn)
	{
		var held = playerIn.getItemInHand(handIn);
		
		if(worldIn.isClientSide) return new InteractionResultHolder<>(InteractionResult.PASS, held);
		
		return PlayerDataManager.handleDataSafely(playerIn, data ->
		{
			PlayerSkillBase base = getSkillFromScroll(held);
			
			if(base == null)
				return new InteractionResultHolder<>(InteractionResult.PASS, held);
			
			if(!data.hasSkillScroll(base) && data.unlockSkillScroll(base, true))
			{
				ItemStack used = held.copy();
				
				if(!playerIn.isCreative())
					held.shrink(1);
				
				playerIn.swing(handIn);
				worldIn.playSound(null, playerIn.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.5F, 1F);
				
				int slot = handIn == InteractionHand.OFF_HAND ? -2 : playerIn.getInventory().selected;
				Network.sendTo(new PacketScrollUnlockedSkill(slot, used, base.getRegistryName()), playerIn);
				
				return new InteractionResultHolder<>(InteractionResult.SUCCESS, held);
			} else if(data.getSkillLevel(base) < base.getMaxLevel())
			{
				data.setSkillLevel(base, data.getSkillLevel(base) + 1);
				ItemStack used = held.copy();
				
				if(!playerIn.isCreative())
					held.shrink(1);
				
				playerIn.swing(handIn);
				worldIn.playSound(null, playerIn.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.5F, 1F);
				
				int slot = handIn == InteractionHand.OFF_HAND ? -2 : playerIn.getInventory().selected;
				Network.sendTo(new PacketScrollLevelupSkill(slot, used, base.getRegistryName()), playerIn);
				return new InteractionResultHolder<>(InteractionResult.SUCCESS, held);
			}
			
			return new InteractionResultHolder<>(InteractionResult.PASS, held);
		}, new InteractionResultHolder<>(InteractionResult.PASS, held));
	}
	
	@Override
	public void inventoryTick(ItemStack pStack, Level pLevel, Entity pEntity, int pSlotId, boolean pIsSelected)
	{
		if(ConfigsIS.autouseScrolls && pEntity instanceof ServerPlayer playerIn)
		{
			PlayerDataManager.handleDataSafely(playerIn, data ->
			{
				PlayerSkillBase base = getSkillFromScroll(pStack);
				
				if(base == null)
					return;
				
				if(!data.hasSkillScroll(base) && data.unlockSkillScroll(base, true))
				{
					ItemStack used = pStack.copy();
					
					pStack.shrink(1);
					
					pLevel.playSound(null, playerIn.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.5F, 1F);
					Network.sendTo(new PacketScrollUnlockedSkill(pSlotId, used, base.getRegistryName()), playerIn);
				} else if(data.getSkillLevel(base) < base.getMaxLevel())
				{
					data.setSkillLevel(base, data.getSkillLevel(base) + 1);
					ItemStack used = pStack.copy();
					
					pStack.shrink(1);
					
					pLevel.playSound(null, playerIn.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.5F, 1F);
					Network.sendTo(new PacketScrollLevelupSkill(pSlotId, used, base.getRegistryName()), playerIn);
				}
			});
		}
		super.inventoryTick(pStack, pLevel, pEntity, pSlotId, pIsSelected);
	}
	
	@Override
	public Optional<TooltipComponent> getTooltipImage(ItemStack stack)
	{
		return Optional.ofNullable(getSkillFromScroll(stack)).map(SkillTooltip::new);
	}
}