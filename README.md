# _BlinkOCR_ SDK for Android

[![Build Status](https://travis-ci.org/BlinkOCR/blinkocr-android.png)](https://travis-ci.org/BlinkOCR/blinkocr-android)

_BlinkOCR_ SDK for Android is SDK that enables you to easily add near real time OCR functionality to your app. With provided camera management you can easily create an app that scans receipts, e-mails and much more. You can also scan images stored as [Android Bitmaps](http://developer.android.com/reference/android/graphics/Bitmap.html) that are loaded either from gallery, network or SD card.

With _BlinkOCR_ you can scan free-form text or specialized formats like dates, amounts, e-mails and much more. Using specialized formats yields much better scanning quality than using free-form text mode.

Using _BlinkOCR_ in your app requires a valid license key. You can obtain a trial license key by registering to [Microblink dashboard](https://microblink.com/login). After registering, you will be able to generate a license key for your app. License key is bound to [package name](http://tools.android.com/tech-docs/new-build-system/applicationid-vs-packagename) of your app, so please make sure you enter the correct package name when asked.

See below for more information about how to integrate _BlinkOCR_ SDK into your app.

# Table of contents

* [Android _BlinkOCR_ integration instructions](#intro)
* [Quick Start](#quickStart)
  * [Quick start with demo app](#quickDemo)
  * [Quick integration of _BlinkOCR_ into your app](#quickIntegration)
  * [Eclipse integration instructions](#eclipseIntegration)
  * [How to integrate _BlinkOCR_ into your project using Maven](#mavenIntegration)
  * [_BlinkOCR's_ dependencies](#dependencies)
  * [Performing your first scan](#quickScan)
* [Advanced _BlinkOCR_ integration instructions](#advancedIntegration)
  * [Checking if _BlinkOCR_ is supported](#supportCheck)
  * [Customization of `BlinkOCRActivity` activity](#scanActivityCustomization)
  * [Embedding `RecognizerView` into custom scan activity](#recognizerView)
  * [`RecognizerView` reference](#recognizerViewReference)
* [Using direct API for recognition of Android Bitmaps](#directAPI)
  * [Understanding DirectAPI's state machine](#directAPIStateMachine)
  * [Using DirectAPI while RecognizerView is active](#directAPIWithRecognizer)
  * [Using ImageListener to obtain images that are being processed](#imageListener)
* [Recognition settings and results](#recognitionSettingsAndResults)
  * [Generic settings](#genericSettings)
  * [Scanning segments with BlinkOCR recognizer](#blinkOCR)
* [Processor architecture considerations](#archConsider)
  * [Reducing the final size of your app](#reduceSize)
  * [Combining _BlinkOCR_ with other native libraries](#combineNativeLibraries)
* [Troubleshooting](#troubleshoot)
  * [Integration problems](#integrationTroubleshoot)
  * [SDK problems](#sdkTroubleshoot)
* [Additional info](#info)

# <a name="intro"></a> Android _BlinkOCR_ integration instructions

The package contains Android Archive (AAR) that contains everything you need to use _BlinkOCR_ library. Besides AAR, package also contains a demo project that contains following modules:



 - BlinkOCRApp demonstrates integration of BlinkOCR component
 
_BlinkOCR_ is supported on Android SDK version 10 (Android 2.3) or later.


The library contains one activity: `BlinkOCRActivity`. It is responsible for camera control and recognition. If you create your own scanning UI, you will need to embed `RecognizerView` into your activity and pass activity's lifecycle events to it and it will control the camera and recognition process.

# <a name="quickStart"></a> Quick Start

## <a name="quickDemo"></a> Quick start with demo app

1. Open Android Studio.
2. In Quick Start dialog choose _Import project (Eclipse ADT, Gradle, etc.)_.
3. In File dialog select _BlinkOCRDemo_ folder.
4. Wait for project to load. If Android studio asks you to reload project on startup, select `Yes`.

## <a name="quickIntegration"></a> Quick integration of _BlinkOCR_ into your app

1. In Android Studio menu, click _File_, select _New_ and then select _Module_.
2. In new window, select _Import .JAR or .AAR Package_, and click _Next_.
3. In _File name_ field, enter the path to _LibRecognizer.aar_ and click _Finish_.
4. In your app's `build.gradle`, add dependency to `LibRecognizer`:

	```
	dependencies {
   		compile project(':LibRecognizer')
	}
	```
5. If you plan to use ProGuard, add following lines to your `proguard-rules.pro`:
	
	```
	-keep class com.microblink.** { *; }
	-keepclassmembers class com.microblink.** {
		*;
	}
	-dontwarn android.hardware.**
	-dontwarn android.support.v4.**
	```
6. Add _BlinkOCR's_ dependencies. See [_BlinkOCR's_ dependencies](#dependencies) section for more information.
	
## <a name="eclipseIntegration"></a> Eclipse integration instructions

We do not provide Eclipse integration demo apps. We encourage you to use Android Studio. We also do not test integrating _BlinkOCR_ with Eclipse. If you are having problems with _BlinkOCR_, make sure you have tried integrating it with Android Studio prior contacting us.

However, if you still want to use Eclipse, you will need to convert AAR archive to Eclipse library project format. You can do this by doing the following:

1. In Eclipse, create a new _Android library project_ in your workspace.
2. Clear the `src` and `res` folders.
3. Unzip the `LibRecognizer.aar` file. You can rename it to zip and then unzip it or use any tool.
4. Copy the `classes.jar` to `libs` folder of your Eclipse library project. If `libs` folder does not exist, create it.
5. Copy `android-support-v4.jar` to `libs` folder of your Eclipse library project. You can find `android-support-v4.jar` in `/path/to/your/android/SDK/extras/android/support/v4/android-support-v4.jar`.
6. Copy the contents of `jni` folder to `libs` folder of your Eclipse library project.
7. Replace the `res` folder on library project with the `res` folder of the `LibRecognizer.aar` file.

You’ve already created the project that contains almost everything you need. Now let’s see how to configure your project to reference this library project.

1. In the project you want to use the library (henceforth, "target project") add the library project as a dependency
2. Open the `AndroidManifest.xml` file inside `LibRecognizer.aar` file and make sure to copy all permissions, features and activities to the `AndroidManifest.xml` file of the target project.
3. Clean and Rebuild your target project
4. Add _BlinkOCR's_ dependencies. See [_BlinkOCR's_ dependencies](#dependencies) section for more information.

## <a name="mavenIntegration"></a> How to integrate _BlinkOCR_ into your project using Maven

Maven repository for _BlinkOCR_ SDK is: [http://maven.microblink.com](http://maven.microblink.com).

### Using gradle
In your build.gradle you first need to add _BlinkOCR_ maven repository to repositories list:

```
repositories {
	maven { url 'http://maven.microblink.com' }
}
```

After that, you just need to add _BlinkOCR_ as a dependency to your application:

```
dependencies {
    compile 'com.microblink:blinkocr:1.6.0'
}
```

Do not forget to add _BlinkOCR's_ dependencies to your app's dependencies. To see what are dependencies of _BlinkOCR_, check section [_BlinkOCR's_ dependencies](#dependencies).

### Using android-maven-plugin

Open your pom.xml file and add these directives as appropriate:

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
		  <version>1.6.0</version>
  	</dependency>
<dependencies>
```

Maven dependency requires android-maven-plugin version 4.0.0 (AAR support is required).

Do not forget to add _BlinkOCR's_ dependencies to your app's dependencies. To see what are dependencies of _BlinkOCR_, check section [_BlinkOCR's_ dependencies](#dependencies).

## <a name="dependencies"></a> _BlinkOCR's_ dependencies

_BlinkOCR_ depends on [Android support library](https://developer.android.com/tools/support-library/index.html).

To include that library into your app, in Android studio simply add following line in `dependencies` section:

```
compile 'com.android.support:support-v4:22.2.1'
```

If using Eclipse, you have already performed the step in [Eclipse integration instructions](#eclipseIntegration) in which you have copied android-support-v4.jar into `libs` folder of your Eclipse library. Just make sure Android support library version is at least `22.2.1`.

## <a name="quickScan"></a> Performing your first scan
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

Android 2.3 is the minimum android version on which _BlinkOCR_ is supported, but if required we may support even Android 2.2 devices, however additional testing on those devices will be required.

Camera video preview resolution also matters. In order to perform successful scans, camera preview resolution cannot be too low. _BlinkOCR_ requires minimum 480p camera preview resolution in order to perform scan. It must be noted that camera preview resolution is not the same as the video record resolution, although on most devices those are the same. However, there are some devices that allow recording of HD video (720p resolution), but do not allow high enough camera preview resolution (for example, [Sony Xperia Go](http://www.gsmarena.com/sony_xperia_go-4782.php) supports video record resolution at 720p, but camera preview resolution is only 320p - _BlinkOCR_ does not work on that device).

_BlinkOCR_ is native application, written in C++ and available for multiple platforms. Because of this, _BlinkOCR_ cannot work on devices that have obscure hardware architectures. We have compiled _BlinkOCR_ native code only for most popular Android [ABIs](https://en.wikipedia.org/wiki/Application_binary_interface). See [Processor architecture considerations](#archConsider) for more information about native libraries in _BlinkOCR_ and instructions how to disable certain architectures in order to reduce the size of final app.

### Checking for _BlinkOCR_ support in your app
To check whether the _BlinkOCR_ is supported on the device, you can do it in the following way:
	
```java
// check if BlinkOCR is supported on the device
RecognizerCompatibilityStatus supportStatus = RecognizerCompatibility.getRecognizerCompatibilityStatus(this);
if(supportStatus == RecognizerCompatibilityStatus.RECOGNIZER_SUPPORTED) {
	Toast.makeText(this, "BlinkOCR is supported!", Toast.LENGTH_LONG).show();
} else {
	Toast.makeText(this, "BlinkOCR is not supported! Reason: " + supportStatus.name(), Toast.LENGTH_LONG).show();
}
```

## <a name="scanActivityCustomization"></a> Customization of `BlinkOCRActivity` activity

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
	// set the license key
	intent.putExtra(BlinkOCRActivity.EXTRAS_SHOW_OCR_RESULT, false);
	```

* **`BlinkOCRActivity.EXTRAS_IMAGE_LISTENER`** - with this extra you can set your implementation of [ImageListener interface](https://blinkocr.github.io/blinkocr-android/com/microblink/image/ImageListener.html) that will obtain images that are being processed. Make sure that your [ImageListener](https://blinkocr.github.io/blinkocr-android/com/microblink/image/ImageListener.html) implementation correctly implements [Parcelable](https://developer.android.com/reference/android/os/Parcelable.html) interface with static [CREATOR](https://developer.android.com/reference/android/os/Parcelable.Creator.html) field. Without this, you might encounter a runtime error. For more information and example, see [Using ImageListener to obtain images that are being processed](#imageListener)

## <a name="recognizerView"></a> Embedding `RecognizerView` into custom scan activity
This section will discuss how to embed `RecognizerView` into your scan activity and perform scan.

1. First make sure that `RecognizerView` is a member field in your activity. This is required because you will need to pass all activity's lifecycle events to `RecognizerView`.
2. It is recommended to keep your scan activity in one orientation, such as `portrait` or `landscape`. Setting `sensor` as scan activity's orientation will trigger full restart of activity whenever device orientation changes. This will provide very poor user experience because both camera and _BlinkOCR_ native library will have to be restarted every time. There are measures for this behaviour and will be discussed [later](#scanOrientation).
3. In your activity's `onCreate` method, create a new `RecognizerView`, define its [settings and listeners](#recognizerViewReference) and then call its `create` method. After that, add your views that should be layouted on top of camera view.
4. Override your activity's `onStart`, `onResume`, `onPause`, `onStop` and `onDestroy` methods and call `RecognizerView's` lifecycle methods `start`, `resume`, `pause`, `stop` and `destroy`. This will ensure correct camera and native resource management. If you plan to manage `RecognizerView's` lifecycle independently of host activity's lifecycle, make sure the order of calls to lifecycle methods is the same as is with activities (i.e. you should not call `resume` method if `create` and `start` were not called first).

Here is the minimum example of integration of `RecognizerView` as the only view in your activity:

```java
public class MyScanActivity extends Activity implements ScanResultListener, CameraEventsListener {
	private RecognizerView mRecognizerView;
		
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// create RecognizerView
		mRecognizerView = new RecognizerView(this);
		   
		// setup array of recognition settings (described in chapter "Recognition 
		// settings and results")
		RecognizerSettings[] settArray = setupSettingsArray();
		if(!RecognizerCompatibility.cameraHasAutofocus(CameraType.CAMERA_BACKFACE, this)) {
			setarr = RecognizerSettingsUtils.filterOutRecognizersThatRequireAutofocus(setarr);
		}
		mRecognizerView.setRecognitionSettings(settings);
		
        try {
            // set license key
            mRecognizerView.setLicenseKey(this, "your license key");
        } catch (InvalidLicenceKeyException exc) {
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
    public void onScanningDone(BaseRecognitionResult[] dataArray, RecognitionType recognitionType) {
    	// this method is from ScanResultListener and will be called when scanning completes
    	// multiple scan results may be returned, depending on generic settings that define
    	// whether all found objects should be returned or only the first one (see subchapter
    	// "Generic settings" in chapter "Recognition settings and results")
    	
    	// When this method gets called, scanning gets paused. To resume scanning after this
    	// method has been called, call resumeScanning method.
    	mRecognizerView.resumeScanning();
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
    public void onStartupError(Throwable exc) {
        /** 
         * This method is from CameraEventsListener and will be called when opening of
         * camera resulted in exception. 
         * Known exceptions that can occur are following:
         *      * com.microblink.hardware.camera.CameraResolutionTooSmallException is thrown when largest possible camera preview
         *        resolution is not enough for making a successful scan
         *      * java.lang.UnsatisfiedLinkError is thrown when native library was not successfully loaded thus making scans impossible
         *      * java.lang.Throwable is thrown in all other cases (for example when camera is not ready because it is used by other
         *        apps or some unknown error has occurred)
         */
    }

    @Override
    public void onNotSupported(NotSupportedReason reason) {
        // This method is from CameraEventsListener and will be called when scanning is not supported 
        // on device. Reason for not being supported is given in 'reason' parameter.
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

__Important__

If you use `sensor` or similar screen orientation for your scan activity there is a catch. No matter if your activity is set to be restarted on configuration change or only notified via `onConfigurationChanged` method, if your activity's orientation is changed from `portrait` to `reversePortrait` or from `landscape` to `reverseLandscape` or vice versa, your activity will not be notified of this change in any way - it will not be neither restarted nor `onConfigurationChanged` will be called - the views in your activity will just be rotated by 180 degrees. This is a problem because it will make your camera preview upside down. In order to fix this, you first need to [find a way how to get notified of this change](https://stackoverflow.com/questions/9909037/how-to-detect-screen-rotation-through-180-degrees-from-landscape-to-landscape-or) and then you should call `changeConfiguration` method of `RecognizerView` so it will correct camera preview orientation.

## <a name="recognizerViewReference"></a> `RecognizerView` reference
The complete reference of `RecognizerView` is available in [Javadoc](https://blinkocr.github.io/blinkocr-android/com/microblink/view/recognition/RecognizerView.html). The usage example is provided in `` demo app provided with SDK. This section just gives a quick overview of `RecognizerView's` most important methods.

##### <a name="recognizerView_create"></a> `create()`
This method should be called in activity's `onCreate` method. It will initialize `RecognizerView's` internal fields and will initialize camera control thread. This method must be called after all other settings are already defined, such as listeners and recognition settings. After calling this method, you can add child views to `RecognizerView` with method `addChildView(View, boolean)`.

##### <a name="recognizerView_start"></a> `start()`
This method should be called in activity's `onStart` method. It will initialize background processing thread and start native library initialization on that thread.

##### <a name="recognizerView_resume"></a> `resume()`
This method should be called in activity's `onResume` method. It will trigger background initialization of camera.

##### <a name="recognizerView_pause"></a> `pause()`
This method should be called in activity's `onPause` method. It will stop the camera, but will keep native library loaded.

##### <a name="recognizerView_stop"></a> `stop()`
This method should be called in activity's `onStop` method. It will deinitialize native library, terminate background processing thread and free all resources that are no longer necessary.

##### <a name="recognizerView_destroy"></a> `destroy()`
This method should be called in activity's `onDestroy` method. It will free all resources allocated in `create()` and will terminate camera control thread.

##### <a name="recognizerView_changeConfiguration"></a> `changeConfiguration(Configuration)`
This method should be called in activity's `onConfigurationChanged` method. It will adapt camera surface to new configuration without the restart of the activity. See [Scan activity's orientation](#scanOrientation) for more information.

##### <a name="recognizerView_setCameraType"></a> `setCameraType(CameraType)`
With this method you can define which camera on device will be used. Default camera used is back facing camera.

##### <a name="recognizerView_setAspectMode"></a> `setAspectMode(CameraAspectMode)`
Define the [aspect mode of camera](https://blinkocr.github.io/blinkocr-android/com/microblink/view/CameraAspectMode.html). If set to `ASPECT_FIT` (default), then camera preview will be letterboxed inside available view space. If set to `ASPECT_FILL`, camera preview will be zoomed and cropped to use the entire view space.

##### <a name="recognizerView_setRecognitionSettings"></a> `setRecognitionSettings(RecognizerSettings[])`
With this method you can set the array of `RecognizerSettings` objects. Those objects will contain information about what will be scanned and how will scan be performed. For more information about recognition settings and results see [Recognition settings and results](#recognitionSettingsAndResults). This method must be called before `create()`.

##### <a name="recognizerView_setGenericRecognizerSettings"></a> `setGenericRecognizerSettings(GenericRecognizerSettings)`
With this method you can set the generic settings that will be affect all enabled recognizers or the whole recognition process. For more information about generic settings, see [Generic settings](#genericSettings). This method must be called before `create()`.

##### <a name="recognizerView_reconfigureRecognizers1"></a> `reconfigureRecognizers(RecognizerSettings[], GenericRecognizerSettings)`
With this method you can reconfigure the recognition process while recognizer is active. Unlike `setRecognitionSettings` and `setGenericRecognizerSettings`, this method can be called while recognizer is active (i.e. after `resume` was called), but paused (either `pauseScanning` was called or `onScanningDone` callback is being handled). For more information about recognition settings see [Recognition settings and results](#recognitionSettingsAndResults).

##### <a name="recognizerView_reconfigureRecognizers2"></a> `reconfigureRecognizers(RecognizerSettings[])`
With this method you can reconfigure the recognition process while recognizer is active. Unlike `setRecognitionSettings`, this method can be called while recognizer is active (i.e. after `resume` was called), but paused (either `pauseScanning` was called or `onScanningDone` callback is being handled). For more information about recognition settings see [Recognition settings and results](#recognitionSettingsAndResults).

##### <a name="recognizerView_setOrientationAllowedListener"></a> `setOrientationAllowedListener(OrientationAllowedListener)`
With this method you can set a [OrientationAllowedListener](https://blinkocr.github.io/blinkocr-android/com/microblink/view/OrientationAllowedListener.html) which will be asked if current orientation is allowed. If orientation is allowed, it will be used to rotate rotatable views to it and it will be passed to native library so that recognizers can be aware of the new orientation.

##### <a name="recognizerView_setRecognizerViewEventListener"></a> `setRecognizerViewEventListener(RecognizerViewEventListener)`
With this method you can set a [RecognizerViewEventListener](https://blinkocr.github.io/blinkocr-android/com/microblink/view/recognition/RecognizerViewEventListener.html) which will be notified when certain recognition events occur, such as when object has been detected.

##### <a name="recognizerView_setScanResultListener"></a> `setScanResultListener(ScanResultListener)`
With this method you can set a [ScanResultListener](https://blinkocr.github.io/blinkocr-android/com/microblink/view/recognition/ScanResultListener.html) which will be notified when recognition completes. After recognition completes, `RecognizerView` will pause its scanning loop and to continue the scanning you will have to call `resumeScanning` method. In this method you can obtain data from scanning results. For more information see [Recognition settings and results](#recognitionSettingsAndResults).

##### <a name="recognizerView_setCameraEventsListener"></a> `setCameraEventsListener(CameraEventsListener)`
With this method you can set a [CameraEventsListener](https://blinkocr.github.io/blinkocr-android/com/microblink/view/CameraEventsListener.html) which will be notified when various camera events occur, such as when camera preview has started, autofocus has failed or there has been an error while starting the camera.

##### <a name="recognizerView_recognizeBitmap"></a> `recognizeBitmap(Bitmap, ScanResultListener)`
This method can be used to request recognition of [Android Bitmap](https://developer.android.com/reference/android/graphics/Bitmap.html) between video frames. This method will implicitly call [pauseScanning](#recognizerView_pauseScanning) to prevent analysis of video frames while bitmap is being processed. The scan result will be returned via provided ScanResultListener, thus not polluting RecognizerView's default ScanResultListener. This method is much easier to use than [making all precautions when DirectAPI and RecognizerView are both active](#directAPIWithRecognizer).

##### <a name="recognizerView_recognizeBitmapWithSettings"></a> `recognizeBitmapWithSettings(Bitmap, ScanResultListener, RecognizerSettings[], GenericRecognizerSettings)`
Same as [recognizeBitmap](#recognizerView_recognizeBitmap), except given settings will be used for this single recognition and default settings will be restored after recognition ends.

##### <a name="recognizerView_pauseScanning"></a> `pauseScanning()`
This method pauses the scanning loop, but keeps both camera and native library initialized. This method is called internally when scan completes before `onScanningDone` is called.

##### <a name="recognizerView_resumeScanning"></a> `resumeScanning()`
With this method you can resume the paused scanning loop. This method implicitly calls `resetRecognitionState()`.

##### <a name="recognizerView_resumeScanningWithoutStateReset"></a> `resumeScanningWithoutStateReset()`
With this method you can resume the paused scanning loop without resetting recognition state. Be aware that after resuming, old recognition state might be reused for boosting recognition result. This may not be always a desired behaviour.

##### <a name="recognizerView_resetRecognitionState"></a> `resetRecognitionState()`
With this method you can reset internal recognition state. State is usually kept to improve recognition quality over time, but without resetting recognition state sometimes you might get poorer results (for example if you scan one object and then another without resetting state you might end up with result that contains properties from both scanned objects).

##### <a name="recognizerView_addChildView"></a> `addChildView(View, boolean)`
With this method you can add your own view on top of `RecognizerView`. `RecognizerView` will ensure that your view will be layouted exactly above camera preview surface (which can be letterboxed if aspect ratio of camera preview size does not match the aspect ratio of `RecognizerView` and camera aspect mode is set to `ASPECT_FIT`). Boolean parameter defines whether your view should be rotated with device orientation changes. The rotation is independent of host activity's orientation changes and allowed orientations will be determined from [OrientationAllowedListener](https://blinkocr.github.io/blinkocr-android/com/microblink/view/OrientationAllowedListener.html). See also [Scan activity's orientation](#scanOrientation) for more information why you should rotate your views independently of activity.

##### <a name="recognizerView_isCameraFocused"></a> `isCameraFocused()` 
This method returns `true` if camera thinks it has focused on object. Note that camera has to be loaded for this method to work.

##### <a name="recognizerView_focusCamera"></a> `focusCamera()` 
This method requests camera to perform autofocus. If camera does not support autofocus feature, method does nothing. Note that camera has to be loaded for this method to work.

##### <a name="recognizerView_isCameraTorchSupported"></a> `isCameraTorchSupported()` 
This method returns `true` if camera supports torch flash mode. Note that camera has to be loaded for this method to work.

##### <a name="recognizerView_setTorchState"></a> `setTorchState(boolean, SuccessCallback)` 
If torch flash mode is supported on camera, this method can be used to enable/disable torch flash mode. After operation is performed, [SuccessCallback](https://blinkocr.github.io/blinkocr-android/com/microblink/hardware/SuccessCallback.html) will be called with boolean indicating whether operation has succeeded or not. Note that camera has to be loaded for this method to work and that callback might be called on background non-UI thread.

##### <a name="recognizerView_setScanningRegion"></a> `setScanningRegion(Rectangle, boolean)`
You can use this method to define the scanning region and define whether this scanning region will be rotated with device if [OrientationAllowedListener](https://blinkocr.github.io/blinkocr-android/com/microblink/view/OrientationAllowedListener.html) determines that orientation is allowed. This is useful if you have your own camera overlay on top of `RecognizerView` that is set as rotatable view - you can thus synchronize the rotation of the view with the rotation of the scanning region native code will scan.

Scanning region is defined as [Rectangle](https://blinkocr.github.io/blinkocr-android/com/microblink/geometry/Rectangle.html). First parameter of rectangle is x-coordinate represented as percentage of view width, second parameter is y-coordinate represented as percentage of view height, third parameter is region width represented as percentage of view width and fourth parameter is region height represented as percentage of view height.

View width and height are defined in current context, i.e. they depend on screen orientation. If you allow your ROI view to be rotated, then in portrait view width will be smaller than height, whilst in landscape orientation width will be larger than height. This complies with view designer preview. If you choose not to rotate your ROI view, then your ROI view will be laid out either in portrait or landscape, depending on setting for your scan activity in `AndroidManifest.xml`

Note that scanning region only reflects to native code - it does not have any impact on user interface. You are required to create a matching user interface that will visualize the same scanning region you set here.

##### <a name="recognizerView_setMeteringAreas"/></a> `setMeteringAreas(Rectangle[])`
This method can only be called when camera is active. You can use this method to define regions which camera will use to perform meterings for focus, white balance and exposure corrections. On devices that do not support metering areas, this will be ignored. Some devices support multiple metering areas and some support only one. If device supports only one metering area, only the first rectangle from array will be used.

Each region is defined as [Rectangle](https://blinkocr.github.io/blinkocr-android/com/microblink/geometry/Rectangle.html). First parameter of rectangle is x-coordinate represented as percentage of view width, second parameter is y-coordinate represented as percentage of view height, third parameter is region width represented as percentage of view width and fourth parameter is region height represented as percentage of view height.

View width and height are defined in current context, i.e. they depend on screen orientation, as defined in `AndroidManifest.xml`. In portrait orientation view width will be smaller than height, whilst in landscape orientation width will be larger than height. This complies with view designer preview.

##### <a name="recognizerView_setImageListener"></a> `setImageListener(ImageListener)`
You can use this method to define [image listener](https://blinkocr.github.io/blinkocr-android/com/microblink/image/ImageListener.html) that will obtain images that are currently being processed by the native library. For more information and example implementation, see [Using ImageListener to obtain images that are being processed](#imageListener)

##### `setLicenseKey(String licenseKey)`
This method sets the license key that will unlock all features of the native library. You can obtain your license key from [Microblink website](http://microblink.com/login).

##### `setLicenseKey(String licenseKey, String licenseOwner)`
Use this method to set a license key that is bound to a licensee, not the application package name. You will use this method when you obtain a license key that allows you to use _BlinkOCR_ SDK in multiple applications. You can obtain your license key from [Microblink website](http://microblink.com/login).

# <a name="directAPI"></a> Using direct API for recognition of Android Bitmaps

This section will describe how to use direct API to recognize android Bitmaps without the need for camera. You can use direct API anywhere from your application, not just from activities.

1. First, you need to obtain reference to [Recognizer singleton](https://blinkocr.github.io/blinkocr-android/com/microblink/directApi/Recognizer.html).
2. Second, you need to initialize the recognizer.
3. After initialization, you can use singleton to process images. You cannot process multiple images in parallel.
4. Do not forget to terminate the recognizer after usage (it is a shared resource).

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
	   mRecognizer = Recognizer.getSingletonInstance();
		
	   try {
	       // set license key
	       mRecognizer.setLicenseKey(this, "your license key");
	   } catch (InvalidLicenceKeyException exc) {
	       return;
	   }

		// setupSettingsArray method is described in chapter "Recognition 
		// settings and results")
		mRecognizer.initialize(this, null, setupSettingsArray(), new DirectApiErrorListener() {
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
		mRecognizer.recognize(bitmap, this);
	}

	@Override
	protected void onStop() {
	   super.onStop();
	   mRecognizer.terminate();
	}

    @Override
    public void onScanningDone(BaseRecognitionResult[] dataArray, RecognitionType recognitionType) {
    	// this method is from ScanResultListener and will be called when scanning completes
    	// multiple scan results may be returned, depending on generic settings that define
    	// whether all found objects should be returned or only the first one (see subchapter
    	// "Generic settings" in chapter "Recognition settings and results")
    	
    	finish(); // in this example, just finish the activity
    }
    
}
```

## <a name="directAPIStateMachine"></a> Understanding DirectAPI's state machine

DirectAPI's Recognizer singleton is actually a state machine which can be in one of 4 states: `OFFLINE`, `UNLOCKED`, `READY` and `WORKING`. 

- When you obtain the reference to Recognizer singleton, it will be in `OFFLINE` state. 
- First you need to unlock the Recognizer by providing a valid licence key using `setLicenseKey` method. If you attempt to call `setLicenseKey` while Recognizer is not in `OFFLINE` state, you will get `IllegalStateException`.
- After successful unlocking, Recognizer singleton will move to `UNLOCKED` state.
- Once in `UNLOCKED` state, you can initialize Recognizer by calling `initialize` method. If you call `initialize` method while Recognizer is not in `UNLOCKED` state, you will get `IllegalStateException`.
- After successful initialization, Recognizer will move to `READY` state. Now you can call `recognize` method.
- When starting recognition with `recognize` or `recognizeWithSettings` method, Recognizer will move to `WORKING` state. If you attempt to call these methods while Recognizer is not in `READY` state, you will get `IllegalStateException`
- Recognition is performed on background thread so it is safe to call all Recognizer's method from UI thread
- When recognition is finished, Recognizer first moves back to `READY` state and then returns the result via provided `ScanResultListener`. 
- Please note that `ScanResultListener`'s `onScanningDone` method will be called on background processing thread, so make sure you do not perform UI operations in this calback.
- By calling `terminate` method, Recognizer singleton will release all its internal resources and will request processing thread to terminate. Note that even after calling `terminate` you might receive `onScanningDone` event if there was work in progress when `terminate` was called.
- `terminate` method can be called from any Recognizer singleton's state
- You can observe Recognizer singleton's state with method `getCurrentState`

## <a name="directAPIWithRecognizer"></a> Using DirectAPI while RecognizerView is active
Both [RecognizerView](#recognizerView) and DirectAPI recognizer use the same internal singleton that manages native code. This singleton handles initialization and termination of native library and propagating recognition settings to native library. If both RecognizerView and DirectAPI attempt to use the same singleton, a race condition will occur. This race condition is always solved in RecognizerView's favor, i.e.:

- if RecognizerView initializes the internal singleton before DirectAPI, DirectAPI's method `initialize` will detect that and will make sure that its settings are applied immediately before performing recognition and after recognition RecognizerView's settings will be restored to internal singleton
- if DirectAPI initializes the internal singleton before RecognizerView, RecognizerView will detect that and will overwrite internal singleton's settings with its own settings. The side effect is that next call to `recognize` on DirectAPI's Recognizer will **not** use settings given to `initialize` method, but will instead use settings given to RecognizerView. In order to ensure that your settings are used for recognition of bitmap, you should call method `recognizeWithSettings` which besides bitmap and result listener needs to receive settings that will be used for recognition of bitmap

If this raises too much confusion, we suggest not using DirectAPI while RecognizerView is active, instead use RecognizerView's methods [recognizeBitmap](#recognizerView_recognizeBitmap) or [recognizerBitmapWithSettings](#recognizerView_recognizeBitmapWithSettings) which will require no race conditions to be resolved.

## <a name="imageListener"></a> Using ImageListener to obtain images that are being processed

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

# <a name="recognitionSettingsAndResults"></a> Recognition settings and results

This chapter will discuss various recognition settings used to configure different recognizers and scan results generated by them.

## <a name="genericSettings"></a> Generic settings

Generic settings affect all enabled recognizers and the whole recognition process. The complete reference can be found in [javadoc](https://blinkocr.github.io/blinkocr-android/com/microblink/recognizers/settings/GenericRecognizerSettings.html). Here is the list of methods that are most relevant:

##### `setAllowMultipleScanResultsOnSingleImage(boolean)`
Sets whether or not outputting of multiple scan results from same image is allowed. If that is `true`, it is possible to return multiple recognition results produced by different recognizers from same image. However, single recognizer can still produce only a single result from single image. By default, this option is `false`, i.e. the array of `BaseRecognitionResults` will contain at most 1 element. The upside of setting that option to `false` is the speed - if you enable lots of recognizers, as soon as the first recognizer succeeds in scanning, recognition chain will be terminated and other recognizers will not get a chance to analyze the image. The downside is that you are then unable to obtain multiple results from different recognizers from single image.

##### `setNumMsBeforeTimeout(int)`
Sets the number of miliseconds _BlinkOCR_ will attempt to perform the scan it exits with timeout error. On timeout returned array of `BaseRecognitionResults` might be null, empty or may contain only elements that are not valid (`isValid` returns `false`) or are empty (`isEmpty` returns `true`).

##### `setFrameQualityEstimationMode(FrameQualityEstimationMode)`
Sets the mode of the frame quality estimation. Frame quality estimation is the process of estimating the quality of video frame so only best quality frames can be chosen for processing so no time is wasted on processing frames that are of too poor quality to contain any meaningful information. It is **not** used when performing recognition of [Android bitmaps](https://developer.android.com/reference/android/graphics/Bitmap.html) using [Direct API](#directAPI). You can choose 3 different frame quality estimation modes: automatic, always on and always off.

- In **automatic** mode (default), frame quality estimation will be used if device contains multiple processor cores or if on single core device at least one active recognizer requires frame quality estimation.
- In **always on** mode, frame quality estimation will be used always, regardless of device or active recognizers.
- In **always off** mode, frame quality estimation will be always disabled, regardless of device or active recognizers. This is not recommended setting because it can significantly decrease quality of the scanning process.

## <a name="blinkOCR"></a> Scanning segments with BlinkOCR recognizer

This section discusses the setting up of BlinkOCR recognizer and obtaining results from it. You should also check the `BlinkOCRApp` demo for example.

### Setting up BlinkOCR recognizer

BlinkOCR recognizer is consisted of one or more parsers that are grouped in parser groups. Each parser knows how to extract certain element from OCR result and also knows what are the best OCR engine options required to perform OCR on image. Parsers can be grouped in parser groups. Parser groups contain one or more parsers and are responsible for merging required OCR engine options of each parser in group and performing OCR only once and then letting each parser in group parse the data. Thus, you can make for own best tradeoff between speed and accuracy - putting each parser into its own group will give best accuracy, but will perform OCR of image for each parser which can consume a lot of processing time. On the other hand, putting all parsers into same group will perform only one OCR but with settings that are combined for all parsers in group, thus possibly reducing parsing quality.

Let's see this on example: assume we have two parsers at our disposal: `AmountParser` and `EMailParser`. `AmountParser` knows how to extract amount's from OCR result and requires from OCR only to recognise digits, periods and commas and ignore letters. On the other hand, `EMailParser` knows how to extract e-mails from OCR result and requires from OCR to recognise letters, digits, '@' characters and periods, but not commas. 

If we put both `AmountParser` and `EMailParser` into same parser group, the merged OCR engine settings will require recognition od all letters, all digits, '@' character, both period and comma. Such OCR result will contain all characters for `EMailParser` to properly parse e-mail, but might confuse `AmountParser` if OCR misclassifies some characters into digits.

If we put `AmountParser` in one parser group and `EMailParser` in another parser group, OCR will be performed for each parser group independently, thus preventing the `AmountParser` confusion, but two OCR passes of image will be performed, which can have a performance impact.

So to sum it up, BlinkOCR recognizer performs OCR of image for each available parser group and then runs all parsers in that group on obtained OCR result and saves parsed data. 

By definition, each parser results with string that represents a parsed data. The parsed string is stored under parser's name which has to be unique within parser group. So, when defining settings for BlinkOCR recognizer, when adding parsers, you need to provide a name for the parser (you will use that name for obtaining result later) and optionally provide a name for the parser group in which parser will be put into.

To activate BlinkOCR recognizer, you need to create [BlinkOCRRecognizerSettings](https://blinkocr.github.io/blinkocr-android/com/microblink/recognizers/ocr/blinkocr/BlinkOCRRecognizerSettings.html), add some parsers to it and add it to `RecognizerSettings` array. You can use the following code snippet to perform that:

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


- Amount parser - represented by [AmountParserSettings](https://blinkocr.github.io/blinkocr-android/com/microblink/recognizers/ocr/blinkocr/parser/generic/AmountParserSettings.html)
	- used for parsing amounts from OCR result
- IBAN parser - represented by [IbanParserSettings](https://blinkocr.github.io/blinkocr-android/com/microblink/recognizers/ocr/blinkocr/parser/generic/IbanParserSettings.html)
	- used for parsing International Bank Account Numbers (IBANs) from OCR result
- E-mail parser - represented by [EMailParserSettings](https://blinkocr.github.io/blinkocr-android/com/microblink/recognizers/ocr/blinkocr/parser/generic/EMailParserSettings.html)
	- used for parsing e-mail addresses
- Date parser - represented by [DateParserSettings](https://blinkocr.github.io/blinkocr-android/com/microblink/recognizers/ocr/blinkocr/parser/generic/DateParserSettings.html)
	- used for parsing dates in various formats
- Raw parser - represented by [RawParserSettings](https://blinkocr.github.io/blinkocr-android/com/microblink/recognizers/ocr/blinkocr/parser/generic/RawParserSettings.html)
	- used for obtaining raw OCR result

- Croatian reference parser - represented by [CroReferenceParserSettings](https://blinkocr.github.io/blinkocr-android/com/microblink/recognizers/ocr/blinkocr/parser/croatia/CroReferenceParserSettings.html)
	- used for parsing croatian payment reference numbers from OCR result

- Swedish amount parser - represented by [SweAmountParserSettings](https://blinkocr.github.io/blinkocr-android/com/microblink/recognizers/ocr/blinkocr/parser/sweden/SweAmountParserSettings.html)
	- used for parsing amounts from OCR of Swedish payment slips
- Swedish bank giro number parser - represented by [SweBankGiroParserSettings](https://blinkocr.github.io/blinkocr-android/com/microblink/recognizers/ocr/blinkocr/parser/sweden/SweBankGiroParserSettings.html)
	- used for parsing bank giro numbers from OCR of Swedish payment slips
- Swedish payment reference number parser - represented by [SweReferenceParserSettings](https://blinkocr.github.io/blinkocr-android/com/microblink/recognizers/ocr/blinkocr/parser/sweden/SweReferenceParserSettings.html)
	- used for parsing payment reference numbers from OCR of Swedish payment slips
- Swedish slip code parser - represented by [SweSlipCodeParserSettings](https://blinkocr.github.io/blinkocr-android/com/microblink/recognizers/ocr/blinkocr/parser/sweden/SweSlipCodeParserSettings.html)
	- used for parsing slip codes from OCR of Swedish payment slips

- Serbian bank account number parser - represented by [SerbAccountParserSettings](https://blinkocr.github.io/blinkocr-android/com/microblink/recognizers/ocr/blinkocr/parser/serbia/SerbAccountParserSettings.html)
	- used for parsing bank account numbers from Serbian payment slips
- Serbian payment reference number parser - represented by [SerbReferenceParserSettings](https://blinkocr.github.io/blinkocr-android/com/microblink/recognizers/ocr/blinkocr/parser/serbia/SerbReferenceParserSettings.html)
	- used for parsing payment reference numbers from Serbian payment slips

- Macedonian bank account number parser - represented by [MkdAccountParserSettings](https://blinkocr.github.io/blinkocr-android/com/microblink/recognizers/ocr/blinkocr/parser/macedonia/MkdAccountParserSettings.html)
	- used for parsing bank account numbers from Macedonian payment slips
- Macedonian payment reference number parser - represented by [MkdReferenceParserSettings](https://blinkocr.github.io/blinkocr-android/com/microblink/recognizers/ocr/blinkocr/parser/macedonia/MkdReferenceParserSettings.html)
	- used for parsing payment reference numbers from Macedonian payment slips

### Obtaining results from BlinkOCR recognizer

BlinkOCR recognizer produces [BlinkOCRRecognitionResult](https://blinkocr.github.io/blinkocr-android/com/microblink/recognizers/ocr/blinkocr/BlinkOCRRecognitionResult.html). You can use `instanceof` operator to check if element in results array is instance of `BlinkOCRRecognitionResult` class. See the following snipper for an example:

```java
@Override
public void onScanningDone(BaseRecognitionResult[] dataArray, RecognitionType recognitionType) {
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

# <a name="archConsider"></a> Processor architecture considerations

_BlinkOCR_ is distributed with both ARMv7, ARM64 and x86 native library binaries.

ARMv7 architecture gives the ability to take advantage of hardware accelerated floating point operations and SIMD processing with [NEON](http://www.arm.com/products/processors/technologies/neon.php). This gives _BlinkOCR_ a huge performance boost on devices that have ARMv7 processors. Most new devices (all since 2012.) have ARMv7 processor so it makes little sense not to take advantage of performance boosts that those processors can give. 

ARM64 is the new processor architecture that some new high end devices use. ARM64 processors are very powerful and also have the possibility to take advantage of new NEON64 SIMD instruction set to quickly process multiple pixels with single instruction.

x86 architecture gives the ability to obtain native speed on x86 android devices, like [Prestigio 5430](http://www.gsmarena.com/prestigio_multiphone_5430_duo-5721.php). Without that, _BlinkOCR_ will not work on such devices, or it will be run on top of ARM emulator that is shipped with device - this will give a huge performance penalty.

However, there are some issues to be considered:

- ARMv7 build of native library cannot be run on devices that do not have ARMv7 compatible processor (list of those old devices can be found [here](http://www.getawesomeinstantly.com/list-of-armv5-armv6-and-armv5-devices/))
- ARMv7 processors does not understand x86 instruction set
- x86 processors do not understand neither ARM64 nor ARMv7 instruction sets
- however, some x86 android devices ship with the builtin [ARM emulator](http://commonsware.com/blog/2013/11/21/libhoudini-what-it-means-for-developers.html) - such devices are able to run ARM binaries but with performance penalty. There is also a risk that builtin ARM emulator will not understand some specific ARM instruction and will crash.
- ARM64 processors understand ARMv7 instruction set, but ARMv7 processors does not understand ARM64 instructions
- if ARM64 processor executes ARMv7 code, it does not take advantage of modern NEON64 SIMD operations and does not take advantage of 64-bit registers it has - it runs in emulation mode

`LibRecognizer.aar` archive contains ARMv7, ARM64 and x86 builds of native library. By default, when you integrate _BlinkOCR_ into your app, your app will contain native builds for all processor architectures. Thus, _BlinkOCR_ will work on ARMv7 and x86 devices and will use ARMv7 features on ARMv7 devices and ARM64 features on ARM64 devices. However, the size of your application will be rather large.

## <a name="reduceSize"></a> Reducing the final size of your app

If your final app is too large because of _BlinkOCR_, you can decide to create multiple flavors of your app - one flavor for each architecture. With gradle and Android studio this is very easy - just add the following code to `build.gradle` file of your app:

```
android {
  ...
  splits {
    abi {
      enable true
      reset()
      include 'x86', 'armeabi-v7a', 'arm64-v8a'
      universalApk true
    }
  }
}
```

With that build instructions, gradle will build four different APK files for your app. Each APK will contain only native library for one processor architecture and one APK will contain all architectures. In order for Google Play to accept multiple APKs of the same app, you need to ensure that each APK has different version code. This can easily be done by defining a version code prefix that is dependent on architecture and adding real version code number to it in following gradle script:

```
// map for the version code
def abiVersionCodes = ['armeabi-v7a':1, 'x86':2, 'arm64-v8a':3]

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

However, if you are using Eclipse, things get really complicated. Eclipse does not support build flavors and you will either need to remove support for some processors or create three different library projects from `LibRecognizer.aar` - each one for specific processor architecture. In the next section, we will discuss how to remove processor architecture support from Eclipse library project.

### Removing processor architecture support in Eclipse

This section assumes that you have set up and prepared your Eclipse project from `LibRecognizer.aar` as described in chapter [Eclipse integration instructions](#eclipseIntegration).

Native libraryies in eclipse library project are located in subfolder `libs`:

- `libs/armeabi-v7a` contains native libraries for ARMv7 processor arhitecture
- `libs/x86` contains native libraries for x86 processor architecture
- `libs/arm64-v8a` contains native libraries for ARM64 processor architecture

To remove a support for processor architecture, you should simply delete appropriate folder inside Eclipse library project:

- to remove ARMv7 support, delete folder `libs/armeabi-v7a`
- to remove x86 support, delete folder `libs/x86`
- to remove ARM64 support, delete folder `libs/arm64-v8a`

### Consequences of removing processor architecture

However, removing a processor architecture has some consequences:

- by removing ARMv7 support _BlinkOCR_ will not work on devices that have ARMv7 processors. 
- by removing ARM64 support, _BlinkOCR_ will not use ARM64 features on ARM64 device
- by removing x86 support, _BlinkOCR_ will not work on devices that have x86 processor, except in situations when devices have ARM emulator - in that case, _BlinkOCR_ will work, but will be slow

Our recommendation is to include all architectures into your app - it will work on all devices and will provide best user experience. However, if you really need to reduce the size of your app, we recommend releasing separate version of your app for each processor architecture.

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


