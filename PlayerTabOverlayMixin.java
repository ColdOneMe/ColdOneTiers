package com.coldonetiers.mixin;

import com.coldonetiers.TierFormatter;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Значок тира в списке игроков (Tab). */
@Mixin(PlayerTabOverlay.class)
public abstract class PlayerTabOverlayMixin {
    @Inject(method = "getNameForDisplay", at = @At("RETURN"), cancellable = true, require = 0)
    private void coldonetiers$decorate(PlayerInfo info, CallbackInfoReturnable<Component> cir) {
        cir.setReturnValue(TierFormatter.decorate(cir.getReturnValue(), true));
    }
}
