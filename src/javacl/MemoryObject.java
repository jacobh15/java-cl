package javacl;

import java.nio.ByteBuffer;

import org.lwjgl.opencl.CL10;
import org.lwjgl.system.MemoryUtil;

public abstract class MemoryObject extends CLObject implements KernelArgumentSetter, Releaseable {
	
	protected MemoryType type;
	protected ByteBuffer hostMemory;
	protected boolean useHostPtr, useHostMem, copiedHost, read, write;
	protected Context context;
	
	MemoryObject(long p, Context ctx, MemoryType type, ByteBuffer buffer, 
			boolean read, boolean write, boolean useHostPtr, boolean useHostMem, boolean copiedHost) {
		super(p);
		context = ctx;
		hostMemory = buffer;
		this.read = read;
		this.write = write;
		this.useHostPtr = useHostPtr;
		this.useHostMem = useHostMem;
		this.copiedHost = copiedHost;
		this.type = type;
	}
	
	public void release(){
		cl.errorCheck(CL10.clReleaseMemObject(ptr));
		MemoryUtil.memFree(hostMemory);
	}
	
	public Context getContext(){
		return context;
	}
	
	public String toString(){
		return type + " in " + context;
	}
	
	public MemoryType getType(){
		return type;
	}
	
	public ByteBuffer getHostMemory(){
		return hostMemory;
	}
	
	public boolean canRead(){
		return read;
	}
	
	public boolean isReadOnly(){
		return read && !write;
	}
	
	public boolean canWrite(){
		return write;
	}
	
	public boolean isWriteOnly(){
		return write && !read;
	}
	
	public boolean isReadAndWrite(){
		return write && read;
	}
	
	public boolean deviceUsingHostMemory(){
		return useHostMem;
	}
	
	public boolean deviceUsingHostDataBuffer(){
		return useHostPtr;
	}
	
	public boolean deviceCopiedHostDataBuffer(){
		return copiedHost;
	}
	
	public boolean hostHasDataBuffer(){
		return hostMemory != null;
	}
	
	public boolean deviceHasDataBuffer(){
		return ptr != 0;
	}
}
