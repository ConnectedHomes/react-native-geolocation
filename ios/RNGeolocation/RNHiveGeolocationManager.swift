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
import UserNotifications

public struct RNHiveLocationRequest {
    let requestId: String
    let comletion: RNHiveLocationRequestCompletion
}

enum RNHiveGeofenceCrossingEvent: String {
    case entry = "ENTER"
    case exit  = "EXIT"
}

public typealias RNHiveLocationRequestCompletion = (_ locations: [CLLocation]?, _ error: Error?) -> Void
public typealias RNHiveGeofenceRequestCompletion = (_ regions: [CLCircularRegion]?, _ error: Error?) -> Void
public typealias RNHiveGeofenceEventResponder = (_ geofenceEvent: RNHiveGeofenceEvent?, _ error: Error?) -> Void


struct GeolocationNotification: Codable {
    let title: String
    let body: String
    let associatedNodeId: String
    
    enum GeolocationNotificationKeys: String, CodingKey {
        case title, body, timestamp,  associatedNodeId
    }
    
    init(title: String, body: String, associatedNodeId: String) {
        self.title = title
        self.body = body
        self.associatedNodeId = associatedNodeId
    }
    
    func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: GeolocationNotificationKeys.self)
        try container.encode(title, forKey: .title)
        try container.encode(body, forKey: .body)
        try container.encode(associatedNodeId, forKey: .associatedNodeId)
    }
    
    init(from decoder: Decoder) throws {
        let values = try decoder.container(keyedBy: GeolocationNotificationKeys.self)
        title = try values.decode(String.self, forKey: .title)
        body = try values.decode(String.self, forKey: .body)
        associatedNodeId  = try values.decode(String.self, forKey: .associatedNodeId)
    }
    
}


@objc(RNHiveGeolocationManager)
public class RNHiveGeolocationManager: NSObject {
    
    static let shared = RNHiveGeolocationManager()
    
    private let locationManager = CLLocationManager()
    
    private let savedGeofencesKey = "savedItems"
    private let arrivingNotificationKey = "arrivingNotification"
    private let leavingNotificationKey = "leavingNotification"
    private var geofences: [RNHiveGeofence] = []
    private var regions: [CLCircularRegion] = []
    private var pendingLocationRequests: [RNHiveLocationRequest] = []
    /**
     serves as a workaround to store a single event that triggered app to start in the background,
     so that it can be retrieved later after JS finishes it's procedures.
     */
    private var pendingGeofenceNotification: RNHiveGeofenceEvent? = nil
    
    private var geofenceRequestCompletion: RNHiveGeofenceRequestCompletion? = nil
    private var geofenceEventResponder: RNHiveGeofenceEventResponder? = nil
    private var arrivingNotification: GeolocationNotification? = nil
    private var leavingNotification: GeolocationNotification? = nil
    
    public override init() {
        super.init()
        self.locationManager.delegate = self
    }
    
    @objc public func addArrivingNotification(_ arrivingNotiticationDict: [String: String]?, leavingNotificationDict: [String: String]?) {
        if let arrivingDict = arrivingNotiticationDict,
            let arrivingTitle = arrivingDict["title"],
            let arrivingBody = arrivingDict["body"],
            let arrivingNodeId = arrivingDict["triggeringNodeId"] {
            arrivingNotification = GeolocationNotification(title: arrivingTitle, body: arrivingBody, associatedNodeId: arrivingNodeId)
        } else {
            arrivingNotification = nil
            UserDefaults.standard.removeObject(forKey: arrivingNotificationKey)
        }
        
        if let leavingDict = leavingNotificationDict,
            let leavingTitle = leavingDict["title"],
            let leavingBody = leavingDict["body"],
            let leavingNodeId = leavingDict["triggeringNodeId"] {
            leavingNotification = GeolocationNotification(title: leavingTitle, body: leavingBody, associatedNodeId: leavingNodeId)
        } else {
            leavingNotification = nil
            UserDefaults.standard.removeObject(forKey: leavingNotificationKey)
        }
        
        cacheGeofenceNotifications()
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
    
    @objc public func configure() {
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
        if CLLocationManager.authorizationStatus() != .authorizedAlways && CLLocationManager.authorizationStatus() != .authorizedWhenInUse {
            print("Error: not authorized to start monitoring")
        }
        geofenceRequestCompletion = completion
        for geofence in geofences {
            startMonitoring(geofence: geofence)
        }
        
    }
    
    @objc public func triggerStoredEvents() {
        guard let pendingNotification = pendingGeofenceNotification,
            let responder = geofenceEventResponder else {
                return
        }
        responder(pendingNotification, nil)
        pendingGeofenceNotification = nil
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
        if CLLocationManager.authorizationStatus() == .authorizedAlways || CLLocationManager.authorizationStatus() == .authorizedWhenInUse {
            locationManager.delegate = self
            locationManager.startUpdatingLocation()
        } else {
            requestLocationPermissions()
        }
    }
    
    private func requestLocationPermissions() {
        if CLLocationManager.authorizationStatus() != .authorizedAlways && CLLocationManager.authorizationStatus() != .authorizedWhenInUse {
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
    
    private func cacheGeofenceNotifications() {
        print("saving arriving event to cache")
        
        let encoder = JSONEncoder()
        do {
            let data = try encoder.encode(arrivingNotification)
            UserDefaults.standard.set(data, forKey: arrivingNotificationKey)
        } catch {
            print("error saving notification")
        }
        
        print("saving leaving event to cache")
        do {
            let data = try encoder.encode(leavingNotification)
            UserDefaults.standard.set(data, forKey: leavingNotificationKey)
        } catch {
            print("error saving notification")
        }
    }
    
    
    private func loadCachedGeofenceNotifications() {
        guard let savedArriving = UserDefaults.standard.data(forKey: arrivingNotificationKey),
            let savedLeaving = UserDefaults.standard.data(forKey: leavingNotificationKey)
            else {
                return
        }
        let decoder = JSONDecoder()
        if let arriving = try? decoder.decode(GeolocationNotification.self, from: savedArriving) as GeolocationNotification {
            arrivingNotification = arriving
            
        }
        if let leaving = try? decoder.decode(GeolocationNotification.self, from: savedLeaving) as GeolocationNotification {
            leavingNotification = leaving
        }
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
            print("notification posted from handle events")
        } else {
            saveGeofenceEventNotification(event: geofenceEvent)
        }
    }
    
    private func saveGeofenceEventNotification(event: RNHiveGeofenceEvent) {
        pendingGeofenceNotification = event
    }
}

extension RNHiveGeolocationManager: CLLocationManagerDelegate {
    
    public func locationManager(_ manager: CLLocationManager, didChangeAuthorization status: CLAuthorizationStatus) {
        if status == .authorizedAlways || status == .authorizedWhenInUse {
            requestLocation(completion: nil)
        }
    }
    
    public func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        if let location = locations.first {
            print("coordinates: \(location.coordinate.latitude), \(location.coordinate.longitude)")
        }
        processPendingLocationRequests(locations: locations, error: nil)
    }
    
    public func locationManager(_ manager: CLLocationManager, monitoringDidFailFor region: CLRegion?, withError error: Error) {
        print("monitoring failed for region: \(String(describing: region?.identifier))")
        if let circularRegion = region as? CLCircularRegion {
            processGeofenceCompletion(regions: [circularRegion], error: error)
        }
    }
    
    public func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        print("location managed did fail with error: \(error)")
        processPendingLocationRequests(locations: nil, error: error)
    }
    
    public func locationManager(_ manager: CLLocationManager, didStartMonitoringFor region: CLRegion) {
        print("monitoring started for region: \(region)")
        if let circularRegion = region as? CLCircularRegion {
            processGeofenceCompletion(regions: [circularRegion], error: nil)
        }
        
    }
    
    public func locationManager(_ manager: CLLocationManager, didEnterRegion region: CLRegion) {
        if region is CLCircularRegion {
            handleRegionEvent(for: region, event: .entry)
        }
    }
    
    public func locationManager(_ manager: CLLocationManager, didExitRegion region: CLRegion) {
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
    
    
    private func showLocalNotification(_ identifier:String, title: String, body: String, extras: [AnyHashable : Any]) {
        let notificationContent = UNMutableNotificationContent()
        notificationContent.body = body
        notificationContent.title = title
        notificationContent.sound = UNNotificationSound.default
        notificationContent.badge = 0
        notificationContent.userInfo = extras
        
        let trigger = UNTimeIntervalNotificationTrigger(timeInterval: 0.5, repeats: false)
        let request = UNNotificationRequest(identifier: identifier, content: notificationContent, trigger: trigger)
        UNUserNotificationCenter.current().add(request) { error in
            if let error = error {
                print("Error: \(error)")
            }
        }
    }
    
    private func postLocalNotification(for geofence: RNHiveGeofence, event: RNHiveGeofenceEvent, crossingType: RNHiveGeofenceCrossingEvent) {
        
        if crossingType == .entry,
            let arriving = arrivingNotification {
            showLocalNotification("arriving", title: arriving.title, body: arriving.body, extras: ["data":["triggeringNodeId": arriving.associatedNodeId]])
        } else if crossingType == .exit,
            let leaving = leavingNotification {
            showLocalNotification("leaving", title: leaving.title, body: leaving.body, extras: ["data":["triggeringNodeId": leaving.associatedNodeId]])
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
