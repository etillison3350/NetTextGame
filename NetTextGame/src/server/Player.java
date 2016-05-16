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
import java.util.TreeMap;
import java.util.stream.Collectors;

public class Player extends Entity {

	public static final List<String> registered = Arrays.asList("alias", "cut", "items", "list", "walk", "creep", "sneak", "quietly", "north", "south", "east", "west");

	public boolean hunter, escaped = false;
	public int moves = -65536, savedMoves;

	public final Map<String, Double> items = new TreeMap<>();

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

	public void printf(String format, Object... args) {
		print(String.format(format, args));
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

		if (commands[0].equals("help")) {
			if (commands.length == 1) {
				print("Available commands: alias, creep, cut, help, items, rest, walk.\nUse help <command> for more details.");
			} else {
				switch (commands[1]) {
					case "walk":
						print("Usage: walk <direction>\nMove in the given direction.\nMove cost: 4");
						break;
					case "creep":
						print("Usage: creep <direction>\nMove quietly in the given direction.\nMove cost: 7");
						break;
					case "alias":
						print("Usage: alias <term> <replacement>\nDefine the given term as the replacement.\nUsage: alias list\nLists all currently defined aliases.");
						break;
					case "cut":
						print("Usage: cut <direction>\nAttempt to cut down the tree in the given direction. Large trees will be damaged.\nMove cost: 3, or 1 if there is not a tree in the given direction.");
						break;
					case "items":
						print("Usage: items\nLists all items that you have.");
						break;
					case "rest":
						print("Usage: rest [moves]\nSave the specified number of moves (or 1, if no number is specified) until the next turn. Moves have a greater chance of being saved if you have fewer mvoes left.");
						break;
					case "help":
						print("Usage: help\nGives a list of commands.\nUsage: help <command>\nGives details on the given command.");
						break;
					default:
						print("That is not a valid command.");
						break;
				}
			}
		} else if (commands[0].equals("alias")) {
			if (commands.length < 2) {
				print("Please enter a term to define.");
				return;
			}

			if (commands[1].equals("list")) {
				if (aliases.isEmpty()) {
					print("You haven't defined any aliases.");
				} else {
					aliases.forEach((alias, replacement) -> print(alias + " -> " + replacement));
				}
				return;
			}

			if (registered.contains(commands[1])) {
				print("\"" + commands[1] + "\" cannot be redefined.");
				return;
			}

			if (commands.length < 3) {
				print("Please enter a term or terms to replace \"" + commands[1] + "\" with");
				return;
			}

			aliases.put(commands[1], Arrays.stream(commands).skip(2).collect(Collectors.joining(" ")));
			print(commands[1] + " has been redefined as " + aliases.get(commands[1]));
		} else if (commands[0].equals("creep") || commands[0].equals("walk")) {
			if (commands.length < 2) {
				print("Please enter a direction to move in.");
				return;
			}

			boolean quiet = commands[0].equals("creep");
			String dir = commands[1];

			if (commands[1].equals("quietly")) {
				if (commands.length < 3) {
					print("Please enter a direction to move in.");
					return;
				}

				dir = commands[2];
				quiet = true;
			}

			if (moves < (quiet ? 7 : 4)) {
				print("You don't have enough moves left to do that.");
				return;
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
					print("You can't walk that way.");
					return;
			}
			moves -= quiet ? 7 : 4;

			if (nx < 0 || nx >= 30 || ny < 0 || ny >= 30) {
				if (this.hunter || !this.items.containsKey("the crystal"))
					print("You have reached the edge of the forest.");
				else
					escaped = true;
				
				return;
			}

			boolean move = true;
			String stand = null;
			for (Entity e : Server.getEntitiesAt(nx, ny)) {
				if (!e.isPassable()) {
					print("You walked into a" + (e.getName().matches("^[aeiou].+?") ? "n " : " ") + e.getName() + ".");
					move = false;
					break;
				}

				if (e.getName().startsWith("item")) {
					print("You found " + e.getName().substring(5));
					items.put(e.getName().substring(5), e.getState());
				} else if (e instanceof Silhouette && ((Silhouette) e).summoner != this) {
					print("You see a humanoid figure. It walks into you, and dissolves into shadows.");
				} else if (stand == null) {
					stand = e.getName();
				}
			}

			if (move) {
				this.setX(nx);
				this.setY(ny);
				print("You walked " + dir + ".");
				if (stand != null) print("You are standing on a " + stand + ".");
				print(Server.world[this.getY()][this.getX()].getMessage());
			}

		} else if (commands[0].equals("cut")) {
			if (moves < 3) {
				print("You don't have enough moves left to do that.");
				return;
			}

			if (commands.length < 2) {
				print("Please enter a direction to cut in.");
				return;
			}

			List<Entity> ents;
			switch (commands[1]) {
				case "north":
					ents = Server.getEntitiesAt(this.getX(), this.getY() - 1);
					break;
				case "east":
					ents = Server.getEntitiesAt(this.getX() + 1, this.getY());
					break;
				case "south":
					ents = Server.getEntitiesAt(this.getX(), this.getY() + 1);
					break;
				case "west":
					ents = Server.getEntitiesAt(this.getX() - 1, this.getY());
					break;
				default:
					print("You can't cut in that direction.");
					return;
			}

			if (ents.stream().allMatch(e -> e.isPassable())) {
				if (ents.stream().anyMatch(e -> e instanceof Silhouette)) {
					ents.stream().filter(e -> e instanceof Silhouette).forEach(Server.objects::remove);
					print("You conjure a magical axe, and swing with all your might. Too late, you realize that what you though was a tree is actually a humanoid figure. As your axe passes thourhg it, it dissolves into shadow.");
					moves -= Server.rand.nextInt(2) + 1;
				} else {
					if (Server.rand.nextInt(9) == 0) {
						print("You conjure a magical axe, and swing with all your might. Too late, you realize that there's nothing there, and narrowly miss your leg.");
						moves -= 2;
					} else {
						print("There's nothing to cut in the " + commands[1] + ".");
						moves--;
					}
				}
			} else {
				if (ents.stream().anyMatch(e -> e.getName().equals("tree"))) {
					for (Entity e : ents) {
						if (e.getName().equals("tree")) {
							if (e.addState(0, -Math.abs(Server.rand.nextGaussian() + 1) * 1.66 - 1) < 0) {
								int dir = Server.rand.nextInt(4);
								int cx = e.getState() > 4 ? (dir - 1) % 2 : 0;
								int cy = e.getState() > 4 ? (dir - 2) % 2 : 0;
								boolean hit = this.getX() == e.getX() + cx && this.getY() == e.getY() + cy;
								if (hit) {
									moves--;
									if (moves < 0) savedMoves--;
								}
								print("You conjure a magical axe, and swing with all your might. The tree you hit falls over" + (hit ? ", toward you, and you jump out of the way. The tree narrowly misses you." : "."));
								Server.objects.remove(e);
								if (e.getState(1) > 4) {
									Server.objects.add(new Entity("stump", e.getX(), e.getY(), true, 6));
									for (int i = 1; i < e.getState(1) * 0.75; i++) {
										Server.objects.add(new Entity("fallen log", e.getX() * cx * i, e.getY() + cy * i, true, 6, e.getState()));
									}
								}
							} else {
								print("You conjure a magical axe, and swing with all your might. You made a large dent in the tree, but it does not fall down.");
							}
						} else if (e.getName().startsWith("item")) {
							print("You found " + e.getName().substring(5));
							items.put(e.getName().substring(5), e.getState());
						}
					}
					moves -= 3;
				} else {
					for (Entity e : ents) {
						if (!e.isPassable()) {
							print("You conjure a magical axe, and swing with all your might. Too late, you realize that what you though was a tree is actually a " + e.getName() + ". Your axe glances off, and you narrowly miss your leg.");
							moves -= 3;
							break;
						}
					}
				}
			}
		} else if (commands[0].equals("items")) {
			if (items.isEmpty()) {
				print("You don't have any items.");
			} else {
				items.forEach((item, durability) -> printf("%s (%.1f durability)", item, durability));
			}
		} else if (commands[0].equals("rest")) {
			int n = 1;
			try {
				n = Integer.parseInt(commands[1]);
			} catch (Exception e) {}

			if (n < 1) {
				print("You can't save less than one move.");
				return;
			} else if (n > moves) {
				print("You don't have that many moves to save.");
				return;
			}

			int sm = 0;
			for (int i = 0; i < n; i++) {
				moves--;
				if (Server.rand.nextInt(moves / 2 + 1) == 0) {
					savedMoves++;
					sm++;
				}
			}
			print("You saved " + sm + " move" + (sm == 1 ? "." : "s."));
		} else {
			print("You cannot perform that action.");
		}
	}

	public void resetMoves() {
		this.moves = savedMoves + Math.max(3, 15 + (int) (Server.rand.nextGaussian() * 4));
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
