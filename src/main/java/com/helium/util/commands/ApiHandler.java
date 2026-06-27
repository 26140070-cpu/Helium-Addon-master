package com.helium.util.commands;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import net.minecraft.text.Text;
import com.helium.util.LogUtil;
import com.helium.util.commands.Nullable;
import java.net.URISyntaxException;
import com.helium.util.HeliumUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

public class ApiHandler {
    public static final String API_2B2T_URL = "https://api.2b2t.vc/";

    public static void sendErrorResponse() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player != null) {
            player.sendMessage(
                    Text.of(
                            "§8<"+HeliumUtils.rCC()
                                    +"§o✨"+"§r§8> §4An error occurred§7, §4please try again later or check §7latest.log §4for more info§7.."
                    ), false
            );
        }
    }

    @com.helium.util.commands.Nullable
    public String fetchResponse(String requestString) {
        HttpClient client = HttpClient.newHttpClient();

        HttpRequest req;
        try {
            req = HttpRequest.newBuilder().uri(new URI(requestString))
                    .header("Accept", "*/*")
                    .method("GET", HttpRequest.BodyPublishers.noBody())
                    .timeout(Duration.ofSeconds(30))
                    .build();
        } catch (URISyntaxException err) {
            sendErrorResponse();
            LogUtil.error(err.toString(), "ApiHandler");
            return null;
        }

        if (req == null) {
            sendErrorResponse();
            return null;
        }

        HttpResponse<String> res = null;
        try {
            res = client.sendAsync(req, HttpResponse.BodyHandlers.ofString()).get();
        } catch (Exception err) {
            LogUtil.error(err.toString(), "ApiHandler");
        }

        if (res == null) {
            sendErrorResponse();
            return null;
        }

        if (res.statusCode() == 200) {
            return res.body();
        } else if (res.statusCode() == 204) {
            return "204 Undocumented";
        } else {
            sendErrorResponse();
            LogUtil.warn("Received unexpected response from api.2b2t.vc: \"" + res + "\"", "ApiHandler");
        }

        return null;
    }
}