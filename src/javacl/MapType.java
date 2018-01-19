package javacl;

import org.lwjgl.opencl.CL10;

public enum MapType {
	READ(CL10.CL_MAP_READ), WRITE(CL10.CL_MAP_WRITE), READ_WRITE(0);
	
	int clMapType;
	
	private MapType(int clType){
		clMapType = clType;
	}
}
