package server;

import java.awt.Point;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

public class Player {

	/**
	 * The name of this player
	 */
	public final String name;

	/**
	 * Whether or not this player is the hunter
	 */
	public boolean hunter;

	public int x, y;
	public Socket socket;
	public PrintWriter out;
	public BufferedReader in;

	/**
	 * The number of moves the player has remaining
	 */
	public int moves;

	public final List<Noise> noises = new ArrayList<>();

	/**
	 * Whether or not this player has won the game
	 */
	public boolean won = false;

	/**
	 * Maps aliases. Keys are replaced with values
	 */
	public Map<String, String> aliases = new HashMap<>();

	public static final Random rand = new Random();

	public Player(String name, int x, int y, Socket socket) throws IOException {
		this.name = name;

		this.x = x;
		this.y = y;
		this.socket = socket;
		out = new PrintWriter(socket.getOutputStream());
		in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
	}

	/**
	 * <ul>
	 * <li><b><i>parse</i></b><br>
	 * <br>
	 * {@code public String parse(String str)}<br>
	 * <br>
	 * Executes the given command.<br>
	 * @param str The command to execute
	 * @return The text to be outputted to the player. Failure messages begin with an <code>*</code>.
	 *         </ul>
	 */
	public String parse(String str) {
		String[] command = str.trim().toLowerCase().split("\\s+");

		if (command.length < 1) return "*Please enter a command.";

		if (aliases.keySet().contains(command[0])) {
			List<String> cmd = new ArrayList<>(Arrays.asList(command));
			cmd.remove(0);
			cmd.addAll(0, Arrays.asList(aliases.get(command[0]).split("\\s+")));
			command = cmd.toArray(new String[cmd.size()]);
		}

		if (command[0].equals("alias")) {
			if (command.length < 2) return "*Please enter a word to define";
			if (command[1].equals("alias") || command[1].equals("walk") || command[1].equals("look") || command[1].equals("creep") || command[1].equals("sneak") || command[1].equals("cut") || command[1].equals("skip") || command[1].equals("stand")) return "*That word cannot be redefined.";
			if (command.length < 3) return "*Please enter text to redefine \"" + command[1] + "\" as.";
			String alias = Arrays.stream(command).skip(2).collect(Collectors.joining(" "));
			aliases.put(command[1], alias);
			return "\"" + alias + "\" has been redefined as \"" + command[1] + "\".";
		} else if (command[0].equals("walk") || command[0].equals("creep") || command[0].equals("sneak")) {
			if (command.length < 2) return "*Please enter a direction";

			boolean creep = command[0].equals("creep") || command[0].equals("sneak") || (command.length >= 3 && (command[1].equals("quietly") || command[2].equals("quietly")));
			if (creep && moves < 2) return "*You don't have enough moves left to do that.";

			String dir;
			int nx = x, ny = y;
			switch (command[1].equals("quietly") ? command[2] : command[1]) {
				case "n":
				case "north":
					dir = "north";
					ny--;
					break;
				case "e":
				case "east":
					dir = "east";
					nx++;
					break;
				case "s":
				case "south":
					dir = "south";
					ny++;
					break;
				case "w":
				case "west":
					dir = "west";
					nx--;
					break;
				default:
					return "*You cannot walk that way.";
			}
			moves -= creep ? 2 : 1;
			switch (move(nx, ny)) {
				case 0:
					noises.add(new Noise(rand.nextBoolean() ? "leaves crinkling" : "twigs snapping", (creep ? 0.375 : 1) * treeChance(nx, ny), new Point(nx, ny)));
					return "You walked " + dir + ".";
				case -1:
					if (hunter) {
						noises.add(new Noise(rand.nextBoolean() ? "leaves crinkling" : "twigs snapping", (creep ? 0.375 : 1) * treeChance(nx, ny), new Point(nx, ny)));
						return "You have reached the edge of the forest.";
					} else {
						won = true;
						return null;
					}
				case 1:
					noises.add(new Noise(rand.nextBoolean() ? "leaves crinkling" : "twigs snapping", (creep ? 0.375 : 1) * treeChance(nx, ny), new Point(nx, ny)));
					return "You walked into a tree.";
				case 2:
					noises.add(new Noise("a splash", 0.8, new Point(nx, ny)));
					return "You have fallen into a river.";
				case 3:
					noises.add(new Noise(rand.nextBoolean() ? "footsteps" : "twigs snapping", (creep ? 0.3 : 0.8) * treeChance(nx, ny), new Point(nx, ny)));
					return "You are standing on a log.";
				default:
					return "You walked into a mysterious object.";
			}
		} else if (command[0].equals("look")) {
			noises.add(new Noise(rand.nextBoolean() ? "leaves crinkling" : "twigs snapping", treeChance(this.x, this.y), new Point(this.x, this.y)));
			moves--;
			return "You look around, and see " + Server.nearby(x, y) + ".";
		} else if (command[0].equals("cut")) {
			if (moves < 2) return "*You don't have enough moves left to do that.";

			List<Point> trees = new ArrayList<>();
			if (y > 0 && Server.world[y - 1][x] == 1) trees.add(new Point(x, y - 1));
			if (y < 29 && Server.world[y + 1][x] == 1) trees.add(new Point(x, y + 1));
			if (x > 0 && Server.world[y][x - 1] == 1) trees.add(new Point(x - 1, y));
			if (x < 29 && Server.world[y][x + 1] == 1) trees.add(new Point(x + 1, y));

			if (trees.size() <= 0) {
				moves--;
				return "There are no trees to cut down.";
			}
			noises.add(new Noise("branches snapping and a loud thud", 1, new Point(this.x, this.y)));
			Point tree = trees.get(rand.nextInt(trees.size()));
			int h = rand.nextInt(5);
			int dir = rand.nextInt(4);
			for (int i = 0; i < h; i++) {
				Server.world[tree.y][tree.x] = 3;
				if (dir % 2 == 0)
					tree.x += dir - 1;
				else
					tree.y += dir - 2;
				if (Server.world[tree.y][tree.x] == 1) break;
			}
			moves -= 2;
			return "You cut down a tree.";
		} else if (command[0].equals("skip") || command[0].equals("stand")) {
			moves--;
			return "You stood still.";
		} else {
			return "*You cannot perform that action.";
		}
	}

	/**
	 * <ul>
	 * <li><b><i>treeChance</i></b><br>
	 * <br>
	 * {@code double treeChance()}<br>
	 * <br>
	 * description<br>
	 * @param x
	 * @param y
	 * @return
	 * 		</ul>
	 */
	private double treeChance(int x, int y) {
		int noise = 0;
		for (int yy = -2; yy <= 2; yy++) {
			for (int xx = -2; xx <= 2; xx++) {
				try {
					if (Server.world[y + yy][x + xx] == 1) noise += 4 - (Math.abs(yy) + Math.abs(xx));
				} catch (ArrayIndexOutOfBoundsException e) {}
			}
		}

		return Math.log(noise) / 3.6635616461296463;
	}

	/**
	 * <ul>
	 * <li><b><i>noise</i></b><br>
	 * <br>
	 * {@code public String noise(Player other)}<br>
	 * <br>
	 * Gets the noise that this player would hear<br>
	 * @param other The other player in the game.
	 * @return The noises that this player hears.
	 *         </ul>
	 */
	public String noise(Player other) {
		List<Noise> noises = new ArrayList<>(other.noises);
		Collections.shuffle(noises);

		double dist = Math.pow(1.035, -Math.pow(Math.abs(other.y - this.y) + Math.abs(other.x - this.x), 1.075));
		for (Noise noise : noises) {
			if (rand.nextDouble() < noise.chance * dist) {
				return "You hear " + noise.noise + " in the " + direction(other.x - this.x, other.y - this.y);
			}
		}

		if (Server.world[this.y][this.x] != 2) {
			int rNoise = 0;
			int[] dir = new int[4];
			for (int y = -2; y <= 2; y++) {
				for (int x = -2; x <= 2; x++) {
					try {
						if (Server.world[this.y + y][this.x + x] == 2) {
							if (y <= 0) dir[0]++;
							if (y >= 0) dir[2]++;
							if (x <= 0) dir[1]++;
							if (x >= 0) dir[3]++;
							rNoise++;
						}
					} catch (ArrayIndexOutOfBoundsException e) {}
				}
			}
			int max = Arrays.stream(dir).max().getAsInt();
			List<String> dirs = new ArrayList<>();
			String[] dn = {"north", "west", "south", "east"};
			for (int i = 0; i < 4; i++) {
				if (dir[i] == max) dirs.add(dn[i]);
			}
			if (rand.nextInt((int) (4 - (rNoise / 12.0))) == 0) return "You hear flowing water in the " + dirs.get(rand.nextInt(dirs.size())) + ".";
		}

		return null;
	}

	/**
	 * <ul>
	 * <li><b><i>direction</i></b><br>
	 * <br>
	 * {@code private static String direction(int dx, int dy)}<br>
	 * <br>
	 * Gets a direction represented by the given values. The direction is picked randomly from all directions that apply, weighted by <code>dx</code> and <code>dy</code>.<br>
	 * @param dx The difference in x values.
	 * @param dy The difference in y values.
	 * @return <code>"north"</code>, <code>"south"</code>, <code>"east"</code>, or <code>"west"</code>.
	 *         </ul>
	 */
	private static String direction(int dx, int dy) {
		double x = Math.abs(dx);
		if (Math.random() < x / (Math.abs(dy) + x)) {
			return (dx < 0 ? "east" : "west");
		} else {
			return (dy < 0 ? "north" : "south");
		}
	}

	/**
	 * <ul>
	 * <li><b><i>move</i></b><br>
	 * <br>
	 * {@code public int move(int x, int y)}<br>
	 * <br>
	 * Attempts to move to the specified location
	 * @param x
	 * @param y
	 * @return 0 if the move was successful, -1 if the target position if off of the map, or the map value of the square blocking this movement.
	 *         </ul>
	 */
	public int move(int x, int y) {
		if (x < 0 || x >= 30 || y < 0 || y >= 30) return -1;

		if (Server.traversable(Server.world[y][x])) {
			this.x = x;
			this.y = y;
		}
		return Server.world[y][x];
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
		out.println(text);
		out.flush();
		System.out.println(Server.date.format(new Date()) + " [SERVER -> " + this.name + "] " + text.replace("\n", "\n\t"));
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
		System.out.println(Server.date.format(new Date()) + " [" + this.name + "] " + ret);
		return ret;
	}

	/**
	 * <ul>
	 * <li><b><i>request</i></b><br>
	 * <br>
	 * {@code public void request()}<br>
	 * <br>
	 * Requests input from the client.<br>
	 * </ul>
	 */
	public void request() {
		out.println(">");
		System.out.println(Server.date.format(new Date()) + " [INFO] Requesting output from " + this.name);
		out.flush();
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

	public static class Noise {

		public final String noise;
		public final double chance;

		public Noise(String noise, double chance, Point loc) {
			this.noise = noise;
			this.chance = chance;
		}

		@Override
		public String toString() {
			return String.format("%s (%.2f%% chance)", noise, chance * 100);
		}
	}

}
