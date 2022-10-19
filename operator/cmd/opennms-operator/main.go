/*
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package main

import (
	"github.com/OpenNMS/opennms-operator/config"
	"github.com/OpenNMS/opennms-operator/internal/handlers"
	"github.com/OpenNMS/opennms-operator/internal/image"
	"github.com/OpenNMS/opennms-operator/internal/reconciler"
	"github.com/OpenNMS/opennms-operator/internal/scheme"
	"github.com/OpenNMS/opennms-operator/internal/util/values"
	"go.uber.org/zap/zapcore"
	"k8s.io/apimachinery/pkg/runtime/serializer"
	_ "k8s.io/client-go/plugin/pkg/client/auth/gcp"
	"os"
	ctrl "sigs.k8s.io/controller-runtime"
	"sigs.k8s.io/controller-runtime/pkg/log/zap"
)

var (
	K8sScheme = scheme.GetScheme()
	setupLog  = ctrl.Log.WithName("setup")
)

func main() {
	operatorConfig := config.LoadConfig()

	handlers.ConfigFilePath = operatorConfig.DefaultOpenNMSTemplateLoc

	loggerOptions := zap.Options{
		Development: operatorConfig.DevMode,
		TimeEncoder: zapcore.ISO8601TimeEncoder,
	}

	ctrl.SetLogger(zap.New(zap.UseFlagOptions(&loggerOptions)))
	logger := ctrl.Log.WithName("reconciler").WithName("OpenNMS")

	mgr, err := ctrl.NewManager(ctrl.GetConfigOrDie(), ctrl.Options{
		Scheme:             K8sScheme,
		MetricsBindAddress: ":9090",
		Port:               9443,
		LeaderElection:     false,
		LeaderElectionID:   "",
		Namespace:          "", // namespaced-scope when the value is not an empty string
	})
	if err != nil {
		setupLog.Error(err, "unable to define OpenNMS operator")
		os.Exit(1)
	}
	k8sClient := mgr.GetClient()

	defaultValues := values.GetDefaultValues(operatorConfig)

	imageChecker := image.NewImageUpdater(k8sClient, operatorConfig.ImageUpdateFreq)
	if err = (&reconciler.OpenNMSReconciler{
		Client:        mgr.GetClient(),
		Log:           logger,
		Scheme:        mgr.GetScheme(),
		CodecFactory:  serializer.NewCodecFactory(mgr.GetScheme()),
		Config:        operatorConfig,
		DefaultValues: defaultValues,
		Instances:     map[string]*reconciler.Instance{},
		ImageChecker:  imageChecker,
	}).SetupWithManager(mgr); err != nil {
		setupLog.Error(err, "unable to create OpenNMS controller", "controller", "OpenNMS")
		os.Exit(1)
	}

	setupLog.Info("starting operator")
	if err := mgr.Start(ctrl.SetupSignalHandler()); err != nil {
		setupLog.Error(err, "problem starting operator")
		os.Exit(1)
	}
}
