package server;

public class Entity {

	private String name;
	private int x, y, order;
	private double[] states;
	private boolean passable;

	/*
	 * Orders: -2: crystal, 0: player, 1: item, 2: tree, 4: silhouette, 6: log/stump
	 */
	
	public Entity(String name, int x, int y, boolean passable, int order) {
		this(name, x, y, passable, order, new double[0]);
	}

	public Entity(String name, int x, int y, boolean passable, int order, double... states) {
		this.name = name;
		this.x = x;
		this.y = y;
		this.passable = passable;
		this.order = order;
		this.states = states;
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
	
	public void setState(int n, double state) {
		this.states[n] = state;
	}
	
	public double addState(int n, double state) {
		return (this.states[n] += state);
	}
	
	public double getState(int n) {
		return states[n];
	}
	
	public double getState() {
		return getState(0);
	}
	
	public boolean hasState(int n) {
		return states.length > n;
	}
	
	public int numStates() {
		return states.length;
	}

	public boolean isPassable() {
		return passable;
	}

	public void setPassable(boolean passable) {
		this.passable = passable;
	}

}
