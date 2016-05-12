package server.old;

public enum Terrain {
	FOREST,
	RIVER,
	CLEARING;

	public String getMessage() {
		return "You are in a" + (this.name().substring(0, 1).matches("[AEIOU]") ? "n " : " ") + this.name().toLowerCase() + ".";
	}
}
