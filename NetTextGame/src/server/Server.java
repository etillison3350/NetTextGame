package server;

import java.awt.Point;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.SortedSet;
import java.util.TreeSet;

public class Server {

	public static final SimpleDateFormat date = new SimpleDateFormat("[HH:mm:ss.SSS]");
	public static final Random rand = new Random();

	public static Terrain[][] world;
	public static SortedSet<Entity> objects;
	public static Player p1, p2;
	public static Entity crystal;

	public static void main(String[] args) {
		try (ServerSocket socket = new ServerSocket()) {
			int port = 0;
			Scanner input = new Scanner(System.in);
			while (true) {
				try {
					System.out.println("Enter IP address:");
					String address = input.nextLine();
					while (true) {
						System.out.println("Enter port:");
						try {
							port = Integer.parseInt(input.nextLine());
							if (port > 0 && port < 65536) break;
						} catch (Exception e) {}
					}
					socket.bind(new InetSocketAddress(address, port));
					break;
				} catch (Exception e) {
					print("Failed to bind to port. Perhaps another server is already running on that port?");
				}
			}
			input.close();
			print("Server bound to " + socket.getInetAddress().getHostAddress() + ":" + port);

			int xc, yc;
			do {
				double t = rand.nextDouble() * Math.PI * 2;
				double r = 7 + 7 * rand.nextGaussian();
				xc = (int) Math.round(r * Math.cos(t) + 14.5);
				yc = (int) Math.round(r * Math.sin(t) + 14.5);
			} while (xc < 0 || xc >= 30 || yc < 0 || yc >= 30);

			int x1, y1;
			do {
				double t = rand.nextDouble() * Math.PI * 2;
				double r = 15 + 3.21 * rand.nextGaussian();
				x1 = (int) Math.round(r * Math.cos(t) + 14.5);
				y1 = (int) Math.round(r * Math.sin(t) + 14.5);
			} while (x1 < 0 || x1 >= 30 || y1 < 0 || y1 >= 30);

			int x2, y2;
			do {
				double t = rand.nextDouble() * Math.PI * 2;
				double r = 15 + 3.21 * rand.nextGaussian();
				x2 = (int) Math.round(r * Math.cos(t) + x1);
				y2 = (int) Math.round(r * Math.sin(t) + y1);
			} while (x2 < 0 || x2 >= 30 || y2 < 0 || y2 >= 30);
			print("Generating world...");
			generateWorld(xc, yc, x1, y1, x2, y2);

			crystal = new Entity("item-the crystal", xc, yc, true, -2);
			objects.add(crystal);

			print("Waiting for players to connect...");
			p1 = new Player(socket.accept(), 1, x1, y1);
			print("Accepted player 1 at " + p1.socket.getInetAddress().getHostAddress());
			Thread player1 = new Thread(new Runnable() {

				@Override
				public void run() {
					try {
						p1.print("Welcome.\nWould you like to be the hunter or the hunted?");
						while (true) {
							String h = p1.read().toLowerCase();
							if (h.matches("hunte[dr]")) {
								p1.hunter = h.charAt(5) == 'r';
								break;
							} else {
								p1.print("Please write \"hunter\" or \"hunted\"");
							}
						}
					} catch (IOException e) {
						try {
							p2.print("The other player has disconnected from the server.");
						} catch (Exception exception) {}
					}
				}

			});
			player1.start();
			p2 = new Player(socket.accept(), 2, x2, y2);
			p2.print("Welcome.\nWaiting for the other player...");
			p2.hunter = !p1.hunter;
			player1.join();

			p1.print("You are " + (p1.hunter ? "hunting." : "being hunted."));
			p2.print("You are " + (p2.hunter ? "hunting." : "being hunted."));
			if (p2.hunter)
				p1.resetMoves();
			else
				p2.resetMoves();

			new Thread(runnableFor(p1, p2)).start();
			new Thread(runnableFor(p2, p1)).start();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private static boolean gameOver = false;

	private static Runnable runnableFor(Player player, Player other) {
		return new Runnable() {

			@Override
			public void run() {
				while (!gameOver) {
					try {
						if (player.moves > 0) player.print("Enter an action (" + player.moves + " moves left):");
						player.execute(player.read().toLowerCase());
					} catch (IOException e) {
						other.print("The other player has disconnected from ther server.");
						gameOver = true;
						break;
					}

					if (player.hunter && player.getX() == other.getX() && player.getY() == other.getY()) {
						player.print("You come upon a lone man walking through the forest. Drawing a knife, you approach the man from behind, and drive the knife between his ribs.\nYou win!");
						other.print("You hear a twig snap behind you. You turn around, but there is nobody there. As you turn back around, you see a flash of silver, then you cry out in pain and collapse in a pool of your own blood.\nYou lose.");
						gameOver = true;
						break;
					} else if (!player.hunter && player.escaped) {
						player.print("In the corner of your vision, you see a faint light. As you sprint toward it, you recognize a streetlight overlooking a road. You have escaped!\nYou win!");
						other.print("You search for hours longer, but as the sun illuminates the forest, you have found nothing. Your quarry must have escaped.\nYou lose.");
						gameOver = true;
						break;
					}

					if (player.moves <= 0 && player.moves > -32768) {
						player.moves = -65536;
						other.resetMoves();
						other.print("Enter an action (" + other.moves + " moves left):");
					}
				}

				try {
					player.close();
				} catch (IOException e) {}
				try {
					other.close();
				} catch (IOException e) {}
			}
		};
	}

	/**
	 * <ul>
	 * <li><b><i>print</i></b><br>
	 * <br>
	 * {@code protected static void print(String text)}<br>
	 * <br>
	 * Outputs the given text as a server log<br>
	 * @param text - The text to output
	 *        </ul>
	 */
	protected static void print(String text) {
		System.out.println(date.format(new Date()) + " [INFO] " + text);
	}

	/**
	 * <ul>
	 * <li><b><i>generateWorld</i></b><br>
	 * <br>
	 * {@code private static void generateWorld(int xc, int yc, int x1, int y1, int x2, int y2)}<br>
	 * <br>
	 * Generates a world, making sure that players can complete their objective.<br>
	 * @param xc The x coordinate of the crystal
	 * @param yc The y coordinate of the crystal
	 * @param x1 The x coordinate of the first player
	 * @param y1 The y coordinate of the first player
	 * @param x2 The x coordinate of the second player
	 * @param y2 The y coordinate of the second player
	 * @return The generated world.
	 *         </ul>
	 */
	private static void generateWorld(int xc, int yc, int x1, int y1, int x2, int y2) {
		world = new Terrain[30][30];
		objects = new TreeSet<>(new Comparator<Entity>() {

			@Override
			public int compare(Entity o1, Entity o2) {
				int d = Integer.compare(o1.getX(), o2.getX());
				if (d != 0) return d;
				d = Integer.compare(o1.getY(), o2.getY());
				if (d != 0) return d;
				d = Integer.compare(o1.getOrder(), o2.getOrder());
				if (d != 0) return d;
				return Integer.compare(o1.hashCode(), o2.hashCode());
			}

		});

		for (int y = 0; y < 30; y++) {
			for (int x = 0; x < 30; x++) {
				world[y][x] = Terrain.FOREST;
			}
		}

		List<Point> field = new ArrayList<>();
		int fx = rand.nextInt(30), fy = rand.nextInt(30);
		field.add(new Point(fx, fy));
		world[fy][fx] = Terrain.CLEARING;
		for (int fs = rand.nextInt(25) + 25; field.size() < fs;) {
			Point e = field.get(rand.nextInt(field.size()));
			if (rand.nextFloat() > 6F / (Math.abs(fx - e.x) + Math.abs(fy - e.y))) continue;
			List<Point> adj = new ArrayList<>();
			if (e.x > 0 && world[e.y][e.x - 1] != Terrain.CLEARING) adj.add(new Point(e.x - 1, e.y));
			if (e.x < 29 && world[e.y][e.x + 1] != Terrain.CLEARING) adj.add(new Point(e.x + 1, e.y));
			if (e.y > 0 && world[e.y - 1][e.x] != Terrain.CLEARING) adj.add(new Point(e.x, e.y - 1));
			if (e.y < 29 && world[e.y + 1][e.x] != Terrain.CLEARING) adj.add(new Point(e.x, e.y + 1));
			if (adj.size() <= 0) continue;
			Point n = adj.get(rand.nextInt(adj.size()));
			world[n.y][n.x] = Terrain.CLEARING;
			field.add(n);
		}

		final int rvr = rand.nextInt(4) + 2;
		for (int r = 0; r < rvr; r++) {
			int side = rand.nextInt(4);
			double rx = side == 0 ? 0 : (side == 2 ? 29 : rand.nextInt(30));
			double ry = side == 1 ? 0 : (side == 3 ? 29 : rand.nextInt(30));
			double d = (side + rand.nextDouble() - 0.5) * Math.PI * 0.5;

			double l = rand.nextInt(30) + 10;
			for (int i = 0; i < l; i++) {
				world[(int) ry][(int) rx] = Terrain.RIVER;
				rx = Math.max(0, Math.min(29, rx + 0.7 * Math.cos(d)));
				ry = Math.max(0, Math.min(29, ry + 0.7 * Math.sin(d)));
				d += rand.nextGaussian() * 0.4;
			}
		}

		for (int y = 0; y < 30; y++) {
			for (int x = 0; x < 30; x++) {
				if (world[y][x] == Terrain.FOREST && !(rand.nextDouble() > 0.35 || (x == xc && y == yc) || (x == x1 && y == y1) || (x == x2 && y == y2))) {
					objects.add(new Entity("tree", x, y, false, 2, Math.max(1, 12 + rand.nextGaussian() * 2)));
				}
			}
		}

		try {
			Pathfinder.getPathFromEdge(new Point(xc, yc));
			Pathfinder.getPath(new Point(xc, yc), new Point(x1, y1));
			Pathfinder.getPath(new Point(x1, y1), new Point(x2, y2));
		} catch (Exception e) {
			generateWorld(xc, yc, x1, y1, x2, y2);
		}
	}

	public static boolean isPassable(int x, int y) {
		if (x < 0 || x >= 30 || y < 0 || y >= 30) return false;
		for (Entity e : getEntitiesAt(x, y)) {
			if (!e.isPassable()) return false;
		}
		return true;
	}

	public static List<Entity> getEntitiesAt(int x, int y) {
		return new ArrayList<>(objects.subSet(new Placeholder(x, y, false), new Placeholder(x, y, true)));
	}

	private static final class Placeholder extends Entity {

		public Placeholder(int x, int y, boolean end) {
			super("", x, y, true, end ? Integer.MAX_VALUE : Integer.MIN_VALUE);
		}

	}

}
