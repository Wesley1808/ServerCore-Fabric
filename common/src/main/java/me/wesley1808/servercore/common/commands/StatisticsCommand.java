package me.wesley1808.servercore.common.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import me.wesley1808.servercore.common.config.Config;
import me.wesley1808.servercore.common.config.data.CommandConfig;
import me.wesley1808.servercore.common.dynamic.DynamicManager;
import me.wesley1808.servercore.common.services.Formatter;
import me.wesley1808.servercore.common.services.Permission;
import me.wesley1808.servercore.common.utils.Util;
import me.wesley1808.servercore.common.utils.statistics.GroupBy;
import me.wesley1808.servercore.common.utils.statistics.StatisticType;
import me.wesley1808.servercore.common.utils.statistics.Statistics;
import me.wesley1808.servercore.common.utils.statistics.entry.StatisticEntry;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static net.minecraft.commands.arguments.EntityArgument.getPlayer;
import static net.minecraft.commands.arguments.EntityArgument.player;

public class StatisticsCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var statistics = literal("statistics").requires(Permission.require("command.statistics", 2));
        statistics.executes(ctx -> displayOverview(ctx.getSource()));

        for (StatisticType type : StatisticType.values()) {
            statistics.then(literal(type.getCommandFormat())
                    .executes(ctx -> display(ctx, type, 1))
                    .then(argument("page", integer(1))
                            .executes(ctx -> display(ctx, type, getInteger(ctx, "page")))
                    )

                    .then(literal("byType")
                            .executes(ctx -> display(ctx, type, GroupBy.TYPE, 1))
                            .then(argument("page", integer(1))
                                    .executes(ctx -> display(ctx, type, GroupBy.TYPE, getInteger(ctx, "page")))
                            )

                            .then(argument("player", player())
                                    .executes(ctx -> display(ctx, type, 1, getPlayer(ctx, "player")))
                                    .then(argument("page", integer(1))
                                            .executes(ctx -> display(ctx, type, getInteger(ctx, "page"), getPlayer(ctx, "player")))
                                    )
                            )
                    )

                    .then(literal("byPlayer")
                            .executes(ctx -> display(ctx, type, GroupBy.PLAYER, 1))
                            .then(argument("page", integer(1))
                                    .executes(ctx -> display(ctx, type, GroupBy.PLAYER, getInteger(ctx, "page")))
                            )
                    )
            );
        }

        dispatcher.register(statistics);
    }

    private static int displayOverview(CommandSourceStack source) {
        Statistics statistics = Statistics.getInstance(source.getServer());
        CommandConfig config = Config.get().commands();
        source.sendSuccess(() -> {
            MutableComponent component = Component.empty();
            Component title = Component.literal("Statistics").withColor(config.tertiaryValue());
            if (source.isPlayer()) {
                Formatter.addLines(component, 16, config.primaryValue(), title);
            } else {
                component.append(title);
            }

            final double mspt = DynamicManager.getInstance(source.getServer()).getAverageTickTime();
            final double tps = mspt != 0 ? Math.min((1000 / mspt), 20) : 20;
            component.append(Formatter.parse("\n<dark_gray>» <c:%s>TPS: <c:%s>%.2f</c> - MSPT: <c:%s>%.2f".formatted(
                    config.primaryHex(), config.secondaryHex(), tps,
                    config.secondaryHex(), mspt
            )));

            component.append(Formatter.parse("\n<dark_gray>» <c:%s>Total chunk count: <c:%s>%d".formatted(
                    config.primaryHex(), config.secondaryHex(), statistics.getChunkCount(true)
            )));

            component.append(Formatter.parse("\n<dark_gray>» <c:%s>Total entity count: <c:%s>%d".formatted(
                    config.primaryHex(), config.secondaryHex(), statistics.getAllEntities().size()
            )));

            component.append(Formatter.parse("\n<dark_gray>» <c:%s>Total block entity count: <c:%s>%d".formatted(
                    config.primaryHex(), config.secondaryHex(), statistics.getAllBlockEntities().size()
            )));

            return component;
        }, false);
        return Command.SINGLE_SUCCESS;
    }

    private static int display(CommandContext<CommandSourceStack> context, StatisticType type, int page) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player != null) {
            return display(context, type, GroupBy.TYPE, page, player);
        } else {
            return display(context, type, GroupBy.TYPE, page);
        }
    }

    private static int display(CommandContext<CommandSourceStack> context, StatisticType type, GroupBy groupBy, int page) {
        return display(context, type, groupBy, page, null);
    }

    private static int display(CommandContext<CommandSourceStack> context, StatisticType type, int page, @Nullable ServerPlayer player) {
        return display(context, type, GroupBy.TYPE, page, player);
    }

    private static int display(CommandContext<CommandSourceStack> context, StatisticType type, GroupBy groupBy, int page, @Nullable ServerPlayer player) {
        return switch (type) {
            case ENTITY -> displayEntities(context, groupBy, page, player);
            case BLOCK_ENTITY -> displayBlockEntities(context, groupBy, page, player);
        };
    }

    private static int displayEntities(CommandContext<CommandSourceStack> context, GroupBy groupBy, int page, @Nullable ServerPlayer player) {
        MinecraftServer server = context.getSource().getServer();
        Statistics statistics = Statistics.getInstance(server);
        Map<String, StatisticEntry<Entity>> map;
        if (groupBy == GroupBy.PLAYER) {
            map = statistics.getEntitiesByPlayer(server.getPlayerList().getPlayers());
        } else {
            map = statistics.getEntitiesByType(player == null ? statistics.getAllEntities() : statistics.getEntitiesNear(player));
        }

        displayFeedback(context, Util.sortByValue(map), StatisticType.ENTITY, groupBy, page, player);
        return Command.SINGLE_SUCCESS;
    }

    private static int displayBlockEntities(CommandContext<CommandSourceStack> context, GroupBy groupBy, int page, @Nullable ServerPlayer player) {
        MinecraftServer server = context.getSource().getServer();
        Statistics statistics = Statistics.getInstance(server);
        Map<String, StatisticEntry<TickingBlockEntity>> map;
        if (groupBy == GroupBy.PLAYER) {
            map = statistics.getBlockEntitiesByPlayer(server.getPlayerList().getPlayers());
        } else {
            map = statistics.getBlockEntitiesByType(player == null ? statistics.getAllBlockEntities() : statistics.getBlockEntitiesNear(player));
        }

        displayFeedback(context, Util.sortByValue(map), StatisticType.BLOCK_ENTITY, groupBy, page, player);
        return Command.SINGLE_SUCCESS;
    }

    private static <T> void displayFeedback(CommandContext<CommandSourceStack> context, List<Map.Entry<String, StatisticEntry<T>>> formattedEntries, StatisticType type, GroupBy groupBy, int page, @Nullable ServerPlayer player) {
        CommandSourceStack source = context.getSource();
        MutableComponent component = Component.empty();
        CommandConfig config = Config.get().commands();

        component.append(createHeader(type, groupBy, source, player, config));

        boolean success = Util.iteratePage(formattedEntries, page, 8, (entry, index) ->
                component.append(createEntry(entry, index, type, groupBy, config))
        );

        if (success) {
            source.sendSuccess(() -> component.append(createFooter(page, Util.getPage(formattedEntries.size(), 8), type, context, config)), false);
        } else if (page == 1) {
            source.sendFailure(Component.literal(String.format("No %s were found!", type.getName().toLowerCase())));
        } else {
            source.sendFailure(Component.literal("Page doesn't exist!"));
        }
    }

    private static <T> Component createEntry(Map.Entry<String, StatisticEntry<T>> entry, int index, StatisticType type, GroupBy groupBy, CommandConfig config) {
        MutableComponent component = Component.empty();

        component.append(Formatter.parse("\n<c:%s>%d. <c:%s>%s %s".formatted(
                config.secondaryHex(), index,
                config.primaryHex(), entry.getKey(), entry.getValue().formatValue()
        )));

        if (groupBy == GroupBy.PLAYER) {
            component.withStyle((style) -> style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/statistics %s byType %s".formatted(
                    type.getCommandFormat(),
                    entry.getKey()
            ))));
        }

        return component;
    }

    private static Component createHeader(StatisticType type, GroupBy groupBy, CommandSourceStack source, ServerPlayer player, CommandConfig config) {
        MutableComponent component = Component.empty();
        Component title;

        if (player == null) {
            title = Formatter.parse("<c:%s><c:%s>%s</c> by <c:%s>%s".formatted(
                    config.primaryHex(), config.tertiaryHex(), type.getName(),
                    config.tertiaryHex(), groupBy.getName()
            ));
        } else {
            title = Formatter.parse("<c:%s><c:%s>%s</c> for <c:%s>%s".formatted(
                    config.primaryHex(), config.tertiaryHex(), type.getName(),
                    config.tertiaryHex(), player.getScoreboardName()
            ));
        }

        if (source.isPlayer()) {
            Formatter.addLines(component, 16, config.primaryValue(), title);
        } else {
            component.append(title);
        }
        return component;
    }

    private static Component createFooter(int page, int pageCount, StatisticType type, CommandContext<CommandSourceStack> context, CommandConfig config) {
        MutableComponent component = Component.literal("\n");
        MutableComponent footer = Component.empty();

        String command = parseCommand(context.getInput());
        MutableComponent prevPage = Component.literal("<<").withColor(config.secondaryValue());
        if (page > 1) {
            prevPage.withStyle((style) -> style.withClickEvent(new ClickEvent(
                    ClickEvent.Action.RUN_COMMAND,
                    command.replace("%page_nr%", String.valueOf(page - 1))
            )));
        }

        MutableComponent nextPage = Component.literal(">>").withColor(config.secondaryValue());
        nextPage.withStyle((style) -> style.withClickEvent(new ClickEvent(
                ClickEvent.Action.RUN_COMMAND,
                command.replace("%page_nr%", String.valueOf(page + 1))
        )));

        footer.append(prevPage);
        footer.append(Formatter.parse(" <c:%s>Page <c:%s>%d</c> of <c:%s>%d ".formatted(
                config.primaryHex(), config.tertiaryHex(), page,
                config.tertiaryHex(), pageCount
        )));
        footer.append(nextPage);

        if (context.getSource().isPlayer()) {
            Formatter.addLines(component, type == StatisticType.BLOCK_ENTITY ? 20 : 16, config.primaryValue(), footer);
        } else {
            component.append(footer);
        }

        return component;
    }

    private static String parseCommand(String input) {
        String command = input.replaceAll("\\b\\d+\\b", "%page_nr%");
        if (!command.contains("%page_nr%")) {
            command += " %page_nr%";
        }
        return "/" + command;
    }
}
