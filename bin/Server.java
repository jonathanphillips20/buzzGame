import java.io.*;
import java.net.*;

public class Server {
	private DatagramSocket socket;
	private int[] playerPort;
	private InetAddress[] playerAddr;
	private String[] playerName;
	private int numPlayers;

	public static void main(String args[]) {
		if (args.length < 2) {
			System.out.println("Invalid use please use: java Server <port> <numPlayers>");
			return;
		}
		try {
			new Server(Integer.parseInt(args[0]),Integer.parseInt(args[1]));
		} catch (NumberFormatException e) {
			System.out.println("In valid arguments. Cannot resolve \""+args[0]+"\" or \""+args[1]+"\" as a number");
			return;
		}
	}

	public Server(int myPort, int numPlayers) {
		this.numPlayers = numPlayers;
		playerPort = new int[numPlayers];
		playerAddr = new InetAddress[numPlayers];
		playerName = new String[numPlayers];

		try {
			socket = new DatagramSocket(myPort);
			System.out.println("Waiting for players to connect.");
			for (int i = 0; i < numPlayers; i++) {
				byte[] b = new byte[80];
				DatagramPacket p = new DatagramPacket(b, b.length);
				socket.receive(p);
				playerPort[i] = p.getPort();
				playerAddr[i] = p.getAddress();
				playerName[i] = new String(p.getData()).trim();
				socket.send(new DatagramPacket(new byte[] { (byte) -1 }, 1, playerAddr[i], playerPort[i]));
				System.out.println(playerName[i] + "-" + playerAddr[i].getHostAddress() + ":" + playerPort[i] + " connected");
			}
			while (true) {
				System.out.print("Press ENTER to begin.");
				BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
				br.read();
				System.out.print("\tRunning: ");
				startGame();
			}
		} catch (SocketException e) {
			System.out.println("Port \""+myPort+"\" already in use");
			return;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void startGame() throws IOException {
		for (int i = 0; i < numPlayers; i++) {
			DatagramPacket p = new DatagramPacket(new byte[] { (byte) 1 }, 1, playerAddr[i], playerPort[i]);
			socket.send(p);
		}
		boolean found = false;
		int x = -1;
		while (!found) {
			byte[] b = new byte[1];
			DatagramPacket r = new DatagramPacket(b, 1);
			try {
				socket.setSoTimeout(10 * 1000);
				socket.receive(r);
				socket.setSoTimeout(0);
			} catch (SocketTimeoutException e) {
				System.out.println("No one pressed in 10 seconds...");
				break;
			}
			int port = r.getPort();
			InetAddress a = r.getAddress();

			for (int i = 0; i < numPlayers; i++) {
				if (port == playerPort[i] && a.equals(playerAddr[i])) {
					x = i;
					found = true;
				}
			}
		}
		if (x != -1) {
			System.out.println(playerName[x] + " won");
		}

		for (int i = 0; i < numPlayers; i++) {
			if (i != x) {
				socket.send(new DatagramPacket(new byte[] { (byte) 3 }, 1, playerAddr[i], playerPort[i]));
			} else {
				socket.send(new DatagramPacket(new byte[] { (byte) 2 }, 1, playerAddr[i], playerPort[i]));
			}
		}

		socket.setSoTimeout(250);
		for (int i = 0; i < numPlayers; i++) { // ping all clients
			byte[] b = new byte[80];
			DatagramPacket ping = new DatagramPacket(b, b.length);

			socket.send(new DatagramPacket(new byte[] { (byte) 0 }, 1, playerAddr[i], playerPort[i]));

			try {
				socket.receive(ping);
			} catch (SocketTimeoutException e) {
				System.out.println(playerName[i] + "-" + playerAddr[i].getHostAddress() + ":" + playerPort[i] + " disconnected, waiting for new player");
				socket.setSoTimeout(0);
				socket.receive(ping);
				socket.setSoTimeout(250);
				playerPort[i] = ping.getPort();
				playerAddr[i] = ping.getAddress();
				playerName[i] = new String(ping.getData()).trim();
				System.out.println(playerName[i] + "-" + playerAddr[i].getHostAddress() + ":" + playerPort[i] + " connected");
				socket.send(new DatagramPacket(new byte[] { (byte) -1 }, 1, playerAddr[i], playerPort[i]));
			}
		}
		socket.setSoTimeout(0);
	}
}
