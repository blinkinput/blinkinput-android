# Customizing BlinkInput with Templating API

This article will discuss how templating API can be used to perform scanning of documents which are not supported out of the box by BlinkInput SDK.

The templating API is an extension of `DetectorRecognizer` which is part of the BlinkInput SDK. Detector recognizer can be used for performing detection of various documents and besides that, it can also be used for implementing support for scanning custom document types by using templating API. 

The [next section](#detectorTemplating) will describe the concept of the templating API and how to use `DetectorRecognizer` to scan fields of interest from generic documents.

The [section that follows](#detectorTemplatingSample) will explain in more details how templating API should be used on concrete implemetation example for the Croatian identity card, with attached code snippets.

## <a name="detectorTemplating"></a> Scanning generic documents

For implementing support for scanning generic documents, `DetectorRecognizer` is used. Document processing by using templating API is described in the following few sections.

### 1) Document detection

First, document position should be detected on the input image because all fields of interest are defined in coordinates relative to document detection. For that purpose, `DetectorRecognizer` should be configured with appropriate `Detector` for the expected document type. When the document is detected, all further processing is done on the detected part of the image.

> `Detector` is an object that knows how to find a certain object on a camera image. For example `DocumentDetector` can be used to find documents by using edge detection and predefined aspect ratios. Another example is `MRTDDetector` that can find documents containing machine readable zone.

### 2) Defining locations of interest on the detected document

For each location of interest on the detected document, processing should be performed to extract needed information. To make processing of the document location possible, for example to perform the OCR, it should be dewarped (cropped and rotated) first. The concrete processor operates on the dewarped piece of the input image. So, for each document field that should be processed, the following should be defined:

- location coordinates relative to document detection
- the dewarp policy which determines the resulting image chunk for processing
- processors that will extract information from the prepared chunk of the image

For that purpose, `ProcessorGroup` is used.

> `ProcessorGroup` represents a group of processors that will be executed on the dewarped input image.

In addition to defining processors, which will be described in the next section, it is used for defining the document location on which processors will be executed and for choosing the dewarp policy.

> `DewarpPolicy` is an object which defines how specific location of interest should be dewarped. It determines the height and width of the resulting dewarped image in pixels.

There are three concrete types of the dewarp policies available:

- `FixedDewarpPolicy`: 
    - defines the exact height of the dewarped image in pixels
    - **usually the best policy for processor groups that use a legacy OCR engine**
- `DPIBasedDewarpPolicy`:
    - defines the desired DPI (*Dots Per Inch*)
    - the height of the dewarped image will be calculated based on the actual physical size of the document provided by the used detector and chosen DPI
    - **usually the best policy for processor groups that prepare location's raw image for output**
- `NoUpScalingDewarpPolicy`: 
    - defines the maximum allowed height of the dewarped image in pixels
    - the height of the dewarped image will be calculated in a way that no part of the image will be up-scaled
    - if the height of the resulting image is larger than maximum allowed, then the maximum allowed height will be used as actual height, which effectively scales down the image
    - **usually the best policy for processors that use neural networks, for example DEEP OCR, hologram detection or NN-based classification**

### 3) Processing locations of interest

When the chunk of the image which represents the location of interest from the scanned document is prepared, all processors from the associated group are executed to extract data. 

> `Processor` is an object that can perform recognition of the image. It is similar to `Recognizer`, but it is not stand-alone. `Processor` must be used within some `Recognizer` that supports processors like it is the case with the templating API recognizers. 

Available processors are:

- `ParserGroupProcessor`:
    - performs the OCR on the input image
    - lets all parsers associated with the group to extract data from the OCR result
- `ImageReturnProcessor`:
    - simply saves the input image

OCR is performed once for each activated `ParserGroupProcessor`. Before performing the OCR, best possible OCR engine options are calculated by combining engine options needed by each `Parser` from the group. For example, if one parser expects and produces the result from uppercase characters and other parser extracts data from digits, both uppercase characters and digits must be added to the list of allowed characters that can appear in the OCR result.

> `Parser` is an object that can extract structured data from raw OCR result.

There are a lot of different parsers available. For example, to extract dates from the OCR result, `DateParser` can be used. Another example is generic `RegexParser` which returns strings that are accepted by the predefined regular expression.
    
### 4) Document classification based on the processed data

There may be different versions of the same document type, which may have slight differences in contained information and positions of the fields. For example, the normal case for templating API is implementing support for both old and new version of the document where new version may contain additional information which is not present on the old version of the document. Also, positions of the fields may be different for the old and new version of the document.

To support such cases, there is a concept called a `TemplatingClass`. 

> `TemplatingClass` is an object containing two collections of processor groups and a classifier.

The two collections of processor groups within `TemplatingClass` are the classification processor group collection and non-classification processor group collection. The idea is that first all processor groups within classification collection perform processing. `TemplatingClassifier` decides then whether the object being recognized belongs to the current class and if it decides so, non-classification collection performs processing. The final `TemplatingRecognizer` then just contains a bunch of `TemplatingClass` objects.

> `TemplatingClassifier` is an object which decides whether the document that is being scanned belongs to the associated `TemplatingClass` or not, based on the data extracted by the classification processor groups.

## <a name="detectorTemplatingSample"></a> Templating API sample for Croatian ID card

This section will explain how to use templating API on the implementation example for [Croatian identity card](https://en.wikipedia.org/wiki/Croatian_identity_card). Code snippets will be written in Java, using Android BlinkInput SDK. The entire code sample which will be explained here can be found [here](https://github.com/blinkinput/blinkinput-android/blob/master/BlinkInputSample/BlinkInputTemplatingSample/src/main/java/com/microblink/util/templating/CroatianIDFrontSideTemplatingUtil.java).

Let's start by examining how front side of Croatian identity card looks like. Here are the pictures of front sides of both old and new versions of Croatian identity card:

![Front side of the old Croatian ID card](images/oldFront.jpg)
![Front side of the new Croatian ID card](images/newFront.jpg)

We will have two different `TemplatingClasses`, one for the old and one for the new version of the document. First, we will define locations of document number on both old and new versions of ID and then `TemplatingClassifier` will tell us whether the scanned document belongs to the associated `TemplatingClass`. After classifications, the recognizer will be able to use correct locations for each document version to extract information.

In templating API utility class [CroatianIDFrontSideTemplatingUtil.java](https://github.com/blinkinput/blinkinput-android/blob/master/BlinkInputSample/BlinkInputTemplatingSample/src/main/java/com/microblink/util/templating/CroatianIDFrontSideTemplatingUtil.java) there are methods which configure all needed components for the final implementation of the recognizer. They are called in the following order:

```java
// first, configure parsers that will extract OCR results
configureParsers();

// second, group configured parsers into ParserGroupProcessors and also
// add ImageReturnProcessors that will obtain images
configureProcessors();

// third, group processors into processor groups and define relative locations within
// document for each processor group to work on
configureProcessorGroups();

// fourth, group processor groups into document classes and for each class define a classifier
// that will determine whether document belongs to this class or not
configureClasses();

// finally, create document detector and associate it with DetectorRecognizer. Also, associate
// document classes with the same DetectorRecognizer.
configureDetectorRecognizer();
```

Implementation will be explained from the last called configuration method to the first one because this is the logical order when someone thinks about the implementation.

Let's start in a step by step manner.

For performing detection of ID card, we will use [DocumentDetector](https://blinkid.github.io/blinkid-android/com/microblink/entities/detectors/quad/document/DocumentDetector.html). `DocumentDetector` can detect document which conforms to any of the [DocumentSpecifications](https://blinkid.github.io/blinkid-android/com/microblink/entities/detectors/quad/document/DocumentSpecification.html) used in the initialisation of document detector. `DocumentSpecification` object defines low-level settings required for accurate detection of the document, like aspect ratio, expected positions and much more. Refer to [Javadoc](https://blinkid.github.io/blinkid-android/com/microblink/entities/detectors/quad/document/DocumentSpecification.html) for more information. To ease the creation of DocumentSpecification, BlinkInput SDK already provides prebuilt DocumentSpecification objects for common document sizes, like ID1 card (credit-card-like document), cheques, etc. You can use method [createFromPreset](https://blinkid.github.io/blinkid-android/com/microblink/entities/detectors/quad/document/DocumentSpecification.html#createFromPreset-com.microblink.entities.detectors.quad.document.DocumentSpecificationPreset-) to automatically obtain DocumentSpecification tweaked with optimal parameters.

```java
DocumentSpecification docSpecId1 = DocumentSpecification.createFromPreset(DocumentSpecificationPreset.DOCUMENT_SPECIFICATION_PRESET_ID1_CARD);
```

After that, we need to instantiate [DetectorRecognizer](https://blinkid.github.io/blinkid-android/com/microblink/entities/recognizers/detector/DetectorRecognizer.html), and use prepared detector which can detect ID card sized documents:

```java
mDocumentDetector = new DocumentDetector(docSpecId1);
// recognizer which is used for scanning, configured with the chosen detector
mDetectorRecognizer = new DetectorRecognizer(mDocumentDetector);
```

Since there are two versions of ID cards, we will need to set two templating classes - one for the old ID card and one for the new one:

```java
// here we set previously configured templating classes
mDetectorRecognizer.setTemplatingClasses(mOldID, mNewID);
```

We need to add support for correct recognition when the document is held upside down. Since card-like documents are symmetric, simple detection of quadrilateral representing the document will not tell us the orientation of the document. For that matter, we need to enable detection of upside down document:

```java
mDetectorRecognizer.setAllowFlippedRecognition(true);
```

This will ensure that after detection has been performed, locations for the classification processor groups will be dewarped, OCRed and parsed as if detection orientation is correct. If neither of parsers succeeds in parsing OCR data from any location, the detection will be flipped and everything will be repeated. Keep in mind that allowing flipped recognition requires very robust parsing of classification locations.

Templating classes are created in the following way:

```java
// configure old version class
{
    mOldID = new TemplatingClass();
    mOldID.setTemplatingClassifier(new CroIDOldTemplatingClassifier(mOldID, mOldDocumentNumberParser));

    mOldID.setClassificationProcessorGroups(mDocumentNumberOldID);
    mOldID.setNonClassificationProcessorGroups(mFirstNameOldID, mLastNameOldID, mSexCitizenshipDOBOldID, mFaceOldID, mFullDocument);
}
// configure new version class
{
    mNewID = new TemplatingClass();
    mNewID.setTemplatingClassifier(new CroIDNewTemplatingClassifier(mNewID, mNewDocumentNumberParser));

    mNewID.setClassificationProcessorGroups(mDocumentNumberNewID);
    mNewID.setNonClassificationProcessorGroups(mFirstNameNewID, mLastNameNewID, mSexCitizenshipDOBNewID, mFaceNewID, mFullDocument);
}
```

In this example, the extracted document number is used for classification of the document. Because of that, document number processor group is added to the classification processor groups collection. All other fields of interest are added to the collection of the non-classification processor groups.

[It will be shown later](#templatingClassifiers) how templating classifiers are implemented. For now, it is important to know that they simply tell whether the document belongs to the associated class: `true` or `false`.

Let's see how processor groups, which are responsible for processing locations of interest, are defined. Here we will give an example for the last name field. 

First, we need to precisely define the location of the last name field inside the document.

![Last name on the new Croatian ID card](images/newFront_surname.jpg)

To measure that, we take a ruler and first measure the dimensions of the document. For Croatian Identity card, we measure 85 mm width and 54 mm height. After that we need to measure the location of the last name field. After measuring we see that last name field starts from 23 mm from the left and 11 mm from the top and has width of 31 mm and height of 9 mm.

Here is the source code snippet that shows how to define last name `ProcessorGroups` for the both new and old versions of the ID card.

```java

//------------------------------------------------------------------------------------------
// Last name
//------------------------------------------------------------------------------------------
//
// The Croatian ID card has width of 85mm and height of 54mm. If we take a ruler and measure
// the locations of fields, we get the following measurements:
//
// on new croatian ID card, last name is located in following rectangle:
//
// left = 23 mm
// right = 54 mm
// top = 11 mm
// bottom = 20 mm
//
// ProcessorGroup requires converting this into relative coordinates, so we
// get the following:
//
// x = 23mm / 85mm = 0.271
// y = 11mm / 54mm = 0.204
// w = (54mm - 23mm) / 85mm = 0.365
// h = (20mm - 11mm) / 54mm = 0.167
//
// on old croatian ID card, last name is located in following rectangle:
//
// left = 23 mm
// right = 50 mm
// top = 11 mm
// bottom = 17 mm
//
// ProcessorGroup requires converting this into relative coordinates, so we
// get the following:
//
// x = 23mm / 85mm = 0.271
// y = 11mm / 54mm = 0.204
// width = (50mm - 23mm) / 85mm = 0.318
// height = (17mm - 11mm) / 54mm = 0.111
//
//------------------------------------------------------------------------------------------

mLastNameNewID = new ProcessorGroup(
        // location as described above
        new Rectangle(0.282f, 0.204f, 0.353f, 0.167f),
        // dewarp height as described above will be achieved using fixed dewarp policy
        new FixedDewarpPolicy(100),
        // processors in this processor group. Note that same processor can be in multiple
        // processor groups
        mLastNameParserGroup
);

mLastNameOldID = new ProcessorGroup(
        // location as described above
        new Rectangle(0.271f, 0.204f, 0.318f, 0.111f),
        // dewarp height as described above will be achieved using fixed dewarp policy
        new FixedDewarpPolicy(100),
        // processors in this processor group
        mLastNameParserGroup
);
```

Here is the code snippet which shows how to define `ProcessorGroups` for obtaining document images.

```java
//------------------------------------------------------------------------------------------
// Face image
//------------------------------------------------------------------------------------------
// In the same way as above, we create ProcessorGroup for image of the face on document.
//------------------------------------------------------------------------------------------

mFaceOldID = new ProcessorGroup(
        new Rectangle( 0.650f, 0.277f, 0.270f, 0.630f ),
        // use DPI-based policy to ensure images of 200 DPI
        new DPIBasedDewarpPolicy(200),
        mFaceImage
);

mFaceNewID = new ProcessorGroup(
        new Rectangle( 0.659f, 0.407f, 0.294f, 0.574f),
        // use DPI-based policy to ensure images of 200 DPI
        new DPIBasedDewarpPolicy(200),
        mFaceImage
);

//------------------------------------------------------------------------------------------
// Full document image
//------------------------------------------------------------------------------------------
// location of the full document is same regardless of document version
//------------------------------------------------------------------------------------------

mFullDocument = new ProcessorGroup(
        new Rectangle(0.f, 0.f, 1.f, 1.f),
        new DPIBasedDewarpPolicy(200),
        mFullDocumentImage
);
```

The definition of all used processors can be found in the [complete code sample](https://github.com/blinkinput/blinkinput-android/blob/master/BlinkInputSample/BlinkInputTemplatingSample/src/main/java/com/microblink/util/templating/CroatianIDFrontSideTemplatingUtil.java), here we will only show how `ParserGroupProcessor` for the last name is created and an example of `ImageReturnProcessor` for obtaining the face image of the ID card owner.

Here is the code snippet for defining the `ParserGroupProcessor` for the last name:

```java
// For extracting last names, we will use regex parser with regular expression which
// attempts to extract as may uppercase words as possible from single line.
mLastNameParser = new RegexParser("([A-ZŠĐŽČĆ]+ ?)+");

// we will tweak OCR engine options for the regex parser
BlinkOCREngineOptions options = (BlinkOCREngineOptions) mLastNameParser.getOcrEngineOptions();

// only uppercase characters are allowed
options.addUppercaseCharsToWhitelist(OcrFont.OCR_FONT_ANY);
// also specific Croatian characters should be added to the whitelist
options.addCharToWhitelist('Š', OcrFont.OCR_FONT_ANY);
options.addCharToWhitelist('Đ', OcrFont.OCR_FONT_ANY);
options.addCharToWhitelist('Ž', OcrFont.OCR_FONT_ANY);
options.addCharToWhitelist('Č', OcrFont.OCR_FONT_ANY);
options.addCharToWhitelist('Ć', OcrFont.OCR_FONT_ANY);

// put last name parser in its own parser group
mLastNameParserGroup = new ParserGroupProcessor(mLastNameParser);
```

Definition of the `ImageReturnProcessor` for the face image is very simple. 

```java
mFaceImage = new ImageReturnProcessor();
```
Position of the image is configured by the enclosing `ProcessorGroup`, which is shown earlier.

### <a name="templatingClassifiers"></a> Implementing the templating classifiers

Each `TemplatingClass` has associated templating classifier. For the old version of the Croatian identity card, there is `CroIDOldTemplatingClassifier` and for new one there is `CroIDNewTemplatingClassifier`. 

So, let's start with the `CroIDOldTemplatingClassifier`.

As every concrete templating classifier, it implements `TemplatingClassifier` interface, which requires implementing its `classify` method that is invoked while evaluating associated `TemplatingClass`. First, all processors within classification processor groups are executed. Then this method is invoked to determine whether non-classification processor groups should also be executed. If this method returns `false`, then non-classification processor groups will not be executed and evaluation will continue to next `TemplatingClass` within `TemplatingRecognizer`.

As we are making the classification decision based on the document number, which is returned by the `RegexParser` from the `ParserGroupProcessor` that is in the classification processor group, our classifier must be able to retrieve parsed data from the document number parser. For that purpose, it keeps the reference to the mentioned parser.

Also, because `TemplatingRecognizer` can be parcelized and run on the different activity from the one within it is created, classifier also implements `Parcelable` interface (`TemplatingClassifier` interface extends `Parcelable`). This is the most tricky part of the classifier implementation, it will be described later. For now, it is important to notice that our classifier has some additional member variables for that purpose.

```java
private static final class CroIDOldTemplatingClassifier implements TemplatingClassifier {

    private TemplatingClass mMyTemplatingClass;
    private RegexParser mOldDocumentNumberParser;
    private ParserParcelization mParcelizedOldDocumentNumberParser;
    
    CroIDOldTemplatingClassifier( @NonNull TemplatingClass myTemplatingClass, @Nullable RegexParser oldDocumentNumberParser ) {
        mMyTemplatingClass = myTemplatingClass;
        mOldDocumentNumberParser = oldDocumentNumberParser;
    }
    
    @Override
    public boolean classify(@NonNull TemplatingClass currentClass) {
        // obtains reference to the document number parser which is active in the current context
        // this will be explained later
        RegexParser oldDocumentNumberParser = obtainReferenceToDocumentNumberParser(currentClass);
    
        // if old document number parser has succeeded in parsing the document number, then
        // we are certain that we are scanning old version of Croatian National ID card
        String oldDocumentNumber = oldDocumentNumberParser.getResult().getParsedString();
        return !"".equals(oldDocumentNumber);
    }

    ...

}
```

Probably, you have noticed `ParserParcelization` class and its purpose is not clear at first sight. `ParserParcelization` is utility class that helps to serialize captured parser within templating classifier. It contains information how to access given `Parser` after `TemplatingClassifier` has been serialized and deserialized via `Parcel`. It is used for implementing the parcelization of the `CroIDOldTemplatingClassifier`.

**Notice that parser instance for the document number that is used during the scan after parcelization/deparcelization is not the same instance that is captured in `CroIDOldTemplatingClassifier` before parcelization. So, we need a mechanism to access active parser instance for classification during the scan.**

Here is the code snippet which shows how writing to `Parcel` is implemented:

```java
@Override
public void writeToParcel(Parcel dest, int flags) {
    //--------------------------------------------------------------------------------------
    // IMPLEMENTATION NOTE:
    //--------------------------------------------------------------------------------------
    // If we write mMyTemplatingClass to dest, we will trigger StackOverflowException because
    // this classifier is contained within mMyTemplatingClass, so writeToParcel will be called
    // recursively.
    // If we write mOldDocumentNumberParser to dest, it will be OK, but the problem will be
    // on deserialization side - the deparcelized instance of the parser will not be the same
    // as the one actually used for recognition and therefore it will not be possible to use
    // it for classification.
    //
    // To address this problem, we will create a ParserParcelization instance around our
    // parser and class. The ParcelParcelization will simply find our parser withing given
    // Templating Class and remember its coordinates. This coordinates will then be written
    // to dest and restored when creating this object from Parcel. Finally, those coordinates
    // will then be used to obtain access to the same parser within the context of recognition.
    //--------------------------------------------------------------------------------------

    ParserParcelization oldDocumentNumberParcelization = new ParserParcelization(mOldDocumentNumberParser, mMyTemplatingClass);
    // we do not need to use writeParcelable because ParserParcelization is not polymorphic
    oldDocumentNumberParcelization.writeToParcel(dest, flags);
}
```

When constructing the classifier from parcel, we just need to use `ParserParcelitation.CREATOR` to read previously written `ParserParcelization` instance from the `Parcel`. It knows how to obtain `Parser` reference from the given `TemplatingClass` which is passed to `classify` method.

```java
/**
 * Constructor from {@link Parcel}
 * @param in Parcel containing serialized classifier.
 */
private CroIDOldTemplatingClassifier(Parcel in) {
    mParcelizedOldDocumentNumberParser = ParserParcelization.CREATOR.createFromParcel(in);
}
```

The last thing that should be explained is how `obtainReferenceToDocumentNumberParser` method which is used in `classify` method is implemented. Here is its implementation with explanations:

```java
private RegexParser obtainReferenceToDocumentNumberParser(@NonNull TemplatingClass currentClass) {
    if ( mMyTemplatingClass == currentClass ) {
        // if the captured templating class is the same reference as currentClass, this means
        // that we are still using the original instance of the classifier, which has access
        // to original document number parser

        return mOldDocumentNumberParser;
    } else {
        // if references are not the same, this means that classifier has been parcelized
        // and then deparcelized during transmission to another activity. We need to ensure
        // that we perform the check of the document number parser's result within the
        // context we are currently running, so we need to utilize ParserParcelization
        // obtained during creating from Parcel to obtain access to the correct parser.
        // For more information, see implementation note in writeToParcel below.
        return mParcelizedOldDocumentNumberParser.getParser(currentClass);
    }
}
```

The implementation of the `CroIDNewTemplatingClassifier` is similar to the described implementation for `CroIDOldTemplatingClassifier`, so we will not repeat all steps. The only difference is that `CroIDNewTemplatingClassifier` captures the reference to `RegexParser` for the document number from the new version of the identity card and checks whether that parser has produced the result in its `classify` method. 
