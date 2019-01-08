/*
 * 
 * Lab 1 : Sockets and Thread Management
 * 
 * References Used:
 * for Lab 1:
 * [0] The Book: Computer Networks: A top-Down Approach By Kurose and Ross 6th Edition Chapter 2.7
 * [1] source code from here -->    https://www.geeksforgeeks.org/introducing-threads-socket-programming-java/
 * [2] https://www.youtube.com/watch?v=vCDrGJWqR8w
 * [3] https://www.youtube.com/watch?v=pr02nyPqLBU&index=38&list=PL27BCE863B6A864E3
 * [4] https://www.youtube.com/watch?v=8lXI4YIIR9k
 * [5] https://www.oracle.com/technetwork/java/socket-140484.html
 * 
 * for Lab 2:
 * [6] https://docs.oracle.com/javase/7/docs/api/java/util/Queue.html
 * [7] http://tutorials.jenkov.com/java-collections/queue.html
 * [8] https://www.geeksforgeeks.org/queue-interface-java/
 * [9] http://www.drdobbs.com/windows/how-do-i-queue-java-threads/184410696
 */

import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.Random;

//this class contains the main functions of the server and how the window looks like
public class Client extends JFrame
{
	private JTextField userText;   //for user to type input text
	private JTextArea chatWindow;   //for displaying messages sent and received
	private JPanel bottomPanel;    //to add both buttons to the bottom of window 
	private JButton clearButton, endButton;   // for the buttons 
	private ObjectOutputStream output;  //a stream that flows from server to client
	private ObjectInputStream input;   //stream that flows from client to server
	private String message = "";    //for the message the client will send
	private String serverIP;     //for the server IP address
	private Socket connection;   //to setup the connection between client and server
	
	//constructor
	public Client(String host)    //unlike the server this cannot be access by anyone, so we only pass IP address of server(host)
	{
		super("Client Screen!");   //name of the client window
		serverIP= host;            //to get the server IP address which is the local host we are working on  127.0.0.1
		
		userText= new JTextField();   //creating the textBox for the user to type messages in
		add(userText, BorderLayout.NORTH);  //place the textBox at the top of the window

		userText.addActionListener(
				new ActionListener(){    //there is a listener that listens to textbox and wait for you to hit Enter button
					public void actionPerformed(ActionEvent event){   //once we hit Enter button
						
						//to generate a random number between 3 and 10 send it to the server to wait
						Random random = new Random(); 
						int randomNumber = random.nextInt(10-3) + 3;       //   https://stackoverflow.com/questions/5887709/getting-random-numbers-in-java
						sendMessage(String.valueOf(randomNumber)+ " - " +event.getActionCommand());    //we will send the number first to the server to wait and then the message printed

						userText.setText("");   //to reset the message input box
					}
				}
			);
		
		chatWindow = new JTextArea();   //to create the chatWindow to show all messages exchange between client and server
		add(new JScrollPane(chatWindow));  //add scroll to the window
		chatWindow.setEditable(false);   //user cannot type in the window
		
		bottomPanel = new JPanel();    //initializing the bottomPanel
		
		// so when the buttons are added, they are placed in the middle of the panel
		bottomPanel.setLayout(new FlowLayout(FlowLayout.CENTER));   
		
		//add both buttons to the panel and each button has a label
		bottomPanel.add(clearButton = new JButton("Clear Window"));
		bottomPanel.add(endButton = new JButton("End Connection"));
		
		//to place the panel at the bottom of the window
		add(bottomPanel,BorderLayout.SOUTH);
		
		setSize(500,500);  //the size of the window
		setVisible(true);  //so the window can appear on our screen
		
		//when the 'Clear Window' button is clicked the window will be cleared
		clearButton.addActionListener(new ActionListener() {
	        public void actionPerformed(ActionEvent e) {
	            chatWindow.setText("");
	        }
	    });
		
		//when the 'End Connection' button is pressed, streams and socket will be closed and the other end will receive the END message
		endButton.addActionListener(new ActionListener() {
	        public void actionPerformed(ActionEvent e) {
	  
				sendMessage("0 - END");
	            closeConnection();
	        }
	    });
	}
	
	//connect to the server
	public void startRunning()
	{
		try
		{
			//to connect to the server 
			chatWindow.append("\nAttempting Connection... \n");
			
			//to connect to the server we need the IP address of the server and port number of the server socket
			connection = new Socket(InetAddress.getByName(serverIP), 8080);
			
			//will notify the user that the connection was successful
			chatWindow.append("\nConnected to: "+ connection.getInetAddress().getHostName()+"\n");
	
			//create the streams
			output = new ObjectOutputStream(connection.getOutputStream());   //create the output stream (to send from the client to the server)
			output.flush();   //to make sure the buffer is empty
			input = new ObjectInputStream(connection.getInputStream());   //to create the input stream (from the server to the client)
			chatWindow.append("\nStreams are open! You are connected! \n");   //to notify the user that stream creation was successful
			
			userText.setEditable(true);    // enables the textBox for the user to type
			
			//make the conversation
			whileConnection();
		}
		catch(EOFException eofException)
		{
			chatWindow.append("\nClient terminating connection... \n");
		}
		catch(IOException ioException)
		{
			ioException.printStackTrace();
		}
		finally
		{
			//if we cannot connect to the server, we will close the connections
			//for example, we run the client but there is no server running, this will happen
			closeConnection();
		}
	}
	
	//during the conversation with server
	private void whileConnection()throws IOException
	{
		String result[] = { "" , "" };  //for spliting the message sent by the server
		
		//in this loop we read from the server as long as the server does not send END 
		do
		{
			try
			{
				message = (String) input.readObject();  //read whatever message sent from server
				result = message.split("\r\n\r\n");   //separate the HTTPResponse from the message the server sent
				
				chatWindow.append("\nSERVER - " + result[1] + "\n");    //displays the message
			}
			catch(ClassNotFoundException classNotFoundException)
			{
				chatWindow.append("\nUnknown Message! \n");
			}
		}while(!result[1].equalsIgnoreCase("END"));
		//if the server sent END, we will exit the loop
		
		// this will appear on the client window to notify that the server ended the connection
		chatWindow.append("\nSERVER ENDED CONNECTION!\n");  
		userText.setEditable(false);   //writing in the textBox will be disabled
		closeConnection();   //sockets and streams will be closed on the client side
	}
	
	//close the connection and streams
	private void closeConnection()
	{
		chatWindow.append("\nClosing Connection... \n");  //this will appear on the window
		userText.setEditable(false);   //writing in the textBox will be disabled
		try
		{
			output.flush();   //to make sure that there is nothing left in the output buffer
			output.close();  //to close the output stream 
			input.close();    //to close the input stream
			connection.close();   //to close the socket
		}
		catch(IOException ioException)
		{
			ioException.printStackTrace();
		}
	}
	
	//send message to server
	private void sendMessage(String message)
	{	
		try 
		{
			//the client can send either a random number or an HTTPRequest
			//first we must know what was sent by trying to convert the message to an int
			String HTTPRequest="POST / HTTP/1.1 \r\nHost: localhost\r\nUser-Agent: my browser\r\nAccept: text/html,application/xhtml+xml,application/xml;\r\nAccept-Language: en-gb,en;q=0.5\r\nAccept-Encoding: gzip, deflate\r\nConnection: keep-alive\r\nContent-Type: application/x-www-form-urlencoded\r\nDate: "+ LocalDateTime.now() +"\r\nContent-Length: "+message.length()+"\r\n\r\n"+message;
			
			//if the above line is successful, we will reach this and write the message to the ouput stream
			output.writeObject("CLIENT - "+ HTTPRequest);
			output.flush();   //to clear the output buffer
		} 
		catch (IOException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//this will show the message that was sent by the client on the chatWindow 
		chatWindow.append("\nCLIENT - " + message + "\n");
	}
}