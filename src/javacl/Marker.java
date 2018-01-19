package javacl;

public class Marker extends Event {
	
	private boolean set;
	
	public Marker(){
		super(0, null);
		set = false;
	}
	
	Marker(long p, CommandQueue q){
		super(p, q);
		set = true;
	}
	
	void set(long p, CommandQueue q){
		ptr = p;
		this.q = q;
		set = true;
	}
	
	public boolean wasQueued(){
		return set;
	}
}
