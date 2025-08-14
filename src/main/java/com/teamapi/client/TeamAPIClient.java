package com.teamapi.client;

import com.teamapi.TeamAPI;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.network.PacketByteBuf;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客户端队伍API实现
 */
@Environment(EnvType.CLIENT)
public final class TeamAPIClient implements ClientModInitializer {
    private static final Map<String, String> teamCache = new ConcurrentHashMap<>();
    private static KeyBinding openTeamUIKey;
    private static boolean initialized = false;

    @Override
    public void onInitializeClient() {
        if (initialized) return;
        initialized = true;

        registerKeyBindings();
        registerClientNetworkHandlers();
    }

    private void registerKeyBindings() {
        openTeamUIKey = KeyBindingHelper.registerKeyBinding(new  KeyBinding(
                "key.teamapi.open_ui",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                "category.teamapi.general"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client  -> {
            while (openTeamUIKey.wasPressed())  {
                if (client.player  != null) {
                    requestTeamData();
                    TeamUIScreen.open(teamId  ->
                            TeamAPI.getInstance().getTeamDisplayName(teamId)
                    );
                }
            }
        });
    }

    private void registerClientNetworkHandlers() {
        ClientPlayNetworking.registerGlobalReceiver(TeamAPI.SYNC_TEAMS,  (client, handler, buf, responseSender) -> {
            Map<String, String> newData = decodeTeamData(buf);
            updateTeamCache(newData);

            client.execute(()  -> {
                if (client.currentScreen  instanceof TeamUIScreen) {
                    ((TeamUIScreen) client.currentScreen).updateTeamData(newData);
                }
            });
        });
    }

    @NotNull
    private static Map<String, String> decodeTeamData(PacketByteBuf buf) {
        Map<String, String> newData = new HashMap<>();
        int count = buf.readInt();
        for (int i = 0; i < count; i++) {
            newData.put(buf.readString(),  buf.readString());
        }
        return newData;
    }

    private static void updateTeamCache(@NotNull Map<String, String> newData) {
        teamCache.clear();
        teamCache.putAll(newData);
    }

    /**
     * 向服务器请求最新的队伍数据
     */
    public static void requestTeamData() {
        if (MinecraftClient.getInstance().getNetworkHandler()  != null) {
            ClientPlayNetworking.send(TeamAPI.REQUEST_TEAMS,  PacketByteBufs.empty());
        }
    }

    /**
     * 获取客户端缓存的队伍数据（防御性拷贝）
     */
    @NotNull
    public static Map<String, String> getTeamCache() {
        return new HashMap<>(teamCache);
    }

    /**
     * 获取队伍UI快捷键绑定
     */
    @NotNull
    public static KeyBinding getOpenTeamUIKey() {
        return openTeamUIKey;
    }
}