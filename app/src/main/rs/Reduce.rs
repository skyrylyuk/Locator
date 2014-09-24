#pragma version(1)
#pragma rs java_package_name(com.gunlocator)

#include "rs_cl.rsh"

short stride;
short * data;

void root(const short *v_in, short *v_out, uint32_t x) {
    if (x < stride) {
        short v = data[x + stride];
        // rsDebug("=========== v",v);
        short absv = abs(v);
        // rsDebug("=========== absv ",absv);
        data[x] += absv;
    }
}