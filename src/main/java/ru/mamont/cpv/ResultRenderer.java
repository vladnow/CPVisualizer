package ru.mamont.cpv;

import java.util.UUID;

/** Server-thread rendering boundary. A future Fabric transport can implement the same lifecycle. */
public interface ResultRenderer extends AutoCloseable {
    void tick();
    void clear(UUID owner);
    @Override void close();
}
