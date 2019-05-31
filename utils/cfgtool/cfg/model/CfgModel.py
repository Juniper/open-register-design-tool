#!/usr/bin/python3
'''
@author: snellenbach

Config sequence model
'''
from enum import Enum, unique
import re
from cfg.model.RegModelWrapper import RegModelWrapper
from cfg.model.Utils import MsgUtils
from cfg.output.OutBuilder import OutBuilder as ob

# ------- config model node classes

class BaseCfgNode:
    _nodeStack = []  # stack of active config nodes for auto-add
    _outBuilder = None
    
    def __init__(self, sourceAstNode=None, comment=''):
        self.sourceAstNode = sourceAstNode
        self.comment = comment
        self.children = []
        self.parent = None
        self.allowedTags = set() # set of allowed versions for this level (parser allows currently allows in class, method)
        # add this node to parent (top of stack)
        if __class__._nodeStack:
            self.parent = __class__._nodeStack[-1]
            self.parent.addChild(self)
    def addChild(self, child):
        self.children.append(child)
    def popChild(self):
        ''' pop last added child from this node '''
        if self.children:
            self.children.pop()
    def display(self, indent = 0):
        ''' display config model node info recursively '''
        print('  '*indent + 'base:')
        for child in self.children:
            child.display(indent+1)
    @staticmethod
    def finishNode(omit):
        ''' Pop current node from the active model stack.  Optionally, remove this node if omit is set. '''
        __class__.popNode()
        if omit:
            parent = __class__._nodeStack[-1]
            parent.popChild()
    @staticmethod
    def popNode():
        ''' pop cfg node from top of the stack '''
        return __class__._nodeStack.pop()
    @staticmethod
    def peekNode():
        ''' return cfg node at top of the stack '''
        return __class__._nodeStack[-1]
    def hierDisplay(self, indent, s):
        ''' display config model node info recursively '''
        print('  '*indent + s)
        for child in self.children:
            child.display(indent+1)
    def resolvePaths(self):
        ''' resolve all paths in this config model node info recursively '''
        for child in self.children:
            child.resolvePaths()
    def setOutBuilder(self, outBuilder):
        ''' set specified output builder '''
        #print(f'BaseCfgNode setOutBuilder: called in {type(self)}, outBuilder type={type(outBuilder)}')
        BaseCfgNode._outBuilder = outBuilder
    def generateOutput(self):
        ''' generate specified output for this config model recursively '''
        #print(f'BaseCfgNode generateOutput: called in {type(self)}')
        for child in self.children:
            child.generateOutput()
   
class HierCfgNode(BaseCfgNode):
    ''' hierarchical node (pushed to node stack on create) '''
    def __init__(self, sourceAstNode = None, comment=''):
        BaseCfgNode.__init__(self, sourceAstNode, comment)
        # append this node to the stack
        __class__._nodeStack.append(self)
        self.vars = {} # dict of vars defined in this node scope
    def whatami(self):
        return 'unspecified hierarchy'
    def findVar(self, varName, allowInputs = True):
        ''' find a variable by name traversing from current node thru ancestors '''
        if self.vars.__contains__(varName):
            retVar = self.vars[varName]
            if allowInputs or (type(retVar) is not CfgInputVariable):
                return retVar
            MsgUtils.errorExit('input variable ' + varName + ' can not be assigned a value.')
            return None
        elif self.parent is None:
            return None
        else:
            return self.parent.findVar(varName)
    def getInputList(self):
        return {k: v for k, v in self.vars.items() if type(v) is CfgInputVariable}        
    def verifyInputParms(self, inputListStr, callingNode):
        ''' check that a list of call parameter strings matches inputs for this hier and return the list of resolved inputs '''
        if type(inputListStr) is not str:
            MsgUtils.errorExit(f'misformed input list found when in call of {self.whatami()} {self.name}')
        inputList = [] if not inputListStr else inputListStr.split(',')
        inputCount = len(inputList)
        inputParms = self.getInputList()
        inputParmCount = len(inputParms)
        #print(f"HierCfgNode verifyInputParms: inputList={inputList}, in len={inputCount}, vars=({', '.join(str(e) for e in inputParms.values())}), parm len={inputParmCount}, callNode type={type(callingNode)}")
        if inputCount != inputParmCount:
            MsgUtils.errorExit(f'incorrect number of input parameters (found {inputCount}, expected {inputParmCount}) in call of {self.whatami()} {self.name}')
        # loop and resolve inputs CfgVariable.resolveRhsExpression(className, CfgClassNode, False, True) 
        resolvedInputList = []
        for inVal, inParm in zip(inputList, inputParms.values()):
            resolvedInputList.append(CfgVariable.resolveRhsExpression(inVal, inParm.vartype, True, True))
        return resolvedInputList

class CfgClassNode(HierCfgNode):
    _classes = {}
    _current = None
    def __init__(self, name, sourceAstNode = None, comment=''):
        HierCfgNode.__init__(self, sourceAstNode, comment)
        self.name = name
        self.methods = {}
        __class__._classes[self.name] = self
        __class__._current = self
        #print('creating class node, name=', self.name)
    def whatami(self):
        return 'class'
    @staticmethod
    def getCurrent():
        ''' return last created CfgClassNode '''
        return __class__._current
    @staticmethod
    def findClass(className):
        ''' return a CfgClassNode by name '''
        return None if className not in __class__._classes else __class__._classes[className]
    def findMethod(self, methodName):
        ''' return a CfgMethodNode in this class by name '''
        return None if methodName not in self.methods else self.methods[methodName]
    def display(self, indent = 0):
        inParms = self.getInputList()
        self.hierDisplay(indent, f"class: {self.name}, vars=({', '.join(str(e) for e in self.vars.values())}), inputs=({', '.join(str(e) for e in inParms.values())}), allowed versions='{self.allowedTags}")
    def generateOutput(self):
        ''' generate specified output for this class node '''
        #print(f'CfgClassNode generateOutput: called in {type(self)}')
        BaseCfgNode._outBuilder.enterClass(self)
        for child in self.children:
            child.generateOutput()
        BaseCfgNode._outBuilder.exitClass(self)
   
class CfgMethodNode(HierCfgNode):
    def __init__(self, name, sourceAstNode = None, comment=''):
        HierCfgNode.__init__(self, sourceAstNode, comment)
        self.name = name
        self.args = []
        # add method to dict in current class scope
        parent = BaseCfgNode._nodeStack[-2]
        parent.methods[self.name] = self
        #print('creating method node, name=', self.name)
    def whatami(self):
        return 'method'
    def display(self, indent = 0):
        inParms = self.getInputList()
        self.hierDisplay(indent, f"method: {self.name}, vars=({', '.join(str(e) for e in self.vars.values())}), inputs=({', '.join(str(e) for e in inParms.values())})")
    def generateOutput(self):
        ''' generate specified output for this method node '''
        #print(f'CfgMethodNode generateOutput: called in {type(self)}')
        BaseCfgNode._outBuilder.enterMethod(self)
        for child in self.children:
            child.generateOutput()
        BaseCfgNode._outBuilder.exitMethod(self)
        
@unique
class ConfigAssignType(Enum):
    UNSUPPORTED = 0
    EQ = 1
    def isSupported(self):
        return type(self) is not ConfigAssignType.UNSUPPORTED
    @staticmethod
    def resolve(opStr):
        ''' convert a string to ConfigAssignType '''
        if type(opStr) is ConfigAssignType: # if type is already correct, just return input
            return opStr
        if opStr == '=':
            return ConfigAssignType.EQ
        else:
            return ConfigAssignType.UNSUPPORTED

class CfgAssign(BaseCfgNode):
    def __init__(self, left=None, op=ConfigAssignType.UNSUPPORTED, right=None, sourceAstNode = None):
        BaseCfgNode.__init__(self, sourceAstNode)
        self.op = ConfigAssignType.resolve(op) 
        self.left = left  # TODO - resolve here and remove checks from builder or allow default var create? 
        self.right = right  # maybe pass target type into assign?  or verify type match?
    def isValid(self):
        if self.op.isSupported() and (self.left is not None) and (self.right is not None):
            return True
        return False     
    def isRead(self):
        ''' return True if assign involves a reg read '''
        return (type(self.right) is CfgReadNode)
    def display(self, indent = 0):
        self.hierDisplay(indent, f'assign: {self.left} {self.op.name} {self.right}')
    def resolvePaths(self):
        if self.isRead():
            self.right.resolvePaths()


class CfgMethodCall(BaseCfgNode):
    def __init__(self, className, methodName, parmList, sourceAstNode = None):
        BaseCfgNode.__init__(self, sourceAstNode)
        # if className specified in call path resolve class as a variable, else use current class
        if className:
            cfgClassVar = CfgVariable.resolveRhsExpression(className, CfgClassNode, False, True)  # find the class variable
            #self.cfgClass = CfgClassNode.getCurrent()  # TODO add findVar option for non-none className
            self.cfgClass = CfgClassNode.findClass(cfgClassVar.val[0].name)  # TODO - saved call name structure shoul be fixed
        else:
            self.cfgClass = CfgClassNode.getCurrent()
        #if not cfgClass:
        #    MsgUtils.errorExit('unable to resolve cfgClass ' + str(className) + ' in call of method ' + methodName)
        self.cfgMethod = self.cfgClass.findMethod(methodName)
        if not self.cfgMethod:
            MsgUtils.errorExit(f'unable to resolve method {methodName} in cfgClass {self.cfgClass.name}')
        self.parmList = self.cfgMethod.verifyInputParms(parmList, self.parent)
    def display(self, indent = 0):
        self.hierDisplay(indent, f'call: cfgClass={self.cfgClass.name}, method={self.cfgMethod.name}, parms={self.parmList}')
        
class CfgCaseNode(HierCfgNode):
    def __init__(self, selectVar, sourceAstNode = None): 
        HierCfgNode.__init__(self, sourceAstNode)
        self.selectVar = HierCfgNode.findVar(self, selectVar)
        #print('creating case node, select var=' + str(self.selectVar))
    def display(self, indent = 0):
        self.hierDisplay(indent, f'case: select var={self.selectVar}')
        
class CfgCaseBlockNode(HierCfgNode):
    _currentChoices = set()  # init current choice set
    def __init__(self, sourceAstNode = None): 
        HierCfgNode.__init__(self, sourceAstNode)
        self.selectVals = set(__class__._currentChoices) # copy current set of choices
        __class__._currentChoices.clear() # clear current choices
        #print('creating case block node, choices=' + str(self.selectVals))
    def display(self, indent = 0):
        self.hierDisplay(indent, f'case block: choices={self.selectVals}')
    @staticmethod
    def addChoice(choiceName):
        __class__._currentChoices.add(choiceName)
        
class CfgNumericForNode(HierCfgNode):
    def __init__(self, name, rangeStart, rangeEnd, sourceAstNode = None): 
        HierCfgNode.__init__(self, sourceAstNode)
        self.forVar = CfgVariable(name, CfgNumDataType)
        self.rangeStart = CfgVariable.resolveRhsExpression(rangeStart, CfgNumDataType)
        self.rangeEnd = CfgVariable.resolveRhsExpression(rangeEnd, CfgNumDataType)
        #print('creating numeric for loop node, iterator var=' + str(self.forVar) + ' rangeStart=' + str(self.rangeStart) + ' rangeEnd=' + str(self.rangeEnd))
    def display(self, indent = 0):
        self.hierDisplay(indent, f'for (numeric): iterator={self.forVar} rangeStart={self.rangeStart} rangeEnd={self.rangeEnd}')
        
class CfgPathForNode(HierCfgNode):
    def __init__(self, name, path, sourceAstNode = None): 
        HierCfgNode.__init__(self, sourceAstNode)
        self.forVar = CfgVariable(name, CfgPathDataType)
        self.path = CfgVariable.resolveRhsExpression(path, CfgPathDataType) # create path range
        self.forVar.val = self.path   # assign path to loop var so full path prefix can be extracted recursively using var
        #print('creating path for loop node, iterator var=' + str(self.forVar) + ' path=' + str(self.path))
    def display(self, indent = 0):
        self.hierDisplay(indent, f'for (path): iterator={self.forVar}, range={self.path}')
    def resolvePaths(self):
        ''' resolve paths in this for node '''
        print(f'resolve CfgPathForNode path: {self.path}')  # TODO
        if type(self.path) is CfgPathDataType:
            self.path.resolvePath(self.allowedTags)  #TODO - any checks for a var?, how is version resolve handled?
        # resolve paths in child nodes
        for child in self.children:
            child.resolvePaths()

class CfgPrintNode(BaseCfgNode):
    def __init__(self, form, form_vars, sourceAstNode = None):
        BaseCfgNode.__init__(self, sourceAstNode)
        self.form = form   # form can also be a list of comma separated args
        self.form_vars = form_vars
        #print('creating display node, form=', self.form, 'form_vars=', self.form_vars)
    def display(self, indent = 0):
        self.hierDisplay(indent, 'print: ' + str(self.form) + ', vars=' + str(self.form_vars))
        
class CfgWaitNode(BaseCfgNode):
    def __init__(self, time, sourceAstNode = None):
        BaseCfgNode.__init__(self, sourceAstNode)
        self.time = time   # time in ms
        #print('creating wait node, time=', self.time)
    def display(self, indent = 0):
        self.hierDisplay(indent, 'wait: ' + str(self.time))
        
class CfgWriteNode(BaseCfgNode):
    def __init__(self, path, value, wtype, isRmw = False, sourceAstNode = None):
        BaseCfgNode.__init__(self, sourceAstNode)
        self.path = CfgVariable.resolveRhsExpression(path, CfgPathDataType)
        self.wtype = CfgPathHierType.resolve(wtype)
        self.value = CfgVariable.resolveRhsExpression(value, CfgNumDataType)
        self.isRmw = isRmw
        #print('creating write node, path=', str(self.path), 'value=', str(self.value))
    def display(self, indent = 0):
        self.hierDisplay(indent, 'write: ' + str(self.path) + ', wtype=' + str(self.wtype) + ', value=' + str(self.value) + ', rmw=' + str(self.isRmw))
        pass
    def resolvePaths(self):
        ''' resolve paths in this write node '''
        print(f'resolve CfgWriteNode path: {self.path}, wtype: {self.wtype}, rwm: {self.isRmw} --- self.path type={type(self.path)}')  # TODO
        if type(self.path) is CfgPathDataType:
            self.path.resolvePath(self.allowedTags, self.wtype)  #TODO - any checks for a var?, how is version resolve handled?
    def generateOutput(self):
        ''' generate specified output for this write node '''
        #print(f'CfgWriteNode generateOutput: called in {type(self)}')
        if self.wtype.isReg():
            BaseCfgNode._outBuilder.doRegWrite(self)
        else:
            BaseCfgNode._outBuilder.doFieldWrite(self)
        
class CfgWhileNode(HierCfgNode):
    def __init__(self, compare, delay = 1, timeout = None, sourceAstNode = None): 
        HierCfgNode.__init__(self, sourceAstNode)
        self.compare = compare
        self.delay = delay
        self.timeout = timeout
        #print('creating poll node, compare=', self.compare, 'delay=', self.delay)
        CfgWaitNode(self.delay)
    def display(self, indent = 0):
        prefix = 'poll ' if self.compare.isPoll() else ''
        self.hierDisplay(indent, prefix + 'while: ' + str(self.compare) + ' timeout=' + str(self.timeout))
    def isPoll(self):
        ''' return True if compare involves a reg read '''
        return self.compare.isPoll()
    def resolvePaths(self):
        ''' resolve paths in this for while node '''
        if self.isPoll():
            self.compare.resolvePaths()
        for child in self.children:
            child.resolvePaths()
        
# ------- config model support classes (not BaseCfgNode children)

@unique
class CfgPathHierType(Enum):
    UNKNOWN = 0
    REGSET = 1
    REG = 2
    FIELDSET = 3
    FIELD = 4
    @staticmethod
    def resolve(hierStr):
        ''' convert a string to CfgPathHierType '''
        if type(hierStr) is CfgPathHierType: # if type is already correct, just return input
            return hierStr
        if 'RegSet' in hierStr:
            return CfgPathHierType.REGSET
        elif 'FieldSet' in hierStr:
            return CfgPathHierType.FIELDSET
        elif 'Reg' in hierStr:
            return CfgPathHierType.REG
        elif 'Field' in hierStr:
            return CfgPathHierType.FIELD
        else:
            return CfgPathHierType.UNKNOWN
    def isReg(self):
        return self is CfgPathHierType.REG
    def matchesRegModelType(self, regModType):
        if self is CfgPathHierType.UNKNOWN:
            return True
        #print(f'  -> CfgPathHierType matchesRegModelType: self type={self.name}, regModType={regModType.name}')  # TODO
        if self.name == regModType.name:
            return True
        return False
        
class CfgReadNode(): 
    def __init__(self, path, rtype = CfgPathHierType.UNKNOWN, sourceAstNode = None):
        self.path = CfgVariable.resolveRhsExpression(path, CfgPathDataType)
        self.rtype = CfgPathHierType.resolve(rtype)
        self.sourceAstNode = sourceAstNode  # TODO - change to srcInfo
        #print('creating read node, path=', self.path)
    def __str__(self):
        return f'read {self.path}, rtype={self.rtype}'
    def resolvePaths(self):
        ''' resolve paths in this read '''
        print(f'resolve CfgReadNode path: {self.path}, rtype={self.rtype}')  # TODO
        if type(self.path) is CfgPathDataType:
            self.path.resolvePath(set(), self.rtype)  # read node has no allowed tag override, TODO - any checks for a var?, how is version resolve handled?
        
# ------- config model data classes

class CfgDataType():
    def __init__(self):
        pass
    def isValid(self):
        return hasattr(self, 'val') and (self.val is not None)
        
class CfgBoolDataType(CfgDataType):
    def __init__(self):
        pass
        
class CfgNumDataType(CfgDataType):
    def __init__(self, s):
        self.size = None
        self.hasSize = False
        intval = __class__.strToInt(s)
        if intval is not None:
            self.val = intval
    @staticmethod
    def strToInt(s):
        ''' convert str to int if possible, else return None '''
        try: 
            out = int(s, 0)
            return out
        except ValueError:
            return None
    def __str__(self):
        return str(self.val) + (('(size=' + str(self.size) + ')') if self.size else '') if self.isValid() else 'invalid num'
    def needsSize(self):
        return self.hasSize and self.size is None  
                   
class CfgEnumDataType(CfgNumDataType):  # FIXME use separate type
    def __init__(self):
        pass
    
class CfgPathDataElement():
    def __init__(self, pelemstr):
        self.name = None  # invalid if name is None
        self.start = None
        self.end = None
        self.isIndexed = False
        self.hasRange = False  # is element indexed with start not equal to end
        self.annotations = {}
        if '[' in pelemstr:  # detect an array
            self.isIndexed = True
            pat = re.compile('(\\w+)\\s*\\[(.*)\\]')
            mat = pat.match(pelemstr)
            if mat:
                self.name = mat.group(1)
                arraystr = mat.group(2)
                if ':' in arraystr:
                    self.hasRange = True
                    pat = re.compile('(\\w+|\\*)\\s*:\\s*(\\w+|\\*)')
                    mat = pat.match(arraystr)
                    if mat:
                        leftstr = mat.group(1)
                        rightstr = mat.group(2)
                        if leftstr == '*':
                            self.hasRange = True
                        else:
                            self.start = leftstr
                        if rightstr == '*':
                            self.hasRange = True
                        else:
                            self.end = rightstr
                    #else:
                    #    print('CfgPathDataElement array match failed for s=' + arraystr)
                elif '*' in arraystr:  # detect full range wildcard
                    self.hasRange = True 
                else:
                    self.start = arraystr #  single index case
                    self.end = arraystr
        else:
            self.name = pelemstr  # scalar, so just save the name
    def isVar(self):
        ''' return true if this path element is a path variable '''
        return hasattr(self, 'baseVar')
    def isRootVar(self):
        ''' return true if this path element is a path variable representing root of the reg model '''
        return self.isVar() and (self.name == 'root')
    def needsResolution(self):
        return self.isIndexed and ((self.start is None) or (self.end is None))
    def getElementString(self, unrollBase, leftIdx, rightIdx=None):
        if unrollBase and self.isVar() and not self.isRootVar():
            return self.baseVar.val.genFullPathStr()
        if not self.isIndexed:
            return self.name
        if not rightIdx or (rightIdx == leftIdx):
            return f'{self.name}[{leftIdx}]'
        return f'{self.name}[{leftIdx}:{rightIdx}]'
    def getFullElementString(self):   # TODO
        ''' return full element string '''
        startStr = str(self.start) if self.start else '*'
        endStr = str(self.end) if self.end else '*'
        return self.getElementString(True, startStr, endStr)
    def getRawElementString(self):
        ''' return raw element string '''
        startStr = str(self.start) if self.start else '*'
        endStr = str(self.end) if self.end else '*'
        return self.getElementString(False, startStr, endStr)
    def getSampleElementString(self):
        ''' return sample element string for model lookup with indices set to 0 '''
        return self.getElementString(True, 0)
    def __str__(self):
        return self.getRawElementString()
   
class CfgPathDataType(CfgDataType):
    def __init__(self, pathstr):
        self.htype = CfgPathHierType.UNKNOWN  # resolved path type is unknown by default
        self.call = None  # default to no call
        basepathstr = ''
        if '(' in pathstr:  # detect a call and remove from path
            pat = re.compile('(.*)\\.(\\w+)')
            mat = pat.match(pathstr)
            if mat:
                basepathstr = mat.group(1)
                self.call = mat.group(2)
                #print(f'found call match path={self.val}, call={self.call}')
        else:
            basepathstr = pathstr  # TODO - store as path elem tuples?   also TODO allow range wildcards
        # create a list of path elements
        self.val = []
        newlist = basepathstr.split('.')
        for elemstr in newlist:
            elem = CfgPathDataElement(elemstr)
            self.val.append(elem)
        # check for valid path var extract
        if not self.val:
            MsgUtils.errorExit(f'unable create path from string={pathstr}')
        firstPathElement = self.getBasePathElem()
        # check for valid path base variable
        baseVar = CfgVariable.resolveLhsExpression(firstPathElement.name, CfgPathDataType, False, False) # check for existing base path variable
        if not baseVar:
            MsgUtils.errorExit(f'unable to resolve root of path {pathstr}')
        firstPathElement.baseVar = baseVar  # save the referenced path variable in first element
    def genFullPathStr(self):
        ''' return path with base var unrolled '''
        return '.'.join([ elem.getFullElementString() for elem in self.getPathList() ])
    def genRawPathStr(self):
        ''' return raw path (no base var unroll) '''
        return '.'.join([ elem.getRawElementString() for elem in self.getPathList() ])
    def genSamplePathStr(self):
        ''' return sample path for model lookup with all indices set to 0 '''
        return '.'.join([ elem.getSampleElementString() for elem in self.getPathList() ])
    def hasCall(self):
        return self.call is not None
    def setRegset(self):
        self.htype = CfgPathHierType.REGSET
    def setReg(self):
        self.htype = CfgPathHierType.REG
    def setFieldset(self):
        self.htype = CfgPathHierType.FIELDSET
    def setField(self):
        self.htype = CfgPathHierType.FIELD
    def getBasePathElem(self):
        ''' return the base path element '''
        return self.getPathList()[0]
    def getBasePathVar(self):
        ''' return the base path variable '''
        return self.getBasePathElem().baseVar
    def needsResolution(self):
        if not self.getBasePathVar(): # or self.getBasePath().needsResolution():  # TODO - need variable needsResolution method?
            return True
        for elem in self.getPathList(): # check to see if any path elems are unresolved
            if elem.needsResolution():
                return True
        return False
    def isMultiPath(self):
        for elem in self.getPathList(): # check to see if any path elems have more than single element range
            if elem.hasRange:
                return True
        return False
    def resolvePath(self, allowedTags, targetType=CfgPathHierType.UNKNOWN):  # TODO also pass in allowedTags
        ''' resolve path type and any path index wildcards by referencing the regmodel '''
        print(f'  -> resolvePath CfgPathDataType raw path: {self} full path: {self.genFullPathStr()} sample path: {self.genSamplePathStr()}')  # TODO
        regModel = RegModelWrapper.getRegModelRoot()
        if not regModel:
            if self.needsResolution():
                MsgUtils.errorExit(f'Path {self} has unresolved info, but no register model is defined.')
            return # if no model and resolved we're done
        # extract valid version tags and annotate path elements for each
        validTags = RegModelWrapper.getValidTags(allowedTags)
        print(f'  -> resolvePath CfgPathDataType: allowedTags={allowedTags}, regmod tags: {RegModelWrapper.getRegModelTags()} valid tags: {validTags}')  # TODO
        for tag in validTags:
            plist = regModel.get_path_instance_list(tag, self.genSamplePathStr())
            if 'error' in plist:
                MsgUtils.errorExit(f'Path {self.genRawPathStr()} was not found in register model using tag="{tag}".')
            if not targetType.matchesRegModelType(plist['type']):  # check that path type returned from model matches target
                MsgUtils.errorExit(f'Expected type of path {self.genRawPathStr()} ({targetType}) does not match returned register model type ({plist["type"]}).')
            # TODO - check that MultPath elems are allowed
            self.annotatePath(tag, plist['instances'])
        #print(f'  -> resolvePath CfgPathDataType model returns: {plist}') 
    def annotatePath(self, tag, regModelPath):
        # extract the full path by expanding lead path vars
        expandedPath = self.getExpandedPathList()
        #print(f'  -> CfgPathDataType annotatePath: this path len={len(self.getPathList())}, expanded path len={len(expandedPath)}, regmod path len={len(regModelPath)}, path={regModelPath}')
        if len(expandedPath) != len(regModelPath):
            MsgUtils.errorExit(f'Path {self.genRawPathStr()} does not match form of returned register model path.')
        # now loop and append regmodel info to local (non expanded) path elements
        localIndex = len(expandedPath) - len(self.getPathList())
        for pathElem, regModElem in zip(self.getPathList(), regModelPath[localIndex:]):  # only annotate local path elements
            print(f'  -> CfgPathDataType annotatePath: element annotation, tag={tag}, elem={pathElem.name}, mod elem type={type(regModElem)}')
            annotation = RegModelWrapper.createAnnotation(regModElem)
            pathElem.annotations[tag] = annotation   # annotate pathElem by tag
    def getPathList(self):
        ''' return non-expanded path list '''
        return self.val
    def getExpandedPathList(self):
        ''' generate full path list by unrolling base path variable '''
        if self.getBasePathElem().isRootVar():
            return self.getPathList()
        else:
            if len(self.getPathList()) > 1:
                return self.getBasePathElem().baseVar.val.getExpandedPathList() + self.getPathList()[1:]  # remove lead element and append remainder
            else:
                return self.getBasePathElem().val.getExpandedPathList()
    def __str__(self):
        return f'ptype={self.htype.name}, path={self.genRawPathStr()}, needsResolution={self.needsResolution()}'        

# ------- variable classes

class CfgVariable:
    def __init__(self, name, vartype = CfgNumDataType):
        self.name = name
        self.vartype = vartype
        self.val = None
        # add var in current scope
        parent = BaseCfgNode._nodeStack[-1]
        if parent.findVar(self.name):
            MsgUtils.errorExit('variable ' + self.name + ' is already defined.')
        if not name.isalnum():
            MsgUtils.errorExit('variable name ' + self.name + ' is not valid.')
        parent.vars[self.name] = self
        #print (f'--- cfg_model CfgVariable: adding var {self.name}, parent type is {type(parent)}')
    def __str__(self):
        return self.vartype.__name__ + ' ' + self.name
    @staticmethod
    def resolveRhsExpression(inVal, targetVarType, allowInstCreate = True, exitOnFail = True):  # targetVarType is valid CfgDataType
        ''' given an unknown rhs expression, return an existing variable or instance (new from str or existing) of specified target data type '''
        if type(inVal) is targetVarType:  # already target type so done
            return inVal 
        if (type(inVal) is CfgVariable) and (inVal.vartype is targetVarType):  # already a variable so done
            return inVal 
        if type(inVal) is str:
            # try to find an existing variable
            foundVar = HierCfgNode.peekNode().findVar(inVal)
            if (foundVar is not None) and (foundVar.vartype is targetVarType):
                return foundVar
            # else try creating new target instance
            if allowInstCreate:
                newVal = targetVarType(inVal)
                if newVal.isValid():
                    return newVal
            if exitOnFail:
                MsgUtils.errorExit('unable to resolve rhs expression ' + str(inVal) + ' to a value or variable.')
    @staticmethod
    def resolveLhsExpression(inVar, targetVarType, allowVarCreate = True, exitOnFail = True):  # targetVarType is valid CfgDataType
        ''' given an unknown lhs expression, return an existing variable or create a new variable of specified target data type from str '''
        if (type(inVar) is CfgVariable) and (inVar.vartype is targetVarType):  # already a variable so done
            return inVar 
        if type(inVar) is str:
            # try to find an existing (non-input) variable
            foundVar = HierCfgNode.peekNode().findVar(inVar, False) # input variables are not allowed on lhs
            if (foundVar is not None) and (foundVar.vartype is targetVarType):
                return foundVar
            # else create a new var of target type
            if allowVarCreate:
                return CfgVariable(inVar, targetVarType)
            if exitOnFail:
                MsgUtils.errorExit('unable to resolve lhs expression ' + str(inVar) + ' to a variable.')
        
class CfgInputVariable(CfgVariable):
    def __str__(self):
        return 'input ' + self.vartype.__name__ + ' ' + self.name

# ------- config model compare class

@unique
class ConfigCompareType(Enum):
    UNSUPPORTED = 0
    EQ = 1
    NE = 2
    GT = 3
    LT = 4
    GE = 5
    LE = 6
    def isSupported(self):
        return type(self) is not ConfigCompareType.UNSUPPORTED
    @staticmethod
    def resolve(opStr):
        ''' convert a string to ConfigCompareType '''
        if type(opStr) is ConfigCompareType: # if type is already correct, just return input
            return opStr
        if opStr == '==':
            return ConfigCompareType.EQ
        elif opStr == '!=':
            return ConfigCompareType.NE
        elif opStr == '>':
            return ConfigCompareType.GT
        elif opStr == '<':
            return ConfigCompareType.LT
        elif opStr == '>=':
            return ConfigCompareType.GE
        elif opStr == '<=':
            return ConfigCompareType.LE
        else :
            return ConfigCompareType.UNSUPPORTED

class CfgCompare():
    def __init__(self, left=None, op=ConfigCompareType.UNSUPPORTED, right=None):
        self.op = op if type(op) is ConfigCompareType else ConfigCompareType.resolve(op)
        self.left = left if type(left) is CfgReadNode else left # TODO - extract into val or variable
        self.right = right if type(right) is CfgReadNode else right # TODO - extract into val or variable 
    def isValid(self):
        if self.op.isSupported() and (self.left is not None) and (self.right is not None):
            return True
        return False     
    def leftIsPoll(self):
        return type(self.left) is CfgReadNode
    def rightIsPoll(self):
        return type(self.right) is CfgReadNode
    def isPoll(self):
        ''' return True if compare involves a reg read '''
        return self.leftIsPoll() or self.rightIsPoll()
    def __str__(self):
        return f'l=({self.left}) op={self.op.name} r=({self.right})'     
    def resolvePaths(self):
        ''' resolve paths in this compare node '''
        if self.leftIsPoll():
            self.left.resolvePaths()
        if self.rightIsPoll():
            self.right.resolvePaths()

# ------ config model visitor   TODO
