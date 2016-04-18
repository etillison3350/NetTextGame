package server;

import java.awt.Point;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;

public class Server {

	public static final SimpleDateFormat date = new SimpleDateFormat("[HH:mm:ss.SSS]");
	public static final Random rand = new Random();

	public static int[][] world;

	private static Player p1, p2;

	public static void main(String[] args) {
		Scanner input = new Scanner(System.in);
		System.out.println("Enter IP address:");
		String address = input.nextLine();
		int port = 0;
		while (true) {
			System.out.println("Enter port:");
			try {
				port = Integer.parseInt(input.next());
				if (port > 0 && port <= 65535) break;
			} catch (Exception e) {}
		}
		input.close();

		try (ServerSocket socket = new ServerSocket()) {
			socket.bind(new InetSocketAddress(address, port));
			print("Server bound to " + socket.getInetAddress().getHostAddress() + ":" + port);

			final int x1 = Math.max(0, Math.min(29, (int) (rand.nextGaussian() * 5 + 15)));
			final int y1 = Math.max(0, Math.min(29, (int) (rand.nextGaussian() * 5 + 15)));
			final int x2 = Math.max(0, Math.min(29, (int) (rand.nextGaussian() * 5 + 15)));
			final int y2 = Math.max(0, Math.min(29, (int) (rand.nextGaussian() * 5 + 15)));
			print("Generating world...");
			world = generateWorld(x1, y1, x2, y2);
			print("Waiting for players to connect...");

			p1 = new Player("PLAYER1", x1, y1, socket.accept());
			print("Accepted player 1 at " + p1.socket.getInetAddress().getHostAddress());

			p1.print("Welcome. Would you like to be the hunter or the hunted?");
			p1.request();
			while (true) {
				String h = p1.read();
				if (h.matches("hunte[dr]")) {
					p1.hunter = h.charAt(5) == 'r';
					break;
				} else {
					p1.print("Please write \"hunter\" or \"hunted\"");
					p1.request();
				}
			}
			p1.print("You are " + (p1.hunter ? "hunting" : "being hunted") + ".\nWaiting for another player to connect...");

			p2 = new Player("PLAYER2", x2, y2, socket.accept());
			print("Accepted player 2 at " + p2.socket.getInetAddress().getHostAddress());
			p2.hunter = !p1.hunter;
			p2.print("Welcome. You are " + (p2.hunter ? "hunting." : "being hunted."));

			boolean turn = p2.hunter;
			while (true) {
				if (turn) {
					p2.print("Waiting for other player...");

					for (p1.moves = 2 + (int) Math.abs(rand.nextGaussian() * (p1.hunter ? 1 : 0.8125)); p1.moves > 0;) {
						p1.print("Enter an action (" + p1.moves + " left):");
						p1.request();
						while (true) {
							String s = null;
							try {
								s = p1.parse(p1.read());
							} catch (IOException e) {
								p1.moves = 0;
								p2.print("The other player has disconnected from the server.");
								p1.close();
								p2.close();
								return;
							}
							if (s == null) break;
							p1.print(s.replaceFirst("^\\*", ""));
							if (s.charAt(0) == '*') {
								p1.request();
							} else {
								String noise = p2.noise(p1);
								if (noise != null) p2.print(noise);
								p1.noises.clear();
								break;
							}
						}
					}
				} else {
					p1.print("Waiting for other player...");
					for (p2.moves = 2 + (int) Math.abs(rand.nextGaussian() * (p2.hunter ? 1 : 0.8125)); p2.moves > 0;) {
						p2.print("Enter an action (" + p2.moves + " left):");
						p2.request();
						while (true) {
							String s = null;
							try {
								s = p2.parse(p2.read());
							} catch (IOException e) {
								p2.moves = 0;
								p1.print("The other player has disconnected from the server.");
								p1.close();
								p2.close();
								return;
							}
							if (s == null) break;
							p2.print(s.replaceFirst("^\\*", ""));
							if (s.charAt(0) == '*') {
								p2.request();
							} else {
								String noise = p1.noise(p2);
								if (noise != null) p1.print(noise);
								p2.noises.clear();
								break;
							}
						}
					}
				}

				turn = !turn;

				if (p1.x == p2.x && p1.y == p2.y) {
					if (p1.hunter) {
						p1.print("You come upon a lone man walking through the forest. Drawing a knife, you approach the man from behind, and drive the knife between his ribs.\nYou win!");
						int d = Math.min(Math.min(p2.x, 29 - p2.x), Math.min(p2.y, 29 - p2.y));
						if (rand.nextInt((int) Math.ceil(Math.sqrt(d))) == 0) {
							p2.print("In the corner of your vision, you see a faint light. As you sprint toward it, you trip over a root. Hearing footsteps behind you, you turn to see a masked figure. You try to get up and run, but he quickly slips a knife into your side.\nYou lose.");
						} else {
							p2.print("You hear a twig snap behind you. You turn around, but there is nobody there. As you turn back around, you see a flash of silver, then you cry out in pain and collapse in a pool of your own blood.\nYou lose.");
						}
					} else {
						p2.print("You come upon a lone man walking through the forest. Drawing a knife, you approach the man from behind, and drive the knife between his ribs.\nYou win!");
						int d = Math.min(Math.min(p1.x, 29 - p1.x), Math.min(p1.y, 29 - p1.y));
						if (rand.nextInt((int) Math.ceil(Math.sqrt(d))) == 0) {
							p1.print("In the corner of your vision, you see a faint light. As you sprint toward it, you trip over a root. Hearing footsteps behind you, you turn to see a masked figure. You try to get up and run, but he quickly slips a knife into your side.\nYou lose.");
						} else {
							p1.print("You hear a twig snap behind you. You turn around, but there is nobody there. As you turn back around, you see a flash of silver, then you cry out in pain and collapse in a pool of your own blood.\nYou lose.");
						}
					}

					break;
				} else if (p1.won || p2.won) {
					if (p1.won) {
						if (p1.hunter) {
							p1.print("You come upon a lone man walking through the forest. Drawing a knife, you approach the man from behind, and drive the knife between his ribs.\nYou win!");
							p2.print("You hear a twig snap behind you. You turn around, but there is nobody there. As you turn back around, you see a flash of silver, then you cry out in pain and collapse in a pool of your own blood.\nYou lose.");
						} else {
							p1.print("In the corner of your vision, you see a faint light. As you sprint toward it, you recognize a streetlight overlooking a road. You have escaped!\nYou win!");
							p2.print("You search for hours longer, but as the sun illuminates the forest, you have found nothing. Your quarry must have escaped.\nYou lose.");
						}
					} else {
						if (p2.hunter) {
							p2.print("You come upon a lone man walking through the forest. Drawing a knife, you approach the man from behind, and drive the knife between his ribs.\nYou win!");
							p1.print("You hear a twig snap behind you. You turn around, but there is nobody there. As you turn back around, you see a flash of silver, then you cry out in pain and collapse in a pool of your own blood.\nYou lose.");
						} else {
							p2.print("In the corner of your vision, you see a faint light. As you sprint toward it, you recognize a streetlight overlooking a road. You have escaped!\nYou win!");
							p1.print("You search for hours longer, but as the sun illuminates the forest, you have found nothing. Your quarry must have escaped.\nYou lose.");
						}
					}

					break;
				}
			}

			p1.close();
			p2.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * <ul>
	 * <li><b><i>print</i></b><br>
	 * <br>
	 * {@code protected static void print(String text)}<br>
	 * <br>
	 * Outputs the given text as a server log<br>
	 * @param text The text to output
	 *        </ul>
	 */
	protected static void print(String text) {
		System.out.println(date.format(new Date()) + " [INFO] " + text);
	}

	/**
	 * <ul>
	 * <li><b><i>nearby</i></b><br>
	 * <br>
	 * {@code public static String nearby(int x, int y)}<br>
	 * <br>
	 * @param x
	 * @param y
	 * @return A list of things that a player would be able to "see" from the given point (objects in a 7x7 square)
	 *         </ul>
	 */
	public static String nearby(int x, int y) {
		Set<String> ret = new TreeSet<>();

		int dx1 = Math.abs(x - p1.x);
		int dy1 = Math.abs(y - p1.y);
		int dx2 = Math.abs(x - p2.x);
		int dy2 = Math.abs(y - p2.y);
		if ((dx1 <= 3 && dy1 <= 3 && !(dx1 == 0 && dy1 == 0)) || (dx2 <= 3 && dy2 <= 3 && !(dx2 == 0 && dy2 == 0))) {
			ret.add("a humanoid silhouette");
		}

		int trees = 0;
		for (int yy = -3; yy <= 3; yy++) {
			for (int xx = -3; xx <= 3; xx++) {
				try {
					switch (world[yy + y][xx + x]) {
						case 1:
							ret.remove((trees > 9 ? "a lot of" : trees) + " tree" + (trees++ == 1 ? "" : "s"));
							ret.add((trees > 9 ? "a lot of" : trees) + " tree" + (trees == 1 ? "" : "s"));
							break;
						case 2:
							ret.add("water");
							break;
						case 3:
							ret.add("a freshly cut log");
							break;
					}
				} catch (ArrayIndexOutOfBoundsException e) {
					ret.add("a faint light");
				}
			}
		}

		if (rand.nextInt(50) == 0) ret.add("an emerald green cat");

		return ret.toString().replace("[", "").replace("]", "").replaceAll(",(\\s*[^,]+)$", ", and$1");
	}

	/**
	 * <ul>
	 * <li><b><i>traversable</i></b><br>
	 * <br>
	 * {@code public static boolean traversable(int terrain)}<br>
	 * <br>
	 * @param terrain
	 * @return Whether or not the given terrain type can be walked on.
	 *         </ul>
	 */
	public static boolean traversable(int terrain) {
		return terrain == 0 || terrain == 2 || terrain == 3;
	}

	/**
	 * <ul>
	 * <li><b><i>generateWorld</i></b><br>
	 * <br>
	 * {@code private static int[][] generateWorld(int x1, int y1, int x2, int y2)}<br>
	 * <br>
	 * Generates a world, making sure that players can complete their objective.<br>
	 * @param x1 The x coordinate of the first player
	 * @param y1 The y coordinate of the first player
	 * @param x2 The x coordinate of the second player
	 * @param y2 The y coordinate of the second player
	 * @return The generated world.
	 *         </ul>
	 */
	private static int[][] generateWorld(int x1, int y1, int x2, int y2) {
		int[][] ret = new int[30][30];

		TreeSet<Point> open = new TreeSet<>(new Comparator<Point>() {

			@Override
			public int compare(Point o1, Point o2) {
				int d = Integer.compare(Math.abs(x1 - o1.x) + Math.abs(y1 - o1.y), Math.abs(x1 - o2.x) + Math.abs(y1 - o2.y));
				if (d == 0) {
					d = Integer.compare(o1.x, o2.x);
					if (d == 0) d = Integer.compare(o1.y, o2.y);
				}
				return d;
			}

		});
		Set<Point> closed = new HashSet<>();

		for (int y = 0; y < 30; y++) {
			for (int x = 0; x < 30; x++) {
				if (rand.nextDouble() > 0.35 || (x == x1 && y == y1) || (x == x2 && y == y2)) {
					ret[y][x] = 0;
					if (x == 0 || x == 29 || y == 0 || y == 29) open.add(new Point(x, y));
				} else {
					ret[y][x] = 1;
				}
			}
		}

		final int rvr = rand.nextInt(4) + 2;
		for (int r = 0; r < rvr; r++) {
			int side = rand.nextInt(4);
			double rx = side == 0 ? 0 : (side == 2 ? 29 : rand.nextInt(30));
			double ry = side == 1 ? 0 : (side == 3 ? 29 : rand.nextInt(30));
			double d = (side + rand.nextDouble() - 0.5) * Math.PI * 0.5;

			open.remove(new Point((int) rx, (int) ry));

			double l = rand.nextInt(30) + 10;
			for (int i = 0; i < l; i++) {
				ret[(int) ry][(int) rx] = 2;
				rx = Math.max(0, Math.min(29, rx + 0.7 * Math.cos(d)));
				ry = Math.max(0, Math.min(29, ry + 0.7 * Math.sin(d)));
				d += rand.nextGaussian() * 0.4;
			}
		}

		while (!open.isEmpty()) {
			Point node = open.first();
			open.remove(node);
			closed.add(node);

			Point[] nodes = {new Point(node.x - 1, node.y), new Point(node.x + 1, node.y), new Point(node.x, node.y - 1), new Point(node.x, node.y + 1)};
			for (Point n : nodes) {
				if (n.x < 0 || n.x >= 30 || n.y < 0 || n.y >= 30 || closed.contains(n) || open.contains(n) || ret[n.y][n.x] != 0) continue;

				if (n.x == x1 && n.y == y1) return ret;

				open.add(n);
			}
		}

		return generateWorld(x1, y1, x2, y2);
	}

}
