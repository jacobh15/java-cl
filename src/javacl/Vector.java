package javacl;

import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;

import org.lwjgl.opencl.CL10;

public abstract class Vector extends MemoryObject {
	
	int components;
	int bytes;
	
	Vector(long p, Context ctx, MemoryType type, ByteBuffer buffer, boolean read, boolean write, boolean useHostPtr,
			boolean useHostMem, boolean copiedHost) {
		super(p, ctx, type, buffer, read, write, useHostPtr, useHostMem, copiedHost);
		components = type.numComps();
		bytes = components * type.bytesPerComp();
	}
	
	public int numComps(){
		return components;
	}
	
	public int sizeInBytes(){
		return bytes;
	}
	
	protected void printErr(){
		System.err.println("Only 1-, 2-, 3-, and 4-component vector sets are supported. Attempted: " + components + ". Use"
				+ "a pointer instead.");
	}
	
	public static class Byte extends Vector {
		
		Byte(long p, Context ctx, MemoryType type, ByteBuffer buffer, boolean read, boolean write, boolean useHostPtr,
				boolean useHostMem, boolean copiedHost) {
			super(p, ctx, type, buffer, read, write, useHostPtr, useHostMem, copiedHost);
		}
		
		public byte get(int comp){
			return hostMemory.get(comp);
		}
		
		public void setArg(long kernel, int arg) {
			switch(components){
			case 1:
				cl.errorCheck(CL10.clSetKernelArg1b(kernel, arg, get(0))); break;
			case 2:
				cl.errorCheck(CL10.clSetKernelArg2b(kernel, arg, get(0), get(1))); break;
			case 3:
				cl.errorCheck(CL10.clSetKernelArg3b(kernel, arg, get(0), get(1), get(2))); break;
			case 4:
				cl.errorCheck(CL10.clSetKernelArg4b(kernel, arg, get(0), get(1), get(2), get(3))); break;
			default:
				printErr();
			}
		}
	}
	
	public static class Short extends Vector {
		ShortBuffer shortview;
		
		Short(long p, Context ctx,  MemoryType type, ByteBuffer buffer, boolean read, boolean write, boolean useHostPtr,
				boolean useHostMem, boolean copiedHost) {
			super(p, ctx, type, buffer, read, write, useHostPtr, useHostMem, copiedHost);
			shortview = buffer.asShortBuffer();
		}
		
		public short get(int comp){
			return shortview.get(comp);
		}
		
		public void setArg(long kernel, int arg) {
			switch(components){
			case 1:
				cl.errorCheck(CL10.clSetKernelArg1s(kernel, arg, get(0))); break;
			case 2:
				cl.errorCheck(CL10.clSetKernelArg2s(kernel, arg, get(0), get(1))); break;
			case 3:
				cl.errorCheck(CL10.clSetKernelArg3s(kernel, arg, get(0), get(1), get(2))); break;
			case 4:
				cl.errorCheck(CL10.clSetKernelArg4s(kernel, arg, get(0), get(1), get(2), get(3))); break;
			default:
				printErr();
			}
		}
	}
	
	public static class Int extends Vector {
		IntBuffer intview;
		
		Int(long p, Context ctx,  MemoryType type, ByteBuffer buffer, boolean read, boolean write, boolean useHostPtr,
				boolean useHostMem, boolean copiedHost) {
			super(p, ctx, type, buffer, read, write, useHostPtr, useHostMem, copiedHost);
			intview = buffer.asIntBuffer();
		}
		
		public int get(int comp){
			return intview.get(comp);
		}
		
		public void setArg(long kernel, int arg) {
			switch(components){
			case 1:
				cl.errorCheck(CL10.clSetKernelArg1i(kernel, arg, get(0))); break;
			case 2:
				cl.errorCheck(CL10.clSetKernelArg2i(kernel, arg, get(0), get(1))); break;
			case 3:
				cl.errorCheck(CL10.clSetKernelArg3i(kernel, arg, get(0), get(1), get(2))); break;
			case 4:
				cl.errorCheck(CL10.clSetKernelArg4i(kernel, arg, get(0), get(1), get(2), get(3))); break;
			default:
				printErr();
			}
		}
	}
	
	public static class Long extends Vector {
		LongBuffer longview;
		
		Long(long p, Context ctx,  MemoryType type, ByteBuffer buffer, boolean read, boolean write, boolean useHostPtr,
				boolean useHostMem, boolean copiedHost) {
			super(p, ctx, type, buffer, read, write, useHostPtr, useHostMem, copiedHost);
			longview = buffer.asLongBuffer();
		}
		
		public long get(int comp){
			return longview.get(comp);
		}
		
		public void setArg(long kernel, int arg) {
			switch(components){
			case 1:
				cl.errorCheck(CL10.clSetKernelArg1l(kernel, arg, get(0))); break;
			case 2:
				cl.errorCheck(CL10.clSetKernelArg2l(kernel, arg, get(0), get(1))); break;
			case 3:
				cl.errorCheck(CL10.clSetKernelArg3l(kernel, arg, get(0), get(1), get(2))); break;
			case 4:
				cl.errorCheck(CL10.clSetKernelArg4l(kernel, arg, get(0), get(1), get(2), get(3))); break;
			default:
				printErr();
			}
		}
	}
	
	public static class Float extends Vector {
		FloatBuffer floatview;
		
		Float(long p, Context ctx,  MemoryType type, ByteBuffer buffer, boolean read, boolean write, boolean useHostPtr,
				boolean useHostMem, boolean copiedHost) {
			super(p, ctx, type, buffer, read, write, useHostPtr, useHostMem, copiedHost);
			floatview = buffer.asFloatBuffer();
		}
		
		public float get(int comp){
			return floatview.get(comp);
		}
		
		public void setArg(long kernel, int arg) {
			switch(components){
			case 1:
				cl.errorCheck(CL10.clSetKernelArg1f(kernel, arg, get(0))); break;
			case 2:
				cl.errorCheck(CL10.clSetKernelArg2f(kernel, arg, get(0), get(1))); break;
			case 3:
				cl.errorCheck(CL10.clSetKernelArg3f(kernel, arg, get(0), get(1), get(2))); break;
			case 4:
				cl.errorCheck(CL10.clSetKernelArg4f(kernel, arg, get(0), get(1), get(2), get(3))); break;
			default:
				printErr();
			}
		}
	}
	
	public static class Double extends Vector {
		DoubleBuffer doubleview;
		
		Double(long p, Context ctx,  MemoryType type, ByteBuffer buffer, boolean read, boolean write, boolean useHostPtr,
				boolean useHostMem, boolean copiedHost) {
			super(p, ctx, type, buffer, read, write, useHostPtr, useHostMem, copiedHost);
			doubleview = buffer.asDoubleBuffer();
		}
		
		public double get(int comp){
			return doubleview.get(comp);
		}
		
		public void setArg(long kernel, int arg) {
			switch(components){
			case 1:
				cl.errorCheck(CL10.clSetKernelArg1d(kernel, arg, get(0))); break;
			case 2:
				cl.errorCheck(CL10.clSetKernelArg2d(kernel, arg, get(0), get(1))); break;
			case 3:
				cl.errorCheck(CL10.clSetKernelArg3d(kernel, arg, get(0), get(1), get(2))); break;
			case 4:
				cl.errorCheck(CL10.clSetKernelArg4d(kernel, arg, get(0), get(1), get(2), get(3))); break;
			default:
				printErr();
			}
		}
	}
}
