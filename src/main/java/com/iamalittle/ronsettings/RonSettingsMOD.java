package com.iamalittle.ronsettings;

import com.iamalittle.ronsettings.Tag.PlayerTag;
import com.mojang.logging.LogUtils;
import com.iamalittle.ronsettings.commands.RONModifyResourcesCommand;
import com.iamalittle.ronsettings.commands.UnitsParameter;
import com.iamalittle.ronsettings.Tag.UnitsTag; // 假设你的标签方法在这个类中
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent; // 导入实体加入事件
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Mod(RonSettingsMOD.MODID)
public class RonSettingsMOD {
    public static final String MODID = "ronsettings";
    private static final Logger LOGGER = LogUtils.getLogger();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public RonSettingsMOD() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::setup);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void setup(final FMLCommonSetupEvent event) {
        LOGGER.info("RonSettingsMOD setup initialized");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("Registering commands");

        // 注册命令
        RONModifyResourcesCommand.register(event.getServer().getCommands().getDispatcher());
        UnitsParameter.register(event.getServer().getCommands().getDispatcher());

        // 定期更新在线玩家的 RTSteam 标签
        scheduler.scheduleAtFixedRate(() -> {
            for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
                PlayerTag.assignRTSteamTags(player);
            }
        }, 0, 5, TimeUnit.SECONDS);
    }

    @SubscribeEvent
    public void onEntityJoin(EntityJoinLevelEvent event) {
        // 检查加入游戏的实体是否是 Unit 类型
        if (event.getEntity() instanceof Unit) {
            Unit unit = (Unit) event.getEntity();
            // 获取当前的 MinecraftServer 实例
            MinecraftServer server = event.getLevel().getServer();

            if (server != null) {
                // 获取单位的标签
                String tagValue = UnitsTag.getTagByFaction(unit);
                // 延迟执行命令以添加标签
                scheduler.schedule(() -> UnitsTag.addTagToUnit(unit,  tagValue), 1, TimeUnit.SECONDS);

                // 调用方法给单位添加RTSteam标签
                scheduler.schedule(() ->UnitsTag.addRTSteam(server, unit),1, TimeUnit.SECONDS); // 添加这一行
            }
        } else if (event.getEntity() instanceof ServerPlayer) {
            // 如果实体是玩家，仅分配 RTS 标签，而不发送消息
            PlayerTag.assignRTSteamTags((ServerPlayer) event.getEntity());
        }
    }

    @SubscribeEvent
    public void onRegisterCommand(RegisterCommandsEvent evt) {
        evt.getDispatcher().register(Commands.literal("rts-refreshRTSteamTagsList")
                .executes(command -> {
                    ServerPlayer player = command.getSource().getPlayerOrException();
                    PlayerTag.assignRTSteamTags(player); // 调用分配方法
                    PlayerTag.sendRTSPlayerList(player); // 向当前玩家发送 RTS 玩家列表消息
                    return 1;
                }));
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        // 关闭调度器
        if (!scheduler.isShutdown()) {
            scheduler.shutdown(); // 关闭调度器
        }
    }
}
