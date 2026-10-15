package pl.afkminer;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import org.lwjgl.glfw.GLFW;

public class AfkMinerClient implements ClientModInitializer {
    private static KeyBinding settingsKey;
    private static KeyBinding toggleKey;
    private static final KeyBinding.Category CATEGORY =
            KeyBinding.Category.create(Identifier.of("afkminer", "main"));

    private float lastHealth = -1;
    private boolean savedPauseOnLostFocus = true;
    private boolean overridingFocus = false;

    @Override
    public void onInitializeClient() {
        Config.load();

        settingsKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.afkminer.settings", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_X, CATEGORY));
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.afkminer.toggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G, CATEGORY));

        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    private void tick(MinecraftClient client) {
        while (settingsKey.wasPressed()) {
            if (client.currentScreen == null) {
                client.setScreen(new SettingsScreen(null));
            }
        }
        while (toggleKey.wasPressed()) {
            setEnabled(client, !Config.INSTANCE.enabled);
        }

        Config cfg = Config.INSTANCE;
        ClientPlayerEntity player = client.player;

        if (player == null || client.world == null || client.interactionManager == null) {
            lastHealth = -1;
            return;
        }

        handleFocusOverride(client, cfg.enabled && cfg.keepRunningUnfocused);

        if (!cfg.enabled) {
            lastHealth = player.getHealth();
            return;
        }

        // --- warunki zatrzymania ---
        if (cfg.stopOnLowDurability) {
            ItemStack stack = player.getMainHandStack();
            if (stack.isDamageable() && (stack.getMaxDamage() - stack.getDamage()) <= cfg.minDurability) {
                stop(client, "narzędzie ma mało wytrzymałości");
                return;
            }
        }
        if (cfg.stopOnLowHealth && player.getHealth() <= cfg.minHealth) {
            stop(client, "mało zdrowia");
            return;
        }
        if (cfg.stopOnDamage && lastHealth >= 0 && player.getHealth() < lastHealth) {
            stop(client, "dostałeś obrażenia");
            return;
        }
        if (cfg.stopOnFullInventory && player.getInventory().getEmptySlot() == -1) {
            stop(client, "pełny ekwipunek");
            return;
        }
        lastHealth = player.getHealth();

        // --- kopanie bloku na celowniku (dziala tez gdy GUI jest otwarte) ---
        HitResult hit = client.crosshairTarget;
        if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
            BlockHitResult bhr = (BlockHitResult) hit;
            if (!client.world.getBlockState(bhr.getBlockPos()).isAir()) {
                if (client.interactionManager.updateBlockBreakingProgress(bhr.getBlockPos(), bhr.getSide())) {
                    if (cfg.swingArm) player.swingHand(Hand.MAIN_HAND);
                }
            }
        } else {
            client.interactionManager.cancelBlockBreaking();
        }
    }

    private void handleFocusOverride(MinecraftClient client, boolean want) {
        if (want && !overridingFocus) {
            savedPauseOnLostFocus = client.options.pauseOnLostFocus;
            client.options.pauseOnLostFocus = false;
            overridingFocus = true;
        } else if (!want && overridingFocus) {
            client.options.pauseOnLostFocus = savedPauseOnLostFocus;
            overridingFocus = false;
        }
    }

    public static void setEnabled(MinecraftClient client, boolean value) {
        Config.INSTANCE.enabled = value;
        Config.save();
        if (!value && client.interactionManager != null) client.interactionManager.cancelBlockBreaking();
        message(client, "§6AFK Miner: " + (value ? "§aWŁĄCZONY" : "§cWYŁĄCZONY"));
    }

    private void stop(MinecraftClient client, String reason) {
        setEnabled(client, false);
        message(client, "§cAFK Miner zatrzymany: " + reason);
    }

    public static void message(MinecraftClient client, String text) {
        if (Config.INSTANCE.showMessages && client.player != null) {
            client.player.sendMessage(Text.literal(text), true);
        }
    }
}
