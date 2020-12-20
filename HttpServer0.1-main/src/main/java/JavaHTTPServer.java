import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.StringTokenizer;


// The tutorial can be found just here on the SSaurel's Blog : 
// Each Client Connection will be managed in a dedicated Thread
public class JavaHTTPServer implements Runnable{ 
	
	static final File WEB_ROOT = new File("C:\\Users\\loren\\Desktop\\HttpServer0.1-main\\files");
	static final String DEFAULT_FILE = "index.html";
	static final String FILE_NOT_FOUND = "404.html";
	static final String METHOD_NOT_SUPPORTED = "not_supported.html";
        static final String PAGE_NOT_FOUND = "301.html";
        private static ResultSet resultSet = null; 
        private static Statement stmt;
        private static DataOutputStream outputServer;
        private static Connection conn;
	// port to listen connection
	static final int PORT = 3000;
	
	// verbose mode
	static final boolean verbose = true;
	
	// Client Connection via Socket Class
	private Socket connect;
	
	public JavaHTTPServer(Socket c) {
		connect = c;
	}
	
	public static void main(String[] args) {
		try {
			ServerSocket serverConnect = new ServerSocket(PORT);
			System.out.println("Server started.\nListening for connections on port : " + PORT + " ...\n");
			
			// we listen until user halts server execution
			while (true) {
				JavaHTTPServer myServer = new JavaHTTPServer(serverConnect.accept());
				
				if (verbose) {
					System.out.println("Connecton opened. (" + new Date() + ")");
				}
				
				// create dedicated thread to manage the client connection
				Thread thread = new Thread(myServer);
				thread.start();
			}
			
		} catch (IOException e) {
			System.err.println("Server Connection error : " + e.getMessage());
		}
	}

	@Override
	public void run() {
		// we manage our particular client connection
		BufferedReader in = null; PrintWriter out = null; BufferedOutputStream dataOut = null;
		String fileRequested = null;
		
		try {
			// we read characters from the client via input stream on the socket
			in = new BufferedReader(new InputStreamReader(connect.getInputStream()));
			// we get character output stream to client (for headers)
			out = new PrintWriter(connect.getOutputStream());
			// get binary output stream to client (for requested data)
			dataOut = new BufferedOutputStream(connect.getOutputStream());
			
			// get first line of the request from the client
			String input = in.readLine();
			// we parse the request with a string tokenizer
			StringTokenizer parse = new StringTokenizer(input);
			String method = parse.nextToken().toUpperCase(); // we get the HTTP method of the client
			// we get file requested
			fileRequested = parse.nextToken().toLowerCase();
			
			// we support only GET and HEAD methods, we check
			if (!method.equals("GET")  &&  !method.equals("HEAD")) {
				if (verbose) {
					System.out.println("501 Not Implemented : " + method + " method.");
				}

                                    // we return the not supported file to the client
                                    File file = new File(WEB_ROOT, METHOD_NOT_SUPPORTED);
                                    int fileLength = (int) file.length();
                                    String contentMimeType = "text/html";
                                    //read content to return to client
                                    byte[] fileData = readFileData(file, fileLength);
                                    

                                    // we send HTTP Headers with data to client
                                    out.println("HTTP/1.1 501 Not Implemented");
                                    out.println("Server: Java HTTP Server from SSaurel : 1.0");
                                    out.println("Date: " + new Date());
                                    out.println("Content-type: " + contentMimeType);
                                    out.println("Content-length: " + fileLength);
                                    out.println(); // blank line between headers and content, very important !
                                    out.flush(); // flush character output stream buffer
                                    // file
                                    dataOut.write(fileData, 0, fileLength);
                                    dataOut.flush();

                                } else {
				// GET or HEAD method
                                
				if (fileRequested.endsWith("/")) {
					fileRequested += DEFAULT_FILE;
				}else if(fileRequested.equals("/puntivendita.xml")){
                                    ObjectMapper objMap = new ObjectMapper();
                                    PuntiVendita pv = objMap.readValue(new File(WEB_ROOT+"/puntivendita.json"), PuntiVendita.class);
                                    XmlMapper xmlMapper = new XmlMapper();
                                    xmlMapper.writeValue(new File(WEB_ROOT+"/puntivendita.xml"),pv);
                                    File file = new File(WEB_ROOT+"/puntivendita.xml");
                                }else if(fileRequested.contains("db"))
                                {
                                    try{
                                      Class.forName("com.mysql.cj.jdbc.Driver").newInstance();
                                      conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/sys?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC", "root", "12345"); //Connect
                                      stmt = conn.createStatement();
                                      if(conn == null)
                                      {
                                         System.out.println("MySQL Server non è in esecuzione");
                                         return;
                                      }
                                      else
                                      {
                                            System.out.println("Connesso al server MySQL");
                                      }
                                      try
                                      {
                                         resultSet = stmt.executeQuery("SELECT * FROM prova"); 
                                      } 
                                      catch(SQLException e)
                                      {
                                        e.printStackTrace();
                                      }catch(Exception e){
                                        System.out.println(e.getMessage());
                                      }
                                    }catch(Exception e)
                                    {                     
                                        System.out.println( e.getMessage());
                                    }
                                    ArrayList<Prova> list = new ArrayList();
                                        try {
                                            while (resultSet.next()) {
                                               list.add(new Prova(resultSet.getString("nome"), resultSet.getString("cognome")));
                                        }
                                        } catch (Exception e) {
                                        }
                                    if(fileRequested.contains("/db.json"))
                                    {
                                        ObjectMapper objMap = new ObjectMapper();
                                        objMap.writeValue(new File(WEB_ROOT+"/Prova.json"),  list);
                                        fileRequested = "/Prova.json";
                                        File file = new File(WEB_ROOT+"/Prova.json");
                                    }else if(fileRequested.contains("/db.xml"))
                                    {
                                        XmlMapper xmlMapper = new XmlMapper();
                                        xmlMapper.writeValue(new File(WEB_ROOT+"/Prova.xml"), list);
                                        fileRequested = "/Prova.xml";
                                        File file = new File(WEB_ROOT+"/Prova.xml");
                                    }
                                }
				File file = new File(WEB_ROOT, fileRequested);
				int fileLength = (int) file.length();
				String content = getContentType(fileRequested);
				
				if (method.equals("GET")) { // GET method so we return content
					byte[] fileData = readFileData(file, fileLength);
					
					// send HTTP Headers
					out.println("HTTP/1.1 200 OK");
					out.println("Server: Java HTTP Server from SSaurel : 1.0");
					out.println("Date: " + new Date());
					out.println("Content-type: " + content);
					out.println("Content-length: " + fileLength);
					out.println(); // blank line between headers and content, very important !
					out.flush(); // flush character output stream buffer
					dataOut.write(fileData, 0, fileLength);
					dataOut.flush();
				}
				
				if (verbose) {
					System.out.println("File " + fileRequested + " of type " + content + " returned");
				}
				
			}
			
		} catch (FileNotFoundException fnfe) {
			try {
				fileNotFound(out, dataOut, fileRequested);
			} catch (IOException ioe) {
				System.err.println("Error with file not found exception : " + ioe.getMessage());
			}
			
		} catch (IOException ioe) {
			System.err.println("Server error : " + ioe);
		} finally {
			try {
				in.close();
				out.close();
				dataOut.close();
				connect.close(); // we close socket connection
			} catch (Exception e) {
				System.err.println("Error closing stream : " + e.getMessage());
			} 
			
			if (verbose) {
				System.out.println("Connection closed.\n");
			}
		}
		
		
	}
	
	private byte[] readFileData(File file, int fileLength) throws IOException {
		FileInputStream fileIn = null;
		byte[] fileData = new byte[fileLength];
		
		try {
			fileIn = new FileInputStream(file);
			fileIn.read(fileData);
		} finally {
			if (fileIn != null) 
				fileIn.close();
		}
		
		return fileData;
	}
	
	// return supported MIME Types
	private String getContentType(String fileRequested) {
		if (fileRequested.endsWith(".htm")  ||  fileRequested.endsWith(".html"))
			return "text/html";
                else if (fileRequested.endsWith(".xml"))
                    return "text/xml";
                else if (fileRequested.endsWith(".json"))
                    return "text/json";
		else
                    return "text/plain";   
	}
	
	private void fileNotFound(PrintWriter out, OutputStream dataOut, String fileRequested) throws IOException {
            String content = "text/html";
            if(fileRequested.endsWith(".html")){
		File file = new File(WEB_ROOT, FILE_NOT_FOUND);
		int fileLength = (int) file.length();
		byte[] fileData = readFileData(file, fileLength);
		
                
		out.println("HTTP/1.1 404 File Not Found");
		out.println("Server: Java HTTP Server from SSaurel : 1.0");
		out.println("Date: " + new Date());
		out.println("Content-type: " + content);
		out.println("Content-length: " + fileLength);
		out.println(); // blank line between headers and content, very important !
		out.flush(); // flush character output stream buffer
		
		dataOut.write(fileData, 0, fileLength);
                dataOut.flush();

                if (verbose) {
                    System.out.println("File " + fileRequested + " not found");
                }
        }else
                {
                    File file = new File(WEB_ROOT, PAGE_NOT_FOUND);
                    int fileLength = (int) file.length();
                    byte[] fileData = readFileData(file, fileLength);
                
                    out.println("HTTP/1.1 301 Page Not Found");
                    out.println("Server: Java HTTP Server from SSaurel : 1.0");
                    out.println("Date: " + new Date());
                    out.println("Content-type: " + content);
                    out.println("Content-length: " + fileLength);
                    out.println(); // blank line between headers and content, very important !
                    out.flush(); // flush character output stream buffer
                    
                    dataOut.write(fileData, 0, fileLength);
                    dataOut.flush();
		
                    if (verbose) {
                        System.out.println("Page " + fileRequested + " not found");
                    }
                }
	}
	
}