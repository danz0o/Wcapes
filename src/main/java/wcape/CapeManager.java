package wcape;

import com.mojang.blaze3d.platform.NativeImage;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.function.Consumer;

public class CapeManager {
    public static final Identifier CAPE_ID = Identifier.fromNamespaceAndPath("wcape", "custom_cape");
    public static volatile boolean active = false;

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .connectTimeout(Duration.ofSeconds(10)).build();

    private static Path saveFile() {
        return FabricLoader.getInstance().getConfigDir().resolve("wcape.txt");
    }

    /** Download the image off-thread, then register it as a texture on the render thread. */
    public static void load(String url, Consumer<String> onDone) {
        Thread t = new Thread(() -> {
            try {
                HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                        .timeout(Duration.ofSeconds(20))
                        .header("User-Agent", "Wcape/1.0").build();
                HttpResponse<InputStream> res = HTTP.send(req, HttpResponse.BodyHandlers.ofInputStream());
                if (res.statusCode() != 200) {
                    onDone.accept("Error: HTTP " + res.statusCode());
                    return;
                }
                NativeImage img;
                try (InputStream in = res.body()) {
                    img = NativeImage.read(in); // PNG only
                }
                final NativeImage image = img;
                Minecraft mc = Minecraft.getInstance();
                mc.execute(() -> {
                    mc.getTextureManager().register(CAPE_ID, new DynamicTexture(() -> "wcape", image));
                    active = true;
                    try { Files.writeString(saveFile(), url); } catch (Exception ignored) {}
                    onDone.accept("Cape applied!");
                });
            } catch (Exception e) {
                onDone.accept("Error: " + e.getMessage());
            }
        }, "Wcape-Download");
        t.setDaemon(true);
        t.start();
    }

    public static void clear() {
        active = false;
        try { Files.deleteIfExists(saveFile()); } catch (Exception ignored) {}
    }

    public static void loadSaved() {
        try {
            Path f = saveFile();
            if (Files.exists(f)) load(Files.readString(f).trim(), s -> {});
        } catch (Exception ignored) {}
    }
}
