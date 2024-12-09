package com.iamalittle.ronsettings.Tag;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.solegendary.reignofnether.player.PlayerServerEvents;
import com.solegendary.reignofnether.player.RTSPlayer;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.util.Faction;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
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

    // 给单个单位添加阵营标签
    public static void addTagToUnit(Unit unit,  String tagValue) {
        if (unit == null || tagValue == null) {
            return; // 确保单位和标签键值不为空
        }

        // 获取 rts_units_data，如果不存在则初始化
        CompoundTag rtsUnitsDataTag = new CompoundTag();

        // 将阵营标签添加到 rts_units_data 标签中
        rtsUnitsDataTag.putString("Faction", tagValue); // 使用提供的标签键和值

        // 存储更新后的 rts_units_data 标签回单位的持久化数据中
        ((Entity) unit).getPersistentData().put("rts_units_faction", rtsUnitsDataTag);

        // 打印调试信息
        System.out.println("已将阵营标签 " + "Faction" + " 的值 " + tagValue + " 添加到单位 " + ((Entity) unit).getUUID() + " 的持久化数据中。");
    }

    // 添加RTSteam标签到单位
    public static void addRTSteam(MinecraftServer server, Unit unit) {
        if (unit == null) {
            return;
        }

        String ownerName = unit.getOwnerName(); // 获取单位的拥有者名字
        if (ownerName != null && !ownerName.isEmpty()) {
            String rtsteamValue = PlayerTag.getRTSteamValueByPlayer(ownerName); // 调用获取变量方法

            if (rtsteamValue != null) {
                // 创建 RTSteam 标签
                String rtsteamTag = "RTSteam" +  rtsteamValue;

                // 将标签应用到单位的 NBT 数据
                CompoundTag unitDataTag = new CompoundTag();
                unitDataTag.putString("RTSteam", "RTSteam"+rtsteamValue);
                // 将数据存储到单位的持久化数据（或直接在单位的 NBT 数据上）
                ((Entity) unit).getPersistentData().put("rts_units_team", unitDataTag);

                System.out.println("已将 RTSteam 标签: " + rtsteamTag + " 应用到拥有者为: " + ownerName + " 的单位。");
            } else {
                System.out.println("没有为拥有者: " + ownerName + " 找到对应的 RTSteam 变量。");
            }
        } else {
            System.out.println("单位没有拥有者。");
        }
    }



    // 获取所有单位的方法
    public static List<Unit> getAllUnits(MinecraftServer server) {
        List<Unit> units = new ArrayList<>();

        // 获取主世界的实例，确保我们使用的是 ServerLevel
        ServerLevel world = server.getLevel(Level.OVERWORLD); // 使用适当的 ResourceKey 来获取世界

        if (world != null) { // 确保世界不为空
            // 使用流操作获取所有实体并筛选出 Unit 类型
            world.getEntities().getAll().forEach(entity -> {
                if (entity instanceof Unit) { // 检查实体是否是 Unit 类型
                    units.add((Unit) entity); // 添加到单位列表中
                }
            });
        }

        return units;
    }
}
