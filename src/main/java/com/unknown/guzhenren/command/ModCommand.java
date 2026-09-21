package com.unknown.guzhenren.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.unknown.guzhenren.Guzhenren;
import com.unknown.guzhenren.command.sub.CmdInfo;
import com.unknown.guzhenren.command.sub.CmdReset;
import com.unknown.guzhenren.command.sub.CmdTravel;
import com.unknown.guzhenren.command.sub.aperture.CmdAperture;
import com.unknown.guzhenren.command.sub.aperture.CmdAwaken;
import com.unknown.guzhenren.command.sub.body.CmdBody;
import com.unknown.guzhenren.command.sub.mind.CmdMind;
import com.unknown.guzhenren.command.sub.path.CmdPath;
import com.unknown.guzhenren.command.sub.soul.CmdSoul;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * The root of {@code /guzhenren} (alias {@code /gzr}), assembling every subcommand under it.
 *
 * <p>Registers the literal {@code guzhenren} with permission level 2, then attaches the eight root
 * branches ({@link com.unknown.guzhenren.command.sub.CmdInfo}, {@code CmdAwaken}, {@code CmdReset},
 * {@code CmdAperture}, {@code CmdBody}, {@code CmdSoul}, {@code CmdPath}, {@code CmdMind}) -- all
 * attachment-data commands. The
 * {@code gzr} alias is a {@code redirect} to that root, so everything typed after it parses into a
 * child context.
 *
 * <p>World-environment commands live on the separate {@code guworld} root (same permission level):
 * currently {@link com.unknown.guzhenren.command.sub.CmdTravel}'s {@code enter}/{@code exit}.
 *
 * @author Alex
 * @version 1.0.0
 * @see com.unknown.guzhenren.command.ModEnumArgument
 * @since 1.0.0
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class ModCommand {

    private ModCommand() {}
    private static final int PERMISSION_LEVEL = 2;
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }
    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralCommandNode<CommandSourceStack> root = dispatcher.register(
                Commands.literal("guzhenren")
                        .requires(source -> source.hasPermission(PERMISSION_LEVEL))
                        .then(CmdInfo.node())
                        .then(CmdAwaken.node())
                        .then(CmdReset.node())
                        .then(CmdAperture.node())
                        .then(CmdBody.node())
                        .then(CmdSoul.node())
                        .then(CmdPath.node())
                        .then(CmdMind.node()));

        dispatcher.register(Commands.literal("gzr")
                .requires(source -> source.hasPermission(PERMISSION_LEVEL))
                .redirect(root));

        dispatcher.register(Commands.literal("guworld")
                .requires(source -> source.hasPermission(PERMISSION_LEVEL))
                .then(CmdTravel.enterNode())
                .then(CmdTravel.exitNode()));
    }
}
