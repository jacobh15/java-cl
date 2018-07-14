package javacl;

import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.PointerBuffer;
import org.lwjgl.opencl.CL;
import org.lwjgl.opencl.CL10;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

public class OpenCLTools {
	private static OpenCLTools tools;
	
	private OpenCLTools(){
		releaseQueue = new ArrayList<>();
		pbholders = new HashMap<>();
	}
	
	public static OpenCLTools getOpenCLTools(){
		if(tools == null)
			tools = new OpenCLTools();
		return tools;
	}
	
	static OpenCLTools cl(){
		return getOpenCLTools();
	}
	
	private List<Platform> platforms;
	private List<Releaseable> releaseQueue;
	private Map<PointerBuffer, PBHolder> pbholders;
	
	public List<Platform> getPlatforms(){
		if(platforms == null){
			try(MemoryStack stack = MemoryStack.stackPush()){
				IntBuffer size = stack.mallocInt(1);
				errorCheck(CL10.clGetPlatformIDs(null, size));
				PointerBuffer platformIDs = stack.mallocPointer(size.get(0));
				errorCheck(CL10.clGetPlatformIDs(platformIDs, (IntBuffer)null));
				platforms = new ArrayList<>(size.get(0));
				for(int i = 0; i < size.get(0); i++){
					platforms.add(new Platform(platformIDs.get(i)));
				}
			}catch(CLException e){
				throw e;
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		if(platforms != null)
			return new ArrayList<>(platforms);
		return null;
	}
	
	void errorCheck(int r){
		if(r != CL10.CL_SUCCESS)
			throw new CLException(r);
	}
	
	public <T extends CLObject> byte[] queryInfo(T obj, int paramName){
		byte[] ret = null;
		try(MemoryStack stack = MemoryStack.stackPush()){
			PointerBuffer size = stack.mallocPointer(1);
			ByteBuffer buffer;
			if(obj instanceof Platform)
				errorCheck(CL10.clGetPlatformInfo(obj.ptr, paramName, (long[])null, size));
			else if(obj instanceof Device)
				errorCheck(CL10.clGetDeviceInfo(obj.ptr, paramName, (long[])null, size));
			else if(obj instanceof Context)
				errorCheck(CL10.clGetContextInfo(obj.ptr, paramName, (ByteBuffer)null, size));
			else if(obj instanceof CommandQueue)
				errorCheck(CL10.clGetCommandQueueInfo(obj.ptr, paramName, (ByteBuffer)null, size));
			else if(obj instanceof ProgramBuild)
				errorCheck(CL10.clGetProgramBuildInfo(obj.ptr, ((ProgramBuild)obj).getDevice().ptr, paramName, (ByteBuffer)null, size));
			else if(obj instanceof Program)
				errorCheck(CL10.clGetProgramInfo(obj.ptr, paramName, (ByteBuffer)null, size));
			else if(obj instanceof Kernel)
				errorCheck(CL10.clGetKernelInfo(obj.ptr, paramName, (ByteBuffer)null, size));
			else if(obj instanceof ProfilingEvent)
				errorCheck(CL10.clGetEventProfilingInfo(obj.ptr, paramName, (ByteBuffer)null, size));
			else if(obj instanceof Event)
				errorCheck(CL10.clGetEventInfo(obj.ptr, paramName, (ByteBuffer)null, size));
			else
				throw new Exception("Invalid CLObject type");
			
			ret = new byte[(int)size.get(0)];
			buffer = stack.malloc((int)size.get(0));
			if(obj instanceof Platform)
				errorCheck(CL10.clGetPlatformInfo(obj.ptr, paramName, buffer, null));
			else if(obj instanceof Device)
				errorCheck(CL10.clGetDeviceInfo(obj.ptr, paramName, buffer, null));
			else if(obj instanceof Context)
				errorCheck(CL10.clGetContextInfo(obj.ptr, paramName, buffer, null));
			else if(obj instanceof CommandQueue)
				errorCheck(CL10.clGetCommandQueueInfo(obj.ptr, paramName, buffer, null));
			else if(obj instanceof ProgramBuild)
				errorCheck(CL10.clGetProgramBuildInfo(obj.ptr, ((ProgramBuild)obj).getDevice().ptr, paramName, buffer, null));
			else if(obj instanceof Program)
				errorCheck(CL10.clGetProgramInfo(obj.ptr, paramName, buffer, null));
			else if(obj instanceof Kernel)
				errorCheck(CL10.clGetKernelInfo(obj.ptr, paramName, buffer, null));
			else if(obj instanceof ProfilingEvent)
				errorCheck(CL10.clGetEventProfilingInfo(obj.ptr, paramName, buffer, null));
			else if(obj instanceof Event)
				errorCheck(CL10.clGetEventInfo(obj.ptr, paramName, buffer, null));
			buffer.get(ret);
		}catch(Exception e){
			e.printStackTrace();
		}
		return ret;
	}
	
	void addToReleaseQueue(Releaseable...rels){
		for(int i = 0; i < rels.length; i++)
			releaseQueue.add(rels[i]);
	}
	
	void removeFromReleaseQueue(Releaseable rel){
		releaseQueue.remove(rel);
	}
	
	public void releaseItemsInReleaseQueue(){
		for(int i = 0; i < releaseQueue.size(); i++){
			releaseQueue.get(i).release();
		}
		releaseQueue.clear();
	}
	
	public void releaseItem(Releaseable rel) {
		rel.release();
		removeFromReleaseQueue(rel);
	}
	
	PointerBuffer convertPointerBuffer(Event[] events){
		PointerBuffer list = null;
		if(events != null && events.length > 0){
			int validEvents = 0;
			for(int i = 0; i < events.length; i++) {
				if(events[i] != null && events[i].ptr != 0)
					validEvents++;
			}
			list = MemoryUtil.memAllocPointer(validEvents);
			for(int i = 0; i < events.length; i++) {
				if(events[i] != null && events[i].ptr != 0)
					list.put(i, events[i].ptr);
			}
			pbholders.put(list, new PBHolder(list));
		}
		return list;
	}
	
	void releasePointerBuffer(PointerBuffer pb) {
		if(pbholders.containsKey(pb)) {
			releaseItem(pbholders.get(pb));
		}
	}
	
	private static class PBHolder implements Releaseable {
		PointerBuffer pb;
		PBHolder(PointerBuffer b){pb = b;}
		public void release(){
			MemoryUtil.memFree(pb);
		}
	}
	
	public <T extends CLObject> String queryInfoString(T obj, int paramName){
		return new String(queryInfo(obj, paramName));
	}
	
	public <T extends CLObject> int queryInfoInt(T obj, int paramName){
		byte[] bytes = queryInfo(obj, paramName);
		return ((bytes[3] & 0xFF) << 24) | ((bytes[2] & 0xFF) << 16) | ((bytes[1] & 0xFF) << 8) | (bytes[0] & 0xFF);
	}
	
	public <T extends CLObject> long queryInfoLong(T obj, int paramName){
		byte[] bytes = queryInfo(obj, paramName);
		return ((long)(bytes[7] & 0xFF) << 56) | ((long)(bytes[6] & 0xFF) << 48) | ((long)(bytes[5] & 0xFF) << 40) |
				((long)(bytes[4] & 0xFF) << 32) | ((long)(bytes[3] & 0xFF) << 24) | ((long)(bytes[2] & 0xFF) << 16) |
				((long)(bytes[1] & 0xFF) << 8) | (long)(bytes[0] & 0xFF);
				
	}
	
	public <T extends CLObject> boolean queryInfoBool(T obj, int paramName){
		return queryInfo(obj, paramName)[0] == 0 ? false: true;
	}
	
	public void printInfo(PrintStream s){
		List<Platform> platforms = getPlatforms();
		s.println("Platforms (" + platforms.size() + "):");
		for(int i = 0; i < platforms.size(); i++){
			s.println("Name: " + platforms.get(i).getName());
			s.println("\tProfile: " + platforms.get(i).getProfile());
			s.println("\tVendor: " + platforms.get(i).getVendorName());
			s.println("\tVersion: " + platforms.get(i).getVersionString());
			s.println();
			List<Device> devices = platforms.get(i).getDevices();
			s.println("\tDevices (" + devices.size() + "):");
			for(int j = 0; j < devices.size(); j++){
				s.println("\t\tName: " + devices.get(j).getName());
				s.println("\t\tProfile: " + devices.get(j).getProfile());
				s.println("\t\tType: " + devices.get(j).getType());
				s.println("\t\tVendor: " + devices.get(j).getVendorName());
				s.println("\t\tDriver version: " + devices.get(j).getDriverVersion());
				s.println("\t\tVersion: " + devices.get(j).getVersion());
				List<String> extensions = devices.get(j).getSupportedExtensions();
				s.println("\t\tExtensions (" + extensions.size() + "):");
				for(int k = 0; k < extensions.size(); k++){
					s.println("\t\t\t" + extensions.get(k));
				}
			}
			s.print("\n\n");
		}
	}
	
	public void printInfo(){
		printInfo(System.out);
	}
	
	public static void releaseAllContexts(){
		tools.releaseItemsInReleaseQueue();
		tools = null;
	}
	
	public static void unloadLibrary(){
		CL.destroy();
	}
	
	public static void closeCL(){
		releaseAllContexts();
		unloadLibrary();
	}
}
