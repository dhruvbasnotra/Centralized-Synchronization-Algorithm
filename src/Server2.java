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
	
	

