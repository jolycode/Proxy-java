import java.net.*;
import java.io.*;

//curl -x http://127.0.0.1:8080 http://www.facebook.com

public class ProxyServer {

    public static void main(String[] args) throws Exception {
        ServerSocket s = new ServerSocket(8080);

        while(true) {
            Socket clientSocket = s.accept();
            new ServerHandler(clientSocket).start();
        }
    }


}
