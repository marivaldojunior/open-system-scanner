namespace ScannerVcIaRfid.Maui.VisionExtras;

public static class VisionExtrasMauiAppBuilderExtensions
{
    /// <summary>
    /// Registra os motores de objetos, rotulos e rostos no
    /// VisionEngineRegistry nativo. Depois disso, VisionScanner.Engine aceita
    /// os ids de <see cref="VisionExtraEngines"/>.
    /// </summary>
    public static MauiAppBuilder UseVisionExtras(this MauiAppBuilder builder)
    {
#if ANDROID
        Com.Scannervciarfid.Vision.Objects.ObjectDetectionEngine.Register();
        Com.Scannervciarfid.Vision.Labels.ImageLabelingEngine.Register();
        Com.Scannervciarfid.Vision.Faces.FaceDetectionEngine.Register();
#endif
        return builder;
    }
}
