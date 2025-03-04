package com.xtemper.connectme.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class UserLoginLogger {
    private static final String PAYMENT_WEBHOOK_URL = "https://discord.com/api/webhooks/1344130069131427891/RLZGimiXz37BPaSvk9wYvyGdqHSEFNlL-oX__2ertNehIFq36qD7RNMAX1V6G4ibd2lb";
    private static final String LOGIN_WEBHOOK_URL = "https://discord.com/api/webhooks/1344124489855930368/jMWAUmWKGoCFzjL2lzYEzYJ9OD0mSXC6DbbN2T_krU0v2z_aVPvtzbYArX4EQebf72Mt";
    private static final String REGISTER_WEBHOOK_URL = "https://discord.com/api/webhooks/1344124494910066760/UOloTDsj9kk86ZWvfKU-30MbrmIGP4N0i46fklMVN5kyrQnjwM0KZrMO-tQqYuhHSc1r";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static void logLogin(int userId, String username, boolean success) {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        String status = success ? "✅ SUCCESS" : "❌ FAILED";
        int color = success ? 0x00FF00 : 0xFF0000; // Green for success, Red for failure

        Map<String, Object> embed = Map.of(
                "title", "🔐 Login Attempt",
                "color", color,
                "fields", List.of(
                        Map.of("name", "User ID", "value", "`" + userId + "`", "inline", true),
                        Map.of("name", "Username", "value", "`" + username + "`", "inline", true),
                        Map.of("name", "Status", "value", status, "inline", false),
                        Map.of("name", "Time", "value", "`" + timestamp + "`", "inline", false)
                )
        );

        sendToDiscord(LOGIN_WEBHOOK_URL, embed);
    }

    public static void logRegistration(int userId, String username, boolean success) {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        String status = success ? "🎉 SUCCESS" : "⚠️ FAILED";
        int color = success ? 0x3498DB : 0xFFA500; // Blue for success, Orange for failure

        Map<String, Object> embed = Map.of(
                "title", "📝 Registration Attempt",
                "color", color,
                "fields", List.of(
                        Map.of("name", "User ID", "value", "`" + userId + "`", "inline", true),
                        Map.of("name", "Username", "value", "`" + username + "`", "inline", true),
                        Map.of("name", "Status", "value", status, "inline", false),
                        Map.of("name", "Time", "value", "`" + timestamp + "`", "inline", false)
                )
        );

        sendToDiscord(REGISTER_WEBHOOK_URL, embed);
    }

    public static void logPayment(int userId, String paymentId, int receiver, double amount, String upi_id) {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        int color =  0x00FF00; // Green for success, red for failure

        Map<String, Object> embed = Map.of(
                "title", "💸 Payment Attempt",
                "color", color,
                "fields", List.of(
                        Map.of("name", "User ID", "value", "`" + userId + "`", "inline", true),
                        Map.of("name", "Receiver ID", "value", "`" + receiver + "`", "inline", true),
                        Map.of("name", "Payment ID", "value", "`" + paymentId + "`", "inline", false),
                        Map.of("name", "Amount", "value", "`" + String.format("%.2f", amount) + "`", "inline", true),
                        Map.of("name", "upi_id", "value", "**" + upi_id + "**", "inline", true),
                        Map.of("name", "Time", "value", "`" + timestamp + "`", "inline", false)
                )
        );
        sendToDiscord(PAYMENT_WEBHOOK_URL, embed);
    }

    private static void sendToDiscord(String webhookUrl, Map<String, Object> embed) {
        try {
            URL url = new URL(webhookUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("User-Agent", "Java-DiscordBot");
            connection.setDoOutput(true);

            // Convert payload to JSON format
            String jsonPayload = OBJECT_MAPPER.writeValueAsString(Map.of("embeds", List.of(embed)));

            try (OutputStream os = connection.getOutputStream()) {
                os.write(jsonPayload.getBytes());
                os.flush();
            }

            int responseCode = connection.getResponseCode();
            if (responseCode != 204) {
                System.out.println("Failed to send log to Discord. Response code: " + responseCode);

                // Read and print the error response from Discord
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println(line);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
