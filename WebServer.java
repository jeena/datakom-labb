//
// Multithreaded Java WebServer
// (C) 2001 Anders Gidenstam
// (based on a lab in Computer Networking: ..)
//

import java.io.*;
import java.net.*;
import java.util.*;
import java.net.InetAddress.*;

public final class WebServer
{
	
	public static void main(String argv[]) throws Exception
	{
		// Set port number
		int port = 7000;

		// Establish the listening socket
		ServerSocket serverSocket = new ServerSocket(port);
		WebServer.log("Port number is: " + serverSocket.getLocalPort());


		// Wait for and process HTTP service requests
		while (true) {
			
			// Wait for TCP connection
			Socket requestSocket = serverSocket.accept();
			requestSocket.setSoLinger(true, 5);

			// Create an object to handle the request
			HttpRequest request  = new HttpRequest(requestSocket);

			//request.run()

			// Create a new thread for the request
			Thread thread = new Thread(request);

			// Start the thread
			thread.start();
		}
	}
	
	final static void log(String s)
	{
		System.out.println("LOG: " + s);
	}
	
	final static void errLog(String s)
	{
		System.err.println("ERR: " + s);
	}
	
	final static void errLog(Exception e)
	{
		e.printStackTrace();
	}
	
}

final class HttpRequest implements Runnable
{
	// Constants
	final static String HTTP_VERSION = "HTTP/1.0";
	final static String CRLF = "\r\n";

	//   Recognized HTTP methods
	final static class HttpMethod
	{
		final static String GET  = "GET";
		final static String HEAD = "HEAD";
		final static String POST = "POST";
	}
	
	final static class HttpResponse
	{
		final static String OK = HTTP_VERSION + " 200 OK " + CRLF;
		final static String NOT_FOUND = HTTP_VERSION + " 404 Not Found " + CRLF;
		final static String BAD_REQUEST = HTTP_VERSION + " 400 Bad Request " + CRLF;
		final static String INTERNAL_SERVER_ERROR = HTTP_VERSION + " 500 Internal Server Error " + CRLF;
		final static String NOT_IMPLEMENTED = HTTP_VERSION + " 501 Not Implemented " + CRLF;		
	}

	private Socket socket;
	private DataOutputStream outs;
	private BufferedReader br;
	

	// Constructor
	public HttpRequest(Socket socket) throws Exception
	{
			this.socket = socket;
			this.outs = new DataOutputStream(this.socket.getOutputStream());
			this.br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
	}

	// Implements the run() method of the Runnable interface
	public void run()
	{
			try {
				
			  processRequest();
			
			} catch (Exception e) {

				WebServer.errLog(e);
		
				try {

					DataOutputStream outs = new DataOutputStream(socket.getOutputStream());
					outs.writeChars(HttpResponse.INTERNAL_SERVER_ERROR);
					outs.writeChars(CRLF);
					outs.writeChars("<h1>500 Internal Server Error</h1>");
					
				} catch (Exception e2) {
					
					WebServer.errLog(e2);
					
				}
				
			} finally {
				
				// Close streams and sockets
				try	{
					
					br.close();
					outs.close();
					socket.close();	
					
				} catch (Exception e3) {
					
					WebServer.errLog(e3);
					
				}
			}
			
	}

	// Process a HTTP request
	private void processRequest() throws Exception
	{
		// Get the request line of the HTTP request
		String requestLine = br.readLine();
		
		if (requestLine == null) {
			WebServer.errLog("Request is empty");
			return;
		}
		
		// Display the request line
		WebServer.log("[Request] " + requestLine);

		// split on ASCII 32
		String[] tokens = requestLine.split(" ");
		
		String Request = tokens[0];
		if(tokens.length != 3 || tokens[0].length() == 0 || tokens[1].length() == 0 || tokens[2].length() == 0) {
					
			WebServer.errLog("Wrong number of arguments in request!");
			
			sendHeader(HttpResponse.BAD_REQUEST);
			sendText("<h1>400 Bas Request</h1>");
	
		} else if(tokens[1].charAt(0) != '/') {
				
			WebServer.errLog("Illegal URI");
			
			sendHeader(HttpResponse.NOT_IMPLEMENTED);
			sendText("<h1>501 Not Implemented</h1>");
			
		} else if((Request.equals(HttpMethod.GET) || Request.equals(HttpMethod.HEAD)) && tokens[2].equals(HTTP_VERSION)) {
					
			FileInputStream filein;
			try {
		
				File f = new File("." + tokens[1]);
				filein = new FileInputStream(f);
				
				sendHeader(HttpResponse.OK, f);
						
				if(Request.equals(HttpMethod.GET)) {
					sendBytes(filein, outs);	
				}
		
			} catch (FileNotFoundException e) {
				
				sendHeader(HttpResponse.NOT_FOUND);
				sendText("<h1>404 Not Found</h1>");
					
			}
	
		} else if(Request.equals(HttpMethod.POST)) {
					
			sendHeader(HttpResponse.NOT_IMPLEMENTED);
			sendText("<h1>501 Not Implemented</h1>");

		} else {
				
			WebServer.errLog("Illegal URI");
			
			sendHeader(HttpResponse.BAD_REQUEST);
			sendText("<h1>400 Bas Request</h1>");
			
		}

	}

	private void sendHeader(final String r, File f)
	{
		String response = r.toString();
		response += "Date: " + new Date().toString() + CRLF;
		response += "Server: Labbserver" + CRLF;
		response += "Allow: " + HttpMethod.GET + " " + HttpMethod.HEAD + CRLF;

		if (f != null) {
			
			response += "Content-Length: " + f.length() + CRLF;
			response += "Content-Type: " + contentType(f.getName()) + CRLF;
			response += "Last-Modified: " + new Date(f.lastModified()).toString() + CRLF;
			
		} else {
			
			// default content type
			response += "Content-Type: text/html" + CRLF;
			
		}
		
		response += CRLF;
		
		try {

			outs.writeChars(response);			

		} catch(Exception e) {

			WebServer.errLog(e);

		}
	}
	
	private void sendHeader(final String r)
	{
		sendHeader(r, null);		
	}
	
	private void sendText(String s)
	{	
		try {
			
			outs.writeChars(s);
			
		} catch(Exception e) {
			
			WebServer.errLog(e);
			
		}
				
	}
	
	private static void sendBytes(FileInputStream fins, OutputStream outs) throws Exception
	{
		// Coopy buffer
		byte[] buffer = new byte[1024];
		int	bytes = 0;

		while ((bytes = fins.read(buffer)) != -1) {
			outs.write(buffer, 0, bytes);
		}
		
	}

	private static String contentType(String fileName)
	{
		
		if (fileName.toLowerCase().endsWith(".htm") || fileName.toLowerCase().endsWith(".html")) {
			return "text/html";
		} else if (fileName.toLowerCase().endsWith(".gif")) {
			return "image/gif";
		} else if (fileName.toLowerCase().endsWith(".png")) {
			return "image/png";
		} else if (fileName.toLowerCase().endsWith(".jpg") || fileName.toLowerCase().endsWith(".jpeg")) {
			return "image/jpeg";
		} else {
			return "application/octet-stream";
		}
		
	}
	
}
