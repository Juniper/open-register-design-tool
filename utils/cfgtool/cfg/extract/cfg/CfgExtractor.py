#!/usr/bin/python3
'''
cg file parse to cfg model

TODO...
- handle parse of ned forms

@author: snellenbach
'''
#import sys
from antlr4 import * # installed via pip3 install antlr4-python3-runtime
from ConfigLexer import ConfigLexer
from ConfigParser import ConfigParser
from cfg.extract.cfg.CfgModelBuilder import CfgModelBuilder

class CfgExtractor:
    def __init__(self, fName):
        in_fs = FileStream(fName)
        lexer = ConfigLexer(in_fs)
        stream = CommonTokenStream(lexer)
        parser = ConfigParser(stream)
        tree = parser.root()
        builder = CfgModelBuilder() # extends ConfigListener
        walker = ParseTreeWalker()
        walker.walk(builder, tree)
        self.modelRoot = builder.rootCfgNode
 
    def getModel(self):
        return self.modelRoot

