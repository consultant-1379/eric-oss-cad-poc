# coding: utf-8
#
# Copyright Ericsson (c) 2022

"""
    Singleton Meta Class

    Desc:
        This metaclass can be used to implement Singleton Pattern Class

"""


class SingletonMeta(type):
    _instances = {}

    def __call__(cls, *args, **kwargs):
        if cls not in cls._instances:
            instance = super(SingletonMeta, cls).__call__(*args, **kwargs)
            cls._instances[cls] = instance
        return cls._instances[cls]
