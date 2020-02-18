package com.forgeessentials.permissions.commands;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;

import com.forgeessentials.api.APIRegistry;
import com.forgeessentials.api.UserIdent;
import com.forgeessentials.api.permissions.AreaZone;
import com.forgeessentials.api.permissions.FEPermissions;
import com.forgeessentials.api.permissions.WorldZone;
import com.forgeessentials.api.permissions.Zone;
import com.forgeessentials.commons.selections.AreaBase;
import com.forgeessentials.commons.selections.AreaShape;
import com.forgeessentials.core.commands.ParserCommandBase;
import com.forgeessentials.core.misc.TranslatedCommandException;
import com.forgeessentials.protection.ModuleProtection;
import com.forgeessentials.util.CommandParserArgs;
import com.forgeessentials.util.events.EventCancelledException;
import com.forgeessentials.util.output.ChatOutputHandler;
import com.forgeessentials.util.selections.SelectionHandler;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.context.AreaContext;

public class CommandRegion extends ParserCommandBase
{
    public static final String PERM_NODE = "fe.region";
    public static final String PERM_CLAIM = PERM_NODE + ".claim";
    public static final String PERM_ADDMEMBER = PERM_NODE + ".addmember";
    public static final String PERM_ADDMEMBER_OWN = PERM_ADDMEMBER + ".own";
    public static final String PERM_REMOVEMEMBER = PERM_NODE + ".removemember";
    public static final String PERM_REMOVEMEMBER_OWN = PERM_REMOVEMEMBER + ".own";
    public static final String PERM_FLAG = PERM_NODE + ".flag";
    public static final String PERM_RG_FLAG = PERM_NODE + ".rgflag";
    // TODO : Per-flag perms
    public static final String PERM_FLAG_OWN = PERM_FLAG + ".own";
    public static final String PERM_INFO = PERM_NODE + ".info";
    public static final String PERM_INFO_OWN = PERM_INFO + ".own";

    public static final String PERM_OWNER = PERM_NODE + ".owner";
    public static final String PERM_MEMBER = PERM_NODE + ".member";

    public static interface Flag 
    {
        public void setDefault(AreaZone zone);
        public void set(AreaZone zone, UserIdent ident);
        public Flag copy(String value);
        public void setForMembers(AreaZone zone);
        public String toString(String value);
        public String toColouredString(String value);
        public String toColouredString(AreaZone zone);
        public String getName();
    }

    public static class InteractionFlag implements Flag
    {
        private String permission;
        private String name;
        private boolean value;

        public InteractionFlag(String name, String permission, boolean value)
        {
            this.name = name;
            this.permission = permission;
            this.value = value;
        }

        public static boolean parseValue(String value)
        {
            if(value == null) return false;
            return value.equalsIgnoreCase("allow") || value.equalsIgnoreCase("true");
        }

        public InteractionFlag(String name, String permission, String value)
        {
            this.name = name;
            this.permission = permission;
            this.value = parseValue(value);
        }

        public void setDefault(AreaZone zone)
        {
            zone.setGroupPermission(Zone.GROUP_DEFAULT, permission, value);
            zone.setGroupPermission(Zone.GROUP_DEFAULT, PERM_RG_FLAG + "." + name, value);
        }

        public void set(AreaZone zone, UserIdent ident)
        {
            zone.setPlayerPermission(ident, permission, true);
        }

        public Flag copy(String value)
        {
            return new InteractionFlag(name, permission, value);
        }

        public void setForMembers(AreaZone zone)
        {
            List<UserIdent> members = zone.searchForPermission(PERM_MEMBER);
            for(UserIdent member : members)
            {
                zone.setPlayerPermission(member, permission, true);
            }
        }
        
        public String getName()
        {
            return name;
        }

        public String toString(String value)
        {
            if(parseValue(value))  return "+" + name;
            else return "-" + name;
        }

        public String toColouredString(String value)
        {
            String color = parseValue(value) ? "&a" : "&c";
            return color + toString(value) + "&r";
        }

        public String toColouredString(AreaZone zone)
        {
            return toColouredString(zone.checkGroupPermission(Zone.GROUP_DEFAULT, PERM_RG_FLAG + "." + name).toString());
        }
    }

    public static Map<String, Flag> flags = new HashMap<>();

    public static void addFlag(Flag flag)
    {
        flags.put(flag.getName(), flag);
    }
    static {
        addFlag(new InteractionFlag("break", ModuleProtection.PERM_BREAK + ".*", false));
        addFlag(new InteractionFlag("place", ModuleProtection.PERM_PLACE + ".*", false));
        addFlag(new InteractionFlag("use", ModuleProtection.PERM_INTERACT + ".*", false));
    }

    public static void registerPermissions()
    {
        APIRegistry.perms.registerPermission(PERM_CLAIM, DefaultPermissionLevel.ALL, "Claim regions");
        APIRegistry.perms.registerPermission(PERM_INFO, DefaultPermissionLevel.OP, "See info about all regions");
        APIRegistry.perms.registerPermission(PERM_INFO_OWN, DefaultPermissionLevel.ALL, "See info about your regions");
    }

    @Override
    public String getName()
    {
        return "region";
    }

    @Override
    public String getUsage(ICommandSender sender)
    {
        return "/region : Manage players' regions";
    }

    @Override
    public String[] getDefaultAliases()
    {
        return new String[]{"rg"};
    }

    @Override
    public String getPermissionNode()
    {
        return PERM_NODE;
    }

    @Override
    public DefaultPermissionLevel getPermissionLevel()
    {
        return DefaultPermissionLevel.ALL;
    }

    @Override
    public boolean canConsoleUseCommand()
    {
        return false;
    }

    @Override
    public void parse(CommandParserArgs args) throws CommandException
    {
        if(args.isEmpty())
        {
            args.warn("/rg <claim|sel|info|flag> <...>");
            return;
        }

        args.tabComplete("claim", "sel", "flag", "addowner", "removeowner", "addmember", "removemember", "delete");
        
        String arg = args.remove().toLowerCase();
        switch(arg)
        {
        case "claim":
            parseClaim(args);
            break;
        case "sel":
            CommandZone.parseSelect(args);
            break;
        case "flag":
            parseFlag(args);
            break;
        case "info":
            parseInfo(args);
            break;
        default:
            throw new TranslatedCommandException(FEPermissions.MSG_UNKNOWN_SUBCOMMAND);
        }
    }

    private static void parseInfo(CommandParserArgs args) throws CommandException
    {
        if (args.isEmpty())
        {
            args.warn("/rg info <region>");
            return;
        }

        CommandZone.tabCompleteArea(args);
        String rgName = args.remove();

        WorldZone worldZone = args.getWorldZone();
        AreaZone area = CommandZone.getAreaZone(worldZone, rgName);

        args.checkPermission(PERM_INFO_OWN);

        if(area == null)
            throw new TranslatedCommandException("Region %s does not exist", rgName);

        if(args.hasPlayer())
        {
            UserIdent ident = UserIdent.get(args.senderPlayer);
            if(!area.checkPlayerPermission(ident, PERM_OWNER))
                args.checkPermission(PERM_INFO);
        }

        AreaBase abase = area.getArea();
        args.confirm("Region %s", area.getName());
        args.notify("Flags :");
        for(Flag flag : flags.values())
        {
            ChatOutputHandler.sendMessage(args.sender, ChatOutputHandler.formatColors(flag.toColouredString(area)));
        }
    }

    private static void parseFlag(CommandParserArgs args) throws CommandException
    {
        if (args.isEmpty())
        {
            args.warn("/rg flag <region> <flag> <value>");
            StringBuilder builder = new StringBuilder();
            for(String key : flags.keySet())
            {
                builder.append(key);
                builder.append(" ");
            }        
            args.confirm("Known flags: %s", builder.toString());
            return;
        }

        CommandZone.tabCompleteArea(args);

        args.checkPermission(PERM_FLAG_OWN);
        
        String rgName = args.remove();

        if(args.isTabCompletion)
            return;

        WorldZone worldZone = args.getWorldZone();
        AreaZone area = CommandZone.getAreaZone(worldZone, rgName);

        if(area == null)
            throw new TranslatedCommandException("Region %s does not exist", rgName);

        if(args.hasPlayer())
        {
            UserIdent ident = UserIdent.get(args.senderPlayer);
            if(!area.checkPlayerPermission(ident, PERM_OWNER))
                args.checkPermission(PERM_FLAG);
        }
        args.tabComplete(flags.keySet());

        if(args.isTabCompletion)
            return;

        String flagName = args.remove();

        if(!flags.containsKey(flagName))
            throw new TranslatedCommandException("Flag %s does not exist", flagName);
        
        if(args.isEmpty())
        {
            args.warn("/rg flag <region> <flag> <value>");
        }

        String value = args.remove();

        flags.get(flagName).copy(value).setDefault(area);

        args.confirm("Flag set successfully");
    }

    private static void parseClaim(CommandParserArgs args) throws CommandException
    {
        args.checkPermission(PERM_CLAIM);

        if(args.isEmpty())
            throw new TranslatedCommandException(FEPermissions.MSG_NOT_ENOUGH_ARGUMENTS);
        
        String rgName = args.remove();
        WorldZone worldZone = args.getWorldZone();
        AreaZone area = CommandZone.getAreaZone(worldZone, rgName);

        if (area != null)
            throw new TranslatedCommandException(String.format("Region %s exists", rgName));

        if (args.isTabCompletion)
            return;
        
        AreaBase selection = SelectionHandler.getSelection(args.senderPlayer);
        if (selection == null)
            throw new TranslatedCommandException("No selection available. Please select a region first.");

        if (args.hasPlayer())
        {
            args.context = new AreaContext(args.senderPlayer, selection.toAxisAlignedBB());
            args.checkPermission(PERM_CLAIM);
        }

        try 
        {
            area = new AreaZone(worldZone, rgName, selection);
            area.setShape(AreaShape.BOX);
            args.confirm("Region %s has been claimed successfully.", rgName);
            setDefaultRegionPerms(args, area);
        }
        catch(EventCancelledException e)
        {
            throw new TranslatedCommandException("Claiming %s has been cancelled", rgName);
        }
    }

    public static void setDefaultRegionPerms(CommandParserArgs args, AreaZone area) throws CommandException
    {
        if (args.hasPlayer())
            setDefaultPermsForOwner(area, UserIdent.get(args.senderPlayer));

        while(!args.isEmpty())
        {
            String username = args.remove();
            UserIdent ident = UserIdent.get((UUID)null, username);
            if(ident == null)
                args.confirm("User %s does not exist, skipping", username);
            setDefaultPermsForOwner(area, ident);
        }
    }

    public static void setDefaultPermsForMember(AreaZone area, UserIdent ident)
    {
        for(Entry<String, Flag> flag : flags.entrySet())
        {
            flag.getValue().set(area, ident);
        }
        area.setPlayerPermission(ident, PERM_MEMBER, true);
    }

    public static void setDefaultPermsForOwner(AreaZone area, UserIdent ident)
    {
        setDefaultPermsForMember(area, ident);
        
        area.setPlayerPermission(ident, PERM_OWNER, true);
    }


}