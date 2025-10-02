package org.theinfinitys.mixin.client.rendering;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.block.BlockState;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockRenderView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
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
        "net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.DefaultFluidRenderer"})
public class XRaySodiumDefaultFluidRendererMixin {
    /**
     * Hides and shows fluids when using X-Ray with Sodium installed.
     */
    @Inject(at = @At("HEAD"),
            method = "isFullBlockFluidOccluded(Lnet/minecraft/world/BlockRenderView;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/math/Direction;Lnet/minecraft/block/BlockState;Lnet/minecraft/fluid/FluidState;)Z",
            cancellable = true,
            require = 0)
    private void onIsFullBlockFluidOccluded(BlockRenderView world, BlockPos pos,
                                            Direction dir, BlockState state, FluidState fluid,
                                            CallbackInfoReturnable<Boolean> cir) {
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
