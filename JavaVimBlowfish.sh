#!/bin/sh
# Create/update our local jar file if necessary.
# use java -jar to execute.

# make sure necessary executables are on the path.
which=`which which`;
if [ ! -e $which ]; then
    echo "can't find which." >2;
    exit 1;
fi
javac=`$which javac`;
if [ ! -e $javac ]; then
    echo "Please add javac to your path." >2;
    exit 1;
fi
java=`$which java`;
if [ ! -e $java ];
    then echo "Please add java to your path." >2;
    exit 1;
fi
jar=`$which jar`;
if [ ! -e $jar ];
    then echo "Please add jar to your path." >2;
    exit 1;
fi
src="
    ./src/Main.java
    ./src/Cipher/Blowfish/BlowfishECB.java
    ./src/kana_tutor/com/Reader.java
    ./src/kana_tutor/com/SelfTest.java
    ./src/kana_tutor/com/VimBlowfish.java
    ./src/util/ByteBuffer.java
    ./src/util/BytesDebug.java
    ./src/util/Log.java
"
classes="
    src/Main.class
    src/Cipher/Blowfish/BlowfishECB.class
    src/kana_tutor/com/VimBlowfish.class
    src/kana_tutor/com/VimBlowfish$Cipher.class
    src/kana_tutor/com/SelfTest.class
    src/kana_tutor/com/Reader.class
    src/util/ByteBuffer.class
    src/util/BytesDebug.class
    src/util/Log.class
"
jarFile=javaVimBlowfish.jar

# If our jar file isn't the newest of our files, rebuild.
newest=`ls -t $jarFile $src $classes 2> /dev/null | head -1`;
if [ "$newest" = "$jarFile" ]; then
    make=false;
fi
if [ ! -f $jarFile -o "$make" = "true" ]; then
    (
        $javac $src;
        cd src;
        $jar cfe ../$jarFile Main `find . -name \*.class`
    )
fi
$java -jar javaVimBlowfish.jar
