package dev.quickstack.client;

import dev.quickstack.client.mixin.HandledScreenAccessor;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import java.util.ArrayList;
import java.util.List;

public final class QuickStackClient implements ClientModInitializer {
    private static KeyBinding dropKey;
    private static KeyBinding collectKey;
    private static Operation active;

    @Override public void onInitializeClient() {
        dropKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.quickstack.drop", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G, KeyBinding.Category.INVENTORY));
        collectKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.quickstack.collect", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_H, KeyBinding.Category.INVENTORY));
        ClientTickEvents.END_CLIENT_TICK.register(client -> tick(client));
    }

    private static void tick(MinecraftClient client) {
        if (active != null) { active.tick(client); return; }
        if (client.player == null || !(client.currentScreen instanceof HandledScreen<?> screen)) return;
        if (dropKey.wasPressed()) begin(client, screen, true);
        else if (collectKey.wasPressed()) begin(client, screen, false);
    }

    private static void begin(MinecraftClient client, HandledScreen<?> screen, boolean drop) {
        Slot hovered = ((HandledScreenAccessor) screen).quickstack$getFocusedSlot();
        if (hovered == null || !hovered.hasStack() || hovered.inventory == client.player.getInventory()) return;
        ItemStack target = hovered.getStack().copy();
        ScreenHandler handler = screen.getScreenHandler();
        List<Integer> slots = new ArrayList<>();
        int total = 0;
        for (int i = 0; i < handler.slots.size(); i++) {
            Slot slot = handler.slots.get(i);
            if (slot.inventory != client.player.getInventory() && slot.hasStack() && ItemStack.areItemsAndComponentsEqual(slot.getStack(), target)) {
                slots.add(i); total += slot.getStack().getCount();
            }
        }
        if (slots.isEmpty()) { feedback(client, "message.quickstack.none"); return; }
        active = new Operation(handler.syncId, target, slots, total, drop);
    }

    private static void feedback(MinecraftClient client, String key, Object... args) {
        if (client.player != null) client.player.sendMessage(Text.translatable(key, args), true);
    }

    private static final class Operation {
        final int syncId;
        final ItemStack target;
        final List<Integer> sourceSlots;
        final int initialCount;
        final boolean drop;
        int cursor;
        int wait;
        Operation(int syncId, ItemStack target, List<Integer> slots, int initialCount, boolean drop) {
            this.syncId = syncId; this.target = target; this.sourceSlots = slots; this.initialCount = initialCount; this.drop = drop;
        }
        void tick(MinecraftClient client) {
            if (client.player == null || !(client.currentScreen instanceof HandledScreen<?> screen) || screen.getScreenHandler().syncId != syncId) { active = null; return; }
            if (wait-- > 0) return;
            ScreenHandler handler = screen.getScreenHandler();
            while (cursor < sourceSlots.size()) {
                int slotId = sourceSlots.get(cursor++);
                if (slotId >= handler.slots.size()) continue;
                Slot slot = handler.slots.get(slotId);
                if (!slot.hasStack() || !ItemStack.areItemsAndComponentsEqual(slot.getStack(), target)) continue;
                client.interactionManager.clickSlot(syncId, slotId, drop ? 1 : 0, drop ? SlotActionType.THROW : SlotActionType.QUICK_MOVE, client.player);
                wait = 1;
                return;
            }
            int remaining = 0;
            for (int slotId : sourceSlots) if (slotId < handler.slots.size()) {
                Slot slot = handler.slots.get(slotId);
                if (slot.hasStack() && ItemStack.areItemsAndComponentsEqual(slot.getStack(), target)) remaining += slot.getStack().getCount();
            }
            int moved = Math.max(0, initialCount - remaining);
            if (drop) feedback(client, moved == 0 ? "message.quickstack.none" : "message.quickstack.dropped", moved, target.getName());
            else if (moved == 0) feedback(client, "message.quickstack.none");
            else feedback(client, remaining > 0 ? "message.quickstack.partial" : "message.quickstack.collected", moved, remaining, target.getName());
            active = null;
        }
    }
}
