package server;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

public class Pathfinder {

	private Pathfinder() {}

	public static List<Point> getPath(Collection<Point> start, Point end) {
		TreeSet<Node> open = new TreeSet<>(new Comparator<Node>() {

			@Override
			public int compare(Node o1, Node o2) {
				int d = Double.compare(o1.cost + Math.abs(end.x - o1.x) + Math.abs(end.y - o1.y), o1.cost + Math.abs(end.x - o2.x) + Math.abs(end.y - o2.y));
				if (d == 0) {
					d = Integer.compare(o1.x, o2.x);
					if (d == 0) d = Integer.compare(o1.y, o2.y);
				}
				return d;
			}

		});

		Node[][] nodes = new Node[30][30];
		for (int y = 0; y < 30; y++) {
			for (int x = 0; x < 30; x++) {
				nodes[y][x] = new Node(x, y);
				if (!Server.isPassable(x, y)) nodes[y][x].closed = true;
			}
		}

		start.stream().forEach(p -> {
			Node n = nodes[p.y][p.x];
			if (n.closed) return;
			n.cost = 0;
			open.add(n);
		});

		while (!open.isEmpty()) {
			Node node = open.first();
			open.remove(node);

			List<Node> adj = new ArrayList<>();
			if (node.x > 0) adj.add(nodes[node.y][node.x - 1]);
			if (node.x < 29) adj.add(nodes[node.y][node.x + 1]);
			if (node.y > 0) adj.add(nodes[node.y - 1][node.x]);
			if (node.y < 29) adj.add(nodes[node.y + 1][node.x]);

			for (Node n : adj) {
				if (n.closed) continue;
				if (n.x == end.x && n.y == end.y) {
					List<Point> ret = new ArrayList<>();
					ret.add(new Point(n.x, n.y));
					Node nd = node;
					while (nd != null) {
						ret.add(0, new Point(nd.x, nd.y));
						nd = nd.parent;
					}
					return ret;
				}
				if (n.cost > node.cost + 1) {
					n.cost = node.cost + 1;
					n.parent = node;
					open.add(n);
				}
			}

			node.closed = true;
		}

		throw new IllegalArgumentException("No paths could be found between those points.");
	}

	public static List<Point> getPath(Point start, Point end) {
		List<Point> startList = new ArrayList<>();
		startList.add(start);
		return getPath(startList, end);
	}

	public static List<Point> getPathFromEdge(Point end) {
		List<Point> startList = new ArrayList<>();
		for (int x = 0; x < 30; x++) {
			if (Server.isPassable(x, 0)) startList.add(new Point(x, 0));
			if (Server.isPassable(x, 29)) startList.add(new Point(x, 29));
		}
		for (int y = 0; y < 30; y++) {
			if (Server.isPassable(0, y)) startList.add(new Point(0, y));
			if (Server.isPassable(29, y)) startList.add(new Point(29, y));
		}
		return getPath(startList, end);
	}

	private static class Node {

		final int x, y;
		double cost = Integer.MAX_VALUE;
		boolean closed;
		Node parent;

		public Node(int x, int y) {
			this.x = x;
			this.y = y;
		}
	}

}
