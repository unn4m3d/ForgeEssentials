package com.forgeessentials.client.core;

import net.minecraftforge.common.ForgeConfigSpec;

import org.apache.commons.lang3.tuple.Pair;

public class ClientConfig
{
    public static final ClientConfig INSTANCE;
    public static final ForgeConfigSpec SPEC;

    static {
        final Pair<ClientConfig, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(ClientConfig::new);
        INSTANCE = specPair.getLeft();
        SPEC = specPair.getRight();
    }

    ClientConfig(ForgeConfigSpec.Builder builder) {

    }
}
