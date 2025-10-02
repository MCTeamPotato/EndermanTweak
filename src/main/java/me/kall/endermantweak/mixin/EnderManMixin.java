package me.kall.endermantweak.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.monster.EnderMan;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnderMan.class)
public abstract class EnderManMixin {
    @Unique private boolean tp$rain = false;

    @Unique private static String rain$damageId = null;

    @Unique private int rain$tickCount;

    @Inject(method = "hurt", at = @At("RETURN"))
    private void onHurt(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (this.tp$rain) return;
        EnderMan entity = (EnderMan) (Object) this;
        if (rain$damageId == null) rain$damageId = entity.damageSources().drown().getMsgId();
        if (cir.getReturnValue() && entity.level().isRainingAt(entity.blockPosition()) && source.getMsgId().equals(rain$damageId)) {
            this.tp$rain = true;
        }
    }

    @Inject(method = "aiStep", at = @At("RETURN"))
    private void onAiStep(CallbackInfo ci) {
        if (tp$rain) {
            rain$tickCount++;
            if (rain$tickCount == 20 * 10) {
                tp$rain = false;
                rain$tickCount = 0;
            }
        } else if (rain$tickCount != 0) {
            rain$tickCount = 0;
        }
    }

    @Inject(method = "teleport()Z", at = @At("HEAD"), cancellable = true)
    private void onTp(CallbackInfoReturnable<Boolean> cir) {
        EnderMan entity = (EnderMan) (Object) this;
        if (this.tp$rain && !entity.level().isClientSide() && entity.isAlive()) {
            this.tp$rain = false;

            BlockPos pos = entity.blockPosition();
            BlockPos.MutableBlockPos check = new BlockPos.MutableBlockPos();
            RandomSource random = entity.getRandom();
            int radius = 16;

            for (int attempt = 0; attempt < 20; attempt++) {
                int dx = pos.getX() + random.nextInt(radius * 2 + 1) - radius;
                int dz = pos.getZ() + random.nextInt(radius * 2 + 1) - radius;

                for (int y = pos.getY() - 10; y > entity.level().getMinBuildHeight(); y--) {
                    check.set(dx, y, dz);
                    if (tp$isSafe(entity, check)) {
                        entity.teleportTo(check.getX() + 0.5, check.getY() + 1, check.getZ() + 0.5);
                        cir.setReturnValue(true);
                        return;
                    }
                }
            }

            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    for (int y = pos.getY() - 10; y > entity.level().getMinBuildHeight(); y--) {
                        check.set(pos.getX() + dx, y, pos.getZ() + dz);
                        if (tp$isSafe(entity, check)) {
                            entity.teleportTo(check.getX() + 0.5, check.getY() + 1, check.getZ() + 0.5);
                            cir.setReturnValue(true);
                            return;
                        }
                    }
                }
            }
        }
    }

    @Unique
    private boolean tp$isSafe(EnderMan entity, BlockPos.MutableBlockPos pos) {

        int baseY = pos.getY();
        boolean canStandOn = entity.level().getBlockState(pos).entityCanStandOn(entity.level(), pos, entity);

        pos.setY(baseY + 1);
        boolean isEmptyAbove1 = entity.level().isEmptyBlock(pos);

        pos.setY(baseY + 2);
        boolean isEmptyAbove2 = entity.level().isEmptyBlock(pos);

        pos.setY(baseY + 3);
        boolean isEmptyAbove3 = entity.level().isEmptyBlock(pos);

        pos.setY(baseY + 4);
        boolean isEmptyAbove4 = entity.level().isEmptyBlock(pos);

        pos.setY(baseY);

        return canStandOn && isEmptyAbove1 && isEmptyAbove2 && isEmptyAbove3 && isEmptyAbove4;
    }
}
