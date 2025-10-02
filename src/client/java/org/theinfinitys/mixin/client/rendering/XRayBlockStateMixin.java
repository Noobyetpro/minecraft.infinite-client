package org.theinfinitys.mixin.client.rendering;

import net.minecraft.block.AbstractBlock; // Import AbstractBlock
import net.minecraft.block.BlockState;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.theinfinitys.InfiniteClient;
import org.theinfinitys.features.rendering.XRay;
import org.theinfinitys.settings.InfiniteSetting;

@Mixin(AbstractBlock.AbstractBlockState.class) // Corrected Mixin target
public abstract class XRayBlockStateMixin {

    @Inject(method = "isSideInvisible(Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/Direction;)Z", at = @At("HEAD"), cancellable = true)
    private void onIsSideInvisible(BlockState adjacentBlockState, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (InfiniteClient.INSTANCE.isFeatureEnabled(XRay.class)) {
            XRay xRay = InfiniteClient.INSTANCE.getFeature(XRay.class);
            if (xRay != null) {
                InfiniteSetting.BlockListSetting blockListSetting = (InfiniteSetting.BlockListSetting) xRay.getSetting("BlockList");
                if (blockListSetting != null) {
                    // 'this' refers to the AbstractBlockState instance on which isSideInvisible is called
                    // We want to check if 'this' block is in the XRay list
                    if (blockListSetting.getValue().contains(((BlockState)(Object)this).getBlock().getRegistryEntry().registryKey().getValue().toString())) {
                        cir.setReturnValue(false); // If it's an XRay block, it's never invisible from any side
                    } else {
                        // If it's not an XRay block, make it invisible (transparent)
                        cir.setReturnValue(true);
                    }
                }
            }
        }
    }
}