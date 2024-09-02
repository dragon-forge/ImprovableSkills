package org.zeith.improvableskills.data;

import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.zeith.hammerlib.event.player.PlayerLoadedInEvent;
import org.zeith.improvableskills.ImprovableSkills;
import org.zeith.improvableskills.SyncSkills;
import org.zeith.improvableskills.api.PlayerSkillData;
import org.zeith.improvableskills.init.ComponentsIS;
import org.zeith.improvableskills.net.NetSkillCalculator;
import org.zeith.improvableskills.net.PacketSyncSkillData;

import java.io.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

@EventBusSubscriber
public class PlayerDataManager
{
	private static final Map<UUID, PlayerSkillData> DATAS = new HashMap<>();
	private static final ThreadLocal<Player> LPLAYER = ThreadLocal.withInitial(() -> null);
	
	public static void handleDataSafely(Player player, Consumer<PlayerSkillData> acceptor)
	{
		PlayerSkillData psd = getDataFor(player);
		if(psd != null) acceptor.accept(psd);
	}
	
	public static <T> T handleDataSafely(Player player, Function<PlayerSkillData, T> acceptor, T defaultValue)
	{
		PlayerSkillData psd = getDataFor(player);
		if(psd != null)
			return acceptor.apply(psd);
		return defaultValue;
	}
	
	public static PlayerSkillData getDataFor(Player player)
	{
		if(player == null || player instanceof FakePlayer) return null;
		if(player.level().isClientSide)
		{
			if(player.isLocalPlayer()) return SyncSkills.getData();
			return player.getData(ComponentsIS.SKILL_DATA);
		}
		
		LPLAYER.set(player);
		PlayerSkillData data = getDataFor(player.getGameProfile().getId());
		
		// Update player reference -- keep it up-to-date
		if(data != null && data.getPlayer() != player)
			DATAS.put(player.getGameProfile().getId(), data = PlayerSkillData.deserialize(player, data.serializeNBT(player.registryAccess())));
		
		return data != null ? data.toCurrent(player) : null;
	}
	
	private static PlayerSkillData getDataFor(UUID id)
	{
		if(id == null) return null;
		if(DATAS.containsKey(id)) return DATAS.get(id);
		
		Player epl = LPLAYER.get();
		
		// This should never happen, but if it does, we try to reconstruct player's data from the persistent tag in case it's there. (remote players, maybe?!)
		if(epl instanceof ServerPlayer mp)
		{
			var data = mp.getData(ComponentsIS.SKILL_DATA);
			if(data != null) DATAS.put(mp.getUUID(), data);
		}
		
		return null;
	}
	
	///////////////////// DATA LIFECYCLE EVENTS /////////////////////
	
	@SubscribeEvent
	public static void playerTick(PlayerTickEvent.Pre e)
	{
		PlayerDataManager.handleDataSafely(e.getEntity(), PlayerSkillData::handleTick);
	}
	
	@SubscribeEvent
	public static void playerClone(PlayerEvent.Clone e)
	{
		if(e.isWasDeath()) return;
		var oldPlayer = e.getOriginal();
		var newPlayer = e.getEntity();
		
		var od = getDataFor(oldPlayer);
		if(od == null) return;
		
		od.player = newPlayer;
		
		var nd = getDataFor(newPlayer);
		
		if(od == nd) return;
		
		nd.deserializeNBT(newPlayer.registryAccess(), od.serializeNBT(oldPlayer.registryAccess()));
	}
	
	@SubscribeEvent
	public static void playerLoadedIn(PlayerLoadedInEvent e)
	{
		ImprovableSkills.LOG.info("Sending ability data to {} ({})", e.getEntity().getGameProfile().getName(), e.getEntity().getGameProfile().getId());
		PlayerDataManager.handleDataSafely(e.getEntity(), PlayerSkillData::sync);
		NetSkillCalculator.pack().build().sendTo(e.getEntity());
	}
	
	@SubscribeEvent
	public static void respawn(PlayerEvent.PlayerRespawnEvent e)
	{
		if(e.getEntity() instanceof ServerPlayer mp)
			PacketSyncSkillData.sync(mp);
	}
	
	@SubscribeEvent
	public static void serverTick(ServerTickEvent.Post e)
	{
		MinecraftServer mcs = e.getServer();
		PlayerDataManager.DATAS.keySet().removeIf(uuid ->
		{
			var mp = mcs.getPlayerList().getPlayer(uuid);
			if(mp == null) return true;
			PlayerSkillData data = PlayerDataManager.DATAS.get(uuid);
			data.player = mp;
			return false;
		});
	}
	
	@SubscribeEvent
	public static void loadPlayerFromFile(PlayerEvent.LoadFromFile e)
	{
		CompoundTag nbt = null;
		
		try
		{
			File mainFile = e.getPlayerFile(".is3.dat");
			
			if(mainFile.isFile())
				nbt = NbtIo.readCompressed(new FileInputStream(mainFile), NbtAccounter.unlimitedHeap());
		} catch(Exception error)
		{
			ImprovableSkills.LOG.warn("Failed to load player data for {}", e.getEntity().getName());
			error.printStackTrace();
			
			File oldFile = e.getPlayerFile(".is3.dat_old");
			if(oldFile.isFile())
			{
				ImprovableSkills.LOG.warn("Detected old data file forp layer {}, trying to read...", e.getEntity().getName());
				try
				{
					nbt = NbtIo.readCompressed(new FileInputStream(oldFile), NbtAccounter.unlimitedHeap());
				} catch(Exception error2)
				{
					ImprovableSkills.LOG.warn("Failed to load player backup data for {}", e.getEntity().getName());
					error2.printStackTrace();
				}
			}
		}
		
		if(nbt != null) DATAS.put(UUID.fromString(e.getPlayerUUID()), PlayerSkillData.deserialize(e.getEntity(), nbt));
		else DATAS.put(UUID.fromString(e.getPlayerUUID()), new PlayerSkillData(e.getEntity()));
	}
	
	@SubscribeEvent
	public static void savePlayerToFile(PlayerEvent.SaveToFile e)
	{
		PlayerSkillData data = getDataFor(e.getEntity());
		if(data == null)
			return;
		try
		{
			CompoundTag nbt = data.serializeNBT(e.getEntity().registryAccess());
			File tmp = e.getPlayerFile(".is3.dat.tmp");
			File main = e.getPlayerFile(".is3.dat");
			File mainOld = e.getPlayerFile(".is3.dat_old");
			NbtIo.writeCompressed(nbt, new FileOutputStream(tmp));
			if(mainOld.isFile()) mainOld.delete();
			if(main.exists()) main.renameTo(mainOld);
			tmp.renameTo(main);
		} catch(Exception var5)
		{
			ImprovableSkills.LOG.warn("Failed to save player data for {}", e.getEntity().getName());
		}
	}
}