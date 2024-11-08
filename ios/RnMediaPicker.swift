import Foundation
import PhotosUI
import UIKit
import AVFoundation

@objc(RnMediaPicker)
class RnMediaPicker: NSObject {

  var resolve: RCTPromiseResolveBlock?
  var reject: RCTPromiseRejectBlock?
  var imagePickerController: UIImagePickerController?

  @objc func launchLibrary(_ options: NSDictionary, resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) {
    self.resolve = resolve
    self.reject = reject

    let isMultipleSelection = options["isMultipleSelection"] as? Bool ?? false
    let maxSelection = options["maxSelection"] as? Int ?? 1
    let mediaType = options["mediaType"] as? String ?? "image"

    DispatchQueue.main.async {
      if #available(iOS 14, *) {
        self.showPHPicker(isMultipleSelection: isMultipleSelection, maxSelection: maxSelection, mediaType: mediaType)
      } else {
        self.showUIImagePicker(mediaType: mediaType)
      }
    }
  }

  private func showPHPicker(isMultipleSelection: Bool, maxSelection: Int, mediaType: String) {
    guard #available(iOS 14, *), let rootVC =
      UIApplication.shared.keyWindow?.rootViewController else {
        self.reject?("unsupported_version", "PHPickerViewController requires iOS 14 or newer", ["resultCode": 3] as? Error)
      return
    }

    var config = PHPickerConfiguration()
    config.selectionLimit = isMultipleSelection ? maxSelection : 1

    switch mediaType {
    case "image":
      config.filter = PHPickerFilter.images
    case "video":
      config.filter = PHPickerFilter.videos
    case "all":
      config.filter = PHPickerFilter.any(of: [.images, .videos])
    default:
      config.filter = PHPickerFilter.images
    }

    let picker = PHPickerViewController(configuration: config)
    picker.delegate = self
    rootVC.present(picker, animated: true, completion: nil)
  }

  private func showUIImagePicker(mediaType: String) {
    guard let rootVC = UIApplication.shared.keyWindow?.rootViewController
      else {
        self.reject?("no_root_view_controller", "Root view controller not found", ["resultCode": 3] as? Error)
      return
    }

    imagePickerController = UIImagePickerController()
    imagePickerController?.delegate = self
    imagePickerController?.mediaTypes = mediaType == "video" ? ["public.movie"] : ["public.image"]
    imagePickerController?.sourceType = .photoLibrary

    rootVC.present(imagePickerController!, animated: true, completion: nil)
  }

  private func saveToTemporaryDirectory(url: URL, type: String) -> URL? {
    let tempDirectory = FileManager.default.temporaryDirectory
    let fileExtension = type == "image" ? ".jpg" : ".mp4"
    let fileName = UUID().uuidString + fileExtension
    let tempFileURL = tempDirectory.appendingPathComponent(fileName)

    do {
      try FileManager.default.copyItem(at: url, to: tempFileURL)
      return tempFileURL
    } catch {
      print("Error copying file to temporary directory: \(error)")
      return nil
    }
  }

  private func getMediaAttributes(for url: URL, type: String) -> [String: Any] {
    let fileName = url.lastPathComponent
    let mimeType = type == "image" ? "image/jpeg" : "video/mp4"
    let fileSize = (try? FileManager.default.attributesOfItem(atPath: url.path)[.size] as? Int) ?? 0
    var width = 0
    var height = 0

    if type == "image", let image = UIImage(contentsOfFile: url.path) {
      width = Int(image.size.width)
      height = Int(image.size.height)
    } else if type == "video" {
      let asset = AVAsset(url: url)
      if let track = asset.tracks(withMediaType: .video).first {
        width = Int(track.naturalSize.width)
        height = Int(track.naturalSize.height)
      }
    }

    return [
      "name": fileName,
      "mimeType": mimeType,
      "size": fileSize,
      "width": width,
      "height": height
    ]
  }
}

@available(iOS 14, *)
extension RnMediaPicker: PHPickerViewControllerDelegate {
  func picker(_ picker: PHPickerViewController, didFinishPicking results: [PHPickerResult]) {
    picker.dismiss(animated: true, completion: nil)

    var assets: [[String: Any]] = []
    let dispatchGroup = DispatchGroup()

    for result in results {
      dispatchGroup.enter()
      if result.itemProvider.hasItemConformingToTypeIdentifier(UTType.image.identifier) {
        result.itemProvider.loadFileRepresentation(forTypeIdentifier: UTType.image.identifier) { (url, error) in
          if let fileUrl = url, let tempUrl = self.saveToTemporaryDirectory(url: fileUrl, type: "image") {
            var asset = self.getMediaAttributes(for: tempUrl, type: "image")
            asset["uri"] = tempUrl.absoluteString
            assets.append(asset)
          }
          dispatchGroup.leave()
        }
      } else if result.itemProvider.hasItemConformingToTypeIdentifier(UTType.movie.identifier) {
        result.itemProvider.loadFileRepresentation(forTypeIdentifier: UTType.movie.identifier) { (url, error) in
          if let fileUrl = url, let tempUrl = self.saveToTemporaryDirectory(url: fileUrl, type: "video") {
            var asset = self.getMediaAttributes(for: tempUrl, type: "video")
            asset["uri"] = tempUrl.absoluteString
            assets.append(asset)
          }
          dispatchGroup.leave()
        }
      } else {
        dispatchGroup.leave()
      }
    }

    dispatchGroup.notify(queue: .main) {
      let response: [String: Any] = [
        "resultCode": 0,
        "assets": assets
      ]
      self.resolve?(response)
    }
  }
}

extension RnMediaPicker: UIImagePickerControllerDelegate, UINavigationControllerDelegate {
  func imagePickerController(_ picker: UIImagePickerController, didFinishPickingMediaWithInfo info: [UIImagePickerController.InfoKey : Any]) {
    picker.dismiss(animated: true, completion: nil)

    guard let mediaURL = info[.imageURL] as? URL ?? info[.mediaURL] as? URL else {
      self.reject?("no_media", "No media selected", ["resultCode": 999] as? Error)
      return
    }

    let mediaType = (info[.mediaType] as? String) == "public.movie" ? "video" : "image"
    if let tempUrl = saveToTemporaryDirectory(url: mediaURL, type: mediaType) {
      var asset = getMediaAttributes(for: tempUrl, type: mediaType)
      asset["uri"] = tempUrl.absoluteString

      let response: [String: Any] = [
        "resultCode": 0,
        "assets": [asset]
      ]
      self.resolve?(response)
    } else {
      self.reject?("save_failed", "Failed to save media to temporary directory", ["resultCode": 999] as? Error)
    }
  }

  func imagePickerControllerDidCancel(_ picker: UIImagePickerController) {
    picker.dismiss(animated: true, completion: nil)
    self.reject?("user_cancelled", "User canceled the picker", ["resultCode": 999] as? Error)
  }
}

@objc(RnMediaPicker)
extension RnMediaPicker: RCTBridgeModule {
  static func moduleName() -> String! {
    return "RnMediaPicker"
  }

  static func requiresMainQueueSetup() -> Bool {
    return true
  }
}
