
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import java.net.InetSocketAddress;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.*;

public class FoodDeliveryServer {
    static AtomicInteger orderCounter = new AtomicInteger(1000);
    static List<String> orders = Collections.synchronizedList(new ArrayList<>());

    static String menu = """
    [
      {"id":1,"name":"Burger","price":120,"image":"https://images.unsplash.com/photo-1568901346375-23c9450c58cd?w=500"},
      {"id":2,"name":"Pizza","price":250,"image":"https://images.unsplash.com/photo-1513104890138-7c749659a591?w=500"},
      {"id":3,"name":"French Fries","price":90,"image":"https://images.unsplash.com/photo-1573080496219-bb080dd4f877?w=500"},
      {"id":4,"name":"Pasta","price":180,"image":"https://images.unsplash.com/photo-1473093295043-cdd812d0e601?w=500"},
      {"id":5,"name":"Sandwich","price":100,"image":"https://images.unsplash.com/photo-1528735602780-2552fd46c7af?w=500"},
      {"id":6,"name":"Dosa","price":80,"image":"https://www.cookwithmanali.com/wp-content/uploads/2020/05/Masala-Dosa-1014x1536.jpg"}
    ]
    """.replace(" ", "");

    static void send(HttpExchange ex, int status, String content,
                     String type) throws IOException {
        byte[] data = content.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", type);
        ex.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        ex.sendResponseHeaders(status, data.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(data);
        }
    }

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(
            new InetSocketAddress(8080), 0
        );

        server.createContext("/api/foods", ex -> {
            if (!ex.getRequestMethod().equals("GET")) {
                send(ex, 405, "{\"error\":\"Method not allowed\"}",
                     "application/json");
                return;
            }
            send(ex, 200, menu, "application/json");
        });

        server.createContext("/api/orders", ex -> {
            if (!ex.getRequestMethod().equals("POST")) {
                send(ex, 405, "{\"error\":\"Method not allowed\"}",
                     "application/json");
                return;
            }

            String body = new String(
                ex.getRequestBody().readAllBytes(),
                StandardCharsets.UTF_8
            );

            Matcher customerMatch = Pattern.compile(
                "\"customer\"\\s*:\\s*\"([^\"\\\\]{1,100})\""
            ).matcher(body);

            if (!customerMatch.find() ||
                customerMatch.group(1).trim().isEmpty()) {
                send(ex, 400, "{\"error\":\"Enter a valid name\"}",
                     "application/json");
                return;
            }

            Matcher itemMatcher = Pattern.compile(
                "\"id\"\\s*:\\s*(\\d+)\\s*,\\s*\"quantity\"\\s*:\\s*(\\d+)"
            ).matcher(body);

            int total = 0;
            int itemCount = 0;
            while (itemMatcher.find()) {
                int id = Integer.parseInt(itemMatcher.group(1));
                int quantity = Integer.parseInt(itemMatcher.group(2));
                int price = 0;

                if (quantity < 1 || quantity > 50) {
                    send(ex, 400, "{\"error\":\"Invalid quantity\"}",
                         "application/json");
                    return;
                }

                Matcher foodMatcher = Pattern.compile(
                    "\\{\"id\":" + id + ",\"name\":\"[^\"]*\",\"price\":(\\d+)"
                ).matcher(menu);

                if (!foodMatcher.find()) {
                    send(ex, 400, "{\"error\":\"Invalid food item\"}",
                         "application/json");
                    return;
                }

                price = Integer.parseInt(foodMatcher.group(1));
                total += price * quantity;
                itemCount++;
            }

            if (itemCount == 0 || itemCount > 20 || total > 100000) {
                send(ex, 400, "{\"error\":\"Invalid order\"}",
                     "application/json");
                return;
            }

            int orderId = orderCounter.incrementAndGet();
            String saved = "{\"orderId\":" + orderId +
                ",\"customer\":\"" + customerMatch.group(1) +
                "\",\"total\":" + total + "}";
            orders.add(saved);

            send(ex, 201,
                "{\"message\":\"Order placed successfully\"," +
                "\"orderId\":" + orderId +
                ",\"total\":" + total + "}",
                "application/json");
        });

        // Serve frontend files from the frontend folder.
        Path root = Paths.get("frontend").toAbsolutePath().normalize();

        server.createContext("/", ex -> {
            String path = ex.getRequestURI().getPath();
            if (path.equals("/")) path = "/index.html";

            Path file = root.resolve(path.substring(1))
                            .normalize();

            if (!file.startsWith(root) || !Files.isRegularFile(file)) {
                send(ex, 404, "File not found", "text/plain");
                return;
            }

            String type = "text/plain";
            if (path.endsWith(".html")) type = "text/html";
            if (path.endsWith(".css")) type = "text/css";
            if (path.endsWith(".js")) type = "text/javascript";

            send(ex, 200, Files.readString(file), type);
        });

        server.setExecutor(null);
        server.start();

        System.out.println("Food Delivery App is running!");
        System.out.println("Open http://localhost:8080");
    }
}