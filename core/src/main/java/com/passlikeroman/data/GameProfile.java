package com.passlikeroman.data;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

/** Persistent career, cosmetic and stadium choices shared by the screens. */
public final class GameProfile {
    public static final String[] CLUBS = {
        "BOCA JUNIORS", "RIVER PLATE", "RACING CLUB", "INDEPENDIENTE", "SAN LORENZO",
        "ROSARIO CENTRAL", "ESTUDIANTES", "NEWELL'S OLD BOYS",
        "ARGENTINOS JUNIORS", "VELEZ SARSFIELD", "HURACAN", "GIMNASIA", "NEUTRO"
    };
    public static final int[] COSTS = {0, 180, 240, 300, 360, 420, 480, 540, 600, 660, 720, 780, 840};
    private static final int[] RECEPTION_LEVELS = {11, 12, 9, 8, 10, 6, 8, 4, 3, 7, 5, 2, 1};
    private static final int[] PASS_LEVELS = {12, 11, 9, 10, 7, 5, 8, 6, 4, 7, 2, 3, 1};
    public static final String[] STADIUMS = {"LA BOMBONERA", "EL MONUMENTAL", "CILINDRO", "LIBERTADORES", "NUEVO GASOMETRO"};
    public static final int[] STADIUM_COSTS = {0, 250, 320, 400, 480};
    private static final Preferences DATA = Gdx.app.getPreferences("pass-like-roman-profile");
    private static int coins = DATA.getInteger("coins", 0);
    private static int kit = DATA.getInteger("kit", 0);
    private static int stadium = DATA.getInteger("stadium", 0);
    private static final boolean[] OWNED_KITS = loadOwnership("kitOwned_", DATA.getInteger("unlockedKits", 1), CLUBS.length);
    private static final boolean[] OWNED_STADIUMS = loadOwnership("stadiumOwned_", DATA.getInteger("unlockedStadiums", 1), STADIUMS.length);

    private GameProfile() {}
    public static int coins() { return coins; }
    public static int kit() { return kit; }
    public static int stadium() { return stadium; }
    public static int unlockedKits() { return countOwned(OWNED_KITS); }
    public static int unlockedStadiums() { return countOwned(OWNED_STADIUMS); }
    public static boolean isKitOwned(int index) { return index >= 0 && index < OWNED_KITS.length && OWNED_KITS[index]; }
    public static boolean isStadiumOwned(int index) { return index >= 0 && index < OWNED_STADIUMS.length && OWNED_STADIUMS[index]; }
    public static int receptionLevel() { return receptionLevel(kit); }
    public static int receptionLevel(int kitIndex) {
        return RECEPTION_LEVELS[clampKitIndex(kitIndex)];
    }
    public static int passLevel() { return passLevel(kit); }
    public static int passLevel(int kitIndex) {
        return PASS_LEVELS[clampKitIndex(kitIndex)];
    }
    public static float receptionRating() { return ratingForLevel(receptionLevel()); }
    public static float passPrecision() { return ratingForLevel(passLevel()); }

    private static int clampKitIndex(int kitIndex) {
        return Math.max(0, Math.min(kitIndex, CLUBS.length - 1));
    }

    private static float ratingForLevel(int level) {
        return 40f + (level - 1) * (60f / 11f);
    }
    public static void addCoins(int amount) { coins += amount; save(); }
    public static boolean buyOrSelectKit(int index) {
        if (index < 0 || index >= CLUBS.length) return false;
        if (!OWNED_KITS[index]) {
            if (coins < COSTS[index]) return false;
            coins -= COSTS[index];
            OWNED_KITS[index] = true;
        }
        kit = index; save(); return true;
    }
    public static boolean buyOrSelectStadium(int index) {
        if (index < 0 || index >= STADIUMS.length) return false;
        if (!OWNED_STADIUMS[index]) {
            if (coins < STADIUM_COSTS[index]) return false;
            coins -= STADIUM_COSTS[index];
            OWNED_STADIUMS[index] = true;
        }
        stadium = index; save(); return true;
    }

    private static boolean[] loadOwnership(String keyPrefix, int legacyUnlocked, int itemCount) {
        boolean[] owned = new boolean[itemCount];
        int legacyCount = Math.max(0, Math.min(legacyUnlocked, itemCount));
        for (int i = 0; i < itemCount; i++) {
            owned[i] = DATA.contains(keyPrefix + i) ? DATA.getBoolean(keyPrefix + i) : i < legacyCount;
        }
        return owned;
    }

    private static int countOwned(boolean[] owned) {
        int count = 0;
        for (boolean item : owned) if (item) count++;
        return count;
    }

    private static void save() {
        DATA.putInteger("coins", coins).putInteger("kit", kit).putInteger("stadium", stadium)
            .putInteger("unlockedKits", countOwned(OWNED_KITS))
            .putInteger("unlockedStadiums", countOwned(OWNED_STADIUMS));
        for (int i = 0; i < OWNED_KITS.length; i++) DATA.putBoolean("kitOwned_" + i, OWNED_KITS[i]);
        for (int i = 0; i < OWNED_STADIUMS.length; i++) DATA.putBoolean("stadiumOwned_" + i, OWNED_STADIUMS[i]);
        DATA.flush();
    }
}
