import sys
import time
import thread
import argparse
from datetime import datetime
import quickfix as fix

class Application(fix.Application):
    orderID = 0
    execID = 0
    def gen_ord_id(self):
        global orderID
        orderID+=1
        return orderID

    def onCreate(self, sessionID):
        print("onCreate")
        return
    def onLogon(self, sessionID):
        self.sessionID = sessionID
        print ("Successful Logon to session '%s'." % sessionID.toString())
        return

    def onLogout(self, sessionID):
        print("onLogout")
        return

    def toAdmin(self, sessionID, message):
        return

    def fromAdmin(self, sessionID, message):
        return


    def toApp(self, sessionID, message):
        print "Sent the following message: %s" % message.toString()
        return

    def fromApp(self, message, sessionID):
        print "Received the following message: %s" % message.toString()
        return

    def genOrderID(self):
        self.orderID = self.orderID+1
        return `self.orderID`

    def genExecID(self):
        self.execID = self.execID+1
        return `self.execID`

    def put_order(self):
        print("Creating the following order: ")
        trade = fix.Message()
        trade.getHeader().setField(fix.BeginString(fix.BeginString_FIX44)) #
        trade.getHeader().setField(fix.MsgType(fix.MsgType_NewOrderSingle)) #39=D
        trade.setField(fix.ClOrdID(self.genExecID())) #11=Unique order

        trade.setField(fix.HandlInst(fix.HandlInst_MANUAL_ORDER_BEST_EXECUTION)) #21=3 (Manual order, best executiona)
        trade.setField(fix.Symbol('BTC-USD')) #55=SMBL ?
        trade.setField(fix.Side(fix.Side_BUY)) #43=1 Buy
        trade.setField(fix.OrdType(fix.OrdType_LIMIT)) #40=2 Limit order
        trade.setField(fix.OrderQty(10)) #38=100
        trade.setField(fix.Price(9000))
        trade.setField(fix.Username("pairty"))
        trade.setField(fix.Password("pairty"))
        print trade.toString()
        fix.Session.sendToTarget(trade, self.sessionID)


def main():
    try:
        settings = fix.SessionSettings("initiatorsettings.cfg")
        application = Application()
        storeFactory = fix.FileStoreFactory(settings)
        logFactory = fix.FileLogFactory(settings)
        initiator = fix.SocketInitiator(application, storeFactory, settings, logFactory)
        initiator.start()

        while 1:
                input = raw_input()
                if input == '1':
                    print "Putin Order"
                    application.put_order()
                if input == '2':
                    sys.exit(0)
                if input == 'd':
                    import pdb
                    pdb.set_trace()
                else:
                    print "Valid input is 1 for order, 2 for exit"
                    continue
    except (fix.ConfigError, fix.RuntimeError), e:
        print e

if __name__=='__main__':
    parser = argparse.ArgumentParser(description='FIX Client')
    parser.add_argument('file_name', type=str, help='Name of configuration file')
    # args = parser.parse_args()
    # main(args.file_name)
    main()