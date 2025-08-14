package com.teamapi;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Objects;
import java.util.UUID;

/**
 * 存储玩家队伍数据的不可变类
 */
public record PlayerTeamData(UUID playerId, String teamId) {
    /**
     * 创建队伍数据对象
     *
     * @param playerId 玩家UUID (不可为null)
     * @param teamId   队伍ID (可为null表示无队伍)
     */
    public PlayerTeamData(@NotNull UUID playerId, @Nullable String teamId) {
        this.playerId = Objects.requireNonNull(playerId, "玩家ID不能为null");
        this.teamId = teamId;
    }

    /**
     * 获取队伍ID
     *
     * @return 队伍ID，可能为null表示无队伍
     */
    @Override
    @Nullable
    public String teamId() {
        return teamId;
    }

    /**
     * 获取玩家UUID
     */
    @Override
    @NotNull
    public UUID playerId() {
        return playerId;
    }

    /**
     * 检查是否有队伍
     */
    public boolean hasTeam() {
        return teamId != null;
    }

    /**
     * 创建一个新对象(不可变对象模式)
     */
    @NotNull
    public PlayerTeamData withTeamId(@Nullable String newTeamId) {
        return new PlayerTeamData(this.playerId, newTeamId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlayerTeamData that = (PlayerTeamData) o;
        return playerId.equals(that.playerId) &&
                Objects.equals(teamId, that.teamId);
    }

    @Override
    public @NotNull String toString() {
        return "PlayerTeamData{" +
                "playerId=" + playerId +
                ", teamId='" + teamId + '\'' +
                '}';
    }
}