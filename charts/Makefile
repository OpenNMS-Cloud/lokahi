.PHONY: package

all: package

package:
	helm package lokahi -d ./packaged
	helm repo index --url https://opennms-cloud.github.io/lokahi/charts/packaged --merge packaged/index.yaml ./packaged
