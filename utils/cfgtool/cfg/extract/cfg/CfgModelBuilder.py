#!/usr/bin/python3
'''
cg file parse to config model

@author: snellenbach
'''
from ConfigListener import ConfigListener
from ConfigParser import ConfigParser
from cfg.model import CfgModel as Cm

class CfgModelBuilder(ConfigListener):
    def __init__(self):
        self.lastComment = None  # save last comment seen
    
    # ------ ConfigListener override methods
    
    # Enter a parse tree produced by ConfigParser#root.
    def enterRoot(self, ctx:ConfigParser.RootContext):
        self.rootCfgNode = Cm.HierCfgNode()  # add root node to model

    # Exit a parse tree produced by ConfigParser#root.
    def exitRoot(self, ctx:ConfigParser.RootContext):
        pass

    # Enter a parse tree produced by ConfigParser#class_def.
    #class_def
    #  : 'configClass' id_str parameter_list_def
    #    LBRACE
    #      version_check_statement?
    #      method_def*
    #    RBRACE
    def enterClass_def(self, ctx:ConfigParser.Class_defContext):
        name = ctx.id_str().getText()
        #print("enter class_def: " + name) 
        classNd = Cm.CfgClassNode(name)  # add class node to model
        self.addComment(classNd)  # extract comment

    # Exit a parse tree produced by ConfigParser#class_def.
    def exitClass_def(self, ctx:ConfigParser.Class_defContext):
        #print("exit class_def: " + ctx.getChild(1).getText()) 
        Cm.BaseCfgNode.finishNode(False) # pop node from active stack 
        self.lastComment = None  # clear active comment

    # Enter a parse tree produced by ConfigParser#ml_comment.
    def enterMl_comment(self, ctx:ConfigParser.Ml_commentContext):
        self.lastComment = ctx.getText()[2:-2]  # remove comment markers

    # ------ input parameter defines 
    
    # Enter a parse tree produced by ConfigParser#path_def.
    #path_def
    #  : 'path' id_str
    def enterPath_def(self, ctx:ConfigParser.Path_defContext):
        Cm.CfgInputVariable(ctx.id_str().getText(), Cm.CfgPathDataType)
        #print("enter path var def: " + ctx.getChild(0).getText() + " " + ctx.getChild(1).getText()) 

    # Enter a parse tree produced by ConfigParser#enum_def.
    #enum_def
    #  : 'enum' id_str
    def enterEnum_def(self, ctx:ConfigParser.Enum_defContext):
        Cm.CfgInputVariable(ctx.id_str().getText(), Cm.CfgEnumDataType)
        #print("enter enum var def: " + ctx.getChild(0).getText() + " " + ctx.getChild(1).getText()) 

    # Enter a parse tree produced by ConfigParser#bool_def.
    #bool_def
    #  : 'bool' id_str
    def enterBool_def(self, ctx:ConfigParser.Bool_defContext):
        Cm.CfgInputVariable(ctx.id_str().getText(), Cm.CfgBoolDataType)
        #print("enter bool var def: " + ctx.getChild(0).getText() + " " + ctx.getChild(1).getText()) 

    # Enter a parse tree produced by ConfigParser#value_def.
    #value_def
    #  : 'val' id_str
    def enterValue_def(self, ctx:ConfigParser.Value_defContext):
        Cm.CfgInputVariable(ctx.id_str().getText(), Cm.CfgNumDataType)
        #print("enter value var def: " + ctx.getChild(0).getText() + " " + ctx.getChild(1).getText()) 

    # ------  

    # Enter a parse tree produced by ConfigParser#block_statement.
    def enterBlock_statement(self, ctx:ConfigParser.Block_statementContext):
        pass

    # Exit a parse tree produced by ConfigParser#block_statement.
    def exitBlock_statement(self, ctx:ConfigParser.Block_statementContext):
        pass

    # Enter a parse tree produced by ConfigParser#case_statement.
    # case_statement
    #   : 'case' LPAREN id_str RPAREN
    #     LBRACE 
    #       labeled_case_block+ 
    #       default_case_block
    #     RBRACE
    def enterCase_statement(self, ctx:ConfigParser.Case_statementContext):
        selectVarName = ctx.id_str().getText()
        Cm.CfgCaseNode(selectVarName)
        #print('enterCase_statement: children=' + str(ctx.getChildCount()) + ", labeled_case_block()=" + str(ctx.labeled_case_block()))
            
    # Exit a parse tree produced by ConfigParser#case_statement.
    def exitCase_statement(self, ctx:ConfigParser.Case_statementContext):
        Cm.BaseCfgNode.finishNode(False) # pop node from active stack

    # Enter a parse tree produced by ConfigParser#labeled_case_block.
    #  labeled_case_block
    #   : id_str COLON block_statement*
    def enterLabeled_case_block(self, ctx:ConfigParser.Labeled_case_blockContext):
        choiceValName = ctx.id_str().getText()
        Cm.CfgCaseBlockNode.addChoice(choiceValName)
        #print('enterLabeled_case_block: choice=' + choiceValName + ', choices=' + str(Cm.CfgCaseBlockNode._currentChoices))
        if ctx.block_statement():  # if block has statements, these apply to all preceeding choices
            Cm.CfgCaseBlockNode()

    # Exit a parse tree produced by ConfigParser#labeled_case_block.
    def exitLabeled_case_block(self, ctx:ConfigParser.Labeled_case_blockContext):
        if ctx.block_statement():  # if block has statements, a case block was created
            Cm.BaseCfgNode.finishNode(False) # pop node from active stack

    # Enter a parse tree produced by ConfigParser#default_case_block.
    # default_case_block
    #   : 'default' COLON block_statement*
    def enterDefault_case_block(self, ctx:ConfigParser.Default_case_blockContext):
        Cm.CfgCaseBlockNode.addChoice('default')
        #print('enterDefault_case_block: choice=default, choices=' + str(Cm.CfgCaseBlockNode._currentChoices))
        if ctx.block_statement():  # if block has statements, these apply to all preceeding choices
            Cm.CfgCaseBlockNode()

    # Exit a parse tree produced by ConfigParser#default_case_block.
    def exitDefault_case_block(self, ctx:ConfigParser.Default_case_blockContext):
        if ctx.block_statement():  # if block has statements, a case block was created
            Cm.BaseCfgNode.finishNode(False) # pop node from active stack

    # Enter a parse tree produced by ConfigParser#class_assign.
    # class_assign
    #   : path_elem EQ 'new' id_str LPAREN call_parms RPAREN
    def enterClass_assign(self, ctx:ConfigParser.Class_assignContext):
        varName = ctx.path_elem().getText()
        className = ctx.id_str().getText()
        classParms = ctx.call_parms().getText()
        assignVar = Cm.CfgVariable.resolveLhsExpression(varName, Cm.CfgClassNode) # read return variable - special case using CfgClass rather than data type
        foundClass = Cm.CfgClassNode.findClass(className) # find the class that is being called
        #print('enterClass_assign: var=' + varName + ', cfgClass=' + className + ', parms=' + classParms + ', foundClass=' + foundClass.name)
        # extract parms and check vs class signature
        resolvedInputList = foundClass.verifyInputParms(classParms, Cm.BaseCfgNode.peekNode())
        assignVar.val = (foundClass, resolvedInputList)
        Cm.CfgAssign(assignVar, Cm.ConfigAssignType.EQ, foundClass)

    # Exit a parse tree produced by ConfigParser#class_assign.
    def exitClass_assign(self, ctx:ConfigParser.Class_assignContext):
        pass


    # Enter a parse tree produced by ConfigParser#for_loop.
    # for_loop
    #   : 'for' LPAREN id_str COLON (num_range | path) RPAREN
    #     LBRACE block_statement* RBRACE
    # num_range
    #   : value DBLDOT value
    def enterFor_loop(self, ctx:ConfigParser.For_loopContext):
        iterName = ctx.id_str().getText()
        isNumeric = ctx.num_range() is not None
        if isNumeric:
            rangeStart = ctx.num_range().value(0).getText()
            rangeEnd = ctx.num_range().value(1).getText()
            #print('enterFor_loop: numeric var name=' + iterName + ", range_start=" + str(rangeStart)+ ", range_end=" + str(rangeEnd))
            Cm.CfgNumericForNode(iterName, rangeStart, rangeEnd)
        else: # else a path for loop
            path = ctx.path().getText()
            #print('enterFor_loop: path var name=' + iterName + ", path=" + str(path))
            Cm.CfgPathForNode(iterName, path)

    # Exit a parse tree produced by ConfigParser#for_loop.
    def exitFor_loop(self, ctx:ConfigParser.For_loopContext):
        Cm.BaseCfgNode.finishNode(False) # pop node from active stack

    # Enter a parse tree produced by ConfigParser#method_call.
    # method_call
    #   : (path DOT)* path_elem LPAREN call_parms RPAREN  # really not a reg path, a class/method call path
    def enterMethod_call(self, ctx:ConfigParser.Method_callContext):
        callName = ctx.path_elem().getText()
        classInstName = ctx.path(0).getText() if ctx.path() else None
        #if not callName.isidentifier():  # TODO
        parmList = ctx.call_parms().getText()
        Cm.CfgMethodCall(classInstName, callName, parmList)
        #print(f'enterMethod_call: name={callName}, prefix={classInstName}, parms={parmList}')

    # Exit a parse tree produced by ConfigParser#method_call.
    def exitMethod_call(self, ctx:ConfigParser.Method_callContext):
        pass

    # Enter a parse tree produced by ConfigParser#method_def.
    #method_def
    #  : 'method' id_str parameter_list_def
    #    LBRACE 
    #      version_check_statement?
    #      block_statement*
    #    RBRACE
    def enterMethod_def(self, ctx:ConfigParser.Method_defContext):
        name = ctx.id_str().getText()
        #print("enter method_def: " + name) 
        methodNd = Cm.CfgMethodNode(name)  # add method node to model
        self.addComment(methodNd)  # extract comment

    # Exit a parse tree produced by ConfigParser#method_def.
    def exitMethod_def(self, ctx:ConfigParser.Method_defContext):
        #print("exit method_def: " + ctx.getChild(1).getText()) 
        Cm.BaseCfgNode.finishNode(False) # pop node from active stack and omit if invalid
        self.lastComment = None  # clear active comment

    # Enter a parse tree produced by ConfigParser#num_range.
    def enterNum_range(self, ctx:ConfigParser.Num_rangeContext):
        pass

    # Exit a parse tree produced by ConfigParser#num_range.
    def exitNum_range(self, ctx:ConfigParser.Num_rangeContext):
        pass


    # Enter a parse tree produced by ConfigParser#path.
    def enterPath(self, ctx:ConfigParser.PathContext):
        pass

    # Exit a parse tree produced by ConfigParser#path.
    def exitPath(self, ctx:ConfigParser.PathContext):
        pass


    # Enter a parse tree produced by ConfigParser#path_elem.
    def enterPath_elem(self, ctx:ConfigParser.Path_elemContext):
        pass

    # Exit a parse tree produced by ConfigParser#path_elem.
    def exitPath_elem(self, ctx:ConfigParser.Path_elemContext):
        pass


    # Enter a parse tree produced by ConfigParser#path_elem_range.
    def enterPath_elem_range(self, ctx:ConfigParser.Path_elem_rangeContext):
        pass

    # Exit a parse tree produced by ConfigParser#path_elem_range.
    def exitPath_elem_range(self, ctx:ConfigParser.Path_elem_rangeContext):
        pass


    # Enter a parse tree produced by ConfigParser#compare_op.
    def enterCompare_op(self, ctx:ConfigParser.Compare_opContext):
        pass

    # Exit a parse tree produced by ConfigParser#compare_op.
    def exitCompare_op(self, ctx:ConfigParser.Compare_opContext):
        pass


    # Enter a parse tree produced by ConfigParser#id_str.
    def enterId_str(self, ctx:ConfigParser.Id_strContext):
        pass

    # Exit a parse tree produced by ConfigParser#id_str.
    def exitId_str(self, ctx:ConfigParser.Id_strContext):
        pass


    # Enter a parse tree produced by ConfigParser#num.
    def enterNum(self, ctx:ConfigParser.NumContext):
        pass

    # Exit a parse tree produced by ConfigParser#num.
    def exitNum(self, ctx:ConfigParser.NumContext):
        pass

    # Enter a parse tree produced by ConfigParser#poll_config_call.
    # poll_config_call
    #  : ('pollRegWhile' | 'pollFieldWhile') LPAREN path compare_op value (COMMA value (COMMA value)? )? RPAREN
    def enterPoll_config_call(self, ctx:ConfigParser.Poll_config_callContext):
        #print('enterPoll_config_call: ')
        readType = ctx.getChild(0).getText()
        readPath = ctx.path().getText()
        pollOp = ctx.compare_op().getText()
        pollVal = ctx.value(0).getText()
        #print(' ' + readType + ' ' + readPath + ' ' + pollOp + ' ' + pollVal)
        rtype = Cm.CfgPathHierType.resolve(readType) # poll is compare of read value, so explicitly set type
        rNode = Cm.CfgReadNode(readPath, rtype, None)
        pVal = Cm.CfgVariable.resolveRhsExpression(pollVal, Cm.CfgNumDataType) # read compares to a numeric var or instance
        whileCompare = Cm.CfgCompare(rNode, pollOp, pVal)
        Cm.CfgWhileNode(whileCompare)

    # Exit a parse tree produced by ConfigParser#poll_config_call.
    def exitPoll_config_call(self, ctx:ConfigParser.Poll_config_callContext):
        Cm.BaseCfgNode.finishNode(False) # pop node from active stack and omit if invalid

    # Enter a parse tree produced by ConfigParser#read_config_call.
    # read_config_call
    #  : id_str EQ ('readReg' | 'readField') LPAREN path RPAREN
    def enterRead_config_call(self, ctx:ConfigParser.Read_config_callContext):
        #print('enterRead_config_call: ')
        readType = ctx.getChild(2).getText()
        readPath = ctx.path().getText()
        readVar = ctx.id_str().getText()
        #print(' ' + readType + ' ' + readPath + ' ' + readVar)
        rNode = Cm.CfgReadNode(readPath, readType, None)
        #print('enterRead_config_call: findVar name=' + readVar + ", ret=" + str(Cm.BaseCfgNode.peekNode().findVar(readVar, True)))
        rVar = Cm.CfgVariable.resolveLhsExpression(readVar, Cm.CfgNumDataType) # read return variable
        Cm.CfgAssign(rVar, Cm.ConfigAssignType.EQ, rNode)

    # Exit a parse tree produced by ConfigParser#read_config_call.
    def exitRead_config_call(self, ctx:ConfigParser.Read_config_callContext):
        pass

    # Enter a parse tree produced by ConfigParser#simple_assign.
    # simple_assign
    #   : id_str EQ value
    def enterSimple_assign(self, ctx:ConfigParser.Simple_assignContext):
        varName = ctx.id_str().getText()
        rhsValName = ctx.value().getText()
        assignVar = Cm.CfgVariable.resolveLhsExpression(varName, Cm.CfgNumDataType)  # TODO assign numeric only for now
        rhsVal = Cm.CfgVariable.resolveRhsExpression(rhsValName, Cm.CfgNumDataType)  # TODO assign numeric only for now
        #print('enterSimple_assign: var=' + varName + ', rhsValName=' + rhsValName)
        Cm.CfgAssign(assignVar, Cm.ConfigAssignType.EQ, rhsVal) 

    # Exit a parse tree produced by ConfigParser#simple_assign.
    def exitSimple_assign(self, ctx:ConfigParser.Simple_assignContext):
        pass

    # Enter a parse tree produced by ConfigParser#value.
    def enterValue(self, ctx:ConfigParser.ValueContext):
        pass

    # Exit a parse tree produced by ConfigParser#value.
    def exitValue(self, ctx:ConfigParser.ValueContext):
        pass

    # Enter a parse tree produced by ConfigParser#version_check_statement.
    # version_check_statement
    #  : 'allowVersions' LBRACE id_str (COMMA id_str)* RBRACE
    def enterVersion_check_statement(self, ctx:ConfigParser.Version_check_statementContext):
        versionSet = set(vers.getText() for vers in ctx.id_str())
        #print("enter version check: " + str(versionSet)) 
        current = Cm.BaseCfgNode.peekNode()
        current.allowedTags = versionSet
        #pass

    # Exit a parse tree produced by ConfigParser#version_check_statement.
    def exitVersion_check_statement(self, ctx:ConfigParser.Version_check_statementContext):
        pass

    # Enter a parse tree produced by ConfigParser#while_loop.
    # while_loop
    #   : 'while' LPAREN value compare_op value RPAREN
    #     LBRACE block_statement* RBRACE
    def enterWhile_loop(self, ctx:ConfigParser.While_loopContext):
        #print('enterWhile_loop: ')
        leftOp = ctx.value(0).getText()
        rightOp = ctx.value(1).getText()
        compareOp = ctx.compare_op().getText()
        #print(' ' + compareOp + ' ' + rightOp)
        leftVal = Cm.CfgVariable.resolveRhsExpression(leftOp, Cm.CfgNumDataType) # numeric var or instance
        rightVal = Cm.CfgVariable.resolveRhsExpression(rightOp, Cm.CfgNumDataType) # numeric var or instance
        whileCompare = Cm.CfgCompare(leftVal, compareOp, rightVal)
        Cm.CfgWhileNode(whileCompare)

    # Exit a parse tree produced by ConfigParser#while_loop.
    def exitWhile_loop(self, ctx:ConfigParser.While_loopContext):
        Cm.BaseCfgNode.finishNode(False) # pop node from active stack and omit if invalid

    # Enter a parse tree produced by ConfigParser#write_config_call.
    #write_config_call
    #  : ('writeReg' | 'writeField' | 'rmwField') LPAREN path COMMA value RPAREN
    def enterWrite_config_call(self, ctx:ConfigParser.Write_config_callContext):
        #print('enterWrite_config_call: ')
        writeType = ctx.getChild(0).getText()
        writePath = ctx.path().getText()
        writeVal = ctx.value().getText()
        #print(' ' + writeType + ' ' + writePath + ' ' + writeVal)
        isRmw = True if writeType == 'rmwField' else False
        Cm.CfgWriteNode(writePath, writeVal, writeType, isRmw, None)

    # Exit a parse tree produced by ConfigParser#write_config_call.
    def exitWrite_config_call(self, ctx:ConfigParser.Write_config_callContext):
        pass
    
    # -------- support methods
          
    def addComment(self, modNode):
        ''' add current comment to specified model node and clear the active comment ''' 
        if self.lastComment is not None:
            modNode.comment = self.lastComment
            self.lastComment = None
            #print("CfgModelBuilder addComment: " + modNode.comment) 

