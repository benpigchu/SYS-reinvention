# SYS-reinvention

An attempt on risc-v.

## About this project

This is a project of a risc-v32i 5-stage pipeline CPU design and more.

This is one of various projects in the summer system challenge course of Tsinghua Univesity in 2018. You can find related infomations [here (In Chinese)](http://os.cs.tsinghua.edu.cn/oscourse/csproject2018).

## Folders and files

- `docker`: The tools and environments, in a docker image.
- `core`: The core CPU design, a Chisel project.
- `soc`: Fit the core into the Thinpad board, a Vivado project.(not added for now)
- `monitor`: A copy from https://github.com/char-fish-after-lunch/SystemOnCat/tree/master/prog/monitor (only modified part is added)
- `makefile`: It's just a collection of scripts.
