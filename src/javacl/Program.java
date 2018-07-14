package javacl;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.opencl.CL10;
import org.lwjgl.system.MemoryStack;

public class Program extends CLObject implements Releaseable {
	
	private Context context;
	private List<Device> devices;
	private BuildLog buildLog;
	private List<Kernel> kernels;
	private Map<String, Kernel> kernelMap;
	
	Program(long p, Context c, List<Device> devs, BuildLog log){
		super(p);
		context = c;
		devices = new ArrayList<>(devs);
		buildLog = log;
		kernelMap = new HashMap<>();
		kernels = new ArrayList<>();
	}
	
	public void release(){
		for(int i = 0; i < kernels.size(); i++){
			cl.releaseItem(kernels.get(i));
		}
		cl.errorCheck(CL10.clReleaseProgram(ptr));
	}
	
	public Context getContext(){
		return context;
	}
	
	public Kernel createKernel(String name, List<String> argumentNames, List<MemoryType> argumentTypes){
		Kernel kernel = null;
		try(MemoryStack stack = MemoryStack.stackPush()){
			IntBuffer err = stack.mallocInt(1);
			long p = CL10.clCreateKernel(ptr, name, err);
			cl.errorCheck(err.get(0));
			kernel = new Kernel(p, this, name, argumentNames, argumentTypes);
		}catch(CLException e){
			throw e;
		}catch(Exception e){
			e.printStackTrace();
		}
		kernels.add(kernel);
		kernelMap.put(kernel.getName(), kernel);
		return kernel;
	}
	
	public ProgramBuild getBuildObject(Device d){
		if(hasDevice(d))
			return new ProgramBuild(ptr, d);
		return null;
	}
	
	public boolean hasDevice(Device d){
		return devices.contains(d);
	}
	
	public BuildLog getBuildLog(){
		return buildLog;
	}
	
	public List<Device> getDevices(){
		return new ArrayList<>(devices);
	}
	
	public List<Kernel> getKernels(){
		return new ArrayList<>(kernels);
	}
	
	public Kernel getKernel(String name){
		return kernelMap.get(name);
	}
	
	public String toString(){
		return "Program on " + context.toString();
	}
}
