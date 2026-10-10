package io.github.cpvisualizer;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import io.github.cpvisualizer.HistoryResolver.Evidence;

class HistoryResolverTest {
    @Test void findsDestroyedHistoricalContainerWithoutReadingPresentBlock() {
        assertEquals("CHEST",HistoryResolver.materialAt(List.of(new Evidence(4000,0,"CHEST",false),new Evidence(1000,1,"CHEST",false)),3000));
    }
    @Test void detectsReplacement() {
        var rows=List.of(new Evidence(3000,1,"BARREL",false),new Evidence(2000,0,"CHEST",false),new Evidence(1000,1,"CHEST",false));
        assertEquals("BARREL",HistoryResolver.materialAt(rows,4000));
        assertNull(HistoryResolver.materialAt(rows,2500));
    }
    @Test void refusesMissingAndAmbiguousEvidence() {
        assertNull(HistoryResolver.materialAt(List.of(),1000));
        assertNull(HistoryResolver.materialAt(List.of(new Evidence(1000,1,"CHEST",false)),1000));
        assertNull(HistoryResolver.materialAt(List.of(new Evidence(1000,2,"CHEST",true)),2000));
        assertNull(HistoryResolver.materialAt(List.of(new Evidence(1000,2,"CHEST",false),new Evidence(1000,2,"BARREL",false)),2000));
    }
    @Test void interactionConfirmsMaterialAtSameSecond() {
        assertEquals("CHEST",HistoryResolver.materialAt(List.of(new Evidence(1000,2,"CHEST",false)),1000));
    }
}
