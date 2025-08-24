package com.teamapi.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * 通用队伍信息UI界面
 */
public class TeamUIScreen extends Screen {
    public static final int BUTTON_WIDTH = 200;
    public static final int BUTTON_HEIGHT = 20;
    public static final int ROW_SPACING = 25;
    public static final int TOP_MARGIN = 50;
    public static final int TITLE_Y = 20;
    public static final int BOTTOM_BUTTON_Y_OFFSET = 40;

    protected final Map<String, String> teamData = new LinkedHashMap<>(); // 保持插入顺序
    protected ButtonWidget closeButton;
    protected final Function<String, Text> teamDisplayNameMapper;
    protected final BiConsumer<String, String> rowClickHandler;

    /**
     * 创建队伍UI界面
     * @param title 界面标题
     * @param teamDisplayNameMapper 队伍ID到显示名称的转换函数
     * @param rowClickHandler 行点击处理器 (玩家名, 队伍ID)
     */
    public TeamUIScreen(Text title,
                        Function<String, Text> teamDisplayNameMapper,
                        BiConsumer<String, String> rowClickHandler) {
        super(title);
        this.teamDisplayNameMapper = teamDisplayNameMapper;
        this.rowClickHandler = rowClickHandler != null ? rowClickHandler : (p, t) -> {};
    }

    public TeamUIScreen(Text title, Function<String, Text> teamDisplayNameMapper) {
        this(title, teamDisplayNameMapper, null);
    }

    @Override
    protected void init() {
        super.init();
        setupCloseButton();
        refreshTeamList();
    }

    protected void setupCloseButton() {
        this.closeButton = ButtonWidget.builder(
                        Text.literal(" 关闭"),
                        button -> close()
                )
                .dimensions(
                        (this.width - BUTTON_WIDTH) / 2,
                        this.height - BOTTOM_BUTTON_Y_OFFSET,
                        BUTTON_WIDTH,
                        BUTTON_HEIGHT
                )
                .build();
        this.addDrawableChild(this.closeButton);
    }

    protected void refreshTeamList() {
        clearChildrenExceptCloseButton();
        renderTeamRows();
    }

    protected void clearChildrenExceptCloseButton() {
        // 保留关闭按钮
        this.clearChildren();
        this.addDrawableChild(this.closeButton);
    }

    protected void renderTeamRows() {
        int yPos = TOP_MARGIN;
        for (Map.Entry<String, String> entry : teamData.entrySet()) {
            addTeamRow(entry.getKey(), entry.getValue(), yPos);
            yPos += ROW_SPACING;
        }
    }

    protected void addTeamRow(String playerName, String teamId, int yPos) {
        Text displayText = Text.literal(playerName)
                .append(" - ")
                .append(teamDisplayNameMapper.apply(teamId));

        this.addDrawableChild(
                ButtonWidget.builder(displayText, button -> rowClickHandler.accept(playerName, teamId))
                        .dimensions(
                                (width - BUTTON_WIDTH) / 2,
                                yPos,
                                BUTTON_WIDTH,
                                BUTTON_HEIGHT
                        )
                        .build()
        );
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        renderTitle(context);
        super.render(context, mouseX, mouseY, delta);
    }

    public void renderBackground(DrawContext context) {
        if (this.client != null && this.client.world != null) {
            context.fillGradient(0, 0, this.width, this.height, 0xC0101010, 0xD0101010);
        } else {
            context.fill(0, 0, this.width, this.height, 0xFF000000);
        }
    }

    protected void renderTitle(DrawContext context) {
        context.drawCenteredTextWithShadow(
                this.textRenderer,
                this.title,
                this.width / 2,
                TITLE_Y,
                0xFFFFFF
        );
    }

    public void updateTeamData(Map<String, String> newData) {
        this.teamData.clear();
        if (newData != null) {
            this.teamData.putAll(newData);
        }
        if (this.client != null && this.client.currentScreen == this) {
            refreshTeamList();
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    /**
     * 打开队伍UI的快捷方法
     **/
    public static TeamUIScreen open(Function<String, Text> teamDisplayNameMapper) {
        MinecraftClient.getInstance().setScreen(new TeamUIScreen(
                Text.literal(" 队伍信息"),
                teamDisplayNameMapper
        ));
        return null;
    }

    /**
     * 带点击处理的打开方法
     */
    public static void open(Function<String, Text> teamDisplayNameMapper,
                            BiConsumer<String, String> rowClickHandler) {
        MinecraftClient.getInstance().setScreen(new TeamUIScreen(
                Text.literal(" 队伍信息"),
                teamDisplayNameMapper,
                rowClickHandler
        ));
    }
}