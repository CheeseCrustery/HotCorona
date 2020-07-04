package me.noodian.corona.player;

import io.netty.channel.*;
import me.noodian.corona.Game;
import org.bukkit.Bukkit;
import org.bukkit.entity.*;

import java.lang.reflect.*;
import java.util.UUID;
import java.util.logging.Level;


class PlayerPacketHandler extends ChannelDuplexHandler {

	private final static String NMS = "net.minecraft.server.", CRAFTBUKKIT = "org.bukkit.craftbukkit.";

	private final Player player;
	private Class<?>
			PacketPlayOutSpawnEntity,
			PacketPlayOutTitle,
			CraftPlayer,
			IChatBaseComponent,
			EnumTitleAction,
			ChatSerializer;

	PlayerPacketHandler(Player player) {

		// Set player
		this.player = player;

		// Get channel and packet using Reflection
		try {

			// Get classes
			CraftPlayer = getClass("entity.CraftPlayer", CRAFTBUKKIT);
			PacketPlayOutTitle = getClass("PacketPlayOutTitle", NMS);
			PacketPlayOutSpawnEntity = getClass("PacketPlayOutSpawnEntity", NMS);
			IChatBaseComponent = getClass("IChatBaseComponent", NMS);
			EnumTitleAction = PacketPlayOutTitle.getDeclaredClasses()[0];
			ChatSerializer = IChatBaseComponent.getDeclaredClasses()[0];

			// Add self to netty pipeline
			Object handle = CraftPlayer.getMethod("getHandle").invoke(player);
			Object playerConnection = handle.getClass().getField("playerConnection").get(handle);
			Object networkManager = playerConnection.getClass().getField("networkManager").get(playerConnection);
			Channel channel = (Channel) networkManager.getClass().getField("channel").get(networkManager);
			ChannelPipeline pipeline = channel.pipeline();
			pipeline.addBefore("packet_handler", this.player.getName(), this);

		} catch (Exception e) {
			e.printStackTrace();
			Game.get().log(Level.SEVERE, e.getMessage());
		}
	}

	@Override
	// Intercept packets everytime the server sends them out
	public void write(ChannelHandlerContext context, Object packet, ChannelPromise channelPromise) {

		try {

			// Only intercept entity packets
			if (PacketPlayOutSpawnEntity.isInstance(packet)) {

				// Get entity
				Field f_uuid = PacketPlayOutSpawnEntity.getDeclaredField("b");
				f_uuid.setAccessible(true);
				UUID uuid = (UUID) f_uuid.get(packet);
				Entity entity = Bukkit.getServer().getEntity(uuid);

				// Only intercept snowball entities
				if (entity != null && entity.getType() == EntityType.SNOWBALL) return;
			}

			// Hand on packet
			super.write(context, packet, channelPromise);
		} catch (Exception e) {
			e.printStackTrace();
			Game.get().log(Level.SEVERE, e.getMessage());
		}
	}

	// Send a title packet
	void showTitle(String text) {
		try {

			// Make packet
			Object titleEnum = EnumTitleAction.getField("TITLE").get(null);

			Method chatComponentConstructor = ChatSerializer.getMethod("a", String.class);
			Object chatComponent = chatComponentConstructor.invoke(null, "{\"text\":\"" + text +"\"}");

			Class<?>[] packetConstructorArgs = {EnumTitleAction, IChatBaseComponent, int.class, int.class, int.class};
			Constructor<?> packetConstructor = PacketPlayOutTitle.getConstructor(packetConstructorArgs);
			Object packet = packetConstructor.newInstance(titleEnum, chatComponent, 20, 40, 20);

			// Send packet
			Object handle = player.getClass().getMethod("getHandle").invoke(player);
			Object playerConnection = handle.getClass().getField("playerConnection").get(handle);
			playerConnection.getClass().getMethod("sendPacket", getClass("Packet", NMS)).invoke(playerConnection, packet);
		} catch (Exception e) {
			e.printStackTrace();
			Game.get().log(Level.SEVERE, e.getMessage());
		}
	}

	// Remove this from the netty pipeline
	void remove() {

		// Get channel
		Channel channel;
		try {
			Object handle = player.getClass().getMethod("getHandle").invoke(player);
			Object playerConnection = handle.getClass().getField("playerConnection").get(handle);
			Object networkManager = playerConnection.getClass().getField("networkManager").get(playerConnection);
			channel = (Channel) networkManager.getClass().getField("channel").get(networkManager);
		} catch (Exception e) {
			e.printStackTrace();
			Game.get().log(Level.SEVERE, e.getMessage());
			return;
		}

		// Remove from event loop
		channel.eventLoop().submit(() -> {
			channel.pipeline().remove(player.getName());
			return null;
		});
	}

	// Get a class via reflection
	private Class<?> getClass(String name, String prefix) throws ClassNotFoundException {
		String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
		return Class.forName(prefix + version + "." + name);
	}
}
