package com.iamalittle.ronsettings.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.unit.interfaces.AttackerUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.units.monsters.*;
import com.solegendary.reignofnether.unit.units.piglins.*;
import com.solegendary.reignofnether.unit.units.villagers.*;
import com.solegendary.reignofnether.util.Faction;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import java.lang.reflect.Field; // 引入反射类
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class UnitsParameter {
    private static final SuggestionProvider<CommandSourceStack> FACTION_SUGGESTION_PROVIDER = (context, builder) -> {
        Set<String> factions = Set.of(Faction.values()).stream()
                .filter(faction -> faction != Faction.NONE)
                .map(Faction::name)
                .collect(Collectors.toSet());
        return SharedSuggestionProvider.suggest(factions, builder);
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("rts-unitsparameter")
                .then(Commands.argument("faction", StringArgumentType.string())
                        .suggests(FACTION_SUGGESTION_PROVIDER)
                        .then(Commands.literal("present")
                                .executes(context -> {
                                    String factionString = context.getArgument("faction", String.class);
                                    Faction faction = Faction.valueOf(factionString.toUpperCase()); // 转化为Faction枚举
                                    return execute(context, faction);
                                })
                        )
                        .then(Commands.literal("all")
                                .executes(context -> {
                                    String factionString = context.getArgument("faction", String.class);
                                    Faction faction = Faction.valueOf(factionString.toUpperCase()); // 转化为Faction枚举
                                    return executeAll(context, faction);
                                }) // 新增的all命令
                        )
                )
        );
    }

    private static int execute(CommandContext<CommandSourceStack> context, Faction faction) {
        // 根据阵营获取单位列表
        StringBuilder responseMessage = new StringBuilder("阵营: " + faction.name() + " 单位参数:\n");
        List<Unit> units = getUnitsByFaction(faction);
        if (units.isEmpty()) {
            responseMessage.append("没有找到该阵营的单位。");
        } else {
            for (Unit unit : units) {
                // 处理单个单位信息
                String unitName = unit.getClass().getSimpleName().replace("Unit", "");
                responseMessage.append("\u00a7a单位(Unit): \u00a7f").append(unitName)
                        .append(", \u00a7a生命值(HP): \u00a7f").append((unit.getUnitMaxHealth()));
                if (unit instanceof AttackerUnit attackerUnit) {
                    responseMessage.append(", \u00a7a攻击力(AD): \u00a7f").append((attackerUnit.getUnitAttackDamage()))
                            .append(", \u00a7a攻击速度(ASPD): \u00a7f").append((attackerUnit.getAttacksPerSecond()))
                            .append(", \u00a7a攻击距离(AR): \u00a7f").append((attackerUnit.getAttackRange()));
                }
                responseMessage.append(", \u00a7a护甲(AC): \u00a7f").append((unit.getUnitArmorValue()))
                        .append(", \u00a7a移动速度(SPD): \u00a7f").append((unit.getMovementSpeed()))
                        .append("\n")
                        .append("\u00a7a拥有者(Owner): \u00a7f").append(unit.getOwnerName()).append("\n");

                ResourceCost resourceCost = getResourceCostForUnit(unit);
                if (resourceCost != null) {
                    StringBuilder resourceString = new StringBuilder();
                    if (resourceCost.food > 0) {
                        resourceString.append("food:").append(resourceCost.food).append("、");
                    }
                    if (resourceCost.wood > 0) {
                        resourceString.append("wood:").append(resourceCost.wood).append("、");
                    }
                    if (resourceCost.ore > 0) {
                        resourceString.append("ore:").append(resourceCost.ore).append("、");
                    }
                    if (resourceString.length() > 0) {
                        resourceString.setLength(resourceString.length() - 1);
                    }
                    responseMessage.append("所需资源: ").append(resourceString).append("\n").append("\n");
                } else {
                    responseMessage.append("所需资源: 未定义\n");
                }
            }
        }

        context.getSource().sendSuccess(Component.literal(responseMessage.toString()), true);
        return 1;
    }

    private static int executeAll(CommandContext<CommandSourceStack> context, Faction faction) {
        StringBuilder responseMessage = new StringBuilder("阵营: " + faction.name() + " 单位列表:\n");

        // 反射获取单位数据并输出
        Class<?>[] unitClasses;

        // 根据阵营选择单位类
        switch (faction) {
            case MONSTERS:
                unitClasses = new Class[]{
                        CreeperUnit.class, DrownedUnit.class, HuskUnit.class, SilverfishUnit.class,
                        SkeletonUnit.class, SpiderUnit.class, StrayUnit.class, ZoglinUnit.class,
                        ZombieUnit.class, ZombieVillagerUnit.class
                };
                break;
            case PIGLINS:
                unitClasses = new Class[]{
                        BlazeUnit.class,
                        BruteUnit.class,
                        GhastUnit.class,
                        GruntUnit.class,
                        HeadhunterUnit.class,
                        HoglinUnit.class,
                        WitherSkeletonUnit.class
                };
                break;
            case VILLAGERS:
                unitClasses = new Class[]{
                        EvokerUnit.class,
                        IronGolemUnit.class,
                        PillagerUnit.class,
                        RavagerUnit.class,
                        VillagerUnit.class,
                        VindicatorUnit.class,
                        WitchUnit.class
                };
                break;
            default:
                unitClasses = new Class[0]; // 其他阵营为空
                break;
        }

        try {
            for (Class<?> unitClass : unitClasses) {
                // 创建单位实例，使用合理的 Level 实例
                Object unitInstance = unitClass.getConstructor(EntityType.class, Level.class)
                        .newInstance(EntityType.CREEPER, context.getSource().getLevel()); // 确保传入 Level 实例

                // 初始化属性值
                float maxHealth = getFieldValue(unitClass, "maxHealth", unitInstance);
                float attackDamage = getFieldValue(unitClass, "attackDamage", unitInstance);
                float attacksPerSecond = getFieldValue(unitClass, "attacksPerSecond", unitInstance);
                float armorValue = getFieldValue(unitClass, "armorValue", unitInstance);
                float movementSpeed = getFieldValue(unitClass, "movementSpeed", unitInstance);
                float attackRange = getFieldValue(unitClass, "attackRange", unitInstance);
                float aggroRange = getFieldValue(unitClass, "aggroRange", unitInstance);

                // 获取单位的资源成本
                ResourceCost resourceCost = getResourceCostForUnit((Unit) unitInstance);
                StringBuilder resourceString = new StringBuilder();
                if (resourceCost != null) {
                    if (resourceCost.food > 0) {
                        resourceString.append("food:").append(resourceCost.food).append("、");
                    }
                    if (resourceCost.wood > 0) {
                        resourceString.append("wood:").append(resourceCost.wood).append("、");
                    }
                    if (resourceCost.ore > 0) {
                        resourceString.append("ore:").append(resourceCost.ore).append("、");
                    }
                    // 删除最后一个“、”字符
                    if (resourceString.length() > 0) {
                        resourceString.setLength(resourceString.length() - 1);
                    }
                } else {
                    resourceString.append("未定义");
                }
                // 逐一输出各单位的静态属性
                responseMessage.append(String.format("单位: %s, 最大生命值: %.2f, 攻击力: %.2f, " +
                                "攻击速度: %.2f, 护甲值: %.2f, 移动速度: %.2f, 攻击范围: %.2f, 仇恨范围: %.2f\n所需资源: %s\n\n",
                        unitClass.getSimpleName(), maxHealth, attackDamage, attacksPerSecond,
                        armorValue, movementSpeed, attackRange, aggroRange, resourceString.toString()));
            }

        } catch (Exception e) {
            e.printStackTrace();
            responseMessage.append("获取单位数据时发生错误。\n");
        }

        context.getSource().sendSuccess(Component.literal(responseMessage.toString()), true);
        return 1;
    }

    // 新增的获取字段值的方法
    private static float getFieldValue(Class<?> unitClass, String fieldName, Object unitInstance) {
        try {
            Field field = unitClass.getDeclaredField(fieldName);
            field.setAccessible(true);

            return (float) field.get(unitInstance);
        } catch (NoSuchFieldException e) {
            // 如果字段不存在，返回 null 或其他默认值
            return Float.NaN; // 或者返回 0，或者在需要的地方显示 null
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return Float.NaN; // 处理访问错误
        }
    }


    private static ResourceCost getResourceCostForUnit(Unit unit) {
        // 根据单位类型返回对应的资源成本
        if (unit == null) {
            throw new IllegalArgumentException("单位不能为空");
        }

        // 使用映射表来简化资源成本的获取
        return switch (unit.getClass().getSimpleName()) {
            case "CreeperUnit" -> ResourceCosts.CREEPER;
            case "DrownedUnit" -> ResourceCosts.DROWNED;
            case "HuskUnit" -> ResourceCosts.HUSK;
            case "PoisonSpiderUnit" -> ResourceCosts.POISON_SPIDER;
            case "SpiderUnit" -> ResourceCosts.SPIDER;
            case "SkeletonUnit" -> ResourceCosts.SKELETON;
            case "StrayUnit" -> ResourceCosts.STRAY;
            case "WardenUnit" -> ResourceCosts.WARDEN;
            case "ZombieUnit" -> ResourceCosts.ZOMBIE;
            case "ZombieVillagerUnit" -> ResourceCosts.ZOMBIE_VILLAGER;
            case "ZombiePiglinUnit" -> ResourceCosts.ZOMBIE_PIGLIN;
            case "ZoglinUnit" -> ResourceCosts.ZOGLIN;
            case "EndermanUnit" -> ResourceCosts.ENDERMAN;

            case "BlazeUnit" -> ResourceCosts.BLAZE;
            case "GhastUnit" -> ResourceCosts.GHAST;
            case "WitherSkeletonUnit" -> ResourceCosts.WITHER_SKELETON;
            case "BruteUnit" -> ResourceCosts.BRUTE;
            case "GruntUnit" -> ResourceCosts.GRUNT;
            case "HeadhunterUnit" -> ResourceCosts.HEADHUNTER;
            case "HoglinUnit" -> ResourceCosts.HOGLIN;

            case "EvokerUnit" -> ResourceCosts.EVOKER;
            case "IronGolemUnit" -> ResourceCosts.IRON_GOLEM;
            case "PillagerUnit" -> ResourceCosts.PILLAGER;
            case "RavagerUnit" -> ResourceCosts.RAVAGER;
            case "VillagerUnit" -> ResourceCosts.VILLAGER;
            case "VindicatorUnit" -> ResourceCosts.VINDICATOR;
            case "WitchUnit" -> ResourceCosts.WITCH;
            default -> null; // 或者返回一个默认成本
        };
    }

    private static List<Unit> getUnitsByFaction(Faction faction) {
        return UnitServerEvents.getAllUnits().stream()
                .filter(unit -> unit instanceof Unit && ((Unit) unit).getFaction() == faction)
                .map(unit -> (Unit) unit)
                .collect(Collectors.toList());
    }
}
