kernel void transpose(global float* matrix){
	uint i, j, index;
	uint gid = NUM_SWITCHES * get_global_id(0);
	i = (uint)((float)SIZE - .5f - .5f * native_sqrt((float)((2 * SIZE - 1) * (2 * SIZE - 1) - 8 * gid)) + .0002f);
	index = ((i + 1) * (i + 2)) / 2 + gid;
	for(int it = 0; it < NUM_SWITCHES; it++){
		j = index % SIZE;
		i = index / SIZE;
		float tmp = matrix[index];
		uint otherIndex = j * SIZE + i;
		matrix[index] = matrix[otherIndex];
		matrix[otherIndex] = tmp;
		index++;
	}
}