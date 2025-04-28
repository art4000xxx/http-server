package ru.netology.server;

import java.io.BufferedOutputStream;
import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        Server server = new Server(9999);

        // Хендлер для GET /messages
        server.addHandler("GET", "/messages", (request, responseStream) -> {
            String response = "HTTP/1.1 200 OK\r\n" +
                    "Content-Length: 12\r\n" +
                    "Connection: close\r\n" +
                    "\r\n" +
                    "Hello, GET!";
            responseStream.write(response.getBytes());
            responseStream.flush();
        });

        // Хендлер для POST /messages
        server.addHandler("POST", "/messages", (request, responseStream) -> {
            String response = "HTTP/1.1 200 OK\r\n" +
                    "Content-Length: 13\r\n" +
                    "Connection: close\r\n" +
                    "\r\n" +
                    "Hello, POST!";
            responseStream.write(response.getBytes());
            responseStream.flush();
        });

        server.listen();
    }
}