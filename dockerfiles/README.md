# Testimages

*TODO*: automatically create multiarch images for:

- Linux
- Windows

    A container's base image OS must match the host's OS: https://docs.microsoft.com/en-us/virtualization/windowscontainers/deploy-containers/version-compatibility?tabs=windows-server-1909%2Cwindows-10-1909

    Otherwise we'll get error messages like this when trying to run an image with the wrong base image OS:

    > The container operating system does not match the host operating system

    See https://github.com/olljanat/multi-win-version-aspnet for an example of creating Windows multiarch images (source: https://github.com/moby/moby/issues/35247#issuecomment-436634221).
