/*
 * Copyright © 2022 Luis Michaelis
 * SPDX-License-Identifier: LGPL-3.0-only
 */
package net.orbismc.tenacity.util;

import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.SQLException;

public interface DatabaseAction {
	/**
	 * Perform an action which requires a database connection.
	 *
	 * @param conn The connection to use.
	 * @throws SQLException An exception occurring as a result of the database query.
	 */
	void perform(final @NotNull Connection conn) throws SQLException;
}
