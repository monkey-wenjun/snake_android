#!/bin/bash
cd app/src/main/res/raw

# 重命名数字文件
for i in {0..9}; do
    if [ -f "$i.wav" ]; then
        mv "$i.wav" "sound_$i.wav"
    fi
done

# 重命名字母文件
for letter in {A..Z}; do
    if [ -f "$letter.wav" ]; then
        # 转换为小写并重命名
        lower=$(echo "$letter" | tr '[:upper:]' '[:lower:]')
        mv "$letter.wav" "sound_$lower.wav"
    fi
done 