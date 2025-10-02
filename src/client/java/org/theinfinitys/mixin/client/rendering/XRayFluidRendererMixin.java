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
import org.spongepowered.asm.mixin.injection.At;
import org.theinfinitys.InfiniteClient;
import org.theinfinitys.features.rendering.XRay;

@Mixin(FluidRenderer.class)
public class XRayFluidRendererMixin {

  /** Hides and shows fluids when using X-Ray without Sodium installed. */
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
      return original.call(side, height, neighborState);
    }

    Boolean shouldDraw = xray.shouldDrawSide(blockState, pos);
    if (shouldDraw != null) {
      return !shouldDraw;
    }

    return original.call(side, height, neighborState);
  }
}
