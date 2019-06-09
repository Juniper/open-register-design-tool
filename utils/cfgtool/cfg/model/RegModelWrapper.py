#!/usr/bin/python3
'''
@author: snellenbach

Classes for extracted regmodel info
'''
import importlib.util
from cfg.model.Utils import MsgUtils

class RegModelWrapperConfig:
    def __init__(self):  # set config defaults
        self.inhibitAllowedVersionCheck = False   # if true allowed version tags specified in config model will not be checked against tags in regmodel
        self.versionTagOverrideSet = set()        # if non-empty, only this subset of regmodel tags will be processed

class RegModelWrapper:
    _regModTags = set()  # set of version tags supported in register model
    _config = RegModelWrapperConfig()
    _regRoot = None

    @staticmethod
    def importRegModel(pyModelFile):
        ''' read the py register model, store extracted model root instance and version tags '''
        regmodSpec = importlib.util.spec_from_file_location('regmod', pyModelFile)
        regmod = importlib.util.module_from_spec(regmodSpec)
        regmodSpec.loader.exec_module(regmod)
        __class__._regRoot = regmod.ordt_drv_root()
        __class__._regModTags = __class__._regRoot.get_tags()
        if not bool(__class__._regModTags):
            MsgUtils.errorExit(f'No version tags found in extraction of register model from {pyModelFile}')
    
    @staticmethod
    def getAllRegModelTags():
        ''' return all version tags found in the register model '''
        return __class__._regModTags
    
    @staticmethod
    def getRegModelRoot():
        ''' return root instance of the register model '''
        return __class__._regRoot
    
    @staticmethod
    def getRegModelTags():
        ''' return version tags to be used in the register model after applying any overrides '''
        overrides = __class__._config.versionTagOverrideSet
        modeltags = __class__.getAllRegModelTags()
        if not bool(overrides):  # if no overrides just return the full regmodel tag set
            return modeltags  
        # check that the override set is a subset of model
        unsupportedTags = overrides - modeltags
        if bool(unsupportedTags):
            MsgUtils.errorExit(f'versionTagOverrideSet specified includes tags {unsupportedTags} not found in register model list {modeltags}')
        return overrides
    
    @staticmethod
    def checkAllowedVersions(allowedTags):
        ''' issue an error if allowed version Tags specified by config model are not a subset of those in the register model '''
        inhibitCheck = __class__._config.inhibitAllowedVersionCheck
        unsupportedTags = allowedTags - __class__.getAllRegModelTags()
        if bool(unsupportedTags) and not inhibitCheck:
            MsgUtils.errorExit(f'version tags specified {unsupportedTags} are not found in register model list {RegModelWrapper._regModTags}')
        return
    
    @staticmethod
    def getValidTags(allowedTags):
        ''' return intersection of specified allowed tag set and the regmodel tag set including any overrides '''
        modeltags = __class__.getRegModelTags()
        if not bool(allowedTags):  # if no allowed tags specified then use regmodel set
            return modeltags
        __class__.checkAllowedVersions(allowedTags)
        return modeltags | allowedTags
        
    @staticmethod
    def createAnnotation(regModElem):
        ''' return an annotation instance w/ extracted info given a regmodel path element '''
        # if regset or reg
        if hasattr(regModElem, 'child'):
            elemChild = regModElem.child
            # reg
            if hasattr(elemChild, 'width'):
                return RegAnnotation(regModElem.reps, regModElem.offset, regModElem.stride, elemChild.width)
            # regset
            else:
                return RegSetAnnotation(regModElem.reps, regModElem.offset, regModElem.stride)
        # else if a field
        else:
            return FieldAnnotation(regModElem.loidx, regModElem.width, regModElem.reset, regModElem.readable, regModElem.writeable)

# extract regset info from <class 'regmod.ordt_drv_regset_child'> - regmod.ordt_drv_regset, 'child', 'offset', 'reps', 'stride', 'version_map'
class RegSetAnnotation:
    def __init__(self, reps, offset, stride):  # only what can vary by version
        self.reps = reps
        self.offset = offset
        self.stride = stride
        #print (f'createAnnotation RegSetAnnotation: reps={self.reps}, offset={self.offset}, stride={self.stride}')

# extract regset info from <class 'regmod.ordt_drv_regset_child'> - regmod.ordt_drv_reg, 'child', 'offset', 'reps', 'stride', 'version_map'
class RegAnnotation:
    def __init__(self, reps, offset, stride, width):  # only what can vary by version
        self.reps = reps
        self.offset = offset
        self.stride = stride
        self.width = width
        #print (f'createAnnotation RegAnnotation: reps={self.reps}, offset={self.offset}, stride={self.stride}, width={self.width}')

# extract regset info from <class 'regmod.ordt_drv_field'> - 'loidx', 'name', 'readable', 'reset', 'width', 'writeable'
class FieldAnnotation:
    def __init__(self, loidx, width, reset, readable, writeable):  # only what can vary by version
        self.loidx = loidx
        self.width = width
        self.reset = reset
        self.readable = readable
        self.writeable = writeable
        #print (f'createAnnotation FieldAnnotation: loidx={self.loidx}, width={self.width}, reset={self.reset}, readable={self.readable}, writeable={self.writeable}')
