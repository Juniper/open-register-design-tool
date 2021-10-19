package ordt.output.cheader;

import ordt.output.FieldProperties;
import ordt.output.OutputBuilder;
import ordt.output.common.OutputLine;
import ordt.parameters.ExtParameters;

import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.List;


public class CHeaderBuilder extends OutputBuilder {
    private List<OutputLine> memoryMapOutputList = new ArrayList<OutputLine>();
    private List<OutputLine> bitfieldOutputList = new ArrayList<OutputLine>();
    private List<OutputLine> commonOutputList = new ArrayList<OutputLine>();
    private List<OutputLine> explicitFunctionOutputList = new ArrayList<OutputLine>();
    private int indentLvl = 0;
    private final int noIndent = 0;

    /*******************************************************************************************************************
     * User configurable parameters
     ******************************************************************************************************************/

    /*******************************************************************************************************************
     * Internal variables
     ******************************************************************************************************************/

    public CHeaderBuilder(ordt.extract.RegModelIntf model) {
        setBaseBuilderID();   // set unique ID of this instance
        this.model = model;
        setVisitEachReg(false);                 // only need to call once for replicated reg groups
        setVisitEachRegSet(false);              // only need to call once for replicated reg set groups
        setVisitExternalRegisters(true);        // we will visit externals
        setVisitEachExternalRegister(false);    // handle externals as a group
        ordt.output.RhsReference.setInstancePropertyStack(instancePropertyStack);  // update pointer to the instance stack for rhs reference evaluation
        model.getRoot().generateOutput(null, this);   // generate output structures recursively starting at model root
    }

    @Override
    public void addField() {
        while (fieldList.size() > 0){
            FieldProperties field = fieldList.remove();
            int lowIndex = field.getLowIndex();
            int highIndex = field.getLowIndex() + field.getFieldWidth() - 1;

            if (lowIndex == highIndex) {
                String bitIndices = String.format("%d", lowIndex);
                bitfieldOutputList.add(new OutputLine(noIndent, String.format("#define %s BIT(%s)", field.getTextName(), bitIndices)));
            }
            if (lowIndex != highIndex) {
                String bitIndices = String.format("%d, %d", highIndex, lowIndex);
                bitfieldOutputList.add(new OutputLine(noIndent, String.format("#define %s GENMASK(%s)", field.getTextName(), bitIndices)));
            }
        }
    }
    @Override
    public void addAliasField() {
    }

    @Override
    public void addRegister() {
        String regAddress = regProperties.getExtractInstance().getAddress().toString();
        String regName = regProperties.getTextName();

        if (ExtParameters.cheaderAddMemoryMap())
            memoryMapOutputList.add(new OutputLine(indentLvl, String.format("%s = %s,", regName, regAddress)));

        if (ExtParameters.cheaderAddBitfields())
            bitfieldOutputList.add(new OutputLine(noIndent, String.format("\n/* %s registers */", regName)));
    }

    @Override
    public void finishRegister() {
    }

    @Override
    public void addRegSet() {
    }
    @Override
    public void finishRegSet() {
    }

    @Override
    public void addRegMap() {
        if (ExtParameters.cheaderAddMemoryMap())
             addHeader();
    }
    @Override
    public void finishRegMap() {
        if (ExtParameters.cheaderAddMemoryMap())
            endEnum();
    }

    @Override
    public void write(BufferedWriter bw) {
        bufferedWriter = bw;

        // Comments about auto generated file with name and date
        addComments();
        for (OutputLine jsLine : commonOutputList){
            writeStmt(jsLine.getIndent(), jsLine.getLine());
        }

        // Explicitly declare all functions in the same header file
        if (ExtParameters.cheaderExplicitFunctions()){
            explicitFunctions();
            for (OutputLine jsLine : explicitFunctionOutputList){
                writeStmt(jsLine.getIndent(), jsLine.getLine());
            }
        }
        else writeStmt(noIndent,"#include <bits.h>\n");

        // Write memory map (enum)
        if (ExtParameters.cheaderAddMemoryMap())
            // sherlock: memoryMapOutputList is array, OutputLine is datatype for temp variable jsLine
            // sherlock: loop iterates over array and jsline becomes each element
            for (OutputLine jsLine : memoryMapOutputList) {
                writeStmt(jsLine.getIndent(), jsLine.getLine());
            }

        // Write bitfields (#define)
        if (ExtParameters.cheaderAddBitfields())
            for (OutputLine jsLine : bitfieldOutputList) {
                writeStmt(jsLine.getIndent(), jsLine.getLine());
            }

        endComments();
    }

    /*******************************************************************************************************************
     * Builder specific methods
     ******************************************************************************************************************/
    void addComments() {
        commonOutputList.add(new OutputLine(indentLvl, "///////////////////////////////////////////////////////////////////////////////"));
        commonOutputList.add(new OutputLine(indentLvl, "///////////////////////////////////////////////////////////////////////////////"));
        commonOutputList.add(new OutputLine(indentLvl, "//"));
        commonOutputList.add(new OutputLine(indentLvl, String.format("/* This is an automatically generated header file for %s address map */", getAddressMapName())));
        commonOutputList.add(new OutputLine(indentLvl, "//"));
        commonOutputList.add(new OutputLine(indentLvl, "/* DO NOT MODIFY THIS FILE! USE 'openrdt' toolchain to modify register map! */"));
        commonOutputList.add(new OutputLine(indentLvl, "//"));
        commonOutputList.add(new OutputLine(indentLvl, String.format("/* Generated on: %s */", java.time.LocalDate.now().toString())));
        commonOutputList.add(new OutputLine(indentLvl, "//"));
        commonOutputList.add(new OutputLine(indentLvl, "///////////////////////////////////////////////////////////////////////////////"));
        commonOutputList.add(new OutputLine(indentLvl, "///////////////////////////////////////////////////////////////////////////////"));
        commonOutputList.add(new OutputLine(indentLvl, ""));
        commonOutputList.add(new OutputLine(indentLvl, String.format("#ifndef __%s_REGISTER_MAP__", getAddressMapName().toUpperCase())));
        commonOutputList.add(new OutputLine(indentLvl, String.format("#define __%s_REGISTER_MAP__\n", getAddressMapName().toUpperCase())));
    }

    void addHeader() {
        memoryMapOutputList.add(new OutputLine(indentLvl, String.format("/* %s_REGISTERS memory map */", getAddressMapName().toUpperCase())));
        memoryMapOutputList.add(new OutputLine(indentLvl++, String.format("enum %s_REGS {", getAddressMapName().toUpperCase())));
    }

    void endEnum(){
        memoryMapOutputList.add(new OutputLine(--indentLvl, "};"));
    }

    void endComments() {
        commonOutputList.add(new OutputLine(indentLvl, "#endif"));
        writeStmt(noIndent, "\n#endif");
    }

    void explicitFunctions(){
        explicitFunctionOutputList.add(new OutputLine(indentLvl, String.format("/*\n" +
                " * bits.h\n" +
                " *\n" +
                " * Struct and function declarations for dealing with bit assignment.\n" +
                " */\n" +
                "\n" +
                "#ifndef _BITS_H\n" +
                "#define _BITS_H\n" +
                "\n" +
                "#define BITS_PER_LONG 32\n" +
                "\n" +
                "// ## allows token concatenation\n" +
                "//X = 1 and Y = 10 would return 110\n" +
                "#define __AC(X,Y)\t(X##Y)\n" +
                "#define _AC(X,Y)\t__AC(X,Y)\n" +
                "\n" +
                "#define _UL(x)\t\t(_AC(x, UL))\n" +
                "#define UL(x)\t\t(_UL(x))\n" +
                "\n" +
                "#define BIT(nr) (1UL << (nr))\n" +
                "// BIT defines a bit mask for the specified bit number from 0 to whatever fits into an unsigned long\n" +
                "// so BIT(10) should evaluate to decimal 1024 (which is binary 1 left shifted by 10 bits)\n" +
                "\n" +
                "#define GENMASK_INPUT_CHECK(h, l) 0\n" +
                "\n" +
                "// h is high index, l is low index in a bitfield\n" +
                "// __GENMASK returns 32 bit number with 1s in the h-to-l field\n" +
                "// if h = 4 and l = 1, __GENMASK would return 00000000000000000000000000011110\n" +
                "#define __GENMASK(h, l) \\\n" +
                "\t(((~UL(0)) - (UL(1) << (l)) + 1) & \\\n" +
                "\t (~UL(0) >> (BITS_PER_LONG - 1 - (h))))\n" +
                "\n" +
                "#define GENMASK(h, l) \\\n" +
                "\t(GENMASK_INPUT_CHECK(h, l) + __GENMASK(h, l))\n" +
                "\n" +
                "#endif /* _BITS_H */")));
    }
}