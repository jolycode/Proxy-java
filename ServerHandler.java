import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;
import java.io.*;

public class ServerHandler extends Thread {

    Socket clientSocket;
    DataInputStream dis;
    DataOutputStream dos;

    public ServerHandler(Socket clientSocket) throws Exception {
        this.clientSocket = clientSocket;
        dis = new DataInputStream(clientSocket.getInputStream());
        dos = new DataOutputStream(clientSocket.getOutputStream());
    }

    @Override
    public void run() {

        try {

            byte[] headerArr = new byte[5000];
            int hc = 0;

            // only for header part
            while (true) {
                byte i = (byte) dis.read();
                headerArr[hc++] = i;
                if (headerArr[hc - 1] == '\n' && headerArr[hc - 2] == '\r' && headerArr[hc - 3] == '\n'
                        && headerArr[hc - 4] == '\r') { // \r\n\r\n
                    break;
                }

            }

            String header = new String(headerArr, 0, hc);
            System.out.println("-------HEADER FROM CLIENT----");
            System.out.println(header);

            int fsp = header.indexOf(' ');
            int ssp = header.indexOf(' ', fsp + 1);
            int eol = header.indexOf("\r\n");

            String restHeader = header.substring(eol + 2);

            String fullpath = header.substring(fsp + 1, ssp);

            URL url = new URL(fullpath);

            String domain = url.getHost();
            String shortpath = url.getPath().equals("") ? "/" : url.getPath();

            System.out.println(domain);
            System.out.println(shortpath);
            
            String path= header.substring(fsp);
            
            if (path.equals("GET")) { 
            	if (domain.equals("www.facebook.com")) {
            		String html =   "<html>\r\n" +
                                    "<head>\r\n" +
                                        "<h1>401 Not Authorized</h1>\r\n" +
                                    "</head>\r\n" +
                                "</html>\r\n";
            		String response =   "HTTP/1.1 401 Not Authorized\r\n" +
                                    "Server: CSE471Proxy\r\n" +
                                    "Content-Type: text/html; charset=UTF-8\r\n" +
                                    "Content-Length: " + html.length() + "\r\n\r\n" + html;
                dos.writeBytes(response);
                clientSocket.close();
                
            }else{
            	handleProxy(restHeader, domain, shortpath);}
            }else {          
                   String html =   "<html>\r\n" +
                              "<head>\r\n" +
                              "<h1>405 Method Not Allowed CSE 471 Proxy Server</h1>\r\n" +
                              "</head>\r\n" +
                              "</html>\r\n";

               	String response =   "HTTP/1.1 Method not allowed\r\n" +
                        "Server: CSE471Proxy\r\n" +
                        "Content-Type: text/html; charset=UTF-8\r\n" +
                        "Date:"+getServerTime() +"\r\nContent-Length: " + html.length()+"\r\n\r\n" + html;
               	
               	


            
                   dos.writeBytes(response);
            }
            
            
            
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        

    }

    private void handleProxy(String restHeader, String domain, String shortpath) throws Exception {

        Socket proxiedSocket = new Socket(domain, 80);
        DataInputStream dis1 = new DataInputStream(proxiedSocket.getInputStream());
        DataOutputStream dos1 = new DataOutputStream(proxiedSocket.getOutputStream());

        // sent request to web server
        String normalHeader = "GET " + shortpath + " HTTP/1.1\r\n" + restHeader;
        System.out.println("-------HEADER TO WEBSERVER----");
        System.out.println(normalHeader);
        dos1.writeBytes(normalHeader);

        byte[] reponseArr = new byte[5000];
        int rc = 0;

        // only for header part
        while (true) {
            byte i = (byte) dis1.read();
            reponseArr[rc++] = i;
            if (reponseArr[rc - 1] == '\n' && reponseArr[rc - 2] == '\r' && reponseArr[rc - 3] == '\n'
                    && reponseArr[rc - 4] == '\r') { // \r\n\r\n
                break;
            }

        }

        String response = new String(reponseArr, 0, rc);
        System.out.println(response);

        int contIndex = response.indexOf("Content-Length: ");
        int eol = response.indexOf("\r\n", contIndex);
        String contSize = response.substring(contIndex + 16, eol);
        int contSizeInt = Integer.parseInt(contSize);

        System.out.println(contSizeInt);

        byte[] payload = new byte[contSizeInt];
        int pc = 0;

        byte[] buffer = new byte[1024];
        byte j;
//		while((j = (byte) dis1.read())!= -1) {
//			payload[pc++] = j;
//		}

        int sum = 0;
        int read;

        while ((read = dis1.read(buffer)) != -1) {

            System.arraycopy(buffer, 0, payload, sum, read);
            sum += read;
        }

        //System.out.println(new String(payload, 0, pc));

        // header part of response
        dos.writeBytes(response);

        // payload part of the response
        dos.write(payload);
        proxiedSocket.close();

    }
	 static String  getServerTime() {
		Calendar calendar = Calendar.getInstance();
		SimpleDateFormat dateFormat = new SimpleDateFormat(
				"EEE, dd MMM yyy HH:mmm:ss z", Locale.US);
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		return dateFormat.format(calendar.getTime());
	}

}
