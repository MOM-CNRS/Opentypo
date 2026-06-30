package fr.cnrs.opentypo.application.import_typology;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TypologyCsvParserTest {

    @Test
    void parse_preservesNewlinesInsideQuotedFields() {
        String csv = """
                code_type;description_fr
                "T1";"ligne 1
                ligne 2
                ligne 3"
                "T2";"une seule ligne"
                """;

        TypologyCsvParser.ParsedCsv parsed = TypologyCsvParser.parse(csv);

        assertEquals(2, parsed.rows().size());
        assertEquals("T1", parsed.rows().get(0).get("code_type"));
        assertEquals("ligne 1\nligne 2\nligne 3", parsed.rows().get(0).get("description_fr"));
        assertEquals("T2", parsed.rows().get(1).get("code_type"));
        assertEquals("une seule ligne", parsed.rows().get(1).get("description_fr"));
    }

    @Test
    void parse_ignoresNewlinesOutsideQuotes() {
        String csv = "code_type;description_fr\n\"A\";\"x\"\n\n\"B\";\"y\"\n";

        TypologyCsvParser.ParsedCsv parsed = TypologyCsvParser.parse(csv);

        assertEquals(2, parsed.rows().size());
        assertEquals("A", parsed.rows().get(0).get("code_type"));
        assertEquals("B", parsed.rows().get(1).get("code_type"));
    }

    @Test
    void parse_handlesEscapedQuotesAndWindowsLineEndings() {
        String csv = "code_type;description_fr\r\n\"T1\";\"dit \"\"hello\"\" ici\"\r\n";

        TypologyCsvParser.ParsedCsv parsed = TypologyCsvParser.parse(csv);

        assertEquals(1, parsed.rows().size());
        assertEquals("dit \"hello\" ici", parsed.rows().get(0).get("description_fr"));
    }

    @Test
    void parse_rejectsFileWithoutDataRows() {
        assertThrows(IllegalArgumentException.class, () -> TypologyCsvParser.parse("code_type;description_fr\n"));
    }
}
