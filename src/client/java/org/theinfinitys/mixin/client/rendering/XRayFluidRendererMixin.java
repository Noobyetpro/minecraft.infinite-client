package org.theinfinitys.mixin.client.rendering;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.block.FluidRenderer;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockRenderView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.theinfinitys.InfiniteClient;
import org.theinfinitys.features.rendering.XRay;

@Mixin(FluidRenderer.class)
public class XRayFluidRendererMixin {
    @Unique
    private static final ThreadLocal<Float> currentOpacity = ThreadLocal.withInitial(() -> 1F);

    /**
     * Hides and shows fluids when using X-Ray without Sodium installed.
     */
    @WrapOperation(
            at =
            @At(
                    value = "INVOKE",
                    target =
                            "Lnet/minecraft/client/render/block/FluidRenderer;shouldSkipRendering(Lnet/minecraft/util/math/Direction;FLnet/minecraft/block/BlockState;)Z"),
            method =
                    "render(Lnet/minecraft/world/BlockRenderView;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/client/render/VertexConsumer;Lnet/minecraft/block/BlockState;Lnet/minecraft/fluid/FluidState;)V")
    private boolean modifyShouldSkipRendering(
            Direction side,
            float height,
            BlockState neighborState,
            Operation<Boolean> original,
            BlockRenderView world,
            BlockPos pos,
            VertexConsumer vertexConsumer,
            BlockState blockState,
            FluidState fluidState) {
        XRay xray = InfiniteClient.INSTANCE.getFeature(XRay.class);
        if (xray == null || !InfiniteClient.INSTANCE.isFeatureEnabled(XRay.class)) {
            currentOpacity.set(1F);
            return original.call(side, height, neighborState);
        }

        Boolean shouldDraw = xray.shouldDrawSide(blockState, pos);
        if (shouldDraw != null) {
            currentOpacity.set(shouldDraw ? 1F : xray.getOpacityFloat());
            return !shouldDraw;
        }

        currentOpacity.set(1F);
        return original.call(side, height, neighborState);
    }

    /**
     * Modifies opacity of fluids when using X-Ray without Sodium installed.
     */
    @ModifyConstant(
            method = "vertex(Lnet/minecraft/client/render/VertexConsumer;FFFFFFFFI)V",
            constant = @Constant(floatValue = 1F, ordinal = 0))
    private float modifyOpacity(float original) {
        return currentOpacity.get();
    }
}
