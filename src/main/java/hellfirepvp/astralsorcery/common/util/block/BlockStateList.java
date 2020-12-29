/*******************************************************************************
 * HellFirePvP / Astral Sorcery 2020
 *
 * All rights reserved.
 * The source code is available on github: https://github.com/HellFirePvP/AstralSorcery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.astralsorcery.common.util.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import hellfirepvp.astralsorcery.common.data.config.base.ConfiguredBlockStateList;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * This class is part of the Astral Sorcery Mod
 * The complete source code for this mod can be found on github.
 * Class: BlockStateList
 * Created by HellFirePvP
 * Date: 02.08.2020 / 09:23
 */
public class BlockStateList implements BlockPredicate, Predicate<BlockState> {

    public static final Codec<BlockStateList> CODEC = RecordCodecBuilder.create(codecBuilder -> codecBuilder.group(
            BlockState.CODEC.listOf().fieldOf("blockStates").forGetter(stateList -> {
                List<BlockState> applicable = new ArrayList<>();
                stateList.configuredMatches.forEach(predicate -> {
                    predicate.validMatch.ifLeft(applicable::addAll).ifRight(block -> {
                        applicable.addAll(block.getStateContainer().getValidStates());
                    });
                });
                return applicable;
            }))
    .apply(codecBuilder, states -> {
        BlockStateList list = new BlockStateList();
        states.forEach(list::add);
        return list;
    }));

    private final List<SimpleBlockPredicate> configuredMatches = new ArrayList<>();

    public BlockStateList add(BlockState... states) {
        this.configuredMatches.add(new SimpleBlockPredicate(states));
        return this;
    }

    public BlockStateList add(Block block) {
        this.configuredMatches.add(new SimpleBlockPredicate(block));
        return this;
    }

    public ConfiguredBlockStateList getAsConfig(ForgeConfigSpec.Builder cfgBuilder, String key, String translationKey, String comment) {
        List<String> out = new ArrayList<>();
        configuredMatches.stream().map(SimpleBlockPredicate::getAsConfigList).forEach(out::addAll);
        return new ConfiguredBlockStateList(cfgBuilder
                .comment(comment)
                .translation(translationKey)
                .define(key, out));
    }

    public static BlockStateList fromConfig(List<String> serializedBlockPredicates) {
        BlockStateList list = new BlockStateList();
        for (String str : serializedBlockPredicates) {
            SimpleBlockPredicate predicate = SimpleBlockPredicate.fromConfig(str);
            if (predicate != null) {
                list.configuredMatches.add(predicate);
            }
        }
        return list;
    }

    @Override
    public boolean test(BlockState state) {
        return configuredMatches.stream().anyMatch(predicate -> predicate.test(state));
    }

    @Override
    public boolean test(World world, BlockPos pos, BlockState state) {
        return configuredMatches.stream().anyMatch(predicate -> predicate.test(world, pos, state));
    }
}
