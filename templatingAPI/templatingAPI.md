# Customizing BlinkInput with Templating API

This article will discuss how templating API can be used to perform scanning of documents which are not supported out of the box by BlinkInput SDK.

The templating API is an extension of `DetectorRecognizer` which is part of the BlinkInput SDK. Detector recognizer can be used for performing detection of various documents and besides that, it can also be used for implementing support for scanning custom document types by using templating API. 

The [next section](#detectorTemplating) will describe the concept of the templating API and how to use `DetectorRecognizer` to scan fields of interest from generic documents.

The [section that follows](#detectorTemplatingSample) will explain in more details how templating API should be used on concrete inplemetation example for the Croatian identity card, with attached code snippets.

## <a name="detectorTemplating"></a> Scanning generic documents

For implementing support for scanning generic documents, `DetectorRecognizer` is used. Document processing by using templating API is described in the following few sections.

### 1) Document detection

First, document position should be detected on the input image because all fields of interest are defined in coordinates relative to document detection. For that purpose, `DetectorRecognizer` sould be configured with appropriate `Detector` for the expected document type. When document is detected, all further processing is done on the detected part of the image.

> `Detector` is an object that knows how to find certain object on a camera image. For example `DocumentDetector` can be used to find documents by using edge detection and predefined aspect ratios. Other example is `MRTDDetector` that can find documents containing machine readable zone.

### 2) Defining locations of interest on the detected document

For each location of interest on the detected document, processing should be performed to extract needed information. To make processing of the document location possible, for example to perform the OCR, it should be dewarped (cropped and rotated) first. Concrete processor operates on the dewarped piece of the input image. So, for each document field that should be processed, following should be defined:

- location coordinates relative to document detection
- dewarp policy which determines the resulting image chunk for processing
- processors that will extract information from the prepared chunk of the image

For that purpose `ProcessorGroup` is used.

> `ProcessorGroup` represents a group of processors that will be executed on the dewarped input image.

In addition to defining processors, which will be described in the next section, it is used for defining the document location on which processors will be executed and for choosing the dewarp policy.

> `DewarpPolicy` is an object which defines how specific location of interest should be dewarped. It determines the height and width of the resulting dewarped image in pixels.

There are three concrete types of the dewarp policies available:

- `FixedDewarpPolicy`: 
    - defines exact height of the dewarped image in pixels
- `DPIBasedDewarpPolicy`:
    - defines the desired DPI (*Dots Per Inch*)
    - height of the dewarped image will be calculated based on the actual physical size of the document provided by the used detector and choosen DPI
    - **usually the best policy for processor groups that prepare location's raw image for output**
- `NoUpScalingDewarpPolicy`: 
    - defines maximal allowed height of the dewarped image in pixels
    - height of the dewarped image will be calculated in a way that no part of the image will be up-scaled
    - if height of the resulting image is larger than maximal allowed, then maximal allowed height will be used as actual height, which effectively scales down the image
    - **usually the best policy for processors that use neural networks, for example DEEP OCR, hologram detection or NN-based classification**

### 3) Processing locations of interest

When the chunk of the image which represents the location of interest from the scanned document is prepared, all processors from the associated group are executed to extract data. 

> `Processor` is an object that can perform recognition of the image. It is similar to `Recognizer`, but it is not stand-alone. `Processor` must be used within some `Recognizer` that supports processors, like it is the case with the templating API recognizers. 

Available processors are:

- `ParserGroupProcessor`:
    - performs the OCR on the input image
    - lets all parsers associated with the group to extract data from the OCR result
- `ImageReturnProcessor`:
    - simply saves the input image

OCR is performed once for each activated `ParserGroupProcessor`. Before performing the OCR, best possible OCR engine options are calculated by combining engine options needed by each `Parser` from the group. For example, if one parser expects and produces result from upercase characters and other parser extracts data from digits, both uppercase characters and digits must be added to the list of allowed characters that can appear in the OCR result.

> `Parser` is an object that can extract structured data from raw OCR result.

There are a lot of different parsers available, for example to extract dates from the OCR result, `DateParser` can be used. Another example is generic `RegexParser` which returns strings that are accepted by the predefined regular expression.
    
### 4) Document classification based on the processed data

There may be different versions of the same document type, which may have slight differences in contained information and positions of the fields. For example, normal case for templating API is implementing support for both old and new version of the document where new version may contain additional information which is not present on the old version of the document. Also positions of the fields may be different for old and new version of the document.

To support such cases, there is a concept called a `TemplatingClass`. 

> `TemplatingClass` is an object containing two collections of processor groups and a classifier.

The two collections of processor groups within `TemplatingClass` are the classification processor group collection and non-classification processor group collection. The idea is that first all processor groups within classification collection perform processing. `TemplatingClassifier` decides whether the object being recognizer belongs to current class and if it decides so, non-classification collection performs processing. The final `TemplatingRecognizer` then just contains a bunch of `TemplatingClass` objects.

> `TemplatingClassifier` is an object which decides whether document that is being scanned belongs to the associated `TemplatingClass` or not, based on the data extracted by the classification processor groups.

## <a name="detectorTemplatingSample"></a> Templating API sample for Croatian ID card

This section will explain how to use templating API on the implementation example for [Croatian identity card](https://en.wikipedia.org/wiki/Croatian_identity_card). Code snippets will be written in Java, using Android BlinkInput SDK. The entire code sample which will be explained here can be found [here](https://github.com/blinkinput/blinkinput-android/blob/master/BlinkInputSample/BlinkInputTemplatingSample/src/main/java/com/microblink/util/templating/CroatianIDFrontSideTemplatingUtil.java).

Let's start by examining how front side of Croatian identity card looks like. Here are the pictures of front sides of both old and new versions of Croatian identity card:

![Front side of the old Croatian ID card](images/oldFront.jpg)
![Front side of the new Croatian ID card](images/newFront.jpg)

We will have two different `TemplatingClasses`, one for old and one for new version of the document. First, we will define locations of document number on both old and new versions of ID and then `TemplatingClassifier` will tell us whether the scanned document belongs to the associated `TemplatingClass`. After classifications, the recognizer will be able to use correct locations for each document version to extract information.

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

Implementation will be explained from the last called configuration method to the first one, because this is the logical order when someone thinks about the implementation.

Let's start in a step by step manner.

For performing detection of ID card, we will use [DocumentDetector](https://blinkid.github.io/blinkid-android/com/microblink/entities/detectors/quad/document/DocumentDetector.html). `DocumentDetector` can detect document which conforms to any of the [DocumentSpecifications](https://blinkid.github.io/blinkid-android/com/microblink/entities/detectors/quad/document/DocumentSpecification.html) used in initialisation of document detector. `DocumentSpecification` object defines low level settings required for accurate detection of document, like aspect ratio, expected positions and much more. Refer to [javadoc](https://blinkid.github.io/blinkid-android/com/microblink/entities/detectors/quad/document/DocumentSpecification.html) for more information. To ease the creation of DocumentSpecification, BlinkInput SDK already provides prebuilt DocumentSpecification objects for common document sizes, like ID1 card (credit-card-like document), cheques, etc. You can use method [createFromPreset](https://blinkid.github.io/blinkid-android/com/microblink/entities/detectors/quad/document/DocumentSpecification.html#createFromPreset-com.microblink.entities.detectors.quad.document.DocumentSpecificationPreset-) to automatically obtain DocumentSpecification tweaked with optimal parameters.

```java
DocumentSpecification docSpecId1 = DocumentSpecification.createFromPreset(DocumentSpecificationPreset.DOCUMENT_SPECIFICATION_PRESET_ID1_CARD);
```

After that, we need to instantiate [DetectorRecognizer](https://blinkid.github.io/blinkid-android/com/microblink/entities/recognizers/detector/DetectorRecognizer.html), and use prepared detector which can detect ID card sized documents:

```java
mDocumentDetector = new DocumentDetector(docSpecId1);
// recognizer which is used for scanning, configured with the chosen detector
mDetectorRecognizer = new DetectorRecognizer(mDocumentDetector);
```

Since there are two versions of ID cards, we will need to set two templating classes - one for old ID card and one for new one:

```java
// here we set previously configured templating classes
mDetectorRecognizer.setTemplatingClasses(mOldID, mNewID);
```

We need to add support for correct recognition when document is held upside down. Since card-like documents are symmetric, simple detection of quadrilateral representing the document will not tell us the orientation of the document. For that matter, we need to enable detection of upside down document:

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

[It will be shown later](#templatingClassifiers) how templating classifiers are implemented. For now it is important to know that they simply tels whether the document belongs to the associated class: `true` or `false`.

Let's see how processor groups, which are responsible for processing locations of interest, are defined.

```java

//------------------------------------------------------------------------------------------
// First and last name
//------------------------------------------------------------------------------------------
//
// The Croatian ID card has width of 85mm and height of 54mm. If we take a ruler and measure
// the locations of fields, we get the following measurements:
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
// In the same manner we can measure the locations of first name on both old and new ID cards.
//
// Both first and last name can hold a single line of text, but both on new and old ID card
// first name is printed with smaller font than last name. Therefore, we will require that
// dewarped image for last names will be of height 100 pixels and for first names of height 150
// pixels.
// The width of the image will be automatically determined to keep the original aspect ratio.
//------------------------------------------------------------------------------------------

mFirstNameOldID = new ProcessorGroup(
        // location as described above
        new Rectangle(0.282f, 0.333f, 0.306f, 0.167f),
        // dewarp height as described above will be achieved using fixed dewarp policy
        new FixedDewarpPolicy(150),
        // processors in this processor group
        mFirstNameParserGroup
);

mFirstNameNewID = new ProcessorGroup(
        // location as described above
        new Rectangle(0.282f, 0.389f, 0.353f, 0.167f),
        // dewarp height as described above will be achieved using fixed dewarp policy
        new FixedDewarpPolicy(150),
        // processors in this processor group. Note that same processor can be in multiple
        // processor groups
        mFirstNameParserGroup
);

mLastNameOldID = new ProcessorGroup(
        // location as described above
        new Rectangle(0.271f, 0.204f, 0.318f, 0.111f),
        // dewarp height as described above will be achieved using fixed dewarp policy
        new FixedDewarpPolicy(100),
        // processors in this processor group
        mLastNameParserGroup
);

mLastNameNewID = new ProcessorGroup(
        // location as described above
        new Rectangle(0.282f, 0.204f, 0.353f, 0.167f),
        // dewarp height as described above will be achieved using fixed dewarp policy
        new FixedDewarpPolicy(100),
        // processors in this processor group. Note that same processor can be in multiple
        // processor groups
        mLastNameParserGroup
);

//------------------------------------------------------------------------------------------
// Sex, citizenship and date of birth
//------------------------------------------------------------------------------------------
// Sex, citizenship and date of birth parsers are bundled together into single parser group
// processor. Now let's define a processor group for new and old ID version for that
// processor.
//
// Firstly, we need to take a ruler and measure the location from which all these fields
// will be extracted.
// On old croatian ID cards, the location containing both sex, citizenship and date of birth
// is in following rectangle:
//
// left = 35 mm
// right = 57 mm
// top = 27 mm
// bottom = 43 mm
//
// ProcessorGroup requires converting this into relative coordinates, so we
// get the following:
//
// x = 35mm / 85mm = 0.412
// y = 27 mm / 54mm = 0.500
// w = (57mm - 35mm) / 85mm = 0.259
// h = (43mm - 27mm) / 54mm = 0.296
//
// Similarly, on new croatian ID card, rectangle holding same information is the following:
//
// left = 33 mm
// right = 57 mm
// top = 27 mm
// bottom = 43 mm
//
// ProcessorGroup requires converting this into relative coordinates, so we
// get the following:
//
// x = 33mm / 85mm = 0.388
// y = 27mm / 54mm = 0.556
// w = (57mm - 33mm) / 85mm = 0.282
// h = (43mm - 27mm) / 54mm = 0.296
//
// This location contains three fields in three lines of text. So we will set the height of
// dewarped image to 300 pixels.
// The width of the image will be automatically determined to keep the original aspect ratio.
//------------------------------------------------------------------------------------------

mSexCitizenshipDOBOldID = new ProcessorGroup(
        // location as described above
        new Rectangle(0.412f, 0.500f, 0.259f, 0.296f),
        // fixed dewarp policy to get dewarp height of exactly 300 pixels
        new FixedDewarpPolicy(300),
        // processors in this processor group
        mSexCitizenshipDOBGroup
);

mSexCitizenshipDOBNewID = new ProcessorGroup(
        // location as described above
        new Rectangle(0.388f, 0.500f, 0.282f, 0.296f),
        // fixed dewarp policy to get dewarp height of exactly 300 pixels
        new FixedDewarpPolicy(300),
        // processors in this processor group
        mSexCitizenshipDOBGroup
);

//------------------------------------------------------------------------------------------
// Document number
//------------------------------------------------------------------------------------------
// In same way as above, we create ProcessorGroup for old and new versions of document number
// parsers.
//------------------------------------------------------------------------------------------

mDocumentNumberOldID = new ProcessorGroup(
        new Rectangle(0.047f, 0.519f, 0.224f, 0.111f),
        new FixedDewarpPolicy(150),
        mOldDocumentNumberGroup
);

mDocumentNumberNewID = new ProcessorGroup(
        new Rectangle(0.047f, 0.685f, 0.224f, 0.111f),
        new FixedDewarpPolicy(150),
        mNewDocumentNumberGroup
);

//------------------------------------------------------------------------------------------
// Face image
//------------------------------------------------------------------------------------------
// In same way as above, we create ProcessorGroup for image of the face on document.
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
// location of full document is same regardless of document version
//------------------------------------------------------------------------------------------

mFullDocument = new ProcessorGroup(
        new Rectangle(0.f, 0.f, 1.f, 1.f),
        new DPIBasedDewarpPolicy(200),
        mFullDocumentImage
);
```

The definition of all used processors can be found in the [complete code sample](https://github.com/blinkinput/blinkinput-android/blob/master/BlinkInputSample/BlinkInputTemplatingSample/src/main/java/com/microblink/util/templating/CroatianIDFrontSideTemplatingUtil.java), here we will only show how `ParserGroupProcessor` for the first name is created and an example of `ImageReturnProcessor` for obtaining the face image of the ID card owner.

Here is the code snippet for defining the `ParserGroupProcessor` for the first name:

```java
// For extracting first names, we will use regex parser with regular expression which
// attempts to extract as may uppercase words as possible from single line.
mFirstNameParser = new RegexParser("([A-ZŠĐŽČĆ]+ ?)+");

// we will tweak OCR engine options for the regex parser
BlinkOCREngineOptions options = (BlinkOCREngineOptions) mFirstNameParser.getOcrEngineOptions();

// only uppercase characters are allowed
options.addUppercaseCharsToWhitelist(OcrFont.OCR_FONT_ANY);
// also specific Croatian characters should be added to the whitelist
options.addCharToWhitelist('Š', OcrFont.OCR_FONT_ANY);
options.addCharToWhitelist('Đ', OcrFont.OCR_FONT_ANY);
options.addCharToWhitelist('Ž', OcrFont.OCR_FONT_ANY);
options.addCharToWhitelist('Č', OcrFont.OCR_FONT_ANY);
options.addCharToWhitelist('Ć', OcrFont.OCR_FONT_ANY);

// put first name parser in its own parser group
mFirstNameParserGroup = new ParserGroupProcessor(mFirstNameParser);
```

Definition of the `ImageReturnProcessor` for the face image is very simple. 

```java
mFaceImage = new ImageReturnProcessor();
```
Position of the image is configured by the enclosing `ProcessorGroup`, which is shown earlier.

### <a name="templatingClassifiers"></a> Implementing the templating classifiers

**TODO**
