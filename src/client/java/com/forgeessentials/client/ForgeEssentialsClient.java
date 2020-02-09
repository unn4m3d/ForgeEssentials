package com.forgeessentials.client;

import java.util.Map;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.forgeessentials.client.core.ClientProxy;
import com.forgeessentials.client.core.CommonProxy;

//@Mod(modid = ForgeEssentialsClient.MODID, name = "ForgeEssentials Client Addon", version = BuildInfo.BASE_VERSION, guiFactory = "com.forgeessentials.client.gui.forge.FEGUIFactory", useMetadata = true, dependencies = BuildInfo.DEPENDENCIES)
@Mod(ForgeEssentialsClient.MODID)
public class ForgeEssentialsClient
{
    
    public static final String MODID = "forgeessentialsclient";

    public static final Logger feclientlog = LogManager.getLogger("forgeessentials");

    protected static CommonProxy proxy = FMLEnvironment.dist.isClient() ? new ClientProxy() : new CommonProxy();

    protected static ForgeEssentialsClient instance;

    protected static boolean serverHasFE;

    /* ------------------------------------------------------------ */

    public ForgeEssentialsClient() {
        if (instance == null)
        {
            instance = this;
        }

        FMLJavaModLoadingContext.get().getModEventBus().addListener();
    }


    public boolean getServerMods(Map<String, String> map, Dist side)
    {
        if (side.isDedicatedServer())
        {
            if (map.containsKey("forgeessentials"))
            {
                serverHasFE = true;
                feclientlog.info("The server is running ForgeEssentials.");
            }
        }
        return true;
    }

    public void preInit(FMLCommonSetupEvent e)
    {
        if (FMLEnvironment.dist.isDedicatedServer())
            feclientlog.error("ForgeEssentials client does nothing on servers. You should remove it!");
        proxy.doPreInit(e);
    }

    /* ------------------------------------------------------------ */

    public static boolean serverHasFE()
    {
        return serverHasFE;
    }

}
