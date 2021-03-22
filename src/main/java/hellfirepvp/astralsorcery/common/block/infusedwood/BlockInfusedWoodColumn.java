/*******************************************************************************
 * HellFirePvP / Astral Sorcery 2020
 *
 * All rights reserved.
 * The source code is available on github: https://github.com/HellFirePvP/AstralSorcery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.astralsorcery.common.block.infusedwood;

import hellfirepvp.astralsorcery.common.block.base.template.BlockInfusedWoodTemplate;
import hellfirepvp.astralsorcery.common.util.VoxelUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.IWaterLoggable;
import net.minecraft.entity.MobEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.EnumProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.Direction;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.IBooleanFunction;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Locale;

/**
 * This class is part of the Astral Sorcery Mod
 * The complete source code for this mod can be found on github.
 * Class: BlockInfusedWoodColumn
 * Created by HellFirePvP
 * Date: 20.07.2019 / 20:09
 */
public class BlockInfusedWoodColumn extends BlockInfusedWoodTemplate implements IWaterLoggable {

    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final EnumProperty<PillarType> PILLAR_TYPE = EnumProperty.create("pillartype", PillarType.class);

    private final VoxelShape middleShape, bottomShape, topShape;

    public BlockInfusedWoodColumn() {
        this.setDefaultState(this.getStateContainer().getBaseState().with(PILLAR_TYPE, PillarType.MIDDLE).with(WATERLOGGED, false));
        this.middleShape = createPillarShape();
        this.topShape    = createPillarTopShape();
        this.bottomShape = createPillarBottomShape();
    }

    protected VoxelShape createPillarShape() {
        return Block.makeCuboidShape(4, 0, 4, 12, 16, 12);
    }

    protected VoxelShape createPillarTopShape() {
        VoxelShape column = Block.makeCuboidShape(4, 0, 4, 12, 14, 12);
        VoxelShape top = Block.makeCuboidShape(2, 14, 2, 14, 16, 14);

        return VoxelUtils.combineAll(IBooleanFunction.OR,
                column, top);
    }

    protected VoxelShape createPillarBottomShape() {
        VoxelShape column = Block.makeCuboidShape(4, 2, 4, 12, 16, 12);
        VoxelShape bottom = Block.makeCuboidShape(2, 0, 2, 14, 2, 14);

        return VoxelUtils.combineAll(IBooleanFunction.OR,
                column, bottom);
    }

    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
        super.fillStateContainer(builder);
        builder.add(PILLAR_TYPE, WATERLOGGED);
    }

    @Override
    public VoxelShape getShape(BlockState state, IBlockReader world, BlockPos pos, ISelectionContext ctx) {
        switch (state.get(PILLAR_TYPE)) {
            case TOP:
                return this.topShape;
            case BOTTOM:
                return this.bottomShape;
            default:
            case MIDDLE:
                return this.middleShape;
        }
    }

    @Override
    public BlockState updatePostPlacement(BlockState thisState, Direction otherBlockFacing, BlockState otherBlockState, IWorld world, BlockPos thisPos, BlockPos otherBlockPos) {
        if (thisState.get(WATERLOGGED)) {
            world.getPendingFluidTicks().scheduleTick(thisPos, Fluids.WATER, Fluids.WATER.getTickRate(world));
        }
        return this.getThisState(world, thisPos).with(WATERLOGGED, thisState.get(WATERLOGGED));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockItemUseContext ctx) {
        BlockPos blockpos = ctx.getPos();
        World world = ctx.getWorld();
        FluidState ifluidstate = world.getFluidState(blockpos);
        return this.getThisState(world, blockpos).with(WATERLOGGED, ifluidstate.getFluid() == Fluids.WATER);
    }

    private BlockState getThisState(IBlockReader world, BlockPos pos) {
        boolean hasUp   = world.getBlockState(pos.up()).getBlock()   instanceof BlockInfusedWoodColumn;
        boolean hasDown = world.getBlockState(pos.down()).getBlock() instanceof BlockInfusedWoodColumn;
        if (hasUp) {
            if (hasDown) {
                return this.getDefaultState().with(PILLAR_TYPE, PillarType.MIDDLE);
            }
            return this.getDefaultState().with(PILLAR_TYPE, PillarType.BOTTOM);
        } else if (hasDown) {
            return this.getDefaultState().with(PILLAR_TYPE, PillarType.TOP);
        }
        return this.getDefaultState().with(PILLAR_TYPE, PillarType.MIDDLE);
    }

    public FluidState getFluidState(BlockState state) {
        return state.get(WATERLOGGED) ? Fluids.WATER.getStillFluidState(false) : super.getFluidState(state);
    }

    @Nullable
    @Override
    public PathNodeType getAiPathNodeType(BlockState state, IBlockReader world, BlockPos pos, @Nullable MobEntity entity) {
        return PathNodeType.BLOCKED;
    }


    public static enum PillarType implements IStringSerializable {

        TOP,
        MIDDLE,
        BOTTOM;

        @Override
        public String getString() {
            return name().toLowerCase(Locale.ROOT);
        }

        @Override
        public String toString() {
            return this.getString();
        }
    }
}
