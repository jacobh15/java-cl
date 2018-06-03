package javacl;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import org.lwjgl.PointerBuffer;
import org.lwjgl.opencl.CL10;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

public class CLBuffer extends MemoryObject {
	
	int bytes;
	int components;
	int elements;
	boolean mapped;
	PointerBuffer memPointer;
	
	CLBuffer(long p, Context ctx, MemoryType type, ByteBuffer buffer, int bytes,
			boolean read, boolean write, boolean useHostPtr, boolean useHostMem, boolean copiedHost){
		super(p, ctx, type, buffer, read, write, useHostPtr, useHostMem, copiedHost);
		this.bytes = bytes;
		components = bytes / type.bytesPerComp();
		elements = components / type.numComps();
		memPointer = MemoryUtil.memAllocPointer(1).put(0, ptr);
	}
	
	public void release(){
		CL10.clReleaseMemObject(ptr);
		MemoryUtil.memFree(memPointer);
		super.release();
	}
	
	public void setArg(long kernel, int arg){
		cl.errorCheck(CL10.clSetKernelArg(kernel, arg, memPointer));
	}
	
	public boolean deviceHasDataBuffer(){
		return true;
	}
	
	public long numElements(){
		return elements;
	}
	
	public long numComps(){
		return components;
	}
	
	public long numBytes(){
		return bytes;
	}
	
	public long byteOffsetToComponent(int c){
		return c * type.bytesPerComp();
	}
	
	public long byteOffsetToElement(int e){
		return e * type.sizeInBytes();
	}
	
	public void allocHostMemory(){
		if(!hostHasDataBuffer()){
			hostMemory = MemoryUtil.memAlloc((int)bytes);
		}
	}
	
	public Event enqueueRead(CommandQueue q, boolean block, int offset, ByteBuffer dest, Event...wait){
		PointerBuffer waitList = cl.convertPointerBuffer(wait);
		Event ev = null;
		try(MemoryStack stack = MemoryStack.stackPush()){
			PointerBuffer event = stack.mallocPointer(1);
			cl.errorCheck(CL10.clEnqueueReadBuffer(q.ptr, ptr, block, offset, dest, waitList, event));
			ev = new Event(event.get(0), q);
		}catch(CLException e){
			if(waitList != null)
				MemoryUtil.memFree(waitList);
			throw e;
		}catch(Exception e){
			e.printStackTrace();
			System.err.println("Buffer read failed; returning null event.");
		}
		return ev;
	}
	
	public Event enqueueRead(CommandQueue q, boolean block, int offset, Event...wait){
		if(hostHasDataBuffer())
			return enqueueRead(q, block, offset, hostMemory, wait);
		else
			return null;
	}
	
	public Event enqueueWrite(CommandQueue q, boolean block, int offset, ByteBuffer src, Event...wait){
		PointerBuffer waitList = cl.convertPointerBuffer(wait);
		Event ev = null;
		try(MemoryStack stack = MemoryStack.stackPush()){
			PointerBuffer event = stack.mallocPointer(1);
			cl.errorCheck(CL10.clEnqueueWriteBuffer(q.ptr, ptr, block, offset, src, waitList, event));
			ev = new Event(event.get(0), q);
		}catch(CLException e){
			if(waitList != null)
				MemoryUtil.memFree(waitList);
			throw e;
		}catch(Exception e){
			e.printStackTrace();
		}
		return ev;
	}
	
	public Event enqueueWrite(CommandQueue q, boolean block, int offset, Event...wait){
		if(hostHasDataBuffer())
			return enqueueWrite(q, block, offset, hostMemory, wait);
		else
			return null;
	}
	
	public Event enqueueCopyTo(CommandQueue q, int offset, int destOffset, CLBuffer dest, Event...wait){
		PointerBuffer waitList = cl.convertPointerBuffer(wait);
		Event ev = null;
		try(MemoryStack stack = MemoryStack.stackPush()){
			PointerBuffer event = stack.mallocPointer(1);
			cl.errorCheck(CL10.clEnqueueCopyBuffer(q.ptr, ptr, dest.ptr, offset, destOffset, bytes - offset,
					waitList, event));
			ev = new Event(event.get(0), q);
		}catch(CLException e){
			if(waitList != null)
				MemoryUtil.memFree(waitList);
			throw e;
		}catch(Exception e){
			e.printStackTrace();
		}
		return ev;
	}
	
	public Event enqueueCopyFrom(CommandQueue q, int offset, int srcOffset, CLBuffer src, Event...wait){
		PointerBuffer waitList = cl.convertPointerBuffer(wait);
		Event ev = null;
		try(MemoryStack stack = MemoryStack.stackPush()){
			PointerBuffer event = stack.mallocPointer(1);
			cl.errorCheck(CL10.clEnqueueCopyBuffer(q.ptr, src.ptr, ptr, srcOffset, offset, src.bytes - srcOffset,
					waitList, event));
			ev = new Event(event.get(0), q);
		}catch(CLException e){
			if(waitList != null)
				MemoryUtil.memFree(waitList);
			throw e;
		}catch(Exception e){
			e.printStackTrace();
		}
		return ev;
	}
	
	public Event enqueueMap(CommandQueue q, boolean block, MapType type, int offset, Event...wait){
		PointerBuffer waitList = cl.convertPointerBuffer(wait);
		Event ev = null;
		try(MemoryStack stack = MemoryStack.stackPush()){
			PointerBuffer event = stack.mallocPointer(1);
			IntBuffer error = stack.mallocInt(1);
			CL10.clEnqueueMapBuffer(q.ptr, ptr, block, type.clMapType, offset, bytes - offset,
					waitList, event, error, hostMemory);
			cl.errorCheck(error.get(0));
		}catch(CLException e){
			if(waitList != null)
				MemoryUtil.memFree(waitList);
			throw e;
		}catch(Exception e){
			e.printStackTrace();
		}
		return ev;
	}
	
	public Event enqueueUnmap(CommandQueue q, Event...wait){
		PointerBuffer waitList = cl.convertPointerBuffer(wait);
		Event ev = null;
		try(MemoryStack stack = MemoryStack.stackPush()){
			PointerBuffer event = stack.mallocPointer(1);
			cl.errorCheck(CL10.clEnqueueUnmapMemObject(q.ptr, ptr, hostMemory, waitList, event));
		}catch(CLException e){
			if(waitList != null)
				MemoryUtil.memFree(waitList);
			throw e;
		}catch(Exception e){
			e.printStackTrace();
		}
		return ev;
	}
	
	public Event enqueueCopyToImage(CommandQueue q, int offset, ImageRegion region, CLImage dest, Event...wait){
		return dest.enqueueCopyFromBuffer(q, region, this, offset, wait);
	}
	
	public Event enqueueCopyFromImage(CommandQueue q, int offset, ImageRegion region, CLImage src, Event...wait){
		return src.enqueueCopyToBuffer(q, region, this, offset, wait);
	}
}
