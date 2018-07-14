package javacl;

import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.util.List;

import org.lwjgl.opencl.CL10;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

public class MemoryObjectBuilder {
	
	private static final byte TRUE = 1, FALSE = 0;
	
	private Context context;
	private MemoryType memoryType;
	private ImageData imageData;
	private boolean readAndWrite;
	private boolean readOnly;
	private boolean writeOnly;
	private boolean useHostPointer;
	private boolean memAllocHost;
	private boolean memCopyHost;
	private boolean hostPtrNull;
	private boolean requestHostBuffer;
	
	private List<Boolean> booleanDataL;
	private List<Byte> byteDataL;
	private List<Short> shortDataL;
	private List<Integer> intDataL;
	private List<Long> longDataL;
	private List<Float> floatDataL;
	private List<Double> doubleDataL;
	
	private boolean[] booleanData;
	private byte[] byteData;
	private short[] shortData;
	private int[] intData;
	private long[] longData;
	private float[] floatData;
	private double[] doubleData;
	
	private ByteBuffer nativeBuffer;
	
	private int numBytes;
	private int components;
	
	MemoryObjectBuilder(Context ctx){
		context = ctx;
		readAndWrite = true;
		readOnly = false;
		writeOnly = false;
		useHostPointer = false;
		memAllocHost = false;
		memCopyHost = false;
		hostPtrNull = true;
		memoryType = MemoryType.CHARACTER_P;
		requestHostBuffer = false;
	}
	
	public MemoryObject build(){
		MemoryObject mem = null;
		try(MemoryStack stack = MemoryStack.stackPush()){
			IntBuffer error = stack.mallocInt(1);
			
			if(requestHostBuffer){
				nativeBuffer = MemoryUtil.memAlloc(numBytes);
				hostPtrNull = false;
			}
			
			long p = 0;
			int flags = readAndWrite ? CL10.CL_MEM_READ_WRITE: (readOnly ? CL10.CL_MEM_READ_ONLY: CL10.CL_MEM_WRITE_ONLY);
			flags |= useHostPointer ? CL10.CL_MEM_USE_HOST_PTR: 0;
			flags |= (memCopyHost ? CL10.CL_MEM_COPY_HOST_PTR: 0) | (memAllocHost ? CL10.CL_MEM_ALLOC_HOST_PTR : 0);
			if(hostPtrNull){
				if(memCopyHost){
					throw new Exception("Memory object creation failed: attempted to copy host data when host data is null.");
				}
				if(useHostPointer){
					throw new Exception("Memory object creation failed: attempted to use host data when host data is null.");
				}
				if(memoryType.isPointer())
					p = CL10.clCreateBuffer(context.ptr, flags, numBytes, error);
				else if(memoryType == MemoryType.IMAGE2D)
					p = CL10.clCreateImage2D(context.ptr, flags,
							imageData.format, imageData.width, imageData.height, 0, (ByteBuffer)null, error);
				else if(memoryType == MemoryType.IMAGE3D)
					p = CL10.clCreateImage3D(context.ptr, flags,
							imageData.format, imageData.width, imageData.height, imageData.depth, 0, 0, (ByteBuffer)null, error);
				OpenCLTools.cl().errorCheck(error.get(0));
			}else{
				if(nativeBuffer == null){
					fillNativeBuffer();
				}
				p = getPointer(flags, error);
				OpenCLTools.cl().errorCheck(error.get(0));
			}
			mem = getObject(p);
			if(mem != null){
				context.memObjects.add(mem);
			}
		}catch(CLException e){
			throw e;
		}catch(Exception e){
			e.printStackTrace();
		}
		clear();
		return mem;
	}
	
	private MemoryObject getObject(long p){
		boolean read = readOnly || readAndWrite;
		boolean write = writeOnly || readAndWrite;
		if(memoryType.isPointer()){
			return new CLBuffer(p, context, memoryType, nativeBuffer, numBytes, read, write, useHostPointer, memAllocHost, memCopyHost);
		}
		if(memoryType.isImage()){
			return new CLImage(p, context, memoryType, nativeBuffer, imageData, read, write, useHostPointer, memAllocHost, memCopyHost);
		}
		if(memoryType.isBool()){
			return new Scalar.Boolean(p, context, memoryType, nativeBuffer, read, write, useHostPointer, memAllocHost, memCopyHost);
		}else if(memoryType.isByte()){
			if(memoryType.isScalar())
				return new Scalar.Byte(p, context, memoryType, nativeBuffer, read, write, useHostPointer, memAllocHost, memCopyHost);
			else
				return new Vector.Byte(p, context, memoryType, nativeBuffer, read, write, useHostPointer, memAllocHost, memCopyHost);
		}else if(memoryType.isShort()){
			if(memoryType.isScalar())
				return new Scalar.Short(p, context, memoryType, nativeBuffer, read, write, useHostPointer, memAllocHost, memCopyHost);
			else
				return new Vector.Short(p, context, memoryType, nativeBuffer, read, write, useHostPointer, memAllocHost, memCopyHost);
		}else if(memoryType.isInt()){
			if(memoryType.isScalar())
				return new Scalar.Int(p, context, memoryType, nativeBuffer, read, write, useHostPointer, memAllocHost, memCopyHost);
			else
				return new Vector.Int(p, context, memoryType, nativeBuffer, read, write, useHostPointer, memAllocHost, memCopyHost);
		}else if(memoryType.isFloat()){
			if(memoryType.isFloat())
				return new Scalar.Float(p, context, memoryType, nativeBuffer, read, write, useHostPointer, memAllocHost, memCopyHost);
			else
				return new Vector.Float(p, context, memoryType, nativeBuffer, read, write, useHostPointer, memAllocHost, memCopyHost);
		}else if(memoryType.isDouble()){
			if(memoryType.isDouble())
				return new Scalar.Double(p, context, memoryType, nativeBuffer, read, write, useHostPointer, memAllocHost, memCopyHost);
			else
				return new Vector.Double(p, context, memoryType, nativeBuffer, read, write, useHostPointer, memAllocHost, memCopyHost);
		}
		return null;
	}
	
	private long getPointer(int flags, IntBuffer error){
		if(memoryType.isPointer() || memoryType.isImage()){
			if(memoryType.isPointer())
				return CL10.clCreateBuffer(context.ptr, flags, nativeBuffer, error);
			else if(memoryType == MemoryType.IMAGE2D)
				return CL10.clCreateImage2D(context.ptr, flags, imageData.format, imageData.width, imageData.height,
						imageData.rowPitch, nativeBuffer, error);
			else if(memoryType == MemoryType.IMAGE3D)
				return CL10.clCreateImage3D(context.ptr, flags, imageData.format, imageData.width, imageData.height,
						imageData.depth, imageData.rowPitch, imageData.slicePitch, nativeBuffer, error);
		}
		return 0;
	}
	
	private void fillNativeBuffer() throws InvalidDataTypeException {
		nativeBuffer = MemoryUtil.memAlloc(numBytes);
		if(booleanData != null){
			for(int i = 0; i < numBytes; i++)
				nativeBuffer.put(i, booleanData[i] ? TRUE: FALSE);
		}else if(byteData != null){
			nativeBuffer.put(byteData);
		}else if(shortData != null){
			nativeBuffer.asShortBuffer().put(shortData);
		}else if(intData != null){
			nativeBuffer.asIntBuffer().put(intData);
		}else if(longData != null){
			nativeBuffer.asLongBuffer().put(longData);
		}else if(floatData != null){
			nativeBuffer.asFloatBuffer().put(floatData);
		}else if(doubleData != null){
			nativeBuffer.asDoubleBuffer().put(doubleData);
		}else if(booleanDataL != null){
			for(int i = 0; i < booleanDataL.size(); i++)
				nativeBuffer.put(i, booleanDataL.get(i) ? TRUE: FALSE);
		}else if(byteDataL != null){
			for(int i = 0; i < byteDataL.size(); i++)
				nativeBuffer.put(i, byteDataL.get(i));
		}else if(shortDataL != null){
			ShortBuffer buff = nativeBuffer.asShortBuffer();
			for(int i = 0; i < shortDataL.size(); i++)
				buff.put(i, shortDataL.get(i));
		}else if(intDataL != null){
			IntBuffer buff = nativeBuffer.asIntBuffer();
			for(int i = 0; i < intDataL.size(); i++)
				buff.put(i, intDataL.get(i));
		}else if(longDataL != null){
			LongBuffer buff = nativeBuffer.asLongBuffer();
			for(int i = 0; i < longDataL.size(); i++)
				buff.put(i, longDataL.get(i));
		}else if(floatDataL != null){
			FloatBuffer buff = nativeBuffer.asFloatBuffer();
			for(int i = 0; i < floatDataL.size(); i++)
				buff.put(i, floatDataL.get(i));
		}else if(doubleDataL != null){
			DoubleBuffer buff = nativeBuffer.asDoubleBuffer();
			for(int i = 0; i < doubleDataL.size(); i++)
				buff.put(i, doubleDataL.get(i));
		}
	}
	
	public MemoryObjectBuilder context(Context ctx){
		context = ctx;
		return this;
	}
	
	public MemoryObjectBuilder imageData(ImageData data){
		imageData = data;
		components = data.bpp * data.width * data.height;
		if(data.numDimensions == 3)
			components *= data.depth;
		memoryType(data.type);
		requestHostBuffer = hostPtrNull;
		return this;
	}
	
	public MemoryObjectBuilder memoryType(MemoryType type){
		memoryType = type;
		numBytes = type.bytesPerComp() * components;
		return this;
	}
	
	public MemoryObjectBuilder components(int c){
		components = c;
		numBytes = memoryType.bytesPerComp() * components;
		requestHostBuffer = hostPtrNull;
		return this;
	}
	
	public MemoryObjectBuilder elements(int e){
		components(e * memoryType.numComps());
		return this;
	}
	
	public MemoryObjectBuilder nativeBuffer(ByteBuffer buffer){
		nullAll();
		nativeBuffer = buffer;
		hostPtrNull = false;
		numBytes = buffer.capacity();
		components = numBytes / memoryType.bytesPerComp();
		return this;
	}
	
	public MemoryObjectBuilder booleanData(List<Boolean> data){
		nullAll();
		booleanDataL = data;
		hostPtrNull = false;
		components = data.size();
		numBytes = components * memoryType.bytesPerComp();
		return this;
	}
	
	public MemoryObjectBuilder booleanData(boolean...data){
		nullAll();
		booleanData = data;
		hostPtrNull = false;
		components = data.length;
		numBytes = components * memoryType.bytesPerComp();
		return this;
	}
	
	public MemoryObjectBuilder byteData(List<Byte> data){
		nullAll();
		byteDataL = data;
		hostPtrNull = false;
		components = data.size();
		numBytes = components * memoryType.bytesPerComp();
		return this;
	}
	
	public MemoryObjectBuilder byteData(byte...data){
		nullAll();
		byteData = data;
		hostPtrNull = false;
		components = data.length;
		numBytes = components * memoryType.bytesPerComp();
		return this;
	}
	
	public MemoryObjectBuilder shortData(List<Short> data){
		nullAll();
		shortDataL = data;
		hostPtrNull = false;
		components = data.size();
		numBytes = components * memoryType.bytesPerComp();
		return this;
	}
	
	public MemoryObjectBuilder shortData(short...data){
		nullAll();
		shortData = data;
		hostPtrNull = false;
		components = data.length;
		numBytes = components * memoryType.bytesPerComp();
		return this;
	}
	
	public MemoryObjectBuilder intData(List<Integer> data){
		nullAll();
		intDataL = data;
		hostPtrNull = false;
		components = data.size();
		numBytes = components * memoryType.bytesPerComp();
		return this;
	}
	
	public MemoryObjectBuilder intData(int...data){
		nullAll();
		intData = data;
		hostPtrNull = false;
		components = data.length;
		numBytes = components * memoryType.bytesPerComp();
		return this;
	}
	
	public MemoryObjectBuilder longData(List<Long> data){
		nullAll();
		longDataL = data;
		hostPtrNull = false;
		components = data.size();
		numBytes = components * memoryType.bytesPerComp();
		return this;
	}
	
	public MemoryObjectBuilder longData(long...data){
		nullAll();
		longData = data;
		hostPtrNull = false;
		components = data.length;
		numBytes = components * memoryType.bytesPerComp();
		return this;
	}
	
	public MemoryObjectBuilder floatData(List<Float> data){
		nullAll();
		floatDataL = data;
		hostPtrNull = false;
		components = data.size();
		numBytes = components * memoryType.bytesPerComp();
		return this;
	}
	
	public MemoryObjectBuilder floatData(float...data){
		nullAll();
		floatData = data;
		hostPtrNull = false;
		components = data.length;
		numBytes = components * memoryType.bytesPerComp();
		return this;
	}
	
	public MemoryObjectBuilder doubleData(List<Double> data){
		nullAll();
		doubleDataL = data;
		hostPtrNull = false;
		components = data.size();
		numBytes = components * memoryType.bytesPerComp();
		return this;
	}
	
	public MemoryObjectBuilder doubleData(double...data){
		nullAll();
		doubleData = data;
		hostPtrNull = false;
		components = data.length;
		numBytes = components * memoryType.bytesPerComp();
		return this;
	}
	
	public MemoryObjectBuilder sizeInBytes(int bytes){
		nullAll();
		numBytes = bytes;
		return this;
	}
	
	public MemoryObjectBuilder readAndWrite(){
		readAndWrite = true;
		readOnly = false;
		writeOnly = false;
		return this;
	}
	
	public MemoryObjectBuilder readOnly(){
		readOnly = true;
		writeOnly = false;
		readAndWrite = false;
		return this;
	}
	
	public MemoryObjectBuilder writeOnly(){
		writeOnly = true;
		readOnly = false;
		readAndWrite = false;
		return this;
	}
	
	public MemoryObjectBuilder useHostPointer(){
		useHostPointer = true;
		memCopyHost = false;
		memAllocHost = false;
		return this;
	}
	
	public MemoryObjectBuilder dontUseHostPointer(){
		useHostPointer = false;
		return this;
	}
	
	public MemoryObjectBuilder memAllocHost(){
		memAllocHost = true;
		useHostPointer = false;
		return this;
	}
	
	public MemoryObjectBuilder memAllocDevice(){
		memAllocHost = false;
		return this;
	}
	
	public MemoryObjectBuilder memCopyHost(){
		memCopyHost = true;
		useHostPointer = false;
		return this;
	}
	
	public MemoryObjectBuilder memDontCopyHost(){
		memCopyHost = false;
		return this;
	}
	
	private void nullAll(){
		booleanDataL = null;
		byteDataL = null;
		shortDataL = null;
		intDataL = null;
		longDataL = null;
		floatDataL = null;
		doubleDataL = null;
		booleanData = null;
		byteData = null;
		shortData = null;
		intData = null;
		longData = null;
		floatData = null;
		doubleData = null;
		nativeBuffer = null;
		hostPtrNull = true;
		requestHostBuffer = false;
	}
	
	public void clear(){
		nullAll();
		readAndWrite = true;
		readOnly = false;
		writeOnly = false;
		useHostPointer = false;
		memAllocHost = false;
		memCopyHost = false;
		numBytes = 0;
		memoryType = MemoryType.CHARACTER_P;
	}
	
	public static class InvalidDataTypeException extends Exception {
		public InvalidDataTypeException(String msg){
			super(msg);
		}
	}
}
