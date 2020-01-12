#!/bin/sh
cp	state_bak.json state.json
rm zh-crawler.log
rm /dev/shm/zh-crawler.log
mkdir -p data
rm data/*
