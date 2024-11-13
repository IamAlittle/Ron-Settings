package com.iamalittle.ronsettings.Tag;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.solegendary.reignofnether.player.RTSPlayer;
import com.solegendary.reignofnether.player.RTSPlayerSaveData;
import com.solegendary.reignofnether.unit.UnitSave;
import com.solegendary.reignofnether.unit.UnitSaveData;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.util.Faction;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public class UnitsTag {

    // 常量标签字符串
    private static final String VILLAGER_TAG = "villager";
    private static final String MONSTER_TAG = "monster";
    private static final String PIGLIN_TAG = "piglin";
    private static final String NO_FACTION_TAG = "No Faction Tag";
    private static final String INVALID_UNIT = "Invalid Unit";


    // 根据单位和阵营设置标签的方法
    public static String getTagByFaction(Unit unit) {
        if (unit == null) {
            return INVALID_UNIT; // 返回无效单位提示
        }

        Faction faction = unit.getFaction(); // 获取单位的阵营
        switch (faction) {
            case VILLAGERS:
                return VILLAGER_TAG; // 返回村民标签
            case MONSTERS:
                return MONSTER_TAG; // 返回怪物标签
            case PIGLINS:
                return PIGLIN_TAG; // 返回猪灵标签
            case NONE:
            default:
                return NO_FACTION_TAG; // 返回无阵营标签
        }
    }

    // 给单个单位添加标签
    public static void addTagToUnit(MinecraftServer server, Unit unit, String tag) {
        if (unit == null || tag == null) {
            return;
        }

        CommandDispatcher<CommandSourceStack> dispatcher = server.getCommands().getDispatcher();
        CommandSourceStack sourceStack = server.createCommandSourceStack(); // 创建命令源栈

        UUID unitUUID = ((Entity) unit).getUUID(); // 获取单位的UUID
        String command = String.format("tag %s add %s", unitUUID, tag);

        try {
            // 解析并执行命令，但不触发聊天日志
            dispatcher.execute(command, sourceStack.withSuppressedOutput());
        } catch (CommandSyntaxException e) {
            // 捕获并记录命令语法异常
            // 移除日志信息
        }
    }

    // 获取所有单位的方法
    public static List<Unit> getAllUnits(MinecraftServer server) {
        List<Unit> units = new ArrayList<>();

        // 获取主世界的实例，确保我们使用的是 ServerLevel
        ServerLevel world = server.getLevel(Level.OVERWORLD); // 使用适当的ResourceKey来获取世界

        if (world != null) { // 确保世界不为空
            // 使用流操作获取所有实体并筛选出Unit类型
            world.getEntities().getAll().forEach(entity -> {
                if (entity instanceof Unit) { // 检查实体是否是Unit类型
                    units.add((Unit) entity); // 添加到单位列表中
                }
            });
        }

        return units;
    }
}
