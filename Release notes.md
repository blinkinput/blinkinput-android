# Release notes

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