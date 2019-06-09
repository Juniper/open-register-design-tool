#!/usr/bin/python3
'''
@author: snellenbach

Python config form 1 ast visitor.  Builds a generic config model
'''
import ast
import astor
from cfg.model import CfgModel as Cm
import re

class PyF1NodeVisitor(ast.NodeVisitor):  # add generic ast parse class
    showNodes = False
    
    input_config = { 'regs_root' : 'ea' }  # expressions starting with this string will be considered register model hierarchy
    
    def __init__(self):
        self.rootCfgNode = Cm.HierCfgNode()  # add root node to model
    
    # ast.NodeVisitor overrides
    
    def generic_visit(self, node):
        if __class__.showNodes:
            print('generic_visit: found node...', type(node).__name__)
        #if hasattr(node, '_fields'):
        ast.NodeVisitor.generic_visit(self, node)

    def visit_Assign(self, node):
        #print(type(node).__name__, 'found:')
        match = __class__.extract_Assign(node)
        #__class__.showNodes = True
        if not match:
            ast.NodeVisitor.generic_visit(self, node)  # visit children if no match
        __class__.showNodes = False
                
    def visit_ClassDef(self, node):
        #print(type(node).__name__, 'found:', node.name)
        classNd = Cm.CfgClassNode(node.name, node)  # add class node to model
        classNd.comment = __class__.pyF1GetComment(node) # extract comment
        ast.NodeVisitor.generic_visit(self, node)
        Cm.BaseCfgNode.finishNode(False) # pop node from active stack 
        
    def visit_Expr(self, node):
        #print(type(node).__name__, 'found:')
        match = __class__.extract_Expr(node)
        #__class__.showNodes = True
        if not match:
            ast.NodeVisitor.generic_visit(self, node)  # visit children if no match
        __class__.showNodes = False
        
    def visit_FunctionDef(self, node):
        #print(type(node).__name__, 'found:', node.name)
        methodNd = Cm.CfgMethodNode(node.name, node)  # add method node to model
        methodNd.comment = self.pyF1GetComment(node) # extract comment
        self.pyF1AddFunctionArgs(node) # extract arguments
        ast.NodeVisitor.generic_visit(self, node)
        omit = __class__.hasInvalidChildren(node)
        if omit:
            print('python ' + type(node).__name__ + ' name=' + node.name + ' could not be converted to config model\n')
        Cm.BaseCfgNode.finishNode(omit) # pop node from active stack and omit if invalid
        
    def visit_While(self, node):       
        #print(type(node).__name__, 'found:')
        whileNd = self.pyF1ToWhile(node) # extract while (without delay, print options)
        if not whileNd:
            return
        ast.NodeVisitor.generic_visit(self, node)
        omit = __class__.hasInvalidChildren(node)  # TODO need timer extract, then extract print and delay only, and mark While node as valid?
        # TODO - need to reformat pollwhile to extract delay and prints here or just treat as a while w/ isPoll tag
        node.__cfg_valid__ = True  # tag this ast node as translated 

        #if omit:
        #    print(type(node).__name__ + ' name=' + node.name + ' could not be parsed\n')
        Cm.BaseCfgNode.finishNode(omit) # pop node from active stack and omit if invalid
        
    # helper methods to extract ast nodes in possible config model options
        
    @staticmethod
    def extract_Expr(node):
        ''' resolve Expr node into possible model nodes (display) - return True on match '''
        # check for a print node
        found = __class__.pyF1GetPrint(node)
        if found:
            return True
        # check for a wait node
        found = __class__.pyF1GetWait(node)
        if found:
            return True
        return False
        
    @staticmethod
    def extract_Assign(node):
        ''' resolve Assign node into possible model nodes (regWrite) '''
        found = __class__.pyF1GetWrite(node) # extract display
        if found:
            return True
        return False
    
    # helper methods
    
    @staticmethod
    def pyF1GetComment(node):
        ''' extract a comment from Expr node - returns string and tags ast node as valid (return empty str if no match) '''
        exprNd = node.body[0]
        if type(exprNd) is not ast.Expr:
            return ''
        strNd = exprNd.value
        if type(strNd) is not ast.Str:
            return ''
        #print('found comment=' + strNd.s)
        exprNd.__cfg_valid__ = True  # tag this ast node as translated 
        return strNd.s
        
    @staticmethod
    def pyF1AddFunctionArgs(node):
        ''' extract argument list from FunctionDef node and add to current model scope'''
        #outArgs = []
        argsNds = node.args.args
        for argsNd in argsNds:
            if argsNd.arg is not 'self':
                #outArgs.append(argsNd.arg)
                #print('PyF1NodeVisitor pyF1AddFunctionArgs ' + argsNd.arg)
                Cm.CfgInputVariable(argsNd.arg, type(None))  # add an untyped input
        #return outArgs
    
    @staticmethod
    def pyF1ToWhile(whileNd):
        ''' extract a reg/field poll from an ast While node - returns new model While node else returns None on no match '''              
        compNd = whileNd.test
        if type(compNd) is not ast.Compare:
            return None
        compare = __class__.pyF1ToCfgCompare(compNd)
        if not compare.isValid():
            return None
        return Cm.CfgWhileNode(compare, whileNd)
        
        
    @staticmethod
    def pyF1ToCfgCompare(compNd):
        ''' extract a compare node - returns new model node else returns None on no match '''              
        left = __class__.pyF1ToCfgData(compNd.left)
        right = __class__.pyF1ToCfgData(compNd.comparators[0])
        op = __class__.pyToCfgCompareOp(compNd)
        #print(type(compNd).__name__, 'left:', left, 'op:', op, 'right:', right)
        if op.isSupported():
            compNd.__cfg_valid__ = True  # tag this ast node as translated 
        return Cm.CfgCompare(left, op, right)  # TODO check ops here before creating 
        
    @staticmethod
    def pyF1ToCfgData(pyNd):
        ''' convert a py node to appropriate config model data type - returns None on no match 
            form match is checked in following order: int, path w/ read, path only '''
        s = __class__.getSourceString(pyNd)  # convert py node to a string
        data = Cm.CfgNumDataType(s)   # try converting to int
        if data.isValid(): 
            return data
        hstr = __class__.getHierString(s)
        if hstr:
            path = Cm.CfgPathDataType(hstr)   # try converting to path
            if path.isValid():
                if path.hasCall():
                    if path.call == '__get__':
                        path.setField() 
                        return Cm.CfgReadNode(path, pyNd);
                    if path.call == '__read__':
                        path.setReg() 
                        return Cm.CfgReadNode(path, pyNd);
                    print('invalid call in strToCfgDataType path.val=' + path.val + ', path.call=' + path.call)
                    return None  # invalid call found
                else:
                    return path
        return None           

    @staticmethod
    def pyF1GetPrint(exprNd):
        ''' extract a print from Expr node - adds node to model, tags ast node as valid, and returns True on match '''
        if type(exprNd) is not ast.Expr:
            return False
        callNd = exprNd.value
        if type(callNd) is not ast.Call:
            return False
        nameNd = callNd.func
        if type(nameNd) is not ast.Name:
            return False
        if nameNd.id is not 'print':
            return False
        argsNds = callNd.args
        argsNd = callNd.args[0]  # TODO - also need to handle case of multiple print args -> form_vars  getName() for each
        # CASE 1 - single format string with args tuple
        if type(argsNd) is ast.BinOp: 
            if len(argsNds) != 1: 
                return False    
            opNd = argsNd.op
            if type(opNd) is not ast.Mod:
                return False
            form = [ argsNd.left.s ]
            form_vars = [__class__.getSourceString(i) for i in argsNd.right.elts]  # convert each arg to a string
            #print("....print form rhs list")
            #print(form_vars)
            Cm.CfgPrintNode(form, form_vars, exprNd)
        # CASE 2 - multi-arg print 
        else:
            form = []
            for argNd in argsNds:
                if type(argNd) is ast.Str:
                    form.append(argNd.s)
                else:
                    form.append(__class__.getSourceString(argsNd))
            form_vars = []
            Cm.CfgPrintNode(form, form_vars, exprNd)
        exprNd.__cfg_valid__ = True  # tag this ast node as translated 
        return True

    @staticmethod
    def pyF1GetWait(exprNd):
        ''' extract a wait from Expr node - adds node to model, tags ast node as valid, and returns True on match '''
        if type(exprNd) is not ast.Expr:
            return False
        callNd = exprNd.value
        if type(callNd) is not ast.Call:
            return False
        callStr = __class__.getSourceString(callNd)
        pat = re.compile('time.sleep\\((\\w+)')
        mat = pat.match(callStr)
        if mat:
            varstr = mat.group(1)
            #print("....found wait, var=" + varstr)
            intvar = Cm.CfgNumDataType(varstr)
            if intvar.isValid():   # TODO allow a var here also
                intvar.val *= 1000 # convert time.sleep value to ms
                Cm.CfgWaitNode(intvar, exprNd)
                exprNd.__cfg_valid__ = True  # tag this ast node as translated 
                return True
        return False
    
    @staticmethod
    def pyF1GetWrite(assignNd):
        ''' extract a reg write from Assign node - adds node to model, tags ast node as valid, and returns True on match '''
        if type(assignNd) is not ast.Assign:
            return False
        # extract assign targets and paths
        #paths = [__class__.getSourceString(i) for i in assignNd.targets]  # convert each target to a string
        if len(assignNd.targets) != 1:  # only 1 lhs allowed per assign
            return False
        path = __class__.pyF1ToCfgData(assignNd.targets[0])
        #print('found write path=' + targ_path)
        value = __class__.getSourceString(assignNd.value)
        if type(path) is Cm.CfgPathDataType:
            Cm.CfgWriteNode(path, value, assignNd)
            assignNd.__cfg_valid__ = True  # tag this ast node as translated 
            return True
        return False

    # generic ast to config methods
        
    @staticmethod
    def pyToCfgCompareOp(compNd):
        ''' convert a python compare operator to corresponding config model op '''              
        if len(compNd.ops) != 1:  # only ingle comp is allowed
            return Cm.ConfigCompareType.UNSUPPORTED
        elif type(compNd.ops[0]) is ast.Eq:
            return Cm.ConfigCompareType.EQ
        elif type(compNd.ops[0]) is ast.NotEq:
            return Cm.ConfigCompareType.NE
        elif type(compNd.ops[0]) is ast.Lt:
            return Cm.ConfigCompareType.LT
        elif type(compNd.ops[0]) is ast.LtE:
            return Cm.ConfigCompareType.LE
        elif type(compNd.ops[0]) is ast.Gt:
            return Cm.ConfigCompareType.GT
        elif type(compNd.ops[0]) is ast.GtE:
            return Cm.ConfigCompareType.GE
        else:
            return Cm.ConfigCompareType.UNSUPPORTED
      
    @staticmethod
    def getSourceString(pyNd):
        ''' use astor to convert an ast node to string '''
        pystr = astor.to_source(pyNd).rstrip('\n)').lstrip('(')
        if pystr.startswith('('):
            pystr = pystr[1:].rstrip(')', 1)
        return pystr
    
    @staticmethod
    def getHierString(inStr):
        ''' detect if string is a reg model hierarchy or returns None '''
        if inStr.startswith('self.' + __class__.input_config['regs_root']):
            return inStr[5:]     # remove self prefix
        return None
                        
    @staticmethod
    def hasInvalidChildren(fnNode):
        ''' return true if any of a function node's children are not tagged as valid '''
        for child in ast.iter_child_nodes(fnNode):
            #print(type(child).__name__ + '...')
            if not hasattr(child, '__cfg_valid__') and type(child).__name__ != 'arguments':
                print('python ' + type(child).__name__ + ' node has unsupported form...')
                print(__class__.getSourceString(child))
                return True
        return False
    

# example
#x = MyNodeVisitor()
#t = ast.parse('d[x] += v[y, x]')
#x.visit(t)