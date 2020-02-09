package com.forgeessentials.commons.network;

import io.netty.buffer.ByteBuf;

public class Packet2Reach implements IMessage
{

    public float distance;

    public Packet2Reach()
    {
    }

    public Packet2Reach(float distance)
    {
        this.distance = distance;
    }

    @Override
    public void fromBytes(ByteBuf buf)
    {
        distance = buf.readFloat();
    }

    @Override
    public void toBytes(ByteBuf buf)
    {
        buf.writeFloat(distance);
    }

}
