package client;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class Client extends JFrame {

	private static final long serialVersionUID = 8715904577824128661L;

	public static void main(String[] args) {
		Client client = new Client();
		client.setVisible(true);
	}

	private final JTextArea textArea;
	private final JTextField textField;

	private String address;
	private int port;

	public Client() {
		super("Network Text Game");

		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setSize(640, 480);

		this.textArea = new JTextArea();
		this.textArea.setEditable(false);
		this.textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
		this.textArea.setLineWrap(true);
		this.textArea.setWrapStyleWord(true);
		JScrollPane scroll = new JScrollPane(this.textArea);
		this.add(scroll);

		this.textField = new JTextField();
		this.textField.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					textArea.append("> " + textField.getText() + "\n");
					if (address == null) {
						address = textField.getText();
						textField.setText("");
					} else if (port < 1) {
						try {
							port = Integer.parseInt(textField.getText());
							if (port < 0 || port > 65535) {
								port = 0;
								append("Please enter a valid port (between 1 and 65535 inclusive).\n");
							} else {
								textField.setText("");
							}
						} catch (NumberFormatException exception) {}
					} else {
						out.println(textField.getText());
						out.flush();
						textField.setText("");
						textField.setEnabled(false);
					}
				} catch (InterruptedException exception) {}
			}
		});
		this.textField.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
		this.add(this.textField, BorderLayout.SOUTH);

		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					Socket socket;
					while (true) {
						try {
							append("Enter IP address:\n");
							while (address == null) {
								Thread.sleep(10);
							}

							append("Enter port:\n");
							while (port < 1) {
								Thread.sleep(10);
							}
							socket = new Socket(address, port);
							break;
						} catch (Exception e) {
							append("Failed to connect to " + address + ":" + port + ".\n");
							address = null;
							port = 0;
						}
					}

					out = new PrintWriter(socket.getOutputStream());
					in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

					append("Connected to " + socket.getInetAddress().getHostAddress() + ":" + port + "\n");

					while (true) {
						String s = in.readLine();
						if (s.charAt(0) == 4) break;
						if (s.charAt(0) == '>') {
							textField.setEnabled(true);
							textField.requestFocus();
							continue;
						}

						if (s.matches(".*?You are (?:hunting|being hunted).*?")) {
							Client.this.setTitle("Network Text Game - " + (s.contains("being") ? "Hunted" : "Hunter"));
						}

						append(s + "\n");
						scroll.getVerticalScrollBar().setValue(scroll.getVerticalScrollBar().getMaximum());
					}

					in.close();
					out.close();
					socket.close();
				} catch (SocketException e) {
					try {
						append("Your connection to the server has been lost.");
					} catch (InterruptedException e1) {}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
	}

	private synchronized void append(String text) throws InterruptedException {
		for (char c : text.toCharArray()) {
			textArea.append(Character.toString(c));
			Thread.sleep(15);
		}
	}

	private PrintWriter out;
	private BufferedReader in;

}
