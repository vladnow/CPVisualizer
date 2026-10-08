package ru.mamont.cpv;

public final class FilterSelection {
    private FilterSelection() { }
    public static boolean isAll(String value) { return "all".equalsIgnoreCase(value) || "*".equals(value); }
    public static boolean isPlayerActor(String name) { return name != null && !name.isBlank() && !name.startsWith("#"); }
}
