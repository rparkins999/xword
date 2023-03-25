/*
 * Copyright © 2023. Richard P. Parkins, M. A.
 * Released under GPL V3 or later
 *
 * This compresses the dictionaries and makes a header file
 * declaring the arrays containing the compressed versions
 * which is then included by xwordsearch-jni.c
 */

#include <unistd.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>

unsigned char bytes[7000000];
unsigned char * words [700000];
unsigned char * suffixes[700000];
int suffcounts[700000];

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

int maxlen = 0;

void onedict(int fd, int fdout, char * arrayname) {
    int nb = 0;
    int nw = 0;
    int nsuff = 0;
    int n32 = 0;
    char buff[1024];
    words[nw] = bytes;
    while(read(fd, bytes + nb, 1))
    {
        if (bytes[nb] == '\n')
        {
            bytes[nb++] = 0;
            int l = strlen(words[nw]);
            if (l > maxlen) {
                maxlen = l;
            }
            words[++nw] = bytes + nb;
        }
        else if ((bytes[nb] >= 'A') && (bytes[nb] <= 'Z'))
        {
            ++nb;
        }
        else if ((bytes[nb] >= 'a') && (bytes[nb] <= 'z'))
        {
            ++nb;
        }
        else
        {
            fprintf(stderr, "Unexpected char 0x%x in line %d\n", bytes[nb], nw);
        }
    }
    fprintf(stderr, "%d bytes, %d words, average length %g\n", nb-nw, nw, (nb-nw)/(float)nw);
    int err = write(fdout, buff, snprintf(buff, sizeof(buff),
                    "unsigned int %s[] = {\n", arrayname));
    if (err < 0)
    {
        fprintf(stderr,
                "write(\"xworddict.h\", \"#define MAXLEN %d\n"
                "unsigned int %s[] = {\n\") -> %s\n",
                maxlen, arrayname, strerror(errno));
        exit(1);
    }
    int i, j;
    unsigned char * p = NULL;
    for (i = 0; i < nw; ++i)
    {
        if (*(words[i]))
        {
            int nre = 0;
            if (p)
            {
                if (!strncmp(p, words[i], 6))
                {
                    nre = 3;
                }
                else if (!strncmp(p, words[i], 4))
                {
                    nre = 2;
                }
                else if (!strncmp(p, words[i], 2))
                {
                    nre = 1;
                }
            }
            p = words[i] + 2 * nre;
            int outword = nre;
            int q = 2;
            while (*p)
            {
                outword += (*p++ - 'a' + 6) << q;
                q += 5;
                if (q >= 32)
                {
                    ++n32;
                    err = write(fdout, buff, snprintf(buff, sizeof(buff), "0x%8.8x,\n", outword));
                    if (err < 0)
                    {
                        fprintf(stderr, "write(\"xworddict.h\", %s) -> %s\n", buff, strerror(errno));
                        exit(1);
                    }
                    outword = 0;
                    q = 2;
                }
            }
            char * suff = NULL;
            for (j = i + 1; (j < nw) && !strncmp(words[i], words[j], strlen(words[i])); ++j)
            {
                p = words[j] + strlen(words[i]);
                if (!strcmp(p, "s"))
                {
                    outword += 1 << q;
                    *words[j] = 0;
                    suff = p;
                    break;
                }
                else if (!strcmp(p, "ing"))
                {
                    outword += 2 << q;
                    *words[j] = 0;
                    suff = p;
                    break;
                }
                else if (!strcmp(p, "ed"))
                {
                    outword += 3 << q;
                    *words[j] = 0;
                    suff = p;
                    break;
                }
                else if (!strcmp(p, "d"))
                {
                    outword += 4 << q;
                    *words[j] = 0;
                    suff = p;
                    break;
                }
                else if (!strcmp(p, "ly"))
                {
                    outword += 5 << q;
                    *words[j] = 0;
                    suff = p;
                    break;
                }
            }
            ++n32;
            if (suff)
            {
                err = write(fdout, buff, snprintf(buff, sizeof(buff), "0x%8.8x, // %s(%s)\n", outword, words[i], suff));
            }
            else
            {
                err = write(fdout, buff, snprintf(buff, sizeof(buff), "0x%8.8x, // %s\n", outword, words[i]));
            }
            if (err < 0)
            {
                fprintf(stderr, "write(\"xworddict.h\", %s) -> %s\n", buff, strerror(errno));
                exit(1);
            }
            p = words[i];
        }
    }
    err = write(fdout, "};\n",3);
    if (err < 0)
    {
        fprintf(stderr, "write(\"xworddict.h\", \"};\n\") -> %s\n", strerror(errno));
        exit(1);
    }
    fprintf(stderr, "%d 32-bit words\n", n32);
#if 0
    for (i = 0; i < nw; ++i)
    {
        for (j = 0; j < i; ++j)
        {
            int l = strlen(words[j]);
            if (!strncmp(words[j], words[i], l))
            {
                int k;
                for (k = 0; k < nsuff; ++k)
                {
                    if (!strcmp(words[i] + l, suffixes[k]))
                    {
                        ++suffcounts[k];
                        goto found;
                    }
                }
                suffixes[nsuff] = words[i] + l;
                suffcounts[nsuff] = 1;
                ++nsuff;
#if 0
                for (i = 0; i < nw; ++i)
                {
                    for (j = 0; j < i; ++j)
                    {
                        int l = strlen(words[j]);
                        if (!strncmp(words[j], words[i], l))
                        {
                            int k;
                            for (k = 0; k < nsuff; ++k)
                            {
                                if (!strcmp(words[i] + l, suffixes[k]))
                                {
                                    ++suffcounts[k];
                                    goto found1;
                                }
                            }
                            suffixes[nsuff] = words[i] + l;
                            suffcounts[nsuff] = 1;
                            ++nsuff;
                            found1:;
                        }
                    }
                }
                fprintf(stderr, "longest word is %d letters\n", maxlen);
                for (i = 0; i < nsuff - 1; ++i)
                {
                    for (j = nsuff - 1; j > i; --j)
                    {
                        if (suffcounts[j] > suffcounts[i])
                        {
                            int k = suffcounts[j];
                            suffcounts[j] = suffcounts[i];
                            suffcounts[i] = k;
                            unsigned char * p = suffixes[j];
                            suffixes[j] = suffixes[i];
                            suffixes[i] = p;
                        }
                    }
                }
                fprintf(stderr, "Commonest suffixes:-\n");
                if (nsuff > 0)
                {
                    fprintf(stderr, "%s %d times\n", suffixes[0], suffcounts[0]);
                }
                if (nsuff > 1)
                {
                    fprintf(stderr, "%s %d times\n", suffixes[1], suffcounts[1]);
                }
                if (nsuff > 2)
                {
                    fprintf(stderr, "%s %d times\n", suffixes[2], suffcounts[2]);
                }
                if (nsuff > 3)
                {
                    fprintf(stderr, "%s %d times\n", suffixes[3], suffcounts[3]);
                }
                if (nsuff > 4)
                {
                    fprintf(stderr, "%s %d times\n", suffixes[4], suffcounts[4]);
                }
                if (nsuff > 5)
                {
                    fprintf(stderr, "%s %d times\n", suffixes[5], suffcounts[5]);
                }
                if (nsuff > 6)
                {
                    fprintf(stderr, "%s %d times\n", suffixes[6], suffcounts[6]);
                }
#endif
                found:;
            }
        }
    }
    fprintf(stderr, "longest word is %d letters\n", maxlen);
    for (i = 0; i < nsuff - 1; ++i)
    {
        for (j = nsuff - 1; j > i; --j)
        {
            if (suffcounts[j] > suffcounts[i])
            {
                int k = suffcounts[j];
                suffcounts[j] = suffcounts[i];
                suffcounts[i] = k;
                unsigned char * p = suffixes[j];
                suffixes[j] = suffixes[i];
                suffixes[i] = p;
            }
        }
    }
    fprintf(stderr, "Commonest suffixes:-\n");
    if (nsuff > 0)
    {
        fprintf(stderr, "%s %d times\n", suffixes[0], suffcounts[0]);
    }
    if (nsuff > 1)
    {
        fprintf(stderr, "%s %d times\n", suffixes[1], suffcounts[1]);
    }
    if (nsuff > 2)
    {
        fprintf(stderr, "%s %d times\n", suffixes[2], suffcounts[2]);
    }
    if (nsuff > 3)
    {
        fprintf(stderr, "%s %d times\n", suffixes[3], suffcounts[3]);
    }
    if (nsuff > 4)
    {
        fprintf(stderr, "%s %d times\n", suffixes[4], suffcounts[4]);
    }
    if (nsuff > 5)
    {
        fprintf(stderr, "%s %d times\n", suffixes[5], suffcounts[5]);
    }
    if (nsuff > 6)
    {
        fprintf(stderr, "%s %d times\n", suffixes[6], suffcounts[6]);
    }
#endif
}

int main (int argc __attribute__ ((unused)), char argv[] __attribute__ ((unused)))
{
    char buff[1024];
    int fdout = creat("xworddict.h", 0744);
    if (fdout < 0)
    {
        fprintf(stderr, "creat(\"xworddict.h\", 0744) -> %s\n", strerror(errno));
        exit(1);
    }
    int fd = open("words", O_RDONLY);
    if (fd < 0)
    {
        fprintf(stderr, "open(\"words\", O_RDONLY) -> %s\n", strerror(errno));
        exit(1);
    }
    onedict(fd, fdout, "dict");
    fd = close(fd);
    if (fd < 0)
    {
        fprintf(stderr, "close(\"words\") -> %s\n", strerror(errno));
        exit(1);
    }
    
    fd = open("twl06-ScrabbleUSwords", O_RDONLY);
    if (fd < 0)
    {
        fprintf(stderr, "open(\"twl06-ScrabbleUSwords\", O_RDONLY) -> %s\n", strerror(errno));
        exit(1);
    }
    onedict(fd, fdout, "scrabbleUSwords");
    fd = close(fd);
    if (fd < 0)
    {
        fprintf(stderr, "close(\"twl06-ScrabbleUSwords\") -> %s\n", strerror(errno));
        exit(1);
    }
    fd = open("sowpods-ScrabbleUKwords", O_RDONLY);
    if (fd < 0)
    {
        fprintf(stderr, "open(\"sowpods-ScrabbleUKwords\", O_RDONLY) -> %s\n", strerror(errno));
        exit(1);
    }
    onedict(fd, fdout, "scrabbleUKwords");
    fd = close(fd);
    if (fd < 0)
    {
        fprintf(stderr, "close(\"sowpods-ScrabbleUKwords\") -> %s\n", strerror(errno));
        exit(1);
    }
    int err = write(fdout, buff, snprintf(buff, sizeof(buff),
                                          "#define MAXLEN %d\n", maxlen));
    if (err < 0)
    {
        fprintf(stderr,
                "write(\"xworddict.h\", \"#define MAXLEN %d\n -> %s\n",
                maxlen, strerror(errno));
        exit(1);
    }
    return 0;
}
