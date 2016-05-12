package server.old;

public class ItemEntity extends Entity {

	public final Item item;
	
	public ItemEntity(Item item, int x, int y) {
		super(item.type.name, x, y, true, 3);
		
		this.item = item;
	}
	
}
