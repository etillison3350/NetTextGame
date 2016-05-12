package server;

public class Entity {

	private String name;
	private int x, y, order;
	private boolean passable;

	public Entity(String name, int x, int y, boolean passable, int order) {
		this.name = name;
		this.x = x;
		this.y = y;
		this.order = order;
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

	public boolean isPassable() {
		return passable;
	}

	public void setPassable(boolean passable) {
		this.passable = passable;
	}

}
