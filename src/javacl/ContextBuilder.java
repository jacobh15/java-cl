package javacl;

import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.PointerBuffer;
import org.lwjgl.opencl.CL10;
import org.lwjgl.opencl.CLContextCallbackI;
import org.lwjgl.system.MemoryStack;

public class ContextBuilder {
	private Platform platform;
	private List<Device> devices;
	private Map<Long, Long> properties;
	private CLContextCallbackI callback;
	private long callbackData;
	private DeviceType targetDeviceType = DeviceType.ALL;
	private boolean platformSet = false;
	
	ContextBuilder(Platform p){
		platform = p;
		properties = new HashMap<>();
		devices = p.getDevices();
	}
	
	public Context build(){
		Context context = null;
		try(MemoryStack stack = MemoryStack.stackPush()){
			PointerBuffer ctxProps = stack.mallocPointer(3 + 2 * properties.size());
			int i = 2;
			ctxProps.put(0, CL10.CL_CONTEXT_PLATFORM);
			ctxProps.put(1, platform.ptr);
			for(long key: properties.keySet()){
				ctxProps.put(i, key);
				ctxProps.put(i + 1, properties.get(key));
				i += 2;
			}
			ctxProps.put(i, 0);
			
			if(devices.size() == 0){
				devices = platform.getDevices(targetDeviceType);
			}
			PointerBuffer deviceBuffer = stack.mallocPointer(devices.size());
			for(i = 0; i < devices.size(); i++){
				deviceBuffer.put(i, devices.get(i).ptr);
			}
			
			if(devices.size() == 0){
				throw new Exception("No devices found; context creation failed.");
			}
			
			IntBuffer err = stack.mallocInt(1);
			long ptr = CL10.clCreateContext(ctxProps, deviceBuffer, callback, callbackData, err);
			OpenCLTools.cl().errorCheck(err.get(0));
			context = new Context(ptr, platform, devices);
		}catch(CLException e){
			throw e;
		}catch(Exception e){
			System.err.println(e.getMessage());
			e.printStackTrace();
		}
		clear();
		platform.contexts.add(context);
		return context;
	}
	
	public ContextBuilder platform(Platform p){
		if(platform != p || !platformSet){
			platform = p;
			devices.clear();
			platformSet = true;
		}
		return this;
	}
	
	public ContextBuilder devices(List<Device> devs){
		devices.clear();
		devices(devs);
		return this;
	}
	
	public ContextBuilder devices(Device...devs){
		devices.clear();
		devices(devs);
		return this;
	}
	
	public ContextBuilder addDevices(List<Device> devs){
		for(int i = 0; i < devs.size(); i++){
			device(devs.get(i));
		}
		return this;
	}
	
	public ContextBuilder addDevices(Device...devs){
		for(int i = 0; i < devs.length; i++){
			device(devs[i]);
		}
		return this;
	}
	
	public ContextBuilder device(Device dev){
		if(!platformSet){
			devices.clear();
			platform = dev.platform;
			platformSet = true;
		}
			
		if(!devices.contains(dev) && platform.hasDevice(dev));
			devices.add(dev);
		return this;
	}
	
	public ContextBuilder property(long propertyId, long propertyValue){
		properties.put(propertyId, propertyValue);
		return this;
	}
	
	public ContextBuilder callbackData(long data){
		callbackData = data;
		return this;
	}
	
	public ContextBuilder deviceType(DeviceType t){
		targetDeviceType = t;
		devices.clear();
		platformSet = true;
		return this;
	}
	
	public ContextBuilder clear(){
		properties.clear();
		devices.clear();
		callback = null;
		targetDeviceType = DeviceType.ALL;
		callbackData = 0;
		platform = OpenCLTools.getOpenCLTools().getPlatforms().get(0);
		return this;
	}
}
