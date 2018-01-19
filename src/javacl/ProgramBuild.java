package javacl;

public class ProgramBuild extends CLObject {
	
	private Device device;
	
	public ProgramBuild(long p, Device d){
		super(p);
		device = d;
	}
	
	public Device getDevice(){
		return device;
	}
}
