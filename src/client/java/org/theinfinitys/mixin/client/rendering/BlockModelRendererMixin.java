package org.theinfinitys.mixin.client.rendering;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.block.BlockModelRenderer;
import net.minecraft.item.ItemConvertible;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockRenderView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.theinfinitys.InfiniteClient;
import org.theinfinitys.features.rendering.XRay;
import org.theinfinitys.settings.InfiniteSetting;

import java.util.List;

@Mixin(BlockModelRenderer.class)
public abstract class BlockModelRendererMixin implements ItemConvertible {

    // ThreadLocalを使用して、現在のブロックの不透明度を保存
    private static final ThreadLocal<Float> currentOpacity =
            ThreadLocal.withInitial(() -> 1.0F);

    /**
     * Minecraftのデフォルトレンダラー（BlockModelRenderer）でブロックの面を描画するかどうかを制御します。
     * これにより、XRayの表示/非表示ロジックが適用されます。
     * <p>
     * BlockModelRenderer.shouldDrawFace(...) の内部で呼ばれる Block.shouldDrawSide(...) をラップします。
     */
    @WrapOperation(
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/block/Block;shouldDrawSide(Lnet/minecraft/block/BlockState;Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/Direction;)Z"
            ),
            method = "shouldDrawFace(Lnet/minecraft/world/BlockRenderView;Lnet/minecraft/block/BlockState;ZLnet/minecraft/util/math/Direction;Lnet/minecraft/util/math/BlockPos;)Z"
    )
    private static boolean onRenderSmoothOrFlat(
            BlockState state, BlockState otherState, Direction side,
            Operation<Boolean> original,
            BlockRenderView world, BlockState stateButFromTheOtherMethod,
            boolean cull, Direction sideButFromTheOtherMethod, BlockPos pos)
    {
        // XRay featureが無効な場合はオリジナルのメソッドをそのまま呼び出す
        if (!InfiniteClient.INSTANCE.isFeatureEnabled(XRay.class)) {
            currentOpacity.set(1.0F); // 不透明度をリセット
            return original.call(state, otherState, side);
        }

        XRay xRayFeature = InfiniteClient.INSTANCE.getFeature(XRay.class);
        if (xRayFeature == null) {
            currentOpacity.set(1.0F);
            return original.call(state, otherState, side);
        }

        // --- ブロックリストのチェックと不透明度の設定 ---

        InfiniteSetting<?> blockListSetting = xRayFeature.getSetting("BlockList");
        if (!(blockListSetting instanceof InfiniteSetting.BlockListSetting)) {
            currentOpacity.set(1.0F);
            return original.call(state, otherState, side);
        }

        @SuppressWarnings("unchecked")
        List<String> xRayBlockList = ((InfiniteSetting.BlockListSetting) blockListSetting).getValue();

        // 現在のブロックIDを取得
        String currentBlockId = state.getBlock().getRegistryEntry().registryKey().getValue().toString();
        // 隣接ブロックIDを取得
        String adjacentBlockId = otherState.getBlock().getRegistryEntry().registryKey().getValue().toString();

        boolean isCurrentBlockXRay = xRayBlockList.contains(currentBlockId);
        boolean isAdjacentBlockXRay = xRayBlockList.contains(adjacentBlockId);
        boolean isAdjacentBlockAir = otherState.isAir();

        if (isCurrentBlockXRay) {
            // 現在のブロックがXRayブロック（鉱石）の場合、不透明度は常に1F
            currentOpacity.set(1.0F);
            // 隣接ブロックが空気か、XRayブロックでない場合、この面は描画されるべき (falseを返して描画)
            // Block.shouldDrawSideの戻り値は「面を非表示にするかどうか」なので、falseで描画を強制する
            return false;
        } else {
            // 現在のブロックが非XRayブロック（石、土など）の場合
            // Opacity設定を取得
            float opacityFloat = InfiniteClient.INSTANCE.getSettingFloat(XRay.class, "Opacity", 0.0f);
            currentOpacity.set(opacityFloat);

            // 隣接ブロックがXRayブロック、または空気の場合、この面は描画されるべき (falseを返して描画)
            if (isAdjacentBlockXRay || isAdjacentBlockAir) {
                return false;
            } else {
                // 隣接ブロックも非XRayブロック（石が石に接しているなど）の場合、面を非表示にする (trueを返して非表示)
                return true;
            }
        }
    }

    /**
     * BlockModelRenderer.renderQuad(...) の内部で、テクスチャのアルファ値に乗算される定数1.0Fを置き換えます。
     * これにより、上記 onRenderSmoothOrFlat で設定された currentOpacity の値が適用されます。
     */
    @ModifyConstant(
            method = "renderQuad(Lnet/minecraft/world/BlockRenderView;Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/client/render/VertexConsumer;Lnet/minecraft/client/util/math/MatrixStack$Entry;Lnet/minecraft/client/render/model/BakedQuad;Lnet/minecraft/client/render/block/BlockModelRenderer$LightmapCache;I)V",
            constant = @Constant(floatValue = 1.0F)
    )
    private float modifyOpacity(float original) {
        // スレッドローカルに保存された不透明度を返す
        return currentOpacity.get();
    }
}