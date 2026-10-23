package com.coldonetiers.mixin;

import com.coldonetiers.TierFormatter;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Ник игрока (в том числе над головой) получает значок тира. */
@Mixin(Player.class)
public abstract class PlayerMixin {
    @Inject(method = "getDisplayName", at = @At("RETURN"), cancellable = true, require = 0)
    private void coldonetiers$decorate(CallbackInfoReturnable<Component> cir) {
        if ((Object) this instanceof AbstractClientPlayer) {
            cir.setReturnValue(TierFormatter.decorate(cir.getReturnValue(), false));
        }
    }
}
