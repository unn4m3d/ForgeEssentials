package com.forgeessentials.commons.network;

import io.netty.buffer.ByteBuf;

public interface IMessage
{
    void fromBytes(ByteBuf buf);

    void toBytes(ByteBuf buf);
}
