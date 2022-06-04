package net.bunten.enderscape.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.bunten.enderscape.registry.EnderscapeBlocks;
import net.bunten.enderscape.registry.EnderscapeSounds;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.sound.BlockSoundGroup;

@Mixin(Block.class)
public abstract class BlockMixin extends AbstractBlock {
    public BlockMixin(Settings settings) {
        super(settings);
    }

    @Inject(at = @At("HEAD"), method = "getSoundGroup", cancellable = true)
    public void getSoundGroup(BlockState state, CallbackInfoReturnable<BlockSoundGroup> info) {
        // this sucks and should be changed Thanks
        if (state.isIn(EnderscapeBlocks.PURPUR_SOUND_BLOCKS)) {
            info.setReturnValue(EnderscapeSounds.PURPUR);
        } else if (state.isIn(EnderscapeBlocks.CHORUS_SOUND_BLOCKS)) {
            info.setReturnValue(EnderscapeSounds.CHORUS);
        } else if (state.isIn(EnderscapeBlocks.END_STONE_SOUND_BLOCKS)) {
            info.setReturnValue(EnderscapeSounds.END_STONE);
        } else if (state.isIn(EnderscapeBlocks.END_STONE_BRICK_SOUND_BLOCKS)) {
            info.setReturnValue(EnderscapeSounds.END_STONE_BRICKS);
        } else if (state.isIn(EnderscapeBlocks.SHULKER_SOUND_BLOCKS)) {
            info.setReturnValue(EnderscapeSounds.SHULKER);
        }
    }
}