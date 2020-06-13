package me.noodian.corona.player;

import io.netty.channel.*;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.util.UUID;

class PlayerPacketHandler extends ChannelDuplexHandler {

	private final Player m_Player;
	private Channel m_Channel;
	private Class<?> cm_PacketPlayOutSpawnEntity;

	PlayerPacketHandler(Player player) {

		// Set player
		m_Player = player;

		// Get channel and packet using Reflection
		try {
			String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];

			// Get channel object
			Class <?> c_craftPlayer = Class.forName("net.minecraft.server." + version + ".CraftPlayer");
			Object handle = c_craftPlayer.getMethod("getHandle").invoke(player);
			Object playerConnection = handle.getClass().getField("playerConnection").get(handle);
			Object networkManager = playerConnection.getClass().getField("networkManager").get(playerConnection);
			m_Channel = (Channel) networkManager.getClass().getField("channel").get(networkManager);

			// Get packet class
			cm_PacketPlayOutSpawnEntity = Class.forName("net.minecraft.server." + version + ".PacketPlayOutSpawnEntity");
		} catch (Exception e) {
			e.printStackTrace();
		}

		// Add self to netty pipeline
		ChannelPipeline pipeline = m_Channel.pipeline();
		pipeline.addBefore("packet_handler", m_Player.getName(), this);
	}

	void Remove() {
		m_Channel.eventLoop().submit(() -> {
			m_Channel.pipeline().remove(m_Player.getName());
			return null;
		});
	}

	@Override
	public void write(ChannelHandlerContext context, Object packet, ChannelPromise channelPromise) {

		try {
			if (cm_PacketPlayOutSpawnEntity.isInstance(packet)) {

				Field f_uuid = cm_PacketPlayOutSpawnEntity.getDeclaredField("b");
				f_uuid.setAccessible(true);
				UUID uuid = (UUID) f_uuid.get(packet);
				Entity entity = Bukkit.getServer().getEntity(uuid);

				if (entity != null && entity.getType() == EntityType.SNOWBALL) return;
			}

			super.write(context, packet, channelPromise);
		} catch (Exception e) {
			System.out.println("[Corona] ERROR while trying to write outbound packet:");
			System.out.println(e.toString());
		}
	}
}
