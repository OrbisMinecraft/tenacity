/*
 * Copyright © 2022 Luis Michaelis
 * SPDX-License-Identifier: LGPL-3.0-only
 */
package net.orbismc.tenacity;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

import java.io.*;
import java.util.HashMap;

public final class TenacityConfig {
	public TenacityDatabaseConfig database;
	public TenacitySavingConfig saving;

	/**
	 * Loads the configuration from a file.
	 *
	 * @param file The file to load the configuration from.
	 * @return The configuration loaded.
	 * @throws FileNotFoundException If the file does not exist (duh.)
	 */
	public static TenacityConfig load(final File file) throws FileNotFoundException {
		// ... I won't even bother to ask why this causes a ClassNotFound exception if
		// not using a custom Proxy to the Constructor
		final var type = new YAMLConstructorProxy(TenacityConfig.class);
		type.addClassInfo(TenacityDatabaseConfig.class);
		type.addClassInfo(TenacitySavingConfig.class);

		final var yml = new Yaml(type);
		return yml.load(new FileInputStream(file));
	}

	/**
	 * Saves the configuration to a file.
	 *
	 * @param file The file to save the configuration to.
	 * @throws IOException If writing to the file fails.
	 */
	public static void save(final TenacityConfig config, final File file) throws IOException {
		// Why ...? Why SnakeYAML? Is this really what we should be using?
		final var repr = new Representer();
		repr.addClassTag(TenacityConfig.class, Tag.MAP);
		repr.addClassTag(TenacityDatabaseConfig.class, Tag.MAP);
		repr.addClassTag(TenacitySavingConfig.class, Tag.MAP);

		var opt = new DumperOptions();
		opt.setIndent(2);
		opt.setCanonical(false);
		opt.setDefaultScalarStyle(DumperOptions.ScalarStyle.PLAIN);
		opt.setExplicitEnd(false);
		opt.setExplicitStart(false);
		opt.setPrettyFlow(true);
		opt.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

		final var yml = new Yaml(repr, opt);
		yml.dump(config, new FileWriter(file));
	}

	public static class TenacityDatabaseConfig {
		public String username;
		public String password;
		public String host;
		public int port;
		public String name;
		public String url = null;
	}

	public static class TenacitySavingConfig {
		public boolean health;
		public boolean experience;
		public boolean food;
		public boolean effects;
		public boolean recipeBook;
		public boolean inventory;
	}

	/**
	 * Hacky workaround to SnakeYAML's issue with ClassNotFound exceptions when using a regular
	 * {@link Constructor}.
	 */
	private static class YAMLConstructorProxy extends Constructor {
		private final HashMap<String, Class<?>> classMap = new HashMap<>();

		public YAMLConstructorProxy(final Class<?> theRoot) {
			super(theRoot);
			addClassInfo(theRoot);
		}

		public void addClassInfo(final Class<?> c) {
			classMap.put(c.getName(), c);
		}

		/*
		 * This is a modified version of the Constructor. Rather than using a class loader to
		 * get external classes, they are already predefined above. This approach works similar to
		 * the typeTags structure in the original constructor, except that class information is
		 * pre-populated during initialization rather than runtime.
		 *
		 * @see org.yaml.snakeyaml.constructor.Constructor#getClassForNode(org.yaml.snakeyaml.nodes.Node)
		 */
		protected Class<?> getClassForNode(final Node node) {
			String name = node.getTag().getClassName();
			Class<?> cl = classMap.get(name);
			if (cl == null)
				throw new YAMLException("Class not found: " + name);
			else
				return cl;
		}
	}
}
