package com.ans.client.settings;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import com.mojang.blaze3d.vertex.PoseStack;

/** Lightweight settings screen implemented against the Minecraft 1.20.1 GUI API. */
public final class AnsSettingsScreen extends Screen {
   private final Screen parent;
   private int scrollOffset;
   private final List<AnsSettings.Setting> settings = new ArrayList<>();

   public AnsSettingsScreen(Screen parent) {
      super(Component.translatable("ans.settings.title"));
      this.parent = parent;
   }

   @Override
   protected void init() {
      this.settings.clear();
      this.settings.addAll(AnsSettings.all());
      this.rebuildRows();
   }

   private void rebuildRows() {
      this.clearWidgets();
      int rowHeight = 24;
      int top = 38;
      int visibleRows = Math.max(1, (this.height - 86) / rowHeight);
      int maxOffset = Math.max(0, this.settings.size() - visibleRows);
      this.scrollOffset = Math.max(0, Math.min(this.scrollOffset, maxOffset));
      int end = Math.min(this.settings.size(), this.scrollOffset + visibleRows);
      int width = Math.min(330, this.width - 24);
      int x = (this.width - width) / 2;

      for (int i = this.scrollOffset; i < end; i++) {
         AnsSettings.Setting setting = this.settings.get(i);
         String[] label = setting.label();
         Tooltip tooltip = label[1].isEmpty() ? null : Tooltip.create(Component.literal(label[1]));
         int y = top + (i - this.scrollOffset) * rowHeight;
         if (setting.toggle()) {
            CycleButton<Boolean> button = CycleButton.onOffBuilder(Boolean.parseBoolean(setting.value()))
               .withTooltip(value -> tooltip)
               .create(x, y, width, 20, Component.literal(label[0]),
                  (widget, value) -> AnsSettings.set(setting, Boolean.toString(value), true));
            this.addRenderableWidget(button);
         } else {
            String current = setting.options().contains(setting.value()) ? setting.value() : setting.defaultValue();
            if (setting.options().isEmpty()) {
               Button unavailable = Button.builder(Component.literal(label[0] + ": " + current), widget -> {})
                  .bounds(x, y, width, 20).build();
               unavailable.active = false;
               if (tooltip != null) unavailable.setTooltip(tooltip);
               this.addRenderableWidget(unavailable);
            } else {
               CycleButton<String> button = CycleButton.builder(value -> Component.literal(setting.optionLabel(value)), current)
                  .withValues(setting.options())
                  .withTooltip(value -> tooltip)
                  .create(x, y, width, 20, Component.literal(label[0]),
                     (widget, value) -> AnsSettings.set(setting, value, true));
               this.addRenderableWidget(button);
            }
         }
      }

      int footerY = this.height - 28;
      this.addRenderableWidget(Button.builder(Component.translatable("ans.settings.reset"), widget -> {
         AnsSettings.resetAll();
         this.init();
      }).bounds(this.width / 2 - 154, footerY, 150, 20).build());
      this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), widget -> this.onClose())
         .bounds(this.width / 2 + 4, footerY, 150, 20).build());
   }

   @Override
   public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
      this.renderBackground(poseStack);
      drawCenteredString(poseStack, this.font, this.title, this.width / 2, 16, 0xFFFFFF);
      if (this.settings.isEmpty()) {
         drawCenteredString(poseStack, this.font, Component.translatable("ans.settings.missing"), this.width / 2, 44, 0xAAAAAA);
      }
      super.render(poseStack, mouseX, mouseY, partialTick);
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
      int visibleRows = Math.max(1, (this.height - 86) / 24);
      int maxOffset = Math.max(0, this.settings.size() - visibleRows);
      int next = Math.max(0, Math.min(maxOffset, this.scrollOffset - (int)Math.signum(delta)));
      if (next != this.scrollOffset) {
         this.scrollOffset = next;
         this.rebuildRows();
         return true;
      }
      return super.mouseScrolled(mouseX, mouseY, delta);
   }

   void scrollTo(double amount) {
      this.scrollOffset = Math.max(0, (int)Math.round(amount));
      this.rebuildRows();
   }

   @Override
   public void onClose() {
      AnsSettings.save();
      if (this.minecraft != null) this.minecraft.setScreen(this.parent);
   }
}
