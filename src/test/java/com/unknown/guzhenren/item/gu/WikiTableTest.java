package com.unknown.guzhenren.item.gu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Adversarial fixtures for the read-only wiki [设定库] table reader.
 */

class WikiTableTest {

    private static final String TABLE = "| Gu | Cost |\n|---|---:|\n| Boar | 3,600 |\n";
    @Test
    void readsNumbersAndReportsTheirSourceLine() {
        WikiTable.Row row = WikiTable.parse("items.md", TABLE).row("Gu", "Boar");
        row.expect("Cost", List.of(3600L));
        assertEquals("items.md:3", row.location());
        WikiTable.parse("items.md", TABLE).expectMatch("\\| Boar \\| ([0-9,]+)", List.of(3600L));
    }
    @Test
    void changedNumberFailsAtItsSourceLine() {
        AssertionError error = assertThrows(AssertionError.class,
                () -> WikiTable.parse("items.md", TABLE).row("Gu", "Boar").expect("Cost", List.of(800L)));
        assertTrue(error.getMessage().contains("items.md:3"));
        assertTrue(error.getMessage().contains("800"));
        AssertionError inline = assertThrows(AssertionError.class,
                () -> WikiTable.parse("items.md", "prefix 800\n").expectMatch("(800)", List.of(1L)));
        assertTrue(inline.getMessage().contains("items.md:1"));
    }
    @Test
    void missingRowFailsInsteadOfSkipping() {
        assertThrows(AssertionError.class, () -> WikiTable.parse("items.md", TABLE).row("Gu", "Bear"));
    }
    @Test
    void duplicateRowFailsInsteadOfPickingTheFirst() {
        assertThrows(AssertionError.class,
                () -> WikiTable.parse("items.md", TABLE + "| Boar | 800 |\n").row("Gu", "Boar"));
    }
    @Test
    void malformedNumberFailsInsteadOfExtractingItsDigits() {
        assertThrows(AssertionError.class, () -> WikiTable.parse("items.md", TABLE.replace("3,600", "about 3600"))
                .row("Gu", "Boar").expect("Cost", List.of(3600L)));
    }
    @Test
    void malformedColumnCountFails() {
        assertThrows(AssertionError.class,
                () -> WikiTable.parse("items.md", TABLE.replace("3,600 |", "3,600 | extra |")));
    }
    @Test
    void acceptsExplicitListsAndChineseRankRanges() {
        WikiTable.Row row = WikiTable.parse("items.md", TABLE.replace("3,600", "一..五")).row("Gu", "Boar");
        row.expect("Cost", List.of(1L, 2L, 3L, 4L, 5L));
        WikiTable.parse("items.md", TABLE.replace("3,600", "16 / 160 / 1,600"))
                .row("Gu", "Boar").expect("Cost", List.of(16L, 160L, 1600L));
    }
    @Test
    void wikiAliasesDoNotSplitTableCells() {
        WikiTable.Row row = WikiTable.parse("items.md", TABLE.replace("Boar", "[[boar|Boar]]"))
                .row("Gu", "[[boar|Boar]]");
        row.expect("Cost", List.of(3600L));
    }
    @Test
    void missingColumnFailsWithRowLocation() {
        AssertionError error = assertThrows(AssertionError.class,
                () -> WikiTable.parse("items.md", TABLE).row("Gu", "Boar").cell("Hunger"));
        assertTrue(error.getMessage().contains("items.md:3"));
    }
    @Test
    void missingAndDuplicateItemMappingsFail() {
        assertThrows(AssertionError.class,
                () -> WikiTable.requireCoverage(List.of("boar"), List.of("boar", "bear")));
        assertThrows(AssertionError.class,
                () -> WikiTable.requireCoverage(List.of("boar", "boar"), List.of("boar")));
        WikiTable.requireCoverage(List.of("bear", "boar"), List.of("boar", "bear"));

        String table = "| Kind | Cost |\n|---|---:|\n| boar | 3,600 |\n| bear | 800 |\n";
        WikiTable parsed = WikiTable.parse("items.md", table);
        parsed.requireExactRows(List.of("Kind", "Cost"), List.of("boar", "bear"));
        AssertionError stale = assertThrows(AssertionError.class,
                () -> parsed.requireExactRows(List.of("Kind", "Cost"), List.of("boar")));
        assertTrue(stale.getMessage().contains("items.md:4"));
        AssertionError duplicate = assertThrows(AssertionError.class,
                () -> WikiTable.parse("items.md", table + "| boar | 3,600 |\n")
                        .requireExactRows(List.of("Kind", "Cost"), List.of("boar", "bear")));
        assertTrue(duplicate.getMessage().contains("items.md:5"));
        AssertionError missing = assertThrows(AssertionError.class,
                () -> parsed.requireExactRows(List.of("Kind", "Cost"), List.of("boar", "wolf")));
        assertTrue(missing.getMessage().contains("items.md:1"));

        String sameFirstHeader = "| Kind | Cost |\n|---|---:|\n| boar | 3,600 |\n\n"
                + "| Kind | Cooldown |\n|---|---:|\n| ghost | 1 |\n";
        WikiTable.parse("items.md", sameFirstHeader)
                .requireExactRows(List.of("Kind", "Cost"), List.of("boar"));
    }
}
