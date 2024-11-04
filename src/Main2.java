import com.fastcgi.FCGIInterface;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;

public class Main {
    public static void main(String[] args) throws IOException {
        long startTime = System.currentTimeMillis();
        LocalDate currentDate = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        String formattedDate = currentDate.format(formatter);
        var fcgiInterface = new FCGIInterface();
        var httpResponse = "Content-Type: application/json\n" + "Content-Length: %d\n\n%s";
        while (fcgiInterface.FCGIaccept() >= 0) {
            String answer = String.format("{\"error\": %s}", "Ошибка.");
            try {
                HashMap<String, String> params = new HashMap<String, String>();
                String query = FCGIInterface.request.params.getProperty("QUERY_STRING");
// Проверяем, что query не null и не пустая
                if (query != null && !query.isEmpty()) {
                    String[] data = query.split("&");
                    for (String pair : data) {
                        String[] keyValue = pair.split("=");

                        if (keyValue.length == 2) {
                            params.put(keyValue[0], keyValue[1]);
                        } else {
                            answer = String.format("{\"error\": %s}", "Введены неправильные данные.");
                            System.out.printf(httpResponse, answer.getBytes(StandardCharsets.UTF_8).length, answer);
                            break;
                        }
                    }

                    // Проверяем, что все необходимые параметры присутствуют
                    if (params.containsKey("x") && params.containsKey("y") && params.containsKey("r")) {
                        try {
                            int x = Integer.parseInt(params.get("x"));
                            float y = Float.parseFloat(params.get("y"));
                            float r = Float.parseFloat(params.get("r"));

                            // Валидация параметров
                            validateParameters(x, y, r);

                            // Выполнение основной логики
                            answer = String.format("{\"isHit\": %b, \"curTime\": \"%s\", \"dur\": %d}",
                                    CheckResult(x, y, r), formattedDate, System.currentTimeMillis() - startTime);
                        } catch (NumberFormatException e) {
                            answer = String.format("{\"error\": %s}", "Введены неправильные данные.");
                        } catch (ValidationException e) {
                            answer = String.format("{\"error\": %s}", "Ошибка валидации. Переданные параметры не удовлетворяют условиям.");
                        }
                    } else {
                        answer = String.format("{\"error\": %s}", "Отсутствуют необходимые параметры.");
                    }
                } else {
                    answer = String.format("{\"error\": %s}", "Параметры запроса не указаны.");
                }
            } catch (NumberFormatException e) {
                answer = String.format("{\"error\": %s}", "Введены неправильные даные.");
            }
            System.out.printf(httpResponse, answer.getBytes(StandardCharsets.UTF_8).length, answer);
        }
    }

private static String readRequestBody() throws IOException {
    FCGIInterface.request.inStream.fill();
    var contentLength = FCGIInterface.request.inStream.available();
    var buffer = ByteBuffer.allocate(contentLength);
    var readBytes =
            FCGIInterface.request.inStream.read(buffer.array(), 0,
                    contentLength);
    var requestBodyRaw = new byte[readBytes];
    buffer.get(requestBodyRaw);
    buffer.clear();
    return new String(requestBodyRaw, StandardCharsets.UTF_8);
}

public static boolean CheckResult(int x, float y, float r) {
    if (x <= 0 && x >= -r && y >= 0 && y <= r) {
        return true;
    }
    if (x >= 0 && y >= 0 && 4 * (x * x + y * y) <= r * r) {
        return true;
    }
    if (x <= r && 2 * y >= -r && 2 * y <= x - 2 * r) {
        return true;
    }
    return false;
}

public static void validateParameters(int x, float y, float r) throws ValidationException {
// Проверка условия для y
    if (y <= -3 || y >= 5) {
        throw new ValidationException("Значение y должно быть в диапазоне (-3, 5)");
    }

// Проверка условия для x
    if (x < -3 || x > 5) {
        throw new ValidationException("Значение x должно быть в диапазоне {-3, -2, -1, 0, 1, 2, 3, 4, 5}");
    }

// Проверка условия для r
    float[] validR = {1.0f, 1.5f, 2.0f, 2.5f};
    boolean isRValid = false;
    for (float validValue : validR) {
        if (r == validValue) {
            isRValid = true;
            break;
        }
    }
    if (!isRValid) {
        throw new ValidationException("Значение r должно быть одним из следующих: {1.0, 1.5, 2.0, 2.5}");
    }
}
}
