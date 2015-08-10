# Release notes

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