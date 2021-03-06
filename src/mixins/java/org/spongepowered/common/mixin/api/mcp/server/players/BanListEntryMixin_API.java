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
package org.spongepowered.common.mixin.api.mcp.server.players;

import net.kyori.adventure.text.Component;
import net.minecraft.server.players.BanListEntry;
import org.spongepowered.api.service.ban.Ban;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.common.bridge.server.players.BanListEntryBridge;

import java.time.Instant;
import java.util.Date;
import java.util.Optional;

@Mixin(BanListEntry.class)
public abstract class BanListEntryMixin_API<T> extends StoredUserEntryMixin_API<T> implements Ban {

    // @formatter:off
    @Shadow @Final protected Date created;
    @Shadow @Final protected Date expires;
    // @formatter:on

    @Override
    public Optional<Component> reason() {
        return ((BanListEntryBridge) this).bridge$getReason();
    }

    @Override
    public Instant creationDate() {
        return this.created.toInstant();
    }

    @Override
    public Optional<Component> banSource() {
        return ((BanListEntryBridge) this).bridge$getSource();
    }

    @Override
    public Optional<Instant> expirationDate() {
        return Optional.ofNullable(this.expires == null ? null : this.expires.toInstant());
    }
}
