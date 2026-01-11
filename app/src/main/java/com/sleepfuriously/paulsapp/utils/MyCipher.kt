package com.sleepfuriously.paulsapp.utils

import android.util.Log

/**
 * Simple class to encrypt and decrypt strings.
 */
object MyCipher {

    /**
     * The chars to be played with.  This is the default string of Chars which
     * contains all the regular printable ASCII characters.  They are generated
     * programmatically to prevent hackers from getting a clue about what's
     * going on.  You may want to change this a little just to give hackers
     * a bit more to think about (they could easily assume this list).
     */
    private var defaultAlphabet: String = ""

    init {
        // Initialize defaultChars. Printable ASCII are values 32 <= x < 127
        for(i in 32 .. 126) {
            defaultAlphabet += i.toChar()
        }
    }


    /**
     * This will do it!  It will take a string and return an encrypted version.
     * It'll be reversed by calling [decrypt].
     *
     * NOTE
     *  Only valid for ASCII text.  At the moment I have no need for encrypting
     *  full unicode character sets.
     *
     * @param   inputText       The text you want to encrypt.
     *
     * @param   key             A string that will determine the encryption.
     *                          Longer keys are harder to break.  You'll use
     *                          the same key for decrypting.
     *
     * @param   paddingFront    The number of dummy characters to put in front
     *                          in the final encrypted text.  Remember to use
     *                          the same number when decrypting!  Default is 0.
     *
     * @param   paddingBack     Dummy characters at the end.
     *
     * @param   alphabet        The characters to use when encrypting.  This is
     *                          the set of the output characters.  Your input
     *                          text may not use all of these.  But if your
     *                          input text has chars that do not appear here,
     *                          your decryption WILL NOT WORK!
     *                          Defaults to all printable ASCII chars.
     */
    fun encrypt(
        inputText: String,
        key: String,
        paddingFront: Int = DEFAULT_PADDING_FRONT,
        paddingBack: Int = DEFAULT_PADDING_BACK,
        alphabet: String = defaultAlphabet
    ) : String {

        // generate random chars for padding.
        val paddingFrontStr = randomString(paddingFront, alphabet)
        val paddingBackStr = randomString(paddingBack, alphabet)

        /** encrypted message */
        var cipher = ""
        /** position of [inputText] as we go character by character */

        // Loop through each character in inputText.  Find a character in the
        // alphabet and add it to cipher.
        inputText.forEachIndexed { textPos, c ->
            // find the index into our key
            val keyIndex = textPos.mod(key.length)
            val keyChar = key[keyIndex]

            // Figure out the index in the alphabet for this char.  This is the meat.
            //      position_of_c_in_alphabet + position_of_keyChar_in_alphabet
            val index = (alphabet.indexOf(c) + alphabet.indexOf(keyChar)).mod(alphabet.length)

            // add the item in the alphabet at this index to our cipher
            cipher += alphabet[index]
        }

        // Concat the padding with our cipher and that's that!
        return "$paddingFrontStr$cipher$paddingBackStr"
    }


    /**
     * The opposite of [encrypt], this takes an encrypted string and figures
     * out what it should really look like (hopefully!).  Very important that
     * the parameters match exactly with the call to [encrypt].
     *
     * @return      The decrypted string.  On error returns null.
     */
    fun decrypt(
        inputText: String,
        key: String,
        paddingFront: Int = DEFAULT_PADDING_FRONT,
        paddingBack: Int = DEFAULT_PADDING_BACK,
        alphabet: String = defaultAlphabet
    ) : String? {
        /** this is the actual string to decrypt */

        // Quick check to make sure that we're not trying to do something funny.
        // This could happen if we're trying to decrypt something that is not
        // decrypted.
        val endIndex = inputText.length - paddingBack
        if (endIndex < paddingFront) {
            // This can only happen when trying to decrypt something that
            // is not properly encrypted (or encrypted with a different set
            // of paddings).
            Log.e(TAG, "decrypt() - error! endIndex ($endIndex) is less than startIndex($paddingFront)! Aborting!")
            return null
        }


        val encryptedPart = inputText.substring(
            startIndex = paddingFront,
            endIndex = endIndex
        )

        var decryptedTxt = ""
        var keyIndex = 0

        // loop through each letter
        encryptedPart.forEach { c ->
            val indexOfShift = (alphabet.indexOf(c) - alphabet.indexOf(key[keyIndex])).mod(alphabet.length)
            decryptedTxt += alphabet[indexOfShift]
            keyIndex++
            if (keyIndex > key.length - 1) {
                keyIndex = 0
            }
        }

        return decryptedTxt
    }


    /**
     * Generates a string of random characters with given length.
     *
     *  from  https://stackoverflow.com/a/73677850/624814
     *
     * @param   length      The number of characters in the final string.
     *
     * @param   charList    A string with the characters to choose from.
     */
    fun randomString(
        length: Int,
        charList: String
    ): String = CharArray(length) { charList.random() }.concatToString()


    /**
     * This creates a key that will ALWAYS be the same for the same size.
     * A hacker will have to step through the program to figure it out (no
     * long Strings to help).
     *
     * @param   size        The number of characters in the generated key.
     *
     * @param   alphabet    The set of characters to choose from for the key.
     *                      Defaults to all printable ASCII chars.
     *
     * @return  A reproducible key algorithmically generated.  It will always
     *          be the same key if the input are the same.  But it should be
     *          very difficult to reverse engineer.
     */
    fun generateGoodKey(size: Int, alphabet: String = defaultAlphabet) : String {
        //
        // I'm using my fibonacci sequences to make this key.  I'm using
        // fibonacci sequences to generate more sequences to generate
        // indices into the alphabet.  Should be hard to find a pattern.
        //

        // start with 6, 3, 13
        var a = 6L
        var b = 13L
        var iterations = 25L
        var key = ""

        for (i in 0 until size) {
            // get the index into the alphabet
            val index = myFibonacci(a, b, iterations).mod(alphabet.length)

            // and add that letter to our key
            key += alphabet[index]

            a = b
            b = myFibonacci(a, iterations, i.toLong())
            iterations += 3
        }

        return key
    }

    /**
     * Does the fibonacci thing, but instead of starting at 1, it starts
     * with the two given numbers.  It then iterates [iterations] times
     * and returns the value.
     *
     * @param   start1      The first value to add.
     *
     * @param   start2      The second value to add.  This will be added to
     *                      the result of the first addition as the sequence
     *                      rolls on.
     *
     * @param   iterations  The number of times to do the additions.  Note that
     *                      a value of 1 is the simply the same as adding the
     *                      two input values.
     *
     * @return  The final number after iterating.  Will be [start1] if
     *          no iterations are done.
     */
    fun myFibonacci(start1: Long, start2: Long, iterations: Long) : Long {
        var sum = start1
        var previous = start2

        (0 until iterations).forEach { _ ->
            val nextPrevious = sum
            sum += previous
            previous = nextPrevious
        }
        return sum
    }

}

private const val TAG = "MyCipher"
private const val DEFAULT_PADDING_FRONT = 5 + 7
private const val DEFAULT_PADDING_BACK = 8 + 33