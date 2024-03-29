# open-register-design-tool

Ordt is a tool for automation of IC register definition and documentation.  It currently supports 2 input formats:
  1. SystemRDL - a stardard register description format released by [Accellera.org](http://accellera.org/activities/working-groups/systemrdl)
  2. JSpec - a register description format used within Juniper Networks

The tool can generate several outputs from SystemRDL or JSpec, including:
  - SystemVerilog/Verilog RTL code description of registers
  - UVM model of the registers
  - C++ and python models of the registers
  - C header file providing register address and field defines
  - XML and text file register descriptions
  - SystemRDL and JSpec (conversion)

Easiest way to get started with ordt is to download a runnable jar from the [Juniper repo release area](https://github.com/Juniper/open-register-design-tool/releases).
Ordt documentation can be found [here](https://github.com/Juniper/open-register-design-tool/wiki).
