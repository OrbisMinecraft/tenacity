/*
 * Copyright © 2022 Luis Michaelis
 * SPDX-License-Identifier: LGPL-3.0-only
 */
package net.orbismc.tenacity;

import net.orbismc.tenacity.util.DatabaseAccessor;
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
	private MariaDbPoolDataSource database;

	@Override
	public void onEnable() {
		try {
			this.saveDefaultConfig();

			final var configFile = new File(this.getDataFolder(), "config.yml");
			this.config = TenacityConfig.load(configFile);
		} catch (FileNotFoundException e) {
			throw new IllegalStateException("Failed to load configuration", e);
		}

		this.database = new MariaDbPoolDataSource(config.database.url != null ? config.database.url :
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
			final var content = new byte[resource.available()];
			resource.read(content);

			withDatabase(conn -> conn.createStatement().execute(new String(content, StandardCharsets.UTF_8)));
		} catch (IOException e) {
			e.printStackTrace();
		}

		this.getServer().getPluginManager().registerEvents(new TenacityEventListener(this), this);
	}

	public void withDatabase(final @NotNull DatabaseAccessor accessor) {
		try (final var conn = database.getConnection()) {
			try {
				accessor.handle(conn);
			} catch (Exception e) {
				getLogger().severe("Database access failed: %s".formatted(e));
			}
		} catch (SQLException e) {
			getLogger().severe("Failed to establish a connection to the database at %s: %s".formatted(config.database.url != null ? config.database.url : config.database.host, e));
		}
	}

	@Override
	public void onDisable() {
		// Plugin shutdown logic
	}
}
