# Release notes

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