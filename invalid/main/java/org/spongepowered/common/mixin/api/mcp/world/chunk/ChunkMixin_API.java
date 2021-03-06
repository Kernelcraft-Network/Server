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
package org.spongepowered.common.mixin.invalid.api.mcp.world.chunk;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Predicate;
import com.google.common.collect.Sets;
import net.minecraft.server.management.PlayerChunkMapEntry;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ClassInheritanceMultiMap;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.Difficulty;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.provider.BiomeProvider;
import net.minecraft.world.server.ServerWorld;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.ScheduledBlockUpdate;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntitySnapshot;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.util.AABB;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.util.PositionOutOfBoundsException;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.api.world.BlockChangeFlag;
import org.spongepowered.api.world.biome.BiomeType;
import org.spongepowered.api.world.chunk.Chunk;
import org.spongepowered.api.world.extent.Extent;
import org.spongepowered.api.world.volume.biome.workerMutableBiomeVolumeStream;
import org.spongepowered.api.world.volume.block.worker.MutableBlockVolumeStream;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.bridge.server.players.PlayerChunkMapEntryBridge;
import org.spongepowered.common.bridge.world.level.chunk.ChunkBridge;
import org.spongepowered.common.util.Constants;
import org.spongepowered.common.util.VecHelper;
import org.spongepowered.common.world.SpongeBlockChangeFlag;
import org.spongepowered.common.world.extent.ExtentViewDownsize;
import org.spongepowered.common.world.extent.worker.SpongeMutableBiomeVolumeWorker;
import org.spongepowered.common.world.extent.worker.SpongeMutableBlockVolumeWorker;
import org.spongepowered.common.world.storage.SpongeChunkLayout;
import org.spongepowered.math.vector.Vector2d;
import org.spongepowered.math.vector.Vector3d;
import org.spongepowered.math.vector.Vector3i;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

@SuppressWarnings("BoundedWildcard")
@Mixin(net.minecraft.world.chunk.Chunk.class)
public abstract class ChunkMixin_API implements Chunk {

    @Shadow @Final private World world;
    @Shadow @Final public int x;
    @Shadow @Final public int z;
    @Shadow @Final private ClassInheritanceMultiMap<net.minecraft.entity.Entity>[] entityLists;
    @Shadow @Final private Map<BlockPos, TileEntity> tileEntities;
    @Shadow private long inhabitedTime;
    @Shadow private boolean loaded;
    @Shadow public boolean unloadQueued;

    // @formatter:off
    @Shadow @Nullable public abstract TileEntity getTileEntity(BlockPos pos, net.minecraft.world.chunk.Chunk.CreateEntityType p_177424_2_);
    @Shadow public abstract net.minecraft.block.BlockState getBlockState(BlockPos pos);
    @Shadow public abstract net.minecraft.block.BlockState getBlockState(int x, int y, int z);
    @Shadow public abstract Biome getBiome(BlockPos pos, BiomeProvider chunkManager);
    @Shadow public abstract byte[] getBiomeArray();
    @Shadow public abstract void setBiomeArray(byte[] biomeArray);
    @Shadow public abstract <T extends net.minecraft.entity.Entity> void getEntitiesOfTypeWithinAABB(Class <? extends T > entityClass,
        AxisAlignedBB aabb, List<T> listToFill, Predicate <? super T > p_177430_4_);
    @Shadow public abstract BlockPos getPrecipitationHeight(BlockPos pos);
    // @formatter:on

    @Shadow public abstract ChunkPos getPos();

    @Nullable private Vector3i api$chunkPos;
    @Nullable private Vector3i api$blockMin;
    @Nullable private Vector3i api$blockMax;
    @Nullable private Vector3i api$biomeMin;
    @Nullable private Vector3i api$biomeMax;
    @Nullable private UUID api$uuid;

    @Override
    public UUID getUniqueId() {
        if (this.api$uuid == null) {
            @Nullable final UUID uuid = ((org.spongepowered.api.world.World) this.world).getUniqueId();
            if (uuid != null) {
                this.api$uuid = new UUID(uuid.getMostSignificantBits() ^ (this.x * 2 + 1), uuid.getLeastSignificantBits() ^ this.z * 2 + 1);
            }
            this.api$uuid = UUID.randomUUID();
        }
        return this.api$uuid;
    }

    @Override
    public Vector3i getPosition() {
        if (this.api$chunkPos == null) {
            this.api$chunkPos = new Vector3i(this.x, 0, this.z);
        }
        return this.api$chunkPos;
    }

    @Override
    public boolean isLoaded() {
        return this.loaded;
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public boolean loadChunk(final boolean generate) {
        final ServerWorld worldserver = (ServerWorld) this.world;
        if (!generate) {
            return worldserver.getChunkProvider().loadChunk(this.x, this.z) != null;
        }
        return worldserver.getChunkProvider().provideChunk(this.x, this.z) != null;
    }

    @Override
    public boolean unloadChunk() {
        if (((ChunkBridge) this).bridge$isPersistedChunk()) {
            return false;
        }
        // Spawn point checks occur in queueUnload() and are reflected in
        // this.unloadQueued.
        ((ServerWorld) this.world).getChunkProvider().queueUnload((net.minecraft.world.chunk.Chunk) (Object) this);
        return this.unloadQueued;
    }

    @Override
    public int getInhabittedTime() {
        return (int) this.inhabitedTime;
    }

    @Override
    public int getInhabitedTime() {
        return (int) this.inhabitedTime;
    }

    @Override
    public double getRegionalDifficultyFactor() {
        final boolean flag = this.world.getDifficulty() == Difficulty.HARD;
        final float moon = this.world.getCurrentMoonPhaseFactor();
        final float f2 = MathHelper.clamp((this.world.getDayTime() - 72000.0F) / 1440000.0F, 0.0F, 1.0F) * 0.25F;
        float f3 = 0.0F;
        f3 += MathHelper.clamp(this.inhabitedTime / 3600000.0F, 0.0F, 1.0F) * (flag ? 1.0F : 0.75F);
        f3 += MathHelper.clamp(moon * 0.25F, 0.0F, f2);
        return f3;
    }

    @Override
    public double getRegionalDifficultyPercentage() {
        final double region = this.getRegionalDifficultyFactor();
        if (region < 2) {
            return 0;
        }
        if (region > 4) {
            return 1.0;
        }
        return (region - 2.0) / 2.0;
    }

    @Override
    public org.spongepowered.api.world.World getWorld() {
        return (org.spongepowered.api.world.World) this.world;
    }

    @Override
    public BiomeType getBiome(final int x, final int y, final int z) {
        this.checkBiomeBounds(x, y, z);
        return (BiomeType) this.getBiome(new BlockPos(x, y, z), this.world.getBiomeProvider());
    }

    @Override
    public void setBiome(final int x, final int y, final int z, final BiomeType biome) {
        this.checkBiomeBounds(x, y, z);
        // Taken from Chunk#getBiome
        final byte[] biomeArray = this.getBiomeArray();
        final int i = x & 15;
        final int j = z & 15;
        biomeArray[j << 4 | i] = (byte) (Biome.getIdForBiome((Biome) biome) & 255);
        this.setBiomeArray(biomeArray);

        if (this.world instanceof ServerWorld) {
            final PlayerChunkMapEntry entry = ((ServerWorld) this.world).getPlayerChunkMap().getEntry(this.x, this.z);
            if (entry != null) {
                ((PlayerChunkMapEntryBridge) entry).bridge$markBiomesForUpdate();
            }
        }
    }

    @Override
    public BlockState getBlock(final int x, final int y, final int z) {
        this.checkBlockBounds(x, y, z);
        return (BlockState) this.getBlockState(new BlockPos(x, y, z));
    }

    @Override
    public boolean setBlock(final int x, final int y, final int z, final BlockState block) {
        this.checkBlockBounds(x, y, z);
        return this.world.setBlockState(new BlockPos(x, y, z), (net.minecraft.block.BlockState) block, Constants.BlockChangeFlags.ALL);
    }

    @Override
    public boolean setBlock(final int x, final int y, final int z, final BlockState block, final BlockChangeFlag flag) {
        this.checkBlockBounds(x, y, z);
        return this.world.setBlockState(new BlockPos(x, y, z), (net.minecraft.block.BlockState) block, ((SpongeBlockChangeFlag) flag).getRawFlag());
    }

    @Override
    public BlockType getBlockType(final int x, final int y, final int z) {
        this.checkBlockBounds(x, y, z);
        return (BlockType) this.getBlockState(x, y, z).getBlock();
    }

    @Override
    public BlockSnapshot createSnapshot(final int x, final int y, final int z) {
        return ((org.spongepowered.api.world.World) this.world).createSnapshot((this.x << 4) + (x & 15), y, (this.z << 4) + (z & 15));
    }

    @Override
    public boolean restoreSnapshot(final BlockSnapshot snapshot, final boolean force, final BlockChangeFlag flag) {
        return ((org.spongepowered.api.world.World) this.world).restoreSnapshot(snapshot, force, flag);
    }

    @Override
    public boolean restoreSnapshot(final int x, final int y, final int z, final BlockSnapshot snapshot, final boolean force, final BlockChangeFlag flag) {
        return ((org.spongepowered.api.world.World) this.world).restoreSnapshot((this.x << 4) + (x & 15), y, (this.z << 4) + (z & 15), snapshot, force, flag);
    }

    @Override
    public int getHighestYAt(final int x, final int z) {
        return ((org.spongepowered.api.world.World) this.world).getHighestYAt((this.x << 4) + (x & 15), (this.z << 4) + (z & 15));
    }

    @Override
    public int getPrecipitationLevelAt(final int x, final int z) {
        return this.getPrecipitationHeight(new BlockPos(x, 0, z)).getY();
    }

    @Override
    public Vector3i getBiomeMin() {
        if (this.api$biomeMin == null) {
            this.api$biomeMin = new Vector3i(this.getBlockMin().getX(), 0, this.getBlockMin().getZ());
        }
        return this.api$biomeMin;
    }

    @Override
    public Vector3i getBiomeMax() {
        if (this.api$biomeMax == null) {
            this.api$biomeMax = new Vector3i(this.getBlockMax().getX(), 0, this.getBlockMax().getZ());
        }
        return this.api$biomeMax;
    }

    @Override
    public Vector3i getBiomeSize() {
        return Constants.Chunk.BIOME_SIZE;
    }

    @Override
    public Vector3i getBlockMin() {
        if (this.api$blockMin == null) {
            this.api$blockMin = SpongeChunkLayout.instance.forceToWorld(this.getPosition());
        }
        return this.api$blockMin;
    }

    @Override
    public Vector3i getBlockMax() {
        if (this.api$blockMax == null) {
            this.api$blockMax = this.getBlockMin().add(SpongeChunkLayout.CHUNK_SIZE).sub(1, 1, 1);
        }
        return this.api$blockMax;
    }

    @Override
    public Vector3i getBlockSize() {
        return SpongeChunkLayout.CHUNK_SIZE;
    }

    @Override
    public boolean containsBiome(final int x, final int y, final int z) {
        return VecHelper.inBounds(x, y, z, this.getBiomeMin(), this.getBiomeMax());
    }

    @Override
    public boolean containsBlock(final int x, final int y, final int z) {
        return VecHelper.inBounds(x, y, z, this.getBlockMin(), this.getBlockMax());
    }

    private void checkBiomeBounds(final int x, final int y, final int z) {
        if (!this.containsBiome(x, y, z)) {
            throw new PositionOutOfBoundsException(new Vector3i(x, y, z), this.getBiomeMin(), this.getBiomeMax());
        }
    }

    private void checkBlockBounds(final int x, final int y, final int z) {
        if (!this.containsBlock(x, y, z)) {
            throw new PositionOutOfBoundsException(new Vector3i(x, y, z), this.getBlockMin(), this.getBlockMax());
        }
    }

    @Override
    public Extent getExtentView(final Vector3i newMin, final Vector3i newMax) {
        this.checkBlockBounds(newMin.getX(), newMin.getY(), newMin.getZ());
        this.checkBlockBounds(newMax.getX(), newMax.getY(), newMax.getZ());
        return new ExtentViewDownsize(this, newMin, newMax);
    }

    @Override
    public workerMutableBiomeVolumeStream<Chunk> getBiomeWorker() {
        return new SpongeMutableBiomeVolumeWorker<>(this);
    }

    @Override
    public MutableBlockVolumeStream<Chunk> getBlockWorker() {
        return new SpongeMutableBlockVolumeWorker<>(this);
    }

    @Override
    public Entity createEntity(final EntityType type, final Vector3d position)
            throws IllegalArgumentException, IllegalStateException {
        return ((org.spongepowered.api.world.World) this.world).createEntity(type, this.getPosition().mul(16).toDouble().add(position.min(15, this.getBlockMax().getY(), 15)));
    }

    @Override
    public Optional<Entity> createEntity(final DataContainer entityContainer) {
        return ((org.spongepowered.api.world.World) this.world).createEntity(entityContainer);
    }

    @Override
    public Optional<Entity> createEntity(final DataContainer entityContainer, final Vector3d position) {
        final Vector3d min = position.min(15, this.getBlockMax().getY(), 15);
        return ((org.spongepowered.api.world.World) this.world)
            .createEntity(entityContainer, this.getPosition()
                .mul(16)
                .toDouble()
                .add(min));
    }

    @Override
    public boolean spawnEntity(final Entity entity) {
        return ((org.spongepowered.api.world.World) this.world).spawnEntity(entity);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public Collection<Entity> getEntities() {
        final Set<Entity> entities = Sets.newHashSet();
        for (final ClassInheritanceMultiMap entityList : this.entityLists) {
            entities.addAll(entityList);
        }
        return entities;
    }

    @Override
    public Collection<Entity> getEntities(final java.util.function.Predicate<Entity> filter) {
        final Set<Entity> entities = Sets.newHashSet();
        for (final ClassInheritanceMultiMap<net.minecraft.entity.Entity> entityClassMap : this.entityLists) {
            for (final Object entity : entityClassMap) {
                if (filter.test((Entity) entity)) {
                    entities.add((Entity) entity);
                }
            }
        }
        return entities;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public Collection<org.spongepowered.api.block.entity.BlockEntity> getTileEntities() {
        return Sets.newHashSet((Collection) this.tileEntities.values());
    }

    @Override
    public Collection<org.spongepowered.api.block.entity.BlockEntity> getTileEntities(final java.util.function.Predicate<org.spongepowered.api.block.entity.BlockEntity> filter) {
        final Set<org.spongepowered.api.block.entity.BlockEntity> tiles = Sets.newHashSet();
        for (final Map.Entry<BlockPos, TileEntity> entry : this.tileEntities.entrySet()) {
            if (filter.test((org.spongepowered.api.block.entity.BlockEntity) entry.getValue())) {
                tiles.add((org.spongepowered.api.block.entity.BlockEntity) entry.getValue());
            }
        }
        return tiles;
    }

    @Override
    public Optional<org.spongepowered.api.block.entity.BlockEntity> getTileEntity(final int x, final int y, final int z) {
        return Optional.ofNullable((org.spongepowered.api.block.entity.BlockEntity) this.getTileEntity(
                new BlockPos((this.x << 4) + (x & 15), y, (this.z << 4) + (z & 15)), net.minecraft.world.chunk.Chunk.CreateEntityType.CHECK));
    }

    @Override
    public Optional<Entity> restoreSnapshot(final EntitySnapshot snapshot, final Vector3d position) {
        return ((org.spongepowered.api.world.World) this.world).restoreSnapshot(snapshot, position);
    }

    @Override
    public Collection<ScheduledBlockUpdate> getScheduledUpdates(final int x, final int y, final int z) {
        return ((org.spongepowered.api.world.World) this.world).getScheduledUpdates((this.x << 4) + (x & 15), y, (this.z << 4) + (z & 15));
    }

    @Override
    public ScheduledBlockUpdate addScheduledUpdate(final int x, final int y, final int z, final int priority, final int ticks) {
        return ((org.spongepowered.api.world.World) this.world).addScheduledUpdate((this.x << 4) + (x & 15), y, (this.z << 4) + (z & 15), priority, ticks);
    }

    @Override
    public void removeScheduledUpdate(final int x, final int y, final int z, final ScheduledBlockUpdate update) {
        ((org.spongepowered.api.world.World) this.world).removeScheduledUpdate((this.x << 4) + (x & 15), y, (this.z << 4) + (z & 15), update);
    }

    @Override
    public boolean hitBlock(final int x, final int y, final int z, final Direction side, final GameProfile profile) {
        return ((org.spongepowered.api.world.World) this.world).hitBlock((this.x << 4) + (x & 15), y, (this.z << 4) + (z & 15), side, profile);
    }

    @Override
    public boolean interactBlock(final int x, final int y, final int z, final Direction side, final GameProfile profile) {
        return ((org.spongepowered.api.world.World) this.world).interactBlock((this.x << 4) + (x & 15), y, (this.z << 4) + (z & 15), side, profile);
    }

    @Override
    public boolean placeBlock(final int x, final int y, final int z, final BlockState block, final Direction side, final GameProfile profile) {
        return ((org.spongepowered.api.world.World) this.world).placeBlock((this.x << 4) + (x & 15), y, (this.z << 4) + (z & 15), block, side, profile);
    }

    @Override
    public boolean interactBlockWith(final int x, final int y, final int z, final ItemStack itemStack, final Direction side, final GameProfile profile) {
        return ((org.spongepowered.api.world.World) this.world).interactBlockWith((this.x << 4) + (x & 15), y, (this.z << 4) + (z & 15), itemStack, side, profile);
    }

    @Override
    public boolean digBlock(final int x, final int y, final int z, final GameProfile profile) {
        return ((org.spongepowered.api.world.World) this.world).digBlock((this.x << 4) + (x & 15), y, (this.z << 4) + (z & 15), profile);
    }

    @Override
    public boolean digBlockWith(final int x, final int y, final int z, final ItemStack itemStack, final GameProfile profile) {
        return ((org.spongepowered.api.world.World) this.world).digBlockWith((this.x << 4) + (x & 15), y, (this.z << 4) + (z & 15), itemStack, profile);
    }

    @Override
    public int getBlockDigTimeWith(final int x, final int y, final int z, final ItemStack itemStack, final GameProfile profile) {
        return ((org.spongepowered.api.world.World) this.world).getBlockDigTimeWith((this.x << 4) + (x & 15), y, (this.z << 4) + (z & 15), itemStack, profile);
    }

    @Override
    public Optional<AABB> getBlockSelectionBox(final int x, final int y, final int z) {
        this.checkBlockBounds(x, y, z);
        return ((org.spongepowered.api.world.World) this.world).getBlockSelectionBox((this.x << 4) + (x & 15), y, (this.z << 4) + (z & 15));
    }

    @Override
    public Set<Entity> getIntersectingEntities(final AABB box,
            final java.util.function.Predicate<Entity> filter) {
        checkNotNull(box, "box");
        checkNotNull(filter, "filter");
        final List<net.minecraft.entity.Entity> entities = new ArrayList<>();
        this.getEntitiesOfTypeWithinAABB(net.minecraft.entity.Entity.class, VecHelper.toMinecraftAABB(box), entities,
            entity -> filter.test((Entity) entity));
        return entities.stream().map(entity -> (Entity) entity).collect(Collectors.toSet());
    }

    @Override
    public Set<AABB> getIntersectingBlockCollisionBoxes(final AABB box) {
        final Vector3i max = this.getBlockMax().add(Vector3i.ONE);
        return ((org.spongepowered.api.world.World) this.world).getIntersectingBlockCollisionBoxes(box).stream()
                .filter(aabb -> VecHelper.inBounds(aabb.getCenter(), this.getBlockMin(), max))
                .collect(Collectors.toSet());
    }

    @Override
    public Set<AABB> getIntersectingCollisionBoxes(final Entity owner, final AABB box) {
        final Vector3i max = this.getBlockMax().add(Vector3i.ONE);
        return ((org.spongepowered.api.world.World) this.world).getIntersectingCollisionBoxes(owner, box).stream()
                .filter(aabb -> VecHelper.inBounds(aabb.getCenter(), this.getBlockMin(), max))
                .collect(Collectors.toSet());
    }

    @Override
    public Set<org.spongepowered.api.world.volume.entity.ReadableEntityVolume.EntityHit> getIntersectingEntities(final Vector3d start, final Vector3d end, final java.util.function.Predicate<org.spongepowered.api.world.volume.entity.ReadableEntityVolume.EntityHit> filter) {
        checkNotNull(start, "start");
        checkNotNull(end, "end");
        checkNotNull(filter, "filter");
        final Vector3d diff = end.sub(start);
        return this.api$getIntersectingEntities(start, end, diff.normalize(), diff.length(), filter);
    }

    @Override
    public Set<org.spongepowered.api.world.volume.entity.ReadableEntityVolume.EntityHit> getIntersectingEntities(final Vector3d start, Vector3d direction, final double distance,
            final java.util.function.Predicate<org.spongepowered.api.world.volume.entity.ReadableEntityVolume.EntityHit> filter) {
        checkNotNull(start, "start");
        checkNotNull(direction, "direction");
        checkNotNull(filter, "filter");
        direction = direction.normalize();
        return this.api$getIntersectingEntities(start, start.add(direction.mul(distance)), direction, distance, filter);
    }

    private Set<org.spongepowered.api.world.volume.entity.ReadableEntityVolume.EntityHit> api$getIntersectingEntities(final Vector3d start, final Vector3d end, final Vector3d direction, final double distance,
            final java.util.function.Predicate<? super org.spongepowered.api.world.volume.entity.ReadableEntityVolume.EntityHit> filter) {
        final Vector2d entryAndExitY = this.getEntryAndExitY(start, end, direction, distance);
        if (entryAndExitY == null) {
            // Doesn't intersect the chunk, ignore it
            return Collections.emptySet();
        }
        final Set<org.spongepowered.api.world.volume.entity.ReadableEntityVolume.EntityHit> intersections = new HashSet<>();
        ((ChunkBridge) this).bridge$getIntersectingEntities(start, direction, distance, filter, entryAndExitY.getX(), entryAndExitY.getY(), intersections);
        return intersections;
    }

    @Nullable
    private Vector2d getEntryAndExitY(final Vector3d start, final Vector3d end, final Vector3d direction, final double distance) {
        // Modified from AABB.intersects(ray)
        // Increase the bounds to the whole chunk plus a margin of two blocks
        final Vector3i min = this.getBlockMin().sub(2, 2, 2);
        final Vector3i max = this.getBlockMax().add(3, 3, 3);
        // Find the intersections on the -x and +x planes, oriented by direction
        final double txMin;
        final double txMax;
        if (Math.copySign(1, direction.getX()) > 0) {
            txMin = (min.getX() - start.getX()) / direction.getX();
            txMax = (max.getX() - start.getX()) / direction.getX();
        } else {
            txMin = (max.getX() - start.getX()) / direction.getX();
            txMax = (min.getX() - start.getX()) / direction.getX();
        }
        // Find the intersections on the -z and +z planes, oriented by direction
        final double tzMin;
        final double tzMax;
        if (Math.copySign(1, direction.getZ()) > 0) {
            tzMin = (min.getZ() - start.getZ()) / direction.getZ();
            tzMax = (max.getZ() - start.getZ()) / direction.getZ();
        } else {
            tzMin = (max.getZ() - start.getZ()) / direction.getZ();
            tzMax = (min.getZ() - start.getZ()) / direction.getZ();
        }
        // The ray should intersect the -x plane before the +z plane and intersect
        // the -z plane before the +x plane, else it is outside the column
        if (txMin > tzMax || txMax < tzMin) {
            return null;
        }
        // The ray intersects only the furthest min plane on the column and only the closest
        // max plane on the column
        final double tMin = Math.max(tzMin, txMin);
        final double tMax = Math.min(tzMax, txMax);
        // If both intersection points are behind the start, there are no intersections
        if (tMax < 0) {
            return null;
        }
        // If the closest intersection is before the start, use the start y instead
        final double yEntry = tMin < 0 ? start.getY() : direction.getY() * tMin + start.getY();
        // If the furthest intersection is after the end, use the end y instead
        final double yExit = tMax > distance ? end.getY() : direction.getY() * tMax + start.getY();
        //noinspection SuspiciousNameCombination
        return new Vector2d(yEntry, yExit);
    }

    @Override
    public Optional<Chunk> getNeighbor(final Direction direction, final boolean shouldLoad) {
        checkNotNull(direction, "direction");
        checkArgument(!direction.isSecondaryOrdinal(), "Secondary cardinal directions can't be used here");

        if (direction.isUpright() || direction == Direction.NONE) {
            return Optional.of(this);
        }

        final int index = SpongeCommon.directionToIndex(direction);
        final Direction secondary = SpongeCommon.getSecondaryDirection(direction);
        Chunk neighbor = null;
        neighbor = (Chunk) ((ChunkBridge) this).bridge$getNeighborArray()[index];

        if (neighbor == null && shouldLoad) {
            final Vector3i neighborPosition = this.getPosition().add(SpongeCommon.getCardinalDirection(direction).asBlockOffset());
            final Optional<Chunk> cardinal = this.getWorld().loadChunk(neighborPosition, true);
            if (cardinal.isPresent()) {
                neighbor = cardinal.get();
            }
        }

        if (neighbor != null && secondary != Direction.NONE) {
            return neighbor.getNeighbor(secondary, shouldLoad);
        }

        return Optional.ofNullable(neighbor);
    }


}
