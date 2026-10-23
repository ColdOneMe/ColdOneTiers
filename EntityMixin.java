package com.coldonetiers.mixin;

import com.coldonetiers.TierFormatter;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Запасной хук: если игрок не переопределяет getDisplayName, ник берётся отсюда. */
@Mixin(Entity.class)
public abstract class EntityMixin {
    @Inject(method = "getDisplayName", at = @At("RETURN"), cancellable = true, require = 0)
    private void coldonetiers$decorate(CallbackInfoReturnable<Component> cir) {
        if ((Object) this instanceof AbstractClientPlayer) {
            cir.setReturnValue(TierFormatter.decorate(cir.getReturnValue(), false));
        }
    }
}
