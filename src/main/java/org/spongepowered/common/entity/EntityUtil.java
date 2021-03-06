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
package org.spongepowered.common.entity;

import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.entity.SpawnEntityEvent;
import org.spongepowered.common.accessor.server.level.ServerPlayerAccessor;
import org.spongepowered.common.accessor.world.entity.LivingEntityAccessor;
import org.spongepowered.common.bridge.CreatorTrackedBridge;
import org.spongepowered.common.bridge.data.VanishableBridge;
import org.spongepowered.common.bridge.world.entity.PlatformEntityBridge;
import org.spongepowered.common.bridge.server.level.ServerPlayerBridge;
import org.spongepowered.common.bridge.world.level.PlatformServerLevelBridge;
import org.spongepowered.common.bridge.server.level.ServerLevelBridge;
import org.spongepowered.common.event.tracking.PhaseContext;
import org.spongepowered.common.hooks.PlatformHooks;
import org.spongepowered.math.vector.Vector3d;

import java.util.Optional;
import java.util.Random;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundChangeDifficultyPacket;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.storage.LevelData;

public final class EntityUtil {

    public static final Function<PhaseContext<?>, Supplier<Optional<User>>> ENTITY_CREATOR_FUNCTION = (context) ->
        () -> Stream.<Supplier<Optional<User>>>builder()
            .add(() -> context.getSource(User.class))
            .add(context::getNotifier)
            .add(context::getCreator)
            .build()
            .map(Supplier::get)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .findFirst();

    private EntityUtil() {
    }

    public static void performPostChangePlayerWorldLogic(final ServerPlayer player, final ServerLevel fromWorld,
            final ServerLevel originalToWorld, final ServerLevel toWorld, final boolean isPortal) {
        // Sponge Start - Send any platform dimension data
        ((ServerPlayerBridge) player).bridge$sendDimensionData(player.connection.connection, toWorld.dimensionType(), toWorld.dimension());
        // Sponge End
        LevelData worldinfo = toWorld.getLevelData();
        // We send dimension change for portals before loading chunks
        if (!isPortal) {
            // Sponge Start - Allow the platform to handle how dimension changes are sent down
            ((ServerPlayerBridge) player).bridge$sendChangeDimension(toWorld.dimensionType(), toWorld.dimension(), BiomeManager.obfuscateSeed(toWorld.getSeed()),
                    player.gameMode.getGameModeForPlayer(), player.gameMode.getPreviousGameModeForPlayer(),
                    toWorld.isDebug(), toWorld.isFlat(), true);
        }
        // Sponge End
        player.connection.send(new ClientboundChangeDifficultyPacket(worldinfo.getDifficulty(), worldinfo.isDifficultyLocked()));
        final PlayerList playerlist = player.getServer().getPlayerList();
        playerlist.sendPlayerPermissionLevel(player);

        // Sponge Start - Have the platform handle removing the entity from the world. Move this to after the event call so
        //                that we do not remove the player from the world unless we really have teleported..
        ((PlatformServerLevelBridge) fromWorld).bridge$removeEntity(player, true);
        ((PlatformEntityBridge) player).bridge$revive();
        // Sponge End

        player.setLevel(toWorld);
        toWorld.addDuringPortalTeleport(player);
        if (isPortal) {
            ((ServerPlayerAccessor) player).invoker$triggerDimensionChangeTriggers(toWorld);
        }
        player.gameMode.setLevel(toWorld);
        player.connection.send(new ClientboundPlayerAbilitiesPacket(player.abilities));
        playerlist.sendLevelInfo(player, toWorld);
        playerlist.sendAllPlayerInfo(player);

        for (final MobEffectInstance effectinstance : player.getActiveEffects()) {
            player.connection.send(new ClientboundUpdateMobEffectPacket(player.getId(), effectinstance));
        }

        if (isPortal) {
            player.connection.send(new ClientboundLevelEventPacket(1032, BlockPos.ZERO, 0, false));
        }

        ((ServerLevelBridge) fromWorld).bridge$getBossBarManager().onPlayerDisconnect(player);
        ((ServerLevelBridge) toWorld).bridge$getBossBarManager().onPlayerDisconnect(player);

        ((ServerPlayerAccessor) player).accessor$lastSentExp(-1);
        ((ServerPlayerAccessor) player).accessor$lastSentHealth(-1.0f);
        ((ServerPlayerAccessor) player).accessor$lastSentFood(-1);

        if (!isPortal) {
            player.connection.teleport(player.getX(), player.getY(), player.getZ(), player.yRot, player.xRot);
            player.connection.resetPosition();
        }

        if (player.containerMenu != player.inventoryMenu) {
            player.closeContainer();
        }

        // Sponge Start - Call platform event hook after changing dimensions
        PlatformHooks.INSTANCE.getEventHooks().callChangeEntityWorldEventPost(player, fromWorld, originalToWorld);
        // Sponge End
    }

    public static boolean isEntityDead(final net.minecraft.world.entity.Entity entity) {
        if (entity instanceof LivingEntity) {
            final LivingEntity base = (LivingEntity) entity;
            return base.getHealth() <= 0 || base.deathTime > 0 || ((LivingEntityAccessor) entity).accessor$dead();
        }
        return entity.removed;
    }

    public static boolean processEntitySpawnsFromEvent(final SpawnEntityEvent event, final Supplier<Optional<User>> entityCreatorSupplier) {
        boolean spawnedAny = false;
        for (final org.spongepowered.api.entity.Entity entity : event.entities()) {
            // Here is where we need to handle the custom items potentially having custom entities
            spawnedAny = EntityUtil.processEntitySpawn(entity, entityCreatorSupplier);
        }
        return spawnedAny;
    }

    public static boolean processEntitySpawnsFromEvent(final PhaseContext<?> context, final SpawnEntityEvent destruct) {
        return EntityUtil.processEntitySpawnsFromEvent(destruct, EntityUtil.ENTITY_CREATOR_FUNCTION.apply(context));
    }

    @SuppressWarnings("ConstantConditions")
    public static boolean processEntitySpawn(final org.spongepowered.api.entity.Entity entity, final Supplier<Optional<User>> supplier) {
        final Entity minecraftEntity = (Entity) entity;
        if (minecraftEntity instanceof ItemEntity) {
            final ItemStack item = ((ItemEntity) minecraftEntity).getItem();
            if (!item.isEmpty()) {
                final Optional<Entity> customEntityItem = Optional.ofNullable(PlatformHooks.INSTANCE.getWorldHooks().getCustomEntityIfItem(minecraftEntity));
                if (customEntityItem.isPresent()) {
                    // Bypass spawning the entity item, since it is established that the custom entity is spawned.
                    final Entity entityToSpawn = customEntityItem.get();
                    supplier.get()
                        .ifPresent(spawned -> {
                            if (entityToSpawn instanceof CreatorTrackedBridge) {
                                ((CreatorTrackedBridge) entityToSpawn).tracked$setCreatorReference(spawned);
                            }
                        });
                    if (entityToSpawn.removed) {
                        entityToSpawn.removed = false;
                    }
                    // Since forge already has a new event thrown for the entity, we don't need to throw
                    // the event anymore as sponge plugins getting the event after forge mods will
                    // have the modified entity list for entities, so no need to re-capture the entities.
                    entityToSpawn.level.addFreshEntity(entityToSpawn);
                    return true;
                }
            }
        }

        supplier.get()
            .ifPresent(spawned -> {
                if (entity instanceof CreatorTrackedBridge) {
                    ((CreatorTrackedBridge) entity).tracked$setCreatorReference(spawned);
                }
            });
        // Allowed to call force spawn directly since we've applied creator and custom item logic already
        ((net.minecraft.world.level.Level) entity.world()).addFreshEntity((Entity) entity);
        return true;
    }

    /**
     * This is used to create the "dropping" motion for items caused by players. This
     * specifically was being used (and should be the correct math) to drop from the
     * player, when we do item stack captures preventing entity items being created.
     *
     * @param dropAround True if it's being "dropped around the player like dying"
     * @param player The player to drop around from
     * @param random The random instance
     * @return The motion vector
     */
    @SuppressWarnings("unused")
    private static Vector3d createDropMotion(final boolean dropAround, final Player player, final Random random) {
        double x;
        double y;
        double z;
        if (dropAround) {
            final float f = random.nextFloat() * 0.5F;
            final float f1 = random.nextFloat() * ((float) Math.PI * 2F);
            x = -Mth.sin(f1) * f;
            z = Mth.cos(f1) * f;
            y = 0.20000000298023224D;
        } else {
            float f2 = 0.3F;
            x = -Mth.sin(player.yRot * 0.017453292F) * Mth.cos(player.xRot * 0.017453292F) * f2;
            z = Mth.cos(player.yRot * 0.017453292F) * Mth.cos(player.xRot * 0.017453292F) * f2;
            y = - Mth.sin(player.xRot * 0.017453292F) * f2 + 0.1F;
            final float f3 = random.nextFloat() * ((float) Math.PI * 2F);
            f2 = 0.02F * random.nextFloat();
            x += Math.cos(f3) * f2;
            y += (random.nextFloat() - random.nextFloat()) * 0.1F;
            z += Math.sin(f3) * f2;
        }
        return new Vector3d(x, y, z);
    }


    public static boolean isUntargetable(Entity from, Entity target) {
        if (((VanishableBridge) target).bridge$isVanished() && ((VanishableBridge) target).bridge$isVanishPreventsTargeting()) {
            return true;
        }
        // Temporary fix for https://bugs.mojang.com/browse/MC-149563
        return from.level != target.level;
    }
}
