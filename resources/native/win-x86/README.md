The recommended way to build the DLLs is ti use Visual Studio.

I tried a few other ways (cygwin, mingw, code::blocks, devkit) and dould not
get zeromq to build. Have, however, very little experience in Windows tooling,
so that should not be taken as any kind of definitive evidence that it is not
possible. Fortunately, I still had a copy of Visual Studio 2010. I have no
idea, however, how to script that build in the same way as for Mac OS X and
Linux, so you'll have to build it manually if you want to reproduce it.

Should you decide to build the DLLs yourself (and, really, in any kind of
security-sensitive setting, you probably should), do not forget that Visual
Studio will compile your DLLs to be dependent on MSVCPxx.dll and MSVCRxx.dll,
where xx is the version number of your Visual Studio installation.

One very good point about Visual Studio is that it is able, on an x86 machine,
to compile both the x86 and the x86_64 binaries.
