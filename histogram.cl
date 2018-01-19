constant sampler_t sampler = CLK_NORMALIZED_COORDS_FALSE | CLK_ADDRESS_CLAMP_TO_EDGE | CLK_FILTER_NEAREST;

kernel void calc_partial_img_histograms(image2d_t img, int num_pixels_per_work_item, global uint* histogram){
	int localSize = (int)get_local_size(0) * (int)get_local_size(1); //number of work-items per work-group
	
	int imageWidth = get_image_width(img); int imageHeight = get_image_height(img);
	
	int x = get_global_id(0); int y = get_global_id(1);//coords in image (upper left of work-group box)

	local uint tempHistogram[256 * 3];

	int tid = get_local_id(1) * get_local_size(0) + get_local_id(0);//index in the work-group array
	int groupIndex = (get_group_id(1) * get_num_groups(0) + get_group_id(0)) * 256 * 3;

	int j = 256 * 3;
	int index = 0;

	//clear the local histogram buffer in parallel
	
	do{ 
		if(tid < j){ 
			tempHistogram[index + tid] = 0;
		}

		j -= localSize;
		index += localSize;
	}while(j > 0);
	
	barrier(CLK_LOCAL_MEM_FENCE);//wait until the whole storage space is cleared

	//if the x and y coordinates are in the image, add to the frequencies of the three color channels
	int i, idx;

	for(i = 0, idx = x; i < num_pixels_per_work_item; i++, idx += get_global_size(0)){
		if((idx < imageWidth) && (y < imageHeight)){ 
			float4 imageData = read_imagef(img, sampler, (float2)(idx, y));
			uchar indexX, indexY, indexZ;
			indexX = convert_uchar_sat(imageData.x * 255.0f);
			indexY = convert_uchar_sat(imageData.y * 255.0f);
			indexZ = convert_uchar_sat(imageData.z * 255.0f);
			atomic_inc(&tempHistogram[indexX]);
			atomic_inc(&tempHistogram[(uint)indexY + 256]);
			atomic_inc(&tempHistogram[(uint)indexZ + 512]);
		}	
	}
	
	barrier(CLK_LOCAL_MEM_FENCE);//wait until the local histogram is filled
	
	//copy the local histogram to the correct location in the global histogram
	
	if(localSize >= (256 * 3)){ //There is at least one work-item for each histogram entry
		if(tid < (256 * 3)){ //There is a histogram entry corresponding this work-item's id
			histogram[groupIndex + tid] = tempHistogram[tid];
		}
	}else{ //each work-item has to copy multiple entries into the global histogram
		j = 256 * 3;
		index = 0;
		do{ 
			if(tid < j){ 
				histogram[groupIndex + index + tid] = tempHistogram[tid + index];
			}

			j -= localSize;
			index += localSize;
		}while(j > 0);
	}
}

kernel void sum_partial_img_histograms(global uint* partialHistograms, int numGroups, global uint* finalHistogram){ 
	int tid = (int)get_global_id(0);
	int groupIndex;
	int n = numGroups;
	local uint tempHistogram[256 * 3];

	tempHistogram[tid] = partialHistograms[tid];

	groupIndex = 256 * 3;
	while(--n > 0){ 
		tempHistogram[tid] += partialHistograms[tid + groupIndex];
		groupIndex += 256 * 3;
	}

	finalHistogram[tid] = tempHistogram[tid];
}
