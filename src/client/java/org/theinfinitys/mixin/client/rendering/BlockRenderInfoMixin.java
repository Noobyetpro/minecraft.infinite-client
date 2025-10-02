package org.theinfinitys.mixin.client.rendering;

import net.fabricmc.fabric.impl.client.indigo.renderer.render.BlockRenderInfo;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.theinfinitys.InfiniteClient;
import org.theinfinitys.features.rendering.XRay;
import org.theinfinitys.settings.InfiniteSetting;

import java.util.List;

@Mixin(value = BlockRenderInfo.class, remap = false)
public abstract class BlockRenderInfoMixin {

    @Shadow
    public BlockPos blockPos;
    @Shadow
    public BlockState blockState;

    @Inject(at = @At("HEAD"), method = "shouldDrawSide", cancellable = true)
    private void onShouldDrawSide(Direction face, CallbackInfoReturnable<Boolean> cir) {

        // faceがnullの場合はオリジナルの処理に任せる
        if (face == null) {
            return;
        }

        if (!InfiniteClient.INSTANCE.isFeatureEnabled(XRay.class)) {
            return;
        }

        XRay xRayFeature = InfiniteClient.INSTANCE.getFeature(XRay.class);
        if (xRayFeature == null) {
            return;
        }

        var blockListSetting = xRayFeature.getSetting("BlockList");
        if (!(blockListSetting instanceof InfiniteSetting.BlockListSetting)) {
            return;
        }

        @SuppressWarnings("unchecked") List<String> xRayBlockList = ((InfiniteSetting.BlockListSetting) blockListSetting).getValue();
        String currentBlockId = blockState.getBlock().getRegistryEntry().registryKey().getValue().toString();
        boolean isCurrentBlockXRay = xRayBlockList.contains(currentBlockId);
        if (isCurrentBlockXRay) {
            cir.setReturnValue(false);
        } else {
            // 現在のブロックが非XRayブロック（石、土など）の場合

            ClientWorld world = MinecraftClient.getInstance().world;
            if (world == null) {
                return;
            }

            BlockPos adjacentPos = blockPos.offset(face);
            BlockState adjacentState = world.getBlockState(adjacentPos);

            String adjacentBlockId = adjacentState.getBlock().getRegistryEntry().registryKey().getValue().toString();
            boolean isAdjacentBlockXRay = xRayBlockList.contains(adjacentBlockId);

            if (isAdjacentBlockXRay) {
                // 隣がXRayブロック、または空気の場合、面を描画する (false = 非表示にしない)
                // 流体の場合も、ここで描画を許可する
                cir.setReturnValue(true);
            } else {
                // ★★★ 重要な修正: 隣接ブロックも非XRayブロックの場合 ★★★
                // 隣が非XRayブロックなら、その面は非表示にする (true = 非表示にする)
                // これにより、石の中に埋まった石の面が描画されなくなり、負荷が軽減される
                cir.setReturnValue(false);
            }
            return;
        }
    }
}