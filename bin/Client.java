import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

import javax.swing.*;

public class Client {
	private DatagramPacket clicked;
	private DatagramPacket ping;
	private DatagramSocket socket;
	private Client thisClient;
	private ClientGUI thisGUI;
	private String name;
	private boolean jumped;

	public static void main(String args[]) {
		if (args.length < 2) {
			System.out.println("Please enter the servers location in the form \"address:port\" ie:");
			System.out.println("\tlocalhost:5555 or\n\t192.168.1.1:2355 or\n\twww.server.com:5555 etc.");
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			boolean quit = false;
			while (!quit) {
				try {
					String s = br.readLine();
					String[] a = s.split(":");
					System.out.print("Please type your name: ");
					new Client(InetAddress.getByName(a[0]), Integer.parseInt(a[1]), br.readLine());
					quit = true;
				} catch (NullPointerException e) {
					System.out.println("Invalid input, try again");
				} catch (NumberFormatException e) {
					System.out.println("Invalid input, try again");
				} catch (UnknownHostException e) {
					System.out.println("Unknown address, try again");
				} catch (IOException e) {
					System.out.println("Unknown Exception");
					e.printStackTrace();
				}
			}
		} else if (args.length == 2) {
			String[] a = args[0].split(":");
			try {
				new Client(InetAddress.getByName(a[0]), Integer.parseInt(a[1]), args[1]);
			} catch (NumberFormatException e) {
				System.out.println("Invalid port format (Only numbers)");
			} catch (UnknownHostException e) {
				System.out.println("Unknown address must be of the form 'x.x.x.x' or 'www.xyz.com' or 'localhost'");
			} catch (IOException e) {
				System.out.println("Unknown Exception");
				e.printStackTrace();
			}
		} else {
			System.out.println("Invalid Input use java Client <server:port> <name> or no arguments");
		}
	}

	public Client(InetAddress address, int port, String name) throws IOException {
		this.name = name;
		this.thisClient = this;
		this.jumped = false;

		byte[] buf = name.getBytes();
		this.clicked = new DatagramPacket(new byte[] { (byte) 1 }, 1, address, port);
		this.ping = new DatagramPacket(buf, buf.length, address, port);

		this.socket = new DatagramSocket();
		this.socket.setSoTimeout(1);
		this.thisGUI = new ClientGUI(this);

		socket.send(ping); // ping server

		byte[] b = new byte[1];
		DatagramPacket p = new DatagramPacket(b, 1);

		try {
			socket.setSoTimeout(250);
			socket.receive(p);
			socket.setSoTimeout(0);
		} catch (SocketTimeoutException e) {
			thisGUI.disableListeners();
			thisGUI.setText("Server Offline or Too many players connected");
			return;
		}

		while (!socket.isClosed()) {
			try {
				socket.receive(p);
				byte data = p.getData()[0];
				if (data == (byte) 0) { // pinged
					socket.send(ping);
				} else if (data == (byte) 1) { // enable buttons
					if (!jumped) {
						thisGUI.setText("¡PRESS!");
						thisGUI.getButton().setEnabled(true);
					}
					jumped = false;
				} else if (data == (byte) 2) { // disable button, winner
					thisGUI.setText("Winner");
					thisGUI.getButton().setEnabled(false);
				} else if (data == (byte) 3) { // disable button, loser
					thisGUI.setText("Lost");
					thisGUI.getButton().setEnabled(false);
				}
			} catch (SocketTimeoutException e) {

			} catch (SocketException e) {
				// do nothing
			} finally {
				b[0] = (byte) 0;
			}
		}
	}

	public void clicked() {
		try {
			socket.send(clicked);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void quit() {
		this.socket.close();
		this.thisGUI.dispose();
	}

	public void jumped() {
		thisGUI.setText("Pressed too soon");
		thisGUI.getButton().setEnabled(false);
		jumped = true;
	}

	// ////////////////////////
	private class ClientGUI extends JFrame {
		private static final long serialVersionUID = 1L;
		private JTextField f;
		private JButton button;
		private JButton exit;
		private MyAction listener;

		private ClientGUI(Client c) {
			super(name);
			listener = new MyAction(this);
			UIManager.put("Panel.background", Color.BLACK);
			UIManager.put("Panel.foreground", Color.BLACK);

			UIManager.put("TextField.font", new Font("sansserif", Font.PLAIN, 30));
			UIManager.put("TextField.background", Color.BLACK);
			UIManager.put("TextField.foreground", Color.WHITE);
			UIManager.put("TextField.border", BorderFactory.createLineBorder(Color.GRAY));

			UIManager.put("Button.font", new Font("sansserif", Font.PLAIN, 30));
			UIManager.put("Button.background", Color.BLACK);
			UIManager.put("Button.foreground", Color.WHITE);
			UIManager.put("Button.focus", Color.BLACK);
			UIManager.put("Button.border", BorderFactory.createLineBorder(Color.GRAY));

			JPanel j = new JPanel(new GridLayout(3, 1));
			f = new JTextField();
			f.setFocusable(false);
			f.setPreferredSize(new Dimension(250, 50));
			f.addKeyListener(listener);

			button = new JButton("CLICK");
			button.setActionCommand("CLICK");
			button.addActionListener(listener);
			button.addKeyListener(listener);
			button.setEnabled(false);

			exit = new JButton("EXIT");
			exit.setActionCommand("EXIT");
			exit.addActionListener(listener);
			exit.addKeyListener(listener);

			j.add(f);
			j.add(button);
			j.add(exit);

			this.getRootPane().addKeyListener(listener);

			this.add(j);
			this.pack();
			this.setVisible(true);
		}

		public void setText(String s) {
			f.setText(s);
		}

		public JButton getButton() {
			return button;
		}

		public void disableListeners() {
			this.getRootPane().removeKeyListener(listener);
			button.removeKeyListener(listener);
			exit.removeKeyListener(listener);
		}
	}

	private class MyAction implements ActionListener, KeyListener {
		private ClientGUI gui;

		public MyAction(ClientGUI gui) {
			this.gui = gui;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			String ac = e.getActionCommand();
			if (ac.equals("CLICK")) {
				thisGUI.getButton().setEnabled(false);
				thisClient.clicked();
			} else if (ac.equals("JUMPED")) {
				thisClient.jumped();
			} else if (ac.equals("EXIT")) {
				thisClient.quit();
			}
		}

		@Override
		public void keyPressed(KeyEvent e) {
			int c = e.getKeyCode();
			boolean bool = (c == KeyEvent.VK_LEFT || c == KeyEvent.VK_RIGHT || c == KeyEvent.VK_UP || c == KeyEvent.VK_DOWN || c == KeyEvent.VK_0 || c == KeyEvent.VK_NUMPAD0);
			if (bool && gui.getButton().isEnabled()) {
				this.actionPerformed(new ActionEvent(this, 0, "CLICK"));
			} else if (bool && !gui.getButton().isEnabled()) {
				this.actionPerformed(new ActionEvent(this, 0, "JUMPED"));
			} else if (c == KeyEvent.VK_ENTER && e.getSource() instanceof JButton) {
				JButton b = (JButton) e.getSource();
				this.actionPerformed(new ActionEvent(this, 0, b.getActionCommand()));
			}
		}

		@Override
		public void keyReleased(KeyEvent e) {
		}

		@Override
		public void keyTyped(KeyEvent e) {
		}
	}
}
