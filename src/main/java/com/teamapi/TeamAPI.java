package com.teamapi;

import com.teamapi.config.TeamConfig;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TeamAPI implements ModInitializer {
	private static TeamAPI instance;
	private static TeamConfig config;

	// 网络通信标识符
	public static final Identifier SYNC_TEAMS = new Identifier("teamapi", "sync_teams");
	public static final Identifier REQUEST_TEAMS = new Identifier("teamapi", "request_teams");
	public static final Identifier TEAM_CHANGE = new Identifier("teamapi", "team_change");

	// 默认队伍ID
	public static final String RED_TEAM = "red";
	public static final String BLUE_TEAM = "blue";

	private final Map<UUID, PlayerTeamData> playerDataMap = new ConcurrentHashMap<>();
	private final Set<TeamChangeListener> teamChangeListeners = new HashSet<>();

	@Override
	public void onInitialize() {
		AutoConfig.register(TeamConfig.class,  GsonConfigSerializer::new);
		config = AutoConfig.getConfigHolder(TeamConfig.class).getConfig();
		instance = this;
		registerNetworkHandlers();
	}

	private void registerNetworkHandlers() {
		// 处理队伍数据请求
		ServerPlayNetworking.registerGlobalReceiver(REQUEST_TEAMS,  (server, player, handler, buf, responseSender) ->
				syncTeamDataToClient(player)
		);

		// 处理队伍变更请求
		ServerPlayNetworking.registerGlobalReceiver(TEAM_CHANGE,  (server, player, handler, buf, responseSender) -> {
			String targetName = buf.readString();
			String teamId = buf.readString();

			server.execute(()  -> handleTeamChangeRequest(player, targetName, teamId));
		});
	}

	private void handleTeamChangeRequest(ServerPlayerEntity requester, String targetName, String teamId) {
		if (!requester.hasPermissionLevel(2)) {
			requester.sendMessage(Text.literal("你没有权限更改队伍").formatted(Formatting.RED), false);
			return;
		}

		ServerPlayerEntity target = Objects.requireNonNull(requester.getServer()).getPlayerManager().getPlayer(targetName);
		if (target == null) {
			requester.sendMessage(Text.literal("玩家不在线: ").append(targetName).formatted(Formatting.RED), false);
			return;
		}

		setPlayerTeam(target, teamId);
		requester.sendMessage(
				Text.literal("已将玩家 ")
						.append(target.getName())
						.append(" 分配到 ")
						.append(getTeamDisplayName(teamId)),
				false
		);
	}

	// ========== 公开API方法 ==========

	/**
	 * 获取API单例实例
	 */
	@NotNull
	public static TeamAPI getInstance() {
		return instance;
	}

	/**
	 * 获取配置对象
	 */
	@ApiStatus.Experimental
	@NotNull
	public static TeamConfig getConfig() {
		return config;
	}

	/**
	 * 注册队伍变更监听器
	 */
	public void registerTeamChangeListener(@NotNull TeamChangeListener listener) {
		teamChangeListeners.add(listener);
	}

	/**
	 * 注销队伍变更监听器
	 */
	public void unregisterTeamChangeListener(@NotNull TeamChangeListener listener) {
		teamChangeListeners.remove(listener);
	}

	/**
	 * 初始化记分板队伍
	 */
	public void initTeams(@NotNull Scoreboard scoreboard) {
		for (Map.Entry<String, TeamConfig.TeamInfo> entry : config.teams.entrySet())  {
			Team team = scoreboard.getTeam(entry.getKey());
			if (team == null) {
				team = scoreboard.addTeam(entry.getKey());
				team.setDisplayName(Text.literal(entry.getValue().displayName)
						.formatted(Formatting.byName(String.valueOf(entry.getValue().color))));
				team.setNameTagVisibilityRule(AbstractTeam.VisibilityRule.HIDE_FOR_OTHER_TEAMS);
				team.setColor(Formatting.byName(String.valueOf(entry.getValue().color)));
			}
		}
	}

	/**
	 * 设置玩家队伍
	 * @throws IllegalArgumentException 如果队伍ID不存在
	 */
	public void setPlayerTeam(@NotNull PlayerEntity player, @NotNull String teamId) {
		if (!config.teams.containsKey(teamId))  {
			throw new IllegalArgumentException("未知队伍ID: " + teamId);
		}

		if (!(player instanceof ServerPlayerEntity serverPlayer)) {
			return;
		}

		MinecraftServer server = serverPlayer.getServer();
		if (server == null) {
			return;
		}

		// 修改这部分 - 使用不可变对象
		PlayerTeamData newData = new PlayerTeamData(player.getUuid(),  teamId);
		playerDataMap.put(player.getUuid(),  newData);

		applyTeamSettings(serverPlayer, newData);

		// 通知监听器
		for (TeamChangeListener listener : teamChangeListeners) {
			listener.onTeamChanged(player,  teamId);
		}

		// 同步给所有客户端
		syncTeamDataToAllClients(server);
	}

	/**
	 * 获取玩家所在队伍ID
	 * @return 队伍ID，如果玩家没有队伍则返回null
	 */
	@Nullable
	public String getPlayerTeam(@NotNull PlayerEntity player) {
		PlayerTeamData data = playerDataMap.get(player.getUuid());
		return data != null ? data.teamId()  : null;
	}

	/**
	 * 获取队伍显示名称
	 */
	@NotNull
	public Text getTeamDisplayName(@NotNull String teamId) {
		TeamConfig.TeamInfo info = config.teams.get(teamId);
		return info != null
				? Text.literal(info.displayName).formatted(Formatting.byName(String.valueOf(info.color)))
				: Text.literal(teamId);
	}

	/**
	 * 玩家退出时清理数据
	 */
	public void onPlayerDisconnect(@NotNull PlayerEntity player) {
		playerDataMap.remove(player.getUuid());
	}

	// ========== 内部方法 ==========

	private void applyTeamSettings(@NotNull ServerPlayerEntity player, @Nullable PlayerTeamData data) {
		if (data == null || data.teamId()  == null) return;

		String playerName = player.getName().getString();
		Scoreboard scoreboard = player.getScoreboard();
		scoreboard.clearPlayerTeam(playerName);

		Team team = scoreboard.getTeam(data.teamId());
		if (team != null) {
			scoreboard.addPlayerToTeam(playerName,  team);
		}
	}

	private void syncTeamDataToClient(@NotNull ServerPlayerEntity player) {
		Map<String, String> roles = new HashMap<>();
		Objects.requireNonNull(player.getServer())
				.getPlayerManager()
				.getPlayerList()
				.forEach(p -> {
					PlayerTeamData data = playerDataMap.get(p.getUuid());
					if (data != null && data.teamId()  != null) {
						roles.put(p.getName().getString(),  data.teamId());
					}
				});

		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeInt(roles.size());
		roles.forEach((name,  teamId) -> {
			buf.writeString(name);
			buf.writeString(teamId);
		});

		ServerPlayNetworking.send(player,  SYNC_TEAMS, buf);
	}

	private void syncTeamDataToAllClients(@NotNull MinecraftServer server) {
		Map<String, String> roles = new HashMap<>();
		server.getPlayerManager()
				.getPlayerList()
				.forEach(p -> {
					PlayerTeamData data = playerDataMap.get(p.getUuid());
					if (data != null && data.teamId()  != null) {
						roles.put(p.getName().getString(),  data.teamId());
					}
				});

		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeInt(roles.size());
		roles.forEach((name,  teamId) -> {
			buf.writeString(name);
			buf.writeString(teamId);
		});

		server.getPlayerManager().getPlayerList().forEach(p  ->
				ServerPlayNetworking.send(p,  SYNC_TEAMS, buf)
		);
	}

	// ========== 接口定义 ==========

	/**
	 * 队伍变更监听器接口
	 */
	public interface TeamChangeListener {
		void onTeamChanged(@NotNull PlayerEntity player, @NotNull String newTeam);
	}
}