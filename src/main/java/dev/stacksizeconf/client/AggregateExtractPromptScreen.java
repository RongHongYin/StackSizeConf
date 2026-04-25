package dev.stacksizeconf.client;

import dev.stacksizeconf.aggregate.AggregateChestNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class AggregateExtractPromptScreen extends Screen {
    private final Screen parent;
    private final int menuSlot;
    private final int maxAmount;
    private EditBox amountEdit;

    public AggregateExtractPromptScreen(Screen parent, int menuSlot, int maxAmount) {
        super(Component.translatable("gui.stacksizeconf.aggregate.extract.title"));
        this.parent = parent;
        this.menuSlot = menuSlot;
        this.maxAmount = Math.max(1, maxAmount);
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        this.amountEdit = new EditBox(this.font, centerX - 70, centerY - 20, 140, 20, Component.translatable("gui.stacksizeconf.aggregate.extract.input"));
        this.amountEdit.setValue(Integer.toString(this.maxAmount));
        this.amountEdit.setFilter(s -> s.isEmpty() || s.chars().allMatch(Character::isDigit));
        this.addRenderableWidget(this.amountEdit);

        this.addRenderableWidget(Button.builder(Component.translatable("gui.stacksizeconf.aggregate.extract.confirm"), b -> confirm())
                .bounds(centerX - 72, centerY + 8, 70, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.stacksizeconf.aggregate.extract.cancel"), b -> closeToParent())
                .bounds(centerX + 2, centerY + 8, 70, 20).build());
        this.setInitialFocus(this.amountEdit);
    }

    private void confirm() {
        int amount = parseAmount();
        if (amount <= 0) {
            return;
        }
        ClientPlayNetworking.send(new AggregateChestNetworking.AggregateExtractPayload(this.menuSlot, amount));
        closeToParent();
    }

    private int parseAmount() {
        String text = this.amountEdit.getValue();
        if (text == null || text.isBlank()) {
            return 0;
        }
        try {
            int value = Integer.parseInt(text);
            return Math.max(1, Math.min(this.maxAmount, value));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private void closeToParent() {
        Minecraft.getInstance().setScreen(this.parent);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Avoid a second blur pass when this prompt is opened above another container screen.
        this.renderTransparentBackground(guiGraphics);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 44, 0xFFFFFF);
        guiGraphics.drawCenteredString(this.font,
                Component.translatable("gui.stacksizeconf.aggregate.extract.max", this.maxAmount),
                this.width / 2, this.height / 2 - 32, 0xA0A0A0);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
}
