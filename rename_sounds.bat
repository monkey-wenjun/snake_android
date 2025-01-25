@echo off
cd app\src\main\res\raw

REM 重命名数字文件
for %%i in (0 1 2 3 4 5 6 7 8 9) do (
    if exist %%i.wav (
        ren "%%i.wav" "sound_%%i.wav"
    )
)

REM 重命名字母文件
for %%i in (A B C D E F G H I J K L M N O P Q R S T U V W X Y Z) do (
    if exist %%i.wav (
        ren "%%i.wav" "sound_%%i.wav"
    )
) 