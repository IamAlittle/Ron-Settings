package com.iamalittle.ronsettings.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.solegendary.reignofnether.resources.Resources;
import com.solegendary.reignofnether.resources.ResourcesClientboundPacket;
import com.solegendary.reignofnether.resources.ResourcesServerEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.commands.CommandRuntimeException;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import java.util.Collection;

public class RONModifyResourcesCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("rts-modifyresources")
                .then(Commands.argument("target", EntityArgument.players())
                        .then(Commands.literal("give")
                                .then(Commands.literal("food")
                                        .then(Commands.argument("amount", IntegerArgumentType.integer())
                                                .executes(context -> executeCommand(context, "food", "give"))
                                        )
                                )
                                .then(Commands.literal("wood")
                                        .then(Commands.argument("amount", IntegerArgumentType.integer())
                                                .executes(context -> executeCommand(context, "wood", "give"))
                                        )
                                )
                                .then(Commands.literal("ore")
                                        .then(Commands.argument("amount", IntegerArgumentType.integer())
                                                .executes(context -> executeCommand(context, "ore", "give"))
                                        )
                                )
                        )
                        .then(Commands.literal("set")
                                .then(Commands.literal("food")
                                        .then(Commands.argument("amount", IntegerArgumentType.integer())
                                                .executes(context -> executeCommand(context, "food", "set"))
                                        )
                                )
                                .then(Commands.literal("wood")
                                        .then(Commands.argument("amount", IntegerArgumentType.integer())
                                                .executes(context -> executeCommand(context, "wood", "set"))
                                        )
                                )
                                .then(Commands.literal("ore")
                                        .then(Commands.argument("amount", IntegerArgumentType.integer())
                                                .executes(context -> executeCommand(context, "ore", "set"))
                                        )
                                )
                        )
                )
        );
    }

    private static int executeCommand(CommandContext<CommandSourceStack> context, String resourceType, String operation) {
        try {
            Collection<ServerPlayer> targetPlayers = EntityArgument.getPlayers(context, "target");
            int amount = IntegerArgumentType.getInteger(context, "amount");

            if (targetPlayers.isEmpty()) {
                String playerNames = context.getArgument("target", String.class);
                throw new CommandRuntimeException(Component.literal("№:▶ " + playerNames));
            }

            for (ServerPlayer player : targetPlayers) {
                modifyPlayerResources(player, resourceType, amount, operation);

                Component playerName = Component.literal(player.getName().getString()).withStyle(Style.EMPTY.withBold(true));
                String colorCode;

                switch (resourceType) {
                    case "food":
                        colorCode = "\u00a7e"; // 黄色
                        break;
                    case "wood":
                        colorCode = "\u00a7a"; // 绿色
                        break;
                    case "ore":
                        colorCode = "\u00a7d"; // 品红色
                        break;
                    default:
                        colorCode = "\u00a7f"; // 默认颜色（白色）
                        break;
                }
                // 还是硬编码好用啊
                String framedResourceName = colorCode + "[" + resourceType + "]";
                String operationSymbol = operation.equals("give") ? "+" : "==";
                Component successMessage = Component.literal(playerName.getString() + " = " + framedResourceName + " " + operationSymbol + " " + amount);

                context.getSource().sendSuccess(successMessage, true);
            }

            return 1;
        } catch (CommandSyntaxException e) {
            throw new CommandRuntimeException(Component.literal("＠: " + e.getMessage()));
        }
    }

    private static void modifyPlayerResources(ServerPlayer player, String resourceType, int amount, String operation) {
        for (Resources resources : ResourcesServerEvents.resourcesList) {
            if (resources.ownerName.equals(player.getName().getString())) {
                int oldAmount = 0;
                switch (resourceType.toLowerCase()) {
                    case "food":
                        oldAmount = resources.food;
                        if (operation.equals("give")) {
                            resources.food = Math.max(resources.food + amount, 0);
                        } else if (operation.equals("set")) {
                            resources.food = Math.max(amount, 0);
                        }
                        break;
                    case "wood":
                        oldAmount = resources.wood;
                        if (operation.equals("give")) {
                            resources.wood = Math.max(resources.wood + amount, 0);
                        } else if (operation.equals("set")) {
                            resources.wood = Math.max(amount, 0);
                        }
                        break;
                    case "ore":
                        oldAmount = resources.ore;
                        if (operation.equals("give")) {
                            resources.ore = Math.max(resources.ore + amount, 0);
                        } else if (operation.equals("set")) {
                            resources.ore = Math.max(amount, 0);
                        }
                        break;
                }

                // 发送更新包
                ResourcesClientboundPacket.addSubtractResources(new Resources(
                        resources.ownerName,
                        resourceType.equals("food") ? (operation.equals("give") ? amount : resources.food - oldAmount) : 0,
                        resourceType.equals("wood") ? (operation.equals("give") ? amount : resources.wood - oldAmount) : 0,
                        resourceType.equals("ore") ? (operation.equals("give") ? amount : resources.ore - oldAmount) : 0
                ));

                break;
            }
        }
    }
}
