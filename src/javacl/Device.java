package javacl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.lwjgl.opencl.CL10;

public class Device extends CLObject {
	
	private String name;
	private String profile;
	private String vendorName;
	private String version;
	private String driverVersion;
	Platform platform;
	private DeviceType type;
	private List<String> extensions;
	
	Device(long p, Platform plat) {
		super(p);
		platform = plat;
	}
	
	public String getName(){
		if(name == null)
			name = cl.queryInfoString(this, CL10.CL_DEVICE_NAME);
		return name;
	}
	
	public boolean isAvailable(){
		return cl.queryInfoBool(this, CL10.CL_DEVICE_AVAILABLE);
	}
	
	public List<String> getSupportedExtensions(){
		if(extensions == null){
			byte[] bytes = cl.queryInfo(this, CL10.CL_DEVICE_EXTENSIONS);
			extensions = Arrays.asList(new String(bytes).toLowerCase().split(" "));
		}
		return new ArrayList<>(extensions);
	}
	
	public boolean supportsExtension(String name){
		if(extensions == null)
			getSupportedExtensions();
		return extensions.contains(name.toLowerCase());
	}
	
	public String getProfile(){
		if(profile == null)
			profile = cl.queryInfoString(this, CL10.CL_DEVICE_PROFILE);
		return profile;
	}
	
	public DeviceType getType(){
		if(type == null)
			type = DeviceType.getType(cl.queryInfoInt(this, CL10.CL_DEVICE_TYPE));
		return type;
	}
	
	public String getVendorName(){
		if(vendorName == null)
			vendorName = cl.queryInfoString(this, CL10.CL_DEVICE_VENDOR);
		return vendorName;
	}
	
	public String getVersion(){
		if(version == null)
			version = cl.queryInfoString(this, CL10.CL_DEVICE_VERSION);
		return version;
	}
	
	public String getDriverVersion(){
		if(driverVersion == null)
			driverVersion = cl.queryInfoString(this, CL10.CL_DRIVER_VERSION);
		return driverVersion;
	}
	
	public Platform getPlatform(){
		return platform;
	}
	
	public String toString(){
		return getName();
	}
}
