package hellfirepvp.astralsorcery.common.item;

import hellfirepvp.astralsorcery.common.CommonProxy;
import hellfirepvp.astralsorcery.common.constellation.Constellation;
import hellfirepvp.astralsorcery.common.constellation.ConstellationRegistry;
import hellfirepvp.astralsorcery.common.constellation.Tier;
import hellfirepvp.astralsorcery.common.data.research.PlayerProgress;
import hellfirepvp.astralsorcery.common.data.research.ResearchManager;
import hellfirepvp.astralsorcery.common.item.base.IMetaItem;
import hellfirepvp.astralsorcery.common.util.ItemNBTHelper;
import hellfirepvp.astralsorcery.common.util.WRItemObject;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.WeightedRandom;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is part of the Astral Sorcery Mod
 * The complete source code for this mod can be found on github.
 * Class: ItemConstellationPaper
 * Created by HellFirePvP
 * Date: 07.05.2016 / 15:16
 */
public class ItemConstellationPaper extends Item implements IMetaItem {

    public ItemConstellationPaper() {
        setUnlocalizedName("ItemConstellationPaper");
        setMaxDamage(0);
        setMaxStackSize(1);
        setHasSubtypes(true);
        setCreativeTab(CommonProxy.creativeTabAstralSorcery);
    }

    @Override
    public void getSubItems(Item itemIn, CreativeTabs tab, List<ItemStack> subItems) {
        subItems.add(new ItemStack(this, 1));

        for (Tier tier : ConstellationRegistry.ascendingTiers()) {
            for (Constellation c : tier.getConstellations()) {
                ItemStack cPaper = new ItemStack(this, 1);
                setConstellation(cPaper, c);
                subItems.add(cPaper);
            }
        }
    }

    @Override
    public void addInformation(ItemStack stack, EntityPlayer playerIn, List<String> tooltip, boolean advanced) {
        if (stack.getItemDamage() == 1) {
            tooltip.add(TextFormatting.GRAY + I18n.translateToLocal("constellation.noInformation"));
            return;
        }
        Constellation c = getConstellation(stack);
        if (c != null) {
            tooltip.add(TextFormatting.BLUE + I18n.translateToLocal(c.getName()));
        }
    }

    @Override
    public void onUpdate(ItemStack stack, World worldIn, Entity entityIn, int itemSlot, boolean isSelected) {
        if (worldIn.isRemote || getConstellation(stack) != null) return;

        if (entityIn != null && entityIn instanceof EntityPlayer) {
            PlayerProgress progress = ResearchManager.getProgress((EntityPlayer) entityIn);
            if (progress != null) {
                int highest = progress.getTierReached();
                List<Constellation> constellations = new ArrayList<Constellation>();
                for (Tier tier : ConstellationRegistry.ascendingTiers()) {
                    if (tier.tierNumber() > highest) continue;
                    for (Constellation c : tier.getConstellations()) {
                        if (!progress.hasConstellationDiscovered(c.getName())) constellations.add(c);
                    }
                }

                removeInventoryConstellations(((EntityPlayer) entityIn).inventory, constellations);

                if (constellations.isEmpty()) {
                    stack.setItemDamage(1);
                    return;
                }

                List<WRItemObject<Constellation>> wrp = buildWeightedRandomList(constellations);
                WRItemObject<Constellation> result = WeightedRandom.getRandomItem(worldIn.rand, wrp);
                if (worldIn.rand.nextBoolean()) {
                    stack.setItemDamage(1);
                } else {
                    setConstellation(stack, result.getValue());
                }
            }
        }
    }

    private void removeInventoryConstellations(InventoryPlayer inventory, List<Constellation> constellations) {
        if (inventory == null) return;
        for (ItemStack stack : inventory.mainInventory) {
            if (stack == null || stack.getItem() == null) continue;
            if (stack.getItem() instanceof ItemConstellationPaper) {
                Constellation c = getConstellation(stack);
                if (c != null) {
                    constellations.remove(c);
                }
            }
        }
    }

    private List<WRItemObject<Constellation>> buildWeightedRandomList(List<Constellation> constellations) {
        List<WRItemObject<Constellation>> wrc = new ArrayList<WRItemObject<Constellation>>();
        for (Constellation c : constellations) {
            Tier tier = ConstellationRegistry.getTier(c.getAssociatedTier());
            if (tier == null) continue;
            WRItemObject<Constellation> i = new WRItemObject<Constellation>((int) (tier.getShowupChance() * 100), c);
            wrc.add(i);
        }
        return wrc;
    }

    public static Constellation getConstellation(ItemStack stack) {
        Item i = stack.getItem();
        if (!(i instanceof ItemConstellationPaper)) return null;
        String name = ItemNBTHelper.getPersistentData(stack).getString("constellation");
        if (name == null || name.isEmpty()) return null;
        return ConstellationRegistry.getConstellationByName(name);
    }

    public static void setConstellation(ItemStack stack, Constellation constellation) {
        Item i = stack.getItem();
        if (!(i instanceof ItemConstellationPaper)) return;
        String name = constellation.getName();
        if (name == null) return;
        ItemNBTHelper.getPersistentData(stack).setString("constellation", name);
    }

    @Override
    public int[] getSubItems() {
        return new int[]{0, 1};
    }

}