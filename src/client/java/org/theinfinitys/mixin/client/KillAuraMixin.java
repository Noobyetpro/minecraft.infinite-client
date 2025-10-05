package org.theinfinitys.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.MathHelper;
import java.util.stream.StreamSupport;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.theinfinitys.InfiniteClient;
import org.theinfinitys.features.fighting.KillAura;
import org.theinfinitys.features.fighting.NoAttack;
import org.theinfinitys.features.fighting.PlayerManager;
import org.theinfinitys.settings.InfiniteSetting;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Mixin(ClientPlayerEntity.class)
public class KillAuraMixin {
    private int attackCooldown = 0;

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(CallbackInfo ci) {
        KillAura killAuraFeature = InfiniteClient.INSTANCE.getFeature(KillAura.class);
        if (killAuraFeature == null || !killAuraFeature.isEnabled()) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;

        if (client.world == null || client.interactionManager == null || player.isDead()) {
            return;
        }

        float range = ((InfiniteSetting.FloatSetting) killAuraFeature.getSetting("Range")).getValue();
        int attackDelaySetting = ((InfiniteSetting.IntSetting) killAuraFeature.getSetting("AttackDelay")).getValue();
        boolean targetPlayers = ((InfiniteSetting.BooleanSetting) killAuraFeature.getSetting("Players")).getValue();
        boolean targetMobs = ((InfiniteSetting.BooleanSetting) killAuraFeature.getSetting("Mobs")).getValue();
        int maxTargets = ((InfiniteSetting.IntSetting) killAuraFeature.getSetting("MaxTargets")).getValue();
        int attackFrequency = ((InfiniteSetting.IntSetting) killAuraFeature.getSetting("AttackFrequency")).getValue();
        boolean changeAngle = ((InfiniteSetting.BooleanSetting) killAuraFeature.getSetting("ChangeAngle")).getValue();

        // Calculate actual attack delay based on AttackFrequency setting
        int actualAttackDelay;
        if (attackFrequency == 0) {
            // Auto-adjust based on weapon cooldown
            actualAttackDelay = (int) (20.0 / (player.getAttackCooldownProgress(0.5f) * 20.0)); // Approximate
            if (actualAttackDelay == 0) actualAttackDelay = 1; // Prevent division by zero or too fast attacks
        } else {
            actualAttackDelay = attackFrequency;
        }

        if (attackCooldown > 0) {
            attackCooldown--;
            return;
        }


// ... other imports ...

        List<Entity> targets = StreamSupport.stream(client.world.getEntities().spliterator(), false).filter(entity -> entity != player && entity.isAlive() && player.distanceTo(entity) <= range).filter(entity -> {
            if (entity instanceof PlayerEntity) {
                return targetPlayers && !isFriendlyPlayer((PlayerEntity) entity);
            } else if (entity instanceof LivingEntity) {
                return targetMobs && !isProtectedEntity(entity);
            }
            return false;
        }).sorted(Comparator.comparingDouble(player::distanceTo)).limit(maxTargets == 0 ? Long.MAX_VALUE : maxTargets).collect(Collectors.toList());

        for (Entity target : targets) {
            if (changeAngle) {
                faceEntity(player, target);
            }
            client.interactionManager.attackEntity(player, target);
            attackCooldown = actualAttackDelay;
            // Only attack one target per tick if attackDelaySetting is not 0
            if (attackDelaySetting != 0) {
                break;
            }
        }
    }

    private boolean isFriendlyPlayer(PlayerEntity targetPlayer) {
        PlayerManager playerManagerFeature = InfiniteClient.INSTANCE.getFeature(PlayerManager.class);
        if (playerManagerFeature != null && playerManagerFeature.isEnabled()) {
            InfiniteSetting.PlayerListSetting friendsSetting = (InfiniteSetting.PlayerListSetting) playerManagerFeature.getSetting("Friends");
            if (friendsSetting != null) {
                return friendsSetting.getValue().contains(targetPlayer.getName().getString());
            }
        }
        return false;
    }

    private boolean isProtectedEntity(Entity targetEntity) {
        NoAttack noAttackFeature = InfiniteClient.INSTANCE.getFeature(NoAttack.class);
        if (noAttackFeature != null && noAttackFeature.isEnabled()) {
            InfiniteSetting.EntityListSetting protectedEntitiesSetting = (InfiniteSetting.EntityListSetting) noAttackFeature.getSetting("ProtectedEntities");
            if (protectedEntitiesSetting != null) {
                String targetEntityId = Registries.ENTITY_TYPE.getId(targetEntity.getType()).toString();
                return protectedEntitiesSetting.getValue().contains(targetEntityId);
            }
        }
        return false;
    }

    private void faceEntity(ClientPlayerEntity player, Entity target) {
        double x = target.getX() - player.getX();
        double y = target.getY() - (player.getY() + player.getEyeHeight(player.getPose()));
        double z = target.getZ() - player.getZ();

        double dist = Math.sqrt(x * x + z * z);
        float yaw = (float) (MathHelper.atan2(z, x) * 180.0 / Math.PI) - 90.0F;
        float pitch = (float) -(MathHelper.atan2(y, dist) * 180.0 / Math.PI);

        player.setYaw(yaw);
        player.setPitch(pitch);
    }
}
