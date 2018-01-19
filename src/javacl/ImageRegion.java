package javacl;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;

public class ImageRegion implements Releaseable {
	PointerBuffer origin;
	PointerBuffer region;
	
	public ImageRegion(int x, int y, int z, int w, int h, int d){
		origin = MemoryUtil.memAllocPointer(3).put(0, x).put(1, y).put(2, z);
		region = MemoryUtil.memAllocPointer(3).put(0, w).put(1, h).put(2, d);
		OpenCLTools.cl().addToReleaseQueue(this);
	}
	
	public ImageRegion(int w, int h, int d){
		this(0, 0, 0, w, h, d);
	}
	
	public ImageRegion(int x, int y, int w, int h){
		this(x, y, 0, w, h, 0);
	}
	
	public ImageRegion(int w, int h){
		this(0, 0, 0, w, h, 0);
	}
	
	public void release(){
		MemoryUtil.memFree(origin);
		MemoryUtil.memFree(region);
	}
}
