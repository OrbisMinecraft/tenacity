package net.orbismc.tenacity;

import net.orbismc.tenacity.serial.SerializedPlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

public final class TenacityEventListener implements Listener {
	private final Tenacity plugin;

	public TenacityEventListener(Tenacity plugin) {
		this.plugin = plugin;
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onPlayerJoin(final @NotNull PlayerJoinEvent event) {
		final var player = event.getPlayer();

		plugin.withDatabase(conn -> {
			final var stmt = conn.prepareStatement("SELECT * FROM players WHERE uuid=?");
			stmt.setString(1, player.getUniqueId().toString());

			final var result = stmt.executeQuery();
			if (result.next()) {
				SerializedPlayer.fromDatabase(result).apply(plugin.config.saving, player);
				plugin.getLogger().info("Loaded data of player '%s' from the database successfully".formatted(player.getName()));
			} else {
				plugin.getLogger().info("Could not load player '%s' from the database because they joined for the first time".formatted(player.getName()));
			}
		});
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onPlayerQuit(final @NotNull PlayerQuitEvent event) {
		final var player = event.getPlayer();

		plugin.withDatabase(conn -> {
			final var serial = SerializedPlayer.fromPlayer(plugin.config.saving, player);
			final var stmt = conn.prepareStatement("REPLACE INTO players (uuid, " +
					"air, fire, glowing, health, absorption, active_effects, recipe_book, food_level, " +
					"food_exhaustion, food_saturation, xp_level, xp_percentage, xp_total, inventory, " +
					"ender_chest, armor_items, selected_slot) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
			stmt.setString(1, serial.uuid.toString());
			stmt.setInt(2, serial.air);
			stmt.setInt(3, serial.fire);
			stmt.setBoolean(4, serial.glowing);
			stmt.setDouble(5, serial.health);
			stmt.setDouble(6, serial.absorption);
			stmt.setString(7, serial.activeEffects);
			stmt.setString(8, serial.recipeBook);
			stmt.setInt(9, serial.foodLevel);
			stmt.setDouble(10, serial.foodExhaustion);
			stmt.setDouble(11, serial.foodSaturation);
			stmt.setInt(12, serial.xpLevel);
			stmt.setDouble(13, serial.xpPercentage);
			stmt.setInt(14, serial.xpTotal);
			stmt.setString(15, serial.inventory);
			stmt.setString(16, serial.enderChest);
			stmt.setString(17, serial.armorItems);
			stmt.setInt(18, serial.selectedSlot);
			stmt.executeUpdate();
		});
	}
}
