package com.zpkdxgames.plexonutility.command;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimedConfirmationStoreTest {
    @Test void confirmationsAreActorScopedAndOneShot() {
        AtomicLong clock = new AtomicLong(1_000L);
        TimedConfirmationStore<String> store = new TimedConfirmationStore<>(100L, clock::get);
        store.put("player:a", "hostile:64");

        assertTrue(store.consume("player:b").isEmpty());
        assertEquals("hostile:64", store.consume("player:a").orElseThrow());
        assertTrue(store.consume("player:a").isEmpty());
    }

    @Test void expiredConfirmationCannotBeConsumedOrSuggested() {
        AtomicLong clock = new AtomicLong(2_000L);
        TimedConfirmationStore<String> store = new TimedConfirmationStore<>(100L, clock::get);
        store.put("player:a", "mobs:world");
        assertTrue(store.hasValid("player:a"));

        clock.set(2_101L);

        assertFalse(store.hasValid("player:a"));
        assertTrue(store.consume("player:a").isEmpty());
    }

    @Test void newSelectorOrScopeReplacesPreviousPendingAction() {
        AtomicLong clock = new AtomicLong(3_000L);
        TimedConfirmationStore<String> store = new TimedConfirmationStore<>(100L, clock::get);
        store.put("player:a", "hostile:64");
        store.put("player:a", "passive:world");

        assertEquals("passive:world", store.consume("player:a").orElseThrow());
    }

    @Test void explicitClearInvalidatesPendingConfirmation() {
        AtomicLong clock = new AtomicLong(4_000L);
        TimedConfirmationStore<String> store = new TimedConfirmationStore<>(100L, clock::get);
        store.put("console", "all:world");

        store.clear("console");

        assertTrue(store.consume("console").isEmpty());
    }
}
