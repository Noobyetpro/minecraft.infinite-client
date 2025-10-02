package org.theinfinitys.mixin.client.rendering;

import net.minecraft.block.BlockState;
import net.minecraft.client.render.BlockRenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.fluid.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.theinfinitys.InfiniteClient;
import org.theinfinitys.features.rendering.XRay;

@Mixin(RenderLayers.class)
public abstract class XRayRenderLayersMixin {
    /**
     * Puts all blocks on the translucent layer if Opacity X-Ray is enabled.
     */
    @Inject(at = @At("HEAD"), method = "getBlockLayer(Lnet/minecraft/block/BlockState;)Lnet/minecraft/client/render/BlockRenderLayer;", cancellable = true)
    private static void onGetBlockLayer(BlockState state, CallbackInfoReturnable<BlockRenderLayer> cir) {
        XRay xray = InfiniteClient.INSTANCE.getFeature(XRay.class);
        if (xray == null || !InfiniteClient.INSTANCE.isFeatureEnabled(XRay.class)) {
            return;
        }

        if (xray.isOpacityMode()) {
            cir.setReturnValue(BlockRenderLayer.TRANSLUCENT);
        }
    }

    /**
     * Puts all fluids on the translucent layer if Opacity X-Ray is enabled.
     */
    @Inject(at = @At("HEAD"), method = "getFluidLayer(Lnet/minecraft/fluid/FluidState;)Lnet/minecraft/client/render/BlockRenderLayer;", cancellable = true)
    private static void onGetFluidLayer(FluidState state, CallbackInfoReturnable<BlockRenderLayer> cir) {
        XRay xray = InfiniteClient.INSTANCE.getFeature(XRay.class);
        if (xray == null || !InfiniteClient.INSTANCE.isFeatureEnabled(XRay.class)) {
            return;
        }

        if (xray.isOpacityMode()) {
            cir.setReturnValue(BlockRenderLayer.TRANSLUCENT);
        }
    }
}
