package com.forgeessentials.commons.network;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

import net.minecraftforge.fml.network.NetworkEvent.Context;

public interface DefaultNetHandler<REQ extends IMessage>  extends BiConsumer<REQ, Supplier<Context>>
{
    @Override
    default void accept(REQ message, Supplier<Context> ctx) {
        IMessage response = onMessage(message, ctx);
        if (response != null)
        {
            NetworkUtils.netHandler.sendToServer(response);
        }
        ctx.get().setPacketHandled(true);
    }

    IMessage onMessage(REQ message, Supplier<Context> ctx);
}
