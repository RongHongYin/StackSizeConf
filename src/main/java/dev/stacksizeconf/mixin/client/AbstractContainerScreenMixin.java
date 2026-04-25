package dev.stacksizeconf.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.stacksizeconf.aggregate.AggregateInventory;
import dev.stacksizeconf.aggregate.AggregateChestBlockEntity;
import dev.stacksizeconf.aggregate.AggregateChestNetworking;
import dev.stacksizeconf.client.AggregateExtractPromptScreen;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin<T extends AbstractContainerMenu> extends Screen {
    private static final int AGGREGATE_VIEW_SLOTS = 54;
    private static final int MAX_COUNT_TEXT_WIDTH = 15;
    private static final float MIN_COUNT_SCALE = 0.38f;
    private static final int SCROLL_TRACK_WIDTH = 12;
    private static final int SCROLL_TRACK_HEIGHT = 108;
    private static final int SCROLL_X_PADDING = 4;
    private static final int SCROLL_Y = 18;
    private static final int TOTAL_VIRTUAL_SLOTS = 1000;
    private static final int VISIBLE_SLOTS = 54;
    private static final int MAX_SCROLL_OFFSET = TOTAL_VIRTUAL_SLOTS - VISIBLE_SLOTS;
    private static final int SCROLL_STEP = 9;

    private boolean stacksizeconf$draggingScrollbar;
    private int stacksizeconf$clientScrollOffset;
    private EditBox stacksizeconf$searchBox;
    private Button stacksizeconf$sortAscButton;
    private Button stacksizeconf$sortDescButton;
    private AggregateInventory.SortMode stacksizeconf$clientSortMode = AggregateInventory.SortMode.NONE;

    @Shadow
    protected Slot hoveredSlot;

    @Shadow
    protected T menu;

    @Shadow
    protected int leftPos;

    @Shadow
    protected int topPos;

    protected AbstractContainerScreenMixin(net.minecraft.network.chat.Component title) {
        super(title);
    }

    @Inject(method = "mouseClicked(Lnet/minecraft/client/input/MouseButtonEvent;Z)Z", at = @At("HEAD"), cancellable = true)
    private void stacksizeconf$openAggregateExtractPrompt(MouseButtonEvent mouseButtonEvent, boolean dblClick, CallbackInfoReturnable<Boolean> cir) {
        if (this.hoveredSlot == null || !stacksizeconf$isAggregateChestScreen()) {
            return;
        }
        int hoveredMenuSlot = this.menu.slots.indexOf(this.hoveredSlot);
        if (hoveredMenuSlot < 0) {
            return;
        }
        int button = mouseButtonEvent.button();
        // Top container view slots only; let player inventory slots behave normally.
        if (hoveredMenuSlot >= AGGREGATE_VIEW_SLOTS) {
            return;
        }
        if (button != 0 && button != 1) {
            return;
        }
        ItemStack displayed = this.hoveredSlot.getItem();
        if (displayed.isEmpty()) {
            return;
        }
        long displayedTotal = stacksizeconf$getAggregateTotal(displayed);
        int suggested = button == 1 ? 1 : (int) Math.min(Integer.MAX_VALUE, Math.max(1L, displayedTotal));
        if (displayedTotal <= 0L) {
            suggested = button == 1 ? 1 : displayed.getCount();
        }
        Minecraft client = Minecraft.getInstance();
        client.setScreen(new AggregateExtractPromptScreen((Screen) (Object) this, hoveredMenuSlot, suggested));
        cir.setReturnValue(true);
    }

    @Inject(
            method = "renderContents(Lnet/minecraft/client/gui/GuiGraphics;IIF)V",
            at = @At("TAIL")
    )
    private void stacksizeconf$renderAggregateCountOverlay(
            net.minecraft.client.gui.GuiGraphics guiGraphics,
            int mouseX,
            int mouseY,
            float partialTick,
            CallbackInfo ci
    ) {
        if (!stacksizeconf$isAggregateChestScreen()) {
            return;
        }
        int limit = Math.min(AGGREGATE_VIEW_SLOTS, this.menu.slots.size());
        for (int i = 0; i < limit; i++) {
            Slot slot = this.menu.slots.get(i);
            if (slot.getItem().isEmpty()) {
                continue;
            }
            long total = stacksizeconf$getAggregateTotal(slot);
            if (total <= 0L) {
                continue;
            }
            String text = Long.toString(total);
            int width = this.font.width(text);
            float scale = Math.max(MIN_COUNT_SCALE, Math.min(1.0f, MAX_COUNT_TEXT_WIDTH / (float) width));
            float brX = this.leftPos + slot.x + 19 - 2f;
            float brY = this.topPos + slot.y + 6 + 3f + this.font.lineHeight;
            guiGraphics.pose().pushMatrix();
            guiGraphics.pose().translate(brX, brY);
            guiGraphics.pose().scale(scale, scale);
            guiGraphics.drawString(this.font, text, -width, -this.font.lineHeight, 0xFFFFFFFF, true);
            guiGraphics.pose().popMatrix();
        }
        stacksizeconf$renderScrollbar(guiGraphics);
    }

    @Inject(method = "init()V", at = @At("TAIL"))
    private void stacksizeconf$initSearchBox(CallbackInfo ci) {
        if (!stacksizeconf$isAggregateChestScreen()) {
            this.stacksizeconf$searchBox = null;
            this.stacksizeconf$sortAscButton = null;
            this.stacksizeconf$sortDescButton = null;
            return;
        }
        this.stacksizeconf$searchBox = new EditBox(this.font, this.leftPos + 8, this.topPos - 12, 90, 12, Component.translatable("gui.stacksizeconf.aggregate.search"));
        this.stacksizeconf$searchBox.setResponder(value -> ClientPlayNetworking.send(new AggregateChestNetworking.AggregateFilterPayload(value)));
        this.addRenderableWidget(this.stacksizeconf$searchBox);
        this.stacksizeconf$clientSortMode = AggregateInventory.SortMode.NONE;
        this.stacksizeconf$sortAscButton = this.addRenderableWidget(
                Button.builder(Component.literal("↑"), b -> {
                            this.stacksizeconf$clientSortMode = AggregateInventory.SortMode.COUNT_ASC;
                            ClientPlayNetworking.send(new AggregateChestNetworking.AggregateSortPayload(AggregateInventory.SortMode.COUNT_ASC.ordinal()));
                            stacksizeconf$refreshSortButtonState();
                        })
                        .bounds(this.leftPos + 102, this.topPos - 13, 12, 12)
                        .build());
        this.stacksizeconf$sortAscButton.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                Component.translatable("gui.stacksizeconf.aggregate.sort_asc")));
        this.stacksizeconf$sortDescButton = this.addRenderableWidget(
                Button.builder(Component.literal("↓"), b -> {
                            this.stacksizeconf$clientSortMode = AggregateInventory.SortMode.COUNT_DESC;
                            ClientPlayNetworking.send(new AggregateChestNetworking.AggregateSortPayload(AggregateInventory.SortMode.COUNT_DESC.ordinal()));
                            stacksizeconf$refreshSortButtonState();
                        })
                        .bounds(this.leftPos + 116, this.topPos - 13, 12, 12)
                        .build());
        this.stacksizeconf$sortDescButton.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                Component.translatable("gui.stacksizeconf.aggregate.sort_desc")));
        stacksizeconf$refreshSortButtonState();
    }

    private static long stacksizeconf$getAggregateTotal(ItemStack stack) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        if (customData.isEmpty()) {
            return 0L;
        }
        CompoundTag tag = customData.copyTag();
        if (!tag.contains(AggregateChestBlockEntity.AGGREGATE_TOTAL_TAG)) {
            return 0L;
        }
        return Math.max(0L, tag.getLong(AggregateChestBlockEntity.AGGREGATE_TOTAL_TAG).orElse(0L));
    }

    private static long stacksizeconf$getAggregateTotal(Slot slot) {
        long fromData = stacksizeconf$getAggregateTotal(slot.getItem());
        if (fromData > 0L) {
            return fromData;
        }
        if (slot.container instanceof AggregateInventory aggregateInventory) {
            return Math.max(0L, aggregateInventory.getVirtualTotalForSlot(slot.getContainerSlot()));
        }
        return 0L;
    }

    private boolean stacksizeconf$isAggregateChestScreen() {
        if (this.hoveredSlot != null && this.hoveredSlot.container instanceof AggregateInventory) {
            return true;
        }
        Component expected = Component.translatable("container.stacksizeconf.aggregate_chest");
        return this.title != null && this.title.getString().startsWith(expected.getString());
    }

    @Inject(method = "mouseClicked(Lnet/minecraft/client/input/MouseButtonEvent;Z)Z", at = @At("HEAD"), cancellable = true)
    private void stacksizeconf$startScrollbarDrag(MouseButtonEvent mouseButtonEvent, boolean dblClick, CallbackInfoReturnable<Boolean> cir) {
        if (!stacksizeconf$isAggregateChestScreen() || mouseButtonEvent.button() != 0) {
            return;
        }
        if (!stacksizeconf$isOverScrollbar(mouseButtonEvent.x(), mouseButtonEvent.y())) {
            return;
        }
        this.stacksizeconf$draggingScrollbar = true;
        stacksizeconf$setOffsetFromMouse(mouseButtonEvent.y());
        cir.setReturnValue(true);
    }

    @Inject(method = "mouseDragged(Lnet/minecraft/client/input/MouseButtonEvent;DD)Z", at = @At("HEAD"), cancellable = true)
    private void stacksizeconf$dragScrollbar(MouseButtonEvent mouseButtonEvent, double dragX, double dragY, CallbackInfoReturnable<Boolean> cir) {
        if (!this.stacksizeconf$draggingScrollbar || !stacksizeconf$isAggregateChestScreen()) {
            return;
        }
        stacksizeconf$setOffsetFromMouse(mouseButtonEvent.y());
        cir.setReturnValue(true);
    }

    @Inject(method = "mouseReleased(Lnet/minecraft/client/input/MouseButtonEvent;)Z", at = @At("HEAD"))
    private void stacksizeconf$stopScrollbarDrag(MouseButtonEvent mouseButtonEvent, CallbackInfoReturnable<Boolean> cir) {
        if (mouseButtonEvent.button() == 0) {
            this.stacksizeconf$draggingScrollbar = false;
        }
    }

    @Inject(method = "mouseScrolled(DDDD)Z", at = @At("HEAD"), cancellable = true)
    private void stacksizeconf$scrollSearchOrView(double mouseX, double mouseY, double horizontalAmount, double verticalAmount, CallbackInfoReturnable<Boolean> cir) {
        if (!stacksizeconf$isAggregateChestScreen() || Math.abs(verticalAmount) < 1e-6) {
            return;
        }
        if (this.stacksizeconf$searchBox != null && this.stacksizeconf$searchBox.isFocused()) {
            return;
        }
        int rows = verticalAmount > 0 ? -1 : 1;
        stacksizeconf$applyRelativeScroll(rows);
        cir.setReturnValue(true);
    }

    private void stacksizeconf$applyRelativeScroll(int rows) {
        int delta = rows * SCROLL_STEP;
        int next = Math.max(0, Math.min(MAX_SCROLL_OFFSET, this.stacksizeconf$clientScrollOffset + delta));
        if (next == this.stacksizeconf$clientScrollOffset) {
            return;
        }
        this.stacksizeconf$clientScrollOffset = next;
        ClientPlayNetworking.send(new AggregateChestNetworking.AggregateScrollPayload(rows));
    }

    private void stacksizeconf$setOffsetFromMouse(double mouseY) {
        int trackY = this.topPos + SCROLL_Y;
        int thumbHeight = stacksizeconf$getThumbHeight();
        int movable = Math.max(1, SCROLL_TRACK_HEIGHT - thumbHeight);
        double relative = (mouseY - trackY - thumbHeight / 2.0) / movable;
        int offset = (int) Math.round(Math.max(0.0, Math.min(1.0, relative)) * MAX_SCROLL_OFFSET);
        if (offset == this.stacksizeconf$clientScrollOffset) {
            return;
        }
        this.stacksizeconf$clientScrollOffset = offset;
        ClientPlayNetworking.send(new AggregateChestNetworking.AggregateScrollSetPayload(offset));
    }

    private void stacksizeconf$renderScrollbar(net.minecraft.client.gui.GuiGraphics guiGraphics) {
        if (!stacksizeconf$isAggregateChestScreen()) {
            return;
        }
        int trackX1 = this.leftPos + 176 + SCROLL_X_PADDING;
        int trackY1 = this.topPos + SCROLL_Y;
        int trackX2 = trackX1 + SCROLL_TRACK_WIDTH;
        int trackY2 = trackY1 + SCROLL_TRACK_HEIGHT;
        guiGraphics.fill(trackX1, trackY1, trackX2, trackY2, 0xFF7F7F7F);
        int thumbHeight = stacksizeconf$getThumbHeight();
        int movable = Math.max(1, SCROLL_TRACK_HEIGHT - thumbHeight);
        int thumbTop = trackY1 + (int) Math.round((this.stacksizeconf$clientScrollOffset / (double) MAX_SCROLL_OFFSET) * movable);
        guiGraphics.fill(trackX1 + 1, thumbTop, trackX2 - 1, thumbTop + thumbHeight, 0xFFCFCFCF);
    }

    private int stacksizeconf$getThumbHeight() {
        return Math.max(12, (int) Math.round((VISIBLE_SLOTS / (double) TOTAL_VIRTUAL_SLOTS) * SCROLL_TRACK_HEIGHT));
    }

    private boolean stacksizeconf$isOverScrollbar(double mouseX, double mouseY) {
        int trackX1 = this.leftPos + 176 + SCROLL_X_PADDING;
        int trackY1 = this.topPos + SCROLL_Y;
        return mouseX >= trackX1 && mouseX < trackX1 + SCROLL_TRACK_WIDTH
                && mouseY >= trackY1 && mouseY < trackY1 + SCROLL_TRACK_HEIGHT;
    }

    private void stacksizeconf$refreshSortButtonState() {
        if (this.stacksizeconf$sortAscButton != null) {
            this.stacksizeconf$sortAscButton.active = this.stacksizeconf$clientSortMode != AggregateInventory.SortMode.COUNT_ASC;
        }
        if (this.stacksizeconf$sortDescButton != null) {
            this.stacksizeconf$sortDescButton.active = this.stacksizeconf$clientSortMode != AggregateInventory.SortMode.COUNT_DESC;
        }
    }
}
