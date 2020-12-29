/*******************************************************************************
 * HellFirePvP / Astral Sorcery 2020
 *
 * All rights reserved.
 * The source code is available on github: https://github.com/HellFirePvP/AstralSorcery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.astralsorcery.common.perk.type;

import com.google.common.collect.Maps;
import hellfirepvp.astralsorcery.common.lib.RegistriesAS;
import hellfirepvp.astralsorcery.common.perk.modifier.PerkAttributeModifier;
import hellfirepvp.astralsorcery.common.perk.reader.PerkAttributeReader;
import hellfirepvp.astralsorcery.common.perk.source.ModifierSource;
import hellfirepvp.astralsorcery.common.util.ReadWriteLockable;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.registries.ForgeRegistryEntry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * This class is part of the Astral Sorcery Mod
 * The complete source code for this mod can be found on github.
 * Class: PerkAttributeType
 * Created by HellFirePvP
 * Date: 08.08.2019 / 16:56
 */
public class PerkAttributeType extends ForgeRegistryEntry<PerkAttributeType> implements ReadWriteLockable {

    protected static final Random rand = new Random();

    //May be used by subclasses to more efficiently track who's got a perk applied
    private final Map<LogicalSide, Set<UUID>> applicationCache = Maps.newHashMap();
    private final ReadWriteLock accessLock = new ReentrantReadWriteLock(true);

    private final boolean isOnlyMultiplicative;

    protected PerkAttributeType(ResourceLocation key) {
        this(key, false);
    }

    protected PerkAttributeType(ResourceLocation key, boolean isMultiplicative) {
        this.setRegistryName(key);
        this.isOnlyMultiplicative = isMultiplicative;

        this.init();
        this.attachListeners(MinecraftForge.EVENT_BUS);
    }

    public static PerkAttributeType makeDefault(ResourceLocation name, boolean isMultiplicative) {
        return new PerkAttributeType(name, isMultiplicative);
    }

    public boolean isMultiplicative() {
        return isOnlyMultiplicative;
    }

    public ITextComponent getTranslatedName() {
        return new TranslationTextComponent(this.getUnlocalizedName());
    }

    public String getUnlocalizedName() {
        return String.format("perk.attribute.%s.%s.name",
                this.getRegistryName().getNamespace(), this.getRegistryName().getPath());
    }

    protected void init() {}

    protected void attachListeners(IEventBus eventBus) {}

    protected LogicalSide getSide(Entity entity) {
        return entity.getEntityWorld().isRemote() ? LogicalSide.CLIENT : LogicalSide.SERVER;
    }

    @Nullable
    public PerkAttributeReader getReader() {
        return RegistriesAS.REGISTRY_PERK_ATTRIBUTE_READERS.getValue(this.getRegistryName());
    }

    @Nonnull
    public PerkAttributeModifier createModifier(float modifier, ModifierType mode) {
        if (isMultiplicative() && mode == ModifierType.ADDITION) {
            throw new IllegalArgumentException("Tried creating addition-modifier for a multiplicative-only modifier!");
        }
        return new PerkAttributeModifier(this, mode, modifier);
    }

    public void onApply(PlayerEntity player, LogicalSide side, ModifierSource source) {
        this.write(() -> {
            applicationCache.computeIfAbsent(side, s -> new HashSet<>()).add(player.getUniqueID());
        });
    }

    public void onRemove(PlayerEntity player, LogicalSide side, boolean removedCompletely, ModifierSource source) {
        if (removedCompletely) {
            this.write(() -> {
                applicationCache.getOrDefault(side, Collections.emptySet()).remove(player.getUniqueID());
            });
        }
    }

    //Called if no modifiers of this type were applied on the player, but now there is at least 1 added.
    //Called before any modifiers are actually applied!
    public void onModeApply(PlayerEntity player, ModifierType mode, LogicalSide side) {}

    //Called if no more modifiers of this type are applied on the player.
    //Called after that last modifier is removed!
    public void onModeRemove(PlayerEntity player, ModifierType mode, LogicalSide side, boolean removedCompletely) {}

    public boolean hasTypeApplied(PlayerEntity player, LogicalSide side) {
        return this.read(() -> applicationCache.getOrDefault(side, Collections.emptySet()).contains(player.getUniqueID()));
    }

    private void clear(LogicalSide side) {
        this.write(() -> {
            this.applicationCache.remove(side);
        });
    }

    public static void clearCache(LogicalSide side) {
        for (PerkAttributeType type : RegistriesAS.REGISTRY_PERK_ATTRIBUTE_TYPES) {
            type.clear(side);
        }
    }

    @Override
    public ReadWriteLock getLock() {
        return this.accessLock;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PerkAttributeType that = (PerkAttributeType) o;
        return Objects.equals(this.getRegistryName(), that.getRegistryName());
    }

    @Override
    public int hashCode() {
        return this.getRegistryName().hashCode();
    }
}
