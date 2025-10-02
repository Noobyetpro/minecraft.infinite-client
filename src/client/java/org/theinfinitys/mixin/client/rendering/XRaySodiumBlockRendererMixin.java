package org.theinfinitys.mixin.client.rendering;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.theinfinitys.InfiniteClient;
import org.theinfinitys.features.rendering.XRay;

/**
 * Last updated for <a href=
 * "https://github.com/CaffeineMC/sodium/tree/02253db283e4679228ba5fbc30cfc851d17123c8">Sodium
 * 0.6.13+mc1.21.6</a>
 */
@Pseudo
@Mixin(targets = {
        "net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer"},
        remap = false)
public class XRaySodiumBlockRendererMixin {
    @Shadow
    protected BlockState state;

    @Shadow
    protected BlockPos pos;

    /**
     * Modifies opacity of blocks when using X-Ray with Sodium installed.
     */
    @ModifyExpressionValue(at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/render/frapi/mesh/MutableQuadViewImpl;color(I)I"),
            method = "bufferQuad(Lnet/caffeinemc/mods/sodium/client/render/frapi/mesh/MutableQuadViewImpl;[FLnet/caffeinemc/mods/sodium/client/render/chunk/terrain/material/Material;)V",
            require = 0)
    private int onBufferQuad(int original) {
        XRay xray = InfiniteClient.INSTANCE.getFeature(XRay.class);
        if (xray == null || !InfiniteClient.INSTANCE.isFeatureEnabled(XRay.class)) {
            return original;
        }

        if (!xray.isOpacityMode() || xray.isVisible(state.getBlock(), pos)) {
            return original;
        }

        return original & xray.getOpacityColorMask();
    }
}
