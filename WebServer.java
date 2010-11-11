//
// Multithreaded Java WebServer
// (C) 2001 Anders Gidenstam
// (based on a lab in Computer Networking: ..)
//

import java.io.*;
import java.net.*;
import java.util.*;
import java.net.InetAddress.*;
import java.text.SimpleDateFormat;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

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
		System.out.println("LOG: " + s.trim());
	}
	
	final static void errLog(String s)
	{
		System.err.println("ERR: " + s.trim());
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
		final static String OK = HTTP_VERSION + " 200 OK" + CRLF;
		final static String NOT_MODIFIED = HTTP_VERSION + " 304 Not Modified" + CRLF;
		final static String NOT_FOUND = HTTP_VERSION + " 404 Not Found" + CRLF;
		final static String BAD_REQUEST = HTTP_VERSION + " 400 Bad Request" + CRLF;
		final static String INTERNAL_SERVER_ERROR = HTTP_VERSION + " 500 Internal Server Error" + CRLF;
		final static String NOT_IMPLEMENTED = HTTP_VERSION + " 501 Not Implemented" + CRLF;		
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
		
		// get headers
		String h = null;
		Map<String, String> headers = new HashMap<String, String>();

		while	(true) {
			h = br.readLine();

			if (h.length() == 0) break;

			String[] a = h.split(":", 2);
			if (a != null && a.length == 2 && a[0] != null && a[0].length() > 0 && a[1] != null && a[1].length() > 1) {
				headers.put(a[0].toLowerCase().trim(), a[1].trim());
			}
		}
		
		if (requestLine == null) {
			WebServer.errLog("Request is empty");
			return;
		}
		
		// Display the request line
		WebServer.log("[Request] " + requestLine);

		// split on ASCII 32
		String[] tokens = requestLine.split(" ");
		
		String request = tokens[0];
		
		boolean isAHTTPVersion = false;
		if (tokens.length == 3 && tokens[2].length() > 7) {
			Pattern pattern = Pattern.compile("HTTP/\\d\\.\\d");
			Matcher matcher = pattern.matcher(tokens[2]);
			isAHTTPVersion = matcher.find();
		}
		
		
		if(tokens.length != 3 || tokens[0].length() == 0 || tokens[1].length() == 0 || tokens[2].length() == 0 || !isAHTTPVersion) {
					
			WebServer.errLog("Wrong number of arguments in request!");
			WebServer.errLog(HttpResponse.BAD_REQUEST);
			
			sendHeader(HttpResponse.BAD_REQUEST);
			sendText("<h1>400 Bad Request</h1>");
	
		} else if(!tokens[2].equals("HTTP/1.0")) {
			
			WebServer.errLog(HttpResponse.NOT_IMPLEMENTED);

			sendHeader(HttpResponse.NOT_IMPLEMENTED);
			sendText("<h1>501 Not Implemented</h1>");			
			
		} else if(tokens[1].charAt(0) != '/') {
				
			WebServer.errLog(HttpResponse.BAD_REQUEST);
			
			sendHeader(HttpResponse.BAD_REQUEST);
			sendText("<h1>400 Bad Request</h1>");
			
		} else if((request.equals(HttpMethod.GET) || request.equals(HttpMethod.HEAD)) && tokens[2].equals(HTTP_VERSION)) {
					
			FileInputStream filein;
			try {
		
				File f = new File("." + tokens[1]);
				
				// check if the file has been modified since
				boolean modifiedSince = true;
				if (headers.containsKey("if-modified-since")) {

					String dateString = headers.get("if-modified-since");
					SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");

					Date date = format.parse(dateString);
					Date modified = new Date(f.lastModified());

					modifiedSince = modified.after(date);
				}
				
				if (!modifiedSince) {
					
					WebServer.log(HttpResponse.NOT_MODIFIED);
					
					sendHeader(HttpResponse.NOT_MODIFIED);
					sendText("<h1>304 Not Modified</h1>");
					
				} else {
					
					filein = new FileInputStream(f);

					WebServer.log(HttpResponse.OK);

					sendHeader(HttpResponse.OK, f);

					if(request.equals(HttpMethod.GET)) {
						sendBytes(filein, outs);	
					}
					
				}
						
			} catch (FileNotFoundException e) {
				
				WebServer.errLog(HttpResponse.NOT_FOUND);
				
				sendHeader(HttpResponse.NOT_FOUND);
				sendText("<h1>404 Not Found</h1>");
					
			}
	
		} else if(request.equals(HttpMethod.POST)) {
					
			WebServer.errLog(HttpResponse.NOT_IMPLEMENTED);

			sendHeader(HttpResponse.NOT_IMPLEMENTED);
			sendText("<h1>501 Not Implemented</h1>");

		} else {
				
			WebServer.errLog(HttpResponse.BAD_REQUEST);
			
			sendHeader(HttpResponse.BAD_REQUEST);
			sendText("<h1>400 Bad Request</h1>");
			
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
