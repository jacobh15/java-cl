package javacl;

import java.util.HashMap;
import java.util.Map;

import org.lwjgl.opencl.CL10;

public enum CommandType {
	
	
	ND_RANGE_KERNEL(CL10.CL_COMMAND_NDRANGE_KERNEL), TASK(CL10.CL_COMMAND_TASK), NATIVE_KERNEL(CL10.CL_COMMAND_NATIVE_KERNEL),
	READ_BUFFER(CL10.CL_COMMAND_READ_BUFFER), WRITE_BUFFER(CL10.CL_COMMAND_WRITE_BUFFER), COPY_BUFFER(CL10.CL_COMMAND_COPY_BUFFER),
	READ_IMAGE(CL10.CL_COMMAND_READ_IMAGE), WRITE_IMAGE(CL10.CL_COMMAND_WRITE_IMAGE), COPY_IMAGE(CL10.CL_COMMAND_COPY_IMAGE),
	COPY_BUFFER_TO_IMAGE(CL10.CL_COMMAND_COPY_BUFFER_TO_IMAGE), COPY_IMAGE_TO_BUFFER(CL10.CL_COMMAND_COPY_IMAGE_TO_BUFFER),
	MAP_BUFFER(CL10.CL_COMMAND_MAP_BUFFER), MAP_IMAGE(CL10.CL_COMMAND_MAP_IMAGE),
	UNMAP_MEM_OBJECT(CL10.CL_COMMAND_UNMAP_MEM_OBJECT), MARKER(CL10.CL_COMMAND_MARKER),
	AQUIRE_GL_OBJECTS(CL10.CL_COMMAND_ACQUIRE_GL_OBJECTS), RELEASE_GL_OBJECTS(CL10.CL_COMMAND_RELEASE_GL_OBJECTS);
	
	private static final Map<Integer, CommandType> TYPE_MAP = new HashMap<>();
	private static boolean init = false;
	
	private int clID;
	
	private CommandType(int clId){
		clID = clId;
	}
	
	static CommandType getCommand(int clID){
		if(!init){
			CommandType[] values = CommandType.values();
			for(int i = 0; i < values.length; i++){
				TYPE_MAP.put(values[i].clID, values[i]);
			}
		}
		return TYPE_MAP.get(clID);
	}
}
