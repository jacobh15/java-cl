package javacl;

import java.nio.ByteBuffer;

import org.lwjgl.opencl.CLImageFormat;
import org.lwjgl.system.MemoryUtil;

public class ImageData implements Releaseable {
	int numDimensions;
	int width, height, depth;
	int rowPitch, slicePitch;
	int bpp;
	MemoryType type;
	CLImageFormat format;
	private ByteBuffer formatData;
	
	public ImageData(int w, int h, int d, int channels, int channelOrder, int channelType){
		bpp = channels;
		width = w;
		height = h;
		depth = d;
		rowPitch = width * bpp;
		slicePitch = width * height * bpp;
		type = MemoryType.IMAGE3D;
		formatData = MemoryUtil.memAlloc(8);
		formatData.asIntBuffer().put(0, channelOrder).put(1, channelType);
		format = new CLImageFormat(formatData);
		OpenCLTools.cl().addToReleaseQueue(this);
	}
	
	public ImageData(int w, int h, int channels, int channelOrder, int channelType){
		bpp = channels;
		width = w;
		height = h;
		depth = 0;
		rowPitch = width * bpp;
		slicePitch = 0;
		type = MemoryType.IMAGE2D;
		formatData = MemoryUtil.memAlloc(8);
		formatData.asIntBuffer().put(0, channelOrder).put(1, channelType);
		format = new CLImageFormat(formatData);
		OpenCLTools.cl().addToReleaseQueue(this);
	}
	
	public void release(){
		format = null;
		MemoryUtil.memFree(formatData);
	}
}
