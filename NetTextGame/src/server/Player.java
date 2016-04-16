package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

public class Player {

	public final String name;

	public boolean hunter;

	public int x, y;
	public Socket socket;
	public PrintWriter out;
	public BufferedReader in;

	public int moves;
	public int noise;

	public boolean won = false;

	public static final Random rand = new Random();

	public Player(String name, int x, int y, Socket socket) throws IOException {
		this.name = name;

		this.x = x;
		this.y = y;
		this.socket = socket;
		out = new PrintWriter(socket.getOutputStream());
		in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
	}

	public String parse(String str) {
		String[] command = str.trim().toLowerCase().split("\\s+");

		if (command.length < 1) return "*Please enter a command.";

		switch (command[0]) {
			case "walk":
				if (command.length < 2) return "*Please enter a direction";

				String dir;
				int nx = x, ny = y;
				switch (command[1]) {
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
				moves--;
				switch (move(nx, ny)) {
					case 0:
						noise += 5;
						return "You walked " + dir + ".";
					case -1:
						if (hunter) {
							noise += 4;
							return "You have reached the edge of the forest.";
						} else {
							won = true;
							return null;
						}
					case 1:
						noise += 5;
						return "You walked into a tree.";
					case 2:
						noise += 10;
						return "You have fallen into a river.";
					default:
						noise += 5;
						return "You walked into a mysterious object.";
				}
			case "look":
				noise += 1;
				moves--;
				return "You look around, and see " + Server.nearby(x, y) + ".";
			default:
				return "*You cannot perform that action.";
		}
	}

	public String noise(Player other) {
		if (other.noise <= 0) return null;
		
		int dy = this.y - other.y;
		int dx = this.x - other.x;
		if (Server.world[other.y][other.x] == 2) {
			if (rand.nextInt((int) Math.ceil(1.2 / other.noise * (Math.abs(dy) + Math.abs(dx)))) == 0) return "You hear a loud splash in the " + direction(dx, dy) + ".";
		} else {
			int noise = 0;
			for (int y = -2; y <= 2; y++) {
				for (int x = -2; x <= 2; x++) {
					try {
						if (Server.world[other.y + y][other.x + x] == 1) noise += 4 - (Math.abs(x) + Math.abs(y));
					} catch (ArrayIndexOutOfBoundsException e) {}
				}
			}
			if (rand.nextInt((int) Math.ceil(1.0 / other.noise * (Math.abs(dy) + Math.abs(dx)) * (4 - Math.sqrt(noise) * 0.6))) == 0) {
				return "You hear " + (rand.nextBoolean() ? "twigs snapping" : "leaves crinkling") + " in the " + direction(dx, dy) + ".";
			}
		}

		return null;
	}

	private static String direction(int dx, int dy) {
		List<String> dirs = new ArrayList<>();

		if (dy < 0) dirs.add("south");
		if (dy > 0) dirs.add("north");
		if (dx < 0) dirs.add("east");
		if (dx > 0) dirs.add("west");

		return dirs.get(rand.nextInt(dirs.size()));
	}

	/**
	 * <ul>
	 * <li><b><i>move</i></b><br>
	 * <br>
	 * {@code int move()}<br>
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

	public void print(String text) {
		out.println(text);
		out.flush();
		System.out.println(Server.date.format(new Date()) + " [SERVER -> " + this.name + "] " + text.replace("\n", "\n\t"));
	}

	public String read() throws IOException {
		String ret = in.readLine();
		System.out.println(Server.date.format(new Date()) + " [" + this.name + "] " + ret);
		return ret;
	}

	public void request() {
		out.println(">");
		System.out.println(Server.date.format(new Date()) + " [INFO] Requesting output from " + this.name);
		out.flush();
	}

	public void close() throws IOException {
		out.println((char) 4);
		out.flush();
		out.close();
		in.close();
		socket.close();
	}

}
