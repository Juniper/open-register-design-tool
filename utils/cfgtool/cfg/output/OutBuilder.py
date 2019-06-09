#!/usr/bin/python3
'''
@author: snellenbach

output builder class
'''
from enum import Enum
from cfg.model.Utils import MsgUtils

class OutBuilder:
    
    # config info in __init__ TODO
    def __init__(self, fileName, headerInfo):
        self.setConfig()        
        # --- open/create the file for output
        self.outfile = open(fileName, "w")
        MsgUtils.infoMsg(f'writing to file={fileName}\n')
        self.writeComment(0, headerInfo)
    def __del__(self):
        self.outfile.close()
    def writeLine(self, indent, line):
        self.outfile.write(line)
    def writeComment(self, indent, lineList):
        if len(lineList) < 2:
            self.outfile.writelines(self.lineCommentChars + line + '\n' for line in lineList)
            return
        if self.blockCommentChars[0]:
            self.outfile.write(self.blockCommentChars[0])
        self.outfile.writelines(self.blockCommentChars[1] + line + '\n' for line in lineList)
        if self.blockCommentChars[2]:
            self.outfile.write(self.blockCommentChars[2])
    def setConfig(self):
        ''' set config info (overridden by child builders) '''
        self.lineCommentChars = '' # prefix characters for line comments
        self.blockCommentChars = ('', '', '') # tuple of before, line prefix, suffix for block comments 
            
    # ----- output gen methods
    def enterClass(self, cfgNode):
        print(f'OutBuilder enterClass: node type {type(cfgNode)}')
    def exitClass(self, cfgNode):
        print(f'OutBuilder exitClass: node type {type(cfgNode)}')
    def enterMethod(self, cfgNode):
        print(f'OutBuilder enterMethod: node type {type(cfgNode)}')
    def exitMethod(self, cfgNode):
        print(f'OutBuilder exitMethod: node type {type(cfgNode)}')
    def doRegWrite(self, cfgNode):
        print(f'OutBuilder doRegWrite: node type {type(cfgNode)}')
    def doFieldWrite(self, cfgNode):
        print(f'OutBuilder doFieldWrite: node type {type(cfgNode)}')