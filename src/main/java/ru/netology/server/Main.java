package ru.netology.server;

public class Main {
    public static void main(String[] args) {
        final Server server = new Server(9999);
        server.start();
    }
}