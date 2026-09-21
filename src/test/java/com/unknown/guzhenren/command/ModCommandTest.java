package com.unknown.guzhenren.command;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.context.ParsedArgument;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.unknown.guzhenren.attachment.data.aperture.ApertureData;
import com.unknown.guzhenren.command.sub.aperture.CmdAperture;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import net.minecraft.commands.CommandSourceStack;
import org.junit.jupiter.api.Test;

class ModCommandTest {

    @Test
    void soulWritesHaveTheirOwnRootDomain() throws Exception {
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        Method register = ModCommand.class.getDeclaredMethod("register", CommandDispatcher.class);
        register.setAccessible(true);
        register.invoke(null, dispatcher);

        var root = dispatcher.getRoot().getChild("guzhenren");
        var soul = root.getChild("soul");
        assertNotNull(soul);
        assertNotNull(soul.getChild("max"));
        assertNotNull(soul.getChild("current"));
        assertNotNull(soul.getChild("refill"));
        assertNull(root.getChild("body").getChild("soul"));
    }
    @Test
    void pathWritesHaveTheirOwnRootDomain() throws Exception {
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        Method register = ModCommand.class.getDeclaredMethod("register", CommandDispatcher.class);
        register.setAccessible(true);
        register.invoke(null, dispatcher);

        var root = dispatcher.getRoot().getChild("guzhenren");
        var path = root.getChild("path");
        assertNotNull(path);
        assertNotNull(path.getChild("marks"));
        assertNotNull(path.getChild("attainment"));
        assertNotNull(path.getChild("qi"));
        assertNotNull(path.getChild("strength"));
        assertNull(root.getChild("body").getChild("path"));
    }
    @Test
    void bodyPhysiqueCommandsReplaceTheOldLifeFormCommands() throws Exception {
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        Method register = ModCommand.class.getDeclaredMethod("register", CommandDispatcher.class);
        register.setAccessible(true);
        register.invoke(null, dispatcher);

        var body = dispatcher.getRoot().getChild("guzhenren").getChild("body");
        var physique = body.getChild("physique");
        assertNotNull(physique);
        assertNotNull(physique.getChild("add"));
        assertNotNull(physique.getChild("remove"));
        assertNotNull(physique.getChild("extreme").getChild("set"));
        assertNull(body.getChild("lifeform"));
        assertNull(dispatcher.getRoot().getChild("guzhenren").getChild("aperture").getChild("physique"));
    }
    @Test
    void dimensionTravelCommandsLiveOnTheGuworldRoot() throws Exception {
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        Method register = ModCommand.class.getDeclaredMethod("register", CommandDispatcher.class);
        register.setAccessible(true);
        register.invoke(null, dispatcher);

        var guzhenren = dispatcher.getRoot().getChild("guzhenren");
        assertNull(guzhenren.getChild("enter"));
        assertNull(guzhenren.getChild("exit"));

        var guworld = dispatcher.getRoot().getChild("guworld");
        assertNotNull(guworld);
        var enter = guworld.getChild("enter");
        assertNotNull(enter);
        assertNotNull(enter.getChild("dimension"));
        assertNotNull(enter.getChild("dimension").getChild("targets"));
        var exit = guworld.getChild("exit");
        assertNotNull(exit);
        assertNotNull(exit.getChild("targets"));
    }
    @Test
    void unindexedApertureCommandDefaultsToPrimaryAperture() {
        assertEquals(ApertureData.PRIMARY,
                assertDoesNotThrow(() -> apertureOf(apertureContext(null))));
    }
    @Test
    void indexedApertureCommandUsesTheExplicitAperture() {
        assertEquals(1, assertDoesNotThrow(() -> apertureOf(apertureContext(2))));
    }
    @Test
    void indexedApertureCommandRetainsTheWriteSubtree() {
        CommandNode<CommandSourceStack> index = apertureArgument(CmdAperture.node().build());

        assertNotNull(index.getChild("rank"));
        assertNotNull(index.getChild("stage"));
        assertNotNull(index.getChild("talent"));
        assertNotNull(index.getChild("essence"));
    }
    private static CommandContext<CommandSourceStack> apertureContext(Integer index) {
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        CommandNode<CommandSourceStack> aperture = CmdAperture.node().build();
        dispatcher.getRoot().addChild(aperture);

        CommandContextBuilder<CommandSourceStack> context = new CommandContextBuilder<>(
                dispatcher, null, dispatcher.getRoot(), 0)
                .withNode(aperture, StringRange.between(0, 8));
        if (index != null) {
            CommandNode<CommandSourceStack> argument = apertureArgument(aperture);
            context.withNode(argument, StringRange.between(9, 10))
                    .withArgument(argument.getName(), new ParsedArgument<>(9, 10, index));
        }
        return context.build(index == null ? "aperture" : "aperture " + index);
    }
    private static CommandNode<CommandSourceStack> apertureArgument(CommandNode<CommandSourceStack> aperture) {
        return aperture.getChildren().stream()
                .filter(ArgumentCommandNode.class::isInstance)
                .findFirst()
                .orElseThrow();
    }
    private static int apertureOf(CommandContext<CommandSourceStack> context) throws Exception {
        Method apertureOf = CmdAperture.class.getDeclaredMethod("apertureOf", CommandContext.class);
        apertureOf.setAccessible(true);
        try {
            return (int) apertureOf.invoke(null, context);
        } catch (InvocationTargetException exception) {
            if (exception.getCause() instanceof Exception cause) throw cause;
            throw exception;
        }
    }
}
