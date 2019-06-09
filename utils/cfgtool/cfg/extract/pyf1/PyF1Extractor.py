#!/usr/bin/python3
'''
Python form 1 parse to cfg model

Created on Nov 27, 2018

TODO...
- add translations to model:
  - handle var assign (paths, integers, data w/ size)
  - translate If node
  - translate For node
  - functions:
    - allow parameters for a defined function / need var type
    - function returns and implicit return for last Expr child
    - call a defined function
- collapse seq of write calls with same path into path wildcards?

@author: snellenbach
'''
import ast 
import astor
from cfg.extract.pyf1.PyF1NodeVisitor import PyF1NodeVisitor

class PyF1Extractor:
    def __init__(self, fName):
        rfile = open(fName,'r')
        #rfile = open(testDir + 'python/wrap_integers.py','r')
        text = rfile.read()
        rfile.close()
        #print(text)
        
        tree = ast.parse(text)
        
        #wfile = open(testDir + '/output/tree_dump.txt','w')
        #wfile.write(astor.dump_tree(tree))
        #wfile.close()
        #print(astor.dump_tree(tree))
        
        # try a visitor
        visitor = PyF1NodeVisitor()
        #t = ast.parse('d[x] += v[y, x]')
        visitor.visit(tree)
        
        # back to python
        #print(astor.to_source(tree))
        
        # display model hier
        if visitor is not None:
            self.modelRoot = visitor.rootCfgNode
    
    def getModel(self):
        return self.modelRoot
        
