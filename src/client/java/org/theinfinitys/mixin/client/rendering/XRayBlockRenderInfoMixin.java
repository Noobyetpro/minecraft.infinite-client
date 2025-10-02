package org.theinfinitys.mixin.client.rendering;

import net.fabricmc.fabric.impl.client.indigo.renderer.render.BlockRenderInfo;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.theinfinitys.InfiniteClient;
import org.theinfinitys.features.rendering.XRay;

@Pseudo
@Mixin(value = BlockRenderInfo.class, remap = false)
public abstract class XRayBlockRenderInfoMixin {
    @Shadow
    public BlockPos blockPos;
    @Shadow
    public BlockState blockState;

    /**
     * This mixin hides and shows regular blocks when using X-Ray, if Indigo
     * is running and Sodium is not installed.
     */
    @Inject(at = @At("HEAD"), method = "shouldDrawSide", cancellable = true)
    private void onShouldDrawSide(Direction face, CallbackInfoReturnable<Boolean> cir) {
        XRay xray = InfiniteClient.INSTANCE.getFeature(XRay.class);
        if (xray == null || !InfiniteClient.INSTANCE.isFeatureEnabled(XRay.class)) {
            return;
        }

        Boolean shouldDraw = xray.shouldDrawSide(blockState, blockPos);
        if (shouldDraw != null) {
            cir.setReturnValue(shouldDraw);
        }
    }
}
