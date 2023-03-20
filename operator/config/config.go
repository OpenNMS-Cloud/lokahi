package config

import (
	"github.com/spf13/viper"
	"log"
)

type OperatorConfig struct {
	Version                   string `mapstructure:"VERSION"`
	DefaultOpenNMSValuesFile  string `mapstructure:"DEFAULT_OPENNMS_VALUES_FILE"`
	DefaultOpenNMSTemplateLoc string `mapstructure:"DEFAULT_OPENNMS_TEMPLATE_LOC"`
	DevMode                   bool   `mapstructure:"DEV_MODE"`
	OpenshiftMode             bool   `mapstructure:"OPENSHIFT_MODE"`
}

func LoadConfig() OperatorConfig {
	viper.AddConfigPath("./config")
	viper.SetConfigName(".env")
	viper.SetConfigType("env")

	viper.AutomaticEnv()

	err := viper.ReadInConfig()
	if err != nil {
		log.Fatal(err.Error())
	}

	var config OperatorConfig

	err = viper.Unmarshal(&config)
	if err != nil {
		log.Fatal(err.Error())
	}
	return config
}
