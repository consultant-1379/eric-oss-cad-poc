# coding: utf-8
#
# Copyright Ericsson (c) 2022

from abc import ABC, abstractmethod
from typing import Final


# TODO: Logging activities and Exception Handling
class SchemaRegistryParams(ABC):
    """
    Schema Registry Parameters Abstract configuration class  ([*] indicates that corresponding field is required) :

    Final Static Attributes :

        SUBJECT (Final str) [*]: Topic name

        SCHEMA_REGISTRY_URL (Final str) [*]: Schema Registry URL

        SSL_CA_LOCATION (Final str): Path to CA certificate file used to verify the Schema Registry's private key

        SSL_KEY_LOCATION (Final str): Path to client's private key (PEM) used for authentication

        SSL_CERTIFICATE_LOCATION (Final str): Path to client's public key (PEM) used for authentication

        BASIC_AUTH_USERNAME (Final str): Client HTTP credentials username

        BASIC_AUTH_PASSWORD (Final str): Client HTTP credentials password

    """

    SUBJECT: Final = "pm_data-value"
    SCHEMA_REGISTRY_URL: Final = "http://schema-registry:8081"
    # SCHEMA_REGISTRY_URL: Final = "http://localhost:8081"
    SSL_CA_LOCATION: Final = None
    SSL_KEY_LOCATION: Final = None
    SSL_CERTIFICATE_LOCATION: Final = None
    BASIC_AUTH_USERNAME: Final = None
    BASIC_AUTH_PASSWORD: Final = None

    """
          Schema Registry Parameters - get_conf() -> class method

          Desc:
              This method creates and return corresponding dictionary holding all configuration values of needed
              fields for the Schema Registry Client

          Returns:
              Schema registry configuration dictionary

          Note:
               - You can't set SSL_KEY_LOCATION without setting the SSL_CERTIFICATE_LOCATION
               - You have to provide both, username and password, if you want to set the authorization info of the user

    """

    @classmethod
    def get_conf(cls):
        conf = dict()
        conf["url"] = cls.SCHEMA_REGISTRY_URL
        if cls.SSL_CA_LOCATION is not None:
            conf["ssl.ca.location"] = cls.SSL_CA_LOCATION
        if cls.SSL_CERTIFICATE_LOCATION is not None:
            conf["ssl.certificate.location"] = cls.SSL_CERTIFICATE_LOCATION
            if cls.SSL_KEY_LOCATION is not None:
                conf["ssl.key.location"] = cls.SSL_KEY_LOCATION
        if (cls.BASIC_AUTH_USERNAME is not None) and (
            cls.BASIC_AUTH_PASSWORD is not None
        ):
            conf["basic.auth.user.info"] = (
                cls.BASIC_AUTH_USERNAME + ":" + cls.BASIC_AUTH_PASSWORD
            )
        return conf

    """
        Schema Registry Parameters - abstract_method() -> abstract method

        Desc:
            Prevent creating instances of this configuration class
    """

    @abstractmethod
    def abstract_method(self):
        pass
