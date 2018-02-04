#define PI 3.14159265f

uint reversal10(uint x){
	x = ((x >> 1) & 0x55555555u) | ((x & 0x55555555u) << 1);
    x = ((x >> 2) & 0x33333333u) | ((x & 0x33333333u) << 2);
    x = ((x >> 4) & 0x0f0f0f0fu) | ((x & 0x0f0f0f0fu) << 4);
    x = ((x >> 8) & 0x00ff00ffu) | ((x & 0x00ff00ffu) << 8);
    x = ((x >> 16) & 0xffffu) | ((x & 0xffffu) << 16);
	return x >> 22;
}

float2 __attribute__((always_inline))expi(float x){
	float s, c;
	s = sincos(x, &c);
	return (float2)(c, s);
}

float2 __attribute__((always_inline))cmul(float2 z1, float2 z2){
	return (float2)(z1.x * z2.x - z1.y * z2.y, z1.x * z2.y + z1.y * z2.x);
}

float2 __attribute__((always_inline))conj(float2 z){
	return (float2)(z.x, -z.y);
}

float2 __attribute__((always_inline))calcTwiddle(float factor, int n){
	return expi(factor * (float)n);
}

kernel void fft(global float* datareal, global float* dataimag, float scale){
	uint gid = get_global_id(0);
	
	uint id = gid << 4;
	uint storageIndex1 = id;
	uint storageIndex2 = storageIndex1 + 1;
	
	local float2 tempdata[1024];
	
	//pass 1: bit-reversal sort and butterfly 1 for the 16 elements after id * 16
	
	uint retrievalIndex1 = reversal10(storageIndex1);
	uint retrievalIndex2 = reversal10(storageIndex2);
	tempdata[storageIndex1].x = datareal[retrievalIndex1] + datareal[retrievalIndex2];
	tempdata[storageIndex1].y = 0.0f;
	tempdata[storageIndex2].x = datareal[retrievalIndex1] - datareal[retrievalIndex2];
	tempdata[storageIndex2].y = 0.0f;
	
	for(int i = 0; i < 15; i++){
		storageIndex1 += 2;
		storageIndex2 += 2;
		retrievalIndex1 = reversal10(storageIndex1);
		retrievalIndex2 = reversal10(storageIndex2);
		tempdata[storageIndex1].x = datareal[retrievalIndex1] + datareal[retrievalIndex2];
		tempdata[storageIndex1].y = 0.0f;
		tempdata[storageIndex2].x = datareal[retrievalIndex1] - datareal[retrievalIndex2];
		tempdata[storageIndex2].y = 0.0f;
	}
	
	//calc twiddle values
	local float2 twiddleValues[1022];
	float factor;
	if(gid == 0){//first 14 twiddles
		twiddleValues[0] = (float2)(1.0f, 0.0f);
		twiddleValues[1] = (float2)(0.0f, -1.0f);
		factor = -PI / 4.0f;
		for(int i = 0; i < 4; i++){
			twiddleValues[i + 2] = calcTwiddle(factor, i);
		}
		factor = -PI / 8.0f;
		for(int i = 0; i < 8; i++){
			twiddleValues[i + 6] = calcTwiddle(factor, i);
		}
 	}else if(gid == 1){//next 16 twiddles
		factor = -PI / 16.0f;
		for(int i = 0; i < 16; i++){
			twiddleValues[i + 14] = calcTwiddle(factor, i);
		}
	}else if(gid < 4){//items 2 and 3 to calc the next 32 twiddles (16 each)
		factor = -PI / 32.0f;
		uint offset = (gid - 2) << 4;
		for(int i = 0; i < 16; i++){
			twiddleValues[i + 30 + offset] = calcTwiddle(factor, i + offset);
		}
	}else if(gid < 8){//items 4-7 to calc the next 64 twiddles
		factor = -PI / 64.0f;
		uint offset = (gid - 4) << 4;
		for(int i = 0; i < 16; i++){
			twiddleValues[i + offset + 62] = calcTwiddle(factor, i + offset);
		}
	}else if(gid < 16){//items 8-15 to calc the next 128 twiddles
		factor = -PI / 128.0f;
		uint offset = (gid - 8) << 4;
		for(int i = 0; i < 16; i++){
			twiddleValues[i + offset + 126] = calcTwiddle(factor, i + offset);
		}
	}else if(gid < 32){//items 16-31 to calc the next 256 twiddles
		factor = -PI / 256.0f;
		uint offset = (gid - 16) << 4;
		for(int i = 0; i < 16; i++){
			twiddleValues[i + offset + 254] = calcTwiddle(factor, i + offset);
		}
	}else{//the last (32-63) 32 items to calc the final 512 twiddles
		factor = -PI / 512.0f;
		uint offset = (gid - 32) << 4;
		for(int i = 0; i < 16; i++){
			twiddleValues[i + offset + 510] = calcTwiddle(factor, i + offset);
		}
	}
	
	barrier(CLK_LOCAL_MEM_FENCE);
	
	//pass 2: d = 1, twiddleOffset = 0, pairOffset = 1 << d = 2, bundles = 1024 / 2 / 2 = 256, bundleSize = 1024 / bundles = 16
	//each work-item handles 4 bundles
	local float2* twiddlesOffset = twiddleValues;
	uint pair;
	float2 temp, mulled;
	storageIndex1 = id;
	storageIndex2 = storageIndex1 + 2;
	for(int i = 0; i < 8; i++){
		pair = i & 1;
		mulled = cmul(tempdata[storageIndex2], twiddlesOffset[pair]);
		temp = tempdata[storageIndex1];
		tempdata[storageIndex1] = temp + mulled;
		tempdata[storageIndex2] = temp - mulled;
		storageIndex1 += 1 + (pair << 1);//increment storage indices. add 2 if bundle is finished
		storageIndex2 += 1 + (pair << 1);
	}
	
	barrier(CLK_LOCAL_MEM_FENCE);
	
	//pass 3: d = 2, twiddleOffset = 2, pairOffset = 1 << d = 4, bundles = 1024 / 4 / 2 = 128, bundleSize = 1024 / bundles = 8
	//each work-item handles 2 bundles
	twiddlesOffset += 2;
	storageIndex1 = id;
	storageIndex2 = storageIndex1 + 4;
	for(int i = 0; i < 8; i++){
		pair = i & 3;
		mulled = cmul(tempdata[storageIndex2], twiddlesOffset[pair]);
		temp = tempdata[storageIndex1];
		tempdata[storageIndex1] = temp + mulled;
		tempdata[storageIndex2] = temp - mulled;
		storageIndex1 += 1 + (i == 3) * 4;//increment storage indices, add 4 if bundle is finished
		storageIndex2 += 1 + (i == 3) * 4;
	}
	
	barrier(CLK_LOCAL_MEM_FENCE);
	
	//pass 4: d = 3, twiddleOffset = 6, pairOffset = 1 << d = 8, bundles = 1024 / 8 / 2 = 64, bundleSize = 1024 / bundles = 16
	//each work-item handles 1 bundle
	twiddlesOffset += 4;
	storageIndex1 = id;
	storageIndex2 = storageIndex1 + 8;
	for(int i = 0; i < 8; i++){
		mulled = cmul(tempdata[storageIndex2], twiddlesOffset[i]);
		temp = tempdata[storageIndex1];
		tempdata[storageIndex1] = temp + mulled;
		tempdata[storageIndex2] = temp - mulled;
		storageIndex1++;
		storageIndex2++;
	}
	
	barrier(CLK_LOCAL_MEM_FENCE);
	
	//pass 5: d = 4, twiddleOffset = 14, pairOffset = 1 << d = 16, bundles = 1024 / 16 / 2 = 32, bundleSize = 1024 / bundles = 32
	//each work-item handles 1/2 of a bundle
	twiddlesOffset += 8;
	pair = (id >> 1) & 8;//32 items per bundle <=> 16 pairs, the pair number is (id / 2) % 16, even gid = 0, odd gid = 8
	storageIndex1 = ((gid >> 1) << 5) + pair;
	storageIndex2 = storageIndex1 + 16;
	for(int i = 0; i < 8; i++){
		mulled = cmul(tempdata[storageIndex2], twiddlesOffset[pair]);
		temp = tempdata[storageIndex1];
		tempdata[storageIndex1] = temp + mulled;
		tempdata[storageIndex2] = temp - mulled;
		storageIndex1++;
		storageIndex2++;
		pair++;
	}
	
	barrier(CLK_LOCAL_MEM_FENCE);
	
	//pass 6: d = 5, twiddlesOffset = 30, pairOffset = 1 << d = 32, bundles = 1024 / 32 / 2 = 16, bundleSize = 1024 / bundles = 64
	//each work-item handles 1/4 of a bundle
	twiddlesOffset += 16;
	//32 pairs per bundle, pair number is (id / 2) % 32, gid = 0 mod 4 => 0, gid = 1 mod 4 => 8, gid = 2 mod 4 => 16, gid = 3 mod 4 => 24
	pair = (id >> 1) & 0x1f;
	storageIndex1 = ((gid >> 2) << 6) + pair;
	storageIndex2 = storageIndex1 + 32;
	for(int i = 0; i < 8; i++){
		mulled = cmul(tempdata[storageIndex2], twiddlesOffset[pair]);
		temp = tempdata[storageIndex1];
		tempdata[storageIndex1] = temp + mulled;
		tempdata[storageIndex2] = temp - mulled;
		storageIndex1++;
		storageIndex2++;
		pair++;
	}
	
	barrier(CLK_LOCAL_MEM_FENCE);
	
	//pass 7: d = 6, twiddlesOffset = 62, pairOffset = 1 << d = 64, bundles = 1024 / 64 / 2 = 8, bundleSize = 1024 / bundles = 128
	//each work-item handles 1/8 of a bundle
	twiddlesOffset += 32;
	pair = (id >> 1) & 0x3f;//(id / 2) % 64, gid = 0 mod 8 => 0, gid = 1 mod 8 => 8, gid = 2 mod 8 => 16 etc.
	storageIndex1 = ((gid >> 3) << 7) + pair;
	storageIndex2 = storageIndex1 + 64;
	for(int i = 0; i < 8; i++){
		mulled = cmul(tempdata[storageIndex2], twiddlesOffset[pair]);
		temp = tempdata[storageIndex1];
		tempdata[storageIndex1] = temp + mulled;
		tempdata[storageIndex2] = temp - mulled;
		storageIndex1++;
		storageIndex2++;
		pair++;
	}
	
	barrier(CLK_LOCAL_MEM_FENCE);
	
	//pass 8: d = 7, twiddlesOffset = 126, pairOffset = 1 << d = 128, bundles = 1024 / 128 / 2 = 4, bundleSize = 1024 / bundles = 256
	//each work-item handles 1/16 of a bundle
	twiddlesOffset += 64;
	pair = (id >> 1) & 0x7f;//(id / 2) % 128...you get the point
	storageIndex1 = ((gid >> 4) << 8) + pair;
	storageIndex2 = storageIndex1 + 128;
	for(int i = 0; i < 8; i++){
		mulled = cmul(tempdata[storageIndex2], twiddlesOffset[pair]);
		temp = tempdata[storageIndex1];
		tempdata[storageIndex1] = temp + mulled;
		tempdata[storageIndex2] = temp - mulled;
		storageIndex1++;
		storageIndex2++;
		pair++;
	}
	
	barrier(CLK_LOCAL_MEM_FENCE);
	
	//pass 9: d = 8, twiddlesOffset = 254, pairOffset = 1 << d = 256, bundles = 1024 / 256 / 2 = 2, bundleSize = 1024 / bundles = 512
	//each work-item handles 1/32 of a bundle
	twiddlesOffset += 128;
	pair = (id >> 1) &0xff;//(id / 2) % 256
	storageIndex1 = ((gid >> 5) << 9) + pair;
	storageIndex2 = storageIndex1 + 256;
	for(int i = 0; i < 8; i++){
		mulled = cmul(tempdata[storageIndex2], twiddlesOffset[pair]);
		temp = tempdata[storageIndex1];
		tempdata[storageIndex1] = temp + mulled;
		tempdata[storageIndex2] = temp - mulled;
		storageIndex1++;
		storageIndex2++;
		pair++;
	}
	
	barrier(CLK_LOCAL_MEM_FENCE);
	
	//pass 10: FINALLY! d = 9, twiddlesOffset = 510, pairOffset = 1 << d = 512, bundles = 1024 / 512 / 2 = 1, bundleSize = 1024 / bundles = 1024
	//each work-item handles 1/64 of the bundle
	twiddlesOffset += 256;
	pair = id >> 1;//(id / 2) % 512 -- id < 1024 so...
	storageIndex1 = pair;
	storageIndex2 = storageIndex1 + 512;
	for(int i = 0; i < 8; i++){
		mulled = cmul(tempdata[storageIndex2], twiddlesOffset[pair]);
		temp = tempdata[storageIndex1];
		datareal[storageIndex1] = temp.x + mulled.x;
		dataimag[storageIndex1] = temp.y + mulled.y;
		datareal[storageIndex2] = temp.x - mulled.x;
		dataimag[storageIndex2] = temp.y - mulled.y;
		storageIndex1++;
		storageIndex2++;
		pair++;
	}
}