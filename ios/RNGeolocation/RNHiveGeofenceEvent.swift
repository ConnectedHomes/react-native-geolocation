//
//  HiveGeofenceTrackingEvent.swift
//  Presence
//
//  Created by Andrew on 05/08/2019.
//  Copyright © 2019 Centrica. All rights reserved.
//

import Foundation
import CoreLocation

@objc(RNHiveGeofenceEvent)
public class RNHiveGeofenceEvent: NSObject/*: Codable */ {
    @objc var location: CLLocation
    @objc var geofence: RNHiveGeofence
    @objc var region: CLCircularRegion
    @objc var action: String
    @objc var time: Date
    
    enum RNHiveGeofenceEventDictionaryKeys: String, CodingKey {
        case action     = "action"
        case identifier = "identifier"
        case location   = "location"
        case geofence   = "geofence"
        case timestamp  = "timestamp"
    }
    
    enum RNHiveGeofenceEventKeys: String, CodingKey {
        case location, geofence, region,  action, time
    }
    
    init(geofence: RNHiveGeofence, location: CLLocation, region: CLCircularRegion, time: Date, type: RNHiveGeofenceCrossingEvent) {
        self.geofence = geofence
        self.location = location
        self.region = region
        self.time = time
        self.action = type.rawValue
    }

    @objc public func toDictionary() -> [String: Any] {
        var dictionary = [RNHiveGeofenceEventDictionaryKeys.action.rawValue: self.action,
                          RNHiveGeofenceEventDictionaryKeys.identifier.rawValue: self.geofence.identifier
            ] as [String: Any]
        dictionary[RNHiveGeofenceEventDictionaryKeys.location.rawValue] = self.location.dictionary
        dictionary[RNHiveGeofenceEventDictionaryKeys.geofence.rawValue] = self.geofence.dictionary
        dictionary[RNHiveGeofenceEventDictionaryKeys.timestamp.rawValue] = self.time
        return dictionary
    }
}


private extension Date {
    var millisecondsSince1970: Int64 {
        return Int64((self.timeIntervalSince1970 * 1000.0).rounded())
    }
    
    init(milliseconds: Int64) {
        self = Date(timeIntervalSince1970: TimeInterval(milliseconds / 1000))
    }
}
