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
package org.spongepowered.common.event;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import io.leangen.geantyref.TypeToken;
import org.spongepowered.api.event.EventContextKey;
import org.spongepowered.common.util.AbstractResourceKeyedBuilder;
import org.spongepowered.common.util.TypeTokenUtil;

import java.lang.reflect.Type;

import javax.annotation.Nullable;

public final class SpongeEventContextKeyBuilder<T> extends AbstractResourceKeyedBuilder<EventContextKey<T>, EventContextKey.Builder<T>> implements EventContextKey.Builder<T> {

    @Nullable Type typeClass;

    @SuppressWarnings("unchecked")
    @Override
    public <N> SpongeEventContextKeyBuilder<N> type(final TypeToken<N> aClass) {
        checkArgument(aClass != null, "Class cannot be null!");
        this.typeClass = aClass.getType();
        return (SpongeEventContextKeyBuilder<N>) this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <N> SpongeEventContextKeyBuilder<N> type(final Class<N> aClass) {
        checkArgument(aClass != null, "Class cannot be null!");
        this.typeClass = TypeTokenUtil.requireCompleteGenerics(aClass);
        return (SpongeEventContextKeyBuilder<N>) this;
    }

    @Override
    public EventContextKey<T> build0() {
        checkState(this.typeClass != null, "Allowed type cannot be null!");
        return new SpongeEventContextKey<>(this);
    }

    @Override
    public SpongeEventContextKeyBuilder<T> reset() {
        this.typeClass = null;
        return this;
    }
}
