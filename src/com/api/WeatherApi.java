package com.api;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import org.json.JSONObject;
import org.json.JSONArray;

public class WeatherApi {
    private static final String BASE_URL = "http://api.openweathermap.org/data/2.5/weather?q=";
    private static final String FORECAST_URL = "http://api.openweathermap.org/data/2.5/forecast?q=";
    Properties props = new Properties();
    props.load(new FileInputStream("config.properties"));
    String apiKey = props.getProperty("api.key");
; 

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8085), 0);
        server.createContext("/weather", new WeatherHandler());
        server.createContext("/forecast", new ForecastHandler());
        server.createContext("/", new CORSHandler()); // Handle CORS for all other requests
        server.setExecutor(null); // Use the default executor
        System.out.println("Server is running on http://localhost:8085");
        server.start();
    }

    static class WeatherHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, OPTIONS");
                exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

                if ("OPTIONS".equals(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(204, -1); // No Content
                    return;
                }

                InputStream is = exchange.getRequestBody();
                String requestBody = new Scanner(is, StandardCharsets.UTF_8.name()).useDelimiter("\\A").next();
                String cityName = requestBody.split("=")[1];
                String response = fetchWeatherFromApi(cityName);

                exchange.sendResponseHeaders(200, response.getBytes().length);
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            } else {
                exchange.sendResponseHeaders(405, -1); // 405 Method Not Allowed
            }
        }

        private String fetchWeatherFromApi(String city) {
            try {
                String urlString = BASE_URL + city + "&appid=" + API_KEY + "&units=metric"; // Use metric units
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                int responseCode = connection.getResponseCode();
                if (responseCode == 200) { // HTTP OK
                    InputStream responseStream = connection.getInputStream();
                    String response = new Scanner(responseStream, StandardCharsets.UTF_8.name()).useDelimiter("\\A").next();
                    JSONObject jsonResponse = new JSONObject(response);

                    String cityName = jsonResponse.getString("name");
                    double temperature = jsonResponse.getJSONObject("main").getDouble("temp");
                    int humidity = jsonResponse.getJSONObject("main").getInt("humidity");
                    String condition = jsonResponse.getJSONArray("weather").getJSONObject(0).getString("description");

                    JSONObject result = new JSONObject();
                    result.put("city", cityName);
                    result.put("temperature", temperature);
                    result.put("condition", condition);
                    result.put("humidity", humidity);

                    return result.toString();
                } else {
                    return "{\"error\": \"Unable to fetch weather data for " + city + "\"}";
                }
            } catch (Exception e) {
                e.printStackTrace();
                return "{\"error\": \"" + e.getMessage() + "\"}";
            }
        }
    }

    static class ForecastHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equals(exchange.getRequestMethod())) {
                exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, OPTIONS");
                exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

                if ("OPTIONS".equals(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(204, -1); // No Content
                    return;
                }

                String queryParams = exchange.getRequestURI().getQuery();
                String cityName = queryParams.split("=")[1];
                String response = fetchForecastFromApi(cityName);

                exchange.sendResponseHeaders(200, response.getBytes().length);
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            } else {
                exchange.sendResponseHeaders(405, -1); // 405 Method Not Allowed
            }
        }

        private String fetchForecastFromApi(String city) {
            try {
                String urlString = FORECAST_URL + city + "&appid=" + API_KEY + "&units=metric"; // Use metric units
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                int responseCode = connection.getResponseCode();
                if (responseCode == 200) { // HTTP OK
                    InputStream responseStream = connection.getInputStream();
                    String response = new Scanner(responseStream, StandardCharsets.UTF_8.name()).useDelimiter("\\A").next();
                    JSONObject jsonResponse = new JSONObject(response);
                    JSONArray forecastArray = jsonResponse.getJSONArray("list");

                    JSONArray resultArray = new JSONArray();
                    for (int i = 0; i < forecastArray.length(); i += 8) { // Get daily forecast (every 8 entries)
                        JSONObject dayForecast = forecastArray.getJSONObject(i);
                        String date = dayForecast.getString("dt_txt");
                        double temperature = dayForecast.getJSONObject("main").getDouble("temp");
                        String weather = dayForecast.getJSONArray("weather").getJSONObject(0).getString("description");

                        JSONObject dayResult = new JSONObject();
                        dayResult.put("date", date);
                        dayResult.put("temperature", temperature);
                        dayResult.put("weather", weather);

                        resultArray.put(dayResult);
                    }

                    JSONObject result = new JSONObject();
                    result.put("city", city);
                    result.put("forecast", resultArray);

                    return result.toString();
                } else {
                    return "{\"error\": \"Unable to fetch forecast data for " + city + "\"}";
                }
            } catch (Exception e) {
                e.printStackTrace();
                return "{\"error\": \"" + e.getMessage() + "\"}";
            }
        }
    }

    static class CORSHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1); // No Content
            } else {
                exchange.sendResponseHeaders(404, -1); // Not Found
            }
        }
    }
}
