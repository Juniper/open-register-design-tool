//   Ordt 230719.01 autogenerated file 
//   Input: ./rdl_basic_01/test.rdl
//   Parms: ./rdl_basic_01/test.parms
//   Date: Thu Jul 20 13:46:40 EDT 2023
//

#ifndef __SIMPLE1_REGISTER_MAP__
#define __SIMPLE1_REGISTER_MAP__

#include <bits.h>

/* SIMPLE1_REGISTERS memory map */
enum SIMPLE1_REGS {
  STATS_DISABLE_CHECK_0 = 0x1000,
  STATS_DISABLE_CHECK_1 = 0x1080,
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

/* STATS_DISABLE_CHECK_0 register fields */
#define STATS_DISABLE_CHECK_0_TIMER_SRAM BIT(0)
#define STATS_DISABLE_CHECK_0_CBUF_FIFO BIT(1)
#define STATS_DISABLE_CHECK_0_LUT_ST BIT(2)
#define STATS_DISABLE_CHECK_0_L0_B0 BIT(3)
#define STATS_DISABLE_CHECK_0_L0_B1 BIT(4)
#define STATS_DISABLE_CHECK_0_L0_B2 BIT(5)
#define STATS_DISABLE_CHECK_0_L0_B3 BIT(6)

/* STATS_DISABLE_CHECK_1 register fields */
#define STATS_DISABLE_CHECK_1_TIMER_SRAM BIT(0)
#define STATS_DISABLE_CHECK_1_CBUF_FIFO BIT(1)
#define STATS_DISABLE_CHECK_1_LUT_ST BIT(2)
#define STATS_DISABLE_CHECK_1_L0_B0 BIT(3)
#define STATS_DISABLE_CHECK_1_L0_B1 BIT(4)
#define STATS_DISABLE_CHECK_1_L0_B2 BIT(5)
#define STATS_DISABLE_CHECK_1_L0_B3 BIT(6)

/* STATS_FEATURES register fields */
#define STATS_FEATURES_VERSION GENMASK(31, 30)
#define STATS_FEATURES_DEFAULT_RST_FLD GENMASK(17, 16)
#define STATS_FEATURES_ALT_RST_FLD GENMASK(15, 14)
#define STATS_FEATURES_SYN_LOGGED BIT(9)
#define STATS_FEATURES_WDTH GENMASK(8, 4)
#define STATS_FEATURES_GENERATOR BIT(3)
#define STATS_FEATURES_CHECKER BIT(2)
#define STATS_FEATURES_CORRECTABLE BIT(1)
#define STATS_FEATURES_ADR_LOGGED BIT(0)

/* STATS_LOG_ADDRESS1 register fields */
#define STATS_LOG_ADDRESS1_VALUE GENMASK(9, 0)

/* STATS_LOG_ADDRESS2 register fields */
#define STATS_LOG_ADDRESS2_VALUE GENMASK(9, 0)

/* STATS_WIDE_REG register fields */
#define STATS_WIDE_REG_VALUE GENMASK(95, 0)

/* RDR_REORDER_WINDOW register fields */
#define RDR_REORDER_WINDOW_WS_ENABLE BIT(31)
#define RDR_REORDER_WINDOW_SET_BACK GENMASK(26, 16)
#define RDR_REORDER_WINDOW_SIZE GENMASK(10, 0)

/* RDR_STREAM_RECONF register fields */
#define RDR_STREAM_RECONF_BUSY BIT(16)
#define RDR_STREAM_RECONF_STREAM GENMASK(9, 0)

/* RDR_CP_FP_WR_BUFFER_0 register fields */
#define RDR_CP_FP_WR_BUFFER_0_ADDR0 GENMASK(26, 16)
#define RDR_CP_FP_WR_BUFFER_0_ADDR1 GENMASK(10, 0)

/* RDR_CP_FP_WR_BUFFER_1 register fields */
#define RDR_CP_FP_WR_BUFFER_1_ADDR0 GENMASK(26, 16)
#define RDR_CP_FP_WR_BUFFER_1_ADDR1 GENMASK(10, 0)

/* RDR_CP_FP_WR_BUFFER_2 register fields */
#define RDR_CP_FP_WR_BUFFER_2_ADDR0 GENMASK(26, 16)
#define RDR_CP_FP_WR_BUFFER_2_ADDR1 GENMASK(10, 0)

/* RDR_CP_FP_WR_BUFFER_3 register fields */
#define RDR_CP_FP_WR_BUFFER_3_ADDR0 GENMASK(26, 16)
#define RDR_CP_FP_WR_BUFFER_3_ADDR1 GENMASK(10, 0)

/* RDR_ROLL32_COUNTER_REG register fields */
#define RDR_ROLL32_COUNTER_REG_COUNT GENMASK(3, 0)

/* RDR_SAT32_COUNTER_REG register fields */
#define RDR_SAT32_COUNTER_REG_COUNT GENMASK(3, 0)

/* RDR_RCNT_SAT_LOG register fields */
#define RDR_RCNT_SAT_LOG_EN BIT(31)
#define RDR_RCNT_SAT_LOG_SUBCH GENMASK(30, 26)
#define RDR_RCNT_SAT_LOG_STR GENMASK(25, 16)
#define RDR_RCNT_SAT_LOG_COUNT GENMASK(15, 0)

/* EXTRA_REG (extra_reg name) register fields */
#define EXTRA_REG_VALUE GENMASK(31, 0)

#endif
