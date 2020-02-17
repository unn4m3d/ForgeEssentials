package com.forgeessentials.permissions.commands;

import java.util.UUID;

import com.forgeessentials.api.UserIdent;
import com.forgeessentials.api.permissions.AreaZone;
import com.forgeessentials.api.permissions.FEPermissions;
import com.forgeessentials.api.permissions.WorldZone;
import com.forgeessentials.commons.selections.AreaBase;
import com.forgeessentials.commons.selections.AreaShape;
import com.forgeessentials.core.commands.ParserCommandBase;
import com.forgeessentials.core.misc.TranslatedCommandException;
import com.forgeessentials.protection.ModuleProtection;
import com.forgeessentials.util.CommandParserArgs;
import com.forgeessentials.util.events.EventCancelledException;
import com.forgeessentials.util.selections.SelectionHandler;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.context.AreaContext;

public class CommandRegion extends ParserCommandBase
{
    public static final String PERM_NODE = "fe.region";
    public static final String PERM_CLAIM = PERM_NODE + ".claim";

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
            args.confirm("/rg <claim|sel|info|flag|addowner|removeowner|addmember|removemember|delete>");
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
        default:
            throw new TranslatedCommandException(FEPermissions.MSG_UNKNOWN_SUBCOMMAND);
        }
    }

    private void parseClaim(CommandParserArgs args) throws CommandException
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

    public void setDefaultRegionPerms(CommandParserArgs args, AreaZone area) throws CommandException
    {
        // TODO: Configurable permissions
        area.setGroupPermission("_ALL_", ModuleProtection.PERM_BREAK, false);
        area.setGroupPermission("_ALL_", ModuleProtection.PERM_PLACE, false);
        
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

    public void setDefaultPermsForOwner(AreaZone area, UserIdent ident)
    {
        area.setPlayerPermission(ident, ModuleProtection.PERM_BREAK, true);
        area.setPlayerPermission(ident, ModuleProtection.PERM_PLACE, true);
    }


}