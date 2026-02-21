package me.sailex.secondbrain.mineskin;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Objects;

public class MineSkinProxyClient {

    public static final String PROXY_URL = "https://mineskin.sailex.me/skin";
    public static final Gson GSON = new Gson();
    private static final String SPECIAL_FILE_PATH = "/Special:FilePath/";

    private final HttpClient proxyClient;
    private final HttpClient redirectClient;

    public MineSkinProxyClient() {
        proxyClient = HttpClient.newBuilder().build();
        redirectClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();
    }

    public SkinResponse getSkin(String url) {
        try {
            String resolvedUrl = resolveSkinUrl(url);
            SkinRequest requestBody = new SkinRequest(resolvedUrl, Variant.AUTO);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(PROXY_URL))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(requestBody)))
                    .build();
            HttpResponse<String> response = proxyClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                String responseBody = response.body() == null ? "" : response.body().trim();
                if (responseBody.length() > 300) {
                    responseBody = responseBody.substring(0, 300) + "...";
                }
                throw new MineSkinProxyClientException(
                        "MineSkin proxy request failed with status " + response.statusCode() + " for: " + resolvedUrl
                                + formatOriginalUrl(url, resolvedUrl)
                                + ". Body: " + responseBody,
                        null
                );
            }

            SkinResponse parsed = GSON.fromJson(response.body(), SkinResponse.class);
            if (parsed == null || isBlank(parsed.texture()) || isBlank(parsed.signature())) {
                throw new MineSkinProxyClientException("MineSkin proxy returned invalid skin payload for: " + resolvedUrl
                        + formatOriginalUrl(url, resolvedUrl), null);
            }
            return parsed;
        } catch (IOException | InterruptedException | JsonParseException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new MineSkinProxyClientException("Failed to get skin for: " + url, e);
        }
    }

    private String resolveSkinUrl(String url) throws IOException, InterruptedException {
        if (isBlank(url) || !url.contains(SPECIAL_FILE_PATH)) {
            return url;
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "image/*,*/*;q=0.8")
                .GET()
                .build();
        HttpResponse<Void> response = redirectClient.send(request, HttpResponse.BodyHandlers.discarding());
        URI resolvedUri = response.uri();
        if (resolvedUri == null) {
            return url;
        }
        return resolvedUri.toString();
    }

    private static String formatOriginalUrl(String originalUrl, String resolvedUrl) {
        if (Objects.equals(originalUrl, resolvedUrl)) {
            return "";
        }
        return " (original: " + originalUrl + ")";
    }

    private static boolean isBlank(String value) {
        return Objects.isNull(value) || value.isBlank();
    }

}
