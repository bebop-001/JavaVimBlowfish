#!/bin/sh
#
# Copyright 2019 sjs@kana-tutor.com
#
# Permission is hereby granted, free of charge, to any person
# obtaining a copy of this software and associated documentation
# files (the "Software"), to deal in the Software without
# restriction, including without limitation the rights to use, copy,
# modify, merge, publish, distribute, sublicense, and/or sell copies
# of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be
# included in all copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
# EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
# MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
# NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
# HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
# WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
# DEALINGS IN THE SOFTWARE.

# Create/update our local jar file if necessary.
# use java -jar to execute.

# make sure necessary executables are on the path.
which=`which which`;
if [ ! -e $which ]; then
    echo "can't find which." >2;
    exit 1;
fi
javac=`$which javac`;
if [ "$javac" = "" -o ! -e $javac ]; then
    echo "Please add javac to your path." >2;
    exit 1;
fi
java=`$which java`;
if [ "$java" = "" -o ! -e $java ];
    then echo "Please add java to your path." >2;
    exit 1;
fi
jar=`$which jar`;
if [ "$jar" = "" -o ! -e $jar ];
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
if [ ! -f $jarFile -o "$newest" != "$jarFile" ]; then
    make=true;
fi
if [ "$make" = "true" ]; then
    (
        $javac $src;
        cd src;
        $jar cfe ../$jarFile Main `find . -name \*.class`
    )
fi
$java -jar javaVimBlowfish.jar $*
