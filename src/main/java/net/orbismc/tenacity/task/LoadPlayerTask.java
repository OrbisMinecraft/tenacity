/*
 * Copyright © 2022 Luis Michaelis
 * SPDX-License-Identifier: LGPL-3.0-only
 */
package net.orbismc.tenacity.task;

import net.orbismc.tenacity.Tenacity;

import java.util.Objects;
import java.util.UUID;

public class LoadPlayerTask implements Runnable {
	private final Tenacity plugin;
	private final UUID playerUUID;

	public LoadPlayerTask(Tenacity plugin, UUID player) {
		this.plugin = plugin;
		this.playerUUID = player;
	}

	@Override
	public void run() {
		plugin.withDatabase(conn -> {
			final var stmt = conn.prepareStatement("SELECT last_event FROM players WHERE uuid=?");
			stmt.setString(1, playerUUID.toString());

			final var result = stmt.executeQuery();
			if (result.next()) {
				if (!Objects.equals(result.getString(1), "save")) {
					plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new LoadPlayerTask(plugin, playerUUID), 20);
					return;
				}
			}

			final var player = plugin.getServer().getPlayer(playerUUID);
			if (player == null) {
				plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new LoadPlayerTask(plugin, playerUUID), 20);
				return;
			}

			plugin.doLoadPlayer(player);
			plugin.setLoaded(player, true);
		});
	}
}
