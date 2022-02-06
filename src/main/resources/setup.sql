CREATE TABLE IF NOT EXISTS players
(
    uuid            VARCHAR(64) NOT NULL PRIMARY KEY,

    air             INTEGER,
    fire            INTEGER,
    glowing         BOOLEAN,
    health          DOUBLE,
    absorption      DOUBLE,

    active_effects  TEXT,
    recipe_book     TEXT,

    food_level      INTEGER,
    food_exhaustion DOUBLE,
    food_saturation DOUBLE,

    xp_level        INTEGER,
    xp_percentage   DOUBLE,
    xp_total        INTEGER,

    inventory       TEXT,
    ender_chest     TEXT,
    armor_items     TEXT,
    selected_slot   INTEGER,

    last_event      VARCHAR(16)
);
