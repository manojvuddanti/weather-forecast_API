package com.api;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class HelloWorldApi {
    public static void main(String[] args) throws IOException {
        // Create an HttpServer instance bound to port 8080
        HttpServer server = HttpServer.create(new InetSocketAddress(8085), 0);

        // Define the context and handler for the "hello" endpoint
        server.createContext("/hello", new HelloHandler());

        // Start the server
        server.setExecutor(null); // Use the default executor
        System.out.println("Server is running on http://localhost:8080/hello");
        server.start();
    }

    // Define a handler to process HTTP requests
    static class HelloHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equals(exchange.getRequestMethod())) {
                // Response content
                String response = "Hello, World!";

                // Set the response headers and status code
                exchange.sendResponseHeaders(200, response.getBytes().length);

                // Write the response body
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            } else {
                // Method not allowed
                exchange.sendResponseHeaders(405, -1); // 405 Method Not Allowed
            }
        }
    }
}
