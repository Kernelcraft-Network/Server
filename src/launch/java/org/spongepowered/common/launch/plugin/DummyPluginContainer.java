/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.common.launch.plugin;

import com.google.common.base.MoreObjects;
import org.apache.logging.log4j.Logger;
import org.spongepowered.plugin.PluginContainer;
import org.spongepowered.plugin.metadata.PluginMetadata;

import java.net.URL;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

public final class DummyPluginContainer implements PluginContainer {

    private final PluginMetadata metadata;
    private final Path path;
    private final Logger logger;
    private final Object instance;

    public DummyPluginContainer(final PluginMetadata metadata, final Path path, final Logger logger, final Object instance) {
        this.metadata = metadata;
        this.path = path;
        this.logger = logger;
        this.instance = instance;
    }

    @Override
    public PluginMetadata metadata() {
        return this.metadata;
    }

    @Override
    public Path path() {
        return this.path;
    }

    @Override
    public Logger logger() {
        return this.logger;
    }

    @Override
    public Object instance() {
        return this.instance;
    }

    @Override
    public Optional<URL> locateResource(final URL relative) {
        final ClassLoader classLoader = this.instance().getClass().getClassLoader();
        final URL resolved = classLoader.getResource(relative.getPath());
        return Optional.ofNullable(resolved);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.metadata().id());
    }

    @Override
    public boolean equals(final Object that) {
        if (that == this) {
            return true;
        }

        if (!(that instanceof PluginContainer)) {
            return false;
        }

        return this.metadata().id().equals(((PluginContainer) that).metadata().id());
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .addValue(this.metadata)
            .add("path", this.path)
            .toString();
    }
}
