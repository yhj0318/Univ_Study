/*package Server;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class ServerOne
{
	private ServerSocket socket;
	private Socket clientSocket;
	private DataInputStream dis;
	private DataOutputStream dos;
	public ServerOne()
	{
		connect();
		send_Msg();
		recv_Msg();
		closeAll();
		System.out.println("bye!");
	}
	public void connect()
	{
		try
		{
			socket = new ServerSocket(1234);
			clientSocket = socket.accept();
			System.out.println("서버 : 연결 성공!!!");
			dis = new DataInputStream(clientSocket.getInputStream());
			dos = new DataOutputStream(clientSocket.getOutputStream());		
		}
		catch(IOException e) {e.printStackTrace();}
	}
	public void send_Msg()
	{
		try
		{
			Scanner in = new Scanner(System.in);
			System.out.println("서버 보내기>> ");
			String sendMsg = in.nextLine();
			dos.writeUTF(sendMsg);
		}
		catch(IOException e) {e.printStackTrace();}
	}
	public void recv_Msg()
	{
		try 
		{
			String recvMsg = dis.readUTF();
			System.out.println("클라이언트로 부터: " + recvMsg); 
		} 
		catch (IOException e) {e.printStackTrace();}
	}
	public void closeAll()
	{
		try 
		{
			socket.close();
			clientSocket.close();
			dis.close();
			dos.close();
		} 
		catch (IOException e) {e.printStackTrace();}
	}
	public static void main(String[] args)
	{
		new ServerOne();
	}
}*/
package Server;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class ServerOne
{
	private ServerSocket socket;
	private Socket clientSocket;
	private DataInputStream dis;
	private DataOutputStream dos;
	public ServerOne()
	{
		connect();
		send_Msg();
		recv_Msg();
		//closeAll();
		System.out.println("bye!");
	}
	public void connect()
	{
		try
		{
			socket = new ServerSocket(1234);
			clientSocket = socket.accept();
			System.out.println("서버 : 연결 성공!!!");
			dis = new DataInputStream(clientSocket.getInputStream());
			dos = new DataOutputStream(clientSocket.getOutputStream());		
		}
		catch(IOException e) {e.printStackTrace();}
	}
	public void send_Msg()
	{
		new Thread(new Runnable()
		{
			Scanner in = new Scanner(System.in);
			boolean isStay = true;
			public void run()
			{
				while(isStay)
				{
					try
					{
						String sendMsg = in.nextLine();
						if(sendMsg.equals("/bye"))
							isStay = false;
						dos.writeUTF(sendMsg);
					}
					catch(IOException e) {}
				}
			}
		}).start();
	}
	public void recv_Msg()
	{
		new Thread(new Runnable()
		{
			boolean isRecv = true;
			public void run()
			{
				while(isRecv)
				{
					try
					{
						String recvMsg = dis.readUTF();
						if(recvMsg.equals("/bye"))
							isRecv = false;
						else
							System.out.println("From 클라이언트 : " + recvMsg);
					}
					catch(IOException e) {}
				}
			}
		}).start();
	}
	public void closeAll()
	{
		try 
		{
			socket.close();
			clientSocket.close();
			dis.close();
			dos.close();
		} 
		catch (IOException e) {}
	}
	public static void main(String[] args) {
		new ServerOne();
	}
}