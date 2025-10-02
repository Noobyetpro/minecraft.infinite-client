package org.theinfinitys.mixin.client.rendering;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.theinfinitys.InfiniteClient;
import org.theinfinitys.features.rendering.XRay;

/**
 * Last updated for <a href=
 * "https://github.com/CaffeineMC/sodium/tree/02253db283e4679228ba5fbc30cfc851d17123c8">Sodium
 * 0.6.13+mc1.21.6</a>
 */
@Pseudo
@Mixin(targets = {
        "net.caffeinemc.mods.sodium.client.render.frapi.render.AbstractBlockRenderContext"})
public class XRaySodiumAbstractBlockRenderContextMixin {
    @Shadow
    protected BlockState state;

    @Shadow
    protected BlockPos pos;

    /**
     * Hides and shows blocks when using X-Ray with Sodium installed.
     */
    @Inject(at = @At("HEAD"), method = "isFaceCulled(Lnet/minecraft/util/math/Direction;)Z", cancellable = true, require = 0)
    private void onIsFaceCulled(@Nullable Direction face, CallbackInfoReturnable<Boolean> cir) {
        XRay xray = InfiniteClient.INSTANCE.getFeature(XRay.class);
        if (xray == null || !InfiniteClient.INSTANCE.isFeatureEnabled(XRay.class)) {
            return;
        }

        Boolean shouldDraw = xray.shouldDrawSide(state, pos);
        if (shouldDraw != null) {
            cir.setReturnValue(!shouldDraw);
        }
    }
}
