'''
@author: snellenbach

python format 1 output builder class
'''
from enum import Enum
from cfg.model.Utils import MsgUtils
from cfg.output.OutBuilder import OutBuilder

class PyF1OutBuilder(OutBuilder):
    def setConfig(self):
        ''' set config info for py form 1 '''
        self.lineCommentChars = '# ' # prefix characters for line comments
        self.blockCommentChars = ('', '# ', '') # tuple of before, line prefix, suffix for block comments 

    # ----- output gen method overrides
    def enterClass(self, cfgNode):
        print(f'PyF1OutBuilder enterClass: node type {type(cfgNode)}')
    def exitClass(self, cfgNode):
        print(f'PyF1OutBuilder exitClass: node type {type(cfgNode)}')
    def enterMethod(self, cfgNode):
        print(f'PyF1OutBuilder enterMethod: node type {type(cfgNode)}')
    def exitMethod(self, cfgNode):
        print(f'PyF1OutBuilder exitMethod: node type {type(cfgNode)}')
    def doRegWrite(self, cfgNode):
        #print(f'PyF1OutBuilder doWrite: node type {type(cfgNode)}')
        print(f'PyF1OutBuilder doRegWrite: path: {cfgNode.path}, wtype: {cfgNode.wtype}, rwm: {cfgNode.isRmw} --- self.path type={type(cfgNode.path)}')  # TODO
        self.writeLine(0, f'{cfgNode.path.genRawPathStr()} = {hex(cfgNode.value.val)}')
    def doFieldWrite(self, cfgNode):
        #print(f'PyF1OutBuilder doFieldWrite: node type {type(cfgNode)}')
        print(f'PyF1OutBuilder doFieldWrite: path: {cfgNode.path}, wtype: {cfgNode.wtype}, rwm: {cfgNode.isRmw} --- self.path type={type(cfgNode.path)}')  # TODO
