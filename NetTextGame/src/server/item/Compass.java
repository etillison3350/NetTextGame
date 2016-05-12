//package server.item;
//
//import java.awt.Point;
//import java.util.ArrayDeque;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.stream.Collectors;
//
//import server.ItemEntity;
//import server.Pathfinder;
//import server.Player;
//import server.Server;
//
//public class Compass extends Item {
//
//	public Compass() {
//		super("compass");
//	}
//
//	@Override
//	public String use(Player player) {
//		ArrayDeque<Point> path = new ArrayDeque<>();
//		if (player.hunter) {
//			if (player == Server.p1)
//				path.addAll(Pathfinder.getPath(new Point(player.getX(), player.getY()), new Point(Server.p2.getX(), Server.p2.getY())));
//			else
//				path.addAll(Pathfinder.getPath(new Point(player.getX(), player.getY()), new Point(Server.p1.getX(), Server.p1.getY())));
//		} else if (player.items.contains(((ItemEntity) Server.crystal).item)) {
//			Pathfinder.getPathFromEdge(new Point(player.getX(), player.getY())).forEach(path::addFirst);
//		} else {
//			path.addAll(Pathfinder.getPath(new Point(player.getX(), player.getY()), new Point(Server.crystal.getX(), Server.crystal.getY())));
//		}
//
//		int n = Math.min((int) (10 + Server.rand.nextGaussian() / 3), path.size());
//		
//		List<String> dirs = new ArrayList<>();
//		
//		Point p = path.pop();
//		int l = 1;
//		for (int i = 1; i < n; i++) {
//			Point p2 = path.pop();
//			
//			String dir;
//			if (p2.x > p.x)
//				dir = "east";
//			else if (p2.x < p.x)
//				dir = "west";
//			else if (p2.y > p.y)
//				dir = "south";
//			else
//				dir = "north";
//			
//			if (dir.equals(dirs.get(dirs.size() - 1))) {
//				l++;
//			} else {
//				if (l > 1) dirs.set(dirs.size() - 1, dirs.get(dirs.size() - 1) + " " + l + "x");
//				dirs.add(dir);
//				l = 1;
//			}
//			
//			p = p2;
//		}
//		if (l > 1) dirs.set(dirs.size() - 1, dirs.get(dirs.size() - 1) + " " + l + "x");
//
//		return "*Your compass gives you the following directions:\nGo " + dirs.stream().collect(Collectors.joining(", then ")) + ".\nThen it dissolves into dust.";
//	}
//
//}
