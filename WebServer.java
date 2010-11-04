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
		int port = 0;

		// Establish the listening socket
		ServerSocket serverSocket = new ServerSocket(port);
		System.out.println("Port number is: " + serverSocket.getLocalPort());


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
}

final class HttpRequest implements Runnable
{
	// Constants
	final static String HTTP_VERSION = "HTTP/1.0";
	final static String CRLF = "\r\n";

	//   Recognized HTTP methods
	final static class HTTP_METHOD
	{
		final static String GET  = "GET";
		final static String HEAD = "HEAD";
		final static String POST = "POST";
	}
	
	final static class HTTP_RESPONSE
	{
		final static String OK = HTTP_VERSION + " 200 OK " + CRLF;
		final static String NOT_FOUND = HTTP_VERSION + " 404 Not Found " + CRLF;
		final static String BAD_REQUEST = HTTP_VERSION + " 400 Bad Request " + CRLF;
		final static String INTERNAL_SERVER_ERROR = HTTP_VERSION + " 500 Internal Server Error " + CRLF;
		final static String NOT_IMPLEMENTED = HTTP_VERSION + " 501 Not Implemented " + CRLF;		
	}

	Socket socket;
	

	// Constructor
	public HttpRequest(Socket socket) throws Exception
	{
			this.socket = socket;
	}

	// Implements the run() method of the Runnable interface
	public void run()
	{
			try {
			  processRequest();
			} catch (Exception e) {

				System.err.println(e);
		
				try {

					DataOutputStream outs = new DataOutputStream(socket.getOutputStream());
					outs.writeChars(HTTP_RESPONSE.INTERNAL_SERVER_ERROR);
					outs.writeChars(CRLF);
					outs.writeChars("<h1>500 Internal Server Error</h1>");
					outs.close();
					socket.close();
					
				} catch (Exception e2) {
					System.err.println(e2);
				}
			}
	}

	// Process a HTTP request
	private void processRequest() throws Exception
	{
		// Get the input and output streams of the socket.
		InputStream ins	   = socket.getInputStream();
		DataOutputStream outs = new DataOutputStream(socket.getOutputStream());

		// Set up input stream filters
		BufferedReader br = new BufferedReader(new InputStreamReader(ins));
		Date d = new Date();

		// Get the request line of the HTTP request
		String requestLine = br.readLine();
	
		// Display the request line
		System.out.println();
		System.out.println("Request:");
		System.out.println("  " + requestLine);

		String[] tokens = requestLine.split("\\s+");
		//System.out.println("-->"+tokens.length);
				
		String Request = tokens[0];
		if(tokens.length != 3) {
					
			System.err.println("Wrong number of arguments in request!");
			outs.writeChars(HTTP_RESPONSE.BAD_REQUEST + getDateHeader(d));
			outs.writeChars(CRLF);
			outs.writeChars("<h1>400 Bas Request</h1>");
	
		} else if(tokens[1].charAt(0) != '/') {
				
			System.err.println("illegal url");
			outs.writeChars(HTTP_RESPONSE.NOT_IMPLEMENTED + getDateHeader(d));
			outs.writeChars(CRLF);
			outs.writeChars("<h1>501 Not Implemented</h1>");
			
	
		} else if(Request.equals(HTTP_METHOD.GET) || Request.equals(HTTP_METHOD.HEAD)) {
					
			FileInputStream filein;
			try {
		
				File f = new File("." +tokens[1]);
				filein = new FileInputStream(f);
				String response = createHeader(d,f);
				outs.writeChars(response);
						
				if(Request.equals(HTTP_METHOD.GET)) {
					sendBytes(filein, outs);	
				}
		
			} catch (FileNotFoundException e) {
				
					outs.writeChars(HTTP_RESPONSE.NOT_FOUND + getDateHeader(d));
					outs.writeChars(CRLF);
					outs.writeChars("<h1>404 Not Found</h1>");
					
			}
	
		} else if(Request.equals(HTTP_METHOD.POST)) {
					
			outs.writeChars(HTTP_RESPONSE.NOT_IMPLEMENTED + getDateHeader(d));
			outs.writeChars(CRLF);
			outs.writeChars("<h1>501 Not Implemented</h1>");
	
		} else {
				
			System.err.println("illegal url");
			outs.writeChars(HTTP_RESPONSE.BAD_REQUEST + getDateHeader(d));		
			outs.writeChars(CRLF);
			outs.writeChars("<h1>400 Bas Request</h1>");
			
		}
	
	
		// Close streams and sockets
		outs.close();
		br.close();
		socket.close();
	}

	private String getDateHeader(Date d){
		return "Date: " + d.toString() + CRLF;
	}

	private String createHeader(Date d, File f){
		String response = HTTP_RESPONSE.OK;
		response += getDateHeader(d);
		response += "Server: Labbserver" + CRLF;
		response += "Allow: " + HTTP_METHOD.GET + " " + HTTP_METHOD.HEAD+CRLF;
		response += "Content-Length: " + f.length() + CRLF;
		response += "Content-Type: " + contentType(f.getName()) + CRLF;
		response += "Last-Modified: " + new Date(f.lastModified()).toString() + CRLF;
		return response;
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
