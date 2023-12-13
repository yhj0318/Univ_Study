package server;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.border.EmptyBorder;
import javax.swing.text.DefaultCaret;

public class server extends JFrame implements ActionListener {
	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	private JTextField port_tf;
	private JTextArea textArea = new JTextArea();
	private JButton startBtn = new JButton("서버 실행");
	private JButton stopBtn = new JButton("서버 중지");
	private JToggleButton setModeBtn = new JToggleButton("다크모드");	// 다크모드를 위한 토글버튼
	private JScrollPane scrollPane;
	private JLabel lblNewLabel_2;
	Color labelBackColor = new Color(230,250,255);	// 라벨 백그라운드 컬러
	Color contentPaneBackColor = new Color(240,240,240);	// 컨탠트팬 백그라운드 컬러
	
	// socket 생성 연결 부분
	private ServerSocket ss; // server socket
	private Socket cs; // client socket
	int port = 12345;

	// 기타 변수 관리
	private Vector<ClientInfo> clientVC = new Vector<ClientInfo>();	// 클라이언트 정보를 관리하는 벡터
	private Vector<RoomInfo> roomVC = new Vector<RoomInfo>();	// 방을 관리하는 벡터

	public server() {
		initializeGUI();
		setupActionListeners(); // 11-13
	}

	public void initializeGUI() {
		setTitle("Server Application");

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(30, 100, 321, 370);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);
				
		lblNewLabel_2 = new JLabel("포트 번호");
		lblNewLabel_2.setBounds(12, 242, 60, 20);
		lblNewLabel_2.setOpaque(true);	// true로 해야지 배경색이 잘 나온다. 
		lblNewLabel_2.setHorizontalAlignment(JLabel.CENTER);	// 텍스트 중앙 정렬
		lblNewLabel_2.setBorder(BorderFactory.createLineBorder(Color.GRAY));	// 라벨 테두리 설정
		lblNewLabel_2.setBackground(labelBackColor);
		contentPane.add(lblNewLabel_2);
		
		port_tf = new JTextField();
		port_tf.setBounds(81, 242, 212, 21);
		contentPane.add(port_tf);
		port_tf.setColumns(10);

		startBtn.setBounds(12, 286, 90, 23);
		contentPane.add(startBtn);

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBounds(12, 10, 281, 210);
		contentPane.add(scrollPane);

		scrollPane.setViewportView(textArea);
		textArea.setEditable(false);
		
		stopBtn.setBounds(108, 286, 90, 23);
		contentPane.add(stopBtn);
		stopBtn.setEnabled(false);
		
		setModeBtn.setBounds(202, 286, 90, 23);
		contentPane.add(setModeBtn);
		setModeBtn.setEnabled(true);
		
		this.setVisible(true); // 화면 보이기
	}

	void setupActionListeners() { // 11-13
		startBtn.addActionListener(this);
		stopBtn.addActionListener(this);
		setModeBtn.addActionListener(this);
	}

	// addActionListener()에서 해당하는 버튼이 눌렸을 때 발생하는 이벤트이다.
	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == startBtn)
			startServer(); // 11-13
		else if (e.getSource() == stopBtn)
			stopServer();
		else if (e.getSource() == setModeBtn)
			darkMode();
	}
	
	/**
	 * 다크모드버튼이 눌리면 실행하는 함수이다.
	 * 버튼이 처음 눌리면 다크모드 실행, 다시 눌리면 화이트모드로 바뀐다.
	 */
	private void darkMode()
	{
		if(setModeBtn.isSelected())
		{
			contentPane.setBackground(Color.darkGray);
			textArea.setBackground(Color.gray);
			textArea.setForeground(Color.white);
		}
		else
		{
			contentPane.setBackground(contentPaneBackColor);
			textArea.setBackground(Color.white);
			textArea.setForeground(Color.black);
		}
	}
	
	/**
	 * 서버 실행 버튼이 눌리면 실행하는 함수이다.
	 * 포트번호를 받고 소켓을 열어 클라이언트가 
	 * 접속하길 기다리는 작업을 수행한다.
	 * 서버실행 버튼과 포트번호 텍스트필드가 비활성화되고, 
	 * 서버중지 버튼이 활성화 된다.
	 */
	private void startServer() { // 11-13
		try {
			port = Integer.parseInt(port_tf.getText().trim());
			ss = new ServerSocket(port);
			textArea.append("Server started on port: " + port + "\n");
			startBtn.setEnabled(false);
			port_tf.setEditable(false);
			stopBtn.setEnabled(true);
			waitForClientConnection();
		} catch (NumberFormatException e) {
			textArea.append("Invalid port number.\n");
		} catch (IOException e) {
			textArea.append("Error starting server: " + e.getMessage() + "\n");
		}
	}

	/**
	 * 서버중지 버튼이 눌렸을 때 실행하는 함수이다.
	 * 종료 시 소켓과 스트림을 모두 닫고 방에 대한 정보 또한 다 없앤다.
	 * 서버 실행 버튼과 포트번호 텍스트필드가 활성화 되고, 서버중지 버튼이 비활성화 된다.
	 */
	private void stopServer() { // 11-21

		// 서버 종료 시 모든 클라이언트에게 알림
		for (ClientInfo c : clientVC) {
			c.sendMsg("ServerShutdown/Bye");
			try {
				c.closeStreams();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}

		try {
			ss.close();
			roomVC.removeAllElements();
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		startBtn.setEnabled(true);
		port_tf.setEditable(true);
		stopBtn.setEnabled(false);

	}

	// 클라이언트를 기다리는 함수이다.
	private void waitForClientConnection() {
		new Thread(() -> {
			try {
				while (true) { // 11-22 n대m관계이기에 계속 받는다.
					textArea.append("클라이언트 Socket 접속 대기중\n");
					cs = ss.accept(); // cs를 분실하면 클라이언트와 통신이 불가능
					textArea.append("클아이언트 Socket 접속 완료\n");
					ClientInfo client = new ClientInfo(cs);
					client.start();		// ClientInfo에 대한 스레드이다.
				}
			} catch (IOException e) {
				textArea.append("Error accepting client connection: " + e.getMessage() + "\n");
			}
		}).start();		// watiForClientConnection에 대한 스레드이다.
	}

	/**
	 * n대m관계이기에 클라이언트별로 따로 관리한다.
	 * 전역변수로 선언하면 클라이언트별로 따로 구분되니 
	 * ClientInfo를 선언할 때 마다 데이터가 따로 관리된다.
	 */
	class ClientInfo extends Thread { // 11-21
		private DataInputStream dis;
		private DataOutputStream dos;
		private Socket clientSocket;
		private String clientID = ""; // client ID
		private String roomID = ""; // 11-28

		public ClientInfo(Socket socket) {
			try {
				this.clientSocket = socket;
				dis = new DataInputStream(clientSocket.getInputStream());
				dos = new DataOutputStream(clientSocket.getOutputStream());
			} catch (IOException e) {
				textArea.append("Error in communication: " + e.getMessage() + "\n");
			}
			clientCom();
		}

		// 클라이언트로부터 데이터 수신
		public void run() {
			try {
				String msg = "";
				while (true) {
					msg = dis.readUTF();
					recvMsg(msg);
				}
			} catch (IOException e) {
				// 클라이언트와의 연결이 끊어짐
				handleClientExitProtocol();
			}
		}

		private void clientCom() {
			while (true) {
				try {
					clientID = dis.readUTF(); // 클라이언트로부터 ID 수신
					
					// 중복 클라이언트 ID 검사
					boolean isDuplicate = false;
					for (int i = 0; i < clientVC.size(); i++) {
						ClientInfo c = clientVC.elementAt(i);
						if (c.clientID.equals(clientID)) {
							isDuplicate = true;
							break;
						}
					}

					if (isDuplicate) {
						// 중복된 클라이언트 ID인 경우 클라이언트에게 알림
						sendMsg("DuplicateClientID");
						// 클라이언트로부터 새로운 ID를 재전송받을 수 있도록 처리해야 합니다.
					} else {
						// 클라이언트 ID 중복 검사 패스
						sendMsg("GoodClientID");

						textArea.append("new Client: " + clientID + "\n");

						// 기존 클라이언트 정보를 새로운 클라이언트에게 알림
						for (int i = 0; i < clientVC.size(); i++) {
							ClientInfo c = clientVC.elementAt(i);
							//textArea.append("OldClient: " + c.clientID + "\n");
							sendMsg("OldClient/" + c.clientID);
						}

						// 중복이 아닌 경우 새 클라이언트 정보를 기존 클라이언트들에게 알림 (가입인사)
						broadCast("NewClient/" + clientID);

						// 자신에게 기존의 개설된 채팅 방 정보 전송
						for (RoomInfo r : roomVC) {
							sendMsg("OldRoom/" + r.roomName);
						}

						sendMsg("RoomJlistUpdate/Update"); // ZZZ1
						clientVC.add(this); // 신규 클라이언트 등록
						broadCast("ClientJlistUpdate/Update");
						break;
					}
				} catch (IOException e) {
					textArea.append("Error in communication: " + e.getMessage() + "\n");
				}
			}
		}

		// 클라이언트에게 메세지를 송신하는 함수이다.
		void sendMsg(String msg) {

			try {
				dos.writeUTF(msg);
			} catch (IOException e) {
			}
		}

		/**
		 * 클라이언트로부터 메세지를 수신받는 함수이다.
		 * if else문으로 해당하는 메세지에 맞는 작업을 수행한다.
		 */
		public void recvMsg(String str) { // 11-21

			textArea.append(clientID + "사용자로부터 수신한 메시지: " + str + "\n");
			System.out.println(clientID + "사용자로부터 수신한 메시지: " + str);
			StringTokenizer st = new StringTokenizer(str, "/");
			String protocol = st.nextToken();
			String message = st.nextToken();

			if ("Note".equals(protocol)) // 11-28
				handleNoteProtocol(st, message);
			else if ("CreateRoom".equals(protocol))
				handleCreateRoomProtocol(message);
			else if ("JoinRoom".equals(protocol))
				handleJoinRoomProtocol(st, message);
			else if ("SendMsg".equals(protocol))
				handleSendMsgProtocol(st, message);
			else if ("ClientExit".equals(protocol)) // System.out.println("ClientExit/Bye" + clientID);
				handleClientExitProtocol();
			else if ("ExitRoom".equals(protocol))
				handleExitRoomProtocol(message);
			else
				log("알 수 없는 프로토콜: " + protocol);
		}

		// 클라이언트로부터 쪽지 보내기 버튼이 눌리면 발생하는 이벤트이다.
		private void handleNoteProtocol(StringTokenizer st, String message) {
			String note = st.nextToken();
			// 해당 사용자에게 쪽지 전송
			for (ClientInfo c : clientVC) {
				if (c.clientID.equals(message)) {
					c.sendMsg("NoteS/" + clientID + "/" + note);
					break;
				}
			}
		}
		
		// 클라이언트로부터 방만들기 버튼이 눌리면 발생하는 이벤트이다.
		private void handleCreateRoomProtocol(String roomName) {

			// 방 이름이 이미 존재하는지 확인 //12-01
			boolean roomExists = false;
			for (RoomInfo r : roomVC) {
				if (r.roomName.equals(roomName)) {
					roomExists = true;
					break;
				}
			}
			if (roomExists) {
				// 동일한 이름의 방이 이미 존재하는 경우 처리
				sendMsg("CreateRoomFail/OK");
			} else {
				RoomInfo r = new RoomInfo(roomName, this);
				roomVC.add(r);
				roomID = roomName;
				sendMsg("CreateRoom/" + roomName); // 생성 요청자에게 전송
				broadCast("NewRoom/" + roomName); // 모든 클라이언트들에게
				broadCast("RoomJlistUpdate/Update"); // zzz2

			}
			// sendMsg("RoomJlistUpdate/Update"); //12-5
		}

		// 클라이언트로부터 참여 버튼이 눌리면 발생하는 이벤트이다.
		private void handleJoinRoomProtocol(StringTokenizer st, String roomName) { // 12-01-1
			for (RoomInfo r : roomVC) {
				if (r.roomName.equals(roomName)) {
					r.broadcastRoomMsg("JoinRoomMsg/가입/***" + clientID + "님이 입장하셨습니다.********");
					r.RoomClientVC.add(this);
					roomID = roomName; // RoomName -->RoomID 변경 필요
					sendMsg("JoinRoom/" + roomName);
					break;
				}
			}
		}

		// 클라이언트로부터 전송 버튼이 눌리면 발생하는 이벤트이다.
		private void handleSendMsgProtocol(StringTokenizer st, String roomName) {
			String sendMsg = st.nextToken();
			for (RoomInfo r : roomVC) {
				if (r.roomName.equals(roomName)) {
					r.broadcastRoomMsg("SendMsg/" + clientID + "/" + sendMsg);
				}
			}
		}

		/**
		 *  클라이언트로부터 채팅종료 버튼이 눌리면 발생하는 이벤트이다.
		 *  클라이언트에 대한 스트림을 닫고, clientVC에 들어간 정보를 없앤다.
		 *  소켓도 닫고 모든 사용자에게 채팅을 종료했다는 메세지를 전송한다. 
		 */
		private void handleClientExitProtocol() {
			try {
				closeStreams();
				clientVC.remove(this);
				if (clientSocket != null && !clientSocket.isClosed()) {
					clientSocket.close();
					textArea.append("Client socket closed.\n");
				}

				broadCast("ClientExit/" + clientID);
				broadCast("ClientJlistUpdate/Update");

			} catch (IOException e) {
				logError("사용자 로그아웃 중 오류 발생", e);
			}
		}

		/**
		 * 클라이언트로부터 탈퇴 버튼이 눌리면 발생하는 이벤트이다.
		 * 해당 룸에 대한 RoomClientVC를 없애고, 만약 비어있다면 roomVC에서 완전히 없애는 작업을 수행한다.
		 */
		private void handleExitRoomProtocol(String roomName) {
			roomID = roomName;
			log(clientID + " 사용자가 " + roomName + " 방에서 나감");

			for (RoomInfo r : roomVC) {
				if (r.roomName.equals(roomName)) {
					r.broadcastRoomMsg("ExitRoomMsg/탈퇴/***" + clientID + "님이 채팅방에서 나갔습니다.********");
					r.RoomClientVC.remove(this);
					if (r.RoomClientVC.isEmpty()) {
						roomVC.remove(r);
						broadCast("RoomOut/" + roomName);
						broadCast("RoomJlistUpdate/Update"); // zzz4
					}
					break;
				}
			}
		}

		// 모든 사용자에게 메세지를 전송하는 함수이다.
		private void broadCast(String str) {
			for (ClientInfo c : clientVC) {
				c.sendMsg(str);
			}
		}

		// 로그 출력
		private void log(String message) {
			System.out.println(clientID + ": " + message);
		}

		// 오류 로그 출력
		private void logError(String message, Exception e) {
			System.err.println(clientID + ": " + message);
			e.printStackTrace();
		}

		/** 
		 * 서버중지 버튼을 눌렀을 때 발생하는 함수이다.
		 * 소켓을 전부 닫아준다.
		 */
		public void closeStreams() throws IOException {
			if (dos != null) {
				dos.close();
			}
			if (dis != null) {
				dis.close();
			}
			if (cs != null) {
				cs.close();
				textArea.append(clientID + " Client Socket 종료.\n");
			}
		}
	}

	/**
	 * 방을 관리하는 클래스이다.
	 * 이름과 방에 대한 정보를 가지고있다.
	 * 모든 사용자에게 메세지를 전달하는 함수를 가지고있다.
	 */
	class RoomInfo {
		private String roomName = "";
		private Vector<ClientInfo> RoomClientVC = new Vector<ClientInfo>();

		public RoomInfo(String name, ClientInfo c) {
			this.roomName = name;
			this.RoomClientVC.add(c);
		}

		public void broadcastRoomMsg(String message) {
			for (ClientInfo c : RoomClientVC) {
				c.sendMsg(message);
			}
		}
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		new server();
	}

}
