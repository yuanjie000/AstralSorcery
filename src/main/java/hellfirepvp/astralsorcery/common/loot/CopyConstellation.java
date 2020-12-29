/*******************************************************************************
 * HellFirePvP / Astral Sorcery 2020
 *
 * All rights reserved.
 * The source code is available on github: https://github.com/HellFirePvP/AstralSorcery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.astralsorcery.common.loot;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import hellfirepvp.astralsorcery.common.constellation.ConstellationItem;
import hellfirepvp.astralsorcery.common.constellation.ConstellationTile;
import hellfirepvp.astralsorcery.common.constellation.IMinorConstellation;
import hellfirepvp.astralsorcery.common.constellation.IWeakConstellation;
import hellfirepvp.astralsorcery.common.lib.LootAS;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootFunctionType;
import net.minecraft.loot.LootParameters;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.loot.LootContext;
import net.minecraft.loot.LootFunction;
import net.minecraft.loot.conditions.ILootCondition;

/**
 * This class is part of the Astral Sorcery Mod
 * The complete source code for this mod can be found on github.
 * Class: CopyConstellation
 * Created by HellFirePvP
 * Date: 16.08.2019 / 06:38
 */
public class CopyConstellation extends LootFunction {

    private CopyConstellation(ILootCondition[] conditionsIn) {
        super(conditionsIn);
    }

    @Override
    public LootFunctionType getFunctionType() {
        return LootAS.Functions.COPY_CONSTELLATION;
    }

    @Override
    protected ItemStack doApply(ItemStack stack, LootContext context) {
        if (context.has(LootParameters.BLOCK_ENTITY)) {
            TileEntity tile = context.get(LootParameters.BLOCK_ENTITY);
            if (tile instanceof ConstellationTile && stack.getItem() instanceof ConstellationItem) {
                IWeakConstellation main = ((ConstellationTile) tile).getAttunedConstellation();
                IMinorConstellation trait = ((ConstellationTile) tile).getTraitConstellation();

                ((ConstellationItem) stack.getItem()).setAttunedConstellation(stack, main);
                ((ConstellationItem) stack.getItem()).setTraitConstellation(stack, trait);
            }
        }
        return stack;
    }

    public static LootFunction.Builder<?> builder() {
        return builder(CopyConstellation::new);
    }

    public static class Serializer extends LootFunction.Serializer<CopyConstellation> {

        @Override
        public CopyConstellation deserialize(JsonObject jsonObject, JsonDeserializationContext ctx, ILootCondition[] conditions) {
            return new CopyConstellation(conditions);
        }
    }
}
