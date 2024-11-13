import com.fastcgi.FCGIInterface;


import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.logging.Logger;
import java.util.logging.Level;


public class Main {
    public static void main(String[] args) throws IOException {
        Logger logger = Logger.getLogger(Main.class.getName());
        logger.setLevel(Level.ALL);
        logger.info("Logging has started...");
        long startTime = System.currentTimeMillis();
        String formattedCurTime = Instant.ofEpochMilli(System.currentTimeMillis())
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        logger.info(formattedCurTime);
        var httpResponse = "Content-Type: application/json\n" +
                "Content-Length: %d\n\n%s";
        var fcgiInterface = new FCGIInterface();
        while (fcgiInterface.FCGIaccept() >= 0){
            try{
                logger.info("Hello!");
                var method = FCGIInterface.request.params.getProperty("REQUEST_METHOD");
                HashMap<String, String> params = new HashMap<String, String>();
                if (method.equals("POST")) {
                    String[] data = readRequestBody().split("&");
                    logger.info("request: " + Arrays.toString(data));
                    for (String pair : data) {
                        String[] keyValue = pair.split("=");
                        if (keyValue.length == 2) {
                            params.put(keyValue[0], keyValue[1]);
                        } else {
                            throw new ValidationException("Неверное количество параметров");
                        }
                    }
                }
                if (method.equals("GET")) {
                    var query = FCGIInterface.request.params.getProperty("QUERY_STRING");
                    logger.info("отправленые данные: " + query);
// Проверяем, что query не null и не пустая
                    if (query != null && !query.isEmpty()) {
                        String[] data = query.split("&");
                        for (String pair : data) {
                            String[] keyValue = pair.split("=");

                            if (keyValue.length == 2) {
                                params.put(keyValue[0], keyValue[1]);
                            } else {
                                throw new ValidationException("Введены неправильные данные.");
                            }
                        }
                    }
                }
                // Проверяем, что все необходимые параметры присутствуют
                if (params.containsKey("x") && params.containsKey("y") && params.containsKey("r")) {
                    int x = Integer.parseInt(params.get("x"));
                    float y = Float.parseFloat(params.get("y"));
                    float r = Float.parseFloat(params.get("r"));
                    // Валидация параметров
                    validateParameters(x, y, r);
                    logger.info("перед answer: " + formattedCurTime);
                    String answer = String.format("{\"isHit\": %b, \"curTime\": \"%s\", \"dur\": %d}", CheckResult(x, y, r), formattedCurTime, System.currentTimeMillis() - startTime);
                    logger.info("answer: " + answer);
                    System.out.printf(httpResponse, answer.getBytes(StandardCharsets.UTF_8).length, answer);
                } else {
                    throw new ValidationException("Отсутствуют необходимые параметры.");
                }
            } catch (ValidationException e){
                String answer = String.format("{\"error\": %s}", e.toString());
                System.out.printf(httpResponse, answer.getBytes(StandardCharsets.UTF_8).length, answer);
            }

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
    public static boolean CheckResult(int x, float y, float r){
        if (x <= 0 && x >= -r && y >= 0 && y <= r){
            return true;
        }
        if (x >= 0 && y >=0 && 4*(x*x + y*y) <= r*r){
            return true;
        }
        if (x <= r && 2*y >= -r && 2*y <= x -2*r){
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
        int[] validX = {-3, -2, -1, 0, 1, 2, 3, 4, 5};
        boolean isXValid = false;
        for (float validValue : validX) {
            if (x == validValue) {
                isXValid = true;
                break;
            }
        }
        if (!isXValid) {
            throw new ValidationException("Значение x должно быть одним из следующих: {-3, -2, -1, 0, 1, 2, 3, 4, 5}");
        }

// Проверка условия для r
        float[] validR = {1.0f, 1.5f, 2.0f, 2.5f, 3.0f};
        boolean isRValid = false;
        for (float validValue : validR) {
            if (r == validValue) {
                isRValid = true;
                break;
            }
        }
        if (!isRValid) {
            throw new ValidationException("Значение r должно быть одним из следующих: {1.0, 1.5, 2.0, 2.5, 3.0}");
        }
    }
}