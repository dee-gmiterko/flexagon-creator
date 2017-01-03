# flexagon-creator
Tool for creating hexaflexagons from given images. Script outputs svg image with printable pattern and instructions.

## Thanks to Peter Bradshaw
This is java implementation of Peter Bradshaw's [Flexagon Creator](http://www.flatfeetpete.com/flexagon/).

## Usage
Compile jar using maven.

```mvn package```

Afterwards execute script with paths to your images and output path.

```java -jar target/flexagon-creator-1.0-SNAPSHOT-jar-with-dependencies.jar out.svg 1.png 2.png 3.png 4.png 5.png 6.png```
