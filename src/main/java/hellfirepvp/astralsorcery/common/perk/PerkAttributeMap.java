/*******************************************************************************
 * HellFirePvP / Astral Sorcery 2020
 *
 * All rights reserved.
 * The source code is available on github: https://github.com/HellFirePvP/AstralSorcery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.astralsorcery.common.perk;

import com.google.common.collect.Lists;
import hellfirepvp.astralsorcery.common.data.research.PlayerProgress;
import hellfirepvp.astralsorcery.common.data.research.ResearchHelper;
import hellfirepvp.astralsorcery.common.lib.PerkAttributeTypesAS;
import hellfirepvp.astralsorcery.common.perk.modifier.PerkAttributeModifier;
import hellfirepvp.astralsorcery.common.perk.source.ModifierSource;
import hellfirepvp.astralsorcery.common.perk.type.ModifierType;
import hellfirepvp.astralsorcery.common.perk.type.PerkAttributeType;
import hellfirepvp.astralsorcery.common.util.ReadWriteLockable;
import hellfirepvp.astralsorcery.common.util.log.LogCategory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.fml.LogicalSide;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * This class is part of the Astral Sorcery Mod
 * The complete source code for this mod can be found on github.
 * Class: PerkAttributeMap
 * Created by HellFirePvP
 * Date: 08.08.2019 / 18:20
 */
public class PerkAttributeMap implements ReadWriteLockable {

    private final ReadWriteLock accessLock = new ReentrantReadWriteLock(true);
    private final LogicalSide side;
    private final Map<PerkAttributeType, List<PerkAttributeModifier>> modifiers = Collections.synchronizedMap(new HashMap<>());
    private final List<PerkConverter> converters = new ArrayList<>();

    PerkAttributeMap(LogicalSide side) {
        this.side = side;
    }

    Collection<PerkAttributeModifier> applyModifier(@Nonnull PlayerEntity player, @Nonnull PerkAttributeModifier modifier, @Nullable ModifierSource owningSource) {
        PlayerProgress prog = ResearchHelper.getProgress(player, this.side);
        List<PerkAttributeModifier> added = new ArrayList<>();

        List<PerkAttributeModifier> modify = Lists.newArrayList();
        modify.add(modifier);
        modify.addAll(this.gainModifiers(player, prog, modifier, owningSource));
        for (PerkAttributeModifier mod : modify) {

            PerkAttributeModifier preMod = mod;
            LogCategory.PERKS.info(() -> "Applying unique modifier " + preMod.getComparisonKey());

            mod = this.convertModifier(player, prog, mod, owningSource);

            PerkAttributeModifier postMod = mod;
            LogCategory.PERKS.info(() -> "Applying converted modifier " + postMod.getComparisonKey());
            if (this.cacheModifier(player, mod.getAttributeType(), mod)) {
                added.add(mod);
            } else {
                LogCategory.PERKS.warn(() -> "Could not apply modifier " + postMod.getComparisonKey() + " - already applied!");
            }
        }
        return added;
    }

    private boolean cacheModifier(PlayerEntity player, PerkAttributeType type, PerkAttributeModifier modifier) {
        boolean noModifiers = getModifiersByType(type, modifier.getMode()).isEmpty();
        List<PerkAttributeModifier> modifiers = this.modifiers.computeIfAbsent(type, t -> Lists.newArrayList());
        if (modifiers.contains(modifier)) {
            return false;
        }

        if (noModifiers) {
            type.onModeApply(player, modifier.getMode(), side);
        }
        return modifiers.add(modifier);
    }

    Collection<PerkAttributeModifier> removeModifier(@Nonnull PlayerEntity player, @Nonnull PerkAttributeModifier modifier, @Nullable ModifierSource owningSource) {
        PlayerProgress prog = ResearchHelper.getProgress(player, this.side);
        List<PerkAttributeModifier> removed = new ArrayList<>();

        List<PerkAttributeModifier> modify = Lists.newArrayList();
        modify.add(modifier);
        modify.addAll(this.gainModifiers(player, prog, modifier, owningSource));
        for (PerkAttributeModifier mod : modify) {

            PerkAttributeModifier preMod = mod;
            LogCategory.PERKS.info(() -> "Removing unique modifier " + preMod.getComparisonKey());

            mod = this.convertModifier(player, prog, mod, owningSource);

            PerkAttributeModifier postMod = mod;
            LogCategory.PERKS.info(() -> "Removing converted modifier " + postMod.getComparisonKey());
            if (this.dropModifier(player, mod.getAttributeType(), mod)) {
                removed.add(mod);
            } else {
                LogCategory.PERKS.warn(() -> "Could not remove modifier " + postMod.getComparisonKey() + " - not applied!");
            }
        }
        return removed;
    }

    private boolean dropModifier(PlayerEntity player, PerkAttributeType type, PerkAttributeModifier modifier) {
        if (modifiers.computeIfAbsent(type, t -> Lists.newArrayList()).remove(modifier)) {
            boolean completelyRemoved = modifiers.get(type).isEmpty();
            if (getModifiersByType(type, modifier.getMode()).isEmpty()) {
                type.onModeRemove(player, modifier.getMode(), side, completelyRemoved);
            }
            return true;
        }
        return false;
    }

    @Nonnull
    private PerkAttributeModifier convertModifier(@Nonnull PlayerEntity player, @Nonnull PlayerProgress progress, @Nonnull PerkAttributeModifier modifier, @Nullable ModifierSource owningSource) {
        for (PerkConverter converter : converters) {
            modifier = converter.convertModifier(player, progress, modifier, owningSource);
        }
        return modifier;
    }

    @Nonnull
    private Collection<PerkAttributeModifier> gainModifiers(@Nonnull PlayerEntity player, @Nonnull PlayerProgress progress, @Nonnull PerkAttributeModifier modifier, @Nullable ModifierSource owningSource) {
        Collection<PerkAttributeModifier> modifiers = Lists.newArrayList();
        for (PerkConverter converter : converters) {
            modifiers.addAll(converter.gainExtraModifiers(player, progress, modifier, owningSource));
        }
        return modifiers;
    }

    boolean applyConverter(PlayerEntity player, PerkConverter converter) {
        assertConvertersModifiable();

        LogCategory.PERKS.info(() -> "Try adding converter " + converter.getRegistryName() + " on " + this.side.name());

        if (converters.contains(converter)) {
            return false;
        }

        converters.add(converter);
        converter.onApply(player, side);

        LogCategory.PERKS.info(() -> "Added converter " + converter.getRegistryName());
        return true;
    }

    boolean removeConverter(PlayerEntity player, PerkConverter converter) {
        assertConvertersModifiable();

        LogCategory.PERKS.info(() -> "Try removing converter " + converter.getRegistryName() + " on " + this.side.name());

        if (converters.remove(converter)) {
            converter.onRemove(player, side);

            LogCategory.PERKS.info(() -> "Removed converter " + converter.getRegistryName());
            return true;
        }
        return false;
    }

    void assertConvertersModifiable() {
        int appliedModifiers = 0;
        for (List<PerkAttributeModifier> modifiers : this.modifiers.values()) {
            appliedModifiers += modifiers.size();
        }
        if (appliedModifiers > 0) {

            LogCategory.PERKS.warn(() -> "Following modifiers are still applied on " + this.side.name() + " while trying to modify converters:");
            for (List<PerkAttributeModifier> modifiers : this.modifiers.values()) {
                for (PerkAttributeModifier modifier : modifiers) {
                    LogCategory.PERKS.warn(() -> "Modifier: " + modifier.getComparisonKey());
                }
            }

            throw new IllegalStateException("Trying to modify PerkConverters while modifiers are applied!");
        }
    }

    public boolean hasModifiers(PerkAttributeType type) {
        return this.read(() -> !modifiers.getOrDefault(type, Collections.emptyList()).isEmpty());
    }

    private List<PerkAttributeModifier> getModifiersByType(PerkAttributeType type, ModifierType mode) {
        return modifiers.computeIfAbsent(type, t -> Lists.newArrayList()).stream()
                .filter(mod -> mod.getMode() == mode)
                .collect(Collectors.toList());
    }

    public float getModifier(PlayerEntity player, PlayerProgress progress, PerkAttributeType type) {
        return getModifier(player, progress, type, EnumSet.allOf(ModifierType.class));
    }

    public float getModifier(PlayerEntity player, PlayerProgress progress, PerkAttributeType type, ModifierType mode) {
        return getModifier(player, progress, type, EnumSet.of(mode));
    }

    public float getModifier(PlayerEntity player, PlayerProgress progress, PerkAttributeType type, Collection<ModifierType> applicableModes) {
        float perkEffectModifier;
        if (!type.equals(PerkAttributeTypesAS.ATTR_TYPE_INC_PERK_EFFECT)) {
            perkEffectModifier = getModifier(player, progress, PerkAttributeTypesAS.ATTR_TYPE_INC_PERK_EFFECT);
        } else {
            perkEffectModifier = 1F;
        }

        return this.read(() -> {
            float mod = 1F;
            if (applicableModes.contains(ModifierType.ADDITION)) {
                for (PerkAttributeModifier modifier : getModifiersByType(type, ModifierType.ADDITION)) {
                    mod += (modifier.getValue(player, progress) * perkEffectModifier);
                }
            }
            if (applicableModes.contains(ModifierType.ADDED_MULTIPLY)) {
                float multiply = mod;
                for (PerkAttributeModifier modifier : getModifiersByType(type, ModifierType.ADDED_MULTIPLY)) {
                    mod += multiply * (modifier.getValue(player, progress) * perkEffectModifier);
                }
            }
            if (applicableModes.contains(ModifierType.STACKING_MULTIPLY)) {
                for (PerkAttributeModifier modifier : getModifiersByType(type, ModifierType.STACKING_MULTIPLY)) {
                    mod *= ((modifier.getValue(player, progress) - 1F) * perkEffectModifier) + 1;
                }
            }
            return mod;
        });
    }

    public float modifyValue(PlayerEntity player, PlayerProgress progress, PerkAttributeType type, float value) {
        float perkEffectModifier;
        if (!type.equals(PerkAttributeTypesAS.ATTR_TYPE_INC_PERK_EFFECT)) {
            perkEffectModifier = modifyValue(player, progress, PerkAttributeTypesAS.ATTR_TYPE_INC_PERK_EFFECT, 1F);
        } else {
            perkEffectModifier = 1F;
        }

        return this.read(() -> {
            float val = value;
            for (PerkAttributeModifier mod : getModifiersByType(type, ModifierType.ADDITION)) {
                val += (mod.getValue(player, progress) * perkEffectModifier);
            }
            float multiply = val;
            for (PerkAttributeModifier mod : getModifiersByType(type, ModifierType.ADDED_MULTIPLY)) {
                val += multiply * (mod.getValue(player, progress) * perkEffectModifier);
            }
            for (PerkAttributeModifier mod : getModifiersByType(type, ModifierType.STACKING_MULTIPLY)) {
                val *= ((mod.getValue(player, progress) - 1F) * perkEffectModifier) + 1F;
            }
            return val;
        });
    }

    @Override
    public ReadWriteLock getLock() {
        return this.accessLock;
    }
}
