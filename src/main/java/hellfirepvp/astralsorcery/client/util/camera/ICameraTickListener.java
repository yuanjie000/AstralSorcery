/*******************************************************************************
 * HellFirePvP / Astral Sorcery 2020
 *
 * All rights reserved.
 * The source code is available on github: https://github.com/HellFirePvP/AstralSorcery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.astralsorcery.client.util.camera;

/**
 * This class is part of the Astral Sorcery Mod
 * The complete source code for this mod can be found on github.
 * Class: CameraTickDelegate
 * Created by HellFirePvP
 * Date: 02.12.2019 / 19:58
 */
public interface ICameraTickListener {

    public void onCameraTick(EntityCameraRenderView renderView, EntityClientReplacement focusedEntity);

}
