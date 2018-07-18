.PHONY: env_build env_enter
docker_image ?= reinvention_env
tag ?= 0.0
innerpwd ?= /root/workspace
docker_args ?= -v reinvention_sbt_cache:/root/.ivy2/cache -v $(realpath ./):$(innerpwd) -w $(innerpwd)
env_build:
	@docker build docker/ -t $(docker_image):$(tag)

env:
	@docker run -it --rm $(docker_args) $(docker_image):$(tag)