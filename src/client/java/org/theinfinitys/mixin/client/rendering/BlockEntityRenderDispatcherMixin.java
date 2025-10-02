package org.theinfinitys.mixin.client.rendering;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.theinfinitys.InfiniteClient;
import org.theinfinitys.features.rendering.XRay;
import org.theinfinitys.settings.InfiniteSetting;

import java.util.List;

@Mixin(BlockEntityRenderDispatcher.class)
public class BlockEntityRenderDispatcherMixin {

    /**
     * Renders block entities (like chests, spawners, beacons).
     * This is used to prevent the rendering of block entities that are not on the XRay list,
     * ensuring only XRay targets are visible.
     */
    @Inject(
            at = @At("HEAD"),
            method = "render(Lnet/minecraft/block/entity/BlockEntity;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;)V",
            cancellable = true
    )
    private <E extends BlockEntity> void onRender(E blockEntity,
                                                  float tickDelta, MatrixStack matrices,
                                                  VertexConsumerProvider vertexConsumers, CallbackInfo ci) {
        // XRay featureが無効な場合は何もしない
        if (!InfiniteClient.INSTANCE.isFeatureEnabled(XRay.class)) {
            return;
        }

        XRay xRayFeature = InfiniteClient.INSTANCE.getFeature(XRay.class);
        if (xRayFeature == null) {
            return;
        }

        // --- ブロックリストのチェック ---
        InfiniteSetting<?> blockListSetting = xRayFeature.getSetting("BlockList");
        if (!(blockListSetting instanceof InfiniteSetting.BlockListSetting)) {
            return;
        }

        @SuppressWarnings("unchecked")
        List<String> xRayBlockList = ((InfiniteSetting.BlockListSetting) blockListSetting).getValue();

        // 現在のブロックエンティティが持つブロックのIDを取得
        // BlockEntity.getCachedState() は Mixin環境では利用可能
        String blockId = blockEntity.getCachedState().getBlock().getRegistryEntry().registryKey().getValue().toString();

        // ブロックエンティティのブロックがXRayリストに含まれているかどうかを確認
        boolean isBlockEntityXRay = xRayBlockList.contains(blockId);

        // XRayリストに含まれていないブロックエンティティは描画をキャンセルする (非表示にする)
        if (!isBlockEntityXRay) {
            ci.cancel();
        }
    }
}