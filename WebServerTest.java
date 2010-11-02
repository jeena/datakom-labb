//
// Test program for WebServer
// (C) 2002-2005 Anders Gidenstam
//

import java.io.*;
import java.net.*;
import java.util.*;
import java.text.SimpleDateFormat;

public final class WebServerTest
{
    public static void main(String argv[]) throws Exception
    {
    // Set host and port number
    String host = "localhost";
    int    port = 0;

    if (argv.length > 1) {
        for (int c = 0; c < argv.length; c++) {
        if (argv[c].equals("-p")) {
            port = Integer.parseInt(argv[++c]);
        } else if (argv[c].equals("-h")) {
            host = argv[++c];
        } else {
            System.out.println("usage: WebServerTest [-p <port>] " +
                       "[-h <host>]");
            System.exit(0);
        }
        }
    } else if (argv.length != 0) {
        System.out.println("usage: WebServerTest [-p <port>] [-h <host>]");
        System.exit(0);
    }

    System.out.println("-----------------------------------------------");
    System.out.println("Testing methods on valid resource");
    System.out.println("-----------------------------------------------");
    Tests.httpRequest(host, port,
              Tests.HTTP_METHOD.GET, "/index.html");
    System.out.println("-----------------------------------------------");
    Tests.httpRequest(host, port,
              Tests.HTTP_METHOD.HEAD, "/index.html");
    System.out.println("-----------------------------------------------");
    Tests.httpRequest(host, port,
              "POST", "/index.html");
    System.out.println("-----------------------------------------------");
    Tests.httpRequest(host, port,
              "BLAHA", "/index.html");
    System.out.println("-----------------------------------------------");
    Tests.httpRequest(host, port,
              "GET", "/empty.txt");
    System.out.println("-----------------------------------------------");
        Tests.httpRequest(host, port,
              "HEAD", "/empty.txt");
    System.out.println("-----------------------------------------------");


    System.out.println("-----------------------------------------------");
    System.out.println("Testing methods on invalid resource");
    System.out.println("-----------------------------------------------");
    Tests.httpRequest(host, port,
              Tests.HTTP_METHOD.GET, "/nonexisting.html");
    System.out.println("-----------------------------------------------");
    Tests.httpRequest(host, port,
              Tests.HTTP_METHOD.HEAD, "/nonexisting.html");
    System.out.println("-----------------------------------------------");
    Tests.httpRequest(host, port,
              "BLAHA", "/nonexisting.html");
    System.out.println("-----------------------------------------------");

    System.out.println("-----------------------------------------------");
    System.out.println("Testing illegal request lines");
    System.out.println("-----------------------------------------------");
    Tests.httpRequest(host, port,
              Tests.HTTP_METHOD.GET, "this_is_actually_illegal");
    System.out.println("-----------------------------------------------");
    Tests.httpRequest(host, port,
              Tests.HTTP_METHOD.GET, "");
    System.out.println("-----------------------------------------------");
    Tests.illegalRequest(host, port,
                 Tests.HTTP_METHOD.GET + " /index.html" +
                 Tests.CRLF);
    System.out.println("-----------------------------------------------");
    Tests.illegalRequest(host, port,
                 "GET" + "/nonexisting.htmlHTTP/1.0 HTTP/1.0" +
                 Tests.CRLF);
    System.out.println("-----------------------------------------------");
    Tests.illegalRequest(host, port,
                 "GET /index.html HTTP/1.0 FLEBHL" +
                 Tests.CRLF);
    System.out.println("-----------------------------------------------");
    Tests.illegalRequest(host, port,
                 "HEAD /index.html FLEBHL HTTP/1.0" +
                 Tests.CRLF);
    System.out.println("-----------------------------------------------");
    
    System.out.println("-----------------------------------------------");
    System.out.println("Testing concurrent requests and content types");
    System.out.println("-----------------------------------------------");
    HTTPRequest r1 = new HTTPRequest(host, port,
                     Tests.HTTP_METHOD.GET, "/pic.jpg");
    HTTPRequest r2 = new HTTPRequest(host, port,
                     Tests.HTTP_METHOD.GET, "/pic.gif");
    HTTPRequest r3 = new HTTPRequest(host, port,
                     Tests.HTTP_METHOD.GET, "/pic.png");
    HTTPRequest r4 = new HTTPRequest(host, port,
                     Tests.HTTP_METHOD.GET, "/pic.bin");

    // Finishing the requests in the reverse order will
    // deadlock with any non concurrent webserver. (Maybe..)
    System.out.println("R4:");
    r4.finish();
    System.out.println("R3:");
    r3.finish();
    System.out.println("R2:");
    r2.finish();
    System.out.println("R1:");
    r1.finish();

    System.out.println("-----------------------------------------------");
    System.out.println("Testing conditional GET");
    System.out.println("-----------------------------------------------");
    Tests.httpRequest2(host, port,
              Tests.HTTP_METHOD.GET,
              "/index.html",
              "If-Modified-Since: Mon, 12 Jul 2004 10:19:04 GMT");

    Date recently = new Date(new Date().getTime() - 5000);
    SimpleDateFormat rfc822date =
        new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'");
    rfc822date.setTimeZone(TimeZone.getTimeZone("GMT"));
    Tests.httpRequest2(host, port,
               Tests.HTTP_METHOD.GET,
               "/index.html",
               "If-Modified-Since: " +
               rfc822date.format(recently));
    
    Tests.httpRequest2(host, port,
              Tests.HTTP_METHOD.GET,
              "/index.html",
              "If-Modified-Since: Mon, 12 Jul 2006 10:19:04 GMT");
    


    System.out.println("-----------------------------------------------");
    }

}

final class Tests
{
    final static class HTTP_METHOD
    {
    final static String GET  = "GET";
    final static String HEAD = "HEAD";
    final static String POST = "POST";
    }

    final static String HTTPVERSION = "HTTP/1.0";
    final static String CRLF = "\r\n";

    static void httpRequest(String host,
                int    port,
                String method,
                String resource) throws Exception
    {
                  
    // Establish the connection
    System.out.println("----------------------------------------------");
    Socket socket = new Socket(InetAddress.getByName(host), port);
    System.out.println("----------------------------------------------");
    // Get the input and output streams of the socket.
    InputStream ins       = socket.getInputStream();
    DataOutputStream outs = new DataOutputStream(socket.getOutputStream());

    // Set up input stream filters
    BufferedReader br = new BufferedReader(new InputStreamReader(ins));

    // Make request
    String request =
        makeRequestLine(method, resource, HTTPVERSION, CRLF) +
        "User-Agent: WebServerTest/1.0" + CRLF +
        CRLF;

    // Print request
    System.out.println("> Sending request: ");
    System.out.print(request);
    // Send request
    outs.writeBytes(request);

    // Read and display response
    System.out.println("> Received response: ");
    String response = null;
    while ((response = br.readLine()) != null) {
        System.out.println(response);
    }
    }

    static void httpRequest2(String host,
                 int    port,
                 String method,
                 String resource,
                 String header_lines) throws Exception
    {
                  
    // Establish the connection
    Socket socket = new Socket(InetAddress.getByName(host),
                   port);
    // Get the input and output streams of the socket.
    InputStream ins       = socket.getInputStream();
    DataOutputStream outs = new DataOutputStream(socket.getOutputStream());

    // Set up input stream filters
    BufferedReader br = new BufferedReader(new InputStreamReader(ins));

    // Make request
    String request =
        makeRequestLine(method, resource, HTTPVERSION, CRLF) +
        "User-Agent: WebServerTest/1.0" + CRLF +
        header_lines + CRLF +
        CRLF;

    // Print request
    System.out.println("> Sending request: ");
    System.out.print(request);
    // Send request
    outs.writeBytes(request);

    // Read and display response
    System.out.println("> Received response: ");
    String response = null;
    while ((response = br.readLine()) != null) {
        System.out.println(response);
    }
    }

    static void illegalRequest(String host,
                   int    port,
                   String request) throws Exception
    {
                  
    // Establish the connection
    Socket socket = new Socket(InetAddress.getByName(host),
                   port);
    // Get the input and output streams of the socket.
    InputStream ins       = socket.getInputStream();
    DataOutputStream outs = new DataOutputStream(socket.getOutputStream());

    // Set up input stream filters
    BufferedReader br = new BufferedReader(new InputStreamReader(ins));

    // Add final CRLF to request
    request = request + CRLF;

    // Print request
    System.out.println("> Sending request: ");
    System.out.print(request);
    // Send request
    outs.writeBytes(request);

    // Read and display response
    System.out.println("> Received response: ");
    String response = null;
    while ((response = br.readLine()) != null) {
        System.out.println(response);
    }
    }

    static String makeRequestLine(String method,
                  String resource,
                  String version,
                  String crlf)
    {
    return method + " " + resource + " " + version + crlf;
    }

    static String makeHeaderLine(String header,
                 String value,
                 String crlf)
    {
    return header + ":" + value + crlf;
    }
}

final class HTTPRequest
{
    Socket socket;
    String request;
    DataOutputStream outs;
    BufferedReader br;

    HTTPRequest(String host,
        int    port,
        String method,
        String resource) throws Exception
    {
                  
    // Establish the connection
    socket = new Socket(InetAddress.getByName(host),
                port);
    // Get the input and output streams of the socket.
    InputStream ins       = socket.getInputStream();
    outs = new DataOutputStream(socket.getOutputStream());

    // Set up input stream filters
    br = new BufferedReader(new InputStreamReader(ins));

    // Make request
    request =
        Tests.makeRequestLine(method, resource, Tests.HTTPVERSION,
                  Tests.CRLF) +
        "User-Agent: WebServerTest/1.0" + Tests.CRLF +
        Tests.CRLF;
    }

    void finish() throws Exception
    {
    // Print request
    System.out.println("> Sending request: ");
    System.out.print(request);
    // Send request
    outs.writeBytes(request);

    // Read and display response
    System.out.println("> Received response: ");
    String response = null;
    while ((response = br.readLine()) != null) {
        System.out.println(response);
    }
    }
}
