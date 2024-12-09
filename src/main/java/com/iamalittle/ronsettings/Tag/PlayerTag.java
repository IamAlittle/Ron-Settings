package com.iamalittle.ronsettings.Tag;

import com.solegendary.reignofnether.player.PlayerServerEvents;
import com.solegendary.reignofnether.player.RTSPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.nbt.CompoundTag;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class PlayerTag {

    // 存储玩家与RTSteam标签的映射
    private static HashMap<String, String> playerTags = new HashMap<>();
    private static int nextTagNum = 1; // 下一个可用的 RTSteam 标签编号

    // 分配 RTS 标签并将其存储
    public static void assignRTSteamTags(ServerPlayer player) {
        // 获取所有 RTS 玩家
        List<RTSPlayer> rtsPlayers = PlayerServerEvents.rtsPlayers;

        // 检查该玩家是否是 RTS 玩家
        boolean isRtsPlayer = rtsPlayers.stream().anyMatch(rtsPlayer -> rtsPlayer.name.equals(player.getName().getString()));

        CompoundTag playerDataTag = new CompoundTag();

        if (isRtsPlayer) {
            // 如果是 RTS 玩家，分配 RTSteam 标签
            String tag = playerTags.get(player.getName().getString());
            if (tag == null) {
                // 如果没有，则分配新的 RTSteam 标签
                tag = String.format("RTSteam%02d", nextTagNum++); // 格式化为两位数
                playerTags.put(player.getName().getString(), tag); // 更新标签
            }
            playerDataTag.putString("RTSteam", tag); // 添加到 NBT 数据中
        } else {
            playerDataTag.putString("RTSteam", "RTSteam00"); // 默认设置为 RTSteam00
        }

        // 将数据存储到玩家的持久化数据中
        player.getPersistentData().put("rts_player_data", playerDataTag);

        // 检查 RTS 玩家列表是否为空，如果为空则重置 nextTagNum
        if (rtsPlayers.isEmpty()) {
            nextTagNum = 1; // 重置为1
            playerTags.clear(); // 清空现存的玩家标签
        }
    }


    // 获取特定玩家的 RTSteam 变量
    public static String getRTSteamValueByPlayer(String playerName) {
        // 查找并返回指定玩家的 RTSteam 变量
        String tag = playerTags.get(playerName);
        return tag != null ? tag.split("m")[1] : null; // 返回变量值，不存在则返回 null
    }

    // 发送 RTS 玩家名称列表消息
    public static void sendRTSPlayerList(ServerPlayer player) {
        List<RTSPlayer> rtsPlayers = PlayerServerEvents.rtsPlayers;

        // 获取玩家的名称列表
        List<String> playerNames = rtsPlayers.stream()
                .map(rtsPlayer -> rtsPlayer.name)
                .collect(Collectors.toList());

        // 构造名称列表字符串
        String namesList = String.join(", ", playerNames);

        // 发送 RTS 玩家名称列表消息
        player.sendSystemMessage(Component.literal("RTS 玩家列表: " + namesList));
    }

    // 清除玩家的 RTS 标签
    public static void clearRTSTags(ServerPlayer player) {
        playerTags.remove(player.getName().getString());
        player.getPersistentData().remove("rts_player_data");
        System.out.println("已清除玩家 " + player.getName().getString() + " 的 RTS 标签。");
    }
}
