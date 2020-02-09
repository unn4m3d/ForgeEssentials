package com.forgeessentials.client.core;

import static com.forgeessentials.client.ForgeEssentialsClient.feclientlog;

import java.io.File;
import java.net.URISyntaxException;
import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig.Type;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.network.NetworkEvent.Context;

import com.forgeessentials.client.ForgeEssentialsClient;
import com.forgeessentials.client.auth.ClientAuthNetHandler;
import com.forgeessentials.client.handler.CUIRenderrer;
import com.forgeessentials.client.handler.PermissionOverlay;
import com.forgeessentials.client.handler.QRRenderer;
import com.forgeessentials.client.handler.QuestionerKeyHandler;
import com.forgeessentials.client.handler.ReachDistanceHandler;
import com.forgeessentials.commons.BuildInfo;
import com.forgeessentials.commons.network.DefaultNetHandler;
import com.forgeessentials.commons.network.IMessage;
import com.forgeessentials.commons.network.NetworkUtils;
import com.forgeessentials.commons.network.NetworkUtils.NullMessageHandler;
import com.forgeessentials.commons.network.Packet0Handshake;
import com.forgeessentials.commons.network.Packet1SelectionUpdate;
import com.forgeessentials.commons.network.Packet2Reach;
import com.forgeessentials.commons.network.Packet3PlayerPermissions;
import com.forgeessentials.commons.network.Packet5Noclip;
import com.forgeessentials.commons.network.Packet6AuthLogin;
import com.forgeessentials.commons.network.Packet7Remote;

public class ClientProxy extends CommonProxy
{

    public static final String CONFIG_CAT = "general";

    /* ------------------------------------------------------------ */

    private static int clientTimeTicked;

    private static boolean sentHandshake = true;

    /* ------------------------------------------------------------ */

    public static boolean allowCUI, allowQRCodeRender, allowPermissionRender, allowQuestionerShortcuts, allowAuthAutoLogin;

    public static float reachDistance;

    /* ------------------------------------------------------------ */

    private static CUIRenderrer cuiRenderer = new CUIRenderrer();

    private static QRRenderer qrCodeRenderer = new QRRenderer();

    private static PermissionOverlay permissionOverlay = new PermissionOverlay();

    private ReachDistanceHandler reachDistanceHandler = new ReachDistanceHandler();

    /* ------------------------------------------------------------ */

    public ClientProxy()
    {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public void doPreInit(FMLCommonSetupEvent event)
    {
        try
        {
            BuildInfo.getBuildInfo(new File(getClass().getProtectionDomain().getCodeSource().getLocation().toURI()));
            feclientlog.info(String.format("Running ForgeEssentials client %s (%s)", BuildInfo.getFullVersion(), BuildInfo.getBuildHash()));

            // Initialize configuration
            ModLoadingContext.get().registerConfig(Type.COMMON, ClientConfig.SPEC);
            loadConfig();

            registerNetworkMessages();

            ClientCommandHandler.instance.registerCommand(new FEClientCommand());
        } catch (URISyntaxException e) {}
    }

    private void registerNetworkMessages()
    {
        // Register network messages
        NetworkUtils.registerMessageProxy(Packet0Handshake.class, 0, new NullMessageHandler<Packet0Handshake>() {
            /* dummy */
        });
        NetworkUtils.registerMessage(cuiRenderer, Packet1SelectionUpdate.class, 1);
        NetworkUtils.registerMessage(reachDistanceHandler, Packet2Reach.class, 2);
        NetworkUtils.registerMessage(permissionOverlay, Packet3PlayerPermissions.class, 3);
        NetworkUtils.registerMessage(new DefaultNetHandler<Packet5Noclip>() {
            @Override
            public IMessage onMessage(Packet5Noclip message, Supplier<Context> ctx)
            {
                PlayerEntity player = Minecraft.getInstance().player;
                if (player != null)
                {
                    player.noClip = message.getNoclip();
                }

                return null;
            }
        }, Packet5Noclip.class, 5);
        NetworkUtils.registerMessage(new ClientAuthNetHandler(), Packet6AuthLogin.class, 6);
        NetworkUtils.registerMessage(qrCodeRenderer, Packet7Remote.class, 7);
    }

    /* ------------------------------------------------------------ */

    @SubscribeEvent
    public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event)
    {
        if (event.getModID().equals(ForgeEssentialsClient.MODID))
            loadConfig();
    }

    private void loadConfig()
    {
        config.load();
        config.addCustomCategoryComment(CONFIG_CAT, "Configure ForgeEssentials Client addon features.");

        allowCUI = config.getBoolean("allowCUI", Configuration.CATEGORY_GENERAL, true, "Set to false to disable graphical selections.");
        allowQRCodeRender = config.get(Configuration.CATEGORY_GENERAL, "allowQRCodeRender", true,
                "Set to false to disable QR code rendering when you enter /remote qr.").getBoolean(true);
        allowPermissionRender = config.get(Configuration.CATEGORY_GENERAL, "allowPermRender", true,
                "Set to false to disable visual indication of block/item permissions").getBoolean(true);
        allowQuestionerShortcuts = config.get(Configuration.CATEGORY_GENERAL, "allowQuestionerShortcuts", true,
                "Use shortcut buttons to answer questions. Defaults are F8 for yes and F9 for no, change in game options menu.").getBoolean(true);
        allowAuthAutoLogin = config.get(Configuration.CATEGORY_GENERAL, "allowAuthAutoLogin", true,
                "Save tokens to automatically log in to servers using FE's Authentication Module.").getBoolean(true);
        if (!config.get(Configuration.CATEGORY_GENERAL, "versionCheck", true, "Check for newer versions of ForgeEssentials on load?").getBoolean())
            BuildInfo.checkVersion = false;

        if (allowCUI)
            MinecraftForge.EVENT_BUS.register(cuiRenderer);
        if (allowQRCodeRender)
            MinecraftForge.EVENT_BUS.register(qrCodeRenderer);
        if (allowPermissionRender)
            MinecraftForge.EVENT_BUS.register(permissionOverlay);
        if (allowQuestionerShortcuts)
            new QuestionerKeyHandler();
        BuildInfo.startVersionChecks();

        config.save();
    }

    public static Configuration getConfig()
    {
        return config;
    }

    /* ------------------------------------------------------------ */

    @SubscribeEvent
    public void connectionOpened(FMLNetworkEvent.ClientConnectedToServerEvent e)
    {
        clientTimeTicked = 0;
        sentHandshake = false;
    }

    @SubscribeEvent
    public void clientTickEvent(TickEvent.ClientTickEvent event)
    {
        clientTimeTicked++;
        if (!sentHandshake && clientTimeTicked > 20)
        {
            sentHandshake = true;
            sendClientHandshake();
        }
    }

    public void sendClientHandshake()
    {
        if (ForgeEssentialsClient.serverHasFE())
        {
            ForgeEssentialsClient.feclientlog.info("Sending Handshake Packet to FE Server");
            NetworkUtils.netHandler.sendToServer(new Packet0Handshake());
        }
        else
        {
            ForgeEssentialsClient.feclientlog.warn("Server Does not have FE, can't send initialization Packet");
        }
    }

    public static void resendHandshake()
    {
        sentHandshake = false;
    }

}
