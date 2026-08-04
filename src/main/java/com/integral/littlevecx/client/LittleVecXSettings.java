package com.integral.littlevecx.client;

import com.creativemd.creativecore.CreativeCore;
import com.creativemd.creativecore.common.config.ConfigModGuiFactory;
import com.creativemd.creativecore.common.config.gui.SubGuiConfig;
import com.creativemd.creativecore.common.config.holder.CreativeConfigRegistry;
import com.creativemd.creativecore.common.config.holder.ICreativeConfigHolder;
import com.creativemd.creativecore.common.gui.mc.GuiScreenSub;
import com.creativemd.creativecore.common.utils.mc.JsonUtils;
import com.integral.littlevecx.LittleVecXMod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.relauncher.Side;

/** Makes LittleVecX's CreativeCore settings available from Forge's Mods menu. */
public class LittleVecXSettings extends ConfigModGuiFactory {

    @Override
    public String modid() {
        return LittleVecXMod.MODID;
    }

    @Override
    public ICreativeConfigHolder getHolder() {
        Object value = CreativeConfigRegistry.ROOT.get(modid());
        if (value instanceof ICreativeConfigHolder) {
            ICreativeConfigHolder holder = (ICreativeConfigHolder) value;
            if (!holder.isEmpty(Side.SERVER))
                return holder;
        }
        return null;
    }

    @Override
    public GuiScreen createConfigGui(GuiScreen parentScreen) {
        ICreativeConfigHolder holder = getHolder();
        if (holder == null)
            return null;

        if (Minecraft.getMinecraft().player == null)
            CreativeCore.configHandler.load(modid(), Side.SERVER);
        return new GuiScreenSub(parentScreen, new LittleVecXServerConfigGui(holder));
    }

    /**
     * The Mods menu is also available before a world is open. In that case no
     * server connection exists, so persist the local server config directly.
     */
    private static class LittleVecXServerConfigGui extends SubGuiConfig {

        private LittleVecXServerConfigGui(ICreativeConfigHolder holder) {
            super(holder, Side.SERVER);
        }

        @Override
        public void sendUpdate() {
            if (Minecraft.getMinecraft().player == null) {
                rootHolder.load(false, true, JsonUtils.get(ROOT, rootHolder.path()), Side.SERVER);
                CreativeCore.configHandler.save(Side.SERVER);
            } else
                super.sendUpdate();
        }
    }
}
