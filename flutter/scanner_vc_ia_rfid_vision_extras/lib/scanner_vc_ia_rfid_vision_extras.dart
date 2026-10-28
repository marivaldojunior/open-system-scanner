import 'dart:convert';

/// Ids dos motores extras para VisionScannerWidget.engine (plugin
/// scanner_vc_ia_rfid_flutter). Espelham ENGINE_ID de ObjectDetectionEngine,
/// ImageLabelingEngine e FaceDetectionEngine (Kotlin). Os motores ficam
/// disponiveis so por este plugin estar no pubspec do app.
abstract final class VisionExtraEngines {
  /// Deteccao e rastreio de objetos. Format: VISION_OBJECTS.
  static const String objects = 'objects';

  /// Rotulos genericos da cena. Format: VISION_LABELS.
  static const String labels = 'labels';

  /// Deteccao de rostos (sem reconhecimento). Format: VISION_FACES.
  static const String faces = 'faces';
}

class BoundingBox {
  const BoundingBox({required this.left, required this.top, required this.right, required this.bottom});

  factory BoundingBox.fromJson(Map<String, dynamic>? json) {
    return BoundingBox(
      left: json?['left'] as int? ?? 0,
      top: json?['top'] as int? ?? 0,
      right: json?['right'] as int? ?? 0,
      bottom: json?['bottom'] as int? ?? 0,
    );
  }

  final int left;
  final int top;
  final int right;
  final int bottom;
}

class VisionLabel {
  const VisionLabel({required this.text, required this.confidence, this.index});

  factory VisionLabel.fromJson(Map<String, dynamic> json) {
    return VisionLabel(
      text: json['text'] as String? ?? '',
      confidence: (json['confidence'] as num? ?? 0).toDouble(),
      index: json['index'] as int?,
    );
  }

  final String text;
  final double confidence;

  /// So nos rotulos de cena (VISION_LABELS).
  final int? index;
}

class DetectedObjectInfo {
  const DetectedObjectInfo({required this.trackingId, required this.boundingBox, required this.labels});

  factory DetectedObjectInfo.fromJson(Map<String, dynamic> json) {
    return DetectedObjectInfo(
      trackingId: json['trackingId'] as int?,
      boundingBox: BoundingBox.fromJson(json['boundingBox'] as Map<String, dynamic>?),
      labels: _list(json['labels'], VisionLabel.fromJson),
    );
  }

  final int? trackingId;
  final BoundingBox boundingBox;
  final List<VisionLabel> labels;
}

class DetectedFaceInfo {
  const DetectedFaceInfo({
    required this.trackingId,
    required this.boundingBox,
    this.smilingProbability,
    this.leftEyeOpenProbability,
    this.rightEyeOpenProbability,
    required this.headEulerAngleY,
    required this.headEulerAngleZ,
  });

  factory DetectedFaceInfo.fromJson(Map<String, dynamic> json) {
    return DetectedFaceInfo(
      trackingId: json['trackingId'] as int?,
      boundingBox: BoundingBox.fromJson(json['boundingBox'] as Map<String, dynamic>?),
      smilingProbability: (json['smilingProbability'] as num?)?.toDouble(),
      leftEyeOpenProbability: (json['leftEyeOpenProbability'] as num?)?.toDouble(),
      rightEyeOpenProbability: (json['rightEyeOpenProbability'] as num?)?.toDouble(),
      headEulerAngleY: (json['headEulerAngleY'] as num? ?? 0).toDouble(),
      headEulerAngleZ: (json['headEulerAngleZ'] as num? ?? 0).toDouble(),
    );
  }

  final int? trackingId;
  final BoundingBox boundingBox;
  final double? smilingProbability;
  final double? leftEyeOpenProbability;
  final double? rightEyeOpenProbability;
  final double headEulerAngleY;
  final double headEulerAngleZ;
}

/// Converte ScanResult.value conforme o ScanResult.format. Lanca
/// [FormatException] se o JSON estiver invalido.
abstract final class VisionExtrasPayload {
  static List<DetectedObjectInfo> parseObjects(String json) =>
      _list(_decode(json)['objects'], DetectedObjectInfo.fromJson);

  static List<VisionLabel> parseLabels(String json) => _list(_decode(json)['labels'], VisionLabel.fromJson);

  static List<DetectedFaceInfo> parseFaces(String json) =>
      _list(_decode(json)['faces'], DetectedFaceInfo.fromJson);

  static Map<String, dynamic> _decode(String json) => jsonDecode(json) as Map<String, dynamic>;
}

List<T> _list<T>(Object? value, T Function(Map<String, dynamic>) fromJson) {
  return (value as List<dynamic>? ?? const [])
      .map((dynamic item) => fromJson(item as Map<String, dynamic>))
      .toList(growable: false);
}
