.PHONY: run build travis-build-packages

run: build
	java -Xms16g -Xmx16g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -jar ./target/vostok-airlock-gate-1.0-SNAPSHOT.jar

build:
	mvn clean package

VERSION := $(shell git describe --always --tags --abbrev=0 | tail -c +2)
RELEASE := $(shell git describe --always --tags | awk '{ split($$0,a,"-"); printf "%s%s%s\n",a[2],".",a[3]}')
VENDOR := "SKB Kontur"
LICENSE := MIT
URL := https://github.com/vostok-project/airlock
DESCRIPTION := "Vostok Airlock Gate"
NAME := "vostok-airlock-gate"

travis-prepare:
	sudo apt-get -qq update
	sudo apt-get install -y rpm ruby-dev gcc make
	gem install fpm

travis-build-packages: travis-prepare build
	mkdir -p build/root/usr/lib/vostok/airlock
	mv target/vostok-airlock-gate-1.0-SNAPSHOT.jar build/root/usr/lib/vostok/airlock/gate.jar
	tar -czvPf build/vostok-airlock-gate-$(VERSION).$(RELEASE).tar.gz -C build/root  .
	fpm -t rpm \
		-s "tar" \
		--description $(DESCRIPTION) \
		--vendor $(VENDOR) \
		--url $(URL) \
		--license $(LICENSE) \
		--name $(NAME) \
		--version $(VERSION) \
		--iteration "$(RELEASE)" \
		-p build \
		build/vostok-airlock-gate-$(VERSION).$(RELEASE).tar.gz
	fpm -t deb \
		-s "tar" \
		--description $(DESCRIPTION) \
		--vendor $(VENDOR) \
		--url $(URL) \
		--license $(LICENSE) \
		--name $(NAME) \
		--version "$(VERSION)" \
		--iteration "$(RELEASE)" \
		-p build \
		build/vostok-airlock-gate-$(VERSION).$(RELEASE).tar.gz
