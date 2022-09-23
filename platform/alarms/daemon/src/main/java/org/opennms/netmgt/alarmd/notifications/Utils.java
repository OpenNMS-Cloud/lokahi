package org.opennms.netmgt.alarmd.notifications;

import java.util.Objects;
import java.util.function.Supplier;

public class Utils {
    // HACK: When defining key.deserializer/value.deserializer classes, the kafka client library
    // tries to instantiate them by using the ClassLoader returned by Thread.currentThread().getContextClassLoader() if defined.
    // As that ClassLoader does not know anything about that classes a ClassNotFoundException is thrown
    // By setting the ClassLoader to null, the BundleContextClassLoader of the kafka client library is used instead,
    // which can instantiate those classes more likely (depending on Import/DynamicImport-Package definitions)
    public static <T> T runWithNullContextClassLoader(final Supplier<T> supplier) {
        return runWithGivenClassLoader(supplier, null);
    }

    public static <T> T runWithGivenClassLoader(final Supplier<T> supplier, ClassLoader classLoader) {
        Objects.requireNonNull(supplier);
        final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(classLoader);
            return supplier.get();
        } finally {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
        }
    }
}
