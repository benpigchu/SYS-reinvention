FROM ubuntu:18.04

WORKDIR /root/environment
# install jdk
RUN apt-get update && \
	apt-get install default-jdk -y --no-install-recommends
# install sbt
RUN apt-get install gnupg -y  --no-install-recommends && \
	echo "deb https://dl.bintray.com/sbt/debian /" | tee -a /etc/apt/sources.list.d/sbt.list && \
	apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 642AC823 && \
	apt-get update && \
	apt-get install sbt -y --no-install-recommends
# install verilator
RUN apt-get install git make autoconf g++ flex bison -y --no-install-recommends && \
	git clone http://git.veripool.org/git/verilator
RUN apt-get install libfl-dev -y --no-install-recommends
RUN cd verilator && \
	dpkg -L libfl-dev && \
	git pull && \
	git checkout verilator_3_922 && \
	unset VERILATOR_ROOT && \
	autoconf && \
	./configure && \
	make && \
	make install
# from https://stackoverflow.com/questions/6784463/error-trustanchors-parameter-must-be-non-empty/50103533#50103533
RUN /usr/bin/printf '\xfe\xed\xfe\xed\x00\x00\x00\x02\x00\x00\x00\x00\xe2\x68\x6e\x45\xfb\x43\xdf\xa4\xd9\x92\xdd\x41\xce\xb6\xb2\x1c\x63\x30\xd7\x92' > /etc/ssl/certs/java/cacerts && \
	/var/lib/dpkg/info/ca-certificates-java.postinst configure
ENTRYPOINT ["/bin/bash"]