package net.orbismc.tenacity.util;

import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.SQLException;

public interface DatabaseAccessor {
	void handle(final @NotNull Connection conn) throws SQLException;
}
