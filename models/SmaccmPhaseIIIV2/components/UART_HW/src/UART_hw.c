/* This file has been autogenerated by Ivory
 * Compiler version  0.1.0.3
 */
#include "UART_hw.h"

static void component_entry_aux(void);

void component_entry(void)
{
    component_entry_aux();
    
    int64_t n_local0 = (int64_t) 0;
    int64_t *n_ref1 = &n_local0;
    
    return;
}

void component_init(void)
{
    int64_t n_local0 = (int64_t) 0;
    int64_t *n_ref1 = &n_local0;
}

void component_entry_aux(void)
{
    struct ivory_string_HXCyphertext n_local0 = {};
    struct ivory_string_HXCyphertext *n_ref1 = &n_local0;
    bool n_r2 = UART_out_UART_hw_get_packet(n_ref1);
    
    if (n_r2) {
        callback_input_UART_out_UART_hw_get_packet_handler(n_ref1);
    }
    return;
}