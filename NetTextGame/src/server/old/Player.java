package server.old;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Player extends Entity {

	public boolean hunter;
	public int moves;
	
	public final List<Item> items = new ArrayList<>();
	
	protected final Socket socket;
	protected final PrintWriter out;
	protected final BufferedReader in;

	public Player(Socket socket, int number, int x, int y) throws IOException {
		super("PLAYER" + number, x, y, true, 0);

		this.socket = socket;
		this.out = new PrintWriter(socket.getOutputStream());
		this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
	}

	/**
	 * <ul>
	 * <li><b><i>print</i></b><br>
	 * <br>
	 * {@code public void print(String text)}<br>
	 * <br>
	 * Outputs the given text to the client.<br>
	 * @param text The text to output
	 *        </ul>
	 */
	public void print(String text) {
		if (text.contains("\n")) {
			for (String s : text.split("\n")) {
				print(s);
			}
		} else {
			out.println(text);
			out.flush();
			System.out.println(Server.date.format(new Date()) + " [SERVER -> " + this.getName() + "] " + text);
		}
	}

	/**
	 * <ul>
	 * <li><b><i>read</i></b><br>
	 * <br>
	 * {@code public String read() throws IOException}<br>
	 * <br>
	 * Reads from the client<br>
	 * @return input from the client, as returned by {@link BufferedReader#readLine()}
	 * @throws IOException If there is an IOException while reading.
	 *         </ul>
	 */
	public String read() throws IOException {
		String ret = in.readLine();
		System.out.println(Server.date.format(new Date()) + " [" + this.getName() + "] " + ret);
		return ret;
	}

	/**
	 * <ul>
	 * <li><b><i>close</i></b><br>
	 * <br>
	 * {@code public void close() throws IOException}<br>
	 * <br>
	 * Closes the socket and all streams.<br>
	 * @throws IOException If an IOException occurs while closing the socket or streams.
	 *         </ul>
	 */
	public void close() throws IOException {
		out.println((char) 4);
		out.flush();
		out.close();
		in.close();
		socket.close();
	}

	public void addMoves() {
		this.moves += 8 + (int) (Server.rand.nextGaussian() * 4);
	}

}
