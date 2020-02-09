package com.forgeessentials.client.auth;

import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraftforge.fml.network.NetworkEvent.Context;

import com.forgeessentials.client.core.ClientProxy;
import com.forgeessentials.commons.network.AbstractNetHandler;
import com.forgeessentials.commons.network.IMessage;
import com.forgeessentials.commons.network.Packet6AuthLogin;

public class ClientAuthNetHandler extends AbstractNetHandler<Packet6AuthLogin>
{
    public IMessage onMessage(Packet6AuthLogin message, Supplier<Context> ctx)
    {
        // send empty response if the client has disabled this
        if (!ClientProxy.allowAuthAutoLogin)
            return new Packet6AuthLogin(1, "");

        AuthAutoLogin.KEYSTORE = AuthAutoLogin.load();
        ServerData serverData = Minecraft.getInstance().getCurrentServerData();
        if (serverData != null)
        {
            switch (message.mode)
            {
            case 0:
                return new Packet6AuthLogin(1, AuthAutoLogin.getKey(serverData.serverIP));
            case 2:
                AuthAutoLogin.setKey(serverData.serverIP, message.hash);
                break;
            default:
                break;
            }
        }
        return null;
    }
}