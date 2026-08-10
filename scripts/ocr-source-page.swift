#!/usr/bin/env swift

import AppKit
import Foundation
import Vision

guard CommandLine.arguments.count >= 2 else {
    fputs("Usage: ocr-source-page.swift IMAGE [IMAGE ...]\n", stderr)
    exit(2)
}

struct OCRRecord: Codable {
    let path: String
    let text: String
}

let languages = ["fr-FR", "en-US", "zh-Hans"]
let encoder = JSONEncoder()
encoder.outputFormatting = [.withoutEscapingSlashes]

for imagePath in CommandLine.arguments.dropFirst() {
    guard let image = NSImage(contentsOfFile: imagePath),
          let imageData = image.tiffRepresentation,
          let bitmap = NSBitmapImageRep(data: imageData),
          let cgImage = bitmap.cgImage else {
        fputs("Unable to load image: \(imagePath)\n", stderr)
        continue
    }

    let request = VNRecognizeTextRequest()
    request.recognitionLevel = .accurate
    request.recognitionLanguages = languages
    request.usesLanguageCorrection = true

    let handler = VNImageRequestHandler(cgImage: cgImage, options: [:])
    try handler.perform([request])

    let observations = (request.results ?? []).sorted {
        let yDifference = abs($0.boundingBox.midY - $1.boundingBox.midY)
        if yDifference > 0.01 {
            return $0.boundingBox.midY > $1.boundingBox.midY
        }
        return $0.boundingBox.minX < $1.boundingBox.minX
    }
    let text = observations.compactMap { $0.topCandidates(1).first?.string }.joined(separator: "\n")
    let record = OCRRecord(path: imagePath, text: text)
    if let data = try? encoder.encode(record), let line = String(data: data, encoding: .utf8) {
        print(line)
    }
}
