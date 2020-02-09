package com.forgeessentials.commons.network;

import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;

public class NetworkUtils
{

    private static final String PROTOCOL_VERSION = "1";

    public static SimpleChannel netHandler = NetworkRegistry.newSimpleChannel(new ResourceLocation("forgeessentials"), () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    private static Set<Integer> registeredMessages = new HashSet<>();

    public static class NullMessageHandler<REQ extends IMessage> implements BiConsumer<REQ, Supplier<NetworkEvent.Context>>
    {
        @Override
        public void accept(REQ message, Supplier<NetworkEvent.Context> ctx)
        {
            ctx.get().setPacketHandled(true);
        }

    }

    public static <REQ extends IMessage> void registerMessageProxy(Class<REQ> requestMessageType, int discriminator, NullMessageHandler<REQ> nmh)
    {
        if (!registeredMessages.contains(discriminator))
            netHandler.registerMessage(discriminator, requestMessageType, IMessage::toBytes, packetBuffer -> {
                try
                {
                    REQ r = requestMessageType.newInstance();
                    r.fromBytes(packetBuffer);
                    return r;
                }
                catch (InstantiationException e)
                {
                    e.printStackTrace();
                }
                catch (IllegalAccessException e)
                {
                    e.printStackTrace();
                }
                return null;
            }, nmh);
    }

    public static <REQ extends IMessage> void registerMessage(BiConsumer<REQ, Supplier<NetworkEvent.Context>> messageHandler, Class<REQ> requestMessageType, int discriminator)
    {
        netHandler.registerMessage(discriminator, requestMessageType, IMessage::toBytes, packetBuffer -> {
            try
            {
                REQ r = requestMessageType.newInstance();
                r.fromBytes(packetBuffer);
                return r;
            }
            catch (InstantiationException e)
            {
                e.printStackTrace();
            }
            catch (IllegalAccessException e)
            {
                e.printStackTrace();
            }
            return null;
        }, messageHandler);
        registeredMessages.add(discriminator);
    }

}
