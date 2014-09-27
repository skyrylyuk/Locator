#pragma version(1)
#pragma rs java_package_name(com.gunlocator)

#include "rs_cl.rsh"

// short stride;
short * data;
short * out;

void root(const short *v_in, short *v_out, uint32_t x) {
    rsDebug("=========== x", x);
    switch ( x ) {
        case 0:
            out[x] = 0;
            break;
        case 1:
            out[x] = -1;
            break;

        default:
            out[x] = data[x] + out[x-1];
            break;
    }

}