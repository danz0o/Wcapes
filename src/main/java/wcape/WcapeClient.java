package wcape;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.minecraft.network.chat.Component;

public class WcapeClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientLifecycleEvents.CLIENT_STARTED.register(c -> CapeManager.loadSaved());

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
            dispatcher.register(ClientCommandManager.literal("Wcape")
                .then(ClientCommandManager.literal("off").executes(ctx -> {
                    CapeManager.clear();
                    ctx.getSource().sendFeedback(Component.literal("[Wcape] Cape removed."));
                    return 1;
                }))
                .then(ClientCommandManager.argument("url", StringArgumentType.greedyString())
                    .executes(ctx -> {
                        String url = StringArgumentType.getString(ctx, "url").trim();
                        var src = ctx.getSource();
                        src.sendFeedback(Component.literal("[Wcape] Downloading..."));
                        CapeManager.load(url, msg ->
                            net.minecraft.client.Minecraft.getInstance().execute(() ->
                                src.sendFeedback(Component.literal("[Wcape] " + msg))));
                        return 1;
                    }))));
    }
}
