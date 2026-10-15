package pl.afkminer;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

public class SettingsScreen extends Screen {
    private final Screen parent;
    private ButtonWidget mainButton;

    public SettingsScreen(Screen parent) {
        super(Text.literal("AFK Miner - ustawienia"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        Config c = Config.INSTANCE;
        int w = 220;
        int x = width / 2 - w / 2;
        int y = 40;
        int step = 24;

        mainButton = addDrawableChild(ButtonWidget.builder(mainLabel(), b -> {
            AfkMinerClient.setEnabled(MinecraftClient.getInstance(), !Config.INSTANCE.enabled);
            b.setMessage(mainLabel());
        }).dimensions(x, y, w, 20).build());
        y += step + 6;

        addToggle(x, y, w, "Kopanie w tle (okno nieaktywne)", () -> c.keepRunningUnfocused, v -> c.keepRunningUnfocused = v);
        y += step;
        addToggle(x, y, w, "Animacja ręki", () -> c.swingArm, v -> c.swingArm = v);
        y += step;
        addToggle(x, y, w, "Stop: mało wytrzymałości narzędzia", () -> c.stopOnLowDurability, v -> c.stopOnLowDurability = v);
        y += step;
        addDrawableChild(new IntSlider(x, y, w, 20, "Min. wytrzymałość", 1, 100, c.minDurability, v -> c.minDurability = v));
        y += step;
        addToggle(x, y, w, "Stop: mało zdrowia", () -> c.stopOnLowHealth, v -> c.stopOnLowHealth = v);
        y += step;
        addDrawableChild(new IntSlider(x, y, w, 20, "Min. zdrowie (połówki serc)", 1, 19, c.minHealth, v -> c.minHealth = v));
        y += step;
        addToggle(x, y, w, "Stop: po otrzymaniu obrażeń", () -> c.stopOnDamage, v -> c.stopOnDamage = v);
        y += step;
        addToggle(x, y, w, "Stop: pełny ekwipunek", () -> c.stopOnFullInventory, v -> c.stopOnFullInventory = v);
        y += step;
        addToggle(x, y, w, "Komunikaty na ekranie", () -> c.showMessages, v -> c.showMessages = v);
        y += step + 6;

        addDrawableChild(ButtonWidget.builder(Text.literal("Gotowe"), b -> close())
                .dimensions(x, y, w, 20).build());
    }

    private Text mainLabel() {
        return Text.literal("Kopanie AFK: " + (Config.INSTANCE.enabled ? "§aWŁĄCZONE" : "§cWYŁĄCZONE"));
    }

    private void addToggle(int x, int y, int w, String label, BooleanSupplier get, Consumer<Boolean> set) {
        addDrawableChild(ButtonWidget.builder(toggleText(label, get.getAsBoolean()), b -> {
            boolean nv = !get.getAsBoolean();
            set.accept(nv);
            b.setMessage(toggleText(label, nv));
        }).dimensions(x, y, w, 20).build());
    }

    private Text toggleText(String label, boolean on) {
        return Text.literal(label + ": " + (on ? "§aTAK" : "§cNIE"));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 20, 0xFFFFFF);
    }

    @Override
    public boolean shouldPause() {
        return false; // gra (i kopanie) dziala dalej przy otwartym menu
    }

    @Override
    public void close() {
        Config.save();
        MinecraftClient.getInstance().setScreen(parent);
    }

    private static class IntSlider extends SliderWidget {
        private final String label;
        private final int min, max;
        private final IntConsumer setter;

        IntSlider(int x, int y, int w, int h, String label, int min, int max, int initial, IntConsumer setter) {
            super(x, y, w, h, Text.empty(), (initial - min) / (double) (max - min));
            this.label = label;
            this.min = min;
            this.max = max;
            this.setter = setter;
            updateMessage();
        }

        private int val() {
            return min + (int) Math.round(value * (max - min));
        }

        @Override
        protected void updateMessage() {
            setMessage(Text.literal(label + ": " + val()));
        }

        @Override
        protected void applyValue() {
            setter.accept(val());
        }
    }
}
