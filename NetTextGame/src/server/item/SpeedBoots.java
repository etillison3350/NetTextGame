//package server.item;
//
//import server.Player;
//
//public class SpeedBoots extends Item {
//
//	public int uses = 16;
//	
//	public SpeedBoots() {
//		super("speed boots");
//	}
//	
//	@Override
//	public String use(Player player) {
//		return "*Your speed boots crumble into a pile of leathery shards.";
//	}
//	
//	@Override
//	public boolean move(Player player) {
//		player.moves++;
//		uses--;
//		return true;
//	}
//
//}
