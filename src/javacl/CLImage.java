package javacl;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import org.lwjgl.PointerBuffer;
import org.lwjgl.opencl.CL10;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

public class CLImage extends MemoryObject {
	
	private ImageData imageData;
	private ImageRegion wholeImage;
	private PointerBuffer memPointer;
	
	CLImage(long p, Context ctx, MemoryType type, ByteBuffer buffer, ImageData data, boolean read, boolean write, boolean useHostPtr,
			boolean useHostMem, boolean copiedHost) {
		super(p, ctx, type, buffer, read, write, useHostPtr, useHostMem, copiedHost);
		imageData = data;
		wholeImage = new ImageRegion(imageData.width, imageData.height, imageData.depth);
		memPointer = MemoryUtil.memAllocPointer(1).put(0, ptr);
		cl.removeFromReleaseQueue(imageData);
		cl.removeFromReleaseQueue(wholeImage);
	}
	
	public void release(){
		imageData.release();
		wholeImage.release();
		MemoryUtil.memFree(memPointer);
		super.release();
	}
	
	public boolean deviceHasDataBuffer(){
		return true;
	}
	
	public void setArg(long kernel, int arg){
		CL10.clSetKernelArg(kernel, arg, memPointer);
	}
	
	public ImageData getImageData(){
		return imageData;
	}
	
	public Event enqueueRead(CommandQueue q, boolean block, ImageRegion region, ByteBuffer dest, Event...wait){
		PointerBuffer waitList = cl.convertPointerBuffer(wait);
		int b = block ? 1: 0;
		Event ev = null;
		try(MemoryStack stack = MemoryStack.stackPush()){
			PointerBuffer event = stack.mallocPointer(1);
			cl.errorCheck(CL10.clEnqueueReadImage(q.ptr, ptr, b, region.origin, region.region,
					imageData.rowPitch, imageData.slicePitch, dest, waitList, event));
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
	
	public Event enqueueRead(CommandQueue q, boolean block, ByteBuffer dest, Event...wait){
		return enqueueRead(q, block, wholeImage, dest, wait);
	}
	
	public Event enqueueRead(CommandQueue q, boolean block, ImageRegion region, Event...wait){
		if(hostHasDataBuffer())
			return enqueueRead(q, block, region, hostMemory, wait);
		else
			return null;
	}
	
	public Event enqueueRead(CommandQueue q, boolean block, Event...wait){
		return enqueueRead(q, block, wholeImage, hostMemory, wait);
	}
	
	public Event enqueueWrite(CommandQueue q, boolean block, ImageRegion region, ByteBuffer src, Event...wait){
		PointerBuffer waitList = cl.convertPointerBuffer(wait);
		int b = block ? 1: 0;
		Event ev = null;
		try(MemoryStack stack = MemoryStack.stackPush()){
			PointerBuffer event = stack.mallocPointer(1);
			cl.errorCheck(CL10.clEnqueueWriteImage(q.ptr, ptr, b, region.origin, region.region,
					imageData.rowPitch, imageData.slicePitch, src, waitList, event));
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
	
	public Event enqueueWrite(CommandQueue q, boolean block, ByteBuffer src, Event...wait){
		return enqueueWrite(q, block, wholeImage, src, wait);
	}
	
	public Event enqueueWrite(CommandQueue q, boolean block, ImageRegion region, Event...wait){
		if(hostHasDataBuffer())
			return enqueueWrite(q, block, region, hostMemory, wait);
		else
			return null;
	}
	
	public Event enqueueWrite(CommandQueue q, boolean block, Event...wait){
		return enqueueWrite(q, block, wholeImage, hostMemory, wait);
	}
	
	public Event enqueueCopyTo(CommandQueue q, CLImage dest, ImageRegion regionSrc, ImageRegion regionDest, Event...wait){
		PointerBuffer waitList = cl.convertPointerBuffer(wait);
		Event ev = null;
		try(MemoryStack stack = MemoryStack.stackPush()){
			PointerBuffer event = stack.mallocPointer(1);
			cl.errorCheck(CL10.clEnqueueCopyImage(q.ptr, ptr, dest.ptr, regionSrc.origin, regionDest.origin, regionSrc.region,
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
	
	public Event enqueueCopyTo(CommandQueue q, CLImage dest, ImageRegion regionDest, Event...wait){
		return enqueueCopyTo(q, dest, wholeImage, regionDest, wait);
	}
	
	public Event enqueueCopyTo(CommandQueue q, CLImage dest, Event...wait){
		return enqueueCopyTo(q, dest, wholeImage, dest.wholeImage, wait);
	}
	
	public Event enqueueCopyFrom(CommandQueue q, CLImage src, ImageRegion regionSrc, ImageRegion regionDest, Event...wait){
		PointerBuffer waitList = cl.convertPointerBuffer(wait);
		Event ev = null;
		try(MemoryStack stack = MemoryStack.stackPush()){
			PointerBuffer event = stack.mallocPointer(1);
			cl.errorCheck(CL10.clEnqueueCopyImage(q.ptr, src.ptr, ptr, regionSrc.origin, 
					regionDest.origin, regionSrc.region, waitList, event));
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
	
	public Event enqueueCopyFrom(CommandQueue q, CLImage src, ImageRegion regionDest, Event...wait){
		return enqueueCopyFrom(q, src, src.wholeImage, regionDest, wait);
	}
	
	public Event enqueueMap(CommandQueue q, boolean block, MapType type, ImageRegion region, Event...wait){
		PointerBuffer waitList = cl.convertPointerBuffer(wait);
		PointerBuffer rowPitch = MemoryUtil.memAllocPointer(1).put(0, imageData.rowPitch);
		PointerBuffer slicePitch = MemoryUtil.memAllocPointer(1).put(0, imageData.slicePitch);
		int b = block ? 1: 0;
		Event ev = null;
		try(MemoryStack stack = MemoryStack.stackPush()){
			IntBuffer error = stack.mallocInt(1);
			PointerBuffer event = stack.mallocPointer(1);
			CL10.clEnqueueMapImage(q.ptr, ptr, b, type.clMapType, region.origin, region.region,
					rowPitch, slicePitch, waitList, event, error, hostMemory);
			cl.errorCheck(error.get(0));
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
	
	public Event enqueueUnmap(CommandQueue q, Event...wait){
		PointerBuffer waitList = cl.convertPointerBuffer(wait);
		Event ev = null;
		try(MemoryStack stack = MemoryStack.stackPush()){
			PointerBuffer event = stack.mallocPointer(1);
			cl.errorCheck(CL10.clEnqueueUnmapMemObject(q.ptr, ptr, hostMemory, waitList, event));
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
	
	public Event enqueueCopyFromBuffer(CommandQueue q, ImageRegion region, CLBuffer src, int srcOffset, Event...wait){
		PointerBuffer waitList = cl.convertPointerBuffer(wait);
		Event ev = null;
		try(MemoryStack stack = MemoryStack.stackPush()){
			PointerBuffer event = stack.mallocPointer(1);
			cl.errorCheck(CL10.clEnqueueCopyBufferToImage(q.ptr, src.ptr, ptr, srcOffset,
					region.origin, region.region, waitList, event));
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
	
	public Event enqueueCopyToBuffer(CommandQueue q, ImageRegion region, CLBuffer dest, int destOffset, Event...wait){
		PointerBuffer waitList = cl.convertPointerBuffer(wait);
		Event ev = null;
		try(MemoryStack stack = MemoryStack.stackPush()){
			PointerBuffer event = stack.mallocPointer(1);
			cl.errorCheck(CL10.clEnqueueCopyImageToBuffer(q.ptr, ptr, dest.ptr,
					region.origin, region.region, destOffset, waitList, event));
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
}
