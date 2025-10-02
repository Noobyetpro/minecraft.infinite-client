package org.theinfinitys.mixin.client;

import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.theinfinitys.InfiniteClient;
import org.theinfinitys.features.movement.Freeze;

@Mixin(ClientPlayerEntity.class)
public class FreezePlayerMoveMixin {

  @Inject(method = "sendMovementPackets", at = @At("HEAD"), cancellable = true)
  private void onSendMovementPackets(CallbackInfo ci) {
    if (InfiniteClient.INSTANCE.isFeatureEnabled(Freeze.class)) {
      ci.cancel();
    }
  }
}
