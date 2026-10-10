package io.github.cpvisualizer;

import java.util.List;

/** Conservative historical inference, never a read of today's world blocks. */
public final class HistoryResolver {
    public record Evidence(long time, int action, String material, boolean rolledBack) { }
    private HistoryResolver() { }
    public static String materialAt(List<Evidence> rows, long eventTime) {
        long latest = Long.MIN_VALUE;
        String found = null;
        boolean uncertain = false;
        for (Evidence row : rows) {
            if (row.time > eventTime) continue;
            // CoreProtect timestamps have second precision: placement/break ordering is ambiguous.
            if (row.time == eventTime && row.action != 2) return null;
            if (row.time > latest) { latest = row.time; found = null; uncertain = false; }
            if (row.time != latest) continue;
            if (row.rolledBack || row.action == 0 || row.material == null) uncertain = true;
            else if (found != null && !found.equals(row.material)) uncertain = true;
            else found = row.material;
        }
        return uncertain ? null : found;
    }
}
