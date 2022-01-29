/*
 * Copyright © 2022 Luis Michaelis
 * SPDX-License-Identifier: LGPL-3.0-only
 */
package net.orbismc.tenacity.serial;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.orbismc.tenacity.TenacityConfig;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Represents serialized player data.
 */
public class SerializedPlayer {
	public static final Gson GSON = new Gson();

	public static final Type LIST_OF_OBJECTS = new TypeToken<List<Map<String, Object>>>() {
	}.getType();

	public static final Type LIST_OF_STRINGS = new TypeToken<List<String>>() {
	}.getType();

	public final UUID uuid;
	public Integer air = null;
	public Integer fire = null;
	public Boolean glowing = null;
	public Double health = null;
	public Double absorption = null;
	public String activeEffects = null;
	public String recipeBook = null;
	public Integer selectedSlot = null;
	public Integer foodLevel = null;
	public Double foodExhaustion = null;
	public Double foodSaturation = null;
	public Integer xpLevel = null;
	public Double xpPercentage = null;
	public Integer xpTotal = null;
	public String inventory = null;
	public String enderChest = null;
	public String armorItems = null;

	public SerializedPlayer(UUID uuid) {
		this.uuid = uuid;
	}

	/**
	 * Parse a list of item stacks from JSON.
	 *
	 * @param json A JSON string to read the items from.
	 * @return A list of item stacks.
	 */
	private static ItemStack[] loadItems(final @NotNull String json) {
		final List<Map<String, Object>> rawItems = GSON.fromJson(json, LIST_OF_OBJECTS);
		final ItemStack[] items = new ItemStack[rawItems.size()];

		for (int i = 0; i < rawItems.size(); i++) {
			items[i] = rawItems.get(i) != null ? ItemStack.deserialize(rawItems.get(i)) : null;
		}

		return items;
	}

	/**
	 * Dump a list of items to JSON.
	 *
	 * @param items A list of items to dump.
	 * @return The JSON representation of the items
	 */
	private static String dumpItems(final ItemStack[] items) {
		final var itemsMap = new ArrayList<Map<String, Object>>();
		for (final var item : items) itemsMap.add(item != null ? item.serialize() : null);
		return GSON.toJson(itemsMap);
	}

	/**
	 * Converts a {@link Player} object into a {@link SerializedPlayer} object, copying only the fields
	 * defined in the given config.
	 *
	 * @param config The config to use when converting.
	 * @param player The player to convert.
	 * @return The serialized player created.
	 */
	public static SerializedPlayer fromPlayer(final @NotNull TenacityConfig.TenacitySavingConfig config,
											  final @NotNull Player player) {
		final var serial = new SerializedPlayer(player.getUniqueId());

		if (config.health) {
			serial.air = player.getRemainingAir();
			serial.health = player.getHealth();
			serial.fire = player.getFireTicks();
			serial.glowing = player.isGlowing();
			serial.absorption = player.getAbsorptionAmount();
		}

		if (config.experience) {
			serial.xpLevel = player.getLevel();
			serial.xpPercentage = (double) player.getExp();
			serial.xpTotal = player.getTotalExperience();
		}

		if (config.food) {
			serial.foodLevel = player.getFoodLevel();
			serial.foodExhaustion = (double) player.getExhaustion();
			serial.foodSaturation = (double) player.getSaturation();
		}

		if (config.effects) {
			final var effects = new ArrayList<Map<String, Object>>();
			for (final var effect : player.getActivePotionEffects()) effects.add(effect.serialize());
			serial.activeEffects = GSON.toJson(effects);
		}

		if (config.recipeBook) {
			final var entries = new ArrayList<String>();
			for (final var entry : player.getDiscoveredRecipes()) entries.add(entry.toString());
			serial.recipeBook = GSON.toJson(entries);
		}

		if (config.inventory) {
			final var inventory = player.getInventory();
			serial.selectedSlot = inventory.getHeldItemSlot();
			serial.inventory = dumpItems(inventory.getContents());
			serial.armorItems = dumpItems(inventory.getArmorContents());
			serial.enderChest = GSON.toJson(player.getEnderChest().getContents());
		}

		return serial;
	}

	/**
	 * Loads a serialized player from a {@link ResultSet}. It is required that the result set have
	 * all columns present.
	 *
	 * @param result The result set to query from.
	 * @return The serialized player instance from the database.
	 */
	public static SerializedPlayer fromDatabase(final @NotNull ResultSet result) throws SQLException {
		final var serial = new SerializedPlayer(UUID.fromString(result.getString("uuid")));
		serial.air = result.getInt("air");
		serial.fire = result.getInt("fire");
		serial.glowing = result.getBoolean("glowing");
		serial.health = result.getDouble("health");
		serial.absorption = result.getDouble("absorption");
		serial.activeEffects = result.getString("active_effects");
		serial.recipeBook = result.getString("recipe_book");
		serial.selectedSlot = result.getInt("selected_slot");
		serial.foodLevel = result.getInt("food_level");
		serial.foodExhaustion = result.getDouble("food_exhaustion");
		serial.foodSaturation = result.getDouble("food_saturation");
		serial.xpLevel = result.getInt("xp_level");
		serial.xpPercentage = result.getDouble("xp_percentage");
		serial.xpTotal = result.getInt("xp_total");
		serial.inventory = result.getString("inventory");
		serial.enderChest = result.getString("ender_chest");
		serial.armorItems = result.getString("armor_items");
		return serial;
	}

	/**
	 * Apply the settings from this {@link SerializedPlayer} to the given {@link Player} object.
	 *
	 * @param config The configuration of which values to apply.
	 * @param target The player to apply to.
	 */
	public void apply(final @NotNull TenacityConfig.TenacitySavingConfig config,
					  final @NotNull Player target) {
		if (config.health) {
			target.setRemainingAir(air);
			target.setHealth(health);
			target.setFireTicks(fire);
			target.setGlowing(glowing);
			target.setAbsorptionAmount(absorption);
		}

		if (config.experience) {
			target.setLevel(xpLevel);
			target.setExp(xpPercentage.floatValue());
			target.setTotalExperience(xpTotal);
		}

		if (config.food) {
			target.setFoodLevel(foodLevel);
			target.setExhaustion(foodExhaustion.floatValue());
			target.setSaturation(foodSaturation.floatValue());
		}

		if (config.effects) {
			final List<Map<String, Object>> effects = GSON.fromJson(this.activeEffects, LIST_OF_OBJECTS);
			for (final var effect : effects) target.addPotionEffect(new PotionEffect(effect));
		}

		if (config.recipeBook) {
			final List<String> entries = GSON.fromJson(this.recipeBook, LIST_OF_STRINGS);
			for (final var entry : entries)
				target.discoverRecipe(Objects.requireNonNull(NamespacedKey.fromString(entry)));
		}

		if (config.inventory) {
			final var contents = loadItems(inventory);
			final var armor = loadItems(armorItems);
			final var echest = loadItems(enderChest);

			target.getInventory().clear();
			target.getInventory().setContents(contents);
			target.getInventory().setArmorContents(armor);
			target.getEnderChest().clear();
			target.getEnderChest().setContents(echest);
			target.getInventory().setHeldItemSlot(selectedSlot);
		}
	}
}
