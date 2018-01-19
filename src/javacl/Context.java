package javacl;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.opencl.CL10;

public class Context extends CLObject implements Releaseable {
	
	private Platform platform;
	List<Device> devices;
	List<MemoryObject> memObjects;
	List<Program> programs;
	List<CommandQueue> commandQueues;
	
	Context(long p, Platform plat, List<Device> devs){
		super(p);
		platform = plat;
		devices = new ArrayList<>(devs);
		memObjects = new ArrayList<>();
		programs = new ArrayList<>();
		commandQueues = new ArrayList<>();
	}
	
	public Platform getPlatform(){
		return platform;
	}
	
	public List<Device> getDevices(){
		return new ArrayList<>(devices);
	}
	
	public List<MemoryObject> getMemoryObjects(){
		return new ArrayList<>(memObjects);
	}
	
	public List<CommandQueue> getCommandQueues(){
		return new ArrayList<>(commandQueues);
	}
	
	public List<Program> getPrograms(){
		return new ArrayList<>(programs);
	}
	
	void addMemoryObject(MemoryObject obj){
		memObjects.add(obj);
	}
	
	public ProgramBuilder getProgramBuilder(String src){
		return new ProgramBuilder(this, src);
	}
	
	public CommandQueueBuilder getCommandQueueBuilder(){
		return new CommandQueueBuilder(this);
	}
	
	public MemoryObjectBuilder getMemoryObjectBuilder(){
		return new MemoryObjectBuilder(this);
	}
	
	public boolean hasDevice(Device d){
		return devices.contains(d);
	}
	
	public void release(){
		for(int i = 0; i < commandQueues.size(); i++){
			commandQueues.get(i).release();
		}
		for(int i = 0; i < memObjects.size(); i++){
			memObjects.get(i).release();
		}
		for(int i = 0; i < programs.size(); i++){
			programs.get(i).release();
		}
		cl.errorCheck(CL10.clReleaseContext(ptr));
	}
	
	public String toString(){
		return "Context on " + platform.toString();
	}
}
