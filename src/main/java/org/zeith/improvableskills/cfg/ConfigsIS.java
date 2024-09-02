package org.zeith.improvableskills.cfg;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import org.zeith.hammerlib.annotations.SetupConfigs;
import org.zeith.hammerlib.util.configured.ConfigFile;
import org.zeith.hammerlib.util.configured.ConfiguredLib;
import org.zeith.hammerlib.util.configured.data.IntValueRange;
import org.zeith.hammerlib.util.configured.types.ConfigCategory;
import org.zeith.hammerlib.util.configured.types.ConfigString;
import org.zeith.improvableskills.ImprovableSkills;
import org.zeith.improvableskills.api.registry.PlayerAbilityBase;
import org.zeith.improvableskills.api.registry.PlayerSkillBase;

import java.util.List;
import java.util.Set;

import static net.minecraft.world.level.storage.loot.BuiltInLootTables.*;

public class ConfigsIS
{
	public static ConfigFile config;
	
	private static ConfigCategory gameplay;
	
	public static boolean xpBank;
	public static boolean addBookToInv;
	public static boolean parchmentGeneration = true;
	public static boolean dropScrollsAfterUnlock = true;
	public static int parchmentRarity = 10;
	public static List<String> blockedParchmentChests = List.of();
	
	@SetupConfigs
	public static void reloadCustom(ConfigFile cfgs)
	{
		config = cfgs;
		
		gameplay = cfgs.setupCategory("Gameplay")
				.withComment("Gameplay affecting features");
		{
			xpBank = gameplay.getElement(ConfiguredLib.BOOLEAN, "XP Storage")
					.withDefault(true)
					.withComment("Should XP Bank be active in the book? Disabling this only hides the ability from the player.")
					.getValue();
			
			dropScrollsAfterUnlock = gameplay.getElement(ConfiguredLib.BOOLEAN, "Drop Scrolls After Unlock")
					.withDefault(true)
					.withComment("Should scrolls still drop for players that have unlocked the skill already, but haven't yet maxxed it out?")
					.getValue();
			
			var parchmentFragment = gameplay.getElement(ConfiguredLib.CATEGORY, "Parchment Fragment")
					.withComment("Various configurations for parchment fragment");
			{
				parchmentGeneration = parchmentFragment.getElement(ConfiguredLib.BOOLEAN, "Do Generation")
						.withDefault(true)
						.withComment("Should parchment fragment appear in naturally generated chests?")
						.getValue();
				
				parchmentRarity = parchmentFragment.getElement(ConfiguredLib.INT, "WorldGen Rarity")
						.withRange(IntValueRange.range(1, Integer.MAX_VALUE))
						.withDefault(10)
						.withComment("How rare should parchment fragment be? Higher values make the fragment appear less frequently inside chests.")
						.getValue()
						.intValue();
				
				boolean init = !parchmentFragment.getValue().containsKey("Chest Blocklist");
				
				var bc = parchmentFragment.getElement(ConfiguredLib.STRING.arrayOf(), "Chest Blocklist")
						.withComment("Which chests should be blocked from generating fragments?");
				var lst = bc.getElements();
				if(init)
				{
					Set<ResourceKey<LootTable>> locs = Set.of(
							VILLAGE_WEAPONSMITH,
							VILLAGE_TOOLSMITH,
							VILLAGE_ARMORER,
							VILLAGE_CARTOGRAPHER,
							VILLAGE_MASON,
							VILLAGE_SHEPHERD,
							VILLAGE_BUTCHER,
							VILLAGE_FLETCHER,
							VILLAGE_FISHER,
							VILLAGE_TANNERY,
							VILLAGE_TEMPLE,
							VILLAGE_DESERT_HOUSE,
							VILLAGE_PLAINS_HOUSE,
							VILLAGE_TAIGA_HOUSE,
							VILLAGE_SNOWY_HOUSE,
							VILLAGE_SAVANNA_HOUSE
					);
					
					for(var i : locs)
						lst.add(bc.createElement().withDefault(i.location().toString()));
				}
				
				blockedParchmentChests = bc.getElements().stream().map(ConfigString::getValue).toList();
			}
		}
		
		if(FMLEnvironment.dist == Dist.CLIENT)
		{
			var clientSide = cfgs.setupCategory("Client-side")
					.withComment("Features that only matter when the mod is loaded on client.");
			{
				addBookToInv = clientSide.getElement(ConfiguredLib.BOOLEAN, "Add Book to Inventory")
						.withDefault(true)
						.withComment("Should ImprovableSkills add it's Book of Skills into player's inventory?")
						.getValue();
			}
		}
	}
	
	public static void reloadCosts()
	{
		ConfigCategory costs = gameplay.setupSubCategory("Costs")
				.withComment("Configure how expensive each ability is");
		
		for(PlayerSkillBase skill : ImprovableSkills.SKILLS)
		{
			skill.xpCalculator.load(costs, skill.getRegistryName().toString().replace(":", "_"));
		}
	}
	
	public static boolean enableSkill(PlayerSkillBase skill, ResourceLocation id)
	{
		return config.setupCategory("Skills")
				.withComment("What skills should be enabled?")
				.getElement(ConfiguredLib.BOOLEAN, id.toString())
				.withDefault(true)
				.withComment("Should Skill \"" + skill.getUnlocalizedName(id) + "\" be added to the game?")
				.getValue();
	}
	
	public static boolean enableAbility(PlayerAbilityBase skill, ResourceLocation id)
	{
		return config.setupCategory("Abilities")
				.withComment("What abilities should be enabled?")
				.getElement(ConfiguredLib.BOOLEAN, id.toString())
				.withDefault(true)
				.withComment("Should Ability \"" + skill.getUnlocalizedName(id) + "\" be added to the game?")
				.getValue();
	}
}