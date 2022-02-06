/*
 * Copyright © 2022 Luis Michaelis
 * SPDX-License-Identifier: LGPL-3.0-only
 */
package net.orbismc.tenacity;

import net.orbismc.tenacity.serial.SerializedPlayer;
import net.orbismc.tenacity.util.DatabaseAction;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.mariadb.jdbc.MariaDbPoolDataSource;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Objects;
import java.util.UUID;

public final class Tenacity extends JavaPlugin {
	private final HashSet<UUID> loadQueue = new HashSet<>();
	public TenacityConfig config;
	private MariaDbPoolDataSource pool;

	@Override
	public void onEnable() {
		try {
			this.saveDefaultConfig();

			final var configFile = new File(this.getDataFolder(), "config.yml");
			this.config = TenacityConfig.load(configFile);
		} catch (FileNotFoundException e) {
			throw new IllegalStateException("Failed to load configuration", e);
		}

		this.pool = new MariaDbPoolDataSource(config.database.url != null ? config.database.url :
				"jdbc:mariadb://%s:%d/%s?user=%s&password=%s&maxPoolSize=3".formatted(
						config.database.host,
						config.database.port,
						config.database.name,
						config.database.username,
						config.database.password
				));

		// Run the database setup
		try {
			final var resource = Objects.requireNonNull(getResource("setup.sql"));
			final var content = resource.readAllBytes();
			withDatabase(conn -> conn.createStatement().execute(new String(content, StandardCharsets.UTF_8)));
		} catch (IOException e) {
			getLogger().severe("Failed to set up the database %s".formatted(e));
		}

		this.getServer().getPluginManager().registerEvents(new TenacityEventListener(this), this);
	}

	/**
	 * Checks whether the given player's data has already been loaded or not.
	 *
	 * @param player The player to check.
	 * @return <tt>true</tt> if the player's data has been loaded and <tt>false</tt> if not.
	 */
	public boolean isLoaded(final @NotNull Player player) {
		return this.loadQueue.contains(player.getUniqueId());
	}

	/**
	 * Marks a player as being loaded or unloaded.
	 *
	 * @param player The player to mark.
	 * @param loaded Set to <tt>true</tt> to mark the player as loaded and <tt>false</tt> to mark them as not yet loaded.
	 */
	public void setLoaded(final Player player, boolean loaded) {
		if (loaded) {
			this.loadQueue.remove(player.getUniqueId());
		} else {
			this.loadQueue.add(player.getUniqueId());
		}
	}

	/**
	 * Runs some action inside the database.
	 *
	 * @param action The action to perform
	 */
	public void withDatabase(final @NotNull DatabaseAction action) {
		try (final var conn = pool.getConnection()) {
			try {
				action.perform(conn);
			} catch (final SQLException e) {
				getLogger().severe("Database access failed: %s".formatted(e));
			}
		} catch (final SQLException e) {
			getLogger().severe("Failed to establish a connection to the database at %s: %s".formatted(
					config.database.url != null ? config.database.url : config.database.host, e)
			);
		}
	}

	@Override
	public void onDisable() {
		pool.close();
	}

	/**
	 * Loads a player from the database.
	 *
	 * @param player The player to load.
	 */
	public void doLoadPlayer(final Player player) {
		this.withDatabase(conn -> {
			final var stmt = conn.prepareStatement("SELECT * FROM players WHERE uuid=?");
			stmt.setString(1, player.getUniqueId().toString());

			final var result = stmt.executeQuery();
			if (result.next()) {
				SerializedPlayer.fromDatabase(result).apply(this.config.saving, player);
				this.getLogger().info("Loaded data of player '%s' from the database successfully".formatted(player.getName()));
			} else {
				this.getLogger().info("Could not load player '%s' from the database because they joined for the first time".formatted(player.getName()));
			}

			final var update = conn.prepareStatement("UPDATE players SET last_event='load' WHERE uuid=?");
			update.setString(1, player.getUniqueId().toString());
			update.executeUpdate();
		});
	}
}
