import math
import re
import sys
import csv
import localizationTrilateration

'''
BSSID: Basic Service Set Identifier
SS: signal strength
RSSI: Received signal strength indicator
AP location: access point location

The basic service set (BSS) provides the basic building block of an 802.11 wireless LAN. 
Each BSS is uniquely identified by a BSSID.
'''


'''
Input to this function is the location string. 
This function parses the string using str.split() function, with the delimiter as "::::"
This function also finds the BSSID that has the greatest Signal Strength
'''
def Parsing(location, BSSID_APinfo):
    BSSID_SignalStrength = {}
    MsgArray = location.split("::::")
    PhoneMAC = MsgArray[0]
    APCount = MsgArray[1]
    APData = MsgArray[2]
    TimeStamp = MsgArray[-1]
    start_tags = [m.end() for m in re.finditer("<", APData)]
    end_tags = [m.start() for m in re.finditer(">", APData)]
    max_SS = -sys.maxint - 1
    for index in range(len(start_tags)):
        BSSID_SS = APData[start_tags[index]:end_tags[index]]
        BSSID = BSSID_SS[:BSSID_SS.find('-')]
        SS = int(BSSID_SS[BSSID_SS.find('-'):])
        
        # find the greatest Signal Strength
        if BSSID in BSSID_APinfo and SS > max_SS:
            max_SS = SS
            max_BSSID = BSSID
            
        BSSID_SignalStrength[BSSID] = SS
        
    return (BSSID_SignalStrength, PhoneMAC, TimeStamp, max_BSSID)

'''
Get_SSIDLocation() is a sub function that takes the dictionaries BSSID_SignalStrength, 
BSSID_APinfo as inputs, and return the dictionary BSSID_APinfo. 
Using this function, we can tell the X, Y, Z coordinates of a single BSSID captured.
An additional feature of this function is to report BSSID not found in BSSID_APinfo. 
This happens because only the access points in Phillips Hall are considered into our calculation, 
however, in some of the measurement points,
BSSID relating to access points in Upson Hall or Duffield Hall might be captured as well.
'''
def Get_BSSIDlocation(BSSID_SignalStrength,BSSID_APinfo, APlocation):
    BSSIDlocation = dict()
    
    keys = BSSID_SignalStrength.keys()
    for i in range(len(keys)):
        if keys[i] in BSSID_APinfo:
            temp = BSSID_APinfo[keys[i]][0]
            if temp in APlocation:
                BSSIDlocation[keys[i]] = APlocation.get(temp,"unknown")
            else:
                print "AP location not found " + keys[i] + " " + str(BSSID_SignalStrength[keys[i]])
                
    return BSSIDlocation


'''
WeightedCentroid() is a sub function that takes the dictionaries BSSID_SignalStrength and BSSIDlocation as inputs, 
and returns a list called predicted_location which contains the predicted X, Y and Z coordinates values for the current occupant.
This algorithm is used in three dimensions in this project.
'''
def WeightedCentroid(BSSID_SignalStrength,BSSIDlocation, SignalStrength_RSSI, FloorNum):
    '''BSSID_SignalStrength is a dictionary where the key is the BSSID detected, the correponding 
    #value is the related signal strength. BSSIDlocation is the location of the 
    #"BSSID", only the APlocation has physical address, but we are using mapping
    # to find the APlocation the BSSID belongs to.
    #BSSIDlocation = {'00:0b:86:5c:f9:02': [1,2,3], '00:0b:86:5c:f9:03': [4,5,6]}
    #BSSID_SignalStrength = {'00:0b:86:5c:f9:02': -65, '00:0b:86:5c:f9:03': -65};'''
    floorNum_z = {
        1 : 199.05,
        2 : 650.65,
        3 : 1139.85,
        4 : 1466.35  
    }
    data = {}
    g = 1.3 
    sumweight = 0
    X = 0.00
    Y = 0.00
    Z = 0.00
 
    for SSID in BSSID_SignalStrength:
        if (SSID in BSSIDlocation) and (BSSIDlocation[SSID][2] == floorNum_z[FloorNum]):
            signalStrength = BSSID_SignalStrength[SSID]
            Rssi0 = SignalStrength_RSSI[signalStrength]
            signalpower =  math.pow(10,Rssi0/20.0)
            weight = math.pow(signalpower,g)
            #print SSID, "weight: ", weight, BSSID_APinfo[SSID]
            data[SSID] = weight
            sumweight = sumweight + weight
                    
    for SSID in data.keys():
        if (SSID in BSSIDlocation) and (BSSIDlocation[SSID][2] == floorNum_z[FloorNum]):
            position= BSSIDlocation[SSID]
            x = position[0]
            y = position[1]
            z = position[2]
            weight = data[SSID]
            X = X + x * weight / sumweight
            Y = Y + y * weight / sumweight
            Z = Z + z * weight / sumweight

            
    predicted_location = [X,Y,Z]
    if predicted_location[0]==0:
        predicted_location[0]=-1
    if predicted_location[1]==0:
        predicted_location[1]=-1
    if predicted_location[2]==0:
        predicted_location[2]=-1

    return predicted_location


##'''
##Location_AP() is a sub function that returns the access point locations 
##detected at each measurement point. The inputs to this function are BSSID_SignalStrength, 
##BSSID_APinfo. The num as an input to this file can be used as an iterator, to calculate 
##average for multiple measurements. If a single trial then num=0
##'''
##def Location_AP(num,BSSID_SignalStrength,BSSID_APinfo):
##    Seen_APs = []
##    location_APseen = {}
##    keys = BSSID_SignalStrength.keys()
##    for i in range(len(keys)):
##        if keys[i] in BSSID_APinfo:
##            if BSSID_APinfo[keys[i]] in Seen_APs:
##                continue;
##            else:
##                Seen_APs.append(BSSID_APinfo[keys[i]])
##    location_APseen[num] = Seen_APs
##    
##    return location_APseen

'''
Give the BSSID that has the greatest signal strength
Find the corresponding APID and read its z coordinate, which indicates
which floor this AP is on. Use this floor number as the user's floor number
'''
def getFloorNumber(max_BSSID, BSSID_APinfo, APlocation):
    z_floorNum = {
        199.05 : 1,
        650.65 : 2,
        1139.85 : 3,
        1466.35 : 4 ,  
    }

    if max_BSSID in BSSID_APinfo:
        APID = BSSID_APinfo[max_BSSID][0]
        return z_floorNum[APlocation[APID][2]]
    
    else:
        print "BSSID %s is not in BSSID_APinfo" % (max_BSSID)
        return -1


'''
Input is some constant data: BSSID_APinfo, SignalStrength_RSSI, APlocation
Start to calculate use's position
'''
def start(location, BSSID_APinfo, SignalStrength_RSSI, APlocation):
    (BSSID_SignalStrength, PhoneMAC, TimeStamp, max_BSSID) = Parsing(location, BSSID_APinfo)
    BSSIDlocation = Get_BSSIDlocation(BSSID_SignalStrength, BSSID_APinfo, APlocation)

    # Get floor number
    FloorNum = getFloorNumber(max_BSSID, BSSID_APinfo, APlocation)
    if FloorNum == -1:
        raise 

    '''For running trilateration on the samples'''
    # make ap with multipe signals not double count
    BSSID_FixedSignalStrength = localizationTrilateration.fixRssi(BSSID_SignalStrength, BSSIDlocation)

    predicted_location = WeightedCentroid(BSSID_FixedSignalStrength, BSSIDlocation, SignalStrength_RSSI, FloorNum)

    possiblePts = [] # check points around weighted centroid predicted location (but not in z axis)
    for i in range(-200,210,10):
        for j in range(-200,210,10) :
            possiblePts.append([predicted_location[0] + i, predicted_location[1] + j, predicted_location[2]])
    
    max = 0
    ptPredicted = []
    for pt in possiblePts:
        prob = localizationTrilateration.probabilityPt(pt, BSSID_FixedSignalStrength, BSSIDlocation, FloorNum)
        if prob > max:
            ptPredicted = pt
            max = prob
 

    #The following block is to store output result
    output_file = open("documentation.csv",'a')
    data = csv.writer(output_file,dialect = 'excel')
    if max > 0:
        data.writerow([PhoneMAC,TimeStamp,ptPredicted, FloorNum])
        output_file.close()
        return (ptPredicted, PhoneMAC, FloorNum)
    else:
        data.writerow([PhoneMAC,TimeStamp,predicted_location, FloorNum])
        output_file.close()
        return (predicted_location, PhoneMAC, FloorNum)
