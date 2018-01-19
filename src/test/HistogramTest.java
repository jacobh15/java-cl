package test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.lwjgl.opencl.CL10;
import org.lwjgl.stb.STBImage;

import javacl.CLBuffer;
import javacl.CLException;
import javacl.CLImage;
import javacl.CommandQueue;
import javacl.Context;
import javacl.Device;
import javacl.Event;
import javacl.ImageData;
import javacl.Kernel;
import javacl.MemoryType;
import javacl.NDRange;
import javacl.NDRangeBuilder;
import javacl.OpenCLTools;
import javacl.Platform;
import javacl.Program;
import javacl.ProgramBuilder;
/**
 * The image histogram calculator use case from <i>The OpenCL Programming Guide</i>, in Java
 * @author hauckjp
 *
 */
public class HistogramTest {
	
	private static final int PLAT = 0, DEV = 0;			// indices of platform and device to use. cl.printInfo() to see platforms/devices
	private static final int PIXELS_PER_WORK_ITEM = 4;	// Pixels to be processed by each work-item. Use 4-12 for best results.
	private static final String INPUT_FILE = ""; 		// Location of image
	private static final String OUTPUT_FILE = ""; 		// Text file to write histogram to
	
	public static void main(String[] args){
		OpenCLTools cl = OpenCLTools.getOpenCLTools(); 	// initialize library
		Platform p = cl.getPlatforms().get(PLAT);		// set the platform
		Device d = p.getDevices().get(DEV);				// set the devices
		Context c = p.getContextBuilder().device(d).build();	// build a context with the device on the platform
		
		int[] width = new int[1], height = new int[1], channels = new int[1];
		ByteBuffer data = STBImage.stbi_load(INPUT_FILE, width, height, channels, 4); // load image into ByteBuffer (RGBA)
		
		CLImage clDataIn =  (CLImage)c.getMemoryObjectBuilder()
				.nativeBuffer(data)	//use the buffer from STBI as the host side memory
				.imageData(new ImageData(width[0], height[0], channels[0], CL10.CL_RGBA, CL10.CL_UNORM_INT8)) //RGBA, UNORM_INT8 (unsigned byte)
				.memCopyHost()//copy data to device
				.build();
		
		CLBuffer histogram = (CLBuffer)c.getMemoryObjectBuilder()
				.components(256 * 3)// make an unsigned int[] with one component for each histogram entry
				.memoryType(MemoryType.UNSIGNED_INTEGER_P)// set the memory type to uint*
				.build();//note that host memory will be allocated to match the device memory allocations. This memory is to be
							//used for reads, writes and maps when communicating with OpenCL device

		ProgramBuilder progb = c.getProgramBuilder(fileToString("histogram.cl"));// build the OpenCL program
		Program prog = null;
		try{
			prog = progb.build();
		}catch(CLException e){ // if an OpenCL exception occurs print the build log, close the library and exit
			System.out.println(progb.lastBuildLog().getLog(d));
			OpenCLTools.closeCL();
			System.exit(1);
		}
		Kernel calcPartials = prog.getKernel("calc_partial_img_histograms"); // get the kernel objects
		Kernel sumPartials = prog.getKernel("sum_partial_img_histograms");
		
		int workGroupSize = calcPartials.getMaxWorkGroupSize(d); // get the max work-group size for the first kernel
		int imageSubWidth = (int)Math.ceil((float)width[0]/(float)PIXELS_PER_WORK_ITEM); // calculate the number of work-items per row of pixels
		NDRangeBuilder rangeBuilder = new NDRangeBuilder(2); // get a range builder to make the kernel range objects
		int groupSizeX = 0, groupSizeY = 0;
		if(workGroupSize <= 256){ // select the local work-group dimensions based on the devices capabilities
			groupSizeX = 16;
			groupSizeY = workGroupSize / 16;
		}else if(workGroupSize <= 1024){
			groupSizeX = workGroupSize / 16;
			groupSizeY = 16;
		}else{
			groupSizeX = workGroupSize / 32;
			groupSizeY = 32;
		}
		
		rangeBuilder.localX(groupSizeX).localY(groupSizeY);// set the local work-group dimensions
		
		int sizeX = (int)Math.ceil((float)imageSubWidth/(float)groupSizeX);// calc the number of work-groups needed in each direction
		int sizeY = (int)Math.ceil((float)height[0]/(float)groupSizeY);
		
		int numGroups = sizeX * sizeY; //calc the total number of work-groups needed
		
		rangeBuilder.globalX(sizeX * groupSizeX).globalY(sizeY * groupSizeY);// set the global work-items in each direction
		
		NDRange calcRange = rangeBuilder.build();//build the range object for the first kernel
		
		workGroupSize = sumPartials.getMaxWorkGroupSize(d);// get the max work-group size for the second kernel
		if(workGroupSize < 256){//require a minimum size of 256. close the library and exit if device doesn't support
			System.out.println("A minimum of 256 work-items per work-group is required for this application.");
			OpenCLTools.closeCL();
			System.exit(1);
		}
		
		rangeBuilder.dimensions(1).globalX(256 * 3).localX(256);// set up the range builder for the second kernel
		
		NDRange sumRange = rangeBuilder.build(); // build the second kernel's range object
		
		CLBuffer partialHistograms = (CLBuffer)c.getMemoryObjectBuilder().
				components(256 * 3 * numGroups).memoryType(MemoryType.UNSIGNED_INTEGER_P).build();// allocate enough device space for
																									// the partial histograms
		
		CommandQueue q = c.getCommandQueueBuilder() // create the command queue on the device with profiling enabled
				.device(d)
				.profiling(true)
				.build();
		
		calcPartials.setArg("img", clDataIn)// set the first kernel's arguments
			.setArg("num_pixels_per_work_item", PIXELS_PER_WORK_ITEM).setArg("histogram", partialHistograms);
		Event calcEvent = q.enqueue(calcPartials, calcRange);//enqueue first kernel, record event, and wait for completion
		q.waitForCompletion();
		
		sumPartials.setArg("partialHistograms", partialHistograms)//set second kernel's arguments
			.setArg("numGroups", numGroups).setArg("finalHistogram", histogram);
		Event sumEvent = q.enqueue(sumPartials, sumRange);//enqueue second kernel, record event, and wait for completion
		q.waitForCompletion();
		
		histogram.enqueueRead(q, true, 0);//read the resulting histogram to host memory
		
		IntBuffer hist = histogram.getHostMemory().asIntBuffer();//write histogram to output file
		try(PrintWriter writer = new PrintWriter(new FileOutputStream(new File(OUTPUT_FILE)))){
			for(int i = 0; i < 256 * 3; i++){
				writer.println(hist.get(i));
			}
		}catch(Exception e){}
		
		System.out.println("OpenCL Time (ns)");//use OpenCL profiling to measure kernel runtime
		System.out.println(calcEvent.getTimeFinished() - calcEvent.getTimeStarted() + 
				sumEvent.getTimeFinished() - sumEvent.getTimeStarted());
		System.out.println();
		
		System.out.println("Java Time (ns)");//time the same calculations with a Java loop and be amazed
		long start = System.nanoTime();
		int[] values = new int[256 * 3];
		for(int i = 0; i < data.capacity() / 4; i++){
			values[data.get(i * 4) & 0xFF]++;
			values[(data.get(i * 4 + 1) & 0xFF) + 256]++;
			values[(data.get(i * 4 + 2) & 0xFF) + 512]++;
		}
		long end = System.nanoTime();
		System.out.println(end - start);
		
		//free native resources (closeCL automatically frees all native memory associated with OpenCL contexts)
		STBImage.stbi_image_free(data);
		OpenCLTools.closeCL();
	}
	
	public static String fileToString(String path){
		byte[] encoded;
		try {
			encoded = Files.readAllBytes(Paths.get(path));
			return new String(encoded, Charset.defaultCharset());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
}
