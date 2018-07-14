package javacl;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;

public class NDRange implements Releaseable {
	PointerBuffer globalSizes;
	PointerBuffer localSizes;
	int dimensions;
	
	public NDRange(int globalX, int globalY, int globalZ, int localX, int localY, int localZ, int dims){
		dimensions = dims;
		globalSizes = MemoryUtil.memAllocPointer(3).put(0, globalX).put(1, globalY).put(2, globalZ);
		if(!useNullLocalSize(dims, localX, localY, localZ))
			localSizes = MemoryUtil.memAllocPointer(3).put(0, localX).put(1, localY).put(2, localZ);
		else
			localSizes = null;
		OpenCLTools.cl().addToReleaseQueue(this);
	}
	
	private static boolean useNullLocalSize(int dims, int... locals) {
		for(int i = 0; i < dims; i++) {
			if(locals[i] == -1) {
				return true;
			}
		}
		return false;
	}
	
	public void release(){
		MemoryUtil.memFree(globalSizes);
		MemoryUtil.memFree(localSizes);
	}
	
	public int globalSize(int dim){
		return (int)globalSizes.get(dim);
	}
	
	public int globalX(){
		return (int)globalSizes.get(0);
	}
	
	public int globalY(){
		return (int)globalSizes.get(1);
	}
	
	public int globalZ(){
		return (int)globalSizes.get(2);
	}
	
	public int localSize(int dim){
		return (int)localSizes.get(dim);
	}
	
	public int localX(){
		return (int)localSizes.get(0);
	}
	
	public int localY(){
		return (int)localSizes.get(1);
	}
	
	public int localZ(){
		return (int)localSizes.get(2);
	}
	
	public int dims(){
		return dimensions;
	}
	
	public int numGroups(int dim){
		if(localSizes.get(dim) != 0)
			return globalSize(dim) / localSize(dim);
		else
			return 0;
	}
	
	public int numGroupsX(){
		return globalX() / localX();
	}
	
	public int numGroupsY(){
		if(localSizes.get(1) != 0)
			return globalY() / localY();
		else
			return 0;
	}
	
	public int numGroupsZ(){
		if(localSizes.get(2) != 0)
			return globalZ() / localZ();
		else
			return 0;
	}
}
