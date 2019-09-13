//
//  HiveGeofenceManager.swift
//  Presence
//
//  Created by Andrew on 30/07/2019.
//  Copyright © 2019 Centrica. All rights reserved.
//

import Foundation
import UIKit
import CoreLocation

public struct RNHiveLocationRequest {
    let requestId: String
    let comletion: RNHiveLocationRequestCompletion
}

enum RNHiveGeofenceCrossingEvent: String {
    case entry = "ENTER"
    case exit  = "EXIT"
}

typealias RNHiveLocationRequestCompletion = (_ locations: [CLLocation]?, _ error: Error?) -> Void
typealias RNHiveGeofenceRequestCompletion = (_ regions: [CLCircularRegion]?, _ error: Error?) -> Void
typealias RNHiveGeofenceEventResponder = (_ geofenceEvent: RNHiveGeofenceEvent?, _ error: Error?) -> Void

@objc(RNHiveGeolocationManager)
class RNHiveGeolocationManager: NSObject {
    
    static let shared = RNHiveGeolocationManager()
    
    private let locationManager = CLLocationManager()
    
    private let savedGeofencesKey = "savedItems"
    private var geofences: [RNHiveGeofence] = []
    private var regions: [CLCircularRegion] = []
    private var pendingLocationRequests: [RNHiveLocationRequest] = []
    
    private var geofenceRequestCompletion: RNHiveGeofenceRequestCompletion? = nil
    private var geofenceEventResponder: RNHiveGeofenceEventResponder? = nil
    
    override init() {
        super.init()
        self.locationManager.delegate = self
    }
    
    @objc public func allGeofences() -> [RNHiveGeofence]  {
        guard let savedData = UserDefaults.standard.data(forKey: savedGeofencesKey) else { return [] }
        let decoder = JSONDecoder()
        do {
            let savedGeotifences = try decoder.decode(Array.self, from: savedData) as [RNHiveGeofence]
            geofences = savedGeotifences
            return savedGeotifences
        } catch {
            print("Error loading geofences: \(error).")
            return []
        }
    }
    
    @objc public func monitoredRegions() -> [CLCircularRegion] {
        return regions
    }
    
    @objc public func saveGeofences() {
        let encoder = JSONEncoder()
        do {
            let data = try encoder.encode(geofences)
            UserDefaults.standard.set(data, forKey: savedGeofencesKey)
        } catch {
            print("error encoding geofences")
        }
    }
    
    @objc public func addGeofence(location: CLLocation, radius: CLLocationDistance) -> RNHiveGeofence? {
        let geofence = RNHiveGeofence(coordinate: location.coordinate, radius: radius)
        appendGeofences(with: [geofence])
        return geofence
    }
    
    @objc func configure() {
        self.locationManager.delegate = self
        let _ = allGeofences()
    }
    
    @objc public func onGeofenceEvent(responder: @escaping RNHiveGeofenceEventResponder) {
        self.geofenceEventResponder = responder
    }
    
    @objc public func addGeofence(dictionary: [String: Any]) -> RNHiveGeofence? {
        guard let geofence = RNHiveGeofence(dictionary: dictionary) else {
            return nil
        }
        appendGeofences(with: [geofence])
        return geofence
    }
    
    @objc public func addGeofences(array: [[String: Any]]) {
        for dictionary in array {
            let _ = addGeofence(dictionary: dictionary)
        }
    }
    
    @objc public func removeAllGeofences() {
        geofences.removeAll()
        regions.removeAll()
        UserDefaults.standard.removeObject(forKey: savedGeofencesKey)
        UserDefaults.standard.synchronize()
    }
    
    @objc public func removeGeofence(identifier: String) {
        geofences.removeAll { $0.identifier == identifier }
    }
    
    @objc public func geofence(identifier: String) -> RNHiveGeofence? {
        let lookedUpGeofences = geofences.filter { $0.identifier == identifier }
        return lookedUpGeofences.first
    }
    
    
    @objc public func startMonitoringGeofences(_ completion: RNHiveGeofenceRequestCompletion?) {
        if !CLLocationManager.isMonitoringAvailable(for: CLCircularRegion.self) {
            print("Error: no monitoring available")
            return
        }
        if CLLocationManager.authorizationStatus() != .authorizedAlways {
            print("Error: not authorized to start monitoring")
        }
        geofenceRequestCompletion = completion
        for geofence in geofences {
            startMonitoring(geofence: geofence)
        }
        
    }
    
    @objc public func stopMonitoringGeofences(_ completion: RNHiveGeofenceRequestCompletion?) {
        geofenceRequestCompletion = completion
        for geofence in geofences {
            stopMonitoring(geofence: geofence)
        }
        // not sure why it's not called from RN side
        removeAllGeofences()
    }
    
    @objc public func requestLocation(completion: RNHiveLocationRequestCompletion?) {
        if let completion = completion {
            let locationRequest = RNHiveLocationRequest(requestId: UUID().uuidString, comletion: completion)
            pendingLocationRequests.append(locationRequest)
        }
        if CLLocationManager.authorizationStatus() == .authorizedAlways {
            locationManager.delegate = self
            locationManager.startUpdatingLocation()
        } else {
            requestLocationPermissions()
        }
    }
    
    private func requestLocationPermissions() {
        if CLLocationManager.authorizationStatus() != .authorizedAlways {
            locationManager.delegate = self
            locationManager.requestAlwaysAuthorization()
        }
    }
    
    private func startMonitoring(geofence: RNHiveGeofence) {
        if let region = createLocationRegion(geofence: geofence) {
            locationManager.startMonitoring(for: region)
        }
    }
    
    private func stopMonitoring(geofence: RNHiveGeofence) {
        let monitoredRegions = locationManager.monitoredRegions.compactMap({ $0 as? CLCircularRegion }).filter({ $0.identifier == geofence.identifier })
        for region in monitoredRegions {
            locationManager.stopMonitoring(for: region)
            regions.removeAll(where: { $0.identifier == region.identifier })
        }
    }
    
    private func createLocationRegion(geofence: RNHiveGeofence) -> CLCircularRegion? {
        if regions.filter({ $0.identifier == geofence.identifier }).count > 0 {
            return nil
        }
        let region = CLCircularRegion(center: geofence.coordinate, radius: geofence.radius, identifier: geofence.identifier)
        region.notifyOnExit = geofence.notifyOnExit
        region.notifyOnEntry = geofence.notifyOnEntry
        regions.append(region)
        return region
    }
    
    private func appendGeofences(with newGeofences: [RNHiveGeofence]) {
        for newGeofence in newGeofences {
            if let existingGeofence = geofences.filter({ $0.identifier == newGeofence.identifier }).first {
                existingGeofence.updateValues(newGeofence)
                updateRegionMonitoring(for: existingGeofence)
            } else {
                geofences.append(newGeofence)
            }
        }
        saveGeofences()
    }
    
    private func updateRegionMonitoring(for geofence: RNHiveGeofence) {
        stopMonitoring(geofence: geofence)
        startMonitoring(geofence: geofence)
    }
    
    private func handleRegionEvent(for region: CLRegion, event: RNHiveGeofenceCrossingEvent) {
        
        guard let geofence = allGeofences().filter({ $0.identifier == region.identifier }).first,
              let location = locationManager.location,
              let region = region as? CLCircularRegion else {
            return
        }
        let geofenceEvent = RNHiveGeofenceEvent(geofence: geofence, location: location, region: region, time: Date(), type: event)
        if let responder = geofenceEventResponder {
            responder(geofenceEvent, nil)
        }
    }
}

extension RNHiveGeolocationManager: CLLocationManagerDelegate {

    func locationManager(_ manager: CLLocationManager, didChangeAuthorization status: CLAuthorizationStatus) {
        if status == .authorizedAlways {
            requestLocation(completion: nil)
        }
    }

    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        if let location = locations.first {
            print("coordinates: \(location.coordinate.latitude), \(location.coordinate.longitude)")
        }
        processPendingLocationRequests(locations: locations, error: nil)
    }
    
    func locationManager(_ manager: CLLocationManager, monitoringDidFailFor region: CLRegion?, withError error: Error) {
        print("monitoring failed for region: \(String(describing: region?.identifier))")
        if let circularRegion = region as? CLCircularRegion {
            processGeofenceCompletion(regions: [circularRegion], error: error)
        }
    }
    
    func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        print("location managed did fail with error: \(error)")
        processPendingLocationRequests(locations: nil, error: error)
    }
    
    func locationManager(_ manager: CLLocationManager, didStartMonitoringFor region: CLRegion) {
        print("monitoring started for region: \(region)")
        if let circularRegion = region as? CLCircularRegion {
            processGeofenceCompletion(regions: [circularRegion], error: nil)
        }
        
    }
    
    func locationManager(_ manager: CLLocationManager, didEnterRegion region: CLRegion) {
        if region is CLCircularRegion {
            handleRegionEvent(for: region, event: .entry)
        }
    }
    
    func locationManager(_ manager: CLLocationManager, didExitRegion region: CLRegion) {
        if region is CLCircularRegion {
            handleRegionEvent(for: region, event: .exit)
        }
    }
    
    private func processGeofenceCompletion(regions: [CLCircularRegion]?, error: Error?) {
        
        guard let completion = geofenceRequestCompletion else {
            return
        }
        
        if let regions = regions {
            if let error = error {
                completion(nil, error)
                geofenceRequestCompletion = nil
            }
            if error == nil && regions.count == geofences.count {
                completion(regions, error)
                geofenceRequestCompletion = nil
            }
        }
    }
    
    private func processPendingLocationRequests(locations: [CLLocation]?, error: Error?) {
        if let locationRequest = pendingLocationRequests.first {
            locationRequest.comletion(locations, error)
            pendingLocationRequests.removeAll { $0.requestId == locationRequest.requestId }
        }
    }
    
}


public extension NotificationCenter {
    /// Adds observer with block that will be called asynchronously on specified queue.
    /// Defaults to main operation queue.
    @discardableResult
    func addObserverAsync(forName name: NSNotification.Name?, object obj: Any? = nil, queue: OperationQueue? = OperationQueue.main, using block: @escaping (Notification) -> Void) -> NSObjectProtocol {
        
        let result = addObserver(forName: name, object: obj, queue: nil) { notification in
            queue?.addOperation {
                block(notification)
            }
        }
        return result
    }
}


public extension CLLocation {
    enum CLLocationKeys: String, CodingKey {
        case latitude       = "latitude"
        case longitude      = "longitude"
        case altitude       = "altitude"
        case heading        = "heading"
        case speed          = "speed"
        case horizontalAccuracy = "accuracy"
        case verticalAccuracy   = "altitude_accuracy"
    }
    
    @objc var dictionary: [String: Any]? {
        
        let coordinates = [CLLocationKeys.latitude.rawValue: self.coordinate.latitude,
                           CLLocationKeys.longitude.rawValue: self.coordinate.longitude,
                           CLLocationKeys.altitude.rawValue: self.altitude,
                           CLLocationKeys.heading.rawValue: self.course,
                           CLLocationKeys.speed.rawValue: self.speed,
                           CLLocationKeys.horizontalAccuracy.rawValue: self.horizontalAccuracy,
                           CLLocationKeys.verticalAccuracy.rawValue: self.verticalAccuracy]
        return ["coords": coordinates]
    }
}
