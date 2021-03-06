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
package org.spongepowered.common.mixin.invalid.core.network.rcon;

import static com.google.common.base.Preconditions.checkNotNull;

import net.minecraft.command.CommandSource;
import net.minecraft.network.rcon.ClientThread;
import net.minecraft.network.rcon.RConConsoleSource;
import net.minecraft.util.text.ITextComponent;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.common.bridge.commands.CommandSourceBridge;
import org.spongepowered.common.bridge.commands.CommandSourceProviderBridge;
import org.spongepowered.common.bridge.network.rcon.RConConsoleSourceBridge;
import org.spongepowered.common.bridge.permissions.SubjectBridge;

import javax.annotation.Nullable;

@Mixin(RConConsoleSource.class)
public abstract class RConConsoleSourceMixin implements CommandSourceBridge, RConConsoleSourceBridge, SubjectBridge, CommandSourceProviderBridge {

    @Shadow @Final private StringBuffer buffer;

    @Shadow public abstract CommandSource getCommandSource();

    @Nullable private ClientThread impl$clientThread;

    @Override
    public void bridge$setConnection(final ClientThread conn) {
        this.impl$clientThread = conn;
    }

    @Override
    public ClientThread bridge$getClient() {
        return checkNotNull(this.impl$clientThread, "RCon Client is null");
    }

    /**
     * Add newlines between output lines for a command
     * @param component text coming in
     */
    @Inject(method = "sendMessage", at = @At("RETURN"))
    private void impl$addNewlines(final ITextComponent component, final CallbackInfo ci) {
        this.buffer.append('\n');
    }

    @Override
    public String bridge$getSubjectCollectionIdentifier() {
        return PermissionService.SUBJECTS_SYSTEM;
    }

    @Override
    public Tristate bridge$permDefault(final String permission) {
        return Tristate.TRUE;
    }

    @Override
    public CommandSource bridge$getCommandSource(Cause cause) {
        return this.getCommandSource();
    }
}
