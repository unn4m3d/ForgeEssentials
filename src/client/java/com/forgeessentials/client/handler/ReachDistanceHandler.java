package com.forgeessentials.client.handler;

import java.util.function.Supplier;

import net.minecraftforge.fml.network.NetworkEvent.Context;

import com.forgeessentials.commons.network.DefaultNetHandler;
import com.forgeessentials.commons.network.IMessage;
import com.forgeessentials.commons.network.Packet2Reach;

public class ReachDistanceHandler implements DefaultNetHandler<Packet2Reach>
{

    private static float reachDistance = 0;

    @Override
    public IMessage onMessage(Packet2Reach message, Supplier<Context> ctx)
    {
        reachDistance = message.distance;
        return null;
    }

    public static float getReachDistance()
    {
        return reachDistance;
    }

}
