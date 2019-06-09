#!/usr/bin/python3
'''
@author: snellenbach

config model util classes
'''

# ------- message util class

class MsgUtils:
    @staticmethod
    def errorExit(messageStr):
        print ('ERROR: ' + messageStr)
        exit()
    @staticmethod
    def errorMsg(messageStr):
        print ('ERROR: ' + messageStr)
    @staticmethod
    def warnMsg(messageStr):
        print ('WARNING: ' + messageStr)
    @staticmethod
    def infoMsg(messageStr):
        print ('INFO: ' + messageStr)
        