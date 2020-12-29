/*******************************************************************************
 * HellFirePvP / Astral Sorcery 2020
 *
 * All rights reserved.
 * The source code is available on github: https://github.com/HellFirePvP/AstralSorcery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.astralsorcery.common.starlight.network;

import hellfirepvp.astralsorcery.common.starlight.transmission.IPrismTransmissionNode;
import hellfirepvp.observerlib.common.util.tick.ITickHandler;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.event.TickEvent;

import java.util.*;

/**
 * This class is part of the Astral Sorcery Mod
 * The complete source code for this mod can be found on github.
 * Class: StarlightUpdateHandler
 * Created by HellFirePvP
 * Date: 01.10.2016 / 01:41
 */
public class StarlightUpdateHandler implements ITickHandler {

    private static final StarlightUpdateHandler instance = new StarlightUpdateHandler();
    private static final Map<RegistryKey<World>, List<IPrismTransmissionNode>> updateRequired = new HashMap<>();
    private static final Object accessLock = new Object();

    private StarlightUpdateHandler() {}

    public static StarlightUpdateHandler getInstance() {
        return instance;
    }

    @Override
    public void tick(TickEvent.Type type, Object... context) {
        World world = (World) context[0];
        if (world.isRemote()) {
            return;
        }

        List<IPrismTransmissionNode> nodes = getNodes(world);
        synchronized (accessLock) {
            for (IPrismTransmissionNode node : nodes) {
                node.update(world);
            }
        }
    }

    private List<IPrismTransmissionNode> getNodes(World world) {
        return updateRequired.computeIfAbsent(world.getDimensionKey(), k -> new LinkedList<>());
    }

    public void removeNode(World world, IPrismTransmissionNode node) {
        synchronized (accessLock) {
            getNodes(world).remove(node);
        }
    }

    public void addNode(World world, IPrismTransmissionNode node) {
        synchronized (accessLock) {
            getNodes(world).add(node);
        }
    }

    public void informWorldLoad(World world) {
        synchronized (accessLock) {
            updateRequired.remove(world.getDimensionKey());
        }
    }

    public void clearServer() {
        synchronized (accessLock) {
            updateRequired.clear();
        }
    }

    @Override
    public EnumSet<TickEvent.Type> getHandledTypes() {
        return EnumSet.of(TickEvent.Type.WORLD);
    }

    @Override
    public boolean canFire(TickEvent.Phase phase) {
        return phase == TickEvent.Phase.END;
    }

    @Override
    public String getName() {
        return "Starlight Update Handler";
    }

}
