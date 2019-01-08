/*
 * 
 * Lab 1 : Sockets and Thread Management
 * 
 * References Used:
 * for Lab 1:
 * [0] The Book: Computer Networks: A top-Down Approach By Kurose and Ross 6th Edition Chapter 2.7
 * [1] source code from here --> https://www.geeksforgeeks.org/introducing-threads-socket-programming-java/
 * [2] https://www.youtube.com/watch?v=vCDrGJWqR8w
 * [3] https://www.youtube.com/watch?v=pr02nyPqLBU&index=38&list=PL27BCE863B6A864E3
 * [4] https://www.youtube.com/watch?v=8lXI4YIIR9k
 * [5] https://www.oracle.com/technetwork/java/socket-140484.html
 * 
 * for Lab 2:
 * [6] source code --> https://www.youtube.com/watch?v=wm1O8EE0X8k
 * [7] https://docs.oracle.com/javase/7/docs/api/java/util/Queue.html
 * [8] http://tutorials.jenkov.com/java-collections/queue.html
 * [9] https://www.geeksforgeeks.org/queue-interface-java/
 * [10] http://www.drdobbs.com/windows/how-do-i-queue-java-threads/184410696
 * https://stackoverflow.com/questions/2332537/producer-consumer-threads-using-a-queue
 */

import java.io.*;
import java.net.*;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

//this class contains the main functions of the server and how the window looks like
public class Server2 extends JFrame    //to create a window or frame to show all messages
{
    public static final long CURRENT_TIME_MILLIS = System.currentTimeMillis();
	
	private JTextField userText;   //for user to type input text, we will not use it for the server, I made it for testing purposes
	private JTextArea chatWindow;   //for displaying messages sent and received
	private JPanel bottomPanel;    //to add both buttons to the bottom of window 
	private JButton clearButton, endButton;   // for the buttons
	private ObjectOutputStream output;  //a stream that flows from server to client
	private ObjectInputStream input;   //stream that flows from client to server
	private ServerSocket server;  //to create the socket and setup the server
	private Socket connection;  //to setup the connection between client and server
	private static int clientCounter=0;  //to count the clients connected
	
	static BlockingQueue<ServerThread> queue;   //create the queue for the serverThreads
	
	//this will be the consumer thread that will check the queue and take elements from it in a FIFO manner 
	Thread consumer = new Thread(new Runnable()
	{
		@Override
		public void run() {
			// TODO Auto-generated method stub
			//the thread will call the HandleQueue() function 
			chatWindow.append("\nInside consumer Thread\n");
			HandleQueue();
			chatWindow.append("\nExited consumer Thread\n");
		}

	});
	
	//constructor for the class
	public Server2()
	{
		super("Server Screen");   //title of the window
		
		userText = new JTextField();   //initializing userText
		
		/*this means by default before connected to anyone you cannot write anything because no one 
		is on the other side of the stream*/
		userText.setEditable(false);
		
		//to place the message input box at top of window
		add(userText, BorderLayout.NORTH);   
		
		//this is for testing purposes
		//whenever the user types a message and hits enter this happens
		userText.addActionListener(
				new ActionListener(){    //there is a listener that listens to textbox and wait for you to hit Enter button
					public void actionPerformed(ActionEvent event){   //once we hit Enter button
//						sendMessage(event.getActionCommand());  //this sends the message we typed in the textbox to the window and to the receiver(client)
						userText.setText("");   //to reset the message input box
					}
				}
			);
		
		chatWindow = new JTextArea();  //initializing the chatWindow
		chatWindow.setEditable(false);   //user cannot write in the chatWindow
		add(new JScrollPane(chatWindow));  //to add scroll bars to the window
		
		bottomPanel = new JPanel();     //initializing the bottomPanel
		
		// so when the buttons are added, they are placed in the middle of the panel
		bottomPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
		
		//add both buttons to the panel and each button has a label
		bottomPanel.add(clearButton = new JButton("Clear Window"));
		bottomPanel.add(endButton = new JButton("End Connection"));
		
		//to place the panel at the bottom of the window
		add(bottomPanel,BorderLayout.SOUTH);
		
		setSize(500,500);  //the size of the window
		setVisible(true);  //so the window can appear on our screen
	
		//this listens if the clearButton is clicked
		clearButton.addActionListener(new ActionListener() {
	        public void actionPerformed(ActionEvent e) {   //if it is clicked
	            chatWindow.setText("");                   //clear the chatWindow
	        }
	    });
		
		//listens if the endButton is clicked
		endButton.addActionListener(new ActionListener() {
	        public void actionPerformed(ActionEvent e) {    //if it is clicked
	        	try {
	        		//create an HTTPResponse with message END
	        		String HTTPResponse = "SERVER - HTTP/1.1 200 OK\r\nDate:" + LocalDateTime.now() + "\r\nServer: MyPC/2.0.52 (CentOS)\r\nLast-Modified: Tue, 30 Oct 2007 17:00:02 GMT\r\nAccept-Ranges: bytes\r\nContent-Length: 2652\r\nKeep-Alive: timeout=10, max=100\r\nConnection: Keep-Alive\r\nContent-Type: text/html; charset=ISO-8859-1\r\n\r\nEND";
					
	        		//write the HTTResponse to the output stream 
					output.writeObject("SERVER - " + HTTPResponse);
					chatWindow.append("\nServer Is Closing All Connections!\n");
					//output.flush();     //flush or empty the output buffer before closing the stream
					output.close();     //close the server's output stream
		        	input.close();      //close the server's input stream
		        	connection.close();  //close the socket
		        	server.close();     //close the server socket
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
	        }
	    });
		
		queue = new ArrayBlockingQueue<ServerThread>(100);   //this makes a FIFO thread-safe queue of size 100
		consumer.start();   //starts the consumer thread that will handle the queues
	}
	
	//here we create the class ServerThread to create threads for the multiple clients
	class ServerThread extends Thread 
	{
		Server2 server = null;   //to give the thread the same server socket
		Socket client = null;    //to give the thread the same socket connected to the client
		final ObjectInputStream input;   //to give the thread an input stream
		final ObjectOutputStream output;  //to give the thread an output stream
		int id;  //for giving an ID to each thread
		String message = "";
		Instant timeEntered, timeExited;
		
		//constructor for the class ServerThread
		//it will take the connectionSocket, counter, inputStream, outputStream, serverSocket as parameters
		//each thread will have its own parameters
		ServerThread(Socket client, int count, ObjectInputStream input, ObjectOutputStream output, Server2 server) throws IOException
		{
			//give the thread its own data from the parameters given
			this.client = client;
			this.server = server;
			this.id= count;	
			this.input = input;
			this.output = output;
			
			//give the thread a name using the id given in the form : CLIENT(id)
			this.setName("CLIENT("+this.id+")");
		}
		
		public void setMessage(String message)
		{
			this.message = message;
		}
		
		public String getMessage()
		{
			return this.message;
		}
		
		@Override
		public void run()    //this will run when we start a thread
		{
			try
			{
				//when we reach here, it means that the connection is established
				//all the thread does is exchange messages
				whileConnection();
				
				//if the message sent is END, it will close the thread's streams and sockets
				closeThread();
			}
			catch(IOException ioException)
			{
				chatWindow.append("\nERROR: "+ ioException+"\n");
			} catch (InterruptedException interruptedException) {
				// TODO Auto-generated catch block
				chatWindow.append("\nERROR: "+ interruptedException+"\n");
			}
		}
		
		//during the communication this function will happen
		private void whileConnection() throws IOException, InterruptedException
		{
			String message = "";  //to get the message from the input stream
			
			//to separate the message from the client's name
			//result[0] will carry CLIENT(id) 
			//result[1] will carry the HTTPResponse and the random number
			//result[2] will carry the message that was typed by the client
			String[] result = { "" , "" , ""};  
			
			//once we reach here we enable the user's ability to type message
			//userText.setEditable(true);  
			
			//we sent this message to notify the client that the server has connected to it
			message= " You can now exchange messages! \n";
			sendThreadMessage(message);
			
			//we stay in this do-while loop as long as the client  does not send END
			do
			{
				try
				{
					//reads data from the input stream
					message = (String)input.readObject();
					this.setMessage(message);
					result = message.split(" - ");
					this.timeEntered = Instant.now();
					ServerThread serverThreadproducer = new ServerThread(this.client, this.id, this.input, this.output, this.server);
					serverThreadproducer.message = this.message;
					serverThreadproducer.timeEntered = this.timeEntered;
					serverThreadproducer.setName(this.getName());
					queue.put(serverThreadproducer);
					chatWindow.append("\nElement added to queue!!\n");
				}
				catch (ClassNotFoundException classNotFoundException)
				{
					chatWindow.append("\nUnknown data sent from client!\n");
				}
			}while(!result[2].equalsIgnoreCase("END"));  //if the client sends END, the conversation will end
			//server will print that the client ended the connection and close the threads sockets and streams
			chatWindow.append("\nCLIENT ENDED CONNECTION!\n");
			closeThread();
		}
		
		//send message to client
		public void sendThreadMessage(String message)
		{
			try
			{
				//here we create the HTTPResponse and add the message to the data field
				String HTTPResponse = "SERVER - HTTP/1.1 200 OK\r\nDate:" + LocalDateTime.now() + "\r\nServer: MyPC/2.0.52 (CentOS)\r\nLast-Modified: Tue, 30 Oct 2007 17:00:02 GMT\r\nAccept-Ranges: bytes\r\nContent-Length: 2652\r\nKeep-Alive: timeout=10, max=100\r\nConnection: Keep-Alive\r\nContent-Type: text/html; charset=ISO-8859-1\r\n\r\n" + message;
				
				output.writeObject("SERVER - " + HTTPResponse);   //we send the HTTPResponse through the output stream
				output.flush();  //flush or empty the stream just in case any bytes are leftover in it
				
				//to display the HTTPResponse on the server window
				chatWindow.append("\n" + HTTPResponse + "\n"); 
			}
			catch(IOException ioException)
			{
				chatWindow.append("\nERROR: Can't send message! \n");
			}
		}
		
		//close streams and sockets after finish communication
		private void closeThread()
		{
			chatWindow.append("\n" + Thread.currentThread().getName() + " Is Closing All Connections... \n");   //this will appear on server screen
			//userText.setEditable(false);  //disable typing messages
			try
			{
				//close streams to/from server
				output.flush();
				output.close();
				input.close();
			}
			catch(IOException ioException)
			{
			ioException.printStackTrace();	
			}
		}	
	}
	
	//setup and run the server after the constructor finishes
	public void startRunning() throws IOException
	{
		//creates the server socket with port number 8080
		server = new ServerSocket(8080);  
				
		//if server is ok and runnning this will appear  
		chatWindow.append("\nServer Ready... \n");
				
		while(true)  //this will run forever
		{
			//must wait for someone to connect with server
			waitForConnection();
		}
	}
		
	//server waits for connection from clients
	private void waitForConnection() throws IOException
	{
		chatWindow.append("\nWaiting for client... \n");     //appears on the screen to notify that the server is running and all is well
		connection = server.accept();   //when client asks to connect with server, the server accepts the connection
		clientCounter++;   //we increase the counter, this counter will be sent with the create as an ID for the thread
			
		//print on server screen that a CLIENT(id) has connected
		chatWindow.append("\nCLIENT(" + clientCounter + ") now connected to " + connection.getInetAddress().getHostName()+"\n"); //shows IP address of server
		
		output = new ObjectOutputStream(connection.getOutputStream());  //creates the output stream to send to client
		output.flush();  //to remove leftover data in output buffer
			
		input = new ObjectInputStream(connection.getInputStream());  //creates the input stream to receive from client 
			
		chatWindow.append("\nStreams are now setup!\n");
			
		//create the server thread with its own socket, id, input stream and output stream
		ServerThread serverThread= new ServerThread(connection, clientCounter, input, output, this);
	       serverThread.start();  //to start the thread
	}

	public void HandleQueue() {	
		while(true)
		{
			try 
			{
				chatWindow.append("\nEntered HandleQueue()!!\n");
				Thread.sleep(100);
				ServerThread serverThreadConsumer = queue.take();
				
				//here we split the message at the - to separate the CLIENT(id) from the random number from the message sent
				String[] result = serverThreadConsumer.getMessage().split(" - ");
				String[] data = result[1].split("\r\n\r\n");
					
				//we convert the second part of the message to check if it is int
				int intValue = Integer.parseInt(data[1]);
					    
				//if it is not int, we will enter the catch(NumberFormatException)
				//if it is int, we will reach this line, disable the textbox and will print on server screen and send message to client.
				//then server will sleep for the value of the int sent
				//userText.setEditable(false);
				chatWindow.append("\n" + Thread.currentThread().getName() + " - " + result[1] + "\n");
				chatWindow.append("\nServer pausing for <"+intValue+ "> seconds...\n");
				serverThreadConsumer.sendThreadMessage("Server pausing for <"+intValue+ "> seconds...");
				Thread.sleep(intValue*1000);
				chatWindow.append("\nServer waited <" + intValue + "> seconds for " + serverThreadConsumer.getName() + "... \n");
				serverThreadConsumer.sendThreadMessage("Server waited <" + intValue + "> seconds for " + serverThreadConsumer.getName() + "...");
				serverThreadConsumer.timeExited = Instant.now();
				Duration timeElapsed = Duration.between(serverThreadConsumer.timeEntered, serverThreadConsumer.timeExited);
				long durationSeconds = timeElapsed.getSeconds();
					
				//send the parsed message to the client and how much time it waited
				serverThreadConsumer.sendThreadMessage("Received Message and Parsed it Successfully! You sent " + result[2] + ".\n");
				serverThreadConsumer.sendThreadMessage("You waited in queue for " + durationSeconds + " seconds.");
				
			}
			catch (InterruptedException ioException) 
			{
				// TODO Auto-generated catch block
				chatWindow.append("\nHandleQueue Error\n");
			}
		}
	}	
}

