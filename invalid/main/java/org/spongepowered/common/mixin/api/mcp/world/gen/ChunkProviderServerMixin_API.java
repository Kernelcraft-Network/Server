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
package org.spongepowered.common.mixin.invalid.api.mcp.world.gen;

import net.minecraft.world.chunk.storage.ChunkLoader;
import net.minecraft.world.server.ServerChunkProvider;
import net.minecraft.world.server.ServerWorld;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.world.storage.ChunkDataStream;
import org.spongepowered.api.world.storage.WorldProperties;
import org.spongepowered.api.world.storage.WorldStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.common.bridge.world.level.chunk.storage.AnvilChunkLoaderBridge;
import org.spongepowered.common.world.storage.SpongeChunkDataStream;
import org.spongepowered.common.world.storage.WorldStorageUtil;
import org.spongepowered.math.vector.Vector3i;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Mixin(ServerChunkProvider.class)
public abstract class ChunkProviderServerMixin_API implements WorldStorage {

    @Shadow @Final private ServerWorld world;
    @Shadow @Final private ChunkLoader chunkLoader;

    @Override
    public ChunkDataStream getGeneratedChunks() {
        if (!(this.chunkLoader instanceof AnvilChunkLoaderBridge)) {
            throw new UnsupportedOperationException("unknown chunkLoader");
        }
        return new SpongeChunkDataStream(((AnvilChunkLoaderBridge) this.chunkLoader).bridge$getWorldDir());
    }

    @Override
    public CompletableFuture<Boolean> doesChunkExist(Vector3i chunkCoords) {
        return WorldStorageUtil.doesChunkExist(this.world, this.chunkLoader, chunkCoords);
    }

    @Override
    public CompletableFuture<Optional<DataContainer>> getChunkData(Vector3i chunkCoords) {
        return WorldStorageUtil.getChunkData(this.world, this.chunkLoader, chunkCoords);
    }

    @Override
    public WorldProperties getWorldProperties() {
        return (WorldProperties) this.world.getWorldInfo();
    }


}
