package com.forgeessentials.client.handler;

import java.io.IOException;
import java.net.URL;
import java.util.function.Supplier;

import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.network.NetworkEvent.Context;

import org.lwjgl.opengl.GL11;

import com.forgeessentials.commons.network.DefaultNetHandler;
import com.forgeessentials.commons.network.IMessage;
import com.forgeessentials.commons.network.Packet7Remote;

@OnlyIn(Dist.CLIENT)
public class QRRenderer implements DefaultNetHandler<Packet7Remote>
{

    private static ResourceLocation qrCode;

    @SubscribeEvent
    public void render(RenderWorldLastEvent event)
    {
        EntityPlayer player = FMLClientHandler.instance().getClient().player;
        if (player == null)
            return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.currentScreen != null && qrCode != null)
        {
            mc.entityRenderer.setupOverlayRendering();
            mc.renderEngine.bindTexture(qrCode);
            GL11.glPushMatrix();
            GL11.glScalef(0.5F, 0.5F, 0);
            GL11.glColor4f(1, 1, 1, 1);
            mc.currentScreen.drawTexturedModalRect(0, 0, 0, 0, 256, 256);
            GL11.glPopMatrix();
        }
        else if (qrCode != null)
        {
            mc.renderEngine.deleteTexture(qrCode);
            qrCode = null;
        }
    }

    @Override
    public IMessage onMessage(Packet7Remote message, Supplier<Context> ctx)
    {
        try
        {
            DynamicTexture qrCodeTexture = new DynamicTexture(ImageIO.read(new URL(message.link)));
            qrCode = Minecraft.getInstance().renderEngine.getDynamicTextureLocation("qr_code", qrCodeTexture);
        }
        catch (IOException e)
        {
            StringTextComponent cmsg = new StringTextComponent("Could not load QR Code. " + e.getMessage());
            cmsg.getStyle().setColor(TextFormatting.RED);
            PlayerEntity player = Minecraft.getInstance().player;
            if (player != null)
            {
                player.sendMessage(cmsg);
            }
            e.printStackTrace();
        }
        return null;
    }
    
}
