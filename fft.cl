#define PI 3.14159265f

uint reversal10(uint x){
	x = ((x >> 1) & 0x55555555u) | ((x & 0x55555555u) << 1);
    x = ((x >> 2) & 0x33333333u) | ((x & 0x33333333u) << 2);
    x = ((x >> 4) & 0x0f0f0f0fu) | ((x & 0x0f0f0f0fu) << 4);
    x = ((x >> 8) & 0x00ff00ffu) | ((x & 0x00ff00ffu) << 8);
    x = ((x >> 16) & 0xffffu) | ((x & 0xffffu) << 16);
	return x >> 22;
}

uint reversal5(uint x){
	x = ((x >> 1) & 0x55555555u) | ((x & 0x55555555u) << 1);
    x = ((x >> 2) & 0x33333333u) | ((x & 0x33333333u) << 2);
    x = ((x >> 4) & 0x0f0f0f0fu) | ((x & 0x0f0f0f0fu) << 4);
    x = ((x >> 8) & 0x00ff00ffu) | ((x & 0x00ff00ffu) << 8);
    x = ((x >> 16) & 0xffffu) | ((x & 0xffffu) << 16);
	return x >> 27;
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
	
	local float tempreal[32];
	local float tempimag[32];
	
	//pass 1--copy and bit-reversal sort
	int storageIndex1 = gid << 1;
	int storageIndex2 = storageIndex1 + 1;
	
	int retrievalIndex1 = reversal5(storageIndex1);
	int retrievalIndex2 = reversal5(storageIndex2);
	
	float firstRetrievalValue = datareal[retrievalIndex1];
	float secondRetrievalValue = datareal[retrievalIndex2];
	tempreal[storageIndex1] = firstRetrievalValue + secondRetrievalValue;
	tempimag[storageIndex1] = 0.0f;
	tempreal[storageIndex2] = firstRetrievalValue - secondRetrievalValue;
	tempimag[storageIndex2] = 0.0f;	
	
	local float2 twiddleFactors[30];
	
	barrier(CLK_LOCAL_MEM_FENCE);
	
	//calculate twiddle factors
	if(gid == 0){
		twiddleFactors[0] = (float2)(1.0f, 0.0f);
		twiddleFactors[1] = (float2)(0.0f, -1.0f);
	}else if(gid < 3){
		twiddleFactors[storageIndex1] = calcTwiddle(-PI / 4.0f, storageIndex1 - 2);
		twiddleFactors[storageIndex2] = calcTwiddle(-PI / 4.0f, storageIndex2 - 2);
	}else if(gid < 7){
		twiddleFactors[storageIndex1] = calcTwiddle(-PI / 8.0f, storageIndex1 - 6);
		twiddleFactors[storageIndex2] = calcTwiddle(-PI / 8.0f, storageIndex2 - 6);
	}else if(gid < 15){
		twiddleFactors[storageIndex1] = calcTwiddle(-PI / 16.0f, storageIndex1 - 14);
		twiddleFactors[storageIndex2] = calcTwiddle(-PI / 16.0f, storageIndex2 - 14);
	}
	
	barrier(CLK_LOCAL_MEM_FENCE);
	
	//pass 2: d = 1, twiddleOffset = 0, pairOffset = 1 << d = 2, bundles = 32 / 2 / 2 = 8
	local float2* twiddleFactorsOffset = twiddleFactors;
	int bundle = gid >> 1;
	int pair = gid % 2;
	storageIndex1 = (bundle << 2) + pair;
	storageIndex2 = storageIndex1 + 2;
	float2 mulled = cmul(twiddleFactorsOffset[pair], (float2)(tempreal[storageIndex2], tempimag[storageIndex2]));
	float2 temp = (float2)(tempreal[storageIndex1], tempimag[storageIndex1]) + mulled;
	tempreal[storageIndex2] = tempreal[storageIndex1] - mulled.x;
	tempimag[storageIndex2] = tempimag[storageIndex1] - mulled.y;
	tempreal[storageIndex1] = temp.x;
	tempimag[storageIndex1] = temp.y;
	
	barrier(CLK_LOCAL_MEM_FENCE);
	
	//pass 3: d = 2, twiddleOffset = 2, pairOffset = 1 << d = 4, bundles = 32 / 4 / 2 = 4
	twiddleFactorsOffset += 2;
	bundle = gid >> 2;
	pair = gid % 4;
	storageIndex1 = (bundle << 3) + pair;
	storageIndex2 = storageIndex1 + 4;
	mulled = cmul(twiddleFactorsOffset[pair], (float2)(tempreal[storageIndex2], tempimag[storageIndex2]));
	temp = (float2)(tempreal[storageIndex1], tempimag[storageIndex1]) + mulled;
	tempreal[storageIndex2] = tempreal[storageIndex1] - mulled.x;
	tempimag[storageIndex2] = tempimag[storageIndex1] - mulled.y;
	tempreal[storageIndex1] = temp.x;
	tempimag[storageIndex1] = temp.y;
	
	barrier(CLK_LOCAL_MEM_FENCE);
	
	//pass 4: d = 3, twiddleOffset = 6, pairOffset = 1 << d = 8, bundles = 32 / 8 / 2 = 2
	twiddleFactorsOffset += 4;
	bundle = gid >> 3;
	pair = gid % 8;
	storageIndex1 = (bundle << 4) + pair;
	storageIndex2 = storageIndex1 + 8;
	mulled = cmul(twiddleFactorsOffset[pair], (float2)(tempreal[storageIndex2], tempimag[storageIndex2]));
	temp = (float2)(tempreal[storageIndex1], tempimag[storageIndex1]) + mulled;
	tempreal[storageIndex2] = tempreal[storageIndex1] - mulled.x;
	tempimag[storageIndex2] = tempimag[storageIndex1] - mulled.y;
	tempreal[storageIndex1] = temp.x;
	tempimag[storageIndex1] = temp.y;
	
	barrier(CLK_LOCAL_MEM_FENCE);
	
	//pass 5: d = 4, twiddleOffset = 14, pairOffset = 1 << d = 16, bundles = 32 / 16 / 2 = 1
	twiddleFactorsOffset += 8;
	pair = gid;
	storageIndex1 = pair;
	storageIndex2 = pair + 16;
	mulled = cmul(twiddleFactorsOffset[pair], (float2)(tempreal[storageIndex2], tempimag[storageIndex2]));
	datareal[storageIndex1] = scale * (tempreal[storageIndex1] + mulled.x);
	dataimag[storageIndex1] = scale * (tempimag[storageIndex1] + mulled.y);
	datareal[storageIndex2] = scale * (tempreal[storageIndex1] - mulled.x);
	dataimag[storageIndex2] = scale * (tempimag[storageIndex1] - mulled.y);
}
