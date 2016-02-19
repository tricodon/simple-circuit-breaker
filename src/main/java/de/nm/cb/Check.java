package de.nm.cb;

import java.util.function.Supplier;

public class Check {
    private Check() { }

    public static void notNull(Object toCheck, String format, Object... args) {
        if (toCheck == null) {
            throw new NullPointerException(String.format(format, args));
        }
    }
}
