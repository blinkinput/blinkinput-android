# _BlinkOCR_ SDK for Android

[![Build Status](https://travis-ci.org/BlinkOCR/blinkocr-android.svg?branch=master)](https://travis-ci.org/BlinkOCR/blinkocr-android)

_BlinkOCR_ SDK for Android is SDK that enables you to easily add near real time OCR functionality to your app. With provided camera management you can easily create an app that scans receipts, e-mails and much more. As of version `1.8.0` you can also scan barcodes when using [custom UI integration](#recognizerView). You can also scan images stored as [Android Bitmaps](http://developer.android.com/reference/android/graphics/Bitmap.html) that are loaded either from gallery, network or SD card.

With _BlinkOCR_ you can scan free-form text or specialized formats like dates, amounts, e-mails and much more. Using specialized formats yields much better scanning quality than using free-form text mode.

Using _BlinkOCR_ in your app requires a valid license key. You can obtain a trial license key by registering to [Microblink dashboard](https://microblink.com/login). After registering, you will be able to generate a license key for your app. License key is bound to [package name](http://tools.android.com/tech-docs/new-build-system/applicationid-vs-packagename) of your app, so please make sure you enter the correct package name when asked.

See below for more information about how to integrate _BlinkOCR_ SDK into your app and also check latest [Release notes](Release notes.md).

# Table of contents

* [Android _BlinkOCR_ integration instructions](#intro)
* [Quick Start](#quickStart)
  * [Quick start with demo app](#quickDemo)
  * [Integrating _BlinkOCR_ into your project using Maven](#mavenIntegration)
  * [Android studio integration instructions](#quickIntegration)
  * [Eclipse integration instructions](#eclipseIntegration)
  * [Performing your first segment scan](#quickScan)
* [Advanced _BlinkOCR_ integration instructions](#advancedIntegration)
  * [Checking if _BlinkOCR_ is supported](#supportCheck)
  * [Customization of `BlinkOCRActivity` activity](#segmentScanActivityCustomization)
  * [Embedding `RecognizerView` into custom scan activity](#recognizerView)
  * [`RecognizerView` reference](#recognizerViewReference)
* [Using direct API for recognition of Android Bitmaps](#directAPI)
  * [Understanding DirectAPI's state machine](#directAPIStateMachine)
  * [Using DirectAPI while RecognizerView is active](#directAPIWithRecognizer)
  * [Obtaining various metadata with _MetadataListener_](#metadataListener)
  * [Using ImageListener to obtain images that are being processed](#imageListener)
* [Recognition settings and results](#recognitionSettingsAndResults)
  * [[Recognition settings](https://blinkocr.github.io/blinkocr-android/com/microblink/recognizers/settings/RecognitionSettings.html)](#recognitionSettings)
  * [Scanning segments with BlinkOCR recognizer](#blinkOCR)
  * [Scanning PDF417 barcodes](#pdf417Recognizer)
  * [Scanning one dimensional barcodes with _BlinkOCR_'s implementation](#custom1DBarDecoder)
  * [Scanning barcodes with ZXing implementation](#zxing)
* [Processor architecture considerations](#archConsider)
  * [Reducing the final size of your app](#reduceSize)
  * [Combining _BlinkOCR_ with other native libraries](#combineNativeLibraries)
* [Troubleshooting](#troubleshoot)
  * [Integration problems](#integrationTroubleshoot)
  * [SDK problems](#sdkTroubleshoot)
* [Additional info](#info)

# <a name="intro"></a> Android _BlinkOCR_ integration instructions

The package contains Android Archive (AAR) that contains everything you need to use _BlinkOCR_ library. This AAR is also available in maven repository for easier integration into your app. For more information about maven integration procedure, check [maven integration section](#mavenIntegration).

Besides AAR, package also contains a demo project that contains following modules:

- _BlinkOCRSegmentDemo_ shows how to use simple Intent-based API to scan little text segments. It also shows you how to create a custom scan activity for scanning little text segments.
- _BlinkOCRFullScreen_ shows how to perform full camera frame generic OCR, how to draw OCR results on screen and how to obtain [OcrResult](https://blinkocr.github.io/blinkocr-android/com/microblink/results/ocr/OcrResult.html) object for further processing. This app also shows how to scan Code128 or Code39 barcode on same screen that is used for OCR.
- _BlinkOCRDirectAPI_ shows how to perform OCR of [Android Bitmaps](https://developer.android.com/reference/android/graphics/Bitmap.html)
- _BlinkOCRCombination_ shows how to perform OCR of camera frame, obtain that same camera frame and process it again with DirectAPi. You can test this app with [PDF within demo app folder](BlinkOCRDemo/BlinkOCRCombination/combinationScan.pdf).
 
Source code of all demo apps is given to you to show you how to perform integration of _BlinkOCR_ SDK into your app. You can use this source code and all resources as you wish. You can use demo apps as basis for creating your own app, or you can copy/paste code and/or resources from demo apps into your app and use them as you wish without even asking us for permission.

_BlinkOCR_ is supported on Android SDK version 10 (Android 2.3.3) or later.

The library contains one activity: `BlinkOCRActivity`. It is responsible for camera control and recognition of small segments. It is ideal if you need to quickly scan small text segments, like date, amount or e-mail. 

For advanced use cases, you will need to embed `RecognizerView` into your activity and pass activity's lifecycle events to it and it will control the camera and recognition process. For more information, see [Embedding `RecognizerView` into custom scan activity](#recognizerView).

# <a name="quickStart"></a> Quick Start

## <a name="quickDemo"></a> Quick start with demo app

1. Open Android Studio.
2. In Quick Start dialog choose _Import project (Eclipse ADT, Gradle, etc.)_.
3. In File dialog select _BlinkOCRDemo_ folder.
4. Wait for project to load. If Android studio asks you to reload project on startup, select `Yes`.

## <a name="mavenIntegration"></a> Integrating _BlinkOCR_ into your project using Maven

Maven repository for _BlinkOCR_ SDK is: [http://maven.microblink.com](http://maven.microblink.com). If you do not want to perform integration via Maven, simply skip to [Android Studio integration instructions](#quickIntegration) or [Eclipse integration instructions](#eclipseIntegration).

### Using gradle or Android Studio

In your `build.gradle` you first need to add _BlinkOCR_ maven repository to repositories list:

```
repositories {
	maven { url 'http://maven.microblink.com' }
}
```

After that, you just need to add _BlinkOCR_ as a dependency to your application (make sure, `transitive` is set to true):

```
dependencies {
    compile('com.microblink:blinkocr:2.5.0@aar') {
    	transitive = true
    }
}
```

If you plan to use ProGuard, add following lines to your `proguard-rules.pro`:
	
```
-keep class com.microblink.** { *; }
-keepclassmembers class com.microblink.** { *; }
-dontwarn android.hardware.**
-dontwarn android.support.v4.**
```

### Using android-maven-plugin

[Android Maven Plugin](https://simpligility.github.io/android-maven-plugin/) v4.0.0 or newer is required.

Open your `pom.xml` file and add these directives as appropriate:

```xml
<repositories>
   	<repository>
       	<id>MicroblinkRepo</id>
       	<url>http://maven.microblink.com</url>
   	</repository>
</repositories>

<dependencies>
	<dependency>
		  <groupId>com.microblink</groupId>
		  <artifactId>blinkocr</artifactId>
		  <version>2.5.0</version>
		  <type>aar</type>
  	</dependency>
</dependencies>
```

## <a name="quickIntegration"></a> Android studio integration instructions

1. In Android Studio menu, click _File_, select _New_ and then select _Module_.
2. In new window, select _Import .JAR or .AAR Package_, and click _Next_.
3. In _File name_ field, enter the path to _LibRecognizer.aar_ and click _Finish_.
4. In your app's `build.gradle`, add dependency to `LibRecognizer` and appcompat-v7:

	```
	dependencies {
   		compile project(':LibRecognizer')
 		compile "com.android.support:appcompat-v7:23.2.1"
	}
	```
5. If you plan to use ProGuard, add following lines to your `proguard-rules.pro`:
	
	```
	-keep class com.microblink.** { *; }
	-keepclassmembers class com.microblink.** { *; }
	-dontwarn android.hardware.**
	-dontwarn android.support.v4.**
	```
	
## <a name="eclipseIntegration"></a> Eclipse integration instructions

We do not provide Eclipse integration demo apps. We encourage you to use Android Studio. We also do not test integrating _BlinkOCR_ with Eclipse. If you are having problems with _BlinkOCR_, make sure you have tried integrating it with Android Studio prior contacting us.

However, if you still want to use Eclipse, you will need to convert AAR archive to Eclipse library project format. You can do this by doing the following:

1. In Eclipse, create a new _Android library project_ in your workspace.
2. Clear the `src` and `res` folders.
3. Unzip the `LibRecognizer.aar` file. You can rename it to zip and then unzip it using any tool.
4. Copy the `classes.jar` to `libs` folder of your Eclipse library project. If `libs` folder does not exist, create it.
5. Copy the contents of `jni` folder to `libs` folder of your Eclipse library project.
6. Replace the `res` folder on library project with the `res` folder of the `LibRecognizer.aar` file.

You’ve already created the project that contains almost everything you need. Now let’s see how to configure your project to reference this library project.

1. In the project you want to use the library (henceforth, "target project") add the library project as a dependency
2. Open the `AndroidManifest.xml` file inside `LibRecognizer.aar` file and make sure to copy all permissions, features and activities to the `AndroidManifest.xml` file of the target project.
3. Copy the contents of `assets` folder from `LibRecognizer.aar` into `assets` folder of target project. If `assets` folder in target project does not exist, create it.
4. Clean and Rebuild your target project
5. If you plan to use ProGuard, add same statements as in [Android studio guide](#quickIntegration) to your ProGuard configuration file.
6. Add appcompat-v7 library to your workspace and reference it by target project (modern ADT plugin for Eclipse does this automatically for all new android projects).

## <a name="quickScan"></a> Performing your first segment scan
1. You can start recognition process by starting `BlinkOCRActivity` activity with Intent initialized in the following way:
	
	```java
	// Intent for BlinkOCRActivity Activity
	Intent intent = new Intent(this, BlinkOCRActivity.class);
	
	// set your licence key
	// obtain your licence key at http://microblink.com/login or
	// contact us at http://help.microblink.com
	intent.putExtra(BlinkOCRActivity.EXTRAS_LICENSE_KEY, "Add your licence key here");

	// setup array of scan configurations. Each scan configuration
	// contains 4 elements: resource ID for title displayed
	// in BlinkOCRActivity activity, resource ID for text
	// displayed in activity, name of the scan element (used
	// for obtaining results) and parser setting defining
	// how the data will be extracted.
	// For more information about parser setting, check the
	// chapter "Scanning segments with BlinkOCR recognizer"
	ScanConfiguration[] confArray = new ScanConfiguration[] {
                new ScanConfiguration(R.string.amount_title, R.string.amount_msg, "Amount", new AmountParserSettings()),
                new ScanConfiguration(R.string.email_title, R.string.email_msg, "EMail", new EMailParserSettings()),
                new ScanConfiguration(R.string.raw_title, R.string.raw_msg, "Raw", new RawParserSettings())
        };
	intent.putExtra(BlinkOCRActivity.EXTRAS_SCAN_CONFIGURATION, confArray);

	// Starting Activity
	startActivityForResult(intent, MY_REQUEST_CODE);
	```
2. After `BlinkOCRActivity` activity finishes the scan, it will return to the calling activity and will call method `onActivityResult`. You can obtain the scanning results in that method.

	```java
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		if (requestCode == MY_REQUEST_CODE) {
			if (resultCode == BlinkOCRActivity.RESULT_OK && data != null) {
				// perform processing of the data here
				
				// for example, obtain parcelable recognition result
				Bundle extras = data.getExtras();
				Bundle results = extras.getBundle(BlinkOCRActivity.EXTRAS_SCAN_RESULTS);
				
				// results bundle contains result strings in keys defined
				// by scan configuration name
				// for example, if set up as in step 1, then you can obtain
				// e-mail address with following line
				String email = results.getString("EMail");
			}
		}
	}
	```

# <a name="advancedIntegration"></a> Advanced _BlinkOCR_ integration instructions
This section will cover more advanced details in _BlinkOCR_ integration. First part will discuss the methods for checking whether _BlinkOCR_ is supported on current device. Second part will cover the possible customization of builtin `BlinkOCRActivity` activity, third part will describe how to embed `RecognizerView` into your activity and fourth part will describe how to use direct API to recognize directly android bitmaps without the need of camera.

## <a name="supportCheck"></a> Checking if _BlinkOCR_ is supported

### _BlinkOCR_ requirements
Even before starting the scan activity, you should check if _BlinkOCR_ is supported on current device. In order to be supported, device needs to have camera. 

Android 2.3 is the minimum android version on which _BlinkOCR_ is supported.

Camera video preview resolution also matters. In order to perform successful scans, camera preview resolution cannot be too low. _BlinkOCR_ requires minimum 480p camera preview resolution in order to perform scan. It must be noted that camera preview resolution is not the same as the video record resolution, although on most devices those are the same. However, there are some devices that allow recording of HD video (720p resolution), but do not allow high enough camera preview resolution (for example, [Sony Xperia Go](http://www.gsmarena.com/sony_xperia_go-4782.php) supports video record resolution at 720p, but camera preview resolution is only 320p - _BlinkOCR_ does not work on that device).

_BlinkOCR_ is native application, written in C++ and available for multiple platforms. Because of this, _BlinkOCR_ cannot work on devices that have obscure hardware architectures. We have compiled _BlinkOCR_ native code only for most popular Android [ABIs](https://en.wikipedia.org/wiki/Application_binary_interface). See [Processor architecture considerations](#archConsider) for more information about native libraries in _BlinkOCR_ and instructions how to disable certain architectures in order to reduce the size of final app.

### Checking for _BlinkOCR_ support in your app
To check whether the _BlinkOCR_ is supported on the device, you can do it in the following way:
	
```java
// check if BlinkOCR is supported on the device
RecognizerCompatibilityStatus status = RecognizerCompatibility.getRecognizerCompatibilityStatus(this);
if(status == RecognizerCompatibilityStatus.RECOGNIZER_SUPPORTED) {
	Toast.makeText(this, "BlinkOCR is supported!", Toast.LENGTH_LONG).show();
} else {
	Toast.makeText(this, "BlinkOCR is not supported! Reason: " + status.name(), Toast.LENGTH_LONG).show();
}
```

## <a name="segmentScanActivityCustomization"></a> Customization of `BlinkOCRActivity` activity

### `BlinkOCRActivity` intent extras

This section will discuss possible parameters that can be sent over `Intent` for `BlinkOCRActivity` activity that can customize default behaviour. There are several intent extras that can be sent to `BlinkOCRActivity` actitivy:
	
* **`BlinkOCRActivity.EXTRAS_SCAN_CONFIGURATION`** - with this extra you must set the array of [ScanConfiguration](https://blinkocr.github.io/blinkocr-android/com/microblink/ocr/ScanConfiguration.html) objects. Each `ScanConfiguration` object will define specific scan configuration that will be performed. `ScanConfiguration` defines two string resource ID's - title of the scanned item and text that will be displayed above field where scan is performed. Besides that it defines the name of scanned item and object defining the OCR parser settings. More information about parser settings can be found in chapter [Scanning segments with BlinkOCR recognizer](#blinkOCR). Here is only important that each scan configuration represents a single parser group and BlinkOCRActivity ensures that only one parser group is active at a time. After defining scan configuration array, you need to put it into intent extra with following code snippet:
	
	```java
	intent.putExtra(BlinkOCRActivity.EXTRAS_SCAN_CONFIGURATION, confArray);
	```
	
* **`BlinkOCRActivity.EXTRAS_SCAN_RESULTS`** - you can use this extra in `onActivityResult` method of calling activity to obtain bundle with recognition results. Bundle will contain only strings representing scanned data under keys defined with each scan configuration. If you also need to obtain OCR result structure, then you need to perform [advanced integration](#recognizerView). You can use the following snippet to obtain scan results:

	```java
	Bundle results = data.getBundle(BlinkOCRActivity.EXTRAS_SCAN_RESULTS);
	```
	
* **`BlinkOCRActivity.EXTRAS_HELP_INTENT`** - with this extra you can set fully initialized intent that will be sent when user clicks the help button. You can put any extras you want to your intent - all will be delivered to your activity when user clicks the help button. If you do not set help intent, help button will not be shown in camera interface. To set the intent for help activity, use the following code snippet:
	
	```java
	/** Set the intent which will be sent when user taps help button. 
	 *  If you don't set the intent, help button will not be shown.
	 *  Note that this applies only to default PhotoPay camera UI.
	 * */
	intent.putExtra(BlinkOCRActivity.EXTRAS_HELP_INTENT, new Intent(this, HelpActivity.class));
	```
* **`BlinkOCRActivity.EXTRAS_CAMERA_VIDEO_PRESET`** - with this extra you can set the video resolution preset that will be used when choosing camera resolution for scanning. For more information, see [javadoc](https://blinkocr.github.io/blinkocr-android/com/microblink/hardware/camera/VideoResolutionPreset.html). For example, to use 720p video resolution preset, use the following code snippet:

	```java
	intent.putExtra(BlinkOCRActivity.EXTRAS_CAMERA_VIDEO_PRESET, (Parcelable)VideoResolutionPreset.VIDEO_RESOLUTION_720p);
	```

* **`BlinkOCRActivity.EXTRAS_LICENSE_KEY`** - with this extra you can set the license key for _BlinkOCR_. You can obtain your licence key from [Microblink website](http://microblink.com/login) or you can contact us at [http://help.microblink.com](http://help.microblink.com). Once you obtain a license key, you can set it with following snippet:

	```java
	// set the license key
	intent.putExtra(BlinkOCRActivity.EXTRAS_LICENSE_KEY, "Enter_License_Key_Here");
	```
	
	Licence key is bound to package name of your application. For example, if you have licence key that is bound to `com.microblink.ocr` app package, you cannot use the same key in other applications. However, if you purchase Premium licence, you will get licence key that can be used in multiple applications. This licence key will then not be bound to package name of the app. Instead, it will be bound to the licencee string that needs to be provided to the library together with the licence key. To provide licencee string, use the `EXTRAS_LICENSEE` intent extra like this:

	```java
	// set the license key
	intent.putExtra(BlinkOCRActivity.EXTRAS_LICENSE_KEY, "Enter_License_Key_Here");
	intent.putExtra(BlinkOCRActivity.EXTRAS_LICENSEE, "Enter_Licensee_Here");
	```

* **`BlinkOCRActivity.EXTRAS_SHOW_OCR_RESULT`** - with this extra you can define whether OCR result should be drawn on camera preview as it arrives. This is enabled by default, to disable it, use the following snippet:

	```java
	// enable showing of OCR result
	intent.putExtra(BlinkOCRActivity.EXTRAS_SHOW_OCR_RESULT, false);
	```

* **`BlinkOCRActivity.EXTRAS_SHOW_OCR_RESULT_MODE`** - if OCR result should be drawn on camera preview, this extra defines how it will be drawn. Here you need to pass instance of [ShowOcrResultMode](https://blinkocr.github.io/blinkocr-android/com/microblink/activity/ShowOcrResultMode.html). By default, `ShowOcrResultMode.ANIMATED_DOTS` is used. You can also enable `ShowOcrResultMode.STATIC_CHARS` to draw recognized chars instead of dots. To set this extra, use the following snippet:

	```java
	// display colored static chars instead of animated dots
	intent.putExtra(BlinkOCRActivity.EXTRAS_SHOW_OCR_RESULT_MODE, (Parcelable) ShowOcrResultMode.STATIC_CHARS);
	```

* **`BlinkOCRActivity.EXTRAS_IMAGE_LISTENER`** - with this extra you can set your implementation of [ImageListener interface](https://blinkocr.github.io/blinkocr-android/com/microblink/image/ImageListener.html) that will obtain images that are being processed. Make sure that your [ImageListener](https://blinkocr.github.io/blinkocr-android/com/microblink/image/ImageListener.html) implementation correctly implements [Parcelable](https://developer.android.com/reference/android/os/Parcelable.html) interface with static [CREATOR](https://developer.android.com/reference/android/os/Parcelable.Creator.html) field. Without this, you might encounter a runtime error. For more information and example, see [Using ImageListener to obtain images that are being processed](#imageListener). By default, _ImageListener_ will receive all possible images that become available during recognition process. This will introduce performance penalty because most of those images will probably not be used so sending them will just waste time. To control which images should become available to _ImageListener_, you can also set [ImageMetadata settings](https://blinkocr.github.io/blinkocr-android/com/microblink/metadata/MetadataSettings.ImageMetadataSettings.html) with `BlinkOCRActivity.EXTRAS_IMAGE_METADATA_SETTINGS`

* **`BlinkOCRActivity.EXTRAS_IMAGE_METADATA_SETTINGS`** - with this extra you can set [ImageMetadata settings](https://blinkocr.github.io/blinkocr-android/com/microblink/metadata/MetadataSettings.ImageMetadataSettings.html) which will define which images will be sent to [ImageListener interface](https://blinkocr.github.io/blinkocr-android/com/microblink/image/ImageListener.html) given via `BlinkOCRActivity.EXTRAS_IMAGE_LISTENER` extra. If _ImageListener_ is not given via Intent, then this extra has no effect. You can see example usage of _ImageMetadata Settings_ in chapter [Obtaining various metadata with _MetadataListener_](#metadataListener) and in provided demo apps.

## <a name="recognizerView"></a> Embedding `RecognizerView` into custom scan activity
This section will discuss how to embed [RecognizerView](https://blinkocr.github.io/blinkocr-android/com/microblink/view/recognition/RecognizerView.html) into your scan activity and perform scan.

1. First make sure that `RecognizerView` is a member field in your activity. This is required because you will need to pass all activity's lifecycle events to `RecognizerView`.
2. It is recommended to keep your scan activity in one orientation, such as `portrait` or `landscape`. Setting `sensor` as scan activity's orientation will trigger full restart of activity whenever device orientation changes. This will provide very poor user experience because both camera and _BlinkOCR_ native library will have to be restarted every time. There are measures for this behaviour and will be discussed [later](#scanOrientation).
3. In your activity's `onCreate` method, create a new `RecognizerView`, define its [settings and listeners](#recognizerViewReference) and then call its `create` method. After that, add your views that should be layouted on top of camera view.
4. Override your activity's `onStart`, `onResume`, `onPause`, `onStop` and `onDestroy` methods and call `RecognizerView's` lifecycle methods `start`, `resume`, `pause`, `stop` and `destroy`. This will ensure correct camera and native resource management. If you plan to manage `RecognizerView's` lifecycle independently of host activity's lifecycle, make sure the order of calls to lifecycle methods is the same as is with activities (i.e. you should not call `resume` method if `create` and `start` were not called first).

Here is the minimum example of integration of `RecognizerView` as the only view in your activity:

```java
public class MyScanActivity extends Activity implements ScanResultListener, CameraEventsListener {
	private static final int PERMISSION_CAMERA_REQUEST_CODE = 69;
	private RecognizerView mRecognizerView;
		
	@Override
	protected void onCreate(Bundle savedInstanceState) {				
		// create RecognizerView
		mRecognizerView = new RecognizerView(this);
		   
		RecognitionSettings settings = new RecognitionSettings();
		// setup array of recognition settings (described in chapter "Recognition 
		// settings and results")
		RecognizerSettings[] settArray = setupSettingsArray();
		if(!RecognizerCompatibility.cameraHasAutofocus(CameraType.CAMERA_BACKFACE, this)) {
			settArray = RecognizerSettingsUtils.filterOutRecognizersThatRequireAutofocus(settArray);
		}
		settings.setRecognizerSettingsArray(settArray);
		mRecognizerView.setRecognitionSettings(settings);
		
		try {
		    // set license key
		    mRecognizerView.setLicenseKey(this, "your license key");
		} catch (InvalidLicenceKeyException exc) {
		    finish();
		    return;
		}
		
		// scan result listener will be notified when scan result gets available
		mRecognizerView.setScanResultListener(this);
		// camera events listener will be notified about camera lifecycle and errors
		mRecognizerView.setCameraEventsListener(this);
		
		// set camera aspect mode
		// ASPECT_FIT will fit the camera preview inside the view
		// ASPECT_FILL will zoom and crop the camera preview, but will use the
		// entire view surface
		mRecognizerView.setAspectMode(CameraAspectMode.ASPECT_FILL);
		   
		mRecognizerView.create();
		
		setContentView(mRecognizerView);
	}
	
	@Override
	protected void onStart() {
	   super.onStart();
	   // you need to pass all activity's lifecycle methods to RecognizerView
	   mRecognizerView.start();
	}
	
	@Override
	protected void onResume() {
	   	super.onResume();
	   	// you need to pass all activity's lifecycle methods to RecognizerView
       mRecognizerView.resume();
	}

	@Override
	protected void onPause() {
	   	super.onPause();
	   	// you need to pass all activity's lifecycle methods to RecognizerView
		mRecognizerView.pause();
	}

	@Override
	protected void onStop() {
	   super.onStop();
	   // you need to pass all activity's lifecycle methods to RecognizerView
	   mRecognizerView.stop();
	}
	
	@Override
	protected void onDestroy() {
	   super.onDestroy();
	   // you need to pass all activity's lifecycle methods to RecognizerView
	   mRecognizerView.destroy();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
	   super.onConfigurationChanged(newConfig);
	   // you need to pass all activity's lifecycle methods to RecognizerView
	   mRecognizerView.changeConfiguration(newConfig);
	}
		
    @Override
    public void onScanningDone(RecognitionResults results) {
    	// this method is from ScanResultListener and will be called when scanning completes
    	// RecognitionResults may contain multiple results in array returned
    	// by method getRecognitionResults().
    	// This depends on settings in RecognitionSettings object that was
    	// given to RecognizerView.
    	// For more information, see chapter "Recognition settings and results")
    	
    	// After this method ends, scanning will be resumed and recognition
    	// state will be retained. If you want to prevent that, then
    	// you should call:
    	// mRecognizerView.resetRecognitionState();

		// If you want to pause scanning to prevent receiving recognition
		// results, you should call:
		// mRecognizerView.pauseScanning();
		// After scanning is paused, you will have to resume it with:
		// mRecognizerView.resumeScanning(true);
		// boolean in resumeScanning method indicates whether recognition
		// state should be automatically reset when resuming scanning
    }
    
    @Override
    public void onCameraPreviewStarted() {
        // this method is from CameraEventsListener and will be called when camera preview starts
    }
    
    @Override
    public void onCameraPreviewStopped() {
        // this method is from CameraEventsListener and will be called when camera preview stops
    }

    @Override
    public void onError(Throwable exc) {
        /** 
         * This method is from CameraEventsListener and will be called when 
         * opening of camera resulted in exception or recognition process
         * encountered an error. The error details will be given in exc
         * parameter.
         */
    }
    
    @Override
    @TargetApi(23)
    public void onCameraPermissionDenied() {
    	/**
    	 * Called on Android 6.0 and newer if camera permission is not given
    	 * by user. You should request permission from user to access camera.
    	 */
    	 requestPermissions(new String[]{Manifest.permission.CAMERA}, PERMISSION_CAMERA_REQUEST_CODE);
    	 /**
    	  * Please note that user might have not given permission to use 
    	  * camera. In that case, you have to explain to user that without
    	  * camera permissions scanning will not work.
    	  * For more information about requesting permissions at runtime, check
    	  * this article:
    	  * https://developer.android.com/training/permissions/requesting.html
    	  */
    }
    
    @Override
    public void onAutofocusFailed() {
	    /**
	     * This method is from CameraEventsListener will be called when camera focusing has failed. 
	     * Camera manager usually tries different focusing strategies and this method is called when all 
	     * those strategies fail to indicate that either object on which camera is being focused is too 
	     * close or ambient light conditions are poor.
	     */
    }
    
    @Override
    public void onAutofocusStarted(Rect[] areas) {
	    /**
	     * This method is from CameraEventsListener and will be called when camera focusing has started.
	     * You can utilize this method to draw focusing animation on UI.
	     * Areas parameter is array of rectangles where focus is being measured. 
	     * It can be null on devices that do not support fine-grained camera control.
	     */
    }

    @Override
    public void onAutofocusStopped(Rect[] areas) {
	    /**
	     * This method is from CameraEventsListener and will be called when camera focusing has stopped.
	     * You can utilize this method to remove focusing animation on UI.
	     * Areas parameter is array of rectangles where focus is being measured. 
	     * It can be null on devices that do not support fine-grained camera control.
	     */
    }
}
```

### <a name="scanOrientation"></a> Scan activity's orientation

If activity's `screenOrientation` property in `AndroidManifest.xml` is set to `sensor`, `fullSensor` or similar, activity will be restarted every time device changes orientation from portrait to landscape and vice versa. While restarting activity, its `onPause`, `onStop` and `onDestroy` methods will be called and then new activity will be created anew. This is a potential problem for scan activity because in its lifecycle it controls both camera and native library - restarting the activity will trigger both restart of the camera and native library. This is a problem because changing orientation from landscape to portrait and vice versa will be very slow, thus degrading a user experience. **We do not recommend such setting.**

For that matter, we recommend setting your scan activity to either `portrait` or `landscape` mode and handle device orientation changes manually. To help you with this, `RecognizerView` supports adding child views to it that will be rotated regardless of activity's `screenOrientation`. You add a view you wish to be rotated (such as view that contains buttons, status messages, etc.) to `RecognizerView` with `addChildView` method. The second parameter of the method is a boolean that defines whether the view you are adding will be rotated with device. To define allowed orientations, implement [OrientationAllowedListener](https://blinkocr.github.io/blinkocr-android/com/microblink/view/OrientationAllowedListener.html) interface and add it to `RecognizerView` with method `setOrientationAllowedListener`. **This is the recommended way of rotating camera overlay.**

However, if you really want to set `screenOrientation` property to `sensor` or similar and want Android to handle orientation changes of your scan activity, then we recommend to set `configChanges` property of your activity to `orientation|screenSize`. This will tell Android not to restart your activity when device orientation changes. Instead, activity's `onConfigurationChanged` method will be called so that activity can be notified of the configuration change. In your implementation of this method, you should call `changeConfiguration` method of `RecognizerView` so it can adapt its camera surface and child views to new configuration. Note that on Android versions older than 4.0 changing of configuration will require restart of camera, which can be slow.

## <a name="recognizerViewReference"></a> `RecognizerView` reference
The complete reference of `RecognizerView` is available in [Javadoc](https://blinkocr.github.io/blinkocr-android/com/microblink/view/recognition/RecognizerView.html). The usage example is provided in `BlinkOCRFullScreen` demo app provided with SDK. This section just gives a quick overview of `RecognizerView's` most important methods.

##### <a name="recognizerView_create"></a> [`create()`](https://blinkocr.github.io/blinkocr-android/com/microblink/view/recognition/RecognizerView.html#create--)
This method should be called in activity's `onCreate` method. It will initialize `RecognizerView's` internal fields and will initialize camera control thread. This method must be called after all other settings are already defined, such as listeners and recognition settings. After calling this method, you can add child views to `RecognizerView` with method `addChildView(View, boolean)`.

##### <a name="recognizerView_start"></a> [`start()`](https://blinkocr.github.io/blinkocr-android/com/microblink/view/recognition/RecognizerView.html#start--)
This method should be called in activity's `onStart` method. It will initialize background processing thread and start native library initialization on that thread.

##### <a name="recognizerView_resume"></a> [`resume()`](https://blinkocr.github.io/blinkocr-android/com/microblink/view/recognition/RecognizerView.html#resume--)
This method should be called in activity's `onResume` method. It will trigger background initialization of camera. After camera is loaded, it will start camera frame recognition, except if scanning loop is paused.

##### <a name="recognizerView_pause"></a> [`pause()`](https://blinkocr.github.io/blinkocr-android/com/microblink/view/recognition/RecognizerView.html#pause--)
This method should be called in activity's `onPause` method. It will stop the camera, but will keep native library loaded.

##### <a name="recognizerView_stop"></a> [`stop()`](https://blinkocr.github.io/blinkocr-android/com/microblink/view/recognition/RecognizerView.html#stop--)
This method should be called in activity's `onStop` method. It will deinitialize native library, terminate background processing thread and free all resources that are no longer necessary.

##### <a name="recognizerView_destroy"></a> [`destroy()`](https://blinkocr.github.io/blinkocr-android/com/microblink/view/recognition/RecognizerView.html#destroy--)
This method should be called in activity's `onDestroy` method. It will free all resources allocated in `create()` and will terminate camera control thread.

##### <a name="recognizerView_changeConfiguration"></a> [`changeConfiguration(Configuration)`](https://blinkocr.github.io/blinkocr-android/com/microblink/view/BaseCameraView.html#changeConfiguration-android.content.res.Configuration-)
This method should be called in activity's `onConfigurationChanged` method. It will adapt camera surface to new configuration without the restart of the activity. See [Scan activity's orientation](#scanOrientation) for more information.

##### <a name="recognizerView_setCameraType"></a> [`setCameraType(CameraType)`](https://blinkocr.github.io/blinkocr-android/com/microblink/view/BaseCameraView.html#setCameraType-com.microblink.hardware.camera.CameraType-)
With this method you can define which camera on device will be used. Default camera used is back facing camera.

##### <a name="recognizerView_setAspectMode"></a> [`setAspectMode(CameraAspectMode)`](https://blinkocr.github.io/blinkocr-android/com/microblink/view/BaseCameraView.html#setAspectMode-com.microblink.view.CameraAspectMode-)
Define the [aspect mode of camera](https://blinkocr.github.io/blinkocr-android/com/microblink/view/CameraAspectMode.html). If set to `ASPECT_FIT` (default), then camera preview will be letterboxed inside available view space. If set to `ASPECT_FILL`, camera preview will be zoomed and cropped to use the entire view space.

##### <a name="recognizerView_setVideoResolutionPreset"></a> [`setVideoResolutionPreset(VideoResolutionPreset)`](https://blinkocr.github.io/blinkocr-android/com/microblink/view/BaseCameraView.html#setVideoResolutionPreset-com.microblink.hardware.camera.VideoResolutionPreset-)
Define the [video resolution preset](https://blinkocr.github.io/blinkocr-android/com/microblink/hardware/camera/VideoResolutionPreset.html) that will be used when choosing camera resolution for scanning.

##### <a name="recognizerView_setRecognitionSettings"></a> [`setRecognitionSettings(RecognitionSettings)`](https://blinkocr.github.io/blinkocr-android/com/microblink/view/recognition/RecognizerView.html#setRecognitionSettings-com.microblink.recognizers.settings.RecognitionSettings-)
With this method you can set recognition settings that contains information what will be scanned and how will scan be performed. For more information about recognition settings and results see [Recognition settings and results](#recognitionSettingsAndResults). This method must be called before `create()`.

##### <a name="recognizerView_reconfigureRecognizers1"></a> [`reconfigureRecognizers(RecognitionSettings)`](https://blinkocr.github.io/blinkocr-android/com/microblink/view/recognition/RecognizerView.html#reconfigureRecognizers-com.microblink.recognizers.settings.RecognitionSettings-)
With this method you can reconfigure the recognition process while recognizer is active. Unlike `setRecognitionSettings`, this method must be called while recognizer is active (i.e. after `resume` was called). For more information about recognition settings see [Recognition settings and results](#recognitionSettingsAndResults).

##### <a name="recognizerView_setOrientationAllowedListener"></a> [`setOrientationAllowedListener(OrientationAllowedListener)`](https://blinkocr.github.io/blinkocr-android/com/microblink/view/BaseCameraView.html#setOrientationAllowedListener-com.microblink.view.OrientationAllowedListener-)
With this method you can set a [OrientationAllowedListener](https://blinkocr.github.io/blinkocr-android/com/microblink/view/OrientationAllowedListener.html) which will be asked if current orientation is allowed. If orientation is allowed, it will be used to rotate rotatable views to it and it will be passed to native library so that recognizers can be aware of the new orientation. If you do not set this listener, recognition will be performed only in orientation defined by current activity's orientation.

##### <a name="recognizerView_setScanResultListener"></a> [`setScanResultListener(ScanResultListener)`](https://blinkocr.github.io/blinkocr-android/com/microblink/view/recognition/RecognizerView.html#setScanResultListener-com.microblink.view.recognition.ScanResultListener-)
With this method you can set a [ScanResultListener](https://blinkocr.github.io/blinkocr-android/com/microblink/view/recognition/ScanResultListener.html) which will be notified when recognition completes. After recognition completes, `RecognizerView` will pause its scanning loop and to continue the scanning you will have to call `resumeScanning` method. In this method you can obtain data from scanning results. For more information see [Recognition settings and results](#recognitionSettingsAndResults).

##### <a name="recognizerView_setCameraEventsListener"></a> [`setCameraEventsListener(CameraEventsListener)`](https://blinkocr.github.io/blinkocr-android/com/microblink/view/BaseCameraView.html#setCameraEventsListener-com.microblink.view.CameraEventsListener-)
With this method you can set a [CameraEventsListener](https://blinkocr.github.io/blinkocr-android/com/microblink/view/CameraEventsListener.html) which will be notified when various camera events occur, such as when camera preview has started, autofocus has failed or there has been an error while using the camera or performing the recognition.

##### <a name="recognizerView_pauseScanning"></a> [`pauseScanning()`](https://blinkocr.github.io/blinkocr-android/com/microblink/view/recognition/RecognizerView.html#pauseScanning--)
This method pauses the scanning loop, but keeps both camera and native library initialized. Pause and resume scanning methods count the number of calls, so if you called `pauseScanning()` twice, you will have to call `resumeScanning` twice to actually resume scanning.

##### <a name="recognizerView_resumeScanning"></a> [`resumeScanning(boolean)`](https://blinkocr.github.io/blinkocr-android/com/microblink/view/recognition/RecognizerView.html#resumeScanning-boolean-)
With this method you can resume the paused scanning loop. If called with `true` parameter, implicitly calls `resetRecognitionState()`. If called with `false`, old recognition state will not be reset, so it could be reused for boosting recognition result. This may not be always a desired behaviour.  Pause and resume scanning methods count the number of calls, so if you called `pauseScanning()` twice, you will have to call `resumeScanning` twice to actually resume scanning loop.


##### <a name="recognizerView_setInitialScanningPaused"></a> [`setInitialScanningPaused()`](https://blinkocr.github.io/blinkocr-android/com/microblink/view/recognition/RecognizerView.html#setInitialScanningPaused-boolean-)
This method lets you set up RecognizerView to not automatically resume scanning first time [resume](#recognizerView_resume) is called. An example use case of when you might want this is if you want to display onboarding help when opening camera first time and want to prevent scanning in background while onboarding is displayed over camera preview.

##### <a name="recognizerView_resetRecognitionState"></a> [`resetRecognitionState()`](https://blinkocr.github.io/blinkocr-android/com/microblink/view/recognition/RecognizerView.html#resetRecognitionState--)
With this method you can reset internal recognition state. State is usually kept to improve recognition quality over time, but without resetting recognition state sometimes you might get poorer results (for example if you scan one object and then another without resetting state you might end up with result that contains properties from both scanned objects).

##### <a name="recognizerView_addChildView"></a> [`addChildView(View, boolean)`](https://blinkocr.github.io/blinkocr-android/com/microblink/view/BaseCameraView.html#addChildView-android.view.View-boolean-)
With this method you can add your own view on top of `RecognizerView`. `RecognizerView` will ensure that your view will be layouted exactly above camera preview surface (which can be letterboxed if aspect ratio of camera preview size does not match the aspect ratio of `RecognizerView` and camera aspect mode is set to `ASPECT_FIT`). Boolean parameter defines whether your view should be rotated with device orientation changes. The rotation is independent of host activity's orientation changes and allowed orientations will be determined from [OrientationAllowedListener](https://blinkocr.github.io/blinkocr-android/com/microblink/view/OrientationAllowedListener.html). See also [Scan activity's orientation](#scanOrientation) for more information why you should rotate your views independently of activity.

##### <a name="recognizerView_isCameraFocused"></a> [`isCameraFocused()`](https://blinkocr.github.io/blinkocr-android/com/microblink/view/BaseCameraView.html#isCameraFocused--) 
This method returns `true` if camera thinks it has focused on object. Note that camera has to be active for this method to work. If camera is not active, returns `false`.

##### <a name="recognizerView_focusCamera"></a> [`focusCamera()`](https://blinkocr.github.io/blinkocr-android/com/microblink/view/BaseCameraView.html#focusCamera--) 
This method requests camera to perform autofocus. If camera does not support autofocus feature, method does nothing. Note that camera has to be active for this method to work.

##### <a name="recognizerView_isCameraTorchSupported"></a> [`isCameraTorchSupported()`](https://blinkocr.github.io/blinkocr-android/com/microblink/view/BaseCameraView.html#isCameraTorchSupported--)
This method returns `true` if camera supports torch flash mode. Note that camera has to be active for this method to work. If camera is not active, returns `false`.

##### <a name="recognizerView_setTorchState"></a> [`setTorchState(boolean, SuccessCallback)`](https://blinkocr.github.io/blinkocr-android/com/microblink/view/BaseCameraView.html#setTorchState-boolean-com.microblink.hardware.SuccessCallback-) 
If torch flash mode is supported on camera, this method can be used to enable/disable torch flash mode. After operation is performed, [SuccessCallback](https://blinkocr.github.io/blinkocr-android/com/microblink/hardware/SuccessCallback.html) will be called with boolean indicating whether operation has succeeded or not. Note that camera has to be active for this method to work and that callback might be called on background non-UI thread.

##### <a name="recognizerView_setScanningRegion"></a> [`setScanningRegion(Rectangle, boolean)`](https://blinkocr.github.io/blinkocr-android/com/microblink/view/recognition/RecognizerView.html#setScanningRegion-com.microblink.geometry.Rectangle-boolean-)
You can use this method to define the scanning region and define whether this scanning region will be rotated with device if [OrientationAllowedListener](https://blinkocr.github.io/blinkocr-android/com/microblink/view/OrientationAllowedListener.html) determines that orientation is allowed. This is useful if you have your own camera overlay on top of `RecognizerView` that is set as rotatable view - you can thus synchronize the rotation of the view with the rotation of the scanning region native code will scan.

Scanning region is defined as [Rectangle](https://blinkocr.github.io/blinkocr-android/com/microblink/geometry/Rectangle.html). First parameter of rectangle is x-coordinate represented as percentage of view width, second parameter is y-coordinate represented as percentage of view height, third parameter is region width represented as percentage of view width and fourth parameter is region height represented as percentage of view height.

View width and height are defined in current context, i.e. they depend on screen orientation. If you allow your ROI view to be rotated, then in portrait view width will be smaller than height, whilst in landscape orientation width will be larger than height. This complies with view designer preview. If you choose not to rotate your ROI view, then your ROI view will be laid out either in portrait or landscape, depending on setting for your scan activity in `AndroidManifest.xml`

Note that scanning region only reflects to native code - it does not have any impact on user interface. You are required to create a matching user interface that will visualize the same scanning region you set here.

##### <a name="recognizerView_setMeteringAreas"/></a> [`setMeteringAreas(Rectangle[],boolean)`](https://blinkocr.github.io/blinkocr-android/com/microblink/view/BaseCameraView.html#setMeteringAreas-com.microblink.geometry.Rectangle:A-boolean-)
This method can only be called when camera is active. You can use this method to define regions which camera will use to perform meterings for focus, white balance and exposure corrections. On devices that do not support metering areas, this will be ignored. Some devices support multiple metering areas and some support only one. If device supports only one metering area, only the first rectangle from array will be used.

Each region is defined as [Rectangle](https://blinkocr.github.io/blinkocr-android/com/microblink/geometry/Rectangle.html). First parameter of rectangle is x-coordinate represented as percentage of view width, second parameter is y-coordinate represented as percentage of view height, third parameter is region width represented as percentage of view width and fourth parameter is region height represented as percentage of view height.

View width and height are defined in current context, i.e. they depend on current device orientation. If you have custom [OrientationAllowedListener](https://blinkocr.github.io/blinkocr-android/com/microblink/view/OrientationAllowedListener.html), then device orientation will be the last orientation that you have allowed in your listener. If you don't have it set, orientation will be the orientation of activity as defined in `AndroidManifest.xml`. In portrait orientation view width will be smaller than height, whilst in landscape orientation width will be larger than height. This complies with view designer preview.

Second boolean parameter indicates whether or not metering areas should be automatically updated when device orientation changes.

##### <a name="recognizerView_setMetadataListener"></a> [`setMetadadaListener(MetadataListener, MetadataSettings)`](https://blinkocr.github.io/blinkocr-android/com/microblink/view/recognition/RecognizerView.html#setMetadataListener-com.microblink.metadata.MetadataListener-com.microblink.metadata.MetadataSettings-)
You can use this method to define [metadata listener](https://blinkocr.github.io/blinkocr-android/com/microblink/metadata/MetadataListener.html) that will obtain various metadata
from the current recognition process. Which metadata will be available depends on [metadata settings](https://blinkocr.github.io/blinkocr-android/com/microblink/metadata/MetadataSettings.html). For more information and examples, check demo applications and section [Obtaining various metadata with _MetadataListener_](#metadataListener).

##### <a name="recognizerView_setLicenseKey1"></a> [`setLicenseKey(String licenseKey)`](https://blinkocr.github.io/blinkocr-android/com/microblink/view/recognition/RecognizerView.html#setLicenseKey-java.lang.String-)
This method sets the license key that will unlock all features of the native library. You can obtain your license key from [Microblink website](http://microblink.com/login).

##### <a name="recognizerView_setLicenseKey2"></a> [`setLicenseKey(String licenseKey, String licensee)`](https://blinkocr.github.io/blinkocr-android/com/microblink/view/recognition/RecognizerView.html#setLicenseKey-java.lang.String-java.lang.String-)
Use this method to set a license key that is bound to a licensee, not the application package name. You will use this method when you obtain a license key that allows you to use _BlinkOCR_ SDK in multiple applications. You can obtain your license key from [Microblink website](http://microblink.com/login).

# <a name="directAPI"></a> Using direct API for recognition of Android Bitmaps

This section will describe how to use direct API to recognize android Bitmaps without the need for camera. You can use direct API anywhere from your application, not just from activities.

1. First, you need to obtain reference to [Recognizer singleton](https://blinkocr.github.io/blinkocr-android/com/microblink/directApi/Recognizer.html) using [getSingletonInstance](https://blinkocr.github.io/blinkocr-android/com/microblink/directApi/Recognizer.html#getSingletonInstance--).
2. Second, you need to [initialize the recognizer](https://blinkocr.github.io/blinkocr-android/com/microblink/directApi/Recognizer.html#initialize-android.content.Context-com.microblink.recognizers.settings.RecognitionSettings-com.microblink.directApi.DirectApiErrorListener-).
3. After initialization, you can use singleton to [process images](https://blinkocr.github.io/blinkocr-android/com/microblink/directApi/Recognizer.html#recognizeBitmap-android.graphics.Bitmap-com.microblink.hardware.orientation.Orientation-com.microblink.view.recognition.ScanResultListener-). You cannot process multiple images in parallel.
4. Do not forget to [terminate](https://blinkocr.github.io/blinkocr-android/com/microblink/directApi/Recognizer.html#terminate--) the recognizer after usage (it is a shared resource).

Here is the minimum example of usage of direct API for recognizing android Bitmap:

```java
public class DirectAPIActivity extends Activity implements ScanResultListener {
	private Recognizer mRecognizer;
		
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// initialize your activity here
	}
	
	@Override
	protected void onStart() {
	   super.onStart();
	   try {
		   mRecognizer = Recognizer.getSingletonInstance();
		} catch (FeatureNotSupportedException e) {
			Toast.makeText(this, "Feature not supported! Reason: " + e.getReason().getDescription(), Toast.LENGTH_LONG).show();
			finish();
			return;
		}
	   try {
	       // set license key
	       mRecognizer.setLicenseKey(this, "your license key");
	   } catch (InvalidLicenceKeyException exc) {
	       finish();
	       return;
	   }
		RecognitionSettings settings = new RecognitionSettings();
		// setupSettingsArray method is described in chapter "Recognition 
		// settings and results")
		settings.setRecognizerSettingsArray(setupSettingsArray());
		mRecognizer.initialize(this, settings, new DirectApiErrorListener() {
			@Override
			public void onRecognizerError(Throwable t) {
				Toast.makeText(DirectAPIActivity.this, "There was an error in initialization of Recognizer: " + t.getMessage(), Toast.LENGTH_SHORT).show();
				finish();
			}
		});
	}
	
	@Override
	protected void onResume() {
	   super.onResume();
		// start recognition
		Bitmap bitmap = BitmapFactory.decodeFile("/path/to/some/file.jpg");
		mRecognizer.recognize(bitmap, Orientation.ORIENTATION_LANDSCAPE_RIGHT, this);
	}

	@Override
	protected void onStop() {
	   super.onStop();
	   mRecognizer.terminate();
	}

    @Override
    public void onScanningDone(RecognitionResults results) {
    	// this method is from ScanResultListener and will be called 
    	// when scanning completes
    	// RecognitionResults may contain multiple results in array returned
    	// by method getRecognitionResults().
    	// This depends on settings in RecognitionSettings object that was
    	// given to RecognizerView.
    	// For more information, see chapter "Recognition settings and results")
    	    	
    	finish(); // in this example, just finish the activity
    }
    
}
```

## <a name="directAPIStateMachine"></a> Understanding DirectAPI's state machine

DirectAPI's Recognizer singleton is actually a state machine which can be in one of 4 states: `OFFLINE`, `UNLOCKED`, `READY` and `WORKING`. 

- When you obtain the reference to Recognizer singleton, it will be in `OFFLINE` state. 
- First you need to unlock the Recognizer by providing a valid licence key using [`setLicenseKey`](https://blinkocr.github.io/blinkocr-android/com/microblink/directApi/Recognizer.html#setLicenseKey-android.content.Context-java.lang.String-) method. If you attempt to call `setLicenseKey` while Recognizer is not in `OFFLINE` state, you will get `IllegalStateException`.
- After successful unlocking, Recognizer singleton will move to `UNLOCKED` state.
- Once in `UNLOCKED` state, you can initialize Recognizer by calling [`initialize`](https://blinkocr.github.io/blinkocr-android/com/microblink/directApi/Recognizer.html#initialize-android.content.Context-com.microblink.recognizers.settings.RecognitionSettings-com.microblink.directApi.DirectApiErrorListener-) method. If you call `initialize` method while Recognizer is not in `UNLOCKED` state, you will get `IllegalStateException`.
- After successful initialization, Recognizer will move to `READY` state. Now you can call any of the `recognize*` methods.
- When starting recognition with any of the `recognize*` methods, Recognizer will move to `WORKING` state. If you attempt to call these methods while Recognizer is not in `READY` state, you will get `IllegalStateException`
- Recognition is performed on background thread so it is safe to call all Recognizer's method from UI thread
- When recognition is finished, Recognizer first moves back to `READY` state and then returns the result via provided [`ScanResultListener`](https://blinkocr.github.io/blinkocr-android/com/microblink/view/recognition/ScanResultListener.html). 
- Please note that `ScanResultListener`'s [`onScanningDone`](https://blinkocr.github.io/blinkocr-android/com/microblink/view/recognition/ScanResultListener.html#onScanningDone-com.microblink.recognizers.RecognitionResults-) method will be called on background processing thread, so make sure you do not perform UI operations in this calback.
- By calling [`terminate`](https://blinkocr.github.io/blinkocr-android/com/microblink/directApi/Recognizer.html#terminate--) method, Recognizer singleton will release all its internal resources and will request processing thread to terminate. Note that even after calling `terminate` you might receive `onScanningDone` event if there was work in progress when `terminate` was called.
- `terminate` method can be called from any Recognizer singleton's state
- You can observe Recognizer singleton's state with method [`getCurrentState`](https://blinkocr.github.io/blinkocr-android/com/microblink/directApi/Recognizer.html#getCurrentState--)

## <a name="directAPIWithRecognizer"></a> Using DirectAPI while RecognizerView is active
Both [RecognizerView](#recognizerView) and DirectAPI recognizer use the same internal singleton that manages native code. This singleton handles initialization and termination of native library and propagating recognition settings to native library. It is possible to use RecognizerView and DirectAPI together, as internal singleton will make sure correct synchronization and correct recognition settings are used. If you run into problems while using DirectAPI in combination with RecognizerView, [let us know](http://help.microblink.com)!

## <a name="metadataListener"></a> Obtaining various metadata with _MetadataListener_

This section will give an example how to use [Metadata listener](https://blinkocr.github.io/blinkocr-android/com/microblink/metadata/MetadataListener.html) to obtain various metadata, such as object detection location, images that are being processed and much more. Which metadata will be obtainable is configured with [Metadata settings](https://blinkocr.github.io/blinkocr-android/com/microblink/metadata/MetadataSettings.html). You must set both _MetadataSettings_ and your implementation of _MetadataListener_ before calling [create](#recognizerView_create) method of [RecognizerView](#recognizerView). Setting them after causes undefined behaviour.

The following code snippet shows how to configure _MetadataSettings_ to obtain detection location, video frame that was used to perform and dewarped image of the document being scanned (**NOTE:** the availability of metadata depends on currently active recognisers and their settings. Not all recognisers can produce all types of metadata. Check [Recognition settings and results](#recognitionSettingsAndResults) article for more information about recognisers and their settings):

```java
// this snippet should be in onCreate method of your scanning activity

MetadataSettings ms = new MetadataSettings();
// enable receiving of detection location
ms.setDetectionMetadataAllowed(true);

// ImageMetadataSettings contains settings for defining which images will be returned
MetadataSettings.ImageMetadataSettings ims = new MetadataSettings.ImageMetadataSettings();
// enable returning of dewarped images, if they are available
ims.setDewarpedImageEnabled(true);
// enable returning of image that was used to obtain valid scanning result
ims.setSuccessfulScanFrameEnabled(true)

// set ImageMetadataSettings to MetadataSettings object
ms.setImageMetadataSettings(ims);

// this line must be called before mRecognizerView.create()
mRecognizerView.setMetadataListener(myMetadataListener, ms);
```

The following snippet shows one possible implementation of _MetadataListener_:

```java
public class MyMetadataListener implements MetadataListener {

	/**
	 * Called when metadata is available.
	 */
    @Override
    public void onMetadataAvailable(Metadata metadata) {
    	// detection location will be available as DetectionMetadata
        if (metadata instanceof DetectionMetadata) {
        	// DetectionMetadata contains DetectorResult which is null if object detection
        	// has failed and non-null otherwise
        	// Let's assume that we have a QuadViewManager which can display animated frame
        	// around detected object (for reference, please check javadoc and demo apps)
            DetectorResult dr = ((DetectionMetadata) metadata).getDetectionResult();
            if (dr == null) {
            	// animate frame to default location if detection has failed
                mQuadViewManager.animateQuadToDefaultPosition();
            } else if (dr instanceof QuadDetectorResult) {
            	// otherwise, animate frame to detected location
                mQuadViewManager.animateQuadToDetectionPosition((QuadDetectorResult) dr);
            }
        // images will be available inside ImageMetadata
        } else if (metadata instanceof ImageMetadata) {
        	// obtain image
        	
        	// Please note that Image's internal buffers are valid only
        	// until this method ends. If you want to save image for later,
        	// obtained a cloned image with image.clone().
        	
            Image image = ((ImageMetadata) metadata).getImage();
            // to convert the image to Bitmap, call image.convertToBitmap()
            
            // after this line, image gets disposed. If you want to save it
            // for later, you need to clone it with image.clone()
        }
    }
}
```

Here are javadoc links to all classes that appeared in previous code snippet:

- [Metadata](https://blinkocr.github.io/blinkocr-android/com/microblink/metadata/Metadata.html)
- [DetectionMetadata](https://blinkocr.github.io/blinkocr-android/com/microblink/metadata/DetectionMetadata.html)
- [DetectorResult](https://blinkocr.github.io/blinkocr-android/com/microblink/detectors/DetectorResult.html)
- [QuadViewManager](https://blinkocr.github.io/blinkocr-android/com/microblink/view/viewfinder/quadview/QuadViewManager.html)
- [QuadDetectorResult](https://blinkocr.github.io/blinkocr-android/com/microblink/detectors/quad/QuadDetectorResult.html)
- [ImageMetadata](https://blinkocr.github.io/blinkocr-android/com/microblink/metadata/ImageMetadata.html)
- [Image](https://blinkocr.github.io/blinkocr-android/com/microblink/image/Image.html)

## <a name="imageListener"></a> Using ImageListener to obtain images that are being processed

There are two ways of obtaining images that are being processed:

- if _BlinkOCRActivity_ is being used to perform scanning, then you need to implement [ImageListener interface](https://blinkocr.github.io/blinkocr-android/com/microblink/image/ImageListener.html) and send your implementation via Intent to _BlinkOCRActivity_. Note that while this seems easier, this actually introduces a large performance penalty because _ImageListener_ will receive all images, including ones you do not actually need, except in cases when you also provide [ImageMetadata settings](https://blinkocr.github.io/blinkocr-android/com/microblink/metadata/MetadataSettings.ImageMetadataSettings.html) with `BlinkOCRActivity.EXTRAS_IMAGE_METADATA_SETTINGS` extra.
- if [RecognizerView](#recognizerView) is directly embedded into your scanning activity, then you should initialise it with [Metadata settings](https://blinkocr.github.io/blinkocr-android/com/microblink/metadata/MetadataSettings.html) and your implementation of [Metadata listener interface](https://blinkocr.github.io/blinkocr-android/com/microblink/metadata/MetadataListener.html). The _MetadataSettings_ will define which metadata will be reported to _MetadataListener_. The metadata can contain various data, such as images, object detection location etc. To see documentation and example how to use _MetadataListener_ to obtain images and other metadata, see section [Obtaining various metadata with _MetadataListener_](#metadataListener).

This section will give an example how to implement [ImageListener interface](https://blinkocr.github.io/blinkocr-android/com/microblink/image/ImageListener.html) that will obtain images that are being processed. `ImageListener` has only one method that needs to be implemented: `onImageAvailable(Image)`. This method is called whenever library has available image for current processing step. [Image](https://blinkocr.github.io/blinkocr-android/com/microblink/image/Image.html) is class that contains all information about available image, including buffer with image pixels. Image can be in several format and of several types. [ImageFormat](https://blinkocr.github.io/blinkocr-android/com/microblink/image/ImageFormat.html) defines the pixel format of the image, while [ImageType](https://blinkocr.github.io/blinkocr-android/com/microblink/image/ImageType.html) defines the type of the image. `ImageListener` interface extends android's [Parcelable interface](https://developer.android.com/reference/android/os/Parcelable.html) so it is possible to send implementations via [intents](https://developer.android.com/reference/android/content/Intent.html).

Here is the example implementation of [ImageListener interface](https://blinkocr.github.io/blinkocr-android/com/microblink/image/ImageListener.html). This implementation will save all images into folder `myImages` on device's external storage:

```java
public class MyImageListener implements ImageListener {

   /**
    * Called when library has image available.
    */
    @Override
    public void onImageAvailable(Image image) {
        // we will save images to 'myImages' folder on external storage
        // image filenames will be 'imageType - currentTimestamp.jpg'
        String output = Environment.getExternalStorageDirectory().getAbsolutePath() + "/myImages";
        File f = new File(output);
        if(!f.exists()) {
            f.mkdirs();
        }
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        String dateString = dateFormat.format(new Date());
        String filename = null;
        switch(image.getImageFormat()) {
            case ALPHA_8: {
                filename = output + "/alpha_8 - " + image.getImageName() + " - " + dateString + ".jpg";
                break;
            }
            case BGRA_8888: {
                filename = output + "/bgra - " + image.getImageName() + " - " + dateString + ".jpg";
                break;
            }
            case YUV_NV21: {
                filename = output + "/yuv - " + image.getImageName()+ " - " + dateString + ".jpg";
                break;
            }
        }
        Bitmap b = image.convertToBitmap();
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(filename);
            boolean success = b.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            if(!success) {
                Log.e(this, "Failed to compress bitmap!");
                if(fos != null) {
                    try {
                        fos.close();
                    } catch (IOException ignored) {
                    } finally {
                        fos = null;
                    }
                    new File(filename).delete();
                }
            }
        } catch (FileNotFoundException e) {
            Log.e(this, e, "Failed to save image");
        } finally {
            if(fos != null) {
                try {
                    fos.close();
                } catch (IOException ignored) {
                }
            }
        }
        // after this line, image gets disposed. If you want to save it
        // for later, you need to clone it with image.clone()
    }

    /**
     * ImageListener interface extends Parcelable interface, so we also need to implement
     * that interface. The implementation of Parcelable interface is below this line.
     */

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
    }

    public static final Creator<MyImageListener> CREATOR = new Creator<MyImageListener>() {
        @Override
        public MyImageListener createFromParcel(Parcel source) {
            return new MyImageListener();
        }

        @Override
        public MyImageListener[] newArray(int size) {
            return new MyImageListener[size];
        }
    };
}
```

Note that [ImageListener](https://blinkocr.github.io/blinkocr-android/com/microblink/image/ImageListener.html) can only be given to _BlinkOCRActivity_ via Intent, while to [RecognizerView](#recognizerView), you need to give [Metadata listener](https://blinkocr.github.io/blinkocr-android/com/microblink/metadata/MetadataListener.html) and [Metadata settings](https://blinkocr.github.io/blinkocr-android/com/microblink/metadata/MetadataSettings.html) that defines which metadata should be obtained. When you give _ImageListener_ to _BlinkOCRActivity_ via Intent, it internally registers a _MetadataListener_ that enables obtaining of all available image types and invokes _ImageListener_ given via Intent with the result. For more information and examples how to use _MetadataListener_ for obtaining images, refer to demo applications.

# <a name="recognitionSettingsAndResults"></a> Recognition settings and results

This chapter will discuss various recognition settings used to configure different recognizers and scan results generated by them.

## <a name="recognitionSettings"></a> [Recognition settings](https://blinkocr.github.io/blinkocr-android/com/microblink/recognizers/settings/RecognitionSettings.html)

Recognition settings define what will be scanned and how will the recognition process be performed. Here is the list of methods that are most relevant:

##### [`setAllowMultipleScanResultsOnSingleImage(boolean)`](https://blinkocr.github.io/blinkocr-android/com/microblink/recognizers/settings/RecognitionSettings.html#setAllowMultipleScanResultsOnSingleImage-boolean-)
Sets whether or not outputting of multiple scan results from same image is allowed. If that is `true`, it is possible to return multiple recognition results produced by different recognizers from same image. However, single recognizer can still produce only a single result from single image. By default, this option is `false`, i.e. the array of `BaseRecognitionResults` will contain at most 1 element. The upside of setting that option to `false` is the speed - if you enable lots of recognizers, as soon as the first recognizer succeeds in scanning, recognition chain will be terminated and other recognizers will not get a chance to analyze the image. The downside is that you are then unable to obtain multiple results from different recognizers from single image.

##### [`setNumMsBeforeTimeout(int)`](https://blinkocr.github.io/blinkocr-android/com/microblink/recognizers/settings/RecognitionSettings.html#setNumMsBeforeTimeout-int-)
Sets the number of miliseconds _BlinkOCR_ will attempt to perform the scan it exits with timeout error. On timeout returned array of `BaseRecognitionResults` inside [RecognitionResults](https://blinkocr.github.io/blinkocr-android/com/microblink/recognizers/RecognitionResults.html) might be null, empty or may contain only elements that are not valid ([`isValid`](https://blinkocr.github.io/blinkocr-android/com/microblink/recognizers/BaseRecognitionResult.html#isValid--) returns `false`) or are empty ([`isEmpty`](https://blinkocr.github.io/blinkocr-android/com/microblink/recognizers/BaseRecognitionResult.html#isEmpty--) returns `true`).

##### [`setFrameQualityEstimationMode(FrameQualityEstimationMode)`](https://blinkocr.github.io/blinkocr-android/com/microblink/recognizers/settings/RecognitionSettings.html#setFrameQualityEstimationMode-com.microblink.recognizers.settings.RecognitionSettings.FrameQualityEstimationMode-)
Sets the mode of the frame quality estimation. Frame quality estimation is the process of estimating the quality of video frame so only best quality frames can be chosen for processing so no time is wasted on processing frames that are of too poor quality to contain any meaningful information. It is **not** used when performing recognition of [Android bitmaps](https://developer.android.com/reference/android/graphics/Bitmap.html) using [Direct API](#directAPI). You can choose 3 different frame quality estimation modes: automatic, always on and always off.

- In **automatic** mode (default), frame quality estimation will be used if device contains multiple processor cores or if on single core device at least one active recognizer requires frame quality estimation.
- In **always on** mode, frame quality estimation will be used always, regardless of device or active recognizers.
- In **always off** mode, frame quality estimation will be always disabled, regardless of device or active recognizers. This is not recommended setting because it can significantly decrease quality of the scanning process.

##### [`setRecognizerSettingsArray(RecognizerSettings[])`](https://blinkocr.github.io/blinkocr-android/com/microblink/recognizers/settings/RecognitionSettings.html#setRecognizerSettingsArray-com.microblink.recognizers.settings.RecognizerSettings:A-)
Sets the array of [RecognizerSettings](https://blinkocr.github.io/blinkocr-android/com/microblink/recognizers/settings/RecognizerSettings.html) that will define which recognizers should be activated and how should the be set up. The list of available _RecognizerSettings_ and their specifics are given below.

## <a name="blinkOCR"></a> Scanning segments with BlinkOCR recognizer

This section discusses the setting up of BlinkOCR recognizer and obtaining results from it. You should also check the demo for example.

### Setting up BlinkOCR recognizer

BlinkOCR recognizer is consisted of one or more parsers that are grouped in parser groups. Each parser knows how to extract certain element from OCR result and also knows what are the best OCR engine options required to perform OCR on image. Parsers can be grouped in parser groups. Parser groups contain one or more parsers and are responsible for merging required OCR engine options of each parser in group and performing OCR only once and then letting each parser in group parse the data. Thus, you can make for own best tradeoff between speed and accuracy - putting each parser into its own group will give best accuracy, but will perform OCR of image for each parser which can consume a lot of processing time. On the other hand, putting all parsers into same group will perform only one OCR but with settings that are combined for all parsers in group, thus possibly reducing parsing quality.

Let's see this on example: assume we have two parsers at our disposal: `AmountParser` and `EMailParser`. `AmountParser` knows how to extract amount's from OCR result and requires from OCR only to recognise digits, periods and commas and ignore letters. On the other hand, `EMailParser` knows how to extract e-mails from OCR result and requires from OCR to recognise letters, digits, '@' characters and periods, but not commas. 

If we put both `AmountParser` and `EMailParser` into same parser group, the merged OCR engine settings will require recognition of all letters, all digits, '@' character, both period and comma. Such OCR result will contain all characters for `EMailParser` to properly parse e-mail, but might confuse `AmountParser` if OCR misclassifies some characters into digits.

If we put `AmountParser` in one parser group and `EMailParser` in another parser group, OCR will be performed for each parser group independently, thus preventing the `AmountParser` confusion, but two OCR passes of image will be performed, which can have a performance impact.

So to sum it up, BlinkOCR recognizer performs OCR of image for each available parser group and then runs all parsers in that group on obtained OCR result and saves parsed data. 

By definition, each parser results with string that represents a parsed data. The parsed string is stored under parser's name which has to be unique within parser group. So, when defining settings for BlinkOCR recognizer, when adding parsers, you need to provide a name for the parser (you will use that name for obtaining result later) and optionally provide a name for the parser group in which parser will be put into.

To activate BlinkOCR recognizer, you need to create [BlinkOCRRecognizerSettings](https://blinkocr.github.io/blinkocr-android/com/microblink/recognizers/blinkocr/BlinkOCRRecognizerSettings.html), add some parsers to it and add it to `RecognizerSettings` array. You can use the following code snippet to perform that:

```java
private RecognizerSettings[] setupSettingsArray() {
	BlinkOCRRecognizerSettings sett = new BlinkOCRRecognizerSettings();
	
	// add amount parser to default parser group
	sett.addParser("myAmountParser", new AmountParserSettings());
	
	// now add sett to recognizer settings array that is used to configure
	// recognition
	return new RecognizerSettings[] { sett };
}
```

The following is a list of available parsers:


- Amount parser - represented by [AmountParserSettings](https://blinkocr.github.io/blinkocr-android/com/microblink/recognizers/blinkocr/parser/generic/AmountParserSettings.html)
	- used for parsing amounts from OCR result
- IBAN parser - represented by [IbanParserSettings](https://blinkocr.github.io/blinkocr-android/com/microblink/recognizers/blinkocr/parser/generic/IbanParserSettings.html)
	- used for parsing International Bank Account Numbers (IBANs) from OCR result
- E-mail parser - represented by [EMailParserSettings](https://blinkocr.github.io/blinkocr-android/com/microblink/recognizers/blinkocr/parser/generic/EMailParserSettings.html)
	- used for parsing e-mail addresses
- Date parser - represented by [DateParserSettings](https://blinkocr.github.io/blinkocr-android/com/microblink/recognizers/blinkocr/parser/generic/DateParserSettings.html)
	- used for parsing dates in various formats
- Raw parser - represented by [RawParserSettings](https://blinkocr.github.io/blinkocr-android/com/microblink/recognizers/blinkocr/parser/generic/RawParserSettings.html)
	- used for obtaining raw OCR result

- Regex parser - represented by [RegexParserSettings](https://blinkocr.github.io/blinkocr-android/com/microblink/recognizers/blinkocr/parser/regex/RegexParserSettings.html)
	- used for parsing arbitrary regular expressions
	- please note that some features, like back references, match grouping and certain regex metacharacters are not supported. See javadoc for more info.

### Obtaining results from BlinkOCR recognizer

BlinkOCR recognizer produces [BlinkOCRRecognitionResult](https://blinkocr.github.io/blinkocr-android/com/microblink/recognizers/blinkocr/BlinkOCRRecognitionResult.html). You can use `instanceof` operator to check if element in results array is instance of `BlinkOCRRecognitionResult` class. See the following snippet for an example:

```java
@Override
public void onScanningDone(RecognitionResults results) {
	BaseRecognitionResult[] dataArray = results.getRecognitionResults();
	for(BaseRecognitionResult baseResult : dataArray) {
		if(baseResult instanceof BlinkOCRRecognitionResult) {
			BlinkOCRRecognitionResult result = (BlinkOCRRecognitionResult) baseResult;
			
	        // you can use getters of BlinkOCRRecognitionResult class to 
	        // obtain scanned information
	        if(result.isValid() && !result.isEmpty()) {
	        	 // use the parser name provided to BlinkOCRRecognizerSettings to
	        	 // obtain parsed result provided by given parser
	        	 // obtain result of "myAmountParser" in default parsing group
		        String parsedAmount = result.getParsedResult("myAmountParser");
		        // note that parsed result can be null or empty even if result
		        // is marked as non-empty and valid
		        if(parsedAmount != null && !parsedAmount.isEmpty()) {
		        	// do whatever you want with parsed result
		        }
		        // obtain OCR result for default parsing group
		        // OCR result exists if result is valid and non-empty
		        OcrResult ocrResult = result.getOcrResult();
	        } else {
	        	// not all relevant data was scanned, ask user
	        	// to try again
	        }
		}
	}
}
```

Available getters are:

##### `boolean isValid()`
Returns `true` if scan result contains at least one OCR result in one parsing group.

##### `boolean isEmpty()`
Returns `true` if scan result is empty, i.e. nothing was scanned. All getters should return `null` for empty result.

##### `String getParsedResult(String parserName)`
Returns the parsed result provided by parser with name `parserName` added to default parser group. If parser with name `parserName` does not exists in default parser group, returns `null`. If parser exists, but has failed to parse any data, returns empty string.

##### `String getParsedResult(String parserGroupName, String parserName)`
Returns the parsed result provided by parser with name `parserName` added to parser group named `parserGroupName`. If parser with name `parserName` does not exists in parser group with name `parserGroupName` or if parser group does not exists, returns `null`. If parser exists, but has failed to parse any data, returns empty string.

##### `OcrResult getOcrResult()`
Returns the [OCR result](https://blinkocr.github.io/blinkocr-android/com/microblink/results/ocr/OcrResult.html) structure for default parser group.

##### `OcrResult getOcrResult(String parserGroupName)`
Returns the [OCR result](https://blinkocr.github.io/blinkocr-android/com/microblink/results/ocr/OcrResult.html) structure for parser group named `parserGroupName`.

## <a name="pdf417Recognizer"></a> Scanning PDF417 barcodes

This section discusses the settings for setting up PDF417 recognizer and explains how to obtain results from PDF417 recognizer.

### Setting up PDF417 recognizer

To activate PDF417 recognizer, you need to create a [Pdf417RecognizerSettings](https://blinkocr.github.io/blinkocr-android/com/microblink/recognizers/blinkbarcode/pdf417/Pdf417RecognizerSettings.html) and add it to `RecognizerSettings` array. You can do this using following code snippet:

```java
private RecognizerSettings[] setupSettingsArray() {
	Pdf417RecognizerSettings sett = new Pdf417RecognizerSettings();
	// disable scanning of white barcodes on black background
	sett.setInverseScanning(false);
	// allow scanning of barcodes that have invalid checksum
	sett.setUncertainScanning(true);
	// disable scanning of barcodes that do not have quiet zone
	// as defined by the standard
	sett.setNullQuietZoneAllowed(false);

	// now add sett to recognizer settings array that is used to configure
	// recognition
	return new RecognizerSettings[] { sett };
}
```

As can be seen from example, you can tweak PDF417 recognition parameters with methods of `Pdf417RecognizerSettings`.

##### `setUncertainScanning(boolean)`
By setting this to `true`, you will enable scanning of non-standard elements, but there is no guarantee that all data will be read. This option is used when multiple rows are missing (e.g. not whole barcode is printed). Default is `false`.

##### `setNullQuietZoneAllowed(boolean)`
By setting this to `true`, you will allow scanning barcodes which don't have quiet zone surrounding it (e.g. text concatenated with barcode). This option can significantly increase recognition time. Default is `false`.

##### `setInverseScanning(boolean)`
By setting this to `true`, you will enable scanning of barcodes with inverse intensity values (i.e. white barcodes on dark background). This option can significantly increase recognition time. Default is `false`.

### Obtaining results from PDF417 recognizer
PDF417 recognizer produces [Pdf417ScanResult](https://blinkocr.github.io/blinkocr-android/com/microblink/recognizers/blinkbarcode/pdf417/Pdf417ScanResult.html). You can use `instanceof` operator to check if element in results array is instance of `Pdf417ScanResult` class. See the following snippet for an example:

```java
@Override
public void onScanningDone(RecognitionResults results) {
	BaseRecognitionResult[] dataArray = results.getRecognitionResults();
	for(BaseRecognitionResult baseResult : dataArray) {
		if(baseResult instanceof Pdf417ScanResult) {
			Pdf417ScanResult result = (Pdf417ScanResult) baseResult;
			
	        // getStringData getter will return the string version of barcode contents
			String barcodeData = result.getStringData();
			// isUncertain getter will tell you if scanned barcode is uncertain
			boolean uncertainData = result.isUncertain();
			// getRawData getter will return the raw data information object of barcode contents
			BarcodeDetailedData rawData = result.getRawData();
			// BarcodeDetailedData contains information about barcode's binary layout, if you
			// are only interested in raw bytes, you can obtain them with getAllData getter
			byte[] rawDataBuffer = rawData.getAllData();
		}
	}
}
```

As you can see from the example, obtaining data is rather simple. You just need to call several methods of the `Pdf417ScanResult` object:

##### `String getStringData()`
This method will return the string representation of barcode contents. Note that PDF417 barcode can contain binary data so sometimes it makes little sense to obtain only string representation of barcode data.

##### `boolean isUncertain()`
This method will return the boolean indicating if scanned barcode is uncertain. This can return `true` only if scanning of uncertain barcodes is allowed, as explained earlier.

##### `BarcodeDetailedData getRawData()`
This method will return the object that contains information about barcode's binary layout. You can see information about that object in [javadoc](https://blinkocr.github.io/blinkocr-android/com/microblink/results/barcode/BarcodeDetailedData.html). However, if you only need to access byte array containing, you can call method `getAllData` of `BarcodeDetailedData` object.

##### `Quadrilateral getPositionOnImage()`
Returns the position of barcode on image. Note that returned coordinates are in image's coordinate system which is not related to view coordinate system used for UI.

## <a name="custom1DBarDecoder"></a> Scanning one dimensional barcodes with _BlinkOCR_'s implementation

This section discusses the settings for setting up 1D barcode recognizer that uses _BlinkOCR_'s implementation of scanning algorithms and explains how to obtain results from that recognizer. Henceforth, the 1D barcode recognizer that uses _BlinkOCR_'s implementation of scanning algorithms will be refered as "Bardecoder recognizer".

### Setting up Bardecoder recognizer

To activate Bardecoder recognizer, you need to create a [BarDecoderRecognizerSettings](https://blinkocr.github.io/blinkocr-android/com/microblink/recognizers/blinkbarcode/bardecoder/BarDecoderRecognizerSettings.html) and add it to `RecognizerSettings` array. You can do this using following code snippet:

```java
private RecognizerSettings[] setupSettingsArray() {
	BarDecoderRecognizerSettings sett = new BarDecoderRecognizerSettings();
	// activate scanning of Code39 barcodes
	sett.setScanCode39(true);
	// activate scanning of Code128 barcodes
	sett.setScanCode128(true);
	// disable scanning of white barcodes on black background
	sett.setInverseScanning(false);
	// disable slower algorithm for low resolution barcodes
	sett.setTryHarder(false);

	// now add sett to recognizer settings array that is used to configure
	// recognition
	return new RecognizerSettings[] { sett };
}
```

As can be seen from example, you can tweak Bardecoder recognition parameters with methods of `BarDecoderRecognizerSettings`.

##### `setScanCode128(boolean)`
Method activates or deactivates the scanning of Code128 1D barcodes. Default (initial) value is `false`.

##### `setScanCode39(boolean)`
Method activates or deactivates the scanning of Code39 1D barcodes. Default (initial) value is `false`.

##### `setInverseScanning(boolean)`
By setting this to `true`, you will enable scanning of barcodes with inverse intensity values (i.e. white barcodes on dark background). This option can significantly increase recognition time. Default is `false`.

##### `setTryHarder(boolean)`
By setting this to `true`, you will enabled scanning of lower resolution barcodes at cost of additional processing time. This option can significantly increase recognition time. Default is `false`.

### Obtaining results from Bardecoder recognizer

Bardecoder recognizer produces [BarDecoderScanResult](https://blinkocr.github.io/blinkocr-android/com/microblink/recognizers/barcode/blinkbardecoder/BarDecoderScanResult.html). You can use `instanceof` operator to check if element in results array is instance of `BarDecoderScanResult` class. See the following snippet for example:

```java
@Override
public void onScanningDone(RecognitionResults results) {
	BaseRecognitionResult[] dataArray = results.getRecognitionResults();
	for(BaseRecognitionResult baseResult : dataArray) {
		if(baseResult instanceof BarDecoderScanResult) {
			BarDecoderScanResult result = (BarDecoderScanResult) baseResult;
			
			// getBarcodeType getter will return a BarcodeType enum that will define
			// the type of the barcode scanned
			BarcodeType barType = result.getBarcodeType();
	        // getStringData getter will return the string version of barcode contents
			String barcodeData = result.getStringData();
			// getRawData getter will return the raw data information object of barcode contents
			BarcodeDetailedData rawData = result.getRawData();
			// BarcodeDetailedData contains information about barcode's binary layout, if you
			// are only interested in raw bytes, you can obtain them with getAllData getter
			byte[] rawDataBuffer = rawData.getAllData();
		}
	}
}
```

As you can see from the example, obtaining data is rather simple. You just need to call several methods of the `BarDecoderScanResult` object:

##### `String getStringData()`
This method will return the string representation of barcode contents. 

##### `BarcodeDetailedData getRawData()`
This method will return the object that contains information about barcode's binary layout. You can see information about that object in [javadoc](https://blinkocr.github.io/blinkocr-android/com/microblink/results/barcode/BarcodeDetailedData.html). However, if you only need to access byte array containing, you can call method `getAllData` of `BarcodeDetailedData` object.

##### `String getExtendedStringData()`
This method will return the string representation of extended barcode contents. This is available only if barcode that supports extended encoding mode was scanned (e.g. code39).

##### `BarcodeDetailedData getExtendedRawData()`
This method will return the object that contains information about barcode's binary layout when decoded in extended mode. You can see information about that object in [javadoc](https://blinkocr.github.io/blinkocr-android/com/microblink/results/barcode/BarcodeDetailedData.html). However, if you only need to access byte array containing, you can call method `getAllData` of `BarcodeDetailedData` object. This is available only if barcode that supports extended encoding mode was scanned (e.g. code39).

##### `getBarcodeType()`
This method will return a [BarcodeType](https://blinkocr.github.io/blinkocr-android/com/microblink/recognizers/blinkbarcode/BarcodeType.html) enum that defines the type of barcode scanned.

## <a name="zxing"></a> Scanning barcodes with ZXing implementation

This section discusses the settings for setting up barcode recognizer that use ZXing's implementation of scanning algorithms and explains how to obtain results from it. _BlinkOCR_ uses ZXing's [c++ port](https://github.com/zxing/zxing/tree/00f634024ceeee591f54e6984ea7dd666fab22ae/cpp) to support barcodes for which we still do not have our own scanning algorithms. Also, since ZXing's c++ port is not maintained anymore, we also provide updates and bugfixes to it inside our codebase.

### Setting up ZXing recognizer

To activate ZXing recognizer, you need to create [ZXingRecognizerSettings](https://blinkocr.github.io/blinkocr-android/com/microblink/recognizers/blinkbarcode/zxing/ZXingRecognizerSettings.html) and add it to `RecognizerSettings` array. You can do this using the following code snippet:

```java
private RecognizerSettings[] setupSettingsArray() {
	ZXingRecognizerSettings sett=  new ZXingRecognizerSettings();
	// disable scanning of white barcodes on black background
	sett.setInverseScanning(false);
	// activate scanning of QR codes
	sett.setScanQRCode(true);

	// now add sett to recognizer settings array that is used to configure
	// recognition
	return new RecognizerSettings[] { sett };
}
```

As can be seen from example, you can tweak ZXing recognition parameters with methods of `ZXingRecognizerSettings`. Note that some barcodes, such as Code 39 are available for scanning with [_BlinkOCR_'s implementation](#custom1DBarDecoder). You can choose to use only one implementation or both (just put both settings objects into `RecognizerSettings` array). Using both implementations increases the chance of correct barcode recognition, but requires more processing time. Of course, we recommend using the _BlinkOCR_'s implementation for supported barcodes.

##### `setScanAztecCode(boolean)`
Method activates or deactivates the scanning of Aztec 2D barcodes. Default (initial) value is `false`.

##### `setScanCode128(boolean)`
Method activates or deactivates the scanning of Code128 1D barcodes. Default (initial) value is `false`.

##### `setScanCode39(boolean)`
Method activates or deactivates the scanning of Code39 1D barcodes. Default (initial) value is `false`.

##### `setScanDataMatrixCode(boolean)`
Method activates or deactivates the scanning of Data Matrix 2D barcodes. Default (initial) value is `false`.

##### `setScanEAN13Code(boolean)`
Method activates or deactivates the scanning of EAN 13 1D barcodes. Default (initial) value is `false`.

##### `setScanEAN8Code(boolean)`
Method activates or deactivates the scanning of EAN 8 1D barcodes. Default (initial) value is `false`.

##### `shouldScanITFCode(boolean)`
Method activates or deactivates the scanning of ITF 1D barcodes. Default (initial) value is `false`.

##### `setScanQRCode(boolean)`
Method activates or deactivates the scanning of QR 2D barcodes. Default (initial) value is `false`.

##### `setScanUPCACode(boolean)`
Method activates or deactivates the scanning of UPC A 1D barcodes. Default (initial) value is `false`.

##### `setScanUPCECode(boolean)`
Method activates or deactivates the scanning of UPC E 1D barcodes. Default (initial) value is `false`.

##### `setInverseScanning(boolean)`
By setting this to `true`, you will enable scanning of barcodes with inverse intensity values (i.e. white barcodes on dark background). This option can significantly increase recognition time. Default is `false`.

##### `setSlowThoroughScan(boolean)`
Use this method to enable slower, but more thorough scan procedure when scanning barcodes. By default, this option is turned on.

### Obtaining results from ZXing recognizer

ZXing recognizer produces [ZXingScanResult](https://blinkocr.github.io/blinkocr-android/com/microblink/recognizers/blinkbarcode/zxing/ZXingScanResult.html). You can use `instanceof` operator to check if element in results array is instance of `ZXingScanResult` class. See the following snippet for example:

```java
@Override
public void onScanningDone(RecognitionResults results) {
	BaseRecognitionResult[] dataArray = results.getRecognitionResults();
	for(BaseRecognitionResult baseResult : dataArray) {
		if(baseResult instanceof ZXingScanResult) {
			ZXingScanResult result = (ZXingScanResult) baseResult;
			
			// getBarcodeType getter will return a BarcodeType enum that will define
			// the type of the barcode scanned
			BarcodeType barType = result.getBarcodeType();
	        // getStringData getter will return the string version of barcode contents
			String barcodeData = result.getStringData();
		}
	}
}
```

As you can see from the example, obtaining data is rather simple. You just need to call several methods of the `ZXingScanResult` object:

##### `String getStringData()`
This method will return the string representation of barcode contents. 

##### `getBarcodeType()`
This method will return a [BarcodeType](https://blinkocr.github.io/blinkocr-android/com/microblink/recognizers/blinkbarcode/BarcodeType.html) enum that defines the type of barcode scanned.

# <a name="archConsider"></a> Processor architecture considerations

_BlinkOCR_ is distributed with both ARMv7, ARM64, x86 and x86_64 native library binaries.

ARMv7 architecture gives the ability to take advantage of hardware accelerated floating point operations and SIMD processing with [NEON](http://www.arm.com/products/processors/technologies/neon.php). This gives _BlinkOCR_ a huge performance boost on devices that have ARMv7 processors. Most new devices (all since 2012.) have ARMv7 processor so it makes little sense not to take advantage of performance boosts that those processors can give. Also note that some devices with ARMv7 processors do not support NEON instruction sets. Most popular are those based on [NVIDIA Tegra 2](https://en.wikipedia.org/wiki/Tegra#Tegra_2) fall into this category. Since these devices are old by today's standard, _BlinkOCR_ does not support them.

ARM64 is the new processor architecture that some new high end devices use. ARM64 processors are very powerful and also have the possibility to take advantage of new NEON64 SIMD instruction set to quickly process multiple pixels with single instruction.

x86 architecture gives the ability to obtain native speed on x86 android devices, like [Prestigio 5430](http://www.gsmarena.com/prestigio_multiphone_5430_duo-5721.php). Without that, _BlinkOCR_ will not work on such devices, or it will be run on top of ARM emulator that is shipped with device - this will give a huge performance penalty.

x86_64 architecture gives better performance than x86 on devices that use 64-bit Intel Atom processor.

However, there are some issues to be considered:

- ARMv7 build of native library cannot be run on devices that do not have ARMv7 compatible processor (list of those old devices can be found [here](http://www.getawesomeinstantly.com/list-of-armv5-armv6-and-armv5-devices/))
- ARMv7 processors does not understand x86 instruction set
- x86 processors do not understand neither ARM64 nor ARMv7 instruction sets
- however, some x86 android devices ship with the builtin [ARM emulator](http://commonsware.com/blog/2013/11/21/libhoudini-what-it-means-for-developers.html) - such devices are able to run ARM binaries but with performance penalty. There is also a risk that builtin ARM emulator will not understand some specific ARM instruction and will crash.
- ARM64 processors understand ARMv7 instruction set, but ARMv7 processors does not understand ARM64 instructions
- if ARM64 processor executes ARMv7 code, it does not take advantage of modern NEON64 SIMD operations and does not take advantage of 64-bit registers it has - it runs in emulation mode
- x86_64 processors understand x86 instruction set, but x86 processors do not understand x86_64 instruction set
- if x86_64 processor executes x86 code, it does not take advantage of 64-bit registers and use two instructions instead of one for 64-bit operations

`LibRecognizer.aar` archive contains ARMv7, ARM64, x86 and x86_64 builds of native library. By default, when you integrate _BlinkOCR_ into your app, your app will contain native builds for all processor architectures. Thus, _BlinkOCR_ will work on ARMv7, ARM64, x86 and x86_64 devices and will use ARMv7 features on ARMv7 devices and ARM64 features on ARM64 devices. However, the size of your application will be rather large.

## <a name="reduceSize"></a> Reducing the final size of your app

If your final app is too large because of _BlinkOCR_, you can decide to create multiple flavors of your app - one flavor for each architecture. With gradle and Android studio this is very easy - just add the following code to `build.gradle` file of your app:

```
android {
  ...
  splits {
    abi {
      enable true
      reset()
      include 'x86', 'armeabi-v7a', 'arm64-v8a', 'x86_64'
      universalApk true
    }
  }
}
```

With that build instructions, gradle will build four different APK files for your app. Each APK will contain only native library for one processor architecture and one APK will contain all architectures. In order for Google Play to accept multiple APKs of the same app, you need to ensure that each APK has different version code. This can easily be done by defining a version code prefix that is dependent on architecture and adding real version code number to it in following gradle script:

```
// map for the version code
def abiVersionCodes = ['armeabi-v7a':1, 'arm64-v8a':2, 'x86':3, 'x86_64':4]

import com.android.build.OutputFile

android.applicationVariants.all { variant ->
    // assign different version code for each output
    variant.outputs.each { output ->
        def filter = output.getFilter(OutputFile.ABI)
        if(filter != null) {
            output.versionCodeOverride = abiVersionCodes.get(output.getFilter(OutputFile.ABI)) * 1000000 + android.defaultConfig.versionCode
        }
    }
}
```

For more information about creating APK splits with gradle, check [this article from Google](https://sites.google.com/a/android.com/tools/tech-docs/new-build-system/user-guide/apk-splits#TOC-ABIs-Splits).

After generating multiple APK's, you need to upload them to Google Play. For tutorial and rules about uploading multiple APK's to Google Play, please read the [official Google article about multiple APKs](https://developer.android.com/google/play/publishing/multiple-apks.html).

### Removing processor architecture support in gradle without using APK splits

If you will not be distributing your app via Google Play or for some other reasons you want to have single APK of smaller size, you can completely remove support for certaing CPU architecture from your APK. **This is not recommended as this has [consequences](#archConsequences)**.

To remove certain CPU arhitecture, add following statement to your `android` block inside `build.gradle`:

```
android {
	...
	exclude 'lib/<ABI>/libBlinkOCR.so'
}
```

where `<ABI>` represents the CPU architecture you want to remove:

- to remove ARMv7 support, use `exclude 'lib/armeabi-v7a/libBlinkOCR.so'`
- to remove x86 support, use `exclude 'lib/x86/libBlinkOCR.so'`
- to remove ARM64 support, use `exclude 'lib/arm64-v8a/libBlinkOCR.so'`
- to remove x86_64 support, use `exclude 'lib/x86_64/libBlinkOCR.so'`

You can also remove multiple processor architectures by specifying `exclude` directive multiple times. Just bear in mind that removing processor architecture will have sideeffects on performance and stability of your app. Please read [this](#archConsequences) for more information.

### Removing processor architecture support in Eclipse

This section assumes that you have set up and prepared your Eclipse project from `LibRecognizer.aar` as described in chapter [Eclipse integration instructions](#eclipseIntegration).

If you are using Eclipse, removing processor architecture support gets really complicated. Eclipse does not support build flavors and you will either need to remove support for some processors or create several different library projects from `LibRecognizer.aar` - each one for specific processor architecture. 

Native libraryies in eclipse library project are located in subfolder `libs`:

- `libs/armeabi-v7a` contains native libraries for ARMv7 processor arhitecture
- `libs/x86` contains native libraries for x86 processor architecture
- `libs/arm64-v8a` contains native libraries for ARM64 processor architecture
- `libs/x86_64` contains native libraries for x86_64 processor architecture

To remove a support for processor architecture, you should simply delete appropriate folder inside Eclipse library project:

- to remove ARMv7 support, delete folder `libs/armeabi-v7a`
- to remove x86 support, delete folder `libs/x86`
- to remove ARM64 support, delete folder `libs/arm64-v8a`
- to remove x86_64 support, delete folder `libs/x86_64`

### <a name="archConsequences"></a> Consequences of removing processor architecture

However, removing a processor architecture has some consequences:

- by removing ARMv7 support _BlinkOCR_ will not work on devices that have ARMv7 processors. 
- by removing ARM64 support, _BlinkOCR_ will not use ARM64 features on ARM64 device
- by removing x86 support, _BlinkOCR_ will not work on devices that have x86 processor, except in situations when devices have ARM emulator - in that case, _BlinkOCR_ will work, but will be slow
- by removing x86_64 support, _BlinkOCR_ will not use 64-bit optimizations on x86_64 processor, but if x86 support is not removed, _BlinkOCR_ should work

Our recommendation is to include all architectures into your app - it will work on all devices and will provide best user experience. However, if you really need to reduce the size of your app, we recommend releasing separate version of your app for each processor architecture. It is easiest to do that with [APK splits](#reduceSize).

## <a name="combineNativeLibraries"></a> Combining _BlinkOCR_ with other native libraries

If you are combining _BlinkOCR_ library with some other libraries that contain native code into your application, make sure you match the architectures of all native libraries. For example, if third party library has got only ARMv7 and x86 versions, you must use exactly ARMv7 and x86 versions of _BlinkOCR_ with that library, but not ARM64. Using these architectures will crash your app in initialization step because JVM will try to load all its native dependencies in same preferred architecture and will fail with `UnsatisfiedLinkError`.

# <a name="troubleshoot"></a> Troubleshooting

## <a name="integrationTroubleshoot"></a> Integration problems

In case of problems with integration of the SDK, first make sure that you have tried integrating it into Android Studio by following [integration instructions](#quickIntegration). Althought we do provide [Eclipse ADT integration](#eclipseIntegration) integration instructions, we officialy do not support Eclipse ADT anymore. Also, for any other IDEs unfortunately you are on your own.

If you have followed [Android Studio integration instructions](#quickIntegration) and are still having integration problems, please contact us at [help.microblink.com](http://help.microblink.com).

## <a name="sdkTroubleshoot"></a> SDK problems

In case of problems with using the SDK, you should do as follows:

### Licencing problems

If you are getting "invalid licence key" error or having other licence-related problems (e.g. some feature is not enabled that should be or there is a watermark on top of camera), first check the ADB logcat. All licence-related problems are logged to error log so it is easy to determine what went wrong.

When you have determine what is the licence-relate problem or you simply do not understand the log, you should contact us [help.microblink.com](http://help.microblink.com). When contacting us, please make sure you provide following information:

* exact package name of your app (from your `AndroidManifest.xml` and/or your `build.gradle` file)
* licence key that is causing problems
* please stress out that you are reporting problem related to Android version of _BlinkOCR_ SDK
* if unsure about the problem, you should also provide excerpt from ADB logcat containing licence error

### Other problems

If you are having problems with scanning certain items, undesired behaviour on specific device(s), crashes inside _BlinkOCR_ or anything unmentioned, please do as follows:

* enable logging to get the ability to see what is library doing. To enable logging, put this line in your application:

	```java
	com.microblink.util.Log.setLogLevel(com.microblink.util.Log.LogLevel.LOG_VERBOSE);
	```

	After this line, library will display as much information about its work as possible. Please save the entire log of scanning session to a file that you will send to us. It is important to send the entire log, not just the part where crash occured, because crashes are sometimes caused by unexpected behaviour in the early stage of the library initialization.
	
* Contact us at [help.microblink.com](http://help.microblink.com) describing your problem and provide following information:
	* log file obtained in previous step
	* high resolution scan/photo of the item that you are trying to scan
	* information about device that you are using - we need exact model name of the device. You can obtain that information with [this app](https://play.google.com/store/apps/details?id=com.jphilli85.deviceinfo&hl=en)
	* please stress out that you are reporting problem related to Android version of _BlinkOCR_ SDK


# <a name="info"></a> Additional info
Complete API reference can be found in [Javadoc](https://blinkocr.github.io/blinkocr-android/index.html). 

For any other questions, feel free to contact us at [help.microblink.com](http://help.microblink.com).


