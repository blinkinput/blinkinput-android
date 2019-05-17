# Release notes

## 4.1.0

### New features:
- enabled capturing of high resolution camera frames:
    - When custom UI integration is performed, enable this functionality by using method `RecognizerRunnerView.setHighResFrameCaptureEnabled` and use `RecognizerRunnerView.captureHighResImage` to capture image
    - When using provided scan activities, high resolution full camera frames taken at the moment of successful scan are returned if this option is enabled through `UISettings`. Concrete `UISettings` which implement interface `HighResSuccessFrameCaptureUIOptions` support this feature.

### Improvements for existing features:
- added option to force overlay orientation for `BarcodeOverlayController` (`BarcodeScanActivity`) - use `BarcodeUISettings.setForcedOrientation(OverlayOrientation)`
- `RecognizerRunnerView` is lifecycle-aware now, it implements `android.arch.lifecycle.LifecycleObserver` interface
- improved image return processor:
    - the processor now estimates detected (dewarped) document image quality and returns the best quality dewarped image from the best quality detection
- `BarcodeScanActivity` by default does not show result dialog after scan
- updated default UI icons

### Minor API changes:
- Scanning timeout that can be configured by using `RecognizerBundle.setNumMsBeforeTimeout` is by default set to `RecognizerBundle.TIMEOUT_INFINITY`, which means that timeout is disabled by default. Previous default timeout value was 10 seconds.
- renamded enum `com.microblink.uisettings.options.ShowOcrResultMode` to `com.microblink.uisettings.options.OcrResultDisplayMode`
- for all `UISettings` classes which support setting of OCR result display mode, renamed method `setShowOcrResultMode` to `setOcrResultDisplayMode`

### Bug fixes:
- fixed crashes on Nexus 6
- removed incorrect autofocus check that was performed before concrete camera type is chosen
- fixed crash on some devices when using `VIDEO_RESOLUTION_MAX_AVAILABLE`
- fixed problems in camera management:
    - default camera surface is `TextureVeiw` for devices that use Camera1 API, otherwise `SurfaceView` is used
- fixed camera autofocus problems on Samsung S9/S9+ when optimisation for near scanning is enabled
- fixed autofocus problems in `Field by field` scanning on Huawei P20 pro, Huawei P20 and Huawei P20 lite 
- fixed bug which caused that results from the previous scan are cleared when the scan activity is run again and entities which have produced results are not used in the new scan
- various other bug fixes and improvements

## 4.0.0
- new API, which is not backward compatible. Please check [README](README.md) and updated demo applications for more information, but the gist of it is:
    - `RecognizerView` has been renamed to `RecognizerRunnerView` and `Recognizer` singleton to `RecognizerRunner`
    - `SegmentScanActivity` has been renamed to `FieldByFieldScanActivity`
    - `RandomScanActivity` does not exist anymore
    - previously internal `Recognizer` objects are not internal anymore - instead of having opaque `RecognizerSettings` and `RecognizerResult` objects, you now have stateful `Recognizer` object that contains its `Result` within and mutates it while performing recognition. 
        - similarly we now have stateful `Parser` and `Detector` objects
        - introduced new `Processor` object type
        - For more information, see [README](README.md), updated demo applications and [this blog post](https://microblink.com/blog/major-change-of-the-api-and-in-the-license-key-formats)
    - added `RecognizerRunnerFragment` with support for various scanning overlays in a manner similar to iOS API. This now allows you to use built-in UI, which was previously strictly available for built-in activities, in form of fragment anywhere within your activity. Full details are given in [README](README.md) and in updated demo applications.
- new licence format, which is not backward compatible. Full details are given in [README](README.md) and in updated applications, but the gist of it is:
    - licence can now be provided with either file, byte array or base64-encoded bytes
- introduced preview version of next generation `DeepOCR`. Check _BlinkInputRawOcrSample_ to see how it can be enabled.


## 3.4.0

### New features:
- Added `VinRecognizer` for scanning VIN (*Vehicle Identification Number*) barcodes
- Added unified `BarcodeRecognizer` for scanning various tipes of barcodes
    - `ZXingRecognizer` and  `BarDecoderRecognizer` are **deprecated**, `BarcodeRecognizer` should be used for all barcode types that are supported by these recognizers
- added support for reading mirrored QR codes:
	- affects all recognizers that perform QR code scanning
- introduced `GlareDetector` which is by default used in all recognizers whose settings implement `GlareDetectorOptions`:
    - when glare is detected, OCR will not be performed on the affected document position to prevent errors in the extracted data
    - if the glare detector is used and obtaining of glare metadata is enabled in `MetadataSettings`, glare status will be reported to `MetadataListener`
    - glare detector can be disabled by using `setDetectGlare(boolean)` method on the recognizer settings
- added `QuadDetectorWithSizeResult` which inherits existing `QuadDetectorResult`:
    - it's subclasses are `DocumentDetectorResult` and `MRTDDetectorResult`
    - returns information about physical size (height) in inches of the detected location when physical size is known
- added support for characters `{` and `}` in `OCR_FONT_VERDANA` to OCR engine

### Minor API changes:
- Date fields in recognition results are returned as `com.microblink.results.date.Date` class which represents immutable dates that are consisted of day, month and year
- `OcrLine.getChars()` method returns `CharWithVariants` array. OCR char is defined by all its parameters (value, font, position, quality, etc.) and for each resulting char it is possible to have multiple variants. For example it is possible to have same char value with different font.
- `RegexParserSettings` and `RawParserSettings` now work with `AbstractOCREngineOptions`, which is a base class of `BlinkOCREngineOptions`
	- default engine options returned by method `getOcrEngineOptions` for both parser settings return instance of `BlinkOCREngineOptions`
- `BlinkOCRRecognizerSettings` is now deprecated and will be removed in `v4.0.0`
	- use `DetectorRecognizerSettings` to perform scanning of templated documents
	- use `BlinkInputRecognizerSettings` for segment scan or for full-screen OCR
	- until `v4.0.0`, `BlinkOCRRecognizerSettings` will behave as before, however you are encouraged to update your code not to use it anymore
- `DocumentClassifier` interface is moved from `com.microblink.recognizers.blinkocr` to `com.microblink.recognizers.detector` package and `DocumentClassifier.classifyDocument()` now accepts `DetectorRecognitionResult` as parameter for document classification

### Improvements for existing features:
- improved `TopUpParser`:
    - added option to return USSD code without prefix
- improved `IbanParser`:
    - improved extraction of IBANs without prefix and introduced `setAlwaysReturnPrefix` option to always return prefix (country code)
    - added support for french IBANs
- enabled reading of Pdf417 barcodes having width/height bar aspect ratio less than 2:1
- improved date parsing:
	- affects date parser and all recognizers which perform date parsing

### Bug fixes:
- fixed returning of images inside TemplatingAPI for frames when document was not correctly detected
- Fixed bug in SegmentScanActivity:
    - scan results are no longer hidden on shake event

## 3.3.0

- optimised native binary size
    - 33% size reduction for `arm64-v8a` ABI
    - 39% size reduction for `armeabi-v7a` ABI
    - 34% size reduction for `x86` ABI
    - 31% size reduction for `x86_64` ABI
- `LibBlinkInput` is now fully ProGuard-compatible, i.e. you no longer need to exclude `com.microblink.**` classes in your ProGuard configuration
- removed support for Android 2.3 and Android 4.0 - minimum required Android version is now Android 4.1 (API level 16)
	- devices with Android 4.0 and earlier take [less than 2% of market share](https://developer.android.com/about/dashboards/index.html#Platform) and is very costly to support them
- removed `isItalic` and `isBold` getters from `OcrChar` class
    - they always returned `false`, since OCR engine cannot accurately detect that
- removed `setLineGroupingEnabled` and `isLineGroupingEnabled` from `BlinkOCREngineOptions` because disabling line grouping completely destroyed the OCR accuracy
- improved `TopUpParser`:
    - added option to enable all prefixes at the same time (generic prefix)
    - added suport for 14 digits long sim numbers in addition to existing lengths (12, 19, 20)
- `DateParser` can parse dates with month names in English (either full or abbreviated), if this option is enabled
- added support for polish IBAN without PL prefix to `IBANParser`
- prefixed custom attributes to avoid name collisions with attributes from other libraries:
    - `CameraViewGroup`: renamed animateRotation to `mb_animateRotation`, animationDuration to `mb_animationDuration`, rotatable to `mb_rotatable`
    - `BaseCameraView`:  renamed initialOrientation to `mb_initialOrientation`, aspectMode to `mb_aspectMode`

## 3.2.0

- added support for Android 7 multi-window mode
- fixed autofocus bug on Huawei Honor 8
- fixed black camera on Motorola Moto Z
- made camera focusing more stable on some devices
    - _stable_ means less "jumpy" when searching for focused image
- MobileCoupon parser renamed to TopUp parser

## 3.1.0

- removed `RecognizerView` method `setInitialScanningPaused`. For achieving the same functionality, method `pauseScanning` should be used.
- added support for scanning IBANs that contain spaces and dashes
- added support for scanning IBAN from Georgia in Segment Scan
- added support for cancelling ongoing DirectAPI recognition call

## 3.0.0

- _LibRecognizer.aar_ renamed to _LibBlinkInput.aar_
- _libBlinkOCR.so_ renamed to _libBlinkInput.so_
- workaround for camera bug on some samsung devices
- fixed camera bug on LG X Cam
- fixed camera bug on OnePlus 3
- fixed rare NPE in SegmentScanActivity
- improved IBAN parser
- improved amount parser
	- amount parser settings now does not have _expectAsterixOrEqualsPrefix_ and _expectCurrencySymbol_ anymore - this is now handled automatically
- migrated to libc++ native runtime and used clang from NDKr13b for building the native code
    - this enabled c++14 features which will help us yield much better performance in the future

## 2.8.0
- added `MobileCouponsParser` for reading prepaid codes from mobile phone coupons
- `DateParser` returns result as java `Date` object and as original date `String`
- added method `getSpecificParsedResult` to `TemplatingRecognitionResult` (`BlinkOCRRecognitionResult`) which returns specific parser results, e.g. java `Date` for `DateParser`
- enabled reading of longer ITF barcodes

## 2.7.0
- support for parsing Vehicle Identification Numbers (VINs)
- renamed BlinkOCRActivity to SegmentScanActivity
- added RandomScanActivity which is similar to SegmentScanActivity but it does not force the user to scan text segments in the predefined order
- improved autofocus support on SGS6 and SGS7
- fixed memory leak in RecognitionProcessCallback, leak was caused by Recognizer singleton holding reference to both Context and MetadataListener even after termination

## 2.6.0
- added support for using detectors to perform detection of various documents
- added support for combination of detectors with BlinkOCR recognizer to perform OCR of only parts of detected documents
    - refer to documentation and demo apps for example
- updated BlinkOCRActivity, through ScanConfiguration it is now possible to make some scan field optional (user can skip it) and set the field size

## 2.5.0
- FailedDetectionMetadata, PointsDetectionMetadata and QuadDetectionMetadata have been replaced with DetectionMetadata which now holds a DetectorResult
    - DetectorResult is more flexible as it allows more different detection types to be added in future
- fixed several possible crashes in camera management
- fixed autofocus bug on LG devices when metering areas or non-default zoom level were set
- fixed autofocus bug on LG G4 (not related to bug above)
- added option to set the OCR document type in BlinkOCREngineOptions
- introduced new setting options for generic AmountParser:
    - set parser to Arabic-Indic mode (amounts with Arabic-Indic digits)
    - allow amounts with space separated digit groups (thousands)
    - set ideal (expected) number of digits before decimal point
- added factory method `createFromPreset` to generic AmountParserSettings that creates the settings from one of the available presets (`GENERIC`, `LARGE_AMOUNT`) 

## 2.4.0
- reconfigureRecognizers method now throws an error if phone does not have autofocus and at least one of new recognizers require it
- raw resources are now packed as assets
- fixed bug with isScanningPaused which sometimes returned bogus value and caused scanning to work even if initial scanning was set to be paused
- support for scanning custom camera frames via DirectAPI
- fixed bug on some devices causing it to never start scanning if device was not shaken
- increased OCR engine initialisation speed
- improved Frame Quality Estimation on low-end devices (fixed regression introduced in v1.6.0)
- added new options to BlinkOcrEngineOptions
- added RegexParser which can parse almost any regular expression from OCR result

## 2.3.0
- support detecting on activity flip event
- fixed crash in RecognizerCompatibility on ARMv7 without NEON
- added RecognizerCompatibility to javadoc
- fixed NPE in BarcodeDetailedData

## 2.2.0
- improved performance of conversion of `Image` object into `Bitmap`
- fixed crash that could be caused by quickly restarting camera activity
- fixed bug in camera layout in certain aspect ratios of camera view
- fixed bug in segment scan when put on landscape activity
- fixed bug in handling `setMeteringAreas`
- `setMeteringAreas` now receives boolean indicating whether set areas should be rotated with device
- added support for specifying camera aspect mode from XML

## 2.1.0
- fixed crash that occurred when detection image was being sent from native to Java
- fixed issue that cause irrationaly refusal to scan anything on high-end devices
- fixed bug in scanning 1D barcodes with ZXing when scanning region was set
	- the bug caused not to honor set region
- when embedding `RecognizerView` into custom scan activity, you no longer need to take care of whether runtime permissions are set or not. You can now simply pass all lifecycle events to `RecognizerView` and if camera permission is denied, a new callback method `onCameraPermissionDenied` of `CameraEventsListener` will be invoked to give you chance to ask user for permissions
	- please refer to updated demo apps for example of new callback
- **IMPORTANT** - `onScanningDone` callback method does not automatically pause scanning loop anymore. As soon as `onScanningDone` method ends, scanning will be automatically resumed without resetting state
	- if you need to reset state, please call `resetRecognitionState` in your implementation of `onScanningDone`
	- if you need to have scanning paused after `onScanningDone` ends, please call `pauseScanning` in your implementation of `onScanningDone`. Do not forget to call `resumeScanning` to resume scanning after it has been paused.
- `pauseScanning` and `resumeScanning` calls are now counted, i.e. if you call `pauseScanning` twice, you will also need to call `resumeScanning` twice to actually resume scanning
	- this is practical if you show multiple onboarding views over camera and you want the scanning paused while each is shown and you do not know in which order they will be dismissed. Now you can simply call `pauseScanning` on showing the onboarding view and `resumeScanning` on dismissing it, regardless of how many onboarding views you have
	- if you want to show onboarding help first time your scan activity starts, you can call `setInitialScanningPaused(true)` which will ensure that first time camera is started, the scanning will not automatically start - you will need to call `resumeScanning` to start scanning after your onboarding view is dismissed
- added support for `x86_64` architecture


## 2.0.0
- new API which is easier to understand, but is not backward compatible. Please check [README](README.md) and updated demo applications for more information.
- removed support for ARMv7 devices which do not support NEON SIMD
	- this enabled us to increase recognition speed at cost of not supporting old devices like those using NVIDIA Tegra 2
	- you can check [this article](https://microblink.zendesk.com/hc/en-us/articles/206113151-Removing-support-for-devices-without-NEON-SIMD-extensions) for more information about NEON and why we use it
- added official support for Android 6.0 and it's runtime camera permissions
	- if using provided _BlinkOCRActivity_, the logic behind asking user to give camera permission is handled internally
	- if integrating using custom UI, you are required to ask user to give you permission to use camera. To make this easier, we have provided a _CameraPermissionManager_ class which does all heavylifting code about managing states when asking user for camera permission. Refer to demo apps to see how it is used.
- BlinkOCR now depends on appcompat-v7 library, instead of full android-support library.
	- even older versions of BlinkOCR required only features from appcompat-v7 so we now decided to make appcompat-v7 as dependency because it is much smaller than full support library and is also default dependency of all new Android apps.
- completely rewritten JNI layer which now gives much lower overhead in communication between Java and native code
	- in our internal tests, this yielded up to 3 times better performance in OCR
- fixed issue with Nexus 5X upside down camera
- when using DirectAPI, recognized Bitmap is not recycled anymore so it can be reused


## 1.9.0
- fixed auto-exposure bug on Nexus 6
- improved OCR quality and performance
- fixed autofocus issue on devices that do not support continuous autofocus
- support for defining camera video resolution preset
	- to define video resolution preset via Intent, use `BlinkOCRActivity.EXTRAS_CAMERA_VIDEO_PRESET`
	- to define video resolution preset on `RecognizerView`, use method `setVideoResolutionPreset`

## 1.8.0
- added support for scanning barcodes
- fixed race condition causing memory leak or rare crashes
- fixed `NullPointerException` in `BaseCameraView.dispatchTouchEvent`
- fixed bug that caused returning scan result from old video frame
- fixed `NullPointerException` in camera2 management
- fixed rare race condition in gesture recognizer
- fixed segmentation fault on recognizer reconfiguration operation
- fixed freeze when camera was being quickly turned on and off
- fixed bug in IBAN parser that caused not to parse IBAN's that contain letters, such as in UK, Ireland or Netherlands
- ensured `RecognizerView` lifecycle methods are called on UI thread
- ensure `onCameraPreviewStarted` is not called if camera is immediately closed after start before the call should have taken place
- ensure `onScanningDone` is not called after `RecognizerView` has been paused, even if it had result ready just before pausing
- default maximum number of chars in `raw parser` is now 3000 (it used to be 600)
- it is now possible to define maximum allowed number of char recognition variants via `BlinkOCREngineOptions`. Default value is `0`.
- when calling `onDisplayOcrResult` callback, make sure OCR char recognition variants are not sent to Java - this is both slow and not required
- added support for using _BlinkOCR_ as camera capture API. To do that, implement following:
	- when using `RecognizerView` do not call `setRecognitionSettings` or call it with `null` or empty array
	- implement `ImageListener` interface and set the listener with `setImageListener`
	- as a reminder - you can process video frames obtained that way using direct API method `recognizeImageWithSettings`
- reorganized integration demo apps
	- `BlinkOCRSegmentDemo` shows how to use simple Intent-based API to scan little text segments. It also shows you how to create a custom scan activity for scanning little text segments.
	- `BlinkOCRFullScreen` shows how to perform full camera frame generic OCR, how to draw OCR results on screen and how to obtain `OcrResult` object for further processing. This app also shows how to scan Code128 or Code39 barcode on same screen that is used for OCR.
	- `BlinkOCRDirectAPI` shows how to perform OCR of `Bitmap` object obtained from camera
	- all demo apps now use Maven integration method because it is much easier than importing AAR manually
- **removed** parsers specific to country standards - these are now available as part of our [PhotoPay](https://microblink.com/photopay) product
	- removed croatian parsers
	- removed serbian parsers
	- removed macedonian parsers
	- removed swedish parsers

## 1.7.1
- fixed NullPointerException when RecognizerSettings array element was `null`

## 1.7.0
- [ImageListener](https://blinkocr.github.io/blinkocr-android/com/microblink/image/ImageListener.html) can now receive [DEWARPED](https://blinkocr.github.io/blinkocr-android/com/microblink/image/ImageType.html#DEWARPED) images and [Image](https://blinkocr.github.io/blinkocr-android/com/microblink/image/Image.html) now contains information about its [Orientation](https://blinkocr.github.io/blinkocr-android/com/microblink/hardware/orientation/Orientation.html)
- [recognizeBitmap](https://blinkocr.github.io/blinkocr-android/com/microblink/view/recognition/RecognizerView.html#recognizeBitmap(android.graphics.Bitmap, com.microblink.view.recognition.ScanResultListener)) method can now receive orientation of given [Bitmap](https://developer.android.com/reference/android/graphics/Bitmap.html)
- it is now possible to recognize [Image](https://blinkocr.github.io/blinkocr-android/com/microblink/image/Image.html) objects directly, without slow conversion into [Bitmap](https://developer.android.com/reference/android/graphics/Bitmap.html)
- removed method `resumeScanningWithoutStateReset` - method `resumeScanning` of [RecognizerView](https://blinkocr.github.io/blinkocr-android/com/microblink/view/recognition/RecognizerView.html) now receives `boolean` indicating whether internal state should be reset

## 1.6.0
- removed dependency to deprecated [Horizontal Variable ListView](https://github.com/sephiroth74/HorizontalVariableListView) - default activity now only requires [Android support library](https://developer.android.com/tools/support-library/index.html)
- new and improved android camera management
	- on devices that support it, utilize [Camera2 API](https://developer.android.com/reference/android/hardware/camera2/package-summary.html) for better per frame camera control
	- new and improved algorithm for choosing which frame is of good enough quality to be processed - there is now less latency from initialization of camera until first scan result

## 1.5.0
- added support for Macedonian parsers
- added date parser
- fixed crash in DirectAPI when recognizer was terminated in the middle of recognition process
- removed support for ARMv6 processors because those are too slow for OCR

## 1.4.0
- added support for Serbian parsers
- fixed camera orientation detection when RecognizerView is not initialized with Activity context

## 1.3.0
- added support for Croatian and Swedish parsers
- added more fonts to OCR model file

## 1.2.0
- support for controlling camera zoom level
- support "easy" integration mode with provided BlinkOCRActivity - check README
- Raw parser is now more customizable - check javadoc for class RawParserSettings
	- character whitelist can now be defined
	- maximum and minimum height of text line can be defined
	- color vs. grayscale image processing support
	- maximum expected number of chars can now be defined

## 1.1.0
- support for parsing e-mails
- introduced new licence key format (generate your free licence key on [https://microblink.com/login](https://microblink.com/login) or contact us at [http://help.microblink.com](http://help.microblink.com)

## 1.0.0

- Initial release
- Support for parsing amounts and IBANs