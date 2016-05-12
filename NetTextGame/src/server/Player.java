package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Player extends Entity {

	public static final List<String> registered = Arrays.asList("alias", "list", "walk", "creep", "sneak", "quietly", "north", "south", "east", "west");

	public boolean hunter;
	public int moves, savedMoves;

	public final Map<String, Double> items = new HashMap<>();

	private final Map<String, String> aliases = new HashMap<>();

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

	public void execute(String command) {
		String[] commands = command.split("\\s+");

		String cmd = "";
		for (int c = 0; c < commands.length; c++) {
			String repl = aliases.get(commands[c]);
			cmd += (repl == null ? commands[c] : repl) + " ";
		}
		commands = cmd.trim().split("\\s+");

		switch (commands[0]) {
			case "help":
				if (commands.length == 1) {
					print("Available commands: alias, creep, help, rest, walk.\nUse help <command> for more details.");
				} else {
					switch (commands[1]) {
						case "walk":
							print("Usage: walk <direction>\nMove in the given direction.\nMove cost: 4");
							break;
						case "creep":
							print("Usage: creep <direction>\nMove quietly in the given direction.\nMove cost: 7");
							break;
						case "alias":
							print("Usage: alias <term> <replacement>\nDefines the given term as the replacement.\nUsage: alias list\nLists all currently defined aliases.");
							break;
						case "rest":
							print("Usage: rest [moves]\nSaves the specified number of moves (or 1, if no number is specified) until the next turn. A move has a 80% chance of being saved.");
						case "help":
							print("Usage: help\nGives a list of commands.\nUsage: help <command>\nGives details on the given command.");
							break;
					}
				}
				break;
			case "alias":
				if (commands.length < 2) {
					print("Please enter a term to define.");
					break;
				}
				
				if (commands[1].equals("list")) {
					aliases.forEach((alias, replacement) -> print(alias + " -> " + replacement));
					break;
				}

				if (registered.contains(commands[1])) {
					print("\"" + commands[1] + "\" cannot be redefined.");
					break;
				}

				if (commands.length < 3) {
					print("Please enter a term or terms to replace \"" + commands[1] + "\" with");
					break;
				}

				aliases.put(commands[1], Arrays.stream(commands).skip(2).collect(Collectors.joining(" ")));
				print(commands[1] + " has been redefined as " + aliases.get(commands[1]));
				break;
			case "creep":
			case "walk":
				if (commands.length < 2) {
					print("Please enter a direction to move in.");
					break;
				}

				boolean quiet = commands[0].equals("creep");
				String dir = commands[1];

				if (commands[1].equals("quietly")) {
					dir = commands[1];
					quiet = true;
					break;
				}
				
				if (moves < (quiet ? 7 : 4)) {
					print("You don't have enough moves left to do that.");
					break;
				}

				int nx = this.getX(), ny = this.getY();
				switch (dir) {
					case "north":
						ny--;
						break;
					case "east":
						nx++;
						break;
					case "south":
						ny++;
						break;
					case "west":
						nx--;
						break;
					default:
						nx = Integer.MIN_VALUE;
						print("You can't walk that way.");
						break;
				}
				if (nx > -65536) {
					moves -= 4;
					boolean move = true;
					for (Entity e : Server.getEntitiesAt(nx, ny)) {
						if (!e.isPassable()) {
							System.out.println("You walked into a" + (e.getName().matches("^[aeiou].+?") ? "n " : " ") + e.getName() + ".");
							move = false;
							break;
						}

						if (e.getName().startsWith("item")) {
							print("You found " + e.getName().substring(5));
						} else if (e instanceof Silhouette && ((Silhouette) e).summoner != this) {
							print("You see a humanoid figure. It walks into you, and dissolves into shadows.");
						}
					}

					if (move) {
						this.setX(nx);
						this.setY(ny);
						print("You walked " + dir);
					}
				}
				break;
			case "rest":
				int n = 1;
				try {
					n = Integer.parseInt(commands[1]);
				} catch (Exception e) {}

				if (n < 1) {
					print("You can't save less than one move.");
					break;
				} else if (n > moves) {
					print("You don't have that many moves to save.");
				}
				
				int sm = 0;
				for (int i = 0; i < n; i++) {
					moves--;
					if (Server.rand.nextInt(5) != 0) {
						savedMoves++;
						sm++;
					}
				}
				print("You saved " + sm + " moves.");
				break;
		}
	}

	public void resetMoves() {
		this.moves = savedMoves + Math.max(1, 12 + (int) (Server.rand.nextGaussian() * 4));
		this.savedMoves = 0;
	}

	public static class Noise {

		public final String name;
		public final double value;
		public final int x, y;

		public Noise(String name, int x, int y, double value) {
			this.name = name;
			this.x = x;
			this.y = y;
			this.value = value;
		}

	}

}
