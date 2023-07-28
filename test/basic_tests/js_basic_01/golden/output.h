//   Ordt 230719.01 autogenerated file 
//   Input: ./js_basic_01/test.js
//   Parms: ./js_basic_01/test.parms
//   Date: Thu Jul 20 13:46:38 EDT 2023
//

#ifndef __SIMPLE1_REGISTER_MAP__
#define __SIMPLE1_REGISTER_MAP__

#include <bits.h>

/* SIMPLE1_REGISTERS memory map */
enum SIMPLE1_REGS {
  STATS_DISABLE_CHECK_0 = 0x1000,
  STATS_DISABLE_CHECK_1 = 0x1004,
  STATS_FEATURES = 0x1100,
  STATS_LOG_ADDRESS1 = 0x1104,
  STATS_LOG_ADDRESS2 = 0x1110,
  STATS_WIDE_REG = 0x1120,
  RDR_REORDER_WINDOW = 0x4000,
  RDR_STREAM_RECONF = 0x4004,
  RDR_CP_FP_WR_BUFFER_0 = 0x61c0,
  RDR_CP_FP_WR_BUFFER_1 = 0x61c4,
  RDR_CP_FP_WR_BUFFER_2 = 0x61c8,
  RDR_CP_FP_WR_BUFFER_3 = 0x61cc,
  RDR_ROLL32_COUNTER_REG = 0x61d0,
  RDR_SAT32_COUNTER_REG = 0x61d4,
  RDR_RCNT_SAT_LOG = 0x61d8,
  EXTRA_REG = 0x61dc
};

/* STATS_DISABLE_CHECK_0 (disable_check register) register fields */
#define STATS_DISABLE_CHECK_0_L0_B3 BIT(6)    /* l0_b3 field */
#define STATS_DISABLE_CHECK_0_L0_B2 BIT(5)    /* l0_b2 field */
#define STATS_DISABLE_CHECK_0_L0_B1 BIT(4)    /* l0_b1 field */
#define STATS_DISABLE_CHECK_0_L0_B0 BIT(3)    /* l0_b0 field */
#define STATS_DISABLE_CHECK_0_LUT_ST BIT(2)    /* lut_st field */
#define STATS_DISABLE_CHECK_0_CBUF_FIFO BIT(1)    /* cbuf_fifo field */
#define STATS_DISABLE_CHECK_0_TIMER_SRAM BIT(0)    /* timer_sram field */

/* STATS_DISABLE_CHECK_1 (disable_check register) register fields */
#define STATS_DISABLE_CHECK_1_L0_B3 BIT(6)    /* l0_b3 field */
#define STATS_DISABLE_CHECK_1_L0_B2 BIT(5)    /* l0_b2 field */
#define STATS_DISABLE_CHECK_1_L0_B1 BIT(4)    /* l0_b1 field */
#define STATS_DISABLE_CHECK_1_L0_B0 BIT(3)    /* l0_b0 field */
#define STATS_DISABLE_CHECK_1_LUT_ST BIT(2)    /* lut_st field */
#define STATS_DISABLE_CHECK_1_CBUF_FIFO BIT(1)    /* cbuf_fifo field */
#define STATS_DISABLE_CHECK_1_TIMER_SRAM BIT(0)    /* timer_sram field */

/* STATS_FEATURES (features register) register fields */
#define STATS_FEATURES_VERSION GENMASK(31, 30)    /* version field */
#define STATS_FEATURES_SYN_LOGGED BIT(9)    /* syn_logged field */
#define STATS_FEATURES_WDTH GENMASK(8, 4)    /* wdth field */
#define STATS_FEATURES_GENERATOR BIT(3)    /* generator field */
#define STATS_FEATURES_CHECKER BIT(2)    /* checker field */
#define STATS_FEATURES_CORRECTABLE BIT(1)    /* correctable field */
#define STATS_FEATURES_ADR_LOGGED BIT(0)    /* adr_logged field */

/* STATS_LOG_ADDRESS1 (log_address1 register) register fields */
#define STATS_LOG_ADDRESS1_VALUE GENMASK(9, 0)    /* value field */

/* STATS_LOG_ADDRESS2 (log_address2 register) register fields */
#define STATS_LOG_ADDRESS2_VALUE GENMASK(9, 0)    /* value field */

/* STATS_WIDE_REG (wide_reg register) register fields */
#define STATS_WIDE_REG_VALUE GENMASK(95, 0)    /* value field */

/* RDR_REORDER_WINDOW (reorder_window register) register fields */
#define RDR_REORDER_WINDOW_WS_ENABLE BIT(31)    /* ws_enable field */
#define RDR_REORDER_WINDOW_SET_BACK GENMASK(26, 16)    /* set_back field */
#define RDR_REORDER_WINDOW_SIZE GENMASK(10, 0)    /* size field */

/* RDR_STREAM_RECONF (stream_reconf register) register fields */
#define RDR_STREAM_RECONF_BUSY BIT(16)    /* busy field */
#define RDR_STREAM_RECONF_STREAM GENMASK(9, 0)    /* stream field */

/* RDR_CP_FP_WR_BUFFER_0 (buffer register) register fields */
#define RDR_CP_FP_WR_BUFFER_0_ADDR0 GENMASK(26, 16)    /* addr0 field */
#define RDR_CP_FP_WR_BUFFER_0_ADDR1 GENMASK(10, 0)    /* addr1 field */

/* RDR_CP_FP_WR_BUFFER_1 (buffer register) register fields */
#define RDR_CP_FP_WR_BUFFER_1_ADDR0 GENMASK(26, 16)    /* addr0 field */
#define RDR_CP_FP_WR_BUFFER_1_ADDR1 GENMASK(10, 0)    /* addr1 field */

/* RDR_CP_FP_WR_BUFFER_2 (buffer register) register fields */
#define RDR_CP_FP_WR_BUFFER_2_ADDR0 GENMASK(26, 16)    /* addr0 field */
#define RDR_CP_FP_WR_BUFFER_2_ADDR1 GENMASK(10, 0)    /* addr1 field */

/* RDR_CP_FP_WR_BUFFER_3 (buffer register) register fields */
#define RDR_CP_FP_WR_BUFFER_3_ADDR0 GENMASK(26, 16)    /* addr0 field */
#define RDR_CP_FP_WR_BUFFER_3_ADDR1 GENMASK(10, 0)    /* addr1 field */

/* RDR_ROLL32_COUNTER_REG (roll32_counter_reg register) register fields */
#define RDR_ROLL32_COUNTER_REG_COUNT GENMASK(3, 0)    /* count field */

/* RDR_SAT32_COUNTER_REG (sat32_counter_reg register) register fields */
#define RDR_SAT32_COUNTER_REG_COUNT GENMASK(3, 0)    /* count field */

/* RDR_RCNT_SAT_LOG (rcnt_sat_log register) register fields */
#define RDR_RCNT_SAT_LOG_EN BIT(31)    /* en field */
#define RDR_RCNT_SAT_LOG_SUBCH GENMASK(30, 26)    /* subch field */
#define RDR_RCNT_SAT_LOG_STR GENMASK(25, 16)    /* str field */
#define RDR_RCNT_SAT_LOG_COUNT GENMASK(15, 0)    /* count field */

/* EXTRA_REG (extra_reg name) register fields */
#define EXTRA_REG_VALUE GENMASK(31, 0)    /* value field */

#endif