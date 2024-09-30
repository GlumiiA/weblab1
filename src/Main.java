import com.fastcgi.FCGIInterface;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;

public class Main {
    public static void main(String[] args) throws IOException {
        FileWriter logger = new FileWriter("log.txt");
        logger.write("begin");
        long startTime = System.currentTimeMillis();
        var fcgiInterface = new FCGIInterface();
        var httpResponse = "Content-Type: application/json\n" +
                "Content-Length: %d\n\n%s";
        while (fcgiInterface.FCGIaccept() >= 0){
            try{
                logger.write("fcgiaccept");
                HashMap<String, String> params = new HashMap<String, String>();
                String[] data = readRequestBody().split("&");
                logger.write("request: " + Arrays.toString(data));
                for (String pair : data) {
                    String[] keyValue = pair.split("=");
                    params.put(keyValue[0], keyValue[1]);
                }
                int x = Integer.parseInt(params.get("x"));
                float y = Float.parseFloat(params.get("y"));
                float r = Float.parseFloat(params.get("r"));
                String answer = String.format("{\"isHit\": %b, \"curTime\": \"%s\", \"dur\": %d}", CheckResult(x,y,r), System.currentTimeMillis(), System.currentTimeMillis() - startTime);
                System.out.printf(httpResponse, answer.getBytes(StandardCharsets.UTF_8).length, answer);
            }catch (Exception e){
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
}