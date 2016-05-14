package server;

public class Entity {

	private String name;
	private int x, y, order;
	private double state;
	private boolean passable;

	/*
	 * Orders: -2: crystal, 0: player, 2: tree, 4: silhouette, 6: log/stump
	 */
	
	public Entity(String name, int x, int y, boolean passable, int order) {
		this(name, x, y, passable, order, 0);
	}

	public Entity(String name, int x, int y, boolean passable, int order, double state) {
		this.name = name;
		this.x = x;
		this.y = y;
		this.order = order;
		this.state = state;
	}

	public String getName() {
		return name;
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public void setX(int x) {
		this.x = x;
	}

	public void setY(int y) {
		this.y = y;
	}

	public int getOrder() {
		return order;
	}
	
	public void setState(double state) {
		this.state = state;
	}
	
	public double addState(double state) {
		return (this.state += state);
	}
	
	public double getState() {
		return state;
	}

	public boolean isPassable() {
		return passable;
	}

	public void setPassable(boolean passable) {
		this.passable = passable;
	}

}
