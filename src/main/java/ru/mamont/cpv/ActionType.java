package ru.mamont.cpv;

public enum ActionType {
    PLACE, BREAK, INTERACT, CONTAINER_ADD, CONTAINER_REMOVE, ALL;
    public boolean blocks() { return this != CONTAINER_ADD && this != CONTAINER_REMOVE; }
    public boolean containers() { return this == ALL || this == CONTAINER_ADD || this == CONTAINER_REMOVE; }
}
