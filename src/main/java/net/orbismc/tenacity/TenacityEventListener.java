/*
 * Copyright © 2022 Luis Michaelis
 * SPDX-License-Identifier: LGPL-3.0-only
 */
package net.orbismc.tenacity;

import net.orbismc.tenacity.serial.SerializedPlayer;
import net.orbismc.tenacity.task.LoadPlayerTask;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

/**
 * The main event lister for <i>tenacity</i>.
 */
public final class TenacityEventListener implements Listener {
	private final Tenacity plugin;

	public TenacityEventListener(Tenacity plugin) {
		this.plugin = plugin;
	}

	/**
	 * Handles a player joining the world. This will load the player data from the database.
	 *
	 * @param event The event to handle.
	 */
	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onPlayerJoin(final @NotNull PlayerJoinEvent event) {
		final var player = event.getPlayer();
		plugin.setLoaded(player, false);
		plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new LoadPlayerTask(plugin, player.getUniqueId()), 20);
	}

	/**
	 * Makes sure that players whose inventories haven't loaded in yet can't actually interact with theirs.
	 *
	 * @param event The event to handle.
	 */
	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onInventoryClick(final @NotNull InventoryClickEvent event) {
		if (!(event.getWhoClicked() instanceof final Player player)) return;

		// If the player is not loaded yet, don't allow inventory clicks
		if (!plugin.isLoaded(player)) event.setCancelled(true);
	}

	/**
	 * Makes sure that players whose inventories haven't loaded in yet can't actually pick up items.
	 *
	 * @param event The event to handle.
	 */
	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onPickupItem(final @NotNull InventoryPickupItemEvent event) {
		if (!(event.getInventory().getHolder() instanceof final Player player)) return;

		// If the player is not loaded yet, don't allow picking up items
		if (!plugin.isLoaded(player)) event.setCancelled(true);
	}

	/**
	 * Handles a player leaving the world. This will save the player data to the database.
	 *
	 * @param event The event to handle.
	 */
	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onPlayerQuit(final @NotNull PlayerQuitEvent event) {
		final var player = event.getPlayer();

		plugin.withDatabase(conn -> {
			final var serial = SerializedPlayer.fromPlayer(plugin.config.saving, player);
			final var stmt = conn.prepareStatement("REPLACE INTO players (uuid, " +
					"air, fire, glowing, health, absorption, active_effects, recipe_book, food_level, " +
					"food_exhaustion, food_saturation, xp_level, xp_percentage, xp_total, inventory, " +
					"ender_chest, armor_items, selected_slot, last_event) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
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
			stmt.setString(19, "save");
			stmt.executeUpdate();

			plugin.getLogger().info("Saved player data of '%s' to the database successfully.".formatted(player.getName()));
		});
	}
}
