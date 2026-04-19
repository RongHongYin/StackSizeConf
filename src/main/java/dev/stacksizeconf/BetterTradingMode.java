package dev.stacksizeconf;

public enum BetterTradingMode {
    /** Vanilla villager trading. */
    OFF,
    /** Sold-out offers restock when the trading GUI is closed and opened again (no work-day wait). */
    QUICK_RESTOCK,
    /** Master-tier villagers keep offers in stock; optional per-click output cap. */
    INFINITE
}
