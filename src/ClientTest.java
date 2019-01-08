/*
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

import javax.swing.JFrame;

//this class basically creates an Object of class Client.java and runs it
public class ClientTest {
	public static void main(String[] args)
	{		
		//creation of the Client.java object
		Client client;
		client = new Client("127.0.0.1");  //this IP is the local host, the same computer I am at
		
		//to create an X button in the top right corner to allow us to exit the program using it
		client.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		//to run the object
		client.startRunning();
	}
}