/* This file has been autogenerated by Ivory
 * Compiler version  0.1.0.3
 */
#ifndef __HEARTBEAT_TYPES_H__
#define __HEARTBEAT_TYPES_H__
#ifdef __cplusplus
extern "C" {
#endif
#include "arming_mode_types.h"
#include "ivory.h"
#include "ivory_serialize.h"
#include "time_micros_types.h"
typedef struct heartbeat { int64_t time;
                           uint8_t arming_mode;
} heartbeat;
void heartbeat_get_le(const uint8_t *n_var0, uint32_t n_var1, struct heartbeat *n_var2);
void heartbeat_get_be(const uint8_t *n_var0, uint32_t n_var1, struct heartbeat *n_var2);
void heartbeat_set_le(uint8_t *n_var0, uint32_t n_var1, const struct heartbeat *n_var2);
void heartbeat_set_be(uint8_t *n_var0, uint32_t n_var1, const struct heartbeat *n_var2);

#ifdef __cplusplus
}
#endif
#endif /* __HEARTBEAT_TYPES_H__ */