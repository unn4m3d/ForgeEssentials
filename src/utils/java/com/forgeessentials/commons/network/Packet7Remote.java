package com.forgeessentials.commons.network;

import java.nio.charset.StandardCharsets;

import net.minecraftforge.fml.common.network.ByteBufUtils;
import io.netty.buffer.ByteBuf;

public class Packet7Remote implements IMessage
{
    public String link;

    public Packet7Remote() {}

    public Packet7Remote(String link)
    {
        this.link = link;
    }

    @Override
    public void fromBytes(ByteBuf buf)
    {
        link = buf.readCharSequence(buf.readInt(), StandardCharsets.UTF_8).toString();
    }

    @Override
    public void toBytes(ByteBuf buf)
    {
        int pos = buf.writerIndex();
        buf.writeInt(0);
        int written = buf.writeCharSequence(link, StandardCharsets.UTF_8);
        buf.setInt(pos, written);
    }
}
