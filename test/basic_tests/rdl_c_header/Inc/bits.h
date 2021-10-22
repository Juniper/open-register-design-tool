/*
 * bits.h
 *
 * Struct and function declarations for dealing with bit assignment.
 */

#ifndef _BITS_H
#define _BITS_H

#define BITS_PER_LONG 32

// ## allows token concatenation
//X = 1 and Y = 10 would return 110
#define __AC(X,Y)	(X##Y)
#define _AC(X,Y)	__AC(X,Y)

#define _UL(x)		(_AC(x, UL))
#define UL(x)		(_UL(x))

#define BIT(nr) (1UL << (nr))
// BIT defines a bit mask for the specified bit number from 0 to whatever fits into an unsigned long
// so BIT(10) should evaluate to decimal 1024 (which is binary 1 left shifted by 10 bits)

#define GENMASK_INPUT_CHECK(h, l) 0

// h is high index, l is low index in a bitfield
// __GENMASK returns 32 bit number with 1s in the h-to-l field
// if h = 4 and l = 1, __GENMASK would return 00000000000000000000000000011110
#define __GENMASK(h, l) \
	(((~UL(0)) - (UL(1) << (l)) + 1) & \
	 (~UL(0) >> (BITS_PER_LONG - 1 - (h))))

#define GENMASK(h, l) \
	(GENMASK_INPUT_CHECK(h, l) + __GENMASK(h, l))

#endif /* _BITS_H */