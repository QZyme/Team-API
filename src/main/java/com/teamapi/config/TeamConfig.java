package com.teamapi.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.annotation.Nullable;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static com.teamapi.TeamAPI.BLUE_TEAM;
import static com.teamapi.TeamAPI.RED_TEAM;

@Config(name = "teamapi")
public class TeamConfig implements ConfigData {
    /**
     * 队伍信息配置类
     */
    public static final class TeamInfo {
        @ConfigEntry.Gui.Tooltip(count = 2)
        public String displayName;

        @ConfigEntry.Gui.Tooltip(count = 2)
        public Formatting color;

        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.Category("advanced")
        @ApiStatus.Experimental
        private String icon = "";

        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.Category("advanced")
        @ApiStatus.Experimental
        private boolean canFly = false;

        // 空构造方法用于反序列化
        public TeamInfo() {}

        /**
         * 创建队伍信息
         * @param displayName 显示名称
         * @param color 颜色枚举
         */
        public TeamInfo(String displayName, Formatting color) {
            this.displayName  = displayName;
            this.color  = color;
        }

        // Getter方法
        public String getDisplayName() { return displayName; }
        public Formatting getColor() { return color; }
        public String getIcon() { return icon; }
        public boolean canFly() { return canFly; }

        // Setter方法
        public void setDisplayName(String displayName) { this.displayName  = displayName; }
        public void setColor(Formatting color) { this.color  = color; }
        public void setIcon(String icon) { this.icon  = icon; }
        public void setCanFly(boolean canFly) { this.canFly  = canFly; }
    }

    @ConfigEntry.Gui.CollapsibleObject
    public final Map<String, TeamInfo> teams = new LinkedHashMap<>();

    public TeamConfig() {
        // 初始化默认队伍配置
        initializeDefaultTeams();
    }

    private void initializeDefaultTeams() {
        teams.put(RED_TEAM,  new TeamInfo("红队", Formatting.RED));
        teams.put(BLUE_TEAM,  new TeamInfo("蓝队", Formatting.BLUE));
    }

    /**
     * 获取所有队伍配置（不可修改的视图）
     */
    public @NotNull Map<String, TeamInfo> getTeams() {
        return Collections.unmodifiableMap(teams);
    }

    /**
     * 获取特定队伍配置
     */
    public @Nullable TeamInfo getTeamInfo(String teamId) {
        return teams.get(teamId);
    }

    @Override
    public void validatePostLoad() {
        // 确保存在基本队伍配置
        teams.putIfAbsent(RED_TEAM,  new TeamInfo("红队", Formatting.RED));
        teams.putIfAbsent(BLUE_TEAM,  new TeamInfo("蓝队", Formatting.BLUE));

        // 验证颜色配置
        teams.values().forEach(info  -> {
            if (info.color  == null) {
                info.color  = Formatting.WHITE;
            }
        });
    }
}