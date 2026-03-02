package org.patchbukkit;

import org.bukkit.Material;
import org.bukkit.material.MaterialData;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings({ "deprecation", "removal" })
public final class PatchBukkitLegacy {
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(PatchBukkitLegacy.class);

    private static final String[] COLORS = {
        "WHITE", "ORANGE", "MAGENTA", "LIGHT_BLUE", "YELLOW", "LIME", "PINK",
        "GRAY", "LIGHT_GRAY", "CYAN", "PURPLE", "BLUE", "BROWN", "GREEN", "RED", "BLACK"
    };

    // Banner data values use inverted color order (0=BLACK, 15=WHITE)
    private static final String[] BANNER_COLORS = {
        "BLACK", "RED", "GREEN", "BROWN", "BLUE", "PURPLE", "CYAN", "LIGHT_GRAY",
        "GRAY", "PINK", "LIME", "YELLOW", "LIGHT_BLUE", "MAGENTA", "ORANGE", "WHITE"
    };

    // Data-value dependent mappings: legacy name -> array of modern names indexed by data value
    private static final Map<String, String[]> DATA_VALUE_MAP = new HashMap<>();

    // Simple 1:1 rename mappings: legacy name (without LEGACY_ prefix) -> modern name
    private static final Map<String, String> RENAME_MAP = new HashMap<>();

    static {
        initDataValueMap();
        initRenameMap();
    }

    private static String[] withSuffix(String[] prefixes, String suffix) {
        String[] result = new String[prefixes.length];
        for (int i = 0; i < prefixes.length; i++) {
            result[i] = prefixes[i] + "_" + suffix;
        }
        return result;
    }

    private static void initDataValueMap() {
        // 16-color materials
        DATA_VALUE_MAP.put("WOOL", withSuffix(COLORS, "WOOL"));
        DATA_VALUE_MAP.put("STAINED_GLASS", withSuffix(COLORS, "STAINED_GLASS"));
        DATA_VALUE_MAP.put("STAINED_GLASS_PANE", withSuffix(COLORS, "STAINED_GLASS_PANE"));
        DATA_VALUE_MAP.put("STAINED_CLAY", withSuffix(COLORS, "TERRACOTTA"));
        DATA_VALUE_MAP.put("CARPET", withSuffix(COLORS, "CARPET"));
        DATA_VALUE_MAP.put("CONCRETE", withSuffix(COLORS, "CONCRETE"));
        DATA_VALUE_MAP.put("CONCRETE_POWDER", withSuffix(COLORS, "CONCRETE_POWDER"));
        DATA_VALUE_MAP.put("BED", withSuffix(COLORS, "BED"));
        DATA_VALUE_MAP.put("BED_BLOCK", withSuffix(COLORS, "BED"));
        DATA_VALUE_MAP.put("BANNER", withSuffix(BANNER_COLORS, "BANNER"));
        DATA_VALUE_MAP.put("STANDING_BANNER", withSuffix(BANNER_COLORS, "BANNER"));
        DATA_VALUE_MAP.put("WALL_BANNER", withSuffix(BANNER_COLORS, "WALL_BANNER"));

        // Wood types (LOG/LEAVES use data % 4 to strip axis/decay bits)
        DATA_VALUE_MAP.put("WOOD", new String[]{
            "OAK_PLANKS", "SPRUCE_PLANKS", "BIRCH_PLANKS",
            "JUNGLE_PLANKS", "ACACIA_PLANKS", "DARK_OAK_PLANKS"
        });
        DATA_VALUE_MAP.put("LOG", new String[]{
            "OAK_LOG", "SPRUCE_LOG", "BIRCH_LOG", "JUNGLE_LOG"
        });
        DATA_VALUE_MAP.put("LOG_2", new String[]{"ACACIA_LOG", "DARK_OAK_LOG"});
        DATA_VALUE_MAP.put("LEAVES", new String[]{
            "OAK_LEAVES", "SPRUCE_LEAVES", "BIRCH_LEAVES", "JUNGLE_LEAVES"
        });
        DATA_VALUE_MAP.put("LEAVES_2", new String[]{"ACACIA_LEAVES", "DARK_OAK_LEAVES"});
        DATA_VALUE_MAP.put("SAPLING", new String[]{
            "OAK_SAPLING", "SPRUCE_SAPLING", "BIRCH_SAPLING",
            "JUNGLE_SAPLING", "ACACIA_SAPLING", "DARK_OAK_SAPLING"
        });
        // Wood slabs: 8-entry array so data % 8 strips upper/lower bit
        String[] woodSlabs = {
            "OAK_SLAB", "SPRUCE_SLAB", "BIRCH_SLAB", "JUNGLE_SLAB",
            "ACACIA_SLAB", "DARK_OAK_SLAB", "OAK_SLAB", "OAK_SLAB"
        };
        DATA_VALUE_MAP.put("WOOD_STEP", woodSlabs);
        DATA_VALUE_MAP.put("WOOD_DOUBLE_STEP", woodSlabs);

        // Stone variants
        DATA_VALUE_MAP.put("STONE", new String[]{
            "STONE", "GRANITE", "POLISHED_GRANITE", "DIORITE",
            "POLISHED_DIORITE", "ANDESITE", "POLISHED_ANDESITE"
        });
        DATA_VALUE_MAP.put("DIRT", new String[]{"DIRT", "COARSE_DIRT", "PODZOL"});
        DATA_VALUE_MAP.put("SAND", new String[]{"SAND", "RED_SAND"});
        DATA_VALUE_MAP.put("SANDSTONE", new String[]{
            "SANDSTONE", "CHISELED_SANDSTONE", "CUT_SANDSTONE"
        });
        DATA_VALUE_MAP.put("RED_SANDSTONE", new String[]{
            "RED_SANDSTONE", "CHISELED_RED_SANDSTONE", "CUT_RED_SANDSTONE"
        });
        DATA_VALUE_MAP.put("SMOOTH_BRICK", new String[]{
            "STONE_BRICKS", "MOSSY_STONE_BRICKS",
            "CRACKED_STONE_BRICKS", "CHISELED_STONE_BRICKS"
        });
        DATA_VALUE_MAP.put("PRISMARINE", new String[]{
            "PRISMARINE", "PRISMARINE_BRICKS", "DARK_PRISMARINE"
        });
        DATA_VALUE_MAP.put("COBBLE_WALL", new String[]{
            "COBBLESTONE_WALL", "MOSSY_COBBLESTONE_WALL"
        });
        // Quartz: 5-entry array so data 3,4 (pillar facing) still resolve to QUARTZ_PILLAR
        DATA_VALUE_MAP.put("QUARTZ_BLOCK", new String[]{
            "QUARTZ_BLOCK", "CHISELED_QUARTZ_BLOCK",
            "QUARTZ_PILLAR", "QUARTZ_PILLAR", "QUARTZ_PILLAR"
        });
        // Anvil: 12-entry array; damage = data/4, facing = data%4
        DATA_VALUE_MAP.put("ANVIL", new String[]{
            "ANVIL", "ANVIL", "ANVIL", "ANVIL",
            "CHIPPED_ANVIL", "CHIPPED_ANVIL", "CHIPPED_ANVIL", "CHIPPED_ANVIL",
            "DAMAGED_ANVIL", "DAMAGED_ANVIL", "DAMAGED_ANVIL", "DAMAGED_ANVIL"
        });
        DATA_VALUE_MAP.put("SPONGE", new String[]{"SPONGE", "WET_SPONGE"});

        // Stone slabs (data % 8 strips upper/lower bit)
        String[] stoneSlabs = {
            "STONE_SLAB", "SANDSTONE_SLAB", "PETRIFIED_OAK_SLAB",
            "COBBLESTONE_SLAB", "BRICK_SLAB", "STONE_BRICK_SLAB",
            "NETHER_BRICK_SLAB", "QUARTZ_SLAB"
        };
        DATA_VALUE_MAP.put("STEP", stoneSlabs);
        DATA_VALUE_MAP.put("DOUBLE_STEP", stoneSlabs);

        // Silverfish blocks
        DATA_VALUE_MAP.put("MONSTER_EGGS", new String[]{
            "INFESTED_STONE", "INFESTED_COBBLESTONE", "INFESTED_STONE_BRICKS",
            "INFESTED_MOSSY_STONE_BRICKS", "INFESTED_CRACKED_STONE_BRICKS",
            "INFESTED_CHISELED_STONE_BRICKS"
        });

        // Plants
        DATA_VALUE_MAP.put("LONG_GRASS", new String[]{"DEAD_BUSH", "SHORT_GRASS", "FERN"});
        DATA_VALUE_MAP.put("RED_ROSE", new String[]{
            "POPPY", "BLUE_ORCHID", "ALLIUM", "AZURE_BLUET",
            "RED_TULIP", "ORANGE_TULIP", "WHITE_TULIP", "PINK_TULIP", "OXEYE_DAISY"
        });
        DATA_VALUE_MAP.put("DOUBLE_PLANT", new String[]{
            "SUNFLOWER", "LILAC", "TALL_GRASS", "LARGE_FERN", "ROSE_BUSH", "PEONY"
        });

        // Items with data variants
        DATA_VALUE_MAP.put("INK_SACK", new String[]{
            "INK_SAC", "RED_DYE", "GREEN_DYE", "COCOA_BEANS",
            "LAPIS_LAZULI", "PURPLE_DYE", "CYAN_DYE", "LIGHT_GRAY_DYE",
            "GRAY_DYE", "PINK_DYE", "LIME_DYE", "YELLOW_DYE",
            "LIGHT_BLUE_DYE", "MAGENTA_DYE", "ORANGE_DYE", "BONE_MEAL"
        });
        DATA_VALUE_MAP.put("RAW_FISH", new String[]{
            "COD", "SALMON", "TROPICAL_FISH", "PUFFERFISH"
        });
        DATA_VALUE_MAP.put("COOKED_FISH", new String[]{"COOKED_COD", "COOKED_SALMON"});
        DATA_VALUE_MAP.put("COAL", new String[]{"COAL", "CHARCOAL"});
        DATA_VALUE_MAP.put("GOLDEN_APPLE", new String[]{
            "GOLDEN_APPLE", "ENCHANTED_GOLDEN_APPLE"
        });
        String[] skulls = {
            "SKELETON_SKULL", "WITHER_SKELETON_SKULL", "ZOMBIE_HEAD",
            "PLAYER_HEAD", "CREEPER_HEAD", "DRAGON_HEAD"
        };
        DATA_VALUE_MAP.put("SKULL_ITEM", skulls);
        DATA_VALUE_MAP.put("SKULL", skulls);
    }

    private static void initRenameMap() {
        // GOLD_* -> GOLDEN_*
        RENAME_MAP.put("GOLD_SWORD", "GOLDEN_SWORD");
        RENAME_MAP.put("GOLD_PICKAXE", "GOLDEN_PICKAXE");
        RENAME_MAP.put("GOLD_AXE", "GOLDEN_AXE");
        RENAME_MAP.put("GOLD_SPADE", "GOLDEN_SHOVEL");
        RENAME_MAP.put("GOLD_HOE", "GOLDEN_HOE");
        RENAME_MAP.put("GOLD_HELMET", "GOLDEN_HELMET");
        RENAME_MAP.put("GOLD_CHESTPLATE", "GOLDEN_CHESTPLATE");
        RENAME_MAP.put("GOLD_LEGGINGS", "GOLDEN_LEGGINGS");
        RENAME_MAP.put("GOLD_BOOTS", "GOLDEN_BOOTS");
        RENAME_MAP.put("GOLD_BARDING", "GOLDEN_HORSE_ARMOR");
        RENAME_MAP.put("GOLD_PLATE", "LIGHT_WEIGHTED_PRESSURE_PLATE");
        RENAME_MAP.put("GOLD_RECORD", "MUSIC_DISC_13");

        // WOOD_* -> WOODEN_*
        RENAME_MAP.put("WOOD_SWORD", "WOODEN_SWORD");
        RENAME_MAP.put("WOOD_PICKAXE", "WOODEN_PICKAXE");
        RENAME_MAP.put("WOOD_AXE", "WOODEN_AXE");
        RENAME_MAP.put("WOOD_SPADE", "WOODEN_SHOVEL");
        RENAME_MAP.put("WOOD_HOE", "WOODEN_HOE");
        RENAME_MAP.put("WOOD_PLATE", "OAK_PRESSURE_PLATE");
        RENAME_MAP.put("WOOD_BUTTON", "OAK_BUTTON");
        RENAME_MAP.put("WOOD_DOOR", "OAK_DOOR");

        // *_SPADE -> *_SHOVEL
        RENAME_MAP.put("DIAMOND_SPADE", "DIAMOND_SHOVEL");
        RENAME_MAP.put("IRON_SPADE", "IRON_SHOVEL");
        RENAME_MAP.put("STONE_SPADE", "STONE_SHOVEL");

        // Block renames
        RENAME_MAP.put("GRASS", "GRASS_BLOCK");
        RENAME_MAP.put("WEB", "COBWEB");
        RENAME_MAP.put("WORKBENCH", "CRAFTING_TABLE");
        RENAME_MAP.put("SOIL", "FARMLAND");
        RENAME_MAP.put("MOB_SPAWNER", "SPAWNER");
        RENAME_MAP.put("THIN_GLASS", "GLASS_PANE");
        RENAME_MAP.put("IRON_FENCE", "IRON_BARS");
        RENAME_MAP.put("NETHER_FENCE", "NETHER_BRICK_FENCE");
        RENAME_MAP.put("FENCE", "OAK_FENCE");
        RENAME_MAP.put("FENCE_GATE", "OAK_FENCE_GATE");
        RENAME_MAP.put("TRAP_DOOR", "OAK_TRAPDOOR");
        RENAME_MAP.put("HARD_CLAY", "TERRACOTTA");
        RENAME_MAP.put("STONE_PLATE", "STONE_PRESSURE_PLATE");
        RENAME_MAP.put("IRON_PLATE", "HEAVY_WEIGHTED_PRESSURE_PLATE");
        RENAME_MAP.put("BURNING_FURNACE", "FURNACE");
        RENAME_MAP.put("ENCHANTMENT_TABLE", "ENCHANTING_TABLE");
        RENAME_MAP.put("ENDER_PORTAL", "END_PORTAL");
        RENAME_MAP.put("ENDER_PORTAL_FRAME", "END_PORTAL_FRAME");
        RENAME_MAP.put("ENDER_STONE", "END_STONE");
        RENAME_MAP.put("END_BRICKS", "END_STONE_BRICKS");
        RENAME_MAP.put("MELON_BLOCK", "MELON");
        RENAME_MAP.put("SMOOTH_STAIRS", "SANDSTONE_STAIRS");
        RENAME_MAP.put("WOOD_STAIRS", "OAK_STAIRS");
        RENAME_MAP.put("WOODEN_DOOR", "OAK_DOOR");
        RENAME_MAP.put("SIGN_POST", "OAK_SIGN");
        RENAME_MAP.put("SIGN", "OAK_SIGN");
        RENAME_MAP.put("WALL_SIGN", "OAK_WALL_SIGN");
        RENAME_MAP.put("SUGAR_CANE_BLOCK", "SUGAR_CANE");
        RENAME_MAP.put("PISTON_BASE", "PISTON");
        RENAME_MAP.put("PISTON_STICKY_BASE", "STICKY_PISTON");
        RENAME_MAP.put("REDSTONE_LAMP_ON", "REDSTONE_LAMP");
        RENAME_MAP.put("REDSTONE_LAMP_OFF", "REDSTONE_LAMP");
        RENAME_MAP.put("REDSTONE_TORCH_ON", "REDSTONE_TORCH");
        RENAME_MAP.put("REDSTONE_TORCH_OFF", "REDSTONE_TORCH");
        RENAME_MAP.put("DIODE", "REPEATER");
        RENAME_MAP.put("DIODE_BLOCK_ON", "REPEATER");
        RENAME_MAP.put("DIODE_BLOCK_OFF", "REPEATER");
        RENAME_MAP.put("REDSTONE_COMPARATOR", "COMPARATOR");
        RENAME_MAP.put("REDSTONE_COMPARATOR_ON", "COMPARATOR");
        RENAME_MAP.put("REDSTONE_COMPARATOR_OFF", "COMPARATOR");
        RENAME_MAP.put("DAYLIGHT_DETECTOR_INVERTED", "DAYLIGHT_DETECTOR");
        RENAME_MAP.put("COMMAND", "COMMAND_BLOCK");
        RENAME_MAP.put("COMMAND_CHAIN", "CHAIN_COMMAND_BLOCK");
        RENAME_MAP.put("COMMAND_REPEATING", "REPEATING_COMMAND_BLOCK");
        RENAME_MAP.put("YELLOW_FLOWER", "DANDELION");
        RENAME_MAP.put("BRICK", "BRICKS");
        RENAME_MAP.put("NETHER_BRICK", "NETHER_BRICKS");
        RENAME_MAP.put("NETHER_BRICK_ITEM", "NETHER_BRICK");
        RENAME_MAP.put("CLAY_BRICK", "BRICK");
        RENAME_MAP.put("STATIONARY_WATER", "WATER");
        RENAME_MAP.put("STATIONARY_LAVA", "LAVA");
        RENAME_MAP.put("GLOWING_REDSTONE_ORE", "REDSTONE_ORE");
        RENAME_MAP.put("HUGE_MUSHROOM_1", "BROWN_MUSHROOM_BLOCK");
        RENAME_MAP.put("HUGE_MUSHROOM_2", "RED_MUSHROOM_BLOCK");
        RENAME_MAP.put("CAKE_BLOCK", "CAKE");
        RENAME_MAP.put("CROPS", "WHEAT");
        RENAME_MAP.put("POTATO", "POTATOES");
        RENAME_MAP.put("POTATO_ITEM", "POTATO");
        RENAME_MAP.put("CARROT", "CARROTS");
        RENAME_MAP.put("CARROT_ITEM", "CARROT");
        RENAME_MAP.put("BEETROOT_BLOCK", "BEETROOTS");
        RENAME_MAP.put("GRASS_PATH", "DIRT_PATH");
        RENAME_MAP.put("PISTON_EXTENSION", "PISTON_HEAD");
        RENAME_MAP.put("PISTON_MOVING_PIECE", "MOVING_PISTON");
        RENAME_MAP.put("IRON_DOOR_BLOCK", "IRON_DOOR");
        RENAME_MAP.put("RAILS", "RAIL");
        RENAME_MAP.put("PORTAL", "NETHER_PORTAL");
        RENAME_MAP.put("MYCEL", "MYCELIUM");
        RENAME_MAP.put("WATER_LILY", "LILY_PAD");
        RENAME_MAP.put("NETHER_WARTS", "NETHER_WART");
        RENAME_MAP.put("SPRUCE_WOOD_STAIRS", "SPRUCE_STAIRS");
        RENAME_MAP.put("BIRCH_WOOD_STAIRS", "BIRCH_STAIRS");
        RENAME_MAP.put("JUNGLE_WOOD_STAIRS", "JUNGLE_STAIRS");
        RENAME_MAP.put("MAGMA", "MAGMA_BLOCK");
        RENAME_MAP.put("RED_NETHER_BRICK", "RED_NETHER_BRICKS");
        RENAME_MAP.put("QUARTZ_ORE", "NETHER_QUARTZ_ORE");
        RENAME_MAP.put("DOUBLE_STONE_SLAB2", "RED_SANDSTONE_SLAB");
        RENAME_MAP.put("STONE_SLAB2", "RED_SANDSTONE_SLAB");
        RENAME_MAP.put("PURPUR_DOUBLE_SLAB", "PURPUR_SLAB");
        RENAME_MAP.put("SILVER_SHULKER_BOX", "LIGHT_GRAY_SHULKER_BOX");
        RENAME_MAP.put("SILVER_GLAZED_TERRACOTTA", "LIGHT_GRAY_GLAZED_TERRACOTTA");

        // Item renames
        RENAME_MAP.put("SULPHUR", "GUNPOWDER");
        RENAME_MAP.put("WATCH", "CLOCK");
        RENAME_MAP.put("LEASH", "LEAD");
        RENAME_MAP.put("FIREWORK", "FIREWORK_ROCKET");
        RENAME_MAP.put("FIREWORK_CHARGE", "FIREWORK_STAR");
        RENAME_MAP.put("BOOK_AND_QUILL", "WRITABLE_BOOK");
        RENAME_MAP.put("SNOW_BALL", "SNOWBALL");
        RENAME_MAP.put("EXP_BOTTLE", "EXPERIENCE_BOTTLE");
        RENAME_MAP.put("EMPTY_MAP", "MAP");
        RENAME_MAP.put("MELON", "MELON_SLICE");
        RENAME_MAP.put("SPECKLED_MELON", "GLISTERING_MELON_SLICE");
        RENAME_MAP.put("RAW_BEEF", "BEEF");
        RENAME_MAP.put("RAW_CHICKEN", "CHICKEN");
        RENAME_MAP.put("GRILLED_PORK", "COOKED_PORKCHOP");
        RENAME_MAP.put("PORK", "PORKCHOP");
        RENAME_MAP.put("MUSHROOM_SOUP", "MUSHROOM_STEW");
        RENAME_MAP.put("NETHER_STALK", "NETHER_WART");
        RENAME_MAP.put("SEEDS", "WHEAT_SEEDS");
        RENAME_MAP.put("TOTEM", "TOTEM_OF_UNDYING");
        RENAME_MAP.put("CHORUS_FRUIT_POPPED", "POPPED_CHORUS_FRUIT");
        RENAME_MAP.put("FLOWER_POT_ITEM", "FLOWER_POT");
        RENAME_MAP.put("BREWING_STAND_ITEM", "BREWING_STAND");
        RENAME_MAP.put("CAULDRON_ITEM", "CAULDRON");
        RENAME_MAP.put("EYE_OF_ENDER", "ENDER_EYE");
        RENAME_MAP.put("FIREBALL", "FIRE_CHARGE");
        RENAME_MAP.put("CARROT_STICK", "CARROT_ON_A_STICK");
        RENAME_MAP.put("DRAGONS_BREATH", "DRAGON_BREATH");
        RENAME_MAP.put("MONSTER_EGG", "PIG_SPAWN_EGG");

        // Minecarts
        RENAME_MAP.put("STORAGE_MINECART", "CHEST_MINECART");
        RENAME_MAP.put("POWERED_MINECART", "FURNACE_MINECART");
        RENAME_MAP.put("EXPLOSIVE_MINECART", "TNT_MINECART");
        RENAME_MAP.put("COMMAND_MINECART", "COMMAND_BLOCK_MINECART");

        // Boats
        RENAME_MAP.put("BOAT", "OAK_BOAT");
        RENAME_MAP.put("BOAT_SPRUCE", "SPRUCE_BOAT");
        RENAME_MAP.put("BOAT_BIRCH", "BIRCH_BOAT");
        RENAME_MAP.put("BOAT_JUNGLE", "JUNGLE_BOAT");
        RENAME_MAP.put("BOAT_ACACIA", "ACACIA_BOAT");
        RENAME_MAP.put("BOAT_DARK_OAK", "DARK_OAK_BOAT");

        // Horse armor
        RENAME_MAP.put("IRON_BARDING", "IRON_HORSE_ARMOR");
        RENAME_MAP.put("DIAMOND_BARDING", "DIAMOND_HORSE_ARMOR");

        // Door items
        RENAME_MAP.put("SPRUCE_DOOR_ITEM", "SPRUCE_DOOR");
        RENAME_MAP.put("BIRCH_DOOR_ITEM", "BIRCH_DOOR");
        RENAME_MAP.put("JUNGLE_DOOR_ITEM", "JUNGLE_DOOR");
        RENAME_MAP.put("ACACIA_DOOR_ITEM", "ACACIA_DOOR");
        RENAME_MAP.put("DARK_OAK_DOOR_ITEM", "DARK_OAK_DOOR");

        // Music discs
        RENAME_MAP.put("GREEN_RECORD", "MUSIC_DISC_CAT");
        RENAME_MAP.put("RECORD_3", "MUSIC_DISC_BLOCKS");
        RENAME_MAP.put("RECORD_4", "MUSIC_DISC_CHIRP");
        RENAME_MAP.put("RECORD_5", "MUSIC_DISC_FAR");
        RENAME_MAP.put("RECORD_6", "MUSIC_DISC_MALL");
        RENAME_MAP.put("RECORD_7", "MUSIC_DISC_MELLOHI");
        RENAME_MAP.put("RECORD_8", "MUSIC_DISC_STAL");
        RENAME_MAP.put("RECORD_9", "MUSIC_DISC_STRAD");
        RENAME_MAP.put("RECORD_10", "MUSIC_DISC_WARD");
        RENAME_MAP.put("RECORD_11", "MUSIC_DISC_11");
        RENAME_MAP.put("RECORD_12", "MUSIC_DISC_WAIT");
    }

    public static Material fromLegacy(Material material) {
        if (material == null || !material.isLegacy()) {
            return material;
        }

        return PatchBukkitLegacy.fromLegacy(new MaterialData(material));
    }

    public static Material fromLegacy(MaterialData materialData) {
        return PatchBukkitLegacy.fromLegacy(materialData, false);
    }

    public static Material fromLegacy(MaterialData materialData, boolean itemPriority) {
        Material material = materialData.getItemType();
        if (material == null || !material.isLegacy()) {
            return material;
        }

        String legacyName = material.name();
        String name = legacyName.startsWith("LEGACY_") ? legacyName.substring(7) : legacyName;
        int data = materialData.getData() & 0xFF;

        // Tier 1: Data-value dependent lookup
        String[] dataMap = DATA_VALUE_MAP.get(name);
        if (dataMap != null) {
            int index = data % dataMap.length;
            Material result = Material.getMaterial(dataMap[index]);
            if (result != null) {
                return result;
            }
        }

        // Tier 2: Simple rename lookup
        String renamed = RENAME_MAP.get(name);
        if (renamed != null) {
            Material result = Material.getMaterial(renamed);
            if (result != null) {
                return result;
            }
        }

        // Tier 3: Direct name match (handles ~275 materials like DIAMOND -> DIAMOND)
        Material direct = Material.getMaterial(name);
        if (direct != null) {
            return direct;
        }

        // Fallback: log warning and return AIR
        LOGGER.warn("Unknown legacy material: {} (data={}), returning AIR", legacyName, data);
        return Material.AIR;
    }
}
