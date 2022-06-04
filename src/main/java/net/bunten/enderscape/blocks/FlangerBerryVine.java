package net.bunten.enderscape.blocks;

import java.util.Random;

import org.jetbrains.annotations.Nullable;

import net.bunten.enderscape.blocks.properties.FlangerBerryStage;
import net.bunten.enderscape.interfaces.LayerMapped;
import net.bunten.enderscape.registry.EnderscapeBlocks;
import net.bunten.enderscape.util.MathUtil;
import net.bunten.enderscape.util.Util;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.Fertilizable;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ShearsItem;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;

public class FlangerBerryVine extends Block implements LayerMapped, Fertilizable {

    public static final BooleanProperty ATTACHED = Properties.ATTACHED;
    public static final IntProperty AGE = Properties.AGE_15;
    public static final int MAX_AGE = 15;

    protected static final Block BERRY = EnderscapeBlocks.FLANGER_BERRY_BLOCK;

    public FlangerBerryVine(Settings settings) {
        super(settings);
        setDefaultState(getVineState(false, 0));
    }

    /**
     * Gets the random age the block will use to determine age upon placement
     */
    public BlockState getRandomGrowthState(WorldAccess world) {
        return getDefaultState().with(AGE, MathUtil.nextInt(world.getRandom(), (int) (MAX_AGE * 0.1), (int) (MAX_AGE * 0.6)));
    }

    @Override
    @Nullable
    public BlockState getPlacementState(ItemPlacementContext context) {
        BlockState state = context.getWorld().getBlockState(context.getBlockPos().down());
        if (state.isOf(this)) {
            return getDefaultState();
        }

        return getRandomGrowthState(context.getWorld());
    }

    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(ATTACHED, AGE);
    }

    protected BlockState age(BlockState state, boolean attached) {
        state = state.with(ATTACHED, attached);
        return state.get(AGE) < MAX_AGE ? state.cycle(AGE) : state;
    }

    private BlockState getVineState(boolean attached, int age) {
        return getDefaultState().with(ATTACHED, attached).with(AGE, age);
    }

    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext ctx) {
        if (state.get(ATTACHED)) {
            return createCuboidShape(2.5, 0, 2.5, 13.5, 16, 13.5);
        } else {
            return createCuboidShape(2.5, 2.5, 2.5, 13.5, 16, 13.5);
        }
    }

    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState newState, WorldAccess world, BlockPos pos, BlockPos posFrom) {
        if (!state.canPlaceAt(world, pos)) {
            return Blocks.AIR.getDefaultState();
        } else {
            var age = state.get(AGE);
            var down = getBlockState(world, pos.down());

            if (down.isOf(BERRY)) {
                return getVineState(true, age);
            } else if (down.isOf(this)) {
                return getVineState(true, down.get(AGE));
            } else {
                return getVineState(false, age);
            }
        }
    }

    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        var above = getBlockState(world, pos.up());
        return above.isOf(this) || above.isIn(EnderscapeBlocks.FLANGER_BERRY_VINE_SUPPORT_BLOCKS) && above.isSideSolidFullSquare(world, pos, Direction.UP);
    }

    public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (random.nextInt(12) == 0) {
            grow(world, random, pos, state);
            return;
        }
    }

    public boolean isFertilizable(BlockView world, BlockPos pos, BlockState state, boolean isClient) {
        for (pos = ((state.get(ATTACHED)) ? pos.down() : pos); pos.getY() >= world.getBottomY(); pos = pos.down()) {
            if (world.getBlockState(pos).isOf(this) && world.getBlockState(pos.down()).isAir()) {
                return true;
            }
        }
        return false;
    }

    public boolean canGrow(World world, Random random, BlockPos pos, BlockState state) {
        return true;
    }

    public void grow(ServerWorld world, Random random, BlockPos pos, BlockState state) {
        for (pos = ((state.get(ATTACHED)) ? pos.down() : pos); pos.getY() > world.getBottomY(); pos = pos.down()) {
            var state2 = world.getBlockState(pos);
            if (state2.isOf(this)) {
                var down = world.getBlockState(pos.down());
                if (state2.get(AGE) == MAX_AGE) {
                    if (down.isAir()) {
                        world.setBlockState(pos, getVineState(true, MAX_AGE));
        
                        BlockSoundGroup group = BERRY.getDefaultState().getSoundGroup();
                        Util.playSound(world, pos.down(), group.getPlaceSound(), SoundCategory.BLOCKS, 1, group.getPitch() * 0.8F);

                        setBlockState(world, pos.down(), BERRY.getDefaultState().with(FlangerBerryBlock.STAGE, FlangerBerryStage.FLOWER));
                        break; 
                    }
                } else {
                    if (down.isAir()) {
                        world.setBlockState(pos.down(), age(state, false));
                        world.setBlockState(pos, age(state, true));
                        break;
                    }
                }
                continue;
            }
        }
    }

    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity mob, Hand hand, BlockHitResult hit) {
        var stack = mob.getStackInHand(hand);
        if (stack.getItem() instanceof ShearsItem && state.get(AGE) < MAX_AGE && !state.get(ATTACHED)) {
            mob.incrementStat(Stats.USED.getOrCreateStat(stack.getItem()));
            if (mob instanceof ServerPlayerEntity server) {
                Criteria.ITEM_USED_ON_BLOCK.trigger(server, pos, stack);
            }

            world.playSound(mob, pos, SoundEvents.BLOCK_GROWING_PLANT_CROP, SoundCategory.BLOCKS, 1, 1);
            world.setBlockState(pos, state.with(AGE, MAX_AGE));
            if (mob != null) {
                stack.damage(1, mob, player -> player.sendToolBreakStatus(hand));
            }

            return ActionResult.success(world.isClient());
        }

        return ActionResult.PASS;
    }

    public boolean canPathfindThrough(BlockState state, BlockView world, BlockPos pos, NavigationType type) {
        return type == NavigationType.AIR && !collidable || super.canPathfindThrough(state, world, pos, type);
    }

    @Override
    public LayerType getLayerType() {
        return LayerType.CUTOUT;
    }

    private BlockState getBlockState(WorldView world, BlockPos pos) {
        return world.getBlockState(pos);
    }

    private void setBlockState(World world, BlockPos pos, BlockState state) {
        world.setBlockState(pos, state, NOTIFY_ALL);
    }
}