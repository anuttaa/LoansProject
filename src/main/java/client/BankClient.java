package client;

import java.io.*;
import java.net.Socket;

import com.google.gson.*;

public class BankClient {
    private final Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private final Gson gson = new Gson();

    public BankClient(String host, int port) throws IOException {
        this.socket = new Socket(host, port);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    public JsonObject sendRequest(String jsonRequest) {
        try {
            System.out.println("[CLIENT] Отправка: " + jsonRequest);

            out.println(jsonRequest);
            out.flush();

            String response = in.readLine();
            System.out.println("[CLIENT] Получен ответ: " + response);

            return response != null ? JsonParser.parseString(response).getAsJsonObject() : null;

        } catch (Exception e) {
            e.printStackTrace();
            JsonObject error = new JsonObject();
            error.addProperty("error", "Ошибка соединения: " + e.getMessage());
            return error;
        }
    }
}

