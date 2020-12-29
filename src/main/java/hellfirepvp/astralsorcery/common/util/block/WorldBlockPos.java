/*******************************************************************************
 * HellFirePvP / Astral Sorcery 2020
 *
 * All rights reserved.
 * The source code is available on github: https://github.com/HellFirePvP/AstralSorcery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.astralsorcery.common.util.block;

import hellfirepvp.astralsorcery.common.util.MiscUtils;
import hellfirepvp.astralsorcery.common.util.object.TransformReference;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3i;
import net.minecraft.world.World;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.LogicalSidedProvider;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.function.Function;

/**
 * This class is part of the Astral Sorcery Mod
 * The complete source code for this mod can be found on github.
 * Class: WorldBlockPos
 * Created by HellFirePvP
 * Date: 07.11.2016 / 11:47
 */
public class WorldBlockPos extends BlockPos {

    private final TransformReference<RegistryKey<World>, World> worldReference;

    private WorldBlockPos(TransformReference<RegistryKey<World>, World> worldReference, BlockPos pos) {
        super(pos);
        this.worldReference = worldReference;
    }

    private WorldBlockPos(RegistryKey<World> type, BlockPos pos, Function<RegistryKey<World>, World> worldProvider) {
        super(pos);
        this.worldReference = new TransformReference<>(type, worldProvider);
    }

    public static WorldBlockPos wrapServer(World world, BlockPos pos) {
        return new WorldBlockPos(world.getDimensionKey(), pos, type -> {
            MinecraftServer server = LogicalSidedProvider.INSTANCE.get(LogicalSide.SERVER);
            return server.getWorld(type);
        });
    }

    public static WorldBlockPos wrapTileEntity(TileEntity tile) {
        return new WorldBlockPos(tile.getWorld().getDimensionKey(), tile.getPos(), type -> tile.getWorld());
    }

    public RegistryKey<World> getWorldKey() {
        return this.worldReference.getReference();
    }

    private WorldBlockPos wrapInternal(BlockPos pos) {
        return new WorldBlockPos(this.worldReference, pos);
    }

    @Override
    public WorldBlockPos add(int x, int y, int z) {
        return wrapInternal(super.add(x, y, z));
    }

    @Override
    public WorldBlockPos add(double x, double y, double z) {
        return wrapInternal(super.add(x, y, z));
    }

    @Override
    public WorldBlockPos add(Vector3i vec) {
        return wrapInternal(super.add(vec));
    }

    @Nullable
    public <T extends TileEntity> T getTileAt(Class<T> tileClass, boolean forceChunkLoad) {
        World world = this.worldReference.getValue();
        if (world != null) {
            return MiscUtils.getTileAt(world, this, tileClass, forceChunkLoad);
        }
        return null;
    }

    @Nullable
    public World getWorld() {
        return this.worldReference.getValue();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        WorldBlockPos that = (WorldBlockPos) o;
        return Objects.equals(getWorldKey(), that.getWorldKey());
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + getWorldKey().hashCode();
        return result;
    }
}
