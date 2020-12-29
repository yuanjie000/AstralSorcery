/*******************************************************************************
 * HellFirePvP / Astral Sorcery 2020
 *
 * All rights reserved.
 * The source code is available on github: https://github.com/HellFirePvP/AstralSorcery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.astralsorcery.common.item.lens;

import hellfirepvp.astralsorcery.AstralSorcery;
import hellfirepvp.astralsorcery.common.auxiliary.BlockBreakHelper;
import hellfirepvp.astralsorcery.common.lib.ColorsAS;
import hellfirepvp.astralsorcery.common.lib.ItemsAS;
import hellfirepvp.astralsorcery.common.network.PacketChannel;
import hellfirepvp.astralsorcery.common.network.play.server.PktPlayEffect;
import hellfirepvp.astralsorcery.common.util.PartialEffectExecutor;
import hellfirepvp.astralsorcery.common.util.data.ByteBufUtils;
import hellfirepvp.astralsorcery.common.util.data.Vector3;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * This class is part of the Astral Sorcery Mod
 * The complete source code for this mod can be found on github.
 * Class: ItemColoredLensBreak
 * Created by HellFirePvP
 * Date: 21.09.2019 / 18:30
 */
public class ItemColoredLensBreak extends ItemColoredLens {

    private static final ColorTypeBreak COLOR_TYPE_BREAK = new ColorTypeBreak();

    public ItemColoredLensBreak() {
        super(COLOR_TYPE_BREAK);
    }

    private static class ColorTypeBreak extends LensColorType {

        private ColorTypeBreak() {
            super(AstralSorcery.key("break"),
                    TargetType.BLOCK,
                    () -> new ItemStack(ItemsAS.COLORED_LENS_BREAK),
                    ColorsAS.COLORED_LENS_BREAK,
                    0.1F,
                    false);
        }

        @Override
        public void entityInBeam(World world, Vector3 origin, Vector3 target, Entity entity, PartialEffectExecutor executor) {}

        @Override
        public void blockInBeam(World world, BlockPos pos, BlockState state, PartialEffectExecutor executor) {
            if (world.isRemote()) {
                return;
            }

            boolean ranOnce = executor.executeAll(() -> {
                BlockBreakHelper.addProgress(world, pos, 0.4F, () -> {
                    float hardness = state.getBlockHardness(world, pos);
                    if (hardness < 0) {
                        return null;
                    }
                    return hardness * Math.max(1, state.getHarvestLevel());
                });
            });
            if (ranOnce) {
                PktPlayEffect pkt = new PktPlayEffect(PktPlayEffect.Type.BEAM_BREAK)
                        .addData((buf) -> {
                            ByteBufUtils.writePos(buf, pos);
                            buf.writeInt(Block.getStateId(state));
                        });
                PacketChannel.CHANNEL.sendToAllAround(pkt, PacketChannel.pointFromPos(world, pos, 16));
            }
        }
    }
}
