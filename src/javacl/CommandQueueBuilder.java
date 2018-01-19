package javacl;

import java.nio.IntBuffer;

import org.lwjgl.opencl.CL10;
import org.lwjgl.system.MemoryStack;

public class CommandQueueBuilder {
	
	private Context context;
	private Device device;
	private boolean outOfOrder;
	private boolean profiling;
	
	CommandQueueBuilder(Context c){
		context = c;
		device = c.devices.get(0);
	}
	
	public CommandQueue build(){
		CommandQueue Q = null;
		try(MemoryStack stack = MemoryStack.stackPush()){
			long props = (outOfOrder ? CL10.CL_QUEUE_OUT_OF_ORDER_EXEC_MODE_ENABLE: 0) |
					(profiling ? CL10.CL_QUEUE_PROFILING_ENABLE : 0);
			IntBuffer err = stack.mallocInt(1);
			Q = new CommandQueue(CL10.clCreateCommandQueue(context.ptr, device.ptr, props, err), context, device, profiling);
			OpenCLTools.cl().errorCheck(err.get(0));
		}catch(CLException e){
			throw e;
		}catch(Exception e){
			e.printStackTrace();
		}
		clear();
		context.commandQueues.add(Q);
		return Q;
	}
	
	public CommandQueueBuilder profiling(boolean prof){
		profiling = prof;
		return this;
	}
	
	public CommandQueueBuilder outOfOrderMode(boolean ooo){
		outOfOrder = ooo;
		return this;
	}
	
	public CommandQueueBuilder context(Context c){
		context = c;
		device = c.devices.get(0);
		return this;
	}
	
	public CommandQueueBuilder device(Device d){
		if(context.devices.contains(d))
			device = d;
		else
			throw new IllegalArgumentException("Device is not in specified context.");
		return this;
	}
	
	public void clear(){
		outOfOrder = false;
		profiling = false;
		device = context.devices.get(0);
	}
}
