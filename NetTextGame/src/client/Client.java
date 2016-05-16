package client;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayDeque;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
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
	private final JScrollPane scroll;

	private PrintWriter out;
	private BufferedReader in;

	private String address;
	private int port;

	private boolean confirmClose = true, runAppend = true;

	private ArrayDeque<String> appendQueue = new ArrayDeque<>();

	public Client() {
		super("Network Text Game");

		this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		this.setSize(640, 480);
		this.setMinimumSize(new Dimension(384, 192));

		this.setBackground(Color.BLACK);

		this.textArea = new JTextArea();
		this.textArea.setEditable(false);
		this.textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
		this.textArea.setLineWrap(true);
		this.textArea.setWrapStyleWord(true);
		this.textArea.setBackground(Color.BLACK);
		this.textArea.setForeground(Color.WHITE);
		this.textArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 1, 5));
		this.scroll = new JScrollPane(this.textArea);
		this.scroll.getVerticalScrollBar().setUI(new GrayScrollBarUI());
		this.scroll.setBackground(Color.LIGHT_GRAY);
		this.scroll.setBorder(BorderFactory.createEmptyBorder(0, 0, 2, 0));
		this.add(this.scroll);

		this.textField = new JTextField();
		this.textField.setBackground(Color.BLACK);
		this.textField.setForeground(Color.WHITE);
		this.textField.setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 5));
		this.textField.setCaretColor(Color.LIGHT_GRAY);
		this.textField.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (textField.getText().isEmpty()) return;

				scroll.getVerticalScrollBar().setValue(scroll.getVerticalScrollBar().getMaximum());
				scroll.repaint();
				appendQueue.addLast("> " + textField.getText() + "\n");
				if (address == null) {
					address = textField.getText();
					textField.setText("");
				} else if (port < 1) {
					try {
						port = Integer.parseInt(textField.getText());
						if (port < 0 || port > 65535) {
							port = 0;
							appendQueue.addLast("Please enter a valid port (between 1 and 65535 inclusive).\n");
						} else {
							textField.setText("");
						}
					} catch (NumberFormatException exception) {}
				} else {
					if (out != null) {
						out.println(textField.getText());
						out.flush();
					}
					textField.setText("");
				}
			}
		});
		this.textField.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
		this.add(this.textField, BorderLayout.SOUTH);

		this.addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosing(WindowEvent e) {
				if (confirmClose || JOptionPane.showConfirmDialog(Client.this, "Are you sure you want to exit?", "Exit", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) {
					Client.this.dispose();
					System.exit(0);
				}
			}

		});

		new Thread(new Runnable() {

			@Override
			public void run() {
				while (runAppend) {
					try {
						if (appendQueue.isEmpty()) {
							Thread.sleep(100);
						} else {
							String text = appendQueue.pop();
							if (text.startsWith(">")) {
								textArea.append(text);
							} else {
								for (char c : text.toCharArray()) {
									textArea.append(Character.toString(c));
									Thread.sleep(15);
								}
							}
							scroll.getVerticalScrollBar().setValue(scroll.getVerticalScrollBar().getMaximum());
							scroll.repaint();
						}
					} catch (Exception e) {}
				}
			}
		}).start();

		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					Socket socket;
					while (true) {
						try {
							textField.setEnabled(true);
							appendQueue.addLast("Enter IP address:\n");
							while (address == null) {
								Thread.sleep(10);
							}

							appendQueue.addLast("Enter port:\n");
							while (port < 1) {
								Thread.sleep(10);
							}
							textField.setEnabled(false);
							socket = new Socket(address, port);
							break;
						} catch (Exception e) {
							appendQueue.addLast("Failed to connect to " + address + ":" + port + ".\n");
							address = null;
							port = 0;
						}
					}

					out = new PrintWriter(socket.getOutputStream());
					in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

					appendQueue.addLast("Connected to " + socket.getInetAddress().getHostAddress() + ":" + port + "\n");

					textField.setEnabled(true);
					textField.requestFocus();

					confirmClose = false;

					while (true) {
						String s = in.readLine();
						if (s.charAt(0) == 4) break;

						if (s.matches(".*?You are (?:hunting|being hunted).*?")) {
							Client.this.setTitle("Network Text Game - " + (s.contains("being") ? "Hunted" : "Hunter"));
						}

						appendQueue.addLast(s + "\n");
					}

					confirmClose = true;
					runAppend = false;

					in.close();
					out.close();
					socket.close();
				} catch (SocketException e) {
					appendQueue.addLast("Your connection to the server has been lost.");
					confirmClose = true;
					runAppend = false;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
	}

	@Override
	public void setVisible(boolean b) {
		super.setVisible(b);
		this.textField.requestFocus();
	}

}
