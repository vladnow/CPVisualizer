package io.github.cpvisualizer;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TimeParserTest {
    @Test void parsesPresetsAndCompoundDurations() {
        assertEquals(900,TimeParser.seconds("15m"));
        assertEquals(21600,TimeParser.seconds("6H"));
        assertEquals(108000,TimeParser.seconds("1d6h"));
        assertEquals(2592000,TimeParser.seconds("30d"));
    }
    @Test void rejectsUnboundedMalformedAndOverflowingInput() {
        for(String input:new String[]{"","0s","-1h","1hfoo","foo1h","1 h","1","999999999999999999999999d","2147483648s","9999999999d"})
            assertThrows(IllegalArgumentException.class,()->TimeParser.seconds(input),input);
    }
}
