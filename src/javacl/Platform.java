package javacl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opencl.CL10;
import org.lwjgl.system.MemoryStack;

public class Platform extends CLObject implements Releaseable {
	private static final int[] INT_STORAGE = new int[1];
	
	private String name;
	private String vendorName;
	private String versionString;
	private String profile;
	private List<String> extensions;
	private Map<DeviceType, List<Device>> devices;
	List<Context> contexts;
	
	public Platform(long p){
		super(p);
		contexts = new ArrayList<>();
	}
	
	public void release(){
		for(int i = 0; i < contexts.size(); i++){
			contexts.get(i).release();
		}
	}
	
	public List<Context> getContexts(){
		return new ArrayList<>(contexts);
	}
	
	public String getName(){
		if(name == null)
			name = cl.queryInfoString(this, CL10.CL_PLATFORM_NAME);
		return name;
	}
	
	public String getVendorName(){
		if(vendorName == null)
				vendorName = cl.queryInfoString(this, CL10.CL_PLATFORM_VENDOR);
		return vendorName;
	}

	public String getVersionString(){
		if(versionString == null)
			versionString = cl.queryInfoString(this, CL10.CL_PLATFORM_VERSION);
		return versionString;
	}
	
	public String getProfile(){
		if(profile == null)
			profile = cl.queryInfoString(this, CL10.CL_PLATFORM_PROFILE);
		return profile;
	}
	
	public List<String> getSupportedExtensions(){
		if(extensions == null){
			byte[] bytes = cl.queryInfo(this, CL10.CL_PLATFORM_EXTENSIONS);
			extensions = Arrays.asList(new String(bytes).toLowerCase().split(" "));
		}
		return new ArrayList<>(extensions);
	}
	
	public boolean supportsExtension(String name){
		if(extensions == null)
			getSupportedExtensions();
		return extensions.contains(name.toLowerCase());
	}
	
	public List<Device> getDevices(DeviceType type){
		if(devices == null){
			List<Device> devs = null;
			try(MemoryStack stack = MemoryStack.stackPush()){
				cl.errorCheck(CL10.clGetDeviceIDs(ptr, DeviceType.ALL.clType, null, INT_STORAGE));
				PointerBuffer deviceIDs = BufferUtils.createPointerBuffer(INT_STORAGE[0]);
				cl.errorCheck(CL10.clGetDeviceIDs(ptr, DeviceType.ALL.clType, deviceIDs, INT_STORAGE));
				devs = new ArrayList<>();
				for(int i = 0; i < INT_STORAGE[0]; i++){
					devs.add(new Device(deviceIDs.get(i), this));
				}
			}catch(CLException e){
				throw e;
			}catch(Exception e){
				e.printStackTrace();
			}
			if(devs == null)
				return null;
			else{
				devices = new HashMap<>();
				for(int i = 0; i < DeviceType.TYPES.length; i++){
					if(DeviceType.TYPES[i] == DeviceType.ALL)
						continue;
					List<Device> subList = new ArrayList<>();
					for(int j = 0; j < devs.size(); j++){
						if(devs.get(j).getType() == DeviceType.TYPES[i])
							subList.add(devs.get(j));
					}
					devices.put(DeviceType.TYPES[i], subList);
				}
				devices.put(DeviceType.ALL, devs);
			}
		}
		return new ArrayList<>(devices.get(type));
	}
	
	public List<Device> getDevices(){
		return getDevices(DeviceType.ALL);
	}
	
	public boolean hasDevice(Device d){
		return devices.get(DeviceType.ALL).contains(d);
	}
	
	public ContextBuilder getContextBuilder(){
		return new ContextBuilder(this);
	}
	
	public String toString(){
		return "Platform " + getName();
	}
}
