package server.old;

import java.awt.Point;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public enum ItemType {

	CRYSTAL("Crystal"),
	COMPASS("Compass", 2, (player, args) -> {
		ArrayDeque<Point> path = new ArrayDeque<>();
		if (player.hunter) {
			if (player == Server.p1)
				path.addAll(Pathfinder.getPath(new Point(player.getX(), player.getY()), new Point(Server.p2.getX(), Server.p2.getY())));
			else
				path.addAll(Pathfinder.getPath(new Point(player.getX(), player.getY()), new Point(Server.p1.getX(), Server.p1.getY())));
		} else if (player.items.contains(((ItemEntity) Server.crystal).item)) {
			Pathfinder.getPathFromEdge(new Point(player.getX(), player.getY())).forEach(path::addFirst);
		} else {
			path.addAll(Pathfinder.getPath(new Point(player.getX(), player.getY()), new Point(Server.crystal.getX(), Server.crystal.getY())));
		}

		int n = Math.min((int) (10 + Server.rand.nextGaussian() / 3), path.size());

		List<String> dirs = new ArrayList<>();

		Point p = path.pop();
		int l = 1;
		for (int i = 1; i < n; i++) {
			Point p2 = path.pop();

			String dir;
			if (p2.x > p.x)
				dir = "east";
			else if (p2.x < p.x)
				dir = "west";
			else if (p2.y > p.y)
				dir = "south";
			else
				dir = "north";

			if (dir.equals(dirs.get(dirs.size() - 1))) {
				l++;
			} else {
				if (l > 1) dirs.set(dirs.size() - 1, dirs.get(dirs.size() - 1) + " " + l + "x");
				dirs.add(dir);
				l = 1;
			}

			p = p2;
		}
		if (l > 1) dirs.set(dirs.size() - 1, dirs.get(dirs.size() - 1) + " " + l + "x");

		return "Your compass gives you the following directions:\nGo " + dirs.stream().collect(Collectors.joining(", then ")) + ".\nThen it dissolves into dust.";
	}),
	SPEED("Speed Boots", 1, 1, 0.75, 11),
	TRANQUILITY("Tranquility Shoes", 1, 2.0 / 3.0, 1, 11),
	ENERGY("Energy Stone", 1.7, 1, 1, 11),
	SILHOUETTE("Silhouette", 1, (player, args) -> {
		Silhouette s = new Silhouette(player);
		s.move();
		Server.objects.add(s);
		return "A dark, humanoid figure appears, and noisily trudges away through the woods."; 
	}),
	GUIDE("Guided Legs", 11, null),
	HERBICIDE("Herbicide", 3, (player, args) -> {
		boolean done = false;
		for (int y = -3; y <= 3; y++) {
			for (int x = -3; x <= 3; x++) {
				for (Entity e : Server.getEntitiesAt(x, y)) {
					if (e.getName().equals("tree") && Server.rand.nextInt(4) != 0) {
						done = true;
						Server.objects.remove(e);
					}
				}
			}
		}
		
		return "You open the container of herbicide, " + (done ? "and several nearby trees shrivel and disintegrate." : "but there are no trees nearby to remove.");
	}),
	GHOST("Ghostly Amulet", 11, null),
	SAPLING("Sapling", 4, (player, args) -> {
		return null;
	});

	public final String name;
	public final double moveMult, noiseMult, moveCostMult;
	public final BiFunction<Player, String[], String> use;
	public final int moves;
	private int uses;

	private ItemType(String name) {
		this(name, 0, null);
	}

	private ItemType(String name, int moves, BiFunction<Player, String[], String> use) {
		this(name, moves, 1, 1, 1, 1, use);
	}

	private ItemType(String name, double moveMult, double noiseMult, double moveCostMult, int uses) {
		this(name, 0, moveMult, noiseMult, moveCostMult, uses, null);
	}

	private ItemType(String name, int moves, double moveMult, double noiseMult, double moveCostMult, int uses, BiFunction<Player, String[], String> use) {
		this.name = name;
		this.moves = moves;
		this.moveMult = moveMult;
		this.noiseMult = noiseMult;
		this.moveCostMult = moveCostMult;
		this.use = use;

		this.uses = uses;
	}

	/**
	 * <ul>
	 * <li><b><i>getName</i></b><br>
	 * <br>
	 * {@code public abstract void getName()}<br>
	 * <br>
	 * Get the name of this item.<br>
	 * </ul>
	 */
	public String getName() {
		return name;
	}

	public String use(Player player, String[] args) {
		if (use != null) {
			uses--;
			return use.apply(player, args);
		} else {
			return "Your " + this.name.toLowerCase() + "dissolve into dust.";
		}
	}

}
