import math


''' Combines rssi values from same access points to one key-value pair (name-rssi_avg)
    Note: this is because different IP addresses correspond to the same access point
    fixedRssi = { SSID: RSSI, SSID2: RSSI2, ... }
    countRssi = { [x,y,z]: [count, SSIDofPosition], ...'''
def fixRssi(ptRssi, apLocation):
    fixedRssi = {}
    # to calculate average, need to know how many times of each AP has already been seen
    countRssi = {}
    
    for SSID in ptRssi:
        if (SSID in apLocation) and (tuple(apLocation[SSID]) in countRssi):
            position = tuple(apLocation[SSID])
            positionSSID = countRssi[position][1]
            # average weighted 
            fixedRssi[positionSSID] = (fixedRssi[positionSSID] * countRssi[position][0] + ptRssi[SSID]) / (countRssi[position][0] + 1) 
            #print fixedRssi[positionSSID]
            # count ++
            countRssi[position][0] += 1
        elif (SSID in apLocation):
            fixedRssi[SSID] = ptRssi[SSID]
            countRssi[tuple(apLocation[SSID])] = [1, SSID]
        else:
            fixedRssi[SSID] = ptRssi[SSID]
    return fixedRssi
    

''' probability of current position as current point with given access point position and rssi'''
def probabilityPt(pt, apPtsRssi, apPtsPosition, FloorNum):
    floorNum_z = {
        1 : 199.05,
        2 : 650.65,
        3 : 1139.85,
        4 : 1466.35  
    }
    # x, y is the current point of probability in question
    # apx, apy is the current access point being analyzed
    L = -36 +14   # L = RSSI at 1m from access point
    n = 1.37 + 3.2 # n = average attenuation
    x = pt[0]
    y = pt[1]
    count = 0
    prob = 0
    for SSID in apPtsRssi:
        if (SSID in apPtsPosition) and (apPtsPosition[SSID][2] == floorNum_z[FloorNum]):
            apx = apPtsPosition[SSID][0]
            apy = apPtsPosition[SSID][1]
            rssi = apPtsRssi[SSID]

            distance = math.pow(10,(L-rssi)/(10*n)) # distance from access point of current point
            #print str(apx) + " " + str(apy) + " "+ str(apPtsPosition[SSID][2]) + " " + str(distance) + " " + str(rssi) + " " + str(distance)
            sigma = .64 * distance + 5 # to correct for increased distance in low strength signals
            deltaD = math.sqrt(math.pow(apx - x, 2) + math.pow(apy - y, 2)) - distance
            prob += 1 / math.sqrt(2 * math.pi * sigma) * math.pow(math.e, -1 * math.pow(deltaD,2) / ( 2 * math.pow(sigma, 2)))
            count = count + 1
    #print count
    return prob

# # pts are all the predicted locations (around the predicted weighted centroid alg)
# # apPts are the positions and rssi of every access point
# # returns list of probabilities for each pt in pts
# def allProbabilities(pts, apPts):
#     totalProb = 0
#     probabilityOfPts = []
#     for pt in pts:
#         temp = probability(pt,apPts)
#         totalProb += temp
#         probabilityOfPts[pt] = temp
#         print temp
#     return probabilityOfPts
# 
# def test():
#     print("success")
        
