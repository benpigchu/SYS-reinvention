# Docker

The tools and environments, in a docker image.

## What's inside

- Chisel related tools
	- JDK
	- scala-sbt
	- verilation
- rsicv64-gcc toolchain

## How to use

Build the docker image with `make env_build`, and then `make env` to enter the shell in the container.

Note: you need a prebuilt risc-v64 toolchain, like the one on https://files.twd2.net/riscv/ . Also you need a deb for libmpfr4 to make it run on ubuntu 18.04