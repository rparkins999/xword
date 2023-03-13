/*
 * ============================================================================
 *  Name     : xwordsearch.c
 *  Part of  : xword
 *  Created  : 08-06-2014 by Richard P. Parkins, M. A.
 *  Description:
 *     Native code search engine for xword
 *  Copyright: Richard P. Parkins, M. A.
 * ============================================================================
 */

// generates code
#include "xworddict.h"

/* Dictionary encoding:
 * 5 bit code:
 * 00000 end of word
 * 00001 optional s followed by end of word
 * 00010 optional ing followed by end of word
 * 00011 optional ed followed by end of word
 * 00100 optional d followed by end of word
 * 00101 optional ly followed by end of word
 * 00110 a
 * 00111 b
 * 01000 c
 * 01001 d
 * 01010 e
 * 01011 f
 * 01100 g
 * 01101 h
 * 01110 i
 * 01111 j
 * 10000 k
 * 10001 l
 * 10010 m
 * 10011 n
 * 10100 o
 * 10101 p
 * 10110 q
 * 10111 r
 * 11000 s
 * 11001 t
 * 11010 u
 * 11011 v
 * 11100 w
 * 11101 x
 * 11110 y
 * 11111 z
 *
 * six of these are packed into the top 30 bits of a 32-bit word
 * in the first word of an entry, the bottom 2 bits are half the number (0-6)
 * of initial letters to re-use from the previous entry
 * in subsequent words of an entry (if they exist) the bottom 2 bits are unused
 */

#include <jni.h>

static jmethodID AddItem;
static JNIEnv* env;
static jthrowable exc;
static jobject thiz;

void addit(const char * test, int len)
{
    jstring s = (*env)->NewStringUTF(env, test);
    if (!(exc = (*env)->ExceptionOccurred(env)))
    {
        (*env)->CallVoidMethod(env, thiz, AddItem, s);
        exc = (*env)->ExceptionOccurred(env);
    }
}

void testanagram(const char * test, const char * word, int len)
{
    /* Cheap and dirty bubble sort */
    int i, j;
    char buff[MAXLEN];
    for (i = 0; i < len; ++i)
    {
        buff[i] = test[i];
    }
    /* Cheap and dirty bubble sort */
    for (i = len - 1; i > 0; --i)
    {
        for (j = 0; j < i; ++j)
        {
            if (buff[j] > buff[j+1])
            {
                char c = buff[j];
                buff[j] = buff[j+1];
                buff[j+1] = c;
            }
        }
    }
    for (i = 0; i < len; ++i)
    {
        if (buff[i] != word[i])
        {
            return;
        }
    }
    for (i = 0; i < len; ++i)
    {
        buff[i] = test[i];
    }
    buff[len] = 0;
    addit(buff, len);
}

void testmatch(const char * test, const char * word, int len)
{
    int i;
    for (i = 0; i < len; ++i)
    {
        if (   (word[i] != '?')
            && (word[i] != test[i]))
        {
            return;
        }
    }
    char buff[MAXLEN];
    for (i = 0; i < len; ++i)
    {
        buff[i] = test[i];
    }
    buff[len] = 0;
    addit(buff, len);
}

void Java_uk_co_yahoo_p1rpp_xword_MainActivity_search(
    JNIEnv* envp, jobject thizp, jstring match)
{
    env = envp;
    thiz = thizp;
    jclass clazz = (*env)->GetObjectClass(env, thiz);
    if ((exc = (*env)->ExceptionOccurred(env)))
    {
        (*env)->Throw(env, exc);
    }
    AddItem = (*env)->GetMethodID(env, clazz, "AddItem",
                                  "(Ljava/lang/String;)V");
    if ((exc = (*env)->ExceptionOccurred(env)))
    {
        (*env)->Throw(env, exc);
    }
    int len = (*env)->GetStringUTFLength(env, match);
    const char * sa = (*env)->GetStringUTFChars(env, match, (jboolean *)0);
    jboolean isanagram = JNI_TRUE;
    int i, j;
    for (i = 0; i < len; ++i)
    {
        if (sa[i] == '?')
        {
            isanagram = JNI_FALSE;
            break;
        }
    }
    const char * word;
    char buff[MAXLEN];
    if (isanagram)
    {
        for (i = 0; i < len; ++i)
        {
            buff[i] = sa[i];
        }
        /* Cheap and dirty bubble sort */
        for (i = len - 1; i > 0; --i)
        {
            for (j = 0; j < i; ++j)
            {
                if (buff[j] > buff[j+1])
                {
                    char c = buff[j];
                    buff[j] = buff[j+1];
                    buff[j+1] = c;
                }
            }
        }
        word = buff;
    }
    else
    {
        word = sa;
    }
    char test[MAXLEN];

    /* search dictionary */
    const unsigned int * d = dict;
    const unsigned int * de = dict + sizeof(dict) / sizeof(*dict);
    while (d < de)
    {
        unsigned int w1 = *d++;
        int q = 2;
        char * pw = test + 2 * (w1 & 3);
        for(;;)
        {
            char b = (char)((w1 >> q) & 31);
            if (b < 6)
            {
                switch (b)
                {
                    case 0:
                        if (pw - test == len)
                        {
                            if (isanagram)
                            {
                                testanagram(test, word, len);
                            }
                            else
                            {
                                testmatch(test, word, len);
                            }
                        }
                        break;
                    case 1:
                        if (pw - test == len)
                        {
                            if (isanagram)
                            {
                                testanagram(test, word, len);
                            }
                            else
                            {
                                testmatch(test, word, len);
                            }
                        }
                        else if (pw + 1 - test == len)
                        {
                            *pw++ = 's';
                            if (isanagram)
                            {
                                testanagram(test, word, len);
                            }
                            else
                            {
                                testmatch(test, word, len);
                            }
                        }
                        break;
                    case 2:
                        if (pw - test == len)
                        {
                            if (isanagram)
                            {
                                testanagram(test, word, len);
                            }
                            else
                            {
                                testmatch(test, word, len);
                            }
                        }
                        else if (pw + 3 - test == len)
                        {
                            *pw++ = 'i';
                            *pw++ = 'n';
                            *pw++ = 'g';
                            if (isanagram)
                            {
                                testanagram(test, word, len);
                            }
                            else
                            {
                                testmatch(test, word, len);
                            }
                        }
                        break;
                    case 3:
                        if (pw - test == len)
                        {
                            if (isanagram)
                            {
                                testanagram(test, word, len);
                            }
                            else
                            {
                                testmatch(test, word, len);
                            }
                        }
                        else if (pw + 2 - test == len)
                        {
                            *pw++ = 'e';
                            *pw++ = 'd';
                            if (isanagram)
                            {
                                testanagram(test, word, len);
                            }
                            else
                            {
                                testmatch(test, word, len);
                            }
                        }
                        break;
                    case 4:
                        if (pw - test == len)
                        {
                            if (isanagram)
                            {
                                testanagram(test, word, len);
                            }
                            else
                            {
                                testmatch(test, word, len);
                            }
                        }
                        else if (pw + 1 - test == len)
                        {
                            *pw++ = 'd';
                            if (isanagram)
                            {
                                testanagram(test, word, len);
                            }
                            else
                            {
                                testmatch(test, word, len);
                            }
                        }
                        break;
                    case 5:
                        if (pw - test == len)
                        {
                            if (isanagram)
                            {
                                testanagram(test, word, len);
                            }
                            else
                            {
                                testmatch(test, word, len);
                            }
                        }
                        else if (pw + 2 - test == len)
                        {
                            *pw++ = 'l';
                            *pw++ = 'y';
                            if (isanagram)
                            {
                                testanagram(test, word, len);
                            }
                            else
                            {
                                testmatch(test, word, len);
                            }
                        }
                        break;
                }
                if (exc)
                {
                    (*env)->ReleaseStringUTFChars(env, match, sa);
                    (*env)->Throw(env, exc);
                    /* doesn't return */
                }
                else
                {
                    break; // out of for loop
                }
            }
            else
            {
                *pw++ = b + 'a' - 6;
                if (q < 27)
                {
                    q += 5;
                }
                else
                {
                    w1 = *d++;
                    q = 2;
                }
            }
        }
    }

    (*env)->ReleaseStringUTFChars(env, match, sa);
    return;
}
