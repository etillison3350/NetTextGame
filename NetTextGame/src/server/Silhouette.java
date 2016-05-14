package server;


public class Silhouette extends Entity {

	public final Player summoner;
	
	public Silhouette(Player summoner) {
		super("silhouette", summoner.getX(), summoner.getY(), true, 4, 18);
		this.summoner = summoner;
	}
	
	public boolean move() {
		return true;
	}

}
