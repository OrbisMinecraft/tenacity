/*
 * Copyright © 2022 Luis Michaelis
 * SPDX-License-Identifier: LGPL-3.0-only
 */
package net.orbismc.tenacity;

import net.orbismc.tenacity.util.DatabaseAction;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.mariadb.jdbc.MariaDbPoolDataSource;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Objects;

public final class Tenacity extends JavaPlugin {
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
	 * Runs some action inside the database.
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
}
