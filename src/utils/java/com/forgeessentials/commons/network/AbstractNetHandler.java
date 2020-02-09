package com.forgeessentials.commons.network;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

import net.minecraftforge.fml.network.NetworkEvent.Context;

public abstract class AbstractNetHandler<REQ extends IMessage>  implements BiConsumer<REQ, Supplier<Context>>
{
    @Override
    public void accept(REQ message, Supplier<Context> ctx) {
        IMessage response = onMessage(message, ctx);
        if (response != null)
        {
            NetworkUtils.netHandler.sendToServer(response);
        }
        ctx.get().setPacketHandled(true);
    }

    public abstract IMessage onMessage(REQ message, Supplier<Context> ctx);
}
