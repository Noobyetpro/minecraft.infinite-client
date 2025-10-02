package org.theinfinitys.mixin.client.rendering;

import net.fabricmc.fabric.impl.client.indigo.renderer.mesh.MutableQuadViewImpl;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.AbstractTerrainRenderContext;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.BlockRenderInfo;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.theinfinitys.InfiniteClient;
import org.theinfinitys.features.rendering.XRay;
import org.theinfinitys.settings.InfiniteSetting;

import java.util.List;

@Mixin(value = AbstractTerrainRenderContext.class, remap = false)
public abstract class AbstractTerrainRenderContextMixin {

    @Shadow @Final
    private BlockRenderInfo blockInfo;

    /**
     * Applies X-Ray's opacity mask to the block color after all the normal
     * coloring and shading is done, if Indigo is running.
     */
    @Inject(
            at = @At("RETURN"),
            method = "shadeQuad(Lnet/fabricmc/fabric/impl/client/indigo/renderer/mesh/MutableQuadViewImpl;ZZZ)V"
    )
    private void onShadeQuad(MutableQuadViewImpl quad, boolean ao,
                             boolean emissive, boolean vanillaShade, CallbackInfo ci) {
        // XRay featureを取得し、無効な場合は処理を終了
        if (!InfiniteClient.INSTANCE.isFeatureEnabled(XRay.class)) {
            return;
        }
        XRay xRayFeature = InfiniteClient.INSTANCE.getFeature(XRay.class);
        // --- ブロックリストのチェック ---
        InfiniteSetting<?> blockListSetting = xRayFeature.getSetting("BlockList");
        if (!(blockListSetting instanceof InfiniteSetting.BlockListSetting)) {
            return;
        }

        @SuppressWarnings("unchecked")
        List<String> xRayBlockList = ((InfiniteSetting.BlockListSetting) blockListSetting).getValue();

        // 現在のブロックIDを取得
        String blockId = blockInfo.blockState.getBlock().getRegistryEntry().registryKey().getValue().toString();

        // XRayブロックリストに含まれているブロック（鉱石など）は不透明度を適用しない
        if (xRayBlockList.contains(blockId)) {
            return;
        }

        // --- Opacity値の取得と計算 ---

        // Opacityの設定値を取得 (0.0f - 0.99f)
        float opacityFloat = InfiniteClient.INSTANCE.getSettingFloat(XRay.class, "Opacity", 0.0f);

        // Opacityが0.0f（完全に透明）の場合は処理をスキップ
        if (opacityFloat <= 0.0f) {
            return;
        }

        // 不透明度（0x00 から 0xFF）を計算し、境界値に収める
        // 0.0f -> 0x00 (透明), 0.99f -> 0xFE (ほぼ不透明)
        int alphaValue = (int) (opacityFloat * 255.0f);
        if (alphaValue < 0x00) alphaValue = 0x00;
        if (alphaValue > 0xFF) alphaValue = 0xFF;


        // --- クアッド（面）の色を修正 ---
        for (int i = 0; i < 4; i++) {
            int oldColor = quad.color(i);

            // 既存の色のRGB (0x00FFFFFF) を保持し、新しいアルファ値 (alphaValue << 24) を結合する
            // oldColor & 0x00FFFFFF は既存のRGB値
            // alphaValue << 24 は新しいアルファ値
            int newColor = (oldColor & 0x00FFFFFF) | (alphaValue << 24);

            quad.color(i, newColor);
        }
    }
}