package com.ibarnstormer.balefireplugin.interactions;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.spatial.SpatialResource;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.server.core.asset.type.particle.config.WorldParticle;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.entity.*;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;
import com.ibarnstormer.balefireplugin.BalefirePlugin;
import it.unimi.dsi.fastutil.objects.ObjectList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class BalefireBlastInteraction extends SimpleInstantInteraction {

    protected int numExplosions = 10;
    protected double explosionSeparation = 2;
    @Nullable
    protected ExplosionConfig explosionConfig;
    @Nullable
    protected WorldParticle explosionParticles;
    protected transient int explosionSoundIdx;
    @Nullable
    protected String explosionSoundId;

    @Nonnull
    public static final BuilderCodec<BalefireBlastInteraction> CODEC = (((((((BuilderCodec.builder(BalefireBlastInteraction.class, BalefireBlastInteraction::new, SimpleInstantInteraction.CODEC).documentation("Interaction for the Balefire Spell"))
            .appendInherited(
                    new KeyedCodec<>("NumExplosions", Codec.INTEGER),
                    (interaction, i) -> interaction.numExplosions = i,
                    (interaction) -> interaction.numExplosions,
                    (interaction, parent) -> interaction.numExplosions = parent.numExplosions)
            .documentation("Number of explosions to create.").add())
            .appendInherited(
                    new KeyedCodec<>("ExplosionSeparation", Codec.DOUBLE),
                    (interaction, d) -> interaction.explosionSeparation = d,
                    (interaction) -> interaction.explosionSeparation,
                    (interaction, parent) -> interaction.explosionSeparation = parent.explosionSeparation)
            .documentation("Distance between each explosion.").add())
            .appendInherited(
                    new KeyedCodec<>("ExplosionConfig", ExplosionConfig.CODEC),
                    (interaction, cfg) -> interaction.explosionConfig = cfg,
                    (interaction) -> interaction.explosionConfig,
                    (interaction, parent) -> interaction.explosionConfig = parent.explosionConfig)
            .documentation("Explosion Config.").add())
            .appendInherited(
                    new KeyedCodec<>("ExplosionParticles", WorldParticle.CODEC),
                    (interaction, p) -> interaction.explosionParticles = p,
                    (interaction) -> interaction.explosionParticles,
                    (interaction, parent) -> interaction.explosionParticles = parent.explosionParticles)
            .documentation("Explosion Particles to use.").add())
            .appendInherited(
                    new KeyedCodec<>("ExplosionSoundId", Codec.STRING),
                    (interaction, id) -> interaction.explosionSoundId = id,
                    (interaction) -> interaction.explosionSoundId,
                    (interaction, parent) -> interaction.explosionSoundId = parent.explosionSoundId)
            .documentation("Id for the explosion sound.").add())
            .afterDecode(BalefireBlastInteraction::postInit))
            .build();


    public BalefireBlastInteraction() {

    }

    protected void postInit() {
        if(explosionSoundId != null) {
            explosionSoundIdx = SoundEvent.getAssetMap().getIndex(explosionSoundId);
        }
    }

    @Override
    protected void firstRun(@Nonnull InteractionType interactionType, @Nonnull InteractionContext interactionContext, @Nonnull CooldownHandler cooldownHandler) {
        CommandBuffer<EntityStore> commandBuffer = interactionContext.getCommandBuffer();

        assert commandBuffer != null;

        World world = commandBuffer.getExternalData().getWorld();
        Ref<EntityStore> attackerRef = interactionContext.getEntity();

        Transform lookVec = TargetUtil.getLook(attackerRef, commandBuffer);

        Vector3d lookPos = lookVec.getPosition();
        Vector3d lookDir = lookVec.getDirection().normalize();
        Vector3d castOrigin = lookPos.addScaled(lookDir, 2);

        for(int i = 0; i < numExplosions; i++) {
            Vector3d pos = castOrigin.addScaled(lookDir, explosionSeparation);
            createExplosion(commandBuffer, attackerRef, world, pos);
        }
    }

    protected void simulateFirstRun(@Nonnull InteractionType type, @Nonnull InteractionContext context, @Nonnull CooldownHandler cooldownHandler) {
    }

    private void createExplosion(CommandBuffer<EntityStore> commandBuffer, Ref<EntityStore> attackerRef, World world, Vector3d pos) {
        if(explosionConfig != null) {
            ExplosionUtils.performExplosion(new Damage.EntitySource(attackerRef), pos, explosionConfig, attackerRef, commandBuffer, world.getChunkStore().getStore());

            if(explosionParticles != null) {
                SpatialResource<Ref<EntityStore>, EntityStore> playerSpatialResource = commandBuffer.getResource(EntityModule.get().getPlayerSpatialResourceType());
                ObjectList<Ref<EntityStore>> results = SpatialResource.getThreadLocalReferenceList();
                playerSpatialResource.getSpatialStructure().collect(pos, 75.0, results);
                ParticleUtil.spawnParticleEffect(explosionParticles, pos, results, commandBuffer);
            }

            if(explosionSoundIdx != 0) {
                SoundUtil.playSoundEvent3d(explosionSoundIdx, SoundCategory.SFX, pos, commandBuffer);
            }
        }
        else {
            BalefirePlugin.getInstance().getLogger().atWarning().log(String.format("Could not create balefire explosion at %f, %f, %f due to missing explosion config", pos.x, pos.y, pos.z));
        }
    }
}
