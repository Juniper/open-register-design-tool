package ordt.output.cheader;

import ordt.extract.RegNumber;
import ordt.output.FieldProperties;
import ordt.output.OutputBuilder;
import ordt.output.common.OutputLine;
import ordt.parameters.ExtParameters;

import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class CHeaderBuilder extends OutputBuilder {
    private List<OutputLine> bitfieldOutputList = new ArrayList<OutputLine>();
    private final int noIndent = 0;

    public CHeaderBuilder(ordt.extract.RegModelIntf model) {
        setBaseBuilderID();   // set unique ID of this instance
        this.model = model;
        setVisitEachReg(true);                 // only need to call once for replicated reg groups
        setVisitEachRegSet(true);              // only need to call once for replicated reg set groups
        setVisitExternalRegisters(true);        // we will visit externals
        setVisitEachExternalRegister(true);    // handle externals as a group
        ordt.output.RhsReference.setInstancePropertyStack(instancePropertyStack);  // update pointer to the instance stack for rhs reference evaluation
        model.getRoot().generateOutput(null, this);   // generate output structures recursively starting at model root
    }

    public class MemoryMapEntry {
      public RegNumber address;
      public String regName;
      public MemoryMapEntry(RegNumber address, String regName) {
        this.address = address;
        this.regName = regName;
      }
    }
    private List<MemoryMapEntry> memoryMapEntryList = new ArrayList<MemoryMapEntry>();

    String explicitFunctionString =
                 "/*\n" +
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
                "#endif /* _BITS_H */\n";

    /*******************************************************************************************************************
     * Builder override methods
     ******************************************************************************************************************/

    @Override
    public void addField() {
        int lowIndex = fieldProperties.getLowIndex();
        int highIndex = fieldProperties.getLowIndex() + fieldProperties.getFieldWidth() - 1;
        String fieldName = fieldProperties.getBaseName().toUpperCase();
        String textNameComment = (fieldProperties.getTextName() == null) ? "" : "    /* " + fieldProperties.getTextName() + " */";

        if (lowIndex == highIndex) {
            String bitIndices = String.format("%d", lowIndex);
            bitfieldOutputList.add(new OutputLine(noIndent, String.format("#define %s BIT(%s)%s", fieldName, bitIndices, textNameComment)));
        }
        else {
            String bitIndices = String.format("%d, %d", highIndex, lowIndex);
            bitfieldOutputList.add(new OutputLine(noIndent, String.format("#define %s GENMASK(%s)%s", fieldName, bitIndices, textNameComment)));
        }
    }
    @Override
    public void addAliasField() {
    }

    @Override
    public void addRegister() {
        String regName = regProperties.getBaseName().toUpperCase();

        if (ExtParameters.cheaderAddMemoryMap()) 
            memoryMapEntryList.add(new MemoryMapEntry(regProperties.getBaseAddress(), regName));
        
        if (ExtParameters.cheaderAddBitfields()) {
            String textName = (regProperties.getTextName() == null) ? "" : " (" + regProperties.getTextName() + ")";
            bitfieldOutputList.add(new OutputLine(noIndent, "\n/* " + regName + textName + " register fields */"));
        }
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
    }
    @Override
    public void finishRegMap() {
    }

    @Override
    public void write(BufferedWriter bw) {
        bufferedWriter = bw;

        writeStmt(0, String.format("#ifndef __%s_REGISTER_MAP__", getAddressMapName().toUpperCase()));
        writeStmt(0, String.format("#define __%s_REGISTER_MAP__\n", getAddressMapName().toUpperCase()));

        // Explicitly declare all functions in the same header file
        if (ExtParameters.cheaderExplicitFunctions())
            writeStmt(noIndent, explicitFunctionString); 
        else writeStmt(noIndent, "#include <bits.h>\n");

        // Write memory map (enum)
        if (ExtParameters.cheaderAddMemoryMap()) {
            writeStmt(0, String.format("/* %s_REGISTERS memory map */", getAddressMapName().toUpperCase()));
            writeStmt(0, String.format("enum %s_REGS {", getAddressMapName().toUpperCase()));
            Iterator<MemoryMapEntry> mapIter = memoryMapEntryList.iterator();
            while (mapIter.hasNext()) {
                MemoryMapEntry mapEntry = mapIter.next();
                String suffix = (mapIter.hasNext())? "," : "";
                writeStmt(1, mapEntry.regName + " = " + mapEntry.address.toString() + suffix);
            }
            writeStmt(0, "};");
        }

        // Write bitfields (#define)
        if (ExtParameters.cheaderAddBitfields())
            for (OutputLine jsLine : bitfieldOutputList) {
                writeStmt(jsLine.getIndent(), jsLine.getLine());
            }

        writeStmt(noIndent, "\n#endif");
    }
    
}