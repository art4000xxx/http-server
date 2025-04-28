package ru.netology.server;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private final int port;
    private final ExecutorService threadPool;
    private final Map<String, Map<String, Handler>> handlers;

    public Server(int port) {
        this.port = port;
        this.threadPool = Executors.newFixedThreadPool(64);
        this.handlers = new HashMap<>();
    }

    public void addHandler(String method, String path, Handler handler) {
        handlers.computeIfAbsent(method, k -> new HashMap<>()).put(path, handler);
    }

    public void listen() {
        try (var serverSocket = new ServerSocket(port)) {
            System.out.println("Server started on port " + port);
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    final var socket = serverSocket.accept();
                    threadPool.submit(() -> handleConnection(socket));
                } catch (IOException e) {
                    System.err.println("Error accepting connection: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            threadPool.shutdown();
        }
    }

    private void handleConnection(Socket socket) {
        try (
                var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                var out = new BufferedOutputStream(socket.getOutputStream())
        ) {
            // Чтение первой строки запроса
            String requestLine = in.readLine();
            if (requestLine == null || requestLine.isEmpty()) {
                sendErrorResponse(out, 400, "Bad Request");
                return;
            }

            // Парсинг строки запроса
            String[] parts = requestLine.split(" ");
            if (parts.length != 3) {
                sendErrorResponse(out, 400, "Bad Request");
                return;
            }

            String method = parts[0];
            String path = parts[1].split("\\?")[0]; // Игнорируем query-параметры

            // Чтение заголовков
            Map<String, String> headers = new HashMap<>();
            String line;
            while (!(line = in.readLine()).isEmpty()) {
                int index = line.indexOf(":");
                if (index != -1) {
                    headers.put(line.substring(0, index).trim(), line.substring(index + 1).trim());
                }
            }

            // Чтение тела (если есть)
            StringBuilder body = new StringBuilder();
            if (headers.containsKey("Content-Length")) {
                int length = Integer.parseInt(headers.get("Content-Length"));
                char[] buffer = new char[length];
                int read = in.read(buffer, 0, length);
                if (read != -1) {
                    body.append(buffer, 0, read);
                }
            }

            Request request = new Request(method, path, headers, body.toString());

            // Поиск и вызов хендлера
            Map<String, Handler> methodHandlers = handlers.get(method);
            if (methodHandlers != null) {
                Handler handler = methodHandlers.get(path);
                if (handler != null) {
                    handler.handle(request, out);
                    out.flush();
                    return;
                }
            }

            // Если хендлер не найден
            sendErrorResponse(out, 404, "Not Found");
        } catch (IOException e) {
            System.err.println("Error handling connection: " + e.getMessage());
            try {
                sendErrorResponse(new BufferedOutputStream(socket.getOutputStream()), 500, "Internal Server Error");
            } catch (IOException ex) {
                System.err.println("Failed to send error response: " + ex.getMessage());
            }
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                System.err.println("Error closing socket: " + e.getMessage());
            }
        }
    }

    private void sendErrorResponse(BufferedOutputStream out, int statusCode, String statusText) throws IOException {
        String response = "HTTP/1.1 " + statusCode + " " + statusText + "\r\n" +
                "Content-Length: 0\r\n" +
                "Connection: close\r\n" +
                "\r\n";
        out.write(response.getBytes());
        out.flush();
    }
}