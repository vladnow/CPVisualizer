package ru.mamont.cpv;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FilterSelectionTest {
    @Test void allSelectorIsExplicitAndCaseInsensitive() {
        assertTrue(FilterSelection.isAll("all"));assertTrue(FilterSelection.isAll("ALL"));assertTrue(FilterSelection.isAll("*"));
        assertFalse(FilterSelection.isAll("Alex"));assertFalse(FilterSelection.isAll("tall"));assertFalse(FilterSelection.isAll(null));
    }
    @Test void allPlayersExcludesSystemActorsAndMissingNames() {
        assertTrue(FilterSelection.isPlayerActor("Alex"));assertTrue(FilterSelection.isPlayerActor(".BedrockUser"));
        assertFalse(FilterSelection.isPlayerActor("#fire"));assertFalse(FilterSelection.isPlayerActor("#hopper"));
        assertFalse(FilterSelection.isPlayerActor(""));assertFalse(FilterSelection.isPlayerActor(null));
    }
}
