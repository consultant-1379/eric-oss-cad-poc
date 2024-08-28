# COPYRIGHT Ericsson 2023
#
# The copyright to the computer program(s) herein is the property of
# Ericsson Inc. The programs may be used and/or copied only with written
# permission from Ericsson Inc. or in accordance with the terms and
# conditions stipulated in the agreement/contract under which the
# program(s) have been supplied.

async def async_enumerate(async_sequence, start=0):
    idx = start
    async for element in async_sequence:
        yield idx, element
        idx += 1


async def streaming_json_async_generator(generator, **kwargs):
    yield "["
    async for idx, item in async_enumerate(generator):
        if idx > 0:
            yield ","
        yield item.json(**kwargs)
    yield "]"


async def streaming_json_generator(generator, **kwargs):
    yield "["
    for idx, item in enumerate(generator):
        if idx > 0:
            yield ","
        yield item.json(**kwargs)
    yield "]"
