package org.opennms.horizon.shared.lang;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Combined class loader which delegates lookup to dynamic list of class loaders which can change over time.
 */
public class CompoundClassLoader extends ClassLoader {

    private final List<ClassLoader> loaders;

    public CompoundClassLoader(List<ClassLoader> loaders) {
        this.loaders = loaders;
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        for (ClassLoader loader : loaders) {
            try {
                return loader.loadClass(name);
            } catch (ClassNotFoundException e) {
                // skip error
            }
        }
        return super.loadClass(name);
    }

    @Override
    protected Enumeration<URL> findResources(String name) throws IOException {
        final List<Enumeration<URL>> enums = loaders.stream()
                .map(cl -> {
                    try {
                        return cl.getResources(name);
                    } catch (IOException e) {
                    }
                    return null;
                }).filter(Objects::nonNull)
                .collect(Collectors.toList());

        return new CompoundEnumeration<>(enums);
    }

    final class CompoundEnumeration<E> implements Enumeration<E> {
        private final List<Enumeration<E>> enums;
        private int index;

        public CompoundEnumeration(List<Enumeration<E>> enums) {
            this.enums = enums;
        }

        private boolean next() {
            while (index < enums.size()) {
                if (enums.get(index) != null && enums.get(index).hasMoreElements()) {
                    return true;
                }
                index++;
            }
            return false;
        }

        public boolean hasMoreElements() {
            return next();
        }

        public E nextElement() {
            if (!next()) {
                throw new NoSuchElementException();
            }
            return enums.get(index).nextElement();
        }
    }
}
