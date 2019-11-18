require 'json'

package = JSON.parse(File.read(File.join(__dir__, 'package.json')))

Pod::Spec.new do |s|
  s.name         = 'react-native-geolocation'
  s.version      = package['version']
  s.summary      = package['description']
  s.license      = package['license']

  s.authors      = package['author']
  s.homepage     = package['homepage']
  s.platform     = :ios, "10.0"

  s.source       = { :git => "https://github.com/ConnectedHomes/react-native-geolocation.git", :tag => "v#{s.version}" }
  s.source_files  = "ios/RNGeolocation/*.{h,m,swift}"
  s.dependency 'React'
  s.swift_version = '5.0'
end
