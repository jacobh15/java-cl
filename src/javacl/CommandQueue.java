package javacl;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.PointerBuffer;
import org.lwjgl.opencl.CL10;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

public class CommandQueue extends CLObject implements Releaseable {
	
	private Context context;
	private Device device;
	private boolean profilingEnabled;
	private List<PointerBuffer> eventLists;
	
	public CommandQueue(long p, Context c, Device d, boolean profiling){
		super(p);
		context = c;
		device = d;
		profilingEnabled = profiling;
		eventLists = new ArrayList<>();
	}
	
	public void waitForCompletion(){
		CL10.clFinish(ptr);
		for(int i = 0; i < eventLists.size(); i++) {
			OpenCLTools.getOpenCLTools().releasePointerBuffer(eventLists.get(i));
		}
		eventLists.clear();
	}
	
	public Context getContext(){
		return context;
	}
	
	public Device getDevice(){
		return device;
	}
	
	public void release(){
		cl.errorCheck(CL10.clReleaseCommandQueue(ptr));
	}
	
	public boolean profilingEnabled(){
		return profilingEnabled;
	}
	
	public Event enqueue(Kernel kernel, NDRange workSpace, Event...waitlist){
		Event ev = null;
		PointerBuffer list = cl.convertPointerBuffer(waitlist);
		try(MemoryStack stack = MemoryStack.stackPush()){
			PointerBuffer event = stack.mallocPointer(1);
			cl.errorCheck(CL10.clEnqueueNDRangeKernel(ptr, kernel.ptr, workSpace.dimensions, null,
					workSpace.globalSizes, workSpace.localSizes, list, event));
			
			ev = new Event(event.get(0), this);
			eventLists.add(list);
		}catch(CLException e){
			if(list != null)
				MemoryUtil.memFree(list);
			throw e;
		}catch(Exception e){
			e.printStackTrace();
			System.err.println("Kernel enqueuement may have failed; returning null event.");
		}
		return ev;
	}
	
	public Marker enqueue(Marker m) {
		try(MemoryStack stack = MemoryStack.stackPush()){
			PointerBuffer event = stack.mallocPointer(1);
			cl.errorCheck(CL10.clEnqueueMarker(ptr, event));
			m.set(event.get(0), this);
		}catch(CLException e){
			throw e;
		}catch(Exception e){
			e.printStackTrace();
		}
		return m;
	}
	
	public String toString(){
		return "Command queue on " + context.toString();
	}
}
