package server;

public enum Terrain {
	FOREST,
	RIVER,
	CLEARING;

	public String getMessage() {
		return "You are in a" + (this.name().matches("^[AEIOU].+?") ? "n " : " ") + this.name().toLowerCase() + ".";
	}
}
