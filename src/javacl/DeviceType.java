package javacl;

import java.util.HashMap;
import java.util.Map;

import org.lwjgl.opencl.CL10;
import org.lwjgl.opencl.CL12;

public enum DeviceType {
	ACCELERATOR(CL10.CL_DEVICE_TYPE_ACCELERATOR),
	CPU(CL10.CL_DEVICE_TYPE_CPU),
	CUSTOM(CL12.CL_DEVICE_TYPE_CUSTOM),
	GPU(CL10.CL_DEVICE_TYPE_GPU),
	ALL(CL10.CL_DEVICE_TYPE_ALL);
	
	static final DeviceType[] TYPES = DeviceType.values();
	private static final Map<Integer, DeviceType> TYPE_MAP = new HashMap<>();
	private static boolean init = false;
	
	int clType;
	
	private DeviceType(int type){
		clType = type;
	}
	
	static DeviceType getType(int type){
		if(!init){
			for(int i = 0; i < TYPES.length; i++){
				TYPE_MAP.put(TYPES[i].clType, TYPES[i]);
			}
			init = true;
		}
		return TYPE_MAP.get(type);
	}
}
