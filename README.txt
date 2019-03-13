
This source implements the blowfish encryption used in Vim's blowfish2
format using Java.

It implements the same functionality as:
https://github.com/bebop-001/vimBlowfish.git

It's implemented in Java instead of Python because my goal is a simple
text editor in Android that also implements this functionality.  Since
it's Android I needed a version written in Java.  As with my VimBlowfish
application, I would never have been able to understand the algorithm
used by vim without the example of VimDecrypt by Gertjan van Zwietenat 
https://github.com/gertjanvanzwieten/vimdecrypt which he
developed using https://github.com/nlitsme/vimdecrypt by
Willem Hengeveld itsme@xs4all.nl.

I use the Blowfish.java from  _Applied_Cryptography_ by Bruce Schneier
instead of the one in the java library at javax.crypto.Cipher because
the standard java Blowfish implementation requires SecretKeySpec
when generating its password but vim uses the SHA256 message digest.

I developed this under IntelliJ's IDEA JetBrains IDE.  See
https://www.jetbrains.com/idea/ for more information.  I used the
free version and use that for my java development.

You will also find a shell utility called JavaVimBlowfish.sh
which will build the javaVimBlowfish.jar from the source java
files if necessary and execute the jar.

Usage from execution of the script:
USAGE: JavaVimBlowfish [-t -f] inFile outFile [password]
  -t  Run the selftest, print results and exit
  -f  force write to the output file if it exists.  If not set and the
      output file exists, we abort.
  inFile   the input file
  outFile  the output file
  password the encrypt/decrypt password.  Optional unless inFile or outFile
     is stdin opr stdout.
     We examine the input file and if it is a non-encrypted file, we 
  encrypt the result and send it to outFile.  Likewise, if the input
  is encrypted we decrypt and send the output to outFile.
     '-' for inFile/outFile means send input/output to stdin/stdout.
     If inFile or outFile is to stdin/stdout, a command line password
  must be supplied.

