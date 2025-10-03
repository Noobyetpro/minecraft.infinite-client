package org.theinfinitys.mixin.client;

import net.minecraft.client.network.ClientPlayerInteractionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.theinfinitys.InfiniteClient;
import org.theinfinitys.features.movement.FastBreak;

@Mixin(ClientPlayerInteractionManager.class)
public abstract class FastBreakMixin {

  @Inject(method = "tick", at = @At("TAIL"))
  private void resetBlockBreakingCooldown(CallbackInfo ci) {
    if (InfiniteClient.INSTANCE.isFeatureEnabled(FastBreak.class)) {
      ((ClientPlayerInteractionManagerAccessor) this).setBlockBreakingCooldown(0);
    }
  }
}
