.PHONY: package

all: package

package:
	helm package opennms -d ./packaged
	helm repo index --url https://opennms-cloud.github.io/horizon-stream/charts/packaged --merge packaged/index.yaml ./packaged
