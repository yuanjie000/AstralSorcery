/*******************************************************************************
 * HellFirePvP / Astral Sorcery 2020
 *
 * All rights reserved.
 * The source code is available on github: https://github.com/HellFirePvP/AstralSorcery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.astralsorcery.common.perk.type;

import hellfirepvp.astralsorcery.common.auxiliary.charge.AlignmentChargeHandler;
import hellfirepvp.astralsorcery.common.data.config.base.ConfigEntry;
import hellfirepvp.astralsorcery.common.data.research.ResearchHelper;
import hellfirepvp.astralsorcery.common.event.AttributeEvent;
import hellfirepvp.astralsorcery.common.lib.PerkAttributeTypesAS;
import hellfirepvp.astralsorcery.common.perk.PerkAttributeHelper;
import hellfirepvp.astralsorcery.common.perk.modifier.AttributeModifierDodge;
import hellfirepvp.astralsorcery.common.perk.modifier.PerkAttributeModifier;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.LogicalSide;

import javax.annotation.Nonnull;

/**
 * This class is part of the Astral Sorcery Mod
 * The complete source code for this mod can be found on github.
 * Class: AttributeTypeDodge
 * Created by HellFirePvP
 * Date: 25.08.2019 / 00:28
 */
public class AttributeTypeDodge extends PerkAttributeType {

    public static final Config CONFIG = new Config("type." + PerkAttributeTypesAS.KEY_ATTR_TYPE_INC_DODGE.getPath());

    public AttributeTypeDodge() {
        super(PerkAttributeTypesAS.KEY_ATTR_TYPE_INC_DODGE);
    }

    @Override
    protected void attachListeners(IEventBus eventBus) {
        super.attachListeners(eventBus);
        eventBus.addListener(EventPriority.HIGH, this::onDamageTaken);
    }

    @Nonnull
    @Override
    public PerkAttributeModifier createModifier(float modifier, ModifierType mode) {
        return new AttributeModifierDodge(this, mode, modifier);
    }

    private void onDamageTaken(LivingDamageEvent event) {
        if (!(event.getEntityLiving() instanceof PlayerEntity)) {
            return;
        }
        PlayerEntity player = (PlayerEntity) event.getEntityLiving();
        LogicalSide side = this.getSide(player);
        if (!hasTypeApplied(player, side)) {
            return;
        }
        float chance = PerkAttributeHelper.getOrCreateMap(player, side)
                .modifyValue(player, ResearchHelper.getProgress(player, side), this, 0F);
        chance /= 100.0F;
        chance = AttributeEvent.postProcessModded(player, this, chance);
        if (chance >= rand.nextFloat() && AlignmentChargeHandler.INSTANCE.drainCharge(player, side, CONFIG.chargeCost.get(), false)) {
            event.setCanceled(true);
        }
    }

    private static class Config extends ConfigEntry {

        private ForgeConfigSpec.IntValue chargeCost;

        private Config(String section) {
            super(section);
        }

        @Override
        public void createEntries(ForgeConfigSpec.Builder cfgBuilder) {
            this.chargeCost = cfgBuilder
                    .comment("Defines the amount of starlight charge consumed per dodged damage.")
                    .translation(translationKey("chargeCost"))
                    .defineInRange("chargeCost", 80, 1, 500);
        }
    }
}
